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
package org.eclipse.babel.build.core.reports;

import java.io.OutputStream;

import org.eclipse.babel.build.core.LocaleProxy;
import org.eclipse.babel.build.core.ResourceProxy;
import org.eclipse.babel.build.core.coverage.PluginCoverageInformation;
import org.eclipse.babel.build.core.coverage.ResourceCoverageInformation;


public interface CoverageReport {
	public static class utils{
		public static final int calculateCoverageScore(LocaleProxy locale, ResourceProxy resource, PluginCoverageInformation info){
			ResourceCoverageInformation resourceInfo = info.getResourceCoverage().get(resource.getRelativePath());
			
			if(resourceInfo == null){
				return 0;
			}
			
			Boolean matchingForLocale = resourceInfo.getMatchingForLocale(locale);
			if(matchingForLocale == null){
				return 0;
			}
			
			Integer coverage = resourceInfo.getMatchedPercentageForLocale(locale);
			if(coverage != null){
				return coverage;
			}
			
			return matchingForLocale ? 100 : 0;
		}
	}
	
	public void render(OutputStream stream) throws Exception;
}
