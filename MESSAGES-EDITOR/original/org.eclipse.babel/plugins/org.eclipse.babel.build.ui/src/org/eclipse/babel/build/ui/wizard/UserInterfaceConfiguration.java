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
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.babel.build.core.Configuration;
import org.eclipse.babel.build.core.Filter;
import org.eclipse.babel.build.core.LocaleGroup;
import org.eclipse.babel.build.core.LocaleProxy;
import org.eclipse.babel.build.core.PluginProxy;
import org.eclipse.babel.build.core.Range;
import org.eclipse.babel.build.core.ResourceProxy;
import org.eclipse.babel.build.core.eclipsetarget.EclipseTarget;
import org.eclipse.babel.build.core.exceptions.InvalidFilenameException;
import org.eclipse.babel.build.core.exceptions.InvalidLocationException;
import org.eclipse.babel.build.core.exceptions.MissingLocationException;
import org.eclipse.babel.build.core.translationcatalogue.TranslationCatalogue;

public class UserInterfaceConfiguration implements Configuration {

	private IProgressMonitor fMonitor;
	
	private final File fWorkingDirectory;
	private final File fEclipseRoot;
	private final File fTranslationsRoot;
	
	private EclipseTarget fEclipseTarget = null;
	private TranslationCatalogue fTranslationCatalogue = null;
	
	private final List<PluginProxy> fPluginProxies;
	private final Set<LocaleProxy> fLocales;
	private final Set<LocaleGroup> fLocaleGroups;
	private final Set<String> fExcludeList;
	private List<Filter> fReportFilters;
	private File fReport;
	private final LanguagePackGenerationHandler fParentHandler;
	private final boolean fIncludeXmlReport;
	private final boolean fLongReport;
	private String fLocaleExtension;

	
	public UserInterfaceConfiguration(File workingDirectoryPath, 
			File translationCatalogueLocation, 
			File eclipseArchiveLocation,
			List<String> locales,
			Set<String> excludeList,
			List<String> reportFilterPatterns,
			LanguagePackGenerationHandler parentHandler,
			boolean includeXmlReport,
			boolean longReport) {
		this.fIncludeXmlReport = includeXmlReport;
		this.fLongReport = longReport;
		this.fParentHandler = parentHandler;
		this.fWorkingDirectory = workingDirectoryPath;
		this.fEclipseRoot = eclipseArchiveLocation;
		this.fTranslationsRoot = translationCatalogueLocation;
		this.fPluginProxies = null;
		
		this.fLocales = getLocaleProxies(locales);
		this.fLocaleGroups = getLocaleGroups(locales);
		
		this.fExcludeList = excludeList;
		this.fReport = new File(fWorkingDirectory, "coverage.xml");
		this.fReportFilters = buildFilterList(reportFilterPatterns);
		eclipseInstall();
		
		fLocaleExtension = "";
		if (this.fLocales.size() == 1 && this.fLocaleGroups.isEmpty()) {
			LocaleProxy singleLocale = this.fLocales.iterator().next();
			fLocaleExtension += Messages.getString("Characters_Underscore") + singleLocale.getName();	//$NON-NLS-1$
		}
	}
	
	public UserInterfaceConfiguration(File workingDirectoryPath, 
			File translationCatalogueLocation, 
			List<PluginProxy> pluginProxies,
			List<String> locales,
			Set<String> excludeList,
			List<String> reportFilterPatterns,
			LanguagePackGenerationHandler parentHandler,
			boolean includeXmlReport,
			boolean longReport) {
		this.fIncludeXmlReport = includeXmlReport;
		this.fLongReport = longReport;
		this.fParentHandler = parentHandler;
		this.fWorkingDirectory = workingDirectoryPath;
		this.fEclipseRoot = null;
		this.fTranslationsRoot = translationCatalogueLocation;
		this.fPluginProxies = pluginProxies;
		
		this.fLocales = getLocaleProxies(locales);
		this.fLocaleGroups = getLocaleGroups(locales);
		
		this.fExcludeList = excludeList;
		this.fReport = new File(fWorkingDirectory, "coverage.xml");
		this.fReportFilters = buildFilterList(reportFilterPatterns);
		eclipseInstall();
		
		fLocaleExtension = "";
		if (this.fLocales.size() == 1 && this.fLocaleGroups.isEmpty()) {
			LocaleProxy singleLocale = this.fLocales.iterator().next();
			fLocaleExtension += Messages.getString("Characters_Underscore") + singleLocale.getName();	//$NON-NLS-1$
		}
	}
	
	private List<Filter> buildFilterList(List<String> filterPatterns){
		List<Filter> filters = new LinkedList<Filter>();
		for(String pattern : filterPatterns){
			filters.add(new Filter(pattern));
		}
		return filters;
	}
	
