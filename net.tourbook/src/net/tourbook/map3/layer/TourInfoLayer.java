/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
package net.tourbook.map3.layer;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.AnnotationLayer;
import gov.nasa.worldwind.render.AnnotationAttributes;
import gov.nasa.worldwind.render.DrawContext;

import java.awt.Insets;
import java.awt.Point;

import net.tourbook.common.UI;
import net.tourbook.map3.view.Map3Manager;

import org.eclipse.jface.dialogs.IDialogSettings;

/**
 *
 */
public class TourInfoLayer extends AnnotationLayer implements IToolLayer {

	public static final String		MAP3_LAYER_ID	= "TourInfoLayer";	//$NON-NLS-1$

	/**
	 * Track point annotation when a tour track is hovered.
	 */
	private TrackPointAnnotation	_hoveredTrackPoint;

	public TourInfoLayer(final IDialogSettings state) {

		setPickEnabled(false);

		createAllSlider();
	}

	public void createAllSlider() {

		_hoveredTrackPoint = createHoveredTrackPoint();

		addAnnotation(_hoveredTrackPoint);
	}

	private TrackPointAnnotation createHoveredTrackPoint() {

		final TrackPointAnnotation trackPoint = new TrackPointAnnotation(UI.EMPTY_STRING, Position.ZERO);

		trackPoint.setAlwaysOnTop(true);

		final AnnotationAttributes attributes = trackPoint.getAttributes();

		attributes.setCornerRadius(ChartSliderLayer.CHART_SLIDER_CORNER_RADIUS);
		attributes.setInsets(new Insets(
				ChartSliderLayer.CHART_SLIDER_MARGIN,
				ChartSliderLayer.CHART_SLIDER_MARGIN + 3,
				ChartSliderLayer.CHART_SLIDER_MARGIN,
				ChartSliderLayer.CHART_SLIDER_MARGIN));

		attributes.setDrawOffset(new Point(0, ChartSliderLayer.CHART_SLIDER_DRAW_OFFSET_Y));
		attributes.setLeaderGapWidth(ChartSliderLayer.CHART_SLIDER_LEADER_GAP);
		attributes.setFont(UI.AWT_FONT_ARIAL_BOLD_12);

		// initially hide the annotation
		attributes.setVisible(false);

		return trackPoint;
	}

	@Override
	protected void doRender(final DrawContext dc) {

		_hoveredTrackPoint.setSliderPosition(dc);

		super.doRender(dc);
	}

	@Override
	public int getDefaultPosition() {
		return Map3Manager.INSERT_BEFORE_PLACE_NAMES;
	}

	public TrackPointAnnotation getHoveredTrackPoint() {
		return _hoveredTrackPoint;
	}

	public void setTrackPointVisible(final boolean isVisible) {

		// show/hide track point
		_hoveredTrackPoint.getAttributes().setVisible(isVisible);

		// show/hide layer
		setEnabled(isVisible);
	}

}
