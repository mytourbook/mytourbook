/*******************************************************************************
 * Copyright (c) 2008 Nigel Westbury.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Nigel Westbury - initial API and implementation
 *******************************************************************************/

package org.eclipse.babel.runtime;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.babel.runtime.external.ITranslatableSet;
import org.eclipse.babel.runtime.external.TranslatableText;
import org.eclipse.babel.runtime.external.TranslatableResourceBundle;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.babel.runtime";

	// The shared instance
	private static Activator plugin;
	
	//Resource bundle.
	private TranslatableResourceBundle resourceBundle;

	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

		/*
		 * Registering the updatable bundles now ensures that they are more
		 * likely to be seen by the translation dialog. If this plug-in has not
		 * been started then they still won't be seen, so perhaps we really
		 * should be registering updatable resource bundles in an extension
		 * point.
		 */
		
		/* This is the Java 6 method...
		resourceBundle = (TranslatableResourceBundle)ResourceBundle.getBundle("org.eclipse.babel.runtime.messages",
  	          new UpdatableResourceControl(getStateLocation()));
		*/
		resourceBundle = TranslatableResourceBundle.get(getBundle(), getClass().getClassLoader(), "org.eclipse.babel.runtime.messages");
		
		TranslatableResourceBundle.register(getBundle(), "org.eclipse.babel.runtime.messages");

		// Now done in TranslatableNLS derived class		
//		TranslatableResourceBundle.register(resourceBundle, getBundle());
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	public static TranslatableText getLocalizableText(String key) {
		return new TranslatableText(getDefault().resourceBundle, key);
	}

	public static ImageDescriptor createImageDescriptor(String name) {
		try {
			URL installURL = getDefault().getBundle().getEntry("/");
			URL url = new URL(installURL, name);
			return ImageDescriptor.createFromURL(url);
		} catch (MalformedURLException e) {
			// should not happen
			return ImageDescriptor.getMissingImageDescriptor();
		}
	}

    protected void initializeImageRegistry(ImageRegistry registry) {
    	addToImageRegistry(registry, "icons/localizable.gif");
    	addToImageRegistry(registry, "icons/nonLocalizable.gif");
    }

    private void addToImageRegistry(ImageRegistry registry, String filePath) {
    	URL url = FileLocator.find(plugin.getBundle(), new Path(filePath), null);
    	ImageDescriptor desc = ImageDescriptor.createFromURL(url);
    	registry.put(filePath, desc);
    }

	public static Image getImage(String key) {
		return plugin.getImageRegistry().get(key);
	}

	private TranslatableMenuItem translatableMenu;
	private ITranslatableSet menuTextSet;
	
	public void setTranslatableMenu(TranslatableMenuItem translatableMenu, ITranslatableSet menuTextSet) {
		this.translatableMenu = translatableMenu;
		this.menuTextSet = menuTextSet;
	}
	
	public TranslatableMenuItem getTranslatableMenu() {
		return translatableMenu;
	}
	
	public ITranslatableSet getMenuTextSet() {
		return menuTextSet;
	}
}
