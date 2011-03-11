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
import java.util.Collection;
import java.util.List;

import org.eclipse.babel.runtime.external.ITranslatableText;
import org.eclipse.babel.runtime.external.TranslatableResourceBundle;
import org.xml.sax.Attributes;

public class LocalizableContribution implements IObjectBuiltFromElement {

	private static final String EXTENSION = "extension"; //$NON-NLS-1$
	private static final String EXTENSION_NAME = "name"; //$NON-NLS-1$
	private static final String EXTENSION_ID = "id"; //$NON-NLS-1$
	private static final String EXTENSION_TARGET = "point"; //$NON-NLS-1$

	private String defaultNamespace = null;

	private TranslatableResourceBundle translationBundle;
	
	private List<LocalizableExtension> children = new ArrayList<LocalizableExtension>();

	protected LocalizableContribution(String defaultNamespace, TranslatableResourceBundle translationBundle) {
		this.defaultNamespace = defaultNamespace;
		this.translationBundle = translationBundle;
	}

	protected Collection<LocalizableExtension> getExtensions() {
		return children;
	}

	public String getDefaultNamespace() {
		return defaultNamespace;
	}

	/**
	 * This method looks for the original text in the plugin.xml file that
	 * matches the passed criteria. The original text may be of the format '%'
	 * followed by the resource bundle key.
	 * 
	 * @param extensionPointId
	 *            the id of the extension point whose extensions will be
	 *            searched
	 * @param elementName
	 *            the element name of the configuration elements within the
	 *            extensions that are to be examined
	 * @param configurationElementId
	 *            the id of the required configuration element, the id being
	 *            assumed to have been specified by the 'id' attribute of the
	 *            configuration element
	 * @param attributeName
	 *            the name of the attribute which contains the required
	 *            localizable text
	 * @return an appropriate ITranslatableText implementation, being a
	 *         TranslatableText object if the original text started with a
	 *         percent character and being a NonTranslatableText object if the
	 *         text did not start with the percent character
	 */
	public ITranslatableText getLocalizableText(String extensionPointId, String elementName, String configurationElementId, String attributeName) {
		ConfigurationElement element = null;
		for (LocalizableExtension extension : children) {
			if (extension.getExtensionPointIdentifier().equals(extensionPointId)) {
				element = extension.findConfigurationElement(elementName, configurationElementId);
				if (element != null) {
					break;
				}
			}
		}
		
		if (element == null) {
			// Something is wrong.
			return null;
		}
		
		String originalText = element.getAttribute(attributeName);
		
		return PluginXmlRegistry.translate(translationBundle, originalText);
	}

	public ITranslatableText getLocalizableText(String extensionPointId, String elementName, String subElementName, String configurationElementId, String attributeName) {
		ConfigurationElement element = null;
		for (LocalizableExtension extension : children) {
			if (extension.getExtensionPointIdentifier().equals(extensionPointId)) {
				element = extension.findConfigurationElement(elementName, subElementName, configurationElementId);
				if (element != null) {
					break;
				}
			}
		}
		
		if (element == null) {
			// Something is wrong.
			return null;
		}
		
		String originalText = element.getAttribute(attributeName);
		
		return PluginXmlRegistry.translate(translationBundle, originalText);
	}

	public void characters(char[] ch, int start, int length) {
		// Nothing to do
	}

	public void endElement(String uri, String elementName, String name) {
		// Nothing to do
	}

	public IObjectBuiltFromElement startElement(String uri, String elementName, String name,
			Attributes attributes) {
		if (elementName.equals(EXTENSION)) {
			LocalizableExtension currentExtension = new LocalizableExtension(name, name, attributes);
			children.add(currentExtension);
			
			String simpleId = null;
			String namespaceName = null;
			String label = null;
			
			// Process Attributes
			int len = (attributes != null) ? attributes.getLength() : 0;
			for (int i = 0; i < len; i++) {
				String attrName = attributes.getLocalName(i);
				String attrValue = attributes.getValue(i).trim();

				if (attrName.equals(EXTENSION_NAME))
					currentExtension.setLabel(attrValue);// was translated
				else if (attrName.equals(EXTENSION_ID)) {
					int simpleIdStart = attrValue.lastIndexOf('.');
					if ((simpleIdStart != -1)) {
						simpleId = attrValue.substring(simpleIdStart + 1);
						namespaceName = attrValue.substring(0, simpleIdStart);
					} else {
						simpleId = attrValue;
						namespaceName = defaultNamespace;
					}
					currentExtension.setSimpleIdentifier(simpleId);
					currentExtension.setNamespaceIdentifier(namespaceName);
				} else if (attrName.equals(EXTENSION_TARGET)) {
					// check if point is specified as a simple or qualified name
					String targetName;
					if (attrValue.lastIndexOf('.') == -1) {
						String baseId = defaultNamespace;
						targetName = baseId + '.' + attrValue;
					} else
						targetName = attrValue;
					currentExtension.setExtensionPointIdentifier(targetName);
				}
			}
			if (currentExtension.getExtensionPointIdentifier() == null) {
				// Missing attribute, ignore extension
				return new IgnoredElement();
			}

			// TODO:
			currentExtension.setLabel(label);
			currentExtension.setSimpleIdentifier(simpleId);
			currentExtension.setNamespaceIdentifier(namespaceName);
			return currentExtension;
		} else {
		/*
		 * If the element name is anything other than an extension (whether an
		 * extension point or something unrecognized), we ignore it.
		 */
			return new IgnoredElement();
		}
	}

	public String getValue(String extensionPointId, String elementName, String subElementName, String subElementName2, String configurationElementId, String attributeName) {
		ConfigurationElement element = null;
		for (LocalizableExtension extension : children) {
			if (extension.getExtensionPointIdentifier().equals(extensionPointId)) {
				element = extension.findConfigurationElement(elementName, subElementName, subElementName2, configurationElementId);
				if (element != null) {
					break;
				}
			}
		}
		
		if (element == null) {
			// Something is wrong.
			return null;
		}
		
		return element.getAttribute(attributeName);
	}
}
