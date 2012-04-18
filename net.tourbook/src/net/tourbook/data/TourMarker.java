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
package net.tourbook.data;

import java.io.StringWriter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import net.tourbook.Messages;
import net.tourbook.chart.ChartLabel;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.UI;

import org.eclipse.swt.graphics.Rectangle;

@Entity
@XmlType(name = "TourMarker")
@XmlRootElement(name = "TourMarker")
@XmlAccessorType(XmlAccessType.NONE)
public class TourMarker implements Cloneable, Comparable<Object>, IXmlSerializable {

	public static final int			DB_LENGTH_LABEL			= 255;
	public static final int			DB_LENGTH_CATEGORY		= 100;

	/**
	 * visual position for markers, they must correspond to the position in {@link ChartLabel}
	 */
	public static final String[]	visualPositionLabels	= new String[] {
			Messages.Tour_Marker_Position_vertical_above, // 				0
			Messages.Tour_Marker_Position_vertical_below, //				1
			Messages.Tour_Marker_Position_vertical_chart_top, // 			2
			Messages.Tour_Marker_Position_vertical_chart_bottom, // 		3
			Messages.Tour_Marker_Position_horizontal_above_left, // 		4
			Messages.Tour_Marker_Position_horizontal_above_centered, // 	5
			Messages.Tour_Marker_Position_horizontal_above_right, // 		6
			Messages.Tour_Marker_Position_horizontal_below_left, // 		7
			Messages.Tour_Marker_Position_horizontal_below_centered, // 	8
			Messages.Tour_Marker_Position_horizontal_below_right, // 		9
			Messages.Tour_Marker_Position_horizontal_left, // 				10
			Messages.Tour_Marker_Position_horizontal_right, // 				11
															};

	/**
	 * Unique id for the {@link TourMarker} entity
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long					markerId				= TourDatabase.ENTITY_IS_NOT_SAVED;

	@ManyToOne(optional = false)
	private TourData				tourData;

	/**
	 * Contains the marker type which is defined in {@link ChartLabel} like
	 * {@link ChartLabel#MARKER_TYPE_DEVICE}
	 */
	private int						type;

	/**
	 * Time in seconds relative to the tour start. When value is not available it is set to
	 * <code>-1</code>.
	 */
	@XmlElement
	private int						time					= -1;

	/**
	 * Distance field before db version 20, this field is required for data conversion AND <b>to
	 * load entities</b> !!!
	 */
	private int						distance				= -1;

	/**
	 * Distance in meters in the metric system or <code>-1</code> when the distance is not
	 * available.
	 * <p>
	 * 20 == db version 20
	 */
	@XmlElement
	private float					distance20				= -1;

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
	@XmlAttribute
	private int						serieIndex;

	@XmlElement
	private String					label					= UI.EMPTY_STRING;

	private String					category				= UI.EMPTY_STRING;

	/**
	 * visibleType is used to show the marker with different visible effects (color)
	 */
	@Transient
	private int						_visibleType;

	/**
	 * unique id for manually created markers because the {@link #markerId} is 0 when the marker is
	 * not persisted
	 */
	@Transient
	private long					_createId				= 0;

	/**
	 * Contains <b>width</b> and <b>height</b> of the marker image.
	 * <p>
	 * <b>x</b> and <b>y</b> contain the width and height of the marker banner which is the marker
	 * label text including the border
	 */
	@Transient
	private Rectangle				_markerBounds;

	/**
	 * manually created marker or imported marker create a unique id to identify them, saved marker
	 * are compared with the marker id
	 */
	private static int				_createCounter			= 0;

	public TourMarker() {}

	public TourMarker(final TourData tourData, final int markerType) {

		this.tourData = tourData;
		this.type = markerType;

		_createId = ++_createCounter;
	}

	@Override
	public TourMarker clone() {

		TourMarker newTourMarker = null;

		try {
			newTourMarker = (TourMarker) super.clone();
		} catch (final CloneNotSupportedException e) {
			e.printStackTrace();
		}

		return newTourMarker;
	}

