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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.babel.core.util.BabelUtils;

/**
 * Key tree node.
 * @author Pascal Essiembre
 */
public class KeyTreeNode implements Comparable<KeyTreeNode> {

    public static final KeyTreeNode[] EMPTY_KEY_TREE_NODES =
            new KeyTreeNode[] {};
    private final KeyTreeNode parent;
    private final String name;
    private String messageKey;
    private final Map<String, KeyTreeNode> children = new TreeMap<String, KeyTreeNode>();
    
    /**
     * Constructor.
     * @param parent parent node
     * @param name node name
     * @param messageKey messages bundle key
     */
    public KeyTreeNode(
    		KeyTreeNode parent, String name, String messageKey) {
        super();
        this.parent = parent;
        this.name = name;
        this.messageKey = messageKey;
        if (parent != null) {
            parent.addChild(this);
        }
    }

    /**
     * Gets the node name.
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the node parent.
     * @return Returns the parent.
     */
    public KeyTreeNode getParent() {
        return parent;
    }

    /**
     * Gets the message key represented by this node.
     * @return message key
     */
    public String getMessageKey() {
        return messageKey;
    }
    
    /**
     * Gets all notes from root to this node.
     * @return all notes from root to this node
     */
    /*default*/ KeyTreeNode[] getPath() {
        List<KeyTreeNode> nodes = new ArrayList<KeyTreeNode>();
        KeyTreeNode node = this;
        while (node != null && node.getName() != null) {
            nodes.add(0, node);
            node = node.getParent();
        }
        return nodes.toArray(EMPTY_KEY_TREE_NODES);
    }

    /*default*/ KeyTreeNode[] getChildren() {
        return children.values().toArray(EMPTY_KEY_TREE_NODES);
    }
    /*default*/ boolean hasChildren() {
        return !children.isEmpty();
    }
    /*default*/ KeyTreeNode getChild(String childName) {
        return children.get(childName);
    }
    
    /**
     * @return the children without creating a new object
     */
    Collection<KeyTreeNode> getChildrenInternal() {
    	return children.values();
    }
    
    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(KeyTreeNode node) {
        if (parent == null && node.parent != null) {
            return -1;
        }
        if (parent != null && node.parent == null) {
            return 1;
        }
        return name.compareTo(node.name);
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof KeyTreeNode)) {
            return false;
        }
        KeyTreeNode node = (KeyTreeNode) obj;
        return BabelUtils.equals(name, node.name)
                && BabelUtils.equals(parent, node.parent);
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "KeyTreeNode=[[parent=" + parent //$NON-NLS-1$
              + "][name=" + name //$NON-NLS-1$
              + "][messageKey=" + messageKey + "]]"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    /*default*/ void addChild(KeyTreeNode childNode) {
        children.put(childNode.getName(), childNode);
    }
    /*default*/ void removeChild(KeyTreeNode childNode) {
        children.remove(childNode.getName());
        //TODO remove parent on child node?
    }
    
}
