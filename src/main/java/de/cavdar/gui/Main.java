package de.cavdar.gui;

import de.cavdar.gui.design.base.MainFrame;
import de.cavdar.gui.util.EnvironmentLockManager;
import de.cavdar.gui.util.TimelineLogger;
import de.cavdar.gui.view.itsq.ItsqExplorerView;
import de.cavdar.gui.view.itsq.ItsqMigrationToolView;

import javax.swing.*;

/**
 * ITSQ-Explorer Application entry point.
 * Contains the main() method to start the MDI application.
 *
 * @author ITSQ-Explorer (basierend auf TemplateGUI)
 * @version 1.0
 */
public class Main {

    /**
     * Application entry point.
     *
     * @param args command line arguments: [config-file-path]
     */
    public static void main(String[] args) {
        // Set config file path from command line argument (must be before AppConfig is loaded)
        if (args.length > 0 && args[0] != null && !args[0].isEmpty()) {
            System.setProperty("config.file", args[0]);
            TimelineLogger.info(Main.class, "Config file set from argument: {}", args[0]);
        }

        // Register shutdown hook to release environment lock on exit
        EnvironmentLockManager.registerShutdownHook();

        TimelineLogger.info(Main.class, "Starting ITSQ-Explorer");
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();

            // Register ITSQ-Test-Set view
            frame.registerView(ItsqExplorerView::new);

            // Register ITSQ Migration Tool view
            frame.registerView(ItsqMigrationToolView::new);

            frame.setVisible(true);
        });
    }
}
