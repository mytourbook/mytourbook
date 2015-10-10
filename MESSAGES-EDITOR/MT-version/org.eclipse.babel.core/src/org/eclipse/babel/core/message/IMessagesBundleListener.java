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
package org.eclipse.babel.core.message;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * A listener for changes on a {@link MessagesBundle}.
 * @author Pascal Essiembre (pascal@essiembre.com)
 * @see MessagesBundle
 */
public interface IMessagesBundleListener extends PropertyChangeListener {
    /**
     * A message was added.
     * @param messagesBundle the messages bundle on which the message was added.
     * @param message the message
     */
    void messageAdded(MessagesBundle messagesBundle, Message message);
    /**
     * A message was removed.
     * @param messagesBundle the messages bundle on which the message was
     *                       removed.
     * @param message the message
     */
    void messageRemoved(MessagesBundle messagesBundle, Message message);
    /**
     * A message was changed.
     * @param messagesBundle the messages bundle on which the message was
     *                       changed.
     * @param message the message
     */
    void messageChanged(
            MessagesBundle messagesBundle, PropertyChangeEvent changeEvent);
}
