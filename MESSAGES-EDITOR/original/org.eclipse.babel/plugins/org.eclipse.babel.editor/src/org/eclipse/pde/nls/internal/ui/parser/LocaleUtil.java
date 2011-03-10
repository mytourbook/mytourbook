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
package org.eclipse.pde.nls.internal.ui.parser;

import java.util.Locale;
import java.util.StringTokenizer;

public class LocaleUtil {

	private LocaleUtil() {
	}

	public static Locale parseLocale(String name) throws IllegalArgumentException {
		String language = ""; //$NON-NLS-1$
		String country = ""; //$NON-NLS-1$
		String variant = ""; //$NON-NLS-1$

		StringTokenizer tokenizer = new StringTokenizer(name, "_"); //$NON-NLS-1$
		if (tokenizer.hasMoreTokens())
			language = tokenizer.nextToken();
		if (tokenizer.hasMoreTokens())
			country = tokenizer.nextToken();
		if (tokenizer.hasMoreTokens())
			variant = tokenizer.nextToken();

		if (!language.equals("") && language.length() != 2) //$NON-NLS-1$
			throw new IllegalArgumentException();
		if (!country.equals("") && country.length() != 2) //$NON-NLS-1$
			throw new IllegalArgumentException();

		if (!language.equals("")) { //$NON-NLS-1$
			char l1 = language.charAt(0);
			char l2 = language.charAt(1);
			if (!('a' <= l1 && l1 <= 'z' && 'a' <= l2 && l2 <= 'z'))
				throw new IllegalArgumentException();
		}

		if (!country.equals("")) { //$NON-NLS-1$
			char c1 = country.charAt(0);
			char c2 = country.charAt(1);
			if (!('A' <= c1 && c1 <= 'Z' && 'A' <= c2 && c2 <= 'Z'))
				throw new IllegalArgumentException();
		}

		return new Locale(language, country, variant);
	}

}
