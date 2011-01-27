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
package org.eclipse.babel.editor.widgets;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;

/**
 * A button that when clicked, will execute the given action.
 * @author Pascal Essiembre (pascal@essiembre.com)
 */
public class ActionButton extends Composite {

    /**
     * @param parent
     * @param action
     */
    public ActionButton(Composite parent, IAction action) {
        super(parent, SWT.NONE);
        RowLayout layout = new RowLayout();
        setLayout(layout);
        layout.marginBottom = 0;
        layout.marginLeft = 0;
        layout.marginRight = 0;
        layout.marginTop = 0;

        ToolBarManager toolBarMgr = new ToolBarManager(SWT.FLAT);
        toolBarMgr.createControl(this);
        toolBarMgr.add(action);
        toolBarMgr.update(true);
    }
}
