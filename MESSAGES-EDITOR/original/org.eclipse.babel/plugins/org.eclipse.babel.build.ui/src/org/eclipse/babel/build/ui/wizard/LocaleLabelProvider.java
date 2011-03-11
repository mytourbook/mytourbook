/*******************************************************************************
 * Copyright (c) 2001, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.babel.build.ui.wizard;

import java.util.Locale;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

public class LocaleLabelProvider extends LabelProvider{
	public LocaleLabelProvider() {
	}
	
	@Override
	public Image getImage(Object element) {
		//return BuildToolImages.LOCALE.createImage();
		return null;
	}

	@Override
	public String getText(Object element) {
		Locale locale = (Locale) element;
		String country = locale.getDisplayCountry();
		String varient = locale.getDisplayVariant();
		
		return locale.getDisplayLanguage() + " " + getDisplay(country, varient); //$NON-NLS-1$
	}

	private String getDisplay(String country, String varient) {
		if("".equals(country)){
			return "";
		}
		
		if("".equals(varient)){
			return String.format("(%s)", country);
		}
		return String.format("(%s: %s)", country, varient);
	}
}
