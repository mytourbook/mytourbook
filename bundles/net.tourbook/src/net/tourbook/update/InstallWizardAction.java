/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/
package net.tourbook.update;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

public class InstallWizardAction implements IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow	window;

	public InstallWizardAction() {
	// do nothing
	}

	@Override
	public void dispose() {
	// do nothing
	}

	@Override
	public void init(final IWorkbenchWindow window) {
		this.window = window;
	}

	private void openInstaller(final IWorkbenchWindow window) {
		BusyIndicator.showWhile(window.getShell().getDisplay(), new Runnable() {
			@Override
			public void run() {
//				UpdateManagerUI.openInstaller(window.getShell());
			}
		});
	}

	public void run() {
		openInstaller(PlatformUI.getWorkbench().getActiveWorkbenchWindow());
	}

	@Override
	public void run(final IAction action) {
		openInstaller(window);
	}

	@Override
	public void selectionChanged(final IAction action, final ISelection selection) {
	// do nothing
	}

}
