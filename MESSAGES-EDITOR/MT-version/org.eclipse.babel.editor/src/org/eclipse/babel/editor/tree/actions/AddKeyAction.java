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
public class AddKeyAction extends AbstractTreeAction {

    /**
     * 
     */
    public AddKeyAction(MessagesEditor editor, TreeViewer treeViewer) {
        super(editor, treeViewer);
        setText(MessagesEditorPlugin.getString("key.add")); //$NON-NLS-1$
        setImageDescriptor(UIUtils.getImageDescriptor(UIUtils.IMAGE_ADD));
        setToolTipText("TODO put something here"); //TODO put tooltip
    }


    /**
     * @see org.eclipse.jface.action.Action#run()
     */
    public void run() {
        KeyTreeNode node = getNodeSelection();
        String key = node.getMessageKey();
        String msgHead = MessagesEditorPlugin.getString("dialog.add.head");
        String msgBody = MessagesEditorPlugin.getString("dialog.add.body");
        InputDialog dialog = new InputDialog(
                getShell(), msgHead, msgBody, key, new IInputValidator() {
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
            messagesBundleGroup.addMessages(inputKey);
        }
    }
    
    
}
