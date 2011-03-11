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

package org.eclipse.babel.runtime.actions;

import org.eclipse.babel.runtime.Activator;
import org.eclipse.babel.runtime.Messages;
import org.eclipse.babel.runtime.external.TranslatableNLS;
import org.eclipse.babel.runtime.external.ITranslatableSet;
import org.eclipse.babel.runtime.external.ITranslatableText;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/**
 * Our sample action implements workbench action delegate.
 * The action proxy will be created by the workbench and
 * shown in the UI. When the user tries to use the action,
 * this delegate will be created and execution will be 
 * delegated to it.
 * @see IWorkbenchWindowActionDelegate
 */
public class LocalizeAction implements IWorkbenchWindowActionDelegate {
	private IWorkbenchWindow window;
	/**
	 * The constructor.
	 */
	public LocalizeAction() {
	}

	/**
	 * The action has been activated. The argument of the
	 * method represents the 'real' action sitting
	 * in the workbench UI.
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	public void run(IAction action) {
		IWorkbenchPart activePart = window.getActivePage().getActivePart();

		ITranslatableText tabTitle;
		if (activePart instanceof IEditorPart) {
			tabTitle = TranslatableNLS.bind(Messages.LocalizeDialog_TabTitle_EditorPart, activePart.getTitle()); //$NON-NLS-1$
		} else if (activePart instanceof IViewPart) {
			tabTitle = TranslatableNLS.bind(Messages.LocalizeDialog_TabTitle_ViewPart, activePart.getTitle()); //$NON-NLS-1$
		} else {
			tabTitle = TranslatableNLS.bind(Messages.LocalizeDialog_TabTitle_OtherPart, activePart.getTitle()); //$NON-NLS-1$
		}
		
		ITranslatableSet languageSet = (ITranslatableSet)activePart.getAdapter(ITranslatableSet.class);

		Dialog dialog = new LocalizeDialog(window.getShell(), tabTitle, languageSet, Activator.getDefault().getMenuTextSet()); 
		dialog.open();
	}

	/**
	 * Selection in the workbench has been changed. We 
	 * can change the state of the 'real' action here
	 * if we want, but this can only happen after 
	 * the delegate has been created.
	 * @see IWorkbenchWindowActionDelegate#selectionChanged
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}

	/**
	 * We can use this method to dispose of any system
	 * resources we previously allocated.
	 * @see IWorkbenchWindowActionDelegate#dispose
	 */
	public void dispose() {
	}

	/**
	 * We will cache window object in order to
	 * be able to provide parent shell for the message dialog.
	 * @see IWorkbenchWindowActionDelegate#init
	 */
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}
}