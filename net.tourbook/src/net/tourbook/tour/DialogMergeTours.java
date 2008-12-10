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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;

public class DialogMergeTours extends TitleAreaDialog {

	private static final int		MAX_ADJUST_MINUTES	= 60;
	private static final int		MAX_ADJUST_SECONDS	= 60;
	private static final int		MAX_ADJUST_HOURS	= 5;
	private static final int		MAX_ADJUST_ALTITUDE	= 200;

	private Image					fShellImage;

	private TourData				fIntoTourData;
	private TourData				fFromTourData;

	private TourChart				fTourChart;
	private Label					fLabelAltitudeDiff;
	private Scale					fScaleAltitude;
	private Label					fLabelAdjustSecondsValue;
	private Scale					fScaleAdjustSeconds;
	private Label					fLabelAdjustMinuteValue;
	private Scale					fScaleAdjustMinutes;
	private Label					fLabelAdjustHourValue;
	private Scale					fScaleAdjustHours;
	private Button					fBtnReset;
	private Button					fBtnShowOriginal;

	private boolean					fIsTourDirty		= false;
	private boolean					fIsDirtyDisabled;
	private boolean					fIsTourSaved		= false;

	private final IDialogSettings	fDialogSettings;

	private int[]					fBackupFromTimeSerie;
	private int[]					fBackupFromDistanceSerie;
	private int[]					fBackupFromAltitudeSerie;

	private int[]					fBackupIntoTemperatureSerie;

	private int						fBackupIntoTimeOffset;
	private int						fBackupIntoAltitudeOffset;

	private TourChartConfiguration	fTourChartConfig;

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
		fScaleAltitude.setSelection(MAX_ADJUST_ALTITUDE);

		onModifyAdjustmentSettings();
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

		final int[] newIntoTemperatureSerie = new int[serieLength];
		final int[] newFromAltiDiffSerie = new int[serieLength];

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
			final int fromAltitude = fromAltitudeSerie[fromIndex] - yMergeOffset;

			newFromDistanceSerie[intoIndex] = intoDistanceSerie[intoIndex];
			newFromTimeSerie[intoIndex] = intoTime;
			newFromAltitudeSerie[intoIndex] = fromAltitude;

//			System.out.println(fromAltitude);
// TODO remove SYSTEM.OUT.PRINTLN

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

		super.create();

		setTitle(NLS.bind(Messages.tour_merger_dialog_header_title, TourManager.getTourTitle(fIntoTourData)));
		setMessage(NLS.bind(Messages.tour_merger_dialog_header_message, TourManager.getTourTitle(fFromTourData)));

		createDataBackup();
		computeMergedData();

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

		return dlgContainer;
	}

	private void createUI(final Composite parent) {

		final Composite dlgContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(dlgContainer);
		GridLayoutFactory.fillDefaults().margins(10, 10).applyTo(dlgContainer);

		createUITourChart(dlgContainer);
		createUISectionAdjustments(dlgContainer);
		createUISectionButtons(dlgContainer);
	}

	private void createUISectionAdjustments(final Composite parent) {

		final PixelConverter pc = new PixelConverter(parent);
		final int valueWidth = pc.convertWidthInCharsToPixels(4);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(6).applyTo(container);

		/*
		 * scale: altitude
		 */
		Label label = new Label(container, SWT.NONE);
		label.setText(Messages.tour_merger_label_adjust_altitude);

		fLabelAltitudeDiff = new Label(container, SWT.TRAIL);
		GridDataFactory.fillDefaults()
				.align(SWT.END, SWT.CENTER)
				.hint(pc.convertWidthInCharsToPixels(8), SWT.DEFAULT)
				.applyTo(fLabelAltitudeDiff);

		fScaleAltitude = new Scale(container, SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleAltitude);

		fScaleAltitude.setMinimum(0);
		fScaleAltitude.setMaximum(MAX_ADJUST_ALTITUDE * 2);
		fScaleAltitude.setPageIncrement(MAX_ADJUST_ALTITUDE / 10);
		fScaleAltitude.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyAdjustmentSettings();
			}
		});

		/*
		 * scale: adjust seconds
		 */
		label = new Label(container, SWT.NONE);
		label.setText(Messages.tour_merger_label_adjust_seconds);

		fLabelAdjustSecondsValue = new Label(container, SWT.TRAIL);
		GridDataFactory.fillDefaults()
				.align(SWT.END, SWT.CENTER)
				.hint(valueWidth, SWT.DEFAULT)
				.applyTo(fLabelAdjustSecondsValue);

		fScaleAdjustSeconds = new Scale(container, SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleAdjustSeconds);

		fScaleAdjustSeconds.setMinimum(0);
		fScaleAdjustSeconds.setMaximum(120);
		fScaleAdjustSeconds.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyAdjustmentSettings();
			}
		});

		/*
		 * scale: adjust minutes
		 */
		label = new Label(container, SWT.NONE);
		label.setText(Messages.tour_merger_label_adjust_minutes);

		fLabelAdjustMinuteValue = new Label(container, SWT.TRAIL);
		GridDataFactory.fillDefaults()
				.align(SWT.END, SWT.CENTER)
				.hint(valueWidth, SWT.DEFAULT)
				.applyTo(fLabelAdjustMinuteValue);

		fScaleAdjustMinutes = new Scale(container, SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleAdjustMinutes);

		fScaleAdjustMinutes.setMinimum(0);
		fScaleAdjustMinutes.setMaximum(120);
		fScaleAdjustMinutes.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyAdjustmentSettings();
			}
		});

		/*
		 * scale: adjust hours
		 */
		label = new Label(container, SWT.NONE);
		label.setText(Messages.tour_merger_label_adjust_hours);

		fLabelAdjustHourValue = new Label(container, SWT.TRAIL);
		GridDataFactory.fillDefaults()
				.align(SWT.END, SWT.CENTER)
				.hint(valueWidth, SWT.DEFAULT)
				.applyTo(fLabelAdjustHourValue);
