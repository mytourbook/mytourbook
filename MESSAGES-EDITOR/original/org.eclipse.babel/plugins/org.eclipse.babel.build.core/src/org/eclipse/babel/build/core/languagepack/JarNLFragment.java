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
package org.eclipse.babel.build.core.languagepack;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.eclipse.babel.build.core.Configuration;
import org.eclipse.babel.build.core.LocaleProxy;
import org.eclipse.babel.build.core.Messages;
import org.eclipse.babel.build.core.PluginProxy;
import org.eclipse.babel.build.core.ResourceProxy;
import org.eclipse.babel.build.core.coverage.PluginCoverageInformation;
import org.eclipse.babel.build.core.eclipsetarget.EclipseTarget;
import org.eclipse.babel.build.core.eclipsetarget.FragmentEclipseArchiveMediator;
import org.eclipse.babel.build.core.eclipsetarget.FragmentEclipseInstallMediator;
import org.eclipse.babel.build.core.eclipsetarget.FragmentEclipseTargetMediator;
import org.eclipse.babel.build.core.translationcatalogue.TranslationCatalogue;


/**
 * 
 * Responsible for generating an NL Fragment as a jar in the language pack.  
 *
 */
public class JarNLFragment implements NLFragment {

	private File directory;
	private PluginProxy eclipseInstallPlugin;
	
	private Map<String, Set<Object>> propertiesMap = new HashMap<String, Set<Object>>();
	private FragmentEclipseTargetMediator mediator;
	
	public JarNLFragment(File directory, PluginProxy eclipseInstallPlugin, EclipseTarget eclipseTarget) throws Exception {
		this.directory = new File(directory.getAbsolutePath() + JAR_EXTENSION);
		this.eclipseInstallPlugin = eclipseInstallPlugin;
		if (eclipseTarget.isArchive()) {
			mediator = new FragmentEclipseArchiveMediator(this.eclipseInstallPlugin, eclipseTarget.getLocation());
		} else {
			mediator = new FragmentEclipseInstallMediator(this.eclipseInstallPlugin);
		}
	}
	
	public PluginCoverageInformation generateFragment(Configuration config) throws Exception {
	
		// Keeps track of whether or not the about.html file has been added to the fragment 
		boolean aboutAdded = false;
		
		TranslationCatalogue translationCatalogue = config.translations();
		Set <LocaleProxy> locales = config.locales();
		
		// An empty list of locales means all of them should be parsed
		if(locales.isEmpty() && config.localeGroups().isEmpty()) {
			locales.addAll(translationCatalogue.getAllLocales());
		}
		
		Map<String, PluginProxy> translationCataloguePluginMap = 
			getPluginOrFeatureForSpecifiedLocales(config, eclipseInstallPlugin);
			
		// The output stream that will be used to write to the jar nl fragment
		JarOutputStream out = null;
		FileOutputStream fileOut = null;
		
		PluginCoverageInformation coverage = new PluginCoverageInformation(eclipseInstallPlugin);
		
		try {
			/* 	If the eclipse target plug-in is a jar, then the set of properties for all
				of its properties files are stored in a map for quick access, in order to avoid
			 	having to open and close a stream for each properties file	*/
			if (this.eclipseInstallPlugin.isJar()) {
				this.propertiesMap = mediator.extractEclipseTargetProperties();
			}		
			
			for(LocaleProxy locale: locales) {
				
				boolean pluginIsMatchedToThisLocale = 
					translationCataloguePluginMap.containsKey(locale.getName());
									
				if ( pluginIsMatchedToThisLocale ) {				
					PluginProxy translationArchivePlugin = 
						translationCataloguePluginMap.get(locale.getName());
						
					for(ResourceProxy resource: eclipseInstallPlugin.getResources()){
						//If the resource is the about.html file
						if (resource.getRelativePath().equalsIgnoreCase(ABOUT_FILE) && !aboutAdded) {
							if(out == null) {
								fileOut = new FileOutputStream(directory);
								out = new JarOutputStream(fileOut, composeManifestContent(config.localeExtension()));
							}
							aboutAdded = mediator.writeAboutHtmlFile(resource.getFileResource(), out);
						} 
						//Want to make sure that this block of code is not executed for a Manifest file
						else if (!resource.getRelativePath().equalsIgnoreCase(MANIFEST_PATH)) {
							ResourceProxy translationResource = translationCatalogue.
								getResourceTranslation(translationArchivePlugin, resource);
							
							if(translationResource != null) {
								if(out == null) {
									fileOut = new FileOutputStream(directory);
									out = new JarOutputStream(fileOut, composeManifestContent(config.localeExtension()));
								}												
								writeResource(out, translationResource, locale);
								
								if (resource.getRelativePath().endsWith(Messages.getString("Extensions_properties"))) {		//$NON-NLS-1$
									// If the resource is a properties file, then compute property coverage
									int value = computePropertyCoverageForLocale(resource, translationResource);									
									coverage.setResourceCoverageForLocale(locale, resource, true, value);
								}
								else {
									//Mark that this resource has been matched for this locale
									coverage.setResourceCoverageForLocale(locale, resource, true);
								}
							}
							else {
								//Mark that this resource has NOT been matched for this locale
								coverage.setResourceCoverageForLocale(locale, resource, false);
							}
						}
					}
				}
				coverage.setPluginMatchingForLocale(locale, pluginIsMatchedToThisLocale);
			}
			// Complete the ZIP file
			if(out != null) {
				out.close();
				fileOut.close();
			}
		}
		catch (IOException e) {
			System.out.println(e.getMessage());
		}
		
		return coverage;
	}
	
