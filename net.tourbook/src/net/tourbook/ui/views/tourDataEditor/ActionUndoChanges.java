/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourDataEditor;

import net.tourbook.Messages;
import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.jface.action.Action;

class ActionUndoChanges extends Action {

	/**
	 * 
	 */
	private final TourDataEditorView	fTourDataEditor;

	public ActionUndoChanges(final TourDataEditorView tourPropertiesView) {

		super(null, AS_PUSH_BUTTON);
		
		fTourDataEditor = tourPropertiesView;

		setText(Messages.app_action_undo_modifications);
		setToolTipText(Messages.app_action_undo_modifications_tooltip);

		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__undo_edit));
		setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__undo_edit_disabled));

		setEnabled(false);
	}

	@Override
	public void run() {
		fTourDataEditor.actionUndoChanges();
	}
}