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
package net.tourbook.ui.views.rawData;

import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.jface.action.Action;

public class ActionSourceTourGraph extends Action {

	private final int			fGraphId;

	private DialogMergeTours	fDialogMergeTours;

	/**
	 * Creates an action for a toggle button
	 * 
	 * @param dialogMergeTours
	 * @param graphId
	 * @param label
	 * @param toolTip
	 * @param imageEnabled
	 * @param imageDisabled
	 */
	public ActionSourceTourGraph(	final DialogMergeTours dialogMergeTours,
									final int graphId,
									final String label,
									final String toolTip,
									final String imageEnabled,
									final String imageDisabled) {

		super(label, AS_CHECK_BOX);

		fDialogMergeTours = dialogMergeTours;
		fGraphId = graphId;

		setToolTipText(toolTip);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(imageEnabled));

		if (imageDisabled != null) {
			setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(imageDisabled));
		}
	}

	public int getGraphId() {
		return fGraphId;
	}

	@Override
	public void run() {
		fDialogMergeTours.actionSetSourceTourGraph(fGraphId);
	}

}
