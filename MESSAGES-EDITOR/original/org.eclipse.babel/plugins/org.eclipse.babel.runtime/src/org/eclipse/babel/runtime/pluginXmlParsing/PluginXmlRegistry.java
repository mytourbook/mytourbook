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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.babel.runtime.external.ITranslatableText;
import org.eclipse.babel.runtime.external.NonTranslatableText;
import org.eclipse.babel.runtime.external.TranslatableResourceBundle;
import org.eclipse.babel.runtime.external.TranslatableText;
import org.eclipse.core.internal.registry.osgi.EclipseBundleListener;
import org.eclipse.core.internal.runtime.Activator;
import org.eclipse.core.internal.runtime.DevClassPathHelper;
import org.eclipse.core.internal.runtime.ResourceTranslator;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class PluginXmlRegistry {

	private static PluginXmlRegistry theInstance = new PluginXmlRegistry();

	private Map<Bundle, LocalizableContribution> registry = new HashMap<Bundle, LocalizableContribution>();

	private SAXParserFactory theXMLParserFactory = null;
	
	public static PluginXmlRegistry getInstance() {
		return theInstance;
	}

	// Helper method
	static ITranslatableText translate(TranslatableResourceBundle translationBundle, String originalText) {
		String trimmedText = originalText.trim();
		if (trimmedText.length() == 0)
			return new NonTranslatableText(trimmedText);
		if (trimmedText.charAt(0) != '%')
			return new NonTranslatableText(trimmedText);
		return new TranslatableText(translationBundle, trimmedText.substring(1));
	}

	
	
	/**
	 * This method is copied from EclipseBundleListener.addBundle.
	 * 
	 * @param osgiBundle
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 */
	public LocalizableContribution getLocalizableContribution(Bundle osgiBundle) throws ParserConfigurationException, SAXException, IOException {
		LocalizableContribution contribution = registry.get(osgiBundle);

		// if the given bundle already exists in the registry then return.
		if (contribution != null) {
			return contribution;
		}
		
		/*
		 * This is not really the right place for this, but
		 * we create an updatable resource bundle here.
		 * The reason this is not the right place is that the
		 * updatable bundle should be used by the Eclipse framework,
		 * but I don't know how to do that without changing the
		 * core Eclipse code.
		 */
		String bundleResourceFile = (String)osgiBundle.getHeaders().get(Constants.BUNDLE_LOCALIZATION);
		if (bundleResourceFile == null) {
			bundleResourceFile = Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME;
		}

		// TODO: Fragments etc. are not supported here.

		
		String[] nlVarients = buildNLVariants(Locale.getDefault().toString());
		PropertyResourceBundle resourceBundle = null;
		for (int i = nlVarients.length - 1; i >= 0; i--) {
			String fullPath = bundleResourceFile + ".properties" + (nlVarients[i].equals("") ? nlVarients[i] : '_' + nlVarients[i]); //$NON-NLS-1$ //$NON-NLS-2$
			URL varientURL = osgiBundle.getEntry(bundleResourceFile + ".properties" + (nlVarients[i].equals("") ? nlVarients[i] : '_' + nlVarients[i])); //$NON-NLS-1$ //$NON-NLS-2$
			if (varientURL == null) {
				break;
			}
				InputStream resourceStream = null;
				try {
					resourceStream = varientURL.openStream();
					resourceBundle = new MyPropertyResourceBundle(resourceStream, resourceBundle);
				} catch (IOException e) {
					// ignore and continue
				} finally {
					if (resourceStream != null) {
						try {
							resourceStream.close();
						} catch (IOException e3) {
							//Ignore exception
						}
					}
				}

		}
		
		
		
		TranslatableResourceBundle translationBundle = null;
		try {
			ResourceBundle immutableTranslationBundle = ResourceTranslator.getResourceBundle(osgiBundle);

/* This code works with Java 6 only			
			translationBundle = (TranslatableResourceBundle)ResourceBundle.getBundle(bundleResourceFile,
					Locale.getDefault(),
					createTempClassloader(osgiBundle),
		  	          new UpdatableResourceControl(Platform.getStateLocation(bundle)));
			TranslatableResourceBundle.register(translationBundle, osgiBundle);
*/
			translationBundle = TranslatableResourceBundle.get(osgiBundle, createTempClassloader(osgiBundle), bundleResourceFile);
			
				
		} catch (MissingResourceException e) {
			//Ignore the exception
		}

		contribution = new LocalizableContribution(osgiBundle.getSymbolicName(), translationBundle);
		registry.put(osgiBundle, contribution);
		
   		URL pluginManifest = EclipseBundleListener.getExtensionURL(osgiBundle, false);
		if (pluginManifest == null)
			return contribution;
		
		InputStream is;
		try {
			is = new BufferedInputStream(pluginManifest.openStream());
		} catch (IOException ex) {
			is = null;
		}
		if (is == null)
			return contribution;
		ExtensionsParser parser = new ExtensionsParser(this);
				
				try {
					parser.parseManifest(getXMLParser(), new InputSource(is), contribution, translationBundle);
				} finally {
					try {
						is.close();
					} catch (IOException ioe) {
						// nothing to do
					}
				}

		return contribution;
	}

	/**
	 * Returns the parser used by the registry to parse descriptions of extension points and extensions.
	 * This method must not return <code>null</code>.
	 *
	 * Copied from RegistryStrategy.getXMLParser
	 */
	public SAXParserFactory getXMLParser() {
		if (theXMLParserFactory == null)
			theXMLParserFactory = SAXParserFactory.newInstance();
		return theXMLParserFactory;
	}
	
// These methods are copied from ManifestLocalization in the OSGi project.

	private String[] buildNLVariants(String nl) {
		ArrayList<String> result = new ArrayList<String>();
		int lastSeparator;
		while ((lastSeparator = nl.lastIndexOf('_')) != -1) {
			result.add(nl);
			if (lastSeparator != -1) {
				nl = nl.substring(0, lastSeparator);
			}
		}
		result.add(nl);
		// always add the default locale string
		result.add(""); //$NON-NLS-1$
		return (String[]) result.toArray(new String[result.size()]);
	}


	private static ClassLoader createTempClassloader(Bundle b) {
		ArrayList<URL> classpath = new ArrayList<URL>();
		addClasspathEntries(b, classpath);
		addBundleRoot(b, classpath);
		addDevEntries(b, classpath);
		addFragments(b, classpath);
		URL[] urls = new URL[classpath.size()];
		return new URLClassLoader(classpath.toArray(urls));
	}

	private static void addFragments(Bundle host, ArrayList<URL> classpath) {
		Activator activator = Activator.getDefault();
		if (activator == null)
			return;
		Bundle[] fragments = activator.getFragments(host);
		if (fragments == null)
			return;

		for (int i = 0; i < fragments.length; i++) {
			addClasspathEntries(fragments[i], classpath);
			addDevEntries(fragments[i], classpath);
		}
	}

	private static void addClasspathEntries(Bundle b, ArrayList<URL> classpath) {
		ManifestElement[] classpathElements;
		try {
			classpathElements = ManifestElement.parseHeader(Constants.BUNDLE_CLASSPATH, (String) b.getHeaders("").get(Constants.BUNDLE_CLASSPATH)); //$NON-NLS-1$
			if (classpathElements == null)
				return;
			for (int i = 0; i < classpathElements.length; i++) {
				URL classpathEntry = b.getEntry(classpathElements[i].getValue());
				if (classpathEntry != null)
					classpath.add(classpathEntry);
			}
		} catch (BundleException e) {
			//ignore
		}
	}

	private static void addBundleRoot(Bundle b, ArrayList<URL> classpath) {
		classpath.add(b.getEntry("/")); //$NON-NLS-1$
	}

	private static void addDevEntries(Bundle b, ArrayList<URL> classpath) {
		if (!DevClassPathHelper.inDevelopmentMode())
			return;

		String[] binaryPaths = DevClassPathHelper.getDevClassPath(b.getSymbolicName());
		for (int i = 0; i < binaryPaths.length; i++) {
			URL classpathEntry = b.getEntry(binaryPaths[i]);
			if (classpathEntry != null)
				classpath.add(classpathEntry);
		}
	}
}
