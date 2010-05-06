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
import net.tourbook.chart.ChartLabel;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourType;
import net.tourbook.data.TourWayPoint;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.ui.UI;
import net.tourbook.util.Util;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.Bullet;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GlyphMetrics;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class DialogJoinTours extends TitleAreaDialog {

	private static final String			STATE_JOINED_TIME			= "JoinedTime";										//$NON-NLS-1$
	private static final String			STATE_CREATE_TOUR_MARKER	= "CreateTourMarker";									//$NON-NLS-1$

	private final IDialogSettings		_state						= TourbookPlugin
																			.getDefault()
																			.getDialogSettingsSection("DialogJoinTours");	//$NON-NLS-1$

	private final ArrayList<TourData>	_selectedTours;

	/*
	 * UI controls
	 */
	private DateTime					_dtTourDate;
	private DateTime					_dtTourTime;

	private Button						_rdoKeepOriginalTime;
	private Button						_rdoConcatenateTime;

	private Button						_chkCreateTourMarker;

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

		createUI(dlgContainer);
		updateUI();

		restoreState();
		enableControls();

		return dlgContainer;
	}

	private void createUI(final Composite parent) {

		Label label;

		final SelectionAdapter selectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				enableControls();
			}
		};

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			/*
			 * keep tour time
			 */
			final Group groupTourTime = new Group(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).span(3, 1).applyTo(groupTourTime);
			groupTourTime.setText(Messages.Dialog_JoinTours_Group_JoinedTourTime);
			GridLayoutFactory.swtDefaults().applyTo(groupTourTime);
			{
				_rdoKeepOriginalTime = new Button(groupTourTime, SWT.RADIO);
				_rdoKeepOriginalTime.setText(Messages.Dialog_JoinTours_Radio_KeepTime);
				_rdoKeepOriginalTime.addSelectionListener(selectionAdapter);

				_rdoConcatenateTime = new Button(groupTourTime, SWT.RADIO);
				_rdoConcatenateTime.setText(Messages.Dialog_JoinTours_Radio_ConcatenateTime);
				_rdoConcatenateTime.addSelectionListener(selectionAdapter);

				final Composite dateContainer = new Composite(groupTourTime, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.grab(false, false)
						.indent(16, 0)
//						.span(2, 1)
						.applyTo(dateContainer);
				GridLayoutFactory.fillDefaults().numColumns(4).applyTo(dateContainer);
				{
					/*
					 * tour start: date
					 */
					label = new Label(dateContainer, SWT.NONE);
					label.setText(Messages.Dialog_JoinTours_Label_TourDate);

					_dtTourDate = new DateTime(dateContainer, SWT.DATE | SWT.DROP_DOWN | SWT.BORDER);
					GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).applyTo(_dtTourDate);

					/*
					 * tour start: time
					 */
					label = new Label(dateContainer, SWT.NONE);
					GridDataFactory.fillDefaults().indent(20, 0).align(SWT.FILL, SWT.CENTER).applyTo(label);
					label.setText(Messages.Dialog_JoinTours_Label_TourTime);

					_dtTourTime = new DateTime(dateContainer, SWT.TIME | SWT.DROP_DOWN | SWT.BORDER);
					GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).applyTo(_dtTourTime);
				}
			}

			/*
			 * checkbox: set marker for each tour
			 */
			_chkCreateTourMarker = new Button(container, SWT.CHECK);
			GridDataFactory.fillDefaults().span(3, 1).applyTo(_chkCreateTourMarker);
			_chkCreateTourMarker.setText(Messages.Dialog_JoinTours_Checkbox_CreateTourMarker);

			/*
			 * info
			 */
			label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).indent(0, 10).applyTo(label);
			label.setText(Messages.Dialog_JoinTours_Label_OtherFields);

			// use a bulleted list to display this info
			final StyleRange style = new StyleRange();
			style.metrics = new GlyphMetrics(0, 0, 10);
			final Bullet bullet = new Bullet(style);

			final String infoText = Messages.Dialog_JoinTours_Label_OtherFieldsInfo;
			final int lineCount = Util.countCharacter(infoText, '\n');

			final StyledText styledText = new StyledText(container, SWT.READ_ONLY);
			GridDataFactory.fillDefaults()//¨
					.align(SWT.FILL, SWT.BEGINNING)
					.indent(0, 10)
					.span(2, 1)
					.applyTo(styledText);
			styledText.setText(infoText);
			styledText.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
			styledText.setLineBullet(0, lineCount + 1, bullet);

		}

