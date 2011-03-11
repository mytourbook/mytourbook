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
package org.eclipse.babel.build.core.eclipsetarget;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

import org.eclipse.babel.build.core.Messages;
import org.eclipse.babel.build.core.PluginProxy;
import org.eclipse.babel.build.core.ResourceProxy;
import org.eclipse.babel.build.core.exceptions.InvalidLocationException;


public class EclipseInstallParser implements EclipseParser {

	private final static String PLUGINS_PATH = Messages.getString("Paths_plugins_directory"); //$NON-NLS-1$
	private final static String FEATURES_PATH = Messages.getString("Paths_features_directory"); //$NON-NLS-1$
	
	private Set<String> excludeList = new HashSet<String>();
	
	private File eclipseInstallLocation;
	//The lists of plug-ins/features will be stored in maps for faster lookup
	private Map<String, PluginProxy> plugins = new HashMap<String, PluginProxy>();
	private Map<String, PluginProxy> features = new HashMap<String, PluginProxy>();
	
	public EclipseInstallParser(File eclipseInstallLocation) throws InvalidLocationException {
		this.eclipseInstallLocation = eclipseInstallLocation;
		
		validateTarget();
		useDefaultList();
	}
	
	public EclipseInstallParser(File eclipseInstallLocation, Set<String> excludeList) throws InvalidLocationException {
		this.eclipseInstallLocation = eclipseInstallLocation;
		
		validateTarget();
		
		useDefaultList();
		this.excludeList.addAll(excludeList);
	}
	
	private void validateTarget() throws InvalidLocationException {
		String pluginPath = Messages.getString("Paths_plugins_directory");		//$NON-NLS-1$
		pluginPath = pluginPath.replace(Messages.getString("Characters_entry_separator"), File.separator);	//$NON-NLS-1$
		String featurePath = Messages.getString("Paths_features_directory");	//$NON-NLS-1$
		featurePath = featurePath.replace(Messages.getString("Characters_entry_separator"), File.separator);	//$NON-NLS-1$
		
		File pluginDir = new File(this.eclipseInstallLocation.getAbsolutePath(), pluginPath);
		File featureDir = new File(this.eclipseInstallLocation.getAbsolutePath(), featurePath);
		
		if ( (!pluginDir.exists()) || (!featureDir.exists()) ) {
			throw new InvalidLocationException();
		}
	}
	
	public void parse() throws Exception {
		
		PluginProxy newPlugin;
		File pluginsRoot = new File(this.eclipseInstallLocation.getAbsolutePath() + File.separatorChar +  PLUGINS_PATH);
		
		for (File plugin: pluginsRoot.listFiles()) {
			List<ResourceProxy> pluginResources;
			if (isValidPlugin(plugin.getName())) {
				if(plugin.isDirectory()) {
					pluginResources = extractResources(plugin, plugin.getName());
					newPlugin = new PluginProxy(plugin, pluginResources, false, false);
					plugins.put(newPlugin.getName(), newPlugin);
				} else {
					//Handle JAR Plug-in
					pluginResources = extractResourcesFromJar(plugin);
					newPlugin = new PluginProxy(plugin, pluginResources, true, false);
					plugins.put(newPlugin.getName(), newPlugin);
				}
			}
		}
		
		PluginProxy newFeature;
		File featuresRoot = new File(this.eclipseInstallLocation.getAbsolutePath() + File.separatorChar +  FEATURES_PATH);
		
		for (File feature: featuresRoot.listFiles()) {
			List<ResourceProxy> featureResources;
			if (isValidPlugin(feature.getName())) {
				if(feature.isDirectory()) {
					featureResources = extractResources(feature, feature.getName());
					newFeature = new PluginProxy(feature, featureResources, false, true);
					features.put(newFeature.getName(), newFeature);
				} else {
					//Handle JAR Feature
					featureResources = extractResourcesFromJar(feature);
					newFeature = new PluginProxy(feature, featureResources, true, true);
					features.put(newFeature.getName(), newFeature);
				}
			}
		}
	}
	
