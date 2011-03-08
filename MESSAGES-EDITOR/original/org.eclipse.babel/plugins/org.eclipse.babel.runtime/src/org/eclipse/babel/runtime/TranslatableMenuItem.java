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

package org.eclipse.babel.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.eclipse.babel.runtime.external.ITranslatableText;
import org.eclipse.core.runtime.IAdaptable;

/**
 * This object forms a tree that exactly matches (in theory) the menu.
 * 
 * The only difference between this object tree and the menu is that this
 * tree contains all menu items even if they are not shown because the activities
 * feature has them filtered out.  The reason for this is that this tree does not
 * then have to be re-built if activities are activated.  The enabled activities
 * should be checked by the menu tree content provider in the translation dialog.
 * That ensures that the user always sees the same menu in the tree in the translation
 * dialog as the actual menu in the workbench.
 */
public class TranslatableMenuItem implements IAdaptable {

	ITranslatableText localizableText;
	
	List<TranslatableMenuItem> children = new ArrayList<TranslatableMenuItem>();
	
	TranslatableMenuItem(ITranslatableText localizableText) {
		this.localizableText = localizableText;
	}
	
	public Object getAdapter(Class adapter) {
		if (adapter == ITranslatableText.class) {
			return localizableText;
		}
		return null;
	}

	public Collection<TranslatableMenuItem> getChildren() {
		return children;
	}

	public void add(TranslatableMenuItem childItem) {
		children.add(childItem);
	}

	public ITranslatableText getLocalizableText() {
		return localizableText;
	}

}
