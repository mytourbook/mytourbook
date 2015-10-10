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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.babel.core.message.tree.visitor.IKeyTreeVisitor;


/**
 * Base key tree model implementation.
 * @author Pascal Essiembre
 */
public abstract class AbstractKeyTreeModel implements IKeyTreeModel {

    private List<IKeyTreeModelListener> listeners = new ArrayList<IKeyTreeModelListener>();
    private Comparator<KeyTreeNode> comparator;
    
    protected static final KeyTreeNode[] EMPTY_NODES = new KeyTreeNode[]{};


    /**
     * @see org.eclipse.babel.core.message.tree.IKeyTreeModel
     *      #addKeyTreeModelListener(
     *              org.eclipse.babel.core.message.tree.IKeyTreeModelListener)
     */
    public void addKeyTreeModelListener(IKeyTreeModelListener listener) {
        listeners.add(0, listener);
    }

    /**
     * @see org.eclipse.babel.core.message.tree.IKeyTreeModel#
     *        removeKeyTreeModelListener(
     *                org.eclipse.babel.core.message.tree.IKeyTreeModelListener)
     */
    public void removeKeyTreeModelListener(IKeyTreeModelListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Notify all listeners that a node was added.
     * @param node added node
     */
    protected void fireNodeAdded(KeyTreeNode node)  {
        for (IKeyTreeModelListener listener : listeners) {
            listener.nodeAdded(node);
        }
    }
    /**
     * Notify all listeners that a node was removed.
     * @param node removed node
     */
    protected void fireNodeRemoved(KeyTreeNode node)  {
    	for (IKeyTreeModelListener listener : listeners) {
            listener.nodeRemoved(node);
        }
    }

    /**
     * @see org.eclipse.babel.core.message.tree.IKeyTreeModel#getBranch(
     *              org.eclipse.babel.core.message.tree.KeyTreeNode)
     */
    public KeyTreeNode[] getBranch(KeyTreeNode parentNode) {
        Set<KeyTreeNode> childNodes = new TreeSet<KeyTreeNode>();
        childNodes.add(parentNode);
        for (KeyTreeNode childNode : getChildren(parentNode)) {
            childNodes.addAll(
                    Arrays.asList(getBranch(childNode)));
        }
        return childNodes.toArray(EMPTY_NODES);
    }

    /**
     * @see org.eclipse.babel.core.message.tree.IKeyTreeModel#accept(
     *              org.eclipse.babel.core.message.tree.visitor.IKeyTreeVisitor,
     *              org.eclipse.babel.core.message.tree.KeyTreeNode)
     */
    public void accept(IKeyTreeVisitor visitor, KeyTreeNode node) {
        if (node != null) {
            visitor.visitKeyTreeNode(node);
        }
        KeyTreeNode[] nodes = getChildren(node);
        for (int i = 0; i < nodes.length; i++) {
            accept(visitor, nodes[i]);
        }
    }

    /**
     * Gets the comparator.
     * @return the comparator
     */
    public Comparator<KeyTreeNode> getComparator() {
        return comparator;
    }

    /**
     * Sets the comparator.
     * @param comparator the comparator to set
     */
    public void setComparator(Comparator<KeyTreeNode> comparator) {
        this.comparator = comparator;
    }
    
    /**
     * Depth first for the first leaf node that is not filtered.
     * It makes the entire branch not not filtered
     * 
     * @param filter The leaf filter.
     * @param node
     * @return true if this node or one of its descendant is in the filter (ie is displayed)
     */
    public boolean isBranchFiltered(IKeyTreeNodeLeafFilter filter, KeyTreeNode node) {
    	if (!node.hasChildren()) {
    		return filter.isFilteredLeaf(node);
    	} else {
    		//depth first:
    		for (KeyTreeNode childNode : node.getChildrenInternal()) {
    			if (isBranchFiltered(filter, childNode)) {
    				return true;
    			}
    		}
    	}
    	return false;
    }
    
    
}
