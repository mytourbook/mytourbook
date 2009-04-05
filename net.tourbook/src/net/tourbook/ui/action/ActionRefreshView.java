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
package net.tourbook.ui.action;

import net.tourbook.Messages;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.util.ITourViewer;

import org.eclipse.jface.action.Action;

public class ActionRefreshView extends Action {

	private ITourViewer	fTourViewer;

	public ActionRefreshView(final ITourViewer tourViewer) {

		super(null, AS_PUSH_BUTTON);

		fTourViewer = tourViewer;

		setToolTipText(Messages.tag_view_action_refresh_view_tooltip);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__refresh));
	}

	@Override
	public void run() {
		fTourViewer.reloadViewer();
	}

}
