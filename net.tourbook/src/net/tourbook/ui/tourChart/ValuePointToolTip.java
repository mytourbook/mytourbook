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
package net.tourbook.ui.tourChart;

import java.text.NumberFormat;

import net.tourbook.chart.IValuePointToolTip;
import net.tourbook.data.TourData;
import net.tourbook.ui.Messages;
import net.tourbook.ui.UI;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * This tooltip is displayed when the mouse is hovered over a value point in a line graph and
 * displays value point information.
 */
public class ValuePointToolTip extends ToolTipVP implements IValuePointToolTip {

//	private static final int		SHELL_MARGIN	= 5;

	private TourData				_tourData;

	private boolean					_isAltitude;
	private boolean					_isCadence;
	private boolean					_isDistance;
	private boolean					_isGradient;
	private boolean					_isPace;
	private boolean					_isPower;
	private boolean					_isPulse;
	private boolean					_isTemperature;

	private int						_currentValueIndex;

	private final DateTimeFormatter	_dtFormatter	= DateTimeFormat.mediumDateTime();

	private final NumberFormat		_nf1			= NumberFormat.getNumberInstance();
	private final NumberFormat		_nf1NoGroup		= NumberFormat.getNumberInstance();
	private final NumberFormat		_nf3			= NumberFormat.getNumberInstance();
	private final NumberFormat		_nf3NoGroup		= NumberFormat.getNumberInstance();
	{
		_nf1.setMinimumFractionDigits(1);
		_nf1.setMaximumFractionDigits(1);
		_nf3.setMinimumFractionDigits(3);
		_nf3.setMaximumFractionDigits(3);

		_nf1NoGroup.setMinimumFractionDigits(1);
		_nf1NoGroup.setMaximumFractionDigits(1);
		_nf1NoGroup.setGroupingUsed(false);

		_nf3NoGroup.setMinimumFractionDigits(3);
		_nf3NoGroup.setMaximumFractionDigits(3);
		_nf3NoGroup.setGroupingUsed(false);
	}

	/*
	 * UI resources
	 */
	private Color					_bgColor;
	private Color					_fgColor;
	private Font					_boldFont;
	private PixelConverter			_pc;

	/*
	 * UI controls
	 */
	private Control					_ttControl;
	private Composite				_ttContainer;

	private Text					_txtTime;
	private Text					_txtAltitude;
	private Text					_txtDistance;

	public ValuePointToolTip(final Control control) {

		super(control);

		_ttControl = control;
	}

	@Override
	protected Composite createToolTipContentArea(final Event event, final Composite parent) {

		Composite shell;

		if (_tourData == null || _tourData.timeSerie == null || _tourData.timeSerie.length == 0) {

			// there are no data available

			shell = createUI99NoData(parent);

		} else {

			// tour data is available

			shell = createUI(parent);
		}

		return shell;
	}

	private Composite createUI(final Composite parent) {

		final Display display = parent.getDisplay();

		_bgColor = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
		_fgColor = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);
		_boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);

		_pc = new PixelConverter(parent);

		final Composite shell = createUI10Shell(parent);

		updateUI(_currentValueIndex, true);
//		enableControls();
//
//		// compute width for all controls and equalize column width for the different sections
		_ttContainer.layout(true, true);
