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
package net.tourbook.data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import net.tourbook.Messages;

@Entity
public class TourMarker {

	/**
	 * marker was created in the device
	 */
	public final static int			MARKER_TYPE_DEVICE						= 1;

	/**
	 * marker was created in the tourbook application
	 */
	public final static int			MARKER_TYPE_CUSTOM						= 2;

	/*
	 * visual position for the markers in the chart
	 */
	public final static int			VISUAL_VERTICAL_ABOVE_GRAPH				= 0;
	public final static int			VISUAL_VERTICAL_BELOW_GRAPH				= 1;
	public final static int			VISUAL_VERTICAL_TOP_CHART				= 2;
	public final static int			VISUAL_VERTICAL_BOTTOM_CHART			= 3;
	public final static int			VISUAL_HORIZONTAL_ABOVE_GRAPH_LEFT		= 4;
	public final static int			VISUAL_HORIZONTAL_ABOVE_GRAPH_CENTERED	= 5;
	public final static int			VISUAL_HORIZONTAL_ABOVE_GRAPH_RIGHT		= 6;
	public final static int			VISUAL_HORIZONTAL_BELOW_GRAPH_LEFT		= 7;
	public final static int			VISUAL_HORIZONTAL_BELOW_GRAPH_CENTERED	= 8;
	public final static int			VISUAL_HORIZONTAL_BELOW_GRAPH_RIGHT		= 9;

	@Transient
	public static final String[]	visualPositionLabels					= new String[] {
			Messages.TourMarker_Position_vertical_above, // 0
			Messages.TourMarker_Position_vertical_below, // 1
			Messages.TourMarker_Position_vertical_chart_top, // 2
			Messages.TourMarker_Position_vertical_chart_bottom, // 3
			Messages.TourMarker_Position_horizontal_above_left, // 4
			Messages.TourMarker_Position_horizontal_above_centered, // 5
			Messages.TourMarker_Position_horizontal_above_right, // 6
			Messages.TourMarker_Position_horizontal_below_left, // 7
			Messages.TourMarker_Position_horizontal_below_centered, // 8
			Messages.TourMarker_Position_horizontal_below_right // 9
																			};

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long					markerId;

	@ManyToOne(optional = false)
	private TourData				tourData;

	private int						type;

	private int						time;
	private int						distance;

	private int						visualPosition;

	private int						labelXOffset;
	private int						labelYOffset;

	/**
	 * marker type contains the type of the marker, this can be: crossing,
	 * hotel, view point
	 */
	private long					markerType;

	/**
	 * position of this marker in the data serie
	 */
	private int						serieIndex;

	private String					label									= ""; //$NON-NLS-1$
	private String					category								= ""; //$NON-NLS-1$

	public TourMarker() {}

	public TourMarker(TourData tourData, int markerType) {
		this.tourData = tourData;
		this.type = markerType;
	}

	public long getMarkerId() {
		return markerId;
	}

	public TourData getTourData() {
		return tourData;
	}

	public void setTime(int time) {
		this.time = time;
	}

	public int getTime() {
		return time;
	}

	public void setDistance(int distance) {
		this.distance = distance;
	}

	public int getDistance() {
		return distance;
	}

	public void setSerieIndex(int serieIndex) {
		this.serieIndex = serieIndex;
	}

	public int getSerieIndex() {
		return serieIndex;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	public int getType() {
		return type;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getCategory() {
		return category;
	}

	public int getVisualPosition() {
		return visualPosition;
	}

	public void setVisualPosition(int visualPosition) {
		this.visualPosition = visualPosition;
	}

}
