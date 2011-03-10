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
package org.eclipse.babel.build.core.languagepack;

import java.io.File;

import org.eclipse.babel.build.core.Configuration;
import org.eclipse.babel.build.core.LocaleProxy;
import org.eclipse.babel.build.core.Messages;
import org.eclipse.babel.build.core.ResourceProxy;
import org.eclipse.babel.build.core.coverage.PluginCoverageInformation;



public interface NLFragment {
		
	public final String META_INF_DIR_NAME = Messages.getString("Paths_meta_inf_directory"); //$NON-NLS-1$
	public final String MANIFEST_FILE_NAME = Messages.getString("Filename_manifest"); //$NON-NLS-1$
	public final String MANIFEST_PATH = META_INF_DIR_NAME + File.separator + MANIFEST_FILE_NAME;
	public final String MANIFEST_ASSIGN = Messages.getString("Characters_manifest_assign") + " "; //$NON-NLS-1$	$NON-NLS-2$
	public final String SLASH = Messages.getString("Characters_entry_separator");	//$NON-NLS-1$
	public final String JAR_RESOURCE_SUFFIX = Messages.getString("Suffixes_jar_resource_folder");	//$NON-NLS-1$
	public final String JAR_EXTENSION = Messages.getString("Extensions_jar"); //$NON-NLS-1$
	public final String ABOUT_FILE = Messages.getString("Paths_about_html");	//$NON-NLS-1$
	
	/*
	public final String PERIOD = "."; //$NON-NLS-1$
	public final String MIN_MINOR = "0"; //$NON-NLS-1$
	public final String MAX_MINOR = "9"; //$NON-NLS-1$
	public final String LEFT_SQUARE_BRACKET = "["; //$NON-NLS-1$
	public final String RIGHT_PARENTHESIS = ")"; //$NON-NLS-1$
	public final String DEFAULT_VERSION = "1.0.0"; //$NON-NLS-1$
	public final String VERSION_FORMAT_WITH_QUALIFIER = "\\d+\\.\\d+\\.\\d+\\..+"; //$NON-NLS-1$
	*/
	
	/** Generates an NLFragment for a specific plug-in, and returns coverage information about that plug-in. */
	public PluginCoverageInformation generateFragment(Configuration config) throws Exception;
	
	/** Given a resource and locale, determines name of the resource generated in the language pack. */
	public String determineTranslatedResourceName(ResourceProxy resource, LocaleProxy locale);
}