	private int computePropertyCoverageForLocale(ResourceProxy eclipseTargetResource, ResourceProxy translationResource) throws Exception {		
		// Load the set of properties within the translated resource 
		InputStream transIn = new FileInputStream(translationResource.getFileResource());
		Properties transProperty = new Properties();
		transProperty.load(transIn);
		
		// Load the set of properties within the eclipse target resource
		Set<Object> properties;		
		if (this.eclipseInstallPlugin.isJar()) {
			properties = new HashSet<Object>(this.propertiesMap.get(eclipseTargetResource.getCanonicalPath()));
		} else {
			InputStream in = mediator.getResourceAsStream(eclipseTargetResource);
			Properties property = new Properties();
			if (in != null) {
				property.load(in);
				in.close();
			}
			properties = new HashSet<Object>(property.keySet());
		}
		transIn.close();
		
		// Compute the amount of eclipse target resource properties covered by the translation resource
		int initialSize = properties.size();
		initialSize = initialSize < 1 ? 1 : initialSize;
		properties.removeAll(transProperty.keySet());
		return (100*(initialSize - properties.size()))/initialSize;
	}
	
	private Manifest composeManifestContent(String localeExtension) throws Exception {
		Manifest manifest = new Manifest();
		
		Attributes attributes = manifest.getMainAttributes();
		
		attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0"); //$NON-NLS-1$
		attributes.putValue(Messages.getString("Manifest_key_bundle_name"), eclipseInstallPlugin.getName() + " " + Messages.getString("Suffixes_fragment_name")); //$NON-NLS-1$	$NON-NLS-2$ $NON-NLS-3$
		attributes.putValue(Messages.getString("Manifest_key_bundle_version"), eclipseInstallPlugin.getVersion());	//$NON-NLS-1$
		attributes.putValue( Messages.getString("Manifest_key_bundle_symbolic_name"), eclipseInstallPlugin.getName() + 
				Messages.getString("Extensions_nl") + localeExtension + Messages.getString("Manifest_value_bundle_symbolic_name_suffix") );	//$NON-NLS-1$ $NON-NLS-2$	$NON-NLS-3$
		
		if ( eclipseInstallPlugin.getName().contains(( Messages.getString("Prefixes_eclipse_plugin") )) ) { //$NON-NLS-1$
			attributes.putValue(Messages.getString("Manifest_key_bundle_vendor"), Messages.getString("Provider_name_eclipse"));	//$NON-NLS-1$ $NON-NLS-2$
		} else if ( eclipseInstallPlugin.getName().contains(( Messages.getString("Prefixes_ibm_plugin") )) ) { //$NON-NLS-1$
			attributes.putValue(Messages.getString("Manifest_key_bundle_vendor"), Messages.getString("Provider_name_ibm"));	//$NON-NLS-1$ $NON-NLS-2$
		} else {
			attributes.putValue(Messages.getString("Manifest_key_bundle_vendor"), Messages.getString("Provider_name_unknown"));	//$NON-NLS-1$ $NON-NLS-2$
		}
		
		attributes.putValue(Messages.getString("Manifest_key_fragment_host"), eclipseInstallPlugin.getName());	//$NON-NLS-1$

		return manifest;
	}

