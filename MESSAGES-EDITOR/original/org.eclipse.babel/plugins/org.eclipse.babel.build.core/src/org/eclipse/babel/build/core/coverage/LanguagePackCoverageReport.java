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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.babel.build.core.LocaleProxy;


public class LanguagePackCoverageReport implements CoverageReport {
	private List<PluginCoverageInformation> pluginCoverageReports = new ArrayList<PluginCoverageInformation>();
	private Map<LocaleProxy, Integer> matchesPerLocale = new HashMap<LocaleProxy, Integer>();
	private List<LocaleProxy> locales = new ArrayList<LocaleProxy>();
	
	public LanguagePackCoverageReport(Collection<LocaleProxy> locales) {
		super();
		this.locales.addAll(locales);
		initializeMap();
	}
	
	/** Adds coverage information about a single plug-in to overall coverage report. */
	public void addPluginCoverageToReport(PluginCoverageInformation pluginCoverageInformation) {		
		this.pluginCoverageReports.add(pluginCoverageInformation);
		
		for (LocaleProxy locale : pluginCoverageInformation.getPluginMatchingPerLocale().keySet()) {
			if (pluginCoverageInformation.getPluginMatchingPerLocale().get(locale)) {
				this.matchesPerLocale.put(locale, this.matchesPerLocale.get(locale) + 1);
			}
		}
	}

	/** Returns coverage information about each individual plug-in in the Eclipse install. */
	public List<PluginCoverageInformation> getPluginCoverageReports() {
		return pluginCoverageReports;
	}
	
	/** Returns number of matched plug-ins for each locale. */
	public Map<LocaleProxy, Integer> getMatchesPerLocale() {
		return matchesPerLocale;
	}
	
	private void initializeMap() {
		for (LocaleProxy locale : locales) {
			this.matchesPerLocale.put(locale, 0);
		}
	}
}
