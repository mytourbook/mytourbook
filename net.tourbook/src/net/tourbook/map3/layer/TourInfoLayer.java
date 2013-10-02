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
import net.tourbook.common.UI;
import net.tourbook.map3.view.Map3Manager;

import org.eclipse.jface.dialogs.IDialogSettings;

/**
 *
 */
public class TourInfoLayer extends AnnotationLayer implements IToolLayer {

	public static final String	MAP3_LAYER_ID	= "TourInfoLayer";	//$NON-NLS-1$

	private GlobeAnnotation		_hoveredTrackPoint;

	public TourInfoLayer(final IDialogSettings state) {

		setPickEnabled(false);

		createAllSlider();
	}

	public void createAllSlider() {

		_hoveredTrackPoint = createHoveredTrackPoint();

		addAnnotation(_hoveredTrackPoint);
	}

	private GlobeAnnotation createHoveredTrackPoint() {

		final GlobeAnnotation trackPoint = new GlobeAnnotation(UI.EMPTY_STRING, Position.ZERO);

		trackPoint.setAlwaysOnTop(true);

		final AnnotationAttributes attributes = trackPoint.getAttributes();

		attributes.setVisible(false);

		return trackPoint;
	}

	@Override
	public int getDefaultPosition() {

		return Map3Manager.INSERT_BEFORE_PLACE_NAMES;
	}

	public GlobeAnnotation getTrackPoint() {
		return _hoveredTrackPoint;
	}

	public void setTrackPointVisible(final boolean isVisible) {

		// show/hide track point
		_hoveredTrackPoint.getAttributes().setVisible(isVisible);

		// show/hide layer
		setEnabled(isVisible);
	}

}
