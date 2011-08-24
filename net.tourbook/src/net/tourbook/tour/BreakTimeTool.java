/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourData;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

/**
 * This class contains all data which are needed to compute tour break time.
 */
public class BreakTimeTool {

	private static String					_prefBreakTimeMethodId;
	private static double					_prefMinAvgSpeedAS;
	private static double					_prefMinSliceSpeedAS;
	private static int						_prefMinSliceTimeAS;
	private static float					_prefMinSliceSpeed;
	private static float					_prefMinAvgSpeed;
	private static int						_prefShortestTime;
	private static float					_prefMaxDistance;
	private static int						_prefSliceDiff;

	public static final String				BREAK_TIME_METHOD_BY_AVG_SLICE_SPEED	= "AvgSliceSpeed";				//$NON-NLS-1$
	public static final String				BREAK_TIME_METHOD_BY_AVG_SPEED			= "AvgSpeed";					//$NON-NLS-1$
	public static final String				BREAK_TIME_METHOD_BY_SLICE_SPEED		= "SliceSpeed";				//$NON-NLS-1$
	public static final String				BREAK_TIME_METHOD_BY_TIME_DISTANCE		= "TimeDistance";				//$NON-NLS-1$

	// sequence is used for the combo selection box
	public static final BreakTimeMethod[]	BREAK_TIME_METHODS						= {
																					//
			new BreakTimeMethod(BREAK_TIME_METHOD_BY_AVG_SLICE_SPEED,//
					Messages.Compute_BreakTime_Method_SpeedByAverageAndSlice),
			//
			new BreakTimeMethod(BREAK_TIME_METHOD_BY_AVG_SPEED, //
					Messages.Compute_BreakTime_Method_SpeedByAverage),
			//
			new BreakTimeMethod(BREAK_TIME_METHOD_BY_SLICE_SPEED, //
					Messages.Compute_BreakTime_Method_SpeedBySlice),
			//
			new BreakTimeMethod(BREAK_TIME_METHOD_BY_TIME_DISTANCE, //
					Messages.Compute_BreakTime_Method_TimeDistance)
																					//
																					};

	private static IPreferenceStore			_prefStore								= TourbookPlugin.getDefault()//
																							.getPreferenceStore();
	private static boolean					_isPrefSet;

	/**
	 * method how break time is computed
	 */
	public String							breakTimeMethodId;

	/**
	 * shortes tims in seconds
	 */
	public int								breakShortestTime;

	/**
	 * max distance in meter
	 */
	public float							breakMaxDistance;

	/**
	 * time between 2 time slices in minutes
	 */
	public int								breakSliceDiff;

	/**
	 * slice speed in km/h
	 */
	public float							breakMinSliceSpeed;

	/**
	 * average speed in km/h
	 */
	public float							breakMinAvgSpeed;

	/**
	 * average speed in km/h
	 */
	public double							breakMinAvgSpeedAS;

	/**
	 * slice speed in km/h
	 */
	public double							breakMinSliceSpeedAS;

	/**
	 * Minimum time in seconds for the slice speed
	 */
	public int								breakMinSliceTimeAS;

	@SuppressWarnings("unused")
	private BreakTimeTool() {}

	public BreakTimeTool(	final String breakTimeMethodId,
							final int breakShortestTime,
							final float breakMaxDistance,
							final float breakMinSliceSpeed,
							final float breakMinAvgSpeed,
							final int breakSliceDiff,
							final double breakMinAvgSpeedAS,
							final double breakMinSliceSpeedAS,
							final int breakMinSliceTimeAS) {

		this.breakTimeMethodId = breakTimeMethodId;

		this.breakMinAvgSpeedAS = breakMinAvgSpeedAS;
		this.breakMinSliceSpeedAS = breakMinSliceSpeedAS;
		this.breakMinSliceTimeAS = breakMinSliceTimeAS;

		this.breakMinSliceSpeed = breakMinSliceSpeed;
		this.breakMinAvgSpeed = breakMinAvgSpeed;

		this.breakShortestTime = breakShortestTime;
		this.breakMaxDistance = breakMaxDistance;
		this.breakSliceDiff = breakSliceDiff;
	}

