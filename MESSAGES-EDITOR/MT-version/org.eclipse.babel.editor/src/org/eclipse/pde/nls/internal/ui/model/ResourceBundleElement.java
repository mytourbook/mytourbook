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

public abstract class ResourceBundleElement {

	private final ResourceBundleElement parent;

	public ResourceBundleElement(ResourceBundleElement parent) {
		this.parent = parent;
	}
	
	public ResourceBundleElement getParent() {
		return parent;
	}

}
