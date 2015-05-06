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
package net.tourbook.export;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourWayPoint;
import net.tourbook.database.TourDatabase;
import net.tourbook.ext.velocity.VelocityService;
import net.tourbook.extension.export.ExportTourExtension;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.FileCollisionBehavior;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.tools.generic.MathTool;
import org.dinopolis.gpstool.gpsinput.GPSRoute;
import org.dinopolis.gpstool.gpsinput.GPSTrack;
import org.dinopolis.gpstool.gpsinput.GPSTrackpoint;
import org.dinopolis.gpstool.gpsinput.garmin.GarminTrack;
import org.dinopolis.gpstool.gpsinput.garmin.GarminTrackpointAdapter;
import org.dinopolis.gpstool.gpsinput.garmin.GarminTrackpointD304;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.osgi.framework.Version;

public class DialogExportTour extends TitleAreaDialog {

	private static final String				EXPORT_ID_GPX					= "net.tourbook.export.gpx";					//$NON-NLS-1$
	private static final String				EXPORT_ID_TCX					= "net.tourbook.export.tcx";					//$NON-NLS-1$

	private static final String				STATE_GPX_IS_ABSOLUTE_DISTANCE	= "STATE_GPX_IS_ABSOLUTE_DISTANCE";			//$NON-NLS-1$
	private static final String				STATE_GPX_IS_EXPORT_DESCRITION	= "STATE_GPX_IS_EXPORT_DESCRITION";			//$NON-NLS-1$
	private static final String				STATE_GPX_IS_EXPORT_MARKERS		= "STATE_GPX_IS_EXPORT_MARKERS";				//$NON-NLS-1$
	private static final String				STATE_GPX_IS_EXPORT_TOUR_DATA	= "STATE_GPX_IS_EXPORT_TOUR_DATA";				//$NON-NLS-1$

	private static final String				STATE_TCX_IS_COURSES			= "STATE_TCX_IS_COURSES";						//$NON-NLS-1$
	private static final String				STATE_TCX_IS_EXPORT_DESCRITION	= "STATE_TCX_IS_EXPORT_DESCRITION";			//$NON-NLS-1$
	private static final String				STATE_TCX_IS_NAME_FROM_TOUR		= "STATE_TCX_IS_NAME_FROM_TOUR";				//$NON-NLS-1$
	private static final String				STATE_TCX_COURSE_NAME			= "STATE_TCX_COURSE_NAME";						//$NON-NLS-1$

	private static final String				STATE_CAMOUFLAGE_SPEED			= "camouflageSpeedValue";						//$NON-NLS-1$
	private static final String				STATE_IS_CAMOUFLAGE_SPEED		= "isCamouflageSpeed";							//$NON-NLS-1$
	private static final String				STATE_IS_EXPORT_TOUR_RANGE		= "isExportTourRange";							//$NON-NLS-1$
	private static final String				STATE_IS_OVERWRITE_FILES		= "isOverwriteFiles";							//$NON-NLS-1$
	private static final String				STATE_IS_MERGE_ALL_TOURS		= "isMergeAllTours";							//$NON-NLS-1$
	private static final String				STATE_EXPORT_PATH_NAME			= "exportPathName";							//$NON-NLS-1$
	private static final String				STATE_EXPORT_FILE_NAME			= "exportFileName";							//$NON-NLS-1$

	/*
	 * Velocity (VC) context values
	 */
	private static final String				VC_HAS_TOUR_MARKERS				= "hasTourMarkers";							//$NON-NLS-1$
	private static final String				VC_HAS_TRACKS					= "hasTracks";									//$NON-NLS-1$
	private static final String				VC_HAS_WAY_POINTS				= "hasWayPoints";								//$NON-NLS-1$
	private static final String				VC_IS_EXPORT_ALL_TOUR_DATA		= "isExportAllTourData";						//$NON-NLS-1$

	private static final String				VC_LAP							= "lap";										//$NON-NLS-1$
	private static final String				VC_TOUR_DATA					= "tourData";									//$NON-NLS-1$
	private static final String				VC_TOUR_MARKERS					= "tourMarkers";								//$NON-NLS-1$
	private static final String				VC_TRACKS						= "tracks";									//$NON-NLS-1$
	private static final String				VC_WAY_POINTS					= "wayPoints";									//$NON-NLS-1$

	private static final String				ZERO							= "0";											//$NON-NLS-1$

	private static final int				VERTICAL_SECTION_MARGIN			= 10;
	private static final int				SIZING_TEXT_FIELD_WIDTH			= 250;
	private static final int				COMBO_HISTORY_LENGTH			= 20;

	private static String					_dlgDefaultMessage;
	//
	private static final DecimalFormat		_nf1							= (DecimalFormat) NumberFormat
																					.getInstance(Locale.US);
	private static final DecimalFormat		_nf3							= (DecimalFormat) NumberFormat
																					.getInstance(Locale.US);
	private static final DecimalFormat		_nf8							= (DecimalFormat) NumberFormat
																					.getInstance(Locale.US);

	private static final DateTimeFormatter	_dtIso							= ISODateTimeFormat.dateTimeNoMillis();
	private static final SimpleDateFormat	_dateFormat						= new SimpleDateFormat();
	private static final DateFormat			_timeFormatter					= DateFormat
																					.getTimeInstance(DateFormat.MEDIUM);

