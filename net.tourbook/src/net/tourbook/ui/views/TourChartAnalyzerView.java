/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
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
import net.tourbook.tour.SelectionTourChart;
import net.tourbook.tour.TourChart;
import net.tourbook.tour.TourManager;

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
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

public class TourChartAnalyzerView extends ViewPart {

	public static final String			ID				= "net.tourbook.views.TourChartAnalyzer";	//$NON-NLS-1$

	private static final int			LAYOUT_TINY		= 0;
	private static final int			LAYOUT_SMALL	= 1;
	private static final int			LAYOUT_MEDIUM	= 2;
	private static final int			LAYOUT_LARGE	= 3;

	private ISelectionListener			fSelectionListener;

	private Font						fontBold;

	private Color						fBgColorData;
	private Color						fBgColorHeader;

	private Composite					fPartContainer;
	private Composite					fContainer;

	private ChartDataModel				fChartDataModel;

	private ArrayList<ChartDrawingData>	fDrawingData;
	private ArrayList<GraphInfo>		fGraphInfos;

	private final ColorCache			fColorCache		= new ColorCache();

	private SelectionChartInfo			fChartInfo;

	private int							fLayout;

	private ScrolledComposite			fScrolledContainer;

	/**
	 * This class generates and contains the labels for one row in the view
	 */
	class GraphInfo {

		Label			left;
		Label			right;

		Label			min;
		Label			max;

		Label			avg;
		Label			diff;

		ChartDataSerie	fChartData;

		GraphInfo(final ChartDataSerie chartData) {
			fChartData = chartData;
		}

		void createInfoAvg() {

			final Color lineColor = getColor(fChartData.getRgbLine()[0]);

			avg = new Label(fContainer, SWT.TRAIL);
			avg.setForeground(lineColor);
			avg.setBackground(fBgColorData);

			final GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
			gd.horizontalIndent = 5;
			avg.setLayoutData(gd);
		}

		void createInfoDiff() {

			Color lineColor = getColor(fChartData.getRgbLine()[0]);

			diff = new Label(fContainer, SWT.TRAIL);
			diff.setForeground(lineColor);
			diff.setBackground(fBgColorData);

			final GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
			gd.horizontalIndent = 5;
			diff.setLayoutData(gd);
		}

		void createInfoLeft() {

			left = new Label(fContainer, SWT.TRAIL);
			left.setForeground(getColor(fChartData.getRgbLine()[0]));
			left.setBackground(fBgColorData);

			final GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
			gd.horizontalIndent = 5;
			left.setLayoutData(gd);
		}

		void createInfoMin() {

			min = new Label(fContainer, SWT.TRAIL);
			min.setForeground(getColor(fChartData.getRgbLine()[0]));
			min.setBackground(fBgColorData);

			final GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
			gd.horizontalIndent = 5;
			min.setLayoutData(gd);

		}

		void createInfoMax() {

			max = new Label(fContainer, SWT.TRAIL);
			max.setForeground(getColor(fChartData.getRgbLine()[0]));
			max.setBackground(fBgColorData);

			final GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
			gd.horizontalIndent = 5;
			max.setLayoutData(gd);
		}

		void createInfoRight() {

			right = new Label(fContainer, SWT.TRAIL);
			right.setForeground(getColor(fChartData.getRgbLine()[0]));
			right.setBackground(fBgColorData);

			final GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
			gd.horizontalIndent = 5;
			right.setLayoutData(gd);
		}

