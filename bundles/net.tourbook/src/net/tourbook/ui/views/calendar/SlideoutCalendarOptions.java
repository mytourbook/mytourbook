/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.calendar;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.color.ColorSelectorExtended;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.font.IFontEditorListener;
import net.tourbook.common.font.SimpleFontEditor;
import net.tourbook.common.formatter.FormatManager;
import net.tourbook.common.formatter.ValueFormat;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.common.tooltip.SlideoutLocation;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.ui.views.calendar.CalendarProfileManager.CalendarColor_ComboData;
import net.tourbook.ui.views.calendar.CalendarProfileManager.ColumnLayout_ComboData;
import net.tourbook.ui.views.calendar.CalendarProfileManager.DateColumn_ComboData;
import net.tourbook.ui.views.calendar.CalendarProfileManager.DayContentColor_ComboData;
import net.tourbook.ui.views.calendar.CalendarProfileManager.DayHeaderDateFormat_ComboData;
import net.tourbook.ui.views.calendar.CalendarProfileManager.ICalendarProfileListener;
import net.tourbook.ui.views.calendar.CalendarProfileManager.ProfileDefaultId_ComboData;
import net.tourbook.ui.views.calendar.CalendarProfileManager.TourBackground_ComboData;
import net.tourbook.ui.views.calendar.CalendarProfileManager.TourBorder_ComboData;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;

/**
 * Properties slideout for the calendar view.
 */
public class SlideoutCalendarOptions extends AdvancedSlideout implements ICalendarProfileListener,
		IColorSelectorListener {

// SET_FORMATTING_OFF
	//
	private static final String							STATE_SELECTED_TAB		= "STATE_SELECTED_TAB";		//$NON-NLS-1$
	//
	private static final IDialogSettings				_state					= TourbookPlugin.getState("SlideoutCalendarOptions");																//$NON-NLS-1$
	private static final ArrayList<CalendarProfile>		_allCalendarProfiles	= CalendarProfileManager.getAllCalendarProfiles();
	//
// SET_FORMATTING_ON
	//
	static {}
	//
	/**
	 * Contains the app default id's and the user default id's
	 */
	ArrayList<ProfileDefaultId_ComboData>	_allDefaultComboData	= new ArrayList<>();
	//
	private IFontEditorListener				_defaultFontEditorListener;
	private MouseWheelListener				_defaultMouseWheelListener;
	private IPropertyChangeListener			_defaultPropertyChangeListener;
	private SelectionAdapter				_defaultSelectionListener;
	private FocusListener					_keepOpenListener;
	private SelectionAdapter				_tourValueListener;
	private SelectionAdapter				_weekValueListener;
	//
	private long							_dragStartViewerLeft;
	//
	private boolean							_isUpdateUI;
	//
	private PixelConverter					_pc;
	private int								_subItemIndent;

	/*
	 * This is a hack to vertical center the font label, otherwise it will be complicated to set it
	 * correctly
	 */
	private int								_fontLabelVIndent		= 5;

	/*
	 * UI controls
	 */
	private CalendarView					_calendarView;
	//
	private Button							_btnProfile_Copy;
	private Button							_btnProfile_Delete;
	private Button							_btnApplyAppDefaults;
	//
	private Button							_chkIsHideDayDateWhenNoTour;
	private Button							_chkIsShowDateColumn;
	private Button							_chkIsShowDayDateWeekendColor;
	private Button							_chkIsShowDayDate;
	private Button							_chkIsShowMonthColor;
	private Button							_chkIsShowSummaryColumn;
	private Button							_chkIsShowTourContent;
	private Button							_chkIsShowTourValueUnit;
	private Button							_chkIsShowWeekValueUnit;
	private Button							_chkIsShowYearColumns;
	private Button							_chkIsTruncateTourText;
	private Button							_chkIsUserDefaultId;
	private Button							_chkUseDraggedScrolling;
	//
	private Button							_rdoWeekRow_Height;
	private Button							_rdoWeekRow_Number;
	private Button							_rdoYear_ColumnNumber;
	private Button							_rdoYear_ColumnDayWidth;
	//
	private Button[]						_chkTour_AllIsShowLines;
	private Button[]						_chkWeek_AllIsShowLines;
	//
	private ColorSelectorExtended			_colorMonth_AlternateColor;
	private ColorSelectorExtended			_colorMonth_AlternateColor2;
	private ColorSelectorExtended			_colorCalendar_BackgroundColor;
	private ColorSelectorExtended			_colorCalendar_ForegroundColor;
	private ColorSelectorExtended			_colorDay_HoveredColor;
	private ColorSelectorExtended			_colorDay_SelectedColor;
	private ColorSelectorExtended			_colorTour_BackgroundColor1;
	private ColorSelectorExtended			_colorTour_BackgroundColor2;
	private ColorSelectorExtended			_colorTour_BorderColor;
	private ColorSelectorExtended			_colorTour_ContentColor;
	private ColorSelectorExtended			_colorTour_DraggedColor;
	private ColorSelectorExtended			_colorTour_HoveredColor;
	private ColorSelectorExtended			_colorTour_SelectedColor;
	private ColorSelectorExtended			_colorTour_TitleColor;
	private ColorSelectorExtended			_colorTour_ValueColor;
	private ColorSelectorExtended			_colorWeek_ValueColor;
	//
	private Combo							_comboDateColumn;
	private Combo							_comboDayHeaderDateFormat;
	private Combo							_comboProfiles;
	private Combo							_comboProfile_AllDefaultId;
	private Combo							_comboTour_Background;
	private Combo							_comboTour_BackgroundColor1;
	private Combo							_comboTour_BackgroundColor2;
	private Combo							_comboTour_BorderLayout;
	private Combo							_comboTour_BorderColor;
	private Combo							_comboTour_ContentColor;
	private Combo							_comboTour_DraggedColor;
	private Combo							_comboTour_HoveredColor;
	private Combo							_comboTour_SelectedColor;
	private Combo							_comboTour_TitleColor;
	private Combo							_comboTour_ValueColor;
	private Combo							_comboWeek_ValueColor;
	private Combo							_comboYear_ColumnStart;
	//
	private Combo[]							_comboTour_AllValues;
	private Combo[]							_comboTour_AllFormats;
	private Combo[]							_comboWeek_AllValues;
	private Combo[]							_comboWeek_AllFormats;
	//
	private Composite						_tourFormatterContainer;
	private Composite						_weekFormatterContainer;
	//
	private Label							_lblDateColumn_Content;
	private Label							_lblDateColumn_Font;
	private Label							_lblDateColumn_Width;
	private Label							_lblDayDate_Margin;
	private Label							_lblDayHeader_Font;
	private Label							_lblDayHeader_Format;
	private Label							_lblProfile_Name;
	private Label							_lblProfile_AppDefaultId;
	private Label							_lblProfile_UserDefaultId;
	private Label							_lblProfile_UserParentDefaultId;
	private Label							_lblTour_ContentFont;
	private Label							_lblTour_Margin;
	private Label							_lblTour_TitleFont;
	private Label							_lblTour_TruncatedLines;
	private Label							_lblTour_ValueColumns;
	private Label							_lblTour_ValueFont;
	private Label							_lblWeek_ColumnWidth;
	private Label							_lblWeek_ValueFont;
	private Label							_lblWeek_Margin;
	private Label							_lblYearColumn_HeaderFont;
	private Label							_lblYear_ColumnSpacing;
	private Label							_lblYear_ColumnStart;
	//
	private SimpleFontEditor				_fontEditorDayDate;
	private SimpleFontEditor				_fontEditorDateColumn;
	private SimpleFontEditor				_fontEditorTourTitle;
	private SimpleFontEditor				_fontEditorTourContent;
	private SimpleFontEditor				_fontEditorTourValue;
	private SimpleFontEditor				_fontEditorWeekValue;
	private SimpleFontEditor				_fontEditorYearColumnHeader;
	//
	private Spinner							_spinnerDateColumnWidth;
	private Spinner							_spinnerDayDate_Margin_Top;
	private Spinner							_spinnerDayDate_Margin_Left;
	private Spinner							_spinnerTour_BackgroundWidth;
	private Spinner							_spinnerTour_BorderWidth;
	private Spinner							_spinnerTour_Margin_Top;
	private Spinner							_spinnerTour_Margin_Bottom;
	private Spinner							_spinnerTour_Margin_Left;
	private Spinner							_spinnerTour_Margin_Right;
	private Spinner							_spinnerTour_TruncatedLines;
	private Spinner							_spinnerTour_ValueColumns;
	private Spinner							_spinnerWeek_ColumnWidth;
	private Spinner							_spinnerWeek_Height;
	private Spinner							_spinnerWeek_Margin_Top;
	private Spinner							_spinnerWeek_Margin_Left;
	private Spinner							_spinnerWeek_Margin_Bottom;
	private Spinner							_spinnerWeek_Margin_Right;
	private Spinner							_spinnerWeek_Rows;
	private Spinner							_spinnerYear_Columns;
	private Spinner							_spinnerYear_ColumnSpacing;
	private Spinner							_spinnerYear_DayWidth;
	//
	private TabFolder						_tabFolder;
	//
	private TableViewer						_profileViewer;
	//
	private Text							_txtProfileName;
	private Text							_txtUserDefaultId;
	private Text							_txtUserParentDefaultId;
	//
	private ToolItem						_toolItem;

	private class ProfileProvider implements IStructuredContentProvider {

		@Override
		public void dispose() {}

		@Override
		public Object[] getElements(final Object inputElement) {
			return _allCalendarProfiles.toArray();
		}

		@Override
		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	/**
	 * @param ownerControl
	 * @param toolItem
	 * @param state
	 * @param calendarView
	 * @param gridPrefPrefix
	 */
	public SlideoutCalendarOptions(	final ToolItem toolItem,
									final IDialogSettings state,
									final CalendarView calendarView) {

		super(toolItem.getParent(), state, null);

		_calendarView = calendarView;
		_toolItem = toolItem;

		setTitleText(Messages.Slideout_CalendarOptions_Label_Title);
		setSlideoutLocation(SlideoutLocation.BELOW_RIGHT);
	}

	@Override
	public void colorDialogOpened(final boolean isDialogOpened) {

		setIsAnotherDialogOpened(isDialogOpened);
	}

	private void createActions() {

	}

	@Override
	protected void createSlideoutContent(final Composite parent) {

		createActions();
		createUI(parent);

		fillUI();
		fillUI_Profiles();

		restoreState_UI();

		// load viewer
		_profileViewer.setInput(new Object());

		// first load the viewer, then select the profile
		restoreState_Profile();

		enableControls();
		enableControls_Profiles();
		enableControls_ProfileProperties();
	}

	@Override
	protected void createTitleBarControls(final Composite parent) {

		// this method is called 1st

		initUI(parent);

		{
			/*
			 * Combo: Profiles
			 */
			_comboProfiles = new Combo(parent, SWT.READ_ONLY | SWT.BORDER);
			_comboProfiles.setVisibleItemCount(50);
			_comboProfiles.addFocusListener(_keepOpenListener);
			_comboProfiles.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectProfile();
				}
			});
			GridDataFactory
					.fillDefaults()
					.grab(true, false)
					.align(SWT.FILL, SWT.CENTER)
					.indent(0, 0)
					.hint(_pc.convertWidthInCharsToPixels(10), SWT.DEFAULT)
					.applyTo(_comboProfiles);
		}
	}

	private Composite createUI(final Composite parent) {

		final Composite shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(shellContainer);
		{
			_tabFolder = new TabFolder(shellContainer, SWT.TOP);
			GridDataFactory
					.fillDefaults()
					.indent(0, 5)
					.applyTo(_tabFolder);
			{
				// profiles
				final TabItem tabProfile = new TabItem(_tabFolder, SWT.NONE);
				tabProfile.setControl(createUI_100_Tab_Profile(_tabFolder));
				tabProfile.setText(Messages.Slideout_CalendarOptions_Tab_Profiles);

				// calendar layout
				final TabItem tabLayout = new TabItem(_tabFolder, SWT.NONE);
				tabLayout.setControl(createUI_200_Tab_CalendarLayout(_tabFolder));
				tabLayout.setText(Messages.Slideout_CalendarOptions_Tab_CalendarLayout);

				// tour layout
				final TabItem tabDayContent = new TabItem(_tabFolder, SWT.NONE);
				tabDayContent.setControl(createUI_400_Tab_TourLayout(_tabFolder));
				tabDayContent.setText(Messages.Slideout_CalendarOptions_Tab_TourLayout);

				// tour content
				final TabItem tabTour = new TabItem(_tabFolder, SWT.NONE);
				tabTour.setControl(createUI_600_Tab_TourContent(_tabFolder));
				tabTour.setText(Messages.Slideout_CalendarOptions_Tab_TourContent);

				// week
				final TabItem tabWeekSummary = new TabItem(_tabFolder, SWT.NONE);
				tabWeekSummary.setControl(createUI_800_Tab_WeekSummary(_tabFolder));
				tabWeekSummary.setText(Messages.Slideout_CalendarOptions_Tab_WeekSummary);
			}

		}

		return shellContainer;
	}

	private Control createUI_100_Tab_Profile(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		{
			createUI_110_Profiles(container);
			createUI_150_ProfileData(container);
		}

		return container;
	}

	private Composite createUI_110_Profiles(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory
				.fillDefaults()
				.numColumns(2)
				.spacing(5, 3)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		{
			{
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Slideout_CalendarOptions_Label_Profiles);
				GridDataFactory.fillDefaults().span(2, 1).applyTo(label);
			}

			createUI_120_ProfileViewer(container);
			createUI_122_DragDrop();

			createUI_130_ProfileActions(container);

			{
				// hint to use drag & drop
				final Label label = new Label(parent, SWT.WRAP);
				label.setText(Messages.Slideout_CalendarOptions_Label_Profile_DragDropHint);
				GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(label);
			}
		}

		return container;
	}

	private void createUI_120_ProfileViewer(final Composite parent) {

		final Composite layoutContainer = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()//
				.grab(true, true)
				.hint(_pc.convertWidthInCharsToPixels(50), _pc.convertHeightInCharsToPixels(10))
				.applyTo(layoutContainer);

		final TableColumnLayout tableLayout = new TableColumnLayout();
		layoutContainer.setLayout(tableLayout);

		/*
		 * create table
		 */
		final Table table = new Table(layoutContainer, SWT.FULL_SELECTION);

		table.setLayout(new TableLayout());

		// !!! this prevents that the horizontal scrollbar is displayed, but is not always working :-(
//		table.setHeaderVisible(false);
		table.setHeaderVisible(true);

		_profileViewer = new TableViewer(table);

		/*
		 * create columns
		 */
		TableViewerColumn tvc;
		TableColumn tc;

		{
			// Column: Profile name

			tvc = new TableViewerColumn(_profileViewer, SWT.LEAD);
			tc = tvc.getColumn();
			tc.setText(Messages.Slideout_CalendarOptions_ColumnHeader_Name);
			tvc.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(final ViewerCell cell) {

					final CalendarProfile profile = (CalendarProfile) cell.getElement();

					cell.setText(profile.profileName);
				}
			});
			tableLayout.setColumnData(tc, new ColumnWeightData(3, false));
		}
		{
			// Column: Default ID

			tvc = new TableViewerColumn(_profileViewer, SWT.LEAD);
			tc = tvc.getColumn();
			tc.setText(Messages.Slideout_CalendarOptions_ColumnHeader_DefaultId);
			tvc.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(final ViewerCell cell) {

					final CalendarProfile profile = (CalendarProfile) cell.getElement();
					final DefaultId defaultId = profile.defaultId;

					// show translated default ID
					for (final ProfileDefaultId_ComboData comboData : CalendarProfileManager
							.getAllAppDefault_ComboData()) {

						if (comboData.defaultId.equals(defaultId)) {

							// this shows the default id
							cell.setText(comboData.labelOrUserId);
							return;
						}
					}

					cell.setText("???"); //$NON-NLS-1$
				}
			});
			tableLayout.setColumnData(tc, new ColumnWeightData(3, false));
		}
		{
			// Column: User parent default ID

			tvc = new TableViewerColumn(_profileViewer, SWT.LEAD);
			tc = tvc.getColumn();
			tc.setText(Messages.Slideout_CalendarOptions_ColumnHeader_UserParentDefaultId);
			tvc.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(final ViewerCell cell) {

					final CalendarProfile profile = (CalendarProfile) cell.getElement();

					cell.setText(profile.userParentDefaultId);
				}
			});
			tableLayout.setColumnData(tc, new ColumnWeightData(3, false));
		}
		{
			// Column: User default ID

			tvc = new TableViewerColumn(_profileViewer, SWT.LEAD);
			tc = tvc.getColumn();
			tc.setText(Messages.Slideout_CalendarOptions_ColumnHeader_UserDefaultId);
			tvc.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(final ViewerCell cell) {

					final CalendarProfile profile = (CalendarProfile) cell.getElement();

					cell.setText(profile.userDefaultId);
				}
			});
			tableLayout.setColumnData(tc, new ColumnWeightData(3, false));
		}
		{
			// Column: Is App ID

			tvc = new TableViewerColumn(_profileViewer, SWT.CENTER);
			tc = tvc.getColumn();
			tc.setText(Messages.Slideout_CalendarOptions_ColumnHeader_IsAppId);
			tc.setToolTipText(Messages.Slideout_CalendarOptions_ColumnHeader_IsAppId_Tooltip);
			tvc.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(final ViewerCell cell) {

					final CalendarProfile profile = (CalendarProfile) cell.getElement();

					cell.setText(String.valueOf(profile.isDefaultDefault ? 'x' : ' '));
				}
			});
			tableLayout.setColumnData(tc, new ColumnWeightData(1, false));
		}
		{
			// Column: Is User ID

			tvc = new TableViewerColumn(_profileViewer, SWT.CENTER);
			tc = tvc.getColumn();
			tc.setText(Messages.Slideout_CalendarOptions_ColumnHeader_IsUserId);
			tc.setToolTipText(Messages.Slideout_CalendarOptions_ColumnHeader_IsUserId_Tooltip);
			tvc.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(final ViewerCell cell) {

					final boolean isUserDefault = ((CalendarProfile) cell.getElement()).isUserParentDefault;

					cell.setText(String.valueOf(isUserDefault ? 'x' : ' '));
				}
			});
			tableLayout.setColumnData(tc, new ColumnWeightData(1, false));
		}

