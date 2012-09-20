/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.tourChart;

import net.tourbook.chart.CustomOverlay;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;

public class ChartPhotoOverlay implements CustomOverlay {

	private ChartLayerPhoto	_photoLayer;
	private PhotoGroup		_hoveredPhotoGroup;

	@Override
	public void draw(final GC gcOverlay) {

		if (_photoLayer == null || _hoveredPhotoGroup == null) {
			return;
		}

		final Device display = gcOverlay.getDevice();
		gcOverlay.setForeground(display.getSystemColor(SWT.COLOR_WHITE));
		gcOverlay.setBackground(display.getSystemColor(SWT.COLOR_RED));

		_photoLayer.drawGroup(gcOverlay, _hoveredPhotoGroup);
	}

	@Override
	public boolean onMouseMove(final int devXMouse, final int devYMouse) {

		if (_photoLayer == null) {
			return false;
		}

		_hoveredPhotoGroup = null;

		// find photo group which is hovered with the mouse
		for (final PhotoGroup photoGroup : _photoLayer.getPhotoGroups()) {

			if (devXMouse >= photoGroup.hGridStart && devXMouse <= photoGroup.hGridEnd) {

				_hoveredPhotoGroup = photoGroup;

				return true;
			}
		}

		return false;
	}

	public void setPhotoLayer(final ChartLayerPhoto photoLayer) {
		_photoLayer = photoLayer;
	}

}
