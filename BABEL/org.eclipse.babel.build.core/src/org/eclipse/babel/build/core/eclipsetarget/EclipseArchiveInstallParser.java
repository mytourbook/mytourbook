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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.eclipse.babel.build.core.Messages;
import org.eclipse.babel.build.core.PluginProxy;
import org.eclipse.babel.build.core.ResourceProxy;
import org.eclipse.babel.build.core.exceptions.InvalidLocationException;


public class EclipseArchiveInstallParser implements EclipseParser {

	private ZipFile eclipseArchive;
	private File archiveLocation;
	
	//The lists of plug-ins/features will be stored in maps for faster lookup
	private Map<String, PluginProxy> plugins = new HashMap<String, PluginProxy>();
	private Map<String, PluginProxy> features = new HashMap<String, PluginProxy>();
	
	private Set<String> excludeList;
	
	public EclipseArchiveInstallParser(ZipFile eclipseArchive, File archiveLocation) throws InvalidLocationException {
		this.eclipseArchive = eclipseArchive;
		this.archiveLocation = archiveLocation;
		
		validateTarget();
		useDefaultList();
	}
	
	public EclipseArchiveInstallParser(ZipFile eclipseArchive, File archiveLocation, Set<String> excludeList) throws InvalidLocationException {
		this.eclipseArchive = eclipseArchive;
		this.archiveLocation = archiveLocation;
		
		validateTarget();
		
		useDefaultList();
		this.excludeList.addAll(excludeList);
	}
	
	@SuppressWarnings("unchecked")
	private void validateTarget() throws InvalidLocationException {
		boolean foundPluginsPath = false;
		boolean foundFeaturesPath = false;
		for (Enumeration entries = eclipseArchive.entries(); entries.hasMoreElements();) {
			ZipEntry entry = (ZipEntry)entries.nextElement();
			
			if (entry.getName().contains(PLUGINS_PATH)) {
				foundPluginsPath = true;
			} else if (entry.getName().contains(FEATURES_PATH)) {
				foundFeaturesPath = true;
			}
		}
		if ( (!foundFeaturesPath) || (!foundPluginsPath) ) {
			throw new InvalidLocationException();
		}
	}
	
	private class ExtractionParameters {
		List<ResourceProxy> resourcesOfLastPlugin = new LinkedList<ResourceProxy>();		
		ZipEntry entry;
		ZipEntry lastPluginOrFeatureEntry = null;
		String name;
		String tempPluginOrFeaturePath;
		String lastPluginOrFeature = PLUGIN_MASK;
		String lastPluginOrFeaturePath = ""; //$NON-NLS-1$
		
		public List<ResourceProxy> getResourcesOfLastPlugin() {
			return resourcesOfLastPlugin;
		}
		public ZipEntry getEntry() {
			return entry;
		}
		public ZipEntry getLastPluginOrFeatureEntry() {
			return lastPluginOrFeatureEntry;
		}
		public String getName() {
			return name;
		}
		public String getTempPluginOrFeaturePath() {
			return tempPluginOrFeaturePath;
		}
		public String getLastPluginOrFeature() {
			return lastPluginOrFeature;
		}
		public String getLastPluginOrFeaturePath() {
			return lastPluginOrFeaturePath;
		}
		public void setResourcesOfLastPlugin(List<ResourceProxy> resourcesOfLastPlugin) {
			this.resourcesOfLastPlugin = resourcesOfLastPlugin;
		}
		public void setEntry(ZipEntry entry) {
			this.entry = entry;
		}
		public void setLastPluginOrFeatureEntry(ZipEntry lastPluginOrFeatureEntry) {
			this.lastPluginOrFeatureEntry = lastPluginOrFeatureEntry;
		}
		public void setName(String name) {
			this.name = name;
		}
		public void setTempPluginOrFeaturePath(String tempPluginOrFeaturePath) {
			this.tempPluginOrFeaturePath = tempPluginOrFeaturePath;
		}
		public void setLastPluginOrFeature(String lastPluginOrFeature) {
			this.lastPluginOrFeature = lastPluginOrFeature;
		}
		public void setLastPluginOrFeaturePath(String lastPluginOrFeaturePath) {
			this.lastPluginOrFeaturePath = lastPluginOrFeaturePath;
		}
	}
	