//// this is for debugging
//		{
//			// Column: ID
//
//			tvc = new TableViewerColumn(_profileViewer, SWT.LEAD);
//			tc = tvc.getColumn();
//			tc.setText("Profile ID");//$NON-NLS-1$
//			tvc.setLabelProvider(new CellLabelProvider() {
//				@Override
//				public void update(final ViewerCell cell) {
//
//					final CalendarProfile profile = (CalendarProfile) cell.getElement();
//
//					cell.setText(profile.id);
//				}
//			});
//			tableLayout.setColumnData(tc, new ColumnWeightData(1, false));
//		}

		/*
		 * create table viewer
		 */
		_profileViewer.setContentProvider(new ProfileProvider());

// viewer is not sorted, with drag&drop the sequence can be set
//		_profileViewer.setComparator(new ProfileComparator());

		_profileViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				onProfile_Select();
			}
		});

		_profileViewer.addDoubleClickListener(new IDoubleClickListener() {

			@Override
			public void doubleClick(final DoubleClickEvent event) {

				// set focus to  profile name
				_txtProfileName.setFocus();
				_txtProfileName.selectAll();
			}
		});

		_profileViewer.getTable().addKeyListener(new KeyListener() {

			@Override
			public void keyPressed(final KeyEvent e) {

				if (e.keyCode == SWT.DEL) {
					onProfile_Delete();
				}
			}

			@Override
			public void keyReleased(final KeyEvent e) {}
		});
	}

	private void createUI_122_DragDrop() {

		/*
		 * set drag adapter
		 */
		_profileViewer.addDragSupport(
				DND.DROP_MOVE,
				new Transfer[] { LocalSelectionTransfer.getTransfer() },
				new DragSourceListener() {

					@Override
					public void dragFinished(final DragSourceEvent event) {

						final LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();

						if (event.doit == false) {
							return;
						}

						transfer.setSelection(null);
						transfer.setSelectionSetTime(0);
					}

					@Override
					public void dragSetData(final DragSourceEvent event) {
						// data are set in LocalSelectionTransfer
					}

					@Override
					public void dragStart(final DragSourceEvent event) {

						final LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
						final ISelection selection = _profileViewer.getSelection();

						transfer.setSelection(selection);
						transfer.setSelectionSetTime(_dragStartViewerLeft = event.time & 0xFFFFFFFFL);

						event.doit = !selection.isEmpty();
					}
				});

		/*
		 * set drop adapter
		 */
		final ViewerDropAdapter viewerDropAdapter = new ViewerDropAdapter(_profileViewer) {

			private Widget _tableItem;

			@Override
			public void dragOver(final DropTargetEvent dropEvent) {

				// keep table item
				_tableItem = dropEvent.item;

				super.dragOver(dropEvent);
			}

			@Override
			public boolean performDrop(final Object data) {

				if (data instanceof StructuredSelection) {
					final StructuredSelection selection = (StructuredSelection) data;

					if (selection.getFirstElement() instanceof CalendarProfile) {

						final CalendarProfile profileItem = (CalendarProfile) selection.getFirstElement();

						final int location = getCurrentLocation();
						final Table profileTable = _profileViewer.getTable();

						/*
						 * check if drag was startet from this profile, remove the profile item
						 * before the moved profile is inserted
						 */
						if (LocalSelectionTransfer.getTransfer().getSelectionSetTime() == _dragStartViewerLeft) {
							_profileViewer.remove(profileItem);
						}

						int profileIndex;

						if (_tableItem == null) {

							_profileViewer.add(profileItem);
							profileIndex = profileTable.getItemCount() - 1;

						} else {

							// get index of the target in the table
							profileIndex = profileTable.indexOf((TableItem) _tableItem);
							if (profileIndex == -1) {
								return false;
							}

							if (location == LOCATION_BEFORE) {
								_profileViewer.insert(profileItem, profileIndex);
							} else if (location == LOCATION_AFTER || location == LOCATION_ON) {
								_profileViewer.insert(profileItem, ++profileIndex);
							}
						}

						// update model
						_allCalendarProfiles.remove(profileItem);
						_allCalendarProfiles.add(profileIndex, profileItem);

						// reselect profile item
						_profileViewer.setSelection(new StructuredSelection(profileItem));

						// set focus to selection
						profileTable.setSelection(profileIndex);
						profileTable.setFocus();

						/*
						 * Update UI profile combos
						 */
						_calendarView.fillUI_Profiles();

						fillUI_Profiles();
						_isUpdateUI = true;
						{
							final int activeProfileIndex = CalendarProfileManager.getActiveCalendarProfileIndex();
							_comboProfiles.select(activeProfileIndex);
						}
						_isUpdateUI = false;

						return true;
					}
				}

				return false;
			}

			@Override
			public boolean validateDrop(final Object target, final int operation, final TransferData transferType) {

				final ISelection selection = LocalSelectionTransfer.getTransfer().getSelection();
				if (selection instanceof StructuredSelection) {
					final Object dragFilter = ((StructuredSelection) selection).getFirstElement();
					if (target == dragFilter) {
						return false;
					}
				}

				if (LocalSelectionTransfer.getTransfer().isSupportedType(transferType) == false) {
					return false;
				}

				return true;
			}

		};

		_profileViewer.addDropSupport(
				DND.DROP_MOVE,
				new Transfer[] { LocalSelectionTransfer.getTransfer() },
				viewerDropAdapter);

	}

	private void createUI_130_ProfileActions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()
				.align(SWT.FILL, SWT.BEGINNING)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{
			{
				/*
				 * Button: New
				 */
				final Button button = new Button(container, SWT.PUSH);
				button.setText(Messages.App_Action_New);
				button.setToolTipText(Messages.Slideout_CalendarOptions_Action_AddProfile_Tooltip);
				button.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onProfile_Add();
					}
				});

				// set button default width
				UI.setButtonLayoutData(button);
			}
			{
				/*
				 * Button: Copy
				 */
				_btnProfile_Copy = new Button(container, SWT.PUSH);
				_btnProfile_Copy.setText(Messages.App_Action_Copy);
				_btnProfile_Copy.setToolTipText(Messages.Slideout_CalendarOptions_Action_CopyProfile_Tooltip);
				_btnProfile_Copy.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onProfile_Copy();
					}
				});

				// set button default width
				UI.setButtonLayoutData(_btnProfile_Copy);
			}
			{
				/*
				 * Button: Delete
				 */
				_btnProfile_Delete = new Button(container, SWT.PUSH);
				_btnProfile_Delete.setText(Messages.App_Action_Delete_WithConfirm);
				_btnProfile_Delete.setToolTipText(Messages.Slideout_CalendarOptions_Action_DeleteProfile_Tooltip);
				_btnProfile_Delete.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onProfile_Delete();
					}
				});

				// set button default width
				UI.setButtonLayoutData(_btnProfile_Delete);
			}
		}
	}

	private void createUI_150_ProfileData(final Composite parent) {

		final SelectionAdapter selectionListener = new SelectionAdapter() {

			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModify_ProfileProperties();
			}
		};

		final ModifyListener modifyListener = new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent e) {
				onModify_ProfileProperties();
			}
		};

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory
				.fillDefaults()
				.numColumns(2)
				.extendedMargins(0, 0, 20, 0)
				.applyTo(container);
		{
			{
				/*
				 * Profile Name
				 */

				// label
				_lblProfile_Name = new Label(container, SWT.NONE);
				_lblProfile_Name.setText(Messages.Slideout_CalendarOptions_Label_Profile_Name);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_lblProfile_Name);

				// text
				_txtProfileName = new Text(container, SWT.BORDER);
				_txtProfileName.addModifyListener(modifyListener);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(_txtProfileName);
			}
			{
				/*
				 * Profile default ID
				 */

				// label
				_lblProfile_AppDefaultId = new Label(container, SWT.NONE);
				_lblProfile_AppDefaultId.setText(Messages.Slideout_CalendarOptions_Label_Profile_DefaultId);
				_lblProfile_AppDefaultId.setToolTipText(
						Messages.Slideout_CalendarOptions_Label_Profile_DefaultId_Tooltip);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_lblProfile_AppDefaultId);

				final Composite containerDefaultId = new Composite(container, SWT.NONE);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(containerDefaultId);
				GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerDefaultId);
				{
					// combo
					_comboProfile_AllDefaultId = new Combo(containerDefaultId, SWT.DROP_DOWN | SWT.READ_ONLY);
					_comboProfile_AllDefaultId.setVisibleItemCount(20);
					_comboProfile_AllDefaultId.addSelectionListener(selectionListener);
					_comboProfile_AllDefaultId.addFocusListener(_keepOpenListener);

					/*
					 * Button: Apply defaults
					 */
					_btnApplyAppDefaults = new Button(containerDefaultId, SWT.PUSH);
					_btnApplyAppDefaults.setText(Messages.App_Action_ApplyDefaults);
					_btnApplyAppDefaults.setToolTipText(Messages.App_Action_ApplyDefaults_Tooltip);
					_btnApplyAppDefaults.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(final SelectionEvent e) {
							onProfile_ApplyDefaults();
						}
					});
					GridDataFactory.fillDefaults().indent(10, 0).applyTo(_btnApplyAppDefaults);

					// set button default width
					UI.setButtonLayoutData(_btnApplyAppDefaults);
				}
			}
			{
				/*
				 * Is User default profile
				 */

				// checkbox
				_chkIsUserDefaultId = new Button(container, SWT.CHECK);
				_chkIsUserDefaultId.setText(Messages.Slideout_CalendarOptions_Checkbox_IsUserDefaultProfile);
				_chkIsUserDefaultId.setToolTipText(
						Messages.Slideout_CalendarOptions_Checkbox_IsUserDefaultProfile_Tooltip);
				_chkIsUserDefaultId.addSelectionListener(selectionListener);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).span(2, 1).applyTo(_chkIsUserDefaultId);
			}
			{
				/*
				 * User parent default id
				 */

				// label
				_lblProfile_UserParentDefaultId = new Label(container, SWT.NONE);
				_lblProfile_UserParentDefaultId.setText(
						Messages.Slideout_CalendarOptions_Label_Profile_UserParentDefaultID);
				GridDataFactory
						.fillDefaults()
						.align(SWT.FILL, SWT.CENTER)
						.indent(_subItemIndent, 0)
						.applyTo(_lblProfile_UserParentDefaultId);

				// text
				_txtUserParentDefaultId = new Text(container, SWT.BORDER);
				_txtUserParentDefaultId.addModifyListener(modifyListener);
				GridDataFactory
						.fillDefaults()
						.align(SWT.FILL, SWT.CENTER)
						.grab(true, false)
						.applyTo(_txtUserParentDefaultId);
			}
			{
				/*
				 * User default id
				 */

				// label
				_lblProfile_UserDefaultId = new Label(container, SWT.NONE);
				_lblProfile_UserDefaultId.setText(Messages.Slideout_CalendarOptions_Label_Profile_UserDefaultID);
				GridDataFactory
						.fillDefaults()
						.align(SWT.FILL, SWT.CENTER)
						//				.indent(_subItemIndent, 0)
						.applyTo(_lblProfile_UserDefaultId);

				// text
				_txtUserDefaultId = new Text(container, SWT.BORDER);
				_txtUserDefaultId.addModifyListener(modifyListener);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(_txtUserDefaultId);
			}
		}
	}

	private Control createUI_200_Tab_CalendarLayout(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(container);
		{
			createUI_210_Layout(container);
			createUI_240_DateColumn(container);
			createUI_260_YearColumns(container);
		}

		return container;
	}

	private void createUI_210_Layout(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Slideout_CalendarOptions_Group_Layout);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		GridLayoutFactory
				.swtDefaults()//
				.numColumns(2)
				.spacing(10, LayoutConstants.getSpacing().y)
				.applyTo(group);
//		group.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			createUI_212__Col1(group);
			createUI_214__Col2(group);
		}
	}

	private void createUI_212__Col1(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()
				.grab(true, false)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			{
				/*
				 * Calendar foreground color
				 */

				// label
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Slideout_CalendarOptions_Label_Calendar_ForegroundColor);

				// Color selector
				_colorCalendar_ForegroundColor = createUI_ColorSelector(container);
			}
			{
				/*
				 * Calendar background color
				 */

				// label
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Slideout_CalendarOptions_Label_Calendar_BackgroundColor);

				// Color selector
				_colorCalendar_BackgroundColor = createUI_ColorSelector(container);
			}
			{
				/*
				 * Hovered day color
				 */

				// label
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Slideout_CalendarOptions_Label_Day_HoveredColor);

				// Color selector
				_colorDay_HoveredColor = createUI_ColorSelector(container);
			}
			{
				/*
				 * Selected day color
				 */

				// label
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Slideout_CalendarOptions_Label_Day_SelectedColor);

				// Color selector
				_colorDay_SelectedColor = createUI_ColorSelector(container);
			}
		}
	}

	private void createUI_214__Col2(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			{
				/*
				 * Month alternate color
				 */

				// checkbox
				_chkIsShowMonthColor = new Button(container, SWT.CHECK);
				_chkIsShowMonthColor.setText(
						Messages.Slideout_CalendarOptions_Checkbox_IsToggleMonthColor);
				_chkIsShowMonthColor.setToolTipText(
						Messages.Slideout_CalendarOptions_Checkbox_IsToggleMonthColor_Tooltip);
				_chkIsShowMonthColor.addSelectionListener(_defaultSelectionListener);
				GridDataFactory
						.fillDefaults()//
						.applyTo(_chkIsShowMonthColor);

				final Composite toggleContainer = new Composite(container, SWT.NONE);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(toggleContainer);
				GridLayoutFactory.fillDefaults().numColumns(2).applyTo(toggleContainer);
				{
					// Color selectors
					_colorMonth_AlternateColor = createUI_ColorSelector(toggleContainer);
					_colorMonth_AlternateColor2 = createUI_ColorSelector(toggleContainer);
				}
			}
			{
				/*
				 * Number of week rows
				 */

				// radio
				_rdoWeekRow_Number = new Button(container, SWT.RADIO);
				_rdoWeekRow_Number.setText(Messages.Slideout_CalendarOptions_Radio_Weeks_ByNumber);
				_rdoWeekRow_Number.addSelectionListener(_defaultSelectionListener);

				// spinner
				_spinnerWeek_Rows = new Spinner(container, SWT.BORDER);
				_spinnerWeek_Rows.setMinimum(CalendarProfileManager.WEEK_ROWS_MIN);
				_spinnerWeek_Rows.setMaximum(CalendarProfileManager.WEEK_ROWS_MAX);
				_spinnerWeek_Rows.setIncrement(1);
				_spinnerWeek_Rows.setPageIncrement(2);
				_spinnerWeek_Rows.addSelectionListener(_defaultSelectionListener);
				_spinnerWeek_Rows.addMouseWheelListener(_defaultMouseWheelListener);
			}
			{
				/*
				 * Week height
				 */

				// radio: Set column width
				_rdoWeekRow_Height = new Button(container, SWT.RADIO);
				_rdoWeekRow_Height.setText(Messages.Slideout_CalendarOptions_Radio_Weeks_ByHeight);
				_rdoWeekRow_Height.addSelectionListener(_defaultSelectionListener);

				// spinner: height
				_spinnerWeek_Height = new Spinner(container, SWT.BORDER);
				_spinnerWeek_Height.setMinimum(CalendarProfileManager.WEEK_HEIGHT_MIN);
				_spinnerWeek_Height.setMaximum(CalendarProfileManager.WEEK_HEIGHT_MAX);
				_spinnerWeek_Height.setIncrement(1);
				_spinnerWeek_Height.setPageIncrement(10);
				_spinnerWeek_Height.addSelectionListener(_defaultSelectionListener);
				_spinnerWeek_Height.addMouseWheelListener(_defaultMouseWheelListener);
			}

			{
				/*
				 * Mouse wheel scrolling
				 */

				// checkbox
				_chkUseDraggedScrolling = new Button(container, SWT.CHECK);
				_chkUseDraggedScrolling.setText(Messages.Slideout_CalendarOptions_Checkbox_UseDraggedScrolling);
				_chkUseDraggedScrolling.setToolTipText(
						Messages.Slideout_CalendarOptions_Checkbox_UseDraggedScrolling_Tooltip);
				_chkUseDraggedScrolling.addSelectionListener(_defaultSelectionListener);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.BEGINNING)
						.span(2, 1)
						.applyTo(_chkUseDraggedScrolling);
			}
		}
	}

	private void createUI_240_DateColumn(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Slideout_CalendarOptions_Group_DateColumn);
		GridDataFactory
				.fillDefaults()//
				//				.grab(true, true)
				.applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
