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

package org.eclipse.babel.runtime.external;

import java.util.Locale;

public abstract class TranslatableTextInput {

	private ITranslatableText localizedText;
	
	public TranslatableTextInput(ITranslatableText localizedText) {
		this.localizedText = localizedText;
		
		updateControl();
	}
	
	/**
	 * This method should be used when the caller wants to take responsibility
	 * for setting the initial text but the setText method in the given TranslatableTextInput
	 * object is to take responsibility to set the text should it be changed.
	 * 
	 * This method is used, for example, when setting the part name.  This is called
	 * from the constructor, and EditorPart.setPartName cannot be called at that point
	 * because the part has not yet been constructed.  In fact, this method should be
	 * used in any situation where the initial text value must be passed to a constructor.
	 */
	public TranslatableTextInput(ITranslatableText localizedText, boolean noInitialUpdate) {
		this.localizedText = localizedText;
	}
	
	public void updateControl() {
		updateControl(localizedText.getLocalizedText(Locale.getDefault()));
	}

	public abstract void updateControl(String text);

	public ITranslatableText getLocalizedTextObject() {
		return localizedText;
	}
}
