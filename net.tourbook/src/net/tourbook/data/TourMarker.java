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
package net.tourbook.data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import net.tourbook.Messages;
import net.tourbook.chart.ChartMarker;

@Entity
public class TourMarker implements Cloneable {

	/**
	 * visual position for markers, they must correspond to the position in {@link ChartMarker}
	 */
	@Transient
	public static final String[]	visualPositionLabels	= new String[] {
			Messages.Tour_Marker_Position_vertical_above, // 0
			Messages.Tour_Marker_Position_vertical_below, // 1
			Messages.Tour_Marker_Position_vertical_chart_top, // 2
			Messages.Tour_Marker_Position_vertical_chart_bottom, // 3
			Messages.Tour_Marker_Position_horizontal_above_left, // 4
			Messages.Tour_Marker_Position_horizontal_above_centered, // 5
			Messages.Tour_Marker_Position_horizontal_above_right, // 6
			Messages.Tour_Marker_Position_horizontal_below_left, // 7
			Messages.Tour_Marker_Position_horizontal_below_centered, // 8
			Messages.Tour_Marker_Position_horizontal_below_right, // 9
			Messages.Tour_Marker_Position_horizontal_left, // 10
			Messages.Tour_Marker_Position_horizontal_right, // 11
															};

	/**
	 * Unique id for the {@link TourMarker} entity
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long					markerId;

	@ManyToOne(optional = false)
	private TourData				tourData;

	/**
	 * Contains the marker type which is defined in {@link ChartMarker} like
	 * {@link ChartMarker#MARKER_TYPE_DEVICE}
	 */
	private int						type;

	private int						time;

	/**
	 * distance in metric system
	 */
	private int						distance;

	private int						visualPosition;

	private int						labelXOffset;

	private int						labelYOffset;
	/**
	 * markerType contains the type of the marker, this can be: crossing, hotel, view point
	 */
	private long					markerType;

	/**
	 * position of this marker in the data serie
	 */
	private int						serieIndex;

	private String					label					= "";	//$NON-NLS-1$

	private String					category				= "";	//$NON-NLS-1$
	/**
	 * visibleType is used to show the marker with different visible effects (color)
	 */
	@Transient
	private int						visibleType;

	public TourMarker() {}

	public TourMarker(final TourData tourData, final int markerType) {
		this.tourData = tourData;
		this.type = markerType;
	}

	public TourMarker(final TourMarker tourMarker) {

		category = new String(tourMarker.category);
		label = new String(tourMarker.label);

		distance = tourMarker.distance;
		labelXOffset = tourMarker.labelXOffset;
		labelYOffset = tourMarker.labelYOffset;
		markerId = tourMarker.markerId;
		markerType = tourMarker.markerType;
		serieIndex = tourMarker.serieIndex;
		time = tourMarker.time;
		type = tourMarker.type;
		visualPosition = tourMarker.visualPosition;

		tourData = tourMarker.tourData;
	}

	@Override
	public TourMarker clone() {
		return new TourMarker(this);
	}

	/**
	 * compares two markers
	 * 
	 * @param comparedMarker
	 * @param ignoreType
	 *            set <code>true</code> to not compare the type field
	 * @return Returns true when the content of the markers are equal
	 */
	public boolean compareTo(final TourMarker comparedMarker, final boolean ignoreType) {

		if (category.compareTo(comparedMarker.category) != 0) {
			return false;
		} else if (label.compareTo(comparedMarker.label) != 0) {
			return false;
		} else if (distance != comparedMarker.distance) {
			return false;
		} else if (labelXOffset != comparedMarker.labelXOffset) {
			return false;
		} else if (labelYOffset != comparedMarker.labelYOffset) {
			return false;
		} else if (markerId != comparedMarker.markerId) {
			return false;
		} else if (markerType != comparedMarker.markerType) {
			return false;
		} else if (serieIndex != comparedMarker.serieIndex) {
			return false;
		} else if (time != comparedMarker.time) {
			return false;
		} else if (ignoreType == false && type != comparedMarker.type) {
			return false;
		} else if (visualPosition != comparedMarker.visualPosition) {
			return false;
		} else if (tourData != comparedMarker.tourData) {
			return false;
		}

		return true;
	}

