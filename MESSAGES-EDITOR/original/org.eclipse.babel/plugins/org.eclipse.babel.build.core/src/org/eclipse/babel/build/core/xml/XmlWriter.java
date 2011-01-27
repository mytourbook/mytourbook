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

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * A content handler which serialises SAX events to a stream.
 * Useful for serialising XML documents.
 * 
 * For a higher level XML serialisation library {@link org.eclipse.babel.build.core.xml.Builder}
 */
public class XmlWriter implements ContentHandler {
	
	private final PrintWriter out;
	
	public XmlWriter(PrintWriter out){
		this.out = out;
	}
	
	public XmlWriter(Writer out){
		this(new PrintWriter(out));
	}
	
	
	public XmlWriter(OutputStream out) {
		this(new OutputStreamWriter(out));
	}

	public void characters(char[] ch, int start, int length) throws SAXException {
		out.write(ch, start, length);
	}

	public void endDocument() throws SAXException {
		out.flush();
	}

	public void endElement(String uri, String localName, String name) throws SAXException {
		out.format("</%s>\n", localName);
	}

	public void endPrefixMapping(String prefix) throws SAXException {
		throw new UnsupportedOperationException();
	}

	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
		throw new UnsupportedOperationException();
	}

	public void processingInstruction(String target, String data) throws SAXException {
		throw new UnsupportedOperationException();
	}

	public void setDocumentLocator(Locator locator) {
		throw new UnsupportedOperationException();
	}

	public void skippedEntity(String name) throws SAXException {
		throw new UnsupportedOperationException();
	}

	public void startDocument() throws SAXException {
		out.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
	}

	public void startElement(String uri, String localName, String name, Attributes atts) throws SAXException {
		List<String> attrs = new LinkedList<String>();
		int len = atts.getLength();
		
		if(len > 0){
			attrs.add("");
		}
		for(int i = 0; i < len; i++){
			attrs.add(String.format("%s=\"%s\"", atts.getQName(i), atts.getValue(i)));
		}
		
		out.format("<%s%s>", localName, join(" ", attrs));
	}

	private <T> String join(String seperator, Iterable<T> parts){
		StringBuilder builder = new StringBuilder("");
		for(Iterator<T> it = parts.iterator(); it.hasNext();){
			T part = it.next();
			
			builder.append(part.toString());
			if(it.hasNext()){
				builder.append(seperator);
			}
		}
		
		return builder.toString();
	}

	public void startPrefixMapping(String prefix, String uri)
			throws SAXException {
		throw new UnsupportedOperationException();
	}

}
