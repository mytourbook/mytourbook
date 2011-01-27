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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;
import java.util.Set;

import org.osgi.framework.Bundle;

public class TranslatableResourceBundle extends ResourceBundle {
	private static final String EXTENSION = ".properties"; //$NON-NLS-1$
	private static String[] nlSuffixes;
	
	private static Map<Bundle, Collection<TranslatableResourceBundle>> allUpdatableBundles = new HashMap<Bundle, Collection<TranslatableResourceBundle>>();

	private Bundle osgiBundle;
	private String description;
	private TranslatableResourceFile variantResources;

	/**
	 * For some extraordinary reason, the base version of locale and its accessors is private,
	 * so we have to create our own.
	 */
	private Locale myLocale;
	
	public TranslatableResourceBundle(String description, TranslatableResourceFile variantResources, Locale locale, Bundle osgiBundle) {
		this.description = description;
		this.variantResources = variantResources;
		this.myLocale = locale;
		this.osgiBundle = osgiBundle;
	}

	/**
	 * This method always returns the Locale, unlike
	 * the base method getLocale which return the Locale
	 * only under certain circumstances and at other times
	 * returns null.
	 */
	public Locale getMyLocale() {
		return myLocale;
	}
	
	@Override
	protected Object handleGetObject(String key) {
		return variantResources.getValue(key);
	}

	@Override
	public Enumeration<String> getKeys() {
		/*
		 * updatedProperties can only contain keys that are also contained in
		 * readOnlyProperties or parents (fallback bundles) of this bundle.
		 * Therefore we don't need to look at the updatedProperties.
		 */
        return new ConcatenatedEnumeration(variantResources.getKeys(),
                (parent != null) ? parent.getKeys() : null);
	}

