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
package org.eclipse.babel.build.core.translationcatalogue;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.babel.build.core.LocaleProxy;
import org.eclipse.babel.build.core.Messages;
import org.eclipse.babel.build.core.PluginProxy;
import org.eclipse.babel.build.core.ResourceProxy;
import org.eclipse.babel.build.core.exceptions.InvalidLocationException;


public class TranslationCatalogueSimpleParser implements
		TranslationCatalogueParser {

	private File rootDirectory;
	private Set<LocaleProxy> locales;
	
	public TranslationCatalogueSimpleParser(File rootDirectory, Set<LocaleProxy> locales) {
		this.locales = locales;
		this.rootDirectory = rootDirectory;
	}

	/**
	 * 
	 * @param eclipseInstallPlugin
	 * @return The different versions (one for each locale) of the same plug-in within the catalogue.
	 */
	public Map<String, PluginProxy> getPluginForSpecifiedLocales (PluginProxy eclipseInstallPlugin) {
		Map<String, PluginProxy> plugins = new HashMap<String, PluginProxy>();
		
		for (LocaleProxy locale : this.locales) {
			PluginProxy plugin = getPluginForLocale(locale, eclipseInstallPlugin);
			if (plugin != null) {
				plugins.put(locale.getName(), plugin);
			}
		}
		return plugins;
	}
	
	/**
	 * 
	 * @param eclipseInstallPlugin
	 * @return The different versions (one for each locale) of the same feature within the catalogue.
	 */
	public Map<String, PluginProxy> getFeatureForSpecifiedLocales (PluginProxy eclipseInstallFeature) {
		Map<String, PluginProxy> features = new HashMap<String, PluginProxy>();
		
		for (LocaleProxy locale : this.locales) {
			PluginProxy feature = getFeatureForLocale(locale, eclipseInstallFeature);
			if ( feature != null ) {
				features.put( locale.getName(), feature );
			}
		}
	
		return features;
	}
	
	/**
	 * Assumes that the locale folder is directly under the translation catalogue's root folder.
	 * 
	 * @param locale
	 * @param eclipseInstallPlugin
	 * @return The eclipseInstallPlugin's corresponding plug-in in the catalogue for the specified locale.
	 */
	private PluginProxy getPluginForLocale(LocaleProxy locale, PluginProxy eclipseInstallPlugin) {
		
		for (File localeDir: rootDirectory.listFiles()) {					
			if ( localeDir.getName().equalsIgnoreCase(locale.getName()) ) {
				
				File pluginsRoot = new File(rootDirectory.getAbsolutePath() + 
						File.separatorChar + localeDir.getName() + 
						File.separatorChar + PLUGINS_PATH);
				
				File pluginFile = new File(pluginsRoot.getAbsolutePath() + File.separator + eclipseInstallPlugin.getName());
				
				if(pluginFile.exists()) {
					List<ResourceProxy> pluginResources = extractResources(pluginFile, pluginFile.getName());
					return new PluginProxy(pluginFile, pluginResources, false, false);
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Assumes that the locale folder is directly under the translation catalogue's root folder.
	 * 
	 * @param locale
	 * @param eclipseInstallFeature
	 * @return The eclipseInstallFeature's corresponding plug-in in the catalogue for the specified locale.
	 */
	private PluginProxy getFeatureForLocale(LocaleProxy locale, PluginProxy eclipseInstallFeature) {
		
		for (File localeDir: rootDirectory.listFiles()) {		
			if ( localeDir.getName().equalsIgnoreCase(locale.getName()) ) {
				
				File featuresRoot = new File(rootDirectory.getAbsolutePath() + 
						File.separatorChar + localeDir.getName() + 
						File.separatorChar + FEATURES_PATH);
				
				File featureFile = new File(featuresRoot.getAbsolutePath() + File.separator + eclipseInstallFeature.getName());
				
				if(featureFile.exists()) {
					List<ResourceProxy> featureResources = extractResources(featureFile, featureFile.getName());
					return new PluginProxy(featureFile, featureResources, false, false);
				}
			}
		}
		
		return null;
	}

	public ResourceProxy getResourceTranslation(PluginProxy translationCataloguePlugin, ResourceProxy eclipseInstallPluginResource) {
		for (ResourceProxy candidateTranslatedResource: translationCataloguePlugin.getResources()) {
			if (candidateTranslatedResource.getRelativePath().
					equalsIgnoreCase(eclipseInstallPluginResource.getRelativePath())) {
				return candidateTranslatedResource;
			}
		}
		return null;
	}
	
	private List<ResourceProxy> extractResources(File file, String pluginName) {
		List<ResourceProxy> resources = new LinkedList<ResourceProxy>();
		for (File subFile: file.listFiles()) {
			if (subFile.isDirectory()) {
				resources.addAll(extractResources(subFile, pluginName));					
			}
			else {
				String absolutePath = subFile.getAbsolutePath();
				String relativePath = absolutePath.substring(absolutePath.indexOf(pluginName));
				relativePath = relativePath.substring(pluginName.length() + 1);
				resources.add(new ResourceProxy(subFile, relativePath));
			}
		}
		return resources;
	}
		
	/**
	 * Extract the list of all the relevant locales featured in the Translation Catalogue.
	 * The assumption is that all folders within the catalogue's root folder are locales.
	 */
	public Set<LocaleProxy> findRelevantLocalesInCatalogue() throws InvalidLocationException {
		Set<LocaleProxy> allLocales = new HashSet<LocaleProxy>();
		
		for (File localeDir: rootDirectory.listFiles()) {
			LocaleProxy candidateLocale = new LocaleProxy(localeDir.getName());
			/* 	If it's in the list of specified locales, or if the latter is empty (meaning all 
				locales are to be included), then the candidate is a relevant locale.	*/
			if (locales.contains(candidateLocale) || locales.isEmpty()) {				
				allLocales.add(candidateLocale);
			}
		}
		
		if (allLocales.isEmpty()) {
			throw new InvalidLocationException(Messages.getString("Error_invalid_translation_catalogue"));	//$NON-NLS-1$
		} else if (locales.isEmpty()) {
			locales = allLocales;
		}
		
		return allLocales;
	}

}