	private static Set<LocaleProxy> getLocaleProxies(List<String> localeNames){		
		Set<LocaleProxy> locales = new HashSet<LocaleProxy>();
		for(String localeName : localeNames){
			if (! LocaleGroup.isValidGroupName(localeName) && !LocaleGroup.isValidGroupFullName(localeName)){ 
				locales.add(new LocaleProxy(localeName));
			}
		}
	
		return locales;
	}
	
	private static Set<LocaleGroup> getLocaleGroups(List<String> locales){
		Set<LocaleGroup> set = new HashSet<LocaleGroup>();
		for(String localeName : locales){
			if (LocaleGroup.isValidGroupName(localeName)) {
				set.add(LocaleGroup.get(localeName));
			}
			else if(LocaleGroup.isValidGroupFullName(localeName)) {
				set.add(LocaleGroup.getByFullName(localeName));
			}
		}
		return set;
	}
	
	public Range compatibilityRange() {
		// TODO Auto-generated method stub
		return null;
	}

	public EclipseTarget eclipseInstall() {
		try {
			if (fEclipseTarget == null){
				if(fEclipseRoot == null) {
					fEclipseTarget = new EclipseTarget(fPluginProxies);
				}
				else {
					fEclipseTarget = new EclipseTarget(fEclipseRoot, fExcludeList);
					fEclipseTarget.populatePlugins();
				}
			}
		} catch (InvalidLocationException i) {
			fParentHandler.notifyError(Messages.getString("Error_InvalidEclipseTarget") + Messages.getString("Error_EclipseEntryLocation") + fEclipseRoot);	//$NON-NLS-1$
			return null;
		} catch (MissingLocationException m) {
			fParentHandler.notifyError(Messages.getString("Error_MissingEclipseTarget") + Messages.getString("Error_EclipseEntryLocation") + fEclipseRoot);	//$NON-NLS-1$
			return null;
		} catch(InvalidFilenameException f) {
			fParentHandler.notifyError(Messages.getString("Error_InvalidEclipseTargetName") + Messages.getString("Error_EclipseEntryLocation") + fEclipseRoot);	//$NON-NLS-1$
			return null;
		} catch (Exception e) {
			fParentHandler.notifyError(Messages.getString("Error_EclipseTarget") + Messages.getString("Error_EclipseEntryLocation") + fEclipseRoot );	//$NON-NLS-1$
			return null;
		}
		return fEclipseTarget;
	}

	public Set<String> excludeList() {
		return fExcludeList;
	}

	public List<Filter> filters() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean includePseudoTranslations() {
		// TODO Auto-generated method stub
		return false;
	}

	public Set<LocaleGroup> localeGroups() {
		return fLocaleGroups;
	}

	public Set<LocaleProxy> locales() {
		return fLocales;
	}

	public File reportLocation() {
		return fReport;
	}

	public Date timestamp() {
		// TODO Auto-generated method stub
		return null;
	}

	public TranslationCatalogue translations() {	
		if (fTranslationCatalogue == null){
			try {
				
				if (fLocaleGroups.isEmpty()) {
					fTranslationCatalogue = new TranslationCatalogue(fTranslationsRoot, fLocales);
				} else {
					fLocales.clear();
					fTranslationCatalogue = new TranslationCatalogue(fTranslationsRoot, fLocaleGroups);
					fLocales.addAll(fTranslationCatalogue.getAllLocales());
				}
				
			} catch (MissingLocationException m) {
				fParentHandler.notifyError(Messages.getString("Error_MissingTranslationCatalogue") + Messages.getString("Error_TranslationsEntryLocation") + fTranslationsRoot);	//$NON-NLS-1$
				return null;
			} catch (InvalidLocationException i) {
				fParentHandler.notifyError(i.getMessage());
				return null;
			} catch(InvalidFilenameException f) {
				fParentHandler.notifyError(Messages.getString("Error_InvalidTranslationCatalogueName") + Messages.getString("Error_TranslationsEntryLocation") + fTranslationsRoot);	//$NON-NLS-1$
				return null;
			}
		}
		return fTranslationCatalogue;
	}

	public File workingDirectory() {
		return fWorkingDirectory;
	}

	public boolean includeResource(PluginProxy plugin, ResourceProxy resource) {
		for(Filter filter : fReportFilters){
			if(filter.matches(plugin, resource)){
				return filter.isInclusive();
			}
		}
		return true;
	}
	
	public void setProgressMonitor(IProgressMonitor monitor) {
		this.fMonitor = monitor;
	}

	public void notifyProgress(String fragmentName) {
		fMonitor.subTask("Generating fragment for " + fragmentName);
		fMonitor.worked(1);
	}

	public boolean includeXmlReport() {
		return fIncludeXmlReport;
	}

	public boolean longReport() {
		return fLongReport;
	}

	public String localeExtension() {
		return fLocaleExtension;
	}

}
