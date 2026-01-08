package de.cavdar.gui.util;

import javax.swing.*;
import java.net.URL;

/**
 * Utility class for loading icons from the resources/icons folder.
 *
 * @author StandardMDIGUI
 * @version 1.0
 * @since 2024-12-26
 */
public final class IconLoader {
    private static final String ICONS_PATH = "icons/";

    private IconLoader() {
        // Utility class - no instantiation
    }

    /**
     * Loads an icon from the resources/icons folder.
     *
     * @param name the icon filename (e.g., "client.png")
     * @return the loaded Icon, or null if not found
     */
    public static Icon load(String name) {
        URL url = IconLoader.class.getClassLoader().getResource(ICONS_PATH + name);
        if (url != null) {
            return new ImageIcon(url);
        }
        TimelineLogger.warn(IconLoader.class, "Icon not found: {}", name);
        return null;
    }

    /**
     * Loads an icon with a fallback to UIManager icon.
     *
     * @param name        the icon filename
     * @param fallbackKey UIManager icon key as fallback
     * @return the loaded Icon, or fallback if not found
     */
    public static Icon loadWithFallback(String name, String fallbackKey) {
        Icon icon = load(name);
        if (icon != null) {
            return icon;
        }
        return UIManager.getIcon(fallbackKey);
    }
}
