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
package net.tourbook.chart;

import org.eclipse.swt.graphics.GC;

public class ChartMarkerOverlay implements CustomOverlay {

	private ChartMarkerLayer	_markerLayer;
	private ChartLabel			_hoveredMarker;

	@Override
	public boolean draw(final GC gc) {

		if (_markerLayer == null || _hoveredMarker == null) {
			return false;
		}

		_markerLayer.drawHoveredMarker(gc);

		return true;
	}

	/**
	 * @return Returns hovered marker or <code>null</code> when a marker is not hovered.
	 */
	public ChartLabel getHoveredMarker() {

		return _hoveredMarker;
	}

	public void hideHoveredMarker() {

		_hoveredMarker = null;

		if (_markerLayer != null) {
			_markerLayer.hideHoveredMarker();
		}
	}

	@Override
	public void onMouseMove(final ChartMouseEvent mouseEvent) {

		// reset ALLWAYS
		_hoveredMarker = null;

		if (_markerLayer == null) {

			mouseEvent.isWorked = false;
			return;
		}

		_hoveredMarker = _markerLayer.getHoveredMarker(mouseEvent);

		final boolean isMarkerHovered = _hoveredMarker != null;

		mouseEvent.isWorked = isMarkerHovered;
	}

	public void setMarkerLayer(final ChartMarkerLayer markerLayer) {
		_markerLayer = markerLayer;
	}

}
