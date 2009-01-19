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

package net.tourbook.tour;

import java.text.NumberFormat;
import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ISliderMoveListener;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.data.TourData;
import net.tourbook.math.CubicSpline;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.tourChart.ChartLayer2ndAltiSerie;
import net.tourbook.ui.tourChart.I2ndAltiLayer;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.tourChart.TourChartConfiguration;
import net.tourbook.util.Util;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog to adjust the altitude, this dialog can be opened from within a tour chart or from the
 * tree viewer
 */
public class DialogAdjustAltitude extends TitleAreaDialog implements I2ndAltiLayer {

	private static final int			ADJUST_TYPE_UNTIL_LEFT_SLIDER	= 1000;
	private static final int			ADJUST_TYPE_WHOLE_TOUR			= 1001;
	private static final int			ADJUST_TYPE_START_AND_END		= 1002;
	private static final int			ADJUST_TYPE_MAX_HEIGHT			= 1003;
	private static final int			ADJUST_TYPE_END					= 1004;

	private static final String			PREF_ADJUST_TYPE				= "adjust.altitude.adjust_type";	//$NON-NLS-1$

	private final IPreferenceStore		fPrefStore						= TourbookPlugin.getDefault()
																				.getPreferenceStore();
	private final NumberFormat			fNF								= NumberFormat.getNumberInstance();

	private Image						fShellImage;

	private boolean						fIsChartUpdated;
	private boolean						fIsTourSaved					= false;

	private TourData					fTourData;
	private int[]						fBackupAltitudeSerie;
	private int[]						fSrtmValues;

	private TourChart					fTourChart;
	private TourChartConfiguration		fTourChartConfig;

	private Composite					fDialogContainer;

	private Combo						fComboAdjustType;

	private static AdjustmentType[]		fAllAdjustmentTypes;
	private ArrayList<AdjustmentType>	fAvailableAdjustTypes			= new ArrayList<AdjustmentType>();

	private Scale						fScaleSrmtMathVarX1;
	private Scale						fScaleSrmtMathVarY1;
	private Scale						fScaleSrmtMathVarX2;
	private Scale						fScaleSrmtMathVarY2;
	private Scale						fScaleSrmtMathVarXBorder;
	private Scale						fScaleSrmtMathVarYBorder;

	private Label						fLabelMathVarX1;
	private Label						fLabelMathVarY1;
	private Label						fLabelMathVarX2;
	private Label						fLabelMathVarY2;
	private Label						fLabelMathVarXBorder;
	private Label						fLabelMathVarYBorder;

	private float						fMathVarX1;
	private float						fMathVarY1;
	private float						fMathVarX2;
	private float						fMathVarY2;
	private float						fMathVarXBorder;
	private float						fMathVarYBorder;

	{
		fNF.setMinimumFractionDigits(3);
		fNF.setMaximumFractionDigits(3);

		fAllAdjustmentTypes = new AdjustmentType[] {
			new AdjustmentType(ADJUST_TYPE_UNTIL_LEFT_SLIDER, Messages.adjust_altitude_type_until_left_slider),
			new AdjustmentType(ADJUST_TYPE_WHOLE_TOUR, Messages.adjust_altitude_type_adjust_whole_tour),
			new AdjustmentType(ADJUST_TYPE_START_AND_END, Messages.adjust_altitude_type_start_and_end),
			new AdjustmentType(ADJUST_TYPE_MAX_HEIGHT, Messages.adjust_altitude_type_adjust_height),
			new AdjustmentType(ADJUST_TYPE_END, Messages.adjust_altitude_type_adjust_end) //
		};
	};

	private class AdjustmentType {

		int		id;
		String	visibleName;

		AdjustmentType(final int id, final String visibleName) {
			this.id = id;
			this.visibleName = visibleName;
		}
	}

