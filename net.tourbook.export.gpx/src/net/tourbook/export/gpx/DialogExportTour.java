/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
package net.tourbook.export.gpx;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.export.ExportTourExtension;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ImageComboLabel;
import net.tourbook.ui.UI;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.dinopolis.gpstool.gpsinput.GPSRoute;
import org.dinopolis.gpstool.gpsinput.GPSTrack;
import org.dinopolis.gpstool.gpsinput.GPSTrackpoint;
import org.dinopolis.gpstool.gpsinput.GPSWaypoint;
import org.dinopolis.gpstool.gpsinput.garmin.GarminTrack;
import org.dinopolis.gpstool.gpsinput.garmin.GarminTrackpointAdapter;
import org.dinopolis.gpstool.gpsinput.garmin.GarminTrackpointD304;
import org.dinopolis.util.text.OneArgumentMessageFormat;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.ProgressIndicator;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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
import org.eclipse.swt.widgets.Text;
import org.joda.time.DateTime;

public class DialogExportTour extends TitleAreaDialog {

	private static final int				VERTICAL_SECTION_MARGIN		= 10;

	private static final int				SIZING_TEXT_FIELD_WIDTH		= 250;
	private static final int				COMBO_HISTORY_LENGTH		= 20;

	private final IDialogSettings			fState						= TourbookPlugin.getDefault()
																				.getDialogSettingsSection("DialogExportTour");	//$NON-NLS-1$

	private static final String				STATE_IS_MERGE_ALL_TOURS	= "isMergeAllTours";									//$NON-NLS-1$
	private static final String				STATE_IS_CAMOUFLAGE_SPEED	= "isCamouflageSpeed";									//$NON-NLS-1$
	private static final String				STATE_CAMOUFLAGE_SPEED		= "camouflageSpeedValue";								//$NON-NLS-1$
	private static final String				STATE_EXPORT_PATH_NAME		= "exportPathName";									//$NON-NLS-1$
	private static final String				STATE_EXPORT_FILE_NAME		= "exportFileName";									//$NON-NLS-1$
	private static final String				STATE_IS_OVERWRITE_FILES	= "isOverwriteFiles";									//$NON-NLS-1$

	final static DecimalFormat				fIntFormatter				= (DecimalFormat) NumberFormat.getInstance(Locale.US);
	final static DecimalFormat				fDouble2Formatter			= (DecimalFormat) NumberFormat.getInstance(Locale.US);
	final static DecimalFormat				fDouble6Formatter			= (DecimalFormat) NumberFormat.getInstance(Locale.US);
	final static OneArgumentMessageFormat	fStringFormatter			= new OneArgumentMessageFormat("{0}", Locale.US);		//$NON-NLS-1$
	final static SimpleDateFormat			fDateFormat					= new SimpleDateFormat();

	private static String					fDlgDefaultMessage;

	static {
		fIntFormatter.applyPattern("000000"); //$NON-NLS-1$
		fDouble2Formatter.applyPattern("0.00"); //$NON-NLS-1$
		fDouble6Formatter.applyPattern("0.0000000"); //$NON-NLS-1$
		fDateFormat.setTimeZone(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$
	}

	private ExportTourExtension				fExportExtensionPoint;
	private ArrayList<TourData>				fTourDataList;

	private Composite						fDlgContainer;

	private Button							fChkCamouflageSpeed;
	private Text							fTxtCamouflageSpeed;
	private Label							fLblCoumouflageSpeedUnit;

	private Composite						fInputContainer;
	private Button							fChkMergeAllTours;

	private Combo							fComboFile;
	private Combo							fComboPath;
	private Button							fBtnSelectFile;
	private Button							fBtnSelectDirectory;
	private Text							fTxtFilePath;
	private Button							fChkOverwriteFiles;

	private ProgressIndicator				fProgressIndicator;
	private ImageComboLabel					fLblExportedFilePath;

	private boolean							fInitializeControls;

	private DateTime						fTrackStartDateTime;

	public DialogExportTour(final Shell parentShell,
							final ExportTourExtension exportExtensionPoint,
							final ArrayList<TourData> tourDataList) {

		super(parentShell);

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE);

		fTourDataList = tourDataList;
		fExportExtensionPoint = exportExtensionPoint;

		fDlgDefaultMessage = NLS.bind(Messages.dialog_export_dialog_message, fExportExtensionPoint.getVisibleName());

	}

