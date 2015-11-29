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

import org.eclipse.babel.core.message.tree.FlatKeyTreeModel;
import org.eclipse.babel.editor.MessagesEditor;
import org.eclipse.babel.editor.plugin.MessagesEditorPlugin;
import org.eclipse.babel.editor.util.UIUtils;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.TreeViewer;

/**
 * @author Pascal Essiembre
 *
 */
public class FlatModelAction extends AbstractTreeAction {

    /**
     * @param editor
     * @param treeViewer
     */
    public FlatModelAction(MessagesEditor editor, TreeViewer treeViewer) {
        super(editor, treeViewer, IAction.AS_RADIO_BUTTON);
        setText(MessagesEditorPlugin.getString("key.layout.flat")); //$NON-NLS-1$
        setImageDescriptor(
                UIUtils.getImageDescriptor(UIUtils.IMAGE_LAYOUT_FLAT));
        setDisabledImageDescriptor(
                UIUtils.getImageDescriptor(UIUtils.IMAGE_LAYOUT_FLAT));
        setToolTipText("Display in a list"); //TODO put tooltip
    }

    /**
     * @see org.eclipse.jface.action.Action#run()
     */
    public void run() {
        editor.setKeyTreeModel(new FlatKeyTreeModel(editor.getBundleGroup()));
    }
}