	static {

		_nf1.setMinimumFractionDigits(1);
		_nf1.setMaximumFractionDigits(1);
		_nf1.setGroupingUsed(false);

		_nf3.setMinimumFractionDigits(1);
		_nf3.setMaximumFractionDigits(3);
		_nf3.setGroupingUsed(false);

		_nf8.setMinimumFractionDigits(1);
		_nf8.setMaximumFractionDigits(8);
		_nf8.setGroupingUsed(false);

		_dtIso.withZoneUTC();
		_dateFormat.applyPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"); //$NON-NLS-1$
		_dateFormat.setTimeZone(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$
	}

	private final String					_formatTemplate;

	private final IDialogSettings			_state							= TourbookPlugin
																					.getState("DialogExportTour");			//$NON-NLS-1$

	private final ExportTourExtension		_exportExtensionPoint;

	private final ArrayList<TourData>		_tourDataList;
	private final int						_tourStartIndex;
	private final int						_tourEndIndex;

	/**
	 * Is <code>true</code> when multiple tours are selected and NOT merged into 1 file.
	 */
	private boolean							_isExport_MultipleToursWithMultipleFiles;

	private boolean							_isInUIInit;

	/**
	 * Is <code>true</code> when GPX export.
	 */
	private boolean							_isSetup_GPX;

	/**
	 * Is <code>true</code> when TCX export.
	 */
	private boolean							_isSetup_TCX;

	/**
	 * Is <code>true</code> when only a part is exported.
	 */
	private boolean							_isSetup_TourRange;

	/**
	 * Is <code>true</code> when multiple tours are exported.
	 */
	private boolean							_isSetup_MultipleTours;

	private int								_mergedDistance;
	private DateTime						_mergedTime;

	private Point							_shellDefaultSize;

	private float							_exportState_CamouflageSpeed;
	private FileCollisionBehavior			_exportState_FileCollisionBehaviour;
	private boolean							_exportState_isAbsoluteDistance;
	private boolean							_exportState_IsCamouflageSpeed;
	private boolean							_exportState_IsMergeTours;
	private boolean							_exportState_IsOverwriteFiles;
	private boolean							_exportState_IsRangeExport;

	private boolean							_exportState_GPX_IsExportMarkers;
	private boolean							_exportState_GPX_IsExportAllTourData;

	private String							_exportState_TCX_CourseName;
	private boolean							_exportState_TCX_IsCourses;
	private boolean							_exportState_TCX_IsExportDescription;

	private PixelConverter					_pc;

	/*
	 * UI controls
	 */
	private Button							_btnSelectDirectory;
	private Button							_btnSelectFile;

	private Button							_chkCamouflageSpeed;
	private Button							_chkExportTourRange;
	private Button							_chkMergeAllTours;
	private Button							_chkOverwriteFiles;

	private Button							_chkGPX_Markers;
	private Button							_chkGPX_NoneGPXFields;
	private Button							_chkGPX_Description;
	private Button							_rdoGPX_DistanceAbsolute;
	private Button							_rdoGPX_DistanceRelative;

	private Button							_chkTCX_Description;
	private Button							_rdoTCX_Activities;
	private Button							_rdoTCX_Courses;
	private Button							_rdoTCX_NameFromField;
	private Button							_rdoTCX_NameFromTour;

	private Combo							_comboFile;
	private Combo							_comboPath;
	private Combo							_comboTcxCourseName;

	private Composite						_dlgContainer;
	private Composite						_inputContainer;

	private Label							_lblCoumouflageSpeedUnit;
	private Label							_lblTcxCourseName;
	private Label							_lblTcxNameFrom;

	private Spinner							_spinnerCamouflageSpeed;

	private Text							_txtFilePath;

	/**
	 * @param parentShell
	 * @param exportExtensionPoint
	 * @param tourDataList
	 * @param tourStartIndex
	 * @param tourEndIndex
	 * @param formatTemplate
	 * @param isOptionDistance
	 */
	public DialogExportTour(final Shell parentShell,
							final ExportTourExtension exportExtensionPoint,
							final ArrayList<TourData> tourDataList,
							final int tourStartIndex,
							final int tourEndIndex,
							final String formatTemplate) {

		super(parentShell);

		int shellStyle = getShellStyle();

		shellStyle = //
		SWT.NONE //
				| SWT.TITLE
				| SWT.CLOSE
				| SWT.MIN
//				| SWT.MAX
				| SWT.RESIZE
				| SWT.NONE;

		// make dialog resizable
		setShellStyle(shellStyle);

		_exportExtensionPoint = exportExtensionPoint;
		_formatTemplate = formatTemplate;

		_tourDataList = tourDataList;
		_tourStartIndex = tourStartIndex;
		_tourEndIndex = tourEndIndex;

		_isSetup_GPX = _exportExtensionPoint.getExportId().equalsIgnoreCase(EXPORT_ID_GPX);
		_isSetup_TCX = _exportExtensionPoint.getExportId().equalsIgnoreCase(EXPORT_ID_TCX);

		_isSetup_MultipleTours = _tourDataList.size() > 1;
		_isSetup_TourRange = _tourDataList.size() == 1 //
				&& _tourStartIndex >= 0
				&& _tourEndIndex > -1;

		_dlgDefaultMessage = NLS.bind(Messages.dialog_export_dialog_message, _exportExtensionPoint.getVisibleName());

		// initialize velocity
		VelocityService.init();
	}

	@Override
	public boolean close() {

		saveState();

		return super.close();
	}

	@Override
	protected void configureShell(final Shell shell) {

		super.configureShell(shell);

		shell.setText(Messages.dialog_export_shell_text);

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

		setTitle(Messages.dialog_export_dialog_title);
		setMessage(_dlgDefaultMessage);

		_isInUIInit = true;
		{
			restoreState();
		}
		_isInUIInit = false;

//		validateFields();
		enableFields();
	}

	@Override
	protected final void createButtonsForButtonBar(final Composite parent) {

		super.createButtonsForButtonBar(parent);

		// set text for the OK button
		getButton(IDialogConstants.OK_ID).setText(Messages.dialog_export_btn_export);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		initUI(parent);

		_dlgContainer = (Composite) super.createDialogArea(parent);

		createUI(_dlgContainer);

		return _dlgContainer;
	}

	private void createUI(final Composite parent) {

		_inputContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_inputContainer);
		GridLayoutFactory.swtDefaults().margins(10, 5).applyTo(_inputContainer);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			createUI_10_Options(_inputContainer);
			createUI_90_ExportFile(_inputContainer);
		}
	}

	private void createUI_10_Options(final Composite parent) {

		/*
		 * What
		 */
		final Group groupWhat = new Group(parent, SWT.NONE);
		groupWhat.setText(Messages.Dialog_Export_Group_What);
		groupWhat.setToolTipText(Messages.Dialog_Export_Group_What_Tooltip);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(groupWhat);
		GridLayoutFactory.swtDefaults().applyTo(groupWhat);
//		groupWhat.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			createUI_20_Option_What(groupWhat);
			createUI_40_Option_TourRange(groupWhat);
		}

		/*
		 * How
		 */
		final Group groupHow = new Group(parent, SWT.NONE);
		groupHow.setText(Messages.Dialog_Export_Group_How);
		groupHow.setToolTipText(Messages.Dialog_Export_Group_How_Tooltip);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(groupHow);
		GridLayoutFactory.swtDefaults().applyTo(groupHow);
