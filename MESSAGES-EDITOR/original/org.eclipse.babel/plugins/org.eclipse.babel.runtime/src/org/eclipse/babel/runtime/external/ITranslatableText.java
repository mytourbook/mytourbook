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

public interface ITranslatableText {

	/**
	 * This method gets the localized text for a particular locale.
	 * The locale must be in the fallback list.  Fallback is used,
	 * so it is up to the caller to compare against the value returned
	 * for the  parent locale if necessary.
	 * 
	 * @param locale
	 * @return
	 */
	String getLocalizedText(Locale locale);

	/**
	 * Checks that the locale of the bundle used to get this message is
	 * the given locale.
	 * 
	 * @param locale
	 * @throws RuntimeException the locale does not match
	 */
	void validateLocale(Locale locale);

	/**
	 * Same as calling <code>getLocalizedText(Locale.getDefault())</code>
	 * 
	 * This method should be used to get text only in cases where the text
	 * is not shown directly in the part (view, editor, or dialog) but appears
	 * non-modally only when the user takes a certain action.  An example would
	 * be a message appearing inside an error dialog box where the message is
	 * including in the list of translatable texts for the parent.  In that case
	 * no dynamic update is required because the user cannot translate text while
	 * the error dialog is open.  Just be sure to call this method each time the
	 * error dialog is opened (i.e. do not cache the returned String).
	 *
	 *NO NO, really this should be used only in cases where the user really
	 *doesn't care about translation, such as in log messages.
	 *
	 * In above scenario, must still be associated with a translation set.
	 * 
	 * @return
	 */
	String getLocalizedText();
}
