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
package net.tourbook.ui.views.rawData;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;

import net.tourbook.Messages;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ISliderMoveListener;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.IDataModelListener;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider2;
import net.tourbook.ui.UI;
import net.tourbook.ui.action.ActionOpenPrefDialog;
import net.tourbook.ui.action.ActionSetTourTypeMenu;
import net.tourbook.ui.tourChart.ChartLayer2ndAltiSerie;
import net.tourbook.ui.tourChart.I2ndAltiLayer;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.tourChart.TourChartConfiguration;
import net.tourbook.util.PixelConverter;
import net.tourbook.util.Util;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
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

public class DialogMergeTours extends TitleAreaDialog implements ITourProvider2, I2ndAltiLayer {

	private static final int		MAX_ADJUST_SECONDS		= 120;
	private static final int		MAX_ADJUST_MINUTES		= 120;								// x 60
	private static final int		MAX_ADJUST_ALTITUDE_1	= 20;
	private static final int		MAX_ADJUST_ALTITUDE_10	= 40;								// x 10

	private static final int		VH_SPACING				= 2;

	private final IDialogSettings	fDialogSettings;

	private TourData				fSourceTour;
	private TourData				fTargetTour;

	private Composite				fDlgContainer;

	private TourChart				fTourChart;
	private TourChartConfiguration	fTourChartConfig;

	/*
	 * vertical adjustment options
	 */
	private Group					fGroupAltitude;

	private Label					fLabelAltitudeDiff1;
	private Label					fLabelAltitudeDiff10;

	private Scale					fScaleAltitude1;
	private Scale					fScaleAltitude10;

	/*
	 * horzontal adjustment options
	 */
	private Button					fChkSynchStartTime;

	private Label					fLabelAdjustMinuteValue;
	private Label					fLabelAdjustSecondsValue;

	private Scale					fScaleAdjustMinutes;
	private Scale					fScaleAdjustSeconds;

	private Label					fLabelAdjustMinuteUnit;
	private Label					fLabelAdjustSecondsUnit;

	private Button					fBtnResetAdjustment;
	private Button					fBtnResetValues;

	/*
	 * save actions
	 */
	private Button					fChkMergeAltitude;
	private Button					fChkMergePulse;
	private Button					fChkMergeTemperature;
	private Button					fChkMergeCadence;

	private Button					fChkAdjustAltiFromSource;
	private Button					fChkAdjustAltiSmoothly;

	private Button					fChkAdjustAltiFromStart;
	private Label					fLblAdjustAltiValueTimeUnit;
	private Label					fLblAdjustAltiValueDistanceUnit;
	private Label					fLblAdjustAltiValueTime;
	private Label					fLblAdjustAltiValueDistance;

	private Button					fChkSetTourType;
	private Link					fTourTypeLink;
	private CLabel					fLblTourType;

	private Button					fChkKeepHVAdjustments;

	/*
	 * display actions
	 */
	private Button					fChkValueDiffScaling;

	private Button					fChkPreviewChart;

	private boolean					fIsTourSaved			= false;
	private boolean					fIsMergeSourceTourModified;
	private boolean					fIsChartUpdated;

	/*
	 * backup data
	 */
	private int[]					fBackupSourceTimeSerie;
	private int[]					fBackupSourceDistanceSerie;
	private int[]					fBackupSourceAltitudeSerie;

	private TourType				fBackupSourceTourType;

	private int[]					fBackupTargetPulseSerie;
	private int[]					fBackupTargetTemperatureSerie;
	private int[]					fBackupTargetCadenceSerie;

	private int						fBackupTargetTimeOffset;
	private int						fBackupTargetAltitudeOffset;

	private ActionOpenPrefDialog	fActionOpenTourTypePrefs;

	private NumberFormat			fNumberFormatter		= NumberFormat.getNumberInstance();
	private static final Calendar	fCalendar				= GregorianCalendar.getInstance();

	private Image					fShellImage;
	private Image					fIconPlaceholder;
	private HashMap<Integer, Image>	fGraphImages			= new HashMap<Integer, Image>();

	private int						fTourStartTimeSynchOffset;
	private int						fTourTimeOffsetBackup;

	private boolean					fIsAdjustAltiFromSourceBackup;
	private boolean					fIsAdjustAltiFromStartBackup;
	private IPropertyChangeListener	fPrefChangeListener;

	/**
	 * @param parentShell
	 * @param mergeSourceTour
	 *            {@link TourData} for the tour which is merge into the other tour
	 * @param mergeTargetTour
	 *            {@link TourData} for the tour into which the other tour is merged
	 */
	public DialogMergeTours(final Shell parentShell, final TourData mergeSourceTour, final TourData mergeTargetTour) {

		super(parentShell);

		// make dialog resizable and display maximize button
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);

		// set icon for the window 
		fShellImage = TourbookPlugin.getImageDescriptor(Messages.image__merge_tours).createImage();
		fIconPlaceholder = TourbookPlugin.getImageDescriptor(Messages.Image__icon_placeholder).createImage();
		setDefaultImage(fShellImage);

		fSourceTour = mergeSourceTour;
		fTargetTour = mergeTargetTour;

		/*
		 * synchronize start time
		 */
		fCalendar.set(fSourceTour.getStartYear(),
				fSourceTour.getStartMonth() - 1,
				fSourceTour.getStartDay(),
				fSourceTour.getStartHour(),
				fSourceTour.getStartMinute(),
				fSourceTour.getStartSecond());
		final long sourceStartTime = fCalendar.getTimeInMillis();

		fCalendar.set(fTargetTour.getStartYear(),
				fTargetTour.getStartMonth() - 1,
				fTargetTour.getStartDay(),
				fTargetTour.getStartHour(),
				fTargetTour.getStartMinute(),
				fTargetTour.getStartSecond());
		final long targetStartTime = fCalendar.getTimeInMillis();

		fTourStartTimeSynchOffset = (int) ((sourceStartTime - targetStartTime) / 1000);

		fNumberFormatter.setMinimumFractionDigits(3);
		fNumberFormatter.setMaximumFractionDigits(3);

