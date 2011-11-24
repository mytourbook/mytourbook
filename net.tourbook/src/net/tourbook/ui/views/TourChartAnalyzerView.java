/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartDrawingData;
import net.tourbook.chart.ColorCache;
import net.tourbook.chart.ComputeChartValue;
import net.tourbook.chart.GraphDrawingData;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.chart.Util;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.SelectionTourChart;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;
import net.tourbook.ui.tourChart.TourChart;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

public class TourChartAnalyzerView extends ViewPart {

	public static final String			ID				= "net.tourbook.views.TourChartAnalyzer";	//$NON-NLS-1$

	private static final int			LAYOUT_TINY		= 0;
	private static final int			LAYOUT_SMALL	= 1;
	private static final int			LAYOUT_MEDIUM	= 2;
	private static final int			LAYOUT_LARGE	= 3;

	private final IPreferenceStore		_prefStore		= TourbookPlugin.getDefault() //
																.getPreferenceStore();

	private IPropertyChangeListener		_prefChangeListener;
	private IPartListener2				_partListener;
	private ISelectionListener			_postSelectionListener;

	private ScrolledComposite			_scrolledContainer;
	private Composite					_partContainer;
	private Composite					_innerScContainer;

	private ChartDataModel				_chartDataModel;

	private ChartDrawingData			_chartDrawingData;
	private ArrayList<GraphDrawingData>	_graphDrawingData;

	private final ArrayList<GraphInfo>	_graphInfos		= new ArrayList<GraphInfo>();

	private final ColorCache			_colorCache		= new ColorCache();

	private Color						_bgColorHeader;
	private Font						_fontBold;

	private SelectionChartInfo			_chartInfo;

	private int							_layoutFormat;

	private int							_valueIndexLeftBackup;
	private int							_valueIndexRightBackup;

	/**
	 * space between columns
	 */
	int									_columnSpacing	= 1;

	private boolean						_isPartVisible	= false;

	public TourChartAnalyzerView() {
		super();
	}

