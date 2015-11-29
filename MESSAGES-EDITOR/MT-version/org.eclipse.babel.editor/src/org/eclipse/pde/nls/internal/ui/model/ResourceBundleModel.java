/*******************************************************************************
 * Copyright (c) 2008 Stefan Mücke and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Stefan Mücke - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.nls.internal.ui.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;

import org.eclipse.babel.editor.plugin.MessagesEditorPlugin;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJarEntryResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;

/**
 * A <code>ResourceBundleModel</code> is the host for all {@link ResourceBundleFamily} elements. 
 */
public class ResourceBundleModel extends ResourceBundleElement {

	private static final String PROPERTIES_SUFFIX = ".properties"; //$NON-NLS-1$

	private static final String JAVA_NATURE = "org.eclipse.jdt.core.javanature"; //$NON-NLS-1$

	private ArrayList<ResourceBundleFamily> bundleFamilies = new ArrayList<ResourceBundleFamily>();

	/**
	 * The locales for which all bundles have been loaded.
	 */
	// TODO Perhaps we should add reference counting to prevent unexpected unloading of bundles 
	private HashSet<Locale> loadedLocales = new HashSet<Locale>();

	public ResourceBundleModel(IProgressMonitor monitor) {
		super(null);
		try {
			populateFromWorkspace(monitor);
		} catch (CoreException e) {
			MessagesEditorPlugin.log(e);
		}
	}

	/**
	 * Returns all resource bundle families contained in this model.
	 * 
	 * @return all resource bundle families contained in this model
	 */
	public ResourceBundleFamily[] getFamilies() {
		return bundleFamilies.toArray(new ResourceBundleFamily[bundleFamilies.size()]);
	}

	public ResourceBundleFamily[] getFamiliesForPluginId(String pluginId) {
		ArrayList<ResourceBundleFamily> found = new ArrayList<ResourceBundleFamily>();
		for (ResourceBundleFamily family : bundleFamilies) {
			if (family.getPluginId().equals(pluginId)) {
				found.add(family);
			}
		}
		return found.toArray(new ResourceBundleFamily[found.size()]);
	}

	public ResourceBundleFamily[] getFamiliesForProjectName(String projectName) {
		ArrayList<ResourceBundleFamily> found = new ArrayList<ResourceBundleFamily>();
		for (ResourceBundleFamily family : bundleFamilies) {
			if (family.getProjectName().equals(projectName)) {
				found.add(family);
			}
		}
		return found.toArray(new ResourceBundleFamily[found.size()]);
	}

	public ResourceBundleFamily[] getFamiliesForProject(IProject project) {
		return getFamiliesForProjectName(project.getName());
	}

	/**
	 * Returns an array of all currently known bundle keys. This always includes
	 * the keys from the default bundles and may include some additional keys
	 * from bundles that have been loaded sometime and that contain keys not found in
	 * a bundle's default bundle. When a bundle is unloaded, these additional keys
	 * will not be removed from the model.
	 * 
	 * @return the array of bundles keys
	 * @throws CoreException 
	 */
	public ResourceBundleKey[] getAllKeys() throws CoreException {
		Locale root = new Locale("", "", "");
		
		// Ensure default bundle is loaded and count keys  
		int size = 0;
		for (ResourceBundleFamily family : bundleFamilies) {
			ResourceBundle bundle = family.getBundle(root);
			if (bundle != null)
				bundle.load();
			size += family.getKeyCount();
		}

		ArrayList<ResourceBundleKey> allKeys = new ArrayList<ResourceBundleKey>(size);
		for (ResourceBundleFamily family : bundleFamilies) {
			ResourceBundleKey[] keys = family.getKeys();
			for (ResourceBundleKey key : keys) {
				allKeys.add(key);
			}
		}

		return allKeys.toArray(new ResourceBundleKey[allKeys.size()]);
	}

