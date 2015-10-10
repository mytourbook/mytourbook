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
import java.util.Collection;

import org.eclipse.babel.core.message.MessagesBundleGroup;
import org.eclipse.babel.core.message.tree.KeyTreeNode;


/**
 * Visitor for going to a tree (or tree branch), and aggregating information
 * about executed checks.
 * @author Pascal Essiembre (pascal@essiembre.com)
 */
public class KeyCheckVisitor implements IKeyTreeVisitor {

    private static final KeyTreeNode[] EMPTY_NODES = new KeyTreeNode[] {};
    
    private IKeyCheck keyCheck;
    private final MessagesBundleGroup messagesBundleGroup;
    
    private final Collection<KeyTreeNode> passedNodes = new ArrayList<KeyTreeNode>();
    private final Collection<KeyTreeNode> failedNodes = new ArrayList<KeyTreeNode>();
    
    /**
     * Constructor.
     */
    public KeyCheckVisitor(
    		MessagesBundleGroup messagesBundleGroup, IKeyCheck keyCheck) {
        super();
        this.keyCheck = keyCheck;
        this.messagesBundleGroup = messagesBundleGroup;
    }
    /**
     * Constructor.
     */
    public KeyCheckVisitor(MessagesBundleGroup messagesBundleGroup) {
        super();
        this.messagesBundleGroup = messagesBundleGroup;
    }
    
    /**
     * @see org.eclipse.babel.core.message.tree.visitor.IKeyTreeVisitor
     *      #visitKeyTreeNode(
     *              org.eclipse.babel.core.message.tree.KeyTreeNode)
     */
    public void visitKeyTreeNode(KeyTreeNode node) {
        if (keyCheck == null) {
            return;
        }
        if (keyCheck.checkKey(messagesBundleGroup, node.getMessageKey())) {
            passedNodes.add(node);
        } else {
            failedNodes.add(node);
        }
    }

    /**
     * Gets all nodes that returned true upon invoking a {@link IKeyCheck}.
     * @return all successful nodes
     */
    public KeyTreeNode[] getPassedNodes() {
        return passedNodes.toArray(EMPTY_NODES);
    }
    /**
     * Gets all nodes that returned false upon invoking a {@link IKeyCheck}.
     * @return all failing nodes
     */
    public KeyTreeNode[] getFailedNodes() {
        return failedNodes.toArray(EMPTY_NODES);
    }
    
    /**
     * Resets all passed and failed nodes.
     */
    public void reset() {
        passedNodes.clear();
        failedNodes.clear();
    }

    /**
     * Sets the key check for this visitor.
     * @param newKeyCheck new key check
     * @param reset whether to reset the passed and failed nodes.
     */
    public void setKeyCheck(IKeyCheck newKeyCheck, boolean reset) {
        if (reset) {
            reset();
        }
        this.keyCheck = newKeyCheck;
    }
}
