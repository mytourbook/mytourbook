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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;

/**
 * A <code>ResourceBundleFamily</code> represents a group of resource bundles
 * that belong together. Member resource bundles may reside in the same project as the
 * default resource bundle, or in case of a plugin project, in a separate fragment
 * project.
 */
public class ResourceBundleFamily extends ResourceBundleElement {

	/**
	 * The project name of the default bundle.
	 */
	private String projectName;
	/**
	 * The plugin id of the default bundle, or <code>null</code> if not a plugin or fragment project.
	 */
	private String pluginId;
	/**
	 * The package name or path.
	 */
	private String packageName;
	/**
	 * The base name that all family members have in common.
	 */
	private String baseName;
	/**
	 * The members that belong to this resource bundle family (excluding the default bundle).
	 */
	private ArrayList<ResourceBundle> members = new ArrayList<ResourceBundle>();
	/**
	 * A collection of known keys.
	 */
	private HashMap<String, ResourceBundleKey> keys = new HashMap<String, ResourceBundleKey>();

	public ResourceBundleFamily(ResourceBundleModel parent, String projectName, String pluginId,
			String packageName, String baseName) {
		super(parent);
		this.projectName = projectName;
		this.pluginId = pluginId;
		this.packageName = packageName;
		this.baseName = baseName;
	}

	public String getProjectName() {
		return projectName;
	}

	public String getPluginId() {
		return pluginId;
	}

	public String getPackageName() {
		return packageName;
	}

	public String getBaseName() {
		return baseName;
	}

	public ResourceBundle[] getBundles() {
		return members.toArray(new ResourceBundle[members.size()]);
	}

	public ResourceBundle getBundle(Locale locale) {
		for (ResourceBundle bundle : members) {
			if (bundle.getLocale().equals(locale)) {
				return bundle;
			}
		}
		return null;
	}

	/*
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		if (pluginId != null) {
			return baseName.hashCode() ^ pluginId.hashCode();
		} else {
			return baseName.hashCode() ^ projectName.hashCode();
		}
	}

	protected void addBundle(ResourceBundle bundle) throws CoreException {
		Assert.isTrue(bundle.getParent() == this);
		members.add(bundle);
	}

	protected void addKey(String key) {
		if (keys.get(key) == null) {
			keys.put(key, new ResourceBundleKey(this, key));
		}
	}

	public ResourceBundleKey[] getKeys() {
		Collection<ResourceBundleKey> values = keys.values();
		return values.toArray(new ResourceBundleKey[values.size()]);
	}

	public int getKeyCount() {
		return keys.size();
	}

	/*
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "projectName=" + projectName + ", packageName=" + packageName + ", baseName=" + baseName;
	}

}