	public void save() {
		variantResources.save();
		
		// TODO: How is this used?  Should we be saving the parent changes?
		if (parent != null) {
			((TranslatableResourceBundle)parent).save();
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
	public String getStringWithoutFallback(String key) {
		return (String)handleGetObject(key);
	}

	/**
	 * Set the value of the message for the given key in this resource bundle.
	 * 
	 * @param key
	 * @param newValue
	 */
	public void setString(String key, String newValue) {
		variantResources.setString(key, newValue);
	}

	/**
	 * Reverts the value of the message for the given key in this resource bundle
	 * back to the value as distributed in the bundle jar.  I.e. the value is removed
	 * from the delta.
	 * 
	 * @param key
	 */
	public void revertString(String key) {
		variantResources.revertString(key);
	}

	/**
	 * @return true if the text for the given key has been modified,
	 * 		false if the current text is the same as the text in the
	 * 		properties file
	 */
	public boolean isDirty(String key) {
		return variantResources.isDirty(key);
	}

	public TranslatableResourceBundle getParent() {
		return (TranslatableResourceBundle)parent;
	}

	/**
	 * If a plug-in uses a resource bundle of this class then it is a good
	 * idea to register it using this method.  The bundle will then be known
	 * to the localization dialog.
	 * 
	 * @param resourceBundle
	 * @param currencyPagePlugin 
	 */
	public static void register(TranslatableResourceBundle resourceBundle, Bundle osgiBundle) {
		Collection<TranslatableResourceBundle> resourceBundles = allUpdatableBundles.get(osgiBundle);
		if (resourceBundles == null) {
			resourceBundles = new ArrayList<TranslatableResourceBundle>();
			allUpdatableBundles.put(osgiBundle, resourceBundles);
		}
		resourceBundles.add(resourceBundle);
		resourceBundle.osgiBundle = osgiBundle;
	}

	public static void register(Bundle bundle, String resourceBundleName) {
		// TODO Auto-generated method stub
		
	}

	public static Map<Bundle, Collection<TranslatableResourceBundle>> getAllResourceBundles() {
		return allUpdatableBundles;
	}

	public Bundle getOsgiBundle() {
		return osgiBundle;
	}

	public String getDescription() {
		return description;
	}

	public static TranslatableResourceBundle get(Bundle osgiBundle, ClassLoader loader, String bundleResourceFile) {
		String [] variants = buildVariants(bundleResourceFile);
		
		Locale nl = Locale.getDefault();
		List<Locale> locales = new ArrayList<Locale>(); 
		locales.add(new Locale("", "", ""));
		if (nl.getLanguage().length() != 0) {
			locales.add(0, new Locale(nl.getLanguage(), "", ""));
			if (nl.getCountry().length() != 0) {
				locales.add(0, new Locale(nl.getLanguage(), nl.getCountry(), ""));
				if (nl.getVariant().length() != 0) {
					locales.add(0, new Locale(nl.getLanguage(), nl.getCountry(), nl.getVariant()));
				}
			}
		}
			
		TranslatableResourceBundle lastBundle = null;
		for (int i = variants.length-1; i >= 0; i--) {
			TranslatableResourceFile variantResources = TranslatableResourceFile.get(osgiBundle, loader, variants[i]);
			TranslatableResourceBundle resourceBundle = new TranslatableResourceBundle(bundleResourceFile, variantResources, locales.get(i), osgiBundle);
			resourceBundle.setParent(lastBundle);
			lastBundle = resourceBundle;
		}
		return lastBundle;
	}

	/*
	 * Build an array of property files to search.  The returned array contains
	 * the property fields in order from most specific to most generic.
	 * So, in the FR_fr locale, it will return file_fr_FR.properties, then
	 * file_fr.properties, and finally file.properties.
	 * 
	 * This is an exact copy of the private method in NLS.
	 */
	private static String[] buildVariants(String root) {
		if (nlSuffixes == null) {
			//build list of suffixes for loading resource bundles
			String nl = Locale.getDefault().toString();
			ArrayList<String> result = new ArrayList<String>(4);
			int lastSeparator;
			while (true) {
				result.add('_' + nl + EXTENSION);
				lastSeparator = nl.lastIndexOf('_');
				if (lastSeparator == -1)
					break;
				nl = nl.substring(0, lastSeparator);
			}
			//add the empty suffix last (most general)
			result.add(EXTENSION);
			nlSuffixes = (String[]) result.toArray(new String[result.size()]);
		}
		root = root.replace('.', '/');
		String[] variants = new String[nlSuffixes.length];
		for (int i = 0; i < variants.length; i++)
			variants[i] = root + nlSuffixes[i];
		return variants;
	}

	/**
	 * Enumeration that returns a set formed by taking all the elements in the first
	 * given set and then also returning anything in the enumeration that is not
	 * in the first given set.
	 */
	class ConcatenatedEnumeration implements Enumeration<String> {

	    private Set<String> firstSet;
	    private Iterator<String> firstSetIter;
	    private Enumeration<String> extraEnumeration;

	    private String element = null;
        
	    /**
	     * @param firstSet set of elements to be returned
	     * @param extraEnumeration an enumeration that returns extra elements to be concatenated
	     * 				if they are not already in the set.
	     */
	    ConcatenatedEnumeration(Set<String> firstSet, Enumeration<String> extraEnumeration) {
	        this.firstSet = firstSet;
	        this.firstSetIter = firstSet.iterator();
	        this.extraEnumeration = extraEnumeration;
	    }

	    public boolean hasMoreElements() {
	    	if (element != null) {
	    		// this is a subsequent call, so return true again
	    		return true;
	    	}

	    	if (firstSetIter.hasNext()) {
	    		element = firstSetIter.next();
	    		return true;
	    	}
	    	
	    	if (extraEnumeration != null) {
	    		while (extraEnumeration.hasMoreElements()) {
	    			element = extraEnumeration.nextElement();
	    			if (!firstSet.contains(element)) {
	    				return true;
	    			}
	    		}
	    	}
	    	
    		return false;
	    }

	    public String nextElement() {
	    	if (!hasMoreElements()) {
	    		throw new NoSuchElementException();
	    	}
	    	String result = element;
	    	element = null;
	    	return result;
	    }
	}
}
