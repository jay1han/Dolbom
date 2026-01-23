#pragma once

enum {
    STOR_TZ_INT = 1,
    STOR_WBAT_4,
    STOR_PBAT_4,
    STOR_NET_4,
    STOR_SIM_4,
    STOR_PLMN_20,
    STOR_WIFI_20,
    STOR_BTID_20,
    STOR_BTC_4,
    STOR_DND_4,
    STOR_NOTI_16,
    STOR_BTON_4,
    STOR_END
};
extern bool changed[STOR_END];

void dict_parse(DictionaryIterator *iter, void *context);
void send_info();
void send_fresh();
void send_batt();
