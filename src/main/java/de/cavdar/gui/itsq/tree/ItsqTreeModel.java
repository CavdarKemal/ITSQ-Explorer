package de.cavdar.gui.itsq.tree;

import de.cavdar.gui.itsq.model.*;
import de.cavdar.gui.util.TimelineLogger;

import javax.swing.tree.DefaultTreeModel;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Tree model for the ITSQ directory structure.
 * Creates typed tree nodes based on the directory structure.
 *
 * Structure:
 * - ITSQ (root)
 *   - OLD / NEW
 *     - ARCHIV-BESTAND(-PHx)
 *       - (PHASE-x)
 *         - *.xml
 *     - REF-EXPORTS
 *       - (PHASE-x)
 *         - c0x (Customer)
 *           - Options.cfg
 *           - Relevanz-xyz (Scenario)
 *             - *.xml
 *             - *.properties
 */
public class ItsqTreeModel extends DefaultTreeModel {

    private static final String ARCHIV_BESTAND = "ARCHIV-BESTAND";
    private static final String REF_EXPORTS = "REF-EXPORTS";

    private int totalFiles = 0;
    private int totalDirs = 0;

    public ItsqTreeModel(File itsqDir) {
        super(null);
        if (itsqDir != null && itsqDir.exists()) {
            reload(itsqDir);
        }
    }

    /**
     * Reloads the tree from the given ITSQ directory.
     *
     * @param itsqDir the ITSQ directory to scan
     */
    public void reload(File itsqDir) {
        if (itsqDir == null || !itsqDir.exists() || !itsqDir.isDirectory()) {
            TimelineLogger.warn(ItsqTreeModel.class, "Invalid ITSQ directory: {}", itsqDir);
            return;
        }

        // Reset statistics
        totalFiles = 0;
        totalDirs = 0;

        // Create root node
        ItsqRoot rootItem = new ItsqRoot(itsqDir);
        ItsqRootTreeNode rootNode = new ItsqRootTreeNode(rootItem);
        setRoot(rootNode);

        // Scan directory structure
        scanDirectory(itsqDir, rootNode, NodeContext.ROOT);

        // Notify listeners
        reload(rootNode);

        TimelineLogger.info(ItsqTreeModel.class, "Loaded ITSQ directory: {} ({} files, {} dirs)",
                itsqDir.getAbsolutePath(), totalFiles, totalDirs);
    }

    /**
     * Recursively scans a directory and creates typed tree nodes.
     *
     * @param dir        the directory to scan
     * @param parentNode the parent tree node
     * @param context    the current node context
     */
    private void scanDirectory(File dir, ItsqTreeNode parentNode, NodeContext context) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }

        // Sort: directories first, then by name
        Arrays.sort(children, Comparator
                .comparing(File::isFile)
                .thenComparing(File::getName));

        for (File child : children) {
            ItsqTreeNode childNode = createNodeForFile(child, context);
            parentNode.add(childNode);

            if (child.isDirectory()) {
                totalDirs++;
                NodeContext childContext = determineChildContext(child, context);
                scanDirectory(child, childNode, childContext);
            } else {
                totalFiles++;
            }
        }
    }

    /**
     * Creates a typed tree node based on the file and current context.
     */
    private ItsqTreeNode createNodeForFile(File file, NodeContext context) {
        String name = file.getName();
        String lowerName = name.toLowerCase();

        // Files: check extension first
        if (file.isFile()) {
            if (lowerName.endsWith(".xml")) {
                return new ItsqXmlTreeNode(new ItsqXmlFile(file));
            }
            if ("options.cfg".equals(lowerName)) {
                return new ItsqOptionsTreeNode(new ItsqOptionsFile(file));
            }
            if (lowerName.endsWith(".properties")) {
                return new ItsqPropertiesTreeNode(new ItsqPropertiesFile(file));
            }
            // Default: treat as generic XML file
            return new ItsqXmlTreeNode(new ItsqXmlFile(file));
        }

        // Directories: determine type based on context and name
        return switch (context) {
            case ROOT -> {
                if (name.startsWith(ARCHIV_BESTAND)) {
                    yield new ItsqArchivBestandTreeNode(new ItsqArchivBestand(file));
                } else if (REF_EXPORTS.equals(name)) {
                    yield new ItsqRefExportsTreeNode(new ItsqRefExports(file));
                }
                // Unknown top-level directory (OLD, NEW) - treat as generic root
                yield new ItsqTreeNode(new ItsqRoot(file));
            }
            case ARCHIV_BESTAND ->
                new ItsqArchivBestandPhaseTreeNode(new ItsqArchivBestandPhase(file));
            case ARCHIV_BESTAND_PHASE ->
                // Files under ARCHIV-BESTAND/PHASE-x are handled above
                new ItsqTreeNode(new ItsqArchivBestandPhase(file));
            case REF_EXPORTS ->
                new ItsqRefExportsPhaseTreeNode(new ItsqRefExportsPhase(file));
            case REF_EXPORTS_PHASE ->
                new ItsqCustomerTreeNode(new ItsqCustomer(file));
            case CUSTOMER -> {
                if (name.toLowerCase().startsWith("relevanz")) {
                    yield new ItsqScenarioTreeNode(new ItsqScenario(file));
                }
                // Other folders under customer
                yield new ItsqScenarioTreeNode(new ItsqScenario(file));
            }
            case SCENARIO ->
                // Subdirectories under scenario
                new ItsqScenarioTreeNode(new ItsqScenario(file));
        };
    }

    /**
     * Determines the context for child nodes.
     */
    private NodeContext determineChildContext(File dir, NodeContext parentContext) {
        String name = dir.getName();

        return switch (parentContext) {
            case ROOT -> {
                if (name.startsWith(ARCHIV_BESTAND)) {
                    yield NodeContext.ARCHIV_BESTAND;
                } else if (REF_EXPORTS.equals(name)) {
                    yield NodeContext.REF_EXPORTS;
                }
                // OLD/NEW directories stay in ROOT context
                yield NodeContext.ROOT;
            }
            case ARCHIV_BESTAND -> NodeContext.ARCHIV_BESTAND_PHASE;
            case ARCHIV_BESTAND_PHASE -> NodeContext.ARCHIV_BESTAND_PHASE;
            case REF_EXPORTS -> NodeContext.REF_EXPORTS_PHASE;
            case REF_EXPORTS_PHASE -> NodeContext.CUSTOMER;
            case CUSTOMER -> NodeContext.SCENARIO;
            case SCENARIO -> NodeContext.SCENARIO;
        };
    }

    public int getTotalFiles() {
        return totalFiles;
    }

    public int getTotalDirs() {
        return totalDirs;
    }

    /**
     * Context enum for determining node types during tree construction.
     */
    private enum NodeContext {
        ROOT,
        ARCHIV_BESTAND,
        ARCHIV_BESTAND_PHASE,
        REF_EXPORTS,
        REF_EXPORTS_PHASE,
        CUSTOMER,
        SCENARIO
    }
}
