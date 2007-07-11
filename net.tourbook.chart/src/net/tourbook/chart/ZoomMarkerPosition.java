/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
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

/**
 * The x-marker position is used to synch 2 charts. Chart A sets the
 * zoomMarkerPositionOut which can be read (and set to zoomMarkerPositionIn)
 * from the chart B. Chart B will then be synched to chart A
 */
public class ZoomMarkerPosition {

	/**
	 * width for the marker, this can be smaller or wider than the visible part
	 * of the chart
	 */
	int						devMarkerWidth;

	/**
	 * offset for the marker start position, this value starts at the left
	 * position of the visible graph, this also can be a negative value
	 */
	int						devMarkerOffset;

	ChartYDataMinMaxKeeper	yDataMinMaxKeeper	= new ChartYDataMinMaxKeeper();

	/**
	 * The ZoomMarkerPosition describes the position and width for the x-marker
	 * in the graph
	 * 
	 * @param chartDataModel
	 */
	public ZoomMarkerPosition(int devMarkerWidth, int devMarkerOffset,
			ChartDataModel chartDataModel) {

		this.devMarkerWidth = devMarkerWidth;
		this.devMarkerOffset = devMarkerOffset;

		yDataMinMaxKeeper.saveMinMaxValues(chartDataModel);
	}

	/**
	 * @param newXMarkerPosition
	 * @return Returns <code>true</code> when the newXMarkerPosition has the
	 *         same values as the current object
	 */
	public boolean isEqual(ZoomMarkerPosition newXMarkerPosition) {
		if (devMarkerWidth == newXMarkerPosition.devMarkerWidth
				&& devMarkerOffset == newXMarkerPosition.devMarkerOffset
				&& !yDataMinMaxKeeper.hasMinMaxChanged()) {

			return true;
		}
		return false;
	}

}
