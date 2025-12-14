package forge.screens.deckeditor.views;

import javax.swing.JPanel;

import forge.deck.io.DeckPreferences;
import forge.game.GameType;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.itemmanager.DeckManager;
import forge.itemmanager.ItemManagerContainer;
import forge.screens.deckeditor.controllers.CJumpstartDecks;
import forge.screens.match.controllers.CDetailPicture;
import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;

/**
 * Assembles Swing components of Jumpstart deck viewer in deck editor.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VJumpstartDecks implements IVDoc<CJumpstartDecks> {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    final Localizer localizer = Localizer.getInstance();
    private final DragTab tab = new DragTab(localizer.getMessage("lblJumpstart"));

    private DeckManager lstDecks;

    //========== Overridden methods

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.EDITOR_JUMPSTART;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getTabLabel()
     */
    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getLayoutControl()
     */
    @Override
    public CJumpstartDecks getLayoutControl() {
        return CJumpstartDecks.SINGLETON_INSTANCE;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#setParentCell(forge.gui.framework.DragCell)
     */
    @Override
    public void setParentCell(DragCell cell0) {
        this.parentCell = cell0;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getParentCell()
     */
    @Override
    public DragCell getParentCell() {
        return this.parentCell;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#populate()
     */
    @Override
    public void populate() {
        CJumpstartDecks.SINGLETON_INSTANCE.refresh(); //ensure decks refreshed in case any deleted or added since last loaded
        String preferredDeck = DeckPreferences.getJumpstartDeck();
        JPanel parentBody = parentCell.getBody();
        parentBody.setLayout(new MigLayout("insets 5, gap 0, wrap, hidemode 3"));
        parentBody.add(new ItemManagerContainer(lstDecks), "push, grow");
        VAllDecks.editPreferredDeck(lstDecks, preferredDeck);
    }

    //========== Retrieval methods
    /** @return {@link JPanel} */
    public DeckManager getLstDecks() {
        return lstDecks;
    }

    public void setCDetailPicture(final CDetailPicture cDetailPicture) {
        this.lstDecks = new DeckManager(GameType.Jumpstart, cDetailPicture);
        this.lstDecks.setCaption(localizer.getMessage("lblJumpstartPacks"));
    }
}

