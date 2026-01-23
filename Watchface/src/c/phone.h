#pragma once

void phone_init();
void phone_deinit();

void phone_charge(int batt, bool plugged, bool charging);
void phone_net(int network_gen, int active_sim, char *carrier);
void phone_wifi(char *text);
void phone_bt(char *id, int charge, bool active);
void phone_dnd(bool quiet);
void phone_noti(char *text);
