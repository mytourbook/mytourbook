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
import net.tourbook.data.TourTag;
import net.tourbook.data.TourType;
import net.tourbook.data.TourWayPoint;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tag.ActionRemoveAllTags;
import net.tourbook.tag.ActionSetTourTag;
import net.tourbook.tag.TagManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.UI;
import net.tourbook.ui.action.ActionOpenPrefDialog;
import net.tourbook.ui.action.ActionSetTourTypeMenu;
import net.tourbook.util.Util;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.Bullet;
import org.eclipse.swt.custom.CLabel;
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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPart;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class DialogJoinTours extends TitleAreaDialog implements ITourProvider {

	private static final String					STATE_TITLE						= "Title";							//$NON-NLS-1$

	private static final String					STATE_IS_CREATE_TOUR_MARKER		= "isCreateTourMarker";			//$NON-NLS-1$
	private static final String					STATE_TOUR_MARKER_TYPE			= "TourMarkerType";				//$NON-NLS-1$
	private static final String					STATE_TOUR_MARKER_TYPE_SMALL	= "small";							//$NON-NLS-1$
	private static final String					STATE_TOUR_MARKER_TYPE_MEDIUM	= "medium";						//$NON-NLS-1$
	private static final String					STATE_TOUR_MARKER_TYPE_LARGE	= "large";							//$NON-NLS-1$

	private static final String					STATE_JOINED_TIME				= "JoinedTime";					//$NON-NLS-1$
	private static final String					STATE_JOINED_TIME_ORIGINAL		= "original";						//$NON-NLS-1$
	private static final String					STATE_JOINED_TIME_CONCATENATED	= "concatenated";					//$NON-NLS-1$

	private final IDialogSettings				_state							= TourbookPlugin
																						.getDefault()
																						.getDialogSettingsSection(
																								"DialogJoinTours"); //$NON-NLS-1$

	private final DateTimeFormatter				_dtFormatterShort				= DateTimeFormat.shortDate();
	private final DateTimeFormatter				_dtFormatterMedium				= DateTimeFormat.shortDateTime();
	private final DateTimeFormatter				_dtFormatterFull				= DateTimeFormat.fullDateTime();

	private ActionSetTourTag					_actionAddTag;
	private ActionSetTourTag					_actionRemoveTag;
	private ActionRemoveAllTags					_actionRemoveAllTags;
	private ActionOpenPrefDialog				_actionOpenTagPrefs;
	private ActionOpenPrefDialog				_actionOpenTourTypePrefs;

	private TourData							_joinedTourData;
	private ArrayList<TourData>					_joinedTourDataList;
	private final TourType						_joinedTourType					= null;
	private final Set<TourTag>					_joinedTourTags					= new HashSet<TourTag>();

	private final ArrayList<TourData>			_selectedTours;

	/*
	 * UI controls
	 */
	private Text								_txtJoinedTitle;
	private org.eclipse.swt.widgets.DateTime	_dtTourDate;
	private org.eclipse.swt.widgets.DateTime	_dtTourTime;

	private Button								_rdoKeepOriginalTime;
	private Button								_rdoConcatenateTime;

	private Button								_chkCreateTourMarker;
	private Button								_rdoTourMarkerShort;
	private Button								_rdoTourMarkerMedium;
	private Button								_rdoTourMarkerLarge;

	private Link								_linkTourType;
	private CLabel								_lblTourType;

	private TourType							_oldTourType;

	private Link								_linkTag;

	private Label								_lblTourTags;

	private ITourEventListener					_tourEventListener;

	private Composite							_innerContainer;

	public DialogJoinTours(final Shell parentShell, final ArrayList<TourData> selectedTours) {

		super(parentShell);

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE);

		_selectedTours = selectedTours;
	}

	private void addTourEventListener() {

		_tourEventListener = new ITourEventListener() {
			@Override
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if ((eventId == TourEventId.TOUR_CHANGED) && (eventData instanceof TourEvent)) {

					final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();
					if ((modifiedTours != null) && (modifiedTours.size() > 0)) {

						updateUI();

						// set layout when tags are added to reflow the layout
						_innerContainer.layout(true);
					}
				}
			}
		};

		TourManager.getInstance().addTourEventListener(_tourEventListener);
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

	private void createActions() {

		_actionAddTag = new ActionSetTourTag(this, true, false);
		_actionRemoveTag = new ActionSetTourTag(this, false, false);
		_actionRemoveAllTags = new ActionRemoveAllTags(this, false);

		_actionOpenTagPrefs = new ActionOpenPrefDialog(
				Messages.action_tag_open_tagging_structure,
				ITourbookPreferences.PREF_PAGE_TAGS);

		_actionOpenTourTypePrefs = new ActionOpenPrefDialog(
				Messages.action_tourType_modify_tourTypes,
				ITourbookPreferences.PREF_PAGE_TOUR_TYPE);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		final Composite dlgContainer = (Composite) super.createDialogArea(parent);

		initTourData();

		createUI(dlgContainer);
		restoreState();

		updateUI();
		enableControls();

		addTourEventListener();

		createActions();
		createMenus();

		return dlgContainer;
	}

	/**
	 * create the drop down menus, this must be created after the parent control is created
	 */
	private void createMenus() {

		/*
		 * tag menu
		 */
		MenuManager menuMgr = new MenuManager();

		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(final IMenuManager menuMgr) {

				final boolean isTagInTour = _joinedTourTags.size() > 0;

				// enable actions
				_actionAddTag.setEnabled(true); // 			// !!! action enablement is overwritten
				_actionRemoveTag.setEnabled(isTagInTour);
				_actionRemoveAllTags.setEnabled(isTagInTour);

				// set menu items
				menuMgr.add(_actionAddTag);
				menuMgr.add(_actionRemoveTag);
				menuMgr.add(_actionRemoveAllTags);

				TagManager.fillRecentTagsIntoMenu(menuMgr, DialogJoinTours.this, true, false);

				menuMgr.add(new Separator());
				menuMgr.add(_actionOpenTagPrefs);
			}
		});

		// set menu for the tag item
		_linkTag.setMenu(menuMgr.createContextMenu(_linkTag));

		/*
		 * tour type menu
		 */
		menuMgr = new MenuManager();

		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(final IMenuManager menuMgr) {

				// set menu items

				ActionSetTourTypeMenu.fillMenu(menuMgr, DialogJoinTours.this, false);

				menuMgr.add(new Separator());
				menuMgr.add(_actionOpenTourTypePrefs);
			}
		});

		// set menu for the tag item
		_linkTourType.setMenu(menuMgr.createContextMenu(_linkTourType));
	}

	private void createUI(final Composite parent) {

		final SelectionAdapter selectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				updateUI();
				enableControls();
			}
		};

		_innerContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_innerContainer);
		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(_innerContainer);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			createUI10Title(_innerContainer);
			createUI20Marker(_innerContainer, selectionAdapter);
			createUI30TypeTags(_innerContainer);
			createUI40TourTime(_innerContainer, selectionAdapter);
			createUI50Info(_innerContainer);
		}
	}

	private void createUI10Title(final Composite parent) {
		Label label;
		/*
		 * tour title
		 */
		label = new Label(parent, SWT.NONE);
		label.setText(Messages.Dialog_JoinTours_Label_Title);
		label.setToolTipText(Messages.Dialog_JoinTours_Label_Title_Tooltip);

		_txtJoinedTitle = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_txtJoinedTitle);
		_txtJoinedTitle.setToolTipText(Messages.Dialog_JoinTours_Label_Title_Tooltip);
	}

	private void createUI20Marker(final Composite parent, final SelectionAdapter selectionAdapter) {
		/*
		 * checkbox: set marker for each tour
		 */
		_chkCreateTourMarker = new Button(parent, SWT.CHECK);
		GridDataFactory.fillDefaults().span(3, 1).applyTo(_chkCreateTourMarker);
		_chkCreateTourMarker.setText(Messages.Dialog_JoinTours_Checkbox_CreateTourMarker);
		_chkCreateTourMarker.addSelectionListener(selectionAdapter);

		final Composite markerContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).span(3, 1).indent(16, 0).applyTo(markerContainer);
		GridLayoutFactory.fillDefaults()//
