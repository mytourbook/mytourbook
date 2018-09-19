/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.rawData;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ISliderMoveListener;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.IDataModelListener;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider2;
import net.tourbook.ui.action.ActionSetTourTypeMenu;
import net.tourbook.ui.tourChart.ChartLayer2ndAltiSerie;
import net.tourbook.ui.tourChart.I2ndAltiLayer;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.tourChart.TourChartConfiguration;

//import net.tourbook.ui.UI;

public class DialogMergeTours extends TitleAreaDialog implements ITourProvider2, I2ndAltiLayer {

	private static final int											MAX_ADJUST_SECONDS		= 120;
	private static final int											MAX_ADJUST_MINUTES		= 120;										// x 60
	private static final int											MAX_ADJUST_ALTITUDE_1	= 20;
	private static final int											MAX_ADJUST_ALTITUDE_10	= 40;											// x 10

	private static final int											VH_SPACING					= 2;

	private final IPreferenceStore									_prefStore					= TourbookPlugin
			.getDefault()
			.getPreferenceStore();

	private final IDialogSettings										_dialogSettings;

	private TourData														_sourceTour;
	private TourData														_targetTour;

	private Composite														_dlgContainer;

	private TourChart														_tourChart;
	private TourChartConfiguration									_tourChartConfig;

	private PixelConverter												_pc;

	/*
	 * vertical adjustment options
	 */
	private Group															_groupAltitude;

	private Label															_lblAltitudeDiff1;
	private Label															_lblAltitudeDiff10;

	private Scale															_scaleAltitude1;
	private Scale															_scaleAltitude10;

	/*
	 * horzontal adjustment options
	 */
	private Button															_chkSynchStartTime;

	private Label															_lblAdjustMinuteValue;
	private Label															_lblAdjustSecondsValue;

	private Scale															_scaleAdjustMinutes;
	private Scale															_scaleAdjustSeconds;

	private Label															_lblAdjustMinuteUnit;
	private Label															_lblAdjustSecondsUnit;

	private Button															_btnResetAdjustment;
	private Button															_btnResetValues;

	/*
	 * save actions
	 */
	private Button															_chkMergeAltitude;
	private Button															_chkMergePulse;
	private Button															_chkMergeTemperature;
	private Button															_chkMergeCadence;

	private Button															_chkAdjustAltiFromSource;
	private Button															_chkAdjustAltiSmoothly;

	private Button															_chkAdjustAltiFromStart;
	private Label															_lblAdjustAltiValueTimeUnit;
	private Label															_lblAdjustAltiValueDistanceUnit;
	private Label															_lblAdjustAltiValueTime;
	private Label															_lblAdjustAltiValueDistance;

	private Button															_chkSetTourType;
	private Link															_linkTourType;
	private CLabel															_lblTourType;

	private Button															_chkKeepHVAdjustments;

	/*
	 * display actions
	 */
	private Button															_chkValueDiffScaling;

	private Button															_chkPreviewChart;

	private boolean														_isTourSaved				= false;
	private boolean														_isMergeSourceTourModified;
	private boolean														_isChartUpdated;

	/*
	 * backup data
	 */
	private int[]															_backupSourceTimeSerie;
	private float[]														_backupSourceDistanceSerie;
	private float[]														_backupSourceAltitudeSerie;

	private TourType														_backupSourceTourType;

	private float[]														_backupTargetPulseSerie;
	private float[]														_backupTargetTemperatureSerie;
	private float[]														_backupTargetCadenceSerie;

	private int																_backupTargetTimeOffset;
	private int																_backupTargetAltitudeOffset;

	private ActionOpenPrefDialog										_actionOpenTourTypePrefs;

	private final NumberFormat											_nf							= NumberFormat.getNumberInstance();

	private final Image													_iconPlaceholder;
	private final HashMap<Integer, Image>							_graphImages				= new HashMap<>();

	private final int														_tourStartTimeSynchOffset;
	private int																_tourTimeOffsetBackup;

	private boolean														_isAdjustAltiFromSourceBackup;
	private boolean														_isAdjustAltiFromStartBackup;

	private org.eclipse.jface.util.IPropertyChangeListener	_prefChangeListener;

	/**
	 * @param parentShell
	 * @param mergeSourceTour
	 *           {@link TourData} for the tour which is merge into the other tour
	 * @param mergeTargetTour
	 *           {@link TourData} for the tour into which the other tour is merged
	 */
	public DialogMergeTours(final Shell parentShell, final TourData mergeSourceTour, final TourData mergeTargetTour) {

		super(parentShell);

		// make dialog resizable and display maximize button
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);

		// set icon for the window
		setDefaultImage(TourbookPlugin.getImageDescriptor(Messages.image__merge_tours).createImage());

		_iconPlaceholder = TourbookPlugin.getImageDescriptor(Messages.Image__icon_placeholder).createImage();

		_sourceTour = mergeSourceTour;
		_targetTour = mergeTargetTour;

		/*
		 * synchronize start time
		 */

		final long sourceStartTime = _sourceTour.getTourStartTimeMS();
		final long targetStartTime = _targetTour.getTourStartTimeMS();

		_tourStartTimeSynchOffset = (int) ((sourceStartTime - targetStartTime) / 1000);

		_nf.setMinimumFractionDigits(3);
		_nf.setMaximumFractionDigits(3);

