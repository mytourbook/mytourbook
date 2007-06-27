/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
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
package net.tourbook.tour;

import net.tourbook.Messages;
import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.jface.action.Action;

public class ActionUndoTyping extends Action {

	private Tour	fTour;

	public ActionUndoTyping(Tour tour) {

		fTour = tour;

		setText(Messages.Tour_Action_undo_typing);
		setToolTipText(Messages.Tour_Action_undo_typing_tooltip);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image_undo_edit));
		setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image_undo_edit_disabled));

		setEnabled(false);
	}

	public void run() {
		fTour.undoTyping();
	}

}
