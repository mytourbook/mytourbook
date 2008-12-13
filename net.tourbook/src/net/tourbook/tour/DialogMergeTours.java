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
import net.tourbook.chart.ISliderMoveListener;
import net.tourbook.chart.SelectionChartInfo;
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

	private Button					fBtnResetAdjustment;
	private Button					fBtnResetValues;

	private Button					fChkAdjustStartAltitude;
	private Button					fChkMergeTemperature;
	private Button					fChkAltiDiffScaling;
	private Button					fChkPreviewChart;

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
	protected boolean				fIsChartUpdated;

	/**
	 * creates a int array backup
	 * 
	 * @param source
	 * @return the backup array or <code>null</code> when the source is <code>null</code>
	 */
	private static int[] createDataSerieBackup(final int[] source) {

		int[] backup = null;

		if (source != null) {
			final int serieLength = source.length;
			backup = new int[serieLength];
			System.arraycopy(source, 0, backup, 0, serieLength);
		}

		return backup;
	}

	/**
	 * @param parentShell
	 * @param mergeIntoTour
	 *            {@link TourData} for the tour into which the other tour is merged
	 * @param mergeFromTour
	 *            {@link TourData} for the tour which is merge into the other tour
	 */
	public DialogMergeTours(final Shell parentShell, final TourData mergeIntoTour, final TourData mergeFromTour) {

		super(parentShell);

		// make dialog resizable and display maximize button
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);

		// set icon for the window 
		fShellImage = TourbookPlugin.getImageDescriptor(Messages.image__merge_tours).createImage();
		setDefaultImage(fShellImage);

		fIntoTourData = mergeIntoTour;
		fFromTourData = mergeFromTour;

		fDialogSettings = TourbookPlugin.getDefault().getDialogSettingsSection(getClass().getName());
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
		final int yMergeOffset = fIntoTourData.getMergedAltitudeOffset();

		final int[] intoTimeSerie = fIntoTourData.timeSerie;
		final int[] intoDistanceSerie = fIntoTourData.distanceSerie;
		final int[] intoAltitudeSerie = fIntoTourData.altitudeSerie;

		final int[] fromTimeSerie = fFromTourData.timeSerie;
		final int[] fromAltitudeSerie = fFromTourData.altitudeSerie;
		final int[] fromTemperatureSerie = fFromTourData.temperatureSerie;

		// check if the data series are available
		final boolean isIntoDistance = intoDistanceSerie != null;
		final boolean isFromTemperature = fromTemperatureSerie != null;

		final int lastFromIndex = fromTimeSerie.length - 1;

		final int serieLength = intoTimeSerie.length;

		final int[] newFromTimeSerie = new int[serieLength];
		final int[] newFromAltitudeSerie = new int[serieLength];
		final int[] newFromAltiDiffSerie = new int[serieLength];

		final int[] newIntoTemperatureSerie = new int[serieLength];

		int fromIndex = 0;
		int fromTime = fromTimeSerie[0] + xMergeOffset;

		/*
		 * create new time/distance serie for the from tour according to the time of the into tour
		 */
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

			newFromTimeSerie[intoIndex] = intoTime;

			newFromAltitudeSerie[intoIndex] = fromAltitude;
			newFromAltiDiffSerie[intoIndex] = fromAltitude - intoAltitude;

			if (isFromTemperature) {
				newIntoTemperatureSerie[intoIndex] = fromTemperatureSerie[fromIndex];
			}
		}

		fFromTourData.mergeAltitudeSerie = newFromAltitudeSerie;
		fFromTourData.mergeAltitudeDiff = newFromAltiDiffSerie;

		if (isFromTemperature) {
			fIntoTourData.temperatureSerie = newIntoTemperatureSerie;
		}

		/*
		 * calculate adjusted start altitude between the start and the first slider, the altitude
		 * will be adjusted according to the distance
		 */
		if (fChkAdjustStartAltitude.getSelection() && isIntoDistance) {

			final int[] adjustedIntoAltitudeSerie = new int[serieLength];

			final float startAltiDiff = newFromAltiDiffSerie[0];
			final int endIndex = fTourChart.getXSliderPosition().getLeftSliderValueIndex();
			final float distanceDiff = intoDistanceSerie[endIndex];

			final int[] altitudeSerie = fIntoTourData.altitudeSerie;

			for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

				if (serieIndex < endIndex) {

					// add adjusted altitude

					final float intoDistance = intoDistanceSerie[serieIndex];
					final float distanceScale = 1 - intoDistance / distanceDiff;
					final int adjustedAltiDiff = (int) (startAltiDiff * distanceScale);
					final int newAltitude = altitudeSerie[serieIndex] + adjustedAltiDiff;

					adjustedIntoAltitudeSerie[serieIndex] = newAltitude;
					newFromAltiDiffSerie[serieIndex] = newFromAltitudeSerie[serieIndex] - newAltitude;

				} else {

					// add altitude which are not adjusted

					adjustedIntoAltitudeSerie[serieIndex] = altitudeSerie[serieIndex];
				}
			}

			fFromTourData.mergeAdjustedAltitudeSerie = adjustedIntoAltitudeSerie;

		} else {

			// disable adjusted altitude
			fFromTourData.mergeAdjustedAltitudeSerie = null;
		}
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
		fTourChart.updateMergeLayer(true);

		updateUIFromTourData();

		// update chart after the UI is updated from the tour
		updateTourChart();

		enableActions();
	}

	@Override
	protected final void createButtonsForButtonBar(final Composite parent) {

		super.createButtonsForButtonBar(parent);

		// rename OK button
		getButton(IDialogConstants.OK_ID).setText(Messages.app_action_save);
	}

	private void createDataBackup() {

		/*
		 * keep a backup of the altitude data because these data will be changed in this dialog
		 */
		fBackupFromTimeSerie = createDataSerieBackup(fFromTourData.timeSerie);
		fBackupFromDistanceSerie = createDataSerieBackup(fFromTourData.distanceSerie);
		fBackupFromAltitudeSerie = createDataSerieBackup(fFromTourData.altitudeSerie);

		fBackupIntoTemperatureSerie = createDataSerieBackup(fIntoTourData.temperatureSerie);

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
		GridLayoutFactory.fillDefaults().margins(10, 0).numColumns(1).applyTo(dlgContainer);
//		dlgContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));

		createUITourChart(dlgContainer);

		final Composite columnContainer = new Composite(dlgContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(columnContainer);
		GridLayoutFactory.fillDefaults().numColumns(2).spacing(20, 0).applyTo(columnContainer);
//		columnContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));

		createUISectionAdjustments(columnContainer);

		final Composite optionContainer = new Composite(columnContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(optionContainer);
		GridLayoutFactory.fillDefaults().margins(0, 0).numColumns(1).applyTo(optionContainer);
//		optionContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));

		createUISectionSaveActions(optionContainer);
		createUISectionDisplayOptions(optionContainer);
		createUISectionButtons(optionContainer);
	}

	private void createUISectionAdjustments(final Composite parent) {

		final PixelConverter pc = new PixelConverter(parent);
		final int valueWidth = pc.convertWidthInCharsToPixels(4);
		Label label;

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));

		/*
		 * group: adjust altitude
		 */
		final Group groupAltitude = new Group(container, SWT.NONE);
		groupAltitude.setText(Messages.tour_merger_group_adjust_altitude);
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
		groupTime.setText(Messages.tour_merger_group_adjust_time);
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
//		fScaleAdjustSeconds.setPageIncrement(1);
		fScaleAdjustSeconds.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelectAdjustmentSettings();
			}
		});

		/*
		 * container: minute and hour scale
		 */
		final Composite minContainer = new Composite(groupTime, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).span(4, 1).applyTo(minContainer);
		GridLayoutFactory.fillDefaults().numColumns(8).spacing(0, 0).applyTo(minContainer);

		/*
		 * scale: adjust minutes
		 */
		fLabelAdjustMinuteValue = new Label(minContainer, SWT.TRAIL);
		GridDataFactory.fillDefaults()
				.align(SWT.END, SWT.CENTER)
				.hint(valueWidth, SWT.DEFAULT)
				.applyTo(fLabelAdjustMinuteValue);

		label = new Label(minContainer, SWT.NONE);
		label.setText(UI.SPACE);

		label = new Label(minContainer, SWT.NONE);
		label.setText(Messages.tour_merger_label_adjust_minutes);

		fScaleAdjustMinutes = new Scale(minContainer, SWT.HORIZONTAL);
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
		fLabelAdjustHourValue = new Label(minContainer, SWT.TRAIL);
		GridDataFactory.fillDefaults()
				.align(SWT.END, SWT.CENTER)
				.hint(valueWidth, SWT.DEFAULT)
				.applyTo(fLabelAdjustHourValue);

		label = new Label(minContainer, SWT.NONE);
		label.setText(UI.SPACE);

		label = new Label(minContainer, SWT.NONE);
		label.setText(Messages.tour_merger_label_adjust_hours);

		fScaleAdjustHours = new Scale(minContainer, SWT.HORIZONTAL);
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
		GridDataFactory.fillDefaults().grab(true, true).indent(0, 0).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);

		// label for horizontal trail adjustment
		final Label label = new Label(container, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(label);

		/*
		 * button: reset all adjustment options
		 */
		fBtnResetAdjustment = new Button(container, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.END).applyTo(fBtnResetAdjustment);
		fBtnResetAdjustment.setText(Messages.tour_merger_btn_reset_adjustment);
		fBtnResetAdjustment.setToolTipText(Messages.tour_merger_btn_reset_adjustment_tooltip);
		fBtnResetAdjustment.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelectResetAdjustments();
			}
		});

		/*
		 * button: show original values
		 */
		fBtnResetValues = new Button(container, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.END).applyTo(fBtnResetValues);
		fBtnResetValues.setText(Messages.tour_merger_btn_reset_values);
		fBtnResetValues.setToolTipText(Messages.tour_merger_btn_reset_values_tooltip);
		fBtnResetValues.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelectResetValues();
			}
		});
	}

	private void createUISectionDisplayOptions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).indent(0, 10).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));

		/*
		 * checkbox: relative or absolute scale
		 */
		fChkAltiDiffScaling = new Button(container, SWT.CHECK);
		fChkAltiDiffScaling.setText(Messages.tour_merger_chk_alti_diff_scaling);
		fChkAltiDiffScaling.setToolTipText(Messages.tour_merger_chk_alti_diff_scaling_tooltip);
		fChkAltiDiffScaling.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				fIsDirtyDisabled = true;
				onSelectAdjustmentSettings();
				fIsDirtyDisabled = false;
			}
		});

		/*
		 * checkbox: preview chart
		 */
		fChkPreviewChart = new Button(container, SWT.CHECK);
		fChkPreviewChart.setText(Messages.tour_merger_chk_preview_graphs);
		fChkPreviewChart.setToolTipText(Messages.tour_merger_chk_preview_graphs_tooltip);
		fChkPreviewChart.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				fIsDirtyDisabled = true;
				onSelectAdjustmentSettings();
				fIsDirtyDisabled = false;
			}
		});
	}

	private void createUISectionSaveActions(final Composite parent) {

		/*
		 * group: save options
		 */
		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.tour_merger_group_save_actions);
		group.setToolTipText(Messages.tour_merger_group_save_actions_tooltip);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.BEGINNING).applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(group);

		/*
		 * checkbox: merge temperature
		 */
		fChkMergeTemperature = new Button(group, SWT.CHECK);
		fChkMergeTemperature.setText(Messages.tour_merger_chk_merge_temperature);
		fChkMergeTemperature.setToolTipText(Messages.tour_merger_chk_merge_temperature_tooltip);

		/*
		 * checkbox: adjust start altitude
		 */
		fChkAdjustStartAltitude = new Button(group, SWT.CHECK);
		fChkAdjustStartAltitude.setText(Messages.tour_merger_chk_adjust_start_altitude);
		fChkAdjustStartAltitude.setToolTipText(Messages.tour_merger_chk_adjust_start_altitude_tooltip);
		fChkAdjustStartAltitude.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelectAdjustmentSettings();
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

		fTourChart.addSliderMoveListener(new ISliderMoveListener() {
			public void sliderMoved(final SelectionChartInfo chartInfo) {
				if (fIsChartUpdated) {
					return;
				}
				onSelectAdjustmentSettings();
			}
		});
	}

	private void enableActions() {

		// enable/disable save button
		getButton(IDialogConstants.OK_ID).setEnabled(fIsTourDirty);

		if (fFromTourData.temperatureSerie == null) {
			fChkMergeTemperature.setSelection(false);
			fChkMergeTemperature.setEnabled(false);
		}

		if (fIntoTourData.distanceSerie == null) {
			fChkAdjustStartAltitude.setSelection(false);
			fChkAdjustStartAltitude.setEnabled(false);
		}
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		// keep window size and position
		return fDialogSettings;
	}

	@Override
	protected void okPressed() {

		if (fIsTourDirty) {

			if (fChkMergeTemperature.getSelection()) {
				// temperature is already merged
			} else {
				// restore temperature values because temperature should not be saved
				fIntoTourData.temperatureSerie = fBackupIntoTemperatureSerie;
			}

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

		final float localAltiDiff1 = altiDiff1 / UI.UNIT_VALUE_ALTITUDE;
		final float localAltiDiff10 = altiDiff10 / UI.UNIT_VALUE_ALTITUDE;

		fLabelAltitudeDiff1.setText(Integer.toString((int) localAltiDiff1) + UI.SPACE + UI.UNIT_LABEL_ALTITUDE);
		fLabelAltitudeDiff10.setText(Integer.toString((int) localAltiDiff10) + UI.SPACE + UI.UNIT_LABEL_ALTITUDE);

		fLabelAdjustSecondsValue.setText(Integer.toString(seconds));
		fLabelAdjustMinuteValue.setText(Integer.toString(minutes));
		fLabelAdjustHourValue.setText(Integer.toString(hours));

		fIntoTourData.setMergedTourTimeOffset(hours * 3600 + minutes * 60 + seconds);
		fIntoTourData.setMergedAltitudeOffset(altiDiff1 + altiDiff10);

		computeMergedData();

		fTourChartConfig.isRelativeAltiDiffScaling = fChkAltiDiffScaling.getSelection();

		if (fChkPreviewChart.getSelection()) {
			// update chart
			updateTourChart();
		} else {
			// update only the merge layer, this is much faster
			fTourChart.updateMergeLayer(true);
		}
	}

	private void onSelectResetAdjustments() {

		fScaleAdjustSeconds.setSelection(MAX_ADJUST_SECONDS);
		fScaleAdjustMinutes.setSelection(MAX_ADJUST_MINUTES);
		fScaleAdjustHours.setSelection(MAX_ADJUST_HOURS);
		fScaleAltitude1.setSelection(MAX_ADJUST_ALTITUDE_1);
		fScaleAltitude10.setSelection(MAX_ADJUST_ALTITUDE_10);

		onSelectAdjustmentSettings();
	}

	private void onSelectResetValues() {

		/*
		 * get original data from the backuped data
		 */
		fFromTourData.timeSerie = createDataSerieBackup(fBackupFromTimeSerie);
		fFromTourData.distanceSerie = createDataSerieBackup(fBackupFromDistanceSerie);
		fFromTourData.altitudeSerie = createDataSerieBackup(fBackupFromAltitudeSerie);

		fIntoTourData.temperatureSerie = createDataSerieBackup(fBackupIntoTemperatureSerie);

		fIntoTourData.setMergedTourTimeOffset(fBackupIntoTimeOffset);
		fIntoTourData.setMergedAltitudeOffset(fBackupIntoAltitudeOffset);

		updateUIFromTourData();

		fIsTourDirty = false;

		enableActions();

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

		fChkPreviewChart.setSelection(prefStore.getBoolean(ITourbookPreferences.MERGE_TOUR_PREVIEW_CHART));
		fChkAltiDiffScaling.setSelection(prefStore.getBoolean(ITourbookPreferences.MERGE_TOUR_ALTITUDE_DIFF_SCALING));

		fChkMergeTemperature.setSelection(prefStore.getBoolean(ITourbookPreferences.MERGE_TOUR_MERGE_TEMPERATURE));
		fChkAdjustStartAltitude.setSelection(prefStore.getBoolean(ITourbookPreferences.MERGE_TOUR_ADJUST_START_ALTITUDE));
	}

	private void saveState() {

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		prefStore.setValue(ITourbookPreferences.MERGE_TOUR_GRAPH_X_AXIS, fTourChartConfig.showTimeOnXAxis
				? TourManager.X_AXIS_TIME
				: TourManager.X_AXIS_DISTANCE);

		prefStore.setValue(ITourbookPreferences.MERGE_TOUR_PREVIEW_CHART, fChkPreviewChart.getSelection());
		prefStore.setValue(ITourbookPreferences.MERGE_TOUR_ALTITUDE_DIFF_SCALING, fChkAltiDiffScaling.getSelection());

		prefStore.setValue(ITourbookPreferences.MERGE_TOUR_MERGE_TEMPERATURE, fChkMergeTemperature.getSelection());
		prefStore.setValue(ITourbookPreferences.MERGE_TOUR_ADJUST_START_ALTITUDE,
				fChkAdjustStartAltitude.getSelection());
	}

	private void setTourDirty() {

		if (fIsDirtyDisabled) {
			return;
		}

		fIsTourDirty = true;

		enableActions();
	}

	private void updateTourChart() {

		fIsChartUpdated = true;

		fTourChart.updateTourChart(fIntoTourData, fTourChartConfig, true);

		fIsChartUpdated = false;
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
		final float altitudeOffset = mergedMetricAltitudeOffset;
		final int altitudeOffset1 = (int) (altitudeOffset % 10);
		final int altitudeOffset10 = (int) (altitudeOffset / 10);

		fScaleAltitude1.setSelection(altitudeOffset1 + MAX_ADJUST_ALTITUDE_1);
		fScaleAltitude10.setSelection(altitudeOffset10 + MAX_ADJUST_ALTITUDE_10);

		onSelectAdjustmentSettings();

		fIsDirtyDisabled = false;
	}

}
