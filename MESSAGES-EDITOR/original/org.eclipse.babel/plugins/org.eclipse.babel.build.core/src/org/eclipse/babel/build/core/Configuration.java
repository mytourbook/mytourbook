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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.babel.build.core.coverage.LanguagePackCoverageReport;
import org.eclipse.babel.build.core.eclipsetarget.EclipseTarget;
import org.eclipse.babel.build.core.reports.CoverageReport;
import org.eclipse.babel.build.core.reports.HtmlCoverageReport;
import org.eclipse.babel.build.core.reports.XmlCoverageReport;
import org.eclipse.babel.build.core.translationcatalogue.TranslationCatalogue;



/**
 * Defines the parameters which configure a run of the tool.
 */
public interface Configuration {
	public static class helper{
		/**
		 * This class should not be instantiated.
		 */
		private helper(){}
		
		/**
		 * Serializes a given configuration to an output stream so that it can be reused.
		 * 
		 * @param out The output stream to which the configuration should be serialized.
		 */
		public static void serialize(OutputStream out){
			// TODO: stub
		}
		
		/**
		 * Parses a stored configuration.
		 *  		  
		 * @param in The input stream from which a configuration should be deserialized.
		 * @return The configuration obtained from parsing the provided input stream.
		 */
		public static Configuration parse(InputStream in){
			// TODO: stub
			return null; 
		}
		
		public static Set<LocaleProxy> parseLocales(String specifier){
			if(null == specifier || "*".equals(specifier)){
				return LocaleProxy.ALL;
			}
			
			Set<LocaleProxy> set = new HashSet<LocaleProxy>();
			for(String name : specifier.split(",")){
				set.add(new LocaleProxy(name));
			}
			return set;
		}
		
		public static void printCoverageReport(CoverageReport report, File file) throws Exception{
			
			FileOutputStream out = new FileOutputStream(file);
			try{
				report.render(out);
			} finally {
				out.close();
			}
		}
		
		public static void printLanguagePackResult(final Configuration config, LanguagePackCoverageReport coverage) throws Exception {
			XmlCoverageReport report = new XmlCoverageReport(config, coverage);
			HtmlCoverageReport htmlReport = new HtmlCoverageReport(config, coverage);
			
			if(config.includeXmlReport()){
				printCoverageReport(report, config.reportLocation());
			}
			
			printCoverageReport(htmlReport, new File(config.reportLocation().getParent(), "coverage.html"));
		}

		public static Set<LocaleProxy> getLocales(String specifier){
			if(null == specifier || LocaleGroup.GROUP_ALL.getName().equals(specifier)){
				return LocaleProxy.ALL;
			}
		
			Set<LocaleProxy> locales = new HashSet<LocaleProxy>();
			for(String locale : specifier.split( CommandLineConfiguration.TOKEN )){
				if (! LocaleGroup.isValidGroupName(locale) && !LocaleGroup.isValidGroupFullName(locale)){ 
					locales.add(new LocaleProxy(locale));
				}
			}
		
			return locales;
		}

		public static Set<LocaleGroup> getLocaleGroups(String specifier){
			Set<LocaleGroup> set = new HashSet<LocaleGroup>();
			if(null == specifier) {
				return set;
			}
			
			if (LocaleGroup.GROUP_ALL.getName().equals(specifier)){
				set.add(LocaleGroup.GROUP_ALL);
			}
		
			for(String token : specifier.split( CommandLineConfiguration.TOKEN )){
				if (LocaleGroup.isValidGroupName(token)) {
					set.add(LocaleGroup.get(token));
				}
				else if(LocaleGroup.isValidGroupFullName(token)) {
					set.add(LocaleGroup.getByFullName(token));
				}
			}
		
			return set;
		}
	}
	
	/**
	 * Retrieve the eclipse install for which the language pack should be generated.
	 * 
	 * @return The eclipse install for which the language pack should be generated. 
	 */
	public EclipseTarget eclipseInstall();
	
	/**
	 * Retrieve the translations from which the translated resources should be retrieved.
	 * 
	 * @return The translations from which the translated resources should be retrieved. 
	 */
	public TranslationCatalogue translations();
	
	/**
	 * Retrieve the timestamp to use in output files.
	 * 
	 * @return Timestamp to use in output files.
	 */
	public Date timestamp();
	
	/**
	 * Retrieve the working directory into which generated artifacts should be stored.
	 * 
	 * @return The working directory into which generated artifacts should be stored.
	 */
	public File workingDirectory();
	
	/**
	 * Retrieve whether or not to include pseudo-translations for missing strings in the generated language pack.
	 * 
	 * @return Whether or not to include pseudo-translations for missing strings in the generated lanaguage pack.
	 */
	public boolean includePseudoTranslations();
	
	/**
	 * Retrieve the range of Eclipse versions the generated lanaguage pack will be compatible with.
	 * 
	 * @return The range of Eclipse versions the generated language pack will be compatible with.
	 */
	public Range compatibilityRange();
	
	/**
	 * Retrieve the location of where the coverage report should be stored.
	 * 
	 * @return The location to which the coverage report should be stored.
	 */
	public File reportLocation();
	
	/**
	 * Queries the configuration as to whether a resource should be included in the coverage report. 
	 * 
	 * @param resouce The resource whose inclusion is at question.
	 * @return Whether or not the resource should be included in the coverage report.
	 */
	public boolean includeResource(PluginProxy plugin, ResourceProxy resouce);
	
	/**
	 * Retrieve the list of filters which specify which resources should be included in the coverage report.
	 * 
	 * @return The list of filters used to select which resources should be included in the coverage report.
	 */
	public List<Filter> filters();
	
	/**
	 * Retrieve the set of locales that should be included in the generated language pack.
	 * 
	 * @return The set of locales that should be included in the generated language pack. 
	 */
	public Set<LocaleProxy> locales();
	
	/**
	 * Retrieve the locale groups that should be included in the generated language pack.
	 * 
	 * @return The locale groups that should be included in the generated language pack. 
	 */
	public Set<LocaleGroup> localeGroups();
	
	/**
	 * Retrieve the list of filename suffixes that should be excluded when parsing the eclipse target.
	 * 
	 * @return The list of filename suffixes that should be excluded when parsing the eclipse target. 
	 */
	public Set<String> excludeList();
	
	/**
	 * Provides notification regarding progress. 
	 * 
	 * @param fragmentName The name of the plugin/feature for which a fragement is being created.
	 */
	public void notifyProgress(String fragmentName);
	
	public boolean includeXmlReport();
	
	public boolean longReport();
	
	public String localeExtension();
}
