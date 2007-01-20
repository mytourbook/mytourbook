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
package net.tourbook.views.tourBook;

import net.tourbook.Messages;
import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.jface.action.Action;

public class ActionShowTourChart extends Action {

	private TourBookView	fViewer;

	public ActionShowTourChart(TourBookView view) {

		super("", AS_CHECK_BOX); //$NON-NLS-1$

		fViewer = view;

		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.TourBook_Image_show_tour_chart));
		setToolTipText(Messages.TourBook_Image_show_tour_chart_tooltip);
	}

	public void run() {
		fViewer.manageDetailVisibility(this);
	}
}
