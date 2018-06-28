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
package net.tourbook.ui.views;

import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.ui.UI;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

/**
 * This class generates and contains the labels for one row in the view
 */
class GraphInfo {

	private final TourChartAnalyzerView	_tourChartAnalyzerView;

	double								prevLeftValue	= Double.MIN_VALUE;
	double								prevRightValue	= Double.MIN_VALUE;

	double								prevMinValue	= Double.MIN_VALUE;
	double								prevMaxValue	= Double.MIN_VALUE;

	double								prevAvgValue	= Double.MIN_VALUE;
	double								prevDiffValue	= Double.MIN_VALUE;

	ChartDataSerie						chartData;
	private int							_columnSpacing;

	/*
	 * UI controls
	 */
	private final Composite				_parent;

	Label								lblAvg;
	Label								lblDiff;
	Label								lblLeft;
	Label								lblMax;
	Label								lblMin;
	Label								lblRight;

	private final Color					_bgColorData;

	GraphInfo(final TourChartAnalyzerView tourChartAnalyzerView, final ChartDataSerie chartData, final Composite parent) {

		_tourChartAnalyzerView = tourChartAnalyzerView;
		_columnSpacing = _tourChartAnalyzerView._columnSpacing;

		this.chartData = chartData;
		_parent = parent;
		_bgColorData = Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND);

	}

	void createUI_Info_10_Left() {

		lblLeft = new Label(_parent, SWT.TRAIL);
		lblLeft.setForeground(_tourChartAnalyzerView.getColor(chartData.getDefaultRGB()));
		lblLeft.setBackground(_bgColorData);

		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.align(SWT.FILL, SWT.CENTER)
				.indent(_columnSpacing, 0)
				.applyTo(lblLeft);
	}

	void createUI_Info_20_Right() {

		lblRight = new Label(_parent, SWT.TRAIL);
		lblRight.setForeground(_tourChartAnalyzerView.getColor(chartData.getDefaultRGB()));
		lblRight.setBackground(_bgColorData);

		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.align(SWT.FILL, SWT.CENTER)
				.indent(_columnSpacing, 0)
				.applyTo(lblRight);
	}

	void createUI_Info_30_Min() {

		lblMin = new Label(_parent, SWT.TRAIL);
		lblMin.setForeground(_tourChartAnalyzerView.getColor(chartData.getDefaultRGB()));
		lblMin.setBackground(_bgColorData);

		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.align(SWT.FILL, SWT.CENTER)
				.indent(_columnSpacing, 0)
				.applyTo(lblMin);
	}

	void createUI_Info_40_Max() {

		lblMax = new Label(_parent, SWT.TRAIL);
		lblMax.setForeground(_tourChartAnalyzerView.getColor(chartData.getDefaultRGB()));
		lblMax.setBackground(_bgColorData);

		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.align(SWT.FILL, SWT.CENTER)
				.indent(_columnSpacing, 0)
				.applyTo(lblMax);
	}

	void createUI_Info_50_Diff() {

		final Color lineColor = _tourChartAnalyzerView.getColor(chartData.getDefaultRGB());

		lblDiff = new Label(_parent, SWT.TRAIL);
		lblDiff.setForeground(lineColor);
		lblDiff.setBackground(_bgColorData);

		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.align(SWT.FILL, SWT.CENTER)
				.indent(_columnSpacing, 0)
				.applyTo(lblDiff);
	}

	void createUI_Info_60_Avg() {

		final Color lineColor = _tourChartAnalyzerView.getColor(chartData.getDefaultRGB());

		lblAvg = new Label(_parent, SWT.TRAIL);
		lblAvg.setForeground(lineColor);
		lblAvg.setBackground(_bgColorData);

		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.align(SWT.FILL, SWT.CENTER)
				.indent(_columnSpacing, 0)
				.applyTo(lblAvg);
	}

	void createUI_ValueLabel() {

		final Color lineColor = _tourChartAnalyzerView.getColor(chartData.getDefaultRGB());

		String labelText;
		if (chartData instanceof ChartDataYSerie) {
			labelText = ((ChartDataYSerie) chartData).getYTitle();
		} else {
			labelText = chartData.getLabel();
		}
		final Label label = new Label(_parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(label);
		label.setText(UI.SPACE + labelText);
		label.setForeground(lineColor);
		label.setBackground(_bgColorData);
	}

	void createUI_ValueUnit() {

		final Color lineColor = _tourChartAnalyzerView.getColor(chartData.getDefaultRGB());

		String toolTip;
		if (chartData instanceof ChartDataYSerie) {
			toolTip = ((ChartDataYSerie) chartData).getYTitle();
		} else {
			toolTip = chartData.getLabel();
		}

		final String unitLabel = chartData.getUnitLabel();

		final Label label = new Label(_parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.align(SWT.FILL, SWT.CENTER)
				.indent(_columnSpacing, 0)
				.grab(true, false)
				.applyTo(label);
		label.setText(unitLabel.length() > 0 ? UI.SPACE + unitLabel : unitLabel);
		label.setToolTipText(toolTip);
		label.setForeground(lineColor);
		label.setBackground(_bgColorData);

		// spacer
//			final Canvas canvas = new Canvas(_innerScContainer, SWT.NONE);
//			GridDataFactory.fillDefaults()//
//					.align(SWT.BEGINNING, SWT.BEGINNING)
//					.hint(0, 0)
//					.applyTo(canvas);
	}
}
