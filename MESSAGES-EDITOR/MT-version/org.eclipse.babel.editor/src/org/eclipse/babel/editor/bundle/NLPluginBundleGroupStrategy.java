/*******************************************************************************
 * Copyright (c) 2007 Pascal Essiembre.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Pascal Essiembre - initial API and implementation
 ******************************************************************************/
package org.eclipse.babel.editor.bundle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.babel.core.message.MessagesBundle;
import org.eclipse.babel.core.util.BabelUtils;
import org.eclipse.babel.editor.util.UIUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IEditorSite;


/**
 * MessagesBundle group strategies for dealing with Eclipse "NL"
 * directory structure within a plugin.
 * <p>
 * This class also falls back to the usual locales as suffixes
 * by calling the methods defined in DefaultBundleGroupStrategy.
 * It enables us to re-use directly this class to support loading resources located
 * inside a fragment. In other words:
 * this class is extended by {@link NLFragmentBundleGroupStrategy}.
 * </p>
 * <p>
 * Note it is unclear how 
 * <p>
 * 
 * 
 * @author Pascal Essiembre
 * @author Hugues Malphettes
 */
public class NLPluginBundleGroupStrategy extends DefaultBundleGroupStrategy {
	
	private static Set<String> ISO_LANG_CODES = new HashSet<String>();
	private static Set<String> ISO_COUNTRY_CODES = new HashSet<String>();
	static {
		String[] isos = Locale.getISOLanguages();
		for (int i = 0; i < isos.length; i++) {
			ISO_LANG_CODES.add(isos[i]);
		}
		String[] isoc = Locale.getISOCountries();
		for (int i = 0; i < isoc.length; i++) {
			ISO_COUNTRY_CODES.add(isoc[i]);
		}
	}

	private IProject[] associatedFragmentProjects;
	protected IFolder nlFolder;
	protected String basePathInsideNL;
	
    /**
     * @param nlFolder when null, this strategy behaves just like
     * DefaultBundleGroupStrategy. Otherwise it is a localized file
     * using the "nl" folder. Most complete example found so far:
     * http://dev.eclipse.org/mhonarc/lists/babel-dev/msg00111.html
     * Although it applies to properties files too:
     * See figure 1 of:
     * http://www.eclipse.org/articles/Article-Speak-The-Local-Language/article.html
     */
    public NLPluginBundleGroupStrategy(IEditorSite site, IFile file,
    		IFolder nlFolder, IProject[] associatedFragmentProjects) {
        super(site, file);
        this.nlFolder = nlFolder;
        this.associatedFragmentProjects = associatedFragmentProjects;
    }

	
    /**
     * @param nlFolder when null, this strategy behaves just like
     * DefaultBundleGroupStrategy. Otherwise it is a localized file
     * using the "nl" folder. Most complete example found so far:
     * http://dev.eclipse.org/mhonarc/lists/babel-dev/msg00111.html
     * Although it applies to properties files too:
     * See figure 1 of:
     * http://www.eclipse.org/articles/Article-Speak-The-Local-Language/article.html
     */
    public NLPluginBundleGroupStrategy(IEditorSite site, IFile file,
    		IFolder nlFolder) {
        super(site, file);
        this.nlFolder = nlFolder;
    }

