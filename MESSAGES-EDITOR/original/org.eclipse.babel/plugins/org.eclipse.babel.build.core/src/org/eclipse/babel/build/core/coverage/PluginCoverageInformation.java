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
package org.eclipse.babel.build.core.coverage;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.babel.build.core.LocaleProxy;
import org.eclipse.babel.build.core.PluginProxy;
import org.eclipse.babel.build.core.ResourceProxy;


public class PluginCoverageInformation {
	public static final Comparator<PluginCoverageInformation> NAME_COMPARATOR = new Comparator<PluginCoverageInformation>(){
		public int compare(PluginCoverageInformation o1, PluginCoverageInformation o2) {
			String name1 = o1.getEclipseArchivePlugin().getName();
			String name2 = o2.getEclipseArchivePlugin().getName();
			return name1.compareTo(name2);
		}
	};
	
	private final PluginProxy eclipseArchivePlugin;
	private Map<String, ResourceCoverageInformation> resourceCoverage = 
		new HashMap<String, ResourceCoverageInformation>();
	private Map<LocaleProxy, Boolean> pluginMatchingPerLocale = new HashMap<LocaleProxy, Boolean>();
	
	public PluginCoverageInformation(PluginProxy eclipseArchivePlugin) {
		super();
		this.eclipseArchivePlugin = eclipseArchivePlugin;
	}

	/** Returns a PluginProxy which represents the plug-in in question. */
	public PluginProxy getEclipseArchivePlugin() {
		return eclipseArchivePlugin;
	}

	/** Returns a map which, for each locale, indicates whether or not plug-in was matched. */
	public Map<LocaleProxy, Boolean> getPluginMatchingPerLocale() {
		return this.pluginMatchingPerLocale;
	}
	
	/** Given a specific locale, indicates whether or not plug-in was matched. */
	public Boolean getPluginMatchingForLocale(String locale) {
		for (LocaleProxy localeFromList: pluginMatchingPerLocale.keySet()) {
			if (localeFromList.getName().compareToIgnoreCase(locale) == 0) {
				return this.pluginMatchingPerLocale.get(localeFromList);
			}
		}
		
		return false;
	}
	
	/** Records whether or not the plug-in in question was matched to a specific locale. */
	public void setPluginMatchingForLocale(LocaleProxy locale, Boolean matched) {
		this.pluginMatchingPerLocale.put(locale, matched);
	}
	
	/** Records whether or not a resource belonging to the plug-in in question was matched to a specific locale. */
	public void setResourceCoverageForLocale(LocaleProxy locale, ResourceProxy resource, Boolean matched) {
		if (this.resourceCoverage.containsKey(resource.getRelativePath())) {
			ResourceCoverageInformation info = this.resourceCoverage.get(resource.getRelativePath());
			info.setMatchingForLocale(locale, matched);
		} else {
			ResourceCoverageInformation info = new ResourceCoverageInformation(resource);
			info.setMatchingForLocale(locale, matched);
			this.resourceCoverage.put(resource.getRelativePath(), info);
			
		}
	}
	
	/** Records whether or not a resource belonging to the plug-in in question was matched to a specific locale. */
	public void setResourceCoverageForLocale(LocaleProxy locale, ResourceProxy resource, Boolean matched, int propertiesCoverage) {
		if (this.resourceCoverage.containsKey(resource.getRelativePath())) {
			ResourceCoverageInformation info = this.resourceCoverage.get(resource.getRelativePath());
			info.setMatchingForLocale(locale, matched);
			info.setMatchedPercentageForLocale(locale, propertiesCoverage);
		} else {
			ResourceCoverageInformation info = new ResourceCoverageInformation(resource);
			info.setMatchingForLocale(locale, matched);
			info.setMatchedPercentageForLocale(locale, propertiesCoverage);
			this.resourceCoverage.put(resource.getRelativePath(), info);
			
		}
	}

	/** Returns coverage information about resource belonging to the plug-in in question */
	public Map<String, ResourceCoverageInformation> getResourceCoverage() {
		return resourceCoverage;
	}
}
