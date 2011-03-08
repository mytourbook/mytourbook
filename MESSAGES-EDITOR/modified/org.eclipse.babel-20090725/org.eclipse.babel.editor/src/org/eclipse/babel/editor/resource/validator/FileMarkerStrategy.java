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

import org.eclipse.babel.core.message.checks.DuplicateValueCheck;
import org.eclipse.babel.core.message.checks.MissingValueCheck;
import org.eclipse.babel.core.util.BabelUtils;
import org.eclipse.babel.editor.plugin.MessagesEditorPlugin;
import org.eclipse.babel.editor.preferences.MsgEditorPreferences;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

/**
 * @author Pascal Essiembre
 *
 */
public class FileMarkerStrategy implements IValidationMarkerStrategy {

    
    /**
     * @see org.eclipse.babel.editor.resource.validator.IValidationMarkerStrategy#markFailed(org.eclipse.core.resources.IResource, org.eclipse.babel.core.bundle.checks.IBundleEntryCheck)
     */
    public void markFailed(ValidationFailureEvent event) {
        if (event.getCheck() instanceof MissingValueCheck) {
            addMarker((IResource) event.getBundleGroup().getMessagesBundle(
                    event.getLocale()).getResource().getSource(),
//            addMarker(event.getResource(),
                    event.getKey(),
                    "Key \"" + event.getKey() //$NON-NLS-1$
                    + "\" is missing a value.", //$NON-NLS-1$
                 getSeverity(MsgEditorPreferences.getInstance()
                		 .getReportMissingValuesLevel()));
            
        } else if (event.getCheck() instanceof DuplicateValueCheck) {
            String duplicates = BabelUtils.join(
                    ((DuplicateValueCheck) event.getCheck())
                            .getDuplicateKeys(), ", ");
            addMarker((IResource) event.getBundleGroup().getMessagesBundle(
                    event.getLocale()).getResource().getSource(),
//            addMarker(event.getResource(),
                    event.getKey(),
                    "Key \"" + event.getKey() //$NON-NLS-1$
                          + "\" duplicates " + duplicates, //$NON-NLS-1$
                  getSeverity(MsgEditorPreferences.getInstance()
                         		 .getReportDuplicateValuesLevel()));
        }
    }

    private void addMarker(
            IResource resource, 
            String key,
            String message, //int lineNumber,
            int severity) {
        try {
            //TODO move MARKER_TYPE elsewhere.
            IMarker marker = resource.createMarker(MessagesEditorPlugin.MARKER_TYPE);
            marker.setAttribute(IMarker.MESSAGE, message);
            marker.setAttribute(IMarker.SEVERITY, severity);
            marker.setAttribute(IMarker.LOCATION, key);
//            if (lineNumber == -1) {
//                lineNumber = 1;
//            }
//            marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
        } catch (CoreException e) {
            throw new RuntimeException("Cannot add marker.", e); //$NON-NLS-1$
        }
    }

    /**
     * Translates the validation level as defined in 
     * MsgEditorPreferences.VALIDATION_MESSAGE_* to the corresponding value
     * for the marker attribute IMarke.SEVERITY.
     * @param msgValidationLevel
     * @return The value for the marker attribute IMarker.SEVERITY.
     */
    private static int getSeverity(int msgValidationLevel) {
    	switch (msgValidationLevel) {
    	case MsgEditorPreferences.VALIDATION_MESSAGE_ERROR:
    		return IMarker.SEVERITY_ERROR;
    	case MsgEditorPreferences.VALIDATION_MESSAGE_WARNING:
    		return IMarker.SEVERITY_WARNING;
    	case MsgEditorPreferences.VALIDATION_MESSAGE_INFO:
    		return IMarker.SEVERITY_INFO;
    	case MsgEditorPreferences.VALIDATION_MESSAGE_IGNORE:
    	default:
    		return IMarker.SEVERITY_INFO;//why are we here?
    	}
    }
    
}