    /**
     * @see org.eclipse.babel.core.bundle.IBundleGroupStrategy#loadBundles()
     */
    public MessagesBundle[] loadMessagesBundles() {
        final Collection<MessagesBundle> bundles = new ArrayList<MessagesBundle>();
    	Collection<IFolder> nlFolders = nlFolder != null ? new ArrayList<IFolder>() : null;
    	if (associatedFragmentProjects != null) {
    		IPath basePath = null;
    		//true when the file opened is located in a source folder.
    		//in that case we don't support the nl structure
    		//as at runtime it only applies to resources that ae no inside the classes.
    		boolean fileIsInsideClasses = false;
    		boolean fileHasLocaleSuffix = false;
    		if (nlFolder == null) {
    			//in that case the project relative path to the container
    			//of the properties file is always the one here.
    			basePath = removePathToSourceFolder(
    					getOpenedFile().getParent().getProjectRelativePath());
    			fileIsInsideClasses = !basePath.equals(
    					getOpenedFile().getParent().getProjectRelativePath());
    			fileHasLocaleSuffix = !getOpenedFile().getName().equals(
    					super.getBaseName() + ".properties");
    			if (!fileHasLocaleSuffix && !fileIsInsideClasses) {
    				basePathInsideNL = basePath.toString();
    			}
    		} else {
    			//the file opened is inside an nl folder.
    			//this will compute the basePathInsideNL:
    			extractLocale(getOpenedFile(), true);
    			basePath = new Path(basePathInsideNL);
    		}
    		
    		for (int i = 0; i < associatedFragmentProjects.length; i++) {
    			IProject frag = associatedFragmentProjects[i];
    			if (fileIsInsideClasses) {
	    			Collection<String> srcPathes = NLFragmentBundleGroupStrategy.getSourceFolderPathes(frag);
	    			if (srcPathes != null) {
	    				//for each source folder, collect the resources we can find
	    				//with the suffix scheme:
	    				for (String srcPath : srcPathes) {
		    				IPath container = new Path(srcPath).append(basePath);
		    				super.collectBundlesInContainer(getContainer(frag, basePath), bundles);
	    				}
	    			}
	    			//also search directly in the bundle:
	    			super.collectBundlesInContainer(getContainer(frag, basePath), bundles);
    			} else {
	    			IFolder nl = frag.getFolder("nl");
	    			if (nl != null && nl.exists()) {
	    				if (nlFolders == null) {
	    					nlFolders = new ArrayList<IFolder>();
	    				}
	    				nlFolders.add(nl);
	    			}
	    			if (!fileHasLocaleSuffix) {
	    				//when the file is not inside nl and has no locale suffix
	    				//and is not inside the classes
	    				//it means we look both inside nl and with the suffix
	    				//based scheme:
	    				super.collectBundlesInContainer(getContainer(frag, basePath), bundles);
	    			}
    			}
    		}
    		
    	}
    	
        if (nlFolders == null) {
        	collectBundlesInContainer(getOpenedFile().getParent(), bundles);
            return bundles.toArray(EMPTY_BUNDLES);
        }
        if (nlFolder != null) {
        	nlFolders.add(nlFolder);
        }
        //get the nl directory.
        //navigate the entire directory from there
        //and look for the file with the same file names.
        final String name = getOpenedFile().getName();
        IResourceVisitor visitor = new IResourceVisitor() {
			public boolean visit(IResource resource) throws CoreException {
				if (resource.getType() == IResource.FILE
						&& resource.getName().equals(name)
						&& !getOpenedFile().equals(resource)) {
	                Locale locale = extractLocale((IFile)resource, false);
	                if (locale != null && UIUtils.isDisplayed(locale)) {
	                	bundles.add(createBundle(locale, resource));
	                }
				}
				return true;
			}
        };
        try {
        	Locale locale = extractLocale(getOpenedFile(), true);
            if (UIUtils.isDisplayed(locale)) {
            	bundles.add(createBundle(locale, getOpenedFile()));
            }
            for (IFolder nlFolder : nlFolders) {
            	nlFolder.accept(visitor);
            }
		} catch (CoreException e) {
			e.printStackTrace();
		}
		//also look for files based on the suffix mechanism
		//if we have located the root locale file:
		IContainer container = null;
		for (MessagesBundle mb : bundles) {
			if (mb.getLocale() == null || mb.getLocale().equals(UIUtils.ROOT_LOCALE)) {
				Object src  = mb.getResource().getSource();
				if (src instanceof IFile) {
					container = ((IFile)src).getParent();
				}
				break;
			}
		}
		if (container != null) {
			super.collectBundlesInContainer(container, bundles);
		}
        return bundles.toArray(EMPTY_BUNDLES);
    }
    
    private static final IContainer getContainer(IProject proj, IPath containerPath) {
    	if (containerPath.segmentCount() == 0) {
    		return proj;
    	}
    	return proj.getFolder(containerPath);
    }
    
    /**
     * @param baseContainerPath
     * @return if the path starts with a path to a source folder this method
     * returns the same path minus the source folder.
     */
    protected IPath removePathToSourceFolder(IPath baseContainerPath) {
    	Collection<String> srcPathes = NLFragmentBundleGroupStrategy.getSourceFolderPathes(
    	        				getOpenedFile().getProject());
    	if (srcPathes == null) {
    		return baseContainerPath;
    	}
    	String projRelativePathStr = baseContainerPath.toString();
	    for (String srcPath : srcPathes) {
    		if (projRelativePathStr.startsWith(srcPath)) {
    			return new Path(projRelativePathStr.substring(srcPath.length()));
    		}
    	}
    	return baseContainerPath;
    }


