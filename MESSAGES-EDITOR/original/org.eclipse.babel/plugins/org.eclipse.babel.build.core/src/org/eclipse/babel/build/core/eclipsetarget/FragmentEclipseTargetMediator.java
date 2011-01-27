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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipException;

import org.eclipse.babel.build.core.Messages;
import org.eclipse.babel.build.core.ResourceProxy;


public interface FragmentEclipseTargetMediator {
	
	public final String SLASH = Messages.getString("Characters_entry_separator");	//$NON-NLS-1$
	public final String JAR_RESOURCE_SUFFIX = Messages.getString("Suffixes_jar_resource_folder");	//$NON-NLS-1$
	public final String JAR_EXTENSION = Messages.getString("Extensions_jar"); //$NON-NLS-1$
	public final String ABOUT_FILE = Messages.getString("Paths_about_html");	//$NON-NLS-1$
	
	public InputStream getResourceAsStream(ResourceProxy resource) throws ZipException, 
			IOException, FileNotFoundException, Exception;
	
	public Map<String, Set<Object>> extractEclipseTargetProperties()
			throws ZipException, IOException, FileNotFoundException;

	public boolean writeAboutHtmlFile(File resource, JarOutputStream out) throws Exception;
}
