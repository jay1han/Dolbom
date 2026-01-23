#include <pebble.h>

#include "dict.h"
#include "watch.h"
#include "phone.h"

bool changed[STOR_END];

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
    MSG_TYPE
} msg_type_t;

static const char MSG_NAME[MSG_TYPE][8] = {
    "NONE",
    "INFO",
    "FRESH",
    "WBATT",
    "ACTION",
    "TZ",
    "DND",
    "CHG",
    "NET",
    "WIFI",
    "BT",
    "NOTI",
};

typedef enum {
    ACTION_FIND = 1,
    ACTION_DND,
    ACTION_TBD
} action_t;

static struct {
    msg_type_t message_type;
    int16_t    timezone_minutes;
    bool       phone_dnd;
    int8_t     phone_battery;
    bool       phone_charging;
    bool       phone_plugged;
    int8_t     network_gen;
    int8_t     active_sim;
    char       *carrier;
    char       *wifi_ssid;
    char       *bt_device;
    int8_t     bt_battery;
    bool       bt_on;
    char       *notifications;
    action_t   action;
} message;

void send_fresh() {
    DictionaryIterator *iter;
    app_message_outbox_begin(&iter);
    dict_write_int8(iter, KEY_MSG_TYPE_I8, MSG_FRESH);
    app_message_outbox_send();
    APP_LOG(APP_LOG_LEVEL_INFO, "FRESH out");
}

void send_info() {
    DictionaryIterator *iter;
    app_message_outbox_begin(&iter);
    
    dict_write_int8(iter, KEY_MSG_TYPE_I8, MSG_INFO);
    dict_write_int8(iter, KEY_MODEL_I8, watch_info_get_model());
    WatchInfoVersion version = watch_info_get_firmware_version();
    uint32_t version_u32 = ((uint32_t)version.major << 16) | ((uint32_t)version.minor << 8) | (uint32_t)version.patch;
    dict_write_uint32(iter, KEY_VERSION_U32, version_u32);
    dict_write_int16(iter, KEY_TZ_MINS_I16, tz_get());
    
    app_message_outbox_send();
    APP_LOG(APP_LOG_LEVEL_INFO, "INFO out");
}

void send_batt() {
    DictionaryIterator *iter;
    app_message_outbox_begin(&iter);

    dict_write_int8(iter, KEY_MSG_TYPE_I8, MSG_WBATT);
    dict_write_int8(iter, KEY_WBATT_I8, watch_battery.charge_percent);
    dict_write_int8(iter, KEY_WPLUG_I8, watch_battery.is_plugged);
    dict_write_int8(iter, KEY_WCHG_I8, watch_battery.is_charging);
    
    app_message_outbox_send();
    APP_LOG(APP_LOG_LEVEL_INFO, "BATT out");
}

void dict_parse(DictionaryIterator *iter, void *context) {
    
    Tuple *tuple = dict_read_first(iter);
    while (tuple) {
        switch(tuple->key) {

        case KEY_MSG_TYPE_I8:
            if (tuple->type == TUPLE_INT && tuple->length == 1) {
                message.message_type = (msg_type_t)tuple->value->int8;
                if (message.message_type <= 0 || message.message_type >= MSG_TYPE)
                    message.message_type = (msg_type_t)0;
            }
            break;
            
        case KEY_TZ_MINS_I16:
            if (tuple->type == TUPLE_INT && tuple->length == 2)
                message.timezone_minutes = tuple->value->int16;
            break;
            
        case KEY_PHONE_DND_I8:
            if (tuple->type == TUPLE_INT && tuple->length == 1)
                message.phone_dnd = tuple->value->int8 != 0;
            break;
            
        case KEY_PHONE_BATT_I8:
            if (tuple->type == TUPLE_INT && tuple->length == 1)
                message.phone_battery = tuple->value->int8;
            break;
            
        case KEY_PHONE_CHG_I8:
            if (tuple->type == TUPLE_INT && tuple->length == 1)
                message.phone_charging = tuple->value->int8 != 0;
            break;
            
        case KEY_PHONE_PLUG_I8:
            if (tuple->type == TUPLE_INT && tuple->length == 1)
                message.phone_plugged = tuple->value->int8 != 0;
            break;
            
        case KEY_NET_I8:
            if (tuple->type == TUPLE_INT && tuple->length == 1)
                message.network_gen = tuple->value->int8;
            break;
            
        case KEY_WIFI_S20:
            if (tuple->type == TUPLE_CSTRING && tuple->length <= 20)
                message.wifi_ssid = tuple->value->cstring;
            break;
            
        case KEY_BTID_S20:
            if (tuple->type == TUPLE_CSTRING && tuple->length <= 20)
                message.bt_device = tuple->value->cstring;
            break;
            
        case KEY_BTC_I8:
            if (tuple->type == TUPLE_INT && tuple->length == 1)
                message.bt_battery = tuple->value->int8;
            break;
            
        case KEY_NOTI_S16:
            if (tuple->type == TUPLE_CSTRING && tuple->length <= 16)
                message.notifications = tuple->value->cstring;
            break;

        case KEY_SIM_I8:
            if (tuple->type == TUPLE_INT && tuple->length == 1)
                message.active_sim = tuple->value->int8;
            break;

        case KEY_CARRIER_S20:
            if (tuple->type == TUPLE_CSTRING && tuple->length <= 20)
                message.carrier = tuple->value->cstring;
            break;

        case KEY_BT_ON_I8:
            if (tuple->type == TUPLE_INT && tuple->length == 1)
                message.bt_on = tuple->value->int8 != 0;
            break;
            
        default: break;
            
        }
        
        tuple = dict_read_next(iter);
    }
    APP_LOG(APP_LOG_LEVEL_INFO, "in %s", MSG_NAME[message.message_type]);
    
    switch(message.message_type) {

    case MSG_INFO: send_info(); break;
    case MSG_TZ: tz_set(message.timezone_minutes); break;
    case MSG_PHONE_DND: phone_dnd(message.phone_dnd); break;
    case MSG_PHONE_CHG: phone_charge(message.phone_battery, message.phone_plugged, message.phone_charging); break;
    case MSG_NET: phone_net(message.network_gen, message.active_sim, message.carrier); break;
    case MSG_WIFI: phone_wifi(message.wifi_ssid); break;
    case MSG_BT: phone_bt(message.bt_device, message.bt_battery, message.bt_on); break;
    case MSG_NOTI: phone_noti(message.notifications); break;
    case MSG_WBATT: send_batt(); break;
    case MSG_ACTION: break;
        
    default: break;
    }
}