    /**
     * Tries to parse a locale directly from the file.
     * Support the locale as a string suffix and the locale as part of a path
     * inside an nl folder.
     * @param file
     * @return The parsed locale or null if no locale could be parsed.
     * If the locale is the root locale UIBableUtils.ROOT_LOCALE is returned.
     */
    private Locale extractLocale(IFile file, boolean docomputeBasePath) {
    	IFolder nl = MessagesBundleGroupFactory.getNLFolder(file);
    	String path = file.getFullPath().removeFileExtension().toString();
    	if (nl == null) {
	    	int ind = path.indexOf('_');
	    	int maxInd = path.length()-1;
	    	while (ind != -1 && ind < maxInd) {
	    		String possibleLocale = path.substring(ind+1);
	    		Locale res = BabelUtils.parseLocale(possibleLocale);
	    		if (res != null) {
	    			return res;
	    		}
	    		ind = path.indexOf('_', ind+1);
	    	}
	    	
	    	return null;
    	}
    	//the locale is not in the suffix.
    	//let's look into the nl folder:
    	int ind = path.lastIndexOf("/nl/");
    	//so the remaining String is a composition of the base path of
    	//the default properties and the path that reflects the locale.
    	//for example:
    	//if the default file is /theproject/icons/img.gif
    	//then the french localized file is /theproject/nl/FR/icons/img.gif
    	//so we need to separate fr from icons/img.gif to locate the base file.
    	//unfortunately we need to look into the values of the tokens
    	//to guess whether they are part of the path leading to the default file
    	//or part of the path that reflects the locale.
    	//we simply look whether 'icons' exist.
    	
    	// in other words: using folders is risky and users could
    	//crash eclipse using locales that conflict with pathes to resources.
    	
    	//so we must verify that the first 2 or 3 tokens after nl are valid ISO codes.
    	//the variant is the most problematic issue
    	//as it is not standardized.
    	
    	//we rely on finding the base properties
    	//to decide whether 'icons' is a variant or a folder.
    	
    	
    	
    	if (ind != -1) {
    		ind = ind + "/nl/".length();
    		int lastFolder = path.lastIndexOf('/');
    		if (lastFolder == ind) {
    			return UIUtils.ROOT_LOCALE;
    		}
    		path = path.substring(ind, lastFolder);
    		StringTokenizer tokens = new StringTokenizer(path, "/", false);
    		switch (tokens.countTokens()) {
    		case 0:
    			return null;
    		case 1:
            	String lang = tokens.nextToken();
                if (!ISO_LANG_CODES.contains(lang)) {
            		return null;
            	}
                if (docomputeBasePath) {
                	basePathInsideNL = "";
                	return new Locale(lang);
                } else if ("".equals(basePathInsideNL)) {
                	return new Locale(lang);
                } else {
                	return null;
                }
            case 2:
            	lang = tokens.nextToken();
            	if (!ISO_LANG_CODES.contains(lang)) {
            		return null;
            	}
            	String country = tokens.nextToken();
            	if (!ISO_COUNTRY_CODES.contains(country)) {
            		//in this case, this might be the beginning
            		//of the base path.
	        		if (isExistingFirstFolderForDefaultLocale(country)) {
	                    if (docomputeBasePath) {
	                    	basePathInsideNL = country;
	                    	return new Locale(lang);
	                    } else if (basePathInsideNL.equals(country)) {
	                    	return new Locale(lang);
	                    } else {
	                    	return null;
	                    }
	        		}
            	}
                if (docomputeBasePath) {
                	basePathInsideNL = "";
                	return new Locale(lang, country);
                } else if (basePathInsideNL.equals(country)) {
                	return new Locale(lang, country);
                } else {
                	return null;
                }
            default:
            	lang = tokens.nextToken();
	        	if (!ISO_LANG_CODES.contains(lang)) {
	        		return null;
	        	}
	        	country = tokens.nextToken();
	        	if (!ISO_COUNTRY_CODES.contains(country)) {
	        		if (isExistingFirstFolderForDefaultLocale(country)) {
	        			StringBuffer b = new StringBuffer(country);
                    	while (tokens.hasMoreTokens()) {
                    		b.append("/" + tokens.nextToken());
                    	}
	                    if (docomputeBasePath) {
	                    	basePathInsideNL = b.toString();
	                    	return new Locale(lang);
	                    } else if (basePathInsideNL.equals(b.toString())) {
	                    	return new Locale(lang);
	                    } else {
	                    	return null;
	                    }
	        		}
            	}
	        	String variant = tokens.nextToken();
        		if (isExistingFirstFolderForDefaultLocale(variant)) {
        		    StringBuffer b = new StringBuffer(variant);
                	while (tokens.hasMoreTokens()) {
                		b.append("/" + tokens.nextToken());
                	}
                    if (docomputeBasePath) {
                    	basePathInsideNL = b.toString();
                    	return new Locale(lang, country);
                    } else if (basePathInsideNL.equals(b.toString())) {
                    	return new Locale(lang);
                    } else {
                    	return null;
                    }
        		}
        		StringBuffer b = new StringBuffer();
            	while (tokens.hasMoreTokens()) {
            		b.append("/" + tokens.nextToken());
            	}
                if (docomputeBasePath) {
                	basePathInsideNL = b.toString();
                	return new Locale(lang, country, variant);
                } else if (basePathInsideNL.equals(b.toString())) {
                	return new Locale(lang);
                } else {
                	return null;
                }
    		}
    	}
    	return UIUtils.ROOT_LOCALE;
    }
    
    /**
     * Called when using an nl structure.<br/>
     * We need to find out whether the variant is in fact a folder.
     * If we locate a folder inside the project with this name we assume it is not a variant.
     * <p>
     * This method is overridden inside the NLFragment thing as we need to check
     * 2 projects over there: the host-plugin project and the current project.
     * </p>
     * @param possibleVariant
     * @return
     */
    protected boolean isExistingFirstFolderForDefaultLocale(String folderName) {
		return getOpenedFile().getProject().getFolder(folderName).exists();
    }
    
}
