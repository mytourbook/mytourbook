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

import org.eclipse.babel.build.core.LocaleGroup;
import org.eclipse.babel.build.core.LocaleProxy;
import org.eclipse.babel.build.core.Messages;
import org.eclipse.babel.build.core.PluginProxy;
import org.eclipse.babel.build.core.ResourceProxy;
import org.eclipse.babel.build.core.exceptions.InvalidLocationException;


public class TranslationCatalogueBulkParser implements TranslationCatalogueParser {

	private File rootDirectory;
	private Set<LocaleGroup> specifiedGroups;
	
	public TranslationCatalogueBulkParser(File rootDirectory, Set<LocaleGroup> specifiedGroups) {
		this.rootDirectory = rootDirectory;
		this.specifiedGroups = specifiedGroups;
	}

	/**
	 * 
	 * @param eclipseInstallPlugin
	 * @return The different versions (one for each locale) of the same plug-in within the catalogue.
	 */
	public Map<String, PluginProxy> getPluginForSpecifiedLocales (PluginProxy eclipseInstallPlugin) {
		Map<String, PluginProxy> plugins = new HashMap<String, PluginProxy>();
		
		for (File subDir: rootDirectory.listFiles()) {
			if (LocaleGroup.isValidGroupName(subDir.getName()) && isIncludedGroup(subDir.getName())) {														
				
				for (File localeDir: subDir.listFiles()) {
						
					String group = subDir.getName();
					
					File pluginsRoot = new File(rootDirectory.getAbsolutePath() + 
							File.separatorChar + group + File.separatorChar + localeDir.getName() + 
							File.separatorChar + PLUGINS_PATH);
					
					File pluginFile = new File(pluginsRoot.getAbsolutePath() + File.separator + eclipseInstallPlugin.getName());
					
					if(pluginFile.exists()) {
						List<ResourceProxy> pluginResources = extractResources(pluginFile, pluginFile.getName());
						plugins.put(localeDir.getName(), new PluginProxy(pluginFile, pluginResources, false, false));
					}
				}
			}
		}
		
		return plugins;
	}
	
	/**
	 * 
	 * @param eclipseInstallFeature
	 * @return The different versions (one for each locale) of the same feature within the catalogue.
	 */
	public Map<String, PluginProxy> getFeatureForSpecifiedLocales (PluginProxy eclipseInstallFeature) {
		Map<String, PluginProxy> features = new HashMap<String, PluginProxy>();
		
		for (File subDir: rootDirectory.listFiles()) {
			if (LocaleGroup.isValidGroupName(subDir.getName()) && isIncludedGroup(subDir.getName())) {														
				
				for (File localeDir: subDir.listFiles()) {
					
					String group = subDir.getName();
					
					File featuresRoot = new File(rootDirectory.getAbsolutePath() + 
							File.separatorChar + group + File.separatorChar + localeDir.getName() + 
							File.separatorChar + FEATURES_PATH);
					
					File featureFile = new File(featuresRoot.getAbsolutePath() + File.separator + eclipseInstallFeature.getName());
					
					if(featureFile.exists()) {
						List<ResourceProxy> featureResources = extractResources(featureFile, featureFile.getName());
						features.put(localeDir.getName(), new PluginProxy(featureFile, featureResources, false, false));
					}
				}
			}
		}
		
		return features;
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
	
	private boolean isIncludedGroup(String name) {
		return this.specifiedGroups.contains(LocaleGroup.GROUP_ALL) || this.specifiedGroups.contains(LocaleGroup.get(name));
	}
	
	/**
	 * Extract the list of all the relevant locales featured in the Translation Catalogue.
	 * The assumption is that all folders within relevant group folders are locales.
	 */
	public Set<LocaleProxy> findRelevantLocalesInCatalogue() throws InvalidLocationException {
		Set<LocaleProxy> allLocales = new HashSet<LocaleProxy>();
		
		for (File groupDir: rootDirectory.listFiles()) {
			if (LocaleGroup.isValidGroupName(groupDir.getName()) && isIncludedGroup(groupDir.getName())) {				
				for (File localeDir: groupDir.listFiles()) {
					allLocales.add(new LocaleProxy(localeDir.getName()));
				}
			}
		}
		
		if (allLocales.isEmpty()) {
			throw new InvalidLocationException(
					Messages.getString("Error_invalid_translation_catalogue_bulk"));	//$NON-NLS-1$
		}
		
		return allLocales;
	}
}
