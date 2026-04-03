package forge.web.simulation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import forge.LobbyPlayer;
import forge.ai.GameState;
import forge.deck.CardPool;
import forge.game.GameEntityView;
import forge.game.card.CardView;
import forge.game.event.GameEvent;
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

/**
 * No-op IGuiGame implementation for headless AI-vs-AI simulation games.
 *
 * Every method is either empty (fire-and-forget) or returns a safe default
 * (blocking choice methods). The gameEndFuture signals when a game completes.
 */
public class
HeadlessGuiGame extends AbstractGuiGame {

    private final CompletableFuture<Void> gameEndFuture = new CompletableFuture<>();

    public CompletableFuture<Void> getGameEndFuture() {
        return gameEndFuture;
    }

    // ========================================================================
    // Lifecycle
    // ========================================================================

    @Override
    public void openView(final TrackableCollection<PlayerView> myPlayers) { }

    @Override
    public void afterGameEnd() {
        super.afterGameEnd();
        gameEndFuture.complete(null);
    }

    @Override
    public void finishGame() { }

    // ========================================================================
    // Fire-and-forget methods (no-ops)
    // ========================================================================

    @Override
    protected void updateCurrentPlayer(final PlayerView player) { }

    @Override
    public void showCombat() { }

    @Override
    public void showPromptMessage(final PlayerView playerView, final String message) { }

    @Override
    public void showCardPromptMessage(final PlayerView playerView, final String message,
                                      final CardView card) { }

    @Override
    public void updateButtons(final PlayerView owner, final String label1, final String label2,
                              final boolean enable1, final boolean enable2, final boolean focus1) { }

    @Override
    public void flashIncorrectAction() { }

    @Override
    public void alertUser() { }

    @Override
    public void updatePlayerControl() { }

    @Override
    public void enableOverlay() { }

    @Override
    public void disableOverlay() { }

    @Override
    public void showManaPool(final PlayerView player) { }

    @Override
    public void hideManaPool(final PlayerView player) { }

    @Override
    public void updateShards(final Iterable<PlayerView> shardsUpdate) { }

    @Override
    public void setCard(final CardView card) { }

    @Override
    public void setPanelSelection(final CardView hostCard) { }

    @Override
    public void setPlayerAvatar(final LobbyPlayer player, final IHasIcon ihi) { }

    @Override
    public void message(final String message, final String title) { }

    @Override
    public void showErrorDialog(final String message, final String title) { }

    @Override
    public void hideZones(final PlayerView controller,
                          final Iterable<PlayerZoneUpdate> zonesToUpdate) { }

    @Override
    public void restoreOldZones(final PlayerView playerView,
                                final PlayerZoneUpdates playerZoneUpdates) { }

    @Override
    public void handleGameEvent(final GameEvent event) { }

    // ========================================================================
    // Blocking choice methods -- return safe defaults
    // ========================================================================

    @Override
    public <T> List<T> getChoices(final String message, final int min, final int max,
                                  final List<T> choices, final List<T> selected,
                                  final FSerializableFunction<T, String> display) {
        if (choices == null || choices.isEmpty() || min <= 0) {
            return Collections.emptyList();
        }
        return new ArrayList<>(choices.subList(0, Math.min(min, choices.size())));
    }

    @Override
    public <T> List<T> order(final String title, final String top,
                             final int remainingObjectsMin, final int remainingObjectsMax,
                             final List<T> sourceChoices, final List<T> destChoices,
                             final CardView referenceCard, final boolean sideboardingMode) {
        return sourceChoices != null ? new ArrayList<>(sourceChoices) : Collections.emptyList();
    }

    @Override
    public boolean confirm(final CardView c, final String question,
                           final boolean defaultIsYes, final List<String> options) {
        return defaultIsYes;
    }

    @Override
    public boolean showConfirmDialog(final String message, final String title,
                                     final String yesButtonText, final String noButtonText,
                                     final boolean defaultYes) {
        return defaultYes;
    }

    @Override
    public int showOptionDialog(final String message, final String title, final FSkinProp icon,
                                final List<String> options, final int defaultOption) {
        return defaultOption;
    }

    @Override
    public String showInputDialog(final String message, final String title, final FSkinProp icon,
                                  final String initialInput, final List<String> inputOptions,
                                  final boolean isNumeric) {
        if (initialInput != null) {
            return initialInput;
        }
        if (inputOptions != null && !inputOptions.isEmpty()) {
            return inputOptions.get(0);
        }
        return isNumeric ? "0" : "";
    }

    @Override
    public boolean isUiSetToSkipPhase(final PlayerView playerTurn, final PhaseType phase) {
        return false;
    }

    @Override
    public SpellAbilityView getAbilityToPlay(final CardView hostCard,
                                             final List<SpellAbilityView> abilities,
                                             final ITriggerEvent triggerEvent) {
        if (abilities == null || abilities.isEmpty()) {
            return null;
        }
        return abilities.get(0);
    }

    @Override
    public Map<CardView, Integer> assignCombatDamage(final CardView attacker,
                                                     final List<CardView> blockers,
                                                     final int damage,
                                                     final GameEntityView defender,
                                                     final boolean overrideOrder,
                                                     final boolean maySkip) {
        // Assign all damage to first blocker (or empty if no blockers)
        final Map<CardView, Integer> result = new HashMap<>();
        if (blockers != null && !blockers.isEmpty()) {
            result.put(blockers.get(0), damage);
        }
        return result;
    }

    @Override
    public Map<Object, Integer> assignGenericAmount(final CardView effectSource,
                                                    final Map<Object, Integer> targets,
                                                    final int amount,
                                                    final boolean atLeastOne,
                                                    final String amountLabel) {
        // Return targets as-is
        return targets != null ? targets : Collections.emptyMap();
    }

    @Override
    public List<PaperCard> sideboard(final CardPool sideboard, final CardPool main,
                                     final String message) {
        return Collections.emptyList();
    }

    @Override
    public GameEntityView chooseSingleEntityForEffect(final String title,
                                                      final List<? extends GameEntityView> optionList,
                                                      final DelayedReveal delayedReveal,
                                                      final boolean isOptional) {
        if (optionList == null || optionList.isEmpty()) {
            return null;
        }
        return isOptional ? null : optionList.get(0);
    }

    @Override
    public List<GameEntityView> chooseEntitiesForEffect(final String title,
                                                        final List<? extends GameEntityView> optionList,
                                                        final int min, final int max,
                                                        final DelayedReveal delayedReveal) {
        if (optionList == null || optionList.isEmpty() || min <= 0) {
            return Collections.emptyList();
        }
        final List<GameEntityView> result = new ArrayList<>();
        for (int i = 0; i < Math.min(min, optionList.size()); i++) {
            result.add(optionList.get(i));
        }
        return result;
    }

    @Override
    public List<CardView> manipulateCardList(final String title, final Iterable<CardView> cards,
                                             final Iterable<CardView> manipulable,
                                             final boolean toTop, final boolean toBottom,
                                             final boolean toAnywhere) {
        final List<CardView> result = new ArrayList<>();
        if (cards != null) {
            for (final CardView cv : cards) {
                result.add(cv);
            }
        }
        return result;
    }

    @Override
    public Iterable<PlayerZoneUpdate> tempShowZones(final PlayerView controller,
                                                    final Iterable<PlayerZoneUpdate> zonesToUpdate) {
        return zonesToUpdate;
    }

    @Override
    public PlayerZoneUpdates openZones(final PlayerView controller,
                                       final Collection<ZoneType> zones,
                                       final Map<PlayerView, Object> players,
                                       final boolean backupLastZones) {
        return new PlayerZoneUpdates();
    }

    @Override
    public GameState getGamestate() {
        return null;
    }
}
