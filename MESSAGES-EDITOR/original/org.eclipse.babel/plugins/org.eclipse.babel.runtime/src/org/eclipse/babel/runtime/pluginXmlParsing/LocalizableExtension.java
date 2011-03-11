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

import org.xml.sax.Attributes;

/**
 * An object which represents the user-defined extension in a plug-in manifest.  
 */
public class LocalizableExtension implements IObjectBuiltFromElement {

	//Extension simple identifier
	private String simpleId;
	//The namespace for the extension. 
	private String namespaceIdentifier;

	ArrayList<ConfigurationElement> children = new ArrayList<ConfigurationElement>();
	
	String label;
	String extensionPointName;
	Attributes attributes;
	
	protected LocalizableExtension(String simpleId, String namespace, Attributes attributes) {
		this.simpleId = simpleId;
		this.namespaceIdentifier = namespace;
		this.attributes = attributes;
	}

	protected String getExtensionPointIdentifier() {
		return this.extensionPointName;
	}

	protected String getSimpleIdentifier() {
		return simpleId;
	}

	protected String getUniqueIdentifier() {
		return simpleId == null ? null : this.getNamespaceIdentifier() + '.' + simpleId;
	}

	void setExtensionPointIdentifier(String value) {
		this.extensionPointName = value;
	}

	void setSimpleIdentifier(String value) {
		simpleId = value;
	}

	String getLabel() {
		return label;
		
	}

	void setLabel(String value) {
		label = value;
	}

	public String getNamespaceIdentifier() {
		return namespaceIdentifier;
	}

	void setNamespaceIdentifier(String value) {
		namespaceIdentifier = value;
	}

	public String toString() {
		return getUniqueIdentifier() + " -> " + getExtensionPointIdentifier(); //$NON-NLS-1$
	}

	/**
	 * This method looks for a configuration element with a given element
	 * name and id.
	 * 
	 * @param elementName
	 *            the element name of the configuration elements this extension
	 *            that are to be examined
	 * @param configurationElementId
	 *            the id of the required configuration element, the id being
	 *            assumed to have been specified by the 'id' attribute of the
	 *            configuration element
	 * @return the configuration element, or null if no element with the given
	 * 			element name and id exists in the extension
	 */
	public ConfigurationElement findConfigurationElement(String elementName, String configurationElementId) {
		for (ConfigurationElement element: children) {
			if (element.getName().equals(elementName)) {
				String x = element.getAttribute("id"); //$NON-NLS-1$
				
					if( element.getAttribute("id").equals(configurationElementId)) { //$NON-NLS-1$
				return element; }
			}
		}
		return null;
	}

	public ConfigurationElement findConfigurationElement(String elementName, String subElementName, String configurationElementId) {
		for (ConfigurationElement element: children) {
			if (element.getName().equals(elementName)) {
				ConfigurationElement subElement = element.findSubConfigurationElement(subElementName, configurationElementId);
				if (subElement != null) {
					return subElement;
				}
			}
		}
		return null;
	}

	public ConfigurationElement findConfigurationElement(String elementName, String subElementName, String subElementName2, String configurationElementId) {
		for (ConfigurationElement element: children) {
			if (element.getName().equals(elementName)) {
				ConfigurationElement subElement = element.findSubConfigurationElement(subElementName, subElementName2, configurationElementId);
				if (subElement != null) {
					return subElement;
				}
			}
		}
		return null;
	}

	public void characters(char[] ch, int start, int length) {
		// Ignore content
	}

	public void endElement(String uri, String elementName, String name) {
		// Finish up extension object
	}

	public IObjectBuiltFromElement startElement(String uri, String elementName, String name, Attributes attributes) {
		ConfigurationElement configurationElement = new ConfigurationElement(elementName, attributes);
		children.add(configurationElement);
		return configurationElement;
	}
}
