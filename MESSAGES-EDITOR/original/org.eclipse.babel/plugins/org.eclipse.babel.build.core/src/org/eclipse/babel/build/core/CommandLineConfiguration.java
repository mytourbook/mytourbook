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
package org.eclipse.babel.build.core;

import java.io.File;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.babel.build.core.eclipsetarget.EclipseTarget;
import org.eclipse.babel.build.core.exceptions.InvalidFilenameException;
import org.eclipse.babel.build.core.exceptions.InvalidLocationException;
import org.eclipse.babel.build.core.exceptions.MissingLocationException;
import org.eclipse.babel.build.core.translationcatalogue.TranslationCatalogue;



/**
 * Configuration which derives it's parameters from parsing a command line.
 */
public class CommandLineConfiguration implements Configuration {
	public class Times{
		public long eclipseInstallPopulation;
		public long translationCataloguePopulation;
		public long languagePackGeneration;
	}
	
	private EclipseTarget eclipseInstall = null;
	private TranslationCatalogue translationCatalogue = null;
	private final Set<LocaleProxy> locales;
	private final Set<LocaleGroup> localeGroups;
	private final File workingDirectory;
	private final File eclipseRoot;
	private final File translationsRoot;
	final Times times = new Times();
	private File report;
	private final Set<String> excludeList;
	private List<Filter> reportFilters;
	
	private final boolean includeXmlReport;
	private final boolean longReport;
	private String localeExtension;
	
	final static String TOKEN = Messages.getString("Characters_locale_token");	//$NON-NLS-1$
	
	public CommandLineConfiguration(String... args) {
		Options opts = new Options(
				"--working-directory=", "--locales=",
				"--translation-archive=!", "--eclipse-archive=!",
				"--coverage-report=", "--exclude-list=",
				"--report-ignore-list=", "--xml", "--long-report"
		).parse(args);
		
		Set<LocaleProxy> locales = helper.getLocales(opts.get("--locales"));
		Set<LocaleGroup> localeGroups = helper.getLocaleGroups(opts.get("--locales"));
		Set<String> excludeList = getExcludeList(opts.get("--exclude-list"));
		
		this.eclipseRoot = new File(opts.get("--eclipse-archive"));
		this.locales = locales;
		this.localeGroups = localeGroups;
		this.translationsRoot = new File(opts.get("--translation-archive"));
		this.workingDirectory = new File(opts.get("--working-directory", "."));
		
		this.excludeList = excludeList;
		report = new File(opts.get("--coverage-report", new File(workingDirectory, "coverage.xml").getAbsolutePath()));
		
		if(!report.isAbsolute()){
			report = new File(workingDirectory, report.getPath());
		}
		
		reportFilters = buildFilterList(opts.get("--report-ignore-list", ""));
	
		includeXmlReport = opts.isSet("--xml");
		longReport = opts.isSet("--long-report");
		
		localeExtension = "";
		if (this.locales.size() == 1 && this.localeGroups.isEmpty()) {
			LocaleProxy singleLocale = this.locales.iterator().next();
			localeExtension += Messages.getString("Characters_underscore") + singleLocale.getName();	//$NON-NLS-1$
		}
	}
	
	private List<Filter> buildFilterList(String specifier){
		List<Filter> filters = new LinkedList<Filter>();
		
		for(String filter : specifier.split(" ")){
			filters.add(new Filter(filter));
		}
		
		return filters;
	}

	public EclipseTarget eclipseInstall() {
		try {
			if (eclipseInstall == null){
				long startEclipseArchivePopulation = System.currentTimeMillis();
				eclipseInstall = new EclipseTarget(eclipseRoot, this.excludeList);
				System.out.println(Messages.getString("Messages_parsing_eclipse_target"));	//$NON-NLS-1$	
				eclipseInstall.populatePlugins();
				System.out.println("Plugins: " + eclipseInstall.getPlugins().size() + " Features: " + eclipseInstall.getFeatures().size()); //$NON-NLS-1$ $NON-NLS-2$
				times.eclipseInstallPopulation = System.currentTimeMillis() - startEclipseArchivePopulation;
			}
		} catch (InvalidLocationException i) {
			System.out.println( Messages.getString("Error_invalid_eclipse_target") );	//$NON-NLS-1$
			System.exit(0);
		} catch (MissingLocationException m) {
			System.out.println( Messages.getString("Error_missing_eclipse_target") );	//$NON-NLS-1$
			System.exit(0);
		} catch (InvalidFilenameException i) {
			System.out.println( Messages.getString("Error_invalid_eclipse_target_name") );	//$NON-NLS-1$
			System.exit(0);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println( Messages.getString("Error_eclipse_target") );	//$NON-NLS-1$
			System.exit(0);
		}
		return eclipseInstall;
	}
	
	public Range compatibilityRange() {
		// TODO Auto-generated method stub
		return null;
	}

	public List<Filter> filters() {
		return reportFilters;
	}

	public boolean includePseudoTranslations() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean includeResource(PluginProxy plugin, ResourceProxy resource) {
		for(Filter filter : reportFilters){
			if(filter.matches(plugin, resource)){
				return filter.isInclusive();
			}
		}
		return true;
	}

	public Set<LocaleProxy> locales() {
		return locales;
	}


	public Set<LocaleGroup> localeGroups() {
		return localeGroups;
	}

	public Set<String> excludeList() {
		return excludeList;
	}

	public File reportLocation() {
		return report;
	}

	public Date timestamp() {
		return new Date();
	}

	public TranslationCatalogue translations() {
		if (translationCatalogue == null){
			try {
				if (localeGroups.isEmpty()) {
					translationCatalogue = new TranslationCatalogue(translationsRoot, locales);
				} else {
					locales.clear();
					translationCatalogue = new TranslationCatalogue(translationsRoot, localeGroups);
					locales.addAll(translationCatalogue.getAllLocales());
				}
			} catch (MissingLocationException m) {
				System.out.println( Messages.getString("Error_missing_translation_catalogue") );	//$NON-NLS-1$
				System.exit(0);
			} catch (InvalidLocationException i) {
				System.out.println( i.getMessage() );
				System.exit(0);
			} catch (InvalidFilenameException i) {
				System.out.println( Messages.getString("Error_invalid_translation_catalogue_name") );	//$NON-NLS-1$
				System.exit(0);
			}
		}
		return translationCatalogue;
	}

	public File workingDirectory() {
		return workingDirectory;
	}

	private static Set<String> getExcludeList(String specifier){
		Set<String> excludeList = new HashSet<String>();
		
		if(null == specifier){
			return excludeList;
		}
		
		for(String suffix : specifier.split( TOKEN )){ 
			excludeList.add(suffix);
		}

		return excludeList;
	}

	public void notifyProgress(String fragmentName) {
		// TODO Auto-generated method stub
	}

	public boolean includeXmlReport() {
		return includeXmlReport;
	}

	public boolean longReport() {
		return longReport;
	}
	
	public final String localeExtension() {
		return localeExtension;
	}
}