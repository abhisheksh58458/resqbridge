package resqbridge;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * All MySQL access lives here.
 *
 *  - authenticate()  checks the users table (passwords are SHA-256 hashed in SQL).
 *  - loadInto()      reads every table into the in-memory DataStore after sign-in.
 *  - snapshot()      turns the DataStore's current state into a list of SQL statements
 *                    which submit() runs on a background thread inside one transaction,
 *                    so the UI never waits on the database.
 *
 * Requires the MySQL Connector/J jar on the runtime classpath (see README).
 */
final class Database {

    private Database() {}

    // ---------------------------------------------------------------- configuration

    private static String url = "jdbc:mysql://localhost:3306/resqbridge"
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";
    private static String user = "root";
    private static String password = "";
    private static boolean configLoaded = false;

    static synchronized void loadConfig() {
        if (configLoaded) return;
        configLoaded = true;
        DriverManager.setLoginTimeout(5);
        Properties p = new Properties();
        String path = System.getProperty("resqbridge.config", "db.properties");
        try (FileInputStream in = new FileInputStream(path)) {
            p.load(in);
        } catch (IOException ignored) {
            // No file: fall back to the defaults above.
        }
        url = p.getProperty("db.url", url);
        user = p.getProperty("db.user", user);
        password = p.getProperty("db.password", password);
    }

    /** Short human-readable target, e.g. "localhost:3306/resqbridge". */
    static String describe() {
        loadConfig();
        return url.replaceFirst("^jdbc:mysql://", "").replaceFirst("\\?.*$", "");
    }

