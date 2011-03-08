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
/**
 * 
 */
package org.eclipse.babel.build.core.reports;

import static org.eclipse.babel.build.core.xml.Builder.sequence;
import static org.eclipse.babel.build.core.xml.Coverage.archive;
import static org.eclipse.babel.build.core.xml.Coverage.coverage;
import static org.eclipse.babel.build.core.xml.Coverage.locale;
import static org.eclipse.babel.build.core.xml.Coverage.locales;
import static org.eclipse.babel.build.core.xml.Coverage.output;
import static org.eclipse.babel.build.core.xml.Coverage.plugin;
import static org.eclipse.babel.build.core.xml.Coverage.plugins;
import static org.eclipse.babel.build.core.xml.Coverage.resource;
import static org.eclipse.babel.build.core.xml.Coverage.translations;

import java.io.OutputStream;
import java.util.Date;

import org.eclipse.babel.build.core.Configuration;
import org.eclipse.babel.build.core.LocaleProxy;
import org.eclipse.babel.build.core.PluginProxy;
import org.eclipse.babel.build.core.ResourceProxy;
import org.eclipse.babel.build.core.coverage.LanguagePackCoverageReport;
import org.eclipse.babel.build.core.coverage.PluginCoverageInformation;
import org.eclipse.babel.build.core.xml.Element;
import org.eclipse.babel.build.core.xml.XmlWriter;
import org.eclipse.babel.build.core.xml.Builder.ToNode;


public class XmlCoverageReport implements CoverageReport{
	public class ResourceToNode implements ToNode<ResourceProxy> {

		private final PluginCoverageInformation info;
		private final PluginProxy plugin;

		public ResourceToNode(PluginCoverageInformation info) {
			this.info = info;
			plugin = info.getEclipseArchivePlugin();
		}

		public Element toNode(final ResourceProxy resource) {
			if(!config.includeResource(plugin, resource)){
				return resource(resource.getRelativePath(), true);
			}
			
			return resource(resource.getRelativePath(),
				sequence(config.locales(), new ToNode<LocaleProxy>(){
					public Element toNode(LocaleProxy locale) {
						int score = CoverageReport.utils.calculateCoverageScore(locale, resource, info);
						return locale(locale.getName(), score);
					}
				}));
		}
	}

	public static class LocaleToNode implements ToNode<LocaleProxy> {
		public Element toNode(LocaleProxy locale) {
			return locale(locale.getName());
		}
	}

	private final Configuration config;
	private final LanguagePackCoverageReport coverage;

	public XmlCoverageReport(Configuration config, LanguagePackCoverageReport coverage){
		this.config = config;
		this.coverage = coverage;
	}

	public Element build() {
		return coverage(config.timestamp() == null ? new Date().toString() : config.timestamp().toString(),
				archive(config.eclipseInstall().getLocation().getAbsolutePath()),
				translations(config.translations().getRootDirectory().getAbsolutePath()),
				output(config.workingDirectory().getAbsolutePath()),
				locales(sequence(config.locales(), new LocaleToNode())),
				plugins(sequence(coverage.getPluginCoverageReports(), new PluginToNode()))
		);
	}

	private class PluginToNode implements ToNode<PluginCoverageInformation> {
		
		public Element toNode(PluginCoverageInformation info) {
			PluginProxy plugin = info.getEclipseArchivePlugin();
			return plugin(plugin.getName(), plugin.getVersion(),
				sequence(plugin.getResources(), new ResourceToNode(info))
			);
		}
		
	}

	public void render(OutputStream stream) throws Exception {
		build().render(new XmlWriter(stream));
	}
}