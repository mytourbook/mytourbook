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

import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.AnnotationLayer;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.render.AnnotationAttributes;
import gov.nasa.worldwind.render.DrawContext;

import java.awt.Insets;
import java.awt.Point;

import net.tourbook.common.UI;

import org.eclipse.jface.dialogs.IDialogSettings;

/**
 * Part of this code is copied from: gov.nasa.worldwindx.examples.analytics.AnalyticSurfaceLegend
 */
public class TrackSliderLayer extends AnnotationLayer implements SelectListener {

	static final int				CHART_SLIDER_DRAW_OFFSET_Y	= 40;
	static final int				CHART_SLIDER_CORNER_RADIUS	= 7;
	static final int				CHART_SLIDER_LEADER_GAP		= 7;
	static final int				CHART_SLIDER_MARGIN			= 5;

	public static final String		MAP3_LAYER_ID				= "TrackSliderLayer";	//$NON-NLS-1$

	private TrackPointAnnotation	_leftSlider;
	private TrackPointAnnotation	_rightSlider;
	private TrackPointLine			_leftSliderLine;
	private TrackPointLine			_rightSliderLine;

	public TrackSliderLayer(final IDialogSettings state) {

		setPickEnabled(false);

		createAllSlider();
	}

	public void createAllSlider() {

		_leftSlider = createSlider(true);
		_rightSlider = createSlider(false);

		addAnnotation(_leftSlider);
		addAnnotation(_rightSlider);

		_leftSliderLine = new TrackPointLine();
		_rightSliderLine = new TrackPointLine();
	}

	private TrackPointAnnotation createSlider(final boolean isLeftSlider) {

		final TrackPointAnnotation slider = new TrackPointAnnotation(UI.EMPTY_STRING, Position.ZERO);
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

//		slider.setPickEnabled(true);

		setPickEnabled(true);

		// initially hide the slider
		attributes.setVisible(false);

		return slider;
	}

	@Override
	protected void doRender(final DrawContext dc) {

		final Position leftSliderPosition = _leftSlider.setSliderPosition(dc);
		final Position rightSliderPosition = _rightSlider.setSliderPosition(dc);

		if (rightSliderPosition != null) {
			_rightSliderLine.makeOrderedRenderable(//
					dc,
					rightSliderPosition,
					_rightSlider.getAttributes().getTextColor());
		}

		if (leftSliderPosition != null) {
			_leftSliderLine.makeOrderedRenderable(//
					dc,
					leftSliderPosition,
					_leftSlider.getAttributes().getTextColor());
		}

		super.doRender(dc);
	}

	public TrackPointAnnotation getLeftSlider() {
		return _leftSlider;
	}

	public TrackPointAnnotation getRightSlider() {
		return _rightSlider;
	}

	/**
	 * This listener is set in set {@link #setupWWSelectionListener(boolean)}
	 * <p>
	 * {@inheritDoc}
	 * 
	 * @see gov.nasa.worldwind.event.SelectListener#selected(gov.nasa.worldwind.event.SelectEvent)
	 */
	@Override
	public void selected(final SelectEvent event) {

//		if (event.getMouseEvent() != null && event.getMouseEvent().isConsumed()) {
//			return;
//		}
//
//		if (Map3Manager.getMap3View().isContextMenuVisible()) {
//
//			// prevent actions when context menu is visible
//
//			return;
//		}

		final String eventAction = event.getEventAction();

		// get hovered object
		final PickedObject pickedObject = event.getTopPickedObject();

		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("\teventAction: " + eventAction) //$NON-NLS-1$
				+ ("\tpickedObject: " + pickedObject) //$NON-NLS-1$
		//
				);
		// TODO remove SYSTEM.OUT.PRINTLN
	}

	public void setSliderVisible(final boolean isVisible) {

		_rightSlider.getAttributes().setVisible(isVisible);
		_leftSlider.getAttributes().setVisible(isVisible);

		_rightSliderLine.setVisible(isVisible);
		_leftSliderLine.setVisible(isVisible);
	}

}
