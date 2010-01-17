/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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

import org.eclipse.osgi.util.NLS;

import de.byteholder.gpx.GeoPosition;

public class MapInfoManager {

	private static MapInfoManager	fInstance;

	private int						fMapPropertyZoom	= -1;
	private GeoPosition				fMapPropertyCenter;

	private NumberFormat			fNf					= NumberFormat.getNumberInstance();

	private MapInfoControl			fInfoWidget;
	{
		fNf.setMinimumFractionDigits(6);
		fNf.setMaximumFractionDigits(6);
	}

	public static MapInfoManager getInstance() {

		if (fInstance == null) {
			fInstance = new MapInfoManager();
		}

		return fInstance;
	}

	public void resetInfo() {
		fMapPropertyCenter = null;
		updateUI();
	}

	void setInfoWidget(final MapInfoControl infoWidget) {
		fInfoWidget = infoWidget;
		updateUI();
	}

	public void setMapCenter(final GeoPosition mapCenter) {
		fMapPropertyCenter = mapCenter;
		updateUI();
	}

	public void setZoom(final int zoom) {
		fMapPropertyZoom = zoom;
		updateUI();
	}

	private void updateUI() {

		// check widget
		if (fInfoWidget == null || fInfoWidget.isDisposed()) {
			return;
		}

		// check data 
		if (fMapPropertyCenter == null || fMapPropertyZoom == -1) {
			fInfoWidget.setText(Messages.statusLine_mapInfo_defaultText);
			return;
		}

		double lon = fMapPropertyCenter.longitude % 360;
		lon = lon > 180 ? //
				lon - 360
				: lon < -180 ? //
						lon + 360
						: lon;

		fInfoWidget.setText(NLS.bind(Messages.statusLine_mapInfo_data, new Object[] {
				fNf.format(fMapPropertyCenter.latitude),
				fNf.format(lon),
				Integer.toString(fMapPropertyZoom + 1) }));
	}

}
