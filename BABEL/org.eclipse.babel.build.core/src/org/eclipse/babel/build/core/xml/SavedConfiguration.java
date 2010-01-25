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
package org.eclipse.babel.build.core.xml;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.eclipse.babel.build.core.Configuration;
import org.eclipse.babel.build.core.Filter;
import org.eclipse.babel.build.core.LocaleGroup;
import org.eclipse.babel.build.core.LocaleProxy;
import org.eclipse.babel.build.core.PluginProxy;
import org.eclipse.babel.build.core.Range;
import org.eclipse.babel.build.core.ResourceProxy;
import org.eclipse.babel.build.core.eclipsetarget.EclipseTarget;
import org.eclipse.babel.build.core.translationcatalogue.TranslationCatalogue;


public class SavedConfiguration implements Configuration {

	public EclipseTarget eclipseInstall() {
		// TODO Auto-generated method stub
		return null;
	}

	public Range compatibilityRange() {
		// TODO Auto-generated method stub
		return null;
	}

	public List<Filter> filters() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean includePseudoTranslations() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean includeResource(PluginProxy plugin, ResourceProxy resouce) {
		// TODO Auto-generated method stub
		return false;
	}

	public File reportLocation() {
		// TODO Auto-generated method stub
		return null;
	}

	public Date timestamp() {
		// TODO Auto-generated method stub
		return null;
	}

	public TranslationCatalogue translations() {
		// TODO Auto-generated method stub
		return null;
	}

	public File workingDirectory() {
		// TODO Auto-generated method stub
		return null;
	}

	public Set<LocaleProxy> locales() {
		// TODO Auto-generated method stub
		return null;
	}

	public Set<LocaleGroup> localeGroups() {
		// TODO Auto-generated method stub
		return null;
	}

	public Set<String> excludeList() {
		// TODO Auto-generated method stub
		return null;
	}

	public void notifyProgress(String fragmentName) {
		// TODO Auto-generated method stub
	}

	public boolean includeXmlReport() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean longReport() {
		// TODO Auto-generated method stub
		return false;
	}

	public String localeExtension() {
		// TODO Auto-generated method stub
		return null;
	}
}
