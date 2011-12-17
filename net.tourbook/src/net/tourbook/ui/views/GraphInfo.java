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

	Label								lblLeft;
	Label								lblRight;

	Label								lblMin;
	Label								lblMax;

	Label								lblAvg;
	Label								lblDiff;

	float								leftValue	= Float.MIN_VALUE;
	float								rightValue	= Float.MIN_VALUE;

	float								minValue	= Float.MIN_VALUE;
	float								maxValue	= Float.MIN_VALUE;

	float								avgValue	= Float.MIN_VALUE;
	float								diffValue	= Float.MIN_VALUE;

	ChartDataSerie						_chartData;

	private final Composite				_parent;

	private final Color					_bgColorData;

	GraphInfo(final TourChartAnalyzerView tourChartAnalyzerView, final ChartDataSerie chartData, final Composite parent) {

		_tourChartAnalyzerView = tourChartAnalyzerView;

		_chartData = chartData;
		_parent = parent;
		_bgColorData = Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND);

	}

	void createUIInfo10Left() {

		lblLeft = new Label(_parent, SWT.TRAIL);
		lblLeft.setForeground(_tourChartAnalyzerView.getColor(_chartData.getDefaultRGB()));
		lblLeft.setBackground(_bgColorData);

		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.align(SWT.FILL, SWT.CENTER)
				.indent(_tourChartAnalyzerView._columnSpacing, 0)
				.applyTo(lblLeft);
	}

	void createUIInfo20Right() {

		lblRight = new Label(_parent, SWT.TRAIL);
		lblRight.setForeground(_tourChartAnalyzerView.getColor(_chartData.getDefaultRGB()));
		lblRight.setBackground(_bgColorData);

		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.align(SWT.FILL, SWT.CENTER)
				.indent(_tourChartAnalyzerView._columnSpacing, 0)
				.applyTo(lblRight);
	}

	void createUIInfo30Min() {

		lblMin = new Label(_parent, SWT.TRAIL);
		lblMin.setForeground(_tourChartAnalyzerView.getColor(_chartData.getDefaultRGB()));
		lblMin.setBackground(_bgColorData);

		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.align(SWT.FILL, SWT.CENTER)
				.indent(_tourChartAnalyzerView._columnSpacing, 0)
				.applyTo(lblMin);
	}

	void createUIInfo40Max() {

		lblMax = new Label(_parent, SWT.TRAIL);
		lblMax.setForeground(_tourChartAnalyzerView.getColor(_chartData.getDefaultRGB()));
		lblMax.setBackground(_bgColorData);

		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.align(SWT.FILL, SWT.CENTER)
				.indent(_tourChartAnalyzerView._columnSpacing, 0)
				.applyTo(lblMax);
	}

	void createUIInfo50Diff() {

		final Color lineColor = _tourChartAnalyzerView.getColor(_chartData.getDefaultRGB());

		lblDiff = new Label(_parent, SWT.TRAIL);
		lblDiff.setForeground(lineColor);
		lblDiff.setBackground(_bgColorData);

		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.align(SWT.FILL, SWT.CENTER)
				.indent(_tourChartAnalyzerView._columnSpacing, 0)
				.applyTo(lblDiff);
	}

	void createUIInfo60Avg() {

		final Color lineColor = _tourChartAnalyzerView.getColor(_chartData.getDefaultRGB());

		lblAvg = new Label(_parent, SWT.TRAIL);
		lblAvg.setForeground(lineColor);
		lblAvg.setBackground(_bgColorData);

		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.align(SWT.FILL, SWT.CENTER)
				.indent(_tourChartAnalyzerView._columnSpacing, 0)
				.applyTo(lblAvg);
	}

	void createUIValueLabel() {

		final Color lineColor = _tourChartAnalyzerView.getColor(_chartData.getDefaultRGB());

		String labelText;
		if (_chartData instanceof ChartDataYSerie) {
			labelText = ((ChartDataYSerie) _chartData).getYTitle();
		} else {
			labelText = _chartData.getLabel();
		}
		final Label label = new Label(_parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(label);
		label.setText(UI.SPACE + labelText);
		label.setForeground(lineColor);
		label.setBackground(_bgColorData);
	}

	void createUIValueUnit() {

		final Color lineColor = _tourChartAnalyzerView.getColor(_chartData.getDefaultRGB());

		String toolTip;
		if (_chartData instanceof ChartDataYSerie) {
			toolTip = ((ChartDataYSerie) _chartData).getYTitle();
		} else {
			toolTip = _chartData.getLabel();
		}

		final Label label = new Label(_parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.align(SWT.FILL, SWT.CENTER)
				.indent(_tourChartAnalyzerView._columnSpacing, 0)
				.grab(true, false)
				.applyTo(label);
		label.setText(UI.SPACE + _chartData.getUnitLabel());
		label.setToolTipText(toolTip);
		label.setForeground(lineColor);
//			label.setBackground(_bgColorHeader);
		label.setBackground(_bgColorData);

		// spacer
//			final Canvas canvas = new Canvas(_innerScContainer, SWT.NONE);
//			GridDataFactory.fillDefaults()//
//					.align(SWT.BEGINNING, SWT.BEGINNING)
//					.hint(0, 0)
//					.applyTo(canvas);
	}
}
