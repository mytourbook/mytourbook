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
import net.tourbook.ext.velocity.VelocityService;
import net.tourbook.extension.export.ExportTourExtension;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.FileCollisionBehavior;
import net.tourbook.ui.ImageComboLabel;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.dinopolis.gpstool.gpsinput.GPSRoute;
import org.dinopolis.gpstool.gpsinput.GPSTrack;
import org.dinopolis.gpstool.gpsinput.GPSTrackpoint;
import org.dinopolis.gpstool.gpsinput.garmin.GarminTrack;
import org.dinopolis.gpstool.gpsinput.garmin.GarminTrackpointAdapter;
import org.dinopolis.gpstool.gpsinput.garmin.GarminTrackpointD304;
import org.dinopolis.util.text.OneArgumentMessageFormat;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.ProgressIndicator;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
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
import org.osgi.framework.Version;

public class DialogExportTour extends TitleAreaDialog {

	private static final String						EXPORT_ID_GPX					= "net.tourbook.export.gpx";					//$NON-NLS-1$
	private static final String						EXPORT_ID_TCX					= "net.tourbook.export.tcx";					//$NON-NLS-1$

	private static final String						STATE_GPX_IS_ABSOLUTE_DISTANCE	= "STATE_GPX_IS_ABSOLUTE_DISTANCE";			//$NON-NLS-1$
	private static final String						STATE_TCX_IS_COURSES			= "tcxIsCourses";								//$NON-NLS-1$
	private static final String						STATE_TCX_IS_NAME_FROM_TOUR		= "tcxIsNameFromTour";							//$NON-NLS-1$
	private static final String						STATE_TCX_COURSE_NAME			= "tcxCourseName";								//$NON-NLS-1$

	private static final String						STATE_IS_MERGE_ALL_TOURS		= "isMergeAllTours";							//$NON-NLS-1$
	private static final String						STATE_IS_EXPORT_TOUR_RANGE		= "isExportTourRange";							//$NON-NLS-1$
	private static final String						STATE_IS_EXPORT_MARKERS			= "isExportMarkers";							//$NON-NLS-1$
	private static final String						STATE_IS_EXPORT_NOTES			= "isExportNotes";								//$NON-NLS-1$

	private static final String						STATE_IS_CAMOUFLAGE_SPEED		= "isCamouflageSpeed";							//$NON-NLS-1$
	private static final String						STATE_CAMOUFLAGE_SPEED			= "camouflageSpeedValue";						//$NON-NLS-1$

	private static final String						STATE_EXPORT_PATH_NAME			= "exportPathName";							//$NON-NLS-1$
	private static final String						STATE_EXPORT_FILE_NAME			= "exportFileName";							//$NON-NLS-1$
	private static final String						STATE_IS_OVERWRITE_FILES		= "isOverwriteFiles";							//$NON-NLS-1$

	/*
	 * Velocity context values
	 */
	private static final String						VC_HAS_ROUTES					= "hasRoutes";									//$NON-NLS-1$
	private static final String						VC_HAS_TRACKS					= "hasTracks";									//$NON-NLS-1$
	private static final String						VC_HAS_WAY_POINTS				= "hasWayPoints";								//$NON-NLS-1$
	private static final String						VC_LAP							= "lap";										//$NON-NLS-1$
	private static final String						VC_TRACKS						= "tracks";									//$NON-NLS-1$
	private static final String						VC_WAYPOINTS					= "waypoints";									//$NON-NLS-1$

	private static final String						ZERO							= "0";											//$NON-NLS-1$

	private static final int						VERTICAL_SECTION_MARGIN			= 10;
	private static final int						SIZING_TEXT_FIELD_WIDTH			= 250;
	private static final int						COMBO_HISTORY_LENGTH			= 20;
	private static final DecimalFormat				_intFormatter					= (DecimalFormat) NumberFormat
																							.getInstance(Locale.US);
	private static final DecimalFormat				_double1Formatter				= (DecimalFormat) NumberFormat
																							.getInstance(Locale.US);
	private static final DecimalFormat				_double2Formatter				= (DecimalFormat) NumberFormat
																							.getInstance(Locale.US);
	private static final DecimalFormat				_double6Formatter				= (DecimalFormat) NumberFormat
																							.getInstance(Locale.US);
	private static final OneArgumentMessageFormat	_stringFormatter				= new OneArgumentMessageFormat(
																							"{0}", Locale.US);						//$NON-NLS-1$
	private static final SimpleDateFormat			_dateFormat						= new SimpleDateFormat();
	private static final DateFormat					_timeFormatter					= DateFormat
																							.getTimeInstance(DateFormat.MEDIUM);
	private static final NumberFormat				_numberFormatter				= NumberFormat.getNumberInstance();

