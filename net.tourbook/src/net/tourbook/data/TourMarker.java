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
			"v: a     Vert:  above", // 0
			"v: b     Vert:  below", // 1
			"v: t     Vert:  chart top", // 2
			"v: b     Vert:  chart bottom", // 3
			"h: a-l   Horiz: above  - left", // 4
			"h: a-c  Horiz: above  - centered", // 5
			"h: a-r  Horiz: above  - right", // 6
			"h: b-l   Horiz: below  - left", // 7
			"h: b-c  Horiz: below  - centered", // 8
			"h: b-r  Horiz: below  - right" // 9
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

	private String					label									= "";
	private String					category								= "";

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
