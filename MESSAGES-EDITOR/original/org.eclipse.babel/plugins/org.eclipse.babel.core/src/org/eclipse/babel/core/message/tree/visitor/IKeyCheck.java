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

import org.eclipse.babel.core.message.MessagesBundleGroup;

/**
 * All purpose key testing.   Use this interface to establish whether
 * a message key within a {@link MessagesBundleGroup} is answering
 * successfully to any condition. 
 * @author Pascal Essiembre
 */
public interface IKeyCheck {

	/**
	 * Checks whether a key meets the implemented condition.
	 * @param messagesBundleGroup messages bundle group
	 * @param key message key to test
	 * @return <code>true</code> if condition is successfully tested
	 */
    boolean checkKey(MessagesBundleGroup messagesBundleGroup, String key);
}
