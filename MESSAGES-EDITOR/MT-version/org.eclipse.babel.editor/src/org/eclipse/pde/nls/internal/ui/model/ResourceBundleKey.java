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

import java.util.Locale;

import org.eclipse.core.runtime.CoreException;

/**
 * A <code>ResourceBundleKey</code> represents a key used in one or more bundles of
 * a {@link ResourceBundleFamily}.
 */
public class ResourceBundleKey extends ResourceBundleElement {

	private String key;

	public ResourceBundleKey(ResourceBundleFamily parent, String key) {
		super(parent);
		this.key = key;
	}
	
	/*
	 * @see org.eclipse.nls.ui.model.ResourceBundleElement#getParent()
	 */
	@Override
	public ResourceBundleFamily getParent() {
		return (ResourceBundleFamily) super.getParent();
	}

	public ResourceBundleFamily getFamily() {
		return getParent();
	}

	public String getName() {
		return key;
	}

	public String getValue(Locale locale) throws CoreException {
		ResourceBundle bundle = getFamily().getBundle(locale);
		if (bundle == null)
			return null;
		return bundle.getString(key);
	}

	public boolean hasValue(Locale locale) throws CoreException {
		return getValue(locale) != null;
	}

	/*
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "ResourceBundleKey {" + key + "}";
	}

}