		private void createValueLabel() {

			Color lineColor = getColor(fChartData.getRgbLine()[0]);
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

			final Color lineColor = getColor(fChartData.getRgbLine()[0]);
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

	private void createHeaderAvg() {

		Label label = new Label(fContainer, SWT.TRAIL);
		label.setText(Messages.TourAnalyzer_Label_average);
		label.setFont(fontBold);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		label.setBackground(fBgColorHeader);
	}

	private void createHeaderDiff() {

		Label label = new Label(fContainer, SWT.TRAIL);
		label.setText(Messages.TourAnalyzer_Label_difference);
		label.setFont(fontBold);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		label.setBackground(fBgColorHeader);
	}

	private void createHeaderLeft() {

		Label label = new Label(fContainer, SWT.TRAIL);
		label.setText(Messages.TourAnalyzer_Label_left);
		label.setFont(fontBold);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		label.setBackground(fBgColorHeader);
	}

	private void createHeaderMax() {

		Label label = new Label(fContainer, SWT.TRAIL);
		label.setText(Messages.TourAnalyzer_Label_maximum);
		label.setFont(fontBold);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		label.setBackground(fBgColorHeader);
	}

	private void createHeaderMin() {

		Label label = new Label(fContainer, SWT.TRAIL);
		label.setText(Messages.TourAnalyzer_Label_minimum);
		label.setFont(fontBold);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		label.setBackground(fBgColorHeader);
	}

	private void createHeaderRight() {

		Label label = new Label(fContainer, SWT.TRAIL);
		label.setText(Messages.TourAnalyzer_Label_right);
		label.setFont(fontBold);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		label.setBackground(fBgColorHeader);
	}

	private void createHeaderUnitLabel() {

		Label label = new Label(fContainer, SWT.LEFT);
		label.setText(""); //$NON-NLS-1$
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		label.setBackground(fBgColorHeader);
	}

	private void createHeaderValueLabel() {

		Label label = new Label(fContainer, SWT.NONE);
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
		int clientWidth = fPartContainer.getClientArea().width;
		fLayout = clientWidth < 110 ? LAYOUT_TINY : clientWidth < 150
				? LAYOUT_SMALL
				: clientWidth < 370 ? LAYOUT_MEDIUM : LAYOUT_LARGE;

		// create scrolled container
		fScrolledContainer = new ScrolledComposite(fPartContainer, SWT.V_SCROLL | SWT.H_SCROLL);
		fScrolledContainer.setExpandVertical(true);
		fScrolledContainer.setExpandHorizontal(true);

		fScrolledContainer.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				fScrolledContainer.setMinSize(fContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
		});

		// create container
		fContainer = new Composite(fScrolledContainer, SWT.NONE);
		fContainer.setBackground(fBgColorHeader);

		GridLayout gl = new GridLayout(fLayout == LAYOUT_TINY ? 2 : fLayout == LAYOUT_SMALL
				? 3
				: fLayout == LAYOUT_MEDIUM ? 4 : 8, false);
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		gl.verticalSpacing = 0;
		gl.horizontalSpacing = 0;
		fContainer.setLayout(gl);

		fScrolledContainer.setContent(fContainer);

		createLayoutDetails();
	}

