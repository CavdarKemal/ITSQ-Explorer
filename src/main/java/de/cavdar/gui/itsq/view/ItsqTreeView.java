package de.cavdar.gui.itsq.view;

import de.cavdar.gui.itsq.design.ItsqTreePanel;
import de.cavdar.gui.itsq.model.ItsqItem;
import de.cavdar.gui.itsq.tree.*;
import de.cavdar.gui.util.TimelineLogger;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.io.File;
import java.util.function.Consumer;

/**
 * View for the ITSQ tree structure.
 * Manages TreeModel, expansion, and selection handling.
 *
 * @author TemplateGUI
 */
public class ItsqTreeView extends ItsqTreePanel {

    private ItsqTreeModel treeModel;
    private Consumer<ItsqTreeNode> selectionCallback;

    public ItsqTreeView() {
        super();
        // Initialize with empty tree model
        treeModel = new ItsqTreeModel(null);
        getTreeItsq().setModel(treeModel);
    }

    /**
     * Sets the callback to be invoked when a tree node is selected.
     *
     * @param callback the callback receiving the selected node
     */
    public void setSelectionCallback(Consumer<ItsqTreeNode> callback) {
        this.selectionCallback = callback;

        // Setup tree selection listener
        getTreeItsq().addTreeSelectionListener(e -> {
            Object selected = getTreeItsq().getLastSelectedPathComponent();
            if (selected instanceof ItsqTreeNode node && selectionCallback != null) {
                selectionCallback.accept(node);
            }
        });
    }

    /**
     * Reloads the tree from the given directory.
     *
     * @param itsqDir the ITSQ directory to scan
     */
    public void reload(File itsqDir) {
        treeModel.reload(itsqDir);
        expandToLevel(2);
        TimelineLogger.debug(ItsqTreeView.class, "Tree reloaded: {} files, {} dirs",
                treeModel.getTotalFiles(), treeModel.getTotalDirs());
    }

    /**
     * Expands the tree to the specified level.
     *
     * @param level the level to expand to (0 = root only)
     */
    public void expandToLevel(int level) {
        Object root = treeModel.getRoot();
        if (root instanceof ItsqTreeNode rootNode) {
            expandNode(rootNode, 0, level);
        }
    }

    private void expandNode(ItsqTreeNode node, int currentLevel, int maxLevel) {
        if (currentLevel >= maxLevel) {
            return;
        }
        TreePath path = new TreePath(node.getPath());
        getTreeItsq().expandPath(path);

        for (int i = 0; i < node.getChildCount(); i++) {
            if (node.getChildAt(i) instanceof ItsqTreeNode child) {
                expandNode(child, currentLevel + 1, maxLevel);
            }
        }
    }

    /**
     * Returns the currently selected ItsqItem, or null if none selected.
     */
    public ItsqItem getSelectedItem() {
        Object selected = getTreeItsq().getLastSelectedPathComponent();
        if (selected instanceof ItsqTreeNode node) {
            return node.getItsqItem();
        }
        return null;
    }

    /**
     * Returns the tree model.
     */
    public ItsqTreeModel getTreeModel() {
        return treeModel;
    }

    /**
     * Returns the total number of files in the tree.
     */
    public int getTotalFiles() {
        return treeModel.getTotalFiles();
    }

    /**
     * Returns the total number of directories in the tree.
     */
    public int getTotalDirs() {
        return treeModel.getTotalDirs();
    }
}