		_dialogSettings = TourbookPlugin.getDefault().getDialogSettingsSection(getClass().getName());
	}

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

					/*
					 * tour data cache is cleared, reload tour data from the database
					 */

					if (_sourceTour.getTourPerson() != null) {
						_sourceTour = TourManager.getInstance().getTourData(_sourceTour.getTourId());
					}

					_targetTour = TourManager.getInstance().getTourData(_targetTour.getTourId());

					onModifyProperties();
				}
			}
		};

		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	/**
	 * set button width
	 */
	private void adjustButtonWidth() {

		final int btnResetAdj = _btnResetAdjustment.getBounds().width;
		final int btnResetValues = _btnResetValues.getBounds().width;
		final int newWidth = Math.max(btnResetAdj, btnResetValues);

		GridData gd;
		gd = (GridData) _btnResetAdjustment.getLayoutData();
		gd.widthHint = newWidth;

		gd = (GridData) _btnResetValues.getLayoutData();
		gd.widthHint = newWidth;
	}

	@Override
	public boolean close() {

		saveState();

		if (_isTourSaved == false) {

			// tour is not saved, dialog is canceled, restore original values

			restoreDataBackup();
		}

		/**
		 * this is a tricky thing:
		 * <p>
		 * when the tour is not saved, the tour must be reverted
		 * <p>
		 * when the tour is saved, reverting the tour sets the editor to not dirty, tour data have
		 * already been saved
		 */
		if (_isMergeSourceTourModified) {

			// revert modified tour type in the merge from tour

			final TourEvent tourEvent = new TourEvent(_sourceTour);
			tourEvent.isReverted = true;

			TourManager.fireEvent(TourEventId.TOUR_CHANGED, tourEvent);
		}

		return super.close();
	}

	private void computeMergedData() {

		int xMergeOffset = _targetTour.getMergedTourTimeOffset();
		final int yMergeOffset = _targetTour.getMergedAltitudeOffset();

		final int[] targetTimeSerie = _targetTour.timeSerie;
		final float[] targetDistanceSerie = _targetTour.distanceSerie;
		final float[] targetAltitudeSerie = _targetTour.altitudeSerie;

		final int[] sourceTimeSerie = _sourceTour.timeSerie;
		final float[] sourceAltitudeSerie = _sourceTour.altitudeSerie;
		final float[] sourcePulseSerie = _sourceTour.pulseSerie;
		final float[] sourceTemperatureSerie = _sourceTour.temperatureSerie;
		final float[] sourceCadenceSerie = _sourceTour.getCadenceSerie();

		if (_chkSynchStartTime.getSelection()) {

			// synchronize start time

			xMergeOffset = _tourStartTimeSynchOffset;
		}

		// check if the data series are available
		final boolean isSourceTemperature = sourceTemperatureSerie != null;
		final boolean isSourcePulse = sourcePulseSerie != null;
		final boolean isSourceCadence = sourceCadenceSerie != null;
		final boolean isSourceAltitude = sourceAltitudeSerie != null;

		final boolean isTargetDistance = targetDistanceSerie != null;
		final boolean isTargetAltitude = targetAltitudeSerie != null;

		final int lastSourceIndex = sourceTimeSerie.length - 1;
		final int serieLength = targetTimeSerie.length;

		final float[] newSourceAltitudeSerie = new float[serieLength];
		final float[] newSourceAltiDiffSerie = new float[serieLength];

		final float[] newTargetPulseSerie = new float[serieLength];
		final float[] newTargetTemperatureSerie = new float[serieLength];
		final float[] newTargetCadenceSerie = new float[serieLength];

		int sourceIndex = 0;
		int sourceTime = sourceTimeSerie[0] + xMergeOffset;
		int sourceTimePrev = 0;
		float sourceAlti = 0;
		float sourceAltiPrev = 0;

		int targetTime = targetTimeSerie[0];
		float newSourceAltitude;

		if (isSourceAltitude) {
			sourceAlti = sourceAltitudeSerie[0] + yMergeOffset;
			sourceAltiPrev = sourceAlti;
			newSourceAltitude = sourceAlti;
		}

		final boolean isLinearInterpolation = _chkAdjustAltiFromSource.getSelection()
				&& _chkAdjustAltiSmoothly.getSelection();

		/*
		 * create new time/distance serie for the source tour according to the time of the target tour
		 */
		for (int targetIndex = 0; targetIndex < serieLength; targetIndex++) {

			targetTime = targetTimeSerie[targetIndex];

			/*
			 * target tour is the leading time data serie, move source time forward to reach target
			 * time
			 */
			while (sourceTime < targetTime) {

				sourceIndex++;

				// check array bounds
				sourceIndex = (sourceIndex <= lastSourceIndex) ? sourceIndex : lastSourceIndex;

				if (sourceIndex == lastSourceIndex) {
					//prevent endless loops
					break;
				}

				sourceTimePrev = sourceTime;
				sourceTime = sourceTimeSerie[sourceIndex] + xMergeOffset;

				if (isSourceAltitude) {
					sourceAltiPrev = sourceAlti;
					sourceAlti = sourceAltitudeSerie[sourceIndex] + yMergeOffset;
				}
			}

			if (isSourceAltitude) {

				if (isLinearInterpolation) {

					/**
					 * do linear interpolation for the altitude
					 * <p>
					 * y2 = (x2-x1)(y3-y1)/(x3-x1) + y1
					 */
					final int x1 = sourceTimePrev;
					final int x2 = targetTime;
					final int x3 = sourceTime;
					final float y1 = sourceAltiPrev;
					final float y3 = sourceAlti;

					final int xDiff = x3 - x1;

					newSourceAltitude = xDiff == 0 ? sourceAltiPrev : (x2 - x1) * (y3 - y1) / xDiff + y1;

				} else {

					/*
					 * the interpolited altitude is not exact above the none interpolite altitude, it is
					 * in the middle of the previous and current altitude
					 */
					// newSourceAltitude = sourceAlti;
					newSourceAltitude = sourceAltiPrev;
				}

				newSourceAltitudeSerie[targetIndex] = newSourceAltitude;

				if (isTargetAltitude) {
					newSourceAltiDiffSerie[targetIndex] = newSourceAltitude - targetAltitudeSerie[targetIndex];
				}
			}

			if (isSourcePulse) {
				newTargetPulseSerie[targetIndex] = sourcePulseSerie[sourceIndex];
			}
			if (isSourceTemperature) {
				newTargetTemperatureSerie[targetIndex] = sourceTemperatureSerie[sourceIndex];
			}
			if (isSourceCadence) {
				newTargetCadenceSerie[targetIndex] = sourceCadenceSerie[sourceIndex];
			}
		}

		_sourceTour.dataSerieAdjustedAlti = null;

		if (isSourceAltitude) {
			_sourceTour.dataSerie2ndAlti = newSourceAltitudeSerie;
		} else {
			_sourceTour.dataSerie2ndAlti = null;
		}

		if (isSourceAltitude && isTargetAltitude) {
			_sourceTour.dataSerieDiffTo2ndAlti = newSourceAltiDiffSerie;
		} else {
			_sourceTour.dataSerieDiffTo2ndAlti = null;
		}

		if (_chkMergePulse.getSelection()) {
			_targetTour.pulseSerie = newTargetPulseSerie;
		} else {
			_targetTour.pulseSerie = _backupTargetPulseSerie;
		}

		if (_chkMergeTemperature.getSelection()) {
			_targetTour.temperatureSerie = newTargetTemperatureSerie;
		} else {
			_targetTour.temperatureSerie = _backupTargetTemperatureSerie;
		}

		if (_chkMergeCadence.getSelection()) {
			_targetTour.setCadenceSerie(newTargetCadenceSerie);
		} else {
			_targetTour.setCadenceSerie(_backupTargetCadenceSerie);
		}

		float altiDiffTime = 0;
		float altiDiffDist = 0;

		if (isSourceAltitude && isTargetAltitude && isTargetDistance) {

			/*
			 * compute adjusted altitude
			 */

			if (_chkAdjustAltiFromStart.getSelection()) {

				/*
				 * adjust start altitude until left slider
				 */

				final float[] adjustedTargetAltitudeSerie = new float[serieLength];

				float startAltiDiff = newSourceAltiDiffSerie[0];
				final int endIndex = _tourChart.getXSliderPosition().getLeftSliderValueIndex();
				final float distanceDiff = targetDistanceSerie[endIndex];

				final float[] altitudeSerie = _targetTour.altitudeSerie;

				for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

					if (serieIndex < endIndex) {

						// add adjusted altitude

						final float targetDistance = targetDistanceSerie[serieIndex];
						final float distanceScale = 1 - targetDistance / distanceDiff;

						final float adjustedAltiDiff = startAltiDiff * distanceScale;
						final float newAltitude = altitudeSerie[serieIndex] + adjustedAltiDiff;

						adjustedTargetAltitudeSerie[serieIndex] = newAltitude;
						newSourceAltiDiffSerie[serieIndex] = newSourceAltitudeSerie[serieIndex] - newAltitude;

					} else {

						// add altitude which are not adjusted

						adjustedTargetAltitudeSerie[serieIndex] = altitudeSerie[serieIndex];
					}
				}

				_sourceTour.dataSerieAdjustedAlti = adjustedTargetAltitudeSerie;

				startAltiDiff /= net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE;

				final int targetEndTime = targetTimeSerie[endIndex];
				final float targetEndDistance = targetDistanceSerie[endIndex];

				// meter/min
				altiDiffTime = targetEndTime == 0 ? //
						0f
						: startAltiDiff / targetEndTime * 60;

				// meter/meter
				altiDiffDist = targetEndDistance == 0 ? //
						0f
						: ((startAltiDiff * 1000) / targetEndDistance) / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

			} else if (_chkAdjustAltiFromSource.getSelection()) {

				/*
				 * adjust target altitude from source altitude
				 */
				final float[] newTargetAltitudeSerie = new float[serieLength];

				for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {
					newTargetAltitudeSerie[serieIndex] = newSourceAltitudeSerie[serieIndex];
				}

				_sourceTour.dataSerieAdjustedAlti = newTargetAltitudeSerie;
			}
		}

		updateUI(altiDiffTime, altiDiffDist);
	}

	@Override
	protected void configureShell(final Shell shell) {

		super.configureShell(shell);

		shell.setText(Messages.tour_merger_dialog_title);

		shell.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
			}
		});
	}

	@Override
	public void create() {

		createDataBackup();
		addPrefListener();

		// create UI widgets
		super.create();

		adjustButtonWidth();
		_dlgContainer.layout(true, true);

		createActions();
		restoreState();

		setTitle(NLS.bind(
				Messages.tour_merger_dialog_header_title,
				TourManager.getTourTitle(_targetTour),
				_targetTour.getDeviceName()));

		setMessage(NLS.bind(
				Messages.tour_merger_dialog_header_message,
				TourManager.getTourTitle(_sourceTour),
				_sourceTour.getDeviceName()));

		// must be done before the merged data are computed
		enableGraphActions();
		setMergedGraphsVisible();

		// set alti diff scaling
		_tourChartConfig.isRelativeValueDiffScaling = _chkValueDiffScaling.getSelection();
		_tourChart.updateLayer2ndAlti(this, true);

		updateUIFromTourData();

		if (_chkSynchStartTime.getSelection()) {
			_tourTimeOffsetBackup = _targetTour.getMergedTourTimeOffset();
			updateUITourTimeOffset(_tourStartTimeSynchOffset);
		}

		// update chart after the UI is updated from the tour
		updateTourChart();

		enableActions();

	}

	@Override
	public ChartLayer2ndAltiSerie create2ndAltiLayer() {

		if (_targetTour == null) {
			return null;
		}

		final TourData mergeSourceTourData = _targetTour.getMergeSourceTourData();

		if (_targetTour.getMergeSourceTourId() == null && mergeSourceTourData == null) {
			return null;
		}

		TourData layerTourData;

		if (mergeSourceTourData != null) {
			layerTourData = mergeSourceTourData;
		} else {
			layerTourData = TourManager.getInstance().getTourData(_targetTour.getMergeSourceTourId());
		}

		if (layerTourData == null) {
			return null;
		}

		final double[] xDataSerie = _tourChartConfig.isShowTimeOnXAxis ? //
				_targetTour.getTimeSerieDouble()
				: _targetTour.getDistanceSerieDouble();

		return new ChartLayer2ndAltiSerie(layerTourData, xDataSerie, _tourChartConfig, null);
	}

	private void createActions() {

		_actionOpenTourTypePrefs = new ActionOpenPrefDialog(
				Messages.action_tourType_modify_tourTypes,
				ITourbookPreferences.PREF_PAGE_TOUR_TYPE);
	}

	@Override
	protected final void createButtonsForButtonBar(final Composite parent) {

		super.createButtonsForButtonBar(parent);

		// rename OK button
		final Button buttonOK = getButton(IDialogConstants.OK_ID);
		buttonOK.setText(Messages.tour_merger_save_target_tour);

		setButtonLayoutData(buttonOK);
	}

	private void createDataBackup() {

		/*
		 * keep a backup of the altitude data because these data will be changed in this dialog
		 */
		_backupSourceTimeSerie = Util.createIntegerCopy(_sourceTour.timeSerie);
		_backupSourceDistanceSerie = Util.createFloatCopy(_sourceTour.distanceSerie);
		_backupSourceAltitudeSerie = Util.createFloatCopy(_sourceTour.altitudeSerie);
		_backupSourceTourType = _sourceTour.getTourType();

		_backupTargetPulseSerie = Util.createFloatCopy(_targetTour.pulseSerie);
		_backupTargetTemperatureSerie = Util.createFloatCopy(_targetTour.temperatureSerie);
		_backupTargetCadenceSerie = Util.createFloatCopy(_targetTour.getCadenceSerie());

		_backupTargetTimeOffset = _targetTour.getMergedTourTimeOffset();
		_backupTargetAltitudeOffset = _targetTour.getMergedAltitudeOffset();
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		_dlgContainer = (Composite) super.createDialogArea(parent);

		createUI(_dlgContainer);

		return _dlgContainer;
	}

	private void createUI(final Composite parent) {

		_pc = new PixelConverter(parent);

		final Composite dlgContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(dlgContainer);
		GridLayoutFactory.fillDefaults().margins(10, 0).numColumns(1).applyTo(dlgContainer);

		createUITourChart(dlgContainer);

		// column container
		final Composite columnContainer = new Composite(dlgContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(columnContainer);
		GridLayoutFactory.fillDefaults().numColumns(3).spacing(10, 0).applyTo(columnContainer);

		/*
		 * column: options
		 */
		final Composite columnOptions = new Composite(columnContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(columnOptions);
		GridLayoutFactory.fillDefaults().margins(0, 0).numColumns(1).applyTo(columnOptions);

		createUISectionSaveActions(columnOptions);

		/*
		 * column: display/adjustments
		 */
		final Composite columnDisplay = new Composite(columnContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(columnDisplay);
		GridLayoutFactory.fillDefaults().margins(0, 0).numColumns(1).applyTo(columnDisplay);

		createUISectionAdjustments(columnDisplay);

		createUISectionResetButtons(columnContainer);

	}

	/**
	 * group: adjust time
	 */
	private void createUIGroupHorizAdjustment(final Composite parent) {

		final int valueWidth = _pc.convertWidthInCharsToPixels(4);
		Label label;

		final Group groupTime = new Group(parent, SWT.NONE);
		groupTime.setText(Messages.tour_merger_group_adjust_time);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(groupTime);
		GridLayoutFactory.swtDefaults()//
				.numColumns(1)
				.spacing(VH_SPACING, VH_SPACING)
				.applyTo(groupTime);

		/*
		 * checkbox: keep horiz. and vert. adjustments
		 */
		_chkSynchStartTime = new Button(groupTime, SWT.CHECK);
		GridDataFactory.fillDefaults().applyTo(_chkSynchStartTime);
		_chkSynchStartTime.setText(Messages.tour_merger_chk_use_synced_start_time);
		_chkSynchStartTime.setToolTipText(Messages.tour_merger_chk_use_synced_start_time_tooltip);
		_chkSynchStartTime.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				// display synched time in the UI
				if (_chkSynchStartTime.getSelection()) {

					// set time offset for the synched tours

					_tourTimeOffsetBackup = getFromUITourTimeOffset();
					updateUITourTimeOffset(_tourStartTimeSynchOffset);

				} else {

					// set time offset manually

					updateUITourTimeOffset(_tourTimeOffsetBackup);
				}

				onModifyProperties();

				if (_chkPreviewChart.getSelection() == false) {

					// preview
					updateTourChart();
				}
			}
		});

		/*
		 * container: seconds scale
		 */
		final Composite timeContainer = new Composite(groupTime, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(timeContainer);
		GridLayoutFactory.fillDefaults().numColumns(4).spacing(0, 0).applyTo(timeContainer);

		/*
		 * scale: adjust seconds
		 */
		_lblAdjustSecondsValue = new Label(timeContainer, SWT.TRAIL);
		GridDataFactory
				.fillDefaults()
				.align(SWT.END, SWT.CENTER)
				.hint(valueWidth, SWT.DEFAULT)
				.applyTo(_lblAdjustSecondsValue);

		label = new Label(timeContainer, SWT.NONE);
		label.setText(UI.SPACE1);

		_lblAdjustSecondsUnit = new Label(timeContainer, SWT.NONE);
		_lblAdjustSecondsUnit.setText(Messages.tour_merger_label_adjust_seconds);

		_scaleAdjustSeconds = new Scale(timeContainer, SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(_scaleAdjustSeconds);
		_scaleAdjustSeconds.setMinimum(0);
		_scaleAdjustSeconds.setMaximum(MAX_ADJUST_SECONDS * 2);
		_scaleAdjustSeconds.setPageIncrement(20);
		_scaleAdjustSeconds.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyProperties();
			}
		});
		_scaleAdjustSeconds.addListener(SWT.MouseDoubleClick, new Listener() {
			@Override
			public void handleEvent(final Event event) {
				onScaleDoubleClick(event.widget);
			}
		});

		/*
		 * scale: adjust minutes
		 */
		_lblAdjustMinuteValue = new Label(timeContainer, SWT.TRAIL);
		GridDataFactory
				.fillDefaults()
				.align(SWT.END, SWT.CENTER)
				.hint(valueWidth, SWT.DEFAULT)
				.applyTo(_lblAdjustMinuteValue);

		label = new Label(timeContainer, SWT.NONE);
		label.setText(UI.SPACE1);

		_lblAdjustMinuteUnit = new Label(timeContainer, SWT.NONE);
		_lblAdjustMinuteUnit.setText(Messages.tour_merger_label_adjust_minutes);

		_scaleAdjustMinutes = new Scale(timeContainer, SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(_scaleAdjustMinutes);
		_scaleAdjustMinutes.setMinimum(0);
		_scaleAdjustMinutes.setMaximum(MAX_ADJUST_MINUTES * 2);
		_scaleAdjustMinutes.setPageIncrement(20);
		_scaleAdjustMinutes.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyProperties();
			}
		});
		_scaleAdjustMinutes.addListener(SWT.MouseDoubleClick, new Listener() {
			@Override
			public void handleEvent(final Event event) {
				onScaleDoubleClick(event.widget);
			}
		});
	}

	/**
	 * group: adjust altitude
	 */
	private void createUIGroupVertAdjustment(final Composite parent) {

		_groupAltitude = new Group(parent, SWT.NONE);
		_groupAltitude.setText(Messages.tour_merger_group_adjust_altitude);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.BEGINNING).applyTo(_groupAltitude);
		GridLayoutFactory.swtDefaults().numColumns(4)