	/**
	 * Loads all the bundles for the given locale into memory.
	 * 
	 * @param locale the locale of the bundles to load
	 * @throws CoreException 
	 */
	public void loadBundles(Locale locale) throws CoreException {
		ResourceBundleFamily[] families = getFamilies();
		for (ResourceBundleFamily family : families) {
			ResourceBundle bundle = family.getBundle(locale);
			if (bundle != null)
				bundle.load();
		}
		loadedLocales.add(locale);
	}

	/**
	 * Unloads all the bundles for the given locale from this model. The default
	 * bundle cannot be unloaded. Such a request will be ignored.
	 * 
	 * @param locale the locale of the bundles to unload
	 */
	public void unloadBundles(Locale locale) {
		if ("".equals(locale.getLanguage()))
			return; // never unload the default bundles

		ResourceBundleFamily[] families = getFamilies();
		for (ResourceBundleFamily family : families) {
			ResourceBundle bundle = family.getBundle(locale);
			if (bundle != null)
				bundle.unload();
		}
		loadedLocales.remove(locale);
	}

	private void populateFromWorkspace(IProgressMonitor monitor) throws CoreException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		IProject[] projects = root.getProjects();
		for (IProject project : projects) {
			try {
				if (!project.isOpen())
					continue;

				IJavaProject javaProject = (IJavaProject) project.getNature(JAVA_NATURE);

				// Plugin and fragment projects
				IPluginModelBase pluginModel = PluginRegistry.findModel(project);
				String pluginId = null;
				if (pluginModel != null) {
					// Get plugin id
					pluginId = pluginModel.getBundleDescription().getName(); // OSGi bundle name
					if (pluginId == null) {
						pluginId = pluginModel.getPluginBase().getId(); // non-OSGi plug-in id
					}
					boolean isFragment = pluginModel instanceof IFragmentModel;
					if (isFragment) {
						IFragmentModel fragmentModel = (IFragmentModel) pluginModel;
						pluginId = fragmentModel.getFragment().getPluginId();
					}

					// Look for additional 'nl' resources
					IFolder nl = project.getFolder("nl"); //$NON-NLS-1$
					if (isFragment && nl.exists()) {
						IResource[] members = nl.members();
						for (IResource member : members) {
							if (member instanceof IFolder) {
								IFolder langFolder = (IFolder) member;
								String language = langFolder.getName();

								// Collect property files
								IFile[] propertyFiles = collectPropertyFiles(langFolder);
								for (IFile file : propertyFiles) {
									// Compute path name
									IPath path = file.getProjectRelativePath();
									String country = ""; //$NON-NLS-1$
									String packageName = null;
									int segmentCount = path.segmentCount();
									if (segmentCount > 1) {
										StringBuilder builder = new StringBuilder();

										// Segment 0: 'nl'
										// Segment 1: language code
										// Segment 2: (country code)
										int begin = 2;
										if (segmentCount > 2 && isCountry(path.segment(2))) {
											begin = 3;
											country = path.segment(2);
										}

										for (int i = begin; i < segmentCount - 1; i++) {
											if (i > begin)
												builder.append('.');
											builder.append(path.segment(i));
										}
										packageName = builder.toString();
									}

									String baseName = getBaseName(file.getName());

									ResourceBundleFamily family = getOrCreateFamily(
											project.getName(),
											pluginId,
											packageName,
											baseName);
									addBundle(family, getLocale(language, country), file);
								}
							}
						}
					}

					// Collect property files
					if (isFragment || javaProject == null) {
						IFile[] propertyFiles = collectPropertyFiles(project);
						for (IFile file : propertyFiles) {
							IPath path = file.getProjectRelativePath();
							int segmentCount = path.segmentCount();

							if (segmentCount > 0 && path.segment(0).equals("nl")) //$NON-NLS-1$
								continue; // 'nl' resource have been processed above

							// Guess package name
							String packageName = null;
							if (segmentCount > 1) {
								StringBuilder builder = new StringBuilder();
								for (int i = 0; i < segmentCount - 1; i++) {
									if (i > 0)
										builder.append('.');
									builder.append(path.segment(i));
								}
								packageName = builder.toString();
							}

							String baseName = getBaseName(file.getName());
							String language = getLanguage(file.getName());
							String country = getCountry(file.getName());

							ResourceBundleFamily family = getOrCreateFamily(
									project.getName(),
									pluginId,
									packageName,
									baseName);
							addBundle(family, getLocale(language, country), file);
						}
					}

				}

				// Look for resource bundles in Java packages (output folders, e.g. 'bin', will be ignored)
				if (javaProject != null) {
					IClasspathEntry[] classpathEntries = javaProject.getResolvedClasspath(true);
					for (IClasspathEntry entry : classpathEntries) {
						if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
							IPath path = entry.getPath();
							IFolder folder = workspace.getRoot().getFolder(path);
							IFile[] propertyFiles = collectPropertyFiles(folder);

							for (IFile file : propertyFiles) {
								String name = file.getName();
								String baseName = getBaseName(name);
								String language = getLanguage(name);
								String country = getCountry(name);
								IPackageFragment pf = javaProject.findPackageFragment(file.getParent()
										.getFullPath());
								String packageName = pf.getElementName();

								ResourceBundleFamily family = getOrCreateFamily(
										project.getName(),
										pluginId,
										packageName,
										baseName);

								addBundle(family, getLocale(language, country), file);
							}
						} else if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
							IPackageFragmentRoot[] findPackageFragmentRoots = javaProject.findPackageFragmentRoots(entry);
							for (IPackageFragmentRoot packageFragmentRoot : findPackageFragmentRoots) {
								IJavaElement[] children = packageFragmentRoot.getChildren();
								for (IJavaElement child : children) {
									IPackageFragment pf = (IPackageFragment) child;
									Object[] nonJavaResources = pf.getNonJavaResources();

									for (Object resource : nonJavaResources) {
										if (resource instanceof IJarEntryResource) {
											IJarEntryResource jarEntryResource = (IJarEntryResource) resource;
											String name = jarEntryResource.getName();
											if (name.endsWith(PROPERTIES_SUFFIX)) {
												String baseName = getBaseName(name);
												String language = getLanguage(name);
												String country = getCountry(name);
												String packageName = pf.getElementName();

												ResourceBundleFamily family = getOrCreateFamily(
														project.getName(),
														pluginId,
														packageName,
														baseName);

												addBundle(
														family,
														getLocale(language, country),
														jarEntryResource);
											}
										}
									}
								}
							}
						}
					}

					// Collect non-Java resources 
					Object[] nonJavaResources = javaProject.getNonJavaResources();
					ArrayList<IFile> files = new ArrayList<IFile>();
					for (Object resource : nonJavaResources) {
						if (resource instanceof IContainer) {
							IContainer container = (IContainer) resource;
							collectPropertyFiles(container, files);
						} else if (resource instanceof IFile) {
							IFile file = (IFile) resource;
							String name = file.getName();
							if (isIgnoredFilename(name))
								continue;
							if (name.endsWith(PROPERTIES_SUFFIX)) {
								files.add(file);
							}
						}
					}
					for (IFile file : files) {

						// Convert path to package name format
						IPath path = file.getProjectRelativePath();
						String packageName = null;
						int segmentCount = path.segmentCount();
						if (segmentCount > 1) {
							StringBuilder builder = new StringBuilder();
							for (int i = 0; i < segmentCount - 1; i++) {
								if (i > 0)
									builder.append('.');
								builder.append(path.segment(i));
							}
							packageName = builder.toString();
						}

						String baseName = getBaseName(file.getName());
						String language = getLanguage(file.getName());
						String country = getCountry(file.getName());

						ResourceBundleFamily family = getOrCreateFamily(
								project.getName(),
								pluginId,
								packageName,
								baseName);
						addBundle(family, getLocale(language, country), file);
					}

				}
			} catch (Exception e) {
				MessagesEditorPlugin.log(e);
			}
		}
	}

	private IFile[] collectPropertyFiles(IContainer container) throws CoreException {
		ArrayList<IFile> files = new ArrayList<IFile>();
		collectPropertyFiles(container, files);
		return files.toArray(new IFile[files.size()]);
	}

	private void collectPropertyFiles(IContainer container, ArrayList<IFile> files) throws CoreException {
		IResource[] members = container.members();
		for (IResource resource : members) {
			if (!resource.exists())
				continue;
			if (resource instanceof IContainer) {
				IContainer childContainer = (IContainer) resource;
				collectPropertyFiles(childContainer, files);
			} else if (resource instanceof IFile) {
				IFile file = (IFile) resource;
				String name = file.getName();
				if (file.getProjectRelativePath().segmentCount() == 0 && isIgnoredFilename(name))
					continue;
				if (name.endsWith(PROPERTIES_SUFFIX)) {
					files.add(file);
				}
			}
		}
	}

	private boolean isCountry(String name) {
		if (name == null || name.length() != 2)
			return false;
		char c1 = name.charAt(0);
		char c2 = name.charAt(1);
		return 'A' <= c1 && c1 <= 'Z' && 'A' <= c2 && c2 <= 'Z';
	}

	private Locale getLocale(String language, String country) {
		if (language == null)
			language = ""; //$NON-NLS-1$
		if (country == null)
			country = ""; //$NON-NLS-1$
		return new Locale(language, country);
	}

	private void addBundle(ResourceBundleFamily family, Locale locale, Object resource) throws CoreException {
		ResourceBundle bundle = new ResourceBundle(family, resource, locale);
		if ("".equals(locale.getLanguage()))
			bundle.load();
		family.addBundle(bundle);
	}

	private String getBaseName(String filename) {
		if (!filename.endsWith(PROPERTIES_SUFFIX))
			throw new IllegalArgumentException();
		String name = filename.substring(0, filename.length() - 11);
		int len = name.length();
		if (len > 3 && name.charAt(len - 3) == '_') {
			if (len > 6 && name.charAt(len - 6) == '_') {
				return name.substring(0, len - 6);
			} else {
				return name.substring(0, len - 3);
			}
		}
		return name;
	}

	private String getLanguage(String filename) {
		if (!filename.endsWith(PROPERTIES_SUFFIX))
			throw new IllegalArgumentException();
		String name = filename.substring(0, filename.length() - 11);
		int len = name.length();
		if (len > 3 && name.charAt(len - 3) == '_') {
			if (len > 6 && name.charAt(len - 6) == '_') {
				return name.substring(len - 5, len - 3);
			} else {
				return name.substring(len - 2);
			}
		}
		return ""; //$NON-NLS-1$
	}

	private String getCountry(String filename) {
		if (!filename.endsWith(PROPERTIES_SUFFIX))
			throw new IllegalArgumentException();
		String name = filename.substring(0, filename.length() - 11);
		int len = name.length();
		if (len > 3 && name.charAt(len - 3) == '_') {
			if (len > 6 && name.charAt(len - 6) == '_') {
				return name.substring(len - 2);
			} else {
				return ""; //$NON-NLS-1$
			}
		}
		return ""; //$NON-NLS-1$
	}

	private ResourceBundleFamily getOrCreateFamily(String projectName, String pluginId, String packageName,
			String baseName) {

		// Ignore project name
		if (pluginId != null)
			projectName = null;

		for (ResourceBundleFamily family : bundleFamilies) {
			if (areEqual(family.getProjectName(), projectName)
					&& areEqual(family.getPluginId(), pluginId)
					&& areEqual(family.getPackageName(), packageName)
					&& areEqual(family.getBaseName(), baseName)) {
				return family;
			}
		}
		ResourceBundleFamily family = new ResourceBundleFamily(
			this,
			projectName,
			pluginId,
			packageName,
			baseName);
		bundleFamilies.add(family);
		return family;
	}

	private boolean isIgnoredFilename(String filename) {
		return filename.equals("build.properties") || filename.equals("logging.properties"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private boolean areEqual(String str1, String str2) {
		return str1 == null && str2 == null || str1 != null && str1.equals(str2);
	}

}