	/**
	 * Creates a clone of another marker but set's new {@link TourData} and set's the state that the
	 * marker is not yet saved
	 * 
	 * @param newTourData
	 * @return
	 */
	public TourMarker clone(final TourData newTourData) {

		final TourMarker newTourMarker = clone();

		newTourMarker.tourData = newTourData;

		newTourMarker.markerId = TourDatabase.ENTITY_IS_NOT_SAVED;
		newTourMarker._createId = ++_createCounter;

		return newTourMarker;
	}

	@Override
	public int compareTo(final Object other) {

		// default sorting for tour markers is by index

		if (other instanceof TourMarker) {
			final TourMarker otherTourMarker = (TourMarker) other;

			return serieIndex - otherTourMarker.getSerieIndex();
		}

		return 0;
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
		} else if (distance20 != comparedMarker.distance20) {
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
		} else if ((ignoreType == false) && (type != comparedMarker.type)) {
			return false;
		} else if (visualPosition != comparedMarker.visualPosition) {
			return false;
		} else if (tourData != comparedMarker.tourData) {
			return false;
		}

		return true;
	}

//	public void setCreateId() {
//		markerId = TourDatabase.ENTITY_IS_NOT_SAVED;
//		_createId = ++_createCounter;
//	}

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

		if (_createId == 0) {

			// tour is from the database
			if (markerId != other.markerId) {
				return false;
			}
		} else {

			// tour was create or imported
			if (_createId != other._createId) {
				return false;
			}
		}

		return true;
	}

	public String getCategory() {
		return category;
	}

	/**
	 * @return Returns distance in meters in the metric system or -1 when the distance is not
	 *         available
	 */
	public float getDistance() {
		return distance20;
	}

	/**
	 * @return Returns the marker label or an empty string when the label is not set.
	 */
	public String getLabel() {
		return label;
	}

	public int getLabelXOffset() {
		return labelXOffset;
	}

	public int getLabelYOffset() {
		return labelYOffset;
	}

	/**
	 * @return Contains <b>width</b> and <b>height</b> of the marker image.
	 *         <p>
	 *         <b>x</b> and <b>y</b> contains the width and height of the marker banner which is the
	 *         marker label text including the border
	 */
	public Rectangle getMarkerBounds() {
		return _markerBounds;
	}

	public long getMarkerId() {
		return markerId;
	}

	/**
	 * @return Returns position of this marker in the data serie
	 */
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
		return _visibleType;
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
		result = prime * result + (int) (_createId ^ (_createId >>> 32));
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

		distance20 = backupMarker.distance20;
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
	 *            Distance in meter or <code>-1</code> when the distance is not available
	 */
	public void setDistance(final float distance) {
		this.distance20 = distance;
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

		backupMarker.category = category;
		backupMarker.label = label;

		backupMarker.distance20 = distance20;
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

	/**
	 * @param markerBounds
	 */
	public void setMarkerBounds(final Rectangle markerBounds) {
		_markerBounds = markerBounds;
	}

	public void setSerieIndex(final int serieIndex) {
		this.serieIndex = serieIndex;
	}

	/**
	 * @param time
	 *            Time in seconds relative to the tour start. When value is not available it is set
	 *            to -1.
	 */
	public void setTime(final int time) {
		this.time = time;
	}

	public void setVisibleType(final int visibleType) {
		this._visibleType = visibleType;
	}

	public void setVisualPosition(final int visualPosition) {
		this.visualPosition = visualPosition;
	}

	@Override
	public String toString() {
		return new StringBuilder()//
				.append(TourMarker.class.getSimpleName())
				.append(" id:") //$NON-NLS-1$
				.append(markerId)
				.append(" createId:") //$NON-NLS-1$
				.append(_createId)
				.append(" distance:") //$NON-NLS-1$
				.append(distance20)
				.append(" time:") //$NON-NLS-1$
				.append(time)
				.append(" serieIndex:") //$NON-NLS-1$
				.append(serieIndex)
				.toString();
	}

	@Override
	public String toXml() {
		try {
			final JAXBContext context = JAXBContext.newInstance(this.getClass());
			final Marshaller marshaller = context.createMarshaller();
			final StringWriter sw = new StringWriter();
			marshaller.marshal(this, sw);
			return sw.toString();
		} catch (final JAXBException e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Convert fields from old to new data type.
	 */
	public void updateDatabase_019_to_020() {
		distance20 = distance;
		distance = 0;
	}

}
