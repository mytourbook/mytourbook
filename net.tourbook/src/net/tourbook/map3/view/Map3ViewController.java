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
package net.tourbook.map3.view;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.awt.ViewInputHandler;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.view.orbit.BasicOrbitView;
import gov.nasa.worldwind.view.orbit.OrbitView;
import gov.nasa.worldwind.view.orbit.OrbitViewInputHandler;

import java.util.List;

import net.tourbook.common.util.StatusUtil;

import org.eclipse.swt.widgets.Display;

/*
 * Original implementation (18.7.2013) gov.nasa.worldwindx.examples.KeepingObjectsInView.ViewController
 * Original implementation (21.7.2013) gov.nasa.worldwindx.examples.kml.KMLViewController
 */
/**
 * Base class for controllers to animate the view to look at KML features. Each controller includes
 * logic to animate to a {@code LookAt} or {@code Camera} view, or to animate to a default view a
 * KML feature that does not define a view. Subclasses of this base class implement animator logic
 * for particular types of {@link View}, for example {@link OrbitView}.
 * <p/>
 * An application that provides a custom View implementation can extend this base class to provide a
 * KML controller for the custom view.
 * 
 * @author pabercrombie
 * @version $Id: KMLViewController.java 1 2011-07-16 23:22:47Z dcollins $
 */
public abstract class Map3ViewController {

	/** Default altitude from which to view a KML feature. */
	public static final double	DEFAULT_VIEW_ALTITUDE	= 10000;

	/** Default altitude from which to view a KML feature. */
	protected double			viewAltitude			= DEFAULT_VIEW_ALTITUDE;

	/** WorldWindow that holds the view to animate. */
	protected WorldWindow		wwd;

	/**
	 * Create the view controller.
	 * 
	 * @param wwd
	 *            WorldWindow that holds the view to animate.
	 */
	protected Map3ViewController(final WorldWindow wwd) {
		this.wwd = wwd;
	}

	/**
	 * Convenience method to create a new view controller appropriate for the
	 * <code>WorldWindow</code>'s current <code>View</code>. Accepted view types are as follows:
	 * <ul>
	 * <li>{@link gov.nasa.worldwind.view.orbit.OrbitView}</li>
	 * <li>{@link gov.nasa.worldwind.view.firstperson.BasicFlyView}</li>
	 * </ul>
	 * . If the <code>View</code> is not one of the recognized types, this returns <code>null</code>
	 * and logs a warning.
	 * 
	 * @param wwd
	 *            the <code>WorldWindow</code> to create a view controller for.
	 * @return A new view controller, or <code>null</code> if the <code>WorldWindow</code>'s
	 *         <code>View</code> type is not one of the recognized types.
	 */
	public static Map3ViewController create(final WorldWindow wwd) {

		if (wwd == null) {
			final String message = Logging.getMessage("nullValue.WorldWindow");
			Logging.logger().severe(message);
			throw new IllegalStateException(message);
		}

		return new Map3ViewController(wwd) {};
	}

	/**
	 * Get the maximum altitude in a list of positions.
	 * 
	 * @param positions
	 *            List of positions to search for max altitude.
	 * @return The maximum elevation in the list of positions. Returns {@code Double.MIN_VALUE} if
	 *         {@code positions} is empty.
	 */
	protected double findMaxAltitude(final List<? extends Position> positions) {
		double maxAltitude = -Double.MAX_VALUE;
		for (final Position p : positions) {
			final double altitude = p.getAltitude();
			if (altitude > maxAltitude) {
				maxAltitude = altitude;
			}
		}

		return maxAltitude;
	}

	/**
	 * Get the default altitude for viewing a KML placemark when the globe flies to a placemark.
	 * This setting is only used if the placemark does not specify a view.
	 * 
	 * @return Default altitude from which to view a placemark.
	 */
	public double getViewAltitude() {
		return this.viewAltitude;
	}

	/**
	 * Go to a view of a list of positions. This method computes a view looking straight down from
	 * an altitude that brings all of the positions into view.
	 * 
	 * @param positions
	 *            List of positions to bring into view
	 */
	public void goToDefaultView(final List<? extends Position> positions) {

		final View view = this.wwd.getView();
		final Globe globe = view.getGlobe();

		if (globe == null) {

			/*
			 * this happenes when a tour is selected and the 3d map is not yet opened but is being
			 * opened with an action button
			 */
			StatusUtil.logInfo("globe == null");

			// try again
			Display.getCurrent().timerExec(200, new Runnable() {
				public void run() {
					gotoView(positions);
				}
			});

			return;
		}

		gotoView(positions);
	}

