/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.action;

import net.tourbook.common.CommonActivator;
import net.tourbook.common.Messages;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ITourViewer;

import org.eclipse.jface.action.Action;

public class ActionModifyColumns extends Action {

	private ITourViewer	_tourViewer;

	public ActionModifyColumns(final ITourViewer tourViewer) {

		_tourViewer = tourViewer;

		setText(Messages.Action_App_CustomizeColumnsAndProfiles);
		setImageDescriptor(CommonActivator.getImageDescriptor(Messages.Image__CustomizeProfilesColumns));
	}

	@Override
	public void run() {

		final ColumnManager columnManager = _tourViewer.getColumnManager();
		
		if (columnManager != null) {
			columnManager.openColumnDialog();
		}
	}

}
