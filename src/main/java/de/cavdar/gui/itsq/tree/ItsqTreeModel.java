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
 *   - ARCHIV-BESTAND
 *     - PHASE-x
 *       - *.xml
 *   - REF-EXPORTS
 *     - PHASE-x
 *       - c0x (Customer)
 *         - Options.cfg
 *         - Relevanz-xyz (Scenario)
 *           - *.xml
 *           - *.properties
 */
public class ItsqTreeModel extends DefaultTreeModel {

    private static final String ARCHIV_BESTAND = "ARCHIV-BESTAND";
    private static final String REF_EXPORTS = "REF-EXPORTS";

    private static final String ALL_FILTER = "Alle";

    private int totalFiles = 0;
    private int totalDirs = 0;
    private String filterText = "";
    private boolean activeOnly = false;
    private String sourceFilter = ALL_FILTER;
    private String phaseFilter = ALL_FILTER;

    public ItsqTreeModel(File itsqDir) {
        super(null);
        if (itsqDir != null && itsqDir.exists()) {
            reload(itsqDir);
        }
    }

    /**
     * Reloads the tree from the given ITSQ directory without filter.
     */
    public void reload(File itsqDir) {
        reload(itsqDir, "", false, ALL_FILTER, ALL_FILTER);
    }

    /**
     * Reloads the tree from the given ITSQ directory with filter settings.
     *
     * @param itsqDir    the ITSQ directory to scan
     * @param filterText filter text for file/directory names (case-insensitive)
     * @param activeOnly if true, only show active items (not yet implemented - placeholder)
     */
    public void reload(File itsqDir, String filterText, boolean activeOnly) {
        reload(itsqDir, filterText, activeOnly, ALL_FILTER, ALL_FILTER);
    }

    /**
     * Reloads the tree from the given ITSQ directory with all filter settings.
     *
     * @param itsqDir      the ITSQ directory to scan
     * @param filterText   filter text for file/directory names (case-insensitive)
     * @param activeOnly   if true, only show active items (not yet implemented - placeholder)
     * @param sourceFilter source filter (ARCHIV-BESTAND, REF-EXPORTS, or "Alle")
     * @param phaseFilter  phase filter (PHASE-1, PHASE-2, or "Alle")
     */
    public void reload(File itsqDir, String filterText, boolean activeOnly,
                       String sourceFilter, String phaseFilter) {
        if (itsqDir == null || !itsqDir.exists() || !itsqDir.isDirectory()) {
            TimelineLogger.warn(ItsqTreeModel.class, "Invalid ITSQ directory: {}", itsqDir);
            return;
        }

        // Store filter settings
        this.filterText = filterText != null ? filterText.toLowerCase() : "";
        this.activeOnly = activeOnly;
        this.sourceFilter = sourceFilter != null ? sourceFilter : ALL_FILTER;
        this.phaseFilter = phaseFilter != null ? phaseFilter : ALL_FILTER;

        // Reset statistics
        totalFiles = 0;
        totalDirs = 0;

        // Create root node
        ItsqRoot rootItem = new ItsqRoot(itsqDir);
        ItsqRootTreeNode rootNode = new ItsqRootTreeNode(rootItem);
        setRoot(rootNode);

        // Scan directory structure with filter
        scanDirectory(itsqDir, rootNode, NodeContext.ROOT, 0);

        // Notify listeners
        reload(rootNode);

        TimelineLogger.info(ItsqTreeModel.class, "Loaded ITSQ directory: {} ({} files, {} dirs, filter: '{}', source: '{}', phase: '{}', activeOnly: {})",
                itsqDir.getAbsolutePath(), totalFiles, totalDirs, filterText, sourceFilter, phaseFilter, activeOnly);
    }

    /**
     * Recursively scans a directory and creates typed tree nodes.
     * Applies filter settings to show only matching items.
     *
     * @param dir        the directory to scan
     * @param parentNode the parent tree node
     * @param context    the current node context
     * @param depth      current depth in the tree (0 = root level)
     */
    private void scanDirectory(File dir, ItsqTreeNode parentNode, NodeContext context, int depth) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }

        // Sort: directories first, then by name
        Arrays.sort(children, Comparator
                .comparing(File::isFile)
                .thenComparing(File::getName));

        for (File child : children) {
            String childName = child.getName();

            // Apply source filter at depth 0 (ARCHIV-BESTAND, REF-EXPORTS)
            if (depth == 0 && !ALL_FILTER.equals(sourceFilter)) {
                if (!childName.equals(sourceFilter)) {
                    continue;
                }
            }

            // Apply phase filter at depth 1 (PHASE-1, PHASE-2)
            if (depth == 1 && !ALL_FILTER.equals(phaseFilter)) {
                if (!childName.equals(phaseFilter)) {
                    continue;
                }
            }

            // Check if this item or any of its children match the text filter
            if (!matchesFilter(child)) {
                continue;
            }

            ItsqTreeNode childNode = createNodeForFile(child, context);
            parentNode.add(childNode);

            if (child.isDirectory()) {
                totalDirs++;
                NodeContext childContext = determineChildContext(child, context);
                scanDirectory(child, childNode, childContext, depth + 1);

                // Remove directory if it has no children after filtering
                if (childNode.getChildCount() == 0 && !filterText.isEmpty()) {
                    // Only remove if name itself doesn't match
                    if (!child.getName().toLowerCase().contains(filterText)) {
                        parentNode.remove(childNode);
                        totalDirs--;
                    }
                }
            } else {
                totalFiles++;
            }
        }
    }

    /**
     * Checks if a file or directory matches the current filter.
     * For directories, also checks if any descendant matches.
     */
    private boolean matchesFilter(File file) {
        // No filter -> everything matches
        if (filterText.isEmpty()) {
            return true;
        }

        String name = file.getName().toLowerCase();

        // Direct name match
        if (name.contains(filterText)) {
            return true;
        }

        // For directories, check if any child matches
        if (file.isDirectory()) {
            return hasMatchingDescendant(file);
        }

        return false;
    }

    /**
     * Recursively checks if any descendant of a directory matches the filter.
     */
    private boolean hasMatchingDescendant(File dir) {
        File[] children = dir.listFiles();
        if (children == null) {
            return false;
        }

        for (File child : children) {
            String name = child.getName().toLowerCase();
            if (name.contains(filterText)) {
                return true;
            }
            if (child.isDirectory() && hasMatchingDescendant(child)) {
                return true;
            }
        }
        return false;
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
                if (ARCHIV_BESTAND.equals(name)) {
                    yield new ItsqArchivBestandTreeNode(new ItsqArchivBestand(file));
                } else if (REF_EXPORTS.equals(name)) {
                    yield new ItsqRefExportsTreeNode(new ItsqRefExports(file));
                }
                // Unknown top-level directory - treat as generic
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
                if (ARCHIV_BESTAND.equals(name)) {
                    yield NodeContext.ARCHIV_BESTAND;
                } else if (REF_EXPORTS.equals(name)) {
                    yield NodeContext.REF_EXPORTS;
                }
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
