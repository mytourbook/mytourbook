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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourType;
import net.tourbook.data.TourWayPoint;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tag.ActionRemoveAllTags;
import net.tourbook.tag.ActionSetTourTag;
import net.tourbook.tag.TagManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.UI;
import net.tourbook.ui.action.ActionOpenPrefDialog;
import net.tourbook.ui.action.ActionSetTourTypeMenu;
import net.tourbook.util.StatusUtil;
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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPart;
import org.joda.time.DateTime;

/**
 * Split tour at a time slice position and save extracted time slices as a new tour
 */
public class DialogExtractTour extends TitleAreaDialog implements ITourProvider {

	private static final String					STATE_TOUR_TITLE						= "TourTitle";						//$NON-NLS-1$
	private static final String					STATE_IS_KEEP_ORIGINAL_TIME				= "isKeepOriginalTime";			//$NON-NLS-1$
	private static final String					STATE_IS_INCLUDE_DESCRIPTION			= "isIncludeDescription";			//$NON-NLS-1$
	private static final String					STATE_IS_INCLUDE_MARKER_WAYPOINTS		= "isIncludeMarkerWaypoints";		//$NON-NLS-1$

	private static final String					STATE_TOUR_TITLE_SOURCE					= "TourTitleSource";				//$NON-NLS-1$
	private static final String					STATE_TOUR_TITLE_SOURCE_FROM_TOUR		= "fromTour";						//$NON-NLS-1$
	private static final String					STATE_TOUR_TITLE_SOURCE_FROM_MARKER		= "fromFirstMarker";				//$NON-NLS-1$
	private static final String					STATE_TOUR_TITLE_SOURCE_CUSTOM			= "custom";						//$NON-NLS-1$

	private static final String					STATE_EXTRACT_METHOD					= "ExtractMethod";					//$NON-NLS-1$
	private static final String					STATE_EXTRACT_METHOD_REMOVE				= "remove";						//$NON-NLS-1$
	private static final String					STATE_EXTRACT_METHOD_KEEP				= "keep";							//$NON-NLS-1$

	private static final String					STATE_TYPE_SELECTED_ID					= "TourTypeId";					//$NON-NLS-1$
	private static final String					STATE_TYPE_SOURCE						= "TourTypeSource";				//$NON-NLS-1$
	private static final String					STATE_TYPE_SOURCE_FROM_SELECTED_TOURS	= "fromTour";						//$NON-NLS-1$
	private static final String					STATE_TYPE_SOURCE_FROM_PREVIOUS_TOUR	= "previous";						//$NON-NLS-1$
	private static final String					STATE_TYPE_SOURCE_CUSTOM				= "custom";						//$NON-NLS-1$

	/**
	 * state: tour title
	 */
	private static final String[]				ALL_STATES_TOUR_TILE_SOURCE				= new String[] {
			STATE_TOUR_TITLE_SOURCE_FROM_TOUR,
			STATE_TOUR_TITLE_SOURCE_FROM_MARKER, //
			STATE_TOUR_TITLE_SOURCE_CUSTOM, //
																						};
	private static final String[]				STATE_COMBO_TEXT_TOUR_TITLE_SOURCE		= new String[] {
			Messages.Dialog_SplitTour_ComboText_TourTitleFromTour,
			Messages.Dialog_SplitTour_ComboText_TourTitleFromFirstMarker,
			Messages.Dialog_SplitTour_ComboText_TourTileCustom,
																						//
																						};
	/**
	 * state: split/extract method
	 */
	private static final String[]				ALL_STATES_EXTRACT_METHOD				= new String[] {
			STATE_EXTRACT_METHOD_REMOVE,
			STATE_EXTRACT_METHOD_KEEP													//
																						};
	private static final String[]				STATE_COMBO_TEXT_SPLIT_METHOD			= new String[] {
			Messages.Dialog_SplitTour_ComboText_RemoveSlices,
			Messages.Dialog_SplitTour_ComboText_KeepSlices								//
																						};

	/**
	 * state: tour type splitted tour
	 */
	private static final String[]				ALL_STATES_TOUR_TYPE					= new String[] {
			STATE_TYPE_SOURCE_FROM_SELECTED_TOURS,
			STATE_TYPE_SOURCE_FROM_PREVIOUS_TOUR,
			STATE_TYPE_SOURCE_CUSTOM													//
																						};
	private static final String[]				STATE_TEXT_TOUR_TYPE_SOURCE				= new String[] {
			Messages.Dialog_SplitTour_ComboText_TourTypeFromTour,
			Messages.Dialog_SplitTour_ComboText_TourTypePrevious,
			Messages.Dialog_SplitTour_ComboText_TourTypeCustom							//
																						};

	private final IDialogSettings				_state									= TourbookPlugin
																								.getDefault()
																								.getDialogSettingsSection(
																										"DialogSplit");	//$NON-NLS-1$

	private TourData							_tourDataSource;
	private TourData							_tourDataTarget;
	private ArrayList<TourData>					_tourDataTargetList;

	private String								_tourTitleFromTour;
	private String								_tourTitleFromCustom;
	private String								_tourTitleFromMarker;

