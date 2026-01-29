#include <pebble.h>

#include "display.h"
#include "watch.h"
#include "phone.h"
#include "dict.h"

static char home[8];
static char away[8];
static char date[12];
static int tz = 8 * 60;
static char wbat[4];
static bool connected = false;

void watch_init() {
    persist_read_string(STOR_WBAT_4, wbat, sizeof(wbat));
    disp_set(disp_wbat, wbat);
    if(!persist_read_bool(STOR_CONN_1))
	disp_connected(false);
}

void watch_deinit() {
    if (changed[STOR_WBAT_4]) {
        persist_write_string(STOR_WBAT_4, wbat);
    }
    
    if (changed[STOR_CONN_1]) {
	persist_write_bool(STOR_CONN_1, connected);
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

static void update_quiet_time() {
    static bool quiet_time = false;
    if (quiet_time != quiet_time_is_active()) {
        quiet_time = quiet_time_is_active();
        if (quiet_time) disp_set(disp_quiet, "Q");
        else disp_set(disp_quiet, "");
    }
}

void time_update() {
    time_t temp = time(NULL);
    struct tm *now = localtime(&temp);
  
    strftime(home, sizeof(home), "%H:%M", now);
    disp_set(disp_home, home);

    strftime(date, sizeof(date), "%a %d %b", now);
    disp_set(disp_date, date);

    tz_update(&temp);
    update_quiet_time();
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
static bool battery_initialized = false;
void charge_update(BatteryChargeState charge_state) {
    if (!battery_initialized) {
        watch_battery = charge_state;
        battery_initialized = true;
    } else if (watch_battery.charge_percent != charge_state.charge_percent ||
             watch_battery.is_charging != charge_state.is_charging ||
             watch_battery.is_plugged != charge_state.is_plugged) {
        watch_battery = charge_state;
        send_batt();
    }
    
    char wbat1[4];
    if (charge_state.charge_percent >= 100) strcpy(wbat1, "00");
    else {
        snprintf(wbat1, sizeof(wbat1), "%d", charge_state.charge_percent);
    }

    if (strcmp(wbat, wbat1) != 0) {
        strncpy(wbat, wbat1, sizeof(wbat));
        wbat[sizeof(wbat) - 1] = 0;
        changed[STOR_WBAT_4] = true;
        disp_set(disp_wbat, wbat);
    }
    
    update_quiet_time();
}

void connection_update(bool current) {
    update_quiet_time();
    if (connected != current) {
	connected = current;
	if (connected) {
	    send_fresh();
	} else {
	    phone_charge(0, false, false);
	    phone_dnd(false);
	    phone_noti("");
	    phone_wifi("");
	    phone_cell(0, 0, "");
	    phone_bt("", 0, 0);
	    phone_net(true);
	}
	changed[STOR_CONN_1] = true;
	disp_connected(connected);
    }
}

void outbox_failed(DictionaryIterator *iter, AppMessageResult reason, void *context) {
}

void outbox_sent(DictionaryIterator *iter, void *context) {
}

void inbox_dropped(AppMessageResult reason, void *context) {
}
