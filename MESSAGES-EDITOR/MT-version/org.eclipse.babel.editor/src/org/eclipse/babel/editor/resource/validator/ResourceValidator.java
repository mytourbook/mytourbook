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

import org.eclipse.babel.core.message.MessagesBundle;
import org.eclipse.babel.core.message.MessagesBundleGroup;
import org.eclipse.core.resources.IFile;


/**
 * @author Pascal Essiembre
 *
 */
public class ResourceValidator {
    
//    public static void validate(
//            IFile file, IValidationMarkerStrategy markerStrategy) {
//        //TODO check if there is a matching EclipsePropertiesEditorResource already open.
//        //else, create MessagesBundle from PropertiesIFileResource
//        
//        //TODO use BundleGroupRegistry (LRUMap)
//        MessagesBundleGroup messagesBundleGroup = MessagesBundleGroupFactory.createBundleGroup(
//                null, file);
//        validate(file, markerStrategy, messagesBundleGroup);
//    }
    
    /**
     * @param file
     * @param markerStrategy
     * @param alreadyBuiltMessageBundle
     */
    public static void validate(
            IFile file, IValidationMarkerStrategy markerStrategy,
            MessagesBundleGroup alreadyBuiltMessageBundle/*,
            Indexer indexer*/) {
        //TODO check if there is a matching EclipsePropertiesEditorResource already open.
        //else, create MessagesBundle from PropertiesIFileResource
    	MessagesBundle messagesBundle = alreadyBuiltMessageBundle.getMessagesBundle(file);
        if (messagesBundle == null) {
            return;
        }
        Locale locale = messagesBundle.getLocale();
        MessagesBundleGroupValidator.validate(alreadyBuiltMessageBundle, locale, markerStrategy/*, indexer*/);
    }
    

    
}
