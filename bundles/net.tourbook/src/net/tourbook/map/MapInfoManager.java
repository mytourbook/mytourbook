/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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
package net.tourbook.map;

import java.text.NumberFormat;

import net.tourbook.map2.Messages;

import org.eclipse.osgi.util.NLS;

public class MapInfoManager {

	private static MapInfoManager	_instance;

	private double					_latitude		= Double.MIN_VALUE;
	private double					_longitude;

	private int						_mapZoomLevel	= -1;

	private final NumberFormat		_nf6			= NumberFormat.getNumberInstance();
	{
		_nf6.setMinimumFractionDigits(6);
		_nf6.setMaximumFractionDigits(6);
	}

	private MapInfoControl _infoWidget;

	public static MapInfoManager getInstance() {

		if (_instance == null) {
			_instance = new MapInfoManager();
		}

		return _instance;
	}

	public void resetInfo() {

		_latitude = Double.MIN_VALUE;

		updateUI();
	}

	void setInfoWidget(final MapInfoControl infoWidget) {

		_infoWidget = infoWidget;

		updateUI();
	}

	public void setMapPosition(final double latitude, final double longitude, final int zoomLevel) {

		_latitude = latitude;
		_longitude = longitude;
		_mapZoomLevel = zoomLevel;

		updateUI();
	}

	private void updateUI() {

		// check widget
		if ((_infoWidget == null) || _infoWidget.isDisposed()) {
			return;
		}

		// check data
		if ((_latitude == Double.MIN_VALUE) || (_mapZoomLevel == -1)) {

			_infoWidget.setText(Messages.statusLine_mapInfo_defaultText);

		} else {

			double lon = _longitude % 360;
			lon = lon > 180 ? //
					lon - 360
					: lon < -180 ? //
							lon + 360
							: lon;

			_infoWidget.setText(
					NLS.bind(
							Messages.statusLine_mapInfo_data,
							new Object[] {
									_nf6.format(_latitude),
									_nf6.format(lon),
									Integer.toString(_mapZoomLevel + 1) }));
		}
	}

}
