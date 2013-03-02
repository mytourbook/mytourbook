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

import gov.nasa.worldwind.layers.Layer;

public class TVIMap3Layer extends TVIMap3Item {

	String	id;

	Layer	wwLayer;

	boolean	isEnabled;
	boolean	isDefaultLayer;

	int		defaultPosition;

	public TVIMap3Layer(final Layer wwLayer, final String uiLayerName) {

		this.wwLayer = wwLayer;
		this.name = uiLayerName;
	}

	@Override
	protected void fetchChildren() {
		// default layer has no children
	}

	@Override
	public boolean hasChildren() {
		return false;
	}

}
