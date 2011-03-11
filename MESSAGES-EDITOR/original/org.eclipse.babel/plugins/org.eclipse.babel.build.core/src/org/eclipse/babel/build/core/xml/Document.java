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
package org.eclipse.babel.build.core.xml;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Implements a Builder document element. 
 */
public class Document implements Element {
	private final Element root;
	

	public Document(Element root) {
		this.root = root;
	}

	public void render(ContentHandler handler) throws SAXException {
		handler.startDocument();
		root.render(handler);
		handler.endDocument();
	}

	public Element attribute(String name, String value) {
		root.attribute(name, value);
		return this;
	}
}
