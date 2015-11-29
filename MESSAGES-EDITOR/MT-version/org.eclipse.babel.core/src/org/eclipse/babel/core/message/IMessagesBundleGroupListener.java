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
 * A listener for changes on a {@link MessagesBundleGroup}.
 * @author Pascal Essiembre (pascal@essiembre.com)
 * @see MessagesBundleGroup
 */
public interface IMessagesBundleGroupListener extends IMessagesBundleListener {

    /**
     * A message key has been added.
     * @param key the added key
     */
    void keyAdded(String key);
    /**
     * A message key has been removed.
     * @param key the removed key
     */
    void keyRemoved(String key);
    /**
     * A messages bundle has been added.
     * @param messagesBundle the messages bundle
     */
    void messagesBundleAdded(MessagesBundle messagesBundle);
    /**
     * A messages bundle has been removed.
     * @param messagesBundle the messages bundle
     */
    void messagesBundleRemoved(MessagesBundle messagesBundle);
    /**
     * A messages bundle was modified.
     * @param messagesBundle the messages bundle
     */
    void messagesBundleChanged(
            MessagesBundle messagesBundle, PropertyChangeEvent changeEvent);

}
