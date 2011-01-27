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

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * High level XML serialization tool modeled after the Ruby Builder API.
 * Contains static methods that allow clients to easily build document trees.
 * 
 * For example:
 * 
 * <pre>
 * Document doc = root("contacts",
 *	element("description", text("People that I know")),
 *	element("people", 
 *		element("person").attribute("name", "Alice"),
 *		element("person").attribute("name", "Bob")
 *	)
 * );
 * 
 * doc.render(new XmlWriter(System.out));
 * </pre>
 */
public class Builder{
	private Builder(){}
	
	
	public interface ToNode<T>{
		Element toNode(T t);
	}
	
	public static <T> Element sequence(final Iterable<T> ts, final ToNode<T> toNode){
		return new Element.NodeIterator(){
			Iterator<T> iter = ts.iterator();
			
			@Override
			public Element next() {
				try{
					T t = iter.next();
					return toNode.toNode(t);
				} catch (NoSuchElementException ex){
					/* Reached the end of the iterable */
					return null;
				}
			}
		};
	}
	
	public static Element nodes(Element... children){
		return new Element.NodeSequence(children);
	}
	
	
	/**
	 * Creates a new element. 
	 * 
	 * @param ns The namespace of the element.
	 * @param tag The local name of the element.
	 * @param elements The child elements of this element.
	 * @return The newly created element.
	 */
	public static Element element(String ns, String tag, Element... elements){
		return new TagElement(ns, tag, elements);
	}
	
	/**
	 * Creates a new element in the default namespace.
	 * 
	 * @param tag The local name of the element.
	 * @param elements The child elements of this element.
	 * @return The newly created element.
	 */
	public static Element element(String tag, Element... elements){
		return element("", tag, elements);
	}
	
	/**
	 * Creates a new text node.
	 * @param text The text to be contained within the text node.
	 * @return The newly created text node.
	 */
	public static Element text(String text){
		return new TextElement(text);
	}
	
	/**
	 * Creates a new document with a root element.
	 * 
	 * @param ns The namespace of the root element.
	 * @param tag The local name of the root element.
	 * @param elements The children of the root element.
	 * @return The newly created document.
	 */
	public static Document root(String ns, String tag, Element...elements){
		return new Document(new TagElement(ns, tag, elements));
	}
	
	/** 
	 * Creates a new document with a root element in the default namespace.
	 * 
	 * @param tag The local name of the root element.
	 * @param elements The children of the root element.
	 * @return The newly create document.
	 */
	public static Document root(String tag, Element... elements){
		return root("", tag, elements);
	}
}