//		groupHow.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			createUI_50_Option_How(groupHow);
		}
	}

	private void createUI_20_Option_What(final Composite parent) {

		if (_isSetup_GPX) {

			/*
			 * checkbox: export description
			 */
			_chkGPX_Description = new Button(parent, SWT.CHECK);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(_chkGPX_Description);
			_chkGPX_Description.setText(Messages.Dialog_Export_Checkbox_Description);

			/*
			 * checkbox: export markers
			 */
			_chkGPX_Markers = new Button(parent, SWT.CHECK);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(_chkGPX_Markers);
			_chkGPX_Markers.setText(Messages.dialog_export_chk_exportMarkers);
			_chkGPX_Markers.setToolTipText(Messages.dialog_export_chk_exportMarkers_tooltip);

			/*
			 * checkbox: export custom data
			 */
			_chkGPX_NoneGPXFields = new Button(parent, SWT.CHECK);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(_chkGPX_NoneGPXFields);
			_chkGPX_NoneGPXFields.setText(Messages.Dialog_Export_Checkbox_TourFields);
			_chkGPX_NoneGPXFields.setToolTipText(Messages.Dialog_Export_Checkbox_TourFields_Tooltip);

		} else if (_isSetup_TCX) {

			/*
			 * checkbox: export description
			 */
			_chkTCX_Description = new Button(parent, SWT.CHECK);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(_chkTCX_Description);
			_chkTCX_Description.setText(Messages.dialog_export_chk_exportNotes);
			_chkTCX_Description.setToolTipText(Messages.dialog_export_chk_exportNotes_tooltip);
		}
	}

	private void createUI_40_Option_TourRange(final Composite parent) {

		if (_isSetup_TourRange == false) {
			return;
		}

		String tourRangeUI = null;

		final TourData tourData = _tourDataList.get(0);
		final int[] timeSerie = tourData.timeSerie;
		if (timeSerie != null) {

			final float[] distanceSerie = tourData.distanceSerie;
			final boolean isDistance = distanceSerie != null;

			final int startTime = timeSerie[_tourStartIndex];
			final int endTime = timeSerie[_tourEndIndex];

			final DateTime dtTour = tourData.getTourStartTime();

			final String uiStartTime = _timeFormatter.format(dtTour.plusSeconds(startTime).toDate());
			final String uiEndTime = _timeFormatter.format(dtTour.plusSeconds(endTime).toDate());

			if (isDistance) {

				tourRangeUI = NLS.bind(
						Messages.dialog_export_chk_tourRangeWithDistance,
						new Object[] {

								uiStartTime,
								uiEndTime,

								_nf3.format(distanceSerie[_tourStartIndex]
										/ 1000
										/ net.tourbook.ui.UI.UNIT_VALUE_DISTANCE),
								_nf3.format(distanceSerie[_tourEndIndex]
										/ 1000
										/ net.tourbook.ui.UI.UNIT_VALUE_DISTANCE),

								UI.UNIT_LABEL_DISTANCE,

								// adjust by 1 to corresponds to the number in the tour editor
								_tourStartIndex + 1,
								_tourEndIndex + 1 });

			} else {

				tourRangeUI = NLS.bind(Messages.dialog_export_chk_tourRangeWithoutDistance, new Object[] {
						uiStartTime,
						uiEndTime,
						_tourStartIndex + 1,
						_tourEndIndex + 1 });
			}
		}

		/*
		 * checkbox: tour range
		 */
		_chkExportTourRange = new Button(parent, SWT.CHECK);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_chkExportTourRange);

		_chkExportTourRange.setText(tourRangeUI != null ? tourRangeUI : Messages.dialog_export_chk_tourRangeDisabled);

		_chkExportTourRange.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				enableFields();
			}
		});
	}

	private void createUI_50_Option_How(final Composite parent) {

		if (_isSetup_MultipleTours) {

			/*
			 * checkbox: merge all tours
			 */
			_chkMergeAllTours = new Button(parent, SWT.CHECK);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(_chkMergeAllTours);
			_chkMergeAllTours.setText(Messages.dialog_export_chk_mergeAllTours);
			_chkMergeAllTours.setToolTipText(Messages.dialog_export_chk_mergeAllTours_tooltip);
			_chkMergeAllTours.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					enableFields();
					setFileName();
				}
			});
		}

		createUI_60_Option_Speed(parent);

		if (_isSetup_GPX) {

			createUI_70_Option_GPX_Distance(parent);

		} else if (_isSetup_TCX) {

			createUI_80_Option_TCX_ActivitiesCourses(parent);
		}
	}

	private void createUI_60_Option_Speed(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
		{
			/*
			 * checkbox: camouflage speed
			 */
			_chkCamouflageSpeed = new Button(container, SWT.CHECK);
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_chkCamouflageSpeed);
			_chkCamouflageSpeed.setText(Messages.dialog_export_chk_camouflageSpeed);
			_chkCamouflageSpeed.setToolTipText(Messages.dialog_export_chk_camouflageSpeed_tooltip);
			_chkCamouflageSpeed.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {

					validateFields();
					enableFields();

					if (_chkCamouflageSpeed.getSelection()) {
						_spinnerCamouflageSpeed.setFocus();
					}
				}
			});

			// text: speed
			_spinnerCamouflageSpeed = new Spinner(container, SWT.BORDER);
			GridDataFactory.fillDefaults() //
					.align(SWT.BEGINNING, SWT.FILL)
					.applyTo(_spinnerCamouflageSpeed);
			_spinnerCamouflageSpeed.setToolTipText(Messages.dialog_export_chk_camouflageSpeedInput_tooltip);
			_spinnerCamouflageSpeed.setPageIncrement(10);
			_spinnerCamouflageSpeed.setMinimum(1);
			_spinnerCamouflageSpeed.setMaximum(1000);
			_spinnerCamouflageSpeed.addMouseWheelListener(new MouseWheelListener() {
				@Override
				public void mouseScrolled(final MouseEvent event) {
					Util.adjustSpinnerValueOnMouseScroll(event);
				}
			});

			// label: unit
			_lblCoumouflageSpeedUnit = new Label(container, SWT.NONE);
			_lblCoumouflageSpeedUnit.setText(UI.SYMBOL_AVERAGE_WITH_SPACE + UI.UNIT_LABEL_SPEED);
			GridDataFactory
					.fillDefaults()
					.grab(true, false)
					.align(SWT.BEGINNING, SWT.CENTER)
					.applyTo(_lblCoumouflageSpeedUnit);
		}
	}

	private void createUI_70_Option_GPX_Distance(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
		{
			// label
			final Label label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(label);
			label.setText(Messages.Dialog_Export_Label_GPX_DistanceValues);

			// radio
			{
				_rdoGPX_DistanceAbsolute = new Button(container, SWT.RADIO);
				_rdoGPX_DistanceAbsolute.setText(Messages.Dialog_Export_Radio_GPX_DistanceAbsolute);
				_rdoGPX_DistanceAbsolute.setToolTipText(Messages.Dialog_Export_Radio_GPX_DistanceAbsolute_Tooltip);

				_rdoGPX_DistanceRelative = new Button(container, SWT.RADIO);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(_rdoGPX_DistanceRelative);
				_rdoGPX_DistanceRelative.setText(Messages.Dialog_Export_Radio_GPX_DistanceRelative);
				_rdoGPX_DistanceRelative.setToolTipText(Messages.Dialog_Export_Radio_GPX_DistanceRelative_Tooltip);
			}
		}
	}

	private void createUI_80_Option_TCX_ActivitiesCourses(final Composite parent) {

		final SelectionAdapter defaultSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				enableFields();
				setFileName();
			}
		};

		final SelectionAdapter nameSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				updateUI_CourseName();
				enableFields();
				setFileName();
			}
		};

		final ModifyListener nameModifyListener = new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent e) {
				validateFields();
			}
		};

		// container
		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			{
				/*
				 * label: tcx type
				 */
				final Label label = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults().applyTo(label);
				label.setText(Messages.Dialog_Export_Label_TCX_Type);

				final Composite containerActivities = new Composite(container, SWT.NONE);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(containerActivities);
				GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerActivities);
				{
					/*
					 * radio: activities
					 */
					_rdoTCX_Courses = new Button(containerActivities, SWT.RADIO);
					GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_rdoTCX_Courses);
					_rdoTCX_Courses.setText(Messages.Dialog_Export_Radio_TCX_Courses);
					_rdoTCX_Courses.setToolTipText(Messages.Dialog_Export_Radio_TCX_Courses_Tooltip);
					_rdoTCX_Courses.addSelectionListener(defaultSelectionListener);

					/*
					 * radio: activities
					 */
					_rdoTCX_Activities = new Button(containerActivities, SWT.RADIO);
					GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_rdoTCX_Activities);
					_rdoTCX_Activities.setText(Messages.Dialog_Export_Radio_TCX_Aktivities);
					_rdoTCX_Activities.setToolTipText(Messages.Dialog_Export_Radio_TCX_Aktivities_Tooltip);
					_rdoTCX_Activities.addSelectionListener(defaultSelectionListener);
				}
			}

			{
				/*
				 * label: course name from
				 */
				_lblTcxNameFrom = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults().applyTo(_lblTcxNameFrom);
				_lblTcxNameFrom.setText(Messages.Dialog_Export_Label_TCX_NameFrom);
				_lblTcxNameFrom.setToolTipText(Messages.Dialog_Export_Label_TCX_NameFrom_Tooltip);

				final Composite containerNameFrom = new Composite(container, SWT.NONE);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(containerNameFrom);
				GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerNameFrom);
				{
					/*
					 * radio: from tour
					 */
					_rdoTCX_NameFromTour = new Button(containerNameFrom, SWT.RADIO);
					GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_rdoTCX_NameFromTour);
					_rdoTCX_NameFromTour.setText(Messages.Dialog_Export_Radio_TCX_NameFromTour);
					_rdoTCX_NameFromTour.addSelectionListener(nameSelectionListener);

					/*
					 * radio: from text field
					 */
					_rdoTCX_NameFromField = new Button(containerNameFrom, SWT.RADIO);
					GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_rdoTCX_NameFromField);
					_rdoTCX_NameFromField.setText(Messages.Dialog_Export_Radio_TCX_NameFromField);
					_rdoTCX_NameFromField.addSelectionListener(nameSelectionListener);
				}
			}

			{
				/*
				 * label: course name
				 */
				_lblTcxCourseName = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults().applyTo(_lblTcxCourseName);
				_lblTcxCourseName.setText(Messages.Dialog_Export_Label_TCX_CourseName);

				/*
				 * combo: name
				 */
				_comboTcxCourseName = new Combo(container, SWT.SINGLE | SWT.BORDER);
				GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_comboTcxCourseName);
				_comboTcxCourseName.setVisibleItemCount(20);
				_comboTcxCourseName.addModifyListener(nameModifyListener);
			}
		}
	}

	private void createUI_90_ExportFile(final Composite parent) {

		Label label;

		final ModifyListener filePathModifyListener = new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent e) {
				validateFields();
			}
		};

		/*
		 * group: filename
		 */
		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.dialog_export_group_exportFileName);
		GridDataFactory.fillDefaults().grab(true, false).indent(0, VERTICAL_SECTION_MARGIN).applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);
		{
			/*
			 * label: filename
			 */
			label = new Label(group, SWT.NONE);
			label.setText(Messages.dialog_export_label_fileName);

			/*
			 * combo: path
			 */
			_comboFile = new Combo(group, SWT.SINGLE | SWT.BORDER);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_comboFile);
			((GridData) _comboFile.getLayoutData()).widthHint = SIZING_TEXT_FIELD_WIDTH;
			_comboFile.setVisibleItemCount(20);
			_comboFile.addVerifyListener(net.tourbook.common.UI.verifyFilenameInput());
			_comboFile.addModifyListener(filePathModifyListener);
			_comboFile.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					validateFields();
				}
			});

			/*
			 * button: browse
			 */
			_btnSelectFile = new Button(group, SWT.PUSH);
			_btnSelectFile.setText(Messages.app_btn_browse);
			_btnSelectFile.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectBrowseFile();
					validateFields();
				}
			});
			setButtonLayoutData(_btnSelectFile);

			// -----------------------------------------------------------------------------

			/*
			 * label: path
			 */
			label = new Label(group, SWT.NONE);
			label.setText(Messages.dialog_export_label_exportFilePath);

			/*
			 * combo: path
			 */
			_comboPath = new Combo(group, SWT.SINGLE | SWT.BORDER);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_comboPath);
			((GridData) _comboPath.getLayoutData()).widthHint = SIZING_TEXT_FIELD_WIDTH;
			_comboPath.setVisibleItemCount(20);
			_comboPath.addModifyListener(filePathModifyListener);
			_comboPath.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					validateFields();
				}
			});

			/*
			 * button: browse
			 */
			_btnSelectDirectory = new Button(group, SWT.PUSH);
			_btnSelectDirectory.setText(Messages.app_btn_browse);
			_btnSelectDirectory.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectBrowseDirectory();
					validateFields();
				}
			});
			setButtonLayoutData(_btnSelectDirectory);

			// -----------------------------------------------------------------------------

			/*
			 * label: file path
			 */
			label = new Label(group, SWT.NONE);
			label.setText(Messages.dialog_export_label_filePath);

			/*
			 * text: filename
			 */
			_txtFilePath = new Text(group, /* SWT.BORDER | */SWT.READ_ONLY);
			GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_txtFilePath);
			_txtFilePath.setToolTipText(Messages.dialog_export_txt_filePath_tooltip);
			_txtFilePath.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

			// -----------------------------------------------------------------------------

			/*
			 * checkbox: overwrite files
			 */
			_chkOverwriteFiles = new Button(group, SWT.CHECK);
			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.CENTER)
					.span(3, 1)
					.indent(0, _pc.convertVerticalDLUsToPixels(4))
					.applyTo(_chkOverwriteFiles);
			_chkOverwriteFiles.setText(Messages.dialog_export_chk_overwriteFiles);
			_chkOverwriteFiles.setToolTipText(Messages.dialog_export_chk_overwriteFiles_tooltip);
		}

	}

	private void doExport() throws IOException {

		// disable button's
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		getButton(IDialogConstants.CANCEL_ID).setEnabled(false);

		_exportState_IsCamouflageSpeed = _chkCamouflageSpeed.getSelection();
		_exportState_IsOverwriteFiles = _chkOverwriteFiles.getSelection();

		_exportState_CamouflageSpeed = _spinnerCamouflageSpeed.getSelection();
		_exportState_CamouflageSpeed *= net.tourbook.ui.UI.UNIT_VALUE_DISTANCE / 3.6f;
		_exportState_FileCollisionBehaviour = new FileCollisionBehavior();

		if (_isSetup_TourRange) {
			_exportState_IsRangeExport = _chkExportTourRange.getSelection();
		}

		if (_isSetup_MultipleTours) {
			_exportState_IsMergeTours = _chkMergeAllTours.getSelection();
		}

		if (_isSetup_GPX) {

			_exportState_isAbsoluteDistance = _rdoGPX_DistanceAbsolute.getSelection();

			_exportState_TCX_IsExportDescription = _chkGPX_Description.getSelection();
			_exportState_GPX_IsExportMarkers = _chkGPX_Markers.getSelection();
			_exportState_GPX_IsExportAllTourData = _chkGPX_NoneGPXFields.getSelection();

		} else if (_isSetup_TCX) {

			// .tcx files do always contain absolute distances
			_exportState_isAbsoluteDistance = true;

			_exportState_TCX_IsExportDescription = _chkTCX_Description.getSelection();

			_exportState_TCX_IsCourses = _rdoTCX_Courses.getSelection();
			_exportState_TCX_CourseName = _comboTcxCourseName.getText();
		}

		final String exportFileName = _txtFilePath.getText();

		if (_tourDataList.size() == 1) {

			// export one tour

			final ArrayList<GarminTrack> tracks = new ArrayList<GarminTrack>();
			final ArrayList<TourWayPoint> wayPoints = new ArrayList<TourWayPoint>();
			final ArrayList<TourMarker> tourMarkers = new ArrayList<TourMarker>();

			final TourData tourData = _tourDataList.get(0);
			final DateTime trackStartTime = TourManager.getTourDateTime(tourData);

			final GarminLap tourLap = doExport_50_Lap(tourData);

			final GarminTrack track = doExport_60_TrackPoints(tourData, trackStartTime);
			if (track != null) {
				tracks.add(track);
			}

			doExport_70_WayPoints(wayPoints, tourMarkers, tourData, trackStartTime);

			doExport_10_Tour(tourData, tracks, wayPoints, tourMarkers, tourLap, exportFileName);

		} else {

			/*
			 * export multiple tours
			 */

			final String exportPathName;

			if (_exportState_IsMergeTours) {
				exportPathName = exportFileName;
			} else {
				exportPathName = getExportPathName();
			}

			try {

				final IRunnableWithProgress exportRunnable = new IRunnableWithProgress() {
					@Override
					public void run(final IProgressMonitor monitor) throws InvocationTargetException,
							InterruptedException {

						try {

							doExport_05_Runnable(monitor, exportPathName);

						} catch (final IOException e) {
							StatusUtil.log(e);
						}
					}
				};

				new ProgressMonitorDialog(Display.getCurrent().getActiveShell()).run(true, true, exportRunnable);

			} catch (final InvocationTargetException e) {
				StatusUtil.showStatus(e);
			} catch (final InterruptedException e) {
				StatusUtil.showStatus(e);
			}

		}
	}

	private void doExport_05_Runnable(final IProgressMonitor monitor, final String exportFileName) throws IOException {

		int exported = 0;
		final int tourSize = _tourDataList.size();

		monitor.beginTask(UI.EMPTY_STRING, tourSize);

		if (_exportState_IsMergeTours) {

			/*
			 * merge all tours into one
			 */

			_mergedTime = TourManager.getTourDateTime(_tourDataList.get(0));
			_mergedDistance = 0;

			final ArrayList<GarminTrack> tracks = new ArrayList<GarminTrack>();
			final ArrayList<TourWayPoint> wayPoints = new ArrayList<TourWayPoint>();
			final ArrayList<TourMarker> tourMarkers = new ArrayList<TourMarker>();

			final GarminLap tourLap = new GarminLap();

			// create tracklist and lap
			for (final TourData tourData : _tourDataList) {

				if (monitor.isCanceled()) {
					return;
				}

				monitor.worked(1);
				monitor.subTask(NLS.bind(Messages.Dialog_Export_SubTask_Export, new Object[] {
						++exported,
						tourSize,
						TourManager.getTourTitle(tourData) }));

				doExport_52_Laps(tourData, tourLap);

				DateTime trackStartTime;
				if (_exportState_IsCamouflageSpeed) {
					trackStartTime = _mergedTime;
				} else {
					trackStartTime = TourManager.getTourDateTime(tourData);
				}

				final GarminTrack track = doExport_60_TrackPoints(tourData, trackStartTime);
				if (track != null) {
					tracks.add(track);
				}

				doExport_70_WayPoints(wayPoints, tourMarkers, tourData, trackStartTime);
			}

			/*
			 * There is currently no listener to stop the velocity evalute method
			 */
			monitor.subTask(NLS.bind(Messages.Dialog_Export_SubTask_CreatingExportFile, exportFileName));

			doExport_10_Tour(null, tracks, wayPoints, tourMarkers, tourLap, exportFileName);

		} else {

			/*
			 * export each tour separately
			 */

			final IPath exportFilePath = new Path(exportFileName).addTrailingSeparator();
			final String fileExtension = _exportExtensionPoint.getFileExtension();

			for (final TourData tourData : _tourDataList) {

				if (monitor.isCanceled()) {
					break;
				}

				// create file path name
				final String tourFileName = net.tourbook.ui.UI.format_yyyymmdd_hhmmss(tourData);

				final String exportFilePathName = exportFilePath
						.append(tourFileName)
						.addFileExtension(fileExtension)
						.toOSString();

				monitor.worked(1);
				monitor.subTask(NLS.bind(Messages.Dialog_Export_SubTask_Export, new Object[] {
						++exported,
						tourSize,
						exportFilePathName }));

				final ArrayList<GarminTrack> tracks = new ArrayList<GarminTrack>();
				final ArrayList<TourWayPoint> wayPoints = new ArrayList<TourWayPoint>();
				final ArrayList<TourMarker> tourMarkers = new ArrayList<TourMarker>();

				final DateTime trackStartTime = TourManager.getTourDateTime(tourData);

				final GarminLap tourLap = doExport_50_Lap(tourData);

				final GarminTrack track = doExport_60_TrackPoints(tourData, trackStartTime);

				if (track != null) {
					tracks.add(track);
				}

				doExport_70_WayPoints(wayPoints, tourMarkers, tourData, trackStartTime);

				doExport_10_Tour(tourData, tracks, wayPoints, tourMarkers, tourLap, exportFilePathName);

				// check if overwrite dialog was canceled
				if (_exportState_FileCollisionBehaviour.value == FileCollisionBehavior.DIALOG_IS_CANCELED) {
					break;
				}
			}
		}
	}

	private void doExport_10_Tour(	final TourData tourData,
									final ArrayList<GarminTrack> tracks,
									final ArrayList<TourWayPoint> wayPoints,
									final ArrayList<TourMarker> tourMarkers,
									final GarminLap lap,
									final String exportFileName) throws IOException {

		boolean isOverwrite = true;

		final File exportFile = new File(exportFileName);
		if (exportFile.exists()) {
			if (_exportState_IsOverwriteFiles) {
				// overwrite is enabled in the UI
			} else {
				isOverwrite = net.tourbook.ui.UI.confirmOverwrite(_exportState_FileCollisionBehaviour, exportFile);
			}
		}

		if (isOverwrite == false) {
			return;
		}

		final VelocityContext vc = new VelocityContext();

		// math tool to convert float into double
		vc.put("math", new MathTool());//$NON-NLS-1$

		if (_isSetup_GPX) {

			vc.put(VC_IS_EXPORT_ALL_TOUR_DATA, _exportState_GPX_IsExportAllTourData && tourData != null);

		} else if (_isSetup_TCX) {

			vc.put("iscourses", _exportState_TCX_IsCourses); //$NON-NLS-1$
			vc.put("coursename", _exportState_TCX_CourseName); //$NON-NLS-1$
		}

		vc.put(VC_LAP, lap);
		vc.put(VC_TRACKS, tracks);
		vc.put(VC_WAY_POINTS, wayPoints);
		vc.put(VC_TOUR_MARKERS, tourMarkers);
		vc.put(VC_TOUR_DATA, tourData);

		vc.put(VC_HAS_TOUR_MARKERS, Boolean.valueOf(tourMarkers.size() > 0));
		vc.put(VC_HAS_TRACKS, Boolean.valueOf(tracks.size() > 0));
		vc.put(VC_HAS_WAY_POINTS, Boolean.valueOf(wayPoints.size() > 0));

		vc.put("dateformat", _dateFormat); //$NON-NLS-1$
		vc.put("dtIso", _dtIso); //$NON-NLS-1$
		vc.put("nf1", _nf1); //$NON-NLS-1$
		vc.put("nf3", _nf3); //$NON-NLS-1$
		vc.put("nf8", _nf8); //$NON-NLS-1$

		doExport_20_TourValues(vc);

		final Writer exportWriter = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(exportFile),
				UI.UTF_8));

		final Reader templateReader = new InputStreamReader(this.getClass().getResourceAsStream(_formatTemplate));

		try {

			Velocity.evaluate(vc, exportWriter, "MyTourbook", templateReader); //$NON-NLS-1$

		} catch (final Exception e) {
			StatusUtil.showStatus(e);
		} finally {
			exportWriter.close();
		}
	}

	/**
	 * Adds some values to the velocity context (e.g. date, ...).
	 * 
	 * @param vcContext
	 *            the velocity context holding all the data
	 */
	private void doExport_20_TourValues(final VelocityContext vcContext) {

		/*
		 * Current time, date
		 */
		final Calendar now = Calendar.getInstance();
		final Date creationDate = now.getTime();
		vcContext.put("creation_date", creationDate); //$NON-NLS-1$

		doExport_21_Creator(vcContext);
		doExport_22_MinMax_LatLon(vcContext);
		doExport_24_MinMax_Other(vcContext, creationDate);
	}

	private void doExport_21_Creator(final VelocityContext vcContext) {

		final Version version = Activator.getDefault().getVersion();

		/*
		 * Version
		 */
		String pluginMajorVersion = ZERO;
		String pluginMinorVersion = ZERO;
		String pluginMicroVersion = ZERO;
		String pluginQualifierVersion = ZERO;

		if (version != null) {
			pluginMajorVersion = Integer.toString(version.getMajor());
			pluginMinorVersion = Integer.toString(version.getMinor());
			pluginMicroVersion = Integer.toString(version.getMicro());
			pluginQualifierVersion = version.getQualifier();
		}

		vcContext.put("pluginMajorVersion", pluginMajorVersion); //$NON-NLS-1$
		vcContext.put("pluginMinorVersion", pluginMinorVersion); //$NON-NLS-1$
		vcContext.put("pluginMicroVersion", pluginMicroVersion); //$NON-NLS-1$
		vcContext.put("pluginQualifierVersion", pluginQualifierVersion); //$NON-NLS-1$

		/*
		 * Creator
		 */
		vcContext.put("creator",//$NON-NLS-1$
				String.format("MyTourbook %d.%d.%d.%s - http://mytourbook.sourceforge.net",//$NON-NLS-1$
						version.getMajor(),
						version.getMinor(),
						version.getMicro(),
						version.getQualifier()));
	}

	/**
	 * Calculate min/max values for latitude/longitude.
	 */
	private void doExport_22_MinMax_LatLon(final VelocityContext vcContext) {

		/*
		 * Extent of waypoint, routes and tracks:
		 */
		double min_latitude = 90.0;
		double min_longitude = 180.0;
		double max_latitude = -90.0;
		double max_longitude = -180.0;

		final List<?> routes = (List<?>) vcContext.get("routes"); //$NON-NLS-1$
		if (routes != null) {
			final Iterator<?> route_iterator = routes.iterator();
			while (route_iterator.hasNext()) {
				final GPSRoute route = (GPSRoute) route_iterator.next();
				min_longitude = route.getMinLongitude();
				max_longitude = route.getMaxLongitude();
				min_latitude = route.getMinLatitude();
				max_latitude = route.getMaxLatitude();
			}
		}

		final List<?> wayPoints = (List<?>) vcContext.get(VC_WAY_POINTS);
		if (wayPoints != null) {
			final Iterator<?> waypoint_iterator = wayPoints.iterator();
			while (waypoint_iterator.hasNext()) {
				final TourWayPoint waypoint = (TourWayPoint) waypoint_iterator.next();
				min_longitude = Math.min(min_longitude, waypoint.getLongitude());
				max_longitude = Math.max(max_longitude, waypoint.getLongitude());
				min_latitude = Math.min(min_latitude, waypoint.getLatitude());
				max_latitude = Math.max(max_latitude, waypoint.getLatitude());
			}
		}

		final List<?> tourMarkers = (List<?>) vcContext.get(VC_TOUR_MARKERS);
		if (tourMarkers != null) {

			for (final Object element : tourMarkers) {
				if (element instanceof TourMarker) {

					final TourMarker tourMarker = (TourMarker) element;

					final double longitude = tourMarker.getLongitude();
					final double latitude = tourMarker.getLatitude();

					if (longitude != TourDatabase.DEFAULT_DOUBLE) {

						min_longitude = Math.min(min_longitude, longitude);
						max_longitude = Math.max(max_longitude, longitude);
						min_latitude = Math.min(min_latitude, latitude);
						max_latitude = Math.max(max_latitude, latitude);
					}
				}
			}
		}

		final List<?> tracks = (List<?>) vcContext.get(VC_TRACKS);
		if (tracks != null) {
			final Iterator<?> track_iterator = tracks.iterator();
			while (track_iterator.hasNext()) {
				final GPSTrack track = (GPSTrack) track_iterator.next();
				min_longitude = Math.min(min_longitude, track.getMinLongitude());
				max_longitude = Math.max(max_longitude, track.getMaxLongitude());
				min_latitude = Math.min(min_latitude, track.getMinLatitude());
				max_latitude = Math.max(max_latitude, track.getMaxLatitude());
			}
		}

		vcContext.put("min_latitude", new Double(min_latitude)); //$NON-NLS-1$
		vcContext.put("min_longitude", new Double(min_longitude)); //$NON-NLS-1$
		vcContext.put("max_latitude", new Double(max_latitude)); //$NON-NLS-1$
		vcContext.put("max_longitude", new Double(max_longitude)); //$NON-NLS-1$
	}

	/**
	 * Min/max time, heart, cadence and other values.
	 */
	private void doExport_24_MinMax_Other(final VelocityContext vcContext, final Date creationDate) {

		Date starttime = null;
		Date endtime = null;
		int heartNum = 0;
		long heartSum = 0;
		int cadNum = 0;
		long cadSum = 0;
		short maximumheartrate = 0;
		double totaldistance = 0;

		final List<?> tracks = (List<?>) vcContext.get(VC_TRACKS);

		for (final Object name : tracks) {

			final GPSTrack track = (GPSTrack) name;
			for (final Iterator<?> wpIter = track.getWaypoints().iterator(); wpIter.hasNext();) {

				final GPSTrackpoint wp = (GPSTrackpoint) wpIter.next();

				// starttime, totaltime
				if (wp.getDate() != null) {
					if (starttime == null) {
						starttime = wp.getDate();
					}
					endtime = wp.getDate();
				}

				if (wp instanceof GarminTrackpointAdapter) {

					final GarminTrackpointAdapter gta = (GarminTrackpointAdapter) wp;

					// average heartrate, maximum heartrate
					if (gta.hasValidHeartrate()) {
						heartSum += gta.getHeartrate();
						heartNum++;
						if (gta.getHeartrate() > maximumheartrate) {
							maximumheartrate = gta.getHeartrate();
						}
					}

					// average cadence
					if (gta.hasValidCadence()) {
						cadSum += gta.getCadence();
						cadNum++;
					}

					// totaldistance
					if (gta.hasValidDistance()) {
						totaldistance = gta.getDistance();
					}
				}
			}
		}

		if (starttime != null) {
			vcContext.put("starttime", starttime); //$NON-NLS-1$
		} else {
			vcContext.put("starttime", creationDate); //$NON-NLS-1$
		}

		if ((starttime != null) && (endtime != null)) {
			vcContext.put("totaltime", ((double) endtime.getTime() - starttime.getTime()) / 1000); //$NON-NLS-1$
		} else {
			vcContext.put("totaltime", (double) 0); //$NON-NLS-1$
		}

		vcContext.put("totaldistance", totaldistance); //$NON-NLS-1$

		if (maximumheartrate != 0) {
			vcContext.put("maximumheartrate", maximumheartrate); //$NON-NLS-1$
		}

		if (heartNum != 0) {
			vcContext.put("averageheartrate", heartSum / heartNum); //$NON-NLS-1$
		}

		if (cadNum != 0) {
			vcContext.put("averagecadence", cadSum / cadNum); //$NON-NLS-1$
		}
	}

	private GarminLap doExport_50_Lap(final TourData tourData) {

		final GarminLap lap = new GarminLap();

		/*
		 * Calories
		 */
		lap.setCalories(tourData.getCalories());

		/*
		 * Description
		 */
		if (_exportState_TCX_IsExportDescription) {
			final String notes = tourData.getTourDescription();
			if ((notes != null) && (notes.length() > 0)) {
				lap.setNotes(notes);
			}
		}

		return lap;
	}

	private void doExport_52_Laps(final TourData tourData, final GarminLap tourLap) {

		/*
		 * Calories
		 */
		int calories = tourLap.getCalories();
		calories += tourData.getCalories();
		tourLap.setCalories(calories);

		/*
		 * Description
		 */
		if (_exportState_TCX_IsExportDescription) {

			final String notes = tourData.getTourDescription();

			if ((notes != null) && (notes.length() > 0)) {

				final String lapNotes = tourLap.getNotes();

				if (lapNotes == null) {
					tourLap.setNotes(notes);
				} else {
					tourLap.setNotes(lapNotes + "\n" + notes); //$NON-NLS-1$
				}
			}
		}

	}

	/**
	 * @param tourData
	 * @param trackDateTime
	 * @return Returns a track or <code>null</code> when tour data cannot be exported.
	 */
	private GarminTrack doExport_60_TrackPoints(final TourData tourData, final DateTime trackDateTime) {

		final int[] timeSerie = tourData.timeSerie;

		// check if all dataseries are available
		if ((timeSerie == null) /* || (latitudeSerie == null) || (longitudeSerie == null) */) {
			return null;
		}

		final float[] altitudeSerie = tourData.altitudeSerie;
		final float[] cadenceSerie = tourData.cadenceSerie;
		final float[] distanceSerie = tourData.distanceSerie;
		final long[] gearSerie = tourData.gearSerie;
		final double[] latitudeSerie = tourData.latitudeSerie;
		final double[] longitudeSerie = tourData.longitudeSerie;
		final float[] pulseSerie = tourData.pulseSerie;
		final float[] temperatureSerie = tourData.temperatureSerie;

		final boolean isAltitude = (altitudeSerie != null) && (altitudeSerie.length > 0);
		final boolean isCadence = (cadenceSerie != null) && (cadenceSerie.length > 0);
		final boolean isDistance = (distanceSerie != null) && (distanceSerie.length > 0);
		final boolean isGear = (gearSerie != null) && (gearSerie.length > 0);
		final boolean isPulse = (pulseSerie != null) && (pulseSerie.length > 0);
		final boolean isTemperature = (temperatureSerie != null) && (temperatureSerie.length > 0);

		int prevTime = -1;
		DateTime lastTrackDateTime = null;

		// default is to use all trackpoints
		int startIndex = 0;
		int endIndex = timeSerie.length - 1;

		// adjust start/end when a part is exported
		if (_exportState_IsRangeExport) {
			startIndex = _tourStartIndex;
			endIndex = _tourEndIndex;
		}

		final GarminTrack track = new GarminTrack();

		/*
		 * Track title/description
		 */
		if (_exportState_TCX_IsExportDescription) {

			final String tourTitle = tourData.getTourTitle();
			if (tourTitle.length() > 0) {
				track.setIdentification(tourTitle);
			}

			final String tourDescription = tourData.getTourDescription();
			if (tourDescription.length() > 0) {
				track.setComment(tourDescription);
			}
		}

		/*
		 * loop: trackpoints
		 */
		for (int serieIndex = startIndex; serieIndex <= endIndex; serieIndex++) {

			final GarminTrackpointD304 tp304 = new GarminTrackpointD304();
			final GarminTrackpointAdapterExtended tpExt = new GarminTrackpointAdapterExtended(tp304);

			// mark as a new track to create the <trkseg>...</trkseg> tags
			if (serieIndex == startIndex) {
				tpExt.setNewTrack(true);
			}

			if (isAltitude) {
				tpExt.setAltitude(altitudeSerie[serieIndex]);
			}

			// I don't know if this is according to the rules to have a gpx/tcx without lat/lon
			if (latitudeSerie != null && longitudeSerie != null) {
				tpExt.setLatitude(latitudeSerie[serieIndex]);
				tpExt.setLongitude(longitudeSerie[serieIndex]);
			}

			float distance = 0;
			if (isDistance) {

				if (_exportState_isAbsoluteDistance) {

					distance = distanceSerie[serieIndex];

				} else if (distanceSerie != null) {

					// skip first distance difference
					if (serieIndex > startIndex) {
						distance = distanceSerie[serieIndex] - distanceSerie[serieIndex - 1];
					}
				}
			}

			int relativeTime;
			if (_exportState_IsCamouflageSpeed && isDistance) {

				// camouflage speed

				relativeTime = (int) (distance / _exportState_CamouflageSpeed);

			} else {

				// keep recorded speed

				relativeTime = timeSerie[serieIndex];
			}

			if (isDistance) {
				tpExt.setDistance(distance + _mergedDistance);
			}

			if (isCadence) {
				tp304.setCadence((short) cadenceSerie[serieIndex]);
			}

			if (isPulse) {
				tp304.setHeartrate((short) pulseSerie[serieIndex]);
			}

			if (isTemperature) {
				tpExt.setTemperature(temperatureSerie[serieIndex]);
			}

			if (isGear) {
				tpExt.setGear(gearSerie[serieIndex]);
			}

			// ignore trackpoints which have the same time
			if (relativeTime != prevTime) {

				lastTrackDateTime = trackDateTime.plusSeconds(relativeTime);
				tpExt.setDate(lastTrackDateTime.toDate());

				track.addWaypoint(tpExt);
			}

			prevTime = relativeTime;
		}

		/*
		 * Keep values for the next merged tour
		 */
		if (isDistance && _exportState_isAbsoluteDistance) {

			final float distanceDiff = distanceSerie[endIndex] - distanceSerie[startIndex];
			_mergedDistance += distanceDiff;
		}

		_mergedTime = lastTrackDateTime;

		return track;
	}

	private void doExport_70_WayPoints(	final ArrayList<TourWayPoint> exportedWayPoints,
										final ArrayList<TourMarker> exportedTourMarkers,
										final TourData tourData,
										final DateTime tourStartTime) {

		// get markers when this option is checked
		if (_exportState_GPX_IsExportMarkers == false) {
			return;
		}

		final int[] timeSerie = tourData.timeSerie;
		final float[] altitudeSerie = tourData.altitudeSerie;
		final double[] latitudeSerie = tourData.latitudeSerie;
		final double[] longitudeSerie = tourData.longitudeSerie;
		final float[] distanceSerie = tourData.distanceSerie;

		final Set<TourMarker> tourMarkers = tourData.getTourMarkers();
		final Set<TourWayPoint> tourWayPoints = tourData.getTourWayPoints();

		// ensure required dataseries are available
		if (timeSerie == null || latitudeSerie == null || longitudeSerie == null) {
			return;
		}

		final boolean isAltitude = altitudeSerie != null;
		final boolean isDistance = (distanceSerie != null) && (distanceSerie.length > 0);

		// default is to use all trackpoints
		int startIndex = 0;
		int endIndex = timeSerie.length - 1;
		boolean isRange = false;

		// adjust start/end when a part is exported
		if (_exportState_IsRangeExport) {
			startIndex = _tourStartIndex;
			endIndex = _tourEndIndex;
			isRange = true;
		}

		/*
		 * Create exported tour marker
		 */
		for (final TourMarker tourMarker : tourMarkers) {

			final int serieIndex = tourMarker.getSerieIndex();

			// skip markers when they are not in the defined range
			if (isRange) {
				if ((serieIndex < startIndex) || (serieIndex > endIndex)) {
					continue;
				}
			}

			/*
			 * get distance
			 */
			float distance = 0;
			if (isDistance) {

				if (_exportState_isAbsoluteDistance) {

					distance = distanceSerie[serieIndex];

				} else if (distanceSerie != null) {

					// skip first distance difference
					if (serieIndex > startIndex) {
						distance = distanceSerie[serieIndex] - distanceSerie[serieIndex - 1];
					}
				}
			}

			/*
			 * get time
			 */
			int relativeTime;
			if (_exportState_IsCamouflageSpeed && isDistance) {

				// camouflage speed

				relativeTime = (int) (distance / _exportState_CamouflageSpeed);

			} else {

				// keep recorded speed

				relativeTime = timeSerie[serieIndex];
			}

			final long markerTime = tourStartTime.getMillis() + relativeTime * 1000;

			/*
			 * Setup exported tour marker
			 */
			final TourMarker exportedTourMarker = tourMarker.clone();

			exportedTourMarker.setTime(relativeTime, markerTime);
			exportedTourMarker.setLatitude(latitudeSerie[serieIndex]);
			exportedTourMarker.setLongitude(longitudeSerie[serieIndex]);

			if (isAltitude) {
				exportedTourMarker.setAltitude(altitudeSerie[serieIndex]);
			}

			if (isDistance) {
				exportedTourMarker.setDistance(distance);
			}

			exportedTourMarkers.add(exportedTourMarker);
		}

		for (final TourWayPoint twp : tourWayPoints) {

			final TourWayPoint wayPoint = new TourWayPoint();

			wayPoint.setTime(twp.getTime());
			wayPoint.setLatitude(twp.getLatitude());
			wayPoint.setLongitude(twp.getLongitude());

			wayPoint.setName(twp.getName());

			// <desc>...</desc>
			final String comment = twp.getComment();
			final String description = twp.getDescription();
			final String descText = description != null ? description : comment;
			if (descText != null) {
				wayPoint.setComment(descText);
			}

			wayPoint.setAltitude(twp.getAltitude());

			wayPoint.setUrlAddress(twp.getUrlAddress());
			wayPoint.setUrlText(twp.getUrlText());
//
//			// <sym>...</sym>
//			wayPoint.setSymbolName(twp.getSymbol());

			exportedWayPoints.add(wayPoint);
		}
	}

	private void enableExportButton(final boolean isEnabled) {
		final Button okButton = getButton(IDialogConstants.OK_ID);
		if (okButton != null) {
			okButton.setEnabled(isEnabled);
		}
	}

	private void enableFields() {

		final boolean isCamouflageSpeed = _chkCamouflageSpeed.getSelection();
		final boolean isSingleTour = _isSetup_MultipleTours == false;
		boolean isMergeIntoOneTour = false;

		if (_isSetup_MultipleTours) {

			isMergeIntoOneTour = _chkMergeAllTours.getSelection();
			_chkMergeAllTours.setEnabled(_isSetup_MultipleTours);
		}

		_isExport_MultipleToursWithMultipleFiles = _isSetup_MultipleTours && isMergeIntoOneTour == false;

		if (_isSetup_GPX) {

			final boolean isNoneGPX = isSingleTour || _isExport_MultipleToursWithMultipleFiles;
			_chkGPX_NoneGPXFields.setEnabled(isNoneGPX);
			if (!isNoneGPX) {
				// deselect when not checked
				_chkGPX_NoneGPXFields.setSelection(false);
			}

		} else if (_isSetup_TCX) {

			final boolean isCourse = _rdoTCX_Courses.getSelection();
			final boolean isFromField = _rdoTCX_NameFromField.getSelection();

			_lblTcxNameFrom.setEnabled(isCourse);
			_rdoTCX_NameFromTour.setEnabled(isCourse);
			_rdoTCX_NameFromField.setEnabled(isCourse);

			_lblTcxCourseName.setEnabled(isCourse && isFromField);
			_comboTcxCourseName.setEnabled(isCourse && isFromField);
		}

		_comboFile.setEnabled(isSingleTour || isMergeIntoOneTour);
		_btnSelectFile.setEnabled(isSingleTour || isMergeIntoOneTour);

		_spinnerCamouflageSpeed.setEnabled(isCamouflageSpeed);
		_lblCoumouflageSpeedUnit.setEnabled(isCamouflageSpeed);

		setFileName();
	}

	private String getCourseName() {
		return _comboTcxCourseName.getText().trim();
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		// keep window size and position
		return _state;
	}

	private String getExportFileName() {
		return _comboFile.getText().trim();
	}

	private String getExportPathName() {
		return _comboPath.getText().trim();
	}

	private String[] getUniqueItems(final String[] pathItems, final String currentItem) {

		final ArrayList<String> pathList = new ArrayList<String>();

		pathList.add(currentItem);

		for (final String pathItem : pathItems) {

			// ignore duplicate entries
			if (currentItem.equals(pathItem) == false) {
				pathList.add(pathItem);
			}

			if (pathList.size() >= COMBO_HISTORY_LENGTH) {
				break;
			}
		}

		return pathList.toArray(new String[pathList.size()]);
	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);
	}

	@Override
	protected void okPressed() {

		net.tourbook.ui.UI.disableAllControls(_inputContainer);

		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			@Override
			public void run() {
				try {
					doExport();
				} catch (final IOException e) {
					StatusUtil.log(e);
				}
			}
		});

		super.okPressed();
	}

	private void onSelectBrowseDirectory() {

		final DirectoryDialog dialog = new DirectoryDialog(_dlgContainer.getShell(), SWT.SAVE);
		dialog.setText(Messages.dialog_export_dir_dialog_text);
		dialog.setMessage(Messages.dialog_export_dir_dialog_message);

		dialog.setFilterPath(getExportPathName());

		final String selectedDirectoryName = dialog.open();

		if (selectedDirectoryName != null) {
			setErrorMessage(null);
			_comboPath.setText(selectedDirectoryName);
		}
	}

	private void onSelectBrowseFile() {

		final String fileExtension = _exportExtensionPoint.getFileExtension();

		final FileDialog dialog = new FileDialog(_dlgContainer.getShell(), SWT.SAVE);
		dialog.setText(Messages.dialog_export_file_dialog_text);

		dialog.setFilterPath(getExportPathName());
		dialog.setFilterExtensions(new String[] { fileExtension });
		dialog.setFileName("*." + fileExtension);//$NON-NLS-1$

		final String selectedFilePath = dialog.open();

		if (selectedFilePath != null) {
			setErrorMessage(null);
			_comboFile.setText(new Path(selectedFilePath).toFile().getName());
		}
	}

	private void restoreState() {

		if (_isSetup_GPX) {

			final boolean isAbsoluteDistance = Util.getStateBoolean(_state, STATE_GPX_IS_ABSOLUTE_DISTANCE, true);

			_chkGPX_Description.setSelection(_state.getBoolean(STATE_GPX_IS_EXPORT_DESCRITION));
			_chkGPX_NoneGPXFields.setSelection(_state.getBoolean(STATE_GPX_IS_EXPORT_TOUR_DATA));
			_chkGPX_Markers.setSelection(_state.getBoolean(STATE_GPX_IS_EXPORT_MARKERS));

			_rdoGPX_DistanceAbsolute.setSelection(isAbsoluteDistance);
			_rdoGPX_DistanceRelative.setSelection(!isAbsoluteDistance);

		} else if (_isSetup_TCX) {

			final boolean isCourses = Util.getStateBoolean(_state, STATE_TCX_IS_COURSES, true);
			final boolean isFromTour = Util.getStateBoolean(_state, STATE_TCX_IS_NAME_FROM_TOUR, true);

			_chkTCX_Description.setSelection(_state.getBoolean(STATE_TCX_IS_EXPORT_DESCRITION));

			_rdoTCX_Courses.setSelection(isCourses);
			_rdoTCX_Activities.setSelection(!isCourses);

			_rdoTCX_NameFromTour.setSelection(isFromTour);
			_rdoTCX_NameFromField.setSelection(!isFromTour);

			net.tourbook.ui.UI.restoreCombo(_comboTcxCourseName, _state.getArray(STATE_TCX_COURSE_NAME));

			updateUI_CourseName();
		}

		// merge all tours
		if (_isSetup_MultipleTours) {
			_chkMergeAllTours.setSelection(_state.getBoolean(STATE_IS_MERGE_ALL_TOURS));
		}

		// export tour part
		if (_isSetup_TourRange) {
			_chkExportTourRange.setSelection(_state.getBoolean(STATE_IS_EXPORT_TOUR_RANGE));
		}

		// camouflage speed
		_chkCamouflageSpeed.setSelection(_state.getBoolean(STATE_IS_CAMOUFLAGE_SPEED));
		_spinnerCamouflageSpeed.setSelection(_state.getInt(STATE_CAMOUFLAGE_SPEED));

		// export file/path
		net.tourbook.ui.UI.restoreCombo(_comboFile, _state.getArray(STATE_EXPORT_FILE_NAME));
		net.tourbook.ui.UI.restoreCombo(_comboPath, _state.getArray(STATE_EXPORT_PATH_NAME));
		_chkOverwriteFiles.setSelection(_state.getBoolean(STATE_IS_OVERWRITE_FILES));
	}

	private void saveState() {

		if (_isSetup_GPX) {

			_state.put(STATE_GPX_IS_EXPORT_DESCRITION, _chkGPX_Description.getSelection());
			_state.put(STATE_GPX_IS_ABSOLUTE_DISTANCE, _rdoGPX_DistanceAbsolute.getSelection());
			_state.put(STATE_GPX_IS_EXPORT_MARKERS, _chkGPX_Markers.getSelection());
			_state.put(STATE_GPX_IS_EXPORT_TOUR_DATA, _chkGPX_NoneGPXFields.getSelection());

		} else if (_isSetup_TCX) {

			_state.put(STATE_TCX_IS_COURSES, _rdoTCX_Courses.getSelection());
			_state.put(STATE_TCX_IS_EXPORT_DESCRITION, _chkTCX_Description.getSelection());
			_state.put(STATE_TCX_IS_NAME_FROM_TOUR, _rdoTCX_NameFromTour.getSelection());
			_state.put(STATE_TCX_COURSE_NAME, getUniqueItems(_comboTcxCourseName.getItems(), getCourseName()));
		}

		// merge all tours
		if (_isSetup_MultipleTours) {
			_state.put(STATE_IS_MERGE_ALL_TOURS, _chkMergeAllTours.getSelection());
		}

		// export tour part
		if (_isSetup_TourRange) {
			_state.put(STATE_IS_EXPORT_TOUR_RANGE, _chkExportTourRange.getSelection());
		}

		// camouflage speed
		_state.put(STATE_IS_CAMOUFLAGE_SPEED, _chkCamouflageSpeed.getSelection());
		_state.put(STATE_CAMOUFLAGE_SPEED, _spinnerCamouflageSpeed.getSelection());

		// export file/path
		if (validateFilePath()) {
			_state.put(STATE_EXPORT_PATH_NAME, getUniqueItems(_comboPath.getItems(), getExportPathName()));
			_state.put(STATE_EXPORT_FILE_NAME, getUniqueItems(_comboFile.getItems(), getExportFileName()));
		}
		_state.put(STATE_IS_OVERWRITE_FILES, _chkOverwriteFiles.getSelection());
	}

	private void setError(final String message) {
		setErrorMessage(message);
		enableExportButton(false);
	}

	/**
	 * Set filename with the first tour date/time, when tour is merged "<#default>" is displayed
	 */
	private void setFileName() {

		// search for the first tour
		TourData minTourData = null;
		final long minTourMillis = 0;

		for (final TourData tourData : _tourDataList) {
			final DateTime checkingTourDate = TourManager.getTourDateTime(tourData);

			if (minTourData == null) {
				minTourData = tourData;
			} else {

				final long tourMillis = checkingTourDate.getMillis();
				if (tourMillis < minTourMillis) {
					minTourData = tourData;
				}
			}
		}

		if (_isExport_MultipleToursWithMultipleFiles) {

			// use default file name for each exported tour

			_comboFile.setText(Messages.dialog_export_label_DefaultFileName);

		} else if (_isSetup_TourRange) {

			// display the start date/time

			final DateTime dtTour = minTourData.getTourStartTime();

			// adjust start time
			final int startTime = minTourData.timeSerie[_tourStartIndex];
			final DateTime tourTime = dtTour.plusSeconds(startTime);

			_comboFile.setText(net.tourbook.ui.UI.format_yyyymmdd_hhmmss(
					tourTime.getYear(),
					tourTime.getMonthOfYear(),
					tourTime.getDayOfMonth(),
					tourTime.getHourOfDay(),
					tourTime.getMinuteOfHour(),
					tourTime.getSecondOfMinute()));
		} else {

			// display the tour date/time

			_comboFile.setText(net.tourbook.ui.UI.format_yyyymmdd_hhmmss(minTourData));
		}
	}

	private void updateUI_CourseName() {

		if (_isSetup_TCX == false) {
			return;
		}

		if (_rdoTCX_NameFromTour.getSelection()) {

			/*
			 * set course name from tour
			 */

			String courseName = UI.EMPTY_STRING;

			for (final TourData tourData : _tourDataList) {
				final String tourTitle = tourData.getTourTitle().trim();
				if (tourTitle.length() > 0) {
					courseName = tourTitle;
					break;
				}
			}
			_comboTcxCourseName.setText(courseName);
		}
	}

	private void validateFields() {

		if (_isInUIInit) {
			return;
		}

		/*
		 * validate fields
		 */

		if (_isSetup_TCX) {
			if (_rdoTCX_Courses.getSelection() && getCourseName().length() == 0) {
				setError(Messages.Dialog_Export_Error_CourseNameIsInvalid);
				_comboTcxCourseName.setFocus();
				return;
			}
		}

		if (validateFilePath() == false) {
			return;
		}

		setErrorMessage(null);
		enableExportButton(true);
	}

	private boolean validateFilePath() {

		// check path
		IPath filePath = new Path(getExportPathName());
		if (new File(filePath.toOSString()).exists() == false) {

			// invalid path
			setError(NLS.bind(Messages.dialog_export_msg_pathIsNotAvailable, filePath.toOSString()));
			return false;
		}

		boolean returnValue = false;

		if (_isExport_MultipleToursWithMultipleFiles) {

			// only the path is checked, the file name is created automatically for each exported tour

			setMessage(_dlgDefaultMessage);

			// build file path with extension
			filePath = filePath
					.addTrailingSeparator()
					.append(Messages.dialog_export_label_DefaultFileName)
					.addFileExtension(_exportExtensionPoint.getFileExtension());

			returnValue = true;

		} else {

			String fileName = getExportFileName();

			// remove extentions
			final int extPos = fileName.indexOf('.');
			if (extPos != -1) {
				fileName = fileName.substring(0, extPos);
			}

			// build file path with extension
			filePath = filePath
					.addTrailingSeparator()
					.append(fileName)
					.addFileExtension(_exportExtensionPoint.getFileExtension());

			final File newFile = new File(filePath.toOSString());

			if ((fileName.length() == 0) || newFile.isDirectory()) {

				// invalid filename

				setError(Messages.dialog_export_msg_fileNameIsInvalid);

			} else if (newFile.exists()) {

				// file already exists

				setMessage(
						NLS.bind(Messages.dialog_export_msg_fileAlreadyExists, filePath.toOSString()),
						IMessageProvider.WARNING);
				returnValue = true;

			} else {

				setMessage(_dlgDefaultMessage);

				try {
					final boolean isFileCreated = newFile.createNewFile();

					// name is correct

					if (isFileCreated) {
						// delete file because the file is created for checking validity
						newFile.delete();
					}
					returnValue = true;

				} catch (final IOException ioe) {
					setError(Messages.dialog_export_msg_fileNameIsInvalid);
				}

			}
		}

		_txtFilePath.setText(filePath.toOSString());

		return returnValue;
	}
}
