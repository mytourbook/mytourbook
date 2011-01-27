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
package org.eclipse.babel.editor.builder;

//import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.babel.core.message.MessagesBundle;
import org.eclipse.babel.core.message.MessagesBundleGroup;
import org.eclipse.babel.editor.bundle.MessagesBundleGroupFactory;
import org.eclipse.babel.editor.plugin.MessagesEditorPlugin;
import org.eclipse.babel.editor.resource.validator.FileMarkerStrategy;
import org.eclipse.babel.editor.resource.validator.IValidationMarkerStrategy;
import org.eclipse.babel.editor.resource.validator.MessagesBundleGroupValidator;
import org.eclipse.babel.editor.util.UIUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * @author Pascal Essiembre
 *
 */
public class Builder extends IncrementalProjectBuilder {

    public static final String BUILDER_ID =
            "org.eclipse.babel.editor.rbeBuilder"; //$NON-NLS-1$
        
    private IValidationMarkerStrategy markerStrategy = new FileMarkerStrategy();
    
	class SampleDeltaVisitor implements IResourceDeltaVisitor {
		/**
		 * @see org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse.core.resources.IResourceDelta)
		 */
		public boolean visit(IResourceDelta delta) throws CoreException {
			IResource resource = delta.getResource();
			switch (delta.getKind()) {
			case IResourceDelta.ADDED:
				// handle added resource
                System.out.println("RBE DELTA added");
				checkBundleResource(resource);
				break;
			case IResourceDelta.REMOVED:
                System.out.println("RBE DELTA Removed"); //$NON-NLS-1$
				// handle removed resource
				break;
			case IResourceDelta.CHANGED:
                System.out.println("RBE DELTA changed");
				// handle changed resource
				checkBundleResource(resource);
				break;
			}
			//return true to continue visiting children.
			return true;
		}
	}

	class SampleResourceVisitor implements IResourceVisitor {
		public boolean visit(IResource resource) {
			checkBundleResource(resource);
			//return true to continue visiting children.
			return true;
		}
	}

	/** list built during a single build of the properties files.
	 * Contains the list of files that must be validated.
	 * The validation is done only at the end of the visitor.
	 * This way the visitor can add extra files to be visited.
	 * For example: if the default properties file is
	 * changed, it is a good idea to rebuild all
	 * localized files in the same MessageBundleGroup even if themselves
	 * were not changed. */
    private Set _resourcesToValidate;

    /**
     * Index built during a single build.
     * <p>
     * The values of that map are message bundles.
     * The key is a resource that belongs to that message bundle.
     * </p>
     */
    private Map<IFile,MessagesBundleGroup> _alreadBuiltMessageBundle;
    
//    /** one indexer per project we open and close it at the beginning and the end of each build. */
//    private Indexer _indexer = new Indexer();
      
