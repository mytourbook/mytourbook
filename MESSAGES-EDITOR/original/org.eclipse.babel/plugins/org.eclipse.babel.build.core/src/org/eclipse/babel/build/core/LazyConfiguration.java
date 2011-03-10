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
import org.eclipse.babel.build.core.translationcatalogue.TranslationCatalogue;


/**
 * Implements a configuration that can be built up incrementally. 
 * It has two states:
 * <ul>
 *    <li>Unvalidated</li>
 *    <li>Validated</li>
 * </ul>
 * 
 * When it is initially created it is in the unvalidated state, in this state the setX, isValidated and validate methods may
 * be called, but all other methods will throw an IllegalStateException. Once the validate method is called, the 
 * state of each field will be validated, and if all fields are valid, it will transition to the validated state.
 * Otherwise, a {@link azure.build.core.ValidationException} will be thrown.
 * 
 * Once in the validated state, the setX methods may not be called, but the other methods can be called normally.
 */
public class LazyConfiguration implements Configuration {
	public class ValidationException extends Exception{
		public ValidationException(String string) {
			super(string);
		}

		private static final long serialVersionUID = 4547650327498908947L;
	}
	
	
	private EclipseTarget eclipseTarget = null;
	private TranslationCatalogue translationCatalogue = null;
	
	private File workingDirectory = new File(".");
	private File report = null;
	private File eclipseRoot;
	private File translationsRoot;
	
	private boolean validated = false;
	private boolean includePseudoTranslations = false;
	private boolean includeXmlReport = false;
	private boolean longReport = false;
	private String localeExtension;
	
	private final List<Filter> filters = new LinkedList<Filter>();
	private final Set<LocaleProxy> locales = new HashSet<LocaleProxy>();
	private final Set<LocaleGroup> localeGroups = new HashSet<LocaleGroup>();
	private final Set<String> excludeList =  new HashSet<String>();
	
	public EclipseTarget eclipseInstall() {
		if(!isValidated()){
			throw new IllegalStateException("LazyConfiguration must be validated before this method may be called");
		}
		try {
			if(eclipseTarget == null){
				eclipseTarget = new EclipseTarget(eclipseRoot, new HashSet<String>());
					eclipseTarget.populatePlugins();		
			}
		} catch (Exception e) {}
		return eclipseTarget;
	}
	
	public void setArchiveRoot(File root){
		if(isValidated()){
			throw new IllegalStateException("setX methods cannot be called once the instance has been validated.");
		}
		
		eclipseRoot = root;
	}

	public Range compatibilityRange() {
		if(!isValidated()){
			throw new IllegalStateException("LazyConfiguration must be validated before this method may be called");
		}
		
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not implemented yet!");
	}
	
	public void setCompatibilityRange(String range){
		if(isValidated()){
			throw new IllegalStateException("setX methods cannot be called once the instance has been validated.");
		}
		
		// TODO
		throw new UnsupportedOperationException("Not implemented yet!");
	}

	public List<Filter> filters() {
		if(!isValidated()){
			throw new IllegalStateException("LazyConfiguration must be validated before this method may be called");
		}
		
		return filters ;
	}
	
	public void addFilters(List<Filter> filters){
		if(isValidated()){
			throw new IllegalStateException("setX methods cannot be called once the instance has been validated.");
		}
		this.filters.addAll(filters);
	}

	public boolean includePseudoTranslations() {
		if(!isValidated()){
			throw new IllegalStateException("LazyConfiguration must be validated before this method may be called");
		}
		
		return includePseudoTranslations;
	}
	
	public void setIncludePseudoTranslations(boolean include){
		if(isValidated()){
			throw new IllegalStateException("setX methods cannot be called once the instance has been validated.");
		}
		
		this.includePseudoTranslations = include;
	}
		
	public boolean includeResource(PluginProxy plugin, ResourceProxy resource) {
		if(!isValidated()){
			throw new IllegalStateException("LazyConfiguration must be validated before this method may be called");
		}
		
		for(Filter filter : filters){
			if(filter.matches(plugin, resource)){
				return filter.isInclusive();
			}
		}
		
		return true;
	}

	public Set<LocaleProxy> locales() {
		if(!isValidated()){
			throw new IllegalStateException("LazyConfiguration must be validated before this method may be called");
		}
		
		return locales;
	}
	
