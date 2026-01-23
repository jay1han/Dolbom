#pragma once

typedef enum {
    disp_quiet = 0,
    disp_date,
    disp_dnd ,
    disp_home,
    disp_noti,
    disp_bton,
    disp_btid,
    disp_btc,
    disp_sim,
    disp_net,
    disp_plmn,
    disp_wifi,
    disp_away,
    disp_wbat,
    disp_pbat,
    disp_end
} disp_t;


void disp_create(Layer *window_layer);
void disp_destroy();
void disp_set(disp_t index, char *text);
void disp_focus(bool in_focus);
void disp_connected(bool connected);
