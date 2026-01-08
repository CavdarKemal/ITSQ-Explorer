package de.cavdar.gui.design.base;

import javax.swing.*;
import java.awt.*;

/**
 * Base GUI panel for all views - contains only layout and components.
 * No listeners or business logic.
 * <p>
 * This class can be extended by GUI designer generated classes.
 * Fields are protected for access from the View class.
 * <p>
 * Structure:
 * - Top: Toolbar (viewToolbar)
 * - Center: Content panel (contentPanel) - to be filled by subclasses
 * - Bottom: Status panel with progress bar and cancel button
 *
 * @author StandardMDIGUI
 * @version 1.0
 * @since 2024-12-25
 */
public class BaseViewPanel extends JPanel {

    // Toolbar
    protected JToolBar viewToolbar;

    // Content area
    protected JPanel contentPanel;

    // Status bar components
    protected JPanel statusPanel;
    protected JProgressBar progressBar;
    protected JButton btnCancel;

    /**
     * Constructs the base panel with standard layout.
     */
    public BaseViewPanel() {
        initComponents();
    }

    /**
     * Initializes all GUI components.
     * Override in subclass for custom initialization.
     */
    protected void initComponents() {
        setLayout(new BorderLayout());

        // Toolbar at top
        viewToolbar = new JToolBar();
        viewToolbar.setFloatable(false);
        add(viewToolbar, BorderLayout.NORTH);

        // Content panel in center
        contentPanel = new JPanel(new BorderLayout());
        add(contentPanel, BorderLayout.CENTER);

        // Status panel at bottom
        statusPanel = new JPanel(new BorderLayout());

        progressBar = new JProgressBar();
        progressBar.setVisible(false);

        btnCancel = new JButton("Cancel");
        btnCancel.setName("Cancel");
        btnCancel.setVisible(false);

        statusPanel.add(progressBar, BorderLayout.CENTER);
        statusPanel.add(btnCancel, BorderLayout.EAST);
        add(statusPanel, BorderLayout.SOUTH);
    }

    /**
     * Shows or hides the progress indicator.
     *
     * @param visible       true to show, false to hide
     * @param indeterminate true for indeterminate progress
     */
    public void setProgressVisible(boolean visible, boolean indeterminate) {
        progressBar.setIndeterminate(indeterminate);
        progressBar.setVisible(visible);
        btnCancel.setVisible(visible);
    }

    // ===== Getters for View access =====

    public JToolBar getViewToolbar() {
        return viewToolbar;
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }

    public JProgressBar getProgressBar() {
        return progressBar;
    }

    public JButton getCancelButton() {
        return btnCancel;
    }

    public JPanel getStatusPanel() {
        return statusPanel;
    }
}