	@SuppressWarnings("unchecked")
	public void parse() throws Exception {
		ExtractionParameters parameters = new ExtractionParameters();
		
		boolean ignorePlugin = false;
		
		for (Enumeration entries = eclipseArchive.entries(); entries.hasMoreElements();) {	 
			parameters.setEntry( (ZipEntry)entries.nextElement() );
			parameters.setName( parameters.getEntry().getName() );			
			
			if (parameters.getName().equalsIgnoreCase(PLUGINS_PATH) 
					|| parameters.getName().equalsIgnoreCase(FEATURES_PATH)) {
				//Ignore case
			}
			/** Potential Non-JARed Plug-in or Feature */
			else if (parameters.getName().endsWith(DIRECTORY_SUFFIX) && (parameters.getName().contains(PLUGINS_PATH) 
					|| parameters.getName().contains(FEATURES_PATH))) {
				
				if (parameters.getName().contains(PLUGINS_PATH)) {
					parameters.setTempPluginOrFeaturePath(PLUGINS_PATH);
				}
				else {
					parameters.setTempPluginOrFeaturePath(FEATURES_PATH);
				}			
				parameters.setName( removePluginOrFeaturePath(parameters.getName(), 1) );
				
				/** Non-JARed Plug-in */
				if (!parameters.getName().contains(DIRECTORY_SUFFIX)) {
					boolean ignorePrevious = ignorePlugin;
					if (!isValidPlugin(parameters.getName())) {
						ignorePlugin = true;
					} else {
						ignorePlugin = false;
					}
					handleNonJarPluginOrFeature(parameters, ignorePrevious);				
				}				
			}
			/** Plug-in JAR */
			else if ( parameters.getName().endsWith(JAR_EXTENSION) && 
					(!parameters.getName().contains(parameters.getLastPluginOrFeature())) && 
					(isValidPlugin(parameters.getName())) ) {
				handleJarPlugin(parameters);				
			}
			else if ( parameters.getName().endsWith(JAR_EXTENSION) && 
					(parameters.getName().contains(parameters.getLastPluginOrFeature())) && 
					(isValidResource(parameters.getName())) ) {
				handleJarResource(parameters);
			}
			/** Translatable Resource for Non-JARed Plug-in or Feature*/
			else if ( isTranslatableResource(parameters.getName(), parameters) && (!ignorePlugin) )  {
				handleTranslatableResource(parameters);				
			}
		}
		
		handleLastPluginOrFeatureEntry(parameters);
	}
	
	private void handleJarResource(ExtractionParameters parameters) throws Exception {
		List<ResourceProxy> jarResources = new ArrayList<ResourceProxy>();
		
		String frontRelativePath = ""; 
		
		if (parameters.getName().contains(DIRECTORY_SUFFIX)) {
			frontRelativePath = parameters.getName().substring(parameters.getName().lastIndexOf(DIRECTORY_SUFFIX)+1);
		}
		frontRelativePath = frontRelativePath.replaceAll(JAR_EXTENSION, "");
		frontRelativePath += "_jar";
		
		InputStream input = eclipseArchive.getInputStream(parameters.getEntry()); 
		ZipInputStream zipInput = new ZipInputStream(input);
		
		ZipEntry zipEntry = zipInput.getNextEntry();
		while(zipEntry != null) {
			String resourceEntryName = zipEntry.getName();
			
			if ( isValidResource(resourceEntryName)  && (!resourceEntryName.contains(META_INF_DIR_NAME))) {
				String relativePath = frontRelativePath + File.separator + resourceEntryName;
				relativePath = relativePath.replace("\\", File.separator);
				relativePath = relativePath.replace("/", File.separator);
				jarResources.add(new ResourceProxy(
						new File(archiveLocation.getAbsolutePath() + SLASH 
								+ parameters.getName() + SLASH + resourceEntryName), relativePath)); //$NON-NLS-1$ //$NON-NLS-2$
			}

			zipEntry = zipInput.getNextEntry();																	
		}
		
		parameters.getResourcesOfLastPlugin().addAll(jarResources);
	}
	
	private void handleNonJarPluginOrFeature(ExtractionParameters parameters, boolean ignorePrevious) {
		//If it's not the first plug-in/feature that we come across
		if (!parameters.getLastPluginOrFeature().equals(PLUGIN_MASK) && (!ignorePrevious)) {
			PluginProxy newPlugin;
			
			String lastEntryName = parameters.getLastPluginOrFeatureEntry().getName();
			if (!lastEntryName.startsWith(File.separator)) {
				lastEntryName = File.separator + lastEntryName;
			}
			
			if (parameters.getLastPluginOrFeaturePath().equals(PLUGINS_PATH)) {
				newPlugin = new PluginProxy(new File(archiveLocation.getAbsolutePath(), 
						lastEntryName), 
						parameters.getResourcesOfLastPlugin(), false, false);
				plugins.put(newPlugin.getName(), newPlugin);
			}
			else {
				newPlugin = new PluginProxy(new File(archiveLocation.getAbsolutePath(), 
						lastEntryName), 
						parameters.getResourcesOfLastPlugin(), false, true);
				features.put(newPlugin.getName(), newPlugin);
			}
			parameters.setResourcesOfLastPlugin( new LinkedList<ResourceProxy>() );
		}
		
		parameters.setLastPluginOrFeature( parameters.getName() );
		parameters.setLastPluginOrFeatureEntry( parameters.getEntry() );
		parameters.setLastPluginOrFeaturePath( parameters.getTempPluginOrFeaturePath() );
	}
	
