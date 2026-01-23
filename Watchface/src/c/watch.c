#include <pebble.h>

#include "display.h"
#include "watch.h"
#include "dict.h"

static char home[8];
static char away[8];
static char date[12];
static int tz = 8 * 60;
static char wbat[4];

void watch_init() {
    persist_read_string(STOR_WBAT_4, wbat, sizeof(wbat));
    disp_set(disp_wbat, wbat);
}

void watch_deinit() {
    if (changed[STOR_WBAT_4]) {
        persist_write_string(STOR_WBAT_4, wbat);
    }
}

static void tz_update(time_t *temp) {
    time_t now;
    if (temp == NULL) {
        now = time(NULL);
        temp = &now;
    }
    *temp += tz * 60;
    strftime(away, sizeof(away), "%H:%M", localtime(temp));
    disp_set(disp_away, away);
}

void time_update() {
    time_t temp = time(NULL);
    struct tm *now = localtime(&temp);
  
    strftime(home, sizeof(home), "%H:%M", now);
    disp_set(disp_home, home);

    strftime(date, sizeof(date), "%a %d %b", now);
    disp_set(disp_date, date);

    tz_update(&temp);
}

void tz_set(int minutes) {
    if (minutes != tz)
        persist_write_int(STOR_TZ_INT, minutes);
    tz = minutes;
    time_update();
}

void tz_init() {
    tz = persist_read_int(STOR_TZ_INT);
}

int tz_get() {
    return tz;
}

BatteryChargeState watch_battery = {0, false, false};

void charge_update(BatteryChargeState charge_state) {
    char wbat1[4];
    watch_battery = charge_state;
    if (charge_state.charge_percent >= 100) strcpy(wbat1, "00");
    else {
        snprintf(wbat1, sizeof(wbat1), "%d", charge_state.charge_percent);
    }

    if (strcmp(wbat, wbat1) != 0) {
        strncpy(wbat, wbat1, sizeof(wbat));
        wbat[sizeof(wbat) - 1] = 0;
        changed[STOR_WBAT_4] = true;
        disp_set(disp_wbat, wbat);
        send_batt();
    }
}

static bool conn_app = false;
static bool conn_kit = false;

static void connection_disp() {
    disp_connected(conn_app && conn_kit);
}

void connection_update(bool connected) {
    conn_app = connected;
    connection_disp();
}

void pebblekit_update(bool connected) {
    conn_kit = connected;
    connection_disp();
}

void outbox_failed(DictionaryIterator *iter, AppMessageResult reason, void *context) {
}

void outbox_sent(DictionaryIterator *iter, void *context) {
}

void inbox_dropped(AppMessageResult reason, void *context) {
}
