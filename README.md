# Daily Agility

A [RuneLite](https://runelite.net/) plugin for tracking daily rooftop agility goals and Marks of Grace collection.

---

## Features

- **Daily Lap Goal** — Set a target number of rooftop laps to complete each day. The counter automatically resets at midnight.
- **Daily Marks of Grace Goal** — Set a target number of Marks of Grace to collect each day. Pickups are detected automatically from inventory changes.
- **Lap Timer** — Tracks the duration of your last lap and maintains a rolling average over the last 100 laps.
- **ETA** — Estimates time remaining to complete your daily lap goal based on your average lap time.
- **Overlay** — Displays all tracked information in a configurable in-game overlay.

---

## Overlay

The overlay renders in the top-left of the screen and shows the following lines, each toggleable in settings:

| Line | Description |
|---|---|
| Laps left | Remaining laps for today. Green with `+` prefix when goal is exceeded. |
| Marks left | Remaining Marks of Grace for today. Green with `+` prefix when goal is exceeded. |
| Last lap | Duration of the most recently completed lap. |
| Avg lap | Rolling average lap time across the last 100 laps. |
| ETA | Estimated time to complete remaining laps based on average lap time. |

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
| Show Last Lap Time | Toggle last lap duration on the overlay. |
| Show ETA | Toggle estimated time remaining on the overlay. |
| Show Average Time | Toggle average lap time on the overlay. |

---

## Notes

- Lap detection is based on the in-game message: `Your <Course> Rooftop lap count is: <count>`. The plugin is confirmed working on **Seers' Village** and should work on all standard rooftop courses.
- Mark of Grace tracking reads inventory changes. The baseline is established on login, so no false counts are triggered on world hop or plugin reload.
- Daily counters reset automatically at the start of a new calendar day based on your system clock.
- Lap history is stored per course and persists across sessions.
