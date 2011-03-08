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

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.babel.core.message.tree.AbstractKeyTreeModel;
import org.eclipse.babel.core.message.tree.KeyTreeNode;
import org.eclipse.babel.core.message.tree.visitor.IKeyTreeVisitor;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;


/**
 * Content provider for key tree viewer.
 * @author Pascal Essiembre
 */
public class KeyTreeContentProvider implements ITreeContentProvider {

    private AbstractKeyTreeModel keyTreeModel;
    private Viewer viewer; 
    private TreeType treeType;
    
    /**
     * @param treeType 
     * 
     */
    public KeyTreeContentProvider(TreeType treeType) {
        this.treeType = treeType;
    }

    /**
     * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(
     *              java.lang.Object)
     */
    public Object[] getChildren(Object parentElement) {
        KeyTreeNode parentNode = (KeyTreeNode) parentElement;
        switch (treeType) {
        case Tree:
    		return keyTreeModel.getChildren(parentNode);
        case Flat:
    		return new KeyTreeNode[0];
    	default:
    		// Should not happen
    		return new KeyTreeNode[0];
        }
    }

    /**
     * @see org.eclipse.jface.viewers.ITreeContentProvider#
     *              getParent(java.lang.Object)
     */
    public Object getParent(Object element) {
        KeyTreeNode node = (KeyTreeNode) element;
        switch (treeType) {
        case Tree:
    		return keyTreeModel.getParent(node);
        case Flat:
    		return keyTreeModel;
    	default:
    		// Should not happen
    		return null;
        }
    }

    /**
     * @see org.eclipse.jface.viewers.ITreeContentProvider#
     *              hasChildren(java.lang.Object)
     */
    public boolean hasChildren(Object element) {
        switch (treeType) {
        case Tree:
            return keyTreeModel.getChildren((KeyTreeNode) element).length > 0;
        case Flat:
    		return false;
    	default:
    		// Should not happen
    		return false;
        }
    }

    /**
     * @see org.eclipse.jface.viewers.IStructuredContentProvider#
     *              getElements(java.lang.Object)
     */
    public Object[] getElements(Object inputElement) {
		switch (treeType) {
        case Tree:
            return keyTreeModel.getRootNodes();
        case Flat:
        	final Collection<KeyTreeNode> actualKeys = new ArrayList<KeyTreeNode>();
        	IKeyTreeVisitor visitor = new IKeyTreeVisitor() {
        		public void visitKeyTreeNode(KeyTreeNode node) {
        			if (node.isUsedAsKey()) {
        				actualKeys.add(node);
        			}
        		}
        	};
        	keyTreeModel.accept(visitor, keyTreeModel.getRootNode());
        	return actualKeys.toArray(); 
        default:
    		// Should not happen
    		return new KeyTreeNode[0];
        }
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
        this.viewer = (TreeViewer) viewer;
        this.keyTreeModel = (AbstractKeyTreeModel) newInput;
    }

	public TreeType getTreeType() {
		return treeType;
	}

	public void setTreeType(TreeType treeType) {
		if (this.treeType != treeType) {
			this.treeType = treeType;
			viewer.refresh();
		}
	}
}