//		UI.setEqualizeColumWidths(_firstColumnControls, 5);
//		UI.setEqualizeColumWidths(_secondColumnControls);

		return shell;

	}

	private Composite createUI10Shell(final Composite parent) {

		final int SHELL_MARGIN = 0;

		/*
		 * shell container is necessary because the margins of the inner container will hide the
		 * tooltip when the mouse is hovered, which is not as it should be.
		 */
		final Composite shellContainer = new Composite(parent, SWT.NONE);
		shellContainer.setForeground(_fgColor);
		shellContainer.setBackground(_bgColor);
		GridLayoutFactory.fillDefaults().applyTo(shellContainer);
//		shellContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
		{
			_ttContainer = new Composite(shellContainer, SWT.NONE);
			_ttContainer.setForeground(_fgColor);
			_ttContainer.setBackground(_bgColor);
			GridLayoutFactory.fillDefaults() //
//					.numColumns(2)
//					.equalWidth(true)
					.margins(SHELL_MARGIN, SHELL_MARGIN)
					.applyTo(_ttContainer);
//			_ttContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
			{
				createUI20(_ttContainer);
			}
		}

		return shellContainer;
	}

	private void createUI20(final Composite parent) {

//		final GraphColorProvider colorProvider = GraphColorProvider.getInstance();

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults()//
//				.margins(0, 0)
				.extendedMargins(2, 0, 0, 0)
				.spacing(5, 0)
				.numColumns(2)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			/*
			 * time
			 */
			Label label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().applyTo(label);
			label.setText(Messages.Tooltip_Label_Time);

//			final Color fgColor = colorCache.getColor(//
//					GraphColorProvider.PREF_GRAPH_TIME, //
//					colorProvider.getGraphColorDefinition(GraphColorProvider.PREF_GRAPH_TIME).getTextColor());

			_txtTime = new Text(container, SWT.READ_ONLY);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtTime);

			/*
			 * distance
			 */
			if (_isDistance) {

				label = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults().applyTo(label);
				label.setText(Messages.Tooltip_Label_Distance);

				_txtDistance = new Text(container, SWT.READ_ONLY);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtDistance);
			}

			/*
			 * altitude
			 */
			if (_isAltitude) {

				label = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults().applyTo(label);
				label.setText(Messages.Tooltip_Label_Altitude);

				_txtAltitude = new Text(container, SWT.READ_ONLY);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtAltitude);
			}
		}
	}

	private Composite createUI99NoData(final Composite parent) {

		/*
		 * shell container is necessary because the margins of the inner container will hide the
		 * tooltip when the mouse is hovered, which is not as it should be.
		 */
		final Composite shellContainer = new Composite(parent, SWT.NONE);
		shellContainer.setForeground(_fgColor);
		shellContainer.setBackground(_bgColor);
		GridLayoutFactory.fillDefaults().applyTo(shellContainer);
		{

			final Composite container = new Composite(shellContainer, SWT.NONE);
			container.setForeground(_fgColor);
			container.setBackground(_bgColor);
			GridLayoutFactory.fillDefaults()//
					.margins(5, 5)
					.applyTo(container);
			{
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Tour_Tooltip_Label_NoTour);
				label.setForeground(_fgColor);
				label.setBackground(_bgColor);
			}
		}

		return shellContainer;
	}

	@Override
	protected Object getToolTipArea(final Event event) {
		// TODO Auto-generated method stub
		return super.getToolTipArea(event);
	}

	/**
	 * @param tourData
	 *            When <code>null</code> the tooltip will be hidden.
	 */
	void setTourData(final TourData tourData) {

		_tourData = tourData;
		_currentValueIndex = 0;

		if (tourData == null) {
			hide();
			return;
		}

		_isAltitude = _tourData.altitudeSerie != null;
		_isCadence = _tourData.cadenceSerie != null;
		_isDistance = _tourData.distanceSerie != null;
		_isGradient = _tourData.gradientSerie != null;
		_isPace = _tourData.getPaceSerie() != null;
		_isPower = _tourData.getPowerSerie() != null;
		_isPulse = _tourData.pulseSerie != null;
		_isTemperature = _tourData.temperatureSerie != null;
	}

	@Override
	public void setValueIndex(final int valueIndex, final int devXMouseMove, final int devYMouseMove) {

		if (_tourData == null || _ttContainer == null) {
			return;
		}

		if (_ttContainer.isDisposed()) {
			// tool tip is disposed, this happens on a mouse exit
			show(new Point(devXMouseMove, devYMouseMove));
		}

		updateUI(valueIndex, false);
	}

	@Override
	protected boolean shouldCreateToolTip(final Event event) {

		if (_tourData == null) {
			return false;
		}

		return super.shouldCreateToolTip(event);
	}

	private void updateUI(int valueIndex, boolean isForceUpdate) {

		final int[] timeSerie = _tourData.timeSerie;

		// check bounds
		if (valueIndex >= timeSerie.length || valueIndex < 0) {
			valueIndex = timeSerie.length;
		}

		// optimize update
		if (isForceUpdate == false && valueIndex == _currentValueIndex) {
			return;
		}

		isForceUpdate = false;
		_currentValueIndex = valueIndex;

		final float time = timeSerie[valueIndex];

		_txtTime.setText(UI.format_hhh_mm_ss((long) time) + UI.SPACE + UI.UNIT_LABEL_TIME);

		if (_isAltitude) {
			_txtAltitude.setText(_nf3NoGroup.format(_tourData.altitudeSerie[valueIndex] / UI.UNIT_VALUE_ALTITUDE)
					+ UI.SPACE
					+ UI.UNIT_LABEL_ALTITUDE);
		}

		if (_isDistance) {

			final float distance = _tourData.distanceSerie[valueIndex] / 1000 / UI.UNIT_VALUE_DISTANCE;

			_txtDistance.setText(_nf3NoGroup.format(distance) + UI.SPACE + UI.UNIT_LABEL_DISTANCE);
		}
	}

}
