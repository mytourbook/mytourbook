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
package org.eclipse.babel.editor.resource.validator;

import java.util.Locale;

import org.eclipse.babel.core.message.MessagesBundleGroup;
import org.eclipse.babel.core.message.checks.IMessageCheck;


/**
 * @author Pascal Essiembre
 *
 */
public class ValidationFailureEvent {

    private final MessagesBundleGroup messagesBundleGroup;
    private final Locale locale;
    private final String key;
//    private final IResource resource;
    private final IMessageCheck check;
    /**
     * @param messagesBundleGroup
     * @param locale
     * @param key
     * @param resource
     * @param check not null
     */
    /*default*/ ValidationFailureEvent(
            final MessagesBundleGroup messagesBundleGroup,
            final Locale locale,
            final String key,
//            final IResource resource,
            final IMessageCheck check) {
        super();
        this.messagesBundleGroup = messagesBundleGroup;
        this.locale = locale;
        this.key = key;
//        this.resource = resource;
        this.check = check;
    }
    /**
     * @return the messagesBundleGroup
     */
    public MessagesBundleGroup getBundleGroup() {
        return messagesBundleGroup;
    }
    /**
     * @return the check, never null
     */
    public IMessageCheck getCheck() {
        return check;
    }
    /**
     * @return the key
     */
    public String getKey() {
        return key;
    }
    /**
     * @return the locale
     */
    public Locale getLocale() {
        return locale;
    }

    
    
}
