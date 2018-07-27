/*******************************************************************************
 * Copyright (C) 2018 Wolfgang Schramm and Contributors
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

import java.util.Map;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.menu.MItem;

import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ITourViewer;

public class ActionModifyColumnsE4 {

	public ActionModifyColumnsE4() {

	}

//	public ActionModifyColumnsE4(final ITourViewer tourViewer) {
//
//		_tourViewer = tourViewer;
//
////		setText(Messages.Action_App_CustomizeColumnsAndProfiles);
////		setImageDescriptor(CommonActivator.getImageDescriptor(Messages.Image__CustomizeProfilesColumns));
//	}

	@Execute
	public static void run(final MItem menuItem) {

		final Map<String, Object> transientData = menuItem.getTransientData();
		final Object tourViewerRaw = transientData.get(ITourViewer.class.getName());
		if (tourViewerRaw instanceof ITourViewer) {

			final ITourViewer tourViewer = (ITourViewer) tourViewerRaw;

			final ColumnManager columnManager = tourViewer.getColumnManager();

			if (columnManager != null) {
				columnManager.openColumnDialog();
			}
		}
	}

}
