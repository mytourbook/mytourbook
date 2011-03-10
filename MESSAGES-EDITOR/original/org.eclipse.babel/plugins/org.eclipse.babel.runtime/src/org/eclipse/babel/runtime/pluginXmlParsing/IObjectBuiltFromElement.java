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

import org.xml.sax.Attributes;

public interface IObjectBuiltFromElement {

	IObjectBuiltFromElement startElement(String uri, String elementName, String name,
			Attributes attributes);

	void endElement(String uri, String elementName, String name);

	void characters(char[] ch, int start, int length);
}
