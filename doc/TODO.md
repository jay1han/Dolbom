# TODO

## Bugs

- Watchface: Quiet time not working

## Tests

## Features

## Sub-functional

- Add instruction to exclude background management (ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

## Internal

- Manage disconnection
  - Use timer on Watch side before declaring connection closed
  - Send FRESH on timeout
- How deep in UI to push Context
  - [Callback-drilling](https://medium.com/proandroiddev/stop-event-drilling-in-jetpack-compose-with-composition-locals-e15004258ec5)

## Non-functional

- Move UI to ui
- Use theme and colorize
- Remove hard-coded dimensions
- Move shared data to companion objects
- Review all names
