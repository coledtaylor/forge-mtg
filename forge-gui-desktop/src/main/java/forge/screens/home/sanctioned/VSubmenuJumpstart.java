package forge.screens.home.sanctioned;

import java.awt.Font;

import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;

import forge.game.GameType;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.itemmanager.DeckManager;
import forge.itemmanager.ItemManagerContainer;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.home.EMenuGroup;
import forge.screens.home.IVSubmenu;
import forge.screens.home.LblHeader;
import forge.screens.home.StartButton;
import forge.screens.home.VHomeUI;
import forge.screens.home.VHomeUI.PnlDisplay;
import forge.toolbox.FLabel;
import forge.toolbox.FRadioButton;
import forge.toolbox.FSkin;
import forge.toolbox.JXButtonPanel;
import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;

/**
 * Assembles Swing components of Jumpstart submenu singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VSubmenuJumpstart implements IVSubmenu<CSubmenuJumpstart> {
    /** */
    SINGLETON_INSTANCE;
    final Localizer localizer = Localizer.getInstance();

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab(localizer.getMessage("lblJumpstart"));

    /** */
    private final LblHeader lblTitle = new LblHeader(localizer.getMessage("lblHeaderJumpstart"));

    private final JPanel pnlStart = new JPanel();
    private final StartButton btnStart = new StartButton();
    private final DeckManager lstPacks = new DeckManager(GameType.Jumpstart, CDeckEditorUI.SINGLETON_INSTANCE.getCDetailPicture());

    private final JRadioButton radRandom = new FRadioButton(localizer.getMessage("lblRandomPacks"));
    private final JRadioButton radManual = new FRadioButton(localizer.getMessage("lblChoosePacks"));

    private final FLabel lblInfo = new FLabel.Builder()
        .fontAlign(SwingConstants.LEFT).fontSize(16).fontStyle(Font.BOLD)
        .text(localizer.getMessage("lblJumpstartText1")).build();

    private final FLabel lblDir1 = new FLabel.Builder()
        .text(localizer.getMessage("lblJumpstartText2"))
        .fontSize(12).build();

    private final FLabel lblDir2 = new FLabel.Builder()
        .text(localizer.getMessage("lblJumpstartText3"))
        .fontSize(12).build();

    private final FLabel lblDir3 = new FLabel.Builder()
        .text(localizer.getMessage("lblJumpstartText4"))
        .fontSize(12).build();

    private final FLabel btnBuildPack = new FLabel.ButtonBuilder().text(localizer.getMessage("btnBuildNewJumpstartPack")).fontSize(16).build();

    /**
     * Constructor.
     */
    VSubmenuJumpstart() {
        btnStart.setEnabled(false);

        lblTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        lstPacks.setCaption(localizer.getMessage("lblJumpstartPacks"));
        lstPacks.setAllowMultipleSelections(true); // Enable multi-select for choosing 2 packs
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#populate()
     */
    @Override
    public void populate() {
        PnlDisplay pnlDisplay = VHomeUI.SINGLETON_INSTANCE.getPnlDisplay();
        pnlDisplay.removeAll();
        pnlDisplay.setLayout(new MigLayout("insets 0, gap 0, wrap, ax right"));
        pnlDisplay.add(lblTitle, "w 80%!, h 40px!, gap 0 0 15px 15px, ax right");

        pnlDisplay.add(lblInfo, "w 80%!, h 30px!, gap 0 10% 20px 5px");
        pnlDisplay.add(lblDir1, "gap 0 0 0 5px");
        pnlDisplay.add(lblDir2, "gap 0 0 0 5px");
        pnlDisplay.add(lblDir3, "gap 0 0 0 20px");

        pnlDisplay.add(btnBuildPack, "w 250px!, h 30px!, ax center, gap 0 10% 0 20px");
        pnlDisplay.add(new ItemManagerContainer(lstPacks), "w 80%!, gap 0 10% 0 0, pushy, growy");

        final JXButtonPanel grpPanel = new JXButtonPanel();
        grpPanel.add(radRandom, "w 200px!, h 30px!");
        grpPanel.add(radManual, "w 200px!, h 30px!");
        radRandom.setSelected(true);

        pnlStart.removeAll();
        pnlStart.setLayout(new MigLayout("insets 0, gap 0, wrap 2"));
        pnlStart.setOpaque(false);
        pnlStart.add(grpPanel, "gapright 20");
        pnlStart.add(btnStart);

        pnlDisplay.add(pnlStart, "gap 0 10% 50px 50px, ax center");

        pnlDisplay.repaintSelf();
        pnlDisplay.revalidate();
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#getGroup()
     */
    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.SANCTIONED;
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getMenuTitle()
     */
    @Override
    public String getMenuTitle() {
        return localizer.getMessage("lblJumpstart");
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getItemEnum()
     */
    @Override
    public EDocID getItemEnum() {
        return EDocID.HOME_JUMPSTART;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_JUMPSTART;
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
    public CSubmenuJumpstart getLayoutControl() {
        return CSubmenuJumpstart.SINGLETON_INSTANCE;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#setParentCell(forge.gui.framework.DragCell)
     */
    @Override
    public void setParentCell(final DragCell cell0) {
        this.parentCell = cell0;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getParentCell()
     */
    @Override
    public DragCell getParentCell() {
        return this.parentCell;
    }

    /** @return {@link forge.itemmanager.DeckManager} */
    public DeckManager getLstPacks() {
        return this.lstPacks;
    }

    /** @return {@link forge.toolbox.FLabel} */
    public FLabel getBtnBuildPack() {
        return this.btnBuildPack;
    }

    /** @return {@link forge.screens.home.StartButton} */
    public StartButton getBtnStart() {
        return this.btnStart;
    }

    /** @return {@link javax.swing.JRadioButton} */
    public JRadioButton getRadRandom() {
        return this.radRandom;
    }

    /** @return {@link javax.swing.JRadioButton} */
    public JRadioButton getRadManual() {
        return this.radManual;
    }
}