//		group.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			{
				/*
				 * Date column
				 */
				// checkbox
				_chkIsShowDateColumn = new Button(group, SWT.CHECK);
				_chkIsShowDateColumn.setText(Messages.Slideout_CalendarOptions_Checkbox_IsShowDateColumn);
				_chkIsShowDateColumn.setToolTipText(
						Messages.Slideout_CalendarOptions_Checkbox_IsShowDateColumn_Tooltip);
				_chkIsShowDateColumn.addSelectionListener(_defaultSelectionListener);
				GridDataFactory
						.fillDefaults()//
						.span(2, 1)
						.applyTo(_chkIsShowDateColumn);
			}

			createUI_243__Col1(group);
			createUI_245__Col2(group);
		}
	}

	private void createUI_243__Col1(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			{
				/*
				 * Column width
				 */

				// label
				_lblDateColumn_Width = new Label(container, SWT.NONE);
				_lblDateColumn_Width.setText(Messages.Slideout_CalendarOptions_Label_DateColumn_Width);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.indent(_subItemIndent, 0)
						.applyTo(_lblDateColumn_Width);

				// size
				_spinnerDateColumnWidth = new Spinner(container, SWT.BORDER);
				_spinnerDateColumnWidth.setMinimum(CalendarProfileManager.DATE_COLUMN_WIDTH_MIN);
				_spinnerDateColumnWidth.setMaximum(CalendarProfileManager.DATE_COLUMN_WIDTH_MAX);
				_spinnerDateColumnWidth.setIncrement(1);
				_spinnerDateColumnWidth.setPageIncrement(10);
				_spinnerDateColumnWidth.addSelectionListener(_defaultSelectionListener);
				_spinnerDateColumnWidth.addMouseWheelListener(_defaultMouseWheelListener);
			}
			{
				/*
				 * Date content
				 */

				// label
				_lblDateColumn_Content = new Label(container, SWT.NONE);
				_lblDateColumn_Content.setText(Messages.Slideout_CalendarOptions_Label_DateColumn_Content);
				_lblDateColumn_Content.setToolTipText(
						Messages.Slideout_CalendarOptions_Label_DateColumn_Content_Tooltip);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.indent(_subItemIndent, 0)
						.applyTo(_lblDateColumn_Content);

				// value
				_comboDateColumn = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
				_comboDateColumn.setVisibleItemCount(20);
				_comboDateColumn.addSelectionListener(_defaultSelectionListener);
				_comboDateColumn.addFocusListener(_keepOpenListener);
			}
		}
	}

	private void createUI_245__Col2(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()//
				//				.grab(true, true)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//			containerRight.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			{
				/*
				 * Font
				 */

				// label
				_lblDateColumn_Font = new Label(container, SWT.NONE);
				_lblDateColumn_Font.setText(Messages.Slideout_CalendarOptions_Label_DateColumn_Font);
				GridDataFactory
						.fillDefaults()//
						.indent(0, _fontLabelVIndent)
						.applyTo(_lblDateColumn_Font);

				// value
				_fontEditorDateColumn = new SimpleFontEditor(container, SWT.NONE);
				_fontEditorDateColumn.addFontListener(_defaultFontEditorListener);
				GridDataFactory.fillDefaults().grab(true, true).applyTo(_fontEditorDateColumn);
			}
		}
	}

	private void createUI_260_YearColumns(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Slideout_CalendarOptions_Group_YearColumns);
		GridDataFactory
				.fillDefaults()//
				//				.grab(true, true)
				.applyTo(group);
		GridLayoutFactory
				.swtDefaults()//
				.numColumns(2)
				.spacing(10, LayoutConstants.getSpacing().y)
				.applyTo(group);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			{
				/*
				 * Year columns
				 */

				// checkbox
				_chkIsShowYearColumns = new Button(group, SWT.CHECK);
				_chkIsShowYearColumns.setText(Messages.Slideout_CalendarOptions_Checkbox_IsShowYearColumn);
				_chkIsShowYearColumns.setToolTipText(
						Messages.Slideout_CalendarOptions_Checkbox_IsShowYearColumn_Tooltip);
				_chkIsShowYearColumns.addSelectionListener(_defaultSelectionListener);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.BEGINNING)
						.span(2, 1)
						.applyTo(_chkIsShowYearColumns);
			}
			createUI_262__Col1(group);
			createUI_264__Col2(group);
		}
	}

	private void createUI_262__Col1(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			{
				/*
				 * Number of year columns
				 */

				// radio: Set width by number of columns
				_rdoYear_ColumnNumber = new Button(container, SWT.RADIO);
				_rdoYear_ColumnNumber.setText(Messages.Slideout_CalendarOptions_Radio_YearColumn_ByNumber);
				_rdoYear_ColumnNumber.addSelectionListener(_defaultSelectionListener);
				GridDataFactory
						.fillDefaults()//
						.indent(_subItemIndent, 0)
						.applyTo(_rdoYear_ColumnNumber);

				// spinner: columns
				_spinnerYear_Columns = new Spinner(container, SWT.BORDER);
				_spinnerYear_Columns.setMinimum(CalendarProfileManager.YEAR_COLUMNS_MIN);
				_spinnerYear_Columns.setMaximum(CalendarProfileManager.YEAR_COLUMNS_MAX);
				_spinnerYear_Columns.setIncrement(1);
				_spinnerYear_Columns.setPageIncrement(2);
				_spinnerYear_Columns.addSelectionListener(_defaultSelectionListener);
				_spinnerYear_Columns.addMouseWheelListener(_defaultMouseWheelListener);

			}
			{
				/*
				 * Year column width
				 */

				// radio: Set column width
				_rdoYear_ColumnDayWidth = new Button(container, SWT.RADIO);
				_rdoYear_ColumnDayWidth.setText(Messages.Slideout_CalendarOptions_Radio_YearColumn_ByDayWidth);
				_rdoYear_ColumnDayWidth.addSelectionListener(_defaultSelectionListener);
				GridDataFactory
						.fillDefaults()//
						.indent(_subItemIndent, 0)
						.applyTo(_rdoYear_ColumnDayWidth);

				// spinner: columns
				_spinnerYear_DayWidth = new Spinner(container, SWT.BORDER);
				_spinnerYear_DayWidth.setMinimum(CalendarProfileManager.YEAR_COLUMN_DAY_WIDTH_MIN);
				_spinnerYear_DayWidth.setMaximum(CalendarProfileManager.YEAR_COLUMN_DAY_WIDTH_MAX);
				_spinnerYear_DayWidth.setIncrement(1);
				_spinnerYear_DayWidth.setPageIncrement(10);
				_spinnerYear_DayWidth.addSelectionListener(_defaultSelectionListener);
				_spinnerYear_DayWidth.addMouseWheelListener(_defaultMouseWheelListener);

			}
			{
				/*
				 * Year columns space
				 */

				// label
				_lblYear_ColumnSpacing = new Label(container, SWT.NONE);
				_lblYear_ColumnSpacing.setText(Messages.Slideout_CalendarOptions_Label_YearColumn_Space);
				_lblYear_ColumnSpacing.setToolTipText(
						Messages.Slideout_CalendarOptions_Label_YearColumn_Space_Tooltip);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.indent(_subItemIndent, 0)
						.applyTo(_lblYear_ColumnSpacing);

				// spinner: columns
				_spinnerYear_ColumnSpacing = new Spinner(container, SWT.BORDER);
				_spinnerYear_ColumnSpacing.setMinimum(CalendarProfileManager.CALENDAR_COLUMNS_SPACE_MIN);
				_spinnerYear_ColumnSpacing.setMaximum(CalendarProfileManager.CALENDAR_COLUMNS_SPACE_MAX);
				_spinnerYear_ColumnSpacing.setIncrement(1);
				_spinnerYear_ColumnSpacing.setPageIncrement(10);
				_spinnerYear_ColumnSpacing.addSelectionListener(_defaultSelectionListener);
				_spinnerYear_ColumnSpacing.addMouseWheelListener(_defaultMouseWheelListener);
			}
			{
				/*
				 * Column start
				 */

				// label
				_lblYear_ColumnStart = new Label(container, SWT.NONE);
				_lblYear_ColumnStart.setText(Messages.Slideout_CalendarOptions_Label_YearColumn_Start);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.indent(_subItemIndent, 0)
						.applyTo(_lblYear_ColumnStart);

				// value
				_comboYear_ColumnStart = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
				_comboYear_ColumnStart.setVisibleItemCount(20);
				_comboYear_ColumnStart.addSelectionListener(_defaultSelectionListener);
				_comboYear_ColumnStart.addFocusListener(_keepOpenListener);
			}
		}
	}

	private void createUI_264__Col2(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()//
				//				.grab(true, true)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			{
				/*
				 * Year header
				 */

				// label
				_lblYearColumn_HeaderFont = new Label(container, SWT.NONE);
				_lblYearColumn_HeaderFont.setText(Messages.Slideout_CalendarOptions_Label_YearColumn_HeaderFont);
				GridDataFactory
						.fillDefaults()//
						.indent(0, _fontLabelVIndent)
						.applyTo(_lblYearColumn_HeaderFont);

				// font/size
				_fontEditorYearColumnHeader = new SimpleFontEditor(container, SWT.NONE);
				_fontEditorYearColumnHeader.addFontListener(_defaultFontEditorListener);
				GridDataFactory
						.fillDefaults()//
						.grab(true, true)
						.applyTo(_fontEditorYearColumnHeader);
			}
		}
	}

	private Control createUI_400_Tab_TourLayout(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(container);
		{
			createUI_410_DayDate(container);
			createUI_420_Color(container);
			createUI_430_Font(container);
		}

		return container;
	}

	private void createUI_410_DayDate(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Slideout_CalendarOptions_Group_DayDate);
		GridDataFactory
				.fillDefaults()//
				.grab(true, false)
				.applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
//		group.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
		{
			{
				/*
				 * Day date
				 */

				// checkbox
				_chkIsShowDayDate = new Button(group, SWT.CHECK);
				_chkIsShowDayDate.setText(Messages.Slideout_CalendarOptions_Checkbox_IsShowDayDate);
				_chkIsShowDayDate.addSelectionListener(_defaultSelectionListener);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.BEGINNING)
						.span(2, 1)
						.applyTo(_chkIsShowDayDate);
			}
			{
				final Composite container = new Composite(group, SWT.NONE);
				GridDataFactory
						.fillDefaults()//
						.grab(true, false)
						.span(2, 1)
						.applyTo(container);
				GridLayoutFactory
						.fillDefaults()//
						.numColumns(2)
						.spacing(20, LayoutConstants.getSpacing().y)
						.applyTo(container);
//				container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
				{
					createUI_412__Col1(container);
					createUI_414__Col2(container);
				}
			}
		}
	}

	private void createUI_412__Col1(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()
				//				.grab(true, false)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			{
				/*
				 * Day date format
				 */

				// label
				_lblDayHeader_Format = new Label(container, SWT.NONE);
				_lblDayHeader_Format.setText(Messages.Slideout_CalendarOptions_Label_DayDate_Format);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.indent(_subItemIndent, 0)
						.applyTo(_lblDayHeader_Format);

				// value
				_comboDayHeaderDateFormat = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
				_comboDayHeaderDateFormat.setVisibleItemCount(20);
				_comboDayHeaderDateFormat.addSelectionListener(_defaultSelectionListener);
				_comboDayHeaderDateFormat.addFocusListener(_keepOpenListener);
			}
			{
				/*
				 * Hide day when empty
				 */

				// checkbox
				_chkIsHideDayDateWhenNoTour = new Button(container, SWT.CHECK);
				_chkIsHideDayDateWhenNoTour.setText(Messages.Slideout_CalendarOptions_Checkbox_IsHideDayWhenEmpty);
				_chkIsHideDayDateWhenNoTour.addSelectionListener(_defaultSelectionListener);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.BEGINNING)
						.indent(_subItemIndent, 0)
						.span(2, 1)
						.applyTo(_chkIsHideDayDateWhenNoTour);
			}
			{
				/*
				 * Show weekend color
				 */

				// checkbox
				_chkIsShowDayDateWeekendColor = new Button(container, SWT.CHECK);
				_chkIsShowDayDateWeekendColor.setText(Messages.Slideout_CalendarOptions_Checkbox_IsShowDayWeekendColor);
				_chkIsShowDayDateWeekendColor.addSelectionListener(_defaultSelectionListener);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.BEGINNING)
						.indent(_subItemIndent, 0)
						.span(2, 1)
						.applyTo(_chkIsShowDayDateWeekendColor);
			}
		}
	}

	private void createUI_414__Col2(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()//
				.grab(true, true)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_CYAN));
		{
			{
				/*
				 * Margins
				 */

				// label
				_lblDayDate_Margin = new Label(container, SWT.NONE);
				_lblDayDate_Margin.setText(Messages.Slideout_CalendarOptions_Label_Margin);
				_lblDayDate_Margin.setToolTipText(Messages.Slideout_CalendarOptions_Label_Margin_DayDate_Tooltip);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_lblDayDate_Margin);

				final Composite valueContainer = new Composite(container, SWT.NONE);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(valueContainer);
				GridLayoutFactory.fillDefaults().numColumns(2).applyTo(valueContainer);
				{
					_spinnerDayDate_Margin_Top = createUI_Margin(valueContainer);
					_spinnerDayDate_Margin_Left = createUI_Margin(valueContainer);
				}
			}
			{
				/*
				 * Font
				 */

				// label
				_lblDayHeader_Font = new Label(container, SWT.NONE);
				_lblDayHeader_Font.setText(Messages.Slideout_CalendarOptions_Label_DayDate_Font);
				GridDataFactory
						.fillDefaults()//
						.grab(true, true)
						.indent(0, _fontLabelVIndent)
						.applyTo(_lblDayHeader_Font);

				// value
				_fontEditorDayDate = new SimpleFontEditor(container, SWT.NONE);
				_fontEditorDayDate.addFontListener(_defaultFontEditorListener);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(_fontEditorDayDate);
			}
		}
	}

	private void createUI_420_Color(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Slideout_CalendarOptions_Group_TourColor);
		GridDataFactory
				.fillDefaults()
				.grab(true, false)
				.applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);
