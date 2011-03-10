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

package org.eclipse.babel.runtime.pluginXmlParsing;

import org.eclipse.core.runtime.Assert;
import org.xml.sax.Attributes;

/**
 * This object represents the top level <plugin> element.
 */
class InitialState implements IObjectBuiltFromElement {
	
	private LocalizableContribution theOneAndOnlyPluginElement = null;
	
	public InitialState(LocalizableContribution contribution) {
		theOneAndOnlyPluginElement = contribution;
	}

	public void characters(char[] ch, int start, int length) {
		// Ignore characters outside the <plugin> element
	}

	public void endElement(String uri, String elementName, String name) {
		// shouldn't get here
		Assert.isTrue(false);
	}

	public IObjectBuiltFromElement startElement(String uri,
			String elementName, String name, Attributes attributes) {
		Assert.isTrue(elementName.equals("plugin")); //$NON-NLS-1$
		// Use the object that has already been constructed and that
		// was passed to us.
		return theOneAndOnlyPluginElement;
	}
}