		fDialogSettings = TourbookPlugin.getDefault().getDialogSettingsSection(getClass().getName());
	}

	private void addPrefListener() {

		final Preferences prefStore = TourbookPlugin.getDefault().getPluginPreferences();

		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(final Preferences.PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

					/*
					 * tour data cache is cleared, reload tour data from the database
					 */

					if (fSourceTour.getTourPerson() != null) {
						fSourceTour = TourManager.getInstance().getTourData(fSourceTour.getTourId());
					}

					fTargetTour = TourManager.getInstance().getTourData(fTargetTour.getTourId());

					onModifyProperties();
				}
			}
		};

		prefStore.addPropertyChangeListener(fPrefChangeListener);
	}

	/**
	 * set button width
	 */
	private void adjustButtonWidth() {

		final int btnResetAdj = fBtnResetAdjustment.getBounds().width;
		final int btnResetValues = fBtnResetValues.getBounds().width;
		final int newWidth = Math.max(btnResetAdj, btnResetValues);

		GridData gd;
		gd = (GridData) fBtnResetAdjustment.getLayoutData();
		gd.widthHint = newWidth;

		gd = (GridData) fBtnResetValues.getLayoutData();
		gd.widthHint = newWidth;
	}

	@Override
	public boolean close() {

		saveState();

		if (fIsTourSaved == false) {

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
		if (fIsMergeSourceTourModified) {

			// revert modified tour type in the merge from tour

			final TourEvent tourEvent = new TourEvent(fSourceTour);
			tourEvent.isReverted = true;

			TourManager.fireEvent(TourEventId.TOUR_CHANGED, tourEvent);
		}

		return super.close();
	}

	private void computeMergedData() {

		int xMergeOffset = fTargetTour.getMergedTourTimeOffset();
		final int yMergeOffset = fTargetTour.getMergedAltitudeOffset();

		final int[] targetTimeSerie = fTargetTour.timeSerie;
		final int[] targetDistanceSerie = fTargetTour.distanceSerie;
		final int[] targetAltitudeSerie = fTargetTour.altitudeSerie;

		final int[] sourceTimeSerie = fSourceTour.timeSerie;
		final int[] sourceAltitudeSerie = fSourceTour.altitudeSerie;
		final int[] sourcePulseSerie = fSourceTour.pulseSerie;
		final int[] sourceTemperatureSerie = fSourceTour.temperatureSerie;
		final int[] sourceCadenceSerie = fSourceTour.cadenceSerie;

		if (fChkSynchStartTime.getSelection()) {

			// synchronize start time

			xMergeOffset = fTourStartTimeSynchOffset;
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

		final int[] newSourceAltitudeSerie = new int[serieLength];
		final int[] newSourceAltiDiffSerie = new int[serieLength];

		final int[] newTargetPulseSerie = new int[serieLength];
		final int[] newTargetTemperatureSerie = new int[serieLength];
		final int[] newTargetCadenceSerie = new int[serieLength];

		int sourceIndex = 0;
		int sourceTime = sourceTimeSerie[0] + xMergeOffset;
		int sourceTimePrev = 0;
		int sourceAlti = 0;
		int sourceAltiPrev = 0;

		int targetTime = targetTimeSerie[0];
		int newSourceAltitude;

		if (isSourceAltitude) {
			sourceAlti = sourceAltitudeSerie[0] + yMergeOffset;
			sourceAltiPrev = sourceAlti;
			newSourceAltitude = sourceAlti;
		}

		final boolean isLinearInterpolation = fChkAdjustAltiFromSource.getSelection()
				&& fChkAdjustAltiSmoothly.getSelection();

		/*
		 * create new time/distance serie for the source tour according to the time of the target
		 * tour
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
					final int y1 = sourceAltiPrev;
					final int y3 = sourceAlti;

					final int xDiff = x3 - x1;

					newSourceAltitude = xDiff == 0 ? sourceAltiPrev : (x2 - x1) * (y3 - y1) / xDiff + y1;

				} else {

					/*
					 * the interpolited altitude is not exact above the none interpolite altitude,
					 * it is in the middle of the previous and current altitude
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

		fSourceTour.dataSerieAdjustedAlti = null;

		if (isSourceAltitude) {
			fSourceTour.dataSerie2ndAlti = newSourceAltitudeSerie;
		} else {
			fSourceTour.dataSerie2ndAlti = null;
		}

		if (isSourceAltitude && isTargetAltitude) {
			fSourceTour.dataSerieDiffTo2ndAlti = newSourceAltiDiffSerie;
		} else {
			fSourceTour.dataSerieDiffTo2ndAlti = null;
		}

		if (fChkMergePulse.getSelection()) {
			fTargetTour.pulseSerie = newTargetPulseSerie;
		} else {
			fTargetTour.pulseSerie = fBackupTargetPulseSerie;
		}

		if (fChkMergeTemperature.getSelection()) {
			fTargetTour.temperatureSerie = newTargetTemperatureSerie;
		} else {
			fTargetTour.temperatureSerie = fBackupTargetTemperatureSerie;
		}

		if (fChkMergeCadence.getSelection()) {
			fTargetTour.cadenceSerie = newTargetCadenceSerie;
		} else {
			fTargetTour.cadenceSerie = fBackupTargetCadenceSerie;
		}

		float altiDiffTime = 0;
		float altiDiffDist = 0;

		if (isSourceAltitude && isTargetAltitude && isTargetDistance) {

			/*
			 * compute adjusted altitude
			 */

			if (fChkAdjustAltiFromStart.getSelection()) {

				/*
				 * adjust start altitude until left slider
				 */

				final int[] adjustedTargetAltitudeSerie = new int[serieLength];

				float startAltiDiff = newSourceAltiDiffSerie[0];
				final int endIndex = fTourChart.getXSliderPosition().getLeftSliderValueIndex();
				final float distanceDiff = targetDistanceSerie[endIndex];

				final int[] altitudeSerie = fTargetTour.altitudeSerie;

				for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

					if (serieIndex < endIndex) {

						// add adjusted altitude

						final float targetDistance = targetDistanceSerie[serieIndex];
						final float distanceScale = 1 - targetDistance / distanceDiff;
						
						final int adjustedAltiDiff = (int) (startAltiDiff * distanceScale);
						final int newAltitude = altitudeSerie[serieIndex] + adjustedAltiDiff;

						adjustedTargetAltitudeSerie[serieIndex] = newAltitude;
						newSourceAltiDiffSerie[serieIndex] = newSourceAltitudeSerie[serieIndex] - newAltitude;

					} else {

						// add altitude which are not adjusted

						adjustedTargetAltitudeSerie[serieIndex] = altitudeSerie[serieIndex];
					}
				}

				fSourceTour.dataSerieAdjustedAlti = adjustedTargetAltitudeSerie;

				startAltiDiff /= UI.UNIT_VALUE_ALTITUDE;

				final int targetEndTime = targetTimeSerie[endIndex];
				final int targetEndDistance = targetDistanceSerie[endIndex];

				// meter/min
				altiDiffTime = targetEndTime == 0 ? //
						0f
						: startAltiDiff / targetEndTime * 60;

				// meter/meter
				altiDiffDist = targetEndDistance == 0 ? //
						0f
						: ((startAltiDiff * 1000) / targetEndDistance) / UI.UNIT_VALUE_DISTANCE;

			} else if (fChkAdjustAltiFromSource.getSelection()) {

				/*
				 * adjust target altitude from source altitude
				 */
				final int[] newTargetAltitudeSerie = new int[serieLength];

				for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {
					newTargetAltitudeSerie[serieIndex] = newSourceAltitudeSerie[serieIndex];
				}

				fSourceTour.dataSerieAdjustedAlti = newTargetAltitudeSerie;
			}
		}

		updateUI(altiDiffTime, altiDiffDist);
	}

	@Override
	protected void configureShell(final Shell shell) {

		super.configureShell(shell);

		shell.setText(Messages.tour_merger_dialog_title);

		shell.addDisposeListener(new DisposeListener() {
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
		fDlgContainer.layout(true, true);

		createActions();
		restoreState();

		setTitle(NLS.bind(Messages.tour_merger_dialog_header_title,
				TourManager.getTourTitle(fTargetTour),
				fTargetTour.getDeviceName()));

		setMessage(NLS.bind(Messages.tour_merger_dialog_header_message,
				TourManager.getTourTitle(fSourceTour),
				fSourceTour.getDeviceName()));

		// must be done before the merged data are computed
		enableGraphActions();
		setMergedGraphsVisible();

		// set alti diff scaling
		fTourChartConfig.isRelativeValueDiffScaling = fChkValueDiffScaling.getSelection();
		fTourChart.update2ndAltiLayer(this, true);

		updateUIFromTourData();

		if (fChkSynchStartTime.getSelection()) {
			fTourTimeOffsetBackup = fTargetTour.getMergedTourTimeOffset();
			updateUITourTimeOffset(fTourStartTimeSynchOffset);
		}

		// update chart after the UI is updated from the tour
		updateTourChart();

		enableActions();

	}

	public ChartLayer2ndAltiSerie create2ndAltiLayer() {

		if (fTargetTour == null) {
			return null;
		}

		final TourData mergeSourceTourData = fTargetTour.getMergeSourceTourData();

		if (fTargetTour.getMergeSourceTourId() == null && mergeSourceTourData == null) {
			return null;
		}

		TourData layerTourData;

		if (mergeSourceTourData != null) {
			layerTourData = mergeSourceTourData;
		} else {
			layerTourData = TourManager.getInstance().getTourData(fTargetTour.getMergeSourceTourId());
		}

		if (layerTourData == null) {
			return null;
		}

		final int[] xDataSerie = fTourChartConfig.showTimeOnXAxis
				? fTargetTour.timeSerie
				: fTargetTour.getDistanceSerie();

		return new ChartLayer2ndAltiSerie(layerTourData, xDataSerie, fTourChartConfig);
	}

	private void createActions() {

		fActionOpenTourTypePrefs = new ActionOpenPrefDialog(Messages.action_tourType_modify_tourTypes,
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
		fBackupSourceTimeSerie = Util.createDataSerieBackup(fSourceTour.timeSerie);
		fBackupSourceDistanceSerie = Util.createDataSerieBackup(fSourceTour.distanceSerie);
		fBackupSourceAltitudeSerie = Util.createDataSerieBackup(fSourceTour.altitudeSerie);
		fBackupSourceTourType = fSourceTour.getTourType();

		fBackupTargetPulseSerie = Util.createDataSerieBackup(fTargetTour.pulseSerie);
		fBackupTargetTemperatureSerie = Util.createDataSerieBackup(fTargetTour.temperatureSerie);
		fBackupTargetCadenceSerie = Util.createDataSerieBackup(fTargetTour.cadenceSerie);

		fBackupTargetTimeOffset = fTargetTour.getMergedTourTimeOffset();
		fBackupTargetAltitudeOffset = fTargetTour.getMergedAltitudeOffset();
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		fDlgContainer = (Composite) super.createDialogArea(parent);

		createUI(fDlgContainer);

		return fDlgContainer;
	}

	private void createUI(final Composite parent) {

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

		final PixelConverter pc = new PixelConverter(parent);
		final int valueWidth = pc.convertWidthInCharsToPixels(4);
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
		fChkSynchStartTime = new Button(groupTime, SWT.CHECK);
		GridDataFactory.fillDefaults().applyTo(fChkSynchStartTime);
		fChkSynchStartTime.setText(Messages.tour_merger_chk_use_synced_start_time);
		fChkSynchStartTime.setToolTipText(Messages.tour_merger_chk_use_synced_start_time_tooltip);
		fChkSynchStartTime.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				// display synched time in the UI
				if (fChkSynchStartTime.getSelection()) {

					// set time offset for the synched tours 

					fTourTimeOffsetBackup = getFromUITourTimeOffset();
					updateUITourTimeOffset(fTourStartTimeSynchOffset);

				} else {

					// set time offset manually 

					updateUITourTimeOffset(fTourTimeOffsetBackup);
				}

				onModifyProperties();

				if (fChkPreviewChart.getSelection() == false) {

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
		fLabelAdjustSecondsValue = new Label(timeContainer, SWT.TRAIL);
		GridDataFactory.fillDefaults()
				.align(SWT.END, SWT.CENTER)
				.hint(valueWidth, SWT.DEFAULT)
				.applyTo(fLabelAdjustSecondsValue);

		label = new Label(timeContainer, SWT.NONE);
		label.setText(UI.SPACE);

		fLabelAdjustSecondsUnit = new Label(timeContainer, SWT.NONE);
		fLabelAdjustSecondsUnit.setText(Messages.tour_merger_label_adjust_seconds);

		fScaleAdjustSeconds = new Scale(timeContainer, SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleAdjustSeconds);
		fScaleAdjustSeconds.setMinimum(0);
		fScaleAdjustSeconds.setMaximum(MAX_ADJUST_SECONDS * 2);
		fScaleAdjustSeconds.setPageIncrement(20);
		fScaleAdjustSeconds.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyProperties();
			}
		});
		fScaleAdjustSeconds.addListener(SWT.MouseDoubleClick, new Listener() {
			public void handleEvent(final Event event) {
				onScaleDoubleClick(event.widget);
			}
		});

		/*
		 * scale: adjust minutes
		 */
		fLabelAdjustMinuteValue = new Label(timeContainer, SWT.TRAIL);
		GridDataFactory.fillDefaults()
				.align(SWT.END, SWT.CENTER)
				.hint(valueWidth, SWT.DEFAULT)
				.applyTo(fLabelAdjustMinuteValue);

		label = new Label(timeContainer, SWT.NONE);
		label.setText(UI.SPACE);

		fLabelAdjustMinuteUnit = new Label(timeContainer, SWT.NONE);
		fLabelAdjustMinuteUnit.setText(Messages.tour_merger_label_adjust_minutes);

		fScaleAdjustMinutes = new Scale(timeContainer, SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleAdjustMinutes);
		fScaleAdjustMinutes.setMinimum(0);
		fScaleAdjustMinutes.setMaximum(MAX_ADJUST_MINUTES * 2);
		fScaleAdjustMinutes.setPageIncrement(20);
		fScaleAdjustMinutes.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyProperties();
			}
		});
		fScaleAdjustMinutes.addListener(SWT.MouseDoubleClick, new Listener() {
			public void handleEvent(final Event event) {
				onScaleDoubleClick(event.widget);
			}
		});
	}

	/**
	 * group: adjust altitude
	 */
	private void createUIGroupVertAdjustment(final Composite parent) {

		final PixelConverter pc = new PixelConverter(parent);

		fGroupAltitude = new Group(parent, SWT.NONE);
		fGroupAltitude.setText(Messages.tour_merger_group_adjust_altitude);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.BEGINNING).applyTo(fGroupAltitude);
		GridLayoutFactory.swtDefaults().numColumns(4)