	private void handleJarPlugin(ExtractionParameters parameters) throws Exception {
		List<ResourceProxy> jarResources = new LinkedList<ResourceProxy>();
		parameters.setName( removePluginOrFeaturePath(parameters.getName(), 0) );
		parameters.setName( parameters.getName().replaceAll(JAR_EXTENSION, "") ); //$NON-NLS-1$
		
		InputStream input = eclipseArchive.getInputStream(parameters.getEntry()); 
		ZipInputStream zipInput = new ZipInputStream(input);
		
		ZipEntry zipEntry = zipInput.getNextEntry();
		while(zipEntry != null) {
			String resourceEntryName = zipEntry.getName();
			
			if ( isValidResource(resourceEntryName) ) {
				jarResources.add(new ResourceProxy(
						new File(archiveLocation.getAbsolutePath() + File.separator +  
						parameters.getEntry().getName() + File.separator + resourceEntryName), resourceEntryName)); //$NON-NLS-1$ //$NON-NLS-2$
			}

			zipEntry = zipInput.getNextEntry();																	
		}
		
		PluginProxy newPlugin = new PluginProxy(new File(archiveLocation.getAbsolutePath(),  
				parameters.getEntry().getName()), jarResources, true, false);
		plugins.put(newPlugin.getName(), newPlugin);
	}
	
	private boolean isTranslatableResource(String resourceName, ExtractionParameters parameters) {
		return isValidResource(resourceName) 
			&& (resourceName.startsWith(PLUGINS_PATH) || resourceName.startsWith(FEATURES_PATH));
	}
	
	private boolean isValidPlugin(String name) {
		name = removePluginOrFeaturePath(name, 0);
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
			
			if (name.length() == 0) {
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
	
	private void handleTranslatableResource(ExtractionParameters parameters) {
		String resource = removePluginOrFeaturePath(parameters.getName(), 0);
		resource =  resource.replaceAll(parameters.getLastPluginOrFeature(), ""); //$NON-NLS-1$
		parameters.getResourcesOfLastPlugin().add(new ResourceProxy(new File(eclipseArchive.getName() + 
				Messages.getString("Characters_entry_separator") + parameters.getName()), resource.substring(1))); //$NON-NLS-1$
	}
	
	private void handleLastPluginOrFeatureEntry(ExtractionParameters parameters) {
		if (parameters.getLastPluginOrFeatureEntry() != null) {
			//Add last feature or plug-in
			PluginProxy newPlugin;
			if (parameters.getLastPluginOrFeaturePath().equals(PLUGINS_PATH)) {
				newPlugin = new PluginProxy(new File(archiveLocation.getAbsolutePath(), 
						parameters.getLastPluginOrFeatureEntry().getName()), parameters.getResourcesOfLastPlugin(), false, false);
				plugins.put(newPlugin.getName(), newPlugin);
			}
			else {
				newPlugin = new PluginProxy(new File(archiveLocation.getAbsolutePath(), 
						parameters.getLastPluginOrFeatureEntry().getName()), parameters.getResourcesOfLastPlugin(), false, true);
				features.put(newPlugin.getName(), newPlugin);				
			}
		}
		parameters.setResourcesOfLastPlugin( new LinkedList<ResourceProxy>() );
	}
	
	private String removePluginOrFeaturePath(String name, int charactersToRemoveAtEnd) {	
		name = name.replaceAll(PLUGINS_PATH, ""); //$NON-NLS-1$
		name = name.replaceAll(FEATURES_PATH, ""); //$NON-NLS-1$
		name = name.substring(0, name.length() - charactersToRemoveAtEnd);
		return name;
	}
	
	private void useDefaultList() {
		this.excludeList = new HashSet<String>();
		for ( String exclude : DEFAULT_EXCLUDE_LIST) {
			this.excludeList.add(exclude);
		}

		this.excludeList.add(PATTERN_DIR);

	}
	
	public Map<String, PluginProxy> getPlugins() {
		return plugins;
	}

	public Map<String, PluginProxy> getFeatures() {
		return features;
	}
}
