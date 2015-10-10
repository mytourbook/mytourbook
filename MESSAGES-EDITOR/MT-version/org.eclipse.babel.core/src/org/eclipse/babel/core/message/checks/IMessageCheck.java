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
package org.eclipse.babel.core.message.checks;

import org.eclipse.babel.core.message.Message;
import org.eclipse.babel.core.message.MessagesBundleGroup;

/**
 * All purpose {@link Message} testing.   Use this interface to establish 
 * whether a message within a {@link MessagesBundleGroup} is answering
 * successfully to any condition. 
 * @author Pascal Essiembre (pascal@essiembre.com)
 */
public interface IMessageCheck {

	/**
	 * Checks whether a {@link Message} meets the implemented condition.
	 * @param messagesBundleGroup messages bundle group
	 * @param message the message being tested
	 * @return <code>true</code> if condition is successfully tested
	 */
    boolean checkKey(MessagesBundleGroup messagesBundleGroup, Message message);
}
