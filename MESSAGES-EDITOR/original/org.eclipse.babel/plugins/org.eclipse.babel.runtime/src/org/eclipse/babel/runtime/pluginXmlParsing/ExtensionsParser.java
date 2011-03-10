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

import java.io.IOException;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.internal.registry.RegistryMessages;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class ExtensionsParser extends DefaultHandler {
	static final String[] EMPTY_STRING_ARRAY = new String[0];

	/*
	 * File name for this extension manifest This to help with error reporting
	 */
	private String locationName = null;

	/**
	 * Current object stack (used to hold the current object we are populating
	 * in this plugin descriptor
	 */
	private Stack<IObjectBuiltFromElement> objectStack = new Stack<IObjectBuiltFromElement>();

	// Owning extension registry
//	private PluginXmlRegistry registry;

	// Resource bundle used to translate the content of the plugin.xml
	protected ResourceBundle resources;

	private LocalizableContribution contribution;

	//This keeps tracks of the value of the configuration element in case the value comes in several pieces (see characters()). See as well bug 75592. 
//	private String configurationElementValue;

	/** 
	 * Status code constant (value 1) indicating a problem in a bundle extensions
	 * manifest (<code>extensions.xml</code>) file.
	 */
	public static final int PARSE_PROBLEM = 1;

	// Keep a group of vectors as a temporary scratch space.  This
	// vectors will be used to populate arrays in the bundle model
	// once processing of the XML file is complete.
	private ArrayList<LocalizableExtension> extensions = new ArrayList<LocalizableExtension>();

	private ArrayList processedExtensionIds = null;

	public ExtensionsParser(PluginXmlRegistry registry) {
		super();
//		this.registry = registry;
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
	 */
	public void characters(char[] ch, int start, int length) {
		objectStack.peek().characters(ch, start, length);
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#endDocument()
	 */
	public void endDocument() {
		// do nothing
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	public void endElement(String uri, String elementName, String qName) {
		objectStack.pop().endElement(uri, elementName, qName);
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#error(org.xml.sax.SAXParseException)
	 */
	public void error(SAXParseException ex) {
		logStatus(ex);
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#fatalError(org.xml.sax.SAXParseException)
	 */
	public void fatalError(SAXParseException ex) throws SAXException {
		logStatus(ex);
		throw ex;
	}

	private void logStatus(SAXParseException ex) {
		String name = ex.getSystemId();
		if (name == null)
			name = locationName;
		if (name == null)
			name = ""; //$NON-NLS-1$
		else
			name = name.substring(1 + name.lastIndexOf("/")); //$NON-NLS-1$

		String msg;
		if (name.equals("")) //$NON-NLS-1$
			msg = NLS.bind(RegistryMessages.parse_error, ex.getMessage());
		else
			msg = NLS.bind(RegistryMessages.parse_errorNameLineColumn, (new Object[] {name, Integer.toString(ex.getLineNumber()), Integer.toString(ex.getColumnNumber()), ex.getMessage()}));
		error(new Status(IStatus.WARNING, RegistryMessages.OWNER_NAME, PARSE_PROBLEM, msg, ex));
	}

	public LocalizableContribution parseManifest(SAXParserFactory factory, InputSource in, LocalizableContribution currentContribution, ResourceBundle bundle) throws ParserConfigurationException, SAXException, IOException {
		long start = 0;
		this.resources = bundle;
		//initialize the parser with this object
		contribution = currentContribution;

		if (factory == null)
			throw new SAXException(RegistryMessages.parse_xmlParserNotAvailable);

		locationName = in.getSystemId();
		if (locationName == null)
			locationName = contribution.getDefaultNamespace();

		factory.setNamespaceAware(true);
		try {
			factory.setFeature("http://xml.org/sax/features/string-interning", true); //$NON-NLS-1$
		} catch (SAXException se) {
			// ignore; we can still operate without string-interning
		}
		factory.setValidating(false);
		factory.newSAXParser().parse(in, this);
		return contribution;
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#startDocument()
	 */
	public void startDocument() {
		objectStack.push(new InitialState(contribution));
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	public void startElement(String uri, String elementName, String qName, Attributes attributes) {
		objectStack.push(objectStack.peek().startElement(uri, elementName, qName, attributes));
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#warning(org.xml.sax.SAXParseException)
	 */
	public void warning(SAXParseException ex) {
		logStatus(ex);
	}

//	private void internalError(String message) {
//		error(new Status(IStatus.WARNING, RegistryMessages.OWNER_NAME, PARSE_PROBLEM, message, null));
//	}

	/**
	 * Handles an error state specified by the status.  The collection of all logged status
	 * objects can be accessed using <code>getStatus()</code>.
	 *
	 * @param error a status detailing the error condition
	 */
	public void error(IStatus error) {
//		status.add(error);
	}

}
