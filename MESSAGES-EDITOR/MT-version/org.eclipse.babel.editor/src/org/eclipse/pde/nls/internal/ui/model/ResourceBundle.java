/*******************************************************************************
 * Copyright (c) 2008 Stefan Mücke and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Stefan Mücke - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.nls.internal.ui.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.babel.editor.plugin.MessagesEditorPlugin;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJarEntryResource;

/**
 * A <code>ResourceBundle</code> represents a single <code>.properties</code> file.
 * <p>
 * <code>ResourceBundle</code> implements lazy loading. A bundle will be loaded
 * automatically when its entries are accessed. It can through the parent model by
 * calling {@link ResourceBundleModel#unloadBundles(Locale)} with the proper locale.
 * </p>
 */
public class ResourceBundle extends ResourceBundleElement {

	private static boolean debug = false;
	
	/**
	 * The bundle's locale.
	 */
	private Locale locale;
	/**
	 * The underlying resource. Either an {@link IFile} or an {@link IJarEntryResource}.
	 */
	private Object resource;

	private HashMap<String, String> entries;

	public ResourceBundle(ResourceBundleFamily parent, Object resource, Locale locale) {
		super(parent);
		this.resource = resource;
		this.locale = locale;
		if (locale == null)
			throw new IllegalArgumentException("Locale may not be null.");
	}

	/**
	 * Returns the family to which this bundle belongs.
	 * 
	 * @return the family to which this bundle belongs
	 */
	public ResourceBundleFamily getFamily() {
		return (ResourceBundleFamily) super.getParent();
	}

	/**
	 * Returns the locale.
	 * 
	 * @return the locale
	 */
	public Locale getLocale() {
		return locale;
	}

	public String getString(String key) throws CoreException {
		load();
		return entries.get(key);
	}

	/**
	 * Returns the underlying resource. This may be an {@link IFile}
	 * or an {@link IJarEntryResource}.
	 * 
	 * @return the underlying resource (an {@link IFile} or an {@link IJarEntryResource})
	 */
	public Object getUnderlyingResource() {
		return resource;
	}

	protected boolean isLoaded() {
		return entries != null;
	}

	public void load() throws CoreException {
		if (isLoaded())
			return;
		entries = new HashMap<String, String>();

		if (resource instanceof IFile) {
			if (debug) {
				System.out.println("Loading " + resource + "...");
			}
			IFile file = (IFile) resource;
			InputStream inputStream = file.getContents();
			Properties properties = new Properties();
			try {
				properties.load(inputStream);
				putAll(properties);
			} catch (IOException e) {
				MessagesEditorPlugin.log("Error reading property file.", e);
			}
		} else if (resource instanceof IJarEntryResource) {
			IJarEntryResource jarEntryResource = (IJarEntryResource) resource;
			InputStream inputStream = jarEntryResource.getContents();
			Properties properties = new Properties();
			try {
				properties.load(inputStream);
				putAll(properties);
			} catch (IOException e) {
				MessagesEditorPlugin.log("Error reading property file.", e);
			}
		} else {
			MessagesEditorPlugin.log("Unknown resource type.", new RuntimeException());
		}
	}

	protected void unload() {
		entries = null;
	}

	public boolean isReadOnly() {
		if (resource instanceof IJarEntryResource)
			return true;
		if (resource instanceof IFile) {
			IFile file = (IFile) resource;
			return file.isReadOnly() || file.isLinked();
		}
		return false;
	}

	protected void putAll(Properties properties) throws CoreException {
		Set<Entry<Object, Object>> entrySet = properties.entrySet();
		Iterator<Entry<Object, Object>> iter = entrySet.iterator();
		ResourceBundleFamily family = getFamily();
		while (iter.hasNext()) {
			Entry<Object, Object> next = iter.next();
			Object key = next.getKey();
			Object value = next.getValue();
			if (key instanceof String && value instanceof String) {
				String stringKey = (String) key;
				entries.put(stringKey, (String) value);
				family.addKey(stringKey);
			}
		}
	}

	public void put(String key, String value) throws CoreException {
		load();
		ResourceBundleFamily family = getFamily();
		entries.put(key, value);
		family.addKey(key);
	}

	public String[] getKeys() throws CoreException {
		load();
		Set<String> keySet = entries.keySet();
		return keySet.toArray(new String[keySet.size()]);
	}

}
