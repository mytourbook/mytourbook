/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm
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

public interface IChartListener {

	/**
	 * set the value diff which is forced for the x-marker
	 * 
	 * @param valueDiff
	 */
	int getXMarkerValueDiff();

	/**
	 * method is called when the x-marker was move, the parameter contain the
	 * new value index
	 * 
	 * @param movedXMarkerStartValueIndex
	 * @param movedXMarkerEndValueIndex
	 */
	void xMarkerMoved(int movedXMarkerStartValueIndex, int movedXMarkerEndValueIndex);
}