	private long								_tourTypeIdFromSelectedTours			= TourDatabase.ENTITY_IS_NOT_SAVED;
	private long								_tourTypeIdPreviousSplittedTour			= TourDatabase.ENTITY_IS_NOT_SAVED;
	private long								_tourTypeIdCustom						= TourDatabase.ENTITY_IS_NOT_SAVED;

	private ITourEventListener					_tourEventListener;

	private int									_extractStartIndex;
	private int									_extractEndIndex;

	private boolean								_isSplitTour;

	/*
	 * UI controls
	 */
	private Composite							_dlgInnerContainer;

	private Text								_txtTourTitle;
	private Combo								_cboTourTitleSource;

	private Combo								_cboSplitMethod;

	private Button								_chkKeepOriginalDateTime;
	private Label								_lblTourStartDate;
	private Label								_lblTourStartTime;
	private org.eclipse.swt.widgets.DateTime	_dtTourDate;
	private org.eclipse.swt.widgets.DateTime	_dtTourTime;

	private Combo								_cboTourTypeSource;
	private Link								_linkTourType;
	private CLabel								_lblTourType;

	private Link								_linkTag;
	private Label								_lblTourTags;

	private Button								_chkIncludeDescription;
	private Button								_chkIncludeMarkerWaypoints;

	private ActionSetTourTag					_actionAddTag;
	private ActionSetTourTag					_actionRemoveTag;
	private ActionRemoveAllTags					_actionRemoveAllTags;
	private ActionOpenPrefDialog				_actionOpenTagPrefs;
	private ActionOpenPrefDialog				_actionOpenTourTypePrefs;

	public DialogExtractTour(final Shell parentShell, final TourData tourData, final int extractStartIndex) {

		super(parentShell);

		_isSplitTour = true;

		setDefaultImage(TourbookPlugin.getImageDescriptor(Messages.Image__MyTourbook16).createImage());

		_tourDataSource = tourData;

		_extractStartIndex = extractStartIndex;
	}

	public DialogExtractTour(	final Shell parentShell,
								final TourData tourData,
								final int extractStartIndex,
								final int extractEndIndex) {

		super(parentShell);

		_isSplitTour = false;

		setDefaultImage(TourbookPlugin.getImageDescriptor(Messages.Image__MyTourbook16).createImage());

		_tourDataSource = tourData;

		_extractStartIndex = extractStartIndex;
		_extractEndIndex = extractEndIndex;
	}

	private void addTourEventListener() {

		_tourEventListener = new ITourEventListener() {
			@Override
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if ((eventId == TourEventId.TOUR_CHANGED) && (eventData instanceof TourEvent)) {

					final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();
					if ((modifiedTours != null) && (modifiedTours.size() > 0)) {

						// check if it's the correct tour
						if (_tourDataTarget == modifiedTours.get(0)) {

							// update custom tour type id
							final String stateTourTypeSource = getStateTourTypeSource();

							if (stateTourTypeSource.equals(STATE_TYPE_SOURCE_CUSTOM)) {
								final TourType tourType = _tourDataTarget.getTourType();
								_tourTypeIdCustom = tourType == null ? TourDatabase.ENTITY_IS_NOT_SAVED : tourType
										.getTypeId();
							}

							// tour type or tags can have been changed within this dialog
							updateUITourTypeTags();

							// enable/disable tag/type context menu
							enableControls();
						}
					}
				}
			}
		};

		TourManager.getInstance().addTourEventListener(_tourEventListener);
	}

	@Override
	protected void configureShell(final Shell shell) {

		super.configureShell(shell);

		shell.setText(_isSplitTour ? //
				Messages.Dialog_SplitTour_DlgArea_Title
				: Messages.Dialog_ExtractTour_DlgArea_Title);

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

		setTitle(_isSplitTour ? //
				Messages.Dialog_SplitTour_DlgArea_Title
				: Messages.Dialog_ExtractTour_DlgArea_Title);
		setMessage(_isSplitTour ? //
				Messages.Dialog_SplitTour_DlgArea_Message
				: Messages.Dialog_ExtractTour_DlgArea_Message);
	}

	/**
	 * create the drop down menus, this must be created after the parent control is created
	 */
	private void createActionMenus() {

		/*
		 * tour type menu
		 */
		final MenuManager typeMenuMgr = new MenuManager();

		typeMenuMgr.setRemoveAllWhenShown(true);
		typeMenuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(final IMenuManager menuMgr) {

				// set menu items

				ActionSetTourTypeMenu.fillMenu(menuMgr, DialogExtractTour.this, false);

				menuMgr.add(new Separator());
				menuMgr.add(_actionOpenTourTypePrefs);
			}
		});

		// set menu for the tag item
		_linkTourType.setMenu(typeMenuMgr.createContextMenu(_linkTourType));

		/*
		 * tag menu
		 */
		final MenuManager tagMenuMgr = new MenuManager();

		tagMenuMgr.setRemoveAllWhenShown(true);
		tagMenuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(final IMenuManager menuMgr) {

				final Set<TourTag> targetTourTags = _tourDataTarget.getTourTags();
				final boolean isTagInTour = targetTourTags != null && targetTourTags.size() > 0;

				// enable actions
				_actionAddTag.setEnabled(true); // 			// !!! action enablement is overwritten
				_actionRemoveTag.setEnabled(isTagInTour);
				_actionRemoveAllTags.setEnabled(isTagInTour);

				// set menu items
				menuMgr.add(_actionAddTag);
				menuMgr.add(_actionRemoveTag);
				menuMgr.add(_actionRemoveAllTags);

				TagManager.fillMenuRecentTags(menuMgr, DialogExtractTour.this, true, false);

				menuMgr.add(new Separator());
				menuMgr.add(_actionOpenTagPrefs);
			}
		});

		// set menu for the tag item
		_linkTag.setMenu(tagMenuMgr.createContextMenu(_linkTag));
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

		initTargetTourData();

		createUI(dlgContainer);
		createActions();
		createActionMenus();

		restoreState();

		updateUITourTypeTags();
		updateUIFromModel();

		enableControls();

		addTourEventListener();

		return dlgContainer;
	}

	private void createUI(final Composite parent) {

		final SelectionAdapter defaultSelectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				enableControls();
			}
		};

		_dlgInnerContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_dlgInnerContainer);
		GridLayoutFactory.swtDefaults().margins(10, 10).numColumns(3).spacing(10, 8).applyTo(_dlgInnerContainer);
