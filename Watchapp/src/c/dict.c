#include <pebble.h>

#include "dict.h"

enum {
    KEY_MSG_TYPE_I8 = 1,
    KEY_MODEL_I8,
    KEY_VERSION_U32,
    KEY_WBATT_I8,
    KEY_WPLUG_I8,
    KEY_WCHG_I8,
    KEY_TZ_MINS_I16,
    KEY_ACTION_I8,
    KEY_PHONE_DND_I8,
    KEY_PHONE_BATT_I8,
    KEY_PHONE_PLUG_I8,
    KEY_PHONE_CHG_I8,
    KEY_NET_I8,
    KEY_SIM_I8,
    KEY_CARRIER_S20,
    KEY_WIFI_S20,
    KEY_BTID_S20,
    KEY_BTC_I8,
    KEY_BT_ON_I8,
    KEY_NOTI_S16,
};

typedef enum {
    MSG_INFO = 1,
    MSG_FRESH,
    MSG_WBATT,
    MSG_ACTION,
    MSG_TZ,
    MSG_PHONE_DND,
    MSG_PHONE_CHG,
    MSG_NET,
    MSG_WIFI,
    MSG_BT,
    MSG_NOTI,
    MSG_PING,
    MSG_PONG,
    MSG_TYPE
} msg_type_t;

void send_action(action_t action) {
    DictionaryIterator *iter;
    app_message_outbox_begin(&iter);
    
    dict_write_int8(iter, KEY_MSG_TYPE_I8, MSG_ACTION);
    dict_write_int8(iter, KEY_ACTION_I8, action);
    
    app_message_outbox_send();
}
