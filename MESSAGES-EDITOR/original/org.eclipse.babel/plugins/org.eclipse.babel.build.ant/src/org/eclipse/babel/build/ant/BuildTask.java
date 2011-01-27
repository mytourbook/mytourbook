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
package org.eclipse.babel.build.ant;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.babel.build.core.Configuration;
import org.eclipse.babel.build.core.LazyConfiguration;
import org.eclipse.babel.build.core.coverage.LanguagePackCoverageReport;
import org.eclipse.babel.build.core.languagepack.LanguagePack;


/**
 * Task used to execute the tool from an ant build script.
 * Builds a {@link org.eclipse.babel.build.core.LazyConfiguration} then 
 * passes it to {@link org.eclipse.babel.build.core.languagepack.LanguagePack}.
 * 
 * <p>All methods are called reflectively by ant depending on attributes
 * supplied by the user in the build file. For example:
 * <pre>
 * &lt;nlsbuild 
 *	eclipse="/tmp/eclipse.zip"
 *	translations="/tmp/translations"
 *	locales="en,zh-TW"
 * /&gt;
 * </pre>
 * 
 * Would result in the following calls:
 * <pre>
 * BuildTask nlsbuild = new BuildTask();
 * nlsbuild.setEclipse("/tmp/eclipse.zip");
 * nlsbuild.setTranslations("/tmp/translations");
 * nlsbuild.setLocales("en,zh_TW");
 * nlsbuild.execute();
 * </pre>
 */

public class BuildTask extends Task {
	public static class Exclude{
		String fileType = "";
		
		public void setFileType(String fileType){
			this.fileType = fileType;
		}
	}
	
	public static class Excludes{
		List<Exclude> fileTypes = new LinkedList<Exclude>();
		
		public Exclude createExclude(){
			Exclude exclude = new Exclude();
			fileTypes.add(exclude);
			return exclude;
		}
	}
	
	private final LazyConfiguration config = new LazyConfiguration();
	private CoverageReport coverageReport = new CoverageReport();
	private Excludes excludes = new Excludes();
	
	
	/**
	 * Constructs a new BuildTask. Required by ant.
	 */
	public BuildTask(){
		super();
	}
	
	/**
	 * Sets the working directory for the build. Called by ant.
	 * 
	 * @param file Path to the working directory.
	 */
	public void setWorkingDirectory(String file){
		config.setWorkingDirectory(new File(file));
	}
	
	/**
	 * Sets the eclipse archive to be used. Called by ant.
	 * 
	 * @param file Path to the Eclipse Archive.
	 */
	public void setEclipse(String file){
		config.setArchiveRoot(new File(file));
	}
	
	public void setLongReport(boolean longReport){
		config.setLongReport(longReport);
	}
	
	public void setIncludeXmlReport(boolean xml){
		config.setIncludeXmlReport(xml);
	}
	
	/**
	 * Sets the locales to be used. Called by ant.
	 * 
	 * @param specifier List of locales to be used (of the format "en,zh_TW").
	 */
	public void setLocales(String specifier){
		config.addLocales(Configuration.helper.getLocales(specifier));
		config.addLocaleGroups(Configuration.helper.getLocaleGroups(specifier));
	}
	
	/**
	 * Sets the translations to be used. Called by ant.
	 * 
	 * @param file Path to the translations.
	 */
	public void setTranslations(String file){
		config.setTranslationsRoot(new File(file));
	}
	
	/**
	 * Sets whether to include pseudo-translations in the generated 
	 * translation catalog. Called by ant.
	 * 
	 * @param include Whether to include pseudo-translations.
	 */
	public void setIncludePseudoTranslations(boolean include){
		config.setIncludePseudoTranslations(include);
	}
	
	public CoverageReport createCoverageReport(){
		coverageReport = new CoverageReport();
		return coverageReport;
	}
	
	public Excludes createExcludes(){
		excludes = new Excludes();
		return excludes;
	}
	
	/**
	 * Build the translation catalog. Called by ant.
	 */
	@Override
	public void execute() throws BuildException {
		try {
			for(Ignore ignore : coverageReport.ignores()){
				config.addFilter(ignore.toFilter());
			}
			
			for(Exclude exclude : excludes.fileTypes){
				config.addExclude(exclude.fileType);
				
			}
			
			config.validate();
			System.out.println("Parsing language pack...");
			LanguagePack languagePack = new LanguagePack(config);
			LanguagePackCoverageReport coverage = languagePack.generate();
			System.out.println("Printing coverage report...");
			Configuration.helper.printLanguagePackResult(config, coverage);
			System.out.println("DONE");
		} catch (Exception e) {
			System.err.println(e.getMessage());
			throw new BuildException(e);
		}
	}
}
