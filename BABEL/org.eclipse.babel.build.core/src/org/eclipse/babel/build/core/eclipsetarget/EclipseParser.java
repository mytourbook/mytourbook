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
package org.eclipse.babel.build.core.eclipsetarget;

import java.util.Map;

import org.eclipse.babel.build.core.Messages;
import org.eclipse.babel.build.core.PluginProxy;


public interface EclipseParser {
	
	public final String JAR_EXTENSION = Messages.getString("Extensions_jar"); //$NON-NLS-1$
	public final String DIRECTORY_SUFFIX = Messages.getString("Suffixes_directory"); //$NON-NLS-1$
	public final String PLUGINS_PATH = Messages.getString("Paths_plugins_directory"); //$NON-NLS-1$
	public final String FEATURES_PATH = Messages.getString("Paths_features_directory"); //$NON-NLS-1$
	public final String SOURCE_SUFFIX = Messages.getString("Patterns_suffixes_source"); //$NON-NLS-1$
	public final String PLUGIN_MASK = Messages.getString("Masks_plugin"); //$NON-NLS-1$
	public final String JAVA_EXTENSION = Messages.getString("Extensions_java"); //$NON-NLS-1$
	public final String CLASS_EXTENSION = Messages.getString("Extensions_class"); //$NON-NLS-1$
	public final String PATTERN_DIR = Messages.getString("Patterns_non_jar_plugin");	//$NON-NLS-1$
	public final String META_INF_DIR_NAME = Messages.getString("Paths_meta_inf_directory"); //$NON-NLS-1$
	public final String JAR_RESOURCE_SUFFIX = Messages.getString("Suffixes_jar_resource_folder");	//$NON-NLS-1$
	public final String SLASH = Messages.getString("Characters_entry_separator");	//$NON-NLS-1$
	
	public final String[] DEFAULT_EXCLUDE_LIST = {PATTERN_DIR, SOURCE_SUFFIX, JAVA_EXTENSION, CLASS_EXTENSION};
	
	/** Parses an eclipse install and populates the plug-ins/features and their resources
	in maps to be accessed at runtime. */
	public void parse() throws Exception;
	
	/** Returns the map containing the eclipse install plug-ins and their translatable
	 * resources. */
	public Map<String, PluginProxy> getPlugins();
	
	/** Returns the map containing the eclipse install features and their translatable
	 * resources. */
	public Map<String, PluginProxy> getFeatures();
}
