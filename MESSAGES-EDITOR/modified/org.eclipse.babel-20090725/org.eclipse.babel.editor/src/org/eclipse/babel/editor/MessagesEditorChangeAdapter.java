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
package org.eclipse.babel.editor;

import org.eclipse.babel.core.message.tree.IKeyTreeModel;

/**
 * @author Pascal Essiembre
 *
 */
public class MessagesEditorChangeAdapter implements IMessagesEditorChangeListener {

    /**
     * 
     */
    public MessagesEditorChangeAdapter() {
        super();
    }

    /**
     * @see org.eclipse.babel.editor.IMessagesEditorChangeListener#keyTreeVisibleChanged(boolean)
     */
    public void keyTreeVisibleChanged(boolean visible) {
        // do nothing
    }
    /**
     * @see org.eclipse.babel.editor.IMessagesEditorChangeListener#keyTreeVisibleChanged(boolean)
     */
    public void showOnlyUnusedAndMissingChanged(int showFilterFlag) {
        // do nothing
    }

    /**
     * @see org.eclipse.babel.editor.IMessagesEditorChangeListener#selectedKeyChanged(java.lang.String, java.lang.String)
     */
    public void selectedKeyChanged(String oldKey, String newKey) {
        // do nothing
    }

    /* (non-Javadoc)
     * @see org.eclilpse.babel.editor.editor.IMessagesEditorChangeListener#keyTreeModelChanged(org.eclipse.babel.core.message.tree.IKeyTreeModel, org.eclipse.babel.core.message.tree.IKeyTreeModel)
     */
    public void keyTreeModelChanged(IKeyTreeModel oldModel, IKeyTreeModel newModel) {
        // do nothing
    }
    
    /**
     * @see org.eclipse.babel.editor.IMessagesEditorChangeListener#editorDisposed()
     */
    public void editorDisposed() {
        // do nothing
    }
}
