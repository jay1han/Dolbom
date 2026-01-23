#include <pebble.h>

#include "display.h"
#include "phone.h"

static Layer *s_window_layer;

#define Left     GTextAlignmentLeft
#define Right    GTextAlignmentRight
#define Center   GTextAlignmentCenter
#define FONT_49  FONT_KEY_ROBOTO_BOLD_SUBSET_49
#define FONT_28  FONT_KEY_GOTHIC_28_BOLD
#define FONT_24  FONT_KEY_GOTHIC_24_BOLD
#define FONT_18  FONT_KEY_GOTHIC_18_BOLD

static struct {
    TextLayer *layer;
    GRect rect;
    uint8_t color;
    int8_t bg_count;
    int8_t bg_index;
    GTextAlignment align;
    const char *font;
} disp[disp_end] = {
    {NULL, {{0, 16},    {144, 31}}, 0xC0, 3, -1, Right , FONT_18},  //  disp_quiet
    {NULL, {{0, -10},   {144, 31}}, 0xFF, 3, -1, Center, FONT_28},  //  disp_date
    {NULL, {{0, -8},    {144, 31}}, 0xFF, 3, -1, Right , FONT_24},  //  disp_dnd !TODO
    {NULL, {{0, 16},    {144, 51}}, 0xC0, 3, -1, Center, FONT_49},  //  disp_home
    {NULL, {{0, 69},    {144, 31}}, 0xFF, 3, -1, Center, FONT_28},  //  disp_noti
    {NULL, {{2, -8},    {142, 29}}, 0xFF, 3, -1, Left  , FONT_24},  //  disp_bton
    {NULL, {{29, 97},   {144, 29}}, 0xC0, 3, -1, Left  , FONT_24},  //  disp_btid
    {NULL, {{0, 95},    {142, 31}}, 0xC0, 4,  3, Right , FONT_28},  //  disp_btc
    {NULL, {{4, 97},    {144, 31}}, 0xC0, 4, -1, Left  , FONT_24},  //  disp_sim
    {NULL, {{2, 116},   {144, 31}}, 0xC0, 4, -1, Left  , FONT_28},  //  disp_net 
    {NULL, {{29, 116},  {144, 31}}, 0xC0, 4, -1, Left  , FONT_28},  //  disp_plmn
    {NULL, {{29, 118},  {144, 29}}, 0xC0, 4, -1, Left  , FONT_24},  //  disp_wifi
    {NULL, {{0, 140},   {140, 31}}, 0xFF, 4, -1, Center, FONT_28},  //  disp_away
    {NULL, {{0, 140},   {144, 31}}, 0xFF, 4, -1, Left  , FONT_28},  //  disp_wbat
    {NULL, {{0, 140},   {144, 31}}, 0xFF, 4, -1, Right , FONT_28},  //  disp_pbat
};

static struct {
    BitmapLayer *layer;
    GRect rect;
    uint8_t color;
} bg[] = {
    {NULL, {{0, 0},     {144, 21}}, 0xC0},
    {NULL, {{0, 74},    {144, 29}}, 0xC0},
    {NULL, {{0, 147},   {144, 22}}, 0xC0},
    {NULL, {{116, 103}, {28, 25}},  0x3F},  // disp_btc
};

static struct {
    BitmapLayer *layer;
    GRect rect;
    uint8_t color;
} disc_layer = {NULL, {{0, 96}, {144, 51}}, 0xC0};

#define BG (sizeof(bg) / sizeof(bg[0]))

static bool quiet_time = false;

static void check_quiet_time() {
    bool is_active = quiet_time_is_active();
    if (quiet_time != is_active) {
        quiet_time = is_active;
        text_layer_set_text(disp[disp_quiet].layer, quiet_time ? "Q" : "");
    }
}

void disp_create(Layer *window_layer) {
    s_window_layer = window_layer;
    int8_t bg_index = 0;
    
    for (disp_t i = 0; i < disp_end; i++) {
        while (bg_index < disp[i].bg_count) {
            BitmapLayer *layer = bitmap_layer_create(bg[bg_index].rect);
            bitmap_layer_set_background_color(layer, (GColor8){.argb=bg[bg_index].color});
            bitmap_layer_set_compositing_mode(layer, GCompOpAnd);
            layer_add_child(window_layer, bitmap_layer_get_layer(layer));
            bg[bg_index].layer = layer;
            bg_index++;
        }
        
        TextLayer *layer = text_layer_create(disp[i].rect);
        text_layer_set_background_color(layer, GColorClear);
        text_layer_set_text_color(layer, (GColor8){.argb=disp[i].color});
        text_layer_set_font(layer, fonts_get_system_font(disp[i].font));
        text_layer_set_text_alignment(layer, disp[i].align);
	text_layer_set_overflow_mode(layer, GTextOverflowModeWordWrap);
        layer_add_child(window_layer, text_layer_get_layer(layer));
        disp[i].layer = layer;
    }

    check_quiet_time();
}

void disp_connected(bool connected) {
    if (!connected) {
        phone_charge(0, false, false);
        phone_dnd(false);
        phone_noti("");
        if (s_window_layer != NULL) {
            BitmapLayer *layer = bitmap_layer_create(disc_layer.rect);
            bitmap_layer_set_background_color(layer, (GColor8){.argb=disc_layer.color});
            bitmap_layer_set_compositing_mode(layer, GCompOpAnd);
            layer_add_child(s_window_layer, bitmap_layer_get_layer(layer));
            disc_layer.layer = layer;
        }
    } else {
        if (disc_layer.layer != NULL) {
            bitmap_layer_destroy(disc_layer.layer);
            disc_layer.layer = NULL;
        }
    }
}

void disp_destroy(void) {
    for (disp_t i = 0; i < disp_end; i++) {
        text_layer_destroy(disp[i].layer);
    }

    for (size_t i = 0; i < BG; i++) {
        bitmap_layer_destroy(bg[i].layer);
    }
}

void disp_set(disp_t index, char *text) {
    if (disp[index].bg_index > 0) {
        if (text[0] != 0){
            bitmap_layer_set_background_color(bg[disp[index].bg_index].layer, (GColor8){.argb=0xFF});
            bitmap_layer_set_compositing_mode(bg[disp[index].bg_index].layer, GCompOpAssign);
        } else {
            bitmap_layer_set_background_color(bg[disp[index].bg_index].layer, (GColor8){.argb=0x3F});
            bitmap_layer_set_compositing_mode(bg[disp[index].bg_index].layer, GCompOpAnd);
        }
    }
    text_layer_set_text(disp[index].layer, text);
    check_quiet_time();
}

void disp_focus(bool in_focus) {
    if (in_focus) check_quiet_time();
}