	private boolean writeResource(JarOutputStream out,
			ResourceProxy translationResource, LocaleProxy locale) {
		try {
			byte[] buf = new byte[1024];
			FileInputStream in = new FileInputStream(new File(translationResource.getFileResource().getAbsolutePath()));
		    
			// The path of the resource entry in the language pack
			String temp = determineTranslatedResourceName(translationResource, locale);			
			temp = temp.replace(File.separator, Messages.getString("Characters_entry_separator"));	//$NON-NLS-1$
			
	        // Add ZIP entry to output stream.
	        out.putNextEntry(new JarEntry(temp));

	        // Transfer bytes from the translation archive file to the new language pack ZIP file
	        int len;
	        while ((len = in.read(buf)) > 0) {
	            out.write(buf, 0, len);
	        }

	        // Complete the entry
	        out.closeEntry();
	        in.close();
	        return true;
		} catch (IOException e) {
			System.out.println(e.getMessage());
			return false;
		}
	}

	public String determineTranslatedResourceName(ResourceProxy resource, LocaleProxy locale) {
		String resourceName = resource.getRelativePath();
		
		/* 	If the resource was held in a jar resource within the plug-in, then the place-holder
			directory representing the jar resource (usually called pluginName_jar) must be removed
			from the path	*/
		if (resourceName.contains(JAR_RESOURCE_SUFFIX + File.separator)) {
			String target = resourceName.substring(0, resourceName.indexOf
					(JAR_RESOURCE_SUFFIX + File.separator) + JAR_RESOURCE_SUFFIX.length());
			resourceName = resourceName.replace(target + File.separator, "");	//$NON-NLS-1$
		}
		
		String resourceExtension = resourceName.substring(
				resourceName.lastIndexOf(Messages.getString("Characters_period"))); //$NON-NLS-1$
		return resourceName.substring(0, resourceName.lastIndexOf(Messages.getString("Characters_period"))) 
			+ Messages.getString("Characters_underscore") + locale.getName() + resourceExtension; //$NON-NLS-1$	$NON-NLS-2$
	}

	private Map<String, PluginProxy> getPluginOrFeatureForSpecifiedLocales(Configuration config, PluginProxy plugin) {
		if (plugin.isFeature()) {
			return config.translations().getFeatureForSpecifiedLocales(plugin);
		}
		return config.translations().getPluginForSpecifiedLocales(plugin);
	}
	/*
	private String incrementRelease(String oldVersion) {
		if (oldVersion.matches(VERSION_FORMAT_WITH_QUALIFIER)) {
			oldVersion = oldVersion.substring(0, oldVersion.lastIndexOf(PERIOD));
		}

		String newVersion = LEFT_SQUARE_BRACKET + oldVersion + ',';
		String oldMinor = oldVersion.substring(oldVersion.indexOf(PERIOD) + 1, oldVersion.lastIndexOf(PERIOD));
		String oldMicro = oldVersion.substring(oldVersion.lastIndexOf(PERIOD) + 1);

		if (oldMinor.compareTo(MAX_MINOR) == 0) {
			String major = Integer.toString(Integer.parseInt(oldVersion.substring(0, oldVersion.indexOf(PERIOD))) + 1);
			newVersion += major + PERIOD + MIN_MINOR + PERIOD + oldMicro + RIGHT_PARENTHESIS;
		} else {
			String major = oldVersion.substring(0, oldVersion.indexOf(PERIOD));
			String newMinor = Integer.toString(Integer.parseInt(oldMinor) + 1);
			newVersion += major + PERIOD + newMinor + PERIOD + oldMicro + RIGHT_PARENTHESIS;
		}

		return newVersion;
	}
	*/

}
