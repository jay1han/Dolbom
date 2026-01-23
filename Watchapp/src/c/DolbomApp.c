#include <pebble.h>

#include "dict.h"

static Window *s_window;
static TextLayer *s_text_layer;

static void up_click_handler(ClickRecognizerRef recognizer, void *context) {
    send_action(ACTION_DND);
    window_stack_pop_all(true);
}

static void select_click_handler(ClickRecognizerRef recognizer, void *context) {
    send_action(ACTION_FIND);
    window_stack_pop_all(true);
}

static void down_click_handler(ClickRecognizerRef recognizer, void *context) {
//    send_action(ACTION_TBD);
    window_stack_pop_all(true);
}

static void click_config_provider(void *context) {
    window_single_click_subscribe(BUTTON_ID_SELECT, select_click_handler);
    window_single_click_subscribe(BUTTON_ID_UP, up_click_handler);
    window_single_click_subscribe(BUTTON_ID_DOWN, down_click_handler);
}

static struct {
    TextLayer *layer;
    int y_pos;
    char text[20];
} options[3] = {
    {NULL, 8, "Quiet mode"},
    {NULL, 64, "Find phone"},
    {NULL, 120, "Go back"}
};

static void window_load(Window *window) {
    Layer *window_layer = window_get_root_layer(window);

    for (size_t i = 0; i < sizeof(options) / sizeof(options[0]); i++) {
        TextLayer *layer = text_layer_create(GRect(-20, options[i].y_pos, 156, 32));
        options[i].layer = layer;
        text_layer_set_text_alignment(layer, GTextAlignmentRight);
        text_layer_set_text(layer, options[i].text);
        text_layer_set_font(layer, fonts_get_system_font(FONT_KEY_GOTHIC_28_BOLD));
	text_layer_set_overflow_mode(layer, GTextOverflowModeTrailingEllipsis);
        layer_add_child(window_layer, text_layer_get_layer(layer));
    }
}

static void window_unload(Window *window) {
    for (size_t i = 0; i < sizeof(options) / sizeof(options[0]); i++) {
        text_layer_destroy(options[i].layer);
    }
}

static void init(void) {
    s_window = window_create();
    window_set_click_config_provider(s_window, click_config_provider);
    window_set_window_handlers(s_window, (WindowHandlers) {
            .load = window_load,
            .unload = window_unload,
        });

    window_stack_push(s_window, true);

    app_message_open(128, 128);
}

static void deinit(void) {
    window_destroy(s_window);
}

int main(void) {
    init();
    app_event_loop();
    deinit();
}
