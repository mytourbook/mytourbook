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
 * Represents an element node in an XML document.
 */
public interface Element {
	/**
	 * Implements an iterator over a sequence of nodes.
	 */
	public class NodeIterator implements Element {
		public Element attribute(String name, String value) {
			throw new UnsupportedOperationException("NodeIterators do not have attributes");
		}

		public void render(ContentHandler handler) throws SAXException {
			Element element;
			while((element = next()) != null){
				element.render(handler);
			}
		}
		
		public Element next(){
			return null;
		}

	}

	public static class utils{
		
		/**
		 * The empty element. Useful when building templates.
		 */
		public static final Element EMPTY_ELEMENT = new Element(){
			public Element attribute(String name, String value) {
				return this;
			}

			public void render(ContentHandler handler) throws SAXException {}
		};
	}
	
	/**
	 * Implements a sequence of Elements.
	 */
	public static class NodeSequence implements Element{
		
		private final Element[] children;

		public NodeSequence(Element... children){
			this.children = children;
		}
		
		/**
		 * This method throws an UnsupportedOperationException as NodeSequences do not have attributes. 
		 */
		public Element attribute(String name, String value) {
			throw new UnsupportedOperationException("NodeSequences do not have attributes.");
		}

		public void render(ContentHandler handler) throws SAXException {
			for(Element child : children){
				child.render(handler);
			}
		}
		
	}

	/**
	 * Recursively render this element and it's children to the provided ContentHandler.
	 * 
	 * @param handler The handler to send the SAX events describing this element and it's children.
	 * @throws SAXException
	 */
	public void render(ContentHandler handler) throws SAXException;
	
	/**
	 * Add an attribute to this element. 
	 * May throw an UnsupportedOperationException if the underlying implementation does not have attributes.
	 * 
	 * @param name The name of the attribute to create.
	 * @param value The value of the created attribute.
	 * @return This element. Allows chained calls to attribute.
	 */
	public Element attribute(String name, String value);
}
