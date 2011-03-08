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
package org.eclipse.babel.core.message.strategy;

import java.util.Locale;

import org.eclipse.babel.core.message.MessageException;
import org.eclipse.babel.core.message.MessagesBundle;
import org.eclipse.babel.core.message.resource.IMessagesResource;



/**
 * This class holds the algorithms required to abstract the internal nature
 * of a <code>MessagesBundleGroup</code>. 
 * @author Pascal Essiembre (pascal@essiembre.com)
 * @see IMessagesResource
 */
public interface IMessagesBundleGroupStrategy {
    //TODO think of a better name for this interface?
    
    /**
     * Creates a name that attempts to uniquely identifies a messages bundle
     * group.  It is not a strict requirement that the name be unique,
     * but doing facilitates users interaction with a message bundle group
     * in a given user-facing implementation.<P>
     * This method is called at construction time of a
     * <code>MessagesBundleGroup</code>. 
     * @return messages bundle group name
     */
    String createMessagesBundleGroupName();
    
    /**
     * Load all bundles making up a messages bundle group from the underlying
     * source.
     * This method is called at construction time of a
     * <code>MessagesBundleGroup</code>. 
     * @return all bundles making a bundle group
     * @throws MessageException problem loading bundles
     */
    MessagesBundle[] loadMessagesBundles() throws MessageException;
    
    /**
     * Creates a new bundle for the given <code>Locale</code>.  If the 
     * <code>Locale</code> is <code>null</code>, the default system
     * <code>Locale</code> is assumed.
     * @param locale locale for which to create the messages bundle
     * @return a new messages bundle
     * @throws MessageException problem creating a new messages bundle
     */
    MessagesBundle createMessagesBundle(Locale locale) throws MessageException;
        
}