//		group.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_CYAN));
		{
			{
				/*
				 * Tour background
				 */

				// label
				final Label label = new Label(group, SWT.NONE);
				label.setText(Messages.Slideout_CalendarOptions_Label_Tour_BackgroundColor);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(label);

				// value
				_comboTour_Background = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
				_comboTour_Background.setVisibleItemCount(20);
				_comboTour_Background.addSelectionListener(_defaultSelectionListener);
				_comboTour_Background.addFocusListener(_keepOpenListener);

				// background width
				_spinnerTour_BackgroundWidth = new Spinner(group, SWT.BORDER);
				_spinnerTour_BackgroundWidth.setMinimum(CalendarProfileManager.TOUR_BACKGROUND_WIDTH_MIN);
				_spinnerTour_BackgroundWidth.setMaximum(CalendarProfileManager.TOUR_BACKGROUND_WIDTH_MAX);
				_spinnerTour_BackgroundWidth.setIncrement(1);
				_spinnerTour_BackgroundWidth.setPageIncrement(10);
				_spinnerTour_BackgroundWidth.addSelectionListener(_defaultSelectionListener);
				_spinnerTour_BackgroundWidth.addMouseWheelListener(_defaultMouseWheelListener);
			}
			{
				// spacer
				new Label(group, SWT.NONE);

				final Composite containerBg = new Composite(group, SWT.NONE);
				GridDataFactory
						.fillDefaults()
						//						.grab(true, false)
						.span(2, 1)
						.applyTo(containerBg);
				GridLayoutFactory.fillDefaults().numColumns(4).applyTo(containerBg);
				{
					// combo color 1
					_comboTour_BackgroundColor1 = new Combo(containerBg, SWT.DROP_DOWN | SWT.READ_ONLY);
					_comboTour_BackgroundColor1.setVisibleItemCount(20);
					_comboTour_BackgroundColor1.addSelectionListener(_defaultSelectionListener);

					// color selector 1
					_colorTour_BackgroundColor1 = createUI_ColorSelector(containerBg);

					// combo color 2
					_comboTour_BackgroundColor2 = new Combo(containerBg, SWT.DROP_DOWN | SWT.READ_ONLY);
					_comboTour_BackgroundColor2.setVisibleItemCount(20);
					_comboTour_BackgroundColor2.addSelectionListener(_defaultSelectionListener);

					// color selector 2
					_colorTour_BackgroundColor2 = createUI_ColorSelector(containerBg);
				}
			}
			{
				/*
				 * Tour border
				 */

				// label
				final Label label = new Label(group, SWT.NONE);
				label.setText(Messages.Slideout_CalendarOptions_Label_Tour_BorderColor);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(label);

				// value
				_comboTour_BorderLayout = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
				_comboTour_BorderLayout.setVisibleItemCount(20);
				_comboTour_BorderLayout.addSelectionListener(_defaultSelectionListener);
				_comboTour_BorderLayout.addFocusListener(_keepOpenListener);

				// border width
				_spinnerTour_BorderWidth = new Spinner(group, SWT.BORDER);
				_spinnerTour_BorderWidth.setMinimum(CalendarProfileManager.TOUR_BORDER_WIDTH_MIN);
				_spinnerTour_BorderWidth.setMaximum(CalendarProfileManager.TOUR_BORDER_WIDTH_MAX);
				_spinnerTour_BorderWidth.setIncrement(1);
				_spinnerTour_BorderWidth.setPageIncrement(10);
				_spinnerTour_BorderWidth.addSelectionListener(_defaultSelectionListener);
				_spinnerTour_BorderWidth.addMouseWheelListener(_defaultMouseWheelListener);
			}
			{
				// spacer
				new Label(group, SWT.NONE);

				final Composite containerBorder = new Composite(group, SWT.NONE);
				GridDataFactory
						.fillDefaults()
						//						.grab(true, false)
						.span(2, 1)
						.applyTo(containerBorder);
				GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerBorder);
				{
					// combo color
					_comboTour_BorderColor = new Combo(containerBorder, SWT.DROP_DOWN | SWT.READ_ONLY);
					_comboTour_BorderColor.setVisibleItemCount(20);
					_comboTour_BorderColor.addSelectionListener(_defaultSelectionListener);

					// color selector
					_colorTour_BorderColor = createUI_ColorSelector(containerBorder);
				}
			}
			{
				/*
				 * Hovered tour color
				 */

				// label
				final Label label = new Label(group, SWT.NONE);
				label.setText(Messages.Slideout_CalendarOptions_Label_Tour_HoveredColor);

				final Composite containerHovered = new Composite(group, SWT.NONE);
				GridDataFactory
						.fillDefaults()
						//						.grab(true, false)
						.span(2, 1)
						.applyTo(containerHovered);
				GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerHovered);
				{
					// combo color
					_comboTour_HoveredColor = new Combo(containerHovered, SWT.DROP_DOWN | SWT.READ_ONLY);
					_comboTour_HoveredColor.setVisibleItemCount(20);
					_comboTour_HoveredColor.addSelectionListener(_defaultSelectionListener);

					// color selector
					_colorTour_HoveredColor = createUI_ColorSelector(containerHovered);
				}
			}
			{
				/*
				 * Selected tour color
				 */

				// label
				final Label label = new Label(group, SWT.NONE);
				label.setText(Messages.Slideout_CalendarOptions_Label_Tour_SelectedColor);

				final Composite containerSelected = new Composite(group, SWT.NONE);
				GridDataFactory
						.fillDefaults()
						//						.grab(true, false)
						.span(2, 1)
						.applyTo(containerSelected);
				GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerSelected);
				{
					// combo color
					_comboTour_SelectedColor = new Combo(containerSelected, SWT.DROP_DOWN | SWT.READ_ONLY);
					_comboTour_SelectedColor.setVisibleItemCount(20);
					_comboTour_SelectedColor.addSelectionListener(_defaultSelectionListener);

					// color selector
					_colorTour_SelectedColor = createUI_ColorSelector(containerSelected);
				}
			}
			{
				/*
				 * Dragged tour color
				 */

				// label
				final Label label = new Label(group, SWT.NONE);
				label.setText(Messages.Slideout_CalendarOptions_Label_Tour_DraggedColor);

				final Composite containerDragged = new Composite(group, SWT.NONE);
				GridDataFactory
						.fillDefaults()
						//						.grab(true, false)
						.span(2, 1)
						.applyTo(containerDragged);
				GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerDragged);
				{

					// combo color
					_comboTour_DraggedColor = new Combo(containerDragged, SWT.DROP_DOWN | SWT.READ_ONLY);
					_comboTour_DraggedColor.setVisibleItemCount(20);
					_comboTour_DraggedColor.addSelectionListener(_defaultSelectionListener);

					// color selector
					_colorTour_DraggedColor = createUI_ColorSelector(containerDragged);
				}
			}
		}
	}

	private void createUI_430_Font(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Slideout_CalendarOptions_Group_TourFont);
		GridDataFactory
				.fillDefaults()//
				.grab(true, false)
				.applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(4).applyTo(group);
