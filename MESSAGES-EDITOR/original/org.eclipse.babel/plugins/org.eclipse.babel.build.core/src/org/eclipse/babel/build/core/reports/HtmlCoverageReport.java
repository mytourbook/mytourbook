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

import static org.eclipse.babel.build.core.xml.Builder.*;
import static org.eclipse.babel.build.core.xml.Html.*;


import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.babel.build.core.Configuration;
import org.eclipse.babel.build.core.LocaleProxy;
import org.eclipse.babel.build.core.PluginProxy;
import org.eclipse.babel.build.core.ResourceProxy;
import org.eclipse.babel.build.core.coverage.LanguagePackCoverageReport;
import org.eclipse.babel.build.core.coverage.PluginCoverageInformation;
import org.eclipse.babel.build.core.coverage.ResourceCoverageInformation;
import org.eclipse.babel.build.core.xml.Element;
import org.eclipse.babel.build.core.xml.XmlWriter;
import org.eclipse.babel.build.core.xml.Builder.ToNode;


public class HtmlCoverageReport implements CoverageReport{
	public class RollUpForLocale implements ToNode<LocaleProxy> {

		private final PluginCoverageInformation info;
		private final PluginProxy plugin;

		public RollUpForLocale(PluginCoverageInformation info) {
			this.info = info;
			this.plugin = info.getEclipseArchivePlugin();
		}

		public Element toNode(LocaleProxy t) {
			int min = Integer.MAX_VALUE;
			
			for(ResourceProxy resource : plugin.getResources()){
				boolean matched = resourceMatched(info, resource);
				boolean included = config.includeResource(plugin, resource);
				int score = CoverageReport.utils.calculateCoverageScore(t, resource, info);
				
				if(matched && included && score < min){
					min = score;
				}
			}
			
			return td("" + min).attribute("class", "" + completeness(min));
		}

	}

	public class PluginSummaryRow implements ToNode<PluginCoverageInformation>{
		public Element toNode(PluginCoverageInformation info) {
			PluginProxy plugin = info.getEclipseArchivePlugin();
			
			if(!hasMatchedResources(info)){
				return Element.utils.EMPTY_ELEMENT;
			}
			
			return tr(
				td(a("#" + plugin.getName(), plugin.getName())).attribute("class", "first"),
				sequence(locales, new RollUpForLocale(info))
			);
		}
	}
	
	public class UnmatchedResource implements ToNode<ResourceProxy> {
		private PluginProxy plugin;
		private final PluginCoverageInformation info;

		public UnmatchedResource(PluginCoverageInformation info){
			this.info = info;
			this.plugin = info.getEclipseArchivePlugin();
		}

		public Element toNode(ResourceProxy resource) {
			boolean matched = resourceMatched(info, resource);
			boolean included = config.includeResource(plugin, resource);
			
			if(matched || !included){
				return Element.utils.EMPTY_ELEMENT;
			}
			
			return li(resource.getRelativePath());
		}

	}


	public class UnmatchedResourceList implements
			ToNode<PluginCoverageInformation> {

		public Element toNode(PluginCoverageInformation info) {
			PluginProxy plugin = info.getEclipseArchivePlugin();
			
			if(hasMatchedResources(info)){
				return Element.utils.EMPTY_ELEMENT;
			}
			
			return nodes(
				h2(plugin.getName()),
				ul(sequence(plugin.getResources(), new UnmatchedResource(info)))
			);
		}

	}


	private final class ResourceCoverageRow implements ToNode<ResourceProxy> {
		private final PluginCoverageInformation info;
		private final PluginProxy plugin;

		private ResourceCoverageRow(PluginCoverageInformation info) {
			this.info = info;
			plugin = info.getEclipseArchivePlugin();
		}

		public Element toNode(final ResourceProxy resource) {
			if(!config.includeResource(plugin, resource)){
				return Element.utils.EMPTY_ELEMENT;
			}
			
			if(!resourceMatched(info, resource)){
				return Element.utils.EMPTY_ELEMENT;
			}
			
			return tr(
					td(resource.getRelativePath()).attribute("class", "first"), 
					sequence(locales, new ToNode<LocaleProxy>(){
						public Element toNode(LocaleProxy locale) {
							int score = CoverageReport.utils.calculateCoverageScore(locale, resource, info);
							return td("" + score ).attribute("class", completeness(score));
						}
					})
			);
		}
	}

