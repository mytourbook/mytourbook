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

package org.eclipse.babel.runtime.external;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.babel.runtime.Messages;
import org.eclipse.core.internal.runtime.RuntimeLog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;

/**
 * An object of this class handles the properties for a single bundle and locality.
 * 
 * There is no regular non-updatable equivalent to this class because a single call
 * to Properties.load would have done.  This class maintains the delta file, while also
 * accessing the fixed properties file.
 */
public class TranslatableResourceFile {
	private static final String NULL_VALUE_TEXT = "<<<null>>>";
	protected static final String VERSION_KEY = "eclipse.translations.version"; //$NON-NLS-1$
	protected static final String VERSION_VALUE = "1"; //$NON-NLS-1$

	static final String F_TRANSLATIONS_DATA = ".translations"; //$NON-NLS-1$
	
	static final int SEVERITY_ERROR = 0x04;

	private static Map<Bundle, Map<String, TranslatableResourceFile>> allResources = new HashMap<Bundle, Map<String, TranslatableResourceFile>>();
	
	private Properties readOnlyProperties;
	private IPath updatedPropertiesFile;
	private Properties updatedProperties;
	
	public TranslatableResourceFile(Properties readOnlyProperties, IPath updatedPropertiesFile) {
		this.readOnlyProperties = readOnlyProperties;
		this.updatedPropertiesFile = updatedPropertiesFile;
		
		load();
	}

