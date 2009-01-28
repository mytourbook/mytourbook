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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;

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
//	private static final String				PREF_SYNCH_MIN_MAX_IS_CHECKED	= "adjust.altitude.synch-min-max.is-checked";	//$NON-NLS-1$

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

	private Scale						fUIScaleX1;
	private Scale						fUIScaleY1;
	private Scale						fUIScaleX1Border;
	private Scale						fUIScaleY1Border;
	private Scale						fUIScaleX2Border;
	private Scale						fUIScaleY2Border;

	private float						fUIScaleValueX1;
	private float						fUIScaleValueY1;
	private float						fUIScaleValueX1Border;
	private float						fUIScaleValueY1Border;
	private float						fUIScaleValueX2Border;
	private float						fUIScaleValueY2Border;

	private float						fUIValueX1;
	private float						fUIValueY1;
	private float						fUIValueX2Border;
	private float						fUIValueY2Border;
	private float						fUIValueX1Border;
	private float						fUIValueY1Border;

	private Label						fLabelX1;
	private Label						fLabelY1;
	private Label						fLabelX1Border;
	private Label						fLabelY1Border;
	private Label						fLabelX2Border;
	private Label						fLabelY2Border;

	private ChartLayer2ndAltiSerie		fChartLayer2ndAltiSerie;

	private int							fPointHitIndex					= -1;

	private float						fSliderDistance;
	private float						fStartAltiDiff;

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

		// srtm values must be available, otherwise this option is not available in the combo box

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
		fStartAltiDiff = -diffTo2ndAlti[0];

		final CubicSpline cubicSpline = createSplineData();

		// get adjusted altitude serie
		for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

			if (serieIndex < sliderIndex) {

				// add adjusted altitude

				final float distance = distanceSerie[serieIndex];
				final float distanceScale = 1 - (distance / fSliderDistance);

				final int adjustedAltiDiff = (int) (fStartAltiDiff * distanceScale);
				final int newAltitude = altitudeSerie[serieIndex] + adjustedAltiDiff;

				final float splineAlti = (float) cubicSpline.interpolate(distance);
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

		final SplineDrawingData drawingData = fChartLayer2ndAltiSerie.getDrawingData();

		final int devX = drawingData.devGraphValueXOffset + mouseEvent.devXMouse;
		final int devY = drawingData.devY0Spline - mouseEvent.devYMouse;

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

		// initialize scale, remove it from the same position as the next spline position
		fUIScaleX1Border.setSelection(0);
		fUIScaleX1.setSelection(50);
		fUIScaleX2Border.setSelection(100);

		fUIScaleY1Border.setSelection(50);
		fUIScaleY1.setSelection(50);
		fUIScaleY2Border.setSelection(50);

		return fDialogContainer;
	}

	/**
	 * create spline values
	 */
	private CubicSpline createSplineData() {

		/*
		 * create UI scale data
		 */
		fUIValueX1Border = 0.1f + fUIScaleValueX1Border * fSliderDistance;
		fUIValueY1Border = 0.0f + fUIScaleValueY1Border * fStartAltiDiff;

		fUIValueX1 = fUIScaleValueX1 * fSliderDistance;
		fUIValueY1 = fUIScaleValueY1 * fStartAltiDiff;

		fUIValueX2Border = 0.1f + fUIScaleValueX2Border * fSliderDistance;
		fUIValueY2Border = 0.0f + fUIScaleValueY2Border * fStartAltiDiff;

		final int spPointLength = 5;

		final SplineData splineData = fTourData.splineDataPoints = new SplineData();

		final double[] splineX = splineData.xValues = new double[spPointLength];
		final double[] splineY = splineData.yValues = new double[spPointLength];
		final boolean[] isMovable = splineData.isPointMovable = new boolean[spPointLength];
		final double[] splineMinX = splineData.xMinValues = new double[spPointLength];
		final double[] splineMaxX = splineData.xMaxValues = new double[spPointLength];

		splineX[0] = 0;
		splineY[0] = 0;
		splineX[1] = fUIValueX1Border;
		splineY[1] = -fUIValueY1Border;
		splineX[2] = fUIValueX1;
		splineY[2] = -fUIValueY1;
		splineX[3] = fUIValueX2Border;
		splineY[3] = -fUIValueY2Border;
		splineX[4] = fSliderDistance;
		splineY[4] = 0;

		isMovable[0] = false;
		isMovable[1] = true;
		isMovable[2] = true;
		isMovable[3] = true;
		isMovable[4] = false;

		splineMinX[0] = Double.NaN;
		splineMaxX[0] = Double.NaN;
		splineMinX[1] = Double.NaN;
		splineMaxX[1] = 0;
		splineMinX[2] = 0;
		splineMaxX[2] = fSliderDistance;
		splineMinX[3] = fSliderDistance;
		splineMaxX[3] = Double.NaN;
		splineMinX[4] = Double.NaN;
		splineMaxX[4] = Double.NaN;

		return new CubicSpline(splineX, splineY);
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

		createUIAdjustments(parent);
	}

	private void createUIAdjustments(final Composite parent) {

		final Composite scaleContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(scaleContainer);
		GridLayoutFactory.fillDefaults().numColumns(4).applyTo(scaleContainer);

		/*
		 * scale: math var X1 border
		 */
		fUIScaleX1Border = new Scale(scaleContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fUIScaleX1Border);
		fUIScaleX1Border.setMaximum(100);
		fUIScaleX1Border.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyProperties();
			}
		});
		fUIScaleX1Border.addListener(SWT.MouseDoubleClick, new Listener() {
			public void handleEvent(final Event event) {
				onScaleDoubleClick(event.widget);
			}
		});

		fLabelX1Border = new Label(scaleContainer, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).hint(40, SWT.DEFAULT).applyTo(fLabelX1Border);

		/*
		 * scale: math var Y1 border
		 */
		fUIScaleY1Border = new Scale(scaleContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fUIScaleY1Border);
		fUIScaleY1Border.setMaximum(100);
		fUIScaleY1Border.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyProperties();
			}
		});
		fUIScaleY1Border.addListener(SWT.MouseDoubleClick, new Listener() {
			public void handleEvent(final Event event) {
				onScaleDoubleClick(event.widget);
			}
		});

		fLabelY1Border = new Label(scaleContainer, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).hint(40, SWT.DEFAULT).applyTo(fLabelY1Border);

		/*
		 * scale: math var X1
		 */
		fUIScaleX1 = new Scale(scaleContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fUIScaleX1);
		fUIScaleX1.setMaximum(100);
		fUIScaleX1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyProperties();
			}
		});
		fUIScaleX1.addListener(SWT.MouseDoubleClick, new Listener() {
			public void handleEvent(final Event event) {
				onScaleDoubleClick(event.widget);
			}
		});

		fLabelX1 = new Label(scaleContainer, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).hint(40, SWT.DEFAULT).applyTo(fLabelX1);

		/*
		 * scale: math var Y1
		 */
		fUIScaleY1 = new Scale(scaleContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fUIScaleY1);
		fUIScaleY1.setMaximum(100);
		fUIScaleY1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyProperties();
			}
		});
		fUIScaleY1.addListener(SWT.MouseDoubleClick, new Listener() {
			public void handleEvent(final Event event) {
				onScaleDoubleClick(event.widget);
			}
		});

		fLabelY1 = new Label(scaleContainer, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).hint(40, SWT.DEFAULT).applyTo(fLabelY1);

		/*
		 * scale: math var X2 border
		 */
		fUIScaleX2Border = new Scale(scaleContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fUIScaleX2Border);
		fUIScaleX2Border.setMaximum(100);
		fUIScaleX2Border.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyProperties();
			}
		});
		fUIScaleX2Border.addListener(SWT.MouseDoubleClick, new Listener() {
			public void handleEvent(final Event event) {
				onScaleDoubleClick(event.widget);
			}
		});

		fLabelX2Border = new Label(scaleContainer, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).hint(40, SWT.DEFAULT).applyTo(fLabelX2Border);

		/*
		 * scale: math var Y2 border
		 */
		fUIScaleY2Border = new Scale(scaleContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fUIScaleY2Border);
		fUIScaleY2Border.setMaximum(100);
		fUIScaleY2Border.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyProperties();
			}
		});
		fUIScaleY2Border.addListener(SWT.MouseDoubleClick, new Listener() {
			public void handleEvent(final Event event) {
				onScaleDoubleClick(event.widget);
			}
		});

		fLabelY2Border = new Label(scaleContainer, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).hint(40, SWT.DEFAULT).applyTo(fLabelY2Border);
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

	private void getValuesFromUI() {

		fUIScaleValueX1Border = (float) fUIScaleX1Border.getSelection() / 100;
		fUIScaleValueY1Border = (float) (fUIScaleY1Border.getSelection() - 50) / 100;

		fUIScaleValueX1 = (float) fUIScaleX1.getSelection() / 100;
		fUIScaleValueY1 = (float) (fUIScaleY1.getSelection() - 50) / 100;

		fUIScaleValueX2Border = (float) fUIScaleX2Border.getSelection() / 100;
		fUIScaleValueY2Border = (float) (fUIScaleY2Border.getSelection() - 50) / 100;
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
			computeAltitudeUntilLeftSlider();
			break;

		default:
			break;
		}

		updateUI();

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

		// check if the mouse hits a spline point
		for (int pointIndex = 0; pointIndex < pointHitRectangles.length; pointIndex++) {

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
			computeAltitudeUntilLeftSlider();

			updateUI();
			fTourChart.update2ndAltiLayer(this, true);

		} else {

			// point is not moved, check if the mouse hits a spline point

			for (final Rectangle pointRect : pointHitRectangles) {
				if (pointRect.contains(mouseEvent.devXMouse, mouseEvent.devYMouse)) {
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

	private void onScaleDoubleClick(final Widget widget) {

		final Scale scale = (Scale) widget;
		final int max = scale.getMaximum();

		scale.setSelection(max / 2);

		onModifyProperties();
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

		fLabelX1.setText(Integer.toString((int) fUIValueX1));
		fLabelX1.pack(true);
		fLabelY1.setText(Integer.toString((int) -fUIValueY1));
		fLabelY1.pack(true);

		fLabelX1Border.setText(Integer.toString((int) fUIValueX1Border));
		fLabelX1Border.pack(true);
		fLabelY1Border.setText(Integer.toString((int) -fUIValueY1Border));
		fLabelY1Border.pack(true);

		fLabelX2Border.setText(Integer.toString((int) fUIValueX2Border));
		fLabelX2Border.pack(true);
		fLabelY2Border.setText(Integer.toString((int) -fUIValueY2Border));
		fLabelY2Border.pack(true);
	}

}
