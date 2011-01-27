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
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.babel.build.core.LocaleGroup;
import org.eclipse.babel.build.core.LocaleProxy;
import org.eclipse.babel.build.core.PluginProxy;
import org.eclipse.babel.build.core.ResourceProxy;
import org.eclipse.babel.build.core.exceptions.InvalidFilenameException;
import org.eclipse.babel.build.core.exceptions.InvalidLocationException;
import org.eclipse.babel.build.core.exceptions.MissingLocationException;


public class TranslationCatalogue {
	
	private File rootDirectory;
	private Set<LocaleProxy> locales = new HashSet<LocaleProxy>();
	private Set<LocaleProxy> allLocales;
	
	private TranslationCatalogueParser catalogueParser;

	public TranslationCatalogue(File root, Collection<LocaleProxy> locales) 
			throws MissingLocationException, InvalidLocationException, InvalidFilenameException {
		this.rootDirectory = root.getAbsoluteFile();
		
		if (!this.rootDirectory.exists()) {
			throw new MissingLocationException();
		} else if (!this.rootDirectory.isDirectory()) {
			throw new InvalidFilenameException();
		} 
		
		this.locales.addAll(locales);
		this.allLocales = new HashSet<LocaleProxy>();
		
		catalogueParser = new TranslationCatalogueSimpleParser(this.rootDirectory, this.locales);
		this.allLocales = catalogueParser.findRelevantLocalesInCatalogue();	
		if (this.locales.isEmpty()) {
			this.locales = this.allLocales;
		}
	}
	
	public TranslationCatalogue(File root, Set<LocaleGroup> localeGroups) 
		throws MissingLocationException, InvalidLocationException, InvalidFilenameException {
		this.rootDirectory = root.getAbsoluteFile();
		
		if (!this.rootDirectory.exists()) {
			throw new MissingLocationException();
		} else if (!this.rootDirectory.isDirectory()) {
			throw new InvalidFilenameException();
		}
		
		this.allLocales = new HashSet<LocaleProxy>();
		
		catalogueParser = new TranslationCatalogueBulkParser(this.rootDirectory, localeGroups);
		this.allLocales = catalogueParser.findRelevantLocalesInCatalogue();
		this.locales = this.allLocales;
	}
	
	/**
	 * @param eclipseInstallPlugin
	 * @return The different versions (one for each locale) of the same plug-in within the catalogue.
	 */
	public Map<String, PluginProxy> getPluginForSpecifiedLocales (PluginProxy eclipseInstallPlugin) {
		return catalogueParser.getPluginForSpecifiedLocales(eclipseInstallPlugin);
	}
	
	/**
	 * @param eclipseInstallFeature
	 * @return The different versions (one for each locale) of the same feature within the catalogue.
	 */
	public Map<String, PluginProxy> getFeatureForSpecifiedLocales (PluginProxy eclipseInstallFeature) {
		return catalogueParser.getFeatureForSpecifiedLocales(eclipseInstallFeature);
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

	public Set<LocaleProxy> getAllLocales() {
		return allLocales;
	}

	public File getRootDirectory() {
		return rootDirectory;
	}
}
