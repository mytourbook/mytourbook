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
import org.eclipse.babel.editor.util.UIUtils;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;

/**
 * @author Pascal Essiembre
 *
 */
public class RenameKeyAction extends AbstractTreeAction {

    /**
     * 
     */
    public RenameKeyAction(MessagesEditor editor, TreeViewer treeViewer) {
        super(editor, treeViewer);
        setText(MessagesEditorPlugin.getString("key.rename")); //$NON-NLS-1$
        setImageDescriptor(UIUtils.getImageDescriptor(UIUtils.IMAGE_RENAME));
        setToolTipText("TODO put something here"); //TODO put tooltip
    }


    /**
     * @see org.eclipse.jface.action.Action#run()
     */
    public void run() {
        KeyTreeNode node = getNodeSelection();
        String key = node.getMessageKey();
        String msgHead = null;
        String msgBody = null;
        if (getContentProvider().hasChildren(node)) {
            msgHead = MessagesEditorPlugin.getString(
                    "dialog.rename.head.multiple"); //$NON-NLS-1$
            msgBody = MessagesEditorPlugin.getString(
                    "dialog.rename.body.multiple", //$NON-NLS-1$
                    key);
        } else {
            msgHead = MessagesEditorPlugin.getString(
                    "dialog.rename.head.single"); //$NON-NLS-1$
            msgBody = MessagesEditorPlugin.getString(
                    "dialog.rename.body.single", key); //$NON-NLS-1$
        }
        // Rename single item
        InputDialog dialog = new InputDialog(
                getShell(), msgHead, msgBody, key,  new IInputValidator() {
                    public String isValid(String newText) {
                        if (getBundleGroup().isMessageKey(newText)) {
                            return  MessagesEditorPlugin.getString(
                                    "dialog.error.exists");
                        }
                        return null;
                    }
                });
        dialog.open();
        if (dialog.getReturnCode() == Window.OK ) {
            String inputKey = dialog.getValue();
            MessagesBundleGroup messagesBundleGroup = getBundleGroup();
            KeyTreeNode[] branchNodes = getBranchNodes(node);
            for (int i = 0; i < branchNodes.length; i++) {
                KeyTreeNode branchNode = branchNodes[i];
                String oldKey = branchNode.getMessageKey();
                if (oldKey.startsWith(key)) {
                    String newKey = inputKey + oldKey.substring(key.length());
                    messagesBundleGroup.renameMessageKeys(oldKey, newKey);
                }
            }
        }
    }
    
    
}
