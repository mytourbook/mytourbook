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

import java.beans.PropertyChangeListener;
import java.util.Locale;

/**
 * A single entry in a <code>MessagesBundle</code>.
 * 
 * @author Pascal Essiembre (pascal@essiembre.com)
 * @see MessagesBundle
 * @see MessagesBundleGroup
 */
public final class Message extends AbstractMessageModel {

    /** For serialisation. */
    private static final long serialVersionUID = 1160670351341655427L;

    public static final String PROPERTY_COMMENT = "comment"; //$NON-NLS-1$
    public static final String PROPERTY_ACTIVE = "active"; //$NON-NLS-1$
    public static final String PROPERTY_TEXT = "text"; //$NON-NLS-1$

    /** Entry unique identifier. */
    private final String key;
    /** Entry locale. */
    private final Locale locale;
    /** Entry comment. */
    private String comment;
    /** Whether this entry is commented out or not. */
    private boolean active = true;
    /** Entry text. */
    private String text;

    /**
     * Constructor.  Key and locale arguments are <code>null</code> safe.
     * @param key unique identifier within a messages bundle
     * @param locale the message locale
     */
    public Message(final String key, final Locale locale) {
        super();
        this.key = (key == null ? "" : key); //$NON-NLS-1$
        this.locale = locale;
    }

    
    
    /**
     * Sets the message comment.
     * @param comment The comment to set.
     */
    public void setComment(String comment) {
        Object oldValue = this.comment;
        this.comment = comment;
        firePropertyChange(PROPERTY_COMMENT, oldValue, comment);
    }

    /**
     * Sets whether the message is active or not.  An inactive message is
     * one that we continue to keep track of, but will not be picked
     * up by internationalisation mechanism (e.g. <code>ResourceBundle</code>).
     * Typically, those are commented (i.e. //) key/text pairs in a
     * *.properties file.
     * @param active The active to set.
     */
    public void setActive(boolean active) {
        boolean oldValue = this.active;
        this.active = active;
        firePropertyChange(PROPERTY_ACTIVE, oldValue, active);
    }

    /**
     * Sets the actual message text.
     * @param text The text to set.
     */
    public void setText(String text) {
        Object oldValue = this.text;
        this.text = text;
        firePropertyChange(PROPERTY_TEXT, oldValue, text);
    }

    /**
     * Gets the comment associated with this message (<code>null</code> if
     * no comments).
     * @return Returns the comment.
     */
    public String getComment() {
        return comment;
    }
    /**
     * Gets the message key attribute.
     * @return Returns the key.
     */
    public String getKey() {
        return key;
    }
        
    /**
     * Gets the message text.
     * @return Returns the text.
     */
    public String getValue() {
        return text;
    }

    /**
     * Gets the message locale.
     * @return Returns the locale
     */
    public Locale getLocale() {
        return locale;
    }
    
    /**
     * Gets whether this message is active or not.
     * @return <code>true</code> if this message is active.
     */
    public boolean isActive() {
        return active;
    }
    
    /**
     * Copies properties of the given message to this message.
     * The properties copied over are all properties but the 
     * message key and locale.
     * @param message
     */
    protected void copyFrom(Message message) {
        setComment(message.getComment());
        setActive(message.isActive());
        setText(message.getValue());
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof Message)) {
            return false;
        }
        Message entry = (Message) obj;
        return equals(key, entry.key)
            && equals(locale, entry.locale)
            && active == entry.active
            && equals(text, entry.text)
            && equals(comment, entry.comment);
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "Message=[[key=" + key  //$NON-NLS-1$
                + "][text=" + text //$NON-NLS-1$
                + "][comment=" + comment //$NON-NLS-1$
                + "][active=" + active //$NON-NLS-1$
                + "]]";  //$NON-NLS-1$
    }
    
    public final synchronized void addMessageListener(
            final PropertyChangeListener listener) {
        addPropertyChangeListener(listener);
    }
    public final synchronized void removeMessageListener(
            final PropertyChangeListener listener) {
        removePropertyChangeListener(listener);
    }
    public final synchronized PropertyChangeListener[] getMessageListeners() {
        return getPropertyChangeListeners();
    }
}
