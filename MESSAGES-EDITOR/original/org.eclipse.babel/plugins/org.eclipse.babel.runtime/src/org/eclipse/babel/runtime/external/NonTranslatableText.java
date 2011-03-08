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

public class NonTranslatableText implements ITranslatableText {

	private String text;
	
	public NonTranslatableText(String text) {
		this.text = text;
	}

	public String getLocalizedText(Locale locale) {
		/*
		 * The value is the same regardless of the locale.
		 */
		return text;
	}

	public String getLocalizedText() {
		return getLocalizedText(Locale.getDefault());
	}

	public void validateLocale(Locale locale) {
		/*
		 * We don't care about locales, so always assume it is a valid
		 * locale.
		 */
	}
}
