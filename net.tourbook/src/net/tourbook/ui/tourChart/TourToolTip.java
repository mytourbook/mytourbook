/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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

import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.Messages;
import net.tourbook.ui.UI;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class TourToolTip extends ToolTip {

	private TourData				_tourData;

	private Color					_bgColor;
	private Color					_fgColor;
	private Font					_boldFont;

	private final DateTimeFormatter	_dateFormatter	= DateTimeFormat.fullDate();

	/*
	 * 1. column
	 */
	private Label					_lblTitle;
	private Label					_lblDistance;
	private Label					_lblDistanceUnit;
	private Label					_lblAltitude;
	private Label					_lblAltitudeUnit;

	/*
	 * 2. column
	 */
	private Label					_lblDate;

	public TourToolTip(final Control control) {
		this(control, NO_RECREATE, false);
	}

	public TourToolTip(final Control control, final int style, final boolean manualActivation) {
		super(control, style, manualActivation);
	}

	@Override
	protected Composite createToolTipContentArea(final Event event, final Composite parent) {

		if (_tourData == null) {
			return null;
		}

		final Display display = parent.getDisplay();
		_bgColor = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
		_fgColor = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);
		_boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);

		final Composite container = createUI(parent);
		updateUI();

		return container;
	}

	private Composite createUI(final Composite parent) {

//		/*
//		 * shell container is necessary because the margins of the inner container will hide the
//		 * tooltip which is not as it should be.
//		 */
//		final Composite shellContainer = new Composite(parent, SWT.NONE);
//		shellContainer.setForeground(_fgColor);
//		shellContainer.setBackground(_bgColor);
//		shellContainer.setLayout(new FillLayout());
//		{

			final Composite container = new Composite(parent, SWT.NONE);
			container.setForeground(_fgColor);
			container.setBackground(_bgColor);
			GridLayoutFactory
					.fillDefaults()
					.numColumns(2)
//					.spacing(20, 5)
//					.extendedMargins(2, 2, 2, 2)
					.applyTo(container);
		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
			{
				/*
				 * title
				 */
				_lblTitle = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults().span(2, 1).applyTo(_lblTitle);
				_lblTitle.setFont(_boldFont);
				_lblTitle.setForeground(_fgColor);
				_lblTitle.setBackground(_bgColor);

				createUI10LeftColumn(container);
				createUI20RightColumn(container);
			}
//		}

		return container;
	}

	private void createUI10LeftColumn(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(container);
		container.setForeground(_fgColor);
		container.setBackground(_bgColor);
		GridLayoutFactory.fillDefaults().numColumns(3).spacing(5, 0).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			/*
			 * distance
			 */
			createUILabel(container, Messages.Tour_Tooltip_Label_Distance);
			_lblDistance = createUILabelValue(container, SWT.TRAIL);
			_lblDistanceUnit = createUILabelValue(container, SWT.LEAD);

			/*
			 * altitude
			 */
			createUILabel(container, Messages.Tour_Tooltip_Label_Altitude);
			_lblAltitude = createUILabelValue(container, SWT.TRAIL);
			_lblAltitudeUnit = createUILabelValue(container, SWT.LEAD);
		}
	}

	private void createUI20RightColumn(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(container);
		container.setForeground(_fgColor);
		container.setBackground(_bgColor);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			/*
			 * date
			 */
			createUILabel(container, Messages.Tour_Tooltip_Label_Date);

			_lblDate = createUILabelValue(container, SWT.LEAD);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(_lblDate);

		}
	}

	private Label createUILabel(final Composite parent, final String labelText) {

		final Label label = new Label(parent, SWT.NONE);
		label.setText(labelText);
		label.setForeground(_fgColor);
		label.setBackground(_bgColor);

		return label;
	}

	private Label createUILabelValue(final Composite parent, final int style) {

		final Label label = new Label(parent, style);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(label);
		label.setForeground(_fgColor);
		label.setBackground(_bgColor);

		return label;
	}

	@Override
	public Point getLocation(final Point tipSize, final Event event) {

//		// try to position the tooltip at the bottom of the cell
//		ViewerCell cell = v.getCell(new Point(event.x, event.y));
//
//		if( cell != null ) {
//			return tree.toDisplay(event.x,cell.getBounds().y+cell.getBounds().height);
//		}

		return super.getLocation(tipSize, event);
	}

