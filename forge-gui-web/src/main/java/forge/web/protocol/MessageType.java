package forge.web.protocol;

/**
 * All WebSocket message types for communication between server and client.
 */
public enum MessageType {
    // Outbound (server -> client): game state and event notifications
    GAME_STATE,
    ZONE_UPDATE,
    PHASE_UPDATE,
    TURN_UPDATE,
    COMBAT_UPDATE,
    STACK_UPDATE,
    PROMPT_CHOICE,
    PROMPT_CONFIRM,
    PROMPT_AMOUNT,
    SHOW_CARDS,
    MESSAGE,
    GAME_LOG,
    BUTTON_UPDATE,
    GAME_OVER,
    ERROR,

    // Inbound (client -> server): player responses
    CHOICE_RESPONSE,
    CONFIRM_RESPONSE,
    AMOUNT_RESPONSE,
    START_GAME,
    BUTTON_OK,
    BUTTON_CANCEL,
    SELECT_CARD,
    UNDO,
    SET_AUTO_PASS
}
