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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.babel.core.message.resource.IMessagesResource;
import org.eclipse.babel.core.message.strategy.IMessagesBundleGroupStrategy;
import org.eclipse.babel.core.util.BabelUtils;

/**
 * Grouping of all messages bundle of the same kind.
 * @author Pascal Essiembre (pascal@essiembre.com)
 */
public class MessagesBundleGroup extends AbstractMessageModel {

    private static final IMessagesBundleGroupListener[]
            EMPTY_GROUP_LISTENERS = new IMessagesBundleGroupListener[] {};
    private static final Message[] EMPTY_MESSAGES = new Message[] {};
    
    public static final String PROPERTY_MESSAGES_BUNDLE_COUNT =
            "messagesBundleCount"; //$NON-NLS-1$
    public static final String PROPERTY_KEY_COUNT =
        "keyCount"; //$NON-NLS-1$

    /** For serialization. */
    private static final long serialVersionUID = -1977849534191384324L;
    /** Bundles forming the group (key=Locale; value=MessagesBundle). */
    private final Map<Locale,MessagesBundle> localeBundles = new HashMap<Locale,MessagesBundle>();
    private final Set<String> keys = new TreeSet<String>(); 
    private final IMessagesBundleListener messagesBundleListener =
            new MessagesBundleListener();
    
    private final IMessagesBundleGroupStrategy groupStrategy;
    private static final Locale[] EMPTY_LOCALES = new Locale[] {};
    private final String name;
    
    /**
     * Creates a new messages bundle group.
     * @param groupStrategy a IMessagesBundleGroupStrategy instance
     */
    public MessagesBundleGroup(IMessagesBundleGroupStrategy groupStrategy) {
        super();
        this.groupStrategy = groupStrategy;
        this.name = groupStrategy.createMessagesBundleGroupName();
        MessagesBundle[] bundles = groupStrategy.loadMessagesBundles();
        if (bundles != null) {
            for (int i = 0; i < bundles.length; i++) {
                addMessagesBundle(bundles[i]);
            }
        }
    }
    
    /**
     * Called before this object will be discarded.
     * Disposes the underlying MessageBundles
     */
    public void dispose() {
    	for (MessagesBundle mb : getMessagesBundles()) {
    		try {
    			mb.dispose();
    		} catch (Throwable t) {
    			//FIXME: remove debug:
    			System.err.println("Error disposing message-bundle " +
    					mb.getResource().getResourceLocationLabel());
    			//disregard crashes: this is a best effort to dispose things.
    		}
    	}
    }

    
    /**
     * Gets the messages bundle matching given locale.
     * @param locale locale of bundle to retreive
     * @return a bundle
     */
    public MessagesBundle getMessagesBundle(Locale locale) {
        return localeBundles.get(locale);
    }

    /**
     * Gets the messages bundle matching given source object.  A source
     * object being a context-specific concrete underlying implementation of a
     * <code>MessagesBundle</code> as per defined in
	 * <code>IMessageResource</code>.
     * @param source the source object to match
     * @return a messages bundle
     * @see IMessagesResource
     */
    public MessagesBundle getMessagesBundle(Object source) {
    	for (MessagesBundle messagesBundle : getMessagesBundles()) {
            if (equals(source, messagesBundle.getResource().getSource())) {
                return messagesBundle;
            }
        }
        return null;
    }
    
    /**
     * Adds an empty <code>MessagesBundle</code> to this group for the
     * given locale.
     * @param locale locale for the new bundle added
     */
    public void addMessagesBundle(Locale locale) {
        addMessagesBundle(groupStrategy.createMessagesBundle(locale));
    }

    /**
     * Gets all locales making up this messages bundle group.
     */
    public Locale[] getLocales() {
        return localeBundles.keySet().toArray(EMPTY_LOCALES);
    }

    /**
     * Gets all messages associated with the given message key.
     * @param key a message key
     * @return messages
     */
    public Message[] getMessages(String key) {
        List<Message> messages = new ArrayList<Message>();
    	for (MessagesBundle messagesBundle : getMessagesBundles()) {
            Message message = messagesBundle.getMessage(key);
            if (message != null) {
                messages.add(message);
            }
        }
        return messages.toArray(EMPTY_MESSAGES);
    }

