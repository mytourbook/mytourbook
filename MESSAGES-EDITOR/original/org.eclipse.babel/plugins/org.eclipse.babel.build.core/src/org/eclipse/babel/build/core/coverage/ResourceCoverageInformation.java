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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.babel.build.core.LocaleProxy;
import org.eclipse.babel.build.core.ResourceProxy;


public class ResourceCoverageInformation {
	private final ResourceProxy resource;
	private Map<LocaleProxy, Boolean> matchingByLocale = new HashMap<LocaleProxy, Boolean>();
	private Map<LocaleProxy, Integer> matchedPercentageForLocale = new HashMap<LocaleProxy, Integer>();
	
	public ResourceCoverageInformation(ResourceProxy resource){
		this.resource = resource;
	}

	public ResourceProxy getResource() {
		return resource;
	}
	
	public void setMatchingForLocale(LocaleProxy locale, Boolean matched) {
		this.matchingByLocale.put(locale, matched);
	}
	
	public Boolean getMatchingForLocale(LocaleProxy locale) {
		return this.matchingByLocale.get(locale);
	}
	
	public Boolean getMatchingForLocale(String locale) {
		for (LocaleProxy localeFromList: matchingByLocale.keySet()) {
			if (localeFromList.getName().compareToIgnoreCase(locale) == 0) {
				return this.matchingByLocale.get(localeFromList);
			}
		}
		
		return false;
	}
	
	public void setMatchedPercentageForLocale(LocaleProxy locale, Integer value) {
		this.matchedPercentageForLocale.put(locale, value);
	}
	
	public Integer getMatchedPercentageForLocale(LocaleProxy locale) {
		return this.matchedPercentageForLocale.get(locale);
	}
	
	public Set<LocaleProxy> getRecordedLocales() {
		return this.matchingByLocale.keySet();
	}
	
	public Map<LocaleProxy, Boolean> getLocaleMatchMap(){
		return Collections.unmodifiableMap(matchingByLocale);
	}
}
