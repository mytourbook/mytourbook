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

import org.eclipse.babel.core.message.MessagesBundleGroup;
import org.eclipse.babel.core.message.tree.KeyTreeNode;
import org.eclipse.babel.editor.MessagesEditor;
import org.eclipse.babel.editor.plugin.MessagesEditorPlugin;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * @author Pascal Essiembre
 *
 */
public class DeleteKeyAction extends AbstractTreeAction {

    /**
     * 
     */
    public DeleteKeyAction(MessagesEditor editor, TreeViewer treeViewer) {
        super(editor, treeViewer);
        setText(MessagesEditorPlugin.getString("key.delete")); //$NON-NLS-1$
        setImageDescriptor(
                PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
                        ISharedImages.IMG_TOOL_DELETE));
        setToolTipText("TODO put something here"); //TODO put tooltip
//        setActionDefinitionId("org.eclilpse.babel.editor.editor.tree.delete");
//      setActionDefinitionId("org.eclipse.ui.edit.delete");
//        editor.getSite().getKeyBindingService().registerAction(this);
    }


    /* (non-Javadoc)
     * @see org.eclipse.jface.action.Action#run()
     */
    public void run() {
        KeyTreeNode node = getNodeSelection();
        String key = node.getMessageKey();
        String msgHead = null;
        String msgBody = null;
        if (getContentProvider().hasChildren(node)) {
            msgHead = MessagesEditorPlugin.getString(
                    "dialog.delete.head.multiple"); //$NON-NLS-1$
            msgBody = MessagesEditorPlugin.getString(
                    "dialog.delete.body.multiple", key);//$NON-NLS-1$ 
        } else {
            msgHead = MessagesEditorPlugin.getString(
                    "dialog.delete.head.single"); //$NON-NLS-1$
            msgBody = MessagesEditorPlugin.getString(
                    "dialog.delete.body.single", key); //$NON-NLS-1$
        }
        MessageBox msgBox = new MessageBox(
                getShell(), SWT.ICON_QUESTION|SWT.OK|SWT.CANCEL);
        msgBox.setMessage(msgBody);
        msgBox.setText(msgHead);
        if (msgBox.open() == SWT.OK) {
            MessagesBundleGroup messagesBundleGroup = getBundleGroup();
            KeyTreeNode[] nodesToDelete = getBranchNodes(node);
            for (int i = 0; i < nodesToDelete.length; i++) {
                KeyTreeNode nodeToDelete = nodesToDelete[i];
                messagesBundleGroup.removeMessages(nodeToDelete.getMessageKey());
            }
        }
    }
    
    
}
