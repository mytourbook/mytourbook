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
package org.eclipse.babel.build.core.translationcatalogue;

import java.util.Map;
import java.util.Set;

import org.eclipse.babel.build.core.LocaleProxy;
import org.eclipse.babel.build.core.Messages;
import org.eclipse.babel.build.core.PluginProxy;
import org.eclipse.babel.build.core.exceptions.InvalidLocationException;


public interface TranslationCatalogueParser {
	public final static String PLUGINS_PATH = Messages.getString("Paths_plugins_directory"); //$NON-NLS-1$
	public final static String FEATURES_PATH = Messages.getString("Paths_features_directory"); //$NON-NLS-1$
	
	/**
	 * @param eclipseInstallPlugin
	 * @return The different versions (one for each locale) of the same plug-in within the catalogue.
	 */
	public Map<String, PluginProxy> getPluginForSpecifiedLocales (PluginProxy eclipseInstallPlugin);
	
	/**
	 * @param eclipseInstallFeature
	 * @return The different versions (one for each locale) of the same feature within the catalogue.
	 */
	public Map<String, PluginProxy> getFeatureForSpecifiedLocales (PluginProxy eclipseInstallFeature);
	
	/**
	 * Extract the list of all the relevant locales featured in the Translation Catalogue.
	 */
	public Set<LocaleProxy> findRelevantLocalesInCatalogue() throws InvalidLocationException;
}