	private void gotoView(final List<? extends Position> positions) {

		final View view = this.wwd.getView();
		final Globe globe = view.getGlobe();

		if (globe == null) {
			return;
		}

		final BasicOrbitView orbitView = (BasicOrbitView) view;

		// If there is only one point, move the view over that point, maintaining the current elevation.
		if (positions.size() == 1) // Only one point
		{
			final Position pos = positions.get(0);
			view.goTo(pos, pos.getAltitude() + this.getViewAltitude());

		} else if (positions.size() > 1)// Many points
		{
			// Compute the sector that bounds all of the points in the list. Move the view so that this entire
			// sector is visible.
			final Sector sector = Sector.boundingSector(positions);

			final double ve = this.wwd.getSceneController().getVerticalExaggeration();

			// Find the highest point in the geometry. Make sure that our bounding cylinder encloses this point.
			final double maxAltitude = this.findMaxAltitude(positions);

			final double[] minAndMaxElevations = globe.getMinAndMaxElevations(sector);
			final double minElevation = minAndMaxElevations[0];
			final double maxElevation = Math.max(minAndMaxElevations[1], maxAltitude);

			final Extent extent = Sector.computeBoundingCylinder(globe, ve, sector, minElevation, maxElevation);
			if (extent == null) {
				final String message = Logging.getMessage("nullValue.SectorIsNull");
				Logging.logger().warning(message);
				return;
			}
			final Angle fov = view.getFieldOfView();

			final Position centerPos = new Position(sector.getCentroid(), maxAltitude);
			final double zoom = extent.getRadius() / (fov.tanHalfAngle() * fov.cosHalfAngle()) + this.getViewAltitude();

			final ViewInputHandler inputViewHander = view.getViewInputHandler();
			if (inputViewHander instanceof OrbitViewInputHandler) {

//				final OrbitViewInputHandler orbitViewHandler = (OrbitViewInputHandler) inputViewHander;
//
//				orbitViewHandler.stopAnimators();
//				orbitViewHandler.addEyePositionAnimator(
//						4000,
//						view.getEyePosition(),
//						positions.get(positions.size() - 1));

// #####################################################################################

				// Stop all animations on the view, and start a 'pan to' animation.
				orbitView.stopAnimations();
				orbitView.addPanToAnimator(centerPos, orbitView.getHeading(), orbitView.getPitch(), zoom);

// #####################################################################################

////				OrbitViewCenterAnimator
//				final MoveToPositionAnimator animator = new MoveToPositionAnimator(
//						view.getEyePosition(),
//						centerPos,
//						0.9,
//						ViewPropertyAccessor.createEyePositionAccessor(view));
//
//				animator.start();
//
//				view.stopAnimations();
//				view.addAnimator(animator);
//				view.firePropertyChange(AVKey.VIEW, null, view);

// #####################################################################################

//		        addPanToAnimator(centerPos, view.getHeading(), view.getPitch(), zoom, true);
//
//		        addPanToAnimator(
//		        		view.getCenterPosition(),
//		        		centerPos,
//
//		        		view.getHeading(),
//		        		heading,
//
//		        		view.getPitch(),
//		        		pitch,
//
//		        		view.getZoom(),
//		        		zoom,
//
//		        		endCenterOnSurface);

//	public void addPanToAnimator(Position centerPos, Angle heading, Angle pitch, double zoom, boolean endCenterOnSurface)
//
// addPanToAnimator(view.getCenterPosition(), centerPos, view.getHeading(), heading, view.getPitch(), pitch, view.getZoom(), zoom, endCenterOnSurface);

//				final boolean endCenterOnSurface = true;
//
//				final int altitudeMode = endCenterOnSurface ? WorldWind.CLAMP_TO_GROUND : WorldWind.ABSOLUTE;
//
//				final long MIN_LENGTH_MILLIS = 2000;
//				final long MAX_LENGTH_MILLIS = 10000;
//
//				final Position beginCenterPos = orbitView.getCenterPosition();
//				final Position endCenterPos = centerPos;
//
//				final Angle beginHeading = orbitView.getHeading();
//				final Angle endHeading = beginHeading;
//				final Angle beginPitch = orbitView.getPitch();
//				final Angle endPitch = beginPitch;
//				final double beginZoom = orbitView.getZoom();
//				final double endZoom = zoom;
//
//				final long timeToMove = AnimationSupport.getScaledTimeMillisecs(
//						beginCenterPos,
//						endCenterPos,
//						MIN_LENGTH_MILLIS,
//						MAX_LENGTH_MILLIS);
//
//				final FlyToOrbitViewAnimator animator = FlyToOrbitViewAnimator.createFlyToOrbitViewAnimator(orbitView,
//				//
//						beginCenterPos,
//						endCenterPos,
//						//
//						beginHeading,
//						endHeading,
//						//
//						beginPitch,
//						endPitch,
//						//
//						beginZoom,
//						endZoom,
//						//
//						timeToMove,
//						altitudeMode);
//
//				animator.start();
//
//				view.stopAnimations();
//				view.addAnimator(animator);
//				view.firePropertyChange(AVKey.VIEW, null, view);

			} else {

				view.goTo(centerPos, zoom);
			}

		}
	}

	/**
	 * Set the default altitude for viewing a KML placemark when the globe flies to a placemark.
	 * This setting is only used if the placemark does not specify a view.
	 * 
	 * @param viewAltitude
	 *            Default altitude from which to view a placemark.
	 */
	public void setViewAltitude(final double viewAltitude) {
		this.viewAltitude = viewAltitude;
	}
}
