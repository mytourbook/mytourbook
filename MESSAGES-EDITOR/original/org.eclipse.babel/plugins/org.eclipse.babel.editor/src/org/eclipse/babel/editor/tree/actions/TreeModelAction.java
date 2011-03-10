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
package org.eclipse.babel.editor.tree.actions;

import org.eclipse.babel.core.message.tree.DefaultKeyTreeModel;
import org.eclipse.babel.editor.MessagesEditor;
import org.eclipse.babel.editor.plugin.MessagesEditorPlugin;
import org.eclipse.babel.editor.util.UIUtils;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.TreeViewer;

/**
 * @author Pascal Essiembre
 *
 */
public class TreeModelAction extends AbstractTreeAction {

    /**
     * @param editor
     * @param treeViewer
     */
    public TreeModelAction(MessagesEditor editor, TreeViewer treeViewer) {
        super(editor, treeViewer, IAction.AS_RADIO_BUTTON);
        setText(MessagesEditorPlugin.getString("key.layout.tree")); //$NON-NLS-1$
        setImageDescriptor(
                UIUtils.getImageDescriptor(UIUtils.IMAGE_LAYOUT_HIERARCHICAL));
        setToolTipText("Display as in a Tree"); //TODO put tooltip
        setChecked(true);
    }

    /**
     * @see org.eclipse.jface.action.Action#run()
     */
    public void run() {
        //TODO see model to tree viewer, not editor...
        editor.setKeyTreeModel(new DefaultKeyTreeModel(editor.getBundleGroup()));
    }
}
