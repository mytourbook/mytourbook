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
import org.eclipse.babel.editor.util.UIUtils;
import org.eclipse.jface.action.Action;

/**
 * @author Pascal Essiembre
 *
 */
public class NewLocaleAction extends Action {

    private MessagesEditor editor;
    
    /**
     * 
     */
    public NewLocaleAction() {
        super("New &Locale...");
//        setText();
        setToolTipText("Add a new locale to the resource bundle.");
        setImageDescriptor(UIUtils.getImageDescriptor(UIUtils.IMAGE_NEW_PROPERTIES_FILE));
        
        
        
    }

    //TODO RBEditor hold such an action registry.  Then move this method to constructor
    public void setEditor(MessagesEditor editor) {
        this.editor = editor;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.action.Action#run()
     */
    public void run() {
    }
    
    
}
