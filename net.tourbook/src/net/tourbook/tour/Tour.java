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
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import net.tourbook.Messages;
import net.tourbook.data.TourData;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IMemento;

public class Tour extends Composite {

	private static final String	MEMENTO_SELECTED_TAB	= "tour.selected-tab"; //$NON-NLS-1$

	private CTabFolder			fTabFolder;
	private CTabItem			fChartItem;
	private CTabItem			fTourDataItem;

	private TourChart			fTourChart;
	private TourData			fTourData;

	public Calendar				fCalendar				= GregorianCalendar.getInstance();
//	private DateFormat		fDateFormatter		= DateFormat.getDateInstance(DateFormat.SHORT);
	private DateFormat			fTimeFormatter			= DateFormat
																.getTimeInstance(DateFormat.SHORT);
	private DateFormat			fDurationFormatter		= DateFormat.getTimeInstance(
																DateFormat.SHORT,
																Locale.GERMAN);
//	private NumberFormat	fNumberFormatter	= NumberFormat.getNumberInstance();

	private Label				fLblDate;
	private Label				fLblStartTime;
	private Label				fLblRecordingTime;
	private Label				fLblDrivingTime;
	private Label				fLblDatapoints;
	private Text				fTextTitle;
	private Text				fTextStartLocation;
	private Text				fTextEndLocation;
	private Text				fTextDescription;

	private ToolBarManager		fTbm;
	private ActionUndoTyping	fActionUndoTyping;

	private TourDataBackup		fTDBackup;

//	protected ListenerList	fPropertyListeners	= new ListenerList();
//
//	/**
//	 * Listener for tour changes
//	 */
//	public interface ITourChangeListener extends EventListener {
//		public void tourChanged(TourChangeEvent event);
//	}
//
//	public static class TourChangeEvent extends EventObject {
//
//		private static final long	serialVersionUID	= 1L;
//
//		protected TourChangeEvent(Tour source) {
//			super(source);
//		}
//	}
//
//	public void addTourChangedListener(ITourChangeListener listener) {
//		fPropertyListeners.add(listener);
//	}
//
//	private void fireTourChanged() {
//		Object[] listeners = fPropertyListeners.getListeners();
//		for (int i = 0; i < listeners.length; ++i) {
//			final ITourChangeListener listener = (ITourChangeListener) listeners[i];
//			SafeRunnable.run(new SafeRunnable() {
//				public void run() {
//					listener.tourChanged(new TourChangeEvent(Tour.this));
//				}
//			});
//		}
//	}

	private class TourDataBackup {
		String	Title;
		String	StartLocation;
		String	EndLocation;
		String	Description;
	}

	public Tour(Composite parent, int style) {

		super(parent, style);

		createControls();
		createActions();

		fTDBackup = new TourDataBackup();
	}

	private void createActions() {

		fActionUndoTyping = new ActionUndoTyping(this);

		fTbm.add(fActionUndoTyping);

		fTbm.update(true);
	}

	private void createControls() {

		setLayout(new FillLayout());

		fTabFolder = new CTabFolder(this, SWT.FLAT | SWT.BOTTOM);

		fTourChart = new TourChart(fTabFolder, SWT.FLAT, true);
//		fTourChart.addBeforeSaveListener(new ITourChartSaveListener() {
//			public void beforeSave() {
//				updateTourData();
//			}
//		});

		fChartItem = new CTabItem(fTabFolder, SWT.FLAT);
		fChartItem.setText(Messages.Tour_Label_tab_tour_chart);
		fChartItem.setControl(fTourChart);

		fTourDataItem = new CTabItem(fTabFolder, SWT.FLAT);
		fTourDataItem.setText(Messages.Tour_Label_tab_tour_data);
		fTourDataItem.setControl(createTourDataControls(fTabFolder));
	}

//	private void updateTourData() {
//		fTourData.setTourTitle(fTextTitle.getText());
//		fTourData.setTourStartPlace(fTextStartLocation.getText());
//		fTourData.setTourEndPlace(fTextEndLocation.getText());
//		fTourData.setTourDescription(fTextDescription.getText());
//	}

	private Control createTourDataControls(Composite parent) {

		// device data / tour viewer
		ViewForm tourForm = new ViewForm(parent, SWT.NONE);

		// create the  toolbar
		final ToolBar toolBar = new ToolBar(tourForm, SWT.FLAT | SWT.WRAP);
		fTbm = new ToolBarManager(toolBar);

		Composite detailContainer = createTourDetail(tourForm);

		tourForm.setTopLeft(toolBar);
		tourForm.setContent(detailContainer);

		return tourForm;
	}

	private Composite createTourDetail(Composite parent) {

		Composite detailContainer = new Composite(parent, SWT.NONE);
		detailContainer.setLayout(new GridLayout(2, false));

		Label label;
		final GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);

		// tour date
		label = new Label(detailContainer, SWT.NONE);
		label.setText(Messages.Tour_Label_tour_date);
		fLblDate = new Label(detailContainer, SWT.NONE);

		// start time
		label = new Label(detailContainer, SWT.NONE);
		label.setText(Messages.Tour_Label_start_time);
		fLblStartTime = new Label(detailContainer, SWT.NONE);

		// recording time
		label = new Label(detailContainer, SWT.NONE);
		label.setText(Messages.Tour_Label_recording_time);
		fLblRecordingTime = new Label(detailContainer, SWT.NONE);

		// driving time
		label = new Label(detailContainer, SWT.NONE);
		label.setText(Messages.Tour_Label_driving_time);
		fLblDrivingTime = new Label(detailContainer, SWT.NONE);

