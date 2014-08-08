/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
package net.tourbook.tour;

import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.data.TourData;
import net.tourbook.tag.ChangedTags;
import net.tourbook.ui.UI;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;

public enum TourEventId {

	/**
	 * Tours has been modified, event data contains the modified tours {@link TourData}
	 */
	TOUR_CHANGED,

	/**
	 * Event data contain the selected tours.
	 */
	TOUR_SELECTION,

	/**
	 * {@link TourData} has been modified, the UI must be updated by reloading {@link TourData}
	 * <p>
	 * When this event is fired, {@link TourDataEditorView} must <b>NOT</b> be dirty, this can be
	 * checked with the method {@link UI#isTourEditorModified(TourData)}, it must return
	 * <code>false</code>.
	 * <p>
	 * When a tour is modified, the event {@link #TOUR_CHANGED} must be fired
	 */
	UPDATE_UI,

	/**
	 * properties of the tour chart has been changed
	 */
	TOUR_CHART_PROPERTY_IS_MODIFIED,

	/**
	 * 
	 */
	SEGMENT_LAYER_CHANGED,

	/**
	 * a reference tour is created
	 */
	REFERENCE_TOUR_IS_CREATED,

	/**
	 * 
	 */
	REFERENCE_TOUR_CHANGED,

	/**
	 * 
	 */
	COMPARE_TOUR_CHANGED,

	/**
	 * Tags for a tour has been modified. The property data contains an object {@link ChangedTags}
	 * which contains the tags and the modified tours
	 */
	NOTIFY_TAG_VIEW,

	/**
	 * structure of the tags changed, this includes add/remove of tags and categories and
	 * tag/category renaming
	 */
	TAG_STRUCTURE_CHANGED,

	/**
	 * Sliders in the tourchart moved. Property data contains {@link SelectionChartInfo} with the
	 * position of the sliders
	 */
	SLIDER_POSITION_CHANGED,

	/**
	 * All computed data for all tours are modified
	 */
	ALL_TOURS_ARE_MODIFIED,

	/**
	 * remove the tour which is currently displayed because the tour is removed from a view or a
	 * view is closed which provided the tour
	 */
	CLEAR_DISPLAYED_TOUR,

	/**
	 * A tour marker is selected, event data contain a {@link SelectionTourMarker}.
	 */
	MARKER_SELECTION,

}
