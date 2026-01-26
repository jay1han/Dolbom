# TODO

## Bugs

- Watchface: Quiet time not working

## Tests

## Features

- Backup/Restore

## Sub-functional

- Watch sends all info with FRESH
- Add instruction to exclude background management (ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
- Replace some Dialogs with full screen and BackHandler

## Internal

- Manage disconnection
  - Use timer on Watch side before declaring connection closed
  - Send FRESH on timeout
- How deep in UI to push Context

## Non-functional

- Move UI to ui
- Use theme and colorize
- Remove hard-coded dimensions
- Move shared data to companion objects
- Review all names