	private List<ResourceProxy> extractResources(File file, String pluginName) throws Exception {
		List<ResourceProxy> resources = new LinkedList<ResourceProxy>();
		for (File subFile: file.listFiles()) {
			if (subFile.isDirectory()) {
				resources.addAll(extractResources(subFile, pluginName));					
			}
			else {
				if (isValidResource(subFile.getName())) {					
					if (subFile.getName().endsWith(JAR_EXTENSION)) {
						resources.addAll(handleJarResource(subFile));
					} else {					
						String absolutePath = subFile.getAbsolutePath();
						String relativePath = absolutePath.substring(absolutePath.indexOf(pluginName));
						relativePath = relativePath.substring(pluginName.length() + 1);
						resources.add(new ResourceProxy(subFile, relativePath));
					}
				}
			}
		}
		return resources;
	}
	
	List<ResourceProxy> handleJarResource(File jarResource) throws Exception {
		List<ResourceProxy> jarResources = new ArrayList<ResourceProxy>();
		
		String frontRelativePath = jarResource.getName(); 
		
		frontRelativePath = frontRelativePath.replaceAll(JAR_EXTENSION, "");	//$NON-NLS-1$
		frontRelativePath += JAR_RESOURCE_SUFFIX;
		
		InputStream input = new FileInputStream(jarResource); 
		JarInputStream zipInput = new JarInputStream(input);
		
		ZipEntry zipEntry = zipInput.getNextEntry();
		while(zipEntry != null) {
			String resourceEntryName = zipEntry.getName();
			resourceEntryName = resourceEntryName.replace("\\", SLASH);	//$NON-NLS-1$
			
			if ( isValidResource(resourceEntryName)  && (!resourceEntryName.contains(META_INF_DIR_NAME))) {
				String relativePath = frontRelativePath + File.separator + resourceEntryName;
				relativePath = relativePath.replace("\\", File.separator);	//$NON-NLS-1$
				relativePath = relativePath.replace(SLASH, File.separator);
				jarResources.add(new ResourceProxy(new File(jarResource.getAbsolutePath() + SLASH
						+ resourceEntryName), relativePath)); //$NON-NLS-1$ //$NON-NLS-2$
			}

			zipEntry = zipInput.getNextEntry();																	
		}
		
		return jarResources;
	}

	private List<ResourceProxy> extractResourcesFromJar(File file) {
		List<ResourceProxy> jarResources = new LinkedList<ResourceProxy>();
		
		try {						
			JarFile jarPluginOrFeature = new JarFile(file);		
			
			Enumeration<JarEntry> jarEntries = jarPluginOrFeature.entries();
			
			JarEntry jarEntry = jarEntries.nextElement();
			while (jarEntry != null) {
				String resourceEntryName = jarEntry.getName();
				
				if ( isValidResource(resourceEntryName) ) {
					jarResources.add(new ResourceProxy(
							new File(file.getAbsolutePath() + File.separator
									+ resourceEntryName), resourceEntryName));
				}
				
				if (jarEntries.hasMoreElements()) {
					jarEntry = jarEntries.nextElement();
				} else {
					jarEntry = null;
				}
			}
			
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		
		return jarResources;
	}
	
	private boolean isValidPlugin(String name) {
		for (String exclude : this.excludeList) {
			if (name.matches(exclude)) {
				return false;
			}
		}
		return true;
	}
	
	private boolean isValidResource(String name) {
		if (name.contains(DIRECTORY_SUFFIX)) {
			name = name.substring(name.lastIndexOf(DIRECTORY_SUFFIX)+1);
			if (name.length() == 0)  {
				return false;
			}
		}
		for (String exclude : this.excludeList) {
			if (name.matches(exclude)) {
				return false;
			}
		}
		return true;
	}
	
	private void useDefaultList() {
		this.excludeList = new HashSet<String>();
		for ( String exclude : DEFAULT_EXCLUDE_LIST) {
			this.excludeList.add(exclude);
		}
	}
	
	public Map<String, PluginProxy> getPlugins() {
		return plugins;
	}

	public Map<String, PluginProxy> getFeatures() {
		return features;
	}
}