//		group.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_CYAN));
		{
			{
				/*
				 * Title font
				 */

				// label
				_lblTour_TitleFont = new Label(group, SWT.NONE);
				_lblTour_TitleFont.setText(Messages.Slideout_CalendarOptions_Label_Tour_TitleFont);
				GridDataFactory.fillDefaults().indent(0, _fontLabelVIndent).applyTo(_lblTour_TitleFont);

				// font/size
				_fontEditorTourTitle = new SimpleFontEditor(group, SWT.NONE);
				_fontEditorTourTitle.addFontListener(_defaultFontEditorListener);
				GridDataFactory.fillDefaults().grab(true, true).applyTo(_fontEditorTourTitle);

				// combo color
				_comboTour_TitleColor = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
				_comboTour_TitleColor.setVisibleItemCount(20);
				_comboTour_TitleColor.addSelectionListener(_defaultSelectionListener);
				GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(_comboTour_TitleColor);

				// color selector
				_colorTour_TitleColor = createUI_ColorSelector(group);
			}
			{
				/*
				 * Content font
				 */

				// label
				_lblTour_ContentFont = new Label(group, SWT.NONE);
				_lblTour_ContentFont.setText(Messages.Slideout_CalendarOptions_Label_Tour_ContentFont);
				GridDataFactory
						.fillDefaults()//
						.indent(0, _fontLabelVIndent)
						.applyTo(_lblTour_ContentFont);

				// font/size
				_fontEditorTourContent = new SimpleFontEditor(group, SWT.NONE);
				_fontEditorTourContent.addFontListener(_defaultFontEditorListener);
				GridDataFactory
						.fillDefaults()//
						.grab(true, true)
						.applyTo(_fontEditorTourContent);

				// combo color
				_comboTour_ContentColor = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
				_comboTour_ContentColor.setVisibleItemCount(20);
				_comboTour_ContentColor.addSelectionListener(_defaultSelectionListener);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.BEGINNING, SWT.BEGINNING)
						.applyTo(_comboTour_ContentColor);

				// color selector
				_colorTour_ContentColor = createUI_ColorSelector(group);
			}
			{
				/*
				 * Value font
				 */

				// label
				_lblTour_ValueFont = new Label(group, SWT.NONE);
				_lblTour_ValueFont.setText(Messages.Slideout_CalendarOptions_Label_Tour_ValueFont);
				GridDataFactory
						.fillDefaults()//
						.indent(0, _fontLabelVIndent)
						.applyTo(_lblTour_ValueFont);

				// font/size
				_fontEditorTourValue = new SimpleFontEditor(group, SWT.NONE);
				_fontEditorTourValue.addFontListener(_defaultFontEditorListener);
				GridDataFactory
						.fillDefaults()//
						.grab(true, true)
						.applyTo(_fontEditorTourValue);

				// combo color
				_comboTour_ValueColor = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
				_comboTour_ValueColor.setVisibleItemCount(20);
				_comboTour_ValueColor.addSelectionListener(_defaultSelectionListener);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.BEGINNING, SWT.BEGINNING)
						.applyTo(_comboTour_ValueColor);

				// color selector
				_colorTour_ValueColor = createUI_ColorSelector(group);
			}
		}
	}

	private Control createUI_600_Tab_TourContent(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(container);
		{
			createUI_610_TourContent(container);
		}

		return container;
	}

	private void createUI_610_TourContent(final Composite parent) {

		{
			/*
			 * Show tour
			 */

			// checkbox
			_chkIsShowTourContent = new Button(parent, SWT.CHECK);
			_chkIsShowTourContent.setText(Messages.Slideout_CalendarOptions_Checkbox_IsShowTour_Content);
			_chkIsShowTourContent.addSelectionListener(_defaultSelectionListener);
			GridDataFactory
					.fillDefaults()//
					.align(SWT.FILL, SWT.BEGINNING)
					.applyTo(_chkIsShowTourContent);
		}

		_tourFormatterContainer = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()//
				.grab(true, false)
				.indent(_subItemIndent, 0)
				.applyTo(_tourFormatterContainer);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(_tourFormatterContainer);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
		{
			createUI_620_Layout(_tourFormatterContainer);
			createUI_630_Margin(_tourFormatterContainer);
			createUI_650_Values(_tourFormatterContainer);
		}
	}

	private void createUI_620_Layout(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()//
				.grab(true, false)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		{
			createUI_622_Col1(container);
			createUI_622_Col2(container);
		}
	}

	private void createUI_622_Col1(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			{
				/*
				 * Show value unit
				 */

				// checkbox
				_chkIsShowTourValueUnit = new Button(container, SWT.CHECK);
				_chkIsShowTourValueUnit.setText(Messages.Slideout_CalendarOptions_Checkbox_IsShowTour_ValueUnit);
				_chkIsShowTourValueUnit.addSelectionListener(_defaultSelectionListener);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.BEGINNING)
						.span(2, 1)
						.applyTo(_chkIsShowTourValueUnit);
			}
			{
				/*
				 * Number of value columns
				 */

				// label
				_lblTour_ValueColumns = new Label(container, SWT.NONE);
				_lblTour_ValueColumns.setText(Messages.Slideout_CalendarOptions_Label_Tour_ValueColumns);
				_lblTour_ValueColumns.setToolTipText(Messages.Slideout_CalendarOptions_Label_Tour_ValueColumns_Tooltip);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(_lblTour_ValueColumns);

				// spinner
				_spinnerTour_ValueColumns = new Spinner(container, SWT.BORDER);
				_spinnerTour_ValueColumns.setMinimum(CalendarProfileManager.TOUR_VALUE_COLUMNS_MIN);
				_spinnerTour_ValueColumns.setMaximum(CalendarProfileManager.TOUR_VALUE_COLUMNS_MAX);
				_spinnerTour_ValueColumns.addSelectionListener(_defaultSelectionListener);
				_spinnerTour_ValueColumns.addMouseWheelListener(_defaultMouseWheelListener);
			}
		}
	}

	private void createUI_622_Col2(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			{
				/*
				 * Truncate text
				 */

				// checkbox
				_chkIsTruncateTourText = new Button(container, SWT.CHECK);
				_chkIsTruncateTourText.setText(Messages.Slideout_CalendarOptions_Checkbox_IsTruncateTourText);
				_chkIsTruncateTourText.addSelectionListener(_defaultSelectionListener);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.BEGINNING)
						.span(2, 1)
						.applyTo(_chkIsTruncateTourText);
			}
			{
				/*
				 * Number of visible lines when truncated
				 */

				// label
				_lblTour_TruncatedLines = new Label(container, SWT.NONE);
				_lblTour_TruncatedLines.setText(Messages.Slideout_CalendarOptions_Label_Tour_TruncatedLines);
				_lblTour_TruncatedLines.setToolTipText(
						Messages.Slideout_CalendarOptions_Label_Tour_TruncatedLines_Tooltip);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.indent(_subItemIndent, 0)
						.applyTo(_lblTour_TruncatedLines);

				// spinner
				_spinnerTour_TruncatedLines = new Spinner(container, SWT.BORDER);
				_spinnerTour_TruncatedLines.setMinimum(CalendarProfileManager.TOUR_TRUNCATED_LINES_MIN);
				_spinnerTour_TruncatedLines.setMaximum(CalendarProfileManager.TOUR_TRUNCATED_LINES_MAX);
				_spinnerTour_TruncatedLines.addSelectionListener(_defaultSelectionListener);
				_spinnerTour_TruncatedLines.addMouseWheelListener(_defaultMouseWheelListener);
			}
		}
	}

	private void createUI_630_Margin(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory
				.fillDefaults()
				.numColumns(2)

				// make is more visible
				.extendedMargins(0, 0, 10, 0)
				.applyTo(container);
		{
			/*
			 * Margins
			 */

			// label
			_lblTour_Margin = new Label(container, SWT.NONE);
			_lblTour_Margin.setText(Messages.Slideout_CalendarOptions_Label_Margin);
			_lblTour_Margin.setToolTipText(Messages.Slideout_CalendarOptions_Label_Margin_Tooltip);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_lblTour_Margin);

			final Composite valueContainer = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(valueContainer);
			GridLayoutFactory.fillDefaults().numColumns(4).applyTo(valueContainer);
			{
				_spinnerTour_Margin_Top = createUI_Margin(valueContainer);
				_spinnerTour_Margin_Left = createUI_Margin(valueContainer);
				_spinnerTour_Margin_Bottom = createUI_Margin(valueContainer);
				_spinnerTour_Margin_Right = createUI_Margin(valueContainer);
			}
		}
	}

	private void createUI_650_Values(final Composite parent) {

		final int defaultTourFormatter = CalendarProfileManager.NUM_DEFAULT_TOUR_FORMATTER;

		_chkTour_AllIsShowLines = new Button[defaultTourFormatter];
		_comboTour_AllValues = new Combo[defaultTourFormatter];
		_comboTour_AllFormats = new Combo[defaultTourFormatter];

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory
				.fillDefaults()//
				.numColumns(3)

				// set top/bottom margin to look less ugly
				.extendedMargins(0, 0, 10, 10)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		{
			for (int lineIndex = 0; lineIndex < defaultTourFormatter; lineIndex++) {

				// checkbox
				final Button chkWeek_IsShowLine = new Button(container, SWT.CHECK);
				chkWeek_IsShowLine.setText(NLS.bind(Messages.Slideout_CalendarOptions_Label_Line, lineIndex + 1));
				chkWeek_IsShowLine.addSelectionListener(_defaultSelectionListener);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(chkWeek_IsShowLine);

				// value
				final Combo comboTour_Value = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
				comboTour_Value.setVisibleItemCount(20);
				comboTour_Value.addSelectionListener(_tourValueListener);
				comboTour_Value.addFocusListener(_keepOpenListener);

				// value format
				final Combo comboTour_Format = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
				comboTour_Format.setVisibleItemCount(20);
				comboTour_Format.addSelectionListener(_defaultSelectionListener);
				comboTour_Format.addFocusListener(_keepOpenListener);
				GridDataFactory
						.fillDefaults()//
						.hint(_pc.convertWidthInCharsToPixels(9), SWT.DEFAULT)
						.applyTo(comboTour_Format);

				_chkTour_AllIsShowLines[lineIndex] = chkWeek_IsShowLine;
				_comboTour_AllValues[lineIndex] = comboTour_Value;
				_comboTour_AllFormats[lineIndex] = comboTour_Format;
			}
		}
	}

	private Control createUI_800_Tab_WeekSummary(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
//		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			createUI_810_WeekSummary(container);
		}

		return container;
	}

	private void createUI_810_WeekSummary(final Composite parent) {

		{
			/*
			 * Show week summary column
			 */

			// checkbox
			_chkIsShowSummaryColumn = new Button(parent, SWT.CHECK);
			_chkIsShowSummaryColumn.setText(Messages.Slideout_CalendarOptions_Checkbox_IsShowWeek_SummaryColumn);
			_chkIsShowSummaryColumn.addSelectionListener(_defaultSelectionListener);
			GridDataFactory
					.fillDefaults()//
					.align(SWT.FILL, SWT.BEGINNING)
					.applyTo(_chkIsShowSummaryColumn);
		}

		_weekFormatterContainer = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()//
				.grab(true, false)
				.indent(_subItemIndent, 0)
				.applyTo(_weekFormatterContainer);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(_weekFormatterContainer);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
		{
			createUI_820_Layout(_weekFormatterContainer);
			createUI_850_Values(_weekFormatterContainer);
		}
	}

	private void createUI_820_Layout(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()//
				.grab(true, false)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			{
				/*
				 * Show value unit
				 */

				// checkbox
				_chkIsShowWeekValueUnit = new Button(container, SWT.CHECK);
				_chkIsShowWeekValueUnit.setText(Messages.Slideout_CalendarOptions_Checkbox_IsShowWeek_ValueUnit);
				_chkIsShowWeekValueUnit.addSelectionListener(_defaultSelectionListener);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.BEGINNING)
						.span(2, 1)
						.applyTo(_chkIsShowWeekValueUnit);
			}
			{
				/*
				 * Column width
				 */

				// label
				_lblWeek_ColumnWidth = new Label(container, SWT.NONE);
				_lblWeek_ColumnWidth.setText(Messages.Slideout_CalendarOptions_Label_Week_ColumnWidth);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(_lblWeek_ColumnWidth);

				// size
				_spinnerWeek_ColumnWidth = new Spinner(container, SWT.BORDER);
				_spinnerWeek_ColumnWidth.setMinimum(CalendarProfileManager.WEEK_COLUMN_WIDTH_MIN);
				_spinnerWeek_ColumnWidth.setMaximum(CalendarProfileManager.WEEK_COLUMN_WIDTH_MAX);
				_spinnerWeek_ColumnWidth.setIncrement(1);
				_spinnerWeek_ColumnWidth.setPageIncrement(10);
				_spinnerWeek_ColumnWidth.addSelectionListener(_defaultSelectionListener);
				_spinnerWeek_ColumnWidth.addMouseWheelListener(_defaultMouseWheelListener);
			}
		}
		{
			/*
			 * Margins
			 */

			// label
			_lblWeek_Margin = new Label(container, SWT.NONE);
			_lblWeek_Margin.setText(Messages.Slideout_CalendarOptions_Label_Margin);
			_lblWeek_Margin.setToolTipText(Messages.Slideout_CalendarOptions_Label_Margin_Tooltip);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_lblWeek_Margin);

			final Composite valueContainer = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(valueContainer);
			GridLayoutFactory.fillDefaults().numColumns(4).applyTo(valueContainer);
			{
				_spinnerWeek_Margin_Top = createUI_Margin(valueContainer);
				_spinnerWeek_Margin_Left = createUI_Margin(valueContainer);
				_spinnerWeek_Margin_Bottom = createUI_Margin(valueContainer);
				_spinnerWeek_Margin_Right = createUI_Margin(valueContainer);
			}
		}

		{
			/*
			 * Font
			 */

			// label
			_lblWeek_ValueFont = new Label(container, SWT.NONE);
			_lblWeek_ValueFont.setText(Messages.Slideout_CalendarOptions_Label_Week_ValueFont);
			GridDataFactory
					.fillDefaults()//
					//						.grab(true, false)
					.indent(0, _fontLabelVIndent)
					.applyTo(_lblWeek_ValueFont);

			final Composite fontContainer = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(fontContainer);
			GridLayoutFactory.fillDefaults().numColumns(3).applyTo(fontContainer);
			{

			}
			// value
			_fontEditorWeekValue = new SimpleFontEditor(fontContainer, SWT.NONE);
			_fontEditorWeekValue.addFontListener(_defaultFontEditorListener);
			GridDataFactory
					.fillDefaults()//
					.grab(true, true)
					.applyTo(_fontEditorWeekValue);
			/*
			 * Value color
			 */

			// combo
			_comboWeek_ValueColor = new Combo(fontContainer, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboWeek_ValueColor.setVisibleItemCount(20);
			_comboWeek_ValueColor.addSelectionListener(_defaultSelectionListener);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(_comboWeek_ValueColor);

			// color selector
			_colorWeek_ValueColor = createUI_ColorSelector(fontContainer);
		}
	}

	private void createUI_850_Values(final Composite parent) {

		final int defaultWeekFormatter = CalendarProfileManager.NUM_DEFAULT_WEEK_FORMATTER;

		_chkWeek_AllIsShowLines = new Button[defaultWeekFormatter];
		_comboWeek_AllValues = new Combo[defaultWeekFormatter];
		_comboWeek_AllFormats = new Combo[defaultWeekFormatter];

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory
				.fillDefaults()//
				.numColumns(3)

				// set bottom margin to look less ugly
				.extendedMargins(0, 0, 10, 10)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		{
			for (int lineIndex = 0; lineIndex < defaultWeekFormatter; lineIndex++) {

				// checkbox
				final Button chkWeek_IsShowLine = new Button(container, SWT.CHECK);
				chkWeek_IsShowLine.setText(NLS.bind(Messages.Slideout_CalendarOptions_Label_Line, lineIndex + 1));
				chkWeek_IsShowLine.addSelectionListener(_defaultSelectionListener);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(chkWeek_IsShowLine);

				// value
				final Combo comboWeek_Value = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
				comboWeek_Value.setVisibleItemCount(20);
				comboWeek_Value.addSelectionListener(_weekValueListener);
				comboWeek_Value.addFocusListener(_keepOpenListener);

				// value format
				final Combo comboWeek_Format = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
				comboWeek_Format.setVisibleItemCount(20);
				comboWeek_Format.addSelectionListener(_defaultSelectionListener);
				comboWeek_Format.addFocusListener(_keepOpenListener);
				GridDataFactory
						.fillDefaults()//
						.hint(_pc.convertWidthInCharsToPixels(9), SWT.DEFAULT)
						.applyTo(comboWeek_Format);

				_chkWeek_AllIsShowLines[lineIndex] = chkWeek_IsShowLine;
				_comboWeek_AllValues[lineIndex] = comboWeek_Value;
				_comboWeek_AllFormats[lineIndex] = comboWeek_Format;
			}
		}
	}

	private ColorSelectorExtended createUI_ColorSelector(final Composite parent) {

		final ColorSelectorExtended colorSelector = new ColorSelectorExtended(parent);

		GridDataFactory
				.fillDefaults()//
				//				.grab(false, true)
				.align(SWT.BEGINNING, SWT.BEGINNING)
				.applyTo(colorSelector.getButton());

		colorSelector.addOpenListener(this);
		colorSelector.addListener(_defaultPropertyChangeListener);

		return colorSelector;
	}

	private Spinner createUI_Margin(final Composite parent) {

		final Spinner spinner = new Spinner(parent, SWT.BORDER);

		spinner.setMinimum(CalendarProfileManager.DEFAULT_MARGIN_MIN);
		spinner.setMaximum(CalendarProfileManager.DEFAULT_MARGIN_MAX);
		spinner.addSelectionListener(_defaultSelectionListener);
		spinner.addMouseWheelListener(_defaultMouseWheelListener);

		// ensure that the -- sign is displayed
		GridDataFactory
				.fillDefaults()
				.hint(_pc.convertWidthInCharsToPixels(3), SWT.DEFAULT)
				.applyTo(spinner);

		return spinner;
	}

	private void enableControls() {

		final boolean isShowDateColumn = _chkIsShowDateColumn.getSelection();
		final boolean isShowWeekColumn = _chkIsShowSummaryColumn.getSelection();
		final boolean isShowDayDate = _chkIsShowDayDate.getSelection();
		final boolean isYearColumns = _chkIsShowYearColumns.getSelection();
		final boolean isShowMonthColor = _chkIsShowMonthColor.getSelection();
		final boolean isShowTourContent = _chkIsShowTourContent.getSelection();
		final boolean isTruncateText = _chkIsTruncateTourText.getSelection();
		final boolean isYearColumnWidth = _rdoYear_ColumnDayWidth.getSelection();
		final boolean isWeekRowHeight = _rdoWeekRow_Height.getSelection();

		final CalendarColor tourBgColor1 = getSelected_Tour_GraphColor(_comboTour_BackgroundColor1);
		final CalendarColor tourBgColor2 = getSelected_Tour_GraphColor(_comboTour_BackgroundColor2);
		final CalendarColor tourBorderColor = getSelected_Tour_GraphColor(_comboTour_BorderColor);
		final CalendarColor tourDraggedColor = getSelected_Tour_GraphColor(_comboTour_DraggedColor);
		final CalendarColor tourHoveredColor = getSelected_Tour_GraphColor(_comboTour_HoveredColor);
		final CalendarColor tourSelectedColor = getSelected_Tour_GraphColor(_comboTour_SelectedColor);

		final CalendarColor contentColor = getSelected_Tour_ContentColor(_comboTour_ContentColor).dayContentColor;
		final CalendarColor titleColor = getSelected_Tour_ContentColor(_comboTour_TitleColor).dayContentColor;
		final CalendarColor valueColor = getSelected_Tour_ContentColor(_comboTour_ValueColor).dayContentColor;

		final CalendarColor weekColor = getSelected_Tour_GraphColor(_comboWeek_ValueColor);

		final boolean isCustomContentColor = contentColor == CalendarColor.CUSTOM;
		final boolean isCustomTitleColor = titleColor == CalendarColor.CUSTOM;
		final boolean isCustomValueColor = valueColor == CalendarColor.CUSTOM;
		final boolean isCustomWeekColor = weekColor == CalendarColor.CUSTOM;

		final TourBackground_ComboData selectedTourBackgroundData = getSelectedTourBackgroundData();
		final boolean isBgColor1 = selectedTourBackgroundData.isColor1;
		final boolean isBgColor2 = selectedTourBackgroundData.isColor2;

		final TourBorder_ComboData selectedTourBorderData = getSelectedTourBorderData();

		// layout
		_colorMonth_AlternateColor.setEnabled(isShowMonthColor);
		_colorMonth_AlternateColor2.setEnabled(isShowMonthColor);
		_spinnerWeek_Height.setEnabled(isWeekRowHeight);
		_spinnerWeek_Rows.setEnabled(isWeekRowHeight == false);

		// year columns
		_comboYear_ColumnStart.setEnabled(isYearColumns);
		_fontEditorYearColumnHeader.setEnabled(isYearColumns);
		_lblYearColumn_HeaderFont.setEnabled(isYearColumns);
		_lblYear_ColumnSpacing.setEnabled(isYearColumns);
		_lblYear_ColumnStart.setEnabled(isYearColumns);
		_rdoYear_ColumnNumber.setEnabled(isYearColumns);
		_rdoYear_ColumnDayWidth.setEnabled(isYearColumns);
		_spinnerYear_Columns.setEnabled(isYearColumns && isYearColumnWidth == false);
		_spinnerYear_ColumnSpacing.setEnabled(isYearColumns);
		_spinnerYear_DayWidth.setEnabled(isYearColumns && isYearColumnWidth);

		// date column
		_comboDateColumn.setEnabled(isShowDateColumn);
		_fontEditorDateColumn.setEnabled(isShowDateColumn);
		_lblDateColumn_Content.setEnabled(isShowDateColumn);
		_lblDateColumn_Font.setEnabled(isShowDateColumn);
		_lblDateColumn_Width.setEnabled(isShowDateColumn);
		_spinnerDateColumnWidth.setEnabled(isShowDateColumn);

		// day date
		_chkIsShowDayDateWeekendColor.setEnabled(isShowDayDate);
		_chkIsHideDayDateWhenNoTour.setEnabled(isShowDayDate);
		_comboDayHeaderDateFormat.setEnabled(isShowDayDate);
		_fontEditorDayDate.setEnabled(isShowDayDate);
		_lblDayHeader_Format.setEnabled(isShowDayDate);
		_lblDayHeader_Font.setEnabled(isShowDayDate);
		_lblDayDate_Margin.setEnabled(isShowDayDate);
		_spinnerDayDate_Margin_Top.setEnabled(isShowDayDate);
		_spinnerDayDate_Margin_Left.setEnabled(isShowDayDate);

		// tour fill
		_colorTour_BackgroundColor1.setEnabled(isBgColor1 && tourBgColor1 == CalendarColor.CUSTOM);
		_colorTour_BackgroundColor2.setEnabled(isBgColor2 && tourBgColor2 == CalendarColor.CUSTOM);
		_colorTour_BorderColor.setEnabled(tourBorderColor == CalendarColor.CUSTOM);
		_colorTour_DraggedColor.setEnabled(tourDraggedColor == CalendarColor.CUSTOM);
		_colorTour_HoveredColor.setEnabled(tourHoveredColor == CalendarColor.CUSTOM);
		_colorTour_SelectedColor.setEnabled(tourSelectedColor == CalendarColor.CUSTOM);
		_comboTour_BackgroundColor1.setEnabled(isBgColor1);
		_comboTour_BackgroundColor2.setEnabled(isBgColor2);
		_comboTour_BorderColor.setEnabled(selectedTourBorderData.isColor);
		_spinnerTour_BackgroundWidth.setEnabled(selectedTourBackgroundData.isWidth);
		_spinnerTour_BorderWidth.setEnabled(selectedTourBorderData.isWidth);

		// tour content
		_chkIsShowTourValueUnit.setEnabled(isShowTourContent);
		_chkIsTruncateTourText.setEnabled(isShowTourContent);
		_colorTour_ContentColor.setEnabled(isShowTourContent && isCustomContentColor);
		_colorTour_TitleColor.setEnabled(isShowTourContent && isCustomTitleColor);
		_colorTour_ValueColor.setEnabled(isShowTourContent && isCustomValueColor);
		_comboTour_ContentColor.setEnabled(isShowTourContent);
		_comboTour_TitleColor.setEnabled(isShowTourContent);
		_comboTour_ValueColor.setEnabled(isShowTourContent);
		_fontEditorTourContent.setEnabled(isShowTourContent);
		_fontEditorTourTitle.setEnabled(isShowTourContent);
		_fontEditorTourValue.setEnabled(isShowTourContent);
		_lblTour_ContentFont.setEnabled(isShowTourContent);
		_lblTour_Margin.setEnabled(isShowTourContent);
		_lblTour_TitleFont.setEnabled(isShowTourContent);
		_lblTour_TruncatedLines.setEnabled(isShowTourContent && isTruncateText);
		_lblTour_ValueColumns.setEnabled(isShowTourContent);
		_lblTour_ValueFont.setEnabled(isShowTourContent);
		_spinnerTour_Margin_Top.setEnabled(isShowTourContent);
		_spinnerTour_Margin_Bottom.setEnabled(isShowTourContent);
		_spinnerTour_Margin_Left.setEnabled(isShowTourContent);
		_spinnerTour_Margin_Right.setEnabled(isShowTourContent);
		_spinnerTour_TruncatedLines.setEnabled(isShowTourContent && isTruncateText);
		_spinnerTour_ValueColumns.setEnabled(isShowTourContent);
		enableControls_TourInfo();

		// week summary
		_chkIsShowWeekValueUnit.setEnabled(isShowWeekColumn);
		_colorWeek_ValueColor.setEnabled(isShowWeekColumn && isCustomWeekColor);
		_comboWeek_ValueColor.setEnabled(isShowWeekColumn);
		_fontEditorWeekValue.setEnabled(isShowWeekColumn);
		_lblWeek_ColumnWidth.setEnabled(isShowWeekColumn);
		_lblWeek_Margin.setEnabled(isShowWeekColumn);
		_lblWeek_ValueFont.setEnabled(isShowWeekColumn);
		_spinnerWeek_ColumnWidth.setEnabled(isShowWeekColumn);
		_spinnerWeek_Margin_Top.setEnabled(isShowWeekColumn);
		_spinnerWeek_Margin_Bottom.setEnabled(isShowWeekColumn);
		_spinnerWeek_Margin_Left.setEnabled(isShowWeekColumn);
		_spinnerWeek_Margin_Right.setEnabled(isShowWeekColumn);
		enableControls_WeekSummary();
	}

	private void enableControls_ProfileProperties() {

		final CalendarProfile profile = CalendarProfileManager.getActiveCalendarProfile();

		final int selectedDefaultIdIndex = _comboProfile_AllDefaultId.getSelectionIndex();
		final ProfileDefaultId_ComboData selectedDefaultComboData = _allDefaultComboData.get(selectedDefaultIdIndex);

		final boolean hasChildProfiles = hasUserChildProfiles(profile);
		final boolean isFromUserDefault = selectedDefaultComboData.defaultId == DefaultId.USER_ID;
		final boolean isDefaultDefault = profile.isDefaultDefault;
		final boolean isUserDefaultId = _chkIsUserDefaultId.getSelection();

		_chkIsUserDefaultId.setEnabled(
				isDefaultDefault == false
						&& isFromUserDefault == false
						&& hasChildProfiles == false);

		_comboProfile_AllDefaultId.setEnabled(isDefaultDefault == false);

		_lblProfile_UserParentDefaultId.setEnabled(isUserDefaultId && hasChildProfiles == false);

		_txtUserParentDefaultId.setEnabled(isUserDefaultId && hasChildProfiles == false);
	}

	private void enableControls_Profiles() {

		final CalendarProfile profile = CalendarProfileManager.getActiveCalendarProfile();
		final int numProfiles = _allCalendarProfiles.size();
		final boolean hasChildProfiles = hasUserChildProfiles(profile);

		_btnProfile_Delete.setEnabled(
				numProfiles > 1

						// default defaults cannot be deleted
						&& profile.isDefaultDefault == false

						// parents cannot be deleted
						&& hasChildProfiles == false);
	}

	private void enableControls_TourInfo() {

		final boolean isShowTourContent = _chkIsShowTourContent.getSelection();

		final DataFormatter[] tourContentFormatter = CalendarProfileManager.allTourContentFormatter;

		for (int lineIndex = 0; lineIndex < CalendarProfileManager.NUM_DEFAULT_TOUR_FORMATTER; lineIndex++) {

			final Button chkIsShowLine = _chkTour_AllIsShowLines[lineIndex];
			final Combo comboWeekValue = _comboTour_AllValues[lineIndex];
			final Combo comboWeekFormat = _comboTour_AllFormats[lineIndex];

			boolean canDoFormatting = false;

			if (isShowTourContent) {

				final int selectedValueIndex = comboWeekValue.getSelectionIndex();

				if (selectedValueIndex >= 0) {

					final DataFormatter selectedFormatter = tourContentFormatter[selectedValueIndex];

					final boolean isOnlyTextFormatter = isOnlyTextFormatter(selectedFormatter);
					final boolean isOnlyOneFormatter = isOnlyOneFormatter(selectedFormatter);

					canDoFormatting = isOnlyOneFormatter == false
							&& isOnlyTextFormatter == false
							&& selectedFormatter.getValueFormats() != null;
				}
			}

			final boolean isShowLine = chkIsShowLine.getSelection();

			chkIsShowLine.setEnabled(isShowTourContent);
			comboWeekValue.setEnabled(isShowTourContent && isShowLine);
			comboWeekFormat.setEnabled(isShowTourContent && isShowLine && canDoFormatting);
		}
	}

	private void enableControls_WeekSummary() {

		final boolean isShowSummaryColumn = _chkIsShowSummaryColumn.getSelection();

		final DataFormatter[] tourWeekSummaryFormatter = CalendarProfileManager.allWeekFormatter;

		for (int lineIndex = 0; lineIndex < CalendarProfileManager.NUM_DEFAULT_WEEK_FORMATTER; lineIndex++) {

			final Button chkIsShowLine = _chkWeek_AllIsShowLines[lineIndex];
			final Combo comboWeekValue = _comboWeek_AllValues[lineIndex];
			final Combo comboWeekFormat = _comboWeek_AllFormats[lineIndex];

			boolean canDoFormatting = false;

			if (isShowSummaryColumn) {

				final int selectedValueIndex = comboWeekValue.getSelectionIndex();

				if (selectedValueIndex >= 0) {

					final DataFormatter selectedWeekFormatter = tourWeekSummaryFormatter[selectedValueIndex];

					canDoFormatting = selectedWeekFormatter.getValueFormats() != null;
				}
			}

			final boolean isShowLine = chkIsShowLine.getSelection();

			chkIsShowLine.setEnabled(isShowSummaryColumn);
			comboWeekValue.setEnabled(isShowSummaryColumn && isShowLine);
			comboWeekFormat.setEnabled(isShowSummaryColumn && isShowLine && canDoFormatting);
		}
	}

	private void fillUI() {

		final boolean backupIsUpdateUI = _isUpdateUI;
		_isUpdateUI = true;
		{
			// Fill Combos

			/*
			 * Layout
			 */
			for (final DateColumn_ComboData data : CalendarProfileManager.getAllDateColumnData()) {
				_comboDateColumn.add(data.label);
			}

			for (final DayHeaderDateFormat_ComboData data : CalendarProfileManager
					.getAllDayHeaderDateFormat_ComboData()) {
				_comboDayHeaderDateFormat.add(data.label);
			}

			for (final ColumnLayout_ComboData data : CalendarProfileManager.getAllColumnLayout_ComboData()) {
				_comboYear_ColumnStart.add(data.label);
			}

			/*
			 * Tour background
			 */
			for (final TourBackground_ComboData data : CalendarProfileManager.getAllTourBackground_ComboData()) {
				_comboTour_Background.add(data.label);
			}
			fillUI_Combo_GraphColor(_comboTour_BackgroundColor1);
			fillUI_Combo_GraphColor(_comboTour_BackgroundColor2);
			fillUI_Combo_GraphColor(_comboTour_DraggedColor);
			fillUI_Combo_GraphColor(_comboTour_HoveredColor);
			fillUI_Combo_GraphColor(_comboTour_SelectedColor);

			/*
			 * Tour border
			 */
			for (final TourBorder_ComboData data : CalendarProfileManager.getAllTourBorderData()) {
				_comboTour_BorderLayout.add(data.label);
			}
			fillUI_Combo_GraphColor(_comboTour_BorderColor);

			/*
			 * Tour content
			 */
			// content, formatter is filled when a value is selected

			// color
			fillUI_Combo_TourContentColor(_comboTour_ContentColor);
			fillUI_Combo_TourContentColor(_comboTour_TitleColor);
			fillUI_Combo_TourContentColor(_comboTour_ValueColor);

			for (int lineIndex = 0; lineIndex < CalendarProfileManager.NUM_DEFAULT_TOUR_FORMATTER; lineIndex++) {

				final Combo comboTourValue = _comboTour_AllValues[lineIndex];

				for (final DataFormatter weekFormatter : CalendarProfileManager.allTourContentFormatter) {
					comboTourValue.add(weekFormatter.getText());
				}
			}

			/*
			 * Week summary values, formatter is filled when a value is selected
			 */
			for (int lineIndex = 0; lineIndex < CalendarProfileManager.NUM_DEFAULT_WEEK_FORMATTER; lineIndex++) {

				final Combo comboWeekValue = _comboWeek_AllValues[lineIndex];

				for (final DataFormatter weekFormatter : CalendarProfileManager.allWeekFormatter) {
					comboWeekValue.add(weekFormatter.getText());
				}
			}

			// week summary colors
			fillUI_Combo_GraphColor(_comboWeek_ValueColor);
		}
		_isUpdateUI = backupIsUpdateUI;
	}

	private void fillUI_Combo_GraphColor(final Combo combo) {

		final CalendarColor_ComboData[] comboData = CalendarProfileManager.getAllGraphColor_ComboData();

		for (final CalendarColor_ComboData data : comboData) {
			combo.add(data.label);
		}
	}

	private void fillUI_Combo_TourContentColor(final Combo combo) {

		final DayContentColor_ComboData[] comboData = CalendarProfileManager.getAllTourContentColor_ComboData();

		for (final DayContentColor_ComboData data : comboData) {
			combo.add(data.label);
		}
	}

	private void fillUI_DefaultIds(final CalendarProfile activeProfile) {

		/*
		 * Create combo data
		 */
		_allDefaultComboData.clear();

		// add app default ids
		for (final ProfileDefaultId_ComboData comboData : CalendarProfileManager.getAllAppDefault_ComboData()) {

			// skip user default id, this is a special id
			if (comboData.defaultId.equals(DefaultId.USER_ID)) {
				continue;
			}

			_allDefaultComboData.add(comboData);
		}

		// add user default ids
		for (final CalendarProfile profile : getAllUserDefaultIds()) {

			// exclude active profile
			if (profile.equals(activeProfile)) {
				continue;
			}

			final String userParentDefaultId = profile.userParentDefaultId;

			if (userParentDefaultId.trim().length() == 0) {

				// skip empty id's
				continue;
			}

			final ProfileDefaultId_ComboData userDefaultComboData = new ProfileDefaultId_ComboData(userParentDefaultId);

			_allDefaultComboData.add(userDefaultComboData);
		}

		/*
		 * Fill combo
		 */
		_comboProfile_AllDefaultId.removeAll();

		for (final ProfileDefaultId_ComboData comboData : _allDefaultComboData) {
			_comboProfile_AllDefaultId.add(comboData.labelOrUserId);
		}
	}

	/**
	 * @param comboFormat
	 * @param dataFormatter
	 * @return
	 */
	private int fillUI_Formats(final Combo comboFormat, final DataFormatter dataFormatter) {

		final ValueFormat[] valueFormats = dataFormatter.getValueFormats();

		if (valueFormats == null) {
			return 0;
		}

		final ValueFormat defaultFormat = dataFormatter.getDefaultFormat();
		int defaultIndex = 0;

		for (int formatIndex = 0; formatIndex < valueFormats.length; formatIndex++) {

			final ValueFormat valueFormat = valueFormats[formatIndex];

			// fill format combo
			comboFormat.add(FormatManager.getValueFormatterName(valueFormat));

			// get index for the default format
			if (valueFormat == defaultFormat) {
				defaultIndex = formatIndex;
			}
		}

		return defaultIndex;
	}

	private void fillUI_Formats(final Widget widget,
								final Combo comboValue,
								final Combo comboFormat,
								final DataFormatter[] allFormatter) {

		if (widget != comboValue) {

			// another combo fired the event

			return;
		}

		comboFormat.removeAll();

		final int selectedFormatterIndex = comboValue.getSelectionIndex();

		if (selectedFormatterIndex < 0) {
			return;
		}

		final int defaultIndex = fillUI_Formats(comboFormat, allFormatter[selectedFormatterIndex]);

		comboFormat.select(defaultIndex);
	}

	private void fillUI_Profiles() {

		final boolean backupIsUpdateUI = _isUpdateUI;
		_isUpdateUI = true;
		{
			_comboProfiles.removeAll();

			for (final CalendarProfile profile : _allCalendarProfiles) {
				_comboProfiles.add(profile.profileName);
			}
		}
		_isUpdateUI = backupIsUpdateUI;
	}

	private ArrayList<CalendarProfile> getAllUserDefaultIds() {

		final ArrayList<CalendarProfile> userDefaultIds = new ArrayList<>();

		for (final CalendarProfile profile : _allCalendarProfiles) {

			if (profile.isUserParentDefault) {
				userDefaultIds.add(profile);
			}
		}

		return userDefaultIds;
	}

	private int getCalendarColumLayoutIndex(final ColumnStart requestedData) {

		final ColumnLayout_ComboData[] allData = CalendarProfileManager.getAllColumnLayout_ComboData();

		for (int dataIndex = 0; dataIndex < allData.length; dataIndex++) {

			final ColumnLayout_ComboData data = allData[dataIndex];

			if (data.columnLayout.equals(requestedData)) {
				return dataIndex;
			}
		}

		// this should not happen
		return 0;
	}

	private int getDateColumnIndex(final DateColumnContent requestedData) {

		final DateColumn_ComboData[] allInfoColumnData = CalendarProfileManager.getAllDateColumnData();

		for (int dataIndex = 0; dataIndex < allInfoColumnData.length; dataIndex++) {

			final DateColumn_ComboData data = allInfoColumnData[dataIndex];

			if (data.dateColumn.equals(requestedData)) {
				return dataIndex;
			}
		}

		// this should not happen
		return 0;
	}

	private int getDayHeaderDateFormatIndex(final DayDateFormat requestedData) {

		final DayHeaderDateFormat_ComboData[] allData = CalendarProfileManager.getAllDayHeaderDateFormat_ComboData();

		for (int dataIndex = 0; dataIndex < allData.length; dataIndex++) {

			final DayHeaderDateFormat_ComboData data = allData[dataIndex];

			if (data.dayHeaderDateFormat.equals(requestedData)) {
				return dataIndex;
			}
		}

		// this should not happen
		return 0;
	}

	private int getGraphColorIndex(final CalendarColor requestedData) {

		final CalendarColor_ComboData[] allData = CalendarProfileManager.getAllGraphColor_ComboData();

		for (int dataIndex = 0; dataIndex < allData.length; dataIndex++) {

			final CalendarColor_ComboData data = allData[dataIndex];

			if (data.color.equals(requestedData)) {
				return dataIndex;
			}
		}

		// this should not happen
		return 0;
	}

	@Override
	protected Rectangle getParentBounds() {

		final Rectangle itemBounds = _toolItem.getBounds();
		final Point itemDisplayPosition = _toolItem.getParent().toDisplay(itemBounds.x, itemBounds.y);

		itemBounds.x = itemDisplayPosition.x;
		itemBounds.y = itemDisplayPosition.y;

		return itemBounds;
	}

	private DayContentColor_ComboData getSelected_Tour_ContentColor(final Combo combo) {

		final DayContentColor_ComboData[] allData = CalendarProfileManager.getAllTourContentColor_ComboData();

		final int selectedIndex = combo.getSelectionIndex();

		if (selectedIndex < 0) {

			for (final DayContentColor_ComboData data : allData) {

				if (data.dayContentColor == CalendarProfileManager.DEFAULT_TOUR_COLOR) {
					return data;
				}
			}

			// return default default
			return allData[0];
		}

		return allData[selectedIndex];
	}

	private CalendarColor getSelected_Tour_GraphColor(final Combo comboColor) {

		final int selectedIndex = comboColor.getSelectionIndex();

		final CalendarColor_ComboData[] allCalendarColorData = CalendarProfileManager.getAllGraphColor_ComboData();

		if (selectedIndex < 0) {
			return allCalendarColorData[0].color;
		}

		return allCalendarColorData[selectedIndex].color;
	}

	private ColumnStart getSelectedColumnLayout() {

		final int selectedIndex = _comboYear_ColumnStart.getSelectionIndex();

		if (selectedIndex < 0) {
			return ColumnStart.CONTINUOUSLY;
		}

		final ColumnLayout_ComboData data = CalendarProfileManager.getAllColumnLayout_ComboData()[selectedIndex];

		return data.columnLayout;
	}

	private DateColumnContent getSelectedDateColumn() {

		final int selectedIndex = _comboDateColumn.getSelectionIndex();

		if (selectedIndex < 0) {
			return DateColumnContent.WEEK_NUMBER;
		}

		final DateColumn_ComboData selectedInfoColumnData = CalendarProfileManager
				.getAllDateColumnData()[selectedIndex];

		return selectedInfoColumnData.dateColumn;
	}

	private DayDateFormat getSelectedDayDateFormat() {

		final int selectedIndex = _comboDayHeaderDateFormat.getSelectionIndex();

		if (selectedIndex < 0) {
			return DayDateFormat.AUTOMATIC;
		}

		final DayHeaderDateFormat_ComboData selectedData = CalendarProfileManager
				.getAllDayHeaderDateFormat_ComboData()[selectedIndex];

		return selectedData.dayHeaderDateFormat;
	}

	private FormatterData[] getSelectedFormatterData(	final DataFormatter[] allFormatter,
														final int numUILines,
														final Button[] chkAllIsShowLines,
														final Combo[] comboAllValues,
														final Combo[] comboAllFormats) {

		final ArrayList<FormatterData> selectedFormatterData = new ArrayList<>();

		for (int lineIndex = 0; lineIndex < numUILines; lineIndex++) {

			final Button chkIsShowLine = chkAllIsShowLines[lineIndex];
			final Combo comboTourValue = comboAllValues[lineIndex];
			final Combo comboTourFormat = comboAllFormats[lineIndex];

			final boolean isShowLine = chkIsShowLine.getSelection();

			final int selectedValueIndex = comboTourValue.getSelectionIndex();
			if (selectedValueIndex > -1) {

				final int selectedFormatIndex = comboTourFormat.getSelectionIndex();

				final DataFormatter selectedDataFormatter = allFormatter[selectedValueIndex];
				final ValueFormat[] valueFormats = selectedDataFormatter.getValueFormats();

				FormatterData formatterData;

				if (valueFormats != null && selectedFormatIndex > -1) {

					formatterData = new FormatterData(
							isShowLine,
							selectedDataFormatter.id,
							valueFormats[selectedFormatIndex]);

				} else {

					// keep selected formatter

					final FormatterID formatterID = valueFormats == null
							? FormatterID.EMPTY
							: selectedDataFormatter.id;

					final ValueFormat valueFormat = selectedFormatIndex == -1 || valueFormats == null
							? ValueFormat.DUMMY_VALUE
							: valueFormats[selectedFormatIndex];

					formatterData = new FormatterData(isShowLine, formatterID, valueFormat);
				}

				selectedFormatterData.add(formatterData);
			}
		}

		return selectedFormatterData.toArray(
				new FormatterData[selectedFormatterData.size()]);
	}

	/**
	 * @return Returns the selected profile in the profile viewer or <code>null</code> when nothing
	 *         is selected
	 */
	private CalendarProfile getSelectedProfile() {

		// get selected profile from viewer
		final StructuredSelection selection = (StructuredSelection) _profileViewer.getSelection();
		final Object firstElement = selection.getFirstElement();

		CalendarProfile selectedProfile = null;
		if (firstElement != null) {
			selectedProfile = (CalendarProfile) firstElement;
		}

		return selectedProfile;
	}

	private TourBackground_ComboData getSelectedTourBackgroundData() {

		final int selectedIndex = _comboTour_Background.getSelectionIndex();

		final TourBackground_ComboData[] allTourBackgroundData = CalendarProfileManager
				.getAllTourBackground_ComboData();

		if (selectedIndex < 0) {

			for (final TourBackground_ComboData data : allTourBackgroundData) {

				if (data.tourBackground == CalendarProfileManager.DEFAULT_TOUR_BACKGROUND) {
					return data;
				}
			}

			// return default default
			return allTourBackgroundData[0];
		}

		return allTourBackgroundData[selectedIndex];
	}

	private TourBorder_ComboData getSelectedTourBorderData() {

		final int selectedIndex = _comboTour_BorderLayout.getSelectionIndex();

		final TourBorder_ComboData[] allTourBorderData = CalendarProfileManager.getAllTourBorderData();

		if (selectedIndex < 0) {

			for (final TourBorder_ComboData data : allTourBorderData) {

				if (data.tourBorder == CalendarProfileManager.DEFAULT_TOUR_BORDER) {
					return data;
				}
			}

			// return default default
			return allTourBorderData[0];
		}

		return allTourBorderData[selectedIndex];
	}

	private int getTourBackgroundIndex(final TourBackground requestedData) {

		final TourBackground_ComboData[] allData = CalendarProfileManager.getAllTourBackground_ComboData();

		for (int dataIndex = 0; dataIndex < allData.length; dataIndex++) {

			final TourBackground_ComboData data = allData[dataIndex];

			if (data.tourBackground.equals(requestedData)) {
				return dataIndex;
			}
		}

		// this should not happen
		return 0;
	}

	private int getTourBorderIndex(final TourBorder requestedData) {

		final TourBorder_ComboData[] allData = CalendarProfileManager.getAllTourBorderData();

		for (int dataIndex = 0; dataIndex < allData.length; dataIndex++) {

			final TourBorder_ComboData data = allData[dataIndex];

			if (data.tourBorder.equals(requestedData)) {
				return dataIndex;
			}
		}

		// this should not happen
		return 0;
	}

	private int getTourColorIndex(final CalendarColor requestedData) {

		final DayContentColor_ComboData[] allData = CalendarProfileManager.getAllTourContentColor_ComboData();

		for (int dataIndex = 0; dataIndex < allData.length; dataIndex++) {

			final DayContentColor_ComboData data = allData[dataIndex];

			if (data.dayContentColor.equals(requestedData)) {
				return dataIndex;
			}
		}

		// this should not happen
		return 0;
	}

	private boolean hasUserChildProfiles(final CalendarProfile currentProfile) {

		final String currentUserDefaultId = currentProfile.userDefaultId.trim();

		// skip empty id's
		if (currentUserDefaultId.length() == 0) {
			return false;
		}

		boolean hasChildProfiles = false;

		if (currentProfile.isUserParentDefault) {

			for (final CalendarProfile profile : _allCalendarProfiles) {

				// skip own profile
				if (currentProfile == profile) {
					continue;
				}

				// skip default defaults
				if (profile.isDefaultDefault) {
					continue;
				}

				// skip empty id's
				if (profile.userDefaultId.length() == 0) {
					return false;
				}

				if (currentUserDefaultId.equals(profile.userDefaultId)) {
					hasChildProfiles = true;
					break;
				}
			}
		}

		return hasChildProfiles;
	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);
		_subItemIndent = _pc.convertHorizontalDLUsToPixels(12);

		_defaultSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModify_Profile();
			}
		};

		_defaultMouseWheelListener = new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
				onModify_Profile();
			}
		};

		_defaultPropertyChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {
				onModify_Profile();
			}
		};

		_tourValueListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModify_TourValue(e.widget);
			}
		};

		_weekValueListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModify_WeekValue(e.widget);
			}
		};

		_keepOpenListener = new FocusListener() {

			@Override
			public void focusGained(final FocusEvent e) {

				/*
				 * This will fix the problem that when the list of a combobox is displayed, then the
				 * slideout will disappear :-(((
				 */
				setIsKeepOpenInternally(true);
			}

			@Override
			public void focusLost(final FocusEvent e) {
				setIsKeepOpenInternally(false);
			}
		};

		_defaultFontEditorListener = new IFontEditorListener() {

			@Override
			public void fontDialogOpened(final boolean isDialogOpened) {
				setIsKeepOpenInternally(isDialogOpened);
			}

			@Override
			public void fontSelected(final FontData font) {
				onModify_Profile();
			}
		};

		CalendarProfileManager.addProfileListener(this);

		parent.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(final DisposeEvent e) {

				onDisposeSlideout();
			}
		});
	}

	private boolean isOnlyOneFormatter(final DataFormatter dataFormatter) {

		final ValueFormat[] allValueFormats = dataFormatter.getValueFormats();

		if (allValueFormats != null
				&& allValueFormats.length == 1) {

			return true;
		}

		return false;
	}

	private boolean isOnlyTextFormatter(final DataFormatter dataFormatter) {

		final ValueFormat[] allValueFormats = dataFormatter.getValueFormats();

		if (allValueFormats != null
				&& allValueFormats.length == 1
				&& allValueFormats[0] == ValueFormat.TEXT) {

			return true;
		}

		return false;
	}

	private void onDisposeSlideout() {

		// reset profile provider
		CalendarProfileManager.removeProfileListener(this);

		saveState_UI();
	}

	@Override
	protected void onFocus() {

		_comboProfiles.setFocus();
	}

	private void onModify_Profile() {

		saveState_Profile();
		CalendarProfileManager.updateFormatterValueFormat();

		enableControls();

		CalendarProfileManager.getActiveCalendarProfile().dump();

		_calendarView.updateUI_Graph();
	}

	private void onModify_ProfileProperties() {

		if (_isUpdateUI) {
			return;
		}

		final CalendarProfile activeProfile = CalendarProfileManager.getActiveCalendarProfile();

		/*
		 * User id CANNOT be deselected when profiles are based on the user default id
		 */
		final boolean isUserDefaultIdOld = activeProfile.isUserParentDefault;
		final boolean isUserDefaultId = _chkIsUserDefaultId.getSelection();

		if (isUserDefaultIdOld && isUserDefaultId == false && activeProfile.userDefaultId.trim().length() != 0) {

			// user id state is modified from true->false -> check if profiles are based on this user default id

			for (final CalendarProfile profile : _allCalendarProfiles) {

				// skip current profile
				if (activeProfile == profile) {
					continue;
				}

				// skip default default profiles
				if (profile.isDefaultDefault) {
					continue;
				}

				// skip empty id's
				if (profile.userDefaultId.length() == 0) {
					continue;
				}

				if (activeProfile.userDefaultId.equals(profile.userDefaultId)) {

					// active profile is based on a user default -> reselect checkbox

					_chkIsUserDefaultId.setSelection(true);

					MessageDialog.openInformation(
							getToolTipShell(),
							Messages.Slideout_CalendarOptions_Dialog_UserDefaultId_Title,
							NLS.bind(
									Messages.Slideout_CalendarOptions_Dialog_UserDefaultId_Message,
									activeProfile.userDefaultId,
									profile.profileName));

					return;
				}
			}
		}

		/*
		 * Update profile name
		 */
		final String modifiedProfileName = _txtProfileName.getText();

		// update text in profile combo
		final int selectedIndex = _comboProfiles.getSelectionIndex();
		_comboProfiles.setItem(selectedIndex, modifiedProfileName);

		// update combo in calendar view
		_calendarView.updateUI_ProfileName(modifiedProfileName);

		/*
		 * Update model+viewer
		 */
		// update profile BEFORE the viewer is updated
		saveState_Profile();

		// update profile viewer
		_profileViewer.update(getSelectedProfile(), null);

		enableControls_Profiles();
		enableControls_ProfileProperties();
	}

	private void onModify_TourValue(final Widget widget) {

		for (int lineIndex = 0; lineIndex < CalendarProfileManager.NUM_DEFAULT_TOUR_FORMATTER; lineIndex++) {

			final Combo comboWeekValue = _comboTour_AllValues[lineIndex];
			final Combo comboWeekFormat = _comboTour_AllFormats[lineIndex];

			fillUI_Formats(widget, comboWeekValue, comboWeekFormat, CalendarProfileManager.allTourContentFormatter);
		}

		_tourFormatterContainer.layout(true, true);

		onModify_Profile();
	}

	private void onModify_WeekValue(final Widget widget) {

		for (int lineIndex = 0; lineIndex < CalendarProfileManager.NUM_DEFAULT_WEEK_FORMATTER; lineIndex++) {

			final Combo comboWeekValue = _comboWeek_AllValues[lineIndex];
			final Combo comboWeekFormat = _comboWeek_AllFormats[lineIndex];

			fillUI_Formats(widget, comboWeekValue, comboWeekFormat, CalendarProfileManager.allWeekFormatter);
		}

		_weekFormatterContainer.layout(true, true);

		onModify_Profile();
	}

	private void onProfile_Add() {

		updateUI_NewProfile(new CalendarProfile());
	}

	private void onProfile_ApplyDefaults() {

		// reset active profile
		CalendarProfileManager.resetActiveCalendarProfile();

		// resetting a profile will delete and recreate a new -> this must be reflected in the viewer
		_profileViewer.refresh();

		final CalendarProfile newActiveProfile = CalendarProfileManager.getActiveCalendarProfile();
		_profileViewer.setSelection(new StructuredSelection(newActiveProfile), true);
	}

	private void onProfile_Copy() {

		final CalendarProfile selectedProfile = getSelectedProfile();

		if (selectedProfile == null) {
			// ignore
			return;
		}

		final CalendarProfile clonedProfile = selectedProfile.clone();

		// reset defaults, a copy can never be an app default
		clonedProfile.isDefaultDefault = false;
		clonedProfile.isUserParentDefault = false;
		clonedProfile.userDefaultId = UI.EMPTY_STRING;

		updateUI_NewProfile(clonedProfile);
	}

	private void onProfile_Delete() {

		final CalendarProfile selectedProfile = getSelectedProfile();

		if (selectedProfile == null) {
			// ignore
			return;
		}

		/*
		 * Confirm deletion
		 */
		setIsKeepOpenInternally(true);
		int confirmDialogResult;
		{
			confirmDialogResult = new MessageDialog(
					Display.getCurrent().getActiveShell(),
					Messages.Slideout_CalendarOptions_Dialog_DeleteProfile_Title,
					null,
					NLS.bind(
							Messages.Slideout_CalendarOptions_Dialog_DeleteProfile_Message,
							selectedProfile.profileName),
					MessageDialog.QUESTION,
					new String[] {
							Messages.App_Action_DeleteProfile,
							IDialogConstants.CANCEL_LABEL
					},
					0).open();
		}
		setIsKeepOpenInternally(false);

		if (confirmDialogResult != 0) {
			return;
		}

		// keep currently selected position
		final int lastIndex = _profileViewer.getTable().getSelectionIndex();

		// update model
		_allCalendarProfiles.remove(selectedProfile);

		// update UI
		_profileViewer.remove(selectedProfile);

		/*
		 * Select another filter at the same position
		 */
		final int numProfiles = _allCalendarProfiles.size();
		final int nextProfileIndex = Math.min(numProfiles - 1, lastIndex);

		final Object nextSelectedProfile = _profileViewer.getElementAt(nextProfileIndex);
		if (nextSelectedProfile == null) {

			// this case should no happen because at least 1 profile must be available

		} else {

			selectProfile((CalendarProfile) nextSelectedProfile);
		}

		// set focus back to the viewer
		_profileViewer.getTable().setFocus();
	}

	/**
	 * Profile is selected in the profile viewer
	 */
	private void onProfile_Select() {

		if (_isUpdateUI) {
			return;
		}

		final CalendarProfile selectedProfile = getSelectedProfile();

		selectProfile(selectedProfile);
	}

	@Override
	protected void onReparentShell(final Shell reparentedShell) {

		super.onReparentShell(reparentedShell);

		// size for the resizable shell is set to DEFAULT
		reparentedShell.pack(true);
	}

	/**
	 * Profile is selected in the combo box
	 */
	private void onSelectProfile() {

		final int selectedIndex = _comboProfiles.getSelectionIndex();
		final ArrayList<CalendarProfile> allProfiles = _allCalendarProfiles;

		final CalendarProfile selectedProfile = allProfiles.get(selectedIndex);

		selectProfile(selectedProfile);
	}

	@Override
	public void profileIsModified() {

		// profile is modified externally

		fillUI_Profiles();

		restoreState_Profile();

		enableControls();
		enableControls_Profiles();
		enableControls_ProfileProperties();
	}

	private void restore_DefaultIds(final CalendarProfile profile) {

		fillUI_DefaultIds(profile);

		final DefaultId defaultId = profile.defaultId;
		final boolean isDefaultDefault = profile.isDefaultDefault;
		final boolean isUserParentDefault = profile.isUserParentDefault;
		final String userDefaultId = profile.userDefaultId;
		final String userParentDefaultId = profile.userParentDefaultId;

		int comboIndex = 0;
		boolean isIdAvailable = false;

		for (int dataIndex = 0; dataIndex < _allDefaultComboData.size(); dataIndex++) {

			final ProfileDefaultId_ComboData comboData = _allDefaultComboData.get(dataIndex);

			final DefaultId comboDefaultId = comboData.defaultId;

			if (isDefaultDefault) {

				if (comboData.isDefaultDefaultId && comboDefaultId == defaultId) {

					comboIndex = dataIndex;
					isIdAvailable = true;
					break;
				}

//			} else if (defaultId.equals(DefaultId.USER_ID)) {
//
//				if (comboData.label.equals(userDefaultId)) {
//
//					comboIndex = dataIndex;
//					isIdAvailable = true;
//					break;
//				}

			} else {

				if (comboDefaultId.equals(defaultId)) {

					comboIndex = dataIndex;
					isIdAvailable = true;
					break;
				}
			}
		}

		if (isIdAvailable == false) {

			StatusUtil.logInfo(
					(UI.timeStampNano() + " [" + getClass().getSimpleName() + "]" //$NON-NLS-1$ //$NON-NLS-2$
							+ " Profile default id is unknown") //$NON-NLS-1$
							+ ("\tDefaultId: " + defaultId) //$NON-NLS-1$
							+ ("\tParentId: " + userParentDefaultId) //$NON-NLS-1$
							+ ("\tUserId: " + userDefaultId) //$NON-NLS-1$
							+ ("\tisApp: " + isDefaultDefault) //$NON-NLS-1$
							+ ("\tisUser: " + isUserParentDefault) //$NON-NLS-1$
			);
		}

		_comboProfile_AllDefaultId.select(comboIndex);

		_chkIsUserDefaultId.setSelection(isUserParentDefault);
		_txtUserDefaultId.setText(userDefaultId);
		_txtUserParentDefaultId.setText(userParentDefaultId);

	}

	void restoreState_Profile() {

		if (_comboProfiles == null || _comboProfiles.isDisposed()) {
			return;
		}

		_isUpdateUI = true;
		{
			final CalendarProfile profile = CalendarProfileManager.getActiveCalendarProfile();

			// get index AFTER getting the active profile because this could change the active profile
			final int activeProfileIndex = CalendarProfileManager.getActiveCalendarProfileIndex();

			// profile viewer/select
			_comboProfiles.select(activeProfileIndex);
			_profileViewer.setSelection(new StructuredSelection(profile), true);

			// profile properties
			_txtProfileName.setText(profile.profileName);
			restore_DefaultIds(profile);

			// layout
			_chkIsShowMonthColor.setSelection(profile.isToggleMonthColor);
			_chkUseDraggedScrolling.setSelection(profile.useDraggedScrolling);
			_colorMonth_AlternateColor.setColorValue(profile.alternateMonthRGB);
			_colorMonth_AlternateColor2.setColorValue(profile.alternateMonth2RGB);
			_colorCalendar_BackgroundColor.setColorValue(profile.calendarBackgroundRGB);
			_colorCalendar_ForegroundColor.setColorValue(profile.calendarForegroundRGB);
			_colorDay_HoveredColor.setColorValue(profile.dayHoveredRGB);
			_colorDay_SelectedColor.setColorValue(profile.daySelectedRGB);
			_rdoWeekRow_Height.setSelection(profile.isWeekRowHeight);
			_rdoWeekRow_Number.setSelection(profile.isWeekRowHeight == false);
			_spinnerWeek_Height.setSelection(profile.weekHeight);
			_spinnerWeek_Rows.setSelection(profile.weekRows);

			// year columns
			_chkIsShowYearColumns.setSelection(profile.isShowYearColumns);
			_comboYear_ColumnStart.select(getCalendarColumLayoutIndex(profile.yearColumnsStart));
			_fontEditorYearColumnHeader.setSelection(profile.yearHeaderFont);
			_rdoYear_ColumnNumber.setSelection(profile.isYearColumnDayWidth == false);
			_rdoYear_ColumnDayWidth.setSelection(profile.isYearColumnDayWidth);
			_spinnerYear_Columns.setSelection(profile.yearColumns);
			_spinnerYear_ColumnSpacing.setSelection(profile.yearColumnsSpacing);
			_spinnerYear_DayWidth.setSelection(profile.yearColumnDayWidth);

			// date column
			_chkIsShowDateColumn.setSelection(profile.isShowDateColumn);
			_spinnerDateColumnWidth.setSelection(profile.dateColumnWidth);
			_comboDateColumn.select(getDateColumnIndex(profile.dateColumnContent));
			_fontEditorDateColumn.setSelection(profile.dateColumnFont);

			// day date
			_chkIsHideDayDateWhenNoTour.setSelection(profile.isHideDayDateWhenNoTour);
			_chkIsShowDayDate.setSelection(profile.isShowDayDate);
			_chkIsShowDayDateWeekendColor.setSelection(profile.isShowDayDateWeekendColor);
			_comboDayHeaderDateFormat.select(getDayHeaderDateFormatIndex(profile.dayDateFormat));
			_fontEditorDayDate.setSelection(profile.dayDateFont);
			_spinnerDayDate_Margin_Top.setSelection(profile.dayDateMarginTop);
			_spinnerDayDate_Margin_Left.setSelection(profile.dayDateMarginLeft);

			// tour background
			_comboTour_Background.select(getTourBackgroundIndex(profile.tourBackground));
			_comboTour_BackgroundColor1.select(getGraphColorIndex(profile.tourBackground1Color));
			_comboTour_BackgroundColor2.select(getGraphColorIndex(profile.tourBackground2Color));
			_comboTour_BorderLayout.select(getTourBorderIndex(profile.tourBorder));
			_comboTour_BorderColor.select(getGraphColorIndex(profile.tourBorderColor));
			_spinnerTour_BackgroundWidth.setSelection(profile.tourBackgroundWidth);
			_spinnerTour_BorderWidth.setSelection(profile.tourBorderWidth);

			_comboTour_DraggedColor.select(getGraphColorIndex(profile.tourDraggedColor));
			_comboTour_HoveredColor.select(getGraphColorIndex(profile.tourHoveredColor));
			_comboTour_SelectedColor.select(getGraphColorIndex(profile.tourSelectedColor));

			_colorTour_BackgroundColor1.setColorValue(profile.tourBackground1RGB);
			_colorTour_BackgroundColor2.setColorValue(profile.tourBackground2RGB);
			_colorTour_BorderColor.setColorValue(profile.tourBorderRGB);
			_colorTour_DraggedColor.setColorValue(profile.tourDraggedRGB);
			_colorTour_HoveredColor.setColorValue(profile.tourHoveredRGB);
			_colorTour_SelectedColor.setColorValue(profile.tourSelectedRGB);

			// tour content
			_chkIsShowTourContent.setSelection(profile.isShowTourContent);
			_chkIsShowTourValueUnit.setSelection(profile.isShowTourValueUnit);
			_chkIsTruncateTourText.setSelection(profile.isTruncateTourText);
			_colorTour_ContentColor.setColorValue(profile.tourContentRGB);
			_colorTour_TitleColor.setColorValue(profile.tourTitleRGB);
			_colorTour_ValueColor.setColorValue(profile.tourValueRGB);
			_comboTour_ContentColor.select(getTourColorIndex(profile.tourContentColor));
			_comboTour_TitleColor.select(getTourColorIndex(profile.tourTitleColor));
			_comboTour_ValueColor.select(getTourColorIndex(profile.tourValueColor));
			_fontEditorTourContent.setSelection(profile.tourContentFont);
			_fontEditorTourTitle.setSelection(profile.tourTitleFont);
			_fontEditorTourValue.setSelection(profile.tourValueFont);
			_spinnerTour_Margin_Top.setSelection(profile.tourMarginTop);
			_spinnerTour_Margin_Bottom.setSelection(profile.tourMarginBottom);
			_spinnerTour_Margin_Left.setSelection(profile.tourMarginLeft);
			_spinnerTour_Margin_Right.setSelection(profile.tourMarginRight);
			_spinnerTour_TruncatedLines.setSelection(profile.tourTruncatedLines);
			_spinnerTour_ValueColumns.setSelection(profile.tourValueColumns);
			selectDataFormatter(
					profile.allTourFormatterData,
					CalendarProfileManager.allTourContentFormatter,
					_chkTour_AllIsShowLines,
					_comboTour_AllValues,
					_comboTour_AllFormats,
					_tourFormatterContainer);

			// week summary column
			_chkIsShowSummaryColumn.setSelection(profile.isShowSummaryColumn);
			_chkIsShowWeekValueUnit.setSelection(profile.isShowWeekValueUnit);
			_comboWeek_ValueColor.select(getGraphColorIndex(profile.weekValueColor));
			_colorWeek_ValueColor.setColorValue(profile.weekValueRGB);
			_fontEditorWeekValue.setSelection(profile.weekValueFont);
			_spinnerWeek_ColumnWidth.setSelection(profile.weekColumnWidth);
			_spinnerWeek_Margin_Top.setSelection(profile.weekMarginTop);
			_spinnerWeek_Margin_Bottom.setSelection(profile.weekMarginBottom);
			_spinnerWeek_Margin_Left.setSelection(profile.weekMarginLeft);
			_spinnerWeek_Margin_Right.setSelection(profile.weekMarginRight);
			selectDataFormatter(
					profile.allWeekFormatterData,
					CalendarProfileManager.allWeekFormatter,
					_chkWeek_AllIsShowLines,
					_comboWeek_AllValues,
					_comboWeek_AllFormats,
					_weekFormatterContainer);
		}
		_isUpdateUI = false;
	}

	private void restoreState_UI() {

		_tabFolder.setSelection(Util.getStateInt(_state, STATE_SELECTED_TAB, 0));
	}

	private void save_DefaultIds(final CalendarProfile profile) {

		final int selectedDefaultIdIndex = _comboProfile_AllDefaultId.getSelectionIndex();
		final ProfileDefaultId_ComboData selectedComboData = _allDefaultComboData.get(selectedDefaultIdIndex);
		final DefaultId selectedDefaultId = selectedComboData.defaultId;

		profile.defaultId = selectedDefaultId;
		profile.isUserParentDefault = _chkIsUserDefaultId.getSelection();
		profile.userParentDefaultId = _txtUserParentDefaultId.getText();
		profile.userDefaultId = selectedDefaultId == DefaultId.USER_ID
				? selectedComboData.labelOrUserId
				: UI.EMPTY_STRING;
	}

	private void saveState_Profile() {

		// update profile

		final CalendarProfile profile = CalendarProfileManager.getActiveCalendarProfile();

		// profile
		save_DefaultIds(profile);
		profile.profileName = _txtProfileName.getText();

		// layout
		profile.alternateMonthRGB = _colorMonth_AlternateColor.getColorValue();
		profile.alternateMonth2RGB = _colorMonth_AlternateColor2.getColorValue();
		profile.calendarBackgroundRGB = _colorCalendar_BackgroundColor.getColorValue();
		profile.calendarForegroundRGB = _colorCalendar_ForegroundColor.getColorValue();
		profile.dayHoveredRGB = _colorDay_HoveredColor.getColorValue();
		profile.daySelectedRGB = _colorDay_SelectedColor.getColorValue();
		profile.isToggleMonthColor = _chkIsShowMonthColor.getSelection();
		profile.isWeekRowHeight = _rdoWeekRow_Height.getSelection();
		profile.useDraggedScrolling = _chkUseDraggedScrolling.getSelection();
		profile.weekHeight = _spinnerWeek_Height.getSelection();
		profile.weekRows = _spinnerWeek_Rows.getSelection();

		// 1. Date column
		profile.dateColumnContent = getSelectedDateColumn();
		profile.dateColumnWidth = _spinnerDateColumnWidth.getSelection();
		profile.dateColumnFont = _fontEditorDateColumn.getSelection();
		profile.isShowDateColumn = _chkIsShowDateColumn.getSelection();

		// 2. Year columns
		profile.isShowYearColumns = _chkIsShowYearColumns.getSelection();
		profile.isYearColumnDayWidth = _rdoYear_ColumnDayWidth.getSelection();
		profile.yearColumns = _spinnerYear_Columns.getSelection();
		profile.yearColumnsStart = getSelectedColumnLayout();
		profile.yearColumnsSpacing = _spinnerYear_ColumnSpacing.getSelection();
		profile.yearColumnDayWidth = _spinnerYear_DayWidth.getSelection();
		profile.yearHeaderFont = _fontEditorYearColumnHeader.getSelection();

		// 3. Week summary column
		profile.isShowSummaryColumn = _chkIsShowSummaryColumn.getSelection();
		profile.isShowWeekValueUnit = _chkIsShowWeekValueUnit.getSelection();
		profile.weekColumnWidth = _spinnerWeek_ColumnWidth.getSelection();
		profile.weekMarginTop = _spinnerWeek_Margin_Top.getSelection();
		profile.weekMarginBottom = _spinnerWeek_Margin_Bottom.getSelection();
		profile.weekMarginLeft = _spinnerWeek_Margin_Left.getSelection();
		profile.weekMarginRight = _spinnerWeek_Margin_Right.getSelection();
		profile.weekValueFont = _fontEditorWeekValue.getSelection();
		profile.weekValueColor = getSelected_Tour_GraphColor(_comboWeek_ValueColor);
		profile.weekValueRGB = _colorWeek_ValueColor.getColorValue();

		// day date
		profile.dayDateFont = _fontEditorDayDate.getSelection();
		profile.dayDateFormat = getSelectedDayDateFormat();
		profile.isHideDayDateWhenNoTour = _chkIsHideDayDateWhenNoTour.getSelection();
		profile.isShowDayDate = _chkIsShowDayDate.getSelection();
		profile.isShowDayDateWeekendColor = _chkIsShowDayDateWeekendColor.getSelection();
		profile.dayDateMarginTop = _spinnerDayDate_Margin_Top.getSelection();
		profile.dayDateMarginLeft = _spinnerDayDate_Margin_Left.getSelection();

		// tour background
		profile.tourBackground = getSelectedTourBackgroundData().tourBackground;
		profile.tourBackgroundWidth = _spinnerTour_BackgroundWidth.getSelection();
		profile.tourBorder = getSelectedTourBorderData().tourBorder;
		profile.tourBorderWidth = _spinnerTour_BorderWidth.getSelection();

		profile.tourBackground1Color = getSelected_Tour_GraphColor(_comboTour_BackgroundColor1);
		profile.tourBackground2Color = getSelected_Tour_GraphColor(_comboTour_BackgroundColor2);
		profile.tourBorderColor = getSelected_Tour_GraphColor(_comboTour_BorderColor);
		profile.tourDraggedColor = getSelected_Tour_GraphColor(_comboTour_DraggedColor);
		profile.tourHoveredColor = getSelected_Tour_GraphColor(_comboTour_HoveredColor);
		profile.tourSelectedColor = getSelected_Tour_GraphColor(_comboTour_SelectedColor);

		profile.tourBackground1RGB = _colorTour_BackgroundColor1.getColorValue();
		profile.tourBackground2RGB = _colorTour_BackgroundColor2.getColorValue();
		profile.tourBorderRGB = _colorTour_BorderColor.getColorValue();
		profile.tourDraggedRGB = _colorTour_DraggedColor.getColorValue();
		profile.tourHoveredRGB = _colorTour_HoveredColor.getColorValue();
		profile.tourSelectedRGB = _colorTour_SelectedColor.getColorValue();

		// tour content
		profile.isShowTourContent = _chkIsShowTourContent.getSelection();
		profile.isShowTourValueUnit = _chkIsShowTourValueUnit.getSelection();
		profile.isTruncateTourText = _chkIsTruncateTourText.getSelection();
		profile.tourContentFont = _fontEditorTourContent.getSelection();
		profile.tourTitleFont = _fontEditorTourTitle.getSelection();
		profile.tourMarginTop = _spinnerTour_Margin_Top.getSelection();
		profile.tourMarginBottom = _spinnerTour_Margin_Bottom.getSelection();
		profile.tourMarginLeft = _spinnerTour_Margin_Left.getSelection();
		profile.tourMarginRight = _spinnerTour_Margin_Right.getSelection();
		profile.tourTruncatedLines = _spinnerTour_TruncatedLines.getSelection();
		profile.tourValueColumns = _spinnerTour_ValueColumns.getSelection();
		profile.tourValueFont = _fontEditorTourValue.getSelection();

		profile.tourContentColor = getSelected_Tour_ContentColor(_comboTour_ContentColor).dayContentColor;
		profile.tourTitleColor = getSelected_Tour_ContentColor(_comboTour_TitleColor).dayContentColor;
		profile.tourValueColor = getSelected_Tour_ContentColor(_comboTour_ValueColor).dayContentColor;

		profile.tourContentRGB = _colorTour_ContentColor.getColorValue();
		profile.tourTitleRGB = _colorTour_TitleColor.getColorValue();
		profile.tourValueRGB = _colorTour_ValueColor.getColorValue();

		profile.allTourFormatterData = getSelectedFormatterData(
				CalendarProfileManager.allTourContentFormatter,
				CalendarProfileManager.NUM_DEFAULT_TOUR_FORMATTER,
				_chkTour_AllIsShowLines,
				_comboTour_AllValues,
				_comboTour_AllFormats);

		profile.allWeekFormatterData = getSelectedFormatterData(
				CalendarProfileManager.allWeekFormatter,
				CalendarProfileManager.NUM_DEFAULT_WEEK_FORMATTER,
				_chkWeek_AllIsShowLines,
				_comboWeek_AllValues,
				_comboWeek_AllFormats);
	}

	private void saveState_UI() {

		final int selectedTabIndex = _tabFolder.getSelectionIndex();
		_state.put(STATE_SELECTED_TAB, selectedTabIndex < 0 ? 0 : selectedTabIndex);
	}

	private void selectDataFormatter(	final FormatterData[] allFormatterData,
										final DataFormatter[] allDataFormatter,
										final Button[] chkAllIsShowLines,
										final Combo[] comboAllValues,
										final Combo[] comboAllFormats,
										final Composite formatterContainer) {
		// loop: all lines
		for (int lineIndex = 0; lineIndex < allFormatterData.length; lineIndex++) {

			final Button chkIsShowLine = chkAllIsShowLines[lineIndex];
			final Combo comboValue = comboAllValues[lineIndex];

			final FormatterData formatterData = allFormatterData[lineIndex];
			final FormatterID formatterId = formatterData.id;

			// loop: all formatter
			for (int formatterIndex = 0; formatterIndex < allDataFormatter.length; formatterIndex++) {

				final DataFormatter dataFormatter = allDataFormatter[formatterIndex];

				if (formatterId == dataFormatter.id) {

					final Combo comboFormat = comboAllFormats[lineIndex];

					// enable before setting a value
					comboValue.setEnabled(true);
					comboFormat.setEnabled(true);

					// select formatter value
					comboValue.select(formatterIndex);

					// remove old formats
					comboFormat.removeAll();

					if (isOnlyTextFormatter(dataFormatter)) {

						// do not fill combo, make it simple

						break;

					} else {

						// fill value format combo
						fillUI_Formats(comboFormat, dataFormatter);

						// select value format
						final ValueFormat[] valueFormats = dataFormatter.getValueFormats();
						if (valueFormats != null) {

							int formatterFormatIndex = 0;

							for (int formatIndex = 0; formatIndex < valueFormats.length; formatIndex++) {

								final ValueFormat valueFormat = valueFormats[formatIndex];

								if (formatterData.valueFormat == valueFormat) {

									formatterFormatIndex = formatIndex;
									break;
								}
							}

							comboFormat.select(formatterFormatIndex);

							break;
						}
					}
				}
			}

			chkIsShowLine.setSelection(formatterData.isEnabled);
		}

		formatterContainer.layout(true, true);
	}

	private void selectProfile(final CalendarProfile selectedProfile) {

		if (selectedProfile == null) {
			return;
		}

		// check if another profile is selected
		final CalendarProfile activeProfile = CalendarProfileManager.getActiveCalendarProfile();
		if (selectedProfile.equals(activeProfile)) {

			// profile has not changed
			return;
		}

		// keep data from previous profile
		saveState_Profile();

		CalendarProfileManager.setActiveCalendarProfile(selectedProfile, true);
		CalendarProfileManager.getActiveCalendarProfile().dump();
	}

	private void updateUI_NewProfile(final CalendarProfile calendarProfile) {

		// update model
		_allCalendarProfiles.add(calendarProfile);

		// update viewer
		_profileViewer.refresh();

		// select new profile
		selectProfile(calendarProfile);

		_txtProfileName.setFocus();
		_txtProfileName.selectAll();
	}

}
