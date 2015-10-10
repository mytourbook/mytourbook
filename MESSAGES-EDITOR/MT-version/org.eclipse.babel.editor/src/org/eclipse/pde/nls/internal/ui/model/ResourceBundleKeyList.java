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

public class ResourceBundleKeyList {

	private final ResourceBundleKey[] keys;

	public ResourceBundleKeyList(ResourceBundleKey[] keys) {
		this.keys = keys;
	}

	public ResourceBundleKey getKey(int index) {
		return keys[index];
	}

	public int getSize() {
		return keys.length;
	}

}
