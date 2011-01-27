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
import java.util.MissingResourceException;
import java.util.Set;

import org.eclipse.babel.runtime.Messages;

public class TranslatableText implements ITranslatableText {

	private TranslatableResourceBundle resourceBundle;
	private String key;
	
	public TranslatableText(TranslatableResourceBundle resourceBundle, String key) {
		if (resourceBundle == null)
			throw new RuntimeException("");
		this.resourceBundle = resourceBundle;
		this.key = key;
	}
	
	public String getLocalizedText() {
		return resourceBundle.getString(key);
	}

	public String getLocalizedText(Locale locale) {
		TranslatableResourceBundle rb = resourceBundle;
		while (rb != null && !rb.getMyLocale().equals(locale)) {
			rb = rb.getParent();
		}
		
		if (rb == null) { 
			throw new RuntimeException();
		}

		try {
			return rb.getString(key);
		} catch (MissingResourceException e) {
			return "%" + key; //$NON-NLS-1$
		}
	}

	/**
	 * 
	 * @param locale
	 * @param newValue the text to be displayed, or null if
	 * 		the text for the parent locale is to be displayed
	 * @param updatedBundles
	 */
	public void setLocalizedText(Locale locale, String newValue, Set<TranslatableResourceBundle> updatedBundles) {
		TranslatableResourceBundle rb = resourceBundle;
		while (rb != null && !rb.getMyLocale().equals(locale)) {
			rb = rb.getParent();
		}
		
		if (rb == null) { 
			throw new RuntimeException();
		}
		
		rb.setString(key, newValue);
		updatedBundles.add(rb);		
	}

	public String getKey() {
		return key;
	}
	
	public ITranslatableText getTooltip() {
		return new FormattedTranslatableText( 
			new NonTranslatableText("{0}\n{1}\n{2}"),	
			Messages.bind(Messages.LocalizeDialog_TableTooltip_Plugin, resourceBundle.getOsgiBundle().getSymbolicName()),
			Messages.bind(Messages.LocalizeDialog_TableTooltip_ResourceBundle, resourceBundle.getDescription()),
			Messages.bind(Messages.LocalizeDialog_TableTooltip_Key, key));
	}

	public void validateLocale(Locale locale) {
		if (!locale.equals(resourceBundle.getMyLocale())) {
			throw new RuntimeException("locales do not match");
		}
	}

	/**
	 * Reverts the text back to the original text in the resource file packaged with the
	 * bundle.
	 */
	public void revertLocalizedText(Locale locale, Set<TranslatableResourceBundle> updatedBundles) {
		TranslatableResourceBundle rb = resourceBundle;
		while (rb != null && !rb.getMyLocale().equals(locale)) {
			rb = rb.getParent();
		}
		
		if (rb == null) { 
			throw new RuntimeException();
		}
		
		rb.revertString(key);
		updatedBundles.add(rb);		
	}

	public boolean isDirty(Locale locale) {
		TranslatableResourceBundle rb = resourceBundle;
		while (rb != null && !rb.getMyLocale().equals(locale)) {
			rb = rb.getParent();
		}
		
		if (rb == null) { 
			throw new RuntimeException();
		}
		
		return rb.isDirty(key);
	}
}
