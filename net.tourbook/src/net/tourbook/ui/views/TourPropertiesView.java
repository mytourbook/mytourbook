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
package net.tourbook.ui.views;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.tour.SelectionActiveEditor;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.TourEditor;
import net.tourbook.tour.TourManager;
import net.tourbook.util.PixelConverter;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.part.ViewPart;

// author: Wolfgang Schramm
// create: 24.08.2007

public class TourPropertiesView extends ViewPart {

	public static final String	ID						= "net.tourbook.views.TourPropertiesView";		//$NON-NLS-1$

	private static final String	MEMENTO_SELECTED_TAB	= "tourProperties.selectedTab";

	private static IMemento		fSessionMemento;

	private CTabFolder			fTabFolder;
	private Label				fLblDate;
	private Label				fLblStartTime;
	private Label				fLblRecordingTime;
	private Label				fLblDrivingTime;
	private Label				fLblDatapoints;
	private Text				fTextTitle;
	private Text				fTextStartLocation;
	private Text				fTextEndLocation;
	private Text				fTextDescription;

	private ISelectionListener	fPostSelectionListener;
	private IPartListener2		fPartListener;

	private TourData			fTourData;
	public Calendar				fCalendar				= GregorianCalendar.getInstance();
	private DateFormat			fTimeFormatter			= DateFormat.getTimeInstance(DateFormat.SHORT);
	private DateFormat			fDurationFormatter		= DateFormat.getTimeInstance(DateFormat.SHORT,
																Locale.GERMAN);

	private TourEditor			fTourEditor;

	private void addPartListener() {

		// set the part listener
		fPartListener = new IPartListener2() {
			public void partActivated(IWorkbenchPartReference partRef) {}

			public void partBroughtToTop(IWorkbenchPartReference partRef) {}

			public void partClosed(IWorkbenchPartReference partRef) {
				if (ID.equals(partRef.getId())) {
					// keep settings for this part
					saveSettings();
				}
			}

			public void partDeactivated(IWorkbenchPartReference partRef) {}

			public void partHidden(IWorkbenchPartReference partRef) {}

			public void partInputChanged(IWorkbenchPartReference partRef) {}

			public void partOpened(IWorkbenchPartReference partRef) {}

			public void partVisible(IWorkbenchPartReference partRef) {}
		};
		// register the listener in the page
		getSite().getPage().addPartListener(fPartListener);
	}

	/**
	 * listen for events when a tour is selected
	 */
	private void addSelectionListener() {

		fPostSelectionListener = new ISelectionListener() {
			public void selectionChanged(IWorkbenchPart part, ISelection selection) {
				onChangeSelection(selection);
			}
		};
		getSite().getPage().addPostSelectionListener(fPostSelectionListener);
	}

	private void createControls(Composite parent) {

		fTabFolder = new CTabFolder(parent, SWT.FLAT | SWT.BOTTOM);

		CTabItem fTabItemLocation = new CTabItem(fTabFolder, SWT.FLAT);
		fTabItemLocation.setText("Location");
		fTabItemLocation.setControl(createTabLocation(fTabFolder));

		CTabItem fTabItemTime = new CTabItem(fTabFolder, SWT.FLAT);
		fTabItemTime.setText("Time");
		fTabItemTime.setControl(createTabTime(fTabFolder));

	}

	@Override
	public void createPartControl(Composite parent) {

		createControls(parent);

		addSelectionListener();
		addPartListener();

		restoreState(fSessionMemento);

		// show data from last selection
		onChangeSelection(getSite().getWorkbenchWindow().getSelectionService().getSelection());
	}

	private Composite createTabLocation(Composite parent) {

		Label label;
		GridData gd;
		final PixelConverter pixelConverter = new PixelConverter(parent);

		final ScrolledComposite scrolledContainer = new ScrolledComposite(parent, SWT.V_SCROLL
				| SWT.H_SCROLL);

		scrolledContainer.setExpandVertical(true);
		scrolledContainer.setExpandHorizontal(true);
		final Composite locationContainer;

		locationContainer = new Composite(scrolledContainer, SWT.NONE);
		locationContainer.setLayout(new GridLayout(2, false));
		locationContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		scrolledContainer.setContent(locationContainer);
		scrolledContainer.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				scrolledContainer.setMinSize(locationContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
		});