	/**
	 * !!!!!!!!!!!!!!!!!<br>
	 * serieIndex is not used for equals or hashcode because this is modified when markers are
	 * deleted<br>
	 * !!!!!!!!!!!!!!!!!<br>
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof TourMarker)) {
			return false;
		}
		final TourMarker other = (TourMarker) obj;
		if (markerId != other.markerId) {
			return false;
		}
		return true;
	}

	public String getCategory() {
		return category;
	}

	/**
	 * @return Returns the distance of the marker in metric system
	 */
	public int getDistance() {
		return distance;
	}

	public String getLabel() {
		return label;
	}

	public int getLabelXOffset() {
		return labelXOffset;
	}

	public int getLabelYOffset() {
		return labelYOffset;
	}

	public long getMarkerId() {
		return markerId;
	}

	public int getSerieIndex() {
		return serieIndex;
	}

	public int getTime() {
		return time;
	}

	public TourData getTourData() {
		return tourData;
	}

	public int getType() {
		return type;
	}

	public int getVisibleType() {
		return visibleType;
	}

	public int getVisualPosition() {
		return visualPosition;
	}

	/**
	 * !!!!!!!!!!!!!!!!!<br>
	 * serieIndex is not used for equals or hashcode because this is modified when markers are
	 * deleted<br>
	 * !!!!!!!!!!!!!!!!!<br>
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (markerId ^ (markerId >>> 32));
		return result;
	}

	/**
	 * restore marker data from a marker backup
	 * 
	 * @param backupMarker
	 */
	public void restoreMarkerFromBackup(final TourMarker backupMarker) {

		category = backupMarker.category;
		label = backupMarker.label;

		distance = backupMarker.distance;
		labelXOffset = backupMarker.labelXOffset;
		labelYOffset = backupMarker.labelYOffset;
		markerId = backupMarker.markerId;
		markerType = backupMarker.markerType;
		serieIndex = backupMarker.serieIndex;
		time = backupMarker.time;
		type = backupMarker.type;
		visualPosition = backupMarker.visualPosition;

		tourData = backupMarker.tourData;
	}

	public void setCategory(final String category) {
		this.category = category;
	}

	/**
	 * Sets the distance with the metric system
	 * 
	 * @param distance
	 */
	public void setDistance(final int distance) {
		this.distance = distance;
	}

	public void setLabel(final String label) {
		this.label = label;
	}

	public void setLabelXOffset(final int labelXOffset) {
		this.labelXOffset = labelXOffset;
	}

	public void setLabelYOffset(final int labelYOffset) {
		this.labelYOffset = labelYOffset;
	}

	/**
	 * copies the current marker into a backup marker
	 * 
	 * @param backupMarker
	 */
	public void setMarkerBackup(final TourMarker backupMarker) {

		backupMarker.category = new String(category);
		backupMarker.label = new String(label);

		backupMarker.distance = distance;
		backupMarker.labelXOffset = labelXOffset;
		backupMarker.labelYOffset = labelYOffset;
		backupMarker.markerId = markerId;
		backupMarker.markerType = markerType;
		backupMarker.serieIndex = serieIndex;
		backupMarker.time = time;
		backupMarker.type = type;
		backupMarker.visualPosition = visualPosition;

		backupMarker.tourData = tourData;
	}

	public void setSerieIndex(final int serieIndex) {
		this.serieIndex = serieIndex;
	}

	public void setTime(final int time) {
		this.time = time;
	}

	public void setVisibleType(final int visibleType) {
		this.visibleType = visibleType;
	}

	public void setVisualPosition(final int visualPosition) {
		this.visualPosition = visualPosition;
	}

	@Override
	public String toString() {
		return new StringBuilder().append(TourMarker.class.getSimpleName())
				.append(" id:")
				.append(markerId)
				.append(" distance:")
				.append(distance)
				.append(" time:")
				.append(time)
				.append(" serieIndex:")
				.append(serieIndex)
				.toString();
	}

}
