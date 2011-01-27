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
package org.eclipse.babel.build.ui.wizard;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;

public class BuildInitializationOperation implements IRunnableWithProgress{
	private BuildToolModelTable fModelPluginsTable;
	private BuildToolModelTable fModelLocalesTable;
	private BuildToolModelTable fModelIgnoreTable;
	private BuildToolModelTable fModelResourceExclusionTable;
	
	private ISelection fPluginSelection;
	private List<String> fSelectedLocales = null;
	private ArrayList<IProject> fSelectedPlugins;
	private boolean fCanceled;
	
	public BuildInitializationOperation(ISelection pluginSelection, String[] localeSelection) {
		fPluginSelection = pluginSelection;
		if (localeSelection != null) {
			fSelectedLocales = Arrays.asList(localeSelection);
		}
	}
	
	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		this.fillLocalesTable();
		this.fillPluginsTable();
		this.fillIgnoreTable();
		this.fillResourceExclusionTable();
	}
	
	private void fillPluginsTable() {
		if (fPluginSelection instanceof IStructuredSelection) {
			IPath MANIFEST_PATH = new Path("META-INF/MANIFEST.MF"); //$NON-NLS-1$
			IPath PLUGIN_PATH = new Path("plugin.xml"); //$NON-NLS-1$
			IPath FRAGMENT_PATH = new Path("fragment.xml"); //$NON-NLS-1$

			Object[] plugins = ((IStructuredSelection) fPluginSelection).toArray();
			
			fSelectedPlugins = new ArrayList<IProject>(plugins.length);
			for (int i = 0; i < plugins.length; i++) {
				//Handle plug-ins java projects (These are also plug-ins
				if(plugins[i] instanceof IJavaProject) {
					plugins[i] = ((IJavaProject)(((IStructuredSelection) fPluginSelection).toArray()[i])).getProject();
				}
				//If a file was selected, get its parent project
				if (plugins[i] instanceof IFile)
					plugins[i] = ((IFile) plugins[i]).getProject();

				//Add the project to the preselected model list
				if (plugins[i] instanceof IProject 
						&& (((IProject)plugins[i]).exists(MANIFEST_PATH)
							|| ((IProject)plugins[i]).exists(PLUGIN_PATH)
							|| ((IProject)plugins[i]).exists(FRAGMENT_PATH))) {
					fSelectedPlugins.add((IProject)plugins[i]);
				}
			}
		}
		//Get all models (workspace and external) excluding fragment models
		IPluginModelBase[] pluginModels = PluginRegistry.getAllModels(false);

		//Populate list to an InternationalizeModelTable
		fModelPluginsTable = new BuildToolModelTable();
		for (int i = 0; i < pluginModels.length; i++) {
			fModelPluginsTable.addToModelTable(pluginModels[i], pluginModels[i].getUnderlyingResource() != null ? isPluginSelected(pluginModels[i].getUnderlyingResource().getProject()) : false);
		}
	}
	
	private void fillLocalesTable() {
		fModelLocalesTable = new BuildToolModelTable();
		Locale[] availableLocales = Locale.getAvailableLocales();
		for (int i = 0; i < availableLocales.length; i++) {
			fModelLocalesTable.addToModelTable(availableLocales[i], availableLocales != null ? isLocaleSelected(availableLocales[i].toString()) : false);
		}
	}
	
	private void fillIgnoreTable() {
		fModelIgnoreTable = new BuildToolModelTable();
	}
	
	private void fillResourceExclusionTable() {
		fModelResourceExclusionTable = new BuildToolModelTable();
	}
	
	/**
	 * 
	 * @return whether or not the operation was canceled
	 */
	public boolean wasCanceled() {
		return fCanceled;
	}

	/**
	 * 
	 * @param project
	 * @return whether or not the project was preselected
	 */
	public boolean isPluginSelected(IProject project) {
		return fSelectedPlugins.contains(project);
	}

	public boolean isLocaleSelected(String locale) {
		if (fSelectedLocales == null) {
			return false;
		}
		
		return fSelectedLocales.contains(locale);
	}
	
	/**
	 * 
	 * @return the BuildToolModelTable containing the plug-ins
	 */
	public BuildToolModelTable getPluginsTable() {
		return fModelPluginsTable;
	}
	
	/**
	 * 
	 * @return the BuildToolModelTable containing the locales
	 */
	public BuildToolModelTable getLocalesTable() {
		return fModelLocalesTable;
	}
	
	/**
	 * 
	 * @return the BuildToolModelTable containing the ignore list
	 */
	public BuildToolModelTable getIgnoreTable() {
		return fModelIgnoreTable;
	}

	/**
	 * 
	 * @return the BuildToolModelTable containing the resource exclusion list
	 */
	public BuildToolModelTable getResourceExclusionTable() {
		return fModelResourceExclusionTable;
	}
}
