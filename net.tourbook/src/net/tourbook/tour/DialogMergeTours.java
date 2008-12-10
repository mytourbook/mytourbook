/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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

import net.tourbook.Messages;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.data.TourData;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.UI;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.tourChart.TourChartConfiguration;
import net.tourbook.util.PixelConverter;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;

public class DialogMergeTours extends TitleAreaDialog {

	private static final int		MAX_ADJUST_SECONDS		= 60;
	private static final int		MAX_ADJUST_MINUTES		= 60;		// x 60
	private static final int		MAX_ADJUST_HOURS		= 5;		// x 60 x 60
	private static final int		MAX_ADJUST_ALTITUDE_1	= 10;
	private static final int		MAX_ADJUST_ALTITUDE_10	= 50;		// x 10

	private Image					fShellImage;

	private TourData				fIntoTourData;
	private TourData				fFromTourData;

	private TourChart				fTourChart;
	private Label					fLabelAltitudeDiff1;
	private Label					fLabelAltitudeDiff10;
	private Scale					fScaleAltitude1;
	private Scale					fScaleAltitude10;

	private Label					fLabelAdjustSecondsValue;
	private Label					fLabelAdjustMinuteValue;
	private Label					fLabelAdjustHourValue;
	private Scale					fScaleAdjustSeconds;
	private Scale					fScaleAdjustMinutes;
	private Scale					fScaleAdjustHours;

	private Button					fBtnReset;
	private Button					fBtnShowOriginal;

	private boolean					fIsTourDirty			= false;
	private boolean					fIsDirtyDisabled;
	private boolean					fIsTourSaved			= false;

	private final IDialogSettings	fDialogSettings;

	private int[]					fBackupFromTimeSerie;
	private int[]					fBackupFromDistanceSerie;
	private int[]					fBackupFromAltitudeSerie;

	private int[]					fBackupIntoTemperatureSerie;

	private int						fBackupIntoTimeOffset;
	private int						fBackupIntoAltitudeOffset;

	private TourChartConfiguration	fTourChartConfig;
	private Button					fChkUpdateChart;
	private Button					fChkAltiDiffScaling;

	public DialogMergeTours(final Shell parentShell, final TourData tourData) {

		super(parentShell);

		// make dialog resizable and display maximize button
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);

		// set icon for the window 
		fShellImage = TourbookPlugin.getImageDescriptor(Messages.Image__merge_tours).createImage();
		setDefaultImage(fShellImage);

		fIntoTourData = tourData;
		fFromTourData = TourManager.getInstance().getTourData(fIntoTourData.getMergeFromTourId());

