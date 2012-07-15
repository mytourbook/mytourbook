/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
package net.tourbook.mapping;

import java.text.NumberFormat;

import net.tourbook.common.map.GeoPosition;

import org.eclipse.osgi.util.NLS;


public class MapInfoManager {

	private static MapInfoManager	_instance;

	private int						_mapZoomLevel	= -1;
	private GeoPosition				_mapMousePosition;

	private final NumberFormat		_nf				= NumberFormat.getNumberInstance();

	private MapInfoControl			_infoWidget;

	{
		_nf.setMinimumFractionDigits(6);
		_nf.setMaximumFractionDigits(6);
	}

	public static MapInfoManager getInstance() {

		if (_instance == null) {
			_instance = new MapInfoManager();
		}

		return _instance;
	}

	public void resetInfo() {
		_mapMousePosition = null;
		updateUI();
	}

	void setInfoWidget(final MapInfoControl infoWidget) {
		_infoWidget = infoWidget;
		updateUI();
	}

	public void setMousePosition(final GeoPosition mousePosition) {
		_mapMousePosition = mousePosition;
		updateUI();
	}

	public void setZoom(final int zoom) {
		_mapZoomLevel = zoom;
		updateUI();
	}

	private void updateUI() {

		// check widget
		if ((_infoWidget == null) || _infoWidget.isDisposed()) {
			return;
		}

		// check data
		if ((_mapMousePosition == null) || (_mapZoomLevel == -1)) {
			_infoWidget.setText(Messages.statusLine_mapInfo_defaultText);
			return;
		}

		double lon = _mapMousePosition.longitude % 360;
		lon = lon > 180 ? //
				lon - 360
				: lon < -180 ? //
						lon + 360
						: lon;

		_infoWidget.setText(NLS.bind(Messages.statusLine_mapInfo_data, new Object[] {
				_nf.format(_mapMousePosition.latitude),
				_nf.format(lon),
				Integer.toString(_mapZoomLevel + 1) }));
	}

}