//	final Display display = _toolTipShell.getDisplay();
//	final Color infoColorBackground = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
//	final Color infoColorForeground = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);
//
//	_toolTipContainer = new Composite(_toolTipShell, SWT.NONE);
//	GridLayoutFactory.fillDefaults().extendedMargins(2, 5, 2, 3).applyTo(_toolTipContainer);
//
//	_toolTipContainer.setBackground(infoColorBackground);
//	_toolTipContainer.setForeground(infoColorForeground);
//
//	_toolTipTitle = new Label(_toolTipContainer, SWT.LEAD);
//	_toolTipTitle.setBackground(infoColorBackground);
//	_toolTipTitle.setForeground(infoColorForeground);
//	_toolTipTitle.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));
//
//	_toolTipLabel = new Label(_toolTipContainer, SWT.LEAD | SWT.WRAP);
//	_toolTipLabel.setBackground(infoColorBackground);
//	_toolTipLabel.setForeground(infoColorForeground);

	@Override
	protected Object getToolTipArea(final Event event) {

//		// Ensure that the tooltip is hidden when the cell is left
//		return v.getCell(new Point(event.x, event.y));

		return super.getToolTipArea(event);
	}

	public void setTourData(final TourData tourData) {
		_tourData = tourData;
	}

	private void updateUI() {

//		final int oldestYear = _currentYear - _numberOfYears + 1;
//		final int tourDOY = tourDOYValues[valueIndex];
//		_calendar.set(oldestYear, 0, 1);
//		_calendar.set(Calendar.DAY_OF_YEAR, tourDOY + 1);
//		final String beginDate = _dateFormatter.format(_calendar.getTime());
//
//		_currentMonth = _calendar.get(Calendar.MONTH) + 1;
//		_selectedTourId = _tourTimeData.fTourIds[valueIndex];
//
//		final String tourTags = TourDatabase.getTagNames();
//		final String tourDescription = _tourTimeData.tourDescription.get(valueIndex).replace(
//				UI.SYSTEM_NEW_LINE,
//				UI.NEW_LINE);
//
//		final int[] startValue = _tourTimeData.fTourTimeStartValues;
//		final int[] endValue = _tourTimeData.fTourTimeEndValues;
//
//		final Integer recordingTime = _tourTimeData.fTourRecordingTimeValues.get(valueIndex);
//		final Integer drivingTime = _tourTimeData.fTourDrivingTimeValues.get(valueIndex);
//		final int breakTime = recordingTime - drivingTime;
//
//		final float distance = _tourTimeData.fTourDistanceValues[valueIndex];
//		final float speed = drivingTime == 0 ? 0 : distance / (drivingTime / 3.6f);
//		final int pace = (int) (distance == 0 ? 0 : (drivingTime * 1000 / distance));

		final DateTime dt = new DateTime(
				_tourData.getStartYear(),
				_tourData.getStartMonth(),
				_tourData.getStartDay(),
				_tourData.getStartHour(),
				_tourData.getStartMinute(),
				_tourData.getStartSecond(),
				0);

		final int recordingTime = _tourData.getTourRecordingTime();
		final int drivingTime = _tourData.getTourDrivingTime();
		final int breakTime = recordingTime - drivingTime;

		final int altiUp = (int) (_tourData.getTourAltUp() / UI.UNIT_VALUE_ALTITUDE);
		final float distance = (_tourData.getTourDistance() / UI.UNIT_VALUE_DISTANCE);

//		dbAltitude.add((int) (result.getInt(8) / UI.UNIT_VALUE_ALTITUDE));
//		dbDistance.add((int) (result.getInt(7) / UI.UNIT_VALUE_DISTANCE));

		final TourType tourType = _tourData.getTourType();
		final String tourTypeName = tourType == null ? //
				UI.EMPTY_STRING
				: TourDatabase.getTourTypeName(tourType.getTypeId());

		String tourTitle = _tourData.getTourTitle();
		if (tourTitle == null || tourTitle.trim().length() == 0) {
			tourTitle = tourTypeName.length() > 0 ? tourTypeName : UI.EMPTY_STRING;
		}

		_lblTitle.setText(tourTitle);

		/*
		 * left column
		 */
		_lblDistance.setText(Integer.toString((int) distance));
		_lblDistanceUnit.setText(UI.UNIT_LABEL_DISTANCE);

		_lblAltitude.setText(Integer.toString(altiUp));
		_lblAltitudeUnit.setText(UI.UNIT_LABEL_ALTITUDE);

		/*
		 * right column
		 */
		_lblDate.setText(_dateFormatter.print(dt.getMillis()));
	}
}
