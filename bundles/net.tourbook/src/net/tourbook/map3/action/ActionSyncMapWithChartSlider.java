/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
package net.tourbook.map3.action;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.map2.Messages;
import net.tourbook.map3.view.Map3View;

import org.eclipse.jface.action.Action;

public class ActionSyncMapWithChartSlider extends Action {

	private Map3View	_map3View;

	public ActionSyncMapWithChartSlider(final Map3View mapView) {

		super(null, AS_CHECK_BOX);

		_map3View = mapView;

		setToolTipText(Messages.map_action_synch_with_slider);

		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.image_action_synch_with_slider));
		setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.image_action_synch_with_slider_disabled));
	}

	@Override
	public void run() {
		_map3View.actionSynch_WithChartSlider();
	}

}
