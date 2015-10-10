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
package org.eclipse.babel.core.message.resource;

import java.util.Locale;

import org.eclipse.babel.core.message.MessagesBundle;

/**
 * Class abstracting the underlying native storage mechanism for persisting
 * internationalised text messages.  
 * @author Pascal Essiembre
 */
public interface IMessagesResource {

	/**
	 * Gets the resource locale.
	 * @return locale
	 */
    Locale getLocale();
    /**
     * Gets the underlying object abstracted by this resource (e.g. a File).
     * @return source object
     */
    Object getSource();
    /**
     * Serializes a {@link MessagesBundle} instance to its native format.
     * @param messagesBundle the MessagesBundle to serialize
     */
    void serialize(MessagesBundle messagesBundle);
    /**
     * Deserializes a {@link MessagesBundle} instance from its native format.
     * @param messagesBundle the MessagesBundle to deserialize
     */
    void deserialize(MessagesBundle messagesBundle);
    /**
     * Adds a messages resource listener.  Implementors are required to notify
     * listeners of changes within the native implementation.
     * @param listener the listener
     */
    void addMessagesResourceChangeListener(
    		IMessagesResourceChangeListener listener);
    /**
     * Removes a messages resource listener.
     * @param listener the listener
     */
    void removeMessagesResourceChangeListener(
    		IMessagesResourceChangeListener listener);
    /**
     * @return The resource location label. or null if unknown.
     */
    String getResourceLocationLabel();
    
    /**
     * Called when the group it belongs to is disposed.
     */
    void dispose();
    
}
