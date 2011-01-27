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

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.eclipse.babel.build.core.LocaleProxy;
import org.eclipse.babel.build.core.coverage.LanguagePackCoverageReport;
import org.eclipse.babel.build.core.coverage.PluginCoverageInformation;
import org.eclipse.babel.build.core.coverage.ResourceCoverageInformation;


public class TextCoverageReport implements CoverageReport {

	private final LanguagePackCoverageReport coverage;

	public TextCoverageReport(LanguagePackCoverageReport coverage){
		this.coverage = coverage;
	}

	public void render(OutputStream stream) throws Exception {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream));
		
		writer.write("Number of total plugins: " + coverage.getPluginCoverageReports().size());	//$NON-NLS-1$
		writer.newLine();
		
		for (LocaleProxy locale: coverage.getMatchesPerLocale().keySet()) {
			writer.write("Number of matched plugins for " + locale.getName() + ": " + 
					coverage.getMatchesPerLocale().get(locale));	//$NON-NLS-1$ $NON-NLS-2$
			writer.newLine();
		}
		
		for (PluginCoverageInformation pluginReport: coverage.getPluginCoverageReports()) {
			writer.newLine();
			writer.write(pluginReport.getEclipseArchivePlugin().getName());
			writer.newLine();
			
			for (LocaleProxy locale : pluginReport.getPluginMatchingPerLocale().keySet()) {
				writer.write(locale.getName() + " -> " + pluginReport.getPluginMatchingPerLocale().get(locale));
				writer.newLine();
			}
			
			for (String resourcePath : pluginReport.getResourceCoverage().keySet()) {
				ResourceCoverageInformation resourceCoverageInfo = pluginReport.getResourceCoverage().get(resourcePath);
				writer.write(resourcePath);
				writer.newLine();
				
				for (LocaleProxy locale : resourceCoverageInfo.getRecordedLocales()) {
					writer.write("Locale " + locale.getName() + ": " + resourceCoverageInfo.getMatchingForLocale(locale));	//$NON-NLS-1$ $NON-NLS-2$
					writer.newLine();
					writer.write("Locale property coverage " + locale.getName() + ": " + resourceCoverageInfo.getMatchedPercentageForLocale(locale));	//$NON-NLS-1$ $NON-NLS-2$
					writer.newLine();
				}
			}
		}
	}
}