//		fLabelAdjustHourValue.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));

		fScaleAdjustHours = new Scale(container, SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleAdjustHours);

		fScaleAdjustHours.setMinimum(0);
		fScaleAdjustHours.setMaximum(MAX_ADJUST_HOURS * 2);
		fScaleAdjustHours.setPageIncrement(1);
		fScaleAdjustHours.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyAdjustmentSettings();
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

	private void createUITourChart(final Composite dlgContainer) {

		fTourChart = new TourChart(dlgContainer, SWT.BORDER, true);
		GridDataFactory.fillDefaults().grab(true, true).minSize(400, 200).applyTo(fTourChart);

		fTourChart.setShowZoomActions(true);
		fTourChart.setShowSlider(true);

		// set altitude visible
		fTourChartConfig = new TourChartConfiguration(true);
		fTourChartConfig.addVisibleGraph(TourManager.GRAPH_ALTITUDE);

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

	private void onModifyAdjustmentSettings() {

		final int altiDiff = fScaleAltitude.getSelection() - MAX_ADJUST_ALTITUDE;
		final int seconds = fScaleAdjustSeconds.getSelection() - 60;
		final int minutes = fScaleAdjustMinutes.getSelection() - 60;
		final int hours = fScaleAdjustHours.getSelection() - MAX_ADJUST_HOURS;

		final int metricAltiDiff = (int) (altiDiff / UI.UNIT_VALUE_ALTITUDE);
		fLabelAltitudeDiff.setText(Integer.toString(metricAltiDiff) + UI.SPACE + UI.UNIT_LABEL_ALTITUDE);

		fLabelAdjustSecondsValue.setText(Integer.toString(seconds));
		fLabelAdjustMinuteValue.setText(Integer.toString(minutes));
		fLabelAdjustHourValue.setText(Integer.toString(hours));

		final int timeOffset = hours * 3600 + minutes * 60 + seconds;

		fIntoTourData.setMergedTourTimeOffset(timeOffset);
		fIntoTourData.setMergedAltitudeOffset(metricAltiDiff);

		// set dirty flag
		setTourDirty();

		computeMergedData();

		// display merge layer
		fTourChart.updateMergeLayer(true);
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

	private void saveState() {

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		prefStore.setValue(ITourbookPreferences.MERGE_TOUR_GRAPH_X_AXIS, fTourChartConfig.showTimeOnXAxis
				? TourManager.X_AXIS_TIME
				: TourManager.X_AXIS_DISTANCE);
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

		final int mergedTourTimeOffset = fIntoTourData.getMergedTourTimeOffset();
		final int mergedAltitudeOffset = fIntoTourData.getMergedAltitudeOffset();

		final int seconds = mergedTourTimeOffset % 60;
		final int minutes = mergedTourTimeOffset / 60 % 60;
		final int hours = mergedTourTimeOffset / 3600;

		fScaleAdjustSeconds.setSelection(seconds + MAX_ADJUST_SECONDS);
		fScaleAdjustMinutes.setSelection(minutes + MAX_ADJUST_MINUTES);
		fScaleAdjustHours.setSelection(hours + MAX_ADJUST_HOURS);

		fScaleAltitude.setSelection((int) ((mergedAltitudeOffset / UI.UNIT_VALUE_ALTITUDE) + MAX_ADJUST_ALTITUDE));

		onModifyAdjustmentSettings();

		fIsDirtyDisabled = false;
	}

}
