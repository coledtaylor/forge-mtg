package forge.screens.home.sanctioned;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

import forge.Singletons;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckBase;
import forge.deck.DeckProxy;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.match.HostedMatch;
import forge.gui.GuiBase;
import forge.gui.SOverlayUtils;
import forge.gui.UiCommand;
import forge.gui.framework.FScreen;
import forge.gui.framework.ICDoc;
import forge.item.InventoryItem;
import forge.itemmanager.ItemManagerConfig;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.deckeditor.controllers.ACEditorBase;
import forge.screens.deckeditor.controllers.CEditorConstructed;
import forge.toolbox.FOptionPane;
import forge.util.Aggregates;

/**
 * Controls the Jumpstart submenu in the home UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
@SuppressWarnings("serial")
public enum CSubmenuJumpstart implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    private final UiCommand cmdDeckSelect = () -> {
        VSubmenuJumpstart.SINGLETON_INSTANCE.getBtnStart().setEnabled(true);
    };

    @Override
    public void register() {
    }

    /* (non-Javadoc)
     * @see forge.gui.control.home.IControlSubmenu#initialize()
     */
    @Override
    public void initialize() {
        final VSubmenuJumpstart view = VSubmenuJumpstart.SINGLETON_INSTANCE;

        view.getLstPacks().setSelectCommand(cmdDeckSelect);

        view.getBtnBuildPack().setCommand((UiCommand) this::buildNewPack);

        view.getBtnStart().addActionListener(e -> startGame());
    }

    /* (non-Javadoc)
     * @see forge.gui.control.home.IControlSubmenu#update()
     */
    @Override
    public void update() {
        final VSubmenuJumpstart view = VSubmenuJumpstart.SINGLETON_INSTANCE;
        // Refresh the pack list to show newly created packs
        view.getLstPacks().setPool(DeckProxy.getAllJumpstartDecks());
        view.getLstPacks().setup(ItemManagerConfig.JUMPSTART_DECKS);

        SwingUtilities.invokeLater(() -> {
            final JButton btnStart = view.getBtnStart();
            if (btnStart.isEnabled()) {
                view.getBtnStart().requestFocusInWindow();
            } else {
                view.getBtnBuildPack().requestFocusInWindow();
            }
        });
    }

    private void startGame() {
        final VSubmenuJumpstart view = VSubmenuJumpstart.SINGLETON_INSTANCE;
        final boolean randomPacks = view.getRadRandom().isSelected();

        // Get all available packs
        final List<DeckProxy> allPacks = new ArrayList<>(DeckProxy.getAllJumpstartDecks());

        if (allPacks.isEmpty()) {
            FOptionPane.showErrorDialog("No Jumpstart packs available. Please create at least 2 packs first.", "No Packs");
            return;
        }

        if (allPacks.size() < 2) {
            FOptionPane.showErrorDialog("You need at least 2 Jumpstart packs to play. Please create more packs.", "Not Enough Packs");
            return;
        }

        Deck humanDeck;
        Deck aiDeck;

        if (randomPacks) {
            // Randomly select 2 packs for human with different colors
            List<DeckProxy> humanPacks = selectPacksWithDifferentColors(allPacks, 2);
            if (humanPacks == null) {
                FOptionPane.showErrorDialog("Cannot find 2 packs with different colors. Please create packs of different colors.", "No Valid Pack Combination");
                return;
            }
            humanDeck = combinePacks(humanPacks.get(0).getDeck(), humanPacks.get(1).getDeck(), "Random Jumpstart");

            // Randomly select 2 packs for AI with different colors
            List<DeckProxy> aiPacks = selectPacksWithDifferentColors(allPacks, 2);
            if (aiPacks == null) {
                FOptionPane.showErrorDialog("Cannot find 2 packs with different colors for AI. Please create packs of different colors.", "No Valid Pack Combination");
                return;
            }
            aiDeck = combinePacks(aiPacks.get(0).getDeck(), aiPacks.get(1).getDeck(), "AI Jumpstart");
        } else {
            // Manual selection mode - user must select exactly 2 packs
            final List<DeckProxy> selectedPacks = new ArrayList<>(view.getLstPacks().getSelectedItems());

            if (selectedPacks == null || selectedPacks.isEmpty()) {
                FOptionPane.showErrorDialog("Please select exactly 2 packs to combine for your deck.", "No Packs Selected");
                return;
            }

            if (selectedPacks.size() == 1) {
                FOptionPane.showErrorDialog("Please select exactly 2 packs. You have selected " + selectedPacks.size() + " pack.\n\nHold Ctrl (or Cmd on Mac) and click to select multiple packs.", "Select 2 Packs");
                return;
            }

            if (selectedPacks.size() > 2) {
                FOptionPane.showErrorDialog("Please select exactly 2 packs. You have selected " + selectedPacks.size() + " packs.", "Too Many Packs Selected");
                return;
            }

            // Validate that the 2 selected packs have different colors
            String color1 = getDominantColor(selectedPacks.get(0).getDeck());
            String color2 = getDominantColor(selectedPacks.get(1).getDeck());
            if (color1.equals(color2)) {
                FOptionPane.showErrorDialog("Both selected packs are " + color1 + ".\n\nPlease select packs of different colors.", "Same Color Packs");
                return;
            }

            // User selected exactly 2 packs with different colors
            humanDeck = combinePacks(selectedPacks.get(0).getDeck(), selectedPacks.get(1).getDeck(), "My Jumpstart");

            // AI gets 2 random packs with different colors
            List<DeckProxy> aiPacks = selectPacksWithDifferentColors(allPacks, 2);
            if (aiPacks == null) {
                FOptionPane.showErrorDialog("Cannot find 2 packs with different colors for AI. Please create packs of different colors.", "No Valid Pack Combination");
                return;
            }
            aiDeck = combinePacks(aiPacks.get(0).getDeck(), aiPacks.get(1).getDeck(), "AI Jumpstart");
        }

        // Deck validation (optional)
        if (FModel.getPreferences().getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY)) {
            if (humanDeck.getMain().countAll() != 40) {
                FOptionPane.showErrorDialog("Combined deck must have exactly 40 cards. Got " + humanDeck.getMain().countAll(), "Invalid Deck");
                return;
            }
        }

        SwingUtilities.invokeLater(() -> {
            SOverlayUtils.startGameOverlay();
            SOverlayUtils.showOverlay();
        });

        // Start the match
        final List<RegisteredPlayer> starter = new ArrayList<>();
        final RegisteredPlayer human = new RegisteredPlayer(humanDeck).setPlayer(GamePlayerUtil.getGuiPlayer());
        starter.add(human);
        starter.add(new RegisteredPlayer(aiDeck).setPlayer(GamePlayerUtil.createAiPlayer()));

        final HostedMatch hostedMatch = GuiBase.getInterface().hostMatch();
        hostedMatch.startMatch(GameType.Jumpstart, null, starter, human, GuiBase.getInterface().getNewGuiGame());

        SwingUtilities.invokeLater(SOverlayUtils::hideOverlay);
    }

    /**
     * Combines two 20-card packs into a single 40-card deck.
     */
    private Deck combinePacks(Deck pack1, Deck pack2, String deckName) {
        Deck combined = new Deck(deckName);
        CardPool mainDeck = combined.getMain();

        // Add all cards from both packs
        for (forge.item.PaperCard card : pack1.getMain().toFlatList()) {
            mainDeck.add(card);
        }
        for (forge.item.PaperCard card : pack2.getMain().toFlatList()) {
            mainDeck.add(card);
        }

        return combined;
    }

    /**
     * Determines the dominant color of a pack based on card color identity.
     * Returns "Colorless", "White", "Blue", "Black", "Red", "Green", or "Multicolor".
     */
    private String getDominantColor(Deck pack) {
        int white = 0, blue = 0, black = 0, red = 0, green = 0, colorless = 0;

        for (forge.item.PaperCard card : pack.getMain().toFlatList()) {
            byte colorIdentity = card.getRules().getColor().getColor();

            // Count each color in the card's color identity
            if (colorIdentity == 0) {
                colorless++;
            } else {
                if ((colorIdentity & forge.card.mana.ManaAtom.WHITE) != 0) white++;
                if ((colorIdentity & forge.card.mana.ManaAtom.BLUE) != 0) blue++;
                if ((colorIdentity & forge.card.mana.ManaAtom.BLACK) != 0) black++;
                if ((colorIdentity & forge.card.mana.ManaAtom.RED) != 0) red++;
                if ((colorIdentity & forge.card.mana.ManaAtom.GREEN) != 0) green++;
            }
        }

        // Find the dominant color
        int maxCount = Math.max(white, Math.max(blue, Math.max(black, Math.max(red, green))));

        // Check if it's colorless
        if (colorless > maxCount) {
            return "Colorless";
        }

        // Check if there are multiple colors with the same max count (multicolor)
        int colorsAtMax = 0;
        if (white == maxCount) colorsAtMax++;
        if (blue == maxCount) colorsAtMax++;
        if (black == maxCount) colorsAtMax++;
        if (red == maxCount) colorsAtMax++;
        if (green == maxCount) colorsAtMax++;

        if (colorsAtMax > 1) {
            return "Multicolor";
        }

        // Return the dominant single color
        if (white == maxCount) return "White";
        if (blue == maxCount) return "Blue";
        if (black == maxCount) return "Black";
        if (red == maxCount) return "Red";
        if (green == maxCount) return "Green";

        return "Colorless";
    }

    /**
     * Randomly selects the specified number of packs ensuring they all have different colors.
     * Returns null if no valid combination exists.
     */
    private List<DeckProxy> selectPacksWithDifferentColors(List<DeckProxy> allPacks, int count) {
        // Try up to 100 times to find a valid combination
        for (int attempt = 0; attempt < 100; attempt++) {
            List<DeckProxy> selected = Aggregates.random(allPacks, count);

            // Check if all selected packs have different colors
            List<String> colors = new ArrayList<>();
            boolean allDifferent = true;

            for (DeckProxy pack : selected) {
                String color = getDominantColor(pack.getDeck());
                if (colors.contains(color)) {
                    allDifferent = false;
                    break;
                }
                colors.add(color);
            }

            if (allDifferent) {
                return selected;
            }
        }

        // Could not find a valid combination after 100 attempts
        return null;
    }

    /**
     * Opens the deck editor to create a new Jumpstart pack.
     */
    @SuppressWarnings("unchecked")
    private <T extends DeckBase> void buildNewPack() {
        final ACEditorBase<? extends InventoryItem, T> editor = (ACEditorBase<? extends InventoryItem, T>)
                new CEditorConstructed(CDeckEditorUI.SINGLETON_INSTANCE.getCDetailPicture(), GameType.Jumpstart);

        Singletons.getControl().setCurrentScreen(FScreen.DECK_EDITOR_CONSTRUCTED);
        CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(editor);


        // Create a new empty 20-card pack template
        Deck newPack = new Deck("New Jumpstart Pack");
        editor.getDeckController().setModel((T) newPack);
    }
}

