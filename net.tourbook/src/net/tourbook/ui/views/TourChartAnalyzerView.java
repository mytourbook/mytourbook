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
package net.tourbook.ui.views;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartDrawingData;
import net.tourbook.chart.ChartUtil;
import net.tourbook.chart.ColorCache;
import net.tourbook.chart.ComputeChartValue;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.tour.SelectionActiveEditor;
import net.tourbook.tour.SelectionTourChart;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.TourEditor;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;
import net.tourbook.ui.tourChart.TourChart;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

public class TourChartAnalyzerView extends ViewPart {

	public static final String			ID				= "net.tourbook.views.TourChartAnalyzer";	//$NON-NLS-1$

	private static final int			LAYOUT_TINY		= 0;
	private static final int			LAYOUT_SMALL	= 1;
	private static final int			LAYOUT_MEDIUM	= 2;
	private static final int			LAYOUT_LARGE	= 3;

	private ISelectionListener			fPostSelectionListener;

	private Font						fontBold;

	private Color						fBgColorData;
	private Color						fBgColorHeader;

	private ScrolledComposite			fScrolledContainer;
	private Composite					fPartContainer;
	private Composite					fContainer;

	private ChartDataModel				fChartDataModel;

	private ArrayList<ChartDrawingData>	fDrawingData;
	private ArrayList<GraphInfo>		fGraphInfos;

	private final ColorCache			fColorCache		= new ColorCache();

	private SelectionChartInfo			fChartInfo;

	private int							fLayout;

	private int							fValueIndexLeftBackup;
	private int							fValueIndexRightBackup;

	/**
	 * This class generates and contains the labels for one row in the view
	 */
	class GraphInfo {

		Label			left;
		Label			right;

		int				leftValue	= Integer.MIN_VALUE;
		int				rightValue	= Integer.MIN_VALUE;

		Label			min;
		Label			max;

		int				minValue	= Integer.MIN_VALUE;
		int				maxValue	= Integer.MIN_VALUE;

		Label			avg;
		Label			diff;

		int				avgValue	= Integer.MIN_VALUE;
		int				diffValue	= Integer.MIN_VALUE;

		ChartDataSerie	fChartData;

		GraphInfo(final ChartDataSerie chartData) {
			fChartData = chartData;
		}

		void createInfoAvg() {

			final Color lineColor = getColor(fChartData.getDefaultRGB());

			avg = new Label(fContainer, SWT.TRAIL);
			avg.setForeground(lineColor);
			avg.setBackground(fBgColorData);

			final GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
			gd.horizontalIndent = 5;
			avg.setLayoutData(gd);
		}

		void createInfoDiff() {

			final Color lineColor = getColor(fChartData.getDefaultRGB());

			diff = new Label(fContainer, SWT.TRAIL);
			diff.setForeground(lineColor);
			diff.setBackground(fBgColorData);

			final GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
			gd.horizontalIndent = 5;
			diff.setLayoutData(gd);
		}

		void createInfoLeft() {

			left = new Label(fContainer, SWT.TRAIL);
			left.setForeground(getColor(fChartData.getDefaultRGB()));
			left.setBackground(fBgColorData);

			final GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
			gd.horizontalIndent = 5;
			left.setLayoutData(gd);
		}

		void createInfoMax() {

			max = new Label(fContainer, SWT.TRAIL);
			max.setForeground(getColor(fChartData.getDefaultRGB()));
			max.setBackground(fBgColorData);

			final GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
			gd.horizontalIndent = 5;
			max.setLayoutData(gd);
		}

		void createInfoMin() {

			min = new Label(fContainer, SWT.TRAIL);
			min.setForeground(getColor(fChartData.getDefaultRGB()));
			min.setBackground(fBgColorData);

			final GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
			gd.horizontalIndent = 5;
			min.setLayoutData(gd);

		}

		void createInfoRight() {

			right = new Label(fContainer, SWT.TRAIL);
			right.setForeground(getColor(fChartData.getDefaultRGB()));
			right.setBackground(fBgColorData);

			final GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
			gd.horizontalIndent = 5;
			right.setLayoutData(gd);
		}

		private void createValueLabel() {

			final Color lineColor = getColor(fChartData.getDefaultRGB());
			Label label;

			String labelText;
			if (fChartData instanceof ChartDataYSerie) {
				labelText = ((ChartDataYSerie) fChartData).getYTitle();
			} else {
				labelText = fChartData.getLabel();
			}
			label = new Label(fContainer, SWT.NONE);
			label.setText(labelText);
			label.setForeground(lineColor);
			label.setBackground(fBgColorHeader);
			final GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
			label.setLayoutData(gd);
		}