    static Connection connect() throws SQLException {
        loadConfig();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL Connector/J driver not found on the classpath.", "08001", e);
        }
        return DriverManager.getConnection(url, user, password);
    }

    /** Connects and counts users; proves the server, credentials and schema are all in place. */
    static int ping() throws SQLException {
        try (Connection c = connect();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    static String friendly(Throwable t) {
        String msg = t.getMessage() == null ? "" : t.getMessage();
        if (t instanceof SQLException) {
            SQLException se = (SQLException) t;
            String state = se.getSQLState() == null ? "" : se.getSQLState();
            if (msg.contains("Connector/J driver not found")) {
                return "MySQL driver missing: put the Connector/J jar on the classpath (see README).";
            }
            if (state.startsWith("08")) {
                return "Cannot reach MySQL at " + describe() + ". Is the server running?";
            }
            if (state.equals("28000")) {
                return "MySQL rejected the username/password in db.properties.";
            }
            if (state.equals("42000") && msg.toLowerCase().contains("unknown database")) {
                return "Database 'resqbridge' not found. Run schema.sql first.";
            }
            if (state.equals("42S02")) {
                return "Tables not found. Run schema.sql first.";
            }
        }
        String shortMsg = msg.length() > 90 ? msg.substring(0, 90) + "..." : msg;
        return "Database error: " + shortMsg;
    }

    // ---------------------------------------------------------------- authentication

    /** Returns the matching active account, or null if the username/password is wrong. */
    static LoginAccount authenticate(String username, String plainPassword) throws SQLException {
        try (Connection c = connect()) {
            LoginAccount found = null;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT username, display_name, user_role, linked_driver_name, linked_camp_name "
                            + "FROM users WHERE username = ? AND password_hash = SHA2(?, 256) AND active = 1")) {
                ps.setString(1, username);
                ps.setString(2, plainPassword);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        found = new LoginAccount(rs.getString(1), "", rs.getString(2), rs.getString(3),
                                rs.getString(4), rs.getString(5));
                    }
                }
            }
            if (found != null) {
                try (PreparedStatement ps = c.prepareStatement("UPDATE users SET last_login = NOW() WHERE username = ?")) {
                    ps.setString(1, found.username);
                    ps.executeUpdate();
                }
            }
            return found;
        }
    }

    // ---------------------------------------------------------------- loading

    private interface RowHandler {
        void handle(ResultSet rs) throws SQLException;
    }

    private static void query(Connection c, String sql, RowHandler h) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) h.handle(rs);
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    /** Replaces everything in the store with what is currently in MySQL. */
    static void loadInto(DataStore s) throws SQLException {
        try (Connection c = connect()) {
            s.camps.clear();
            query(c, "SELECT name, supply_percent, x_frac, y_frac, is_hub, operational FROM camps ORDER BY seq",
                    rs -> s.camps.add(new Camp(rs.getString(1), rs.getInt(2), rs.getDouble(3), rs.getDouble(4),
                            rs.getBoolean(5), rs.getBoolean(6))));

            s.dispatches.clear();
            query(c, "SELECT item, from_loc, to_loc, time_ago, units FROM dispatches ORDER BY seq",
                    rs -> s.dispatches.add(new Dispatch(rs.getString(1), rs.getString(2), rs.getString(3),
                            rs.getString(4), rs.getInt(5))));

            s.donors.clear();
            query(c, "SELECT name, amount FROM donors ORDER BY id",
                    rs -> s.donors.add(new Donor(rs.getString(1), rs.getLong(2))));

            s.drivers.clear();
            query(c, "SELECT name, vehicle, on_duty, destination, reached, fuel_percent, maintenance_issue, "
                    + "shift_start_epoch FROM drivers ORDER BY seq", rs -> {
                Driver d = new Driver(rs.getString(1), rs.getString(2), rs.getBoolean(3), nz(rs.getString(4)),
                        rs.getBoolean(5));
                d.fuelPercent = rs.getInt(6);
                d.maintenanceIssue = nz(rs.getString(7));
                long epoch = rs.getLong(8);
                if (!rs.wasNull()) {
                    d.shiftStart = LocalDateTime.ofInstant(Instant.ofEpochSecond(epoch), ZoneId.systemDefault());
                }
                s.drivers.add(d);
            });

            s.alerts.clear();
            query(c, "SELECT title, detail, severity FROM alerts ORDER BY seq",
                    rs -> s.alerts.add(new AlertItem(rs.getString(1), rs.getString(2), rs.getString(3))));

            s.inventory.clear();
            query(c, "SELECT category, percent_value FROM inventory ORDER BY seq",
                    rs -> s.inventory.put(rs.getString(1), rs.getInt(2)));

            s.pendingAccounts.clear();
            query(c, "SELECT name, email, requested_role, date_requested FROM pending_accounts ORDER BY seq",
                    rs -> s.pendingAccounts.add(new PendingAccount(rs.getString(1), rs.getString(2),
                            rs.getString(3), rs.getString(4))));

            s.hazardReports.clear();
            query(c, "SELECT reporter, location, type, severity, time_text FROM hazard_reports ORDER BY seq",
                    rs -> s.hazardReports.add(new HazardReport(rs.getString(1), rs.getString(2), rs.getString(3),
                            rs.getString(4), rs.getString(5))));

            s.safeZones.clear();
            query(c, "SELECT name, type, distance_km FROM safe_zones ORDER BY seq",
                    rs -> s.safeZones.add(new SafeZone(rs.getString(1), rs.getString(2), rs.getDouble(3))));

            s.equipment.clear();
            query(c, "SELECT name, total_count, in_use FROM equipment ORDER BY seq",
                    rs -> s.equipment.add(new EquipmentItem(rs.getString(1), rs.getInt(2), rs.getInt(3))));

            s.chatMessages.clear();
            query(c, "SELECT channel, from_name, from_role, message_text, time_text FROM chat_messages ORDER BY seq",
                    rs -> s.chatMessages.add(new ChatMessage(rs.getString(1), rs.getString(2), rs.getString(3),
                            rs.getString(4), rs.getString(5))));

            // Newest first, matching how addAudit() inserts at index 0.
            s.auditLog.clear();
            query(c, "SELECT time_text, description FROM audit_log ORDER BY id DESC LIMIT 300",
                    rs -> s.auditLog.add(new AuditLogEntry(rs.getString(1), rs.getString(2))));

            // Delivery runs plus their manifest lines and discrepancies.
            s.deliveryRuns.clear();
            Map<Integer, DeliveryRun> runs = new HashMap<>();
            query(c, "SELECT id, driver_name, from_location, to_camp, status, eta_minutes, check_in_time, "
                    + "check_out_time, proof_signed, proof_note, storage_allocation, offload_team "
                    + "FROM delivery_runs ORDER BY seq", rs -> {
                DeliveryRun r = new DeliveryRun(rs.getString(2), rs.getString(3), rs.getString(4),
                        new ArrayList<ManifestItem>(), rs.getString(5), rs.getInt(6));
                r.checkInTime = nz(rs.getString(7));
                r.checkOutTime = nz(rs.getString(8));
                r.proofSigned = rs.getBoolean(9);
                r.proofNote = nz(rs.getString(10));
                r.storageAllocation = nz(rs.getString(11));
                r.offloadTeam = nz(rs.getString(12));
                runs.put(rs.getInt(1), r);
                s.deliveryRuns.add(r);
            });
            query(c, "SELECT run_id, name, qty, priority FROM manifest_items ORDER BY run_id, seq", rs -> {
                DeliveryRun r = runs.get(rs.getInt(1));
                if (r != null) r.items.add(new ManifestItem(rs.getString(2), rs.getInt(3), rs.getString(4)));
            });
            query(c, "SELECT run_id, description FROM run_discrepancies ORDER BY run_id, seq", rs -> {
                DeliveryRun r = runs.get(rs.getInt(1));
                if (r != null) r.discrepancies.add(rs.getString(2));
            });

            // Key/value settings.
            Map<String, String> kv = new HashMap<>();
            query(c, "SELECT skey, svalue FROM settings", rs -> kv.put(rs.getString(1), rs.getString(2)));
            applyFields(kv, "admin.", s.adminProfile);
            applyFields(kv, "comms.", s.commsSettings);
            applyFields(kv, "integration.", s.integrationSettings);
            applyFields(kv, "security.", s.securitySettings);

            s.totalSuppliesDispatched = intOr(kv.get("total_supplies_dispatched"), s.totalSuppliesDispatched);
            s.totalCampsInNetwork = intOr(kv.get("total_camps_in_network"), s.totalCampsInNetwork);
            s.gateMaxCapacity = intOr(kv.get("gate_max_capacity"), s.gateMaxCapacity);
            s.holdIncomingTrucks = Boolean.parseBoolean(kv.getOrDefault("hold_incoming_trucks", "false"));

            s.resourceCategories.clear();
            for (String cat : splitList(kv.get("resource_categories"))) s.resourceCategories.add(cat);

            for (String role : s.roleNames) {
                String raw = kv.get("perm." + role);
                if (raw == null) continue;
                Set<String> set = s.rolePermissions.computeIfAbsent(role, k -> new LinkedHashSet<>());
                set.clear();
                set.addAll(splitList(raw));
            }
        }
    }

    private static int intOr(String v, int fallback) {
        try {
            return v == null ? fallback : Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static List<String> splitList(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null) return out;
        for (String part : raw.split("\\|")) {
            if (!part.trim().isEmpty()) out.add(part.trim());
        }
        return out;
    }

    /** Copies "<prefix><fieldName>" settings onto the matching boolean/int/String fields of obj. */
    private static void applyFields(Map<String, String> kv, String prefix, Object obj) {
        for (Field f : obj.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            String v = kv.get(prefix + f.getName());
            if (v == null) continue;
            try {
                f.setAccessible(true);
                Class<?> t = f.getType();
                if (t == boolean.class) f.setBoolean(obj, Boolean.parseBoolean(v));
                else if (t == int.class) f.setInt(obj, Integer.parseInt(v.trim()));
                else if (t == String.class) f.set(obj, v);
            } catch (IllegalAccessException | NumberFormatException ignored) {
                // Leave the default value in place.
            }
        }
    }

    private static void collectFields(List<String[]> out, String prefix, Object obj) {
        for (Field f : obj.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            Class<?> t = f.getType();
            if (t != boolean.class && t != int.class && t != String.class) continue;
            try {
                f.setAccessible(true);
                Object v = f.get(obj);
                out.add(new String[]{prefix + f.getName(), v == null ? "" : String.valueOf(v)});
            } catch (IllegalAccessException ignored) {
                // Skip unreadable fields.
            }
        }
    }

    // ---------------------------------------------------------------- saving

    /** One SQL statement plus its bound parameters. */
    static final class Stmt {
        final String sql;
        final Object[] params;

        Stmt(String sql, Object... params) {
            this.sql = sql;
            this.params = params;
        }
    }

    /**
     * Captures the whole current state as SQL. Must be called on the UI thread (it reads the
     * live lists); the returned statements are plain data and safe to run on any thread.
     * The users and audit_log tables are deliberately left alone.
     */
    static List<Stmt> snapshot(DataStore s) {
        List<Stmt> o = new ArrayList<>();
        int i;

        o.add(new Stmt("DELETE FROM camps"));
        i = 0;
        for (Camp x : s.camps) {
            o.add(new Stmt("INSERT INTO camps (seq, name, supply_percent, x_frac, y_frac, is_hub, operational) "
                    + "VALUES (?,?,?,?,?,?,?)", i++, x.name, x.supplyPercent, x.xFrac, x.yFrac, x.hub, x.operational));
        }

        o.add(new Stmt("DELETE FROM dispatches"));
        i = 0;
        for (Dispatch x : s.dispatches) {
            o.add(new Stmt("INSERT INTO dispatches (seq, item, from_loc, to_loc, time_ago, units) VALUES (?,?,?,?,?,?)",
                    i++, x.item, x.from, x.to, x.timeAgo, x.units));
        }

        o.add(new Stmt("DELETE FROM donors"));
        for (Donor x : s.donors) {
            o.add(new Stmt("INSERT INTO donors (name, amount) VALUES (?,?)", x.name, x.amount));
        }

        o.add(new Stmt("DELETE FROM drivers"));
        i = 0;
        for (Driver x : s.drivers) {
            Long epoch = x.shiftStart == null ? null
                    : Long.valueOf(x.shiftStart.atZone(ZoneId.systemDefault()).toEpochSecond());
            o.add(new Stmt("INSERT INTO drivers (seq, name, vehicle, on_duty, destination, reached, fuel_percent, "
                    + "maintenance_issue, shift_start_epoch) VALUES (?,?,?,?,?,?,?,?,?)",
                    i++, x.name, x.vehicle, x.onDuty, x.destination, x.reached, x.fuelPercent,
                    x.maintenanceIssue, epoch));
        }

        o.add(new Stmt("DELETE FROM alerts"));
        i = 0;
        for (AlertItem x : s.alerts) {
            o.add(new Stmt("INSERT INTO alerts (seq, title, detail, severity) VALUES (?,?,?,?)",
                    i++, x.title, x.detail, x.severity));
        }

        o.add(new Stmt("DELETE FROM inventory"));
        i = 0;
        for (Map.Entry<String, Integer> e : s.inventory.entrySet()) {
            o.add(new Stmt("INSERT INTO inventory (seq, category, percent_value) VALUES (?,?,?)",
                    i++, e.getKey(), e.getValue()));
        }

        o.add(new Stmt("DELETE FROM pending_accounts"));
        i = 0;
        for (PendingAccount x : s.pendingAccounts) {
            o.add(new Stmt("INSERT INTO pending_accounts (seq, name, email, requested_role, date_requested) "
                    + "VALUES (?,?,?,?,?)", i++, x.name, x.email, x.requestedRole, x.dateRequested));
        }

        o.add(new Stmt("DELETE FROM hazard_reports"));
        i = 0;
        for (HazardReport x : s.hazardReports) {
            o.add(new Stmt("INSERT INTO hazard_reports (seq, reporter, location, type, severity, time_text) "
                    + "VALUES (?,?,?,?,?,?)", i++, x.reporter, x.location, x.type, x.severity, x.time));
        }

        o.add(new Stmt("DELETE FROM safe_zones"));
        i = 0;
        for (SafeZone x : s.safeZones) {
            o.add(new Stmt("INSERT INTO safe_zones (seq, name, type, distance_km) VALUES (?,?,?,?)",
                    i++, x.name, x.type, x.distanceKm));
        }

        o.add(new Stmt("DELETE FROM equipment"));
        i = 0;
        for (EquipmentItem x : s.equipment) {
            o.add(new Stmt("INSERT INTO equipment (seq, name, total_count, in_use) VALUES (?,?,?,?)",
                    i++, x.name, x.total, x.inUse));
        }

        o.add(new Stmt("DELETE FROM chat_messages"));
        i = 0;
        for (ChatMessage x : s.chatMessages) {
            o.add(new Stmt("INSERT INTO chat_messages (seq, channel, from_name, from_role, message_text, time_text) "
                    + "VALUES (?,?,?,?,?,?)", i++, x.channel, x.fromName, x.fromRole, x.text, x.time));
        }

        // Runs: children first, then parents. Run ids are simply their 1-based position.
        o.add(new Stmt("DELETE FROM manifest_items"));
        o.add(new Stmt("DELETE FROM run_discrepancies"));
        o.add(new Stmt("DELETE FROM delivery_runs"));
        int runId = 0;
        for (DeliveryRun r : s.deliveryRuns) {
            runId++;
            o.add(new Stmt("INSERT INTO delivery_runs (id, seq, driver_name, from_location, to_camp, status, "
                    + "eta_minutes, check_in_time, check_out_time, proof_signed, proof_note, storage_allocation, "
                    + "offload_team) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    runId, runId - 1, r.driverName, r.fromLocation, r.toCamp, r.status, r.etaMinutes,
                    r.checkInTime, r.checkOutTime, r.proofSigned, r.proofNote, r.storageAllocation, r.offloadTeam));
            int k = 0;
            for (ManifestItem m : r.items) {
                o.add(new Stmt("INSERT INTO manifest_items (run_id, seq, name, qty, priority) VALUES (?,?,?,?,?)",
                        runId, k++, m.name, m.qty, m.priority));
            }
            k = 0;
            for (String d : r.discrepancies) {
                o.add(new Stmt("INSERT INTO run_discrepancies (run_id, seq, description) VALUES (?,?,?)",
                        runId, k++, d));
            }
        }

        // Settings as key/value rows.
        List<String[]> kv = new ArrayList<>();
        collectFields(kv, "admin.", s.adminProfile);
        collectFields(kv, "comms.", s.commsSettings);
        collectFields(kv, "integration.", s.integrationSettings);
        collectFields(kv, "security.", s.securitySettings);
        kv.add(new String[]{"total_supplies_dispatched", String.valueOf(s.totalSuppliesDispatched)});
        kv.add(new String[]{"total_camps_in_network", String.valueOf(s.totalCampsInNetwork)});
        kv.add(new String[]{"gate_max_capacity", String.valueOf(s.gateMaxCapacity)});
        kv.add(new String[]{"hold_incoming_trucks", String.valueOf(s.holdIncomingTrucks)});
        kv.add(new String[]{"resource_categories", String.join("|", s.resourceCategories)});
        for (Map.Entry<String, Set<String>> e : s.rolePermissions.entrySet()) {
            kv.add(new String[]{"perm." + e.getKey(), String.join("|", e.getValue())});
        }
        o.add(new Stmt("DELETE FROM settings"));
        for (String[] pair : kv) {
            o.add(new Stmt("INSERT INTO settings (skey, svalue) VALUES (?,?)", pair[0], pair[1]));
        }
        return o;
    }

    static Stmt auditInsert(String time, String description) {
        return new Stmt("INSERT INTO audit_log (time_text, description) VALUES (?,?)", time, description);
    }

    private static void execute(List<Stmt> stmts) throws SQLException {
        try (Connection c = connect()) {
            c.setAutoCommit(false);
            try {
                for (Stmt st : stmts) {
                    try (PreparedStatement ps = c.prepareStatement(st.sql)) {
                        for (int i = 0; i < st.params.length; i++) {
                            Object v = st.params[i];
                            if (v == null) ps.setNull(i + 1, Types.NULL);
                            else ps.setObject(i + 1, v);
                        }
                        ps.executeUpdate();
                    }
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private static final ExecutorService WRITER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "resqbridge-db-writer");
        t.setDaemon(true);
        return t;
    });

    /** Last background-save error, or null when the most recent save succeeded. */
    static volatile String lastError = null;

    /** Queues statements to run, in order, on the single background writer thread. */
    static void submit(List<Stmt> stmts) {
        WRITER.submit(() -> {
            try {
                execute(stmts);
                lastError = null;
            } catch (SQLException e) {
                lastError = friendly(e);
                System.err.println("[ResQBridge] Database save failed: " + e.getMessage());
            }
        });
    }

    /** Blocks (up to 10 s) until every queued save has finished. */
    static void drain() {
        try {
            WRITER.submit(() -> { }).get(10, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            // Timed out or interrupted: nothing more we can do on shutdown.
        }
    }

    static List<Stmt> single(Stmt st) {
        return Arrays.asList(st);
    }
}