//				.extendedMargins(0, 0, 0, 0)
//				.spacing(0, 0)
				.spacing(VH_SPACING, VH_SPACING)
				.applyTo(fGroupAltitude);

		/*
		 * scale: altitude 20m
		 */
		fLabelAltitudeDiff1 = new Label(fGroupAltitude, SWT.TRAIL);
		GridDataFactory.fillDefaults()
				.align(SWT.END, SWT.CENTER)
				.hint(pc.convertWidthInCharsToPixels(8), SWT.DEFAULT)
				.applyTo(fLabelAltitudeDiff1);

		fScaleAltitude1 = new Scale(fGroupAltitude, SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleAltitude1);
		fScaleAltitude1.setMinimum(0);
		fScaleAltitude1.setMaximum(MAX_ADJUST_ALTITUDE_1 * 2);
		fScaleAltitude1.setPageIncrement(5);
		fScaleAltitude1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyProperties();
			}
		});
		fScaleAltitude1.addListener(SWT.MouseDoubleClick, new Listener() {
			public void handleEvent(final Event event) {
				onScaleDoubleClick(event.widget);
			}
		});

		/*
		 * scale: altitude 100m
		 */
		fLabelAltitudeDiff10 = new Label(fGroupAltitude, SWT.TRAIL);
		GridDataFactory.fillDefaults()
				.align(SWT.END, SWT.CENTER)
				.hint(pc.convertWidthInCharsToPixels(8), SWT.DEFAULT)
				.applyTo(fLabelAltitudeDiff10);

		fScaleAltitude10 = new Scale(fGroupAltitude, SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleAltitude10);
		fScaleAltitude10.setMinimum(0);
		fScaleAltitude10.setMaximum(MAX_ADJUST_ALTITUDE_10 * 2);
		fScaleAltitude10.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyProperties();
			}
		});
		fScaleAltitude10.addListener(SWT.MouseDoubleClick, new Listener() {
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
			fGraphImages.put(graphId, image);
			mergeButton.setImage(image);
		} else {
			mergeButton.setImage(fIconPlaceholder);
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
		fChkValueDiffScaling = new Button(container, SWT.CHECK);
		GridDataFactory.swtDefaults()/* .indent(5, 5) .span(4, 1) */.applyTo(fChkValueDiffScaling);
		fChkValueDiffScaling.setText(Messages.tour_merger_chk_alti_diff_scaling);
		fChkValueDiffScaling.setToolTipText(Messages.tour_merger_chk_alti_diff_scaling_tooltip);
		fChkValueDiffScaling.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyProperties();
			}
		});

		/*
		 * checkbox: preview chart
		 */
		fChkPreviewChart = new Button(container, SWT.CHECK);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(fChkPreviewChart);
		fChkPreviewChart.setText(Messages.tour_merger_chk_preview_graphs);
		fChkPreviewChart.setToolTipText(Messages.tour_merger_chk_preview_graphs_tooltip);
		fChkPreviewChart.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				if (fChkPreviewChart.getSelection()) {

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
		fBtnResetAdjustment = new Button(container, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(fBtnResetAdjustment);
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
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(fBtnResetValues);
		fBtnResetValues.setText(Messages.tour_merger_btn_reset_values);
		fBtnResetValues.setToolTipText(Messages.tour_merger_btn_reset_values_tooltip);
		fBtnResetValues.addSelectionListener(new SelectionAdapter() {
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

		final PixelConverter pc = new PixelConverter(parent);

		final int indentOption = pc.convertHorizontalDLUsToPixels(20);
		final int indentOption2 = pc.convertHorizontalDLUsToPixels(13);

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
		fChkMergePulse = createUIMergeAction(group,
				TourManager.GRAPH_PULSE,
				Messages.merge_tour_source_graph_heartbeat,
				Messages.merge_tour_source_graph_heartbeat_tooltip,
				Messages.Image__graph_heartbeat,
				fSourceTour.pulseSerie != null);

		/*
		 * checkbox: merge temperature
		 */
		fChkMergeTemperature = createUIMergeAction(group,
				TourManager.GRAPH_TEMPERATURE,
				Messages.merge_tour_source_graph_temperature,
				Messages.merge_tour_source_graph_temperature_tooltip,
				Messages.Image__graph_temperature,
				fSourceTour.temperatureSerie != null);

		/*
		 * checkbox: merge cadence
		 */
		fChkMergeCadence = createUIMergeAction(group,
				TourManager.GRAPH_CADENCE,
				Messages.merge_tour_source_graph_cadence,
				Messages.merge_tour_source_graph_cadence_tooltip,
				Messages.Image__graph_cadence,
				fSourceTour.cadenceSerie != null);

		/*
		 * checkbox: merge altitude
		 */
		fChkMergeAltitude = createUIMergeAction(group,
				TourManager.GRAPH_ALTITUDE,
				Messages.merge_tour_source_graph_altitude,
				Messages.merge_tour_source_graph_altitude_tooltip,
				Messages.Image__graph_altitude,
				fSourceTour.altitudeSerie != null);

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
		fChkAdjustAltiFromSource = new Button(containerMergeGraph, SWT.RADIO);
		fChkAdjustAltiFromSource.setText(Messages.tour_merger_chk_adjust_altitude_from_source);
		fChkAdjustAltiFromSource.setToolTipText(Messages.tour_merger_chk_adjust_altitude_from_source_tooltip);
//		fChkAdjustAltiFromSource.setImage(fIconPlaceholder);

		fChkAdjustAltiFromSource.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				if (fChkAdjustAltiFromSource.getSelection()) {

					// check one adjust altitude
					if (fChkAdjustAltiFromSource.getSelection() == false
							&& fChkAdjustAltiFromStart.getSelection() == false) {
						fChkAdjustAltiFromSource.setSelection(true);
					}

					// only one altitude adjustment can be set
					fChkAdjustAltiFromStart.setSelection(false);

					fChkMergeAltitude.setSelection(true);

				} else {

					// disable altitude merge option
					fChkMergeAltitude.setSelection(false);
				}

				onModifyProperties();
			}
		});

		/*
		 * checkbox: smooth altitude with linear interpolation
		 */
		fChkAdjustAltiSmoothly = new Button(containerMergeGraph, SWT.CHECK);
		GridDataFactory.fillDefaults().indent(indentOption2, 0).applyTo(fChkAdjustAltiSmoothly);
		fChkAdjustAltiSmoothly.setText(Messages.tour_merger_chk_adjust_altitude_linear_interpolition);
		fChkAdjustAltiSmoothly.setToolTipText(Messages.tour_merger_chk_adjust_altitude_linear_interpolition_tooltip);
		fChkAdjustAltiSmoothly.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyProperties();
			}
		});

		/*
		 * checkbox: adjust start altitude
		 */
		fChkAdjustAltiFromStart = new Button(containerMergeGraph, SWT.RADIO);
		fChkAdjustAltiFromStart.setText(Messages.tour_merger_chk_adjust_start_altitude);
		fChkAdjustAltiFromStart.setToolTipText(Messages.tour_merger_chk_adjust_start_altitude_tooltip);
		fChkAdjustAltiFromStart.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				if (fChkAdjustAltiFromStart.getSelection()) {

					// check one adjust altitude
					if (fChkAdjustAltiFromStart.getSelection() == false
							&& fChkAdjustAltiFromSource.getSelection() == false) {
						fChkAdjustAltiFromStart.setSelection(true);
					}

					// only one altitude adjustment can be done
					fChkAdjustAltiFromSource.setSelection(false);

					fChkMergeAltitude.setSelection(true);

				} else {

					// disable altitude merge option
					fChkMergeAltitude.setSelection(false);
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

		fLblAdjustAltiValueTime = new Label(aaContainer, SWT.TRAIL);
		GridDataFactory.fillDefaults()
				.hint(pc.convertWidthInCharsToPixels(10), SWT.DEFAULT)
				.applyTo(fLblAdjustAltiValueTime);

		fLblAdjustAltiValueTimeUnit = new Label(aaContainer, SWT.NONE);
		fLblAdjustAltiValueTimeUnit.setText(UI.UNIT_LABEL_ALTITUDE + "/min"); //$NON-NLS-1$

		fLblAdjustAltiValueDistance = new Label(aaContainer, SWT.TRAIL);
		GridDataFactory.fillDefaults()
				.hint(pc.convertWidthInCharsToPixels(10), SWT.DEFAULT)
				.applyTo(fLblAdjustAltiValueDistance);

		fLblAdjustAltiValueDistanceUnit = new Label(aaContainer, SWT.NONE);
		fLblAdjustAltiValueDistanceUnit.setText(UI.UNIT_LABEL_ALTITUDE + "/" + UI.UNIT_LABEL_DISTANCE); //$NON-NLS-1$

		/*
		 * checkbox: set tour type
		 */
		fChkSetTourType = new Button(group, SWT.CHECK);
		GridDataFactory.fillDefaults().indent(0, 10).applyTo(fChkSetTourType);
		fChkSetTourType.setText(Messages.tour_merger_chk_set_tour_type);
		fChkSetTourType.setToolTipText(Messages.tour_merger_chk_set_tour_type_tooltip);
		fChkSetTourType.addSelectionListener(new SelectionAdapter() {
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
		fTourTypeLink = new Link(ttContainer, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(fTourTypeLink);
		fTourTypeLink.setText(Messages.tour_editor_label_tour_type);
		fTourTypeLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				UI.openControlMenu(fTourTypeLink);
			}
		});

		/*
		 * tour type menu
		 */
		final MenuManager menuMgr = new MenuManager();

		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(final IMenuManager menuMgr) {

				// set menu items

				ActionSetTourTypeMenu.fillMenu(menuMgr, DialogMergeTours.this, false);

				menuMgr.add(new Separator());
				menuMgr.add(fActionOpenTourTypePrefs);
			}
		});

		// set menu for the tag item
		fTourTypeLink.setMenu(menuMgr.createContextMenu(fTourTypeLink));

		/*
		 * label: tour type icon and text
		 */
		fLblTourType = new CLabel(ttContainer, SWT.NONE);
		GridDataFactory.swtDefaults().grab(true, false).applyTo(fLblTourType);

		/*
		 * checkbox: keep horiz. and vert. adjustments
		 */
		fChkKeepHVAdjustments = new Button(group, SWT.CHECK);
		fChkKeepHVAdjustments.setText(Messages.tour_merger_chk_keep_horiz_vert_adjustments);
		fChkKeepHVAdjustments.setToolTipText(Messages.tour_merger_chk_keep_horiz_vert_adjustments_tooltip);
		fChkKeepHVAdjustments.setSelection(true);
		fChkKeepHVAdjustments.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				// this option cannot be deselected
				fChkKeepHVAdjustments.setSelection(true);
			}
		});
	}

	private void createUITourChart(final Composite dlgContainer) {

		fTourChart = new TourChart(dlgContainer, SWT.BORDER, true);
		GridDataFactory.fillDefaults().grab(true, true).minSize(300, 200).applyTo(fTourChart);

		fTourChart.setShowZoomActions(true);
		fTourChart.setShowSlider(true);

		// set altitude visible
		fTourChartConfig = new TourChartConfiguration(true);

		// set one visible graph
		int visibleGraph = -1;
		if (fTargetTour.altitudeSerie != null) {
			visibleGraph = TourManager.GRAPH_ALTITUDE;
		} else if (fTargetTour.pulseSerie != null) {
			visibleGraph = TourManager.GRAPH_PULSE;
		} else if (fTargetTour.temperatureSerie != null) {
			visibleGraph = TourManager.GRAPH_TEMPERATURE;
		} else if (fTargetTour.cadenceSerie != null) {
			visibleGraph = TourManager.GRAPH_CADENCE;
		}
		if (visibleGraph != -1) {
			fTourChartConfig.addVisibleGraph(visibleGraph);
		}

		// overwrite x-axis from pref store
		fTourChartConfig.setIsShowTimeOnXAxis(TourbookPlugin.getDefault()
				.getPreferenceStore()
				.getString(ITourbookPreferences.MERGE_TOUR_GRAPH_X_AXIS)
				.equals(TourManager.X_AXIS_TIME));

		fTourChart.addDataModelListener(new IDataModelListener() {

			public void dataModelChanged(final ChartDataModel changedChartDataModel) {

				// set title
				changedChartDataModel.setTitle(TourManager.getTourTitleDetailed(fTargetTour));
			}
		});

		fTourChart.addSliderMoveListener(new ISliderMoveListener() {
			public void sliderMoved(final SelectionChartInfo chartInfo) {

				if (fIsChartUpdated) {
					return;
				}

				onModifyProperties();
			}
		});
	}

	private void enableActions() {

		final boolean isMergeAltitude = fChkMergeAltitude.getSelection();
		final boolean isAdjustAltitude = fChkAdjustAltiFromStart.getSelection() && isMergeAltitude;
		final boolean isSetTourType = fChkSetTourType.getSelection();

//		final boolean isMergeActionSelected = isMergeAltitude
//				|| fChkMergePulse.getSelection()
//				|| fChkMergeTemperature.getSelection()
//				|| fChkMergeCadence.getSelection();

		final boolean isAltitudeAvailable = fSourceTour.altitudeSerie != null && fTargetTour.altitudeSerie != null;

		final boolean isSyncStartTime = fChkSynchStartTime.getSelection();
		final boolean isAdjustTime = isSyncStartTime == false;// && (isMergeActionSelected || isAltitudeAvailable);

		// adjust start altitude
		fChkAdjustAltiFromStart.setEnabled(isMergeAltitude && isAltitudeAvailable);
		fLblAdjustAltiValueDistance.setEnabled(isMergeAltitude && isAdjustAltitude);
		fLblAdjustAltiValueDistanceUnit.setEnabled(isMergeAltitude && isAdjustAltitude);
		fLblAdjustAltiValueTime.setEnabled(isMergeAltitude && isAdjustAltitude);
		fLblAdjustAltiValueTimeUnit.setEnabled(isMergeAltitude && isAdjustAltitude);

		// adjust from source altitude
		fChkAdjustAltiFromSource.setEnabled(isMergeAltitude && isAltitudeAvailable);
		fChkAdjustAltiSmoothly.setEnabled(isMergeAltitude && fChkAdjustAltiFromSource.getSelection());

		// set tour type
		fTourTypeLink.setEnabled(isSetTourType);

		/*
		 * CLabel cannot be disabled, show disabled control with another color
		 */
		if (isSetTourType) {
			fLblTourType.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));
		} else {
			fLblTourType.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
		}

		/*
		 * altitude adjustment controls
		 */
		fScaleAltitude1.setEnabled(isAltitudeAvailable);
		fScaleAltitude10.setEnabled(isAltitudeAvailable);
		fLabelAltitudeDiff1.setEnabled(isAltitudeAvailable);
		fLabelAltitudeDiff10.setEnabled(isAltitudeAvailable);

		fChkValueDiffScaling.setEnabled(isAltitudeAvailable);

		/*
		 * time adjustment controls
		 */
		fChkSynchStartTime.setEnabled(true);

		fScaleAdjustMinutes.setEnabled(isAdjustTime);
		fLabelAdjustMinuteValue.setEnabled(isAdjustTime);
		fLabelAdjustMinuteUnit.setEnabled(isAdjustTime);

		fScaleAdjustSeconds.setEnabled(isAdjustTime);
		fLabelAdjustSecondsValue.setEnabled(isAdjustTime);
		fLabelAdjustSecondsUnit.setEnabled(isAdjustTime);

		/*
		 * reset buttons
		 */
		fBtnResetAdjustment.setEnabled(true);
		fBtnResetValues.setEnabled(true);
	}

	private void enableGraphActions() {

		final boolean isAltitude = fSourceTour.altitudeSerie != null && fTargetTour.altitudeSerie != null;
		final boolean isSourcePulse = fSourceTour.pulseSerie != null;
		final boolean isSourceTemperature = fSourceTour.temperatureSerie != null;
		final boolean isSourceCadence = fSourceTour.cadenceSerie != null;

		fChkMergeAltitude.setEnabled(isAltitude);
		fChkMergePulse.setEnabled(isSourcePulse);
		fChkMergeTemperature.setEnabled(isSourceTemperature);
		fChkMergeCadence.setEnabled(isSourceCadence);

		/*
		 * keep state from the pref store but unckeck graphs which are not available
		 */
		if (isAltitude == false) {
			fChkMergeAltitude.setSelection(false);
		}
		if (isSourcePulse == false) {
			fChkMergePulse.setSelection(false);
		}
		if (isSourceTemperature == false) {
			fChkMergeTemperature.setSelection(false);
		}
		if (isSourceCadence == false) {
			fChkMergeCadence.setSelection(false);
		}

		if (fChkMergeAltitude.getSelection()) {

			// ensure that one adjust altitude option is selected
			if (fChkAdjustAltiFromStart.getSelection() == false && fChkAdjustAltiFromSource.getSelection() == false) {
				fChkAdjustAltiFromSource.setSelection(true);
			}
		}
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		// keep window size and position
//		return null;
		return fDialogSettings;
	}

	private int getFromUIAltitudeOffset() {

		final int altiDiff1 = fScaleAltitude1.getSelection() - MAX_ADJUST_ALTITUDE_1;
		final int altiDiff10 = (fScaleAltitude10.getSelection() - MAX_ADJUST_ALTITUDE_10) * 10;

		final float localAltiDiff1 = altiDiff1 / UI.UNIT_VALUE_ALTITUDE;
		final float localAltiDiff10 = altiDiff10 / UI.UNIT_VALUE_ALTITUDE;

		fLabelAltitudeDiff1.setText(Integer.toString((int) localAltiDiff1) + UI.SPACE + UI.UNIT_LABEL_ALTITUDE);
		fLabelAltitudeDiff10.setText(Integer.toString((int) localAltiDiff10) + UI.SPACE + UI.UNIT_LABEL_ALTITUDE);

		return altiDiff1 + altiDiff10;
	}

	/**
	 * @return tour time offset which is set in the UI
	 */
	private int getFromUITourTimeOffset() {

		final int seconds = fScaleAdjustSeconds.getSelection() - MAX_ADJUST_SECONDS;
		final int minutes = fScaleAdjustMinutes.getSelection() - MAX_ADJUST_MINUTES;

		fLabelAdjustSecondsValue.setText(Integer.toString(seconds));
		fLabelAdjustMinuteValue.setText(Integer.toString(minutes));

		return minutes * 60 + seconds;
	}

	public ArrayList<TourData> getSelectedTours() {

		final ArrayList<TourData> selectedTours = new ArrayList<TourData>();
		selectedTours.add(fSourceTour);

		return selectedTours;
	}

	@Override
	protected void okPressed() {

		saveTour();

		super.okPressed();
	}

	private void onDispose() {

		fShellImage.dispose();
		fIconPlaceholder.dispose();

		for (final Image image : fGraphImages.values()) {
			image.dispose();
		}

		TourbookPlugin.getDefault().getPluginPreferences().removePropertyChangeListener(fPrefChangeListener);
	}

	private void onModifyProperties() {

		fTargetTour.setMergedAltitudeOffset(getFromUIAltitudeOffset());
		fTargetTour.setMergedTourTimeOffset(getFromUITourTimeOffset());

		// calculate merged data
		computeMergedData();

		fTourChartConfig.isRelativeValueDiffScaling = fChkValueDiffScaling.getSelection();

		if (fChkPreviewChart.getSelection()) {
			// update chart
			updateTourChart();
		} else {
			// update only the merge layer, this is much faster
			fTourChart.update2ndAltiLayer(this, true);
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

		if (event.widget == fChkMergeAltitude) {

			// merge altitude is modified

			if (fChkMergeAltitude.getSelection()) {

				// merge altitude is selected

				fChkAdjustAltiFromSource.setSelection(fIsAdjustAltiFromSourceBackup);
				fChkAdjustAltiFromStart.setSelection(fIsAdjustAltiFromStartBackup);
			} else {

				// merge altitude is deselected

				fIsAdjustAltiFromSourceBackup = fChkAdjustAltiFromSource.getSelection();
				fIsAdjustAltiFromStartBackup = fChkAdjustAltiFromStart.getSelection();
			}

		}

		if (fChkMergeAltitude.getSelection()) {

			// ensure that one adjust altitude option is selected
			if (fChkAdjustAltiFromStart.getSelection() == false && fChkAdjustAltiFromSource.getSelection() == false) {
				fChkAdjustAltiFromSource.setSelection(true);
			}
		} else {

			// uncheck all altitude adjustments
			fChkAdjustAltiFromSource.setSelection(false);
			fChkAdjustAltiFromStart.setSelection(false);
		}

		setMergedGraphsVisible();

		onModifyProperties();
		updateTourChart();
	}

	private void onSelectResetAdjustments() {

		fScaleAdjustSeconds.setSelection(MAX_ADJUST_SECONDS);
		fScaleAdjustMinutes.setSelection(MAX_ADJUST_MINUTES);
		fScaleAltitude1.setSelection(MAX_ADJUST_ALTITUDE_1);
		fScaleAltitude10.setSelection(MAX_ADJUST_ALTITUDE_10);

		onModifyProperties();
	}

	private void onSelectResetValues() {

		/*
		 * get original data from the backuped data
		 */
		fSourceTour.timeSerie = Util.createDataSerieBackup(fBackupSourceTimeSerie);
		fSourceTour.distanceSerie = Util.createDataSerieBackup(fBackupSourceDistanceSerie);
		fSourceTour.altitudeSerie = Util.createDataSerieBackup(fBackupSourceAltitudeSerie);

		fTargetTour.pulseSerie = Util.createDataSerieBackup(fBackupTargetPulseSerie);
		fTargetTour.temperatureSerie = Util.createDataSerieBackup(fBackupTargetTemperatureSerie);
		fTargetTour.cadenceSerie = Util.createDataSerieBackup(fBackupTargetCadenceSerie);

		fTargetTour.setMergedTourTimeOffset(fBackupTargetTimeOffset);
		fTargetTour.setMergedAltitudeOffset(fBackupTargetAltitudeOffset);

		updateUIFromTourData();

		enableActions();
	}

	/**
	 * Restore values which have been modified in the dialog
	 * 
	 * @param selectedTour
	 */
	private void restoreDataBackup() {

		fSourceTour.timeSerie = fBackupSourceTimeSerie;
		fSourceTour.distanceSerie = fBackupSourceDistanceSerie;
		fSourceTour.altitudeSerie = fBackupSourceAltitudeSerie;
		fSourceTour.setTourType(fBackupSourceTourType);

		fTargetTour.pulseSerie = fBackupTargetPulseSerie;
		fTargetTour.temperatureSerie = fBackupTargetTemperatureSerie;
		fTargetTour.cadenceSerie = fBackupTargetCadenceSerie;

		fTargetTour.setMergedTourTimeOffset(fBackupTargetTimeOffset);
		fTargetTour.setMergedAltitudeOffset(fBackupTargetAltitudeOffset);
	}

	private void restoreState() {

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		fChkPreviewChart.setSelection(prefStore.getBoolean(ITourbookPreferences.MERGE_TOUR_PREVIEW_CHART));
		fChkValueDiffScaling.setSelection(prefStore.getBoolean(ITourbookPreferences.MERGE_TOUR_ALTITUDE_DIFF_SCALING));

		fChkAdjustAltiSmoothly.setSelection(prefStore.getBoolean(ITourbookPreferences.MERGE_TOUR_LINEAR_INTERPOLATION));

		fChkSynchStartTime.setSelection(prefStore.getBoolean(ITourbookPreferences.MERGE_TOUR_SYNC_START_TIME));

		/*
		 * set tour type
		 */
		fChkSetTourType.setSelection(prefStore.getBoolean(ITourbookPreferences.MERGE_TOUR_SET_TOUR_TYPE));
		if (fSourceTour.getTourPerson() == null) {

			// tour is not saved, used tour type id from pref store

			final long tourTypeId = prefStore.getLong(ITourbookPreferences.MERGE_TOUR_SET_TOUR_TYPE_ID);

			final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();

			for (final TourType tourType : tourTypes) {
				if (tourType.getTypeId() == tourTypeId) {

					fSourceTour.setTourType(tourType);

					fIsMergeSourceTourModified = true;

					break;
				}
			}
		}

		// restore merge graph state
		fChkMergeAltitude.setSelection(prefStore.getBoolean(ITourbookPreferences.MERGE_TOUR_MERGE_GRAPH_ALTITUDE));
		fChkMergePulse.setSelection(prefStore.getBoolean(ITourbookPreferences.MERGE_TOUR_MERGE_GRAPH_PULSE));
		fChkMergeTemperature.setSelection(prefStore.getBoolean(ITourbookPreferences.MERGE_TOUR_MERGE_GRAPH_TEMPERATURE));
		fChkMergeCadence.setSelection(prefStore.getBoolean(ITourbookPreferences.MERGE_TOUR_MERGE_GRAPH_CADENCE));
	}

	private void saveState() {

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		prefStore.setValue(ITourbookPreferences.MERGE_TOUR_GRAPH_X_AXIS, fTourChartConfig.showTimeOnXAxis
				? TourManager.X_AXIS_TIME
				: TourManager.X_AXIS_DISTANCE);

		prefStore.setValue(ITourbookPreferences.MERGE_TOUR_PREVIEW_CHART, fChkPreviewChart.getSelection());
		prefStore.setValue(ITourbookPreferences.MERGE_TOUR_ALTITUDE_DIFF_SCALING, fChkValueDiffScaling.getSelection());

		prefStore.setValue(ITourbookPreferences.MERGE_TOUR_LINEAR_INTERPOLATION, fChkAdjustAltiSmoothly.getSelection());
		prefStore.setValue(ITourbookPreferences.MERGE_TOUR_SET_TOUR_TYPE, fChkSetTourType.getSelection());
		prefStore.setValue(ITourbookPreferences.MERGE_TOUR_SYNC_START_TIME, fChkSynchStartTime.getSelection());

		// save tour type id
		final TourType sourceTourType = fSourceTour.getTourType();
		if (sourceTourType != null) {
			prefStore.setValue(ITourbookPreferences.MERGE_TOUR_SET_TOUR_TYPE_ID, sourceTourType.getTypeId());
		}

		// save merged tour graphs
		prefStore.setValue(ITourbookPreferences.MERGE_TOUR_MERGE_GRAPH_ALTITUDE, fChkMergeAltitude.getSelection());
		prefStore.setValue(ITourbookPreferences.MERGE_TOUR_MERGE_GRAPH_PULSE, fChkMergePulse.getSelection());
		prefStore.setValue(ITourbookPreferences.MERGE_TOUR_MERGE_GRAPH_TEMPERATURE, fChkMergeTemperature.getSelection());
		prefStore.setValue(ITourbookPreferences.MERGE_TOUR_MERGE_GRAPH_CADENCE, fChkMergeCadence.getSelection());

	}

	private void saveTour() {

		if (fChkMergeAltitude.getSelection()) {

			// set target altitude values

			if ((fChkAdjustAltiFromStart.getSelection() || fChkAdjustAltiFromSource.getSelection())
					&& fSourceTour.dataSerieAdjustedAlti != null) {

				// update target altitude from adjuste source altitude
				fTargetTour.altitudeSerie = fSourceTour.dataSerieAdjustedAlti;

				// adjust altitude up/down values
				fTargetTour.computeAltitudeUpDown();
			}
		}

		if (fChkMergePulse.getSelection()) {
			// pulse is already merged
		} else {
			// restore original pulse values because these values should not be saved
			fTargetTour.pulseSerie = fBackupTargetPulseSerie;
		}

		if (fChkMergeTemperature.getSelection()) {
			// temperature is already merged
		} else {
			// restore original temperature values because these values should not be saved
			fTargetTour.temperatureSerie = fBackupTargetTemperatureSerie;
		}

		if (fChkMergeCadence.getSelection()) {
			// pulse is already merged
		} else {
			// restore original cadence values because these values should not be saved
			fTargetTour.cadenceSerie = fBackupTargetCadenceSerie;
		}

		if (fChkSetTourType.getSelection() == false) {

			// restore backup values

			fSourceTour.setTourType(fBackupSourceTourType);
		}

		// set tour id into which the tour is merged
		fSourceTour.setTourPerson(fTargetTour.getTourPerson());
		fSourceTour.setMergeTargetTourId(fTargetTour.getTourId());
		fSourceTour.setMergeSourceTourId(null);

		// save modified tours
		final ArrayList<TourData> modifiedTours = new ArrayList<TourData>();
		modifiedTours.add(fTargetTour);
		modifiedTours.add(fSourceTour);

		TourManager.saveModifiedTours(modifiedTours);

		fIsTourSaved = true;
	}

	private void setMergedGraphsVisible() {

		final ArrayList<Integer> visibleGraphs = fTourChartConfig.getVisibleGraphs();

		if (fChkMergeAltitude.getSelection()) {
			if (visibleGraphs.contains(TourManager.GRAPH_ALTITUDE) == false) {
				visibleGraphs.add(TourManager.GRAPH_ALTITUDE);
			}
		}

		if (fChkMergePulse.getSelection()) {
			if (visibleGraphs.contains(TourManager.GRAPH_PULSE) == false) {
				visibleGraphs.add(TourManager.GRAPH_PULSE);
			}
		}

		if (fChkMergeTemperature.getSelection()) {
			if (visibleGraphs.contains(TourManager.GRAPH_TEMPERATURE) == false) {
				visibleGraphs.add(TourManager.GRAPH_TEMPERATURE);
			}
		}

		if (fChkMergeCadence.getSelection()) {
			if (visibleGraphs.contains(TourManager.GRAPH_CADENCE) == false) {
				visibleGraphs.add(TourManager.GRAPH_CADENCE);
			}
		}
	}

	public void toursAreModified(final ArrayList<TourData> modifiedTours) {

		// tour type was modified
		fIsMergeSourceTourModified = true;

		updateUIFromTourData();
	}

	private void updateTourChart() {

		fIsChartUpdated = true;

		fTourChart.updateTourChart(fTargetTour, fTourChartConfig, true);

		fIsChartUpdated = false;
	}

	private void updateUI(final float altiDiffTime, final float altiDiffDist) {

		if (fChkAdjustAltiFromStart.getSelection()) {

			fLblAdjustAltiValueTime.setText(fNumberFormatter.format(altiDiffTime));
			fLblAdjustAltiValueDistance.setText(fNumberFormatter.format(altiDiffDist));

		} else {

			// adjusted alti is disabled

			fLblAdjustAltiValueTime.setText("N/A"); //$NON-NLS-1$
			fLblAdjustAltiValueDistance.setText("N/A"); //$NON-NLS-1$
		}
	}

	/**
	 * set data from the tour into the UI
	 */
	private void updateUIFromTourData() {

		/*
		 * show time offset
		 */
		updateUITourTimeOffset(fTargetTour.getMergedTourTimeOffset());

		/*
		 * show altitude offset
		 */
		final int mergedMetricAltitudeOffset = fTargetTour.getMergedAltitudeOffset();

		final float altitudeOffset = mergedMetricAltitudeOffset;
		final int altitudeOffset1 = (int) (altitudeOffset % 10);
		final int altitudeOffset10 = (int) (altitudeOffset / 10);

		fScaleAltitude1.setSelection(altitudeOffset1 + MAX_ADJUST_ALTITUDE_1);
		fScaleAltitude10.setSelection(altitudeOffset10 + MAX_ADJUST_ALTITUDE_10);

		UI.updateUITourType(fSourceTour.getTourType(), fLblTourType);

		onModifyProperties();
	}

	private void updateUITourTimeOffset(final int tourTimeOffset) {

		final int seconds = tourTimeOffset % 60;
		final int minutes = tourTimeOffset / 60;

		fScaleAdjustSeconds.setSelection(seconds + MAX_ADJUST_SECONDS);
		fScaleAdjustMinutes.setSelection(minutes + MAX_ADJUST_MINUTES);
	}
}
