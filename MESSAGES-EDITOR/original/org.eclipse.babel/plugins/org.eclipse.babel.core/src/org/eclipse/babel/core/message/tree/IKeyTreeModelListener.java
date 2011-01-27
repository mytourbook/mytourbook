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

/**
 * Listener notified of changes to a {@link IKeyTreeModel}.
 * @author Pascal Essiembre
 */
public interface IKeyTreeModelListener {

	/**
	 * Invoked when a key tree node is added.
	 * @param node key tree node
	 */
    void nodeAdded(KeyTreeNode node);
	/**
	 * Invoked when a key tree node is remove.
	 * @param node key tree node
	 */
    void nodeRemoved(KeyTreeNode node);
}