//					.numColumns(3)
				.applyTo(markerContainer);
		{
			_rdoTourMarkerShort = new Button(markerContainer, SWT.RADIO);
			_rdoTourMarkerShort.setText(UI.EMPTY_STRING);

			_rdoTourMarkerMedium = new Button(markerContainer, SWT.RADIO);
			_rdoTourMarkerMedium.setText(UI.EMPTY_STRING);

			_rdoTourMarkerLarge = new Button(markerContainer, SWT.RADIO);
			_rdoTourMarkerLarge.setText(UI.EMPTY_STRING);
		}
	}

	/**
	 * tags
	 */
	private void createUI30TypeTags(final Composite parent) {

		_linkTag = new Link(parent, SWT.NONE);
		_linkTag.setText(Messages.tour_editor_label_tour_tag);
		GridDataFactory.fillDefaults()//
				.align(SWT.BEGINNING, SWT.FILL)
				.applyTo(_linkTag);
		_linkTag.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				UI.openControlMenu(_linkTag);
			}
		});

		_lblTourTags = new Label(parent, SWT.WRAP);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				// hint is necessary that the width is not expanded when the text is long
				.hint(200, SWT.DEFAULT)
				.span(2, 1)
				.applyTo(_lblTourTags);

		/*
		 * tour type
		 */
		_linkTourType = new Link(parent, SWT.NONE);
		_linkTourType.setText(Messages.tour_editor_label_tour_type);
		_linkTourType.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				UI.openControlMenu(_linkTourType);
			}
		});

		_lblTourType = new CLabel(parent, SWT.NONE);
		GridDataFactory.swtDefaults()//
				.grab(true, false)
				.span(2, 1)
				.applyTo(_lblTourType);
	}

	/**
	 * tour time
	 */
	private void createUI40TourTime(final Composite parent, final SelectionAdapter selectionAdapter) {

		final Group groupTourTime = new Group(parent, SWT.NONE);
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
					.applyTo(dateContainer);
			GridLayoutFactory.fillDefaults().numColumns(4).applyTo(dateContainer);
			{
				/*
				 * tour start: date
				 */
				Label label = new Label(dateContainer, SWT.NONE);
				label.setText(Messages.Dialog_JoinTours_Label_TourDate);

				_dtTourDate = new org.eclipse.swt.widgets.DateTime(dateContainer, SWT.DATE | SWT.DROP_DOWN | SWT.BORDER);
				GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).applyTo(_dtTourDate);
				_dtTourDate.addSelectionListener(selectionAdapter);

				/*
				 * tour start: time
				 */
				label = new Label(dateContainer, SWT.NONE);
				GridDataFactory.fillDefaults().indent(20, 0).align(SWT.FILL, SWT.CENTER).applyTo(label);
				label.setText(Messages.Dialog_JoinTours_Label_TourTime);

				_dtTourTime = new org.eclipse.swt.widgets.DateTime(dateContainer, SWT.TIME | SWT.DROP_DOWN | SWT.BORDER);
				GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).applyTo(_dtTourTime);
				_dtTourTime.addSelectionListener(selectionAdapter);
			}
		}
	}

	/**
	 * info
	 */
	private void createUI50Info(final Composite container) {

		final Label label = new Label(container, SWT.NONE);
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
		styledText.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		styledText.setLineBullet(0, lineCount + 1, bullet);
	}

	private void enableControls() {

		final boolean isOriginalTime = _rdoKeepOriginalTime.getSelection();
		final boolean showMarker = _chkCreateTourMarker.getSelection();

		_dtTourDate.setEnabled(isOriginalTime == false);
		_dtTourTime.setEnabled(isOriginalTime == false);

		_rdoTourMarkerShort.setEnabled(showMarker);
		_rdoTourMarkerMedium.setEnabled(showMarker);
		_rdoTourMarkerLarge.setEnabled(showMarker);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		// keep window size and position
//		return _state;
		return null;
	}

	@Override
	public ArrayList<TourData> getSelectedTours() {

		// return joined tour

		return _joinedTourDataList;
	}

	private void initTourData() {

		_joinedTourData = new TourData();

		/*
		 * create a dummy tour id that setting of the tags and tour type works otherwise the tour
		 * can cause NPE when a tour has no id
		 */
		_joinedTourData.createTourIdDummy();

		_joinedTourDataList = new ArrayList<TourData>();
		_joinedTourDataList.add(_joinedTourData);

		/*
		 * set tags and tour type
		 */
		for (final TourData tourData : _selectedTours) {

			// get tour type from the first tour which has a tour type
			if (_oldTourType == null) {
				_oldTourType = tourData.getTourType();
			}

			// get all tags
			final Set<TourTag> tourTags = tourData.getTourTags();
			_joinedTourTags.addAll(tourTags);
		}

		_joinedTourData.setTourType(_oldTourType);
		_joinedTourData.setTourTags(_joinedTourTags);
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

		int joinedCalories = 0;
		boolean isJoinedDistanceFromSensor = false;
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
		DateTime joinedTourStart = null;

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

					joinedTourStart = new DateTime(
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

					final DateTime tourStart = new DateTime(
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

				final TourMarker clonedMarker = tourMarker.clone(_joinedTourData);

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

				final TourMarker tourMarker = new TourMarker(_joinedTourData, ChartLabel.MARKER_TYPE_CUSTOM);

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
			 * create description
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
		_joinedTourData.setStartHour((short) joinedTourStart.getHourOfDay());
		_joinedTourData.setStartMinute((short) joinedTourStart.getMinuteOfHour());
		_joinedTourData.setStartSecond((short) joinedTourStart.getSecondOfMinute());
		_joinedTourData.setStartYear((short) joinedTourStart.getYear());
		_joinedTourData.setStartMonth((short) joinedTourStart.getMonthOfYear());
		_joinedTourData.setStartDay((short) joinedTourStart.getDayOfMonth());

		_joinedTourData.setWeek(joinedTourStart);

		// tour id must be created after the tour date/time is set
		_joinedTourData.createTourId();

		_joinedTourData.setTourTitle(_txtJoinedTitle.getText());
		_joinedTourData.setTourDescription(joinedDescription.toString());

		_joinedTourData.setTourType(_joinedTourType);
		_joinedTourData.setTourTags(_joinedTourTags);
		_joinedTourData.setTourMarkers(joinedTourMarker);
		_joinedTourData.setWayPoints(joinedWayPoints);
		_joinedTourData.setDeviceName(Messages.Dialog_JoinTours_Label_DeviceName);

		_joinedTourData.setIsDistanceFromSensor(isJoinedDistanceFromSensor);
		_joinedTourData.setDeviceTimeInterval(joinedDeviceTimeInterval);
		_joinedTourData.setCalories(joinedCalories);

		if (isJoinAltitude) {
			_joinedTourData.altitudeSerie = joinedAltitudeSerie;
		}
		if (isJoinDistance) {
			_joinedTourData.distanceSerie = joinedDistanceSerie;
		}
		if (isJoinCadence) {
			_joinedTourData.cadenceSerie = joinedCadenceSerie;
		}
		if (isJoinLat) {
			_joinedTourData.latitudeSerie = joinedLatitudeSerie;
		}
		if (isJoinLon) {
			_joinedTourData.longitudeSerie = joinedLongitudeSerie;
		}
		if (isJoinPower) {
			_joinedTourData.setPowerSerie(joinedPowerSerie);
		}
		if (isJoinPulse) {
			_joinedTourData.pulseSerie = joinedPulseSerie;
		}
		if (isJoinSpeed) {
			_joinedTourData.setSpeedSerie(joinedSpeedSerie);
		}
		if (isJoinTemperature) {
			_joinedTourData.temperatureSerie = joinedTemperatureSerie;
		}
		if (isJoinTime) {
			_joinedTourData.timeSerie = joinedTimeSerie;
		}

		_joinedTourData.computeAltitudeUpDown();
		_joinedTourData.computeTourDrivingTime();
		_joinedTourData.computeComputedValues();

		// set person which is required to save a tour
		_joinedTourData.setTourPerson(TourManager.getInstance().getActivePerson());

		TourManager.saveModifiedTour(_joinedTourData);
	}

	@Override
	protected void okPressed() {

		saveState();
		joinTours();

		super.okPressed();
	}

	private void onDispose() {

		TourManager.getInstance().removeTourEventListener(_tourEventListener);
	}

	private void restoreState() {

		_txtJoinedTitle.setText(Util.getStateString(_state, STATE_TITLE, Messages.Dialog_JoinTours_Label_DefaultTitle));

		// time
		final String joinedTime = Util.getStateString(_state, STATE_JOINED_TIME, STATE_JOINED_TIME_ORIGINAL);
		_rdoKeepOriginalTime.setSelection(joinedTime.equals(STATE_JOINED_TIME_ORIGINAL));
		_rdoConcatenateTime.setSelection(joinedTime.equals(STATE_JOINED_TIME_CONCATENATED));

		// marker
		_chkCreateTourMarker.setSelection(Util.getStateBoolean(_state, STATE_IS_CREATE_TOUR_MARKER, true));

		final String selectedMarker = Util.getStateString(_state, STATE_TOUR_MARKER_TYPE, STATE_TOUR_MARKER_TYPE_SMALL);
		_rdoTourMarkerShort.setSelection(selectedMarker.equals(STATE_TOUR_MARKER_TYPE_SMALL));
		_rdoTourMarkerMedium.setSelection(selectedMarker.equals(STATE_TOUR_MARKER_TYPE_MEDIUM));
		_rdoTourMarkerLarge.setSelection(selectedMarker.equals(STATE_TOUR_MARKER_TYPE_LARGE));

		/*
		 * update UI from selected tours
		 */

		// date/time
		final TourData firstTour = _selectedTours.get(0);
		_dtTourDate.setDate(firstTour.getStartYear(), firstTour.getStartMonth() - 1, firstTour.getStartDay());
		_dtTourTime.setTime(firstTour.getStartHour(), firstTour.getStartMinute(), firstTour.getStartSecond());
	}

	private void saveState() {

		_state.put(STATE_TITLE, _txtJoinedTitle.getText());

		/*
		 * joined time
		 */
		final String selectedTime = _rdoKeepOriginalTime.getSelection() //
				? STATE_JOINED_TIME_ORIGINAL
				: _rdoConcatenateTime.getSelection() //
						? STATE_JOINED_TIME_CONCATENATED
						: STATE_JOINED_TIME_ORIGINAL;

		_state.put(STATE_JOINED_TIME, selectedTime);

		/*
		 * marker
		 */
		final String selectedMarker = _rdoTourMarkerShort.getSelection() //
				? STATE_TOUR_MARKER_TYPE_SMALL
				: _rdoTourMarkerMedium.getSelection() //
						? STATE_TOUR_MARKER_TYPE_MEDIUM
						: _rdoTourMarkerLarge.getSelection() //
								? STATE_TOUR_MARKER_TYPE_LARGE
								: STATE_TOUR_MARKER_TYPE_SMALL;

		_state.put(STATE_IS_CREATE_TOUR_MARKER, _chkCreateTourMarker.getSelection());
		_state.put(STATE_TOUR_MARKER_TYPE, selectedMarker);
	}

	private void updateUI() {

		final DateTime joinedTourStart = new DateTime(
				_dtTourDate.getYear(),
				_dtTourDate.getMonth() + 1,
				_dtTourDate.getDay(),
				_dtTourTime.getHours(),
				_dtTourTime.getMinutes(),
				_dtTourTime.getSeconds(),
				0);

		_rdoTourMarkerShort.setText(_dtFormatterShort.print(joinedTourStart.getMillis()));
		_rdoTourMarkerShort.pack(true);

		_rdoTourMarkerMedium.setText(_dtFormatterMedium.print(joinedTourStart.getMillis()));
		_rdoTourMarkerMedium.pack(true);

		_rdoTourMarkerLarge.setText(_dtFormatterFull.print(joinedTourStart.getMillis()));
		_rdoTourMarkerLarge.pack(true);

		// tour type/tags
		UI.updateUITourType(_joinedTourData.getTourType(), _lblTourType, true);
		UI.updateUITags(_joinedTourData, _lblTourTags);

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
