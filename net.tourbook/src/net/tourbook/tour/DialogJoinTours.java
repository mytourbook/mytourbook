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
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.ChartLabel;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourType;
import net.tourbook.data.TourWayPoint;
import net.tourbook.database.PersonManager;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tag.ActionRemoveAllTags;
import net.tourbook.tag.ActionSetTourTag;
import net.tourbook.tag.TagManager;
import net.tourbook.ui.ITourProvider2;
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
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class DialogJoinTours extends TitleAreaDialog implements ITourProvider2 {

	private static final String					STATE_TOUR_TITLE						= "Title";							//$NON-NLS-1$
	private static final String					STATE_TOUR_TYPE_ID						= "TourTypeId";					//$NON-NLS-1$
	private static final String					STATE_PERSON_ID							= "PersonId";						//$NON-NLS-1$

	private static final String					STATE_IS_KEEP_ORIGINAL_TIME				= "isKeepOriginalTime";			//$NON-NLS-1$
	private static final String					STATE_IS_INCLUDE_DESCRIPTION			= "isIncludeDescription";			//$NON-NLS-1$
	private static final String					STATE_IS_INCLUDE_MARKER_WAYPOINTS		= "isIncludeMarkerWaypoints";		//$NON-NLS-1$
	private static final String					STATE_IS_CREATE_TOUR_MARKER				= "isCreateTourMarker";			//$NON-NLS-1$

	private static final String					STATE_JOIN_METHOD						= "JoinMethod";					//$NON-NLS-1$
	private static final String					STATE_JOIN_METHOD_ORIGINAL				= "original";						//$NON-NLS-1$
	private static final String					STATE_JOIN_METHOD_CONCATENATED			= "concatenated";					//$NON-NLS-1$

	private static final String					STATE_TOUR_TITLE_SOURCE					= "TourTitleSource";				//$NON-NLS-1$
	private static final String					STATE_TOUR_TITLE_SOURCE_FROM_TOUR		= "fromTour";						//$NON-NLS-1$
	private static final String					STATE_TOUR_TITLE_SOURCE_CUSTOM			= "custom";						//$NON-NLS-1$

	private static final String					STATE_TYPE_SOURCE						= "TourTypeSource";				//$NON-NLS-1$
	private static final String					STATE_TYPE_SOURCE_FROM_SELECTED_TOURS	= "fromTour";						//$NON-NLS-1$
	private static final String					STATE_TYPE_SOURCE_PREVIOUS_JOINED_TOUR	= "previous";						//$NON-NLS-1$
	private static final String					STATE_TYPE_SOURCE_CUSTOM				= "custom";						//$NON-NLS-1$

	private static final String					STATE_MARKER_TYPE						= "TourMarkerType";				//$NON-NLS-1$
	private static final String					STATE_MARKER_TYPE_SMALL					= "small";							//$NON-NLS-1$
	private static final String					STATE_MARKER_TYPE_MEDIUM				= "medium";						//$NON-NLS-1$
	private static final String					STATE_MARKER_TYPE_LARGE					= "large";							//$NON-NLS-1$

	/**
	 * state: join method
	 */
	private static final String[]				ALL_STATES_JOIN_METHOD					= new String[] {
			STATE_JOIN_METHOD_ORIGINAL,
			STATE_JOIN_METHOD_CONCATENATED												//
																						};
	private static final String[]				STATE_TEXT_JOIN_METHOD					= new String[] {
			Messages.Dialog_JoinTours_ComboText_KeepTime,
			Messages.Dialog_JoinTours_ComboText_ConcatenateTime						//
																						};

	/**
	 * state: tour title
	 */
	private static final String[]				ALL_STATES_TOUR_TILE_SOURCE				= new String[] {
			STATE_TOUR_TITLE_SOURCE_FROM_TOUR,
			STATE_TOUR_TITLE_SOURCE_CUSTOM, //
																						};
	private static final String[]				STATE_COMBO_TEXT_TOUR_TITLE_SOURCE		= new String[] {
			Messages.Dialog_JoinTours_ComboText_TourTitleFromTour,
			Messages.Dialog_JoinTours_ComboText_TourTileCustom,
																						//
																						};

	/**
	 * state: tour type
	 */
	private static final String[]				ALL_STATES_TOUR_TYPE					= new String[] {
			STATE_TYPE_SOURCE_FROM_SELECTED_TOURS,
			STATE_TYPE_SOURCE_PREVIOUS_JOINED_TOUR,
			STATE_TYPE_SOURCE_CUSTOM													//
																						};
	private static final String[]				STATE_TEXT_TOUR_TYPE_SOURCE				= new String[] {
			Messages.Dialog_JoinTours_ComboText_TourTypeFromTour,
			Messages.Dialog_JoinTours_ComboText_TourTypePrevious,
			Messages.Dialog_JoinTours_ComboText_TourTypeCustom							//
																						};

	/**
	 * state: tour marker
	 */
	private static final String[]				ALL_STATES_TOUR_MARKER					= new String[] {
			STATE_MARKER_TYPE_SMALL,
			STATE_MARKER_TYPE_MEDIUM,
			STATE_MARKER_TYPE_LARGE													//
																						};

	private final IDialogSettings				_state									= TourbookPlugin
																								.getDefault()
																								.getDialogSettingsSection(
																										"DialogJoinTours"); //$NON-NLS-1$

	private final DateTimeFormatter				_dtFormatterShort						= DateTimeFormat.shortDate();
	private final DateTimeFormatter				_dtFormatterMedium						= DateTimeFormat
																								.shortDateTime();
	private final DateTimeFormatter				_dtFormatterFull						= DateTimeFormat.fullDateTime();

	private ActionSetTourTag					_actionAddTag;
	private ActionSetTourTag					_actionRemoveTag;
	private ActionRemoveAllTags					_actionRemoveAllTags;
	private ActionOpenPrefDialog				_actionOpenTagPrefs;
	private ActionOpenPrefDialog				_actionOpenTourTypePrefs;

	private TourData							_joinedTourData;
	private ArrayList<TourData>					_joinedTourDataList;

	private final ArrayList<TourData>			_selectedTours;
	private TourPerson[]						_people;

	private long								_tourTypeIdFromSelectedTours			= TourDatabase.ENTITY_IS_NOT_SAVED;
	private long								_tourTypeIdPreviousJoinedTour			= TourDatabase.ENTITY_IS_NOT_SAVED;
	private long								_tourTypeIdCustom						= TourDatabase.ENTITY_IS_NOT_SAVED;

	private String								_tourTitleFromTour;
	private String								_tourTitleFromCustom;

	private ITourEventListener					_tourEventListener;

	/*
	 * UI controls
	 */
	private Composite							_dlgInnerContainer;

	private Combo								_cboJoinMethod;

	private Combo								_cboTourTitleSource;
	private Text								_txtTourTitle;

	private Button								_chkKeepOriginalDateTime;
	private Label								_lblTourStartDate;
	private Label								_lblTourStartTime;
	private org.eclipse.swt.widgets.DateTime	_dtTourDate;
	private org.eclipse.swt.widgets.DateTime	_dtTourTime;

	private Combo								_cboTourType;
	private Link								_linkTourType;
	private CLabel								_lblTourType;

	private Link								_linkTag;
	private Label								_lblTourTags;

	private Button								_chkIncludeDescription;
	private Button								_chkIncludeMarkerWaypoints;
	private Button								_chkCreateTourMarker;
	private Label								_lblMarkerText;
	private Combo								_cboTourMarker;

	private Combo								_cboPerson;
	protected Point								_shellDefaultSize;

	public DialogJoinTours(final Shell parentShell, final ArrayList<TourData> selectedTours) {

		super(parentShell);

		// sort tours by date/time
		Collections.sort(selectedTours);

		_selectedTours = selectedTours;

		// make dialog resizable
		int shellStyle = getShellStyle();
		shellStyle = //
		SWT.NONE //
				| SWT.TITLE
				| SWT.CLOSE
				| SWT.MIN
//				| SWT.MAX
				| SWT.RESIZE
				| SWT.NONE;
		setShellStyle(shellStyle);
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

		shell.addListener(SWT.Resize, new Listener() {
			public void handleEvent(final Event event) {

				// allow resizing the width but not the height

				if (_shellDefaultSize == null) {
					_shellDefaultSize = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
				}

				final Point shellSize = shell.getSize();

				/*
				 * this is not working, the shell is flickering when the shell size is below min
				 * size and I found no way to prevent a resize :-(
				 */
//				if (shellSize.x < _shellDefaultSize.x) {
//					event.doit = false;
//				}

				shellSize.x = shellSize.x < _shellDefaultSize.x ? _shellDefaultSize.x : shellSize.x;
				shellSize.y = _shellDefaultSize.y;

				shell.setSize(shellSize);
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

		updateUITourTypeTags();
		updateUIMarker(true);
		updateUIFromModel();

		enableControls();

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
		final MenuManager tagMenuMgr = new MenuManager();

		tagMenuMgr.setRemoveAllWhenShown(true);
		tagMenuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(final IMenuManager menuMgr) {

				final Set<TourTag> joinedTourTags = _joinedTourData.getTourTags();
				final boolean isTagInTour = joinedTourTags != null && joinedTourTags.size() > 0;

				// enable actions
				_actionAddTag.setEnabled(true); // 			// !!! action enablement is overwritten
				_actionRemoveTag.setEnabled(isTagInTour);
				_actionRemoveAllTags.setEnabled(isTagInTour);

				// set menu items
				menuMgr.add(_actionAddTag);
				menuMgr.add(_actionRemoveTag);
				menuMgr.add(_actionRemoveAllTags);

				TagManager.fillMenuRecentTags(menuMgr, DialogJoinTours.this, true, false);

				menuMgr.add(new Separator());
				menuMgr.add(_actionOpenTagPrefs);
			}
		});

		// set menu for the tag item
		_linkTag.setMenu(tagMenuMgr.createContextMenu(_linkTag));

		/*
		 * tour type menu
		 */
		final MenuManager typeMenuMgr = new MenuManager();

		typeMenuMgr.setRemoveAllWhenShown(true);
		typeMenuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(final IMenuManager menuMgr) {

				// set menu items

				ActionSetTourTypeMenu.fillMenu(menuMgr, DialogJoinTours.this, false);

				menuMgr.add(new Separator());
				menuMgr.add(_actionOpenTourTypePrefs);
			}
		});

		// set menu for the tag item
		_linkTourType.setMenu(typeMenuMgr.createContextMenu(_linkTourType));
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
			createUI10JoinMethod(_dlgInnerContainer, defaultSelectionAdapter);
			createUI20Title(_dlgInnerContainer);
			createUI22TourTime(_dlgInnerContainer, defaultSelectionAdapter);
			createUI30TypeTags(_dlgInnerContainer);
			createUI40Person(_dlgInnerContainer);
			createUI50DescriptionMarker(_dlgInnerContainer, defaultSelectionAdapter);
		}
	}

	/**
	 * tour time
	 */
	private void createUI10JoinMethod(final Composite parent, final SelectionAdapter defaultSelectionAdapter) {

		/*
		 * join method
		 */

		// label
		final Label label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.align(SWT.FILL, SWT.CENTER)
				.applyTo(label);
		label.setText(Messages.Dialog_JoinTours_Label_JoinMethod);

		// combo
		_cboJoinMethod = new Combo(parent, SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_cboJoinMethod);
		_cboJoinMethod.addSelectionListener(defaultSelectionAdapter);

		// fill combo
		for (final String timeText : STATE_TEXT_JOIN_METHOD) {
			_cboJoinMethod.add(timeText);
		}
	}