	public void addLocales(Set<LocaleProxy> locales){
		if(isValidated()){
			throw new IllegalStateException("setX methods cannot be called once the instance has been validated.");
		}
		
		this.locales.addAll(locales);
	}

	public File reportLocation() {
		if(!isValidated()){
			throw new IllegalStateException("LazyConfiguration must be validated before this method may be called");
		}
		
		if(report == null){
			return new File(workingDirectory, "coverage.xml");
		}
		
		return report;
	}
	
	public void setReportLocation(File file){
		if(isValidated()){
			throw new IllegalStateException("setX methods cannot be called once the instance has been validated.");
		}
		
		report = file;
	}

	public Date timestamp() {
		if(!isValidated()){
			throw new IllegalStateException("LazyConfiguration must be validated before this method may be called");
		}
		return new Date();
	}

	public TranslationCatalogue translations() {
		if(!isValidated()){
			throw new IllegalStateException("LazyConfiguration must be validated before this method may be called");
		}
		
		try {
			if(translationCatalogue == null){
				if(localeGroups.isEmpty()){
					translationCatalogue = new TranslationCatalogue(translationsRoot, locales);
				} else {
					locales.clear();
					translationCatalogue = new TranslationCatalogue(translationsRoot, localeGroups);
					locales.addAll(translationCatalogue.getAllLocales());
				}
			}
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		
		return translationCatalogue;
	}
	
	public void setTranslationsRoot(File translations){
		if(isValidated()){
			throw new IllegalStateException("setX methods cannot be called once the instance has been validated.");
		}
		this.translationsRoot = translations;
	}

	public File workingDirectory() {
		if(!isValidated()){
			throw new IllegalStateException("LazyConfiguration must be validated before this method may be called");
		}
		
		return workingDirectory;
	}
	
	public void setWorkingDirectory(File file){
		if(isValidated()){
			throw new IllegalStateException("setX methods cannot be called once the instance has been validated.");
		}
		workingDirectory = file;
	}

	public void validate() throws ValidationException{
		if(eclipseRoot == null || !eclipseRoot.exists()){
			throw new ValidationException("Eclipse root does not exist.");
		}
		
		if(translationsRoot == null || !translationsRoot.exists()){
			throw new ValidationException("Translation root does not exist.");
		}
		
		if(workingDirectory == null || !workingDirectory.exists()){
			throw new ValidationException("Working directory does not exist.");
		}
		
		validated = true;
	}
	
	public boolean isValidated() {
		return validated;
	}

	public Set<LocaleGroup> localeGroups() {
		if(!isValidated()){
			throw new IllegalStateException("LazyConfiguration must be validated before this method may be called");
		}
		
		return localeGroups;
	}
	
	public void addLocaleGroups(Set<LocaleGroup> localeGroups){
		if(isValidated()){
			throw new IllegalStateException("setX methods cannot be called once the instance has been validated.");
		}
		
		this.localeGroups.addAll(localeGroups);
	}
	
	public Set<String> excludeList() {
		if(!isValidated()){
			throw new IllegalStateException("LazyConfiguration must be validated before this method may be called");
		}
		
		return excludeList;
	}
	
	public void addExclude(String exclude){
		this.excludeList.add(exclude);
	}

	public void addFilter(Filter filter) {
		filters.add(filter);
	}

	public void notifyProgress(String fragmentName) {
		// TODO Auto-generated method stub
	}

	public boolean includeXmlReport() {
		if(!isValidated()){
			throw new IllegalStateException("LazyConfiguration must be validated before this method may be called");
		}
		
		return includeXmlReport;
	}
	
	public void setIncludeXmlReport(boolean include){
		if(isValidated()){
			throw new IllegalStateException("setX methods cannot be called once the instance has been validated.");
		}
		
		includeXmlReport = include;
	}

	public boolean longReport() {
		if(!isValidated()){
			throw new IllegalStateException("LazyConfiguration must be validated before this method may be called");
		}
		
		return longReport;
	}
	
	public void setLongReport(boolean longReport){
		if(isValidated()){
			throw new IllegalStateException("setX methods cannot be called once the instance has been validated.");
		}
		
		this.longReport = longReport;
	}
	
	public String localeExtension() {
		if(!isValidated()){
			throw new IllegalStateException("LazyConfiguration must be validated before this method may be called");
		}
		
		return localeExtension;
	}
}
