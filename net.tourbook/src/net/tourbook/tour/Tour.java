/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import net.tourbook.data.TourData;
import net.tourbook.tour.TourOLD.ITourChangeListener;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IMemento;

public class Tour extends Composite {

	private CTabFolder		fTabFolder;
	private CTabItem		fChartItem;
	private CTabItem		fTourDataItem;

	private TourChart		fTourChart;

	public Calendar			fCalendar			= GregorianCalendar.getInstance();
	private DateFormat		fDateFormatter		= DateFormat.getDateInstance(DateFormat.SHORT);
	private DateFormat		fTimeFormatter		= DateFormat.getTimeInstance(DateFormat.SHORT);
	private DateFormat		fDurationFormatter	= DateFormat.getTimeInstance(
														DateFormat.SHORT,
														Locale.GERMAN);
	private NumberFormat	fNumberFormatter	= NumberFormat.getNumberInstance();

	private Label			fLblDate;
	private Label			fLblStartTime;
	private Label			fLblRecordingTime;
	private Label			fLblDrivingTime;
	private Label			fLblTitle;
	private Label			fLblStartLocation;
	private Label			fLblEndLocation;

	public Tour(Composite parent, int style) {

		super(parent, style);

		createControls();

	}

	public void addTourChangedListener(ITourChangeListener tourChangeListener) {

	}

	private void createControls() {

		setLayout(new FillLayout());

		fTabFolder = new CTabFolder(this, SWT.FLAT | SWT.BOTTOM);

		fTourChart = new TourChart(fTabFolder, SWT.FLAT, true);

		fChartItem = new CTabItem(fTabFolder, SWT.FLAT);
		fChartItem.setText("Tour Chart");
		fChartItem.setControl(fTourChart);

		fTourDataItem = new CTabItem(fTabFolder, SWT.FLAT);
		fTourDataItem.setText("Tour Data");
		fTourDataItem.setControl(createTourDataControl());

		// select the tour chart item
		fTabFolder.setSelection(fChartItem);
	}

	private Control createTourDataControl() {

		Composite container = new Composite(fTabFolder, SWT.NONE);
		container.setLayout(new GridLayout(2, false));

		Label label;
		final GridData gd = new GridData(SWT.FILL, SWT.NONE, true, false);

		// tour date
		label = new Label(container, SWT.NONE);
		label.setText("Tour Date:");
		fLblDate = new Label(container, SWT.NONE);
		fLblDate.setLayoutData(gd);

		// start time
		label = new Label(container, SWT.NONE);
		label.setText("Start Time:");
		fLblStartTime = new Label(container, SWT.NONE);
		fLblStartTime.setLayoutData(gd);

		// recording time
		label = new Label(container, SWT.NONE);
		label.setText("Recording Time:");
		fLblRecordingTime = new Label(container, SWT.NONE);
		fLblRecordingTime.setLayoutData(gd);

		// driving time
		label = new Label(container, SWT.NONE);
		label.setText("Driving Time:");
		fLblDrivingTime = new Label(container, SWT.NONE);
		fLblDrivingTime.setLayoutData(gd);

		// title
		label = new Label(container, SWT.NONE);
		label.setText("Title:");
		fLblTitle = new Label(container, SWT.NONE);
		fLblTitle.setLayoutData(gd);

		// start location
		label = new Label(container, SWT.NONE);
		label.setText("Start Location:");
		fLblStartLocation = new Label(container, SWT.NONE);
		fLblStartLocation.setLayoutData(gd);

		// end location
		label = new Label(container, SWT.NONE);
		label.setText("End Location:");
		fLblEndLocation = new Label(container, SWT.NONE);
		fLblEndLocation.setLayoutData(gd);

		return container;
	}

	public TourChart getTourChart() {
		return fTourChart;
	}

	public void refreshTourData(TourData tourData) {

		// tour date
		fLblDate.setText(TourManager.getTourDate(tourData));

		// start time
		fCalendar.set(0, 0, 0, tourData.getStartHour(), tourData.getStartMinute(), 0);
		fLblStartTime.setText(fTimeFormatter.format(fCalendar.getTime()));

		// recording time
		final int recordingTime = tourData.getTourRecordingTime();
		if (recordingTime == 0) {
			fLblRecordingTime.setText("");
		} else {
			fCalendar.set(
					0,
					0,
					0,
					recordingTime / 3600,
					((recordingTime % 3600) / 60),
					((recordingTime % 3600) % 60));

			fLblRecordingTime.setText(fDurationFormatter.format(fCalendar.getTime()));
		}

		// driving time
		final int drivingTime = tourData.getTourDrivingTime();
		if (drivingTime == 0) {
			fLblDrivingTime.setText("");
		} else {
			fCalendar.set(
					0,
					0,
					0,
					drivingTime / 3600,
					((drivingTime % 3600) / 60),
					((drivingTime % 3600) % 60));

			fLblDrivingTime.setText(fDurationFormatter.format(fCalendar.getTime()));
		}

		// tour title
		final String tourTitle = tourData.getTourTitle();
		fLblTitle.setText(tourTitle == null ? "" : tourTitle);

		// start location
		final String startLocation = tourData.getTourStartPlace();
		fLblStartLocation.setText(startLocation == null ? "" : startLocation);

		// end location
		final String endLocation = tourData.getTourEndPlace();
		fLblEndLocation.setText(endLocation == null ? "" : endLocation);

	}

	public void restoreState(IMemento memento) {

	}

	public void saveState(IMemento memento) {

	}

}