	/**
	 * Adds some important values to the velocity context (e.g. date, ...).
	 * 
	 * @param context
	 *            the velocity context holding all the data
	 */
	private void addValuesToContext(final VelocityContext context) {

		context.put("dateformatter", fDateFormat); //$NON-NLS-1$
		context.put("intformatter", fIntFormatter); //$NON-NLS-1$
		context.put("double6formatter", fDouble6Formatter); //$NON-NLS-1$
		context.put("double2formatter", fDouble2Formatter); //$NON-NLS-1$
		context.put("stringformatter", fStringFormatter); //$NON-NLS-1$

		// current time, date
		final Calendar now = Calendar.getInstance();
		final Date creationDate = now.getTime();
		context.put("creation_date", creationDate); //$NON-NLS-1$

//		// author
//		context.put("author", System.getProperty(WizardImportData.SYSPROPERTY_IMPORT_PERSON, "MyTourbook")); //$NON-NLS-1$ //$NON-NLS-2$
//
//		// device infos
//		final String productName = productInfo.getProductName();
//		context.put("devicename", productName.substring(0, productName.indexOf(' '))); //$NON-NLS-1$
//		context.put("productid", "" + productInfo.getProductId()); //$NON-NLS-1$ //$NON-NLS-2$
//		context.put("devicemajorversion", "" + (productInfo.getProductSoftware() / 100)); //$NON-NLS-1$ //$NON-NLS-2$
//		context.put("deviceminorversion", "" + (productInfo.getProductSoftware() % 100)); //$NON-NLS-1$ //$NON-NLS-2$
//
//		// Version
//		String pluginmajorversion = "0"; //$NON-NLS-1$
//		String pluginminorversion = "0"; //$NON-NLS-1$
//		final Version version = Activator.getDefault().getVersion();
//		if (version != null) {
//			pluginmajorversion = "" + version.getMajor(); //$NON-NLS-1$
//			pluginminorversion = "" + version.getMinor(); //$NON-NLS-1$
//		}
//		context.put("pluginmajorversion", pluginmajorversion); //$NON-NLS-1$
//		context.put("pluginminorversion", pluginminorversion); //$NON-NLS-1$

		// extent of waypoint, routes and tracks:
		double min_latitude = 90.0;
		double min_longitude = 180.0;
		double max_latitude = -90.0;
		double max_longitude = -180.0;

		final List routes = (List) context.get("routes"); //$NON-NLS-1$
		if (routes != null) {
			final Iterator route_iterator = routes.iterator();
			while (route_iterator.hasNext()) {
				final GPSRoute route = (GPSRoute) route_iterator.next();
				min_longitude = route.getMinLongitude();
				max_longitude = route.getMaxLongitude();
				min_latitude = route.getMinLatitude();
				max_latitude = route.getMaxLatitude();
			}
		}

		final List tracks = (List) context.get("tracks"); //$NON-NLS-1$
		if (tracks != null) {
			final Iterator track_iterator = tracks.iterator();
			while (track_iterator.hasNext()) {
				final GPSTrack track = (GPSTrack) track_iterator.next();
				min_longitude = Math.min(min_longitude, track.getMinLongitude());
				max_longitude = Math.max(max_longitude, track.getMaxLongitude());
				min_latitude = Math.min(min_latitude, track.getMinLatitude());
				max_latitude = Math.max(max_latitude, track.getMaxLatitude());
			}
		}
		final List waypoints = (List) context.get("waypoints"); //$NON-NLS-1$
		if (waypoints != null) {
			final Iterator waypoint_iterator = waypoints.iterator();
			while (waypoint_iterator.hasNext()) {
				final GPSWaypoint waypoint = (GPSWaypoint) waypoint_iterator.next();
				min_longitude = Math.min(min_longitude, waypoint.getLongitude());
				max_longitude = Math.max(max_longitude, waypoint.getLongitude());
				min_latitude = Math.min(min_latitude, waypoint.getLatitude());
				max_latitude = Math.max(max_latitude, waypoint.getLatitude());
			}
		}
		context.put("min_latitude", new Double(min_latitude)); //$NON-NLS-1$
		context.put("min_longitude", new Double(min_longitude)); //$NON-NLS-1$
		context.put("max_latitude", new Double(max_latitude)); //$NON-NLS-1$
		context.put("max_longitude", new Double(max_longitude)); //$NON-NLS-1$

		Date starttime = null;
		Date endtime = null;
		int heartNum = 0;
		long heartSum = 0;
		int cadNum = 0;
		long cadSum = 0;
		short maximumheartrate = 0;
		double totaldistance = 0;

		for (final Iterator trackIter = tracks.iterator(); trackIter.hasNext();) {
			final GPSTrack track = (GPSTrack) trackIter.next();
			for (final Iterator wpIter = track.getWaypoints().iterator(); wpIter.hasNext();) {
				final GPSTrackpoint wp = (GPSTrackpoint) wpIter.next();

				// starttime, totaltime
				if (wp.getDate() != null) {
					if (starttime == null)
						starttime = wp.getDate();
					endtime = wp.getDate();
				}
				if (wp instanceof GarminTrackpointAdapter) {
					final GarminTrackpointAdapter gta = (GarminTrackpointAdapter) wp;

					// averageheartrate, maximumheartrate
					if (gta.hasValidHeartrate()) {
						heartSum += gta.getHeartrate();
						heartNum++;
						if (gta.getHeartrate() > maximumheartrate)
							maximumheartrate = gta.getHeartrate();
					}

					// averagecadence
					if (gta.hasValidCadence()) {
						cadSum += gta.getCadence();
						cadNum++;
					}

					// totaldistance
					if (gta.hasValidDistance())
						totaldistance = gta.getDistance();
				}
			}
		}

		if (starttime != null)
			context.put("starttime", starttime); //$NON-NLS-1$
		else
			context.put("starttime", creationDate); //$NON-NLS-1$

		if (starttime != null && endtime != null)
			context.put("totaltime", ((double) endtime.getTime() - starttime.getTime()) / 1000); //$NON-NLS-1$
		else
			context.put("totaltime", (double) 0); //$NON-NLS-1$

		context.put("totaldistance", totaldistance); //$NON-NLS-1$

		if (maximumheartrate != 0)
			context.put("maximumheartrate", maximumheartrate); //$NON-NLS-1$
		if (heartNum != 0)
			context.put("averageheartrate", heartSum / heartNum); //$NON-NLS-1$
		if (cadNum != 0)
			context.put("averagecadence", cadSum / cadNum); //$NON-NLS-1$
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

		shell.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {

				// allow resizing the width but not the height

				final Point computedSize = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
				computedSize.x = shell.getSize().x;
				shell.setSize(computedSize);
			}
		});
	}

	@Override
	public void create() {

		super.create();

		setTitle(Messages.dialog_export_dialog_title);
		setMessage(fDlgDefaultMessage);

		fInitializeControls = true;
		{
			restoreState();
		}
		fInitializeControls = false;

		setFileName();
		validateFields();
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

		fDlgContainer = (Composite) super.createDialogArea(parent);

		createUI(fDlgContainer);

		return fDlgContainer;
	}

	private void createUI(final Composite parent) {

		fInputContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(fInputContainer);
		GridLayoutFactory.swtDefaults().margins(10, 5).applyTo(fInputContainer);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));

		createUIOptions(fInputContainer);
		createUIDestination(fInputContainer);
		createUIProgress(parent);
	}

	private void createUIDestination(final Composite parent) {

		Label label;
//		GridData gd;

		final ModifyListener filePathModifyListener = new ModifyListener() {
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
			fComboFile = new Combo(group, SWT.SINGLE | SWT.BORDER);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(fComboFile);
			((GridData) fComboFile.getLayoutData()).widthHint = SIZING_TEXT_FIELD_WIDTH;
			fComboFile.addModifyListener(filePathModifyListener);
			fComboFile.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					validateFields();
				}
			});
			fComboFile.addListener(SWT.Verify, new Listener() {
				public void handleEvent(final Event e) {
					UI.verifyFilenameInput(e);
				}
			});

			/*
			 * button: browse
			 */
			fBtnSelectFile = new Button(group, SWT.PUSH);
			fBtnSelectFile.setText(Messages.app_btn_browse);
			fBtnSelectFile.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectBrowseFile();
					validateFields();
				}
			});
			setButtonLayoutData(fBtnSelectFile);

