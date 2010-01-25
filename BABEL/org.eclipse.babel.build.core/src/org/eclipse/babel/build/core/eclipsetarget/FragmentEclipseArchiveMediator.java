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
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.eclipse.babel.build.core.Messages;
import org.eclipse.babel.build.core.PluginProxy;
import org.eclipse.babel.build.core.ResourceProxy;


public class FragmentEclipseArchiveMediator implements
		FragmentEclipseTargetMediator {

	private PluginProxy eclipseInstallPlugin;
	private ZipFile eclipseArchive;
	private File eclipseArchiveLocation;
	
	public FragmentEclipseArchiveMediator(PluginProxy eclipseInstallPlugin, File eclipseArchiveLocation) throws Exception {
		this.eclipseInstallPlugin = eclipseInstallPlugin;
		this.eclipseArchive = new ZipFile(eclipseArchiveLocation);
		this.eclipseArchiveLocation = eclipseArchiveLocation;
	}

	public Map<String, Set<Object>> extractEclipseTargetProperties()
			throws ZipException, IOException, FileNotFoundException {
		
		Map<String, Set<Object>> propertiesMap = new HashMap<String, Set<Object>>();
		
		// Determine the entry name of the JAR plug-in
		InputStream jarPluginInputStream;
		String jarPluginRelativePath = eclipseInstallPlugin.getPluginLocation().getAbsolutePath().replace(eclipseArchiveLocation.getAbsolutePath(), "");
		jarPluginRelativePath = jarPluginRelativePath.substring(1);
		jarPluginRelativePath = jarPluginRelativePath.replace(
				File.separator, Messages.getString("Characters_entry_separator"));	//$NON-NLS-1$
		
		// Extract the JAR plug-in entry form the eclipse archive
		ZipEntry jarPluginEntry = eclipseArchive.getEntry(jarPluginRelativePath);
		jarPluginInputStream = eclipseArchive.getInputStream(jarPluginEntry);
		
		JarInputStream inputStream = new JarInputStream(jarPluginInputStream);
		
		// Iterate through the plug-ins resources and store data on the properties files		
		JarEntry entry;
		while (( entry  = inputStream.getNextJarEntry() ) != null) {
			String entryName = entry.getName();
			if (entryName.endsWith(Messages.getString("Extensions_properties"))) {	//$NON-NLS-1$
				Properties properties = new Properties();
				properties.load(inputStream);
				propertiesMap.put(entry.getName(), new HashSet<Object>(properties.keySet()));
			}
		}
		inputStream.close();
		jarPluginInputStream.close();
		
		return propertiesMap;
	}

	public InputStream getResourceAsStream(ResourceProxy resource) 
			throws ZipException, IOException, FileNotFoundException, Exception {									

		String resourcePath = Messages.getString("Paths_eclipse_directory_name") + SLASH 
			+ (eclipseInstallPlugin.isFeature() ? "features" : "plugins") 
			+ SLASH + eclipseInstallPlugin.getName() + Messages.getString("Characters_underscore") 
			+ eclipseInstallPlugin.getVersion() + SLASH + resource.getCanonicalPath();	//$NON-NLS-1$ $NON-NLS-2$	$NON-NLS-3$	$NON-NLS-4$
		
		ZipEntry resourceFile = eclipseArchive.getEntry(resourcePath);
		if (resourceFile == null) {
			return null;
		}
		return eclipseArchive.getInputStream(resourceFile);
	}

	public boolean writeAboutHtmlFile(File resource, JarOutputStream out) throws Exception {
		String pluginRelativePath = eclipseInstallPlugin.getPluginLocation().getAbsolutePath().replace(eclipseArchiveLocation.getAbsolutePath(), "");
		pluginRelativePath = pluginRelativePath.substring(1);
		pluginRelativePath = pluginRelativePath.replace(
				File.separator, Messages.getString("Characters_entry_separator"));	//$NON-NLS-1$
		
		ZipEntry archiveEntry;
		if (eclipseInstallPlugin.isJar()) {
			// Obtain an input stream for the jar resource
			archiveEntry = eclipseArchive.getEntry(pluginRelativePath);
			InputStream archiveInputStream = eclipseArchive.getInputStream(archiveEntry); 
			JarInputStream entryInputStream = new JarInputStream(archiveInputStream);
			
			// Find the about.html entry within the jar resource
			JarEntry jarResourceEntry = entryInputStream.getNextJarEntry();
			boolean resourceFound = false;
			while(jarResourceEntry != null && !resourceFound) {
				String resourceEntryName = jarResourceEntry.getName();
				if (resourceEntryName.equalsIgnoreCase(resource.getName())) {
					resourceFound = true;
				} else {
					jarResourceEntry = entryInputStream.getNextJarEntry();
				}
			}
			
			// Add the file to the jar
			out.putNextEntry(new JarEntry(jarResourceEntry.getName()));
			writeToOutputStream(out, entryInputStream);
	        archiveInputStream.close();
	        entryInputStream.close();
		} else {
			// Obtain the about.html entry directly from the eclipse archive 
			archiveEntry = eclipseArchive.getEntry(pluginRelativePath + SLASH + resource.getName());
			out.putNextEntry(new ZipEntry(resource.getName()));
			InputStream archiveInputStream = eclipseArchive.getInputStream(archiveEntry);
			
			// Add the file to the jar
			writeToOutputStream(out, archiveInputStream);
			archiveInputStream.close();
		}
		
		out.closeEntry();
		
		return true;
	}
	
	private void writeToOutputStream(OutputStream out, InputStream in) throws Exception {
		byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
	}

}
