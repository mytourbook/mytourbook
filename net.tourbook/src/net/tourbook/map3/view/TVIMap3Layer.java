/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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

import gov.nasa.worldwind.layers.Layer;
import net.tourbook.common.tooltip.IToolProvider;

public class TVIMap3Layer extends TVIMap3Item {

	private String				id;

	public Layer				wwLayer;

	public boolean				isLayerVisible;
	boolean						isDefaultLayer;

	int							defaultPosition;

	private ICheckStateListener	_checkStateListener;

	public IToolProvider		toolProvider;

	public TVIMap3Layer(final String id, final Layer wwLayer, final String uiLayerName) {

		this.id = id;
		this.wwLayer = wwLayer;
		this.name = uiLayerName;
	}

	void addCheckStateListener(final ICheckStateListener checkStateListener) {
		_checkStateListener = checkStateListener;
	}

	@Override
	protected void fetchChildren() {
		// default layer has no children
	}

	void fireCheckStateListener() {

		if (_checkStateListener != null) {
			_checkStateListener.onSetCheckState(this);
		}
	}

	public String getId() {
		return id;
	}

	@Override
	public boolean hasChildren() {
		return false;
	}

	@Override
	public String toString() {
		return String
				.format(
						"\nTVIMap3Layer\n   id=%s\n   wwLayer=%s\n   isLayerVisible=%s\n   isDefaultLayer=%s\n   defaultPosition=%s\n", //$NON-NLS-1$
						id,
						wwLayer,
						isLayerVisible,
						isDefaultLayer,
						defaultPosition);
	}

}
