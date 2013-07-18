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

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.geom.ExtentHolder;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.view.orbit.OrbitView;
import gov.nasa.worldwindx.examples.util.ExtentVisibilitySupport;

import java.awt.Rectangle;
import java.util.ArrayList;

/*
 * Original implementation (18.7.2013) gov.nasa.worldwindx.examples.KeepingObjectsInView.ViewController
 */
public class ViewController {

	protected static final double	SMOOTHING_FACTOR	= 0.96;

	protected boolean				enabled				= true;
	protected WorldWindow			wwd;
	protected ViewAnimator			animator;
	protected Iterable<?>			objectsToTrack;

	public ViewController(final WorldWindow wwd) {
		this.wwd = wwd;
	}

	protected void addExtents(final ExtentVisibilitySupport vs) {

		// Compute screen extents for WWIcons which have feedback information from their IconRenderer.
		final Iterable<?> iterable = this.getObjectsToTrack();
		if (iterable == null) {
			return;
		}

		final ArrayList<ExtentHolder> extentHolders = new ArrayList<ExtentHolder>();
		final ArrayList<ExtentVisibilitySupport.ScreenExtent> screenExtents = new ArrayList<ExtentVisibilitySupport.ScreenExtent>();

		for (final Object o : iterable) {
			if (o == null) {
				continue;
			}

			if (o instanceof ExtentHolder) {
				extentHolders.add((ExtentHolder) o);
			} else if (o instanceof AVList) {
				final AVList avl = (AVList) o;

				final Object b = avl.getValue(AVKey.FEEDBACK_ENABLED);
				if (b == null || !Boolean.TRUE.equals(b)) {
					continue;
				}

				if (avl.getValue(AVKey.FEEDBACK_REFERENCE_POINT) != null) {
					screenExtents.add(new ExtentVisibilitySupport.ScreenExtent((Vec4) avl
							.getValue(AVKey.FEEDBACK_REFERENCE_POINT), (Rectangle) avl
							.getValue(AVKey.FEEDBACK_SCREEN_BOUNDS)));
				}
			}
		}

		if (!extentHolders.isEmpty()) {
			final Globe globe = this.wwd.getModel().getGlobe();
			final double ve = this.wwd.getSceneController().getVerticalExaggeration();
			vs.setExtents(ExtentVisibilitySupport.extentsFromExtentHolders(extentHolders, globe, ve));
		}

		if (!screenExtents.isEmpty()) {
			vs.setScreenExtents(screenExtents);
		}
	}

	public Position computePositionFromPoint(final Vec4 point) {
		return this.wwd.getModel().getGlobe().computePositionFromPoint(point);
	}

	public Vec4[] computeViewLookAtForScene(final View view) {
		final Globe globe = this.wwd.getModel().getGlobe();
		final double ve = this.wwd.getSceneController().getVerticalExaggeration();

		final ExtentVisibilitySupport vs = new ExtentVisibilitySupport();
		this.addExtents(vs);

		return vs.computeViewLookAtContainingExtents(globe, ve, view);
	}

	public Iterable<?> getObjectsToTrack() {
		return this.objectsToTrack;
	}

	public void gotoScene() {
		final Vec4[] lookAtPoints = this.computeViewLookAtForScene(this.wwd.getView());
		if (lookAtPoints == null || lookAtPoints.length != 3) {
			return;
		}

		final Position centerPos = this.wwd.getModel().getGlobe().computePositionFromPoint(lookAtPoints[1]);
		final double zoom = lookAtPoints[0].distanceTo3(lookAtPoints[1]);

		this.wwd.getView().stopAnimations();
		this.wwd.getView().goTo(centerPos, zoom);
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public boolean isSceneContained(final View view) {
		final ExtentVisibilitySupport vs = new ExtentVisibilitySupport();
		this.addExtents(vs);

		return vs.areExtentsContained(view);
	}

	public void sceneChanged() {
		final OrbitView view = (OrbitView) this.wwd.getView();

		if (!this.isEnabled()) {
			return;
		}

		if (this.isSceneContained(view)) {
			return;
		}

		if (this.animator == null || !this.animator.hasNext()) {

			this.animator = new ViewAnimator(SMOOTHING_FACTOR, view, this);
			this.animator.start();

			view.stopAnimations();
			view.addAnimator(this.animator);

			view.firePropertyChange(AVKey.VIEW, null, view);
		}
	}

	public void setEnabled(final boolean enabled) {
		this.enabled = enabled;

		if (this.animator != null) {
			this.animator.stop();
			this.animator = null;
		}
	}

	public void setObjectsToTrack(final Iterable<?> iterable) {
		this.objectsToTrack = iterable;
	}
}