	private static void checkPrefValues() {

		if (_isPrefSet) {
			return;
		}

		updatePrefValues();
		_isPrefSet = true;

		// observe modifications

		_prefStore.addPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.BREAK_TIME_IS_MODIFIED)
						|| property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// break time or measurement has been modified

					updatePrefValues();
				}
			}
		});
	}

	public static BreakTimeResult computeBreakTimeByAvgSliceSpeed(	final TourData tourData,
																	final double minAvgSpeed,
																	final double minSliceSpeed,
																	final int minSliceTime) {

		final int[] timeSerie = tourData.timeSerie;
		final int[] distanceSerie = tourData.getMetricDistanceSerie();

		final boolean[] breakTimeSerie = new boolean[timeSerie.length];

		final int[] speedSerie = tourData.getSpeedSerieMetric();

		int lastTime = 0;
		int lastDistance = 0;
		int tourBreakTime = 0;

		for (int serieIndex = 0; serieIndex < timeSerie.length; serieIndex++) {

			final int currentTime = timeSerie[serieIndex];
			final int currentDistance = distanceSerie[serieIndex];

			final int timeDiffSlice = currentTime - lastTime;
			final int distDiffSlice = currentDistance - lastDistance;

			final double sliceSpeed = timeDiffSlice == 0 ? 0 : distDiffSlice * 3.6 / timeDiffSlice;
			final double avgSpeed = speedSerie[serieIndex] / 10.0;

			if ((avgSpeed < minAvgSpeed || sliceSpeed < minSliceSpeed) && timeDiffSlice >= minSliceTime) {

				// current time slice is also a break

				breakTimeSerie[serieIndex] = true;
				tourBreakTime += timeDiffSlice;
			}

			lastTime = currentTime;
			lastDistance = currentDistance;
		}

		return new BreakTimeResult(breakTimeSerie, tourBreakTime);
	}

	public static BreakTimeResult computeBreakTimeBySpeed(	final TourData tourData,
															final String breakMethodId,
															final float minSpeed) {

		final int[] timeSerie = tourData.timeSerie;
		final int[] distanceSerie = tourData.getMetricDistanceSerie();

		final boolean[] breakTimeSerie = new boolean[timeSerie.length];

		boolean isSliceSpeed;
		int[] speedSerie = null;

		if (breakMethodId.equals(BreakTimeTool.BREAK_TIME_METHOD_BY_SLICE_SPEED)) {

			// slice speed

			isSliceSpeed = true;

		} else {

			// average speed

			isSliceSpeed = false;

			speedSerie = tourData.getSpeedSerieMetric();
		}

		int lastTime = 0;
		int lastDistance = 0;
		int tourBreakTime = 0;

		for (int serieIndex = 0; serieIndex < timeSerie.length; serieIndex++) {

			final int currentTime = timeSerie[serieIndex];
			final int currentDistance = distanceSerie[serieIndex];

			final int timeDiffSlice = currentTime - lastTime;
			final int distDiffSlice = currentDistance - lastDistance;

			float speedToCheck;
			if (isSliceSpeed) {
				speedToCheck = timeDiffSlice == 0 ? //
						0
						: (float) (distDiffSlice * 3.6 / timeDiffSlice);
			} else {
				speedToCheck = (float) (speedSerie[serieIndex] / 10.0);
			}

			if (speedToCheck <= minSpeed) {

				// current time slice is also a break

				breakTimeSerie[serieIndex] = true;
				tourBreakTime += timeDiffSlice;
			}

			lastTime = currentTime;
			lastDistance = currentDistance;
		}

		return new BreakTimeResult(breakTimeSerie, tourBreakTime);
	}

	public static BreakTimeResult computeBreakTimeByTimeDistance(	final TourData tourData,
																	final int shortestBreakTime,
																	final float maxDistance,
																	final int sliceDiff) {

		final int[] timeSerie = tourData.timeSerie;
		final int[] distanceSerie = tourData.getMetricDistanceSerie();

		// slice diff is ignored when it's set to 0
		final boolean isSliceDiff = sliceDiff > 0;
		final int sliceDiffSeconds = sliceDiff * 60;

		final boolean[] breakTimeSerie = new boolean[timeSerie.length];

		int prevTime = 0;
		int prevDistance = 0;

		for (int serieIndex = 0; serieIndex < timeSerie.length; serieIndex++) {

			final int currentTime = timeSerie[serieIndex];
			final int currentDistance = distanceSerie[serieIndex];

			final int sliceTimeDiff = currentTime - prevTime;
			final int sliceDistDiff = currentDistance - prevDistance;

//			if (serieIndex == 736) {
//				final int a = 0;
//			}

			if (sliceTimeDiff > shortestBreakTime && sliceDistDiff < maxDistance) {

				/*
				 * this is the simplest case for a break, a slice is larger than the shortest break
				 * time
				 */

				breakTimeSerie[serieIndex] = true;

			} else if (isSliceDiff && sliceTimeDiff > sliceDiffSeconds) {

				/*
				 * distance is ignored
				 */

				breakTimeSerie[serieIndex] = true;

			} else {

				/*
				 * go back in the data serie to find all time slices which are within the shortest
				 * break time
				 */
				int prevIndex = serieIndex;
				int timeDiffPrevSlices = 0;
				int distDiffPrevSlices = 0;

				while (timeDiffPrevSlices <= shortestBreakTime) {

					prevIndex--;

					// check bounds
					if ((prevIndex < 0)) {
						break;
					}

					timeDiffPrevSlices = currentTime - timeSerie[prevIndex];
					distDiffPrevSlices = currentDistance - distanceSerie[prevIndex];
				}

				if (timeDiffPrevSlices > shortestBreakTime && distDiffPrevSlices < maxDistance) {

					// current time slice is a break, set break also in previous break time slices

					for (int breakIndex = prevIndex; breakIndex <= serieIndex; breakIndex++) {
						breakTimeSerie[breakIndex] = true;
					}
				}
			}

			prevTime = currentTime;
			prevDistance = currentDistance;
		}

		/*
		 * get breaktime for the whole tour
		 */
		int tourBreakTime = 0;
		prevTime = 0;

		for (int serieIndex = 0; serieIndex < timeSerie.length; serieIndex++) {

			final int currentTime = timeSerie[serieIndex];

			if (breakTimeSerie[serieIndex]) {
				tourBreakTime += currentTime - prevTime;
			}

			prevTime = currentTime;
		}

		return new BreakTimeResult(breakTimeSerie, tourBreakTime);
	}

	/**
	 * @return Returns values from the pref store for computing the break time.
	 */
	public static BreakTimeTool getPrefValues() {

		checkPrefValues();

		return new BreakTimeTool(
				_prefBreakTimeMethodId,
				_prefShortestTime,
				_prefMaxDistance,
				_prefMinSliceSpeed,
				_prefMinAvgSpeed,
				_prefSliceDiff,
				_prefMinAvgSpeedAS,
				_prefMinSliceSpeedAS,
				_prefMinSliceTimeAS);
	}

	private static void updatePrefValues() {

		_prefBreakTimeMethodId = _prefStore.getString(ITourbookPreferences.BREAK_TIME_METHOD2);

		_prefMinAvgSpeedAS = _prefStore.getDouble(ITourbookPreferences.BREAK_TIME_MIN_AVG_SPEED_AS);
		_prefMinSliceSpeedAS = _prefStore.getDouble(ITourbookPreferences.BREAK_TIME_MIN_SLICE_SPEED_AS);
		_prefMinSliceTimeAS = _prefStore.getInt(ITourbookPreferences.BREAK_TIME_MIN_SLICE_TIME_AS);

		_prefMinAvgSpeed = _prefStore.getFloat(ITourbookPreferences.BREAK_TIME_MIN_AVG_SPEED);
		_prefMinSliceSpeed = _prefStore.getFloat(ITourbookPreferences.BREAK_TIME_MIN_SLICE_SPEED);

		_prefShortestTime = _prefStore.getInt(ITourbookPreferences.BREAK_TIME_SHORTEST_TIME);
		_prefMaxDistance = _prefStore.getFloat(ITourbookPreferences.BREAK_TIME_MAX_DISTANCE);
		_prefSliceDiff = _prefStore.getInt(ITourbookPreferences.BREAK_TIME_SLICE_DIFF);
	}

}
