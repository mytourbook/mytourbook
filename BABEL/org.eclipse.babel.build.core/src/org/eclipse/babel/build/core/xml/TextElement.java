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
 * Implements a Builder text node.
 */
public class TextElement implements Element{
	private final String text;
	
	public TextElement(String text){
		this.text = text;
	}
	
	public void render(ContentHandler handler) throws SAXException {
		char[] ch = text.toCharArray();
		handler.characters(ch, 0, ch.length);
	}

	public Element attribute(String name, String value) {
		throw new UnsupportedOperationException();
	}
}