    /**
     * Gets the message matching given key and locale.
     * @param locale locale for which to retrieve the message
     * @param key key matching entry to retrieve the message
     * @return a message
     */
    public Message getMessage(String key, Locale locale) {
        MessagesBundle messagesBundle = getMessagesBundle(locale);
        if (messagesBundle != null) {
            return messagesBundle.getMessage(key);
        }
        return null;
    }

    
    /**
     * Adds a messages bundle to this group.
     * @param messagesBundle bundle to add
     * @throws MessageException if a messages bundle for the same locale
     *         already exists.
     */
    public void addMessagesBundle(MessagesBundle messagesBundle) {
        if (localeBundles.get(messagesBundle.getLocale()) != null) {
            throw new MessageException(
                 "A bundle with the same locale already exists."); //$NON-NLS-1$
        }
        
        int oldBundleCount = localeBundles.size();
        localeBundles.put(messagesBundle.getLocale(), messagesBundle);
        firePropertyChange(PROPERTY_MESSAGES_BUNDLE_COUNT,
                oldBundleCount, localeBundles.size());
        fireMessagesBundleAdded(messagesBundle);
        
        String[] bundleKeys = messagesBundle.getKeys();
        for (int i = 0; i < bundleKeys.length; i++) {
			String key = bundleKeys[i];
            if (!keys.contains(key)) {
                int oldKeyCount = keys.size();
                keys.add(key);
                firePropertyChange(
                        PROPERTY_KEY_COUNT, oldKeyCount, keys.size());
                fireKeyAdded(key);
            }
        }
        messagesBundle.addMessagesBundleListener(messagesBundleListener);
    }

    /**
     * Gets this messages bundle group name.
     * @return bundle group name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Adds an empty message to every messages bundle of this group with the
     * given.
     * @param key message key
     */
    public void addMessages(String key) {
        for (MessagesBundle msgBundle : localeBundles.values()) {
           	msgBundle.addMessage(key);
        }
    }

    /**
     * Renames a key in all messages bundles forming this group.
     * @param sourceKey the message key to rename
     * @param targetKey the new message name
     */
    public void renameMessageKeys(String sourceKey, String targetKey) {
        for (MessagesBundle msgBundle : localeBundles.values()) {
        	msgBundle.renameMessageKey(
            		sourceKey, targetKey);
        }
    }    

    /**
     * Removes messages matching the given key from all messages bundle.
     * @param key key of messages to remove
     */
    public void removeMessages(String key) {
        for (MessagesBundle msgBundle : localeBundles.values()) {
        	msgBundle.removeMessage(key);
        }
    }
    
    /**
     * Sets whether messages matching the <code>key</code> are active or not.
     * @param key key of messages
     */
    public void setMessagesActive(String key, boolean active) {
        for (MessagesBundle msgBundle : localeBundles.values()) {
            Message entry = msgBundle.getMessage(key);
            if (entry != null) {
                entry.setActive(active);
            }
        }
    }
    
    /**
     * Duplicates each messages matching the <code>sourceKey</code> to
     * the <code>newKey</code>.
     * @param sourceKey original key
     * @param targetKey new key
     * @throws MessageException if a target key already exists
     */
    public void duplicateMessages(String sourceKey, String targetKey) {
        if (sourceKey.equals(targetKey)) {
            return;
        }
        for (MessagesBundle msgBundle : localeBundles.values()) {
        	msgBundle.duplicateMessage(
            		sourceKey, targetKey);
        }
    }
    
    /**
     * Returns a collection of all bundles in this group.
     * @return the bundles in this group
     */
    public Collection<MessagesBundle> getMessagesBundles() {
        return localeBundles.values();
    }
    
    /**
     * Gets all keys from all messages bundles.
     * @return all keys from all messages bundles
     */
    public String[] getMessageKeys() {
        return keys.toArray(BabelUtils.EMPTY_STRINGS);
    }
    
    /**
     * Whether the given key is found in this messages bundle group.
     * @param key the key to find
     * @return <code>true</code> if the key exists in this bundle group.
     */
    public boolean isMessageKey(String key) {
        return keys.contains(key);
    }
    