//				.extendedMargins(0, 0, 0, 0)
//				.spacing(0, 0)
				.spacing(VH_SPACING, VH_SPACING)
				.applyTo(_groupAltitude);

		/*
		 * scale: altitude 20m
		 */
		_lblAltitudeDiff1 = new Label(_groupAltitude, SWT.TRAIL);
		GridDataFactory
				.fillDefaults()
				.align(SWT.END, SWT.CENTER)
				.hint(_pc.convertWidthInCharsToPixels(8), SWT.DEFAULT)
				.applyTo(_lblAltitudeDiff1);

		_scaleAltitude1 = new Scale(_groupAltitude, SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(_scaleAltitude1);
		_scaleAltitude1.setMinimum(0);
		_scaleAltitude1.setMaximum(MAX_ADJUST_ALTITUDE_1 * 2);
		_scaleAltitude1.setPageIncrement(5);
		_scaleAltitude1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyProperties();
			}
		});
		_scaleAltitude1.addListener(SWT.MouseDoubleClick, new Listener() {
			@Override
			public void handleEvent(final Event event) {
				onScaleDoubleClick(event.widget);
			}
		});

		/*
		 * scale: altitude 100m
		 */
		_lblAltitudeDiff10 = new Label(_groupAltitude, SWT.TRAIL);
		GridDataFactory
				.fillDefaults()
				.align(SWT.END, SWT.CENTER)
				.hint(_pc.convertWidthInCharsToPixels(8), SWT.DEFAULT)
				.applyTo(_lblAltitudeDiff10);

		_scaleAltitude10 = new Scale(_groupAltitude, SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(_scaleAltitude10);
		_scaleAltitude10.setMinimum(0);
		_scaleAltitude10.setMaximum(MAX_ADJUST_ALTITUDE_10 * 2);
		_scaleAltitude10.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyProperties();
			}
		});
		_scaleAltitude10.addListener(SWT.MouseDoubleClick, new Listener() {
			@Override
			public void handleEvent(final Event event) {
				onScaleDoubleClick(event.widget);
			}
		});
	}

	private Button createUIMergeAction(	final Composite parent,
													final int graphId,
													final String btnText,
													final String btnTooltip,
													final String imageEnabled,
													final boolean isEnabled) {

		final Button mergeButton = new Button(parent, SWT.CHECK);
		mergeButton.setText(btnText);
		mergeButton.setToolTipText(btnTooltip);

		mergeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelectMergeGraph(e);
			}
		});

		if (isEnabled) {
			final Image image = TourbookPlugin.getImageDescriptor(imageEnabled).createImage();
			_graphImages.put(graphId, image);
			mergeButton.setImage(image);
		} else {
			mergeButton.setImage(_iconPlaceholder);
		}

		return mergeButton;
	}

	private void createUISectionAdjustments(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);

		createUIGroupHorizAdjustment(container);
		createUIGroupVertAdjustment(container);
		createUISectionDisplayOptions(container);
	}

	private void createUISectionDisplayOptions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.numColumns(1)
				.spacing(VH_SPACING, VH_SPACING)
				.applyTo(container);

		/*
		 * checkbox: display relative or absolute scale
		 */
		_chkValueDiffScaling = new Button(container, SWT.CHECK);
		GridDataFactory.swtDefaults()/* .indent(5, 5) .span(4, 1) */.applyTo(_chkValueDiffScaling);
		_chkValueDiffScaling.setText(Messages.tour_merger_chk_alti_diff_scaling);
		_chkValueDiffScaling.setToolTipText(Messages.tour_merger_chk_alti_diff_scaling_tooltip);
		_chkValueDiffScaling.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyProperties();
			}
		});

		/*
		 * checkbox: preview chart
		 */
		_chkPreviewChart = new Button(container, SWT.CHECK);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(_chkPreviewChart);
		_chkPreviewChart.setText(Messages.tour_merger_chk_preview_graphs);
		_chkPreviewChart.setToolTipText(Messages.tour_merger_chk_preview_graphs_tooltip);
		_chkPreviewChart.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				if (_chkPreviewChart.getSelection()) {

					setMergedGraphsVisible();

					onModifyProperties();
					updateTourChart();
				}
			}
		});
	}

	private void createUISectionResetButtons(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).align(SWT.END, SWT.FILL).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);

		/*
		 * button: reset all adjustment options
		 */
		_btnResetAdjustment = new Button(container, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(_btnResetAdjustment);
		_btnResetAdjustment.setText(Messages.tour_merger_btn_reset_adjustment);
		_btnResetAdjustment.setToolTipText(Messages.tour_merger_btn_reset_adjustment_tooltip);
		_btnResetAdjustment.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelectResetAdjustments();
			}
		});

		/*
		 * button: show original values
		 */
		_btnResetValues = new Button(container, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(_btnResetValues);
		_btnResetValues.setText(Messages.tour_merger_btn_reset_values);
		_btnResetValues.setToolTipText(Messages.tour_merger_btn_reset_values_tooltip);
		_btnResetValues.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelectResetValues();
			}
		});
	}

	/**
	 * @param parent
	 */
	private void createUISectionSaveActions(final Composite parent) {

		final int indentOption = _pc.convertHorizontalDLUsToPixels(20);
		final int indentOption2 = _pc.convertHorizontalDLUsToPixels(13);

		/*
		 * group: save options
		 */
		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.tour_merger_group_save_actions);
		group.setToolTipText(Messages.tour_merger_group_save_actions_tooltip);
		GridDataFactory.swtDefaults()//
				.grab(true, false)
				.align(SWT.BEGINNING, SWT.END)
				.applyTo(group);
		GridLayoutFactory.swtDefaults()//
				.spacing(VH_SPACING, VH_SPACING)
				.applyTo(group);

		/*
		 * checkbox: merge pulse
		 */
		_chkMergePulse = createUIMergeAction(
				group,
				TourManager.GRAPH_PULSE,
				Messages.merge_tour_source_graph_heartbeat,
				Messages.merge_tour_source_graph_heartbeat_tooltip,
				Messages.Image__graph_heartbeat,
				_sourceTour.pulseSerie != null);

		/*
		 * checkbox: merge temperature
		 */
		_chkMergeTemperature = createUIMergeAction(
				group,
				TourManager.GRAPH_TEMPERATURE,
				Messages.merge_tour_source_graph_temperature,
				Messages.merge_tour_source_graph_temperature_tooltip,
				Messages.Image__graph_temperature,
				_sourceTour.temperatureSerie != null);

		/*
		 * checkbox: merge cadence
		 */
		_chkMergeCadence = createUIMergeAction(
				group,
				TourManager.GRAPH_CADENCE,
				Messages.merge_tour_source_graph_cadence,
				Messages.merge_tour_source_graph_cadence_tooltip,
				Messages.Image__graph_cadence,
				_sourceTour.getCadenceSerie() != null);

		/*
		 * checkbox: merge altitude
		 */
		_chkMergeAltitude = createUIMergeAction(
				group,
				TourManager.GRAPH_ALTITUDE,
				Messages.merge_tour_source_graph_altitude,
				Messages.merge_tour_source_graph_altitude_tooltip,
				Messages.Image__graph_altitude,
				_sourceTour.altitudeSerie != null);

		/*
		 * container: merge altitude
		 */
		final Composite containerMergeGraph = new Composite(group, SWT.NONE);
		GridDataFactory.fillDefaults().indent(indentOption + 16, 0).applyTo(containerMergeGraph);
		GridLayoutFactory.fillDefaults()//
				.spacing(VH_SPACING, VH_SPACING)
				.applyTo(containerMergeGraph);

		/*
		 * checkbox: adjust altitude from source
		 */
		_chkAdjustAltiFromSource = new Button(containerMergeGraph, SWT.RADIO);
		_chkAdjustAltiFromSource.setText(Messages.tour_merger_chk_adjust_altitude_from_source);
		_chkAdjustAltiFromSource.setToolTipText(Messages.tour_merger_chk_adjust_altitude_from_source_tooltip);
