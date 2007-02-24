/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm
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
package net.tourbook.views.rawData;

import net.tourbook.Messages;
import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.jface.action.Action;

public class ActionShowViewDetails extends Action {

	private RawDataView	fViewer;

	public ActionShowViewDetails(RawDataView view) {

		super(null, AS_CHECK_BOX);

		fViewer = view;

		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image_show_view_detail));
		setToolTipText(Messages.RawData_Action_show_tour_chart_tooltip);
	}

	public void run() {
		fViewer.showViewerDetails(isChecked());
	}
}