	private void createLayoutDetails() {

		if (fLayout == LAYOUT_TINY) {

			fGraphInfos = new ArrayList<GraphInfo>();

			// create graph info list
			for (final ChartDataSerie xyData : fChartDataModel.getXyData()) {
				fGraphInfos.add(new GraphInfo(xyData));
			}

			// ----------------------------------------------------------------

			createHeaderLeft();
			createHeaderUnitLabel();

			for (GraphInfo graphInfo : fGraphInfos) {
				graphInfo.createInfoLeft();
				graphInfo.createValueUnit();
			}

			// ----------------------------------------------------------------

			createVerticalBorder();

			createHeaderRight();
			createHeaderUnitLabel();

			for (GraphInfo graphInfo : fGraphInfos) {
				graphInfo.createInfoRight();
				graphInfo.createValueUnit();
			}

			// ----------------------------------------------------------------

			createVerticalBorder();

			createHeaderDiff();
			createHeaderUnitLabel();

			for (GraphInfo graphInfo : fGraphInfos) {
				graphInfo.createInfoDiff();
				graphInfo.createValueUnit();
			}

			// ----------------------------------------------------------------

			createVerticalBorder();

			createHeaderAvg();
			createHeaderUnitLabel();

			for (GraphInfo graphInfo : fGraphInfos) {
				graphInfo.createInfoAvg();
				graphInfo.createValueUnit();
			}

			// ----------------------------------------------------------------

			createVerticalBorder();

			createHeaderMin();
			createHeaderUnitLabel();

			for (GraphInfo graphInfo : fGraphInfos) {
				graphInfo.createInfoMin();
				graphInfo.createValueUnit();
			}

			// ----------------------------------------------------------------

			createVerticalBorder();

			createHeaderMax();
			createHeaderUnitLabel();

			for (GraphInfo graphInfo : fGraphInfos) {
				graphInfo.createInfoMax();
				graphInfo.createValueUnit();
			}

		} else if (fLayout == LAYOUT_SMALL) {

			createHeaderLeft();
			createHeaderRight();
			createHeaderUnitLabel();

			// add all graphs and the x axis to the layout
			fGraphInfos = new ArrayList<GraphInfo>();
			for (final ChartDataSerie xyData : fChartDataModel.getXyData()) {

				GraphInfo graphInfo = new GraphInfo(xyData);

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

			for (GraphInfo graphInfo : fGraphInfos) {
				graphInfo.createInfoDiff();
				graphInfo.createInfoAvg();
				graphInfo.createValueUnit();
			}

			// ----------------------------------------------------------------

			createVerticalBorder();

			createHeaderMin();
			createHeaderMax();
			createHeaderUnitLabel();

			for (GraphInfo graphInfo : fGraphInfos) {
				graphInfo.createInfoMin();
				graphInfo.createInfoMax();
				graphInfo.createValueUnit();
			}

		} else if (fLayout == LAYOUT_MEDIUM) {

			createHeaderLeft();
			createHeaderRight();
			createHeaderDiff();
			createHeaderUnitLabel();

			// add all graphs and the x axis to the layout
			fGraphInfos = new ArrayList<GraphInfo>();
			for (final ChartDataSerie xyData : fChartDataModel.getXyData()) {

				GraphInfo graphInfo = new GraphInfo(xyData);

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

			for (GraphInfo graphInfo : fGraphInfos) {
				graphInfo.createInfoMin();
				graphInfo.createInfoMax();
				graphInfo.createInfoAvg();
				graphInfo.createValueUnit();
			}

		} else if (fLayout == LAYOUT_LARGE) {

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

				GraphInfo graphInfo = new GraphInfo(xyData);

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
	}

	public void createPartControl(final Composite parent) {

		fPartContainer = parent;

		fBgColorData = getColor(new RGB(240, 240, 240));
		// fBgColorData = Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
		// fBgColorData = Display.getCurrent().getSystemColor(SWT.COLOR_WHITE);

		fBgColorHeader = Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
		// fBgColorHeader =
		// Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW);

		fontBold = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);

		setListeners();
	}

	private void createVerticalBorder() {

		GridData gd;

		int columns = fLayout == LAYOUT_TINY ? 1 : fLayout == LAYOUT_SMALL
				? 2
				: fLayout == LAYOUT_MEDIUM ? 3 : 8;

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

	public void dispose() {

		getSite().getPage().removePostSelectionListener(fSelectionListener);

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
			return fColorCache.put(colorKey, rgb);
		} else {
			return color;
		}
	}

	public void setFocus() {
		if (fScrolledContainer != null) {
			fScrolledContainer.setFocus();
		}
	}

	private void setListeners() {

		fPartContainer.addControlListener(new ControlAdapter() {
			public void controlResized(final ControlEvent event) {
				if (fChartDataModel == null) {
					return;
				}
				createLayout();
				updateInfo(fChartInfo);
			}
		});

		fSelectionListener = new ISelectionListener() {
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

				if (selection instanceof SelectionChartInfo) {
					updateInfo((SelectionChartInfo) selection);
				}

				if (selection instanceof SelectionChartXSliderPosition) {
					Chart tourChart = ((SelectionChartXSliderPosition) selection).chart;
					if (tourChart != null) {
						updateInfo(tourChart.getChartInfo());
					}
				}

				if (selection instanceof SelectionTourChart) {
					TourChart tourChart = ((SelectionTourChart) selection).getTourChart();
					if (tourChart != null) {
						updateInfo(tourChart.getChartInfo());
					}
				}
			}
		};
		getSite().getPage().addPostSelectionListener(fSelectionListener);
	}

	private void updateInfo(final SelectionChartInfo chartInfo) {

		if (chartInfo == null) {
			return;
		}

		fChartInfo = chartInfo;

		// check if the layout needs to be recreated
		boolean isLayoutDirty = false;
		if (fChartDataModel != chartInfo.chartDataModel) {
			/*
			 * there is a different data model than before, a new layout needs to be created
			 */
			isLayoutDirty = true;
		}

		// init vars which are used in createLayout()
		fChartDataModel = chartInfo.chartDataModel;
		fDrawingData = chartInfo.chartDrawingData;

		if (fDrawingData == null) {
			// this happened
			return;
		}

		if (fGraphInfos == null || isLayoutDirty) {
			createLayout();
		}

		updateInfoData(chartInfo);

		// refresh the layout after the data has changed
		fPartContainer.layout();
	}

	private void updateInfoData(final SelectionChartInfo chartInfo) {

		final ChartDataXSerie xData = fDrawingData.get(0).getXData();
		int valuesIndexLeft = chartInfo.leftSlider.getValuesIndex();
		int valuesIndexRight = chartInfo.rightSlider.getValuesIndex();

		for (final GraphInfo graphInfo : fGraphInfos) {

			final ChartDataSerie serieData = graphInfo.fChartData;
			TourChartAnalyzerInfo analyzerInfo = (TourChartAnalyzerInfo) serieData
					.getCustomData(TourManager.ANALYZER_INFO);

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
				computeAvg.valuesIndexLeft = valuesIndexLeft;
				computeAvg.valuesIndexRight = valuesIndexRight;
				computeAvg.xData = xData;
				computeAvg.yData = (ChartDataYSerie) serieData;
				computeAvg.chartModel = fChartDataModel;
				avg = computeAvg.compute();
			} else {
				if (avgDiv != 0) {
					avg = avg / avgDiv;
				}
			}

			graphInfo.left.setText(ChartUtil.formatValue(leftValue, xAxisUnit, valueDivisor, true)
					+ " "); //$NON-NLS-1$
			graphInfo.right.setText(ChartUtil
					.formatValue(rightValue, xAxisUnit, valueDivisor, true)
					+ " "); //$NON-NLS-1$

			graphInfo.min.setText(ChartUtil.formatValue(min, xAxisUnit, valueDivisor, true) + " "); //$NON-NLS-1$
			graphInfo.max.setText(ChartUtil.formatValue(max, xAxisUnit, valueDivisor, true) + " "); //$NON-NLS-1$

			// set average value
			if (analyzerInfo.isShowAvg()) {
				if (analyzerInfo.isShowAvgDecimals()) {
					final int avgDivisor = (int) Math.pow(10, analyzerInfo.getAvgDecimals());
					avg *= avgDivisor;
					graphInfo.avg.setText(ChartUtil.formatInteger(
							(int) avg,
							avgDivisor,
							analyzerInfo.getAvgDecimals(),
							false)
							+ " "); //$NON-NLS-1$
				} else {
					graphInfo.avg.setText(ChartUtil.formatValue(
							(int) avg,
							xAxisUnit,
							valueDivisor,
							true)
							+ " "); //$NON-NLS-1$
				}
			} else {
				graphInfo.avg.setText(""); //$NON-NLS-1$
			}

			graphInfo.diff.setText(ChartUtil.formatValue(
					rightValue - leftValue,
					xAxisUnit,
					valueDivisor,
					true)
					+ " "); //$NON-NLS-1$
		}
	}
}
