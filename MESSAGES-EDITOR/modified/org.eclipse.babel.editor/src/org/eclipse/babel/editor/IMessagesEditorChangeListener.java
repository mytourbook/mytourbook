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

import org.eclipse.babel.core.message.tree.AbstractKeyTreeModel;



/**
 * @author Pascal Essiembre
 *
 */
public interface IMessagesEditorChangeListener {

    public static int SHOW_ALL = 0;
    public static int SHOW_ONLY_MISSING_AND_UNUSED = 1;
    public static int SHOW_ONLY_MISSING = 2;
    public static int SHOW_ONLY_UNUSED = 3;
	
    void keyTreeVisibleChanged(boolean visible);
    
    void showOnlyUnusedAndMissingChanged(int showFlag);
    
    void selectedKeyChanged(String oldKey, String newKey);
    
    void keyTreeModelChanged(AbstractKeyTreeModel oldModel, AbstractKeyTreeModel newModel);
    
    void editorDisposed();
}
