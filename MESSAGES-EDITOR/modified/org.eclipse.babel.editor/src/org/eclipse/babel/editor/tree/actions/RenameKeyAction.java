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

import org.eclipse.babel.core.message.tree.KeyTreeNode;
import org.eclipse.babel.editor.MessagesEditor;
import org.eclipse.babel.editor.plugin.MessagesEditorPlugin;
import org.eclipse.babel.editor.refactoring.RenameKeyProcessor;
import org.eclipse.babel.editor.refactoring.RenameKeyWizard;
import org.eclipse.babel.editor.util.UIUtils;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;

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

        // Rename single item
		RenameKeyProcessor refactoring = new RenameKeyProcessor(node, getBundleGroup());
		
		RefactoringWizard wizard = new RenameKeyWizard(node, refactoring);
		try {
			RefactoringWizardOpenOperation operation= new RefactoringWizardOpenOperation(wizard);
			operation.run(getShell(), "Introduce Indirection");
		} catch (InterruptedException exception) {
			// Do nothing
		}
    }
}
