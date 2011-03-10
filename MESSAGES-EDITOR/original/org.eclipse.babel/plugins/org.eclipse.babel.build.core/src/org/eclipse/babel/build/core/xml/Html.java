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

import static org.eclipse.babel.build.core.xml.Builder.*;

/**
 * Implements grammar specific Builder elements to facilitate the generation of HTML documents.  
 */
public class Html {
	public static final String NS = "http://http://www.w3.org/1999/xhtml";
	
	/**
	 * Creates a new HTML document.
	 * 
	 * @param children The children of the root element.
	 * @return The newly created HTML document.
	 */
	public static Document html(Element... children){
		return (Document)root(NS, "html", children).attribute("xmlns", NS);
	}
	
	/**
	 * Creates an HTML head element.
	 * 
	 * @param children The children of the document's head.
	 * @return The newly create head element.
	 */
	public static Element head(Element... children){
		return element(NS, "head", children);
	}
	
	/**
	 * Create an HTML title element.
	 * 
	 * @param title The title of the document.
	 * @return The newly created title element.
	 */
	public static Element title(String title){
		return element(NS, "title", text(title));
	}
	
	/**
	 * Creates an HTML link element for linking to an external CSS stylesheet.
	 * 
	 * @param url The URL of the stylesheet.
	 * @return The newly created link element.
	 */
	public static Element stylesheet(String url){
		return element(NS, "link")
				.attribute("type", "text/css")
				.attribute("href", url)
				.attribute("rel", "stylesheet");
		
	}
	
	public static Element style(String style){
		return element(NS, "style", text(style)).attribute("type", "text/css");
	}
	
	/**
	 * Create an HTML body element.
	 * 	
	 * @param elements The children of the document's body.
	 * @return The newly created body element.
	 */
	public static Element body(Element...elements){
		return element(NS, "body", elements);
	}
	
	/**
	 * Creates an HTML h1 element.
	 *  
	 * @param title The title to be contained within the h1.
	 * @return The newly created h1 element.
	 */
	public static Element h1(String title){
		return h1(text(title));
	}
	
	public static Element h1(Element... children){
		return element(NS, "h1", children);
	}
	
	/**
	 * Creates an HTML h2 element.
	 * 
	 * @param title The title to be contained within the h2.
	 * @return The newly created h2 element.
	 */
	public static Element h2(String title){
		return element(NS, "h2", text(title));
	}
	
	public static Element h2(Element... children){
		return element(NS, "h2", children);
	}
	
	/**
	 * Creates an HTML table element.
	 * 
	 * @param children The children of the table element.
	 * @return The table.
	 */
	public static Element table(Element... children){
		return element(NS, "table", children);
	}
	
	/**
	 * Creates an HTML table row (tr) element.
	 *  
	 * @param children The children of the tr.
	 * @return The tr.
	 */
	public static Element tr(Element... children){
		return element(NS, "tr", children);
	}
	
	/**
	 * Creates an HTML table data cell (td) element.
	 * 
	 * @param children The children of the td.
	 * @return The td.
	 */
	public static Element td(Element... children){
		return element(NS, "td", children);
	}
	
	/**
	 * Creates an HTML table data cell (td) element.
	 * 
	 * @param text The text to be contained within the td.
	 * @return The td.
	 */
	public static Element td(String text){
		return td(text(text));
	}
	
	/**
	 * Creates an HTML table header cell (th) element.
	 * 
	 * @param title The text to be contained within the th.
	 * @return The th.
	 */
	public static Element th(String title){
		return element(NS, "th", text(title));
	}
	
	
	/**
	 * Creates an HTML anchor element for linking to another hypertext document.
	 * 
	 * @param href The url to link to.
	 * @param children The children of the a.
	 * @return The a.
	 */
	public static Element a(String href, Element... children){
		return element(NS, "a", children).attribute("href", href);
	}
	
	/**
	 * Creates an HTML anchor element for linking to another hypertext document.
	 * 
	 * @param href The url to link to.
	 * @param description The descriptive text to use with the link.
	 * @return The a.
	 */
	public static Element a(String href, String description){
		return a(href, text(description));
	}

	/**
	 * Creates an HTML unordered list (ul) element.
	 * 
	 * @param children The children of the ul.
	 * @return The ul.
	 */
	public static Element ul(Element... children){
		return element(NS, "ul", children);
	}
	
	/**
	 * Creates an HTML list item (li) element.
	 * 
	 * @param text The text contained within the li.
	 * @return The li.
	 */
	public static Element li(String text){
		return element(NS, "li", text(text));
	}
}
