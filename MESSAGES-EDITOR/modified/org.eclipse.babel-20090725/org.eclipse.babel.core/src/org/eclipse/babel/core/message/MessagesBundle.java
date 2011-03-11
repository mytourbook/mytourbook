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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.eclipse.babel.core.message.resource.IMessagesResource;
import org.eclipse.babel.core.message.resource.IMessagesResourceChangeListener;
import org.eclipse.babel.core.util.BabelUtils;

/**
 * For a given scope, all messages for a national language.  
 * @author Pascal Essiembre (pascal@essiembre.com)
 */
public class MessagesBundle extends AbstractMessageModel
		implements IMessagesResourceChangeListener {

    private static final long serialVersionUID = -331515196227475652L;

    public static final String PROPERTY_COMMENT = "comment"; //$NON-NLS-1$
    public static final String PROPERTY_MESSAGES_COUNT =
            "messagesCount"; //$NON-NLS-1$

    private static final IMessagesBundleListener[] EMPTY_MSG_BUNDLE_LISTENERS =
        new IMessagesBundleListener[] {};
    private final Collection<String> orderedKeys = new ArrayList<String>();
    private final Map<String, Message> keyedMessages = new HashMap<String, Message>();

    private final IMessagesResource resource;
    
    private final PropertyChangeListener messageListener =
        new PropertyChangeListener(){
                public void propertyChange(PropertyChangeEvent event) {
                    fireMessageChanged(event);
                }
    };
    private String comment;

    /**
     * Creates a new <code>MessagesBundle</code>.
     * @param resource the messages bundle resource
     */
    public MessagesBundle(IMessagesResource resource) {
        super();
        this.resource = resource;
        readFromResource();
        // Handle resource changes
        resource.addMessagesResourceChangeListener(
        		new IMessagesResourceChangeListener(){
            public void resourceChanged(IMessagesResource changedResource) {
                readFromResource();
            }
        });
        // Handle bundle changes
        addMessagesBundleListener(new MessagesBundleAdapter() {
            public void messageChanged(MessagesBundle messagesBundle,
                    PropertyChangeEvent changeEvent) {
                writetoResource();
            }
            public void propertyChange(PropertyChangeEvent evt) {
                writetoResource();
            }
        });
    }

    /**
     * Called before this object will be discarded.
     */
    public void dispose() {
    	
    }
    
    /**
     * Gets the underlying messages resource implementation.
     * @return
     */
    public IMessagesResource getResource() {
        return resource;
    }
    
    public int getMessagesCount() {
        return keyedMessages.size();
    }
    
    /**
     * Gets the locale for the messages bundle (<code>null</code> assumes
     * the default system locale).
     * @return Returns the locale.
     */
    public Locale getLocale() {
        return resource.getLocale();
    }

    /**
     * Gets the overall comment, or description, for this messages bundle..
     * @return Returns the comment.
     */
    public String getComment() {
        return comment;
    }

    /**
     * Sets the comment for this messages bundle.
     * @param comment The comment to set.
     */
    public void setComment(String comment) {
        Object oldValue = this.comment;
        this.comment = comment;
        firePropertyChange(PROPERTY_COMMENT, oldValue, comment);
    }

    /**
     * @see org.eclipse.babel.core.message.resource
     * 		.IMessagesResourceChangeListener#resourceChanged(
     * 				org.eclipse.babel.core.message.resource.IMessagesResource)
     */
    public void resourceChanged(IMessagesResource changedResource) {
        this.resource.deserialize(this);
    }

    /**
     * Adds a message to this messages bundle.  If the message already exists
     * its properties are updated and no new message is added.
     * @param message the message to add
     */
    public void addMessage(Message message) {
        int oldCount = getMessagesCount();
        if (!orderedKeys.contains(message.getKey())) {
            orderedKeys.add(message.getKey());
        }
        if (!keyedMessages.containsKey(message.getKey())) {
            keyedMessages.put(message.getKey(), message);
            message.addMessageListener(messageListener);
            firePropertyChange(
                    PROPERTY_MESSAGES_COUNT, oldCount, getMessagesCount());
            fireMessageAdded(message);
        } else {
            // Entry already exists, update it.
            Message matchingEntry = keyedMessages.get(message.getKey());
            matchingEntry.copyFrom(message);
        }
    }
    /**
     * Removes a message from this messages bundle.
     * @param messageKey the key of the message to remove
     */
    public void removeMessage(String messageKey) {
        int oldCount = getMessagesCount();
        orderedKeys.remove(messageKey);
        Message message = keyedMessages.get(messageKey);
        if (message != null) {
            message.removePropertyChangeListener(messageListener);
            keyedMessages.remove(messageKey);
            firePropertyChange(
                    PROPERTY_MESSAGES_COUNT, oldCount, getMessagesCount());
            fireMessageRemoved(message);
        }
    }
    /**
     * Removes messages from this messages bundle.
     * @param messageKeys the keys of the messages to remove
     */
    public void removeMessages(String[] messageKeys) {
        for (int i = 0; i < messageKeys.length; i++) {
            removeMessage(messageKeys[i]);
        }
    }

    /**
     * Renames a message key.
     * @param sourceKey the message key to rename
     * @param targetKey the new key for the message
     * @throws MessageException if the target key already exists
     */
    public void renameMessageKey(String sourceKey, String targetKey) {
        if (getMessage(targetKey) != null) {
            throw new MessageException(
            		"Cannot rename: target key already exists."); //$NON-NLS-1$
        }
        Message sourceEntry = getMessage(sourceKey);
        if (sourceEntry != null) {
            Message targetEntry = new Message(targetKey, getLocale());
            targetEntry.copyFrom(sourceEntry);
            removeMessage(sourceKey);
            addMessage(targetEntry);
        }
    }
    /**
     * Duplicates a message.
     * @param sourceKey the message key to duplicate
     * @param targetKey the new message key
     * @throws MessageException if the target key already exists
     */
    public void duplicateMessage(String sourceKey, String targetKey) {
        if (getMessage(sourceKey) != null) {
            throw new MessageException(
            	"Cannot duplicate: target key already exists."); //$NON-NLS-1$
        }
        Message sourceEntry = getMessage(sourceKey);
        if (sourceEntry != null) {
            Message targetEntry = new Message(targetKey, getLocale());
            targetEntry.copyFrom(sourceEntry);
            addMessage(targetEntry);
        }
    }

    /**
     * Gets a message.
     * @param key a message key
     * @return a message
     */
    public Message getMessage(String key) {
        return keyedMessages.get(key);
    }

    /**
     * Adds an empty message.
     * @param key the new message key
     */
    public void addMessage(String key) {
        addMessage(new Message(key, getLocale()));
    }

    /**
     * Gets all message keys making up this messages bundle.
     * @return message keys
     */
    public String[] getKeys() {
        return orderedKeys.toArray(BabelUtils.EMPTY_STRINGS);
    }
    
    /**
     * Obtains the set of <code>Message</code> objects in this bundle.
     * @return a collection of <code>Message</code> objects in this bundle
     */
    public Collection<Message> getMessages() {
        return keyedMessages.values();
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        String str = "MessagesBundle=[[locale=" + getLocale() //$NON-NLS-1$
                   + "][comment=" + comment //$NON-NLS-1$
                   + "][entries="; //$NON-NLS-1$
        for (Message message : getMessages()) {
            str += message.toString();
        }
        str += "]]"; //$NON-NLS-1$
        return str;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ((comment == null) ? 0 : comment.hashCode());
        result = PRIME * result + ((messageListener == null)
        		? 0 : messageListener.hashCode());
        result = PRIME * result + ((keyedMessages == null)
        		? 0 : keyedMessages.hashCode());
        result = PRIME * result + ((orderedKeys == null)
        		? 0 : orderedKeys.hashCode());
        result = PRIME * result + ((resource == null)
        		? 0 : resource.hashCode());
        return result;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof MessagesBundle)) {
            return false;
        }
        MessagesBundle messagesBundle = (MessagesBundle) obj;
        return equals(comment, messagesBundle.comment)
            && equals(keyedMessages, messagesBundle.keyedMessages);
    }    

    public final synchronized void addMessagesBundleListener(
            final IMessagesBundleListener listener) {
        addPropertyChangeListener(listener);
    }
    public final synchronized void removeMessagesBundleListener(
            final IMessagesBundleListener listener) {
        removePropertyChangeListener(listener);
    }
    public final synchronized IMessagesBundleListener[] 
                getMessagesBundleListeners() {
        //TODO find more efficient way to avoid class cast.
        return Arrays.asList(
                getPropertyChangeListeners()).toArray(
                        EMPTY_MSG_BUNDLE_LISTENERS);
    }

    
    private void readFromResource() {
        this.resource.deserialize(this);
    }
    private void writetoResource() {
        this.resource.serialize(this);
    }
    
    private void fireMessageAdded(Message message) {
        IMessagesBundleListener[] listeners = getMessagesBundleListeners();
        for (int i = 0; i < listeners.length; i++) {
            IMessagesBundleListener listener = listeners[i];
            listener.messageAdded(this, message);
        }
    }
    private void fireMessageRemoved(Message message) {
        IMessagesBundleListener[] listeners = getMessagesBundleListeners();
        for (int i = 0; i < listeners.length; i++) {
            IMessagesBundleListener listener = listeners[i];
            listener.messageRemoved(this, message);
        }
    }
    private void fireMessageChanged(PropertyChangeEvent event) {
        IMessagesBundleListener[] listeners = getMessagesBundleListeners();
        for (int i = 0; i < listeners.length; i++) {
            IMessagesBundleListener listener = listeners[i];
            listener.messageChanged(this, event);
        }
    }
}
