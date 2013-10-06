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
import gov.nasa.worldwind.render.GlobeAnnotation;

import java.awt.Insets;
import java.awt.Point;

import net.tourbook.common.UI;

import org.eclipse.jface.dialogs.IDialogSettings;

/**
 * Part of this code is copied from: gov.nasa.worldwindx.examples.analytics.AnalyticSurfaceLegend
 */
public class ChartSliderLayer extends AnnotationLayer {

	static final int			CHART_SLIDER_DRAW_OFFSET_Y	= 40;
	static final int			CHART_SLIDER_CORNER_RADIUS	= 7;
	static final int			CHART_SLIDER_LEADER_GAP		= 7;
	static final int			CHART_SLIDER_MARGIN			= 5;

	public static final String	MAP3_LAYER_ID				= "ChartSliderLayer";	//$NON-NLS-1$

	private GlobeAnnotation		_leftSlider;
	private GlobeAnnotation		_rightSlider;

	public ChartSliderLayer(final IDialogSettings state) {

		setPickEnabled(false);

		createAllSlider();
	}

	public void createAllSlider() {

		_leftSlider = createSlider(true);
		_rightSlider = createSlider(false);

		addAnnotation(_leftSlider);
		addAnnotation(_rightSlider);
	}

	private GlobeAnnotation createSlider(final boolean isLeftSlider) {

		final GlobeAnnotation slider = new GlobeAnnotation(UI.EMPTY_STRING, Position.ZERO);
		slider.setAlwaysOnTop(true);

		/*
		 * set attributes
		 */
		final AnnotationAttributes attributes = slider.getAttributes();

		final int drawOffsetX = 10;

		attributes.setCornerRadius(CHART_SLIDER_CORNER_RADIUS);
		attributes.setInsets(new Insets(
				CHART_SLIDER_MARGIN,
				CHART_SLIDER_MARGIN + 3,
				CHART_SLIDER_MARGIN,
				CHART_SLIDER_MARGIN));

		attributes.setDrawOffset(new Point(isLeftSlider ? -drawOffsetX : drawOffsetX, CHART_SLIDER_DRAW_OFFSET_Y));
		attributes.setLeaderGapWidth(CHART_SLIDER_LEADER_GAP);
		attributes.setFont(UI.AWT_FONT_ARIAL_BOLD_12);

		// initially hide the slider
		attributes.setVisible(false);

		return slider;
	}

	public GlobeAnnotation getLeftSlider() {
		return _leftSlider;
	}

	public GlobeAnnotation getRightSlider() {
		return _rightSlider;
	}

	public void setSliderVisible(final boolean isVisible) {

		_rightSlider.getAttributes().setVisible(isVisible);
		_leftSlider.getAttributes().setVisible(isVisible);
	}

}
