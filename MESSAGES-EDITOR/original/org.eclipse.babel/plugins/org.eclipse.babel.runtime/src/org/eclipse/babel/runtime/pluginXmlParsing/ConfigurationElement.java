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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.xml.sax.Attributes;


/**
 * An object which represents the user-defined contents of an extension
 * in a plug-in manifest.
 */
public class ConfigurationElement implements IObjectBuiltFromElement {

	/**
	 * The name of the configuration element
	 */
	private String name;

	private Map<String, String> attributes = new HashMap<String, String>();

	private String configurationElementValue = null;
	
	ArrayList<ConfigurationElement> children = new ArrayList<ConfigurationElement>();
	
	protected ConfigurationElement(String elementName, Attributes attributes) {
		this.name = elementName;
		for (int i = 0; i < attributes.getLength(); i++) {
			this.attributes.put(attributes.getLocalName(i), attributes.getValue(i));
		}
	}

	protected String getValue() {
		return configurationElementValue;
	}

	public String getAttribute(String attrName) {
		return attributes.get(attrName);
	}

	protected String getName() {
		return name;
	}

	public void characters(char[] ch, int start, int length) {
		// Accept character data within an element, is when it is
		// part of a configuration element (i.e. an element within an EXTENSION element
		String value = new String(ch, start, length);
		if (configurationElementValue == null) {
			if (value.trim().length() != 0) {
				configurationElementValue = value;
			}
		} else {
			configurationElementValue = configurationElementValue + value;
		}
	}

	public void endElement(String uri, String elementName, String name) {
		// Now finish up the configuration element object
		if (configurationElementValue != null) {
			configurationElementValue = configurationElementValue.trim();
		}
	}

	public IObjectBuiltFromElement startElement(String uri, String elementName,
			String name, Attributes attributes) {
		/*
		 * Configuration elements may be nested.  For example, actions
		 * inside action sets.
		 */
		ConfigurationElement configurationElement = new ConfigurationElement(elementName, attributes);
		children.add(configurationElement);
		return configurationElement;
	}

	public ConfigurationElement findSubConfigurationElement(
			String subElementName, String configurationElementId) {
		for (ConfigurationElement element: children) {
			if (element.getName().equals(subElementName)) {
				if( element.getAttribute("id").equals(configurationElementId)) { //$NON-NLS-1$
					return element; 
				}
			}
		}
		return null;
	}

	public ConfigurationElement findSubConfigurationElement(
			String subElementName, String subElementName2, String configurationElementId) {
		for (ConfigurationElement element: children) {
			if (element.getName().equals(subElementName)) {
				ConfigurationElement element2 = element.findSubConfigurationElement(subElementName2, configurationElementId);
				if (element2 != null) {
					return element2;
				}
			}
		}
		return null;
	}
}