	private static String							_dlgDefaultMessage;

	static {

		_intFormatter.applyPattern("000000"); //$NON-NLS-1$
		_double1Formatter.applyPattern("0.0"); //$NON-NLS-1$
		_double2Formatter.applyPattern("0.00"); //$NON-NLS-1$
		_double6Formatter.applyPattern("0.0000000"); //$NON-NLS-1$
		_dateFormat.setTimeZone(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$
	}

	private final String							_formatTemplate;

	private final IDialogSettings					_state							= TourbookPlugin
																							.getState("DialogExportTour");			//$NON-NLS-1$

	private final ExportTourExtension				_exportExtensionPoint;

	private final ArrayList<TourData>				_tourDataList;
	private final int								_tourStartIndex;
	private final int								_tourEndIndex;

	private boolean									_isGPX;
	private boolean									_isTCX;
	private boolean									_isInit;

	private DateTime								_mergedStartTime;
	private ProgressIndicator						_progressIndicator;

	/**
	 * Is <code>true</code> when multiple tours are selected and not merged into 1 file
	 */
	private boolean									_isMultipleTourAndMultipleFile;

	private Point									_shellDefaultSize;

	private float									_exportState_CamouflageSpeed;
	private FileCollisionBehavior					_exportState_FileCollisionBehaviour;
	private boolean									_exportState_IsAbsoluteDistance;
	private boolean									_exportState_IsCamouflageSpeed;
	private boolean									_exportState_IsExportDescription;
	private boolean									_exportState_IsExportMarkers;
	private boolean									_exportState_IsOverwriteFiles;

	private PixelConverter							_pc;

	/*
	 * UI controls
	 */
	private Button									_btnSelectDirectory;
	private Button									_btnSelectFile;
	private Button									_chkCamouflageSpeed;
	private Button									_chkExportTourRange;
	private Button									_chkExportMarkers;
	private Button									_chkExportNotes;
	private Button									_chkMergeAllTours;
	private Button									_chkOverwriteFiles;
	private Button									_rdoGpxDistanceAbsolute;
	private Button									_rdoGpxDistanceRelative;
	private Button									_rdoTcxActivities;
	private Button									_rdoTcxCourses;
	private Button									_rdoTcxNameFromField;
	private Button									_rdoTcxNameFromTour;

	private Combo									_comboFile;
	private Combo									_comboPath;
	private Combo									_comboTcxCourseName;

	private Composite								_dlgContainer;
	private Composite								_inputContainer;

	private ImageComboLabel							_lblExportedFilePath;

	private Label									_lblCoumouflageSpeedUnit;
	private Label									_lblTcxCourseName;
	private Label									_lblTcxNameFrom;

	private Spinner									_spinnerCamouflageSpeed;

	private Text									_txtFilePath;

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

		_isTCX = _exportExtensionPoint.getExportId().equalsIgnoreCase(EXPORT_ID_TCX);
		_isGPX = _exportExtensionPoint.getExportId().equalsIgnoreCase(EXPORT_ID_GPX);

		_dlgDefaultMessage = NLS.bind(Messages.dialog_export_dialog_message, _exportExtensionPoint.getVisibleName());

		// initialize velocity
		VelocityService.init();
	}

