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
package org.eclipse.babel.build.ui.wizard;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.eclipse.babel.build.core.Configuration;
import org.eclipse.babel.build.core.PluginProxy;
import org.eclipse.babel.build.core.ResourceProxy;
import org.eclipse.babel.build.core.coverage.LanguagePackCoverageReport;
import org.eclipse.babel.build.core.exceptions.FailedDeletionException;
import org.eclipse.babel.build.core.exceptions.InvalidFilenameException;
import org.eclipse.babel.build.core.languagepack.LanguagePack;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.core.plugin.IPluginModelBase;

public class LanguagePackGenerationHandler {
	
	private List<File> fSelectedPlugins;
	private File fEclipseArchiveLocation;
	
	private List<String> fSelectedLocales;
	
	private File fWorkingDirectoryLocation;
	private File fTranslationCatalogueLocation;
	private Set<String> fExcludeList;
	private List<String> fReportFilterPatterns;
	private boolean fIncludeXmlReport;
	private boolean fLongReport;
	
	private String fErrorMessage = "";
	
	public LanguagePackGenerationHandler(Object[] selectedPlugins, 
			String[] selectedLocales,
			String workingDirectoryLocation,
			String translationCataloguelocation,
			Object[] ignoreList,
			Object[] reportFilterPatterns,
			boolean includeXmlReport,
			boolean longReport) {
		this.fIncludeXmlReport = includeXmlReport;
		this.fLongReport = longReport;
		this.fSelectedPlugins = new ArrayList<File>();
		getPluginLocations(selectedPlugins);
		this.fEclipseArchiveLocation = null;
		this.fSelectedLocales = Arrays.asList(selectedLocales);
		this.fWorkingDirectoryLocation = new File(workingDirectoryLocation);
		this.fTranslationCatalogueLocation = new File(translationCataloguelocation);		
		
		this.fExcludeList = new HashSet<String>();
		for(int i=0; i<ignoreList.length; i++) {
			this.fExcludeList.add(ignoreList[i].toString());
		}
		
		this.fReportFilterPatterns = new ArrayList<String>();
		for(Object pattern : reportFilterPatterns) {
			this.fReportFilterPatterns.add(pattern.toString());
		}
	}
	
	public LanguagePackGenerationHandler(String eclipseArchivePath, 
			String[] selectedLocales,
			String workingDirectoryLocation,
			String translationCataloguelocation,
			Object[] ignoreList,
			Object[] reportFilterPatterns, 
			boolean includeXmlReport,
			boolean longReport) {
		this.fIncludeXmlReport = includeXmlReport;
		this.fLongReport = longReport;
		this.fSelectedPlugins = null;
		this.fEclipseArchiveLocation = new File(eclipseArchivePath);
		this.fSelectedLocales = Arrays.asList(selectedLocales);
		this.fWorkingDirectoryLocation = new File(workingDirectoryLocation);
		this.fTranslationCatalogueLocation = new File(translationCataloguelocation);
		
		this.fExcludeList = new HashSet<String>();
		for(int i=0; i<ignoreList.length; i++) {
			this.fExcludeList.add(ignoreList[i].toString());
		}
		
		this.fReportFilterPatterns = new ArrayList<String>();
		for(Object pattern : reportFilterPatterns) {
			this.fReportFilterPatterns.add(pattern.toString());
		}
	}
	
	public String generateLanguagePack(IProgressMonitor monitor) {
		UserInterfaceConfiguration config;
		monitor.beginTask(Messages.getString("LanguagePackGenerationHandler_EclipseParsingTask"), IProgressMonitor.UNKNOWN);
		if(fSelectedPlugins == null) {
			config = new UserInterfaceConfiguration(fWorkingDirectoryLocation, 
					fTranslationCatalogueLocation, 
					fEclipseArchiveLocation,
					fSelectedLocales,
					fExcludeList,
					fReportFilterPatterns,
					this,
					fIncludeXmlReport,
					fLongReport);
		}
		else {
			List<PluginProxy> pluginProxies = getPluginProxies();
			config = new UserInterfaceConfiguration(fWorkingDirectoryLocation, 
					fTranslationCatalogueLocation, 
					pluginProxies,
					fSelectedLocales,
					fExcludeList,
					fReportFilterPatterns,
					this,
					fIncludeXmlReport,
					fLongReport);
		}
		try {
			int totalWork = config.eclipseInstall().getPlugins().size() + config.eclipseInstall().getFeatures().size();
			totalWork = totalWork + (int)(totalWork*0.2);
			monitor.beginTask(Messages.getString("LanguagePackGenerationHandler_LanguagePackTask"), totalWork);
			config.setProgressMonitor(monitor);
			
			// Generate a language pack
			LanguagePack languagePack = new LanguagePack(config);
			LanguagePackCoverageReport coverage;
			coverage = languagePack.generate();
			
			// Create coverage reports
			monitor.subTask(Messages.getString("LanguagePackGenerationHandler_CoverageReportTask"));
			Configuration.helper.printLanguagePackResult(config, coverage);
			monitor.done();
		} catch (InvalidFilenameException f) {
			return Messages.getString("Error_InvalidWorkingDirectoryName");
		}  catch (FailedDeletionException f) {
			return Messages.getString("Error_DeletingWorkingDirectory");	//$NON-NLS-1$
		} 
		catch (Exception e) {
			if (fErrorMessage.equals("")) {
				fErrorMessage = Messages.getString("Error_LanguagePack");
			}
			return fErrorMessage;
		}		
		return "";
	}
	
	private void getPluginLocations(Object[] plugins) {
		for(Object plugin : plugins) {
			String pluginPath = ((IPluginModelBase) plugin).getInstallLocation();
			fSelectedPlugins.add(new File(pluginPath));
		}
	}
	
	private List<PluginProxy> getPluginProxies() {
		PluginProxy newPlugin;
		List<PluginProxy> plugins = new ArrayList<PluginProxy>();
		for (File plugin: fSelectedPlugins) {
			List<ResourceProxy> pluginResources;
			if(plugin.isDirectory()) {
				pluginResources = extractResources(plugin, plugin.getName());
				newPlugin = new PluginProxy(plugin, pluginResources, false, false);
				plugins.add(newPlugin);
			} else {
				//Handle JAR Plug-in
				pluginResources = extractResourcesFromJar(plugin);
				newPlugin = new PluginProxy(plugin, pluginResources, true, false);
				plugins.add(newPlugin);
			}
		}
		return plugins;
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
									+ resourceEntryName), resourceEntryName)); //$NON-NLS-1$ //$NON-NLS-2$
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
	
	private boolean isValidResource(String resourceEntryName) {
		for (String exclude : this.fExcludeList) {
			if (resourceEntryName.endsWith(exclude)) {
				return false;
			}
		}
		return true;
	}
	
	public void notifyError(String errorMessage) {
		fErrorMessage = errorMessage;
	}

}
