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

import gov.nasa.worldwind.animation.BasicAnimator;
import gov.nasa.worldwind.animation.Interpolator;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.util.WWMath;
import gov.nasa.worldwind.view.orbit.OrbitView;

/*
 * Original implementation (18.7.2013) gov.nasa.worldwindx.examples.KeepingObjectsInView.ViewAnimator
 */
public class ViewAnimator extends BasicAnimator {

	protected static final double	LOCATION_EPSILON	= 1.0e-9;
	protected static final double	ALTITUDE_EPSILON	= 0.1;

	protected OrbitView				view;
	protected ViewController		viewController;
	protected boolean				haveTargets;
	protected Position				centerPosition;
	protected double				zoom;

	public ViewAnimator(final double smoothing, final OrbitView view, final ViewController viewController) {
		super(new Interpolator() {
			public double nextInterpolant() {
				return 1d - smoothing;
			}
		});

		this.view = view;
		this.viewController = viewController;
	}

	@Override
	protected void setImpl(final double interpolant) {
		this.updateTargetValues();

		if (!this.haveTargets) {
			this.stop();
			return;
		}

		if (this.valuesMeetCriteria(this.centerPosition, this.zoom)) {
			this.view.setCenterPosition(this.centerPosition);
			this.view.setZoom(this.zoom);
			this.stop();
		} else {
			final Position newCenterPos = Position.interpolateGreatCircle(
					interpolant,
					this.view.getCenterPosition(),
					this.centerPosition);
			final double newZoom = WWMath.mix(interpolant, this.view.getZoom(), this.zoom);
			this.view.setCenterPosition(newCenterPos);
			this.view.setZoom(newZoom);
		}

		this.view.firePropertyChange(AVKey.VIEW, null, this);
	}

	@Override
	public void stop() {
		super.stop();
		this.haveTargets = false;
	}

	protected void updateTargetValues() {
		if (this.viewController.isSceneContained(this.view)) {
			return;
		}

		final Vec4[] lookAtPoints = this.viewController.computeViewLookAtForScene(this.view);
		if (lookAtPoints == null || lookAtPoints.length != 3) {
			return;
		}

		this.centerPosition = this.viewController.computePositionFromPoint(lookAtPoints[1]);
		this.zoom = lookAtPoints[0].distanceTo3(lookAtPoints[1]);
		if (this.zoom < view.getZoom()) {
			this.zoom = view.getZoom();
		}

		this.haveTargets = true;
	}

	protected boolean valuesMeetCriteria(final Position centerPos, final double zoom) {
		final Angle cd = LatLon.greatCircleDistance(this.view.getCenterPosition(), centerPos);
		final double ed = Math.abs(this.view.getCenterPosition().getElevation() - centerPos.getElevation());
		final double zd = Math.abs(this.view.getZoom() - zoom);

		return cd.degrees < LOCATION_EPSILON && ed < ALTITUDE_EPSILON && zd < ALTITUDE_EPSILON;
	}
}
