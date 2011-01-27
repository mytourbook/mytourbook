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
package org.eclipse.babel.editor.i18n;

import org.eclipse.babel.editor.MessagesEditor;
import org.eclipse.babel.editor.tree.KeyTreeContributor;
import org.eclipse.babel.editor.tree.actions.CollapseAllAction;
import org.eclipse.babel.editor.tree.actions.ExpandAllAction;
import org.eclipse.babel.editor.tree.actions.FlatModelAction;
import org.eclipse.babel.editor.tree.actions.TreeModelAction;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Tree for displaying and navigating through resource bundle keys.
 * @author Pascal Essiembre
 */
public class SideNavComposite extends Composite {

    /** Key Tree Viewer. */
    private TreeViewer treeViewer;
        
    private MessagesEditor editor;
    
    /**
     * Constructor.
     * @param parent parent composite
     * @param keyTree key tree
     */
    public SideNavComposite(
            Composite parent, final MessagesEditor editor) {
        super(parent, SWT.BORDER);
        this.editor = editor;

        // Create a toolbar.
        ToolBarManager toolBarMgr = new ToolBarManager(SWT.FLAT);
        ToolBar toolBar = toolBarMgr.createControl(this);

        
        this.treeViewer = new TreeViewer(this,
                SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);

        
        setLayout(new GridLayout(1, false));

        GridData gid;

        gid = new GridData();
        gid.horizontalAlignment = GridData.END;
        gid.verticalAlignment = GridData.BEGINNING;
        toolBar.setLayoutData(gid);
        toolBarMgr.add(new TreeModelAction(editor, treeViewer));
        toolBarMgr.add(new FlatModelAction(editor, treeViewer));
        toolBarMgr.add(new Separator());
        toolBarMgr.add(new ExpandAllAction(editor, treeViewer));
        toolBarMgr.add(new CollapseAllAction(editor, treeViewer));
        toolBarMgr.update(true);
        
        //TODO have two toolbars, one left-align, and one right, with drop
        //down menu
        
        
//        createTopSection();
        createKeyTree();
        new SideNavTextBoxComposite(this, editor);
    }

    /**
     * Gets the tree viewer.
     * @return tree viewer
     */
    public TreeViewer getTreeViewer() {
        return treeViewer;
    }
    
    /**
     * @see org.eclipse.swt.widgets.Widget#dispose()
     */
    public void dispose() {
        super.dispose();
    }
    
    /**
     * Creates the middle (tree) section of this composite.
     */
    private void createKeyTree() {

        GridData gridData = new GridData();
        gridData.verticalAlignment = GridData.FILL;
        gridData.grabExcessVerticalSpace = true;
        gridData.horizontalAlignment = GridData.FILL;
        gridData.grabExcessHorizontalSpace = true;

        
        KeyTreeContributor treeContributor = new KeyTreeContributor(editor);
        treeContributor.contribute(treeViewer);
        treeViewer.getTree().setLayoutData(gridData);      

    }
    
}
