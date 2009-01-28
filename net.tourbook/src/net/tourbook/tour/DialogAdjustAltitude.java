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
import net.tourbook.chart.ChartMouseEvent;
import net.tourbook.chart.IMouseListener;
import net.tourbook.chart.ISliderMoveListener;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.data.SplineData;
import net.tourbook.data.TourData;
import net.tourbook.math.CubicSpline;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.tourChart.ChartLayer2ndAltiSerie;
import net.tourbook.ui.tourChart.I2ndAltiLayer;
import net.tourbook.ui.tourChart.SplineDrawingData;
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
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
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

	private ChartLayer2ndAltiSerie		fChartLayer2ndAltiSerie;

	private int							fPointHitIndex					= -1;
	private SplineData					fSplineData;
	private int							fAltiDiff;
	private int							fSliderDistance;

	{
		fNF.setMinimumFractionDigits(0);
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
	private void computeAltitudeUntilLeftSlider() {

		// srtm values are available, otherwise this option is not available in the combo box

		final int[] srtm2ndAlti = fTourData.dataSerie2ndAlti = fTourData.getSRTMSerieMetric();

		final int serieLength = fTourData.timeSerie.length;

		final int[] adjustedAltiSerie = fTourData.dataSerieAdjustedAlti = new int[serieLength];
		final int[] diffTo2ndAlti = fTourData.dataSerieDiffTo2ndAlti = new int[serieLength];
		final float[] splineDataSerie = fTourData.dataSerieSpline = new float[serieLength];

		final int sliderIndex = fTourChart.getXSliderPosition().getLeftSliderValueIndex();

		final int[] altitudeSerie = fTourData.altitudeSerie;
		final int[] distanceSerie = fTourData.distanceSerie;
		fSliderDistance = distanceSerie[sliderIndex];

		// get altitude diff serie
		for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {
			diffTo2ndAlti[serieIndex] = altitudeSerie[serieIndex] - srtm2ndAlti[serieIndex];
		}
		fAltiDiff = -diffTo2ndAlti[0];

		final CubicSpline cubicSpline = updateSplineData(fSliderDistance, fAltiDiff);

		// get adjusted altitude serie
		for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

			if (serieIndex < sliderIndex) {

				// add adjusted altitude

				final float distance = distanceSerie[serieIndex];
				final float distanceScale = 1 - (distance / fSliderDistance);

				final int adjustedAltiDiff = (int) (fAltiDiff * distanceScale);
				final int newAltitude = altitudeSerie[serieIndex] + adjustedAltiDiff;

				float splineAlti = 0;
				try {

					splineAlti = (float) cubicSpline.interpolate(distance);

				} catch (final IllegalArgumentException e) {
					final double[] xValues = fTourData.splineDataPoints.xValues;
					System.out.println((xValues[0] + " ")
							+ (xValues[1] + " ")
							+ (xValues[2] + " ")
							+ (xValues[3] + " ")
							+ xValues[4]);
					e.printStackTrace();
					return;
				}
				splineDataSerie[serieIndex] = splineAlti;

				final int adjustedAlti = newAltitude + (int) splineAlti;
				adjustedAltiSerie[serieIndex] = adjustedAlti;
				diffTo2ndAlti[serieIndex] = srtm2ndAlti[serieIndex] - adjustedAlti;

			} else {

				// set altitude which is not adjusted

				adjustedAltiSerie[serieIndex] = altitudeSerie[serieIndex];
			}
		}

	}

	private void computePointMoveValues(final ChartMouseEvent mouseEvent) {

		if (fPointHitIndex == -1) {
			return;
		}

		final SplineDrawingData drawingData = fChartLayer2ndAltiSerie.getDrawingData();
		final float scaleX = drawingData.scaleX;
		final float scaleY = drawingData.scaleY;
		

		float devX = drawingData.devGraphValueXOffset + mouseEvent.devXMouse;
		final float devY = drawingData.devY0Spline - mouseEvent.devYMouse;

		final float[] posX = fSplineData.relativePositionX;
		final float[] posY = fSplineData.relativePositionY;
		
		final double graphXMin = fSplineData.xMinValues[fPointHitIndex];
		final double graphXMax = fSplineData.xMaxValues[fPointHitIndex];

		float graphX = devX / scaleX;
		
		if (graphXMin != Double.NaN) {
			if (graphX < graphXMin) {
				graphX = (float) graphXMin;
			}
		}
		if (graphXMax != Double.NaN) {
			if (graphX > graphXMax) {
				graphX = (float) graphXMax;
			}
		}
		devX = graphX * scaleX;
		
		final int graph1X = fSliderDistance;
		final int graph1Y = fAltiDiff;

		final float dev1X = graph1X * scaleX;
		final float dev1Y = graph1Y * scaleY;

		final float devNewPosX = devX / dev1X;
		final float devNewPosY = devY / dev1Y;

		posX[fPointHitIndex] = devNewPosX;
		posY[fPointHitIndex] = devNewPosY;
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

		fChartLayer2ndAltiSerie = new ChartLayer2ndAltiSerie(fTourData, xDataSerie, fTourChartConfig);

		return fChartLayer2ndAltiSerie;
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

		createSplineData();

		return fDialogContainer;
	}

	/**
	 * create spline values
	 * 
	 * @param altiDiff
	 * @param sliderDistance
	 * @return
	 */
	private void createSplineData() {

		fSplineData = fTourData.splineDataPoints = new SplineData();

		final int pointLength = 5;

		fSplineData.xValues = new double[pointLength];
		fSplineData.yValues = new double[pointLength];

		final boolean[] isMovable = fSplineData.isPointMovable = new boolean[pointLength];
		final double[] splineMinX = fSplineData.xMinValues = new double[pointLength];
		final double[] splineMaxX = fSplineData.xMaxValues = new double[pointLength];

		final float[] posX = fSplineData.relativePositionX = new float[pointLength];
		final float[] posY = fSplineData.relativePositionY = new float[pointLength];

		posX[0] = -0.1f;
		posY[0] = 0;
		posX[1] = 0;
		posY[1] = 0;
		posX[2] = 0.5f;
		posY[2] = 0;
		posX[3] = 1f;
		posY[3] = 0;
		posX[4] = 1.1f;
		posY[4] = 0;

		isMovable[0] = false;
		isMovable[1] = true;
		isMovable[2] = true;
		isMovable[3] = true;
		isMovable[4] = false;

		splineMinX[0] = Double.NaN;
		splineMaxX[0] = Double.NaN;
		splineMinX[1] = 0;
		splineMaxX[1] = 0;
		splineMinX[2] = 0;
		splineMaxX[2] = 0;
		splineMinX[3] = 0;
		splineMaxX[3] = 0;
		splineMinX[4] = Double.NaN;
		splineMaxX[4] = Double.NaN;
	}

	private void createUI(final Composite parent) {

		createUIAdjustmentType(parent);
		createUITourChart(parent);

//		final Composite splineContainer = new Composite(parent, SWT.NONE);
//		GridDataFactory.fillDefaults().grab(true, false).applyTo(splineContainer);
//		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(splineContainer);
//
//		createUISpline(splineContainer);
//		createUISplineActions(splineContainer);

//		createUIAdjustments(parent);
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

		fTourChart.addMouseListener(new IMouseListener() {

			public void mouseDoubleClick(final ChartMouseEvent event) {}

			public void mouseDown(final ChartMouseEvent event) {
				onMouseDown(event);
			}

			public void mouseMove(final ChartMouseEvent chartEvent) {
				onMouseMove(chartEvent);
			}

			public void mouseUp(final ChartMouseEvent event) {
				onMouseUp(event);
			}
		});
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		return TourbookPlugin.getDefault().getDialogSettingsSection(getClass().getName() + "_DialogBounds"); //$NON-NLS-1$
	}

	@Override
	protected void okPressed() {

		saveTour();

		super.okPressed();
	}

	private void onModifyProperties() {

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
			computeAltitudeUntilLeftSlider();
			break;

		default:
			break;
		}

		fTourChart.update2ndAltiLayer(this, true);
	}

	private void onMouseDown(final ChartMouseEvent mouseEvent) {

		if (fChartLayer2ndAltiSerie == null) {
			return;
		}

		final Rectangle[] pointHitRectangles = fChartLayer2ndAltiSerie.getPointHitRectangels();
		if (pointHitRectangles == null) {
			return;
		}

		fPointHitIndex = -1;
		final boolean[] isPointMovable = fSplineData.isPointMovable;

		// check if the mouse hits a spline point
		for (int pointIndex = 0; pointIndex < pointHitRectangles.length; pointIndex++) {

			if (isPointMovable[pointIndex] == false) {
				// ignore none movable points
				continue;
			}

			if (pointHitRectangles[pointIndex].contains(mouseEvent.devXMouse, mouseEvent.devYMouse)) {

				fPointHitIndex = pointIndex;

				mouseEvent.isWorked = true;
				break;
			}
		}
	}

	private void onMouseMove(final ChartMouseEvent mouseEvent) {

		if (fChartLayer2ndAltiSerie == null) {
			return;
		}

		final Rectangle[] pointHitRectangles = fChartLayer2ndAltiSerie.getPointHitRectangels();
		if (pointHitRectangles == null) {
			return;
		}

		if (fPointHitIndex != -1) {

			// point is moved

			computePointMoveValues(mouseEvent);

			onModifyProperties();

			mouseEvent.isWorked = true;

		} else {

			// point is not moved, check if the mouse hits a spline point

			final boolean[] isPointMovable = fSplineData.isPointMovable;
			for (int pointIndex = 0; pointIndex < pointHitRectangles.length; pointIndex++) {

				if (isPointMovable[pointIndex] == false) {
					// ignore none movable points
					continue;
				}

				if (pointHitRectangles[pointIndex].contains(mouseEvent.devXMouse, mouseEvent.devYMouse)) {
					mouseEvent.isWorked = true;
					break;
				}
			}
		}
	}

	private void onMouseUp(final ChartMouseEvent mouseEvent) {

		if (fPointHitIndex == -1) {
			return;
		}

		mouseEvent.isWorked = true;
		fPointHitIndex = -1;
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

	private CubicSpline updateSplineData(final int sliderDistance, final int altiDiff) {

		final double[] splineX = fSplineData.xValues;
		final double[] splineY = fSplineData.yValues;

		final double[] splineMinX = fSplineData.xMinValues;
		final double[] splineMaxX = fSplineData.xMaxValues;

		final float[] posX = fSplineData.relativePositionX;
		final float[] posY = fSplineData.relativePositionY;

		splineX[0] = -0.1f;
		splineX[1] = posX[1] * sliderDistance;
		splineX[2] = posX[2] * sliderDistance;
		splineX[3] = posX[3] * sliderDistance;
		splineX[4] = sliderDistance + 0.1f;

		splineY[0] = 0;
		splineY[1] = posY[1] * altiDiff;
		splineY[2] = posY[2] * altiDiff;
		splineY[3] = posY[3] * altiDiff;
		splineY[4] = 0;

		splineMinX[0] = Double.NaN;
		splineMaxX[0] = Double.NaN;

		splineMinX[1] = 0;
		splineMaxX[1] = sliderDistance;

		splineMinX[2] = 0;
		splineMaxX[2] = sliderDistance;

		splineMinX[3] = 0;
		splineMaxX[3] = sliderDistance;

		splineMinX[4] = Double.NaN;
		splineMaxX[4] = Double.NaN;

		return new CubicSpline(splineX, splineY);
	}

	private void updateTourChart() {

		fIsChartUpdated = true;

		fTourChart.updateTourChart(fTourData, fTourChartConfig, true);

		fIsChartUpdated = false;
	}

}