//			/*
//			 * text: filename
//			 */
//			fTxtFileName = new Text(group, SWT.BORDER);
//			GridDataFactory.fillDefaults().grab(true, false).span(1, 1).applyTo(fTxtFileName);
//			fTxtFileName.setToolTipText(Messages.dialog_export_txt_fileName_tooltip);
//			fTxtFileName.addModifyListener(filePathModifyListener);
//			// spacer
//			new Label(group, SWT.NONE);

			// -----------------------------------------------------------------------------

			/*
			 * label: path
			 */
			label = new Label(group, SWT.NONE);
			label.setText(Messages.dialog_export_label_exportFilePath);

			/*
			 * combo: path
			 */
			fComboPath = new Combo(group, SWT.SINGLE | SWT.BORDER);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(fComboPath);
			((GridData) fComboPath.getLayoutData()).widthHint = SIZING_TEXT_FIELD_WIDTH;
			fComboPath.addModifyListener(filePathModifyListener);
			fComboPath.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					validateFields();
				}
			});

			/*
			 * button: browse
			 */
			fBtnSelectDirectory = new Button(group, SWT.PUSH);
			fBtnSelectDirectory.setText(Messages.app_btn_browse);
			fBtnSelectDirectory.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectBrowseDirectory();
					validateFields();
				}
			});
			setButtonLayoutData(fBtnSelectDirectory);

			// -----------------------------------------------------------------------------

			/*
			 * checkbox: overwrite files
			 */
			fChkOverwriteFiles = new Button(group, SWT.CHECK);
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).span(3, 1).applyTo(fChkOverwriteFiles);
			fChkOverwriteFiles.setText(Messages.dialog_export_chk_overwriteFiles);
			fChkOverwriteFiles.setToolTipText(Messages.dialog_export_chk_overwriteFiles_tooltip);

			// -----------------------------------------------------------------------------

			/*
			 * label: file path
			 */
			label = new Label(group, SWT.NONE);
			label.setText(Messages.dialog_export_label_filePath);

			/*
			 * text: filename
			 */
			fTxtFilePath = new Text(group, /* SWT.BORDER | */SWT.READ_ONLY);
			GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(fTxtFilePath);
			fTxtFilePath.setToolTipText(Messages.dialog_export_txt_filePath_tooltip);

			// spacer