		gd = new GridData(SWT.FILL, SWT.NONE, true, false);

//		Composite containerLocation = new Composite(fContainer, SWT.NONE);
//		containerLocation.setLayout(new GridLayout(2, false));
//		containerLocation.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		{
			// title
			label = new Label(locationContainer, SWT.NONE);
			label.setText(Messages.Tour_Label_tour_title);
			fTextTitle = new Text(locationContainer, SWT.BORDER);
			fTextTitle.setLayoutData(gd);
			fTextTitle.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					onChangeContent();
				}
			});

			// start location
			label = new Label(locationContainer, SWT.NONE);
			label.setText(Messages.Tour_Label_start_location);
			fTextStartLocation = new Text(locationContainer, SWT.BORDER);
			fTextStartLocation.setLayoutData(gd);
			fTextStartLocation.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					onChangeContent();
				}
			});

			// end location
			label = new Label(locationContainer, SWT.NONE);
			label.setText(Messages.Tour_Label_end_location);
			fTextEndLocation = new Text(locationContainer, SWT.BORDER);
			fTextEndLocation.setLayoutData(gd);
			fTextEndLocation.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					onChangeContent();
				}
			});

			// description
			label = new Label(locationContainer, SWT.NONE);
			label.setText(Messages.Tour_Label_description);
			label.setLayoutData(new GridData(SWT.NONE, SWT.TOP, false, false));
			fTextDescription = new Text(locationContainer, SWT.BORDER
					| SWT.WRAP
					| SWT.MULTI
					| SWT.V_SCROLL
					| SWT.H_SCROLL);
			gd = new GridData(SWT.FILL, SWT.FILL, true, true);
			gd.heightHint = pixelConverter.convertHeightInCharsToPixels(2);
			fTextDescription.setLayoutData(gd);
			fTextDescription.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					onChangeContent();
				}
			});
		}

		return scrolledContainer;
	}

	private Composite createTabTime(Composite parent) {

		Label label;

		final ScrolledComposite scrolledContainer = new ScrolledComposite(parent, SWT.V_SCROLL
				| SWT.H_SCROLL);

		scrolledContainer.setExpandVertical(true);
		scrolledContainer.setExpandHorizontal(true);

		final Composite contentContainer = new Composite(scrolledContainer, SWT.NONE);
		contentContainer.setLayout(new GridLayout(4, true));
		contentContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		scrolledContainer.setContent(contentContainer);
		scrolledContainer.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				scrolledContainer.setMinSize(contentContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
		});

		{
			// tour date
			label = new Label(contentContainer, SWT.NONE);
			label.setText(Messages.Tour_Label_tour_date);
			fLblDate = new Label(contentContainer, SWT.NONE);

			// start time
			label = new Label(contentContainer, SWT.NONE);
			label.setText(Messages.Tour_Label_start_time);
			fLblStartTime = new Label(contentContainer, SWT.NONE);

			// recording time
			label = new Label(contentContainer, SWT.NONE);
			label.setText(Messages.Tour_Label_recording_time);
			fLblRecordingTime = new Label(contentContainer, SWT.NONE);

			// driving time
			label = new Label(contentContainer, SWT.NONE);
			label.setText(Messages.Tour_Label_driving_time);
			fLblDrivingTime = new Label(contentContainer, SWT.NONE);

			// # data points
			label = new Label(contentContainer, SWT.NONE);
			label.setText(Messages.Tour_Label_datapoints);
			fLblDatapoints = new Label(contentContainer, SWT.NONE);
		}

		return scrolledContainer;
	}

	@Override
	public void dispose() {

		final IWorkbenchPage page = getSite().getPage();

		page.removePostSelectionListener(fPostSelectionListener);
		page.removePartListener(fPartListener);

		super.dispose();
	}

	/**
	 * enable controls when the data are from an editor
	 */
	private void enableControls() {

		boolean isEditor = fTourEditor != null;

		fTextTitle.setEnabled(isEditor);
		fTextStartLocation.setEnabled(isEditor);
		fTextEndLocation.setEnabled(isEditor);
		fTextDescription.setEnabled(isEditor);
	}

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);

		// set the session memento if it's not yet set
		if (fSessionMemento == null) {
			fSessionMemento = memento;
		}
	}

	private void onChangeContent() {

		if (fTourEditor == null) {
			return;
		}

		fTourEditor.setTourDirty();

		// set changed data
		fTourData.setTourTitle(fTextTitle.getText());
		fTourData.setTourStartPlace(fTextStartLocation.getText());
		fTourData.setTourEndPlace(fTextEndLocation.getText());
		fTourData.setTourDescription(fTextDescription.getText());

	}

	private void onChangeSelection(ISelection selection) {

		if (selection instanceof SelectionTourData) {

			final TourData selectionTourData = ((SelectionTourData) selection).getTourData();

			// check if this tour chart already shows the tour data
			if (selectionTourData == null || selectionTourData == fTourData) {
				return;
			}

			fTourEditor = null;
			updateTourProperties(selectionTourData);

		} else if (selection instanceof SelectionTourId) {

			SelectionTourId tourIdSelection = (SelectionTourId) selection;

			if (fTourData != null) {
				if (fTourData.getTourId().equals(tourIdSelection.getTourId())) {
					// don't reload the same tour
					return;
				}
			}

			final TourData tourData = TourManager.getInstance()
					.getTourData(tourIdSelection.getTourId());

			if (tourData != null) {
				fTourEditor = null;
				updateTourProperties(tourData);
			}

		} else if (selection instanceof SelectionActiveEditor) {

			final IEditorPart editor = ((SelectionActiveEditor) selection).getEditor();
			if (editor instanceof TourEditor) {
				fTourEditor = (TourEditor) editor;
				updateTourProperties(fTourEditor.getTourChart().getTourData());
			}
		}
	}

	private void restoreState(IMemento memento) {

		if (memento == null) {

			// memento is not set, set defaults

			fTabFolder.setSelection(0);

		} else {

			// restore from memento

			fTabFolder.setSelection(0);
			Integer selectedTab = memento.getInteger(MEMENTO_SELECTED_TAB);
			if (selectedTab != null) {
				fTabFolder.setSelection(selectedTab);
			} else {
				fTabFolder.setSelection(0);
			}
		}
	}

	private void saveSettings() {
		fSessionMemento = XMLMemento.createWriteRoot("TourPropertiesView"); //$NON-NLS-1$
		saveState(fSessionMemento);
	}

	@Override
	public void saveState(IMemento memento) {

		memento.putInteger(MEMENTO_SELECTED_TAB, fTabFolder.getSelectionIndex());

	}

	@Override
	public void setFocus() {

	}

	private void updateTourProperties(TourData tourData) {

		enableControls();

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
			fCalendar.set(0,
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
			fCalendar.set(0,
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

//		fTabItemTourData.getControl().pack(true);
//		fContainer.layout();

		//		fContainer.pack(true);
	}

}
