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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;

public class ChartPhotoOverlay implements CustomOverlay {

	private ChartLayerPhoto	_photoLayer;
	private PhotoGroup		_hoveredPhotoGroup;

	private Color			_bgColor;
	private int				_devXMouse;

	@Override
	public boolean draw(final GC gcOverlay) {

		if (_photoLayer == null || _hoveredPhotoGroup == null) {
			return false;
		}

		final Device display = gcOverlay.getDevice();
		gcOverlay.setForeground(display.getSystemColor(SWT.COLOR_WHITE));
		gcOverlay.setBackground(_bgColor);

		_photoLayer.drawPhotoAndGroup(gcOverlay, _hoveredPhotoGroup);

		return true;
	}

	PhotoGroup getHoveredPhotoGroup() {

		// find photo group which is hovered with the mouse
		for (final PhotoGroup photoGroup : _photoLayer.getPhotoGroups()) {

			if (_devXMouse >= photoGroup.hGridStart && _devXMouse <= photoGroup.hGridEnd) {
				return photoGroup;
			}
		}

		return null;
	}

	@Override
	public boolean onMouseMove(final int devXMouse, final int devYMouse) {

		// reset ALLWAYS
		_hoveredPhotoGroup = null;

		if (_photoLayer == null) {
			return false;
		}

		_devXMouse = devXMouse;

		_hoveredPhotoGroup = getHoveredPhotoGroup();

		return _hoveredPhotoGroup != null;
	}

	public void setBackgroundColor(final Color bgColor) {
		_bgColor = bgColor;
	}

	public void setPhotoLayer(final ChartLayerPhoto photoLayer) {
		_photoLayer = photoLayer;
	}

}
