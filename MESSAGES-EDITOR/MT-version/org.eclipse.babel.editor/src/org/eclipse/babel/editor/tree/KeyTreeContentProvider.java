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

import org.eclipse.babel.core.message.tree.IKeyTreeModel;
import org.eclipse.babel.core.message.tree.KeyTreeNode;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;


/**
 * Content provider for key tree viewer.
 * @author Pascal Essiembre
 */
public class KeyTreeContentProvider implements ITreeContentProvider {

    private IKeyTreeModel keyTreeModel;
//    private TreeViewer treeViewer;
    
    /**
     * 
     */
    public KeyTreeContentProvider() {
        super();
    }

    /**
     * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(
     *              java.lang.Object)
     */
    public Object[] getChildren(Object parentElement) {
        return keyTreeModel.getChildren((KeyTreeNode) parentElement);
    }

    /**
     * @see org.eclipse.jface.viewers.ITreeContentProvider#
     *              getParent(java.lang.Object)
     */
    public Object getParent(Object element) {
        return keyTreeModel.getParent((KeyTreeNode) element);
    }

    /**
     * @see org.eclipse.jface.viewers.ITreeContentProvider#
     *              hasChildren(java.lang.Object)
     */
    public boolean hasChildren(Object element) {
        return keyTreeModel.getChildren((KeyTreeNode) element).length > 0;
    }

    /**
     * @see org.eclipse.jface.viewers.IStructuredContentProvider#
     *              getElements(java.lang.Object)
     */
    public Object[] getElements(Object inputElement) {
        return keyTreeModel.getRootNodes();
    }

    /**
     * @see org.eclipse.jface.viewers.IContentProvider#dispose()
     */
    public void dispose() {}

    /**
     * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(
     *              org.eclipse.jface.viewers.Viewer,
     *              java.lang.Object, java.lang.Object)
     */
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
//        this.treeViewer = (TreeViewer) viewer;
        this.keyTreeModel = (IKeyTreeModel) newInput;
    }


}