		void createValueUnit() {

			final Color lineColor = getColor(fChartData.getDefaultRGB());
			Label label;

			String toolTip;
			if (fChartData instanceof ChartDataYSerie) {
				toolTip = ((ChartDataYSerie) fChartData).getYTitle();
			} else {
				toolTip = fChartData.getLabel();
			}

			label = new Label(fContainer, SWT.NONE);
			label.setText(" " + fChartData.getUnitLabel()); //$NON-NLS-1$
			label.setToolTipText(toolTip);
			label.setForeground(lineColor);
			label.setBackground(fBgColorHeader);
			final GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
			label.setLayoutData(gd);
		}
	}

	public TourChartAnalyzerView() {
		super();
	}

	private void addListeners() {

		fPartContainer.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent event) {
				if (fChartDataModel == null) {
					return;
				}
				createLayout();
				updateInfo(fChartInfo);
			}
		});

		fPostSelectionListener = new ISelectionListener() {

			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

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

				} else if (selection instanceof SelectionActiveEditor) {

					final IEditorPart editor = ((SelectionActiveEditor) selection).getEditor();
					if (editor instanceof TourEditor) {
						final TourEditor tourEditor = (TourEditor) editor;
						updateInfo(tourEditor.getTourChart().getChartInfo());
					}
				}
			}
		};

		getSite().getPage().addPostSelectionListener(fPostSelectionListener);
	}

	private void createHeaderAvg() {

		final Label label = new Label(fContainer, SWT.TRAIL);
		label.setText(Messages.TourAnalyzer_Label_average);
		label.setFont(fontBold);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		label.setBackground(fBgColorHeader);
	}

	private void createHeaderDiff() {

		final Label label = new Label(fContainer, SWT.TRAIL);
		label.setText(Messages.TourAnalyzer_Label_difference);
		label.setFont(fontBold);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		label.setBackground(fBgColorHeader);
	}

	private void createHeaderLeft() {

		final Label label = new Label(fContainer, SWT.TRAIL);
		label.setText(Messages.TourAnalyzer_Label_left);
		label.setFont(fontBold);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		label.setBackground(fBgColorHeader);
	}

	private void createHeaderMax() {

		final Label label = new Label(fContainer, SWT.TRAIL);
		label.setText(Messages.TourAnalyzer_Label_maximum);
		label.setFont(fontBold);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		label.setBackground(fBgColorHeader);
	}

	private void createHeaderMin() {

		final Label label = new Label(fContainer, SWT.TRAIL);
		label.setText(Messages.TourAnalyzer_Label_minimum);
		label.setFont(fontBold);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		label.setBackground(fBgColorHeader);
	}

	private void createHeaderRight() {

		final Label label = new Label(fContainer, SWT.TRAIL);
		label.setText(Messages.TourAnalyzer_Label_right);
		label.setFont(fontBold);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		label.setBackground(fBgColorHeader);
	}

	private void createHeaderUnitLabel() {

		final Label label = new Label(fContainer, SWT.LEFT);
		label.setText(""); //$NON-NLS-1$
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		label.setBackground(fBgColorHeader);
	}

	private void createHeaderValueLabel() {

		final Label label = new Label(fContainer, SWT.NONE);
		label.setText(Messages.TourAnalyzer_Label_value);
		label.setFont(fontBold);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		label.setBackground(fBgColorHeader);
	}

	/**
	 * 
	 */
	private void createLayout() {

		// recreate the viewer
		if (fScrolledContainer != null) {
			fScrolledContainer.dispose();
			fScrolledContainer = null;
		}

		// fPartContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));

		// define the layout which is being used
		final int clientWidth = fPartContainer.getClientArea().width;
		fLayout = clientWidth < 110 ? LAYOUT_TINY : clientWidth < 150 ? LAYOUT_SMALL : clientWidth < 370
				? LAYOUT_MEDIUM
				: LAYOUT_LARGE;

		// create scrolled container
		fScrolledContainer = new ScrolledComposite(fPartContainer, SWT.V_SCROLL | SWT.H_SCROLL);
		fScrolledContainer.setExpandVertical(true);
		fScrolledContainer.setExpandHorizontal(true);
		fScrolledContainer.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				fScrolledContainer.setMinSize(fContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
		});

		// create container
		fContainer = new Composite(fScrolledContainer, SWT.NONE);
		fContainer.setBackground(fBgColorHeader);

		final GridLayout gl = new GridLayout(fLayout == LAYOUT_TINY ? 2 : fLayout == LAYOUT_SMALL
				? 3
				: fLayout == LAYOUT_MEDIUM ? 4 : 8, false);
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		gl.verticalSpacing = 0;
		gl.horizontalSpacing = 0;
		fContainer.setLayout(gl);

		fScrolledContainer.setContent(fContainer);

		if (fLayout == LAYOUT_TINY) {
			createLayoutTiny();

		} else if (fLayout == LAYOUT_SMALL) {
			createLayoutSmall();

		} else if (fLayout == LAYOUT_MEDIUM) {
			createLayoutMedium();

		} else if (fLayout == LAYOUT_LARGE) {
			createLayoutLarge();
		}
	}

	private void createLayoutLarge() {

		createHeaderValueLabel();
		createHeaderLeft();
		createHeaderRight();
		createHeaderMin();
		createHeaderMax();
		createHeaderDiff();
		createHeaderAvg();
		createHeaderUnitLabel();

		// add all graphs and the x axis to the layout
		fGraphInfos = new ArrayList<GraphInfo>();
		for (final ChartDataSerie xyData : fChartDataModel.getXyData()) {

			final GraphInfo graphInfo = new GraphInfo(xyData);

			graphInfo.createValueLabel();

			graphInfo.createInfoLeft();
			graphInfo.createInfoRight();
			graphInfo.createInfoMin();
			graphInfo.createInfoMax();
			graphInfo.createInfoDiff();
			graphInfo.createInfoAvg();

			graphInfo.createValueUnit();

			fGraphInfos.add(graphInfo);
		}
	}

	private void createLayoutMedium() {

		createHeaderLeft();
		createHeaderRight();
		createHeaderDiff();
		createHeaderUnitLabel();

		// add all graphs and the x axis to the layout
		fGraphInfos = new ArrayList<GraphInfo>();
		for (final ChartDataSerie xyData : fChartDataModel.getXyData()) {

			final GraphInfo graphInfo = new GraphInfo(xyData);

			graphInfo.createInfoLeft();
			graphInfo.createInfoRight();
			graphInfo.createInfoDiff();
			graphInfo.createValueUnit();

			fGraphInfos.add(graphInfo);
		}

		createVerticalBorder();

		createHeaderMin();
		createHeaderMax();
		createHeaderAvg();
		createHeaderUnitLabel();

		for (final GraphInfo graphInfo : fGraphInfos) {
			graphInfo.createInfoMin();
			graphInfo.createInfoMax();
			graphInfo.createInfoAvg();
			graphInfo.createValueUnit();
		}
	}

	private void createLayoutSmall() {

		createHeaderLeft();
		createHeaderRight();
		createHeaderUnitLabel();

		// add all graphs and the x axis to the layout
		fGraphInfos = new ArrayList<GraphInfo>();
		for (final ChartDataSerie xyData : fChartDataModel.getXyData()) {

			final GraphInfo graphInfo = new GraphInfo(xyData);

			graphInfo.createInfoLeft();
			graphInfo.createInfoRight();
			graphInfo.createValueUnit();

			fGraphInfos.add(graphInfo);
		}

		// ----------------------------------------------------------------

		createVerticalBorder();

		createHeaderDiff();
		createHeaderAvg();
		createHeaderUnitLabel();

		for (final GraphInfo graphInfo : fGraphInfos) {
			graphInfo.createInfoDiff();
			graphInfo.createInfoAvg();
			graphInfo.createValueUnit();
		}

		// ----------------------------------------------------------------

		createVerticalBorder();

		createHeaderMin();
		createHeaderMax();
		createHeaderUnitLabel();

		for (final GraphInfo graphInfo : fGraphInfos) {
			graphInfo.createInfoMin();
			graphInfo.createInfoMax();
			graphInfo.createValueUnit();
		}
	}

	private void createLayoutTiny() {
		fGraphInfos = new ArrayList<GraphInfo>();

		// create graph info list
		for (final ChartDataSerie xyData : fChartDataModel.getXyData()) {
			fGraphInfos.add(new GraphInfo(xyData));
		}

		// ----------------------------------------------------------------

		createHeaderLeft();
		createHeaderUnitLabel();

		for (final GraphInfo graphInfo : fGraphInfos) {
			graphInfo.createInfoLeft();
			graphInfo.createValueUnit();
		}

		// ----------------------------------------------------------------

		createVerticalBorder();

		createHeaderRight();
		createHeaderUnitLabel();

		for (final GraphInfo graphInfo : fGraphInfos) {
			graphInfo.createInfoRight();
			graphInfo.createValueUnit();
		}

		// ----------------------------------------------------------------

		createVerticalBorder();

		createHeaderDiff();
		createHeaderUnitLabel();

		for (final GraphInfo graphInfo : fGraphInfos) {
			graphInfo.createInfoDiff();
			graphInfo.createValueUnit();
		}

		// ----------------------------------------------------------------

		createVerticalBorder();

		createHeaderAvg();
		createHeaderUnitLabel();

		for (final GraphInfo graphInfo : fGraphInfos) {
			graphInfo.createInfoAvg();
			graphInfo.createValueUnit();
		}

		// ----------------------------------------------------------------

		createVerticalBorder();

		createHeaderMin();
		createHeaderUnitLabel();

		for (final GraphInfo graphInfo : fGraphInfos) {
			graphInfo.createInfoMin();
			graphInfo.createValueUnit();
		}

		// ----------------------------------------------------------------

		createVerticalBorder();

		createHeaderMax();
		createHeaderUnitLabel();

		for (final GraphInfo graphInfo : fGraphInfos) {
			graphInfo.createInfoMax();
			graphInfo.createValueUnit();
		}
	}

	@Override
	public void createPartControl(final Composite parent) {

		fPartContainer = parent;

		fBgColorData = getColor(new RGB(240, 240, 240));
		// fBgColorData = Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
		// fBgColorData = Display.getCurrent().getSystemColor(SWT.COLOR_WHITE);

		fBgColorHeader = Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
		// fBgColorHeader =
		// Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW);

		fontBold = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);

		addListeners();

		// show chart info for the current selection
		final ISelection selection = getSite().getWorkbenchWindow().getSelectionService().getSelection();
		if (selection != null) {

		}
	}

	private void createVerticalBorder() {

		GridData gd;

		final int columns = fLayout == LAYOUT_TINY ? 1 : fLayout == LAYOUT_SMALL ? 2 : fLayout == LAYOUT_MEDIUM ? 3 : 8;

		gd = new GridData(SWT.FILL, SWT.FILL, false, false);
		gd.heightHint = 5;
		// gd.horizontalIndent = 15;

		Label label;

		for (int columnIndex = 0; columnIndex < columns; columnIndex++) {
			label = new Label(fContainer, SWT.NONE);
			label.setText(" "); //$NON-NLS-1$
			// label.setBackground(fBgColorData);
			label.setLayoutData(gd);
		}

		label = new Label(fContainer, SWT.NONE);
		label.setText(" "); //$NON-NLS-1$
		// label.setBackground(fBgColorHeader);
		label.setLayoutData(gd);
	}

	@Override
	public void dispose() {

		getSite().getPage().removePostSelectionListener(fPostSelectionListener);

		fColorCache.dispose();

		super.dispose();
	}

	/**
	 * @param rgb
	 * @return Returns the color from the color cache, the color must not be disposed this is done
	 *         when the cache is disposed
	 */
	private Color getColor(final RGB rgb) {

		final String colorKey = rgb.toString();

		final Color color = fColorCache.get(colorKey);

		if (color == null) {
			return fColorCache.createColor(colorKey, rgb);
		} else {
			return color;
		}
	}

	@Override
	public void setFocus() {
		if (fScrolledContainer != null) {
			fScrolledContainer.setFocus();
		}
	}

	private void updateInfo(final SelectionChartInfo chartInfo) {

//		long startTime = System.currentTimeMillis();

		if (chartInfo == null) {
			return;
		}

		fChartInfo = chartInfo;

		// check if the layout needs to be recreated
		boolean isLayoutDirty = false;
		if (fChartDataModel != chartInfo.chartDataModel) {
			/*
			 * data model changed, a new layout needs to be created
			 */
			isLayoutDirty = true;
		}

		// init vars which are used in createLayout()
		fChartDataModel = chartInfo.chartDataModel;
		fDrawingData = chartInfo.chartDrawingData;

		if (fDrawingData == null || fDrawingData.size() == 0 || fDrawingData.get(0) == null) {
			// this happened
			return;
		}

		if (fGraphInfos == null || isLayoutDirty) {
			createLayout();
		}

		updateInfoData(chartInfo);

		// refresh the layout after the data has changed
		fPartContainer.layout();

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

			if (tourChart == null || tourChart.isDisposed()) {
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

		final ChartDataXSerie xData = fDrawingData.get(0).getXData();

		int valuesIndexLeft = chartInfo.leftSliderValuesIndex;
		int valuesIndexRight = chartInfo.rightSliderValuesIndex;

		if (valuesIndexLeft == SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION) {
			valuesIndexLeft = fValueIndexLeftBackup;
		} else {
			fValueIndexLeftBackup = valuesIndexLeft;
		}
		if (valuesIndexRight == SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION) {
			valuesIndexRight = fValueIndexRightBackup;
		} else {
			fValueIndexRightBackup = valuesIndexRight;
		}

		int outCounter = 0;

		for (final GraphInfo graphInfo : fGraphInfos) {

			final ChartDataSerie serieData = graphInfo.fChartData;

			TourChartAnalyzerInfo analyzerInfo = (TourChartAnalyzerInfo) serieData.getCustomData(TourManager.CUSTOM_DATA_ANALYZER_INFO);
			if (analyzerInfo == null) {
				// create default average object
				analyzerInfo = new TourChartAnalyzerInfo();
			}

			final int xAxisUnit = serieData.getAxisUnit();
			final int valueDivisor = serieData.getValueDivisor();

			final int[] values = serieData.getHighValues()[0];
			if (values == null) {
				break;
			}

			if (valuesIndexLeft > values.length) {
				valuesIndexLeft = values.length - 1;
			}
			if (valuesIndexRight > values.length) {
				valuesIndexRight = values.length - 1;
			}

			valuesIndexLeft = Math.max(0, valuesIndexLeft);
			valuesIndexRight = Math.max(0, valuesIndexRight);

			// values at the left/right slider
			final int leftValue = values[valuesIndexLeft];
			final int rightValue = values[valuesIndexRight];

			int dataIndex = valuesIndexLeft;
			float avg = 0;
			int avgDiv = 0;
			int min = 0, max = 0;

			// compute min/max/avg values
			while (dataIndex <= valuesIndexRight) {

				final int value = values[dataIndex];

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
				computeAvg.chartModel = fChartDataModel;

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
				graphInfo.left.setText(ChartUtil.formatValue(leftValue, xAxisUnit, valueDivisor, true) + " "); //$NON-NLS-1$
				outCounter++;
			}

			if (graphInfo.rightValue != rightValue) {
				graphInfo.rightValue = rightValue;
				graphInfo.right.setText(ChartUtil.formatValue(rightValue, xAxisUnit, valueDivisor, true) + " "); //$NON-NLS-1$
				outCounter++;
			}

			if (graphInfo.minValue != min) {
				graphInfo.minValue = min;
				graphInfo.min.setText(ChartUtil.formatValue(min, xAxisUnit, valueDivisor, true) + " "); //$NON-NLS-1$
				outCounter++;
			}

			if (graphInfo.maxValue != max) {
				graphInfo.maxValue = max;
				graphInfo.max.setText(ChartUtil.formatValue(max, xAxisUnit, valueDivisor, true) + " "); //$NON-NLS-1$
				outCounter++;
			}

			// set average value
			if (analyzerInfo.isShowAvg()) {

				float avgValue = avg;

				if (analyzerInfo.isShowAvgDecimals()) {

					final int avgDivisor = (int) Math.pow(10, analyzerInfo.getAvgDecimals());

					avgValue *= avgDivisor;

					if (graphInfo.avgValue != avgValue) {
						graphInfo.avgValue = (int) avgValue;
						graphInfo.avg.setText(ChartUtil.formatInteger((int) avgValue,
								avgDivisor,
								analyzerInfo.getAvgDecimals(),
								false)
								+ " "); //$NON-NLS-1$
						outCounter++;
					}

				} else {
					if (graphInfo.avgValue != avgValue) {
						graphInfo.avgValue = (int) avgValue;
						graphInfo.avg.setText(ChartUtil.formatValue((int) avgValue, xAxisUnit, valueDivisor, true)
								+ " "); //$NON-NLS-1$
						outCounter++;
					}
				}

			} else {
				graphInfo.avg.setText(UI.EMPTY_STRING);
				outCounter++;
			}

			final int diffValue = rightValue - leftValue;
			if (graphInfo.diffValue != diffValue) {
				graphInfo.diffValue = diffValue;
				graphInfo.diff.setText(ChartUtil.formatValue(diffValue, xAxisUnit, valueDivisor, true) + " "); //$NON-NLS-1$
				outCounter++;
			}
		}

//		System.out.print(outCounter);
//
//		long endTime = System.currentTimeMillis();
//		System.out.println(" - " + (endTime - startTime) + " ms");

	}
}
