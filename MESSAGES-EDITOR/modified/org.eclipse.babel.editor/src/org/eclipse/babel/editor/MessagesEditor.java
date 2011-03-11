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
package org.eclipse.babel.editor;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.eclipse.babel.core.message.MessageException;
import org.eclipse.babel.core.message.MessagesBundle;
import org.eclipse.babel.core.message.MessagesBundleGroup;
import org.eclipse.babel.core.message.resource.IMessagesResource;
import org.eclipse.babel.core.message.tree.AbstractKeyTreeModel;
import org.eclipse.babel.editor.builder.ToggleNatureAction;
import org.eclipse.babel.editor.bundle.MessagesBundleGroupFactory;
import org.eclipse.babel.editor.i18n.I18NPage;
import org.eclipse.babel.editor.plugin.MessagesEditorPlugin;
import org.eclipse.babel.editor.preferences.MsgEditorPreferences;
import org.eclipse.babel.editor.resource.EclipsePropertiesEditorResource;
import org.eclipse.babel.editor.util.UIUtils;
import org.eclipse.babel.editor.views.MessagesBundleGroupOutline;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

/**
 * Multi-page editor for editing resource bundles.
 */
public class MessagesEditor extends MultiPageEditorPart
        implements IGotoMarker {

    /** Editor ID, as defined in plugin.xml. */
    public static final String EDITOR_ID = 
       "org.eclilpse.babel.editor.editor.MessagesEditor"; //$NON-NLS-1$

    private String selectedKey;
    private List<IMessagesEditorChangeListener> changeListeners = new ArrayList<IMessagesEditorChangeListener>(2);
    
    /** MessagesBundle group. */
    private MessagesBundleGroup messagesBundleGroup;

    /** Page with key tree and text fields for all locales. */
    private I18NPage i18nPage;
    private final List<Locale> localesIndex = new ArrayList<Locale>();
    private final List<ITextEditor> textEditorsIndex = new ArrayList<ITextEditor>();
    
    private MessagesBundleGroupOutline outline;
    
    private MessagesEditorMarkers markers;
    
    
    private AbstractKeyTreeModel keyTreeModel;
    
    /**
     * Creates a multi-page editor example.
     */
    public MessagesEditor() {
        super();
        outline = new MessagesBundleGroupOutline(this);
    }
    
    public MessagesEditorMarkers getMarkers() {
        return markers;
    }
    
    private IPropertyChangeListener preferenceListener;
    
    /**
     * The <code>MultiPageEditorExample</code> implementation of this method
     * checks that the input is an instance of <code>IFileEditorInput</code>.
     */
    public void init(IEditorSite site, IEditorInput editorInput)
        throws PartInitException {
    	
    	if (editorInput instanceof IFileEditorInput) {
            IFile file = ((IFileEditorInput) editorInput).getFile();
            if (MsgEditorPreferences.getInstance().isBuilderSetupAutomatically()) {
	            IProject p = file.getProject();
	            if (p != null && p.isAccessible()) {
	            	ToggleNatureAction.addOrRemoveNatureOnProject(p, true, true);
	            }
            }
            try {
                messagesBundleGroup = MessagesBundleGroupFactory.createBundleGroup(site, file);
            } catch (MessageException e) {
                throw new PartInitException(
                        "Cannot create bundle group.", e); //$NON-NLS-1$
            }
            markers = new MessagesEditorMarkers(messagesBundleGroup);
            setPartName(messagesBundleGroup.getName());
            setTitleImage(UIUtils.getImage(UIUtils.IMAGE_RESOURCE_BUNDLE));
            closeIfAreadyOpen(site, file);
            super.init(site, editorInput);
            //TODO figure out model to use based on preferences
            keyTreeModel = new AbstractKeyTreeModel(messagesBundleGroup);
//            markerManager = new RBEMarkerManager(this);
        } else {
            throw new PartInitException(
                    "Invalid Input: Must be IFileEditorInput"); //$NON-NLS-1$
        }
    }

//    public RBEMarkerManager getMarkerManager() {
//        return markerManager;
//    }
    
    /**
     * Creates the pages of the multi-page editor.
     */
    protected void createPages() {
        // Create I18N page
        i18nPage = new I18NPage(
                getContainer(), SWT.NONE, this);
        int index = addPage(i18nPage);
        setPageText(index, MessagesEditorPlugin.getString(
                "editor.properties")); //$NON-NLS-1$
        setPageImage(index, UIUtils.getImage(UIUtils.IMAGE_RESOURCE_BUNDLE));
        
        // Create text editor pages for each locales
        try {
            Locale[] locales = messagesBundleGroup.getLocales();
            //first: sort the locales.
            UIUtils.sortLocales(locales);
            //second: filter+sort them according to the filter preferences.
            locales = UIUtils.filterLocales(locales);
            for (int i = 0; i < locales.length; i++) {
            	Locale locale = locales[i];
                MessagesBundle messagesBundle = messagesBundleGroup.getMessagesBundle(locale);
                IMessagesResource resource = messagesBundle.getResource();
                TextEditor textEditor = (TextEditor) resource.getSource();
                index = addPage(textEditor, textEditor.getEditorInput());
                setPageText(index, UIUtils.getDisplayName(
                        messagesBundle.getLocale()));
                setPageImage(index, 
                        UIUtils.getImage(UIUtils.IMAGE_PROPERTIES_FILE));
                localesIndex.add(locale);
                textEditorsIndex.add(textEditor);
            } 
        } catch (PartInitException e) {
            ErrorDialog.openError(getSite().getShell(), 
                "Error creating text editor page.", //$NON-NLS-1$
                null, e.getStatus());
        }
    }
    
    /**
     * Called when the editor's pages need to be reloaded.
     * For example when the filters of locale is changed.
     * <p>
     * Currently this only reloads the index page.
     * TODO: remove and add the new locales? it actually looks quite hard to do.
     * </p>
     */
    public void reloadDisplayedContents() {
    	super.removePage(0);
    	int currentlyActivePage = super.getActivePage();
    	i18nPage.dispose();
    	i18nPage = new I18NPage(
                getContainer(), SWT.NONE, this);
    	super.addPage(0, i18nPage);
    	if (currentlyActivePage == 0) {
    		super.setActivePage(currentlyActivePage);
    	}
    }

    /**
     * Saves the multi-page editor's document.
     */
    public void doSave(IProgressMonitor monitor) {
        for (ITextEditor textEditor : textEditorsIndex) {
            textEditor.doSave(monitor);
        }
//        i18nPage.refreshEditorOnChanges();
//        resourceMediator.save(monitor);
    }
    
    /**
     * @see org.eclipse.ui.ISaveablePart#doSaveAs()
     */
    public void doSaveAs() {
        // Save As not allowed.
    }
    
    /**
     * @see org.eclipse.ui.ISaveablePart#isSaveAsAllowed()
     */
    public boolean isSaveAsAllowed() {
        return false;
    }

    /**
     * Change current page based on locale.  If there is no editors associated
     * with current locale, do nothing.
     * @param locale locale used to identify the page to change to
     */
    public void setActivePage(Locale locale) {
        int index = localesIndex.indexOf(locale);
        if (index > -1) {
            setActivePage(index + 1);
        }
    }

    /**
     * @see org.eclipse.ui.ide.IGotoMarker#gotoMarker(
     *         org.eclipse.core.resources.IMarker)
     */
    public void gotoMarker(IMarker marker) {
//        String key = marker.getAttribute(RBEMarker.KEY, "");
//        if (key != null && key.length() > 0) {
//            setActivePage(0);
//            setSelectedKey(key);
//            getI18NPage().selectLocale(BabelUtils.parseLocale(
//                    marker.getAttribute(RBEMarker.LOCALE, "")));
//        } else {
            IResource resource = marker.getResource();
            Locale[] locales = messagesBundleGroup.getLocales();
            for (int i = 0; i < locales.length; i++) {
                IMessagesResource messagesResource =
                        messagesBundleGroup.getMessagesBundle(locales[i]).getResource();
                if (messagesResource instanceof EclipsePropertiesEditorResource) {
                    EclipsePropertiesEditorResource propFile =
                            (EclipsePropertiesEditorResource) messagesResource;
                    if (resource.equals(propFile.getResource())) {
                    	//ok we got the locale.
                    	//try to open the master i18n page and select the corresponding key.
                    	try {
	                    	String key = (String) marker.getAttribute(IMarker.LOCATION);
	                    	if (key != null && key.length() > 0) {
	                    		getI18NPage().selectLocale(locales[i]);
	                    		setActivePage(0);
	                    		setSelectedKey(key);
	                    		return;
	                    	}
                    	} catch (Exception e) {
                    		e.printStackTrace();//something better.s
                    	}
                    	//it did not work... fall back to the text editor.
                        setActivePage(locales[i]);
                        IDE.gotoMarker(
                                (IEditorPart) propFile.getSource(), marker);
                        break;
                    }
                }
            }
//        }
    }
    
    /**
     * Calculates the contents of page GUI page when it is activated.
     */
    protected void pageChange(int newPageIndex) {
        super.pageChange(newPageIndex);
//        if (newPageIndex == 0) {
//            resourceMediator.reloadProperties();
//            i18nPage.refreshTextBoxes();
//        }
    }

    
    /**
     * Is the given file a member of this resource bundle.
     * @param file file to test
     * @return <code>true</code> if file is part of bundle
     */
    public boolean isBundleMember(IFile file) {
//        return resourceMediator.isResource(file);
        return false;
    }

    private void closeIfAreadyOpen(IEditorSite site, IFile file) {
        IWorkbenchPage[] pages = site.getWorkbenchWindow().getPages();
        for (int i = 0; i < pages.length; i++) {
            IWorkbenchPage page = pages[i];
            IEditorReference[] editors = page.getEditorReferences();
            for (int j = 0; j < editors.length; j++) {
                IEditorPart editor = editors[j].getEditor(false);
                if (editor instanceof MessagesEditor) {
                    MessagesEditor rbe = (MessagesEditor) editor;
                    if (rbe.isBundleMember(file)) {
                        page.closeEditor(editor, true);
                    }
                }
            }
        }
    }

    
    
    /**
     * @see org.eclipse.ui.IWorkbenchPart#dispose()
     */
    public void dispose() {
        for (IMessagesEditorChangeListener listener : changeListeners) {
            listener.editorDisposed();
        }
        i18nPage.dispose();
        for (ITextEditor textEditor : textEditorsIndex) {
            textEditor.dispose();
        }
    }


    /**
     * @return Returns the selectedKey.
     */
    public String getSelectedKey() {
        return selectedKey;
    }
    /**
     * @param selectedKey The selectedKey to set.
     */
    public void setSelectedKey(String activeKey) {
        if ((selectedKey == null && activeKey != null)
                || (selectedKey != null && activeKey == null)
                || (selectedKey != null && !selectedKey.equals(activeKey))) {
            String oldKey = this.selectedKey;
            this.selectedKey = activeKey;
            for (IMessagesEditorChangeListener listener : changeListeners) {
                listener.selectedKeyChanged(oldKey, activeKey);
            }
        }
    }

    public void addChangeListener(IMessagesEditorChangeListener listener) {
        changeListeners.add(0, listener);
    }
    
    public void removeChangeListener(IMessagesEditorChangeListener listener) {
        changeListeners.remove(listener);
    }
    
    public Collection<IMessagesEditorChangeListener> getChangeListeners() {
        return changeListeners;
    }
    
    /**
     * @return Returns the messagesBundleGroup.
     */
    public MessagesBundleGroup getBundleGroup() {
        return messagesBundleGroup;
    }

    /**
     * @return Returns the keyTreeModel.
     */
    public AbstractKeyTreeModel getKeyTreeModel() {
        return keyTreeModel;
    }

    /**
     * @param keyTreeModel The keyTreeModel to set.
     */
    public void setKeyTreeModel(AbstractKeyTreeModel newKeyTreeModel) {
        if ((this.keyTreeModel == null && newKeyTreeModel != null)
                || (keyTreeModel != null && newKeyTreeModel == null)
                || (!keyTreeModel.equals(newKeyTreeModel))) {
        	AbstractKeyTreeModel oldModel = this.keyTreeModel;
            this.keyTreeModel = newKeyTreeModel;
            for (IMessagesEditorChangeListener listener : changeListeners) {
            	listener.keyTreeModelChanged(oldModel, newKeyTreeModel);
            }
        }
    }

    public I18NPage getI18NPage() {
        return i18nPage;
    }
    
    /** one of the SHOW_* constants defined in the {@link IMessagesEditorChangeListener} */    
    private int showOnlyMissingAndUnusedKeys = IMessagesEditorChangeListener.SHOW_ALL;
    /**
     * @return true when only unused and missing keys should be displayed. flase by default.
     */
    public int isShowOnlyUnusedAndMissingKeys() {
        return showOnlyMissingAndUnusedKeys;
    }
    
    public void setShowOnlyUnusedMissingKeys(int showFlag) {
        showOnlyMissingAndUnusedKeys = showFlag;
        for (IMessagesEditorChangeListener listener : getChangeListeners()) {
        	listener.showOnlyUnusedAndMissingChanged(showFlag);
        }
    }

    public Object getAdapter(Class adapter) {
        Object obj = super.getAdapter(adapter);
        if (obj == null) {
            if (IContentOutlinePage.class.equals(adapter)) {
                return (outline);
            }
        }
        return (obj);
    }

    public ITextEditor getTextEditor(Locale locale) {
        int index = localesIndex.indexOf(locale);
        return textEditorsIndex.get(index);
    }
}
