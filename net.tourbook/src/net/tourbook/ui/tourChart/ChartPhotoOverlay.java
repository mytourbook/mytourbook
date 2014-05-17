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
package net.tourbook.ui.tourChart;

import net.tourbook.chart.ChartMouseEvent;
import net.tourbook.chart.CustomOverlay;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;

public class ChartPhotoOverlay implements CustomOverlay {

	private ChartLayerPhoto	_photoLayer;

	private PhotoPaintGroup	_hoveredPhotoGroup;
	private PhotoCategory	_hoveredPhotoCategory;

	@Override
	public boolean draw(final GC gcOverlay) {

		if (_photoLayer == null || _hoveredPhotoGroup == null) {
			return false;
		}

		final Device display = gcOverlay.getDevice();

		gcOverlay.setForeground(display.getSystemColor(SWT.COLOR_WHITE));
		gcOverlay.setBackground(_photoLayer.getPhotoGroupBackgroundColor(_hoveredPhotoCategory.photoType, true));

		_photoLayer.drawPhotoAndGroup(gcOverlay, _hoveredPhotoGroup, _hoveredPhotoCategory);

		return true;
	}

	@Override
	public void onMouseMove(final ChartMouseEvent mouseEvent) {

		// reset ALLWAYS
		_hoveredPhotoGroup = null;

		if (_photoLayer == null) {
			mouseEvent.isWorked = false;
			return;
		}

		_hoveredPhotoCategory = _photoLayer.getHoveredCategory(
				mouseEvent.eventTime,
				mouseEvent.devXMouse,
				mouseEvent.devYMouse);

		_hoveredPhotoGroup = _photoLayer.getHoveredGroup();

		mouseEvent.isWorked = _hoveredPhotoGroup != null;
	}

	public void setPhotoLayer(final ChartLayerPhoto photoLayer) {
		_photoLayer = photoLayer;
	}

}
