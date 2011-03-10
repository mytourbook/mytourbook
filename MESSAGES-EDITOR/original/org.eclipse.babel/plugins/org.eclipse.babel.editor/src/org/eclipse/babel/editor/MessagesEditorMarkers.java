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
package org.eclipse.babel.editor;

import java.beans.PropertyChangeEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Observable;

import org.eclipse.babel.core.message.MessagesBundle;
import org.eclipse.babel.core.message.MessagesBundleGroup;
import org.eclipse.babel.core.message.MessagesBundleGroupAdapter;
import org.eclipse.babel.core.message.checks.IMessageCheck;
import org.eclipse.babel.core.message.checks.MissingValueCheck;
import org.eclipse.babel.editor.resource.validator.IValidationMarkerStrategy;
import org.eclipse.babel.editor.resource.validator.MessagesBundleGroupValidator;
import org.eclipse.babel.editor.resource.validator.ValidationFailureEvent;
import org.eclipse.babel.editor.util.UIUtils;


/**
 * @author Pascal Essiembre
 *
 */
public class MessagesEditorMarkers
        extends Observable implements IValidationMarkerStrategy {

//    private final Collection validationEvents = new ArrayList();
    private final MessagesBundleGroup messagesBundleGroup;
    
   // private Map<String,Set<ValidationFailureEvent>> markersIndex = new HashMap();
    /** index is the name of the key. value is the collection of markers on that key */
    private Map<String, Collection<IMessageCheck>> markersIndex = new HashMap<String, Collection<IMessageCheck>>();
    /** index is the concat of the locale and the key.
     * value is the collection of markers for that key and that locale */
    private Map<String, Collection<IMessageCheck>> localizedMarkersIndex = new HashMap<String, Collection<IMessageCheck>>();
    
    /**
     * @param messagesBundleGroup
     */
    public MessagesEditorMarkers(final MessagesBundleGroup messagesBundleGroup) {
        super();
        this.messagesBundleGroup = messagesBundleGroup;
        validate();
        messagesBundleGroup.addMessagesBundleGroupListener(
                new MessagesBundleGroupAdapter() {
            public void messageChanged(MessagesBundle messagesBundle,
                    PropertyChangeEvent changeEvent) {
                resetMarkers();
            }
            public void messagesBundleChanged(
                    MessagesBundle messagesBundle,
                    PropertyChangeEvent changeEvent) {
                resetMarkers();
            }
            public void propertyChange(PropertyChangeEvent evt) {
                resetMarkers();
            }
            private void resetMarkers() {
                clear();
                validate();
            }
        });
    }
    
    private String hash(Locale locale, String key) {
    	//the '=' is hack to make sure no local=key can ever conflict
    	//with another local=key: in other words
        //it makes a hash of the combination (key+locale).
    	if (locale == null) {
    		locale = UIUtils.ROOT_LOCALE;
    	}
    	return locale + "=" + key;
    }


    /**
     * @see org.eclipse.babel.editor.resource.validator.IValidationMarkerStrategy#markFailed(org.eclipse.core.resources.IResource, org.eclipse.babel.core.bundle.checks.IBundleEntryCheck)
     */
    public void markFailed(ValidationFailureEvent event) {
        Collection<IMessageCheck> markersForKey = markersIndex.get(event.getKey());
        if (markersForKey == null) {
        	markersForKey = new HashSet<IMessageCheck>();
        	markersIndex.put(event.getKey(), markersForKey);
        }
        markersForKey.add(event.getCheck());
        
        String localizedKey = hash(event.getLocale(), event.getKey());
        markersForKey = localizedMarkersIndex.get(localizedKey);
        if (markersForKey == null) {
        	markersForKey = new HashSet<IMessageCheck>();
        	localizedMarkersIndex.put(localizedKey, markersForKey);
        }
        markersForKey.add(event.getCheck());
        
        //System.out.println("CREATE EDITOR MARKER");
        setChanged();
        notifyObservers(this);
    }
    
    public void clear() {
        markersIndex.clear();
        localizedMarkersIndex.clear();
        setChanged();
        notifyObservers(this);
    }

    public boolean isMarked(String key)  {
    	return markersIndex.containsKey(key);
    }

    public Collection<IMessageCheck> getFailedChecks(String key) {
    	return markersIndex.get(key);
    }
    public Collection<IMessageCheck> getFailedChecks(final String key, final Locale locale) {
    	return localizedMarkersIndex.get(hash(locale, key));
    }
        
    private void validate() {
        //TODO in a UI thread
        MessagesBundleGroupValidator.validate(messagesBundleGroup, this);
    }
    
    /**
     * @param key
     * @return true when the key has a missing or unused issue
     */
    public boolean isMissingOrUnusedKey(String key) {
    	Collection<IMessageCheck> markers = getFailedChecks(key);
    	return markers != null && markersContainMissing(markers);
    }
    
    /**
     * @param key
     * @param isMissingOrUnused true when it has been assesed already
     * that it is missing or unused
     * @return true when the key is unused
     */
    public boolean isUnusedKey(String key, boolean isMissingOrUnused) {
    	if (!isMissingOrUnused) {
    		return false;
    	}
    	Collection<IMessageCheck> markers = getFailedChecks(key, UIUtils.ROOT_LOCALE);
    	//if we get a missing on the root locale, it means the
    	//that some localized resources are referring to a key that is not in
    	//the default locale anymore: in other words, assuming the
    	//the code is up to date with the default properties
    	//file, the key is now unused.
    	return markers != null && markersContainMissing(markers);
    }
    
    private boolean markersContainMissing(Collection<IMessageCheck> markers) {
    	for (IMessageCheck marker : markers) {
    		if (marker == MissingValueCheck.MISSING_KEY) {
    			return true;
    		}
    	}
    	return false;
    }
    
}
