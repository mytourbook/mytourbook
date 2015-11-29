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

import org.eclipse.babel.editor.actions.FilterKeysAction;
import org.eclipse.babel.editor.actions.KeyTreeVisibleAction;
import org.eclipse.babel.editor.actions.NewLocaleAction;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.ide.IDEActionFactory;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;


/**
 * Manages the installation/deinstallation of global actions for multi-page 
 * editors. Responsible for the redirection of global actions to the active 
 * editor.
 * Multi-page contributor replaces the contributors for the individual editors
 * in the multi-page editor.
 */
public class MessagesEditorContributor 
        extends MultiPageEditorActionBarContributor {
	private IEditorPart activeEditorPart;

    private KeyTreeVisibleAction toggleKeyTreeAction;
    private NewLocaleAction newLocaleAction;
    private IMenuManager resourceBundleMenu;
    private static String resourceBundleMenuID;
    
    /** singleton: probably not the cleanest way to access it from anywhere. */
    public static ActionGroup FILTERS = new FilterKeysActionGroup();
        
    /**
	 * Creates a multi-page contributor.
	 */
	public MessagesEditorContributor() {
		super();
		createActions();
	}
	/**
	 * Returns the action registed with the given text editor.
     * @param editor eclipse text editor
     * @param actionID action id
	 * @return IAction or null if editor is null.
	 */
	protected IAction getAction(ITextEditor editor, String actionID) {
		return (editor == null ? null : editor.getAction(actionID));
	}

    /**
	 * @see MultiPageEditorActionBarContributor
     *         #setActivePage(org.eclipse.ui.IEditorPart)
	 */
	public void setActivePage(IEditorPart part) {
        if (activeEditorPart == part) {
			return;
        }

		activeEditorPart = part;

		IActionBars actionBars = getActionBars();
		if (actionBars != null) {

			ITextEditor editor = (part instanceof ITextEditor) 
                               ? (ITextEditor) part : null;

			actionBars.setGlobalActionHandler(
				ActionFactory.DELETE.getId(),
				getAction(editor, ITextEditorActionConstants.DELETE));
			actionBars.setGlobalActionHandler(
				ActionFactory.UNDO.getId(),
				getAction(editor, ITextEditorActionConstants.UNDO));
			actionBars.setGlobalActionHandler(
				ActionFactory.REDO.getId(),
				getAction(editor, ITextEditorActionConstants.REDO));
			actionBars.setGlobalActionHandler(
				ActionFactory.CUT.getId(),
				getAction(editor, ITextEditorActionConstants.CUT));
			actionBars.setGlobalActionHandler(
				ActionFactory.COPY.getId(),
				getAction(editor, ITextEditorActionConstants.COPY));
			actionBars.setGlobalActionHandler(
				ActionFactory.PASTE.getId(),
				getAction(editor, ITextEditorActionConstants.PASTE));
			actionBars.setGlobalActionHandler(
				ActionFactory.SELECT_ALL.getId(),
				getAction(editor, ITextEditorActionConstants.SELECT_ALL));
			actionBars.setGlobalActionHandler(
				ActionFactory.FIND.getId(),
				getAction(editor, ITextEditorActionConstants.FIND));
			actionBars.setGlobalActionHandler(
				IDEActionFactory.BOOKMARK.getId(),
				getAction(editor, IDEActionFactory.BOOKMARK.getId()));
			actionBars.updateActionBars();
		}
	}
	private void createActions() {
//		sampleAction = new Action() {
//			public void run() {
//				MessageDialog.openInformation(null,
//        "ResourceBundle Editor Plug-in", "Sample Action Executed");
//			}
//		};
//		sampleAction.setText("Sample Action");
//		sampleAction.setToolTipText("Sample Action tool tip");
//		sampleAction.setImageDescriptor(
//        PlatformUI.getWorkbench().getSharedImages().
//				getImageDescriptor(IDE.SharedImages.IMG_OBJS_TASK_TSK));
        
        toggleKeyTreeAction = new KeyTreeVisibleAction();
        newLocaleAction = new NewLocaleAction();
        

//        toggleKeyTreeAction.setText("Show/Hide Key Tree");
//        toggleKeyTreeAction.setToolTipText("Click to show/hide the key tree");
//        toggleKeyTreeAction.setImageDescriptor(
//        PlatformUI.getWorkbench().getSharedImages().
//                getImageDescriptor(IDE.SharedImages.IMG_OPEN_MARKER));

    }
	/**
	 * @see org.eclipse.ui.part.EditorActionBarContributor
     *         #contributeToMenu(org.eclipse.jface.action.IMenuManager)
	 */
    public void contributeToMenu(IMenuManager manager) {
//        System.out.println("active editor part:" +activeEditorPart);
//        System.out.println("menu editor:" + rbEditor);
		resourceBundleMenu = new MenuManager("&Messages", resourceBundleMenuID);
		manager.prependToGroup(IWorkbenchActionConstants.M_EDIT, resourceBundleMenu);
		resourceBundleMenu.add(toggleKeyTreeAction);
		resourceBundleMenu.add(newLocaleAction);
		
		FILTERS.fillContextMenu(resourceBundleMenu);
	}
    /**
     * @see org.eclipse.ui.part.EditorActionBarContributor
     *         #contributeToToolBar(org.eclipse.jface.action.IToolBarManager)
     */
	public void contributeToToolBar(IToolBarManager manager) {
//        System.out.println("toolbar get page:" + getPage());
//        System.out.println("toolbar editor:" + rbEditor);
		manager.add(new Separator());
//		manager.add(sampleAction);
		manager.add(toggleKeyTreeAction);
        manager.add(newLocaleAction);
        
        ((FilterKeysActionGroup)FILTERS).fillActionBars(getActionBars());
	}
    /* (non-Javadoc)
     * @see org.eclipse.ui.part.MultiPageEditorActionBarContributor#setActiveEditor(org.eclipse.ui.IEditorPart)
     */
    public void setActiveEditor(IEditorPart part) {
        super.setActiveEditor(part);
    	MessagesEditor me = part instanceof MessagesEditor
			? (MessagesEditor)part : null;
        toggleKeyTreeAction.setEditor(me);
        ((FilterKeysActionGroup) FILTERS).setActiveEditor(part);
    }
    
    /**
     * Groups the filter actions together.
     */
    private static class FilterKeysActionGroup extends ActionGroup {
        FilterKeysAction[] filtersAction = new FilterKeysAction[4];

        public FilterKeysActionGroup() {
            filtersAction[0] = new FilterKeysAction(IMessagesEditorChangeListener.SHOW_ALL);
            filtersAction[1] = new FilterKeysAction(IMessagesEditorChangeListener.SHOW_ONLY_MISSING);
            filtersAction[2] = new FilterKeysAction(IMessagesEditorChangeListener.SHOW_ONLY_MISSING_AND_UNUSED);
            filtersAction[3] = new FilterKeysAction(IMessagesEditorChangeListener.SHOW_ONLY_UNUSED);
        }
        
		public void fillActionBars(IActionBars actionBars) {
	        for (int i = 0; i < filtersAction.length; i++) {
	        	actionBars.getToolBarManager().add(filtersAction[i]);
	        }
		}

		public void fillContextMenu(IMenuManager menu) {
	        MenuManager filters = new MenuManager("Filters");
	        for (int i = 0; i < filtersAction.length; i++) {
	        	filters.add(filtersAction[i]);
	        }
	        menu.add(filters);
		}

		public void updateActionBars() {
			for (int i = 0; i < filtersAction.length;i++) {
				filtersAction[i].update();
			}
		}
	    public void setActiveEditor(IEditorPart part) {
	    	MessagesEditor me = part instanceof MessagesEditor
	    		? (MessagesEditor)part : null;
	    	for (int i = 0; i < filtersAction.length; i++) {
	        	filtersAction[i].setEditor(me);
	        }
	    }
    }
    
}
