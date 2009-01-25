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
import net.tourbook.data.SplineData;
import net.tourbook.data.TourData;
import net.tourbook.math.CubicSpline;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.SplineGraph;
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

	private Scale						fScaleSrmtVarX1;
	private Scale						fScaleSrmtVarY1;
	private Scale						fScaleSrmtVarX1Border;
	private Scale						fScaleSrmtVarY1Border;
	private Scale						fScaleSrmtVarX2Border;
	private Scale						fScaleSrmtVarY2Border;

	private Label						fLabelVarX1;
	private Label						fLabelVarY1;
	private Label						fLabelVarX1Border;
	private Label						fLabelVarY1Border;
	private Label						fLabelVarX2Border;
	private Label						fLabelVarY2Border;

	private float						fVarX1;
	private float						fVarY1;
	private float						fVarX1Border;
	private float						fVarY1Border;
	private float						fVarX2Border;
	private float						fVarY2Border;
	private float						fUIVarX1;
	private float						fUIVarY1;
	private float						fUIVarX2Border;
	private float						fUIVarY2Border;
	private float						fUIVarX1Border;
	private float						fUIVarY1Border;
	private SplineGraph					fSpline;

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
	private void computeAdjustTypeUntilLeftSlider() {

		// srtm values must be available, otherwise this option is not available in the combo box

		final int[] srtm2ndAlti = fTourData.dataSerie2ndAlti = fTourData.getSRTMSerieMetric();

		final int serieLength = fTourData.timeSerie.length;

		final int[] adjustedAltiSerie = new int[serieLength];
		final int[] diffTo2ndAlti = new int[serieLength];
		final int[] spline = new int[serieLength];
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
		 * create UI scale data
		 */
		fUIVarX1 = fVarX1 * sliderDistance;
		fUIVarY1 = fVarY1 * startAltiDiff * 1;

		fUIVarX1Border = -0.1f - fVarX1Border * sliderDistance;
		fUIVarX2Border = 0.1f + fVarX2Border * sliderDistance;

		fUIVarY1Border = 0.0f + fVarY1Border * startAltiDiff * 1;
		fUIVarY2Border = 0.0f + fVarY2Border * startAltiDiff * 1;

		/*
		 * create spline values
		 */
		final int spPointLength = 5;
		final double[] splineX = new double[spPointLength];
		final double[] splineY = new double[spPointLength];
		final boolean[] isMovable = new boolean[spPointLength];
		final double[] splineMinX = new double[spPointLength];
		final double[] splineMaxX = new double[spPointLength];

		final SplineData splineData = new SplineData();
		splineData.xValues = splineX;
		splineData.yValues = splineY;
		splineData.isPointMovable = isMovable;
		splineData.xMinValues = splineMinX;
		splineData.xMaxValues = splineMaxX;
		fTourData.splineDataPoints = splineData;

		splineX[0] = fUIVarX1Border;
		splineY[0] = -fUIVarY1Border;
		
		splineX[1] = 0;
		splineY[1] = 0;
		
		splineX[2] = fUIVarX1;
		splineY[2] = -fUIVarY1;
		
		splineX[3] = sliderDistance;
		splineY[3] = 0;

		splineX[4] = sliderDistance + fUIVarX2Border;
		splineY[4] = -fUIVarY2Border;

		isMovable[0] = true;
		isMovable[1] = false;
		isMovable[2] = true;
		isMovable[3] = false;
		isMovable[4] = true;

		splineMinX[0] = Double.NaN;
		splineMaxX[0] = 0;

		splineMinX[1] = Double.NaN;
		splineMaxX[1] = Double.NaN;

		splineMinX[2] = 0;
		splineMaxX[2] = sliderDistance;

		splineMinX[3] = Double.NaN;
		splineMaxX[3] = Double.NaN;

		splineMinX[4] = sliderDistance;
		splineMaxX[4] = Double.NaN;

		final CubicSpline cubicSpline = new CubicSpline(splineX, splineY);

		// get adjusted altitude serie
		for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

			if (serieIndex < sliderIndex) {

				// add adjusted altitude

				final float distance = distanceSerie[serieIndex];
				final float distanceScale = 1 - distance / sliderDistance;

				final int adjustedAltiDiff = (int) (startAltiDiff * distanceScale);
				final int newAltitude = altitudeSerie[serieIndex] + adjustedAltiDiff;

				final int splineAlti = (int) cubicSpline.interpolate(distance);

				final int adjustedAlti = newAltitude + splineAlti;
				adjustedAltiSerie[serieIndex] = adjustedAlti;
				diffTo2ndAlti[serieIndex] = srtm2ndAlti[serieIndex] - adjustedAlti;
				spline[serieIndex] = splineAlti;

			} else {

				// set altitude which is not adjusted

				adjustedAltiSerie[serieIndex] = altitudeSerie[serieIndex];
			}
		}

		fTourData.dataSerieAdjustedAlti = adjustedAltiSerie;
		fTourData.dataSerieDiffTo2ndAlti = diffTo2ndAlti;
		fTourData.dataSerieSpline = spline;
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

		// initialize scale, remove it from the same position as the next spline position
		fScaleSrmtVarX1Border.setSelection(50);
		fScaleSrmtVarX2Border.setSelection(50);

		fScaleSrmtVarX1.setSelection(50);
		fScaleSrmtVarY1.setSelection(50);

		return fDialogContainer;
	}

	private void createUI(final Composite parent) {

		createUIAdjustmentType(parent);
		createUITourChart(parent);

//		final Composite splineContainer = new Composite(parent, SWT.NONE);
//		GridDataFactory.fillDefaults().grab(true, false).applyTo(splineContainer);
//		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(splineContainer);

		createUISpline(parent);
		createUIAdjustments(parent);
	}

	private void createUIAdjustments(final Composite parent) {

		final Composite scaleContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(scaleContainer);
		GridLayoutFactory.fillDefaults().numColumns(4).applyTo(scaleContainer);

		/*
		 * scale: math var X1 border
		 */
		fScaleSrmtVarX1Border = new Scale(scaleContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleSrmtVarX1Border);
		fScaleSrmtVarX1Border.setMaximum(100);
		fScaleSrmtVarX1Border.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyProperties();
			}
		});
		fScaleSrmtVarX1Border.addListener(SWT.MouseDoubleClick, new Listener() {
			public void handleEvent(final Event event) {
				onScaleDoubleClick(event.widget);
			}
		});

		fLabelVarX1Border = new Label(scaleContainer, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).hint(40, SWT.DEFAULT).applyTo(fLabelVarX1Border);

		/*
		 * scale: math var Y1 border
		 */
		fScaleSrmtVarY1Border = new Scale(scaleContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleSrmtVarY1Border);
		fScaleSrmtVarY1Border.setMaximum(100);
		fScaleSrmtVarY1Border.setSelection(50);
		fScaleSrmtVarY1Border.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyProperties();
			}
		});
		fScaleSrmtVarY1Border.addListener(SWT.MouseDoubleClick, new Listener() {
			public void handleEvent(final Event event) {
				onScaleDoubleClick(event.widget);
			}
		});

		fLabelVarY1Border = new Label(scaleContainer, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).hint(40, SWT.DEFAULT).applyTo(fLabelVarY1Border);

		/*
		 * scale: math var X1
		 */
		fScaleSrmtVarX1 = new Scale(scaleContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleSrmtVarX1);
		fScaleSrmtVarX1.setMaximum(100);
		fScaleSrmtVarX1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyProperties();
			}
		});
		fScaleSrmtVarX1.addListener(SWT.MouseDoubleClick, new Listener() {
			public void handleEvent(final Event event) {
				onScaleDoubleClick(event.widget);
			}
		});

		fLabelVarX1 = new Label(scaleContainer, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).hint(40, SWT.DEFAULT).applyTo(fLabelVarX1);

		/*
		 * scale: math var Y1
		 */
		fScaleSrmtVarY1 = new Scale(scaleContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleSrmtVarY1);
		fScaleSrmtVarY1.setMaximum(100);
		fScaleSrmtVarY1.setSelection(50);
		fScaleSrmtVarY1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyProperties();
			}
		});
		fScaleSrmtVarY1.addListener(SWT.MouseDoubleClick, new Listener() {
			public void handleEvent(final Event event) {
				onScaleDoubleClick(event.widget);
			}
		});

		fLabelVarY1 = new Label(scaleContainer, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).hint(40, SWT.DEFAULT).applyTo(fLabelVarY1);

		/*
		 * scale: math var X2 border
		 */
		fScaleSrmtVarX2Border = new Scale(scaleContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleSrmtVarX2Border);
		fScaleSrmtVarX2Border.setMaximum(100);
		fScaleSrmtVarX2Border.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyProperties();
			}
		});
		fScaleSrmtVarX2Border.addListener(SWT.MouseDoubleClick, new Listener() {
			public void handleEvent(final Event event) {
				onScaleDoubleClick(event.widget);
			}
		});

		fLabelVarX2Border = new Label(scaleContainer, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).hint(40, SWT.DEFAULT).applyTo(fLabelVarX2Border);

		/*
		 * scale: math var Y2 border
		 */
		fScaleSrmtVarY2Border = new Scale(scaleContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleSrmtVarY2Border);
		fScaleSrmtVarY2Border.setMaximum(100);
		fScaleSrmtVarY2Border.setSelection(50);
		fScaleSrmtVarY2Border.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyProperties();
			}
		});
		fScaleSrmtVarY2Border.addListener(SWT.MouseDoubleClick, new Listener() {
			public void handleEvent(final Event event) {
				onScaleDoubleClick(event.widget);
			}
		});

		fLabelVarY2Border = new Label(scaleContainer, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).hint(40, SWT.DEFAULT).applyTo(fLabelVarY2Border);
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

	private void createUISpline(final Composite parent) {

		fSpline = new SplineGraph(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.hint(300, 200)
				.applyTo(fSpline);
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

		fVarX1Border = (float) fScaleSrmtVarX1Border.getSelection() / 100;
		fVarY1Border = (float) (fScaleSrmtVarY1Border.getSelection() - 50) / 10;

		fVarX1 = (float) fScaleSrmtVarX1.getSelection() / 100;
		fVarY1 = (float) (fScaleSrmtVarY1.getSelection() - 50) / 100;

		fVarX2Border = (float) fScaleSrmtVarX2Border.getSelection() / 100;
		fVarY2Border = (float) (fScaleSrmtVarY2Border.getSelection() - 50) / 10;
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
		fSpline.updateValues(fTourData.splineDataPoints);
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

		fLabelVarX1.setText(Integer.toString((int) fUIVarX1));
		fLabelVarX1.pack(true);
		fLabelVarY1.setText(Integer.toString((int) -fUIVarY1));
		fLabelVarY1.pack(true);

		fLabelVarX1Border.setText(Integer.toString((int) fUIVarX1Border));
		fLabelVarX1Border.pack(true);
		fLabelVarY1Border.setText(Integer.toString((int) -fUIVarY1Border));
		fLabelVarY1Border.pack(true);

		fLabelVarX2Border.setText(Integer.toString((int) fUIVarX2Border));
		fLabelVarX2Border.pack(true);
		fLabelVarY2Border.setText(Integer.toString((int) -fUIVarY2Border));
		fLabelVarY2Border.pack(true);
	}

}
