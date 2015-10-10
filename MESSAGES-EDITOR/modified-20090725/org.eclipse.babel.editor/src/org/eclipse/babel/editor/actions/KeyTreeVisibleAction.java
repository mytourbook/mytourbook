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
package org.eclipse.babel.editor.actions;

import org.eclipse.babel.editor.MessagesEditor;
import org.eclipse.babel.editor.MessagesEditorChangeAdapter;
import org.eclipse.babel.editor.util.UIUtils;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;

/**
 * @author Pascal Essiembre
 *
 */
public class KeyTreeVisibleAction extends Action {

    private MessagesEditor editor;
    
    /**
     * 
     */
    public KeyTreeVisibleAction() {
        super("Show/Hide Key Tree", IAction.AS_CHECK_BOX);
//        setText();
        setToolTipText("Show/hide the key tree");
        setImageDescriptor(UIUtils.getImageDescriptor(UIUtils.IMAGE_VIEW_LEFT));
    }

    //TODO RBEditor hold such an action registry.  Then move this method to constructor
    public void setEditor(MessagesEditor editor) {
        this.editor = editor;
        editor.addChangeListener(new MessagesEditorChangeAdapter() {
            public void keyTreeVisibleChanged(boolean visible) {
                setChecked(visible);
            }
        });
        setChecked(editor.getI18NPage().isKeyTreeVisible());
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.action.Action#run()
     */
    public void run() {
        editor.getI18NPage().setKeyTreeVisible(
                !editor.getI18NPage().isKeyTreeVisible());
    }
    
    
}
