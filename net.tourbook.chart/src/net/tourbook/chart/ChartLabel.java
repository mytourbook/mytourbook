/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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
 * Administrator 10.08.2005
 */

package net.tourbook.chart;

public class ChartLabel extends ChartMarker {

	/**
	 * visual position for the markers in the chart
	 */
	public final static int	VISUAL_VERTICAL_ABOVE_GRAPH				= 0;
	public final static int	VISUAL_VERTICAL_BELOW_GRAPH				= 1;
	public final static int	VISUAL_VERTICAL_TOP_CHART				= 2;
	public final static int	VISUAL_VERTICAL_BOTTOM_CHART			= 3;
	public final static int	VISUAL_HORIZONTAL_ABOVE_GRAPH_LEFT		= 4;
	public final static int	VISUAL_HORIZONTAL_ABOVE_GRAPH_CENTERED	= 5;
	public final static int	VISUAL_HORIZONTAL_ABOVE_GRAPH_RIGHT		= 6;
	public final static int	VISUAL_HORIZONTAL_BELOW_GRAPH_LEFT		= 7;
	public final static int	VISUAL_HORIZONTAL_BELOW_GRAPH_CENTERED	= 8;
	public final static int	VISUAL_HORIZONTAL_BELOW_GRAPH_RIGHT		= 9;
	public final static int	VISUAL_HORIZONTAL_GRAPH_LEFT			= 10;
	public final static int	VISUAL_HORIZONTAL_GRAPH_RIGHT			= 11;

	/**
	 * marker was created in the device
	 */
	public final static int	MARKER_TYPE_DEVICE						= 1;

	/**
	 * marker was created in the tourbook application
	 */
	public final static int	MARKER_TYPE_CUSTOM						= 2;

	public static final int	VISIBLE_TYPE_DEFAULT					= 0;
	public static final int	VISIBLE_TYPE_TYPE_NEW					= 10;
	public static final int	VISIBLE_TYPE_TYPE_EDIT					= 20;

	public String			markerLabel								= "";	//$NON-NLS-1$

	/**
	 * visual position in the chart
	 */
	public int				visualPosition;

	/**
	 * marker type, this can be <code>TourMarker.MARKER_TYPE_DEVICE</code> or
	 * <code>TourMarker.MARKER_TYPE_CUSTOM</code>
	 */
	public int				type;

	public int				labelXOffset;
	public int				labelYOffset;

	public int				visualType;

	public ChartLabel() {}
}
