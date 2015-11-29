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
package org.eclipse.babel.editor.plugin;

import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;

import org.eclipse.babel.core.message.AbstractIFileChangeListener;
import org.eclipse.babel.core.message.AbstractIFileChangeListener.IFileChangeListenerRegistry;
import org.eclipse.babel.editor.builder.ToggleNatureAction;
import org.eclipse.babel.editor.preferences.MsgEditorPreferences;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.pde.nls.internal.ui.model.ResourceBundleModel;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 * @author Pascal Essiembre (pascal@essiembre.com)
 */
public class MessagesEditorPlugin extends AbstractUIPlugin implements IFileChangeListenerRegistry {

	//TODO move somewhere more appropriate
    public static final String MARKER_TYPE =
        "org.eclipse.babel.editor.nlsproblem"; //$NON-NLS-1$
	
	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.babel.editor";

	// The shared instance
	private static MessagesEditorPlugin plugin;
	
	//Resource bundle.
	//TODO Use Eclipse MessagesBundle instead.
	private ResourceBundle resourceBundle;
	
	//The resource change litener for the entire plugin.
	//objects interested in changes in the workspace resources must
	//subscribe to this listener by calling subscribe/unsubscribe on the plugin.
	private IResourceChangeListener resourceChangeListener;
	
	//The map of resource change subscribers.
	//The key is the full path of the resource listened. The value as set of SimpleResourceChangeListners
	//private Map<String,Set<SimpleResourceChangeListners>> resourceChangeSubscribers;
	private Map<String,Set<AbstractIFileChangeListener>> resourceChangeSubscribers;
	
	private ResourceBundleModel model;

	/**
	 * The constructor
	 */
	public MessagesEditorPlugin() {
		resourceChangeSubscribers = new HashMap<String,Set<AbstractIFileChangeListener>>();
	}

	/**
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(
	 *         org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		
		//make sure the rbe nature and builder are set on java projects
		//if that is what the users prefers.
		if (MsgEditorPreferences.getInstance().isBuilderSetupAutomatically()) {
			ToggleNatureAction.addOrRemoveNatureOnAllJavaProjects(true);
		}

		//TODO replace deprecated
        try {
            URL messagesUrl = FileLocator.find(getBundle(),
                    new Path("$nl$/messages.properties"), null);//$NON-NLS-1$
            if(messagesUrl != null) {
                resourceBundle = new PropertyResourceBundle(
                        messagesUrl.openStream());
            }
        } catch (IOException x) {
            resourceBundle = null;
        }

        //the unique file change listener
        resourceChangeListener = new IResourceChangeListener() {
        	public void resourceChanged(IResourceChangeEvent event) {
        		IResource resource = event.getResource();
        		if (resource != null) {
        			String fullpath = resource.getFullPath().toString();
        			Set<AbstractIFileChangeListener> listeners = resourceChangeSubscribers.get(fullpath);
        			if (listeners != null) {
        				AbstractIFileChangeListener[] larray = listeners.toArray(new AbstractIFileChangeListener[0]);//avoid concurrency issues. kindof.
        				for (int i = 0; i < larray.length; i++) {
        					larray[i].listenedFileChanged(event);
        				}
        			}
        		}
        	}
        };
        ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceChangeListener);
	}

	/**
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(
	 *         org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeListener);
		super.stop(context);
	}
	
	/**
	 * @param rcl Adds a subscriber to a resource change event.
	 */
	public void subscribe(AbstractIFileChangeListener fileChangeListener) {
		synchronized (resourceChangeListener) {
			String channel = fileChangeListener.getListenedFileFullPath();
			Set<AbstractIFileChangeListener> channelListeners = resourceChangeSubscribers.get(channel);
			if (channelListeners == null) {
				channelListeners = new HashSet<AbstractIFileChangeListener>();
				resourceChangeSubscribers.put(channel, channelListeners);
			}
			channelListeners.add(fileChangeListener);
		}
	}
	
