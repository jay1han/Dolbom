#include <pebble.h>

#include "display.h"
#include "watch.h"
#include "phone.h"
#include "dict.h"

static Window *s_main_window;

static void tick_handler(struct tm *tick_time, TimeUnits units_changed) {
    time_update();
}

static void main_window_load(Window *window) {
    Layer *window_layer = window_get_root_layer(window);

    disp_create(window_layer);
}

static void main_window_unload(Window *window) {
    disp_destroy();
}

static void init() {
    APP_LOG(APP_LOG_LEVEL_INFO, "start");
    s_main_window = window_create();

    window_set_window_handlers(s_main_window, (WindowHandlers) {
            .load = main_window_load,
            .unload = main_window_unload
        });
  
    window_stack_push(s_main_window, false);

    app_focus_service_subscribe((AppFocusHandler)disp_focus);

    tz_init();
    phone_init();
    watch_init();

    time_update();
    tick_timer_service_subscribe(MINUTE_UNIT, tick_handler);

    charge_update(battery_state_service_peek());
    battery_state_service_subscribe(charge_update);

    // Unreliable
//    connection_update(connection_service_peek_pebble_app_connection());
//    pebblekit_update(connection_service_peek_pebblekit_connection());
//    connection_service_subscribe((ConnectionHandlers){connection_update, pebblekit_update});

    app_message_register_inbox_received(dict_parse);
    app_message_register_inbox_dropped(inbox_dropped);
    app_message_register_outbox_failed(outbox_failed);
    app_message_register_outbox_sent(outbox_sent);
    app_message_open(128, 128);

    send_fresh();
}

static void deinit() {
    APP_LOG(APP_LOG_LEVEL_INFO, "stop");
    phone_deinit();
    watch_deinit();
    
    app_message_deregister_callbacks();
    connection_service_unsubscribe();
    battery_state_service_unsubscribe();
    tick_timer_service_unsubscribe();
    app_focus_service_unsubscribe();

    window_stack_pop_all(false);
    window_destroy(s_main_window);
}

int main(void) {
    init();
    app_event_loop();
    deinit();
}
