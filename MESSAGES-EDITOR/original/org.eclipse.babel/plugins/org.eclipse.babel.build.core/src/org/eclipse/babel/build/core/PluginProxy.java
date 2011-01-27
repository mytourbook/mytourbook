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
import java.util.List;

public class PluginProxy {
	private String name;
	private File pluginLocation;
	private String version;
	private boolean isJar;
	private boolean isFeature;
	private List<ResourceProxy> resources;
	
	public PluginProxy(File pluginLocation, List<ResourceProxy> resources, boolean isJar, boolean isFeature){
		this.pluginLocation = pluginLocation;
		if(pluginLocation.getName().matches(Messages.getString("Patterns_plugin_name"))) { //$NON-NLS-1$
			this.name = pluginLocation.getName().substring(0, pluginLocation.getName().indexOf(Messages.getString("Characters_underscore"))); //$NON-NLS-1$
			this.version = pluginLocation.getName().substring(pluginLocation.getName().indexOf(Messages.getString("Characters_underscore")) + 1); //$NON-NLS-1$
			if(isJar) {
				this.version = version.replaceAll(Messages.getString("Extensions_jar"), ""); //$NON-NLS-1$
			}			
		}
		else {
			this.name = pluginLocation.getName();
			this.version = ""; //$NON-NLS-1$
		}
		this.resources = resources;
		this.isJar = isJar;
		this.isFeature = isFeature;
	}
	
	public String getName(){
		//TODO: stub
		return this.name;
	}
	
	public String getVersion(){
		//TODO: stub
		return this.version;
	}
	
	public List<ResourceProxy> getResources() {
		return resources;
	}

	public boolean isJar() {
		return isJar;
	}

	public File getPluginLocation() {
		return pluginLocation;
	}

	public boolean isFeature() {
		return isFeature;
	}
}