	/**
	 * @see org.eclipse.core.resources.IncrementalProjectBuilder#build(
     *          int, java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
			throws CoreException {
		try {
			_alreadBuiltMessageBundle = null;
			_resourcesToValidate = null;
			if (kind == FULL_BUILD) {
				fullBuild(monitor);
			} else {
				IResourceDelta delta = getDelta(getProject());
				if (delta == null) {
					fullBuild(monitor);
				} else {
					incrementalBuild(delta, monitor);
				}
			}
		} finally {
			try {
				finishBuild();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				//must dispose the message bundles:
				if (_alreadBuiltMessageBundle != null) {
					for (MessagesBundleGroup msgGrp : _alreadBuiltMessageBundle.values()) {
						try {
							msgGrp.dispose();
						} catch (Throwable t) {
							//FIXME: remove this debugging:
							System.err.println(
									"error disposing message-bundle-group "
									+ msgGrp.getName());
							//disregard crashes: we are doing our best effort to dispose things.
						}
					}
					_alreadBuiltMessageBundle = null;
					_resourcesToValidate = null;
				}
//				if (_indexer != null) {
//					try {
//						_indexer.close(true);
//						_indexer.clear();
//					} catch (CorruptIndexException e) {
//						e.printStackTrace();
//					} catch (IOException e) {
//						e.printStackTrace();
//					}
//				}
			}
		}
		return null;
	}

	/**
	 * Collect the resource bundles to validate and
	 * index the corresponding MessageBundleGroup(s).
	 * @param resource The resource currently validated.
	 */
	void checkBundleResource(IResource resource) {
        if (resource instanceof IFile && resource.getName().endsWith(
                ".properties")) { //$NON-NLS-1$ //TODO have customized?
            IFile file = (IFile) resource;
            if (file.isDerived()) {
            	return;
            }
            //System.err.println("Looking at " + file.getFullPath());
            deleteMarkers(file);
            MessagesBundleGroup msgBundleGrp = null;
            if (_alreadBuiltMessageBundle == null) {
            	_alreadBuiltMessageBundle = new HashMap<IFile,MessagesBundleGroup>();
            	_resourcesToValidate = new HashSet();
            } else {
            	msgBundleGrp = _alreadBuiltMessageBundle.get(file);
            }
            if (msgBundleGrp == null) {
            	msgBundleGrp = MessagesBundleGroupFactory.createBundleGroup(null, file);
            	if (msgBundleGrp != null) {
            		//index the files for which this MessagesBundleGroup
            		//should be used for the validation.
            		//cheaper than creating a group for each on of those
            		//files.
            		boolean validateEntireGroup = false;
                	for (MessagesBundle msgBundle : msgBundleGrp.getMessagesBundles()) {
            			Object src = msgBundle.getResource().getSource();
            			//System.err.println(src + " -> " + msgBundleGrp);
            			if (src instanceof IFile) {//when it is a read-only thing we don't index it.
	            			_alreadBuiltMessageBundle.put((IFile)src, msgBundleGrp);
	            			if (!validateEntireGroup && src == resource) {
	            				if (msgBundle.getLocale() == null
	            						|| msgBundle.getLocale().equals(UIUtils.ROOT_LOCALE)) {
	            					//ok the default properties have been changed.
	            					//make sure that all resources in this bundle group
	            					//are validated too:
	            					validateEntireGroup = true;
	            					
	            					//TODO: eventually something similar.
	            					//with foo_en.properties changed.
	            					//then foo_en_US.properties must be revalidated
	            					//and foo_en_CA.properties as well.
	            					
	            				}
	            			}
            			}
            		}
            		if (validateEntireGroup) {
                    	for (MessagesBundle msgBundle : msgBundleGrp.getMessagesBundles()) {
			    			Object src = msgBundle.getResource().getSource();
			    			_resourcesToValidate.add(src);
                   		}
            		}
            	}
            }
            
            _resourcesToValidate.add(resource);
            
        }
	}
	
	/**
	 * Validates the message bundles collected by the visitor.
	 * Makes sure we validate only once each message bundle and build only once each
	 * MessageBundleGroup it belongs to.
	 */
	private void finishBuild() {
		if (_resourcesToValidate != null) {
			for (Iterator it = _resourcesToValidate.iterator(); it.hasNext();) {
				IFile resource = (IFile)it.next();
				MessagesBundleGroup msgBundleGrp =
					_alreadBuiltMessageBundle.get(resource);
				
				if (msgBundleGrp != null) {
					//when null it is probably because it was skipped from
					//the group because the locale was filtered.
					try {
				//		System.out.println("Validate " + resource); //$NON-NLS-1$
						//TODO check if there is a matching EclipsePropertiesEditorResource already open.
						//else, create MessagesBundle from PropertiesIFileResource
						MessagesBundle messagesBundle = msgBundleGrp.getMessagesBundle(resource);
						if (messagesBundle != null) {
							Locale locale = messagesBundle.getLocale();
							MessagesBundleGroupValidator.validate(msgBundleGrp, locale, markerStrategy);
						}//, _indexer);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
				//	System.out.println("Not validating " + resource); //$NON-NLS-1$
				}
			}
		}
	}

	private void deleteMarkers(IFile file) {
		try {
//            System.out.println("Builder: deleteMarkers"); //$NON-NLS-1$
			file.deleteMarkers(MessagesEditorPlugin.MARKER_TYPE, false, IResource.DEPTH_ZERO);
		} catch (CoreException ce) {
		}
	}

	protected void fullBuild(final IProgressMonitor monitor)
			throws CoreException {
//        System.out.println("Builder: fullBuild"); //$NON-NLS-1$
        getProject().accept(new SampleResourceVisitor());
	}

	protected void incrementalBuild(IResourceDelta delta,
			IProgressMonitor monitor) throws CoreException {
//        System.out.println("Builder: incrementalBuild"); //$NON-NLS-1$
        delta.accept(new SampleDeltaVisitor());
	}
	
	protected void clean(IProgressMonitor monitor) throws CoreException {
		ResourcesPlugin.getWorkspace().getRoot()
			.deleteMarkers(MessagesEditorPlugin.MARKER_TYPE, false, IResource.DEPTH_INFINITE);
	}
	
	
}
