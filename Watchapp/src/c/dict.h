#pragma once

typedef enum {
    ACTION_FIND = 1,
    ACTION_DND,
    ACTION_TBD
} action_t;

void send_action(action_t action);
