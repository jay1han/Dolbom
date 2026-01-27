#include <pebble.h>

#include "display.h"
#include "dict.h"
#include "phone.h"

static char pbat[4];
static char cell[4];
static char sim[4];
static char plmn[20];
static char wifi[20];
static char btid[20];
static char btc[4];
static char bton[4];
static char dnd[4];
static char noti[16];
static char net[4];

void phone_init() {
    persist_read_string(STOR_PBAT_4,  pbat, sizeof(pbat));
    persist_read_string(STOR_CELL_4,  cell, sizeof(cell));
    persist_read_string(STOR_SIM_4,   sim,  sizeof(sim));
    persist_read_string(STOR_NET_4,   net,  sizeof(net));
    persist_read_string(STOR_PLMN_20, plmn, sizeof(plmn));
    persist_read_string(STOR_WIFI_20, wifi, sizeof(wifi));
    persist_read_string(STOR_BTID_20, btid, sizeof(btid));
    persist_read_string(STOR_BTC_4,   btc,  sizeof(btc));
    persist_read_string(STOR_DND_4,   dnd,  sizeof(dnd));
    persist_read_string(STOR_NOTI_16, noti, sizeof(noti));
    persist_read_string(STOR_BTON_4,  bton, sizeof(bton));
    
    disp_set(disp_pbat, pbat);
    disp_set(disp_cell , cell);
    disp_set(disp_sim , sim);
    disp_set(disp_btid, btid);
    disp_set(disp_btc,  btc);
    disp_set(disp_dnd,  dnd);
    disp_set(disp_noti, noti);
    disp_set(disp_bton, bton);
    disp_set(disp_net,  net);
    
    if (wifi[0] != 0)
        disp_set(disp_wifi, wifi);
    else
        disp_set(disp_plmn, plmn);
}

void phone_deinit() {
    if (changed[STOR_PBAT_4]) 
        persist_write_string(STOR_PBAT_4, pbat);
    
    if (changed[STOR_CELL_4]) 
        persist_write_string(STOR_CELL_4, cell);
    
    if (changed[STOR_SIM_4]) 
        persist_write_string(STOR_SIM_4, sim);
    
    if (changed[STOR_NET_4]) 
        persist_write_string(STOR_NET_4, net);
    
    if (changed[STOR_PLMN_20])
        persist_write_string(STOR_PLMN_20, plmn);
    
    if (changed[STOR_WIFI_20])
        persist_write_string(STOR_WIFI_20, wifi);
    
    if (changed[STOR_BTID_20])
        persist_write_string(STOR_BTID_20, btid);
    
    if (changed[STOR_BTC_4]) 
        persist_write_string(STOR_BTC_4, btc);
    
    if (changed[STOR_DND_4]) 
        persist_write_string(STOR_DND_4, dnd);
    
    if (changed[STOR_NOTI_16])
        persist_write_string(STOR_NOTI_16, noti);
    
    if (changed[STOR_BTON_4]) 
        persist_write_string(STOR_BTON_4, bton);
}

void phone_charge(int batt, bool plugged, bool charging) {
    char pbat1[4];
    
    char *p = pbat1;
    if (charging) *(p++) = '+';
    else if (plugged) *(p++) = ':';
    if (batt >= 100) strncpy(p, "00", 3);
    else if (batt <= 0) pbat1[0] = 0;
    else snprintf(p, 3, "%d", batt);
    pbat1[3] = 0;

    APP_LOG(APP_LOG_LEVEL_INFO, "PBAT level=%d plugged=%d charging=%d", batt, plugged, charging);
    if (strcmp(pbat, pbat1)) {
        strncpy(pbat, pbat1, sizeof(pbat));
        pbat[sizeof(pbat) - 1] = 0;
        changed[STOR_PBAT_4] = true;
        disp_set(disp_pbat, pbat);
    }
}