	public DialogAdjustAltitude(final Shell parentShell, final TourData tourData) {

		super(parentShell);

		fTourData = tourData;
		fSrtmValues = fTourData.getSRTMSerie();

		// set icon for the window 
		fShellImage = TourbookPlugin.getImageDescriptor(Messages.Image__edit_adjust_altitude).createImage();
		setDefaultImage(fShellImage);

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
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

	/**
	 * adjust start altitude until left slider
	 */
	private void computeAdjustTypeUntilLeftSlider() {

		// srtm values must be available, otherwise this option is not available in the combo box

		final int[] srtm2ndAlti = fTourData.dataSerie2ndAlti = fTourData.getSRTMSerieMetric();

		final int serieLength = fTourData.timeSerie.length;

		final int[] adjustedAlti = new int[serieLength];
		final int[] diffTo2ndAlti = new int[serieLength];
		final int sliderIndex = fTourChart.getXSliderPosition().getLeftSliderValueIndex();

		final int[] altitudeSerie = fTourData.altitudeSerie;
		final int[] distanceSerie = fTourData.distanceSerie;
		final float sliderDistance = distanceSerie[sliderIndex];

		// get altitude diff serie
		for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {
			diffTo2ndAlti[serieIndex] = altitudeSerie[serieIndex] - srtm2ndAlti[serieIndex];
		}
		final float startAltiDiff = -diffTo2ndAlti[0];

		/*
		 * create spline values
		 */
		final int spPointLength = 6;
		final double[] splineDistance = new double[spPointLength];
		final double[] splineAltitude = new double[spPointLength];

		final double xBorder = fMathVarXBorder / 1;
		final double yBorder = fMathVarYBorder * 10;

		splineDistance[0] = -1 - xBorder * sliderDistance / 2;
		splineDistance[1] = 0;
		splineDistance[2] = fMathVarY1 * sliderDistance;
		splineDistance[3] = fMathVarY2 * sliderDistance;
		splineDistance[4] = sliderDistance;
		splineDistance[5] = 0.001 + sliderDistance + xBorder * sliderDistance / 2;

		splineAltitude[0] = yBorder * startAltiDiff / 2;
		splineAltitude[1] = 0;
		splineAltitude[2] = fMathVarX1 * startAltiDiff;
		splineAltitude[3] = fMathVarX2 * startAltiDiff;
		splineAltitude[4] = 0;
		splineAltitude[5] = yBorder * startAltiDiff / 2;

		final int[][] splinePoints = fTourData.altiDiffSpecialPoints = new int[2][spPointLength];
		for (int pointIndex = 0; pointIndex < spPointLength; pointIndex++) {
			splinePoints[0][pointIndex] = (int) splineDistance[pointIndex]; // X-axis
			splinePoints[1][pointIndex] = (int) splineAltitude[pointIndex]; // Y-axis

			System.out.println("x:" + splinePoints[0][pointIndex] + "\ty:" + splinePoints[1][pointIndex]);
			// TODO remove SYSTEM.OUT.PRINTLN
		}
		System.out.println();
// TODO remove SYSTEM.OUT.PRINTLN

		final CubicSpline cubicSpline = new CubicSpline(splineDistance, splineAltitude);

		// get adjusted altitude serie
		for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

			if (serieIndex < sliderIndex) {

				// add adjusted altitude

				final float distance = distanceSerie[serieIndex];
				final float distanceScale = 1 - distance / sliderDistance;

				final int adjustedAltiDiff = (int) (startAltiDiff * distanceScale);
				final int newAltitude = altitudeSerie[serieIndex] + adjustedAltiDiff;

//				final int splineAlti = (int) cubicSpline.interpolate(distance);
				final int splineAlti = (int) cubicSpline.interpolate(distance);

				adjustedAlti[serieIndex] = newAltitude - splineAlti;
//				diffTo2ndAlti[serieIndex] = srtm2ndAlti[serieIndex] - newAltitude + splineAlti;
				diffTo2ndAlti[serieIndex] = splineAlti;

			} else {

				// set altitude which is not adjusted

				adjustedAlti[serieIndex] = altitudeSerie[serieIndex];
			}
		}

		fTourData.dataSerieAdjustedAlti = adjustedAlti;
		fTourData.dataSerieDiffTo2ndAlti = diffTo2ndAlti;
	}

	@Override
	protected void configureShell(final Shell shell) {

		super.configureShell(shell);

		shell.setText(Messages.adjust_altitude_dlg_shell_title);
	}

	@Override
	public void create() {

		createDataBackup();

		// create UI widgets
		super.create();

		restoreState();

		setTitle(Messages.adjust_altitude_dlg_dialog_title);
		setMessage(NLS.bind(Messages.adjust_altitude_dlg_dialog_message, TourManager.getTourTitle(fTourData)));

		updateTourChart();
	}

	public ChartLayer2ndAltiSerie create2ndAltiLayer() {

		final int[] xDataSerie = fTourChartConfig.showTimeOnXAxis ? fTourData.timeSerie : fTourData.getDistanceSerie();

		return new ChartLayer2ndAltiSerie(fTourData, xDataSerie, fTourChartConfig);
	}

