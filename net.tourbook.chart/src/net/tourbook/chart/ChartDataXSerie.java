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
/**
 * Author: Wolfgang Schramm Created: 21.06.2005
 */

package net.tourbook.chart;

/**
 * Contains the data values for the x-axis
 */
public class ChartDataXSerie extends ChartDataSerie {

	/**
	 * start value for the serie data, this is use to set the start point for time data to the
	 * starting time
	 */
	private int	startValue				= 0;

	/**
	 * index in the x-data at which the graph is painted in the marker color
	 */
	private int	fSynchMarkerStartIndex	= -1;

	/**
	 * index in the x-data at which the graph is stoped to painted in the marker color
	 */
	private int	fSynchMarkerEndIndex	= -1;

	public ChartDataXSerie(int values[]) {
		setMinMaxValues(new int[][] { values });
	}

	/**
	 * @return Returns the startValue.
	 */
	public int getStartValue() {
		return startValue;
	}

	/**
	 * @param startValue
	 *        The startValue to set.
	 */
	public void setStartValue(int startValue) {
		this.startValue = startValue;
	}

	/**
	 * set the start/end value index for the marker which is displayed in a different color
	 * 
	 * @param startIndex
	 * @param endIndex
	 */
	public void setSynchMarkerValueIndex(int startIndex, int endIndex) {
		fSynchMarkerStartIndex = startIndex;
		fSynchMarkerEndIndex = endIndex;
	}

	/**
	 * @return Returns the xMarkerEndIndex.
	 */
	public int getSynchMarkerEndIndex() {
		return fSynchMarkerEndIndex;
	}

	/**
	 * @return Returns the xMarkerStartIndex.
	 */
	public int getSynchMarkerStartIndex() {
		return fSynchMarkerStartIndex;
	}

	@Override
	void setMinMaxValues(int[][] lowValues, int[][] highValues) {

	}
}
