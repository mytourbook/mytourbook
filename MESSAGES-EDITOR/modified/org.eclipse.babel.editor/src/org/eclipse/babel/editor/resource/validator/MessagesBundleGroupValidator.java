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
import org.eclipse.babel.core.message.checks.DuplicateValueCheck;
import org.eclipse.babel.core.message.checks.MissingValueCheck;
import org.eclipse.babel.editor.preferences.MsgEditorPreferences;


/**
 * @author Pascal Essiembre
 *
 */
public class MessagesBundleGroupValidator {    
    
    //TODO Re-think... ??

    public static void validate(
            MessagesBundleGroup messagesBundleGroup,
            Locale locale,
            IValidationMarkerStrategy markerStrategy) {
        //TODO check if there is a matching EclipsePropertiesEditorResource already open.
        //else, create MessagesBundle from PropertiesIFileResource
        
    	DuplicateValueCheck duplicateCheck =
    		MsgEditorPreferences.getInstance().getReportDuplicateValues()
	    		? new DuplicateValueCheck()
	    		: null;
    	String[] keys = messagesBundleGroup.getMessageKeys();
    	for (int i = 0; i < keys.length; i++) {
			String key = keys[i];
            if (MsgEditorPreferences.getInstance().getReportMissingValues()) {
	            if (MissingValueCheck.MISSING_KEY.checkKey(
	                    messagesBundleGroup,
	                    messagesBundleGroup.getMessage(key, locale))) {
	                markerStrategy.markFailed(new ValidationFailureEvent(
	                        messagesBundleGroup, locale, key,
	                        MissingValueCheck.MISSING_KEY));
	            }
            }
            if (duplicateCheck != null) {
            	if (!MsgEditorPreferences.getInstance().getReportDuplicateValuesOnlyInRootLocales()
            			|| (locale == null || locale.toString().length() == 0)) {
            		//either the locale is the root locale either
            		//we report duplicated on all the locales anyways.
		            if (duplicateCheck.checkKey(
		                    messagesBundleGroup,
		                    messagesBundleGroup.getMessage(key, locale))) {
		                markerStrategy.markFailed(new ValidationFailureEvent(
		                        messagesBundleGroup, locale, key, duplicateCheck));
		            }
	            	duplicateCheck.reset();
            	}
            }
        }
    }
    
}
