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

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.babel.core.message.MessagesBundleGroup;
import org.eclipse.babel.core.message.MessagesBundleGroupAdapter;

/**
 * A flat representation of a tree.  In essence this model is meant to represent
 * all {@link MessagesBundleGroup} keys in a non-hierarchical way, yet
 * still using the key tree model for compatibility.
 * @author Pascal Essiembre
 */
public class FlatKeyTreeModel extends AbstractKeyTreeModel {

    private MessagesBundleGroup messagesBundleGroup;
    
    // Cached elements
    private final Map<String,KeyTreeNode> nodes = new TreeMap<String,KeyTreeNode>();
    
    /**
     * Constructor.
     * @param messagesBundleGroup {@link MessagesBundleGroup} instance
     */
    public FlatKeyTreeModel(MessagesBundleGroup messagesBundleGroup) {
        super();
        this.messagesBundleGroup = messagesBundleGroup;
        createTree();
        messagesBundleGroup.addMessagesBundleGroupListener(
                new MessagesBundleGroupAdapter() {
            public void keyAdded(String key) {
                KeyTreeNode node = new KeyTreeNode(null, key, key);
                nodes.put(key, node);
                fireNodeAdded(node);
            }
            public void keyRemoved(String key) {
                KeyTreeNode node = nodes.get(key);
                fireNodeRemoved(node);
            }
        });
    }

    /**
     * @see org.eclipse.babel.core.message.tree.IKeyTreeModel#getRootNodes()
     */
    public KeyTreeNode[] getRootNodes() {
        KeyTreeNode[] rootNodes =
                nodes.values().toArray(EMPTY_NODES);
        if (getComparator() != null) {
            Arrays.sort(rootNodes, getComparator());
        }
        return rootNodes;
    }

    /**
     * @see org.eclipse.babel.core.message.tree.IKeyTreeModel#
     * 		getChildren(org.eclipse.babel.core.message.tree.KeyTreeNode)
     */
    public KeyTreeNode[] getChildren(KeyTreeNode node) {
        return EMPTY_NODES;
    }

    /**
     * @see org.eclipse.babel.core.message.tree.IKeyTreeModel#getParent(
     *              org.eclipse.babel.core.message.tree.KeyTreeNode)
     */
    public KeyTreeNode getParent(KeyTreeNode node) {
        return null;
    }

    /**
     * @see org.eclipse.babel.core.message.tree.IKeyTreeModel
     *      #getMessagesBundleGroup()
     */
    public MessagesBundleGroup getMessagesBundleGroup() {
        return messagesBundleGroup;
    }

    private void createTree() {
        String[] keys = messagesBundleGroup.getMessageKeys();
        nodes.clear();
        for (int i = 0; i < keys.length; i++) {
			String key = keys[i];
            nodes.put(key, new KeyTreeNode(null, key, key));
        }
    }
}
