package forge.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.javalin.websocket.WsContext;

import org.tinylog.Logger;

import forge.LobbyPlayer;
import forge.ai.GameState;
import forge.deck.CardPool;
import forge.game.GameEntityView;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.phase.PhaseType;
import forge.game.player.DelayedReveal;
import forge.game.player.IHasIcon;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbilityView;
import forge.game.zone.ZoneType;
import forge.gamemodes.match.AbstractGuiGame;
import forge.item.PaperCard;
import forge.localinstance.skin.FSkinProp;
import forge.player.PlayerZoneUpdate;
import forge.player.PlayerZoneUpdates;
import forge.trackable.TrackableCollection;
import forge.util.FSerializableFunction;
import forge.util.ITriggerEvent;
import forge.web.WebInputBridge.GameSessionExpiredException;
import forge.web.dto.CardDto;
import forge.web.dto.CombatDto;
import forge.web.dto.GameStateDto;
import forge.web.dto.SpellAbilityDto;
import forge.web.dto.ZoneUpdateDto;
import forge.web.protocol.MessageType;
import forge.web.protocol.OutboundMessage;

/**
 * Full IGuiGame implementation for web clients. Bridges every engine event
 * to a WebSocket message and converts blocking game-thread calls to async
 * WebSocket request-response via CompletableFuture.
 *
 * Follows the NetGuiGame pattern: fire-and-forget methods use send(),
 * blocking methods use sendAndWait().
 */
public class WebGuiGame extends AbstractGuiGame {

    private final WsContext wsContext;
    private final ObjectMapper objectMapper;
    private final WebInputBridge inputBridge;
    private final ViewRegistry viewRegistry;
    private final AtomicLong sequenceCounter = new AtomicLong(0);
    private int humanPlayerId = -1;
    private int lastLogIndex = 0;
    private volatile boolean autoPassEnabled = true;

    private static final long INPUT_TIMEOUT_MINUTES = 5;

    public WebGuiGame(final WsContext wsContext, final ObjectMapper objectMapper,
                      final WebInputBridge inputBridge, final ViewRegistry viewRegistry) {
        this.wsContext = wsContext;
        this.objectMapper = objectMapper;
        this.inputBridge = inputBridge;
        this.viewRegistry = viewRegistry;
    }

    public void setAutoPassEnabled(final boolean enabled) {
        this.autoPassEnabled = enabled;
    }

    public boolean isAutoPassEnabled() {
        return autoPassEnabled;
    }

    // ========================================================================
    // Core send/sendAndWait methods
    // ========================================================================

    private void send(final MessageType type, final Object payload) {
        try {
            final OutboundMessage msg = new OutboundMessage(type, null,
                    sequenceCounter.incrementAndGet(), payload);
            final String json = objectMapper.writeValueAsString(msg);
            wsContext.send(json);
        } catch (final Exception e) {
            Logger.error(e, "Failed to send outbound message of type {}", type);
        }
    }

