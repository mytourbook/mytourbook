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

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

/**
 * Loads an parses the XMLSchemas used to define the format of the coverage reports and saved configurations.
 */
public class Schemas {
	
	/**
	 * The schema which defines the format of the XML coverage reports.
	 */
	public static final Schema COVERAGE = loadSchema("/azure/build/core/xml/coverage.xsd");
	
	/**
	 * The schema which defines the format of saved build configurations.
	 */
	public static final Schema CONFIG = loadSchema("/azure/build/core/xml/config.xsd");

	private static Schema loadSchema(String schema) {
		try {
			return SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI
			).newSchema(Schema.class.getResource(schema));
		} catch (SAXException e) {
			throw new AssertionError("Cannot load schema: (" + schema + ")");
		} finally {
			if(schema == null){
				throw new AssertionError("Cannot load schema: (" + schema + ")");
			}
		}
	}
	
}
