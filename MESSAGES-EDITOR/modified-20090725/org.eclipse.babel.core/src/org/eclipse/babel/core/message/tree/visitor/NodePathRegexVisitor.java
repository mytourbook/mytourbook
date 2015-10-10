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
package org.eclipse.babel.core.message.tree.visitor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.babel.core.message.tree.KeyTreeNode;

/**
 * Visitor for finding keys matching the given regular expression.
 * @author Pascal Essiembre (pascal@essiembre.com)
 */
public class NodePathRegexVisitor implements IKeyTreeVisitor {

    /** Holder for matching keys. */
    private List<KeyTreeNode> nodes = new ArrayList<KeyTreeNode>();
    private final String regex;
    
    /**
     * Constructor.
     */
    public NodePathRegexVisitor(String regex) {
        super();
        this.regex = regex;
    }

    /**
     * @see org.eclipse.babel.core.message.tree.visitor.IKeyTreeVisitor
     *      #visitKeyTreeNode(org.eclipse.babel.core.message.tree.KeyTreeNode)
     */
    public void visitKeyTreeNode(KeyTreeNode node) {
        if (node.getMessageKey().matches(regex)) {
            nodes.add(node);
        }
    }

    /**
     * Gets matching key tree nodes.
     * @return matching key tree nodes
     */
    public List<KeyTreeNode> getKeyTreeNodes() {
        return nodes;
    }

    /**
     * Gets matching key tree node paths.
     * @return matching key tree node paths
     */
    public List<String> getKeyTreeNodePaths() {
        List<String> paths = new ArrayList<String>(nodes.size());
        for (KeyTreeNode node : nodes) {
            paths.add(node.getMessageKey());
        }
        return paths;
    }

    
    /**
     * Gets the first item matched.
     * @return first item matched, or <code>null</code> if none was found
     */
    public KeyTreeNode getKeyTreeNode() {
        if (nodes.size() > 0) {
            return nodes.get(0);
        }
        return null;
    }
}
