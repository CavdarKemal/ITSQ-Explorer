package de.cavdar.gui.design.base;

import de.cavdar.gui.view.base.BaseView;
import de.cavdar.gui.view.db.DatabaseView;
import de.cavdar.gui.util.TimelineLogger;


import javax.swing.*;
import java.awt.*;
import java.beans.PropertyVetoException;
import java.util.Arrays;

/**
 * Panel containing the MDI desktop pane for internal frames.
 * Can be embedded in the main frame's split pane.
 *
 * @author StandardMDIGUI
 * @version 1.0
 * @since 2024-12-25
 */
public class DesktopPanel extends EmbeddablePanel {


    private JDesktopPane desktopPane;
    private DatabaseView databaseView;

    /**
     * Constructs the DesktopPanel.
     */
    public DesktopPanel() {
        super("Desktop");
    }

    @Override
    protected void initializePanel() {
        desktopPane = new JDesktopPane();
        add(desktopPane, BorderLayout.CENTER);
        TimelineLogger.debug(DesktopPanel.class, "DesktopPanel initialized");
    }

    /**
     * Returns the desktop pane.
     *
     * @return the JDesktopPane
     */
    public JDesktopPane getDesktopPane() {
        return desktopPane;
    }

    /**
     * Opens a view in the desktop pane.
     *
     * @param view the view to open
     */
    public void openView(BaseView view) {
        TimelineLogger.info(DesktopPanel.class, "Opening view: {}", view.getTitle());
        desktopPane.add(view);
        view.setVisible(true);
    }

    /**
     * Opens or shows the DatabaseView with the specified connection.
     *
     * @param connectionName the connection name to use
     */
    public void openOrShowDatabaseView(String connectionName) {
        if (databaseView != null && !databaseView.isClosed()) {
            try {
                if (databaseView.isIcon()) {
                    databaseView.setIcon(false);
                }
                databaseView.setSelected(true);
                databaseView.toFront();
            } catch (PropertyVetoException e) {
                TimelineLogger.warn(DesktopPanel.class, "Could not bring DatabaseView to front", e);
            }
        } else {
            databaseView = new DatabaseView(connectionName);
            desktopPane.add(databaseView);
            databaseView.setVisible(true);
        }
    }

    /**
     * Returns all non-iconified frames.
     *
     * @return array of open frames
     */
    private JInternalFrame[] getOpenFrames() {
        return Arrays.stream(desktopPane.getAllFrames())
                .filter(f -> !f.isIcon())
                .toArray(JInternalFrame[]::new);
    }

    /**
     * Arranges frames in cascade layout.
     */
    public void layoutCascaded() {
        JInternalFrame[] frames = getOpenFrames();
        TimelineLogger.debug(DesktopPanel.class, "Arranging {} frames in cascade layout", frames.length);

        int x = 0, y = 0;
        int offset = 30;

        for (JInternalFrame f : frames) {
            try {
                f.setMaximum(false);
            } catch (PropertyVetoException e) {
                TimelineLogger.warn(DesktopPanel.class, "Could not restore frame from maximized state", e);
            }

            f.setBounds(x, y, 600, 400);
            f.toFront();
            x += offset;
            y += offset;

            if (x + 200 > desktopPane.getWidth()) x = 0;
            if (y + 200 > desktopPane.getHeight()) y = 0;
        }
    }

    /**
     * Arranges frames side by side (vertical tile).
     */
    public void layoutTileVertical() {
        JInternalFrame[] frames = getOpenFrames();
        if (frames.length == 0) return;

        TimelineLogger.debug(DesktopPanel.class, "Arranging {} frames in vertical tile layout", frames.length);

        int width = desktopPane.getWidth() / frames.length;
        int height = desktopPane.getHeight();

        for (int i = 0; i < frames.length; i++) {
            try {
                frames[i].setMaximum(false);
            } catch (PropertyVetoException e) {
                TimelineLogger.warn(DesktopPanel.class, "Could not restore frame at position {}", i, e);
            }
            frames[i].setBounds(i * width, 0, width, height);
        }
    }

    /**
     * Arranges frames stacked (horizontal tile).
     */
    public void layoutTileHorizontal() {
        JInternalFrame[] frames = getOpenFrames();
        if (frames.length == 0) return;

        TimelineLogger.debug(DesktopPanel.class, "Arranging {} frames in horizontal tile layout", frames.length);

        int width = desktopPane.getWidth();
        int height = desktopPane.getHeight() / frames.length;

        for (int i = 0; i < frames.length; i++) {
            try {
                frames[i].setMaximum(false);
            } catch (PropertyVetoException e) {
                TimelineLogger.warn(DesktopPanel.class, "Could not restore frame at position {}", i, e);
            }
            frames[i].setBounds(0, i * height, width, height);
        }
    }
}