	private void createDataBackup() {

		/*
		 * keep a backup of the altitude data because these data will be changed in this dialog
		 */
		fBackupAltitudeSerie = Util.createDataSerieBackup(fTourData.altitudeSerie);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		fDialogContainer = (Composite) super.createDialogArea(parent);

		final Composite container = new Composite(fDialogContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().margins(10, 0).applyTo(container);

		createUI(container);

		return fDialogContainer;
	}

	private void createUI(final Composite parent) {

		createUIAdjustmentType(parent);
		createUITourChart(parent);
		createUIAdjustments(parent);
	}

	private void createUIAdjustments(final Composite parent) {

		final Composite scaleContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(scaleContainer);
		GridLayoutFactory.fillDefaults().numColumns(4).applyTo(scaleContainer);

		/*
		 * scale: math var X1
		 */
		fScaleSrmtMathVarX1 = new Scale(scaleContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleSrmtMathVarX1);
		fScaleSrmtMathVarX1.setMaximum(100);
		fScaleSrmtMathVarX1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyProperties();
			}
		});

		fLabelMathVarX1 = new Label(scaleContainer, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).hint(40, SWT.DEFAULT).applyTo(fLabelMathVarX1);

		/*
		 * scale: math var Y1
		 */
		fScaleSrmtMathVarY1 = new Scale(scaleContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleSrmtMathVarY1);
		fScaleSrmtMathVarY1.setMaximum(100);
		fScaleSrmtMathVarY1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyProperties();
			}
		});

		fLabelMathVarY1 = new Label(scaleContainer, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).hint(40, SWT.DEFAULT).applyTo(fLabelMathVarY1);

		/*
		 * scale: math var X2
		 */
		fScaleSrmtMathVarX2 = new Scale(scaleContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleSrmtMathVarX2);
		fScaleSrmtMathVarX2.setMaximum(100);
		fScaleSrmtMathVarX2.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyProperties();
			}
		});

		fLabelMathVarX2 = new Label(scaleContainer, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).hint(40, SWT.DEFAULT).applyTo(fLabelMathVarX2);

		/*
		 * scale: math var Y2
		 */
		fScaleSrmtMathVarY2 = new Scale(scaleContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleSrmtMathVarY2);
		fScaleSrmtMathVarY2.setMaximum(100);
		fScaleSrmtMathVarY2.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyProperties();
			}
		});

		fLabelMathVarY2 = new Label(scaleContainer, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).hint(40, SWT.DEFAULT).applyTo(fLabelMathVarY2);

		/*
		 * scale: math var X border
		 */
		fScaleSrmtMathVarXBorder = new Scale(scaleContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleSrmtMathVarXBorder);
		fScaleSrmtMathVarXBorder.setMaximum(100);
		fScaleSrmtMathVarXBorder.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyProperties();
			}
		});

		fLabelMathVarXBorder = new Label(scaleContainer, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).hint(40, SWT.DEFAULT).applyTo(fLabelMathVarXBorder);

		/*
		 * scale: math var Y border
		 */
		fScaleSrmtMathVarYBorder = new Scale(scaleContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleSrmtMathVarYBorder);
		fScaleSrmtMathVarYBorder.setMaximum(100);
		fScaleSrmtMathVarYBorder.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyProperties();
			}
		});

		fLabelMathVarYBorder = new Label(scaleContainer, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).hint(40, SWT.DEFAULT).applyTo(fLabelMathVarYBorder);
	}

	private void createUIAdjustmentType(final Composite parent) {

		/*
		 * combo: adjust type
		 */
		final Composite typeContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(typeContainer);
		GridLayoutFactory.fillDefaults().numColumns(2).extendedMargins(0, 0, 5, 0).applyTo(typeContainer);

		final Label label = new Label(typeContainer, SWT.NONE);
		label.setText(Messages.adjust_altitude_label_adjustment_type);

		fComboAdjustType = new Combo(typeContainer, SWT.DROP_DOWN | SWT.READ_ONLY);
		fComboAdjustType.setVisibleItemCount(20);
		fComboAdjustType.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyProperties();
			}
		});

		// fill combo
		for (final AdjustmentType adjustType : fAllAdjustmentTypes) {

			if (adjustType.id == ADJUST_TYPE_UNTIL_LEFT_SLIDER && fSrtmValues == null) {
				// skip this type it requires srtm data
				continue;
			}

			fAvailableAdjustTypes.add(adjustType);

			fComboAdjustType.add(adjustType.visibleName);
		}
	}

	private void createUITourChart(final Composite parent) {

		fTourChart = new TourChart(parent, SWT.BORDER, true);
		GridDataFactory.fillDefaults().grab(true, true).indent(0, 0).minSize(300, 200).applyTo(fTourChart);

		// hide the toolbar
//		fTourChart.setToolBarManager(null, false);

		fTourChart.setShowZoomActions(true);
		fTourChart.setShowSlider(true);

		// set altitude visible
		fTourChartConfig = new TourChartConfiguration(true);
		fTourChartConfig.addVisibleGraph(TourManager.GRAPH_ALTITUDE);

		// show the srtm values in the 2nd data serie layer
		fTourChartConfig.isSRTMDataVisible = false;

		// overwrite x-axis from pref store
		fTourChartConfig.setIsShowTimeOnXAxis(fPrefStore.getString(ITourbookPreferences.ADJUST_ALTITUDE_CHART_X_AXIS_UNIT)
				.equals(TourManager.X_AXIS_TIME));

		fTourChart.addDataModelListener(new IDataModelListener() {
			public void dataModelChanged(final ChartDataModel changedChartDataModel) {
				// set title
				changedChartDataModel.setTitle(TourManager.getTourTitleDetailed(fTourData));
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

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		return TourbookPlugin.getDefault().getDialogSettingsSection(getClass().getName() + "_DialogBounds"); //$NON-NLS-1$
	}

	private void getValuesFromUI() {

		fMathVarX1 = (float) fScaleSrmtMathVarX1.getSelection() / 100;
		fMathVarY1 = (float) fScaleSrmtMathVarY1.getSelection() / 100;
		fMathVarX2 = (float) fScaleSrmtMathVarX2.getSelection() / 100;
		fMathVarY2 = (float) fScaleSrmtMathVarY2.getSelection() / 100;

		fMathVarXBorder = (float) fScaleSrmtMathVarXBorder.getSelection() / 10;
		fMathVarYBorder = (float) (fScaleSrmtMathVarYBorder.getSelection() - 50) / 10;
	}

	@Override
	protected void okPressed() {

		saveTour();

		super.okPressed();
	}

	private void onModifyProperties() {

		getValuesFromUI();

		// hide 2nd alti diff & adjustment
		fTourData.dataSerieAdjustedAlti = null;
		fTourData.dataSerieDiffTo2ndAlti = null;

		final int comboIndex = fComboAdjustType.getSelectionIndex();
		AdjustmentType selectedAdjustType;
		if (comboIndex == -1) {
			fComboAdjustType.select(0);
			selectedAdjustType = fAvailableAdjustTypes.get(0);
		} else {
			selectedAdjustType = fAvailableAdjustTypes.get(comboIndex);
		}

		switch (selectedAdjustType.id) {
		case ADJUST_TYPE_UNTIL_LEFT_SLIDER:
			computeAdjustTypeUntilLeftSlider();
			break;

		default:
			break;
		}

		updateUI();

		fTourChart.update2ndAltiLayer(this, true);
	}

	/**
	 * Restore values which have been modified in the dialog
	 * 
	 * @param selectedTour
	 */
	private void restoreDataBackup() {

		fTourData.altitudeSerie = fBackupAltitudeSerie;
	}

	/**
	 * copy the old altitude values back into the tourdata altitude serie
	 */
	public void restoreOriginalAltitudeValues() {

		final int[] altitudeSerie = fTourData.altitudeSerie;

		if (altitudeSerie == null | fBackupAltitudeSerie == null) {
			return;
		}

		for (int altiIndex = 0; altiIndex < altitudeSerie.length; altiIndex++) {
			altitudeSerie[altiIndex] = fBackupAltitudeSerie[altiIndex];
		}

		// recompute imperial altitude values
		fTourData.clearAltitudeSeries();
	}

	private void restoreState() {

		// get previous selected adjustment type, use first type if not found
		final int prefAdjustType = fPrefStore.getInt(PREF_ADJUST_TYPE);
		int comboIndex = 0;
		int typeIndex = 0;
		for (final AdjustmentType availAdjustType : fAvailableAdjustTypes) {
			if (prefAdjustType == availAdjustType.id) {
				comboIndex = typeIndex;
				break;
			}
			typeIndex++;
		}

		fComboAdjustType.select(comboIndex);

	}

	private void saveState() {

		fPrefStore.setValue(PREF_ADJUST_TYPE, fComboAdjustType.getSelectionIndex());

		fPrefStore.setValue(ITourbookPreferences.ADJUST_ALTITUDE_CHART_X_AXIS_UNIT, fTourChartConfig.showTimeOnXAxis
				? TourManager.X_AXIS_TIME
				: TourManager.X_AXIS_DISTANCE);

	}

	private void saveTour() {

		fIsTourSaved = true;
	}

	private void updateTourChart() {

		fIsChartUpdated = true;

		fTourChart.updateTourChart(fTourData, fTourChartConfig, true);

		fIsChartUpdated = false;
	}

	private void updateUI() {

		fLabelMathVarX1.setText(fNF.format(fMathVarX1));
		fLabelMathVarX1.pack(true);

		fLabelMathVarY1.setText(fNF.format(fMathVarY1));
		fLabelMathVarY1.pack(true);

		fLabelMathVarX2.setText(fNF.format(fMathVarX2));
		fLabelMathVarX2.pack(true);

		fLabelMathVarY2.setText(fNF.format(fMathVarY2));
		fLabelMathVarY2.pack(true);

		fLabelMathVarXBorder.setText(fNF.format(fMathVarXBorder));
		fLabelMathVarXBorder.pack(true);
		fLabelMathVarYBorder.setText(fNF.format(fMathVarYBorder));
		fLabelMathVarYBorder.pack(true);
	}

}
