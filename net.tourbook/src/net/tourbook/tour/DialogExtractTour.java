/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourType;
import net.tourbook.data.TourWayPoint;
import net.tourbook.database.PersonManager;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tag.TagMenuManager;
import net.tourbook.ui.ITourProvider2;
import net.tourbook.ui.UI;
import net.tourbook.ui.action.ActionSetTourTypeMenu;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.joda.time.DateTime;

/**
 * Split tour at a time slice position and save extracted time slices as a new tour
 */
public class DialogExtractTour extends TitleAreaDialog implements ITourProvider2 {

	private static final String					STATE_TOUR_TITLE						= "TourTitle";						//$NON-NLS-1$
	private static final String					STATE_PERSON_ID							= "PersonId";						//$NON-NLS-1$
	private static final String					STATE_TOUR_TYPE_ID						= "TourTypeId";					//$NON-NLS-1$

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
			STATE_EXTRACT_METHOD_KEEP,
			STATE_EXTRACT_METHOD_REMOVE,
																						//
																						};
	private static final String[]				STATE_COMBO_TEXT_EXTRACT_METHOD			= new String[] {
			Messages.Dialog_SplitTour_ComboText_KeepSlices,
			Messages.Dialog_SplitTour_ComboText_RemoveSlices,
																						//
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

	private final IDialogSettings				_state									= TourbookPlugin.getDefault() //
																								.getDialogSettingsSection(
																										"DialogSplit");	//$NON-NLS-1$

	private TourDataEditorView					_tourDataEditor;

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

	/**
	 * Last index in the data serie when tour is extracted. This is set to -1 when the tour is
	 * splitted which extract the tour from the {@link #_extractStartIndex} until the last data
	 * serie index.
	 */
	private int									_extractEndIndex;

	/**
	 * Is <code>true</code> when tour is splitted otherwise it is extracted and
	 * {@link #_extractEndIndex} contains the last data serie index.
	 */
	private boolean								_isSplitTour;

	/**
	 * is <code>true</code> when tour slices can be deleted
	 */
	private boolean								_canRemoveTimeSlices;

	private DateTime							_extractedTourStartTime;

	private ActionOpenPrefDialog				_actionOpenTourTypePrefs;

	private TourPerson[]						_people;
	protected Point								_shellDefaultSize;
	private TagMenuManager						_tagMenuMgr;

	/*
	 * UI controls
	 */
	private Composite							_dlgInnerContainer;

	private Combo								_cboPerson;
	private Combo								_cboSplitMethod;
	private Combo								_cboTourTitleSource;
	private Combo								_cboTourTypeSource;

	private Button								_chkIncludeDescription;
	private Button								_chkIncludeMarkerWaypoints;
	private Button								_chkKeepOriginalDateTime;

	private org.eclipse.swt.widgets.DateTime	_dtTourDate;
	private org.eclipse.swt.widgets.DateTime	_dtTourTime;

	private Label								_lblTourStartDate;
	private Label								_lblTourStartTime;
	private Label								_lblTourTags;
	private CLabel								_lblTourType;

	private Link								_linkTourType;
	private Link								_linkTag;

	private Text								_txtTourTitle;

	/**
	 * Split or extract a tour
	 * 
	 * @param parentShell
	 * @param tourData
	 * @param extractStartIndex
	 * @param extractEndIndex
	 *            when -1 the tour is splitted at {@link #_extractStartIndex} otherwise it is
	 *            extracted
	 * @param tourDataEditor
	 */
	public DialogExtractTour(	final Shell parentShell,
								final TourData tourData,
								final int extractStartIndex,
								final int extractEndIndex,
								final TourDataEditorView tourDataEditor) {

		super(parentShell);

		_isSplitTour = extractEndIndex == -1 ? true : false;

		_extractStartIndex = extractStartIndex;
		_extractEndIndex = extractEndIndex;

		_tourDataEditor = tourDataEditor;
		_tourDataSource = tourData;

		setDefaultImage(TourbookPlugin.getImageDescriptor(Messages.Image__MyTourbook16).createImage());

		_canRemoveTimeSlices = _tourDataEditor.getTourData().isContainReferenceTour() == false;

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE);
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

		shell.addListener(SWT.Resize, new Listener() {
			@Override
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

		setTitle(_isSplitTour ? //
				Messages.Dialog_SplitTour_DlgArea_Title
				: Messages.Dialog_ExtractTour_DlgArea_Title);
		setMessage(_isSplitTour ? //
				Messages.Dialog_SplitTour_DlgArea_Message
				: Messages.Dialog_ExtractTour_DlgArea_Message);
	}

	private void createActions() {

		_tagMenuMgr = new TagMenuManager(this, false);

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
		createMenus();

		restoreState();

		updateUITourTypeTags();
		updateUIFromModel();

		enableControls();

		return dlgContainer;
	}

	/**
	 * create the drop down menus, this must be created after the parent control is created
	 */
	private void createMenus() {

		/*
		 * tour type menu
		 */
		final MenuManager typeMenuMgr = new MenuManager();

		typeMenuMgr.setRemoveAllWhenShown(true);
		typeMenuMgr.addMenuListener(new IMenuListener() {
			@Override
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
			@Override
			public void menuAboutToShow(final IMenuManager menuMgr) {

				final Set<TourTag> targetTourTags = _tourDataTarget.getTourTags();
				final boolean isTagInTour = targetTourTags != null && targetTourTags.size() > 0;

				_tagMenuMgr.fillTagMenu(menuMgr);
				_tagMenuMgr.enableTagActions(true, isTagInTour, targetTourTags);
			}
		});

		// set menu for the tag item

		final Menu tagContextMenu = tagMenuMgr.createContextMenu(_linkTag);
		tagContextMenu.addMenuListener(new MenuListener() {

			@Override
			public void menuHidden(final MenuEvent e) {
				_tagMenuMgr.onHideMenu();
			}

			@Override
			public void menuShown(final MenuEvent e) {

				final Rectangle rect = _linkTag.getBounds();
				Point pt = new Point(rect.x, rect.y + rect.height);
				pt = _linkTag.getParent().toDisplay(pt);

				_tagMenuMgr.onShowMenu(e, _linkTag, pt, null);
			}
		});

		_linkTag.setMenu(tagContextMenu);

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
			createUI_10_SplitMethod(_dlgInnerContainer);
			createUI_20_Title(_dlgInnerContainer);
			createUI_22_TourTime(_dlgInnerContainer, defaultSelectionAdapter);
			createUI_30_TypeTags(_dlgInnerContainer);
			createUI_40_Person(_dlgInnerContainer);
			createUI_50_DescriptionMarker(_dlgInnerContainer, defaultSelectionAdapter);
		}
	}

	/**
	 * split method
	 */
	private void createUI_10_SplitMethod(final Composite parent) {

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
		for (final String timeText : STATE_COMBO_TEXT_EXTRACT_METHOD) {
			_cboSplitMethod.add(timeText);
		}
	}

	/**
	 * tour title
	 */
	private void createUI_20_Title(final Composite parent) {

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
	private void createUI_22_TourTime(final Composite parent, final SelectionAdapter defaultSelectionAdapter) {

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
		_chkKeepOriginalDateTime.setText(Messages.Dialog_SplitTour_Checkbox_KeepTime);
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
	private void createUI_30_TypeTags(final Composite parent) {

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
					net.tourbook.common.UI.openControlMenu(_linkTourType);
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
				net.tourbook.common.UI.openControlMenu(_linkTag);
			}
		});

		_lblTourTags = new Label(parent, SWT.WRAP);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				// hint is necessary that the width is not expanded when the text is very long
				.hint(200, SWT.DEFAULT)
				.span(2, 1)
				.applyTo(_lblTourTags);
	}

	/**
	 * person
	 */
	private void createUI_40_Person(final Composite parent) {

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
	private void createUI_50_DescriptionMarker(final Composite parent, final SelectionAdapter defaultSelectionAdapter) {

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

	private void enableControls() {

		final boolean isCustomTime = _chkKeepOriginalDateTime.getSelection() == false;
		final boolean isCustomTourType = getStateTourTypeSource().equals(STATE_TYPE_SOURCE_CUSTOM);
		final boolean isCustomTourTitle = getStateTourTitleSource().equals(STATE_TOUR_TITLE_SOURCE_CUSTOM);

		_txtTourTitle.setEditable(isCustomTourTitle);

		_dtTourDate.setEnabled(isCustomTime);
		_dtTourTime.setEnabled(isCustomTime);
		_lblTourStartDate.setEnabled(isCustomTime);
		_lblTourStartTime.setEnabled(isCustomTime);

		_linkTourType.setEnabled(isCustomTourType);
		_lblTourType.setEnabled(isCustomTourType);

		// enable/disable actions for tags/tour types
//		TagManager.enableRecentTagActions(true, _tourDataTarget.getTourTags());
	}

//	/**
//	 * info
//	 */
//	private void createUI60Info(final Composite container) {
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
//		final String infoText = Messages.Dialog_SplitTour_Label_OtherFieldsInfo;
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

	/**
	 * Create a new tour with the extracted time slices
	 */
	private boolean extractTour() {

		/*
		 * get data series
		 */
		final float[] tourAltitudeSerie = _tourDataSource.altitudeSerie;
		final float[] tourCadenceSerie = _tourDataSource.cadenceSerie;
		final float[] tourDistanceSerie = _tourDataSource.distanceSerie;
		final long[] tourGearSerie = _tourDataSource.gearSerie;
		final double[] tourLatitudeSerie = _tourDataSource.latitudeSerie;
		final double[] tourLongitudeSerie = _tourDataSource.longitudeSerie;
		final float[] tourPulseSerie = _tourDataSource.pulseSerie;
		final float[] tourTemperatureSerie = _tourDataSource.temperatureSerie;
		final int[] tourTimeSerie = _tourDataSource.timeSerie;

		final boolean isTourAltitude = (tourAltitudeSerie != null) && (tourAltitudeSerie.length > 0);
		final boolean isTourCadence = (tourCadenceSerie != null) && (tourCadenceSerie.length > 0);
		final boolean isTourDistance = (tourDistanceSerie != null) && (tourDistanceSerie.length > 0);
		final boolean isTourGear = (tourGearSerie != null) && (tourGearSerie.length > 0);
		final boolean isTourLat = (tourLatitudeSerie != null) && (tourLatitudeSerie.length > 0);
		final boolean isTourLon = (tourLongitudeSerie != null) && (tourLongitudeSerie.length > 0);
		final boolean isTourPulse = (tourPulseSerie != null) && (tourPulseSerie.length > 0);
		final boolean isTourTemperature = (tourTemperatureSerie != null) && (tourTemperatureSerie.length > 0);
		final boolean isTourTime = (tourTimeSerie != null) && (tourTimeSerie.length > 0);

		/*
		 * get speed/power data when data are created by the device
		 */
		float[] tourPowerSerie = null;
		float[] tourSpeedSerie = null;
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
		} else if (isTourGear) {
			dataSerieLength = tourGearSerie.length;
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
			// _extractEndIndex contains -1, set end index to the last time slice
			_extractEndIndex = dataSerieLength - 1;
		}

		final int extractSerieLength = _extractEndIndex - _extractStartIndex + 1;

		final float[] extractAltitudeSerie = new float[extractSerieLength];
		final float[] extractCadenceSerie = new float[extractSerieLength];
		final float[] extractDistanceSerie = new float[extractSerieLength];
		final long[] extractGearSerie = new long[extractSerieLength];
		final double[] extractLatitudeSerie = new double[extractSerieLength];
		final double[] extractLongitudeSerie = new double[extractSerieLength];
		final float[] extractPowerSerie = new float[extractSerieLength];
		final float[] extractPulseSerie = new float[extractSerieLength];
		final float[] extractSpeedSerie = new float[extractSerieLength];
		final float[] extractTemperatureSerie = new float[extractSerieLength];
		final int[] extractTimeSerie = new int[extractSerieLength];

		final HashSet<TourMarker> extractedTourMarker = new HashSet<TourMarker>();
		final ArrayList<TourWayPoint> extractedWayPoints = new ArrayList<TourWayPoint>();

		/*
		 * get start date/time
		 */
		DateTime extractedTourStart = null;
		final boolean isOriginalTime = _chkKeepOriginalDateTime.getSelection();
		if (isOriginalTime) {
			extractedTourStart = _extractedTourStartTime;
		} else {
			extractedTourStart = new DateTime(
					_dtTourDate.getYear(),
					_dtTourDate.getMonth() + 1,
					_dtTourDate.getDay(),
					_dtTourTime.getHours(),
					_dtTourTime.getMinutes(),
					_dtTourTime.getSeconds(),
					0);
		}

		int relTourStartTime = 0;
		int extractedRecordingTime = 0;
		if (isTourTime) {
			relTourStartTime = tourTimeSerie[_extractStartIndex];
			extractedRecordingTime = tourTimeSerie[_extractEndIndex] - relTourStartTime;
		}

		// get distance
		float extractedDistance = 0;
		float relTourStartDistance = 0;
		if (isTourDistance) {
			relTourStartDistance = tourDistanceSerie[_extractStartIndex];
			extractedDistance = tourDistanceSerie[_extractEndIndex] - relTourStartDistance;
		}

		/*
		 * copy existing data series
		 */
		int extractedSerieIndex = 0;
		for (int sourceSerieIndex = _extractStartIndex; sourceSerieIndex <= _extractEndIndex; sourceSerieIndex++) {

			if (isTourTime) {
				extractTimeSerie[extractedSerieIndex] = tourTimeSerie[sourceSerieIndex] - relTourStartTime;
			}

			if (isTourAltitude) {
				extractAltitudeSerie[extractedSerieIndex] = tourAltitudeSerie[sourceSerieIndex];
			}
			if (isTourCadence) {
				extractCadenceSerie[extractedSerieIndex] = tourCadenceSerie[sourceSerieIndex];
			}

			if (isTourDistance) {
				extractDistanceSerie[extractedSerieIndex] = tourDistanceSerie[sourceSerieIndex] - relTourStartDistance;
			}

			if (isTourGear) {
				extractGearSerie[extractedSerieIndex] = tourGearSerie[sourceSerieIndex];
			}

			if (isTourLat) {
				extractLatitudeSerie[extractedSerieIndex] = tourLatitudeSerie[sourceSerieIndex];
			}

			if (isTourLon) {
				extractLongitudeSerie[extractedSerieIndex] = tourLongitudeSerie[sourceSerieIndex];
			}

			if (isTourPulse) {
				extractPulseSerie[extractedSerieIndex] = tourPulseSerie[sourceSerieIndex];
			}

			if (isTourTemperature) {
				extractTemperatureSerie[extractedSerieIndex] = tourTemperatureSerie[sourceSerieIndex];
			}

			if (isTourPower) {
				extractPowerSerie[extractedSerieIndex] = tourPowerSerie[sourceSerieIndex];
			}

			if (isTourSpeed) {
				extractSpeedSerie[extractedSerieIndex] = tourSpeedSerie[sourceSerieIndex];
			}

			extractedSerieIndex++;
		}

		/*
		 * get tour markers, way points
		 */
		final Set<TourMarker> tourMarkers = _tourDataSource.getTourMarkers();
		if (_chkIncludeMarkerWaypoints.getSelection()) {

			for (final TourMarker tourMarker : tourMarkers) {

				final int markerSerieIndex = tourMarker.getSerieIndex();

				// skip marker which are not within the extracted time slices
				if (markerSerieIndex < _extractStartIndex || markerSerieIndex > _extractEndIndex) {
					continue;
				}

				final int extractedMarkerIndex = markerSerieIndex - _extractStartIndex;

				final TourMarker extractedMarker = tourMarker.clone(_tourDataTarget);

				// adjust marker position, position is relativ to the tour start
				extractedMarker.setSerieIndex(extractedMarkerIndex);

				if (isTourTime) {

					final int extractedRelativeTime = extractTimeSerie[extractedMarkerIndex];

					tourMarker.setTime(//
							extractedRelativeTime,
							_tourDataSource.getTourStartTimeMS() + (extractedRelativeTime * 1000));
				}
				if (isTourDistance) {
					tourMarker.setDistance(extractDistanceSerie[extractedMarkerIndex]);
				}

				if (extractAltitudeSerie != null) {
					tourMarker.setAltitude(extractAltitudeSerie[extractedMarkerIndex]);
				}

				if (extractLatitudeSerie != null) {
					tourMarker.setGeoPosition(
							extractLatitudeSerie[extractedMarkerIndex],
							extractLongitudeSerie[extractedMarkerIndex]);
				}

				extractedTourMarker.add(extractedMarker);
			}

			/*
			 * copy all way points, they can be independant of the tour
			 */
			for (final TourWayPoint sourceWayPoint : _tourDataSource.getTourWayPoints()) {
				extractedWayPoints.add(sourceWayPoint.clone(_tourDataTarget));
			}
		}

		// get description
		String extractedDescription = UI.EMPTY_STRING;
		if (_chkIncludeDescription.getSelection()) {
			extractedDescription = _tourDataSource.getTourDescription();
		}

		/*
		 * get calories
		 */
		int extractedCalories = 0;
		if (_extractStartIndex == 0 && _extractEndIndex == (dataSerieLength - 1)) {

			// tour is copied, the calories can also be copied
			extractedCalories = _tourDataSource.getCalories();

		} else {
			// TODO calories should be set when they are computed
		}

		/*
		 * set target tour data
		 */
		_tourDataTarget.setTourStartTime(extractedTourStart);

		// tour id must be created after the tour date/time is set
		_tourDataTarget.createTourId();

		_tourDataTarget.setTourTitle(_txtTourTitle.getText());
		_tourDataTarget.setTourDescription(extractedDescription.toString());

		_tourDataTarget.setTourMarkers(extractedTourMarker);
		_tourDataTarget.setWayPoints(extractedWayPoints);

		_tourDataTarget.setDeviceName(_isSplitTour
				? Messages.Dialog_SplitTour_Label_DeviceName
				: Messages.Dialog_ExtractTour_Label_DeviceName);

		_tourDataTarget.setIsDistanceFromSensor(_tourDataSource.isDistanceSensorPresent());
		_tourDataTarget.setIsPowerSensorPresent(_tourDataSource.isDistanceSensorPresent());
		_tourDataTarget.setIsPulseSensorPresent(_tourDataSource.isPulseSensorPresent());

		_tourDataTarget.setDeviceTimeInterval(_tourDataSource.getDeviceTimeInterval());

		_tourDataTarget.setTourRecordingTime(extractedRecordingTime);
		_tourDataTarget.setTourDistance(extractedDistance);

		_tourDataTarget.setWeather(_tourDataSource.getWeather());
		_tourDataTarget.setWeatherClouds(_tourDataSource.getWeatherClouds());
		_tourDataTarget.setWeatherWindDir(_tourDataSource.getWeatherWindDir());
		_tourDataTarget.setWeatherWindSpeed(_tourDataSource.getWeatherWindSpeed());

		_tourDataTarget.setRestPulse(_tourDataSource.getRestPulse());
		_tourDataTarget.setCalories(extractedCalories);

		_tourDataTarget.setDpTolerance(_tourDataSource.getDpTolerance());

		if (isTourAltitude) {
			_tourDataTarget.altitudeSerie = extractAltitudeSerie;
		}
		if (isTourDistance) {
			_tourDataTarget.distanceSerie = extractDistanceSerie;
		}
		if (isTourCadence) {
			_tourDataTarget.cadenceSerie = extractCadenceSerie;
		}
		if (isTourGear) {
			_tourDataTarget.setGears(extractGearSerie);
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
		if (isTourPulse) {
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
		_tourDataTarget.setTourPerson(getSelectedPerson());

		/*
		 * check size of the fields
		 */
		if (_tourDataTarget.isValidForSave() == false) {
			return false;
		}

		TourManager.saveModifiedTour(_tourDataTarget);

		// check if time slices should be removed
		if (getStateSplitMethod().equals(STATE_EXTRACT_METHOD_REMOVE)) {

			TourManager.removeTimeSlices(_tourDataSource, _extractStartIndex, _extractEndIndex, true);

			_tourDataEditor.updateUI(_tourDataSource, true);

			TourManager.fireEvent(TourEventId.TOUR_CHANGED, new TourEvent(_tourDataSource));
		}

		return true;
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

		// return extracted tour

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
		final Set<TourTag> extractedTourTags = new HashSet<TourTag>();
		final Set<TourTag> tourTags = _tourDataSource.getTourTags();
		extractedTourTags.addAll(tourTags);
		_tourDataTarget.setTourTags(extractedTourTags);

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
				STATE_TOUR_TYPE_ID,
				TourDatabase.ENTITY_IS_NOT_SAVED);

		final String stateTourTypeSource = Util.getStateString(
				_state,
				STATE_TYPE_SOURCE,
				STATE_TYPE_SOURCE_FROM_SELECTED_TOURS);

		long extractedTourTypeId = TourDatabase.ENTITY_IS_NOT_SAVED;

		if (stateTourTypeSource.equals(STATE_TYPE_SOURCE_FROM_SELECTED_TOURS)) {
			extractedTourTypeId = _tourTypeIdFromSelectedTours;
		} else if (stateTourTypeSource.equals(STATE_TYPE_SOURCE_FROM_PREVIOUS_TOUR)
				|| stateTourTypeSource.equals(STATE_TYPE_SOURCE_CUSTOM)) {
			extractedTourTypeId = _tourTypeIdPreviousSplittedTour;
		}

		_tourDataTarget.setTourType(TourDatabase.getTourType(extractedTourTypeId));
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

		updateUITourTitle();
		enableControls();
	}

	private void onSelectTourTypeSource() {

		final String stateTourTypeSource = getStateTourTypeSource();

		long extractedTourTypeId = TourDatabase.ENTITY_IS_NOT_SAVED;

		if (stateTourTypeSource.equals(STATE_TYPE_SOURCE_FROM_SELECTED_TOURS)) {
			extractedTourTypeId = _tourTypeIdFromSelectedTours;
		} else if (stateTourTypeSource.equals(STATE_TYPE_SOURCE_FROM_PREVIOUS_TOUR)) {
			extractedTourTypeId = _tourTypeIdPreviousSplittedTour;
		} else if (stateTourTypeSource.equals(STATE_TYPE_SOURCE_CUSTOM)) {
			extractedTourTypeId = _tourTypeIdCustom;
		}

		_tourDataTarget.setTourType(TourDatabase.getTourType(extractedTourTypeId));

		updateUITourTypeTags();

		enableControls();
	}

	private void restoreState() {

		_txtTourTitle.setText(Util.getStateString(
				_state,
				STATE_TOUR_TITLE,
				Messages.Dialog_SplitTour_Label_DefaultTitle));

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

		final DateTime tourStartTime = _tourDataSource.getTourStartTime();

		int relativeExtractedStartTime = 0;

		final int[] tourTimeSerie = _tourDataSource.timeSerie;
		final boolean isTourTime = (tourTimeSerie != null) && (tourTimeSerie.length > 0);
		if (isTourTime) {
			relativeExtractedStartTime = tourTimeSerie[_extractStartIndex];
		}
		_extractedTourStartTime = tourStartTime.plusSeconds(relativeExtractedStartTime);

		// date/time
		_dtTourDate.setDate(
				_extractedTourStartTime.getYear(),
				_extractedTourStartTime.getMonthOfYear() - 1,
				_extractedTourStartTime.getDayOfMonth());

		_dtTourTime.setTime(
				_extractedTourStartTime.getHourOfDay(),
				_extractedTourStartTime.getMinuteOfHour(),
				_extractedTourStartTime.getSecondOfMinute());

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
		final TourType tourType = _tourDataTarget.getTourType();
		_state.put(STATE_TOUR_TYPE_ID, tourType == null ? TourDatabase.ENTITY_IS_NOT_SAVED : tourType.getTypeId());
		_state.put(STATE_TYPE_SOURCE, getStateTourTypeSource());

		// split method
		_state.put(STATE_EXTRACT_METHOD, getStateSplitMethod());

		// tour start date/time
		_state.put(STATE_IS_KEEP_ORIGINAL_TIME, _chkKeepOriginalDateTime.getSelection());

		// description/marker
		_state.put(STATE_IS_INCLUDE_DESCRIPTION, _chkIncludeDescription.getSelection());
		_state.put(STATE_IS_INCLUDE_MARKER_WAYPOINTS, _chkIncludeMarkerWaypoints.getSelection());

		// person
		_state.put(STATE_PERSON_ID, getSelectedPerson().getPersonId());
	}

	@Override
	public void toursAreModified(final ArrayList<TourData> modifiedTours) {

		if ((modifiedTours != null) && (modifiedTours.size() > 0)) {

			// check if it's the correct tour
			if (_tourDataTarget == modifiedTours.get(0)) {

				// update custom tour type id
				final String stateTourTypeSource = getStateTourTypeSource();

				if (stateTourTypeSource.equals(STATE_TYPE_SOURCE_CUSTOM)) {

					final TourType tourType = _tourDataTarget.getTourType();

					_tourTypeIdCustom = tourType == null ? //
							TourDatabase.ENTITY_IS_NOT_SAVED
							: tourType.getTypeId();
				}

				// tour type or tags can have been changed within this dialog
				updateUITourTypeTags();

				// enable/disable tag/type context menu
				enableControls();
			}
		}
	}

	private void updateUIFromModel() {

		updateUITourTitle();

		if (_canRemoveTimeSlices == false) {

			// select option that time slices cannot be removed

			Util.selectStateInCombo(
					_state,
					STATE_EXTRACT_METHOD,
					ALL_STATES_EXTRACT_METHOD,
					STATE_EXTRACT_METHOD_KEEP,
					_cboSplitMethod);

			_cboSplitMethod.setEnabled(false);
		}
	}

	/**
	 * update tour title
	 */
	private void updateUITourTitle() {

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
		UI.updateUI_TourType(_tourDataTarget, _lblTourType, true);
		UI.updateUI_Tags(_tourDataTarget, _lblTourTags);

		// reflow layout that the tags are aligned correctly
		_dlgInnerContainer.layout(true);
	}

}
