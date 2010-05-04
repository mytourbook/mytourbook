/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.ui.UI;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class DialogJoinTours extends TitleAreaDialog {

	private final IDialogSettings		_state	= TourbookPlugin.getDefault().getDialogSettingsSection(
														"DialogJoinTours"); //$NON-NLS-1$

	private final ArrayList<TourData>	_selectedTours;

	public DialogJoinTours(final Shell parentShell, final ArrayList<TourData> selectedTours) {

		super(parentShell);

		_selectedTours = selectedTours;
	}

	@Override
	protected void configureShell(final Shell shell) {

		super.configureShell(shell);

		shell.setText(Messages.Dialog_JoinTours_DlgArea_Title);

		shell.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
			}
		});
	}

	@Override
	public void create() {

		super.create();

		setTitle(Messages.Dialog_JoinTours_DlgArea_Title);
		setMessage(Messages.Dialog_JoinTours_DlgArea_Message);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		final Composite dlgContainer = (Composite) super.createDialogArea(parent);

		// create ui
		createUI(dlgContainer);

		return dlgContainer;
	}

	private void createUI(final Composite parent) {

		final Label label = new Label(parent, SWT.NONE);
		label.setText("Tour title/description will be set from all tours."); //$NON-NLS-1$
		label.setText("Tour type will be set from the first tour."); //$NON-NLS-1$

	}

	/**
	 * 
	 */
	private void doJoinTours() {

		/*
		 * get number of slices
		 */
		int joinSliceCounter = 0;

		for (final TourData tourData : _selectedTours) {

			final int[] tourTimeSerie = tourData.timeSerie;
			final int[] tourDistanceSerie = tourData.distanceSerie;
			final double[] tourLatitudeSerie = tourData.latitudeSerie;

			final boolean isTourTime = (tourTimeSerie != null) && (tourTimeSerie.length > 0);
			final boolean isTourDistance = (tourDistanceSerie != null) && (tourDistanceSerie.length > 0);
			final boolean isTourLat = (tourLatitudeSerie != null) && (tourLatitudeSerie.length > 0);

			if (isTourTime) {
				joinSliceCounter += tourTimeSerie.length;
			} else if (isTourDistance) {
				joinSliceCounter += tourDistanceSerie.length;
			} else if (isTourLat) {
				joinSliceCounter += tourLatitudeSerie.length;
			}
		}

		boolean isJoinTime = false;
		boolean isJoinAltitude = false;
		boolean isJoinDistance = false;
		boolean isJoinCadence = false;
		boolean isJoinPulse = false;
		boolean isJoinLat = false;
		boolean isJoinLon = false;

		final int[] joinTimeSerie = new int[joinSliceCounter];
		final int[] joinAltitudeSerie = new int[joinSliceCounter];
		final int[] joinCadenceSerie = new int[joinSliceCounter];
		final int[] joinDistanceSerie = new int[joinSliceCounter];
		final int[] joinPulseSerie = new int[joinSliceCounter];
		final double[] joinLatitudeSerie = new double[joinSliceCounter];
		final double[] joinLongitudeSerie = new double[joinSliceCounter];

		final StringBuilder sbTitle = new StringBuilder();
		final StringBuilder sbDescription = new StringBuilder();
		final StringBuilder sbJoinedTours = new StringBuilder();

		final HashSet<TourMarker> joinedTourMarker = new HashSet<TourMarker>();
		boolean isJoinedDistanceFromSensor = false;
		boolean isFirstTour = false;

		int serieIndex = 0;
		int tourSerieIndexOffset = 0;

		/*
		 * copy tour data series into joined data series
		 */
		for (final TourData tourData : _selectedTours) {

			final int[] tourTimeSerie = tourData.timeSerie;
			final int[] tourAltitudeSerie = tourData.altitudeSerie;
			final int[] tourCadenceSerie = tourData.cadenceSerie;
			final int[] tourDistanceSerie = tourData.distanceSerie;
			final int[] tourPulseSerie = tourData.pulseSerie;
			final double[] tourLatitudeSerie = tourData.latitudeSerie;
			final double[] tourLongitudeSerie = tourData.longitudeSerie;

			final boolean isTourTime = (tourTimeSerie != null) && (tourTimeSerie.length > 0);
			final boolean isTourAltitude = (tourAltitudeSerie != null) && (tourAltitudeSerie.length > 0);
			final boolean isTourCadence = (tourCadenceSerie != null) && (tourCadenceSerie.length > 0);
			final boolean isTourDistance = (tourDistanceSerie != null) && (tourDistanceSerie.length > 0);
			final boolean isTourPulse = (tourPulseSerie != null) && (tourPulseSerie.length > 0);
			final boolean isTourLat = (tourLatitudeSerie != null) && (tourLatitudeSerie.length > 0);
			final boolean isTourLon = (tourLongitudeSerie != null) && (tourLongitudeSerie.length > 0);

			int tourSliceCounter = 0;
			if (isTourTime) {
				tourSliceCounter = tourTimeSerie.length;
			} else if (isTourDistance) {
				tourSliceCounter = tourDistanceSerie.length;
			} else if (isTourLat) {
				tourSliceCounter = tourLatitudeSerie.length;
			}

			/*
			 * copy time slices
			 */
			for (int tourSerieIndex = 0; tourSerieIndex < tourSliceCounter; tourSerieIndex++) {

				if (isTourTime) {
					joinTimeSerie[serieIndex] = tourTimeSerie[tourSerieIndex];
					isJoinTime = true;
				}
				if (isTourAltitude) {
					joinAltitudeSerie[serieIndex] = tourAltitudeSerie[tourSerieIndex];
					isJoinAltitude = true;
				}
				if (isTourCadence) {
					joinCadenceSerie[serieIndex] = tourCadenceSerie[tourSerieIndex];
					isJoinCadence = true;
				}
				if (isTourDistance) {
					joinDistanceSerie[serieIndex] = tourDistanceSerie[tourSerieIndex];
					isJoinDistance = true;
				}
				if (isTourPulse) {
					joinPulseSerie[serieIndex] = tourPulseSerie[tourSerieIndex];
					isJoinPulse = true;
				}
				if (isTourLat) {
					joinLatitudeSerie[serieIndex] = tourLatitudeSerie[tourSerieIndex];
					isJoinLat = true;
				}
				if (isTourLon) {
					joinLongitudeSerie[serieIndex] = tourLongitudeSerie[tourSerieIndex];
					isJoinLon = true;
				}
			}

			/*
			 * copy tour markers
			 */
			final Set<TourMarker> tourMarkers = tourData.getTourMarkers();
			for (final TourMarker tourMarker : tourMarkers) {

				final TourMarker clonedMarker = tourMarker.clone();

				// adjust marker position, position is relativ to the tour start
				clonedMarker.setSerieIndex(clonedMarker.getSerieIndex() + tourSerieIndexOffset);

				// a cloned marker has the same marker id, create a new id
				clonedMarker.setMarkerId();

				joinedTourMarker.add(clonedMarker);
			}

			/*
			 * distance from sensor
			 */
			if (isFirstTour) {
				isJoinedDistanceFromSensor = tourData.getIsDistanceFromSensor();
			} else {
				if (isJoinedDistanceFromSensor && tourData.getIsDistanceFromSensor()) {
					// keep TRUE state
				} else {
					isJoinedDistanceFromSensor = false;
				}
			}

			/*
			 * title
			 */
			final String tourTitle = tourData.getTourTitle();
			if (sbTitle.length() > 0) {
				sbTitle.append(UI.DASH_WITH_SPACE);
			}
			sbTitle.append(tourTitle);

			/*
			 * description
			 */
			if (sbDescription.length() > 0) {
				sbDescription.append(UI.NEW_LINE2);
			}
			sbDescription.append(Messages.Dialog_JoinTours_Label_Tour);
			sbDescription.append(TourManager.getTourDateShort(tourData));
			sbDescription.append(UI.NEW_LINE);
			sbDescription.append(tourData.getTourDescription());

			// next tour
			serieIndex++;
			tourSerieIndexOffset = serieIndex;
			isFirstTour = false;
		}

	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		// keep window size and position
		return _state;
//		return null;
	}

	@Override
	protected void okPressed() {

		doJoinTours();

		super.okPressed();
	}

	private void onDispose() {

	}