//		fChkAdjustAltiFromSource.setImage(fIconPlaceholder);

		_chkAdjustAltiFromSource.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				if (_chkAdjustAltiFromSource.getSelection()) {

					// check one adjust altitude
					if (_chkAdjustAltiFromSource.getSelection() == false
							&& _chkAdjustAltiFromStart.getSelection() == false) {
						_chkAdjustAltiFromSource.setSelection(true);
					}

					// only one altitude adjustment can be set
					_chkAdjustAltiFromStart.setSelection(false);

					_chkMergeAltitude.setSelection(true);

				} else {

					// disable altitude merge option
					_chkMergeAltitude.setSelection(false);
				}

				onModifyProperties();
			}
		});

		/*
		 * checkbox: smooth altitude with linear interpolation
		 */
		_chkAdjustAltiSmoothly = new Button(containerMergeGraph, SWT.CHECK);
		GridDataFactory.fillDefaults().indent(indentOption2, 0).applyTo(_chkAdjustAltiSmoothly);
		_chkAdjustAltiSmoothly.setText(Messages.tour_merger_chk_adjust_altitude_linear_interpolition);
		_chkAdjustAltiSmoothly.setToolTipText(Messages.tour_merger_chk_adjust_altitude_linear_interpolition_tooltip);
		_chkAdjustAltiSmoothly.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyProperties();
			}
		});

		/*
		 * checkbox: adjust start altitude
		 */
		_chkAdjustAltiFromStart = new Button(containerMergeGraph, SWT.RADIO);
		_chkAdjustAltiFromStart.setText(Messages.tour_merger_chk_adjust_start_altitude);
		_chkAdjustAltiFromStart.setToolTipText(Messages.tour_merger_chk_adjust_start_altitude_tooltip);
		_chkAdjustAltiFromStart.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				if (_chkAdjustAltiFromStart.getSelection()) {

					// check one adjust altitude
					if (_chkAdjustAltiFromStart.getSelection() == false
							&& _chkAdjustAltiFromSource.getSelection() == false) {
						_chkAdjustAltiFromStart.setSelection(true);
					}

					// only one altitude adjustment can be done
					_chkAdjustAltiFromSource.setSelection(false);

					_chkMergeAltitude.setSelection(true);

				} else {

					// disable altitude merge option
					_chkMergeAltitude.setSelection(false);
				}

				onModifyProperties();
			}
		});

		/*
		 * altitude adjustment values
		 */
		final Composite aaContainer = new Composite(containerMergeGraph, SWT.NONE);
		GridDataFactory.fillDefaults().indent(indentOption2, 0).applyTo(aaContainer);
		GridLayoutFactory.fillDefaults().numColumns(5).spacing(VH_SPACING, VH_SPACING).applyTo(aaContainer);

		_lblAdjustAltiValueTime = new Label(aaContainer, SWT.TRAIL);
		GridDataFactory
				.fillDefaults()
				.hint(_pc.convertWidthInCharsToPixels(10), SWT.DEFAULT)
				.applyTo(_lblAdjustAltiValueTime);

		_lblAdjustAltiValueTimeUnit = new Label(aaContainer, SWT.NONE);
		_lblAdjustAltiValueTimeUnit.setText(UI.UNIT_LABEL_ALTITUDE + "/min"); //$NON-NLS-1$

		_lblAdjustAltiValueDistance = new Label(aaContainer, SWT.TRAIL);
		GridDataFactory
				.fillDefaults()
				.hint(_pc.convertWidthInCharsToPixels(10), SWT.DEFAULT)
				.applyTo(_lblAdjustAltiValueDistance);

		_lblAdjustAltiValueDistanceUnit = new Label(aaContainer, SWT.NONE);
		_lblAdjustAltiValueDistanceUnit.setText(UI.UNIT_LABEL_ALTITUDE + "/" + UI.UNIT_LABEL_DISTANCE); //$NON-NLS-1$

		/*
		 * checkbox: set tour type
		 */
		_chkSetTourType = new Button(group, SWT.CHECK);
		GridDataFactory.fillDefaults().indent(0, 10).applyTo(_chkSetTourType);
		_chkSetTourType.setText(Messages.tour_merger_chk_set_tour_type);
		_chkSetTourType.setToolTipText(Messages.tour_merger_chk_set_tour_type_tooltip);
		_chkSetTourType.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				enableActions();
			}
		});

		final Composite ttContainer = new Composite(group, SWT.NONE);
		GridDataFactory.fillDefaults().indent(indentOption2, 0).applyTo(ttContainer);
		GridLayoutFactory.fillDefaults()//
				.numColumns(2)
				.spacing(VH_SPACING, VH_SPACING)
				.applyTo(ttContainer);

		/*
		 * tour type
		 */
		_linkTourType = new Link(ttContainer, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_linkTourType);
		_linkTourType.setText(Messages.tour_editor_label_tour_type);
		_linkTourType.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				net.tourbook.common.UI.openControlMenu(_linkTourType);
			}
		});

		/*
		 * tour type menu
		 */
		final MenuManager menuMgr = new MenuManager();

		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(final IMenuManager menuMgr) {

				// set menu items

				ActionSetTourTypeMenu.fillMenu(menuMgr, DialogMergeTours.this, false);

				menuMgr.add(new Separator());
				menuMgr.add(_actionOpenTourTypePrefs);
			}
		});

		// set menu for the tag item
		_linkTourType.setMenu(menuMgr.createContextMenu(_linkTourType));

		/*
		 * label: tour type icon and text
		 */
		_lblTourType = new CLabel(ttContainer, SWT.NONE);
		GridDataFactory.swtDefaults().grab(true, false).applyTo(_lblTourType);

		/*
		 * checkbox: keep horiz. and vert. adjustments
		 */
		_chkKeepHVAdjustments = new Button(group, SWT.CHECK);
		_chkKeepHVAdjustments.setText(Messages.tour_merger_chk_keep_horiz_vert_adjustments);
		_chkKeepHVAdjustments.setToolTipText(Messages.tour_merger_chk_keep_horiz_vert_adjustments_tooltip);
		_chkKeepHVAdjustments.setSelection(true);
		_chkKeepHVAdjustments.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				// this option cannot be deselected
				_chkKeepHVAdjustments.setSelection(true);
			}
		});
	}

	private void createUITourChart(final Composite dlgContainer) {

		_tourChart = new TourChart(dlgContainer, SWT.BORDER, null, _dialogSettings);
		GridDataFactory.fillDefaults().grab(true, true).minSize(300, 200).applyTo(_tourChart);

		_tourChart.setShowZoomActions(true);
		_tourChart.setShowSlider(true);

		// set altitude visible
		_tourChartConfig = new TourChartConfiguration(true);

		// set one visible graph
		int visibleGraph = -1;
		if (_targetTour.altitudeSerie != null) {
			visibleGraph = TourManager.GRAPH_ALTITUDE;
		} else if (_targetTour.pulseSerie != null) {
			visibleGraph = TourManager.GRAPH_PULSE;
		} else if (_targetTour.temperatureSerie != null) {
			visibleGraph = TourManager.GRAPH_TEMPERATURE;
		} else if (_targetTour.getCadenceSerie() != null) {
			visibleGraph = TourManager.GRAPH_CADENCE;
		}
		if (visibleGraph != -1) {
			_tourChartConfig.addVisibleGraph(visibleGraph);
		}

		// overwrite x-axis from pref store
		_tourChartConfig.setIsShowTimeOnXAxis(TourbookPlugin
				.getDefault()
				.getPreferenceStore()
				.getString(ITourbookPreferences.MERGE_TOUR_GRAPH_X_AXIS)
				.equals(TourManager.X_AXIS_TIME));

		_tourChart.addDataModelListener(new IDataModelListener() {

			@Override
			public void dataModelChanged(final ChartDataModel changedChartDataModel) {

				// set title
				changedChartDataModel.setTitle(TourManager.getTourTitleDetailed(_targetTour));
			}
		});

		_tourChart.addSliderMoveListener(new ISliderMoveListener() {
			@Override
			public void sliderMoved(final SelectionChartInfo chartInfo) {

				if (_isChartUpdated) {
					return;
				}

				onModifyProperties();
			}
		});
	}

	private void enableActions() {

		final boolean isMergeAltitude = _chkMergeAltitude.getSelection();
		final boolean isAdjustAltitude = _chkAdjustAltiFromStart.getSelection() && isMergeAltitude;
		final boolean isSetTourType = _chkSetTourType.getSelection();

//		final boolean isMergeActionSelected = isMergeAltitude
//				|| fChkMergePulse.getSelection()
//				|| fChkMergeTemperature.getSelection()
//				|| fChkMergeCadence.getSelection();

		final boolean isAltitudeAvailable = _sourceTour.altitudeSerie != null && _targetTour.altitudeSerie != null;

		final boolean isSyncStartTime = _chkSynchStartTime.getSelection();
		final boolean isAdjustTime = isSyncStartTime == false;// && (isMergeActionSelected || isAltitudeAvailable);

		// adjust start altitude
		_chkAdjustAltiFromStart.setEnabled(isMergeAltitude && isAltitudeAvailable);
		_lblAdjustAltiValueDistance.setEnabled(isMergeAltitude && isAdjustAltitude);
		_lblAdjustAltiValueDistanceUnit.setEnabled(isMergeAltitude && isAdjustAltitude);
		_lblAdjustAltiValueTime.setEnabled(isMergeAltitude && isAdjustAltitude);
		_lblAdjustAltiValueTimeUnit.setEnabled(isMergeAltitude && isAdjustAltitude);

		// adjust from source altitude
		_chkAdjustAltiFromSource.setEnabled(isMergeAltitude && isAltitudeAvailable);
		_chkAdjustAltiSmoothly.setEnabled(isMergeAltitude && _chkAdjustAltiFromSource.getSelection());

		// set tour type
		_linkTourType.setEnabled(isSetTourType);

		/*
		 * CLabel cannot be disabled, show disabled control with another color
		 */
		if (isSetTourType) {
			_lblTourType.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));
		} else {
			_lblTourType.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
		}

		/*
		 * altitude adjustment controls
		 */
		_scaleAltitude1.setEnabled(isAltitudeAvailable);
		_scaleAltitude10.setEnabled(isAltitudeAvailable);
		_lblAltitudeDiff1.setEnabled(isAltitudeAvailable);
		_lblAltitudeDiff10.setEnabled(isAltitudeAvailable);

		_chkValueDiffScaling.setEnabled(isAltitudeAvailable);

		/*
		 * time adjustment controls
		 */
		_chkSynchStartTime.setEnabled(true);

		_scaleAdjustMinutes.setEnabled(isAdjustTime);
		_lblAdjustMinuteValue.setEnabled(isAdjustTime);
		_lblAdjustMinuteUnit.setEnabled(isAdjustTime);

		_scaleAdjustSeconds.setEnabled(isAdjustTime);
		_lblAdjustSecondsValue.setEnabled(isAdjustTime);
		_lblAdjustSecondsUnit.setEnabled(isAdjustTime);

		/*
		 * reset buttons
		 */
		_btnResetAdjustment.setEnabled(true);
		_btnResetValues.setEnabled(true);
	}

	private void enableGraphActions() {

		final boolean isAltitude = _sourceTour.altitudeSerie != null && _targetTour.altitudeSerie != null;
		final boolean isSourcePulse = _sourceTour.pulseSerie != null;
		final boolean isSourceTemperature = _sourceTour.temperatureSerie != null;
		final boolean isSourceCadence = _sourceTour.getCadenceSerie() != null;

		_chkMergeAltitude.setEnabled(isAltitude);
		_chkMergePulse.setEnabled(isSourcePulse);
		_chkMergeTemperature.setEnabled(isSourceTemperature);
		_chkMergeCadence.setEnabled(isSourceCadence);

		/*
		 * keep state from the pref store but unckeck graphs which are not available
		 */
		if (isAltitude == false) {
			_chkMergeAltitude.setSelection(false);
		}
		if (isSourcePulse == false) {
			_chkMergePulse.setSelection(false);
		}
		if (isSourceTemperature == false) {
			_chkMergeTemperature.setSelection(false);
		}
		if (isSourceCadence == false) {
			_chkMergeCadence.setSelection(false);
		}

		if (_chkMergeAltitude.getSelection()) {

			// ensure that one adjust altitude option is selected
			if (_chkAdjustAltiFromStart.getSelection() == false && _chkAdjustAltiFromSource.getSelection() == false) {
				_chkAdjustAltiFromSource.setSelection(true);
			}
		}
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		// keep window size and position
//		return null;
		return _dialogSettings;
	}

	private int getFromUIAltitudeOffset() {

		final int altiDiff1 = _scaleAltitude1.getSelection() - MAX_ADJUST_ALTITUDE_1;
		final int altiDiff10 = (_scaleAltitude10.getSelection() - MAX_ADJUST_ALTITUDE_10) * 10;

		final float localAltiDiff1 = altiDiff1 / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE;
		final float localAltiDiff10 = altiDiff10 / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE;

		_lblAltitudeDiff1.setText(Integer.toString((int) localAltiDiff1) + UI.SPACE + UI.UNIT_LABEL_ALTITUDE);
		_lblAltitudeDiff10.setText(Integer.toString((int) localAltiDiff10) + UI.SPACE + UI.UNIT_LABEL_ALTITUDE);

		return altiDiff1 + altiDiff10;
	}

	/**
	 * @return tour time offset which is set in the UI
	 */
	private int getFromUITourTimeOffset() {

		final int seconds = _scaleAdjustSeconds.getSelection() - MAX_ADJUST_SECONDS;
		final int minutes = _scaleAdjustMinutes.getSelection() - MAX_ADJUST_MINUTES;

		_lblAdjustSecondsValue.setText(Integer.toString(seconds));
		_lblAdjustMinuteValue.setText(Integer.toString(minutes));

		return minutes * 60 + seconds;
	}

	@Override
	public ArrayList<TourData> getSelectedTours() {

		final ArrayList<TourData> selectedTours = new ArrayList<>();
		selectedTours.add(_sourceTour);

		return selectedTours;
	}

	@Override
	protected void okPressed() {

		saveTour();

		super.okPressed();
	}

	private void onDispose() {

		_iconPlaceholder.dispose();

		for (final Image image : _graphImages.values()) {
			image.dispose();
		}

		_prefStore.removePropertyChangeListener(_prefChangeListener);
	}

	private void onModifyProperties() {

		_targetTour.setMergedAltitudeOffset(getFromUIAltitudeOffset());
		_targetTour.setMergedTourTimeOffset(getFromUITourTimeOffset());

		// calculate merged data
		computeMergedData();

		_tourChartConfig.isRelativeValueDiffScaling = _chkValueDiffScaling.getSelection();

		if (_chkPreviewChart.getSelection()) {
			// update chart
			updateTourChart();
		} else {
			// update only the merge layer, this is much faster
			_tourChart.updateLayer2ndAlti(this, true);
		}

		enableActions();
	}

	private void onScaleDoubleClick(final Widget widget) {

		final Scale scale = (Scale) widget;
		final int max = scale.getMaximum();

		scale.setSelection(max / 2);

		onModifyProperties();
	}

	private void onSelectMergeGraph(final SelectionEvent event) {

		if (event.widget == _chkMergeAltitude) {

			// merge altitude is modified

			if (_chkMergeAltitude.getSelection()) {

				// merge altitude is selected

				_chkAdjustAltiFromSource.setSelection(_isAdjustAltiFromSourceBackup);
				_chkAdjustAltiFromStart.setSelection(_isAdjustAltiFromStartBackup);
			} else {

				// merge altitude is deselected

				_isAdjustAltiFromSourceBackup = _chkAdjustAltiFromSource.getSelection();
				_isAdjustAltiFromStartBackup = _chkAdjustAltiFromStart.getSelection();
			}

		}

		if (_chkMergeAltitude.getSelection()) {

			// ensure that one adjust altitude option is selected
			if (_chkAdjustAltiFromStart.getSelection() == false && _chkAdjustAltiFromSource.getSelection() == false) {
				_chkAdjustAltiFromSource.setSelection(true);
			}
		} else {

			// uncheck all altitude adjustments
			_chkAdjustAltiFromSource.setSelection(false);
			_chkAdjustAltiFromStart.setSelection(false);
		}

		setMergedGraphsVisible();

		onModifyProperties();
		updateTourChart();
	}

	private void onSelectResetAdjustments() {

		_scaleAdjustSeconds.setSelection(MAX_ADJUST_SECONDS);
		_scaleAdjustMinutes.setSelection(MAX_ADJUST_MINUTES);
		_scaleAltitude1.setSelection(MAX_ADJUST_ALTITUDE_1);
		_scaleAltitude10.setSelection(MAX_ADJUST_ALTITUDE_10);

		onModifyProperties();
	}

	private void onSelectResetValues() {

		/*
		 * get original data from the backuped data
		 */
		_sourceTour.timeSerie = Util.createIntegerCopy(_backupSourceTimeSerie);
		_sourceTour.distanceSerie = Util.createFloatCopy(_backupSourceDistanceSerie);
		_sourceTour.altitudeSerie = Util.createFloatCopy(_backupSourceAltitudeSerie);

		_targetTour.pulseSerie = Util.createFloatCopy(_backupTargetPulseSerie);
		_targetTour.temperatureSerie = Util.createFloatCopy(_backupTargetTemperatureSerie);
		_targetTour.setCadenceSerie(Util.createFloatCopy(_backupTargetCadenceSerie));

		_targetTour.setMergedTourTimeOffset(_backupTargetTimeOffset);
		_targetTour.setMergedAltitudeOffset(_backupTargetAltitudeOffset);

		updateUIFromTourData();

		enableActions();
	}

	/**
	 * Restore values which have been modified in the dialog
	 *
	 * @param selectedTour
	 */
	private void restoreDataBackup() {

		_sourceTour.timeSerie = _backupSourceTimeSerie;
		_sourceTour.distanceSerie = _backupSourceDistanceSerie;
		_sourceTour.altitudeSerie = _backupSourceAltitudeSerie;
		_sourceTour.setTourType(_backupSourceTourType);

		_targetTour.pulseSerie = _backupTargetPulseSerie;
		_targetTour.temperatureSerie = _backupTargetTemperatureSerie;
		_targetTour.setCadenceSerie(_backupTargetCadenceSerie);

		_targetTour.setMergedTourTimeOffset(_backupTargetTimeOffset);
		_targetTour.setMergedAltitudeOffset(_backupTargetAltitudeOffset);
	}

	private void restoreState() {

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		_chkPreviewChart.setSelection(prefStore.getBoolean(ITourbookPreferences.MERGE_TOUR_PREVIEW_CHART));
		_chkValueDiffScaling.setSelection(prefStore.getBoolean(ITourbookPreferences.MERGE_TOUR_ALTITUDE_DIFF_SCALING));

		_chkAdjustAltiSmoothly.setSelection(prefStore.getBoolean(ITourbookPreferences.MERGE_TOUR_LINEAR_INTERPOLATION));

		_chkSynchStartTime.setSelection(prefStore.getBoolean(ITourbookPreferences.MERGE_TOUR_SYNC_START_TIME));

		/*
		 * set tour type
		 */
		_chkSetTourType.setSelection(prefStore.getBoolean(ITourbookPreferences.MERGE_TOUR_SET_TOUR_TYPE));
		if (_sourceTour.getTourPerson() == null) {

			// tour is not saved, used tour type id from pref store

			final long tourTypeId = prefStore.getLong(ITourbookPreferences.MERGE_TOUR_SET_TOUR_TYPE_ID);

			final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();

			for (final TourType tourType : tourTypes) {
				if (tourType.getTypeId() == tourTypeId) {

					_sourceTour.setTourType(tourType);

					_isMergeSourceTourModified = true;

					break;
				}
			}
		}

		// restore merge graph state
		_chkMergeAltitude.setSelection(prefStore.getBoolean(ITourbookPreferences.MERGE_TOUR_MERGE_GRAPH_ALTITUDE));
		_chkMergePulse.setSelection(prefStore.getBoolean(ITourbookPreferences.MERGE_TOUR_MERGE_GRAPH_PULSE));
		_chkMergeTemperature
				.setSelection(prefStore.getBoolean(ITourbookPreferences.MERGE_TOUR_MERGE_GRAPH_TEMPERATURE));
		_chkMergeCadence.setSelection(prefStore.getBoolean(ITourbookPreferences.MERGE_TOUR_MERGE_GRAPH_CADENCE));
	}

	private void saveState() {

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		prefStore.setValue(ITourbookPreferences.MERGE_TOUR_GRAPH_X_AXIS,
				_tourChartConfig.isShowTimeOnXAxis
						? TourManager.X_AXIS_TIME
						: TourManager.X_AXIS_DISTANCE);

		prefStore.setValue(ITourbookPreferences.MERGE_TOUR_PREVIEW_CHART, _chkPreviewChart.getSelection());
		prefStore.setValue(ITourbookPreferences.MERGE_TOUR_ALTITUDE_DIFF_SCALING, _chkValueDiffScaling.getSelection());

		prefStore.setValue(ITourbookPreferences.MERGE_TOUR_LINEAR_INTERPOLATION, _chkAdjustAltiSmoothly.getSelection());
		prefStore.setValue(ITourbookPreferences.MERGE_TOUR_SET_TOUR_TYPE, _chkSetTourType.getSelection());
		prefStore.setValue(ITourbookPreferences.MERGE_TOUR_SYNC_START_TIME, _chkSynchStartTime.getSelection());

		// save tour type id
		final TourType sourceTourType = _sourceTour.getTourType();
		if (sourceTourType != null) {
			prefStore.setValue(ITourbookPreferences.MERGE_TOUR_SET_TOUR_TYPE_ID, sourceTourType.getTypeId());
		}

		// save merged tour graphs
		prefStore.setValue(ITourbookPreferences.MERGE_TOUR_MERGE_GRAPH_ALTITUDE, _chkMergeAltitude.getSelection());
		prefStore.setValue(ITourbookPreferences.MERGE_TOUR_MERGE_GRAPH_PULSE, _chkMergePulse.getSelection());
		prefStore
				.setValue(ITourbookPreferences.MERGE_TOUR_MERGE_GRAPH_TEMPERATURE, _chkMergeTemperature.getSelection());
		prefStore.setValue(ITourbookPreferences.MERGE_TOUR_MERGE_GRAPH_CADENCE, _chkMergeCadence.getSelection());

	}

	private void saveTour() {

		boolean isMerged = false;

		if (_chkMergeAltitude.getSelection()) {

			// set target altitude values

			if ((_chkAdjustAltiFromStart.getSelection() || _chkAdjustAltiFromSource.getSelection())
					&& _sourceTour.dataSerieAdjustedAlti != null) {

				isMerged = true;

				// update target altitude from adjuste source altitude
				_targetTour.altitudeSerie = _sourceTour.dataSerieAdjustedAlti;

				// adjust altitude up/down values
				_targetTour.computeAltitudeUpDown();
			}
		}

		if (_chkMergePulse.getSelection()) {
			// pulse is already merged
			isMerged = true;
		} else {
			// restore original pulse values because these values should not be saved
			_targetTour.pulseSerie = _backupTargetPulseSerie;
		}

		if (_chkMergeTemperature.getSelection()) {
			// temperature is already merged
			isMerged = true;
		} else {
			// restore original temperature values because these values should not be saved
			_targetTour.temperatureSerie = _backupTargetTemperatureSerie;
		}

		if (_chkMergeCadence.getSelection()) {
			// cadence is already merged
			isMerged = true;
		} else {
			// restore original cadence values because these values should not be saved
			_targetTour.setCadenceSerie(_backupTargetCadenceSerie);
		}

		if (_chkSetTourType.getSelection() == false) {

			// restore backup values

			_sourceTour.setTourType(_backupSourceTourType);
		}

		if (isMerged) {
			_targetTour.computeComputedValues();
		}

		// set tour id into which the tour is merged
		_sourceTour.setTourPerson(_targetTour.getTourPerson());
		_sourceTour.setMergeTargetTourId(_targetTour.getTourId());
		_sourceTour.setMergeSourceTourId(null);

		// save modified tours
		final ArrayList<TourData> modifiedTours = new ArrayList<>();
		modifiedTours.add(_targetTour);
		modifiedTours.add(_sourceTour);

		TourManager.saveModifiedTours(modifiedTours);

		_isTourSaved = true;
	}

	private void setMergedGraphsVisible() {

		final ArrayList<Integer> visibleGraphs = _tourChartConfig.getVisibleGraphs();

		if (_chkMergeAltitude.getSelection()) {
			if (visibleGraphs.contains(TourManager.GRAPH_ALTITUDE) == false) {
				visibleGraphs.add(TourManager.GRAPH_ALTITUDE);
			}
		}

		if (_chkMergePulse.getSelection()) {
			if (visibleGraphs.contains(TourManager.GRAPH_PULSE) == false) {
				visibleGraphs.add(TourManager.GRAPH_PULSE);
			}
		}

		if (_chkMergeTemperature.getSelection()) {
			if (visibleGraphs.contains(TourManager.GRAPH_TEMPERATURE) == false) {
				visibleGraphs.add(TourManager.GRAPH_TEMPERATURE);
			}
		}

		if (_chkMergeCadence.getSelection()) {
			if (visibleGraphs.contains(TourManager.GRAPH_CADENCE) == false) {
				visibleGraphs.add(TourManager.GRAPH_CADENCE);
			}
		}
	}

	@Override
	public void toursAreModified(final ArrayList<TourData> modifiedTours) {

		// tour type was modified
		_isMergeSourceTourModified = true;

		updateUIFromTourData();
	}

	private void updateTourChart() {

		_isChartUpdated = true;

		_tourChart.updateTourChart(_targetTour, _tourChartConfig, true);

		_isChartUpdated = false;
	}

	private void updateUI(final float altiDiffTime, final float altiDiffDist) {

		if (_chkAdjustAltiFromStart.getSelection()) {

			_lblAdjustAltiValueTime.setText(_nf.format(altiDiffTime));
			_lblAdjustAltiValueDistance.setText(_nf.format(altiDiffDist));

		} else {

			// adjusted alti is disabled

			_lblAdjustAltiValueTime.setText("N/A"); //$NON-NLS-1$
			_lblAdjustAltiValueDistance.setText("N/A"); //$NON-NLS-1$
		}
	}

	/**
	 * set data from the tour into the UI
	 */
	private void updateUIFromTourData() {

		/*
		 * show time offset
		 */
		updateUITourTimeOffset(_targetTour.getMergedTourTimeOffset());

		/*
		 * show altitude offset
		 */
		final int mergedMetricAltitudeOffset = _targetTour.getMergedAltitudeOffset();

		final float altitudeOffset = mergedMetricAltitudeOffset;
		final int altitudeOffset1 = (int) (altitudeOffset % 10);
		final int altitudeOffset10 = (int) (altitudeOffset / 10);

		_scaleAltitude1.setSelection(altitudeOffset1 + MAX_ADJUST_ALTITUDE_1);
		_scaleAltitude10.setSelection(altitudeOffset10 + MAX_ADJUST_ALTITUDE_10);

		net.tourbook.ui.UI.updateUI_TourType(_sourceTour, _lblTourType, true);

		onModifyProperties();
	}

	private void updateUITourTimeOffset(final int tourTimeOffset) {

		final int seconds = tourTimeOffset % 60;
		final int minutes = tourTimeOffset / 60;

		_scaleAdjustSeconds.setSelection(seconds + MAX_ADJUST_SECONDS);
		_scaleAdjustMinutes.setSelection(minutes + MAX_ADJUST_MINUTES);
	}
}