//			new Label(group, SWT.NONE);
		}

	}

	private void createUIOptions(final Composite parent) {

		// container
		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.dialog_export_group_options);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);
		{
			/*
			 * checkbox: merge all tours
			 */
			fChkMergeAllTours = new Button(group, SWT.CHECK);
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).span(3, 0).applyTo(fChkMergeAllTours);
			fChkMergeAllTours.setText(Messages.dialog_export_chk_mergeAllTours);
			fChkMergeAllTours.setToolTipText(Messages.dialog_export_chk_mergeAllTours_tooltip);
			fChkMergeAllTours.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					enableFields();
				}
			});

			/*
			 * checkbox: camouflage speed
			 */
			fChkCamouflageSpeed = new Button(group, SWT.CHECK);
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(fChkCamouflageSpeed);
			fChkCamouflageSpeed.setText(Messages.dialog_export_chk_camouflageSpeed);
			fChkCamouflageSpeed.setToolTipText(Messages.dialog_export_chk_camouflageSpeed_tooltip);
			fChkCamouflageSpeed.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {

					validateFields();
					enableFields();

					if (fChkCamouflageSpeed.getSelection()) {
						fTxtCamouflageSpeed.setFocus();
					}
				}
			});

			// text: speed
			fTxtCamouflageSpeed = new Text(group, SWT.BORDER | SWT.TRAIL);
			fTxtCamouflageSpeed.setToolTipText(Messages.dialog_export_chk_camouflageSpeedInput_tooltip);
			fTxtCamouflageSpeed.addModifyListener(new ModifyListener() {
				public void modifyText(final ModifyEvent e) {
					validateFields();
					enableFields();
				}
			});
			fTxtCamouflageSpeed.addListener(SWT.Verify, new Listener() {
				public void handleEvent(final Event e) {
					UI.verifyIntegerInput(e, false);
				}
			});

			// label: unit
			fLblCoumouflageSpeedUnit = new Label(group, SWT.NONE);
			fLblCoumouflageSpeedUnit.setText(UI.UNIT_LABEL_SPEED);
			GridDataFactory.fillDefaults()
					.grab(true, false)
					.align(SWT.BEGINNING, SWT.CENTER)
					.applyTo(fLblCoumouflageSpeedUnit);
		}
	}

	private void createUIProgress(final Composite parent) {

		final int selectedTours = fTourDataList.size();

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
			fProgressIndicator = new ProgressIndicator(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(fProgressIndicator);

			/*
			 * label: exported filename
			 */
			fLblExportedFilePath = new ImageComboLabel(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(fLblExportedFilePath);
		}
	}

	private void doExport() throws IOException {

		// disable button's
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		getButton(IDialogConstants.CANCEL_ID).setEnabled(false);

		final String completeFilePath = fTxtFilePath.getText();
		final ArrayList<GarminTrack> tList = new ArrayList<GarminTrack>();

		if (fTourDataList.size() == 1) {

			// export one tour

			final TourData tourData = fTourDataList.get(0);
			tList.add(getTrack(tourData, TourManager.getTourDateTime(tourData)));

			doExportTour(tList, completeFilePath);

		} else {

			if (fChkMergeAllTours.getSelection()) {

				/*
				 * merge all tours into one
				 */

				final boolean isCamouflageSpeed = fChkCamouflageSpeed.getSelection();
				fTrackStartDateTime = TourManager.getTourDateTime(fTourDataList.get(0));
				DateTime trackDateTime;

				// create tracklist
				for (final TourData tourData : fTourDataList) {

					if (isCamouflageSpeed) {
						trackDateTime = fTrackStartDateTime;
					} else {
						trackDateTime = TourManager.getTourDateTime(tourData);
					}

					final GarminTrack track = getTrack(tourData, trackDateTime);
					if (track != null) {
						tList.add(track);
					}
				}

				doExportTour(tList, completeFilePath);

			} else {

				/*
				 * export each tour separately
				 */

				final IPath exportFilePath = new Path(getExportPathName()).addTrailingSeparator();
				fProgressIndicator.beginTask(fTourDataList.size());

				for (final TourData tourData : fTourDataList) {

					// get filepath
					final IPath filePath = exportFilePath.append(UI.format_yyyymmdd_hhmmss(tourData))
							.addFileExtension(fExportExtensionPoint.getFileExtension());

					// create tracklist
					tList.clear();
					tList.add(getTrack(tourData, TourManager.getTourDateTime(tourData)));

					// display exported filepath
					fLblExportedFilePath.setText(NLS.bind(Messages.dialog_export_lbl_exportFilePath,
							filePath.toOSString()));

					// !!! force label update !!!
					fLblExportedFilePath.update();

					fProgressIndicator.worked(1);

					doExportTour(tList, filePath.toOSString());
				}
			}
		}
	}

	private void doExportTour(final ArrayList<GarminTrack> tList, final String exportFileName) throws IOException {

		// create context
		final VelocityContext context = new VelocityContext();

		// prepare context
		context.put("tracks", tList); //$NON-NLS-1$
		context.put("printtracks", new Boolean(true)); //$NON-NLS-1$
		context.put("printwaypoints", new Boolean(false)); //$NON-NLS-1$
		context.put("printroutes", new Boolean(false)); //$NON-NLS-1$

		final Reader templateReader = new InputStreamReader(this.getClass()
				.getResourceAsStream("/format-templates/gpx-1.0.vm")); //$NON-NLS-1$

		final File exportFile = new File(exportFileName);
		if (exportFile.exists()) {

		}
		final Writer exportWriter = new FileWriter(exportFile);

		addValuesToContext(context);

		Velocity.evaluate(context, exportWriter, "MyTourbook", templateReader); //$NON-NLS-1$
		exportWriter.close();
	}

	private void enableExportButton(final boolean isEnabled) {
		final Button okButton = getButton(IDialogConstants.OK_ID);
		if (okButton != null) {
			okButton.setEnabled(isEnabled);
		}
	}

	private void enableFields() {

		final boolean isOneTour = fTourDataList.size() == 1;

		fChkMergeAllTours.setEnabled(isOneTour == false);
		fComboFile.setEnabled(fChkMergeAllTours.getSelection() || isOneTour);

		final boolean isCamouflageTime = fChkCamouflageSpeed.getSelection();
		fTxtCamouflageSpeed.setEnabled(isCamouflageTime);
		fLblCoumouflageSpeedUnit.setEnabled(isCamouflageTime);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		// keep window size and position
		return fState;
	}

	private String getExportFileName() {
		return fComboFile.getText().trim();
	}

	private String getExportPathName() {
		return fComboPath.getText().trim();
	}

	private GarminTrack getTrack(final TourData tourData, final DateTime trackDateTime) {

		final GarminTrack track = new GarminTrack();

		final int[] timeSerie = tourData.timeSerie;
		final int[] altitudeSerie = tourData.altitudeSerie;
		final double[] latitudeSerie = tourData.latitudeSerie;
		final double[] longitudeSerie = tourData.longitudeSerie;
		final int[] distanceSerie = tourData.distanceSerie;

		// check if all dataseries are available
		if (timeSerie == null
				|| altitudeSerie == null
				|| latitudeSerie == null
				|| longitudeSerie == null
				|| distanceSerie == null) {
			return null;
		}

		DateTime lastTrackDateTime = null;

		final boolean isCamouflageSpeed = fChkCamouflageSpeed.getSelection();
		float camouflageSpeed;
		try {
			camouflageSpeed = Float.parseFloat(fTxtCamouflageSpeed.getText());
		} catch (final NumberFormatException e) {
			camouflageSpeed = 0.1F;
		}
		camouflageSpeed *= UI.UNIT_VALUE_DISTANCE / 3.6f;

		/*
		 * loop: all trackpoints
		 */
		for (int serieIndex = 0; serieIndex < timeSerie.length; serieIndex++) {

			final GarminTrackpointAdapter trackPoint = new GarminTrackpointAdapter(new GarminTrackpointD304());

			trackPoint.setAltitude(altitudeSerie[serieIndex]);

			trackPoint.setLongitude(longitudeSerie[serieIndex]);
			trackPoint.setLatitude(latitudeSerie[serieIndex]);

			float currentTime;

			if (isCamouflageSpeed) {

				// camouflage speed

				currentTime = distanceSerie[serieIndex] / camouflageSpeed;

			} else {

				// keep recorded speed

				currentTime = timeSerie[serieIndex];
			}

			lastTrackDateTime = trackDateTime.plusSeconds((int) currentTime);
			trackPoint.setDate(lastTrackDateTime.toDate());

			track.addWaypoint(trackPoint);
		}

		// keep last date/time for the next merged tour
		fTrackStartDateTime = lastTrackDateTime;

		return track;
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

	@Override
	protected void okPressed() {

		UI.disableAllControls(fInputContainer);

		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {
				try {
					doExport();
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
		});

		super.okPressed();
	}

	private void onSelectBrowseDirectory() {

		final DirectoryDialog dialog = new DirectoryDialog(fDlgContainer.getShell(), SWT.SAVE);
		dialog.setText(Messages.dialog_export_dir_dialog_text);
		dialog.setMessage(Messages.dialog_export_dir_dialog_message);

		dialog.setFilterPath(getExportPathName());

		final String selectedDirectoryName = dialog.open();

		if (selectedDirectoryName != null) {
			setErrorMessage(null);
			fComboPath.setText(selectedDirectoryName);
		}
	}

	private void onSelectBrowseFile() {

		final String fileExtension = fExportExtensionPoint.getFileExtension();

		final FileDialog dialog = new FileDialog(fDlgContainer.getShell(), SWT.SAVE);
		dialog.setText(Messages.dialog_export_file_dialog_text);

		dialog.setFilterPath(getExportPathName());
		dialog.setFilterExtensions(new String[] { fileExtension });
		dialog.setFileName("*." + fileExtension);//$NON-NLS-1$

		final String selectedFilePath = dialog.open();

		if (selectedFilePath != null) {
			setErrorMessage(null);
			fComboFile.setText(new Path(selectedFilePath).toFile().getName());
		}
	}

	private void restoreState() {

		fChkMergeAllTours.setSelection(fState.getBoolean(STATE_IS_MERGE_ALL_TOURS));
		fChkOverwriteFiles.setSelection(fState.getBoolean(STATE_IS_OVERWRITE_FILES));

		// camouflage speed
		fChkCamouflageSpeed.setSelection(fState.getBoolean(STATE_IS_CAMOUFLAGE_SPEED));
		final String camouflageSpeed = fState.get(STATE_CAMOUFLAGE_SPEED);
		fTxtCamouflageSpeed.setText(camouflageSpeed == null ? "10" : camouflageSpeed);//$NON-NLS-1$
		fTxtCamouflageSpeed.selectAll();

		// export file/path
		UI.restoreCombo(fComboFile, fState.getArray(STATE_EXPORT_FILE_NAME));
		UI.restoreCombo(fComboPath, fState.getArray(STATE_EXPORT_PATH_NAME));
	}

	private void saveState() {

		// export file/path
		if (validateFilePath()) {
			fState.put(STATE_EXPORT_PATH_NAME, getUniqueItems(fComboPath.getItems(), getExportPathName()));
			fState.put(STATE_EXPORT_FILE_NAME, getUniqueItems(fComboFile.getItems(), getExportFileName()));
		}

		// merge all tours
		fState.put(STATE_IS_MERGE_ALL_TOURS, fChkMergeAllTours.getSelection());

		// camouflage speed
		fState.put(STATE_IS_CAMOUFLAGE_SPEED, fChkCamouflageSpeed.getSelection());
		fState.put(STATE_CAMOUFLAGE_SPEED, fTxtCamouflageSpeed.getText());

		fState.put(STATE_IS_OVERWRITE_FILES, fChkOverwriteFiles.getSelection());
	}

	private void setError(final String message) {
		setErrorMessage(message);
		enableExportButton(false);
	}

	/**
	 * Overwrite filename with the first tour date/time when the tour is not merged
	 */
	private void setFileName() {

		if (fChkMergeAllTours.getSelection() && fChkMergeAllTours.isEnabled()) {
			return;
		}

		// search for the first tour
		TourData minTourData = null;
		final long minTourMillis = 0;

		for (final TourData tourData : fTourDataList) {
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

		fComboFile.setText(UI.format_yyyymmdd_hhmmss(minTourData));
	}

	private void validateFields() {

		if (fInitializeControls) {
			return;
		}

		/*
		 * validate fields
		 */

		if (validateFilePath() == false) {
			return;
		}

		// speed value
		final boolean isEqualizeTimeEnabled = fChkCamouflageSpeed.getSelection();
		if (isEqualizeTimeEnabled) {

			if (UI.verifyIntegerValue(fTxtCamouflageSpeed.getText()) == false) {
				setError(Messages.dialog_export_error_camouflageSpeedIsInvalid);
				fTxtCamouflageSpeed.setFocus();
				return;
			}
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

		String fileName = getExportFileName();

		// remove extentions
		final int extPos = fileName.indexOf('.');
		if (extPos != -1) {
			fileName = fileName.substring(0, extPos);
		}

		// build file path with extension
		filePath = filePath.addTrailingSeparator()
				.append(fileName)
				.addFileExtension(fExportExtensionPoint.getFileExtension());

		final File newFile = new File(filePath.toOSString());

		if (fileName.length() == 0 || newFile.isDirectory()) {

			// invalid filename

			setError(Messages.dialog_export_msg_fileNameIsInvalid);

		} else if (newFile.exists()) {

			// file already exists

			setMessage(NLS.bind(Messages.dialog_export_msg_fileAlreadyExists, filePath.toOSString()),
					IMessageProvider.WARNING);
			returnValue = true;

		} else {

			setMessage(fDlgDefaultMessage);

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

		fTxtFilePath.setText(filePath.toOSString());

		return returnValue;
	}

}
