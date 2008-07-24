/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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

package net.tourbook.ui.views;

import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;

public class ActionOpenView extends Action {

	private final IWorkbenchWindow	window;
	private final String			viewId;

	/**
	 * @param window
	 * @param label
	 * @param toolTip
	 * @param viewId
	 * @param cmdId
	 * @param image
	 */
	public ActionOpenView(	final IWorkbenchWindow window,
							final String label,
							final String toolTip,
							final String viewId,
							final String cmdId,
							final String image) {

		this.window = window;
		this.viewId = viewId;

		setText(label);
		setToolTipText(toolTip);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(image));

		// The id is used to refer to the action in a menu or toolbar
		setId(cmdId);

		// Associate the action with a pre-defined command, to allow key
		// bindings.
//		setActionDefinitionId(cmdId);

	}

	@Override
	public void run() {

		if (window != null) {
			try {
				window.getActivePage().showView(viewId, null, IWorkbenchPage.VIEW_ACTIVATE);
			} catch (final PartInitException e) {
				MessageDialog.openError(window.getShell(), "Error", "Error opening view:" //$NON-NLS-1$ //$NON-NLS-2$
						+ e.getMessage());
			}
		}
	}
}