	/**
	 * @return Returns <code>true</code> when a part of a tour can be exported
	 */
	private boolean canExportTourPart() {
		return (_tourDataList.size() == 1) && (_tourStartIndex >= 0) && (_tourEndIndex > 0);
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

		_isInit = true;
		{
			restoreState();
		}
		_isInit = false;

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
			if (_isGPX) {
				createUI_20_GPXOptions(_inputContainer);
			} else if (_isTCX) {
				createUI_30_TCXOptions(_inputContainer);
			}

			createUI_40_Options(_inputContainer);
			createUI_50_ExportFile(_inputContainer);
		}
		createUI30Progress(parent);
	}

	private void createUI_20_GPXOptions(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Dialog_Export_Group_GPX_Options);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(group);
		{
			createUI_22_GPXOption_Distance(group);
		}
	}

	private void createUI_22_GPXOption_Distance(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
//		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
		{
			// label
			final Label label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(label);
			label.setText(Messages.Dialog_Export_Label_GPX_DistanceValues);

			// radio
			{
				_rdoGpxDistanceAbsolute = new Button(container, SWT.RADIO);
				_rdoGpxDistanceAbsolute.setText(Messages.Dialog_Export_Radio_GPX_DistanceAbsolute);
				_rdoGpxDistanceAbsolute.setToolTipText(Messages.Dialog_Export_Radio_GPX_DistanceAbsolute_Tooltip);

				_rdoGpxDistanceRelative = new Button(container, SWT.RADIO);
				_rdoGpxDistanceRelative.setText(Messages.Dialog_Export_Radio_GPX_DistanceRelative);
				_rdoGpxDistanceRelative.setToolTipText(Messages.Dialog_Export_Radio_GPX_DistanceRelative_Tooltip);
			}
		}
	}

	private void createUI_30_TCXOptions(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Dialog_Export_Group_TCX_Options);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(group);
		{
			createUI_32_TCXOption_ActivitiesCourses(group);
		}
	}

	private void createUI_32_TCXOption_ActivitiesCourses(final Group parent) {

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
				updateUICourseName();
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
					_rdoTcxCourses = new Button(containerActivities, SWT.RADIO);
					GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_rdoTcxCourses);
					_rdoTcxCourses.setText(Messages.Dialog_Export_Radio_TCX_Courses);
					_rdoTcxCourses.setToolTipText(Messages.Dialog_Export_Radio_TCX_Courses_Tooltip);
					_rdoTcxCourses.addSelectionListener(defaultSelectionListener);

					/*
					 * radio: activities
					 */
					_rdoTcxActivities = new Button(containerActivities, SWT.RADIO);
					GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_rdoTcxActivities);
					_rdoTcxActivities.setText(Messages.Dialog_Export_Radio_TCX_Aktivities);
					_rdoTcxActivities.setToolTipText(Messages.Dialog_Export_Radio_TCX_Aktivities_Tooltip);
					_rdoTcxActivities.addSelectionListener(defaultSelectionListener);
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
					_rdoTcxNameFromTour = new Button(containerNameFrom, SWT.RADIO);
					GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_rdoTcxNameFromTour);
					_rdoTcxNameFromTour.setText(Messages.Dialog_Export_Radio_TCX_NameFromTour);
					_rdoTcxNameFromTour.addSelectionListener(nameSelectionListener);

					/*
					 * radio: from text field
					 */
					_rdoTcxNameFromField = new Button(containerNameFrom, SWT.RADIO);
					GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_rdoTcxNameFromField);
					_rdoTcxNameFromField.setText(Messages.Dialog_Export_Radio_TCX_NameFromField);
					_rdoTcxNameFromField.addSelectionListener(nameSelectionListener);
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

	private void createUI_40_Options(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.dialog_export_group_options);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(group);
		{
			createUI_42_Option_CamouflageSpeed(group);
			createUI_44_Option_Misc(group);
			createUI_46_Option_TourRange(group);
		}
	}

	private void createUI_42_Option_CamouflageSpeed(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(container);
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

	private void createUI_44_Option_Misc(final Composite parent) {

		/*
		 * checkbox: export markers
		 */
		{
		_chkExportMarkers = new Button(parent, SWT.CHECK);
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_chkExportMarkers);
		_chkExportMarkers.setText(Messages.dialog_export_chk_exportMarkers);
		_chkExportMarkers.setToolTipText(Messages.dialog_export_chk_exportMarkers_tooltip);
		}

		/*
		 * checkbox: export notes
		 */
		{
		_chkExportNotes = new Button(parent, SWT.CHECK);
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_chkExportNotes);
		_chkExportNotes.setText(Messages.dialog_export_chk_exportNotes);
		_chkExportNotes.setToolTipText(Messages.dialog_export_chk_exportNotes_tooltip);
		}

		/*
		 * checkbox: merge all tours
		 */
		{
		_chkMergeAllTours = new Button(parent, SWT.CHECK);
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_chkMergeAllTours);
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
	}

	private void createUI_46_Option_TourRange(final Composite parent) {

		/*
		 * checkbox: tour range
		 */
		String tourRangeUI = null;

		if ((_tourDataList.size() == 1) && (_tourStartIndex != -1) && (_tourEndIndex != -1)) {

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

					_numberFormatter.setMinimumFractionDigits(3);
					_numberFormatter.setMaximumFractionDigits(3);

					tourRangeUI = NLS.bind(
							Messages.dialog_export_chk_tourRangeWithDistance,
							new Object[] {
									uiStartTime,
									uiEndTime,

									_numberFormatter.format(distanceSerie[_tourStartIndex]
											/ 1000
											/ net.tourbook.ui.UI.UNIT_VALUE_DISTANCE),

									_numberFormatter.format(distanceSerie[_tourEndIndex]
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
		}

		_chkExportTourRange = new Button(parent, SWT.CHECK);
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_chkExportTourRange);

		_chkExportTourRange.setText(tourRangeUI != null ? tourRangeUI : Messages.dialog_export_chk_tourRangeDisabled);

		_chkExportTourRange.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				enableFields();
			}
		});
	}

	private void createUI_50_ExportFile(final Composite parent) {

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

	private void createUI30Progress(final Composite parent) {

		final int selectedTours = _tourDataList.size();

		// hide progress bar when only one tour is exported
		if (selectedTours < 2) {
			return;
		}

		// container
		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).indent(0, VERTICAL_SECTION_MARGIN).applyTo(container);
		GridLayoutFactory.swtDefaults().margins(10, 5).numColumns(1).applyTo(container);
		{
			/*
			 * progress indicator
			 */
			_progressIndicator = new ProgressIndicator(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_progressIndicator);

			/*
			 * label: exported filename
			 */
			_lblExportedFilePath = new ImageComboLabel(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblExportedFilePath);
		}
	}
	private void doExport() throws IOException {

		// disable button's
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		getButton(IDialogConstants.CANCEL_ID).setEnabled(false);

		_exportState_IsOverwriteFiles = _chkOverwriteFiles.getSelection();
		_exportState_IsCamouflageSpeed = _chkCamouflageSpeed.getSelection();
		_exportState_IsExportDescription = _chkExportNotes.getSelection();
		_exportState_IsExportMarkers = _chkExportMarkers.getSelection();

		_exportState_CamouflageSpeed = _spinnerCamouflageSpeed.getSelection();
		_exportState_CamouflageSpeed *= net.tourbook.ui.UI.UNIT_VALUE_DISTANCE / 3.6f;
		_exportState_FileCollisionBehaviour = new FileCollisionBehavior();

		if (_isGPX) {

			_exportState_IsAbsoluteDistance = _rdoGpxDistanceAbsolute.getSelection();

		} else if (_isTCX) {

			// .tcx files do always contain absolute distances
			_exportState_IsAbsoluteDistance = true;
		}

		final String exportFileName = _txtFilePath.getText();

		final ArrayList<TourWayPoint> wayPoints = new ArrayList<TourWayPoint>();

		if (_tourDataList.size() == 1) {

			// export one tour

			final TourData tourData = _tourDataList.get(0);
			final DateTime trackStartTime = TourManager.getTourDateTime(tourData);

			final GarminLap tourLap = doExport_50_Lap(tourData);

			final ArrayList<GarminTrack> tracks = new ArrayList<GarminTrack>();
			final GarminTrack track = doExport_60_TrackPoints(tourData, trackStartTime);
			if (track != null) {
				tracks.add(track);
			}

			doExport_70_WayPoints(wayPoints, tourData, trackStartTime);

			doExport_10_Tour(tracks, wayPoints, tourLap, exportFileName);

		} else {

			/*
			 * export multiple tours
			 */

			if (_chkMergeAllTours.getSelection()) {

				/*
				 * merge all tours into one
				 */

				_mergedStartTime = TourManager.getTourDateTime(_tourDataList.get(0));

				final ArrayList<GarminTrack> tracks = new ArrayList<GarminTrack>();

				final GarminLap tourLap = new GarminLap();

				// create tracklist and lap
				for (final TourData tourData : _tourDataList) {

					doExport_52_Laps(tourData, tourLap);

					DateTime trackStartTime;
					if (_exportState_IsCamouflageSpeed) {
						trackStartTime = _mergedStartTime;
					} else {
						trackStartTime = TourManager.getTourDateTime(tourData);
					}

					final GarminTrack track = doExport_60_TrackPoints(tourData, trackStartTime);
					if (track != null) {
						tracks.add(track);
					}
				}

				doExport_10_Tour(tracks, wayPoints, tourLap, exportFileName);

			} else {

				/*
				 * export each tour separately
				 */

				final String exportPathName = getExportPathName();

				_progressIndicator.beginTask(_tourDataList.size());

				final Job exportJob = new Job("export files") { //$NON-NLS-1$
					@Override
					public IStatus run(final IProgressMonitor monitor) {

						monitor.beginTask(UI.EMPTY_STRING, _tourDataList.size());

						final IPath exportFilePath = new Path(exportPathName).addTrailingSeparator();
						final String fileExtension = _exportExtensionPoint.getFileExtension();

						for (final TourData tourData : _tourDataList) {

							try {

								// create file path name
								final String tourFileName = net.tourbook.ui.UI.format_yyyymmdd_hhmmss(tourData);

								final String exportFilePathName = exportFilePath
										.append(tourFileName)
										.addFileExtension(fileExtension)
										.toOSString();

								/*
								 * update dialog progress monitor
								 */
								Display.getDefault().syncExec(new Runnable() {
									@Override
									public void run() {

										// display exported filepath
										_lblExportedFilePath.setText(NLS.bind(
												Messages.dialog_export_lbl_exportFilePath,
												exportFilePathName));

										// !!! force label update !!!
										_lblExportedFilePath.update();

										_progressIndicator.worked(1);
									}
								});

								final GarminLap tourLap = doExport_50_Lap(tourData);

								// create tracklist
								final ArrayList<GarminTrack> tracks = new ArrayList<GarminTrack>();

								final GarminTrack track = doExport_60_TrackPoints(
										tourData,
										TourManager.getTourDateTime(tourData));

								if (track != null) {
									tracks.add(track);
								}

								doExport_10_Tour(tracks, wayPoints, tourLap, exportFilePathName);

							} catch (final IOException e) {
								StatusUtil.log(e);
							}

							// check if overwrite dialog was canceled
							if (_exportState_FileCollisionBehaviour.value == FileCollisionBehavior.DIALOG_IS_CANCELED) {
								break;
							}
						}

						return Status.OK_STATUS;
					}
				};

				// run export
				exportJob.schedule();

				// wait until export has finished
				try {
					exportJob.join();
				} catch (final InterruptedException e) {
					StatusUtil.log(e);
				}
			}
		}
	}

	private void doExport_10_Tour(	final ArrayList<GarminTrack> tracks,
									final ArrayList<TourWayPoint> wayPoints,
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

		final VelocityContext vcContext = new VelocityContext();

		if (_isTCX) {

			final boolean[] isCourses = { false };
			final String[] courseName = { UI.EMPTY_STRING };

			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {

					isCourses[0] = _rdoTcxCourses.getSelection();
					courseName[0] = _comboTcxCourseName.getText();
				}
			});

			vcContext.put("iscourses", Boolean.valueOf(isCourses[0])); //$NON-NLS-1$
			vcContext.put("coursename", courseName[0]); //$NON-NLS-1$
		}

		vcContext.put(VC_TRACKS, tracks);
		vcContext.put(VC_WAYPOINTS, wayPoints);
		vcContext.put(VC_LAP, lap);

		vcContext.put(VC_HAS_TRACKS, Boolean.valueOf(true));
		vcContext.put(VC_HAS_WAY_POINTS, Boolean.valueOf(wayPoints.size() > 0));
		vcContext.put(VC_HAS_ROUTES, Boolean.valueOf(false));

		vcContext.put("dateformatter", _dateFormat); //$NON-NLS-1$
		vcContext.put("intformatter", _intFormatter); //$NON-NLS-1$
		vcContext.put("double1formatter", _double1Formatter); //$NON-NLS-1$
		vcContext.put("double2formatter", _double2Formatter); //$NON-NLS-1$
		vcContext.put("double6formatter", _double6Formatter); //$NON-NLS-1$
		vcContext.put("stringformatter", _stringFormatter); //$NON-NLS-1$

		doExport_20_TourValues(vcContext);

		final Writer exportWriter = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(exportFile),
				UI.UTF_8));

		final Reader templateReader = new InputStreamReader(this.getClass().getResourceAsStream(_formatTemplate));

		try {
			Velocity.evaluate(vcContext, exportWriter, "MyTourbook", templateReader); //$NON-NLS-1$
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

		final List<?> wayPoints = (List<?>) vcContext.get(VC_WAYPOINTS);
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
		if (_exportState_IsExportDescription) {
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
		if (_exportState_IsExportDescription) {

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
		final double[] latitudeSerie = tourData.latitudeSerie;
		final double[] longitudeSerie = tourData.longitudeSerie;
		final float[] pulseSerie = tourData.pulseSerie;
		final float[] temperatureSerie = tourData.temperatureSerie;

		final boolean isAltitude = (altitudeSerie != null) && (altitudeSerie.length > 0);
		final boolean isCadence = (cadenceSerie != null) && (cadenceSerie.length > 0);
		final boolean isDistance = (distanceSerie != null) && (distanceSerie.length > 0);
		final boolean isPulse = (pulseSerie != null) && (pulseSerie.length > 0);
		final boolean isTemperature = (temperatureSerie != null) && (temperatureSerie.length > 0);

		int prevTime = -1;
		DateTime lastTrackDateTime = null;

		// default is to use all trackpoints
		int startIndex = 0;
		int endIndex = timeSerie.length - 1;

		// adjust start/end when a part is exported
		if (isExportTourPart()) {
			startIndex = _tourStartIndex;
			endIndex = _tourEndIndex;
		}

		final GarminTrack track = new GarminTrack();

		/*
		 * Track name/description
		 */
		final String tourTitle = tourData.getTourTitle();
		if (tourTitle.length() > 0) {
			track.setIdentification(tourTitle);
		}

		final String tourDescription = tourData.getTourDescription();
		if (tourDescription.length() > 0) {
			track.setComment(tourDescription);
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

				if (_exportState_IsAbsoluteDistance) {

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
				tpExt.setDistance(distance);
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

			// ignore trackpoints which have the same time
			if (relativeTime != prevTime) {

				lastTrackDateTime = trackDateTime.plusSeconds(relativeTime);
				tpExt.setDate(lastTrackDateTime.toDate());

				track.addWaypoint(tpExt);
			}

			prevTime = relativeTime;
		}

		// keep last date/time for the next merged tour
		_mergedStartTime = lastTrackDateTime;

		return track;
	}

	private void doExport_70_WayPoints(	final ArrayList<TourWayPoint> wayPoints,
										final TourData tourData,
										final DateTime tourStartTime) {

		// get markers when this option is checked
		if (_exportState_IsExportMarkers == false) {
			return;
		}

		final int[] timeSerie = tourData.timeSerie;
		final float[] altitudeSerie = tourData.altitudeSerie;
		final double[] latitudeSerie = tourData.latitudeSerie;
		final double[] longitudeSerie = tourData.longitudeSerie;
		final float[] distanceSerie = tourData.distanceSerie;

		final Set<TourMarker> tourMarkers = tourData.getTourMarkers();
		final Set<TourWayPoint> tourWayPoints = tourData.getTourWayPoints();

		// check if all dataseries are available
		if ((timeSerie == null) || (latitudeSerie == null) || (longitudeSerie == null) || (tourMarkers == null)) {
			return;
		}

		final boolean isDistance = (distanceSerie != null) && (distanceSerie.length > 0);

		// default is to use all trackpoints
		int startIndex = 0;
		int endIndex = timeSerie.length - 1;
		boolean isRange = false;

		// adjust start/end when a part is exported
		if (isExportTourPart()) {
			startIndex = _tourStartIndex;
			endIndex = _tourEndIndex;
			isRange = true;
		}

		// convert marker into way points
		for (final TourMarker tourMarker : tourMarkers) {

			final int serieIndex = tourMarker.getSerieIndex();

			// skip markers when they are not in the defined range
			if (isRange) {
				if ((serieIndex < startIndex) || (serieIndex > endIndex)) {
					continue;
				}
			}

			float distance = 0;
			if (isDistance) {

				if (_exportState_IsAbsoluteDistance) {

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

			final long wayPointTime = tourStartTime.getMillis() + relativeTime * 1000;

			/*
			 * Setup waypoint
			 */
			final TourWayPoint wayPoint = new TourWayPoint();
			wayPoints.add(wayPoint);

			wayPoint.setTime(wayPointTime);
			wayPoint.setLatitude(latitudeSerie[serieIndex]);
			wayPoint.setLongitude(longitudeSerie[serieIndex]);

			wayPoint.setName(tourMarker.getLabel());
			wayPoint.setComment(tourMarker.getDescription());

			if (altitudeSerie != null) {
				wayPoint.setAltitude(altitudeSerie[serieIndex]);
			}

			wayPoint.setUrlAddress(tourMarker.getUrlAddress());
			wayPoint.setUrlText(tourMarker.getUrlText());
		}

		for (final TourWayPoint twp : tourWayPoints) {

			final TourWayPoint wayPoint = new TourWayPoint();
			wayPoints.add(wayPoint);

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

			if (altitudeSerie != null) {
				wayPoint.setAltitude(twp.getAltitude());
			}

			wayPoint.setUrlAddress(twp.getUrlAddress());
			wayPoint.setUrlText(twp.getUrlText());
//
//			// <sym>...</sym>
//			wayPoint.setSymbolName(twp.getSymbol());
		}
	}

	private void enableExportButton(final boolean isEnabled) {
		final Button okButton = getButton(IDialogConstants.OK_ID);
		if (okButton != null) {
			okButton.setEnabled(isEnabled);
		}
	}

	private void enableFields() {

		if (_isTCX) {

			final boolean isCourse = _rdoTcxCourses.getSelection();
			final boolean isFromField = _rdoTcxNameFromField.getSelection();

			_lblTcxNameFrom.setEnabled(isCourse);
			_rdoTcxNameFromTour.setEnabled(isCourse);
			_rdoTcxNameFromField.setEnabled(isCourse);

			_lblTcxCourseName.setEnabled(isCourse && isFromField);
			_comboTcxCourseName.setEnabled(isCourse && isFromField);
		}

		final boolean isMultipleTours = _tourDataList.size() > 1;
		final boolean isCamouflageSpeed = _chkCamouflageSpeed.getSelection();
		final boolean isMergeTour = _chkMergeAllTours.getSelection();

		_chkMergeAllTours.setEnabled(isMultipleTours);
		// when disabled, uncheck it
		if (_chkMergeAllTours.isEnabled() == false) {
			_chkMergeAllTours.setSelection(false);
		}
		_isMultipleTourAndMultipleFile = _tourDataList.size() > 1 && _chkMergeAllTours.getSelection() == false;

		_comboFile.setEnabled(isMultipleTours == false || isMergeTour);
		_btnSelectFile.setEnabled(isMultipleTours == false || isMergeTour);

		_spinnerCamouflageSpeed.setEnabled(isCamouflageSpeed);
		_lblCoumouflageSpeedUnit.setEnabled(isCamouflageSpeed);

		_chkExportTourRange.setEnabled(canExportTourPart());
		// when disabled, uncheck it
		if (_chkExportTourRange.isEnabled() == false) {
			_chkExportTourRange.setSelection(false);
		}

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

	/**
	 * @return Return <code>true</code> when a part of a tour can be exported
	 */
	private boolean isExportTourPart() {

		final boolean[] result = new boolean[1];

		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				result[0] = _chkExportTourRange.getSelection()
						&& (_tourDataList.size() == 1)
						&& (_tourStartIndex != -1)
						&& (_tourEndIndex != -1);
			}
		});

		return result[0];
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

		if (_isGPX) {

			final boolean isAbsoluteDistance = Util.getStateBoolean(_state, STATE_GPX_IS_ABSOLUTE_DISTANCE, true);

			_rdoGpxDistanceAbsolute.setSelection(isAbsoluteDistance);
			_rdoGpxDistanceRelative.setSelection(!isAbsoluteDistance);
		}

		if (_isTCX) {

			final boolean isCourses = Util.getStateBoolean(_state, STATE_TCX_IS_COURSES, true);
			final boolean isFromTour = Util.getStateBoolean(_state, STATE_TCX_IS_NAME_FROM_TOUR, true);

			_rdoTcxCourses.setSelection(isCourses);
			_rdoTcxActivities.setSelection(!isCourses);

			_rdoTcxNameFromTour.setSelection(isFromTour);
			_rdoTcxNameFromField.setSelection(!isFromTour);

			net.tourbook.ui.UI.restoreCombo(_comboTcxCourseName, _state.getArray(STATE_TCX_COURSE_NAME));

			updateUICourseName();
		}

		_chkMergeAllTours.setSelection(_state.getBoolean(STATE_IS_MERGE_ALL_TOURS));
		_chkExportTourRange.setSelection(_state.getBoolean(STATE_IS_EXPORT_TOUR_RANGE));
		_chkExportMarkers.setSelection(_state.getBoolean(STATE_IS_EXPORT_MARKERS));
		_chkExportNotes.setSelection(_state.getBoolean(STATE_IS_EXPORT_NOTES));

		// camouflage speed
		_chkCamouflageSpeed.setSelection(_state.getBoolean(STATE_IS_CAMOUFLAGE_SPEED));
		_spinnerCamouflageSpeed.setSelection(_state.getInt(STATE_CAMOUFLAGE_SPEED));

		// export file/path
		net.tourbook.ui.UI.restoreCombo(_comboFile, _state.getArray(STATE_EXPORT_FILE_NAME));
		net.tourbook.ui.UI.restoreCombo(_comboPath, _state.getArray(STATE_EXPORT_PATH_NAME));
		_chkOverwriteFiles.setSelection(_state.getBoolean(STATE_IS_OVERWRITE_FILES));
	}

	private void saveState() {

		if (_isGPX) {

			_state.put(STATE_GPX_IS_ABSOLUTE_DISTANCE, _rdoGpxDistanceAbsolute.getSelection());
		}

		if (_isTCX) {

			_state.put(STATE_TCX_IS_COURSES, _rdoTcxCourses.getSelection());
			_state.put(STATE_TCX_IS_NAME_FROM_TOUR, _rdoTcxNameFromTour.getSelection());
			_state.put(STATE_TCX_COURSE_NAME, getUniqueItems(_comboTcxCourseName.getItems(), getCourseName()));
		}

		// export file/path
		if (validateFilePath()) {
			_state.put(STATE_EXPORT_PATH_NAME, getUniqueItems(_comboPath.getItems(), getExportPathName()));
			_state.put(STATE_EXPORT_FILE_NAME, getUniqueItems(_comboFile.getItems(), getExportFileName()));
		}

		// merge all tours
		if (_tourDataList.size() > 1) {
			_state.put(STATE_IS_MERGE_ALL_TOURS, _chkMergeAllTours.getSelection());
		}

		// export tour part
		if (canExportTourPart()) {
			_state.put(STATE_IS_EXPORT_TOUR_RANGE, _chkExportTourRange.getSelection());
		}

		// camouflage speed
		_state.put(STATE_IS_CAMOUFLAGE_SPEED, _chkCamouflageSpeed.getSelection());
		_state.put(STATE_CAMOUFLAGE_SPEED, _spinnerCamouflageSpeed.getSelection());

		_state.put(STATE_IS_OVERWRITE_FILES, _chkOverwriteFiles.getSelection());
		_state.put(STATE_IS_EXPORT_MARKERS, _chkExportMarkers.getSelection());
		_state.put(STATE_IS_EXPORT_NOTES, _chkExportNotes.getSelection());
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

		if (_isMultipleTourAndMultipleFile) {

			// use default file name for each exported tour

			_comboFile.setText(Messages.dialog_export_label_DefaultFileName);

		} else if ((_tourDataList.size() == 1) && (_tourStartIndex != -1) && (_tourEndIndex != -1)) {

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

	private void updateUICourseName() {

		if (_isTCX == false) {
			return;
		}

		if (_rdoTcxNameFromTour.getSelection()) {

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

		if (_isInit) {
			return;
		}

		/*
		 * validate fields
		 */

		if (_isTCX) {
			if (_rdoTcxCourses.getSelection() && getCourseName().length() == 0) {
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

		if (_isMultipleTourAndMultipleFile) {

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