    /**
     * Gets the number of messages bundles in this group.
     * @return the number of messages bundles in this group
     */
    public int getMessagesBundleCount() {
        return localeBundles.size();
    }
    
    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof MessagesBundleGroup)) {
            return false;
        }
        MessagesBundleGroup messagesBundleGroup = (MessagesBundleGroup) obj;
        return equals(localeBundles, messagesBundleGroup.localeBundles);
    }    
    
    public final synchronized void addMessagesBundleGroupListener(
            final IMessagesBundleGroupListener listener) {
        addPropertyChangeListener(listener);
    }
    public final synchronized void removeMessagesBundleGroupListener(
            final IMessagesBundleGroupListener listener) {
        removePropertyChangeListener(listener);
    }
    public final synchronized IMessagesBundleGroupListener[] 
                getMessagesBundleGroupListeners() {
        //TODO find more efficient way to avoid class cast.
        return Arrays.asList(
                getPropertyChangeListeners()).toArray(
                        EMPTY_GROUP_LISTENERS);
    }
    
    
    private void fireKeyAdded(String key) {
        IMessagesBundleGroupListener[] listeners =
                getMessagesBundleGroupListeners();
        for (int i = 0; i < listeners.length; i++) {
            IMessagesBundleGroupListener listener = listeners[i];
            listener.keyAdded(key);
        }
    }
    private void fireKeyRemoved(String key) {
        IMessagesBundleGroupListener[] listeners =
                getMessagesBundleGroupListeners();
        for (int i = 0; i < listeners.length; i++) {
            IMessagesBundleGroupListener listener = listeners[i];
            listener.keyRemoved(key);
        }
    }
    private void fireMessagesBundleAdded(MessagesBundle messagesBundle) {
        IMessagesBundleGroupListener[] listeners =
                getMessagesBundleGroupListeners();
        for (int i = 0; i < listeners.length; i++) {
            IMessagesBundleGroupListener listener = listeners[i];
            listener.messagesBundleAdded(messagesBundle);
        }
    }
    private void fireMessagesBundleRemoved(MessagesBundle messagesBundle) {
        IMessagesBundleGroupListener[] listeners =
                getMessagesBundleGroupListeners();
        for (int i = 0; i < listeners.length; i++) {
            IMessagesBundleGroupListener listener = listeners[i];
            listener.messagesBundleRemoved(messagesBundle);
        }
    }
    
    
    /**
     * Class listening for changes in underlying messages bundle and 
     * relays them to the listeners for MessagesBundleGroup.
     */
    private class MessagesBundleListener implements IMessagesBundleListener {
        public void messageAdded(MessagesBundle messagesBundle,
                Message message) {
            int oldCount = keys.size();
            IMessagesBundleGroupListener[] listeners =
                    getMessagesBundleGroupListeners();
            for (int i = 0; i < listeners.length; i++) {
                IMessagesBundleGroupListener listener = listeners[i];
                listener.messageAdded(messagesBundle, message);
                if (getMessages(message.getKey()).length == 1) {
                    keys.add(message.getKey());
                    firePropertyChange(
                            PROPERTY_KEY_COUNT, oldCount, keys.size());
                    fireKeyAdded(message.getKey());
                }
            }
        }
        public void messageRemoved(MessagesBundle messagesBundle,
                Message message) {
            int oldCount = keys.size();
            IMessagesBundleGroupListener[] listeners =
                    getMessagesBundleGroupListeners();
            for (int i = 0; i < listeners.length; i++) {
                IMessagesBundleGroupListener listener = listeners[i];
                listener.messageRemoved(messagesBundle, message);
                int keyMessagesCount = getMessages(message.getKey()).length;
                if (keyMessagesCount == 0 && keys.contains(message.getKey())) {
                    keys.remove(message.getKey());
                    firePropertyChange(
                            PROPERTY_KEY_COUNT, oldCount, keys.size());
                    fireKeyRemoved(message.getKey());
                }
            }
        }
        public void messageChanged(MessagesBundle messagesBundle,
                PropertyChangeEvent changeEvent) {
            IMessagesBundleGroupListener[] listeners =
                    getMessagesBundleGroupListeners();
            for (int i = 0; i < listeners.length; i++) {
                IMessagesBundleGroupListener listener = listeners[i];
                listener.messageChanged(messagesBundle, changeEvent);
            }
        }
        // MessagesBundle property changes:
        public void propertyChange(PropertyChangeEvent evt) {
            MessagesBundle bundle = (MessagesBundle) evt.getSource();
            IMessagesBundleGroupListener[] listeners =
                    getMessagesBundleGroupListeners();
            for (int i = 0; i < listeners.length; i++) {
                IMessagesBundleGroupListener listener = listeners[i];
                listener.messagesBundleChanged(bundle, evt);
            }
        }
    }
}
