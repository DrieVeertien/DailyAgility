# Daily Agility

A [RuneLite](https://runelite.net/) plugin for tracking daily rooftop agility goals, Marks of Grace collection, and long-term session history.

---

## Features

- **Daily Lap Goal** — Set a target number of rooftop laps to complete each day. The counter automatically resets at midnight.
- **Daily Marks of Grace Goal** — Set a target number of Marks of Grace to collect each day. Pickups are detected automatically from inventory changes.
- **Lap Timer** — Tracks the duration of your last lap and maintains a rolling average over the last 100 laps per course.
- **ETA** — Estimates time remaining to complete your daily lap goal based on your average lap time.
- **Overlay** — Displays all tracked information in a configurable in-game overlay.
- **Session Log Panel** — A side panel that records laps completed and Marks of Grace collected per course per day, stored indefinitely across sessions.
- **CSV Export** — Export your full session log to a CSV file from the panel for use in spreadsheets or other tools.

---

## Overlay

The overlay renders in the top-left of the screen and shows the following lines, each toggleable in settings:

| Line | Description |
|---|---|
| Laps left | Remaining laps for today. Green with `+` prefix when goal is exceeded. |
| Marks left | Remaining Marks of Grace for today. Green with `+` prefix when goal is exceeded. |
| Last lap | Duration of the most recently completed lap. |
| Avg lap | Rolling average lap time across the last 100 laps for the current course. |
| ETA | Estimated time to complete remaining laps based on average lap time. |

### Overlay Display Modes

The overlay can be configured to show under different conditions:

| Mode | Behaviour |
|---|---|
| Always | Overlay is always visible. |
| Show when goal is not completed | Hides once both lap and mark goals are met. |
| Only show when running rooftops | Visible only while actively running. Appears on the first detected lap and automatically hides after 10 minutes with no lap completed (e.g. when you switch to other content). |
| Never | Overlay is disabled entirely. |

---

## Session Log Panel

Accessible via the sidebar icon. Records a permanent log of every day you have run rooftop agility, broken down by course.

### Calendar
- Displays the current month with navigation arrows to move between months.
- Days with recorded data are highlighted in green, including today as soon as its first lap is logged.
- Today is marked with a gold border.
- Clicking a day filters the log list below to only show entries for that date.
- **Show all dates** clears the filter and shows the full history.
- The view updates live as you run: completing a lap refreshes the list only when you are viewing today or all dates, so a date filter you have selected is never reset out from under you.

### Log Entries
Each entry shows:
- Date
- Course name
- Laps completed
- Marks of Grace collected

Log data is stored indefinitely in RuneLite's configuration store and persists across sessions, reinstalls, and world hops.

### Export

The **Export** button (next to *Show all dates*) saves your entire session log to a CSV file. A save dialog lets you choose the location and filename (defaulting to `daily-agility-<date>.csv`). The file contains one row per course per day with the columns:

```
date,course,laps,marks
```

The export runs in the background so it stays responsive even with a large history.

---

## Configuration

Settings are grouped into three sections:

### Laps
| Setting | Description |
|---|---|
| Daily Lap Goal | Number of rooftop laps to complete today. |

### Marks of Grace
| Setting | Description |
|---|---|
| Daily Marks Goal | Number of Marks of Grace to collect today. |

### Display
| Setting | Description |
|---|---|
| Overlay Mode | Controls when the overlay is rendered. |
| Show Laps Left | Toggle remaining lap count on the overlay. |
| Show Marks Left | Toggle remaining mark count on the overlay. |
| Show Last Lap Time | Toggle last lap duration on the overlay. |
| Show Average Time | Toggle average lap time on the overlay. |
| Show ETA | Toggle estimated time remaining on the overlay. |
| Log daily laps | When enabled, completed laps and mark pickups are recorded to the session log. Disabling this stops new data from being written but does not delete existing history. |

---

## Notes

- Lap detection is based on the in-game message: `Your <Course> Rooftop lap count is: <count>`. Confirmed working on all standard rooftop courses.
- Mark of Grace tracking reads inventory changes. The baseline is established on login, so no false counts are triggered on world hop or plugin reload.
- Daily counters reset automatically at the start of a new calendar day based on your system clock. The reset is checked on plugin startup, on login, and on each lap, so it still applies if you leave the client running and log back in the next day.
- Laps with a gap of more than 10 minutes since the previous lap are not timed — this prevents inflated lap times when you were logged out or AFK between laps. The lap still counts toward your daily goal; only its duration is skipped.
- Lap history used for average lap time and ETA is stored per course and capped at the last 100 laps.
- Session log history is unbounded and stored per course per day indefinitely.