void phone_cell(int network_gen, int active_sim, char *carrier) {
    char cell1[4];
    int has_data = network_gen & 0x10;
    int has_gen = network_gen & 0x0F;
    if (has_gen > 0 && has_gen <= 5) {
        cell1[0] = '0' + has_gen;
        if (has_data) cell1[1] = 'G';
        else cell1[1] = 'v';
        cell1[2] = 0;
    } else cell1[0] = 0;
    if (strcmp(cell, cell1)) {
        strncpy(cell, cell1, sizeof(cell));
        cell[sizeof(cell) - 1] = 0;
        changed[STOR_CELL_4] = true;
        disp_set(disp_cell, cell);
    }

    char sim1[4];
    char *p = sim1;
    if (active_sim & 0x0F) *(p++) = '0' + (active_sim & 0x0F);
    if (active_sim & 0x10) *(p++) = 'R';
    *p = 0;
    if (strcmp(sim, sim1)) {
        strncpy(sim, sim1, sizeof(sim));
        sim[sizeof(sim) - 1] = 0;
        changed[STOR_SIM_4] = true;
        disp_set(disp_sim, sim);
    }

    if (strcmp(plmn, carrier)) {
        strncpy(plmn, carrier, sizeof(plmn) - 1);
        plmn[sizeof(plmn) - 1] = 0;
        changed[STOR_PLMN_20] = true;
        
        if (wifi[0] == 0) {
            disp_set(disp_wifi, wifi);
            if (plmn[0] != 0) disp_set(disp_plmn, plmn);
        } else {
            if (plmn[0] != 0) disp_set(disp_plmn, "");
            disp_set(disp_wifi, wifi);
        }
    }
}

void phone_wifi(char *text) {
    if (strcmp(wifi, text)) {
        strncpy(wifi, text, sizeof(wifi));
        wifi[sizeof(wifi) - 1] = 0;
        changed[STOR_WIFI_20] = true;
        if (wifi[0] == 0) {
            disp_set(disp_wifi, wifi);
            if (plmn[0] != 0) disp_set(disp_plmn, plmn);
        } else {
            if (plmn[0] != 0) disp_set(disp_plmn, "");
            disp_set(disp_wifi, wifi);
        }
    }
}

void phone_bt(char *id, int charge, int active) {
    APP_LOG(APP_LOG_LEVEL_INFO, "BT name=%s batt=%d active=%d", id, charge, active);
    
    if (strcmp(btid, id)) {
        strncpy(btid, id, sizeof(btid));
        btid[sizeof(btid) - 1] = 0;
        changed[STOR_BTID_20] = true;
        disp_set(disp_btid, btid);
    }

    char btc1[4];
    if (charge >= 100) strcpy(btc1, "00");
    else if (charge <= 0) btc1[0] = 0;
    else snprintf(btc1, 3, "%d", charge);
    if (strcmp(btc, btc1)) {
        strncpy(btc, btc1, sizeof(btc));
        btc[sizeof(btc) - 1] = 0;
        changed[STOR_BTC_4] = true;
        disp_set(disp_btc, btc);
    }

    char bton1[4];
    char *p = bton1;
    if (active == BT_HEADSET_ACTIVE) *(p++) = 'H';
    else if (active == BT_A2DP_ACTIVE) *(p++) = 'A';
    *p = 0;
    if (strcmp(bton, bton1)) {
        strncpy(bton, bton1, sizeof(bton));
        bton[sizeof(bton) - 1] = 0;
        changed[STOR_BTON_4] = true;
        disp_set(disp_bton, bton);
    }
}

void phone_dnd(bool quiet) {
    if (quiet != (dnd[0] != 0)) {
        if (quiet) strcpy(dnd, "Q");
        else dnd[0] = 0;
        changed[STOR_DND_4] = true;
        disp_set(disp_dnd, dnd);
    }
}

void phone_noti(char *text) {
    if (strcmp(noti, text)) {
        strncpy(noti, text, sizeof(noti));
        noti[sizeof(noti) - 1] = 0;
        changed[STOR_NOTI_16] = true;
        disp_set(disp_noti, noti);
    }
}

void phone_net(bool has_internet) {
    APP_LOG(APP_LOG_LEVEL_INFO, "NET %d", has_internet);
    if (has_internet == (net[0] != 0)) {
        if (has_internet) net[0] = 0;
        else strcpy(net, ">|");
        changed[STOR_NET_4] = true;
        disp_set(disp_net, net);
    }
}