	private static String completeness(int score){
		if(score < 25){
			return "missing";
		}
		
		if(score < 80){
			return "incomplete";
		}
		
		if(score < 100){
			return "almost-complete";
		}
		
		return "complete";
	}
	
	private final class MatchedPluginCoverageTable implements ToNode<PluginCoverageInformation> {
		public Element toNode(final PluginCoverageInformation info) {
			if(!hasMatchedResources(info)){
				return Element.utils.EMPTY_ELEMENT;
			}
			
			
			PluginProxy plugin = info.getEclipseArchivePlugin();
			String name = plugin.getName();
			return nodes(
				h2(a("#" + name, name).attribute("name", name).attribute("class", "target")),
				table(
					tr(th("Resource").attribute("class", "first"), sequence(locales, new ToNode<LocaleProxy>(){
						public Element toNode(LocaleProxy t) {
							return th(t.getName());
						}
					})),
					sequence(plugin.getResources(), new ResourceCoverageRow(info))
				)
			);
		}
	}


	private static final String TITLE = "Language Pack Coverage Report";
	private final LanguagePackCoverageReport coverage;
	private final Configuration config;
	private final List<LocaleProxy> locales;

	public HtmlCoverageReport(Configuration config, LanguagePackCoverageReport coverage){
		this.config = config;
		this.coverage = coverage;
		this.locales = sorted(config.locales(), LocaleProxy.NAME_COMPARATOR);
	}
	
	
	public boolean hasMatchedResources(PluginCoverageInformation info) {
		for(ResourceCoverageInformation coverage : info.getResourceCoverage().values()){
			boolean included = config.includeResource(info.getEclipseArchivePlugin(), coverage.getResource());
			boolean matched = resourceMatched(info, coverage.getResource());
			
			if(included && matched){
				return true;
			}
		}
		
		return false;
	}


	private boolean resourceMatched(PluginCoverageInformation info, ResourceProxy resource) {
		ResourceCoverageInformation coverage = info.getResourceCoverage().get(resource.getCanonicalPath());
		
		if(coverage == null){
			return false;
		}
		return any(coverage.getLocaleMatchMap().values());
	}


	private boolean any(Collection<Boolean> values) {
		for(Boolean value : values){
			if(value != null && value){
				return true;
			}
		}
		
		return false;
	}

	private static final int HEADER_WIDTH = 30;
	private static final int CELL_WIDTH = 4;
	
	public Element build() {
		return html(
				head(
					title(TITLE),
					style(
						"tr, th, td, table { border: 1px solid; }\n" +
						".missing { background-color: #FFBFBF; }\n" +
						".incomplete { background-color: #FFE6BF; }\n" +
						".almost-complete { background-color: #FFFFBF; }\n" +
						".complete { background-color: #BFFFBF; }\n" +
						"a.target { color: black; }\n" +
						String.format(".first { width: %dem; }\n", HEADER_WIDTH) +
						String.format("th, td { width: %dem; }\n", CELL_WIDTH) +
						String.format("table { table-layout: fixed; width: %dem; }", HEADER_WIDTH + locales.size() * CELL_WIDTH)
					)
				),
				body(
					h1(TITLE),
					h2("Plugin Coverage Summary Table"),
					table(
						tr(th("Plugin Name").attribute("class", "first"), sequence(locales, new ToNode<LocaleProxy>(){
							public Element toNode(LocaleProxy t) {
								return th(t.getName());
							}
						})),
						sequence(sorted(coverage.getPluginCoverageReports(), PluginCoverageInformation.NAME_COMPARATOR), new PluginSummaryRow())
					),
					
					sequence(
						sorted(coverage.getPluginCoverageReports(), PluginCoverageInformation.NAME_COMPARATOR), 
						new MatchedPluginCoverageTable()
					),
					unmatchedResources()
				)
		);
	}
	
	private Element unmatchedResources() {
		if(!config.longReport()){
			return Element.utils.EMPTY_ELEMENT;
		}
		
		return nodes(
			h1("Unmatched Resources"),
			sequence(
				sorted(coverage.getPluginCoverageReports(), PluginCoverageInformation.NAME_COMPARATOR), 
				new UnmatchedResourceList()
			)
		);
	}


	private static  <T> List<T> sorted(Iterable<T> ts, Comparator<T> cmp) {
		List<T> list = new LinkedList<T>();
		for(T t : ts){
			list.add(t);
		}
		
		Collections.sort(list, cmp);		
		return list;
	}


	public void render(OutputStream stream) throws Exception {
		build().render(new XmlWriter(stream));
	}
}
