#pragma once

void watch_init();
void watch_deinit();

void time_update();
void tz_init();

int tz_get();
void tz_set(int minutes);
extern BatteryChargeState watch_battery;
void charge_update(BatteryChargeState charge_state);
void connection_update(bool connected);
void pebblekit_update(bool connected);

void outbox_failed(DictionaryIterator *iter, AppMessageResult reason, void *context);
void outbox_sent(DictionaryIterator *iter, void *context);
void inbox_dropped(AppMessageResult reason, void *context);
