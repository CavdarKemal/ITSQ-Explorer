package de.cavdar.gui.view.base;

import javax.swing.*;

/**
 * Interface for views that can be registered with MainFrame.
 * Provides metadata for automatic menu and toolbar creation.
 *
 * @author StandardMDIGUI
 * @version 1.0
 * @since 2024-12-25
 */
public interface ViewInfo {

    /**
     * Returns the label for the menu entry.
     *
     * @return menu label (required)
     */
    String getMenuLabel();

    /**
     * Returns the label for the toolbar button.
     * Return null to not show a toolbar button.
     *
     * @return toolbar label or null
     */
    default String getToolbarLabel() {
        return null;
    }

    /**
     * Returns the icon for menu and toolbar.
     * Return null for no icon.
     *
     * @return icon or null
     */
    default Icon getIcon() {
        return null;
    }

    /**
     * Returns the keyboard shortcut for the menu item.
     * Example: KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK)
     * Return null for no shortcut.
     *
     * @return keyboard shortcut or null
     */
    default KeyStroke getKeyboardShortcut() {
        return null;
    }

    /**
     * Returns the menu group name for organizing menu items.
     * Views with the same group will be grouped together.
     * Return null for default group.
     *
     * @return menu group name or null
     */
    default String getMenuGroup() {
        return null;
    }

    /**
     * Returns the tooltip text for the toolbar button.
     *
     * @return tooltip or null
     */
    default String getToolbarTooltip() {
        return getMenuLabel();
    }
}
