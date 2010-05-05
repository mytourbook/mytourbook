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

	private static final String			STATE_JOINED_TIME	= "JoinedTime";			//$NON-NLS-1$

	private final IDialogSettings		_state				= TourbookPlugin.getDefault().getDialogSettingsSection(
																	"DialogJoinTours"); //$NON-NLS-1$

	private final ArrayList<TourData>	_selectedTours;

	/*
	 * UI controls
	 */
	private DateTime					_dtTourDate;
	private DateTime					_dtTourTime;

	private Button						_rdoKeepOriginalTime;
	private Button						_rdoPreserveTimeDistance;
	private Button						_rdoConcatenateTime;

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

				_rdoPreserveTimeDistance = new Button(groupTourTime, SWT.RADIO);
				_rdoPreserveTimeDistance.setText(Messages.Dialog_JoinTours_Radio_PreserveTourTimeDiff);
				_rdoPreserveTimeDistance.addSelectionListener(selectionAdapter);

				_rdoConcatenateTime = new Button(groupTourTime, SWT.RADIO);
				_rdoConcatenateTime.setText(Messages.Dialog_JoinTours_Radio_ConcatenateTime);
				_rdoConcatenateTime.addSelectionListener(selectionAdapter);
			}

			/*
			 * tour start: date
			 */
			label = new Label(container, SWT.NONE);
			label.setText(Messages.Dialog_JoinTours_Label_TourDate);

			final Composite dateContainer = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(false, false).span(2, 1).applyTo(dateContainer);
			GridLayoutFactory.fillDefaults().numColumns(3).applyTo(dateContainer);
			{
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

//		final StringBuilder sbTitle = new StringBuilder();
//		final StringBuilder sbJoinedTours = new StringBuilder();
		final StringBuilder sbDescription = new StringBuilder();

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
			 * title/description
			 */
			final String tourDescription = tourData.getTourDescription();

			if (sbDescription.length() > 0) {
				// set space between two tours
				sbDescription.append(UI.NEW_LINE2);
			}

			sbDescription.append(Messages.Dialog_JoinTours_Label_Tour);
			sbDescription.append(TourManager.getTourTitleDetailed(tourData));
			if (tourDescription.length() > 0) {
				sbDescription.append(UI.NEW_LINE);
				sbDescription.append(tourDescription);
			}

			// next tour
			serieIndex++;
			tourSerieIndexOffset = serieIndex;
			isFirstTour = false;
		}

		/*
		 * setup tour data
		 */
		final TourData tourData = new TourData();

		tourData.setTourTitle(Messages.Dialog_JoinTours_TourTitle);
		tourData.setTourDescription(sbDescription.toString());

		System.out.println(tourData.getTourDescription());
		System.out.println();
		System.out.println();
		// TODO remove SYSTEM.OUT.PRINTLN

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

	@Override
	protected void okPressed() {

		saveState();
		doJoinTours();

		super.okPressed();
	}

	private void onDispose() {

	}

	private void restoreState() {

		final int joinedTime = Util.getStateInt(_state, STATE_JOINED_TIME, 0);
		_rdoKeepOriginalTime.setSelection(joinedTime == 0);
		_rdoPreserveTimeDistance.setSelection(joinedTime == 1);
		_rdoConcatenateTime.setSelection(joinedTime == 2);
	}

	private void saveState() {

		_state.put(STATE_JOINED_TIME, //
				_rdoKeepOriginalTime.getSelection() //
						? 0
						: _rdoPreserveTimeDistance.getSelection() //
								? 1
								: 2);
	}

	private void updateUI() {

		final TourData firstTour = _selectedTours.get(0);

//		org.joda.time.DateTime dtTourStart = new org.joda.time.DateTime(
//				firstTour.getStartYear(),
//				firstTour.getStartMonth(),
//				firstTour.getStartDay(),
//				firstTour.getStartHour(),
//				firstTour.getStartMinute(),
//				firstTour.getStartSecond(),
//				0);

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
