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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipFile;

import org.eclipse.babel.build.core.PluginProxy;
import org.eclipse.babel.build.core.exceptions.InvalidFilenameException;
import org.eclipse.babel.build.core.exceptions.InvalidLocationException;
import org.eclipse.babel.build.core.exceptions.MissingLocationException;


public class EclipseTarget {

	private final File location;
	private final boolean isArchive;
	//The lists of plug-ins/features will be stored in maps for faster lookup
	Map<String, PluginProxy> plugins = new HashMap<String, PluginProxy>();
	Map<String, PluginProxy> features = new HashMap<String, PluginProxy>();
	
	private Set<String> excludeList;
	
	/**
	 * @param plugins - The list of plug-ins chosen via the wizard.
	 */
	public EclipseTarget(List<PluginProxy> plugins) {
		for (PluginProxy plugin : plugins) {
			this.plugins.put(plugin.getName(), plugin);
		}
		
		this.isArchive = false;
		this.location = new File( plugins.get(0).getPluginLocation().getParent() );
	}
	
	public EclipseTarget(File location, Set<String> excludeList) throws MissingLocationException, InvalidFilenameException {
		this.location = location.getAbsoluteFile();
		
		if (!this.location.exists()) {
			throw new MissingLocationException();
		}
		
		this.excludeList = excludeList;
		
		if (location.isDirectory()) {
			this.isArchive = false;
		} else {
			this.isArchive = true;
		}
	}
	
	/** 
	 * Manipulates an EclipseParser to parse and populate the plug-ins and features
	 * in an Eclipse Install. 
	 */
	public void populatePlugins() throws Exception, InvalidLocationException {
		EclipseParser parser;
		
		if (!this.isArchive) {
			parser = new EclipseInstallParser(location, excludeList); 
		} else {
			ZipFile eclipseArchive = new ZipFile(location);
			parser = new EclipseArchiveInstallParser(eclipseArchive, location, excludeList);
		}
		
		parser.parse();
		
		this.plugins = parser.getPlugins();
		this.features = parser.getFeatures();
	}
	
	public File getLocation(){
		return location;
	}

	public Map<String, PluginProxy> getPlugins() {
		return Collections.unmodifiableMap(plugins);
	}

	public Map<String, PluginProxy> getFeatures() {
		return Collections.unmodifiableMap(features);
	}

	/** Returns whether or not EclipseInstall is an archive. */
	public boolean isArchive() {
		return isArchive;
	}
}
