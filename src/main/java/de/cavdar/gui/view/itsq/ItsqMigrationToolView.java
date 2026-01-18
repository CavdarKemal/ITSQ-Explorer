package de.cavdar.gui.view.itsq;

import de.cavdar.gui.design.base.BaseViewPanel;
import de.cavdar.gui.itsq.view.ItsqMigrationView;
import de.cavdar.gui.model.base.AppConfig;
import de.cavdar.gui.util.TimelineLogger;
import de.cavdar.gui.view.base.BaseView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * ITSQ Migration Tool View - BaseView wrapper for ItsqMigrationView.
 *
 * Provides migration functionality from OLD to NEW ITSQ structure.
 *
 * @author kemal
 * @version 1.0
 */
public class ItsqMigrationToolView extends BaseView {

    private ItsqMigrationView migrationView;

    public ItsqMigrationToolView() {
        super("ITSQ Migration (OLD -> NEW)");
        setSize(900, 700);

        TimelineLogger.debug(ItsqMigrationToolView.class, "ItsqMigrationToolView created");
    }

    @Override
    protected BaseViewPanel createPanel() {
        // Create ItsqMigrationView with AppConfig context
        migrationView = new ItsqMigrationView(AppConfig.getInstance());
        return new MigrationViewWrapper(migrationView);
    }

    @Override
    protected void setupToolbarActions() {
        // No additional toolbar - ItsqMigrationView has its own controls
    }

    @Override
    protected void setupListeners() {
        // All listeners are managed by ItsqMigrationView
    }

    // ===== ViewInfo Implementation =====

    @Override
    public String getMenuLabel() {
        return "ITSQ Migration Tool";
    }

    @Override
    public String getToolbarLabel() {
        return "Migration";
    }

    @Override
    public KeyStroke getKeyboardShortcut() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
    }

    @Override
    public Icon getIcon() {
        try {
            return new ImageIcon(getClass().getResource("/icons/arrow_right.png"));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getMenuGroup() {
        return "Verwaltung";
    }

    @Override
    public String getToolbarTooltip() {
        return "ITSQ von OLD nach NEW migrieren";
    }

    // ===== Accessors =====

    public ItsqMigrationView getMigrationView() {
        return migrationView;
    }

    // ===== Inner Classes =====

    private static class MigrationViewWrapper extends BaseViewPanel {
        public MigrationViewWrapper(ItsqMigrationView migrationView) {
            super();
            viewToolbar.setVisible(false);
            getContentPanel().add(migrationView, BorderLayout.CENTER);
        }
    }
}
