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

import java.util.LinkedList;
import java.util.List;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Implements a builder Element node.
 */
public class TagElement implements Element {
	private final String tag;
	private final AttributesImpl attrs = new AttributesImpl();
	private final List<Element> children = new LinkedList<Element>();
	private final String namespace;
	
	public TagElement(String namespace, String tag, Element... children){
		this.namespace = namespace;
		this.tag = tag;
		
		for(Element element : children){
			this.children.add(element);
		}
	}
	
	public void render(ContentHandler handler) throws SAXException {
		handler.startElement(namespace, tag, null, attrs);
		for(Element child : children){
			child.render(handler);
		}
		handler.endElement(namespace, tag, null);
	}

	public Element attribute(String name, String value) {
		attrs.addAttribute(null, null, name, "string", value);
		return this;
	}
}