		// # data points
		label = new Label(detailContainer, SWT.NONE);
		label.setText(Messages.Tour_Label_datapoints);
		fLblDatapoints = new Label(detailContainer, SWT.NONE);

		// title
		label = new Label(detailContainer, SWT.NONE);
		label.setText(Messages.Tour_Label_tour_title);
		fTextTitle = new Text(detailContainer, SWT.BORDER);
		fTextTitle.setLayoutData(gd);
		fTextTitle.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				onChangeContent();
			}
		});

		// start location
		label = new Label(detailContainer, SWT.NONE);
		label.setText(Messages.Tour_Label_start_location);
		fTextStartLocation = new Text(detailContainer, SWT.BORDER);
		fTextStartLocation.setLayoutData(gd);
		fTextStartLocation.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				onChangeContent();
			}
		});

		// end location
		label = new Label(detailContainer, SWT.NONE);
		label.setText(Messages.Tour_Label_end_location);
		fTextEndLocation = new Text(detailContainer, SWT.BORDER);
		fTextEndLocation.setLayoutData(gd);
		fTextEndLocation.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				onChangeContent();
			}
		});

		// description
		label = new Label(detailContainer, SWT.NONE);
		label.setText(Messages.Tour_Label_description);
		label.setLayoutData(new GridData(SWT.NONE, SWT.TOP, false, false));
		fTextDescription = new Text(detailContainer, SWT.BORDER
				| SWT.WRAP
				| SWT.MULTI
				| SWT.V_SCROLL
				| SWT.H_SCROLL);
		fTextDescription.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		fTextDescription.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				onChangeContent();
			}
		});

		return detailContainer;
	}

	private void onChangeContent() {

		boolean isTourDirty = false;

		if (fTextTitle.getText().compareTo(fTDBackup.Title) != 0) {
			isTourDirty = true;
		} else if (fTextStartLocation.getText().compareTo(fTDBackup.StartLocation) != 0) {
			isTourDirty = true;
		} else if (fTextEndLocation.getText().compareTo(fTDBackup.EndLocation) != 0) {
			isTourDirty = true;
		} else if (fTextDescription.getText().compareTo(fTDBackup.Description) != 0) {
			isTourDirty = true;
		}

		if (isTourDirty) {
			fTourChart.setTourDirty(true);
		}
		fActionUndoTyping.setEnabled(isTourDirty);
	}

	public TourChart getTourChart() {
		return fTourChart;
	}

	public void refreshTourData(TourData tourData) {

		// keep reference
		fTourData = tourData;

		// tour date
		fLblDate.setText(TourManager.getTourDate(tourData));
		fLblDate.pack(true);

		// start time
		fCalendar.set(0, 0, 0, tourData.getStartHour(), tourData.getStartMinute(), 0);
		fLblStartTime.setText(fTimeFormatter.format(fCalendar.getTime()));
		fLblStartTime.pack(true);

		// recording time
		final int recordingTime = tourData.getTourRecordingTime();
		if (recordingTime == 0) {
			fLblRecordingTime.setText(""); //$NON-NLS-1$
		} else {
			fCalendar.set(
					0,
					0,
					0,
					recordingTime / 3600,
					((recordingTime % 3600) / 60),
					((recordingTime % 3600) % 60));

			fLblRecordingTime.setText(fDurationFormatter.format(fCalendar.getTime()));
			fLblRecordingTime.pack(true);
		}

		// driving time
		final int drivingTime = tourData.getTourDrivingTime();
		if (drivingTime == 0) {
			fLblDrivingTime.setText(""); //$NON-NLS-1$
		} else {
			fCalendar.set(
					0,
					0,
					0,
					drivingTime / 3600,
					((drivingTime % 3600) / 60),
					((drivingTime % 3600) % 60));

			fLblDrivingTime.setText(fDurationFormatter.format(fCalendar.getTime()));
			fLblDrivingTime.pack(true);
		}

		// data points
		final int dataPoints = tourData.getSerieData().timeSerie.length;
		fLblDatapoints.setText(Integer.toString(dataPoints));
		fLblDatapoints.pack(true);
		
		setFields();

		fActionUndoTyping.setEnabled(false);
	}

	private void setFields() {

		// tour title
		final String tourTitle = fTourData.getTourTitle();
		fTextTitle.setText(tourTitle);

		// start location
		final String startLocation = fTourData.getTourStartPlace();
		fTextStartLocation.setText(startLocation);

		// end location
		final String endLocation = fTourData.getTourEndPlace();
		fTextEndLocation.setText(endLocation);

		// description
		final String description = fTourData.getTourDescription();
		fTextDescription.setText(description);

		fTDBackup.Title = new String(tourTitle);
		fTDBackup.StartLocation = new String(startLocation);
		fTDBackup.EndLocation = new String(endLocation);
		fTDBackup.Description = new String(description);

	}

	public void restoreState(IMemento memento) {

		if (memento != null) {

			// select tab
			Integer selectedTab = memento.getInteger(MEMENTO_SELECTED_TAB);
			if (selectedTab != null) {
				fTabFolder.setSelection(selectedTab);
			} else {
				fTabFolder.setSelection(0);
			}

		} else {
			fTabFolder.setSelection(0);
		}
	}

	public void saveState(IMemento memento) {
		memento.putInteger(MEMENTO_SELECTED_TAB, fTabFolder.getSelectionIndex());
	}

	/**
	 * set field content to the original values
	 */
	void undoTyping() {
		setFields();
		fActionUndoTyping.setEnabled(false);
	}

}
