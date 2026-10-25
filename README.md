# ResQBridge — Disaster Relief Logistics System (Java Swing)

A desktop recreation of the ResQBridge dashboard, built with plain Java Swing
(no external libraries) so it compiles and runs anywhere a JDK is installed.

## Requirements
- JDK 11 or newer (tested against the language features used; any recent
  JDK — 11, 17, 21 — will work).

## Build

From the `resqbridge` folder (the one containing `src/`):

```bash
mkdir -p out
javac -d out src/resqbridge/*.java
```

## Run

```bash
java -cp out resqbridge.ResQBridgeApp
```

A 1440x900 window opens showing the dashboard.

## What's interactive

- **Sidebar navigation** — switches between Dashboard, Relief Camps, Donors,
  Supply Inventory, Dispatch, Drivers, Alerts, Reports and Settings.
- **Map markers** — click any camp marker (or the Kochi hub) to open an edit
  dialog and change its supply level / operational status. The map, the
  "Active Relief Camps" stat card, and the Relief Camps page all update
  together, since they share one in-memory `DataStore`.
- **Recent Dispatches** — "+ Add" opens a form to log a new dispatch; it
  appears at the top of the list and increases "Total Supplies Dispatched".
- **Supply Inventory** — each category (Medical, Food, Water, Shelter) has a
  spinner; changing it redraws the donut chart and recalculates the overall
  stock percentage live.
- **Top Donors** — "+ Add" opens a form to add a new donor, and each row has
  a delete (✕) button (with a confirmation prompt) to remove unwanted donors.
  The list re-sorts by amount automatically.
- **Drivers page** — "+ Add Driver" opens a form (name, vehicle, optional
  destination, on/off duty). Each driver row shows whether they're
  **En route** or have **Reached** their destination, with a button to
  toggle that, an on/off-duty toggle, and a **Remove** button (with
  confirmation) to take them off the roster. The "Active Drivers" stat
  updates immediately.
- **Alerts page** — "Resolve" removes an alert and updates the "Critical
  Alerts" count on the dashboard.
- **Relief Camps page** — a slider per camp for quick bulk editing, plus an
  operational checkbox.
- **Settings page** — a full admin console:
  - **Admin profile** — edit the signed-in admin's name, email and phone.
  - **Users & Roles** — a role-based access control matrix (Admin / Field
    Rescue Team / Volunteer / Citizen × permissions) with checkboxes;
    a Staff Verification queue to approve/reject new accounts; and a live
    Activity Audit Log that records every meaningful action taken anywhere
    in the app (camp edits, dispatches, driver/donor changes, settings
    changes, etc.) for post-incident review.
  - **Alerts & Communication** — toggle broadcast channels (push, SMS,
    email, emergency voice), configure a Common Alerting Protocol (CAP)
    template (sender ID, category, urgency, severity, certainty), and set
    the default geo-fencing radius for hazard warnings.
  - **Integrations & Data** — manage API keys/connection status for a
    weather feed (NOAA), mapping tools, and seismic data (USGS); configure
    default resource-inventory categories (add/remove); and set offline
    sync bandwidth limits and sync interval for low-connectivity field use.
  - **Security & System** — enforce MFA/OTP for elevated accounts, set data
    retention period and data residency, and toggle system-wide maintenance
    mode with a publishable safety notice.

## Project structure

```
src/resqbridge/
  Theme.java              Color palette and fonts
  UiUtils.java             RoundedPanel + hand-drawn AppIcon pictograms
  DataModels.java          All data models (Camp, Dispatch, Donor, Driver,
                           AlertItem, AdminProfile, PendingAccount,
                           AuditLogEntry, CommunicationSettings,
                           IntegrationSettings, SecuritySettings) + DataStore
  HeaderPanel.java         Top bar (logo, live indicator, admin)
  SidebarPanel.java        Left navigation
  StatCardPanel.java       Reusable stat card (top-right column)
  MapPanel.java            Kerala relief network map + routes + markers
  EditCampDialog.java      Edit a camp's supply level
  DispatchCardPanel.java   Recent Dispatches card + Add Dispatch dialog
  InventoryCardPanel.java  Supply Inventory donut chart + editable legend
  DonorsCardPanel.java     Top Donors card + Add/Delete Donor
  DashboardPanel.java      Assembles the dashboard layout
  CampsPanel.java          Full Relief Camps management page
  DriversPanel.java        Drivers page: add / remove / en-route / reached
  AlertsPanel.java         Alerts page
  SimplePagePanel.java     Placeholder page (Reports)
  SettingsPanel.java       Full admin settings console (profile, users &
                           roles, alerts & comms, integrations & data,
                           security & system)
  ResQBridgeApp.java       Main entry point (JFrame + page switching)
```

## Notes

- This was written and reviewed by hand in a sandbox without a JDK available
  to compile-test it (no `javac` on the box, and network access to install
  one is blocked), so please run the build step above and let me know if you
  hit any compiler error — happy to fix it immediately.
- All icons are drawn in code (`AppIcon`), so there are no external image
  assets or network calls; the whole app is self-contained.