	private void addListeners() {

		final IWorkbenchPage page = getSite().getPage();

		_partContainer.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent event) {

				if (_chartDataModel == null) {
					return;
				}
				createUILayout();
				updateInfo(_chartInfo);
			}
		});

		_postSelectionListener = new ISelectionListener() {
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
				onSelectionChanged(selection);
			}
		};
		page.addPostSelectionListener(_postSelectionListener);

		_partListener = new IPartListener2() {

			public void partActivated(final IWorkbenchPartReference partRef) {}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {}

			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			public void partHidden(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourChartAnalyzerView.this) {
					_isPartVisible = false;
				}
			}

			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			public void partOpened(final IWorkbenchPartReference partRef) {}

			public void partVisible(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourChartAnalyzerView.this) {
					_isPartVisible = true;
				}
			}
		};
		page.addPartListener(_partListener);
	}

	private void addPrefListeners() {

		_prefChangeListener = new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED)) {

					// dispose old colors
					_colorCache.dispose();

					updateInfo(_chartInfo);
				}
			}
		};

		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	@Override
	public void createPartControl(final Composite parent) {

		_partContainer = parent;

		_bgColorHeader = Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);

		_fontBold = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);

		addListeners();
		addPrefListeners();
	}

	private void createUIHeader10Left() {

		final Label label = new Label(_innerScContainer, SWT.TRAIL);
		label.setText(Messages.TourAnalyzer_Label_left + UI.SPACE);
		label.setFont(_fontBold);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		label.setBackground(_bgColorHeader);
	}

	private void createUIHeader20Right() {

		final Label label = new Label(_innerScContainer, SWT.TRAIL);
		label.setText(Messages.TourAnalyzer_Label_right + UI.SPACE);
		label.setFont(_fontBold);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		label.setBackground(_bgColorHeader);
	}

	private void createUIHeader30Min() {

		final Label label = new Label(_innerScContainer, SWT.TRAIL);
		label.setText(Messages.TourAnalyzer_Label_minimum + UI.SPACE);
		label.setFont(_fontBold);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		label.setBackground(_bgColorHeader);
	}

	private void createUIHeader40Max() {

		final Label label = new Label(_innerScContainer, SWT.TRAIL);
		label.setText(Messages.TourAnalyzer_Label_maximum + UI.SPACE);
		label.setFont(_fontBold);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		label.setBackground(_bgColorHeader);
	}

	private void createUIHeader50Diff() {

		final Label label = new Label(_innerScContainer, SWT.TRAIL);
		label.setText(Messages.TourAnalyzer_Label_difference + UI.SPACE);
		label.setFont(_fontBold);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		label.setBackground(_bgColorHeader);
	}

	private void createUIHeader60Avg() {

		final Label label = new Label(_innerScContainer, SWT.TRAIL);
		label.setText(Messages.TourAnalyzer_Label_average + UI.SPACE);
		label.setFont(_fontBold);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		label.setBackground(_bgColorHeader);
	}

	private void createUIHeaderUnitLabel() {

		final Label label = new Label(_innerScContainer, SWT.LEFT);
		label.setText(UI.EMPTY_STRING);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		label.setBackground(_bgColorHeader);

		// spacer
//		final Canvas canvas = new Canvas(_innerScContainer, SWT.NONE);
//		GridDataFactory.fillDefaults()//
//				.align(SWT.BEGINNING, SWT.BEGINNING)
//				.hint(0, 0)
//				.applyTo(canvas);
	}

	private void createUIHeaderValueLabel() {

		final Label label = new Label(_innerScContainer, SWT.NONE);
		label.setText(UI.SPACE + Messages.TourAnalyzer_Label_value);
		label.setFont(_fontBold);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		label.setBackground(_bgColorHeader);
	}

	/**
	 * 
	 */
	private void createUILayout() {

		// recreate the viewer
		if (_scrolledContainer != null) {
			_scrolledContainer.dispose();
			_scrolledContainer = null;
		}

		final PixelConverter pc = new PixelConverter(_partContainer);

		// define the layout which is being used
		final int clientWidth = _partContainer.getClientArea().width;
		_layoutFormat = clientWidth < pc.convertHorizontalDLUsToPixels(100) //
				? LAYOUT_TINY
				: clientWidth < pc.convertHorizontalDLUsToPixels(150) //
						? LAYOUT_SMALL
						: clientWidth < pc.convertHorizontalDLUsToPixels(300) //
								? LAYOUT_MEDIUM
								: LAYOUT_LARGE;

		final int numColumns = _layoutFormat == LAYOUT_TINY //
				? 2
				: _layoutFormat == LAYOUT_SMALL //
						? 3
						: _layoutFormat == LAYOUT_MEDIUM ? 4 : 8;

		// create scrolled container
		_scrolledContainer = new ScrolledComposite(_partContainer, SWT.V_SCROLL | SWT.H_SCROLL);
		_scrolledContainer.setExpandVertical(true);
		_scrolledContainer.setExpandHorizontal(true);
		_scrolledContainer.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				_scrolledContainer.setMinSize(_innerScContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
		});

		// create innter container
		_innerScContainer = new Composite(_scrolledContainer, SWT.NONE);
		GridLayoutFactory.fillDefaults().spacing(0, 0).numColumns(numColumns).applyTo(_innerScContainer);
		_innerScContainer.setBackground(_bgColorHeader);

		_scrolledContainer.setContent(_innerScContainer);

		_columnSpacing = 1;

		if (_layoutFormat == LAYOUT_TINY) {
			createUILayout10Tiny();
		} else if (_layoutFormat == LAYOUT_SMALL) {
			createUILayout20Small();
		} else if (_layoutFormat == LAYOUT_MEDIUM) {
			createUILayout30Medium();
		} else if (_layoutFormat == LAYOUT_LARGE) {
			createUILayout40Large();
		}
	}

	private void createUILayout10Tiny() {

		_graphInfos.clear();

		// create graph info list
		for (final ChartDataSerie xyData : _chartDataModel.getXyData()) {
			_graphInfos.add(new GraphInfo(this, xyData, _innerScContainer));
		}

		// ----------------------------------------------------------------

		createUIHeader10Left();
		createUIHeaderUnitLabel();

		for (final GraphInfo graphInfo : _graphInfos) {
			graphInfo.createUIInfo10Left();
			graphInfo.createUIValueUnit();
		}

		// ----------------------------------------------------------------

		createVerticalBorder();

		createUIHeader20Right();
		createUIHeaderUnitLabel();

		for (final GraphInfo graphInfo : _graphInfos) {
			graphInfo.createUIInfo20Right();
			graphInfo.createUIValueUnit();
		}

		// ----------------------------------------------------------------

		createVerticalBorder();

		createUIHeader50Diff();
		createUIHeaderUnitLabel();

		for (final GraphInfo graphInfo : _graphInfos) {
			graphInfo.createUIInfo50Diff();
			graphInfo.createUIValueUnit();
		}

		// ----------------------------------------------------------------

		createVerticalBorder();

		createUIHeader60Avg();
		createUIHeaderUnitLabel();

		for (final GraphInfo graphInfo : _graphInfos) {
			graphInfo.createUIInfo60Avg();
			graphInfo.createUIValueUnit();
		}

		// ----------------------------------------------------------------

		createVerticalBorder();

		createUIHeader30Min();
		createUIHeaderUnitLabel();

		for (final GraphInfo graphInfo : _graphInfos) {
			graphInfo.createUIInfo30Min();
			graphInfo.createUIValueUnit();
		}

		// ----------------------------------------------------------------

		createVerticalBorder();

		createUIHeader40Max();
		createUIHeaderUnitLabel();

		for (final GraphInfo graphInfo : _graphInfos) {
			graphInfo.createUIInfo40Max();
			graphInfo.createUIValueUnit();
		}
	}

	private void createUILayout20Small() {

		createUIHeader10Left();
		createUIHeader20Right();
		createUIHeaderUnitLabel();

		// add all graphs and the x axis to the layout
		_graphInfos.clear();
		for (final ChartDataSerie xyData : _chartDataModel.getXyData()) {

			final GraphInfo graphInfo = new GraphInfo(this, xyData, _innerScContainer);

			graphInfo.createUIInfo10Left();
			graphInfo.createUIInfo20Right();
			graphInfo.createUIValueUnit();

			_graphInfos.add(graphInfo);
		}

		// ----------------------------------------------------------------

		createVerticalBorder();

		createUIHeader50Diff();
		createUIHeader60Avg();
		createUIHeaderUnitLabel();

		for (final GraphInfo graphInfo : _graphInfos) {
			graphInfo.createUIInfo50Diff();
			graphInfo.createUIInfo60Avg();
			graphInfo.createUIValueUnit();
		}

		// ----------------------------------------------------------------

		createVerticalBorder();

		createUIHeader30Min();
		createUIHeader40Max();
		createUIHeaderUnitLabel();

		for (final GraphInfo graphInfo : _graphInfos) {
			graphInfo.createUIInfo30Min();
			graphInfo.createUIInfo40Max();
			graphInfo.createUIValueUnit();
		}
	}

	private void createUILayout30Medium() {

		createUIHeader10Left();
		createUIHeader20Right();
		createUIHeader50Diff();
		createUIHeaderUnitLabel();

		// add all graphs and the x axis to the layout
		_graphInfos.clear();
		for (final ChartDataSerie xyData : _chartDataModel.getXyData()) {

			final GraphInfo graphInfo = new GraphInfo(this, xyData, _innerScContainer);

			graphInfo.createUIInfo10Left();
			graphInfo.createUIInfo20Right();
			graphInfo.createUIInfo50Diff();
			graphInfo.createUIValueUnit();

			_graphInfos.add(graphInfo);
		}

		createVerticalBorder();

		createUIHeader30Min();
		createUIHeader40Max();
		createUIHeader60Avg();
		createUIHeaderUnitLabel();

		for (final GraphInfo graphInfo : _graphInfos) {
			graphInfo.createUIInfo30Min();
			graphInfo.createUIInfo40Max();
			graphInfo.createUIInfo60Avg();
			graphInfo.createUIValueUnit();
		}
	}

	private void createUILayout40Large() {

		createUIHeaderValueLabel();
		createUIHeader10Left();
		createUIHeader20Right();
		createUIHeader30Min();
		createUIHeader40Max();
		createUIHeader50Diff();
		createUIHeader60Avg();
		createUIHeaderUnitLabel();

		// add all graphs and the x axis to the layout
		_graphInfos.clear();
		for (final ChartDataSerie xyData : _chartDataModel.getXyData()) {

			final GraphInfo graphInfo = new GraphInfo(this, xyData, _innerScContainer);

			graphInfo.createUIValueLabel();

			graphInfo.createUIInfo10Left();
			graphInfo.createUIInfo20Right();
			graphInfo.createUIInfo30Min();
			graphInfo.createUIInfo40Max();
			graphInfo.createUIInfo50Diff();
			graphInfo.createUIInfo60Avg();

			graphInfo.createUIValueUnit();

			_graphInfos.add(graphInfo);
		}
	}

	private void createVerticalBorder() {

		GridData gd;

		final int columns = _layoutFormat == LAYOUT_TINY //
				? 1
				: _layoutFormat == LAYOUT_SMALL //
						? 2
						: _layoutFormat == LAYOUT_MEDIUM ? 3 : 7;

		gd = new GridData(SWT.FILL, SWT.FILL, false, false);
		gd.heightHint = 3;

		Label label;

		for (int columnIndex = 0; columnIndex < columns; columnIndex++) {
			label = new Label(_innerScContainer, SWT.NONE);
			label.setText(UI.SPACE);
			label.setLayoutData(gd);
		}

		label = new Label(_innerScContainer, SWT.NONE);
		label.setText(UI.SPACE);
		label.setLayoutData(gd);
	}

	@Override
	public void dispose() {

		final IWorkbenchPage page = getSite().getPage();

		page.removePostSelectionListener(_postSelectionListener);
		page.removePartListener(_partListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		_colorCache.dispose();

		super.dispose();
	}

	/**
	 * @param rgb
	 * @return Returns the color from the color cache, the color must not be disposed this is done
	 *         when the cache is disposed
	 */
	Color getColor(final RGB rgb) {

		final String colorKey = rgb.toString();

		final Color color = _colorCache.get(colorKey);

		if (color == null) {
			return _colorCache.getColor(colorKey, rgb);
		} else {
			return color;
		}
	}

	private void onSelectionChanged(final ISelection selection) {

		if (_isPartVisible == false) {
			return;
		}

		if (selection instanceof SelectionChartInfo) {

			updateInfo((SelectionChartInfo) selection);

		} else if (selection instanceof SelectionChartXSliderPosition) {

			updateInfo(((SelectionChartXSliderPosition) selection));

		} else if (selection instanceof SelectionTourData) {

			final TourChart tourChart = ((SelectionTourData) selection).getTourChart();

			if (tourChart != null) {
				updateInfo(tourChart.getChartInfo());
			}

		} else if (selection instanceof SelectionTourChart) {

			final TourChart tourChart = ((SelectionTourChart) selection).getTourChart();
			if (tourChart != null) {
				updateInfo(tourChart.getChartInfo());
			}
		}
	}

	@Override
	public void setFocus() {
		if (_scrolledContainer != null) {
			_scrolledContainer.setFocus();
		}
	}

	private void updateInfo(final SelectionChartInfo chartInfo) {

//		long startTime = System.currentTimeMillis();

		if (chartInfo == null) {
			return;
		}

		_chartInfo = chartInfo;

		// check if the layout needs to be recreated
		boolean isLayoutDirty = false;
		if (_chartDataModel != chartInfo.chartDataModel) {
			/*
			 * data model changed, a new layout needs to be created
			 */
			isLayoutDirty = true;
		}

		// init vars which are used in createLayout()
		_chartDataModel = chartInfo.chartDataModel;
		_chartDrawingData = chartInfo.chartDrawingData;
		_graphDrawingData = _chartDrawingData.graphDrawingData;

		if ((_graphDrawingData == null) || (_graphDrawingData.size() == 0) || (_graphDrawingData.get(0) == null)) {
			// this happened
			return;
		}

		if ((_graphInfos == null) || isLayoutDirty) {
			createUILayout();
		}

		updateInfoData(chartInfo);

		// refresh the layout after the data has changed
		_partContainer.layout();

//		long endTime = System.currentTimeMillis();
//		System.out.println(++fCounter + "  " + (endTime - startTime) + " ms");
	}

	/**
	 * selection is not from a chart, so it's possible that the chart has not yet the correct slider
	 * positon, we create the chart info from the slider position
	 */
	private void updateInfo(final SelectionChartXSliderPosition sliderPosition) {

		Chart chart = sliderPosition.getChart();

		if (chart == null) {

			final TourChart tourChart = TourManager.getInstance().getActiveTourChart();

			if ((tourChart == null) || tourChart.isDisposed()) {
				return;
			} else {
				chart = tourChart;
			}
		}

		final SelectionChartInfo chartInfo = new SelectionChartInfo(chart);

		chartInfo.chartDataModel = chart.getChartDataModel();
		chartInfo.chartDrawingData = chart.getChartDrawingData();

		chartInfo.leftSliderValuesIndex = sliderPosition.getLeftSliderValueIndex();
		chartInfo.rightSliderValuesIndex = sliderPosition.getRightSliderValueIndex();

		updateInfo(chartInfo);
	}

	private void updateInfoData(final SelectionChartInfo chartInfo) {

//		long startTime = System.currentTimeMillis();

		final ChartDataXSerie xData = _graphDrawingData.get(0).getXData();

		int valuesIndexLeft = chartInfo.leftSliderValuesIndex;
		int valuesIndexRight = chartInfo.rightSliderValuesIndex;

		if (valuesIndexLeft == SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION) {
			valuesIndexLeft = _valueIndexLeftBackup;
		} else {
			_valueIndexLeftBackup = valuesIndexLeft;
		}
		if (valuesIndexRight == SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION) {
			valuesIndexRight = _valueIndexRightBackup;
		} else {
			_valueIndexRightBackup = valuesIndexRight;
		}

//		int outCounter = 0;

		for (final GraphInfo graphInfo : _graphInfos) {

			final ChartDataSerie serieData = graphInfo._chartData;

			TourChartAnalyzerInfo analyzerInfo = (TourChartAnalyzerInfo) serieData
					.getCustomData(TourManager.CUSTOM_DATA_ANALYZER_INFO);
			if (analyzerInfo == null) {
				// create default average object
				analyzerInfo = new TourChartAnalyzerInfo();
			}

			final int unitType = serieData.getAxisUnit();
			final int valueDivisor = serieData.getValueDivisor();

			final float[] values = serieData.getHighValues()[0];
			if (values == null) {
				break;
			}

			final int endIndex = values.length - 1;
			if (valuesIndexLeft > endIndex) {
				valuesIndexLeft = endIndex;
			}
			if (valuesIndexRight > endIndex) {
				valuesIndexRight = endIndex;
			}

			valuesIndexLeft = Math.max(0, valuesIndexLeft);
			valuesIndexRight = Math.max(0, valuesIndexRight);

			// values at the left/right slider
			final float leftValue = values[valuesIndexLeft];
			final float rightValue = values[valuesIndexRight];

			int dataIndex = valuesIndexLeft;
			float avg = 0;
			int avgDiv = 0;
			float min = 0;
			float max = 0;

			// compute min/max/avg values
			while (dataIndex <= valuesIndexRight) {

				final float value = values[dataIndex];

				avg += value;
				avgDiv++;

				if (dataIndex == valuesIndexLeft) {
					// this is the first value in the dataseries, set initial
					// value
					min = value;
					max = value;
				} else {
					min = Math.min(value, min);
					max = Math.max(value, max);
				}

				dataIndex++;
			}

			final ComputeChartValue computeAvg = analyzerInfo.getComputeChartValue();
			if (computeAvg != null) {

				// average is computed by a callback method

				computeAvg.valueIndexLeft = valuesIndexLeft;
				computeAvg.valueIndexRight = valuesIndexRight;
				computeAvg.xData = xData;
				computeAvg.yData = (ChartDataYSerie) serieData;
				computeAvg.chartModel = _chartDataModel;

				avg = computeAvg.compute();

			} else {
				if (avgDiv != 0) {
					avg = avg / avgDiv;
				}
			}

			/*
			 * optimize performance by displaying only changed values
			 */
			if (graphInfo.leftValue != leftValue) {
				graphInfo.leftValue = leftValue;
				graphInfo.lblLeft.setText(Util.formatValue(leftValue, unitType, valueDivisor, true) + UI.SPACE);
//				outCounter++;
			}

			if (graphInfo.rightValue != rightValue) {
				graphInfo.rightValue = rightValue;
				graphInfo.lblRight.setText(Util.formatValue(rightValue, unitType, valueDivisor, true) + UI.SPACE);
//				outCounter++;
			}

			if (graphInfo.minValue != min) {
				graphInfo.minValue = min;
				graphInfo.lblMin.setText(Util.formatValue(min, unitType, valueDivisor, true) + UI.SPACE);
//				outCounter++;
			}

			if (graphInfo.maxValue != max) {
				graphInfo.maxValue = max;
				graphInfo.lblMax.setText(Util.formatValue(max, unitType, valueDivisor, true) + UI.SPACE);
//				outCounter++;
			}

			// set average value
			if (analyzerInfo.isShowAvg()) {

				float avgValue = avg;

				if (analyzerInfo.isShowAvgDecimals()) {

					final int avgDivisor = (int) Math.pow(10, analyzerInfo.getAvgDecimals());

					avgValue *= avgDivisor;

					if (graphInfo.avgValue != (int) avgValue) {
						graphInfo.avgValue = (int) avgValue;
						graphInfo.lblAvg.setText(Util.formatInteger(
								(int) avgValue,
								avgDivisor,
								analyzerInfo.getAvgDecimals(),
								false)
								+ UI.SPACE);
//						outCounter++;
					}

				} else {
					if (graphInfo.avgValue != (int) avgValue) {
						graphInfo.avgValue = (int) avgValue;
						graphInfo.lblAvg.setText(Util.formatValue((int) avgValue, unitType, valueDivisor, true)
								+ UI.SPACE);
//						outCounter++;
					}
				}

			} else {
				graphInfo.lblAvg.setText(UI.EMPTY_STRING);
//				outCounter++;
			}

			final float diffValue = rightValue - leftValue;
			if (graphInfo.diffValue != diffValue) {
				graphInfo.diffValue = diffValue;
				graphInfo.lblDiff.setText(Util.formatValue(diffValue, unitType, valueDivisor, true) + UI.SPACE);
//				outCounter++;
			}
		}

//		System.out.print(outCounter);
//
//		long endTime = System.currentTimeMillis();
//		System.out.println(" - " + (endTime - startTime) + " ms");

	}
}