//		container.layout(true);
	}

	private void enableControls() {

		final boolean isOriginalTime = _rdoKeepOriginalTime.getSelection();

		_dtTourDate.setEnabled(isOriginalTime == false);
		_dtTourTime.setEnabled(isOriginalTime == false);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		// keep window size and position
//		return _state;
		return null;
	}

	/**
	 * Join the tours and create a new tour
	 */
	private void joinTours() {

		final boolean isOriginalTime = _rdoKeepOriginalTime.getSelection();

		/**
		 * number of slices, time series are already checked in ActionJoinTours
		 */
		int joinedSliceCounter = 0;

		for (final TourData tourData : _selectedTours) {

			final int[] tourTimeSerie = tourData.timeSerie;
			final int[] tourDistanceSerie = tourData.distanceSerie;
			final double[] tourLatitudeSerie = tourData.latitudeSerie;

			final boolean isTourTime = (tourTimeSerie != null) && (tourTimeSerie.length > 0);
			final boolean isTourDistance = (tourDistanceSerie != null) && (tourDistanceSerie.length > 0);
			final boolean isTourLat = (tourLatitudeSerie != null) && (tourLatitudeSerie.length > 0);

			if (isTourTime) {
				joinedSliceCounter += tourTimeSerie.length;
			} else if (isTourDistance) {
				joinedSliceCounter += tourDistanceSerie.length;
			} else if (isTourLat) {
				joinedSliceCounter += tourLatitudeSerie.length;
			}
		}

		boolean isJoinAltitude = false;
		boolean isJoinDistance = false;
		boolean isJoinCadence = false;
		boolean isJoinLat = false;
		boolean isJoinLon = false;
		boolean isJoinPower = false;
		boolean isJoinPulse = false;
		boolean isJoinSpeed = false;
		boolean isJoinTemperature = false;
		boolean isJoinTime = false;

		final int[] joinedAltitudeSerie = new int[joinedSliceCounter];
		final int[] joinedCadenceSerie = new int[joinedSliceCounter];
		final int[] joinedDistanceSerie = new int[joinedSliceCounter];
		final double[] joinedLatitudeSerie = new double[joinedSliceCounter];
		final double[] joinedLongitudeSerie = new double[joinedSliceCounter];
		final int[] joinedPowerSerie = new int[joinedSliceCounter];
		final int[] joinedPulseSerie = new int[joinedSliceCounter];
		final int[] joinedSpeedSerie = new int[joinedSliceCounter];
		final int[] joinedTemperatureSerie = new int[joinedSliceCounter];
		final int[] joinedTimeSerie = new int[joinedSliceCounter];

		final TourData joinedTourData = new TourData();

		int joinedCalories = 0;
		boolean isJoinedDistanceFromSensor = false;
		TourType joinedTourType = null;
		short joinedDeviceTimeInterval = -1;
		final HashSet<TourMarker> joinedTourMarker = new HashSet<TourMarker>();
		final ArrayList<TourWayPoint> joinedWayPoints = new ArrayList<TourWayPoint>();
		final StringBuilder joinedDescription = new StringBuilder();

		int joinedSerieIndex = 0;
		int joinedTourStartIndex = 0;

		int joinedTourStartDistance = 0;

		int relTourTime = 0;
		long relTourTimeOffset = 0;
		long absFirstTourStartTimeSec = 0;
		long absJoinedTourStartTimeSec = 0;
		org.joda.time.DateTime joinedTourStart = null;

		boolean isFirstTour = true;

		/*
		 * copy tour data series into joined data series
		 */
		for (final TourData tourTourData : _selectedTours) {

			final int[] tourAltitudeSerie = tourTourData.altitudeSerie;
			final int[] tourCadenceSerie = tourTourData.cadenceSerie;
			final int[] tourDistanceSerie = tourTourData.distanceSerie;
			final double[] tourLatitudeSerie = tourTourData.latitudeSerie;
			final double[] tourLongitudeSerie = tourTourData.longitudeSerie;
			final int[] tourPulseSerie = tourTourData.pulseSerie;
			final int[] tourTemperatureSerie = tourTourData.temperatureSerie;
			final int[] tourTimeSerie = tourTourData.timeSerie;

			final boolean isTourAltitude = (tourAltitudeSerie != null) && (tourAltitudeSerie.length > 0);
			final boolean isTourCadence = (tourCadenceSerie != null) && (tourCadenceSerie.length > 0);
			final boolean isTourDistance = (tourDistanceSerie != null) && (tourDistanceSerie.length > 0);
			final boolean isTourLat = (tourLatitudeSerie != null) && (tourLatitudeSerie.length > 0);
			final boolean isTourLon = (tourLongitudeSerie != null) && (tourLongitudeSerie.length > 0);
			final boolean isTourPulse = (tourPulseSerie != null) && (tourPulseSerie.length > 0);
			final boolean isTourTemperature = (tourTemperatureSerie != null) && (tourTemperatureSerie.length > 0);
			final boolean isTourTime = (tourTimeSerie != null) && (tourTimeSerie.length > 0);

			/*
			 * get speed/power data when it's from the device
			 */
			int[] tourPowerSerie = null;
			int[] tourSpeedSerie = null;
			final boolean isTourPower = tourTourData.isPowerSerieFromDevice();
			final boolean isTourSpeed = tourTourData.isSpeedSerieFromDevice();
			if (isTourPower) {
				tourPowerSerie = tourTourData.getPowerSerie();
			}
			if (isTourSpeed) {
				tourSpeedSerie = tourTourData.getSpeedSerie();
			}

			/*
			 * set tour time
			 */
			if (isFirstTour) {

				// get start date/time

				if (isOriginalTime) {

					joinedTourStart = new org.joda.time.DateTime(
							tourTourData.getStartYear(),
							tourTourData.getStartMonth(),
							tourTourData.getStartDay(),
							tourTourData.getStartHour(),
							tourTourData.getStartMinute(),
							tourTourData.getStartSecond(),
							0);

				} else {

					joinedTourStart = new org.joda.time.DateTime(
							_dtTourDate.getYear(),
							_dtTourDate.getMonth() + 1,
							_dtTourDate.getDay(),
							_dtTourTime.getHours(),
							_dtTourTime.getMinutes(),
							_dtTourTime.getSeconds(),
							0);
				}

				// tour start in absolute seconds
				absJoinedTourStartTimeSec = joinedTourStart.getMillis() / 1000;
				absFirstTourStartTimeSec = absJoinedTourStartTimeSec;

			} else {

				// get relative time offset

				if (isOriginalTime) {

					final org.joda.time.DateTime tourStart = new org.joda.time.DateTime(
							tourTourData.getStartYear(),
							tourTourData.getStartMonth(),
							tourTourData.getStartDay(),
							tourTourData.getStartHour(),
							tourTourData.getStartMinute(),
							tourTourData.getStartSecond(),
							0);

					final long absTourStartTimeSec = tourStart.getMillis() / 1000;

					// keep original time
					relTourTimeOffset = absTourStartTimeSec - absFirstTourStartTimeSec;

				} else {

					/*
					 * remove time gaps between tours, add relative time from the last tour and add
					 * 1 second for the start of the next tour
					 */
					relTourTimeOffset += relTourTime + 1;
				}
			}

			/*
			 * get number of slices
			 */
			int tourSliceCounter = 0;
			if (isTourTime) {
				tourSliceCounter = tourTimeSerie.length;
			} else if (isTourDistance) {
				tourSliceCounter = tourDistanceSerie.length;
			} else if (isTourLat) {
				tourSliceCounter = tourLatitudeSerie.length;
			}

			int relTourDistance = 0;

			/*
			 * copy data series
			 */
			for (int tourSerieIndex = 0; tourSerieIndex < tourSliceCounter; tourSerieIndex++) {

				if (isTourTime) {

					relTourTime = tourTimeSerie[tourSerieIndex];

					joinedTimeSerie[joinedSerieIndex] = (int) (relTourTimeOffset + relTourTime);

					isJoinTime = true;
				}

				if (isTourAltitude) {
					joinedAltitudeSerie[joinedSerieIndex] = tourAltitudeSerie[tourSerieIndex];
					isJoinAltitude = true;
				}
				if (isTourCadence) {
					joinedCadenceSerie[joinedSerieIndex] = tourCadenceSerie[tourSerieIndex];
					isJoinCadence = true;
				}

				if (isTourDistance) {

					relTourDistance = tourDistanceSerie[tourSerieIndex];

					joinedDistanceSerie[joinedSerieIndex] = joinedTourStartDistance + relTourDistance;
					isJoinDistance = true;
				}

				if (isTourPulse) {
					joinedPulseSerie[joinedSerieIndex] = tourPulseSerie[tourSerieIndex];
					isJoinPulse = true;
				}
				if (isTourLat) {
					joinedLatitudeSerie[joinedSerieIndex] = tourLatitudeSerie[tourSerieIndex];
					isJoinLat = true;
				}
				if (isTourLon) {
					joinedLongitudeSerie[joinedSerieIndex] = tourLongitudeSerie[tourSerieIndex];
					isJoinLon = true;
				}
				if (isTourTemperature) {
					joinedTemperatureSerie[joinedSerieIndex] = tourTemperatureSerie[tourSerieIndex];
					isJoinTemperature = true;
				}
				if (isTourPower) {
					joinedPowerSerie[joinedSerieIndex] = tourPowerSerie[tourSerieIndex];
					isJoinPower = true;
				}
				if (isTourSpeed) {
					joinedSpeedSerie[joinedSerieIndex] = tourSpeedSerie[tourSerieIndex];
					isJoinSpeed = true;
				}

				joinedSerieIndex++;
			}

			/*
			 * copy tour markers
			 */
			final Set<TourMarker> tourMarkers = tourTourData.getTourMarkers();
			for (final TourMarker tourMarker : tourMarkers) {

				final TourMarker clonedMarker = tourMarker.clone(joinedTourData);

				int joinMarkerIndex = joinedTourStartIndex + clonedMarker.getSerieIndex();
				if (joinMarkerIndex >= joinedSliceCounter) {
					joinMarkerIndex = joinedSliceCounter - 1;
				}

				// a cloned marker has the same marker id, create a new id
				clonedMarker.createMarkerId();

				// adjust marker position, position is relativ to the tour start
				clonedMarker.setSerieIndex(joinMarkerIndex);

				if (isJoinTime) {
					tourMarker.setTime(joinedTimeSerie[joinMarkerIndex]);
				}
				if (isJoinDistance) {
					tourMarker.setDistance(joinedDistanceSerie[joinMarkerIndex]);
				}

				joinedTourMarker.add(clonedMarker);
			}

			/*
			 * create tour marker
			 */
			// first find a free marker position in the tour
			int tourMarkerIndex = -1;
			for (int tourSerieIndex = 0; tourSerieIndex < tourSliceCounter; tourSerieIndex++) {

				boolean isIndexAvailable = true;

				// check if a marker occupies the current index
				for (final TourMarker tourMarker : tourMarkers) {
					if (tourMarker.getSerieIndex() == tourSerieIndex) {
						isIndexAvailable = false;
						break;
					}
				}

				if (isIndexAvailable) {
					// free index was found
					tourMarkerIndex = tourSerieIndex;
					break;
				}
			}

			if (tourMarkerIndex != -1) {

				final int joinMarkerIndex = joinedTourStartIndex + tourMarkerIndex;

				final TourMarker tourMarker = new TourMarker(joinedTourData, ChartLabel.MARKER_TYPE_CUSTOM);

				tourMarker.setSerieIndex(joinMarkerIndex);
				tourMarker.setLabel(TourManager.getTourDateFull(tourTourData));
				tourMarker.setVisualPosition(ChartLabel.VISUAL_VERTICAL_ABOVE_GRAPH);

				if (isJoinTime) {
					tourMarker.setTime(joinedTimeSerie[joinMarkerIndex]);
				}
				if (isJoinDistance) {
					tourMarker.setDistance(joinedDistanceSerie[joinMarkerIndex]);
				}

				joinedTourMarker.add(tourMarker);
			}

			/*
			 * copy way points
			 */
			for (final TourWayPoint wayPoint : tourTourData.getTourWayPoints()) {
				joinedWayPoints.add((TourWayPoint) wayPoint.clone());
			}

			/*
			 * create title/description
			 */
			final String tourDescription = tourTourData.getTourDescription();

			if (joinedDescription.length() > 0) {
				// set space between two tours
				joinedDescription.append(UI.NEW_LINE2);
			}

			joinedDescription.append(Messages.Dialog_JoinTours_Label_Tour);
			joinedDescription.append(TourManager.getTourTitleDetailed(tourTourData));
			if (tourDescription.length() > 0) {
				joinedDescription.append(UI.NEW_LINE);
				joinedDescription.append(tourDescription);
			}

			/*
			 * other tour values
			 */
			if (isFirstTour) {
				isJoinedDistanceFromSensor = tourTourData.getIsDistanceFromSensor();
				joinedDeviceTimeInterval = tourTourData.getDeviceTimeInterval();
			} else {
				if (isJoinedDistanceFromSensor && tourTourData.getIsDistanceFromSensor()) {
					// keep TRUE state
				} else {
					isJoinedDistanceFromSensor = false;
				}
				if (joinedDeviceTimeInterval == tourTourData.getDeviceTimeInterval()) {
					// keep value
				} else {
					joinedDeviceTimeInterval = -1;
				}
			}
			if (joinedTourType == null) {
				joinedTourType = tourTourData.getTourType();
			}

			/*
			 * summarize other fields
			 */
			joinedCalories += tourTourData.getCalories();

			/*
			 * init next tour
			 */
			isFirstTour = false;
			joinedTourStartIndex = joinedSerieIndex;
			joinedTourStartDistance += relTourDistance;
		}

		/*
		 * setup tour data
		 */
		joinedTourData.setTourTitle(Messages.Dialog_JoinTours_TourTitle);
		joinedTourData.setTourDescription(joinedDescription.toString());
		joinedTourData.setTourMarkers(joinedTourMarker);
		joinedTourData.setWayPoints(joinedWayPoints);

		joinedTourData.setStartHour((short) joinedTourStart.getHourOfDay());
		joinedTourData.setStartMinute((short) joinedTourStart.getMinuteOfHour());
		joinedTourData.setStartSecond((short) joinedTourStart.getSecondOfMinute());
		joinedTourData.setStartYear((short) joinedTourStart.getYear());
		joinedTourData.setStartMonth((short) joinedTourStart.getMonthOfYear());
		joinedTourData.setStartDay((short) joinedTourStart.getDayOfMonth());

		joinedTourData.setWeek(joinedTourStart);

		joinedTourData.setIsDistanceFromSensor(isJoinedDistanceFromSensor);
		joinedTourData.setDeviceTimeInterval(joinedDeviceTimeInterval);
		joinedTourData.setCalories(joinedCalories);

		if (isJoinAltitude) {
			joinedTourData.altitudeSerie = joinedAltitudeSerie;
		}
		if (isJoinDistance) {
			joinedTourData.distanceSerie = joinedDistanceSerie;
		}
		if (isJoinCadence) {
			joinedTourData.cadenceSerie = joinedCadenceSerie;
		}
		if (isJoinLat) {
			joinedTourData.latitudeSerie = joinedLatitudeSerie;
		}
		if (isJoinLon) {
			joinedTourData.longitudeSerie = joinedLongitudeSerie;
		}
		if (isJoinPower) {
			joinedTourData.setPowerSerie(joinedPowerSerie);
		}
		if (isJoinPulse) {
			joinedTourData.pulseSerie = joinedPulseSerie;
		}
		if (isJoinSpeed) {
			joinedTourData.setSpeedSerie(joinedSpeedSerie);
		}
		if (isJoinTemperature) {
			joinedTourData.temperatureSerie = joinedTemperatureSerie;
		}
		if (isJoinTime) {
			joinedTourData.timeSerie = joinedTimeSerie;
		}

		joinedTourData.computeAltitudeUpDown();
		joinedTourData.computeTourDrivingTime();
		joinedTourData.computeComputedValues();

		joinedTourData.setDeviceName(Messages.Dialog_JoinTours_Label_DeviceName);

		// tour id must be created after the tour date/time is set
		joinedTourData.createTourId();

		joinedTourData.setTourType(joinedTourType);

		// set person which is required to save a tour
		joinedTourData.setTourPerson(TourManager.getInstance().getActivePerson());

		TourManager.saveModifiedTour(joinedTourData);
	}

	@Override
	protected void okPressed() {

		saveState();
		joinTours();

		super.okPressed();
	}

	private void onDispose() {

	}

	private void restoreState() {

		final int joinedTime = Util.getStateInt(_state, STATE_JOINED_TIME, 0);
		_rdoKeepOriginalTime.setSelection(joinedTime == 0);
		_rdoConcatenateTime.setSelection(joinedTime == 1);

		_chkCreateTourMarker.setSelection(Util.getStateBoolean(_state, STATE_CREATE_TOUR_MARKER, true));
	}

	private void saveState() {

		final int selectedTimeJoining = _rdoKeepOriginalTime.getSelection() //
				? 0
				: _rdoConcatenateTime.getSelection() //
						? 1
						: 0;

		_state.put(STATE_JOINED_TIME, selectedTimeJoining);
		_state.put(STATE_CREATE_TOUR_MARKER, _chkCreateTourMarker.getSelection());
	}

	private void updateUI() {

		final TourData firstTour = _selectedTours.get(0);

		_dtTourDate.setDate(firstTour.getStartYear(), firstTour.getStartMonth() - 1, firstTour.getStartDay());
		_dtTourTime.setTime(firstTour.getStartHour(), firstTour.getStartMinute(), firstTour.getStartSecond());
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
