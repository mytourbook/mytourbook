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
package org.eclipse.babel.editor.tree;

import java.util.Observable;
import java.util.Observer;

import org.eclipse.babel.core.message.tree.DefaultKeyTreeModel;
import org.eclipse.babel.core.message.tree.IKeyTreeModel;
import org.eclipse.babel.core.message.tree.IKeyTreeModelListener;
import org.eclipse.babel.core.message.tree.KeyTreeNode;
import org.eclipse.babel.editor.IMessagesEditorChangeListener;
import org.eclipse.babel.editor.MessagesEditor;
import org.eclipse.babel.editor.MessagesEditorChangeAdapter;
import org.eclipse.babel.editor.MessagesEditorMarkers;
import org.eclipse.babel.editor.tree.actions.AddKeyAction;
import org.eclipse.babel.editor.tree.actions.DeleteKeyAction;
import org.eclipse.babel.editor.tree.actions.RenameKeyAction;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;

/**
 * @author Pascal Essiembre
 *
 */
public class KeyTreeContributor {

    //TODO consider keeping instance of this in MessagesEditor
    // and having class variables for the actions.

    private MessagesEditor editor;
    private IKeyTreeModel treeModel;
    
    /**
     * 
     */
    public KeyTreeContributor(final MessagesEditor editor) {
        super();
        this.editor = editor;
        this.treeModel = new DefaultKeyTreeModel(editor.getBundleGroup());
    }



    /**
     * 
     */
    public void contribute(final TreeViewer treeViewer) {
        
        treeViewer.setContentProvider(new KeyTreeContentProvider());
        treeViewer.setLabelProvider(new KeyTreeLabelProvider(editor, treeModel));
        treeViewer.setUseHashlookup(true);
        
        ViewerFilter onlyUnusedAndMissingKeysFilter = new OnlyUnsuedAndMissingKey();
        ViewerFilter[] filters = {onlyUnusedAndMissingKeysFilter};
        treeViewer.setFilters(filters);

//        IKeyBindingService service = editor.getSite().getKeyBindingService();
//        service.setScopes(new String[]{"org.eclilpse.babel.editor.editor.tree"});
        
        contributeActions(treeViewer);

        contributeKeySync(treeViewer);
        
        contributeModelChanges(treeViewer);

        contributeDoubleClick(treeViewer);

        contributeMarkers(treeViewer);
        
        // Set input model
        treeViewer.setInput(treeModel);
        treeViewer.expandAll();
    }

    private class OnlyUnsuedAndMissingKey extends ViewerFilter implements
        IKeyTreeModel.IKeyTreeNodeLeafFilter {

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer,
         *      java.lang.Object, java.lang.Object)
         */
        public boolean select(Viewer viewer, Object parentElement,
                Object element) {
        	if (editor.isShowOnlyUnusedAndMissingKeys() == IMessagesEditorChangeListener.SHOW_ALL
        			|| !(element instanceof KeyTreeNode)) {
        		//no filtering. the element is displayed by default.
        		return true;
        	}
        	if (editor.getI18NPage() != null && editor.getI18NPage().isKeyTreeVisible()) {
        		return editor.getKeyTreeModel().isBranchFiltered(this, (KeyTreeNode)element);
        	} else {
        		return isFilteredLeaf((KeyTreeNode)element);
        	}
        }
        