//		_dlgInnerContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			createUI10SplitMethod(_dlgInnerContainer);
			createUI20Title(_dlgInnerContainer);
			createUI22TourTime(_dlgInnerContainer, defaultSelectionAdapter);
			createUI30TypeTags(_dlgInnerContainer);
			createUI40DescriptionMarker(_dlgInnerContainer, defaultSelectionAdapter);
			createUI50Info(_dlgInnerContainer);
		}
	}

	/**
	 * split method
	 */
	private void createUI10SplitMethod(final Composite parent) {

		// label
		final Label label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.align(SWT.FILL, SWT.CENTER)
				.applyTo(label);
		label.setText(_isSplitTour
				? Messages.Dialog_SplitTour_Label_SplitMethod
				: Messages.Dialog_ExtractTour_Label_SplitMethod);

		_cboSplitMethod = new Combo(parent, SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_cboSplitMethod);

		// fill combo
		for (final String timeText : STATE_COMBO_TEXT_SPLIT_METHOD) {
			_cboSplitMethod.add(timeText);
		}
	}

	/**
	 * tour title
	 */
	private void createUI20Title(final Composite parent) {

		// label: title
		final Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.Dialog_SplitTour_Label_TourTitle);
		label.setToolTipText(Messages.Dialog_SplitTour_Label_TourTitle_Tooltip);

		// combo: title source
		_cboTourTitleSource = new Combo(parent, SWT.READ_ONLY);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.span(2, 1)
				.applyTo(_cboTourTitleSource);
		_cboTourTitleSource.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelectTourTitleSource();
			}
		});

		// fill combo
		for (final String comboText : STATE_COMBO_TEXT_TOUR_TITLE_SOURCE) {
			_cboTourTitleSource.add(comboText);
		}

		// spacer
		new Label(parent, SWT.NONE);

		// text: title
		_txtTourTitle = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.span(2, 1)
				.indent(0, -5)
				.applyTo(_txtTourTitle);
		_txtTourTitle.setToolTipText(Messages.Dialog_SplitTour_Label_TourTitle_Tooltip);
	}

	/**
	 * tour time
	 */
	private void createUI22TourTime(final Composite parent, final SelectionAdapter defaultSelectionAdapter) {

		final SelectionAdapter dateTimeUpdateListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				enableControls();
			}
		};

		/*
		 * keep original time
		 */
		final Label label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.align(SWT.FILL, SWT.CENTER)
				.applyTo(label);
		label.setText(Messages.Dialog_SplitTour_Label_TourStartDateTime);

		_chkKeepOriginalDateTime = new Button(parent, SWT.CHECK);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkKeepOriginalDateTime);
		_chkKeepOriginalDateTime.setText(Messages.Dialog_SplitTour_ComboText_KeepTime);
		_chkKeepOriginalDateTime.addSelectionListener(defaultSelectionAdapter);

		//spacer
		new Label(parent, SWT.NONE);

		/*
		 * tour start date/time
		 */
		final Composite dateContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.span(2, 1)
				.indent(0, -5)
				.applyTo(dateContainer);
		GridLayoutFactory.fillDefaults().numColumns(4).applyTo(dateContainer);
		{
			/*
			 * tour start: date
			 */
			_lblTourStartDate = new Label(dateContainer, SWT.NONE);
			_lblTourStartDate.setText(Messages.Dialog_JoinTours_Label_TourDate);

			_dtTourDate = new org.eclipse.swt.widgets.DateTime(dateContainer, SWT.DATE | SWT.DROP_DOWN | SWT.BORDER);
			GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).applyTo(_dtTourDate);
			_dtTourDate.addSelectionListener(dateTimeUpdateListener);

			/*
			 * tour start: time
			 */
			_lblTourStartTime = new Label(dateContainer, SWT.NONE);
			GridDataFactory.fillDefaults().indent(20, 0).align(SWT.FILL, SWT.CENTER).applyTo(_lblTourStartTime);
			_lblTourStartTime.setText(Messages.Dialog_JoinTours_Label_TourTime);

			_dtTourTime = new org.eclipse.swt.widgets.DateTime(dateContainer, SWT.TIME | SWT.DROP_DOWN | SWT.BORDER);
			GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).applyTo(_dtTourTime);
			_dtTourTime.addSelectionListener(dateTimeUpdateListener);
		}
	}

	/**
	 * tour type & tags
	 * 
	 * @param defaultSelectionAdapter
	 */
	private void createUI30TypeTags(final Composite parent) {

		/*
		 * tour type
		 */
		final Label label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.align(SWT.FILL, SWT.CENTER)
				.applyTo(label);
		label.setText(Messages.Dialog_JoinTours_Label_TourType);

		_cboTourTypeSource = new Combo(parent, SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_cboTourTypeSource);
		_cboTourTypeSource.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelectTourTypeSource();
			}
		});

		// fill combo
		for (final String tourTypeText : STATE_TEXT_TOUR_TYPE_SOURCE) {
			_cboTourTypeSource.add(tourTypeText);
		}

		// spacer
		new Label(parent, SWT.NONE);

		final Composite tourTypeContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false)//
				.indent(0, -8)
				.span(2, 1)
				.applyTo(tourTypeContainer);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(tourTypeContainer);
		{
			_linkTourType = new Link(tourTypeContainer, SWT.NONE);
			_linkTourType.setText(Messages.Dialog_JoinTours_Link_TourType);
			_linkTourType.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					UI.openControlMenu(_linkTourType);
				}
			});

			_lblTourType = new CLabel(tourTypeContainer, SWT.NONE);
			GridDataFactory.swtDefaults().grab(true, false).applyTo(_lblTourType);
		}

		/*
		 * tags
		 */
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
	}

	/**
	 * checkbox: set marker for each tour
	 */
	private void createUI40DescriptionMarker(final Composite parent, final SelectionAdapter defaultSelectionAdapter) {

		/*
		 * checkbox: description
		 */
		_chkIncludeDescription = new Button(parent, SWT.CHECK);
		GridDataFactory.fillDefaults().span(3, 1).indent(0, 10).applyTo(_chkIncludeDescription);
		_chkIncludeDescription.setText(Messages.Dialog_SplitTour_Checkbox_IncludeDescription);

		/*
		 * checkbox: include existing tour marker
		 */
		_chkIncludeMarkerWaypoints = new Button(parent, SWT.CHECK);
		GridDataFactory.fillDefaults().span(3, 1).indent(0, -5).applyTo(_chkIncludeMarkerWaypoints);
		_chkIncludeMarkerWaypoints.setText(Messages.Dialog_JoinTours_Checkbox_IncludeMarkerWaypoints);

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
		GridDataFactory.fillDefaults()//
				.align(SWT.FILL, SWT.BEGINNING)
				.indent(0, 10)
				.span(2, 1)
				.applyTo(styledText);
		styledText.setText(infoText);
		styledText.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		styledText.setLineBullet(0, lineCount + 1, bullet);
	}

	private void enableControls() {

		final boolean isCustomTime = _chkKeepOriginalDateTime.getSelection() == false;
		final boolean isCustomTourType = getStateTourTypeSource().equals(STATE_TYPE_SOURCE_CUSTOM);
		final boolean isCustomTourTitle = getStateTourTitleSource().equals(STATE_TOUR_TITLE_SOURCE_CUSTOM);
		final TourType tourType = _tourDataTarget.getTourType();

		_txtTourTitle.setEditable(isCustomTourTitle);

		_dtTourDate.setEnabled(isCustomTime);
		_dtTourTime.setEnabled(isCustomTime);
		_lblTourStartDate.setEnabled(isCustomTime);
		_lblTourStartTime.setEnabled(isCustomTime);

		_linkTourType.setEnabled(isCustomTourType);
		_lblTourType.setEnabled(isCustomTourType);

		// enable/disable actions for tags/tour types
		TagManager.enableRecentTagActions(true, _tourDataTarget.getTourTags());
//		TourTypeMenuManager.enableRecentTourTypeActions(true, tourType == null
//				? TourDatabase.ENTITY_IS_NOT_SAVED
//				: tourType.getTypeId());
	}

	/**
	 * Create a new tour with the extracted time slices
	 */
	private boolean extractTour() {

		/*
		 * get data series
		 */
		final int[] tourAltitudeSerie = _tourDataSource.altitudeSerie;
		final int[] tourCadenceSerie = _tourDataSource.cadenceSerie;
		final int[] tourDistanceSerie = _tourDataSource.distanceSerie;
		final double[] tourLatitudeSerie = _tourDataSource.latitudeSerie;
		final double[] tourLongitudeSerie = _tourDataSource.longitudeSerie;
		final int[] tourPulseSerie = _tourDataSource.pulseSerie;
		final int[] tourTemperatureSerie = _tourDataSource.temperatureSerie;
		final int[] tourTimeSerie = _tourDataSource.timeSerie;

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
		final boolean isTourPower = _tourDataSource.isPowerSerieFromDevice();
		final boolean isTourSpeed = _tourDataSource.isSpeedSerieFromDevice();
		if (isTourPower) {
			tourPowerSerie = _tourDataSource.getPowerSerie();
		}
		if (isTourSpeed) {
			tourSpeedSerie = _tourDataSource.getSpeedSerie();
		}

		int dataSerieLength = -1;

		if (isTourAltitude) {
			dataSerieLength = tourAltitudeSerie.length;
		} else if (isTourCadence) {
			dataSerieLength = tourCadenceSerie.length;
		} else if (isTourDistance) {
			dataSerieLength = tourDistanceSerie.length;
		} else if (isTourLat) {
			dataSerieLength = tourLatitudeSerie.length;
		} else if (isTourLon) {
			dataSerieLength = tourLongitudeSerie.length;
		} else if (isTourPower) {
			dataSerieLength = tourPowerSerie.length;
		} else if (isTourPulse) {
			dataSerieLength = tourPulseSerie.length;
		} else if (isTourSpeed) {
			dataSerieLength = tourSpeedSerie.length;
		} else if (isTourTemperature) {
			dataSerieLength = tourTemperatureSerie.length;
		} else if (isTourTime) {
			dataSerieLength = tourTimeSerie.length;
		}

		if (dataSerieLength == -1) {
			StatusUtil.showStatus(Messages.NT001_DialogExtractTour_InvalidTourData);
			return false;
		}

		if (_isSplitTour) {
			_extractEndIndex = dataSerieLength - 1;
		}

		final int extractSerieLength = _extractEndIndex - _extractStartIndex + 1;

		final int[] extractAltitudeSerie = new int[extractSerieLength];
		final int[] extractCadenceSerie = new int[extractSerieLength];
		final int[] extractDistanceSerie = new int[extractSerieLength];
		final double[] extractLatitudeSerie = new double[extractSerieLength];
		final double[] extractLongitudeSerie = new double[extractSerieLength];
		final int[] extractPowerSerie = new int[extractSerieLength];
		final int[] extractPulseSerie = new int[extractSerieLength];
		final int[] extractSpeedSerie = new int[extractSerieLength];
		final int[] extractTemperatureSerie = new int[extractSerieLength];
		final int[] extractTimeSerie = new int[extractSerieLength];

		final StringBuilder joinedDescription = new StringBuilder();
		final HashSet<TourMarker> joinedTourMarker = new HashSet<TourMarker>();
		final ArrayList<TourWayPoint> joinedWayPoints = new ArrayList<TourWayPoint>();

		int joinedSerieIndex = 0;
		int joinedTourStartIndex = 0;
		int joinedTourStartDistance = 0;
		int joinedRecordingTime = 0;
		int joinedDistance = 0;
		int joinedCalories = 0;
		boolean isJoinedDistanceFromSensor = false;
		short joinedDeviceTimeInterval = -1;

		int relTourTime = 0;
		long relTourTimeOffset = 0;
		long absFirstTourStartTimeSec = 0;
		long absJoinedTourStartTimeSec = 0;
		DateTime joinedTourStart = null;

		final boolean isOriginalTime = _chkKeepOriginalDateTime.getSelection();

		boolean isFirstTour = true;

		/*
		 * copy tour data series into joined data series
		 */

		/*
		 * set tour time
		 */
		final DateTime tourStartTime = new DateTime(
				_tourDataSource.getStartYear(),
				_tourDataSource.getStartMonth(),
				_tourDataSource.getStartDay(),
				_tourDataSource.getStartHour(),
				_tourDataSource.getStartMinute(),
				_tourDataSource.getStartSecond(),
				0);

		if (isFirstTour) {

			// get start date/time

			if (isOriginalTime) {

				joinedTourStart = tourStartTime;

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

				final DateTime tourStart = tourStartTime;

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

				extractTimeSerie[joinedSerieIndex] = (int) (relTourTimeOffset + relTourTime);
			}

			if (isTourAltitude) {
				extractAltitudeSerie[joinedSerieIndex] = tourAltitudeSerie[tourSerieIndex];
			}
			if (isTourCadence) {
				extractCadenceSerie[joinedSerieIndex] = tourCadenceSerie[tourSerieIndex];
			}

			if (isTourDistance) {

				relTourDistance = tourDistanceSerie[tourSerieIndex];

				extractDistanceSerie[joinedSerieIndex] = joinedTourStartDistance + relTourDistance;
			}

			if (isTourPulse) {
				extractPulseSerie[joinedSerieIndex] = tourPulseSerie[tourSerieIndex];
			}
			if (isTourLat) {
				extractLatitudeSerie[joinedSerieIndex] = tourLatitudeSerie[tourSerieIndex];
			}
			if (isTourLon) {
				extractLongitudeSerie[joinedSerieIndex] = tourLongitudeSerie[tourSerieIndex];
			}
			if (isTourTemperature) {
				extractTemperatureSerie[joinedSerieIndex] = tourTemperatureSerie[tourSerieIndex];
			}
			if (isTourPower) {
				extractPowerSerie[joinedSerieIndex] = tourPowerSerie[tourSerieIndex];
			}
			if (isTourSpeed) {
				extractSpeedSerie[joinedSerieIndex] = tourSpeedSerie[tourSerieIndex];
			}

			joinedSerieIndex++;
		}

		final Set<TourMarker> tourMarkers = _tourDataSource.getTourMarkers();

		if (_chkIncludeMarkerWaypoints.getSelection()) {

			/*
			 * copy tour markers
			 */
			for (final TourMarker tourMarker : tourMarkers) {

				final TourMarker clonedMarker = tourMarker.clone(_tourDataTarget);

				int joinMarkerIndex = joinedTourStartIndex + clonedMarker.getSerieIndex();
				if (joinMarkerIndex >= extractSerieLength) {
					joinMarkerIndex = extractSerieLength - 1;
				}

				// a cloned marker has the same marker id, create a new id
				clonedMarker.createMarkerId();

				// adjust marker position, position is relativ to the tour start
				clonedMarker.setSerieIndex(joinMarkerIndex);

				if (isTourTime) {
					tourMarker.setTime(extractTimeSerie[joinMarkerIndex]);
				}
				if (isTourDistance) {
					tourMarker.setDistance(extractDistanceSerie[joinMarkerIndex]);
				}

				joinedTourMarker.add(clonedMarker);
			}

			/*
			 * copy way points
			 */
			for (final TourWayPoint wayPoint : _tourDataSource.getTourWayPoints()) {
				joinedWayPoints.add((TourWayPoint) wayPoint.clone());
			}
		}

		/*
		 * create description
		 */
		if (_chkIncludeDescription.getSelection()) {

			final String tourDescription = _tourDataSource.getTourDescription();

			if (joinedDescription.length() > 0) {
				// set space between two tours
				joinedDescription.append(UI.NEW_LINE2);
			}

			joinedDescription.append(Messages.Dialog_JoinTours_Label_Tour);
			joinedDescription.append(TourManager.getTourTitleDetailed(_tourDataSource));
			if (tourDescription.length() > 0) {
				joinedDescription.append(UI.NEW_LINE);
				joinedDescription.append(tourDescription);
			}
		}

		/*
		 * other tour values
		 */
		if (isFirstTour) {
			isJoinedDistanceFromSensor = _tourDataSource.getIsDistanceFromSensor();
			joinedDeviceTimeInterval = _tourDataSource.getDeviceTimeInterval();
		} else {
			if (isJoinedDistanceFromSensor && _tourDataSource.getIsDistanceFromSensor()) {
				// keep TRUE state
			} else {
				isJoinedDistanceFromSensor = false;
			}
			if (joinedDeviceTimeInterval == _tourDataSource.getDeviceTimeInterval()) {
				// keep value
			} else {
				joinedDeviceTimeInterval = -1;
			}
		}

		/*
		 * summarize other fields
		 */
		joinedRecordingTime += _tourDataSource.getTourRecordingTime();
		joinedDistance += _tourDataSource.getTourDistance();
		joinedCalories += _tourDataSource.getCalories();

		/*
		 * init next tour
		 */
		isFirstTour = false;
		joinedTourStartIndex = joinedSerieIndex;
		joinedTourStartDistance += relTourDistance;

		/*
		 * set target tour data
		 */
		_tourDataTarget.setStartHour((short) joinedTourStart.getHourOfDay());
		_tourDataTarget.setStartMinute((short) joinedTourStart.getMinuteOfHour());
		_tourDataTarget.setStartSecond((short) joinedTourStart.getSecondOfMinute());
		_tourDataTarget.setStartYear((short) joinedTourStart.getYear());
		_tourDataTarget.setStartMonth((short) joinedTourStart.getMonthOfYear());
		_tourDataTarget.setStartDay((short) joinedTourStart.getDayOfMonth());

		_tourDataTarget.setWeek(joinedTourStart);

		// tour id must be created after the tour date/time is set
		_tourDataTarget.createTourId();

		_tourDataTarget.setTourTitle(_txtTourTitle.getText());
		_tourDataTarget.setTourDescription(joinedDescription.toString());

		_tourDataTarget.setTourMarkers(joinedTourMarker);
		_tourDataTarget.setWayPoints(joinedWayPoints);
		_tourDataTarget.setDeviceName(Messages.Dialog_JoinTours_Label_DeviceName);

		_tourDataTarget.setIsDistanceFromSensor(isJoinedDistanceFromSensor);
		_tourDataTarget.setDeviceTimeInterval(joinedDeviceTimeInterval);
		_tourDataTarget.setCalories(joinedCalories);

		_tourDataTarget.setTourRecordingTime(joinedRecordingTime);
		_tourDataTarget.setTourDistance(joinedDistance);

		// !! tour type and tour tags are already set !!

		if (isTourAltitude) {
			_tourDataTarget.altitudeSerie = extractAltitudeSerie;
		}
		if (isTourDistance) {
			_tourDataTarget.distanceSerie = extractDistanceSerie;
		}
		if (isTourCadence) {
			_tourDataTarget.cadenceSerie = extractCadenceSerie;
		}
		if (isTourLat) {
			_tourDataTarget.latitudeSerie = extractLatitudeSerie;
		}
		if (isTourLon) {
			_tourDataTarget.longitudeSerie = extractLongitudeSerie;
		}
		if (isTourPower) {
			_tourDataTarget.setPowerSerie(extractPowerSerie);
		}
		if (isTourPower) {
			_tourDataTarget.pulseSerie = extractPulseSerie;
		}
		if (isTourSpeed) {
			_tourDataTarget.setSpeedSerie(extractSpeedSerie);
		}
		if (isTourTemperature) {
			_tourDataTarget.temperatureSerie = extractTemperatureSerie;
		}
		if (isTourTime) {
			_tourDataTarget.timeSerie = extractTimeSerie;
		}

		_tourDataTarget.computeAltitudeUpDown();
		_tourDataTarget.computeTourDrivingTime();
		_tourDataTarget.computeComputedValues();

		// set person which is required to save a tour
		_tourDataTarget.setTourPerson(TourbookPlugin.getActivePerson());

		/*
		 * check size of the fields
		 */
		if (_tourDataTarget.isValidForSave() == false) {
			return false;
		}

		TourManager.saveModifiedTour(_tourDataTarget);

		return true;
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

		return _tourDataTargetList;
	}

	private String getStateSplitMethod() {
		return Util.getStateFromCombo(_cboSplitMethod, ALL_STATES_EXTRACT_METHOD, STATE_EXTRACT_METHOD_REMOVE);
	}

	private String getStateTourTitleSource() {
		return Util.getStateFromCombo(
				_cboTourTitleSource,
				ALL_STATES_TOUR_TILE_SOURCE,
				STATE_TOUR_TITLE_SOURCE_FROM_TOUR);
	}

	private String getStateTourTypeSource() {
		return Util.getStateFromCombo(_cboTourTypeSource, ALL_STATES_TOUR_TYPE, STATE_TYPE_SOURCE_FROM_SELECTED_TOURS);
	}

	/**
	 * Create {@link TourData} for the splitted/extracted tour
	 */
	private void initTargetTourData() {

		_tourDataTarget = new TourData();

		/*
		 * create a dummy tour id because setting of the tags and tour type works requires it
		 * otherwise it would cause a NPE when a tour has no id
		 */
		_tourDataTarget.createTourIdDummy();

		_tourDataTargetList = new ArrayList<TourData>();
		_tourDataTargetList.add(_tourDataTarget);

		/*
		 * set tour title
		 */
		_tourTitleFromTour = _tourDataSource.getTourTitle();
		_tourTitleFromMarker = UI.EMPTY_STRING;

		// get title from first marker which is within the splitted tour
		final ArrayList<TourMarker> sortedMarker = new ArrayList<TourMarker>(_tourDataSource.getTourMarkers());
		Collections.sort(sortedMarker);

		for (final TourMarker tourMarker : sortedMarker) {
			if (tourMarker.getSerieIndex() >= _extractStartIndex) {
				_tourTitleFromMarker = tourMarker.getLabel();
				break;
			}
		}
		// set default custom marker
		_tourTitleFromCustom = _tourTitleFromMarker;

		/*
		 * get all tags
		 */
		final Set<TourTag> joinedTourTags = new HashSet<TourTag>();
		final Set<TourTag> tourTags = _tourDataSource.getTourTags();
		joinedTourTags.addAll(tourTags);
		_tourDataTarget.setTourTags(joinedTourTags);

		/*
		 * set tour type
		 */
		// get tour type id
		final TourType tourType = _tourDataSource.getTourType();
		if (tourType != null) {
			_tourTypeIdFromSelectedTours = tourType.getTypeId();
		}

		_tourTypeIdCustom = //
		_tourTypeIdPreviousSplittedTour = Util.getStateLong(
				_state,
				STATE_TYPE_SELECTED_ID,
				TourDatabase.ENTITY_IS_NOT_SAVED);

		final String stateTourTypeSource = Util.getStateString(
				_state,
				STATE_TYPE_SOURCE,
				STATE_TYPE_SOURCE_FROM_SELECTED_TOURS);

		long joinedTourTypeId = TourDatabase.ENTITY_IS_NOT_SAVED;

		if (stateTourTypeSource.equals(STATE_TYPE_SOURCE_FROM_SELECTED_TOURS)) {
			joinedTourTypeId = _tourTypeIdFromSelectedTours;
		} else if (stateTourTypeSource.equals(STATE_TYPE_SOURCE_FROM_PREVIOUS_TOUR)
				|| stateTourTypeSource.equals(STATE_TYPE_SOURCE_CUSTOM)) {
			joinedTourTypeId = _tourTypeIdPreviousSplittedTour;
		}

		_tourDataTarget.setTourType(TourDatabase.getTourType(joinedTourTypeId));
	}

	@Override
	protected void okPressed() {

		if (extractTour() == false) {
			return;
		}

		// state must be set after the tour is saved because the tour type id is set when the tour is saved
		saveState();

		super.okPressed();
	}

	private void onDispose() {

		TourManager.getInstance().removeTourEventListener(_tourEventListener);
	}

	private void onSelectTourTitleSource() {

		updateUIFromModel();
		enableControls();
	}

	private void onSelectTourTypeSource() {

		final String stateTourTypeSource = getStateTourTypeSource();

		long joinedTourTypeId = TourDatabase.ENTITY_IS_NOT_SAVED;

		if (stateTourTypeSource.equals(STATE_TYPE_SOURCE_FROM_SELECTED_TOURS)) {
			joinedTourTypeId = _tourTypeIdFromSelectedTours;
		} else if (stateTourTypeSource.equals(STATE_TYPE_SOURCE_FROM_PREVIOUS_TOUR)) {
			joinedTourTypeId = _tourTypeIdPreviousSplittedTour;
		} else if (stateTourTypeSource.equals(STATE_TYPE_SOURCE_CUSTOM)) {
			joinedTourTypeId = _tourTypeIdCustom;
		}

		_tourDataTarget.setTourType(TourDatabase.getTourType(joinedTourTypeId));

		updateUITourTypeTags();

		enableControls();
	}

	private void restoreState() {

		_txtTourTitle.setText(Util.getStateString(
				_state,
				STATE_TOUR_TITLE,
				Messages.Dialog_JoinTours_Label_DefaultTitle));

		// tour title source
		Util.selectStateInCombo(
				_state,
				STATE_TOUR_TITLE_SOURCE,
				ALL_STATES_TOUR_TILE_SOURCE,
				STATE_TOUR_TITLE_SOURCE_FROM_TOUR,
				_cboTourTitleSource);

		// split method
		Util.selectStateInCombo(
				_state,
				STATE_EXTRACT_METHOD,
				ALL_STATES_EXTRACT_METHOD,
				STATE_EXTRACT_METHOD_REMOVE,
				_cboSplitMethod);

		// tour type source
		Util.selectStateInCombo(
				_state,
				STATE_TYPE_SOURCE,
				ALL_STATES_TOUR_TYPE,
				STATE_TYPE_SOURCE_FROM_SELECTED_TOURS,
				_cboTourTypeSource);

		// tour start date/time
		_chkKeepOriginalDateTime.setSelection(Util.getStateBoolean(_state, STATE_IS_KEEP_ORIGINAL_TIME, true));

		// description/marker/waypoints
		_chkIncludeDescription.setSelection(Util.getStateBoolean(_state, STATE_IS_INCLUDE_DESCRIPTION, true));
		_chkIncludeMarkerWaypoints.setSelection(Util.getStateBoolean(_state, STATE_IS_INCLUDE_MARKER_WAYPOINTS, true));

		/*
		 * update UI from selected tours
		 */

		// date/time
		_dtTourDate.setDate(
				_tourDataSource.getStartYear(),
				_tourDataSource.getStartMonth() - 1,
				_tourDataSource.getStartDay());
		_dtTourTime.setTime(
				_tourDataSource.getStartHour(),
				_tourDataSource.getStartMinute(),
				_tourDataSource.getStartSecond());
	}

	private void saveState() {

		// tour title
		_state.put(STATE_TOUR_TITLE, _txtTourTitle.getText());
		_state.put(STATE_TOUR_TITLE_SOURCE, getStateTourTitleSource());

		// tour type
		final TourType tourType = _tourDataTarget.getTourType();
		_state.put(STATE_TYPE_SELECTED_ID, tourType == null ? TourDatabase.ENTITY_IS_NOT_SAVED : tourType.getTypeId());
		_state.put(STATE_TYPE_SOURCE, getStateTourTypeSource());

		// split method
		_state.put(STATE_EXTRACT_METHOD, getStateSplitMethod());

		// tour start date/time
		_state.put(STATE_IS_KEEP_ORIGINAL_TIME, _chkKeepOriginalDateTime.getSelection());

		// description/marker
		_state.put(STATE_IS_INCLUDE_DESCRIPTION, _chkIncludeDescription.getSelection());
		_state.put(STATE_IS_INCLUDE_MARKER_WAYPOINTS, _chkIncludeMarkerWaypoints.getSelection());
	}

	private void updateUIFromModel() {

		/*
		 * tour title
		 */
		final String stateTourTitleSource = getStateTourTitleSource();

		String tourTitle = _txtTourTitle.getText();

		if (stateTourTitleSource.equals(STATE_TOUR_TITLE_SOURCE_FROM_TOUR)) {
			tourTitle = _tourTitleFromTour;
		} else if (stateTourTitleSource.equals(STATE_TOUR_TITLE_SOURCE_FROM_MARKER)) {
			tourTitle = _tourTitleFromMarker;
		} else if (stateTourTitleSource.equals(STATE_TOUR_TITLE_SOURCE_CUSTOM)) {
			tourTitle = _tourTitleFromCustom;
		}

		// update ui
		_txtTourTitle.setText(tourTitle);

	}

	private void updateUITourTypeTags() {

		// tour type/tags
		UI.updateUITourType(_tourDataTarget, _lblTourType, true);
		UI.updateUITags(_tourDataTarget, _lblTourTags);

		// reflow layout that the tags are aligned correctly
		_dlgInnerContainer.layout(true);
	}

}
