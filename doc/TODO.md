# TODO

## Bugs

- Watchface: Quiet time not working
- Relayed notification doesn't vibrate watch
  - It also doesn't repeat

## Tests

- Test repeated relay

## Features

- Notify when fully charged
- Export/Import

## Sub-functional
  
- Add instruction to exclude background management (ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

## Internal

- Manage disconnection
  - Use timer on Watch side before declaring connection closed
  - Send FRESH on timeout

## Non-functional

- Use theme and colorize
- Remove hard-coded dimensions
- Move shared data to companion objects
- Review all names
