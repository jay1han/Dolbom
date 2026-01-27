# Dolbom Watchface

Current limitations:

- Only works on Aplite and Basalt

## Screen size

| Platform       | Screen size |
|----------------|-------------|
| Aplite, Basalt | 144x168     |
| Time 2         | 200x228     |

## Fonts

- Roboto 49
- Gothic 28 Bold
- Gothic 24 Bold
- Gothic 18 Bold

## Display layout

| ID     | Y Pos | Position | Item              | Text         | Font      |
|--------|-------|----------|-------------------|--------------|-----------|
| `date` | -10   | Center   | Date              | `Sun 31 Dec` | Gothic 28 |
| `dnd`  | -10   | Left     | Quiet mode        | `Q`          | Gothic 28 |
| `pchg` | -10   | Right    | Phone charging    | `C`          | Gothic 28 |
| `conn` | 15    | Left     | Connection status | `C`          | Gothic 18 |
| `home` | 16    | Line     | Home time         | `23:39`      | Roboto 49 |
| `noti` | 68    | Center   | Notifications     | `GK+`        | Gothic 28 |
| `btc`  | 96    | Left     | Bluetooth charge  | `90`         | Gothic 24 |
| `btid` | 96    | Line     | Bluetooth ID      | `Bose`       | Gothic 24 |
| `net`  | 117   | Left     | Network           | `4G`         | Gothic 24 |
| `wifi` | 117   | Line     | WiFi              | `SFR_box`    | Gothic 24 |
| `away` | 140   | Center   | Away date/time    | `03:39`      | Gothic 28 |
| `wbat` | 140   | Left     | Watch Battery     | `99`         | Gothic 28 |
| `pbat` | 140   | Right    | Phone Battery     | `99`         | Gothic 28 |

## Backgrounds

In order to compartmentalize all the fields,
some are black on white and others are white on black.
Black backgrounds are drawn first, then the text overlayed on top.
A small patch of white background is also used to create a white gap.

Note that most text layers extend beyond their expected area,
so that we can use *ellipsis* word-wrapping without actually seeing the three dots.

- (0, 0, 144, 21): `date` and `pchg`

- (0, 72, 144, 26): `noti`

- (0, 147, 144, 22): `away` and `dnd`

# Display items

## Watch state

Date and time is based on the Pebble internal clock, which is synchronized with the phone.
The away time is calculated as a constant offset from the home time,
the offset being defined in hours and minutes (positive or negative).

A one-character code represents the App and PebbleKit connection status.

|               | App OK | App NOK |
|---------------|:------:|:-------:|
| PebbleKit OK  | `C`    | `?`     |
| PebbleKit NOK | `-`    | `X`     |

The watch battery is standard Pebble C SDK.

