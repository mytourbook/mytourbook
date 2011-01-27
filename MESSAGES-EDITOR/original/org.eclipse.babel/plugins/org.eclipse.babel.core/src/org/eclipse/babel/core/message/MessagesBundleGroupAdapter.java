package org.eclipse.babel.core.message;

import java.beans.PropertyChangeEvent;

/**
 * An adapter class for a {@link IMessagesBundleGroupListener}.  Methods 
 * implementation do nothing.
 * @author Pascal Essiembre (pascal@essiembre.com)
 */
public class MessagesBundleGroupAdapter
        implements IMessagesBundleGroupListener {
    /**
     * @see org.eclipse.babel.core.message.IMessagesBundleGroupListener#
     *              keyAdded(java.lang.String)
     */
    public void keyAdded(String key) {
        // do nothing
    }
    /**
     * @see org.eclipse.babel.core.message.IMessagesBundleGroupListener#
     *              keyRemoved(java.lang.String)
     */
    public void keyRemoved(String key) {
        // do nothing
    }
    /**
     * @see org.eclipse.babel.core.message.IMessagesBundleGroupListener#
     *      messagesBundleAdded(org.eclipse.babel.core.message.MessagesBundle)
     */
    public void messagesBundleAdded(MessagesBundle messagesBundle) {
        // do nothing
    }
    /**
     * @see org.eclipse.babel.core.message.IMessagesBundleGroupListener#
     *      messagesBundleChanged(org.eclipse.babel.core.message.MessagesBundle,
     *                            java.beans.PropertyChangeEvent)
     */
    public void messagesBundleChanged(MessagesBundle messagesBundle,
            PropertyChangeEvent changeEvent) {
        // do nothing
    }
    /**
     * @see org.eclipse.babel.core.message.IMessagesBundleGroupListener
     *     #messagesBundleRemoved(org.eclipse.babel.core.message.MessagesBundle)
     */
    public void messagesBundleRemoved(MessagesBundle messagesBundle) {
        // do nothing
    }
    /**
     * @see org.eclipse.babel.core.message.IMessagesBundleListener#messageAdded(
     *              org.eclipse.babel.core.message.MessagesBundle,
     *              org.eclipse.babel.core.message.Message)
     */
    public void messageAdded(MessagesBundle messagesBundle, Message message) {
        // do nothing
    }
    /**
     * @see org.eclipse.babel.core.message.IMessagesBundleListener#
     *      messageChanged(org.eclipse.babel.core.message.MessagesBundle,
     *                     java.beans.PropertyChangeEvent)
     */
    public void messageChanged(MessagesBundle messagesBundle,
            PropertyChangeEvent changeEvent) {
        // do nothing
    }
    /**
     * @see org.eclipse.babel.core.message.IMessagesBundleListener#
     *      messageRemoved(org.eclipse.babel.core.message.MessagesBundle,
     *                     org.eclipse.babel.core.message.Message)
     */
    public void messageRemoved(MessagesBundle messagesBundle, Message message) {
        // do nothing
    }
    /**
     * @see java.beans.PropertyChangeListener#propertyChange(
     *           java.beans.PropertyChangeEvent)
     */
    public void propertyChange(PropertyChangeEvent evt) {
        // do nothing
    }
}
