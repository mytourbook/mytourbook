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

	public static final String	MAP3_LAYER_ID	= "ChartSliderLayer";	//$NON-NLS-1$

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
		final int defaultMargin = 5;
		final int top = defaultMargin;
		final int left = defaultMargin + 3;
		final int bottom = defaultMargin;
		final int right = defaultMargin;
		final Insets insets = new Insets(top, left, bottom, right);

		final int leaderGapWidth = 7;
		final int drawOffsetX = 10;
		final int drawOffsetY = 40;
		final Point drawOffset = new Point(isLeftSlider ? -drawOffsetX : drawOffsetX, drawOffsetY);

		final int cornerRadius = 7;

		attributes.setDrawOffset(drawOffset);
		attributes.setLeaderGapWidth(leaderGapWidth);

		attributes.setCornerRadius(cornerRadius);
		attributes.setInsets(insets);

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