		fDialogSettings = TourbookPlugin.getDefault().getDialogSettingsSection(getClass().getName());
	}

	private void actionResetSettings() {

		fScaleAdjustSeconds.setSelection(MAX_ADJUST_SECONDS);
		fScaleAdjustMinutes.setSelection(MAX_ADJUST_MINUTES);
		fScaleAdjustHours.setSelection(MAX_ADJUST_HOURS);
		fScaleAltitude1.setSelection(MAX_ADJUST_ALTITUDE_1);
		fScaleAltitude10.setSelection(MAX_ADJUST_ALTITUDE_10);

		onSelectAdjustmentSettings();
	}

	private int[] backupDataSerie(final int[] source) {

		int[] backup = null;
		if (source != null) {
			final int serieLength = source.length;
			backup = new int[serieLength];
			System.arraycopy(source, 0, backup, 0, serieLength);
		}

		return backup;
	}

	@Override
	public boolean close() {

		saveState();

		if (fIsTourSaved == false) {

			// tour is not saved, dialog is canceled, restore original values
			restoreDataBackup();
		}

		return super.close();
	}

	private void computeMergedData() {

		final int xMergeOffset = fIntoTourData.getMergedTourTimeOffset();
		final int yMergeOffset = (int) (fIntoTourData.getMergedAltitudeOffset() * UI.UNIT_VALUE_ALTITUDE);

		final int[] intoTimeSerie = fIntoTourData.timeSerie;
		final int[] intoDistanceSerie = fIntoTourData.distanceSerie;
		final int[] intoAltitudeSerie = fIntoTourData.altitudeSerie;

		final int[] fromTimeSerie = fFromTourData.timeSerie;
		final int[] fromAltitudeSerie = fFromTourData.altitudeSerie;
		final int[] fromTemperatureSerie = fFromTourData.temperatureSerie;

		final int lastFromIndex = fromTimeSerie.length - 1;

//		boolean isCreateFromDistance = false;
//		final boolean isCreateIntoTemperature = false;
//
//		if ((fromDistaceSerie == null || fromDistaceSerie.length == 0)
//				&& intoDistanceSerie != null
//				&& intoDistanceSerie.length > 0) {
//
//			// distance is available in destination but not in source
//
//			isCreateFromDistance = true;
//		}
//
//		if ((mergeIntoTemperatureSerie == null || mergeIntoTemperatureSerie.length == 0)
//				&& mergeFromTemperatureSerie != null
//				&& mergeFromTemperatureSerie.length > 0) {
//
//			// temperature is available in source but not in destination
//
//			isCreateIntoTemperature = true;
//			mergeIntoTemperatureSerie = new int[mergeFromTemperatureSerie.length];
//		}

//		if (isCreateFromDistance) {

		final int serieLength = intoTimeSerie.length;

		final int[] newFromDistanceSerie = new int[serieLength];
		final int[] newFromTimeSerie = new int[serieLength];
		final int[] newFromAltitudeSerie = new int[serieLength];
		final int[] newFromAltiDiffSerie = new int[serieLength];

		final int[] newIntoTemperatureSerie = new int[serieLength];

		int fromIndex = 0;
		int fromTime = fromTimeSerie[0] + xMergeOffset;

		for (int intoIndex = 0; intoIndex < serieLength; intoIndex++) {

			final int intoTime = intoTimeSerie[intoIndex];

			/*
			 * time in the merged tour (into...) is the leading time
			 */
			while (fromTime < intoTime) {

				fromIndex++;

				// check array bounds
				fromIndex = (fromIndex <= lastFromIndex) ? fromIndex : lastFromIndex;

				if (fromIndex == lastFromIndex) {
					//prevent endless loops
					break;
				}

				fromTime = fromTimeSerie[fromIndex] + xMergeOffset;
			}

			final int intoAltitude = intoAltitudeSerie[intoIndex];
			final int fromAltitude = fromAltitudeSerie[fromIndex] + yMergeOffset;

			newFromDistanceSerie[intoIndex] = intoDistanceSerie[intoIndex];
			newFromTimeSerie[intoIndex] = intoTime;
			newFromAltitudeSerie[intoIndex] = fromAltitude;

			newFromAltiDiffSerie[intoIndex] = fromAltitude - intoAltitude;
			newIntoTemperatureSerie[intoIndex] = fromTemperatureSerie[fromIndex];
		}

		fFromTourData.mergeAltitudeSerie = newFromAltitudeSerie;
		fFromTourData.mergeAltitudeDiff = newFromAltiDiffSerie;

		fIntoTourData.temperatureSerie = newIntoTemperatureSerie;
//		}
	}

	@Override
	protected void configureShell(final Shell shell) {

		super.configureShell(shell);

		shell.setText(Messages.tour_merger_dialog_title);

		shell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {
				fShellImage.dispose();
			}
		});
	}

	@Override
	public void create() {

		// this will create the UI widgets
		super.create();

		setTitle(NLS.bind(Messages.tour_merger_dialog_header_title, TourManager.getTourTitle(fIntoTourData)));
		setMessage(NLS.bind(Messages.tour_merger_dialog_header_message, TourManager.getTourTitle(fFromTourData)));

		createDataBackup();
		computeMergedData();

		// set alti diff scaling
		fTourChartConfig.isRelativeAltiDiffScaling = fChkAltiDiffScaling.getSelection();
		fTourChart.updateTourChart(fIntoTourData, fTourChartConfig, true);

		updateUIFromTourData();

		enableActions();
	}

	@Override
	protected final void createButtonsForButtonBar(final Composite parent) {

		super.createButtonsForButtonBar(parent);

		// set text for the OK button
		getButton(IDialogConstants.OK_ID).setText(Messages.app_action_save);
	}

	private void createDataBackup() {

		/*
		 * keep a backup of the altitude data because these data will be changed in this dialog
		 */
		fBackupFromTimeSerie = backupDataSerie(fFromTourData.timeSerie);
		fBackupFromDistanceSerie = backupDataSerie(fFromTourData.distanceSerie);
		fBackupFromAltitudeSerie = backupDataSerie(fFromTourData.altitudeSerie);

		fBackupIntoTemperatureSerie = backupDataSerie(fIntoTourData.temperatureSerie);

		fBackupIntoTimeOffset = fIntoTourData.getMergedTourTimeOffset();
		fBackupIntoAltitudeOffset = fIntoTourData.getMergedAltitudeOffset();
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		final Composite dlgContainer = (Composite) super.createDialogArea(parent);

		createUI(dlgContainer);

		restoreState();

		return dlgContainer;
	}

	private void createUI(final Composite parent) {

		final Composite dlgContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(dlgContainer);
		GridLayoutFactory.fillDefaults().margins(10, 10).applyTo(dlgContainer);

		createUITourChart(dlgContainer);
		createUISectionAdjustments(dlgContainer);
		createUISectionOptions(dlgContainer);
		createUISectionButtons(dlgContainer);
	}

	private void createUISectionAdjustments(final Composite parent) {

		final PixelConverter pc = new PixelConverter(parent);
		final int valueWidth = pc.convertWidthInCharsToPixels(4);
		Label label;

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));

		/*
		 * group: adjust altitude
		 */
		final Group groupAltitude = new Group(container, SWT.NONE);
		groupAltitude.setText("Adjust altitude");
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.BEGINNING).applyTo(groupAltitude);
		GridLayoutFactory.fillDefaults().numColumns(2).extendedMargins(0, 0, 0, 0).spacing(0, 0).applyTo(groupAltitude);
