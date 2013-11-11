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
package net.tourbook.map3.view;

import java.util.ArrayList;

import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;

public class TVIMap3Category extends TVIMap3Item {

	private String					id;

	private ArrayList<TVIMap3Layer>	_checkStateNotSet	= new ArrayList<TVIMap3Layer>();

	public TVIMap3Category(final String id, final String name) {

		this.id = id;
		this.name = name;
	}

	@Override
	protected void fetchChildren() {}

	public String getId() {
		return id;
	}

	void setCheckState() {

		if (_checkStateNotSet.size() == 0) {
			// nothing to do
			return;
		}

		ContainerCheckedTreeViewer propViewer = null;

		final Map3LayerView propView = Map3Manager.getMap3LayerView();
		if (propView != null) {

			propViewer = propView.getPropertiesViewer();

			// set check state in the viewer
			for (final TVIMap3Layer layerItem : _checkStateNotSet) {
				propViewer.setChecked(layerItem, layerItem.isLayerVisible);
			}

			// reset
			_checkStateNotSet.clear();
		}
	}

	@Override
	public String toString() {
		return String.format("\nTVIMap3Category\n   id=%s\n", id); //$NON-NLS-1$
	}

}