//	private void setTourData() {
//
//		// create data object for each tour
//		final TourData tourData = new TourData();
//
//		// set tour notes
//		setTourNotes(tourData);
//
//		/*
//		 * set tour start date/time
//		 */
//
//		/*
//		 * Check if date time starts with the date 2007-04-01, this can happen when the tcx file is
//		 * partly corrupt. When tour starts with the date 2007-04-01, move forward in the list until
//		 * another date occures and use this as the start date.
//		 */
//		int validIndex = 0;
//		DateTime dt = null;
//
//		for (final TimeData timeData : _dtList) {
//
//			dt = new DateTime(timeData.absoluteTime);
//
//			if (dt.getYear() == 2007 && dt.getMonthOfYear() == 4 && dt.getDayOfMonth() == 1) {
//
//				// this is an invalid time slice
//
//				validIndex++;
//				continue;
//
//			} else {
//
//				// this is a valid time slice
//				break;
//			}
//		}
//		if (validIndex == 0) {
//
//			// date is not 2007-04-01
//
//		} else {
//
//			if (validIndex == _dtList.size()) {
//
//				// all time data start with 2007-04-01
//
//				dt = new DateTime(_dtList.get(0).absoluteTime);
//
//			} else {
//
//				// the date starts with 2007-04-01 but it changes to another date
//
//				dt = new DateTime(_dtList.get(validIndex).absoluteTime);
//
//				/*
//				 * create a new list by removing invalid time slices
//				 */
//
//				final ArrayList<TimeData> oldDtList = _dtList;
//				_dtList = new ArrayList<TimeData>();
//
//				int _tdIndex = 0;
//				for (final TimeData timeData : oldDtList) {
//
//					if (_tdIndex < validIndex) {
//						_tdIndex++;
//						continue;
//					}
//
//					_dtList.add(timeData);
//				}
//
//				StatusUtil.showStatus(NLS.bind(//
//						"", //$NON-NLS-1$
//						_importFilePath,
//						dt.toString()));
//			}
//		}
//
//		tourData.setIsDistanceFromSensor(_isDistanceFromSensor);
//
//		tourData.setStartHour((short) dt.getHourOfDay());
//		tourData.setStartMinute((short) dt.getMinuteOfHour());
//		tourData.setStartSecond((short) dt.getSecondOfMinute());
//
//		tourData.setStartYear((short) dt.getYear());
//		tourData.setStartMonth((short) dt.getMonthOfYear());
//		tourData.setStartDay((short) dt.getDayOfMonth());
//
//		tourData.setWeek(dt);
//
//		tourData.setDeviceTimeInterval((short) -1);
//		tourData.importRawDataFile = _importFilePath;
//		tourData.setTourImportFilePath(_importFilePath);
//
//		tourData.createTimeSeries(_dtList, true);
//
//		tourData.computeAltitudeUpDown();
//
//		tourData.setDeviceModeName(_activitySport);
//
//		tourData.setCalories(_calories);
//
//		// after all data are added, the tour id can be created
//		final int[] distanceSerie = tourData.getMetricDistanceSerie();
//		String uniqueKey;
//
//		if (_deviceDataReader.isCreateTourIdWithTime) {
//
//			/*
//			 * 25.5.2009: added recording time to the tour distance for the unique key because tour
//			 * export and import found a wrong tour when exporting was done with camouflage speed ->
//			 * this will result in a NEW tour
//			 */
//			final int tourRecordingTime = tourData.getTourRecordingTime();
//
//			if (distanceSerie == null) {
//				uniqueKey = Integer.toString(tourRecordingTime);
//			} else {
//
//				final long tourDistance = distanceSerie[(distanceSerie.length - 1)];
//
//				uniqueKey = Long.toString(tourDistance + tourRecordingTime);
//			}
//
//		} else {
//
//			/*
//			 * original version to create tour id
//			 */
//			if (distanceSerie == null) {
//				uniqueKey = "42984"; //$NON-NLS-1$
//			} else {
//				uniqueKey = Integer.toString(distanceSerie[distanceSerie.length - 1]);
//			}
//		}
//
//		Long tourId;
//
//		/*
//		 * if (fId != null) { try{ tourId = Long.parseLong(fId); } catch (final
//		 * NumberFormatException e) { tourId = tourData.createTourId(uniqueKey); } } else
//		 */
//		{
//			tourId = tourData.createTourId(uniqueKey);
//		}
//
//		// check if the tour is already imported
//		if (_tourDataMap.containsKey(tourId) == false) {
//
//			tourData.computeTourDrivingTime();
//			tourData.computeComputedValues();
//
//			tourData.setDeviceId(_deviceDataReader.deviceId);
//			tourData.setDeviceName(_deviceDataReader.visibleName);
//
//			// add new tour to other tours
//			_tourDataMap.put(tourId, tourData);
//		}
//
//		_isImported = true;
//	}

}
