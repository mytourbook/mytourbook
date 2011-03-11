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
import java.util.StringTokenizer;

import org.eclipse.babel.core.message.MessagesBundleGroup;
import org.eclipse.babel.core.message.MessagesBundleGroupAdapter;


/**
 * Default key tree model, using a delimiter to separate key sections
 * into nodes.  For instance, a dot (.) delimiter on the following key...<p>
 * <code>person.address.street</code><P>
 * ... will result in the following node hierarchy:<p>
 * <pre>
 * person
 *     address
 *         street
 * </p>
 * @author Pascal Essiembre
 */
public class DefaultKeyTreeModel extends AbstractKeyTreeModel {

    private KeyTreeNode rootNode = new KeyTreeNode(null, null, null);
    
    private String delimiter;
    private MessagesBundleGroup messagesBundleGroup;
    
    /**
     * Defaults to ".".
     */
    public DefaultKeyTreeModel(MessagesBundleGroup messagesBundleGroup) {
        this(messagesBundleGroup, "."); //$NON-NLS-1$
    }
    /**
     * Constructor.
     * @param messagesBundleGroup {@link MessagesBundleGroup} instance
     * @param delimiter key section delimiter
     */
    public DefaultKeyTreeModel(
    		MessagesBundleGroup messagesBundleGroup, String delimiter) {
        super();
        this.messagesBundleGroup = messagesBundleGroup;
        this.delimiter = delimiter;
        createTree();
        
        messagesBundleGroup.addMessagesBundleGroupListener(
                new MessagesBundleGroupAdapter() {
            public void keyAdded(String key) {
                createTreeNodes(key);
            }
            public void keyRemoved(String key) {
                removeTreeNodes(key);
            }
        });
    }

    /**
     * Gets the delimiter.
     * @return delimiter
     */
    public String getDelimiter() {
        return delimiter;
    }
    /**
     * Sets the delimiter.
     * @param delimiter delimiter
     */
    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }
    
    /**
     * @see org.eclipse.babel.core.message.tree.IKeyTreeModel#getRootNodes()
     */
    public KeyTreeNode[] getRootNodes() {
        return getChildren(rootNode);
    }

    /**
     * @see org.eclipse.babel.core.message.tree.IKeyTreeModel#
     * 		getChildren(org.eclipse.babel.core.message.tree.KeyTreeNode)
     */
    public KeyTreeNode[] getChildren(KeyTreeNode node) {
        KeyTreeNode[] nodes = node.getChildren();
        if (getComparator() != null) {
            Arrays.sort(nodes, getComparator());
        }
        return nodes;
    }

    /**
     * @see org.eclipse.babel.core.message.tree.IKeyTreeModel#
     *              getParent(org.eclipse.babel.core.message.tree.KeyTreeNode)
     */
    public KeyTreeNode getParent(KeyTreeNode node) {
        return node.getParent();
    }
    
    /**
     * @see org.eclipse.babel.core.message.tree.IKeyTreeModel
     *      #getMessagesBundleGroup()
     */
    public MessagesBundleGroup getMessagesBundleGroup() {
        //TODO consider moving this method (and part of constructor) to super
        return messagesBundleGroup;
    }

    private void createTree() {
        rootNode = new KeyTreeNode(null, null, null);
        String[] keys = messagesBundleGroup.getMessageKeys();
        for (int i = 0; i < keys.length; i++) {
			String key = keys[i];
            createTreeNodes(key);
        }
    }
    
    private void createTreeNodes(String bundleKey) {
        if (bundleKey == null) {
            return;
        }
        StringTokenizer tokens = new StringTokenizer(bundleKey, delimiter);
        KeyTreeNode node = rootNode;
        String bundleKeyPart = ""; //$NON-NLS-1$
        while (tokens.hasMoreTokens()) {
            String name = tokens.nextToken();
            bundleKeyPart += name;
            KeyTreeNode child = node.getChild(name);
            if (child == null) {
                child = new KeyTreeNode(node, name, bundleKeyPart);
                fireNodeAdded(child);
            }
            bundleKeyPart += delimiter;
            node = child;
        }
    }
    private void removeTreeNodes(String bundleKey) {
        if (bundleKey == null) {
            return;
        }
        StringTokenizer tokens = new StringTokenizer(bundleKey, delimiter);
        KeyTreeNode node = rootNode;
        while (tokens.hasMoreTokens()) {
            String name = tokens.nextToken();
            node = node.getChild(name);
            if (node == null) {
                System.err.println(
                    "No RegEx node matching bundleKey to remove"); //$NON-NLS-1$
                return;
            }
        }
        KeyTreeNode parentNode = node.getParent();
        parentNode.removeChild(node);
        fireNodeRemoved(node);
        while (parentNode != rootNode) {
            if (!parentNode.hasChildren() && !messagesBundleGroup.isMessageKey(
                    parentNode.getMessageKey())) {
                parentNode.getParent().removeChild(parentNode);
                fireNodeRemoved(parentNode);
            }
            parentNode = parentNode.getParent();
        }
    }
}