//	/**
//	 * tour title
//	 */
//	private void createUI20Title(final Composite parent) {
//
//		final Label label = new Label(parent, SWT.NONE);
//		label.setText(Messages.Dialog_JoinTours_Label_Title);
//		label.setToolTipText(Messages.Dialog_JoinTours_Label_Title_Tooltip);
//
//		_txtJoinedTitle = new Text(parent, SWT.BORDER);
//		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_txtJoinedTitle);
//		_txtJoinedTitle.setToolTipText(Messages.Dialog_JoinTours_Label_Title_Tooltip);
//	}

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
		 * tour time
		 */
		final Label label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.align(SWT.FILL, SWT.CENTER)
				.applyTo(label);
		label.setText(Messages.Dialog_SplitTour_Label_TourStartDateTime);

		_chkKeepOriginalDateTime = new Button(parent, SWT.CHECK);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkKeepOriginalDateTime);
		_chkKeepOriginalDateTime.setText(Messages.Dialog_SplitTour_Checkbox_KeepTime);
		_chkKeepOriginalDateTime.addSelectionListener(defaultSelectionAdapter);

		/*
		 * tour start date/time
		 */
		//spacer
		new Label(parent, SWT.NONE);

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

		_cboTourType = new Combo(parent, SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_cboTourType);
		_cboTourType.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelectTourTypeSource();
			}
		});

		// fill combo
		for (final String tourTypeText : STATE_TEXT_TOUR_TYPE_SOURCE) {
			_cboTourType.add(tourTypeText);
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
	 * person
	 */
	private void createUI40Person(final Composite parent) {

		// label
		final Label label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.align(SWT.FILL, SWT.CENTER)
				.applyTo(label);
		label.setText(Messages.Dialog_SplitTour_Label_Person);
		label.setToolTipText(Messages.Dialog_SplitTour_Label_Person_Tooltip);

		// combo: person
		_cboPerson = new Combo(parent, SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_cboPerson);
		_cboPerson.setVisibleItemCount(20);
		_cboPerson.setToolTipText(Messages.Dialog_SplitTour_Label_Person_Tooltip);
	}

	/**
	 * checkbox: set marker for each tour
	 */
	private void createUI50DescriptionMarker(final Composite parent, final SelectionAdapter defaultSelectionAdapter) {

		/*
		 * description
		 */
//		Label label = new Label(parent, SWT.NONE);
//		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
//		label.setText(Messages.Dialog_JoinTours_Label_Description);

		// checkbox
		_chkIncludeDescription = new Button(parent, SWT.CHECK);
		GridDataFactory.fillDefaults().span(3, 1).indent(0, 10).applyTo(_chkIncludeDescription);
		_chkIncludeDescription.setText(Messages.Dialog_JoinTours_Checkbox_IncludeDescription);

		/*
		 * include existing tour marker
		 */
//		label = new Label(parent, SWT.NONE);
//		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
//		label.setText(Messages.Dialog_JoinTours_Label_TourMarker);

		// checkbox
		_chkIncludeMarkerWaypoints = new Button(parent, SWT.CHECK);
		GridDataFactory.fillDefaults().span(3, 1).applyTo(_chkIncludeMarkerWaypoints);
		_chkIncludeMarkerWaypoints.setText(Messages.Dialog_JoinTours_Checkbox_IncludeMarkerWaypoints);

		/*
		 * create tour marker
		 */
//		label = new Label(parent, SWT.NONE);
//		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(label);
//		label.setText(Messages.Dialog_JoinTours_Label_CreateTourMarker);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.span(3, 1)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().spacing(0, 3).applyTo(container);
		{
			// checkbox
			_chkCreateTourMarker = new Button(container, SWT.CHECK);
			GridDataFactory.fillDefaults().applyTo(_chkCreateTourMarker);
			_chkCreateTourMarker.setText(Messages.Dialog_JoinTours_Checkbox_CreateTourMarker);
			_chkCreateTourMarker.addSelectionListener(defaultSelectionAdapter);

			final Composite markerContainer = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(markerContainer);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(markerContainer);
			{
				// label
				_lblMarkerText = new Label(markerContainer, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.indent(16, 0)
						.applyTo(_lblMarkerText);
				_lblMarkerText.setText(Messages.Dialog_JoinTours_Label_TourMarkerText);

				// combo
				_cboTourMarker = new Combo(markerContainer, SWT.READ_ONLY);
				GridDataFactory.fillDefaults()//
						.grab(true, false)
						.applyTo(_cboTourMarker);
				_cboTourMarker.addSelectionListener(defaultSelectionAdapter);

				// !!! combo box is filled in updateUIMarker() !!!
			}
		}
	}

//	/**
//	 * info
//	 */
//	private void createUI50Info(final Composite container) {
//
//		final Label label = new Label(container, SWT.NONE);
//		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).indent(0, 10).applyTo(label);
//		label.setText(Messages.Dialog_JoinTours_Label_OtherFields);
//
//		// use a bulleted list to display this info
//		final StyleRange style = new StyleRange();
//		style.metrics = new GlyphMetrics(0, 0, 10);
//		final Bullet bullet = new Bullet(style);
//
//		final String infoText = Messages.Dialog_JoinTours_Label_OtherFieldsInfo;
//		final int lineCount = Util.countCharacter(infoText, '\n');
//
//		final StyledText styledText = new StyledText(container, SWT.READ_ONLY);
//		GridDataFactory.fillDefaults()//
//				.align(SWT.FILL, SWT.BEGINNING)
//				.indent(0, 10)
//				.span(2, 1)
//				.applyTo(styledText);
//		styledText.setText(infoText);
//		styledText.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
//		styledText.setLineBullet(0, lineCount + 1, bullet);
//	}

	private void enableControls() {

		final boolean isCustomTourTitle = getStateTourTitleSource().equals(STATE_TOUR_TITLE_SOURCE_CUSTOM);
		final boolean isCustomTime = _chkKeepOriginalDateTime.getSelection() == false;
		final boolean isCustomTourType = getStateTourTypeSource().equals(STATE_TYPE_SOURCE_CUSTOM);
		final boolean isCreateMarker = _chkCreateTourMarker.getSelection();

		_txtTourTitle.setEditable(isCustomTourTitle);

		_dtTourDate.setEnabled(isCustomTime);
		_dtTourTime.setEnabled(isCustomTime);
		_lblTourStartDate.setEnabled(isCustomTime);
		_lblTourStartTime.setEnabled(isCustomTime);

		_cboTourMarker.setEnabled(isCreateMarker);
		_lblMarkerText.setEnabled(isCreateMarker);

		_linkTourType.setEnabled(isCustomTourType);
		_lblTourType.setEnabled(isCustomTourType);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		// keep window size and position
		return _state;
//		return null;
	}

	private TourPerson getSelectedPerson() {

		final int selectedIndex = _cboPerson.getSelectionIndex();
		final TourPerson person = _people[selectedIndex];

		return person;
	}

	@Override
	public ArrayList<TourData> getSelectedTours() {

		// return joined tour

		return _joinedTourDataList;
	}

	private String getStateJoinMethod() {
		return Util.getStateFromCombo(_cboJoinMethod, ALL_STATES_JOIN_METHOD, STATE_JOIN_METHOD_ORIGINAL);
	}

	private String getStateTourMarker() {
		return Util.getStateFromCombo(_cboTourMarker, ALL_STATES_TOUR_MARKER, STATE_MARKER_TYPE_SMALL);
	}

	private String getStateTourTitleSource() {
		return Util.getStateFromCombo(
				_cboTourTitleSource,
				ALL_STATES_TOUR_TILE_SOURCE,
				STATE_TOUR_TITLE_SOURCE_FROM_TOUR);
	}

	private String getStateTourTypeSource() {
		return Util.getStateFromCombo(_cboTourType, ALL_STATES_TOUR_TYPE, STATE_TYPE_SOURCE_FROM_SELECTED_TOURS);
	}

	private void initTourData() {

		_joinedTourData = new TourData();

		/*
		 * create a dummy tour id because setting of the tags and tour type works requires it
		 * otherwise it would cause a NPE when a tour has no id
		 */
		_joinedTourData.createTourIdDummy();

		_joinedTourDataList = new ArrayList<TourData>();
		_joinedTourDataList.add(_joinedTourData);

		final Set<TourTag> joinedTourTags = new HashSet<TourTag>();

		/*
		 * set tour title
		 */
		_tourTitleFromTour = UI.EMPTY_STRING;

		// get title from first tour which contains a title
		for (final TourData tourData : _selectedTours) {
			final String tourTitle = tourData.getTourTitle();
			if (tourTitle.length() > 0) {
				_tourTitleFromTour = tourTitle;
				break;
			}
		}

		/*
		 * set tags into joined tour and get tour type from selected tours
		 */
		for (final TourData tourData : _selectedTours) {

			// get tour type id from the first tour which has a tour type
			if (_tourTypeIdFromSelectedTours == TourDatabase.ENTITY_IS_NOT_SAVED) {
				final TourType tourType = tourData.getTourType();
				if (tourType != null) {
					_tourTypeIdFromSelectedTours = tourType.getTypeId();
				}
			}

			// get all tags
			final Set<TourTag> tourTags = tourData.getTourTags();
			joinedTourTags.addAll(tourTags);
		}

		/*
		 * set tour type into joined tour
		 */
		_tourTypeIdCustom = _tourTypeIdPreviousJoinedTour = Util.getStateLong(
				_state,
				STATE_TOUR_TYPE_ID,
				TourDatabase.ENTITY_IS_NOT_SAVED);

		final String stateTourTypeSource = Util.getStateString(
				_state,
				STATE_TYPE_SOURCE,
				STATE_TYPE_SOURCE_FROM_SELECTED_TOURS);

		long joinedTourTypeId = TourDatabase.ENTITY_IS_NOT_SAVED;

		if (stateTourTypeSource.equals(STATE_TYPE_SOURCE_FROM_SELECTED_TOURS)) {
			joinedTourTypeId = _tourTypeIdFromSelectedTours;
		} else if (stateTourTypeSource.equals(STATE_TYPE_SOURCE_PREVIOUS_JOINED_TOUR)
				|| stateTourTypeSource.equals(STATE_TYPE_SOURCE_CUSTOM)) {
			joinedTourTypeId = _tourTypeIdPreviousJoinedTour;
		}

		_joinedTourData.setTourType(TourDatabase.getTourType(joinedTourTypeId));
		_joinedTourData.setTourTags(joinedTourTags);
	}

	/**
	 * Join the tours and create a new tour
	 */
	private boolean joinTours() {

		final boolean isOriginalTime = getStateJoinMethod().equals(STATE_JOIN_METHOD_ORIGINAL);

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
//		boolean isJoinTemperature = false;
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

		final StringBuilder joinedDescription = new StringBuilder();
		final HashSet<TourMarker> joinedTourMarker = new HashSet<TourMarker>();
		final ArrayList<TourWayPoint> joinedWayPoints = new ArrayList<TourWayPoint>();

		int joinedSerieIndex = 0;
		int joinedTourStartIndex = 0;
		int joinedTourStartDistance = 0;
		int joinedRecordingTime = 0;
		int joinedDrivingTime = 0;
		int joinedDistance = 0;
		int joinedCalories = 0;
		boolean isJoinedDistanceFromSensor = false;
		short joinedDeviceTimeInterval = -1;
		String joinedWeatherClouds = UI.EMPTY_STRING;
		int joinedWeatherWindDir = 0;
		int joinedWeatherWindSpeed = 0;
		int joinedRestPulse = 0;

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
			final DateTime tourStartTime = new DateTime(
					tourTourData.getStartYear(),
					tourTourData.getStartMonth(),
					tourTourData.getStartDay(),
					tourTourData.getStartHour(),
					tourTourData.getStartMinute(),
					tourTourData.getStartSecond(),
					0);

			if (isFirstTour) {

				// get start date/time

				if (isOriginalTime) {

					joinedTourStart = tourStartTime;

				} else {

					joinedTourStart = new DateTime(
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
				} else {
					// set temperature to temporarily value
					joinedTemperatureSerie[joinedSerieIndex] = Integer.MIN_VALUE;
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

			final Set<TourMarker> tourMarkers = tourTourData.getTourMarkers();

			if (_chkIncludeMarkerWaypoints.getSelection()) {

				/*
				 * copy tour markers
				 */
				for (final TourMarker tourMarker : tourMarkers) {

					final TourMarker clonedMarker = tourMarker.clone(_joinedTourData);

					int joinMarkerIndex = joinedTourStartIndex + clonedMarker.getSerieIndex();
					if (joinMarkerIndex >= joinedSliceCounter) {
						joinMarkerIndex = joinedSliceCounter - 1;
					}

					// adjust marker position, position is relativ to the tour start
					clonedMarker.setSerieIndex(joinMarkerIndex);

					if (isJoinTime) {
						clonedMarker.setTime(joinedTimeSerie[joinMarkerIndex]);
					}
					if (isJoinDistance) {
						clonedMarker.setDistance(joinedDistanceSerie[joinMarkerIndex]);
					}

					joinedTourMarker.add(clonedMarker);
				}

				/*
				 * copy way points
				 */
				for (final TourWayPoint wayPoint : tourTourData.getTourWayPoints()) {
					joinedWayPoints.add((TourWayPoint) wayPoint.clone());
				}
			}

			if (_chkCreateTourMarker.getSelection()) {

				/*
				 * create a tour marker
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

					// create tour marker label
					final String stateTourMarker = getStateTourMarker();
					final long tourStartTimeMS = tourStartTime.getMillis();

					String markerLabel = UI.EMPTY_STRING;

					if (stateTourMarker.equals(STATE_MARKER_TYPE_SMALL)) {
						markerLabel = _dtFormatterShort.print(tourStartTimeMS);
					} else if (stateTourMarker.equals(STATE_MARKER_TYPE_MEDIUM)) {
						markerLabel = _dtFormatterMedium.print(tourStartTimeMS);
					} else if (stateTourMarker.equals(STATE_MARKER_TYPE_LARGE)) {
						markerLabel = _dtFormatterFull.print(tourStartTimeMS);
					}

					final int joinMarkerIndex = joinedTourStartIndex + tourMarkerIndex;

					final TourMarker tourMarker = new TourMarker(_joinedTourData, ChartLabel.MARKER_TYPE_CUSTOM);

					tourMarker.setSerieIndex(joinMarkerIndex);
					tourMarker.setLabel(markerLabel);
					tourMarker.setVisualPosition(ChartLabel.VISUAL_VERTICAL_ABOVE_GRAPH);

					if (isJoinTime) {
						tourMarker.setTime(joinedTimeSerie[joinMarkerIndex]);
					}
					if (isJoinDistance) {
						tourMarker.setDistance(joinedDistanceSerie[joinMarkerIndex]);
					}

					joinedTourMarker.add(tourMarker);
				}
			}

			/*
			 * create description
			 */
			if (_chkIncludeDescription.getSelection()) {

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
			}

			/*
			 * other tour values
			 */
			if (isFirstTour) {
				isJoinedDistanceFromSensor = tourTourData.getIsDistanceFromSensor();
				joinedDeviceTimeInterval = tourTourData.getDeviceTimeInterval();

				joinedWeatherClouds = tourTourData.getWeatherClouds();
				joinedWeatherWindDir = tourTourData.getWeatherWindDir();
				joinedWeatherWindSpeed = tourTourData.getWeatherWindSpeed();

				joinedRestPulse = tourTourData.getRestPulse();

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
			tourTourData.computeTourDrivingTime();

			joinedRecordingTime += tourTourData.getTourRecordingTime();
			joinedDrivingTime += tourTourData.getTourDrivingTime();

			joinedDistance += tourTourData.getTourDistance();
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

		_joinedTourData.setTourTitle(_txtTourTitle.getText());
		_joinedTourData.setTourDescription(joinedDescription.toString());

		_joinedTourData.setTourMarkers(joinedTourMarker);
		_joinedTourData.setWayPoints(joinedWayPoints);
		_joinedTourData.setDeviceName(Messages.Dialog_JoinTours_Label_DeviceName);

		_joinedTourData.setIsDistanceFromSensor(isJoinedDistanceFromSensor);
		_joinedTourData.setDeviceTimeInterval(joinedDeviceTimeInterval);

		_joinedTourData.setCalories(joinedCalories);
		_joinedTourData.setRestPulse(joinedRestPulse);

		_joinedTourData.setWeatherClouds(joinedWeatherClouds);
		_joinedTourData.setWeatherWindDir(joinedWeatherWindDir);
		_joinedTourData.setWeatherWindSpeed(joinedWeatherWindSpeed);


		_joinedTourData.setTourRecordingTime(joinedRecordingTime);
		_joinedTourData.setTourDrivingTime(joinedDrivingTime);
		_joinedTourData.setTourDistance(joinedDistance);

		// !! tour type and tour tags are already set !!

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

		_joinedTourData.temperatureSerie = joinedTemperatureSerie;

		if (isJoinTime) {
			_joinedTourData.timeSerie = joinedTimeSerie;
		}

		_joinedTourData.computeAltitudeUpDown();
		_joinedTourData.computeComputedValues();

		// set person which is required to save a tour
		_joinedTourData.setTourPerson(getSelectedPerson());

		/*
		 * check size of the fields
		 */
		if (_joinedTourData.isValidForSave() == false) {
			return false;
		}

		/*
		 * TourData.computeComputedValues() creates speed data serie which must be removed that
		 * cleanupDataSeries() works correctly
		 */
		_joinedTourData.setSpeedSerie(null);

		_joinedTourData.cleanupDataSeries();

		_joinedTourData = TourManager.saveModifiedTour(_joinedTourData);

		return true;
	}

	@Override
	protected void okPressed() {

		if (joinTours() == false) {
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
		} else if (stateTourTypeSource.equals(STATE_TYPE_SOURCE_PREVIOUS_JOINED_TOUR)) {
			joinedTourTypeId = _tourTypeIdPreviousJoinedTour;
		} else if (stateTourTypeSource.equals(STATE_TYPE_SOURCE_CUSTOM)) {
			joinedTourTypeId = _tourTypeIdCustom;
		}

		_joinedTourData.setTourType(TourDatabase.getTourType(joinedTourTypeId));

		updateUITourTypeTags();

		enableControls();
	}

	private void restoreState() {

		_tourTitleFromCustom = Util.getStateString(
				_state,
				STATE_TOUR_TITLE,
				Messages.Dialog_JoinTours_Label_DefaultTitle);

		// tour title source
		Util.selectStateInCombo(
				_state,
				STATE_TOUR_TITLE_SOURCE,
				ALL_STATES_TOUR_TILE_SOURCE,
				STATE_TOUR_TITLE_SOURCE_FROM_TOUR,
				_cboTourTitleSource);

		// tour type source
		Util.selectStateInCombo(
				_state,
				STATE_TYPE_SOURCE,
				ALL_STATES_TOUR_TYPE,
				STATE_TYPE_SOURCE_FROM_SELECTED_TOURS,
				_cboTourType);

		// join method
		Util.selectStateInCombo(
				_state,
				STATE_JOIN_METHOD,
				ALL_STATES_JOIN_METHOD,
				STATE_JOIN_METHOD_ORIGINAL,
				_cboJoinMethod);

		// tour start date/time
		_chkKeepOriginalDateTime.setSelection(Util.getStateBoolean(_state, STATE_IS_KEEP_ORIGINAL_TIME, true));

		// description/marker/waypoints
		_chkIncludeDescription.setSelection(Util.getStateBoolean(_state, STATE_IS_INCLUDE_DESCRIPTION, true));
		_chkIncludeMarkerWaypoints.setSelection(Util.getStateBoolean(_state, STATE_IS_INCLUDE_MARKER_WAYPOINTS, true));
		_chkCreateTourMarker.setSelection(Util.getStateBoolean(_state, STATE_IS_CREATE_TOUR_MARKER, true));

		/*
		 * update UI from selected tours
		 */

		// date/time
		final TourData firstTour = _selectedTours.get(0);
		_dtTourDate.setDate(firstTour.getStartYear(), firstTour.getStartMonth() - 1, firstTour.getStartDay());
		_dtTourTime.setTime(firstTour.getStartHour(), firstTour.getStartMinute(), firstTour.getStartSecond());

		/*
		 * fill person combo and reselect previous person
		 */
		final long statePersonId = Util.getStateLong(_state, STATE_PERSON_ID, -1);

		if (_people == null) {
			final ArrayList<TourPerson> people = PersonManager.getTourPeople();
			_people = people.toArray(new TourPerson[people.size()]);
		}

		int index = 0;
		int personIndex = 0;

		for (final TourPerson person : _people) {

			_cboPerson.add(person.getName());

			if (person.getPersonId() == statePersonId) {
				personIndex = index;
			}

			index++;
		}

		_cboPerson.select(personIndex);

	}

	private void saveState() {

		// tour title
		_state.put(STATE_TOUR_TITLE, _txtTourTitle.getText());
		_state.put(STATE_TOUR_TITLE_SOURCE, getStateTourTitleSource());

		// tour type
		final TourType tourType = _joinedTourData.getTourType();
		_state.put(STATE_TOUR_TYPE_ID, tourType == null ? TourDatabase.ENTITY_IS_NOT_SAVED : tourType.getTypeId());
		_state.put(STATE_TYPE_SOURCE, getStateTourTypeSource());

		// tour start date/time
		_state.put(STATE_IS_KEEP_ORIGINAL_TIME, _chkKeepOriginalDateTime.getSelection());

		// join method
		_state.put(STATE_JOIN_METHOD, getStateJoinMethod());

		// description/marker
		_state.put(STATE_IS_INCLUDE_DESCRIPTION, _chkIncludeDescription.getSelection());
		_state.put(STATE_IS_INCLUDE_MARKER_WAYPOINTS, _chkIncludeMarkerWaypoints.getSelection());
		_state.put(STATE_IS_CREATE_TOUR_MARKER, _chkCreateTourMarker.getSelection());
		_state.put(STATE_MARKER_TYPE, getStateTourMarker());

		// person
		_state.put(STATE_PERSON_ID, getSelectedPerson().getPersonId());
	}

	@Override
	public void toursAreModified(final ArrayList<TourData> modifiedTours) {

		if ((modifiedTours != null) && (modifiedTours.size() > 0)) {

			// check if it's the correct tour
			if (_joinedTourData == modifiedTours.get(0)) {

				// update custom tour type id
				final String stateTourTypeSource = getStateTourTypeSource();

				if (stateTourTypeSource.equals(STATE_TYPE_SOURCE_CUSTOM)) {
					final TourType tourType = _joinedTourData.getTourType();
					_tourTypeIdCustom = tourType == null ? TourDatabase.ENTITY_IS_NOT_SAVED : tourType.getTypeId();
				}

				// tour type or tags can have been changed within this dialog
				updateUITourTypeTags();
			}
		}
	}

	private void updateUIFromModel() {

		/*
		 * tour title
		 */
		final String stateTourTitleSource = getStateTourTitleSource();

		String tourTitle = _txtTourTitle.getText();

		if (stateTourTitleSource.equals(STATE_TOUR_TITLE_SOURCE_FROM_TOUR)) {
			tourTitle = _tourTitleFromTour;
		} else if (stateTourTitleSource.equals(STATE_TOUR_TITLE_SOURCE_CUSTOM)) {
			tourTitle = _tourTitleFromCustom;
		}

		// update ui
		_txtTourTitle.setText(tourTitle);

	}

	/**
	 * updates marker which requires that the tour date/time control is set
	 * 
	 * @param isRestoreState
	 */
	private void updateUIMarker(final boolean isRestoreState) {

		final DateTime joinedTourStart = new DateTime(
				_dtTourDate.getYear(),
				_dtTourDate.getMonth() + 1,
				_dtTourDate.getDay(),
				_dtTourTime.getHours(),
				_dtTourTime.getMinutes(),
				_dtTourTime.getSeconds(),
				0);

		/**
		 * !!! this list must correspond to the states {@link #ALL_STATES_TOUR_MARKER} !!!
		 */
		final String[] markerItems = new String[3];
		markerItems[0] = NLS.bind(
				Messages.Dialog_JoinTours_ComboText_MarkerTourTime,
				_dtFormatterShort.print(joinedTourStart.getMillis()));

		markerItems[1] = NLS.bind(
				Messages.Dialog_JoinTours_ComboText_MarkerTourTime,
				_dtFormatterMedium.print(joinedTourStart.getMillis()));

		markerItems[2] = NLS.bind(
				Messages.Dialog_JoinTours_ComboText_MarkerTourTime,
				_dtFormatterFull.print(joinedTourStart.getMillis()));

		final int selectedMarkerIndex = _cboTourMarker.getSelectionIndex();

		_cboTourMarker.setItems(markerItems);

		if (isRestoreState) {
			// restore from state
			Util.selectStateInCombo(//
					_state,
					STATE_MARKER_TYPE,
					ALL_STATES_TOUR_MARKER,
					STATE_MARKER_TYPE_SMALL,
					_cboTourMarker);
		} else {
			// restore selection
			_cboTourMarker.select(selectedMarkerIndex);
		}
	}

	private void updateUITourTypeTags() {

		// tour type/tags
		UI.updateUITourType(_joinedTourData, _lblTourType, true);
		UI.updateUITags(_joinedTourData, _lblTourTags);

		// reflow layout that the tags are aligned correctly
		_dlgInnerContainer.layout(true);
	}

}
