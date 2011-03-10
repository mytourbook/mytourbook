/*******************************************************************************
 * Copyright (c) 2008 Nigel Westbury and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Nigel Westbury - initial API and implementation
 *******************************************************************************/

package org.eclipse.babel.runtime.handlers;

import org.eclipse.babel.runtime.Activator;
import org.eclipse.babel.runtime.Messages;
import org.eclipse.babel.runtime.actions.LocalizeDialog;
import org.eclipse.babel.runtime.external.TranslatableNLS;
import org.eclipse.babel.runtime.external.ITranslatableSet;
import org.eclipse.babel.runtime.external.ITranslatableText;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Command to open a dialog box that allows the user to translate text.
 */
public class LocalizeHandler extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		IWorkbenchPart activePart = window.getActivePage().getActivePart();

		ITranslatableText tabTitle;
		ITranslatableSet languageSet;
		if (activePart != null) {
			if (activePart instanceof IEditorPart) {
				tabTitle = TranslatableNLS.bind(Messages.LocalizeDialog_TabTitle_EditorPart, activePart.getTitle()); //$NON-NLS-1$
			} else if (activePart instanceof IViewPart) {
				tabTitle = TranslatableNLS.bind(Messages.LocalizeDialog_TabTitle_ViewPart, activePart.getTitle()); //$NON-NLS-1$
			} else {
				// When can this happen?
				tabTitle = TranslatableNLS.bind(Messages.LocalizeDialog_TabTitle_OtherPart, activePart.getTitle()); //$NON-NLS-1$
			}
			languageSet = (ITranslatableSet)activePart.getAdapter(ITranslatableSet.class);
		} else {
			// No view or editor is active
			tabTitle = null;
			languageSet = null;
		}
		
		Dialog dialog = new LocalizeDialog(window.getShell(), tabTitle, languageSet, Activator.getDefault().getMenuTextSet()); 
		dialog.open();
		
		return null;
	}
}