	protected void load() {
		InputStream input = null;
		updatedProperties = new Properties();
		try {
			input = new BufferedInputStream(new FileInputStream(updatedPropertiesFile.toFile()));
			updatedProperties.load(input);
		} catch (FileNotFoundException e) {
			/*
			 * If the file does not exist then that means only that no messages
			 * have been updated. The file will be created if properties are
			 * updated.
			 */
			return;
		} catch (IOException e) {
			String message = TranslatableNLS.bind(Messages.exception_loadException, updatedPropertiesFile).getLocalizedText();
			RuntimeLog.log(new Status(IStatus.INFO, "org.eclipse.babel.runtime", IStatus.INFO, message, e));
			// Leave the updatedProperties empty and continue
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}

	protected Object handleGetObject(String key) {
		String value = updatedProperties.getProperty(key);
		if (value == null) {
			value = readOnlyProperties.getProperty(key);
		} else if (value.equals(NULL_VALUE_TEXT)) {
			value = null;
		}
		return value;
	}

	/**
	 * 
	 * Returns keys for resources that are modified in this variant.
	 * A key is returned if either a value is specified in the original
	 * properties file for this variant or if a value is provided in the
	 * delta file, however a key is NOT returned if a value was provided
	 * in the original properties file for this variant but the delta file
	 * erases that value (value set to "<<<null>>>").  
	 */
	public Set<String> getKeys() {
		Set<String> keys = new HashSet<String>();

		// Add all the keys from the fixed properties file
		for (Object key: readOnlyProperties.keySet()) {
			keys.add((String)key);
		}
		
		// Adjust the set according to the data in the delta file
		for (Entry<Object, Object> entry: updatedProperties.entrySet()) {
			if (NULL_VALUE_TEXT.equals(entry.getValue())) {
				keys.remove(entry.getKey());
			} else {
				keys.add((String)entry.getKey());
			}
		}
		
        return keys;
	}

	public void save() {
		if (updatedProperties.isEmpty()) {
			// nothing to save. delete existing file if one exists.
			if (updatedPropertiesFile.toFile().exists() && !updatedPropertiesFile.toFile().delete()) {
				String message = TranslatableNLS.bind(Messages.exception_failedDelete, updatedPropertiesFile).getLocalizedText();
				RuntimeLog.log(new Status(IStatus.WARNING, "org.eclipse.babel.runtime", IStatus.WARNING, message, null));
			}
			return;
		}

		updatedProperties.put(VERSION_KEY, VERSION_VALUE);
		OutputStream output = null;
		FileOutputStream fos = null;
		try {
			// create the parent dirs if they don't exist
			File parentFile = updatedPropertiesFile.toFile().getParentFile();
			if (parentFile == null)
				return;
			parentFile.mkdirs();
			// set append to be false so we overwrite current settings.
			fos = new FileOutputStream(updatedPropertiesFile.toOSString(), false);
			output = new BufferedOutputStream(fos);
			updatedProperties.store(output, null);
			output.flush();
			fos.getFD().sync();
		} catch (IOException e) {
			String message = TranslatableNLS.bind(Messages.exception_saveException, updatedPropertiesFile).getLocalizedText();
			RuntimeLog.log(new Status(IStatus.ERROR, "org.eclipse.babel.runtime", IStatus.ERROR, message, e));
		} finally {
			if (output != null)
				try {
					output.close();
				} catch (IOException e) {
					// ignore
				}
		}
	}

	/**
	 * Gets the string value for this key.  This method differs
	 * from getString because this method does not fall back to any other
	 * locale for a value.  For example, if the locale for this bundle
	 * is Canadian french and there is a value found in the french bundle
	 * but no value specifically in the Canadian french bundle then null
	 * will be returned.
	 * <P>
	 * This method is useful to callers that maintain localized strings.
	 * Callers that simply use the localized strings should use the getString
	 * method inherited from ResourceBundle.
	 * 
	 * @param key
	 * @return the value, or null if the key was not found in this bundle
	 */
	public Object getValue(String key) {
		return handleGetObject(key);
	}

	/**
	 * Set the value of the message for the given key in this resource bundle.
	 * 
	 * @param key
	 * @param newValue
	 */
	public void setString(String key, String newValue) {
		/*
		 * If the new value is identical to the read-only value then
		 * we remove any entry from the updated properties.
		 * Otherwise put this new value into the updated properties.
		 * 
		 * Note a slight different between the original property file and
		 * the delta property file.  In the original property file, null
		 * (property not set) means look to the parent locale for a value.
		 * In the delta, null means there is no change in the delta so
		 * the value from the original properties file for this locale should
		 * be used.  If the delta wants to indicate that the parent locale's
		 * text should be used then the property should be set to "<<<null>>>".
		 * That means the value in the original property file for this locale
		 * is ignored and the value from the parent locale is to be used.
		 */
		String readOnlyValue = readOnlyProperties.getProperty(key);
		if (equals(newValue, readOnlyValue)) {
			updatedProperties.remove(key);
		} else {
			updatedProperties.setProperty(key, newValue==null ? NULL_VALUE_TEXT : newValue);
		}
	}

	/**
	 * Reverts the value of the message for the given key in this resource bundle
	 * back to the value as distributed in the bundle jar.  I.e. the value is removed
	 * from the delta.
	 * 
	 * @param key
	 */
	public void revertString(String key) {
		updatedProperties.remove(key);
	}

	/**
	 * @return true if the text for the given key has been modified,
	 * 		false if the current text is the same as the text in the
	 * 		properties file
	 */
	public boolean isDirty(String key) {
		return updatedProperties.containsKey(key);
	}

	private boolean equals(String text1, String text2) {
		return (text1 == null) 
		? (text2 == null)
				: text1.equals(text2);
	}

	public static TranslatableResourceFile get(Bundle osgiBundle, ClassLoader loader, String variant) {
		/*
		 * Look to see if we already have a one in our map.  We don't want to create
		 * two that are the same because then they will not see each other's changes.
		 * Even worse, they may overwrite each other's changes.
		 * 
		 * We use the plugin and the variant name to determine if two resource bundles
		 * are the same.
		 */
		Map<String, TranslatableResourceFile> pluginResources = allResources.get(osgiBundle);
		if (pluginResources == null) {
			pluginResources = new HashMap<String, TranslatableResourceFile>();
			allResources.put(osgiBundle, pluginResources);
		}
		TranslatableResourceFile variantResources = pluginResources.get(variant);
		if (variantResources == null) {
			// loader==null if we're launched off the Java boot classpath
			final InputStream stream = loader==null ? ClassLoader.getSystemResourceAsStream(variant) : loader.getResourceAsStream(variant);
            Properties readOnlyProperties = new Properties();
            if (stream != null) {
            	BufferedInputStream bis = new BufferedInputStream(stream);
            	try {
            		readOnlyProperties.load(bis);
            		bis.close();
            	} catch (IOException e) {
            		TranslatableNLS.log(SEVERITY_ERROR, "Error loading " + variant, e); //$NON-NLS-1$
            	} finally {
            		try {
            			if (bis != null)
            				bis.close();
            			if (stream != null)
            				stream.close();
            		} catch (IOException e) {
            			// ignore
            		}
            	}
            }

			IPath updatedPropertiesFile = Platform.getStateLocation(osgiBundle).append(F_TRANSLATIONS_DATA).append(variant+".properties"); //$NON-NLS-1$
			
			variantResources = new TranslatableResourceFile(readOnlyProperties, updatedPropertiesFile);
			pluginResources.put(variant, variantResources);
		}
 		
		
		// TODO: Make this a weak reference map.
		
		return variantResources;
	}
}