    private void send(final MessageType type, final String inputId, final Object payload) {
        try {
            final OutboundMessage msg = new OutboundMessage(type, inputId,
                    sequenceCounter.incrementAndGet(), payload);
            final String json = objectMapper.writeValueAsString(msg);
            wsContext.send(json);
        } catch (final Exception e) {
            Logger.error(e, "Failed to send outbound message of type {} (inputId={})", type, inputId);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T sendAndWait(final MessageType type, final Object payload,
                              final Class<T> responseType) {
        final String inputId = UUID.randomUUID().toString();
        final CompletableFuture<String> future = inputBridge.register(inputId);
        send(type, inputId, payload);
        try {
            final String rawResponse = future.get(INPUT_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            if (rawResponse == null) {
                return null;
            }
            return objectMapper.readValue(rawResponse, responseType);
        } catch (final TimeoutException e) {
            inputBridge.complete(inputId, null);
            throw new GameSessionExpiredException("Input timeout after " + INPUT_TIMEOUT_MINUTES + " minutes");
        } catch (final GameSessionExpiredException e) {
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException("Input bridge error", e);
        }
    }

    private <T> T sendAndWait(final MessageType type, final Object payload,
                              final TypeReference<T> responseType) {
        final String inputId = UUID.randomUUID().toString();
        final CompletableFuture<String> future = inputBridge.register(inputId);
        send(type, inputId, payload);
        try {
            final String rawResponse = future.get(INPUT_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            if (rawResponse == null) {
                return null;
            }
            return objectMapper.readValue(rawResponse, responseType);
        } catch (final TimeoutException e) {
            inputBridge.complete(inputId, null);
            throw new GameSessionExpiredException("Input timeout after " + INPUT_TIMEOUT_MINUTES + " minutes");
        } catch (final GameSessionExpiredException e) {
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException("Input bridge error", e);
        }
    }

    // ========================================================================
    // Helper: build payload maps
    // ========================================================================

    private Map<String, Object> payloadMap(final Object... keyValues) {
        final Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    // ========================================================================
    // GameLog delta streaming
    // ========================================================================

    private void sendLogDelta() {
        final GameView gv = getGameView();
        if (gv == null) return;
        final forge.game.GameLog gameLog = gv.getGameLog();
        if (gameLog == null) return;

        final java.util.List<forge.game.GameLogEntry> allEntries = gameLog.getLogEntries(null);
        final int totalEntries = allEntries.size();
        if (totalEntries <= lastLogIndex) return;

        // allEntries is in REVERSE order (newest first), so new entries are at indices 0..(newCount-1)
        final int newCount = totalEntries - lastLogIndex;
        final List<Map<String, Object>> entries = new ArrayList<>();
        for (int i = newCount - 1; i >= 0; i--) {
            final forge.game.GameLogEntry entry = allEntries.get(i);
            entries.add(payloadMap(
                "type", entry.type().name(),
                "message", entry.message(),
                "sourceCardId", entry.sourceCard() != null ? entry.sourceCard().getId() : -1
            ));
        }
        lastLogIndex = totalEntries;
        send(MessageType.GAME_LOG, entries);
    }

    // ========================================================================
    // AbstractGuiGame abstract methods
    // ========================================================================

    @Override
    protected void updateCurrentPlayer(final PlayerView player) {
        send(MessageType.MESSAGE, payloadMap(
                "type", "current_player_update",
                "playerId", player != null ? player.getId() : -1
        ));
    }

    // ========================================================================
    // CATEGORY B: Fire-and-forget methods
    // ========================================================================

    @Override
    public void openView(final TrackableCollection<PlayerView> myPlayers) {
        // myPlayers contains the players this GUI controls (the human player)
        if (myPlayers != null && !myPlayers.isEmpty()) {
            humanPlayerId = myPlayers.iterator().next().getId();
        }
        final GameView gv = getGameView();
        if (gv != null) {
            send(MessageType.GAME_STATE, GameStateDto.from(gv));
        }
        // Send an initial BUTTON_UPDATE to identify the human player early,
        // before any prompt arrives (e.g., mulligan decisions)
        if (humanPlayerId >= 0) {
            send(MessageType.BUTTON_UPDATE, payloadMap(
                    "playerId", humanPlayerId,
                    "label1", "OK",
                    "label2", "Cancel",
                    "enable1", false,
                    "enable2", false,
                    "focus1", false
            ));
        }
    }

    @Override
    public void afterGameEnd() {
        super.afterGameEnd();
        send(MessageType.GAME_OVER, payloadMap("type", "game_end"));
    }

    @Override
    public void showCombat() {
        final GameView gv = getGameView();
        if (gv != null) {
            send(MessageType.COMBAT_UPDATE, CombatDto.from(gv.getCombat()));
        }
        sendLogDelta();
    }

    @Override
    public void showPromptMessage(final PlayerView playerView, final String message) {
        send(MessageType.MESSAGE, payloadMap(
                "type", "prompt",
                "playerId", playerView != null ? playerView.getId() : -1,
                "message", message
        ));
    }

    @Override
    public void showCardPromptMessage(final PlayerView playerView, final String message,
                                      final CardView card) {
        send(MessageType.MESSAGE, payloadMap(
                "type", "card_prompt",
                "playerId", playerView != null ? playerView.getId() : -1,
                "message", message,
                "cardId", card != null ? card.getId() : -1
        ));
    }

    @Override
    public void updateButtons(final PlayerView owner, final String label1, final String label2,
                              final boolean enable1, final boolean enable2, final boolean focus1) {
        send(MessageType.BUTTON_UPDATE, payloadMap(
                "playerId", owner != null ? owner.getId() : -1,
                "label1", label1,
                "label2", label2,
                "enable1", enable1,
                "enable2", enable2,
                "focus1", focus1
        ));
        sendLogDelta();
    }

    @Override
    public void flashIncorrectAction() {
        send(MessageType.MESSAGE, payloadMap("type", "flash_incorrect"));
    }

    @Override
    public void alertUser() {
        send(MessageType.MESSAGE, payloadMap("type", "alert"));
    }

    @Override
    public void updatePhase(final boolean saveState) {
        final GameView gv = getGameView();
        send(MessageType.PHASE_UPDATE, payloadMap(
                "phase", gv != null && gv.getPhase() != null ? gv.getPhase().name() : null,
                "saveState", saveState
        ));
        sendLogDelta();
    }

    @Override
    public void updateTurn(final PlayerView player) {
        final GameView gv = getGameView();
        send(MessageType.TURN_UPDATE, payloadMap(
                "activePlayerId", player != null ? player.getId() : -1,
                "turn", gv != null ? gv.getTurn() : 0
        ));
    }

    @Override
    public void updatePlayerControl() {
        final GameView gv = getGameView();
        if (gv != null) {
            send(MessageType.GAME_STATE, GameStateDto.from(gv));
        }
    }

    @Override
    public void enableOverlay() {
        send(MessageType.MESSAGE, payloadMap("type", "enable_overlay"));
    }

    @Override
    public void disableOverlay() {
        send(MessageType.MESSAGE, payloadMap("type", "disable_overlay"));
    }

    @Override
    public void finishGame() {
        send(MessageType.GAME_OVER, payloadMap("type", "finish_game"));
    }

    @Override
    public void showManaPool(final PlayerView player) {
        send(MessageType.MESSAGE, payloadMap(
                "type", "show_mana_pool",
                "playerId", player != null ? player.getId() : -1
        ));
    }

    @Override
    public void hideManaPool(final PlayerView player) {
        send(MessageType.MESSAGE, payloadMap(
                "type", "hide_mana_pool",
                "playerId", player != null ? player.getId() : -1
        ));
    }

    @Override
    public void updateStack() {
        final GameView gv = getGameView();
        if (gv != null && gv.getStack() != null) {
            final List<SpellAbilityDto> stackDtos = new ArrayList<>();
            gv.getStack().forEach(siv -> stackDtos.add(SpellAbilityDto.from(siv)));
            send(MessageType.STACK_UPDATE, stackDtos);
        }
        sendLogDelta();
    }

    @Override
    public void updateZones(final Iterable<PlayerZoneUpdate> zonesToUpdate) {
        if (zonesToUpdate == null) {
            return;
        }
        // Send full game state so the frontend has up-to-date card data.
        // ZONE_UPDATE alone only carries zone names, not card details,
        // so the frontend can't rebuild zones for cards it hasn't seen yet.
        final GameView gv = getGameView();
        if (gv != null) {
            send(MessageType.GAME_STATE, GameStateDto.from(gv));
        }
        sendLogDelta();
    }

    @Override
    public void updateCards(final Iterable<CardView> cards) {
        if (cards == null) {
            return;
        }
        // Send full game state to ensure frontend has all card data.
        // Previously sent a mismatched payload as ZONE_UPDATE that the
        // frontend couldn't parse.
        final GameView gv = getGameView();
        if (gv != null) {
            send(MessageType.GAME_STATE, GameStateDto.from(gv));
        }
    }

    @Override
    public void updateManaPool(final Iterable<PlayerView> manaPoolUpdate) {
        final GameView gv = getGameView();
        if (gv != null) {
            send(MessageType.GAME_STATE, GameStateDto.from(gv));
        }
    }

    @Override
    public void updateLives(final Iterable<PlayerView> livesUpdate) {
        final GameView gv = getGameView();
        if (gv != null) {
            send(MessageType.GAME_STATE, GameStateDto.from(gv));
        }
    }

    @Override
    public void updateShards(final Iterable<PlayerView> shardsUpdate) {
        // Web client does not support adventure shards
    }

    @Override
    public void setCard(final CardView card) {
        send(MessageType.SHOW_CARDS, payloadMap(
                "type", "set_card",
                "card", card != null ? CardDto.from(card) : null
        ));
    }

    @Override
    public void setPanelSelection(final CardView hostCard) {
        send(MessageType.SHOW_CARDS, payloadMap(
                "type", "panel_selection",
                "card", hostCard != null ? CardDto.from(hostCard) : null
        ));
    }

    @Override
    public void setPlayerAvatar(final LobbyPlayer player, final IHasIcon ihi) {
        send(MessageType.MESSAGE, payloadMap(
                "type", "player_avatar",
                "playerName", player != null ? player.getName() : null
        ));
    }

    @Override
    public void message(final String message, final String title) {
        send(MessageType.MESSAGE, payloadMap(
                "type", "message",
                "title", title,
                "message", message
        ));
    }

    @Override
    public void showErrorDialog(final String message, final String title) {
        send(MessageType.ERROR, payloadMap(
                "title", title,
                "message", message
        ));
    }

    @Override
    public void hideZones(final PlayerView controller,
                          final Iterable<PlayerZoneUpdate> zonesToUpdate) {
        send(MessageType.MESSAGE, payloadMap(
                "type", "hide_zones",
                "playerId", controller != null ? controller.getId() : -1
        ));
    }

    @Override
    public void restoreOldZones(final PlayerView playerView,
                                final PlayerZoneUpdates playerZoneUpdates) {
        send(MessageType.MESSAGE, payloadMap(
                "type", "restore_zones",
                "playerId", playerView != null ? playerView.getId() : -1
        ));
    }

    @Override
    public GameState getGamestate() {
        return null;
    }

    // ========================================================================
    // CATEGORY C: Blocking request-response methods
    // ========================================================================

    @Override
    public <T> List<T> getChoices(final String message, final int min, final int max,
                                  final List<T> choices, final List<T> selected,
                                  final FSerializableFunction<T, String> display) {
        if (choices == null || choices.isEmpty()) {
            return Collections.emptyList();
        }

        // Auto-dismiss informational reveals (min=-1, max=-1 means reveal(), not a real choice)
        if (min < 0 && max < 0) {
            Logger.info("Auto-dismissing informational reveal: {}", message);
            return Collections.emptyList();
        }

        // Build display strings for each choice
        final List<String> displayChoices = new ArrayList<>();
        for (final T choice : choices) {
            if (display != null) {
                displayChoices.add(display.apply(choice));
            } else {
                displayChoices.add(choice.toString());
            }
        }

        // Build choice IDs for card ID matching on the frontend
        final List<Integer> choiceIds = new ArrayList<>();
        for (final T choice : choices) {
            if (choice instanceof GameEntityView) {
                choiceIds.add(((GameEntityView) choice).getId());
            } else {
                choiceIds.add(-1);
            }
        }

        final Map<String, Object> payload = payloadMap(
                "message", message,
                "min", min,
                "max", max,
                "choices", displayChoices,
                "choiceIds", choiceIds,
                "selected", selected != null ? selected.stream().map(
                        s -> choices.indexOf(s)).collect(java.util.stream.Collectors.toList()) : null
        );

        final List<Integer> indices = sendAndWait(MessageType.PROMPT_CHOICE, payload,
                new TypeReference<List<Integer>>() { });

        if (indices == null || indices.isEmpty()) {
            if (min <= 0) {
                return Collections.emptyList();
            }
            // Return minimum required choices from the beginning
            return new ArrayList<>(choices.subList(0, Math.min(min, choices.size())));
        }

        final List<T> result = new ArrayList<>();
        for (final int idx : indices) {
            if (idx >= 0 && idx < choices.size()) {
                result.add(choices.get(idx));
            }
        }
        return result;
    }

    @Override
    public <T> List<T> order(final String title, final String top,
                             final int remainingObjectsMin, final int remainingObjectsMax,
                             final List<T> sourceChoices, final List<T> destChoices,
                             final CardView referenceCard, final boolean sideboardingMode) {
        if (sourceChoices == null || sourceChoices.isEmpty()) {
            return Collections.emptyList();
        }

        final List<String> displayChoices = new ArrayList<>();
        for (final T choice : sourceChoices) {
            displayChoices.add(choice.toString());
        }

        final Map<String, Object> payload = payloadMap(
                "title", title,
                "top", top,
                "remainingObjectsMin", remainingObjectsMin,
                "remainingObjectsMax", remainingObjectsMax,
                "choices", displayChoices,
                "cardId", referenceCard != null ? referenceCard.getId() : -1,
                "sideboardingMode", sideboardingMode
        );

        final List<Integer> indices = sendAndWait(MessageType.PROMPT_CHOICE, payload,
                new TypeReference<List<Integer>>() { });

        if (indices == null || indices.isEmpty()) {
            return new ArrayList<>(sourceChoices);
        }

        final List<T> result = new ArrayList<>();
        for (final int idx : indices) {
            if (idx >= 0 && idx < sourceChoices.size()) {
                result.add(sourceChoices.get(idx));
            }
        }
        return result;
    }

    @Override
    public boolean confirm(final CardView c, final String question,
                           final boolean defaultIsYes, final List<String> options) {
        final Map<String, Object> payload = payloadMap(
                "cardId", c != null ? c.getId() : -1,
                "message", question,
                "defaultYes", defaultIsYes,
                "options", options
        );

        final Boolean result = sendAndWait(MessageType.PROMPT_CONFIRM, payload, Boolean.class);
        return result != null ? result : defaultIsYes;
    }

    @Override
    public boolean showConfirmDialog(final String message, final String title,
                                     final String yesButtonText, final String noButtonText,
                                     final boolean defaultYes) {
        final Map<String, Object> payload = payloadMap(
                "message", message,
                "title", title,
                "yesButton", yesButtonText,
                "noButton", noButtonText,
                "defaultYes", defaultYes
        );

        final Boolean result = sendAndWait(MessageType.PROMPT_CONFIRM, payload, Boolean.class);
        return result != null ? result : defaultYes;
    }

    @Override
    public int showOptionDialog(final String message, final String title, final FSkinProp icon,
                                final List<String> options, final int defaultOption) {
        final Map<String, Object> payload = payloadMap(
                "message", message,
                "title", title,
                "options", options,
                "defaultOption", defaultOption
        );

        final Integer result = sendAndWait(MessageType.PROMPT_CHOICE, payload, Integer.class);
        return result != null ? result : defaultOption;
    }

    @Override
    public String showInputDialog(final String message, final String title, final FSkinProp icon,
                                  final String initialInput, final List<String> inputOptions,
                                  final boolean isNumeric) {
        final Map<String, Object> payload = payloadMap(
                "message", message,
                "title", title,
                "initialInput", initialInput,
                "options", inputOptions,
                "isNumeric", isNumeric
        );

        return sendAndWait(MessageType.PROMPT_CHOICE, payload, String.class);
    }

    @Override
    public Map<CardView, Integer> assignCombatDamage(final CardView attacker,
                                                     final List<CardView> blockers,
                                                     final int damage,
                                                     final GameEntityView defender,
                                                     final boolean overrideOrder,
                                                     final boolean maySkip) {
        final List<CardDto> blockerDtos = new ArrayList<>();
        for (final CardView blocker : blockers) {
            blockerDtos.add(CardDto.from(blocker));
        }

        final Map<String, Object> payload = payloadMap(
                "attackerId", attacker.getId(),
                "blockers", blockerDtos,
                "damage", damage,
                "defenderId", defender != null ? defender.getId() : -1,
                "overrideOrder", overrideOrder,
                "maySkip", maySkip
        );

        final Map<Integer, Integer> response = sendAndWait(MessageType.PROMPT_AMOUNT, payload,
                new TypeReference<Map<Integer, Integer>>() { });

        final Map<CardView, Integer> result = new HashMap<>();
        if (response != null) {
            for (final Map.Entry<Integer, Integer> entry : response.entrySet()) {
                // Find the blocker CardView by ID
                for (final CardView blocker : blockers) {
                    if (blocker.getId() == entry.getKey()) {
                        result.put(blocker, entry.getValue());
                        break;
                    }
                }
            }
        }
        return result;
    }

    @Override
    public Map<Object, Integer> assignGenericAmount(final CardView effectSource,
                                                    final Map<Object, Integer> targets,
                                                    final int amount,
                                                    final boolean atLeastOne,
                                                    final String amountLabel) {
        final Map<String, Object> targetInfo = new LinkedHashMap<>();
        for (final Map.Entry<Object, Integer> entry : targets.entrySet()) {
            targetInfo.put(entry.getKey().toString(), entry.getValue());
        }

        final Map<String, Object> payload = payloadMap(
                "sourceCardId", effectSource != null ? effectSource.getId() : -1,
                "targets", targetInfo,
                "amount", amount,
                "atLeastOne", atLeastOne,
                "amountLabel", amountLabel
        );

        final Map<String, Integer> response = sendAndWait(MessageType.PROMPT_AMOUNT, payload,
                new TypeReference<Map<String, Integer>>() { });

        final Map<Object, Integer> result = new LinkedHashMap<>();
        if (response != null) {
            // Map string keys back to original target objects
            for (final Map.Entry<Object, Integer> original : targets.entrySet()) {
                final String key = original.getKey().toString();
                if (response.containsKey(key)) {
                    result.put(original.getKey(), response.get(key));
                } else {
                    result.put(original.getKey(), original.getValue());
                }
            }
        } else {
            result.putAll(targets);
        }
        return result;
    }

    @Override
    public List<PaperCard> sideboard(final CardPool sideboard, final CardPool main,
                                     final String message) {
        final Map<String, Object> payload = payloadMap(
                "message", message,
                "type", "sideboard"
        );

        final List<String> cardNames = sendAndWait(MessageType.PROMPT_CHOICE, payload,
                new TypeReference<List<String>>() { });

        final List<PaperCard> result = new ArrayList<>();
        if (cardNames != null && sideboard != null) {
            for (final String name : cardNames) {
                for (final Map.Entry<PaperCard, Integer> entry : sideboard) {
                    if (entry.getKey().getName().equals(name)) {
                        result.add(entry.getKey());
                        break;
                    }
                }
            }
        }
        return result;
    }

    @Override
    public GameEntityView chooseSingleEntityForEffect(final String title,
                                                      final List<? extends GameEntityView> optionList,
                                                      final DelayedReveal delayedReveal,
                                                      final boolean isOptional) {
        if (optionList == null || optionList.isEmpty()) {
            return null;
        }

        final List<String> displayOptions = new ArrayList<>();
        final List<Integer> choiceIds = new ArrayList<>();
        for (final GameEntityView gev : optionList) {
            displayOptions.add(gev.toString());
            choiceIds.add(gev.getId());
        }

        final Map<String, Object> payload = payloadMap(
                "title", title,
                "message", title,
                "choices", displayOptions,
                "choiceIds", choiceIds,
                "min", isOptional ? 0 : 1,
                "max", 1,
                "isOptional", isOptional
        );

        final List<Integer> indices = sendAndWait(MessageType.PROMPT_CHOICE, payload,
                new TypeReference<List<Integer>>() { });
        if (indices == null || indices.isEmpty()) {
            return isOptional ? null : optionList.get(0);
        }
        final int idx = indices.get(0);
        if (idx < 0 || idx >= optionList.size()) {
            return isOptional ? null : optionList.get(0);
        }
        return optionList.get(idx);
    }

    @Override
    public List<GameEntityView> chooseEntitiesForEffect(final String title,
                                                        final List<? extends GameEntityView> optionList,
                                                        final int min, final int max,
                                                        final DelayedReveal delayedReveal) {
        if (optionList == null || optionList.isEmpty()) {
            return Collections.emptyList();
        }

        final List<String> displayOptions = new ArrayList<>();
        final List<Integer> choiceIds = new ArrayList<>();
        for (final GameEntityView gev : optionList) {
            displayOptions.add(gev.toString());
            choiceIds.add(gev.getId());
        }

        final Map<String, Object> payload = payloadMap(
                "title", title,
                "message", title,
                "choices", displayOptions,
                "choiceIds", choiceIds,
                "min", min,
                "max", max
        );

        final List<Integer> indices = sendAndWait(MessageType.PROMPT_CHOICE, payload,
                new TypeReference<List<Integer>>() { });

        final List<GameEntityView> result = new ArrayList<>();
        if (indices != null) {
            for (final int idx : indices) {
                if (idx >= 0 && idx < optionList.size()) {
                    result.add(optionList.get(idx));
                }
            }
        }
        return result;
    }

    @Override
    public List<CardView> manipulateCardList(final String title, final Iterable<CardView> cards,
                                             final Iterable<CardView> manipulable,
                                             final boolean toTop, final boolean toBottom,
                                             final boolean toAnywhere) {
        final List<CardDto> cardDtos = new ArrayList<>();
        final List<Integer> allCardIds = new ArrayList<>();
        if (cards != null) {
            for (final CardView cv : cards) {
                cardDtos.add(CardDto.from(cv));
                allCardIds.add(cv.getId());
            }
        }

        final Map<String, Object> payload = payloadMap(
                "title", title,
                "cards", cardDtos,
                "toTop", toTop,
                "toBottom", toBottom,
                "toAnywhere", toAnywhere
        );

        final List<Integer> orderedIds = sendAndWait(MessageType.PROMPT_CHOICE, payload,
                new TypeReference<List<Integer>>() { });

        // Build result ordered by client response
        final List<CardView> allCards = new ArrayList<>();
        if (cards != null) {
            for (final CardView cv : cards) {
                allCards.add(cv);
            }
        }

        if (orderedIds == null || orderedIds.isEmpty()) {
            return allCards;
        }

        final List<CardView> result = new ArrayList<>();
        for (final int id : orderedIds) {
            for (final CardView cv : allCards) {
                if (cv.getId() == id) {
                    result.add(cv);
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public Iterable<PlayerZoneUpdate> tempShowZones(final PlayerView controller,
                                                    final Iterable<PlayerZoneUpdate> zonesToUpdate) {
        // Send the zones to show, return the original zones as a no-op cleanup action
        if (zonesToUpdate != null) {
            final List<ZoneUpdateDto> updates = new ArrayList<>();
            for (final PlayerZoneUpdate pzu : zonesToUpdate) {
                updates.add(ZoneUpdateDto.from(pzu));
            }
            send(MessageType.SHOW_CARDS, payloadMap(
                    "type", "temp_show_zones",
                    "playerId", controller != null ? controller.getId() : -1,
                    "zones", updates
            ));
        }
        return zonesToUpdate;
    }

    @Override
    public PlayerZoneUpdates openZones(final PlayerView controller,
                                       final Collection<ZoneType> zones,
                                       final Map<PlayerView, Object> players,
                                       final boolean backupLastZones) {
        send(MessageType.MESSAGE, payloadMap(
                "type", "open_zones",
                "playerId", controller != null ? controller.getId() : -1
        ));
        return new PlayerZoneUpdates();
    }

    @Override
    public SpellAbilityView getAbilityToPlay(final CardView hostCard,
                                             final List<SpellAbilityView> abilities,
                                             final ITriggerEvent triggerEvent) {
        if (abilities == null || abilities.isEmpty()) {
            return null;
        }
        if (abilities.size() == 1) {
            return abilities.get(0);
        }

        final List<String> displayAbilities = new ArrayList<>();
        for (final SpellAbilityView sav : abilities) {
            displayAbilities.add(sav.toString());
        }

        final Map<String, Object> payload = payloadMap(
                "cardId", hostCard != null ? hostCard.getId() : -1,
                "abilities", displayAbilities
        );

        final Integer idx = sendAndWait(MessageType.PROMPT_CHOICE, payload, Integer.class);
        if (idx == null || idx < 0 || idx >= abilities.size()) {
            return null;
        }
        return abilities.get(idx);
    }

    @Override
    public boolean isUiSetToSkipPhase(final PlayerView playerTurn, final PhaseType phase) {
        return false; // Web client does not auto-skip phases
    }

    // ========================================================================
    // Inherited from AbstractGuiGame -- no override needed for these:
    // setGameView, setOriginalGameController, setGameController, setSpectator,
    // updateSingleCard, oneOrNone, one, reveal, getInteger (4 overloads),
    // many (3 overloads), order (1 overload), confirm (2 overloads),
    // message (0-arg), showErrorDialog (0-arg), showConfirmDialog (3 overloads),
    // showInputDialog (3 overloads), setHighlighted, setUsedToPay,
    // setSelectables, clearSelectables, insertInList, handleGameEvent,
    // notifyStackAddition, notifyStackRemoval, handleLandPlayed,
    // autoPassUntilEndOfTurn, mayAutoPass, autoPassCancel,
    // awaitNextInput, cancelAwaitNextInput, updateAutoPassPrompt,
    // shouldAutoYield, setShouldAutoYield, shouldAlwaysAcceptTrigger,
    // shouldAlwaysDeclineTrigger, setShouldAlwaysAcceptTrigger,
    // setShouldAlwaysDeclineTrigger, setShouldAlwaysAskTrigger,
    // clearAutoYields, setCurrentPlayer, isSelecting, isGamePaused,
    // setGamePause, getGameSpeed, setGameSpeed, getDayTime, updateDayTime,
    // showWaitingTimer, isNetGame, setNetGame, getChoices (4-arg),
    // updateRevealedCards, refreshCardDetails, refreshField,
    // updateButtons (3-arg bool), confirm (2-arg), message (1-arg),
    // showErrorDialog (1-arg), showConfirmDialog (2-arg, 3-arg, 4-arg),
    // showInputDialog (3-arg bool, 3-arg icon, 4-arg)
    // ========================================================================

    @Override
    public void updateDependencies() {
        // Not applicable for web client
    }
}
