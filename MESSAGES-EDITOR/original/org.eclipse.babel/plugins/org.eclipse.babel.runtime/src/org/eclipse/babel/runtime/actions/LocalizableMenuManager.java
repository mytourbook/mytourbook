/*******************************************************************************
 * Copyright (c) 2008 Nigel Westbury and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Nigel Westbury - initial API and implementation
 *******************************************************************************/

package org.eclipse.babel.runtime.actions;

import org.eclipse.babel.runtime.external.ITranslatableText;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IContributionItem;

/**
 * A container class that contains a MenuManager object (used
 * by the content provided to get child items) and a ITranslatableText
 * object (used by the label provider and in-place editor support).
 */
public class LocalizableMenuManager implements IAdaptable {

	private IContributionItem menuManager;
	private ITranslatableText localizableText;

	public LocalizableMenuManager(IContributionItem menuManager, ITranslatableText localizableText) {
		this.menuManager = menuManager;
		this.localizableText = localizableText;
	}

	public IContributionItem getMenuManager() {
		return menuManager;
	}

	public ITranslatableText getLocalizableText() {
		return localizableText;
	}

	public Object getAdapter(Class adapter) {
		if (adapter == ITranslatableText.class) {
			return localizableText;
		}
		return null;
	}
}
