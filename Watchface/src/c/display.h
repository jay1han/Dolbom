#pragma once

typedef enum {
    disp_quiet = 0,
    disp_net,
    disp_date,
    disp_dnd ,
    disp_home,
    disp_noti,
    disp_away,
    disp_wbat,
    disp_pbat,
    disp_btid,
    disp_plmn,
    disp_wifi,
    disp_bton,
    disp_cell,
    disp_sim,
    disp_btc,
    disp_end
} disp_t;


void disp_create(Layer *window_layer);
void disp_destroy();
void disp_set(disp_t index, char *text);
void disp_focus(bool in_focus);
void disp_connected(bool connected);
