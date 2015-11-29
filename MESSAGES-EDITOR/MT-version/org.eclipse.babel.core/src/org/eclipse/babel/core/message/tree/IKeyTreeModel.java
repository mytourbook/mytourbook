/*******************************************************************************
 * Copyright (c) 2007 Pascal Essiembre.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Pascal Essiembre - initial API and implementation
 ******************************************************************************/
package org.eclipse.babel.core.message.tree;

import java.util.Comparator;

import org.eclipse.babel.core.message.MessagesBundleGroup;
import org.eclipse.babel.core.message.tree.visitor.IKeyTreeVisitor;


/**
 * Hierarchical representation of all keys making up a 
 * {@link MessagesBundleGroup}.
 * @author Pascal Essiembre (pascal@essiembre.com)
 */
public interface IKeyTreeModel {

	/**
	 * Gets the key tree root nodes.
	 * @return key tree root nodes
	 */
    KeyTreeNode[] getRootNodes();
    
    /**
     * Gets the child nodes of a given key tree node.
     * @param node the node from which to get children
     * @return child nodes
     */
    KeyTreeNode[] getChildren(KeyTreeNode node);

    /**
     * Gets the parent node of the given node.
     * @param node node from which to get parent
     * @return parent node
     */
    KeyTreeNode getParent(KeyTreeNode node);

    /**
     * Adds a key tree model listener.
     * @param listener key tree model listener
     */
    void addKeyTreeModelListener(IKeyTreeModelListener listener);
    /**
     * Removes a key tree model listener.
     * @param listener key tree model listener
     */
    void removeKeyTreeModelListener(IKeyTreeModelListener listener);

    /**
     * Sets the node comparator for sorting sibling nodes.
     * @param comparator node comparator
     */
    void setComparator(Comparator<KeyTreeNode> comparator);
    
    //TODO KeyTreeNode[] getPath(KeyTreeNode node)
    
    /**
     * Gets all nodes on a branch, starting (and including) with parent node.
     * This has the same effect of calling <code>getChildren(KeyTreeNode)</code>
     * recursively on all children.
     * @param parentNode root of a branch
     * @return all nodes on a branch
     */
    KeyTreeNode[] getBranch(KeyTreeNode parentNode);
    
    /**
     * Accepts the visitor, visiting the given node argument, along with all
     * its children.  Passing a <code>null</code> node will
     * walk the entire tree.
     * @param visitor the object to visit
     * @param node the starting key tree node
     */
    void accept(IKeyTreeVisitor visitor, KeyTreeNode node);

    /**
     * Gets the messages bundle group that this key tree represents.
     * @return messages bundle group
     */
    MessagesBundleGroup getMessagesBundleGroup();
    
    /**
     * Depth first for the first leaf node that is not filtered.
     * It makes the entire branch not not filtered
     * 
     * @param filter The leaf filter.
     * @param node
     * @return false if this node or one of its descendant is not filtered
     */
    public boolean isBranchFiltered(IKeyTreeNodeLeafFilter filter, KeyTreeNode node);
    
    interface IKeyTreeNodeLeafFilter {
    	/**
    	 * @param leafNode A leaf node. Must not be called if the node has children
    	 * @return true if this node should be filtered.
    	 */
    	boolean isFilteredLeaf(KeyTreeNode leafNode);
    }
    
}