        /**
    	 * @param node
    	 * @return true if this node should be in the filter. Does not navigate the tree
    	 * of KeyTreeNode. false unless the node is a missing or unused key.
    	 */
    	public boolean isFilteredLeaf(KeyTreeNode node) {
    		MessagesEditorMarkers markers = KeyTreeContributor.this.editor.getMarkers();
    		String key = node.getMessageKey();
    		boolean missingOrUnused = markers.isMissingOrUnusedKey(key);
    		if (!missingOrUnused) {
    			return false;
    		}
    		switch (editor.isShowOnlyUnusedAndMissingKeys()) {
    		case IMessagesEditorChangeListener.SHOW_ONLY_MISSING_AND_UNUSED:
    			return missingOrUnused;
    		case IMessagesEditorChangeListener.SHOW_ONLY_MISSING:
    			return !markers.isUnusedKey(key, missingOrUnused);
    		case IMessagesEditorChangeListener.SHOW_ONLY_UNUSED:
    			return markers.isUnusedKey(key, missingOrUnused);
    		default:
    			return false;
    		}
    	}
        
    }


    /**
     * Contributes markers.
     * @param treeViewer tree viewer
     */
    private void contributeMarkers(final TreeViewer treeViewer) {
        editor.getMarkers().addObserver(new Observer() {
            public void update(Observable o, Object arg) {
                treeViewer.refresh();
            }
        });
//      editor.addChangeListener(new MessagesEditorChangeAdapter() {
//      public void editorDisposed() {
//          editor.getMarkers().clear();
//      }
//  });

        
        
        
        
        
        
        
//        final IMarkerListener markerListener = new IMarkerListener() {
//            public void markerAdded(IMarker marker) {
//                PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable () {
//                    public void run() {
//                        if (!PlatformUI.getWorkbench().getDisplay().isDisposed()) {
//                            treeViewer.refresh(true);
//                        }
//                    }
//                });
//            }
//            public void markerRemoved(IMarker marker) {
//                PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable () {
//                    public void run() {
//                        if (!PlatformUI.getWorkbench().getDisplay().isDisposed()) {
//                            treeViewer.refresh(true);
//                        }
//                    }
//                });
//            }
//        };
//        editor.getMarkerManager().addMarkerListener(markerListener);
//        editor.addChangeListener(new MessagesEditorChangeAdapter() {
//            public void editorDisposed() {
//                editor.getMarkerManager().removeMarkerListener(markerListener);
//            }
//        });
    }



    /**
     * Contributes double-click support, expanding/collapsing nodes.
     * @param treeViewer tree viewer
     */
    private void contributeDoubleClick(final TreeViewer treeViewer) {
        treeViewer.getTree().addMouseListener(new MouseAdapter() {
            public void mouseDoubleClick(MouseEvent event) {
                IStructuredSelection selection = 
                    (IStructuredSelection) treeViewer.getSelection();
                Object element = selection.getFirstElement();
                if (treeViewer.isExpandable(element)) {
                    if (treeViewer.getExpandedState(element)) {
                        treeViewer.collapseToLevel(element, 1);
                    } else {
                        treeViewer.expandToLevel(element, 1);
                    }
                }
            }
        });
    }

    /**
     * Contributes key synchronization between editor and tree selected keys.
     * @param treeViewer tree viewer
     */
    private void contributeModelChanges(final TreeViewer treeViewer) {
        final IKeyTreeModelListener keyTreeListener = new IKeyTreeModelListener() {
            //TODO be smarter about refreshes.
            public void nodeAdded(KeyTreeNode node) {
                treeViewer.refresh(true);
            };
//            public void nodeChanged(KeyTreeNode node) {
//                treeViewer.refresh(true);
//            };
            public void nodeRemoved(KeyTreeNode node) {
                treeViewer.refresh(true);
            };
        };
        treeModel.addKeyTreeModelListener(keyTreeListener);
        editor.addChangeListener(new MessagesEditorChangeAdapter() {
            public void keyTreeModelChanged(IKeyTreeModel oldModel, IKeyTreeModel newModel) {
                oldModel.removeKeyTreeModelListener(keyTreeListener);
                newModel.addKeyTreeModelListener(keyTreeListener);
                treeViewer.setInput(newModel);
                treeViewer.refresh();
            }
        	public void showOnlyUnusedAndMissingChanged(int hideEverythingElse) {
        		treeViewer.refresh();
            }
        });
    }

    /**
     * Contributes key synchronization between editor and tree selected keys.
     * @param treeViewer tree viewer
     */
    private void contributeKeySync(final TreeViewer treeViewer) {
        // changes in tree selected key update the editor
        treeViewer.getTree().addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                IStructuredSelection selection = 
                    (IStructuredSelection) treeViewer.getSelection();
                if (selection != null && selection.getFirstElement() != null) {
                    KeyTreeNode node =
                            (KeyTreeNode) selection.getFirstElement();
                    System.out.println("viewer key/hash:" + node.getMessageKey() + "/" + node.hashCode());
                    editor.setSelectedKey(node.getMessageKey());
                } else {
                    editor.setSelectedKey(null);
                }
            }
        });
        // changes in editor selected key updates the tree
        editor.addChangeListener(new MessagesEditorChangeAdapter() {
            public void selectedKeyChanged(String oldKey, String newKey) {
                ITreeContentProvider provider =
                        (ITreeContentProvider) treeViewer.getContentProvider();
                KeyTreeNode node = findKeyTreeNode(
                        provider, provider.getElements(null), newKey);
                System.out.println("editor key/hash:" + node.getMessageKey() + "/" + node.hashCode());
                
//                String[] test = newKey.split("\\.");
//                treeViewer.setSelection(new StructuredSelection(test), true);
                
                
                treeViewer.setSelection(new StructuredSelection(node), true);
                treeViewer.getTree().showSelection();
            }
        });
    }



    /**
     * Contributes actions to the tree.
     * @param treeViewer tree viewer
     */
    private void contributeActions(final TreeViewer treeViewer) {
        Tree tree = treeViewer.getTree();
        
        // Add menu
        MenuManager menuManager = new MenuManager();
        Menu menu = menuManager.createContextMenu(tree);

        // Add
        final IAction addAction = new AddKeyAction(editor, treeViewer);
        menuManager.add(addAction);
        // Delete
        final IAction deleteAction = new DeleteKeyAction(editor, treeViewer);
        menuManager.add(deleteAction);
        // Rename
        final IAction renameAction = new RenameKeyAction(editor, treeViewer);
        menuManager.add(renameAction);
        
        menuManager.update(true);
        tree.setMenu(menu);
        
        // Bind actions to tree
        tree.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent event) {
                if (event.character == SWT.DEL) {
                    deleteAction.run();
                } else if (event.keyCode == SWT.F2) {
                    renameAction.run();
                }
            }
        });
    }
    
    private KeyTreeNode findKeyTreeNode(
            ITreeContentProvider provider, Object[] nodes, String key) {
        for (int i = 0; i < nodes.length; i++) {
            KeyTreeNode node = (KeyTreeNode) nodes[i];
            if (node.getMessageKey().equals(key)) {
                return node;
            }
            node = findKeyTreeNode(provider, provider.getChildren(node), key);
            if (node != null) {
                return node;
            }
        }
        return null;
    }
    
}
