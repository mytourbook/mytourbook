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
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.joda.time.DateTime;

public class DialogExportTour extends TitleAreaDialog {

	private static final String		SETTINGS_EXPORT_PATH	= "ExportPath";

	private static final int		SIZING_TEXT_FIELD_WIDTH	= 250;
	private static final int		COMBO_HISTORY_LENGTH	= 20;

	private ExportTourExtension		fExportExtensionPoint;
	private ArrayList<TourData>		fTourDataList;

	private final IDialogSettings	fDialogSettings;

	private Combo					fComboPath;
	private Button					fBtnSelectDirectory;

	private Composite				fDlgContainer;

	public DialogExportTour(final Shell parentShell,
							final ExportTourExtension exportExtensionPoint,
							final ArrayList<TourData> tourDataList) {

		super(parentShell);

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE);

//		setDefaultImage(TourbookPlugin.getImageDescriptor(Messages.Image__quick_edit).createImage());

		fTourDataList = tourDataList;
		fExportExtensionPoint = exportExtensionPoint;

		fDialogSettings = TourbookPlugin.getDefault().getDialogSettingsSection(getClass().getName());
	}

	/**
	 * Adds some important values to the velocity context (e.g. date, ...).
	 * 
	 * @param context
	 *            the velocity context holding all the data
	 */
	@SuppressWarnings("unchecked")//$NON-NLS-1$
	private void addValuesToContext(final VelocityContext context) {
		
		final DecimalFormat double6formatter = (DecimalFormat) NumberFormat.getInstance(Locale.US);
		double6formatter.applyPattern("0.0000000"); //$NON-NLS-1$
		final DecimalFormat int_formatter = (DecimalFormat) NumberFormat.getInstance(Locale.US);
		int_formatter.applyPattern("000000"); //$NON-NLS-1$
		final DecimalFormat double2formatter = (DecimalFormat) NumberFormat.getInstance(Locale.US);
		double2formatter.applyPattern("0.00"); //$NON-NLS-1$
		final OneArgumentMessageFormat string_formatter = new OneArgumentMessageFormat("{0}", Locale.US); //$NON-NLS-1$
		final SimpleDateFormat dateFormat = new SimpleDateFormat();
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$
		
		context.put("dateformatter", dateFormat); //$NON-NLS-1$
		context.put("double6formatter", double6formatter); //$NON-NLS-1$
		context.put("intformatter", int_formatter); //$NON-NLS-1$
		context.put("stringformatter", string_formatter); //$NON-NLS-1$
		context.put("double2formatter", double2formatter); //$NON-NLS-1$

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
	}

	@Override
	public void create() {

		super.create();

		setTitle(Messages.dialog_export_dialog_title);
		setMessage(NLS.bind(Messages.dialog_export_dialog_message, fExportExtensionPoint.getVisibleName()));

		restoreState();
	}

	@Override
	protected final void createButtonsForButtonBar(final Composite parent) {

		super.createButtonsForButtonBar(parent);

		// set text for the OK button
//		String okText = null;
//
//		final TourDataEditorView tourDataEditor = TourManager.getTourDataEditor();
//		if (tourDataEditor != null && tourDataEditor.isDirty() && tourDataEditor.getTourData() == fTourDataList) {
//			okText = Messages.app_action_update;
//		} else {
//			okText = Messages.app_action_save;
//		}
//
//		getButton(IDialogConstants.OK_ID).setText(okText);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		fDlgContainer = (Composite) super.createDialogArea(parent);

		createUI(fDlgContainer);

		return fDlgContainer;
	}

	private void createUI(final Composite parent) {

//		final Label label;
//
//		final PixelConverter pixelConverter = new PixelConverter(parent);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.swtDefaults().applyTo(container);

		createUIDestination(container);
	}

	/**
	 * Create the export destination specification widgets
	 * 
	 * @param parent
	 *            org.eclipse.swt.widgets.Composite
	 */
	private void createUIDestination(final Composite parent) {

		final Font font = parent.getFont();

		// container
		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
		container.setFont(font);

		/*
		 * label
		 */
		final Label label = new Label(container, SWT.NONE);
		label.setText(Messages.dialog_export_label_destination);
		label.setFont(font);

		/*
		 * combo: path
		 */
		fComboPath = new Combo(container, SWT.SINGLE | SWT.BORDER);
		fComboPath.setFont(font);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fComboPath);
		final GridData layoutData = (GridData) fComboPath.getLayoutData();
		layoutData.widthHint = SIZING_TEXT_FIELD_WIDTH;
		fComboPath.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				validatePath();
			}
		});
		fComboPath.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				validatePath();
			}
		});

		/*
		 * button: browse
		 */
		// destination browse button
		fBtnSelectDirectory = new Button(container, SWT.PUSH);
		fBtnSelectDirectory.setText(Messages.dialog_export_btn_selectDirectory);
		fBtnSelectDirectory.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelectBrowseDirectory();
			}
		});
		fBtnSelectDirectory.setFont(font);
		setButtonLayoutData(fBtnSelectDirectory);
	}

	private void doExport() throws IOException {

		// create context
		final VelocityContext context = new VelocityContext();

		// prepare context
		final ArrayList<GarminTrack> tList = new ArrayList<GarminTrack>();
		tList.add(getTracks(fTourDataList.get(0)));

		context.put("tracks", tList); //$NON-NLS-1$
		context.put("printtracks", new Boolean(true)); //$NON-NLS-1$
		context.put("printwaypoints", new Boolean(false)); //$NON-NLS-1$
		context.put("printroutes", new Boolean(false)); //$NON-NLS-1$

		final Reader templateReader = new InputStreamReader(this.getClass()
				.getResourceAsStream("/format-templates/gpx-1.0.vm")); //$NON-NLS-1$

		final File exportFile = new File(fComboPath.getText() + File.separator + "export" + ".gpx"); //$NON-NLS-1$
		final Writer exportWriter = new FileWriter(exportFile);

		addValuesToContext(context);

		Velocity.evaluate(context, exportWriter, "MyTourbook", templateReader); //$NON-NLS-1$
		exportWriter.close();
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		// keep window size and position
		return fDialogSettings;
	}
	private GarminTrack getTracks(final TourData tourData) {

		final GarminTrack track = new GarminTrack();

		final int[] altitudeSerie = tourData.altitudeSerie;
		final double[] latitudeSerie = tourData.latitudeSerie;
		final double[] longitudeSerie = tourData.longitudeSerie;
		final int[] timeSerie = tourData.timeSerie;

		final DateTime tourDateTime = TourManager.getTourDate(tourData);

		for (int serieIndex = 0; serieIndex < timeSerie.length; serieIndex++) {

			final GarminTrackpointAdapter trackPoint = new GarminTrackpointAdapter(new GarminTrackpointD304());
			trackPoint.setAltitude(altitudeSerie[serieIndex]);
			trackPoint.setLongitude(longitudeSerie[serieIndex]);
			trackPoint.setLatitude(latitudeSerie[serieIndex]);
			trackPoint.setDate(tourDateTime.plusSeconds(timeSerie[serieIndex]).toDate());

			track.addWaypoint(trackPoint);
		}

		return track;
	}

	/**
	 * @return <code>true</code> when the path is valid in the destination field
	 */
	private boolean isPathValid() {
		return new Path(fComboPath.getText()).toFile().exists();
	}

	@Override
	protected void okPressed() {

		/*
		 * do export
		 */
		try {
			doExport();
		} catch (final IOException e) {
			e.printStackTrace();
		}

		super.okPressed();
	}

	private void onSelectBrowseDirectory() {

		final DirectoryDialog dialog = new DirectoryDialog(fDlgContainer.getShell(), SWT.SAVE);
		dialog.setText(Messages.dialog_export_dir_dialog_text);
		dialog.setMessage(Messages.dialog_export_dir_dialog_message);

		dialog.setFilterPath(fComboPath.getText());

		final String selectedDirectoryName = dialog.open();

		if (selectedDirectoryName != null) {
			setErrorMessage(null);
			fComboPath.setText(selectedDirectoryName);
		}

	}

	private void restoreState() {

		final String[] pathItems = fDialogSettings.getArray(SETTINGS_EXPORT_PATH);

		if (pathItems != null && pathItems.length > 0) {
			for (final String pathItem : pathItems) {
				fComboPath.add(pathItem);
			}

			// restore last used path
			fComboPath.setText(pathItems[0]);
		}
	}

	private void saveState() {

		String[] pathItems = fComboPath.getItems();

		if (isPathValid()) {

			/*
			 * add current path to the path history
			 */

			final String currentPath = fComboPath.getText();

			final ArrayList<String> pathList = new ArrayList<String>();

			pathList.add(currentPath);

			for (final String pathItem : pathItems) {
				// ignore duplicate entries
				if (currentPath.equals(pathItem) == false) {
					pathList.add(pathItem);
				}

				if (pathList.size() >= COMBO_HISTORY_LENGTH) {
					break;
				}
			}

			pathItems = pathList.toArray(new String[pathList.size()]);
		}

		if (pathItems.length > 0) {
			fDialogSettings.put(SETTINGS_EXPORT_PATH, pathItems);
		}
	}

	private void validatePath() {

		if (isPathValid()) {
			setErrorMessage(null);
		} else {
			setErrorMessage(Messages.dialog_export_invalid_path);
		}

	}

}