	/**
	 * @param rcl Removes a subscriber to a resource change event.
	 */
	public void unsubscribe(AbstractIFileChangeListener fileChangeListener) {
		synchronized (resourceChangeListener) {
			String channel = fileChangeListener.getListenedFileFullPath();
			Set<AbstractIFileChangeListener> channelListeners = resourceChangeSubscribers.get(channel);
			if (channelListeners != null
					&& channelListeners.remove(fileChangeListener)
					&& channelListeners.isEmpty()) {
				//nobody left listening to this file.
				resourceChangeSubscribers.remove(channel);
			}
		}
	}
	
	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static MessagesEditorPlugin getDefault() {
		return plugin;
	}

	//--------------------------------------------------------------------------
	//TODO Better way/location for these methods?
	
	/**
	 * Returns the string from the plugin's resource bundle,
	 * or 'key' if not found.
     * @param key the key for which to fetch a localized text
     * @return localized string corresponding to key
	 */
	public static String getString(String key) {
		ResourceBundle bundle = 
                MessagesEditorPlugin.getDefault().getResourceBundle();
		try {
			return (bundle != null) ? bundle.getString(key) : key;
		} catch (MissingResourceException e) {
			return key;
		}
	}

    /**
     * Returns the string from the plugin's resource bundle,
     * or 'key' if not found.
     * @param key the key for which to fetch a localized text
     * @param arg1 runtime argument to replace in key value 
     * @return localized string corresponding to key
     */
    public static String getString(String key, String arg1) {
        return MessageFormat.format(getString(key), new Object[]{arg1});
    }
    
    /**
     * Returns the string from the plugin's resource bundle,
     * or 'key' if not found.
     * @param key the key for which to fetch a localized text
     * @param arg1 runtime first argument to replace in key value
     * @param arg2 runtime second argument to replace in key value
     * @return localized string corresponding to key
     */
    public static String getString(String key, String arg1, String arg2) {
        return MessageFormat.format(
                getString(key), new Object[]{arg1, arg2});
    }
    
    /**
     * Returns the string from the plugin's resource bundle,
     * or 'key' if not found.
     * @param key the key for which to fetch a localized text
     * @param arg1 runtime argument to replace in key value 
     * @param arg2 runtime second argument to replace in key value
     * @param arg3 runtime third argument to replace in key value
     * @return localized string corresponding to key
     */
    public static String getString(
            String key, String arg1, String arg2, String arg3) {
        return MessageFormat.format(
                getString(key), new Object[]{arg1, arg2, arg3});
    }
    
	/**
	 * Returns the plugin's resource bundle.
     * @return resource bundle
	 */
	protected ResourceBundle getResourceBundle() {
		return resourceBundle;
	}
	
	// Stefan's activator methods:
	
	/**
	 * Returns an image descriptor for the given icon filename.
	 * 
	 * @param filename the icon filename relative to the icons path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String filename) {
		String iconPath = "icons/"; //$NON-NLS-1$
		return imageDescriptorFromPlugin(PLUGIN_ID, iconPath + filename);
	}

	public static ResourceBundleModel getModel(IProgressMonitor monitor) {
		if (plugin.model == null) {
			plugin.model = new ResourceBundleModel(monitor);
		}
		return plugin.model;
	}

	public static void disposeModel() {
		if (plugin != null) {
			plugin.model = null;
		}
	}

	// Logging

	/**
	 * Adds the given exception to the log.
	 * 
	 * @param e the exception to log
	 * @return the logged status
	 */
	public static IStatus log(Throwable e) {
		return log(new Status(IStatus.ERROR, PLUGIN_ID, 0, "Internal error.", e));
	}

	/**
	 * Adds the given exception to the log.
	 * 
	 * @param exception the exception to log
	 * @return the logged status
	 */
	public static IStatus log(String message, Throwable exception) {
		return log(new Status(IStatus.ERROR, PLUGIN_ID, -1, message, exception));
	}

	/**
	 * Adds the given <code>IStatus</code> to the log.
	 * 
	 * @param status the status to log
	 * @return the logged status
	 */
	public static IStatus log(IStatus status) {
		getDefault().getLog().log(status);
		return status;
	}

	
}
