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

import org.eclipse.babel.core.message.tree.KeyTreeNode;

/**
 * Objects implementing this interface can act as a visitor to a
 * <code>IKeyTreeModel</code>.
 * @author Pascal Essiembre (pascal@essiembre.com)
 */
public interface IKeyTreeVisitor {
    /**
     * Visits a key tree node.
     * @param item key tree node to visit
     */
	void visitKeyTreeNode(KeyTreeNode node);
}
