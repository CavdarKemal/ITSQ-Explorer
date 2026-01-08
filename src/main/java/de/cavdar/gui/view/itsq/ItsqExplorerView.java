package de.cavdar.gui.view.itsq;

import de.cavdar.gui.design.base.BaseViewPanel;
import de.cavdar.gui.itsq.view.ItsqMainView;
import de.cavdar.gui.model.base.AppConfig;
import de.cavdar.gui.util.TimelineLogger;
import de.cavdar.gui.view.base.BaseView;

import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * ITSQ Explorer View - BaseView wrapper for ItsqMainView.
 *
 * Responsibilities:
 * - Instantiate ItsqMainView with AppConfig context
 * - Trigger initialization when view becomes visible
 * - Provide ViewInfo metadata (menu, toolbar, shortcuts)
 *
 * All GUI management is delegated to ItsqMainView.
 *
 * @author TemplateGUI
 * @version 5.0
 */
public class ItsqExplorerView extends BaseView {

    private ItsqMainView mainView;

    public ItsqExplorerView() {
        super("ITSQ-Test-Sets Verwalten");
        setSize(1000, 700);

        // Initialize data when view is first activated
        addInternalFrameListener(new InternalFrameAdapter() {
            @Override
            public void internalFrameActivated(InternalFrameEvent e) {
                SwingUtilities.invokeLater(() -> mainView.initialize());
            }
        });

        TimelineLogger.debug(ItsqExplorerView.class, "ItsqExplorerView created");
    }

    @Override
    protected BaseViewPanel createPanel() {
        // Create ItsqMainView with AppConfig context
        mainView = new ItsqMainView(AppConfig.getInstance());
        return new ItsqMainViewWrapper(mainView);
    }

    @Override
    protected void setupToolbarActions() {
        // No additional toolbar - ItsqMainView has its own controls
    }

    @Override
    protected void setupListeners() {
        // All listeners are managed by ItsqMainView
    }

    // ===== ViewInfo Implementation =====

    @Override
    public String getMenuLabel() {
        return "ITSQ Explorer (JFD)";
    }

    @Override
    public String getToolbarLabel() {
        return "ITSQ-Test-Set";
    }

    @Override
    public KeyStroke getKeyboardShortcut() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_J, InputEvent.CTRL_DOWN_MASK);
    }

    @Override
    public Icon getIcon() {
        return new ImageIcon(getClass().getResource("/icons/folder_cubes.png"));
    }

    @Override
    public String getMenuGroup() {
        return "Verwaltung";
    }

    // ===== Accessors =====

    public ItsqMainView getMainView() {
        return mainView;
    }

    // ===== Inner Classes =====

    private static class ItsqMainViewWrapper extends BaseViewPanel {
        public ItsqMainViewWrapper(ItsqMainView mainView) {
            super();
            viewToolbar.setVisible(false);
            getContentPanel().add(mainView, BorderLayout.CENTER);
        }
    }
}
