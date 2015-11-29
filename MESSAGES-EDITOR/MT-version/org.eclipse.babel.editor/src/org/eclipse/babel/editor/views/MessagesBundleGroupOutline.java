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
package org.eclipse.babel.editor.views;



import org.eclipse.babel.editor.MessagesEditor;
import org.eclipse.babel.editor.tree.KeyTreeContributor;
import org.eclipse.babel.editor.tree.actions.CollapseAllAction;
import org.eclipse.babel.editor.tree.actions.ExpandAllAction;
import org.eclipse.babel.editor.tree.actions.FlatModelAction;
import org.eclipse.babel.editor.tree.actions.TreeModelAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;


/**
 * This outline provides a view for the property keys coming with
 * with a ResourceBundle
 */
public class MessagesBundleGroupOutline extends ContentOutlinePage {

    private final MessagesEditor editor;
	
	public MessagesBundleGroupOutline(MessagesEditor editor) {
		super();
        this.editor = editor;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
        
        KeyTreeContributor treeContributor = new KeyTreeContributor(editor);
        treeContributor.contribute(getTreeViewer());
	}
	
//	/**
//	 * {@inheritDoc}
//	 */
//	public void dispose() {
//		contributor.dispose();
//		super.dispose();
//	}
//	
//	
//    /**
//     * Gets the selected key tree item.
//     * @return key tree item
//     */
//    public KeyTreeItem getTreeSelection() {
//        IStructuredSelection selection = (IStructuredSelection) getTreeViewer().getSelection();
//        return((KeyTreeItem) selection.getFirstElement());
//    }
//	
//	
//    /**
//     * Gets selected key.
//     * @return selected key
//     */
//    private String getSelectedKey() {
//        String      key  = null;
//        KeyTreeItem item = getTreeSelection();
//        if(item != null) {
//            key = item.getId();
//        }
//        return(key);
//    }
//	
//
    /**
     * {@inheritDoc}
     */
	public void setActionBars(IActionBars actionbars) {
		super.setActionBars(actionbars);
//		filterincomplete   = new ToggleAction(UIUtils.IMAGE_INCOMPLETE_ENTRIES);
//		flataction         = new ToggleAction(UIUtils.IMAGE_LAYOUT_FLAT);
//		hierarchicalaction = new ToggleAction(UIUtils.IMAGE_LAYOUT_HIERARCHICAL);
//		flataction         . setToolTipText(RBEPlugin.getString("key.layout.flat")); //$NON-NLS-1$
//		hierarchicalaction . setToolTipText(RBEPlugin.getString("key.layout.tree")); //$NON-NLS-1$
//		filterincomplete   . setToolTipText(RBEPlugin.getString("key.filter.incomplete")); //$NON-NLS-1$
//		flataction         . setChecked( ! hierarchical );
//		hierarchicalaction . setChecked(   hierarchical );
//		actionbars.getToolBarManager().add( flataction         );
//		actionbars.getToolBarManager().add( hierarchicalaction );
//		actionbars.getToolBarManager().add( filterincomplete   );
        IToolBarManager toolBarMgr = actionbars.getToolBarManager();
        
//        ActionGroup
//        ActionContext
//        IAction
        
        toolBarMgr.add(new TreeModelAction(editor, getTreeViewer()));
        toolBarMgr.add(new FlatModelAction(editor, getTreeViewer()));
        toolBarMgr.add(new Separator());
        toolBarMgr.add(new ExpandAllAction(editor, getTreeViewer()));
        toolBarMgr.add(new CollapseAllAction(editor, getTreeViewer()));
	}
//
//	
//	/**
//	 * Invokes ths functionality according to the toggled action.
//	 * 
//	 * @param action   The action that has been toggled.
//	 */
//	private void update(ToggleAction action) {
//		int actioncode = 0;
//		if(action == filterincomplete) {
//			actioncode = TreeViewerContributor.KT_INCOMPLETE;
//		} else if(action == flataction) {
//			actioncode = TreeViewerContributor.KT_FLAT;
//		} else if(action == hierarchicalaction) {
//			actioncode = TreeViewerContributor.KT_HIERARCHICAL;
//		}
//		contributor.update(actioncode, action.isChecked());
//		flataction.setChecked((contributor.getMode() & TreeViewerContributor.KT_HIERARCHICAL) == 0);
//		hierarchicalaction.setChecked((contributor.getMode() & TreeViewerContributor.KT_HIERARCHICAL) != 0);
//	}
//	
//	
//	/**
//	 * Simple toggle action which delegates it's invocation to
//	 * the method {@link #update(ToggleAction)}.
//	 */
//	private class ToggleAction extends Action {
//		
//		/**
//		 * Initialises this action using the supplied icon.
//		 * 
//		 * @param icon   The icon which shall be displayed.
//		 */
//		public ToggleAction(String icon) {
//			super(null, IAction.AS_CHECK_BOX);
//			setImageDescriptor(RBEPlugin.getImageDescriptor(icon));
//		}
//		
//		/**
//		 * {@inheritDoc}
//		 */
//		public void run() {
//			update(this);
//		}
//		
//	} /* ENDCLASS */
//	
//    
//    /**
//     * Implementation of custom behaviour.
//     */
//	private class LocalBehaviour extends MouseAdapter implements IDeltaListener            , 
//	                                                             ISelectionChangedListener {
//
//		
//		/**
//		 * {@inheritDoc}
//		 */
//        public void selectionChanged(SelectionChangedEvent event) {
//        	String selected = getSelectedKey();
//        	if(selected != null) {
//        		tree.selectKey(selected);
//        	}
//        }
//
//        /**
//         * {@inheritDoc}
//         */
//		public void add(DeltaEvent event) {
//		}
//
//        /**
//         * {@inheritDoc}
//         */
//		public void remove(DeltaEvent event) {
//		}
//
//        /**
//         * {@inheritDoc}
//         */
//		public void modify(DeltaEvent event) {
//		}
//
//        /**
//         * {@inheritDoc}
//         */
//		public void select(DeltaEvent event) {
//			KeyTreeItem item = (KeyTreeItem) event.receiver();
//			if(item != null) {
//				getTreeViewer().setSelection(new StructuredSelection(item));
//			}
//		}
//        
//		/**
//		 * {@inheritDoc}
//		 */
//        public void mouseDoubleClick(MouseEvent event) {
//            Object element = getSelection();
//            if (getTreeViewer().isExpandable(element)) {
//                if (getTreeViewer().getExpandedState(element)) {
//                	getTreeViewer().collapseToLevel(element, 1);
//                } else {
//                	getTreeViewer().expandToLevel(element, 1);
//                }
//            }
//        }
//
//	} /* ENDCLASS */
//

}
