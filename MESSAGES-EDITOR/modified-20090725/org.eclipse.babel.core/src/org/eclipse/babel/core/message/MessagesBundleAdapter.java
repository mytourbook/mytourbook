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

/**
 * An adapter class for a {@link IMessagesBundleListener}.  Methods 
 * implementation do nothing.
 * @author Pascal Essiembre (pascal@essiembre.com)
 */
public class MessagesBundleAdapter implements IMessagesBundleListener {

    /**
     * @see org.eclipse.babel.core.message.IMessagesBundleListener#messageAdded(
     *              org.eclipse.babel.core.message.MessagesBundle,
     *              org.eclipse.babel.core.message.Message)
     */
    public void messageAdded(MessagesBundle messagesBundle, Message message) {
        // do nothing
    }
    /**
     * @see org.eclipse.babel.core.message.IMessagesBundleListener
     *      #messageChanged(org.eclipse.babel.core.message.MessagesBundle,
     *                      java.beans.PropertyChangeEvent)
     */
    public void messageChanged(MessagesBundle messagesBundle,
            PropertyChangeEvent changeEvent) {
        // do nothing
    }
    /**
     * @see org.eclipse.babel.core.message.IMessagesBundleListener
     *      #messageRemoved(org.eclipse.babel.core.message.MessagesBundle,
     *                      org.eclipse.babel.core.message.Message)
     */
    public void messageRemoved(MessagesBundle messagesBundle, Message message) {
        // do nothing
    }
    /**
     * @see java.beans.PropertyChangeListener#propertyChange(
     *              java.beans.PropertyChangeEvent)
     */
    public void propertyChange(PropertyChangeEvent evt) {
        // do nothing
    }
}