//		groupAltitude.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));

		/*
		 * scale: altitude 10m
		 */
		fLabelAltitudeDiff1 = new Label(groupAltitude, SWT.TRAIL);
		GridDataFactory.fillDefaults()
				.align(SWT.END, SWT.CENTER)
				.hint(pc.convertWidthInCharsToPixels(8), SWT.DEFAULT)
				.applyTo(fLabelAltitudeDiff1);

		fScaleAltitude1 = new Scale(groupAltitude, SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleAltitude1);
		fScaleAltitude1.setMinimum(0);
		fScaleAltitude1.setMaximum(MAX_ADJUST_ALTITUDE_1 * 2);
		fScaleAltitude1.setPageIncrement(1);
		fScaleAltitude1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelectAdjustmentSettings();
			}
		});

		/*
		 * scale: altitude 100m
		 */
		fLabelAltitudeDiff10 = new Label(groupAltitude, SWT.TRAIL);
		GridDataFactory.fillDefaults()
				.align(SWT.END, SWT.CENTER)
				.hint(pc.convertWidthInCharsToPixels(8), SWT.DEFAULT)
				.applyTo(fLabelAltitudeDiff10);

		fScaleAltitude10 = new Scale(groupAltitude, SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleAltitude10);
		fScaleAltitude10.setMinimum(0);
		fScaleAltitude10.setMaximum(MAX_ADJUST_ALTITUDE_10 * 2);
		fScaleAltitude10.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelectAdjustmentSettings();
			}
		});

		/*
		 * group: adjust time
		 */
		final Group groupTime = new Group(container, SWT.NONE);
		groupTime.setText("Adjust time");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(groupTime);
		GridLayoutFactory.fillDefaults().numColumns(4).extendedMargins(5, 0, 0, 0).spacing(0, 0).applyTo(groupTime);

		/*
		 * scale: adjust seconds
		 */
		fLabelAdjustSecondsValue = new Label(groupTime, SWT.TRAIL);
		GridDataFactory.fillDefaults()
				.align(SWT.END, SWT.CENTER)
				.hint(valueWidth, SWT.DEFAULT)
				.applyTo(fLabelAdjustSecondsValue);

		label = new Label(groupTime, SWT.NONE);
		label.setText(UI.SPACE);

		label = new Label(groupTime, SWT.NONE);
		label.setText(Messages.tour_merger_label_adjust_seconds);

		fScaleAdjustSeconds = new Scale(groupTime, SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleAdjustSeconds);

		fScaleAdjustSeconds.setMinimum(0);
		fScaleAdjustSeconds.setMaximum(MAX_ADJUST_SECONDS * 2);
		fScaleAdjustSeconds.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelectAdjustmentSettings();
			}
		});

		/*
		 * scale: adjust minutes
		 */
		fLabelAdjustMinuteValue = new Label(groupTime, SWT.TRAIL);
		GridDataFactory.fillDefaults()
				.align(SWT.END, SWT.CENTER)
				.hint(valueWidth, SWT.DEFAULT)
				.applyTo(fLabelAdjustMinuteValue);

		label = new Label(groupTime, SWT.NONE);
		label.setText(UI.SPACE);

		label = new Label(groupTime, SWT.NONE);
		label.setText(Messages.tour_merger_label_adjust_minutes);

		fScaleAdjustMinutes = new Scale(groupTime, SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleAdjustMinutes);
		fScaleAdjustMinutes.setMinimum(0);
		fScaleAdjustMinutes.setMaximum(MAX_ADJUST_MINUTES * 2);
		fScaleAdjustMinutes.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelectAdjustmentSettings();
			}
		});

		/*
		 * scale: adjust hours
		 */
		fLabelAdjustHourValue = new Label(groupTime, SWT.TRAIL);
		GridDataFactory.fillDefaults()
				.align(SWT.END, SWT.CENTER)
				.hint(valueWidth, SWT.DEFAULT)
				.applyTo(fLabelAdjustHourValue);

		label = new Label(groupTime, SWT.NONE);
		label.setText(UI.SPACE);

		label = new Label(groupTime, SWT.NONE);
		label.setText(Messages.tour_merger_label_adjust_hours);

		fScaleAdjustHours = new Scale(groupTime, SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleAdjustHours);
		fScaleAdjustHours.setMinimum(0);
		fScaleAdjustHours.setMaximum(MAX_ADJUST_HOURS * 2);
		fScaleAdjustHours.setPageIncrement(1);
		fScaleAdjustHours.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelectAdjustmentSettings();
			}
		});
	}

	private void createUISectionButtons(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).indent(0, 10).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);

		// label for horizontal trail adjustment
		final Label label = new Label(container, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(label);

		fBtnReset = new Button(container, SWT.NONE);
		fBtnReset.setText(Messages.tour_merger_btn_reset_values);
		fBtnReset.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				actionResetSettings();
			}
		});

		/*
		 * button: compare values
		 */
		fBtnShowOriginal = new Button(container, SWT.NONE);
		fBtnShowOriginal.setText(Messages.tour_merger_btn_show_original_values);
		fBtnShowOriginal.setToolTipText(Messages.tour_merger_btn_show_original_values_tooltip);
		fBtnShowOriginal.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(final KeyEvent e) {
//				if (fIsInitialAltiDisplayed == false && e.character == ' ') {
//					fIsInitialAltiDisplayed = true;
//					setOriginalAltitudeValues();
//					fDialogTourChart.updateTourChart(!fChkScaleYAxis.getSelection());
//				}
			}

			@Override
			public void keyReleased(final KeyEvent e) {
//				setInitialAltitudeValues();
//				fDialogTourChart.updateTourChart(!fChkScaleYAxis.getSelection());
//				fIsInitialAltiDisplayed = false;
			}
		});
	}

	private void createUISectionOptions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).indent(0, 0).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));

		/*
		 * checkbox: preview modified chart
		 */
		fChkUpdateChart = new Button(container, SWT.CHECK);
		fChkUpdateChart.setText("&Preview graphs");
		fChkUpdateChart.setToolTipText("Previews graphs (e.g. temperature) which have been modified with the adjustment options (altitude and time). This will reduce performance when an adjusment option is modified");
		fChkUpdateChart.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				fIsDirtyDisabled = true;
				onSelectAdjustmentSettings();
				fIsDirtyDisabled = false;
			}
		});

		/*
		 * checkbox: relative or absolute scale
		 */
		fChkAltiDiffScaling = new Button(container, SWT.CHECK);
		fChkAltiDiffScaling.setText("Show altitude difference with relative scaling");
		fChkAltiDiffScaling.setToolTipText("The altitude difference can be scaled with absolute or relative scaling.");
		fChkAltiDiffScaling.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				fIsDirtyDisabled = true;
				onSelectAdjustmentSettings();
				fIsDirtyDisabled = false;
			}
		});
	}

	private void createUITourChart(final Composite dlgContainer) {

		fTourChart = new TourChart(dlgContainer, SWT.BORDER, true);
		GridDataFactory.fillDefaults().grab(true, true).minSize(400, 200).applyTo(fTourChart);

		fTourChart.setShowZoomActions(true);
		fTourChart.setShowSlider(true);

		// set altitude visible
		fTourChartConfig = new TourChartConfiguration(true);
		fTourChartConfig.addVisibleGraph(TourManager.GRAPH_ALTITUDE);
//		fTourChartConfig.addVisibleGraph(TourManager.GRAPH_TEMPERATURE);

		// overwrite x-axis from pref store
		fTourChartConfig.setIsShowTimeOnXAxis(TourbookPlugin.getDefault()
				.getPreferenceStore()
				.getString(ITourbookPreferences.MERGE_TOUR_GRAPH_X_AXIS)
				.equals(TourManager.X_AXIS_TIME));

		fTourChart.addDataModelListener(new IDataModelListener() {

			public void dataModelChanged(final ChartDataModel changedChartDataModel) {

				// set title
				changedChartDataModel.setTitle(TourManager.getTourTitleDetailed(fIntoTourData));
			}
		});
	}

	private void enableActions() {

		// enable/disable save button
		getButton(IDialogConstants.OK_ID).setEnabled(fIsTourDirty);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		// keep window size and position
		return fDialogSettings;
	}

	@Override
	protected void okPressed() {

		if (fIsTourDirty) {

			// save merged tour
			TourManager.saveModifiedTour(fIntoTourData);

			fIsTourSaved = true;
		}

		super.okPressed();
	}

	private void onSelectAdjustmentSettings() {

		// set dirty flag
		setTourDirty();

		final int altiDiff1 = fScaleAltitude1.getSelection() - MAX_ADJUST_ALTITUDE_1;
		final int altiDiff10 = (fScaleAltitude10.getSelection() - MAX_ADJUST_ALTITUDE_10) * 10;

		final int seconds = fScaleAdjustSeconds.getSelection() - MAX_ADJUST_SECONDS;
		final int minutes = fScaleAdjustMinutes.getSelection() - MAX_ADJUST_MINUTES;
		final int hours = fScaleAdjustHours.getSelection() - MAX_ADJUST_HOURS;

		fLabelAltitudeDiff1.setText(Integer.toString((int) (altiDiff1 / UI.UNIT_VALUE_ALTITUDE))
				+ UI.SPACE
				+ UI.UNIT_LABEL_ALTITUDE);
		fLabelAltitudeDiff10.setText(Integer.toString((int) (altiDiff10 / UI.UNIT_VALUE_ALTITUDE))
				+ UI.SPACE
				+ UI.UNIT_LABEL_ALTITUDE);

		fLabelAdjustSecondsValue.setText(Integer.toString(seconds));
		fLabelAdjustMinuteValue.setText(Integer.toString(minutes));
		fLabelAdjustHourValue.setText(Integer.toString(hours));

		final int timeOffset = hours * 3600 + minutes * 60 + seconds;
		final int metricAltiDiff = (int) ((altiDiff1 + altiDiff10) / UI.UNIT_VALUE_ALTITUDE);

		fIntoTourData.setMergedTourTimeOffset(timeOffset);
		fIntoTourData.setMergedAltitudeOffset(metricAltiDiff);

		computeMergedData();

		if (fChkUpdateChart.getSelection()) {
			// update chart
			fTourChart.updateTourChart(fIntoTourData, fTourChartConfig, true);
		} else {
			// update only the merge layer, this is much faster
			fTourChart.updateMergeLayer(true);
		}
	}

	/**
	 * Restore values which have been modified in the dialog
	 * 
	 * @param selectedTour
	 */
	private void restoreDataBackup() {

		fFromTourData.timeSerie = fBackupFromTimeSerie;
		fFromTourData.distanceSerie = fBackupFromDistanceSerie;
		fFromTourData.altitudeSerie = fBackupFromAltitudeSerie;

		fIntoTourData.temperatureSerie = fBackupIntoTemperatureSerie;

		fIntoTourData.setMergedTourTimeOffset(fBackupIntoTimeOffset);
		fIntoTourData.setMergedAltitudeOffset(fBackupIntoAltitudeOffset);
	}

	private void restoreState() {

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		fChkUpdateChart.setSelection(prefStore.getBoolean(ITourbookPreferences.MERGE_TOUR_PREVIEW_CHART));

		// set default value
		boolean isRelativeDiffScaling = true;
		if (prefStore.contains(ITourbookPreferences.MERGE_TOUR_ALTITUDE_DIFF_SCALING)) {
			isRelativeDiffScaling = prefStore.getBoolean(ITourbookPreferences.MERGE_TOUR_ALTITUDE_DIFF_SCALING);
		}
		fChkAltiDiffScaling.setSelection(isRelativeDiffScaling);
	}

	private void saveState() {

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		prefStore.setValue(ITourbookPreferences.MERGE_TOUR_GRAPH_X_AXIS, fTourChartConfig.showTimeOnXAxis
				? TourManager.X_AXIS_TIME
				: TourManager.X_AXIS_DISTANCE);

		prefStore.setValue(ITourbookPreferences.MERGE_TOUR_PREVIEW_CHART, fChkUpdateChart.getSelection());
		prefStore.setValue(ITourbookPreferences.MERGE_TOUR_ALTITUDE_DIFF_SCALING, fChkAltiDiffScaling.getSelection());
	}

	private void setTourDirty() {

		if (fIsDirtyDisabled) {
			return;
		}

		if (fIntoTourData != null) {
			fIsTourDirty = true;
		}

		enableActions();
	}

	/**
	 * set data from the tour into the UI
	 */
	private void updateUIFromTourData() {

		fIsDirtyDisabled = true;

		/*
		 * show time offset
		 */
		final int mergedTourTimeOffset = fIntoTourData.getMergedTourTimeOffset();
		final int mergedMetricAltitudeOffset = fIntoTourData.getMergedAltitudeOffset();

		final int seconds = mergedTourTimeOffset % 60;
		final int minutes = mergedTourTimeOffset / 60 % 60;
		final int hours = mergedTourTimeOffset / 3600;

		fScaleAdjustSeconds.setSelection(seconds + MAX_ADJUST_SECONDS);
		fScaleAdjustMinutes.setSelection(minutes + MAX_ADJUST_MINUTES);
		fScaleAdjustHours.setSelection(hours + MAX_ADJUST_HOURS);

		/*
		 * show altitude offset
		 */
		final float altitudeOffset = mergedMetricAltitudeOffset / UI.UNIT_VALUE_ALTITUDE;
		final int altitudeOffset1 = (int) (altitudeOffset % 10);
		final int altitudeOffset10 = (int) (altitudeOffset / 10);

		fScaleAltitude1.setSelection(altitudeOffset1 + MAX_ADJUST_ALTITUDE_1);
		fScaleAltitude10.setSelection(altitudeOffset10 + MAX_ADJUST_ALTITUDE_10);

		onSelectAdjustmentSettings();

		fIsDirtyDisabled = false;
	}

}
