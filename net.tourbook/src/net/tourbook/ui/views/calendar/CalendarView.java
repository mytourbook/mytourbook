package net.tourbook.ui.views.calendar;

import java.util.ArrayList;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Activator;
import net.tourbook.chart.Messages;
import net.tourbook.data.TourData;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.views.calendar.CalendarGraph.Type;
import net.tourbook.util.SelectionProvider;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

public class CalendarView extends ViewPart implements ITourProvider{

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String	ID	= "net.tourbook.views.calendar.CalendarView";
	private final IPreferenceStore	_prefStore	= TourbookPlugin.getDefault().getPreferenceStore();
	private final IDialogSettings	_state		= TourbookPlugin.getDefault().getDialogSettingsSection(
														"TourCalendarView");						//$NON-NLS-1$
	private Action					_back;
	private Action					_forward;
	private Action					_zoomOut;
	private Action					_zoomIn;
	private Action					_synced;
	private Action					_today;

	private PageBook				_pageBook;
	private CalendarForm			_calendarForm;
	private CalendarGraph						_calendarGraph;

	private ISelectionProvider					_selectionProvider;
	private ISelectionListener					_selectionListener;
	private IPartListener2			_partListener;
	private IPropertyChangeListener	_prefChangeListener;
	private ITourEventListener		_tourEventListener;
	private ITourEventListener		_tourPropertyListener;

	private CalendarYearMonthContributionItem	_cymci;

	public CalendarView() {}

	private void addPartListener() {

		_partListener = new IPartListener2() {
			@Override
			public void partActivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(final IWorkbenchPartReference partRef) {

//				if (partRef.getPart(false) == YearStatisticView.this) {
//					saveState();
//				}
				// TODO
			}

			@Override
			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partHidden(final IWorkbenchPartReference partRef) {}

			@Override
			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			@Override
			public void partOpened(final IWorkbenchPartReference partRef) {}

			@Override
			public void partVisible(final IWorkbenchPartReference partRef) {}
		};
		getViewSite().getPage().addPartListener(_partListener);
	}

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {

			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				/*
				 * set a new chart configuration when the preferences has changed
				 */

				if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)) {

					refreshCalendar();

				} else if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

					// update statistics
					refreshCalendar();

				}
			}

		};

		// add pref listener
		_prefStore.addPropertyChangeListener(_prefChangeListener);

	}

	// create and register our selection listener
	private void addSelectionListener() {

		_selectionListener = new ISelectionListener() {

			@Override
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

				// prevent to listen to a selection which is originated by this year chart
				if (part == CalendarView.this) {
					return;
				}

				onSelectionChanged(selection);

			}
		};

		// register selection listener in the page
		getSite().getPage().addPostSelectionListener(_selectionListener);
	}

	// create and register our selection provider
	private void addSelectionProvider() {

		getSite().setSelectionProvider(_selectionProvider = new SelectionProvider());

		_calendarGraph.addSelectionProvider(new ICalendarSelectionProvider() {

			@Override
			public void selectionChanged(final Type type, final long id) {
				if (type == Type.TOUR) {
					_selectionProvider.setSelection(new SelectionTourId(id));
				}
			}

		});
	}

	private void addTourEventListener() {

		_tourPropertyListener = new ITourEventListener() {
			@Override
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if (eventId == TourEventId.TOUR_CHANGED || eventId == TourEventId.UPDATE_UI) {
					/*
					 * it is possible when a tour type was modified, the tour can be hidden or
					 * visible in the viewer because of the tour type filter
					 */
					refreshCalendar();

				} else if (eventId == TourEventId.TAG_STRUCTURE_CHANGED
						|| eventId == TourEventId.ALL_TOURS_ARE_MODIFIED) {

					refreshCalendar();
				}
			}
		};
		TourManager.getInstance().addTourEventListener(_tourPropertyListener);
	}

	private void contributeToActionBars() {
		final IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	@Override
	public void createPartControl(final Composite parent) {
		
		addPartListener();
		addPrefListener();
		addTourEventListener();

		createUI(parent);

		makeActions();
		contributeToActionBars();

		addSelectionListener();
		addSelectionProvider();

		// restore selection
		onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());

		final Menu _contextMenu = TourContextMenu.getInstance().createContextMenu(this, _calendarGraph);

		_calendarGraph.setMenu(_contextMenu);

	}

	private void createUI(final Composite parent) {
		
		_pageBook = new PageBook(parent, SWT.NONE);
		_calendarForm = new CalendarForm(_pageBook, SWT.NORMAL);
		_calendarGraph = _calendarForm.getComponents().getGraph();
		_pageBook.showPage(_calendarForm);
	}
	
	@Override
	public void dispose() {

		TourManager.getInstance().removeTourEventListener(_tourPropertyListener);
		getSite().getPage().removePostSelectionListener(_selectionListener);
		_prefStore.removePropertyChangeListener(_prefChangeListener);

		super.dispose();
	}

	private void fillLocalPullDown(final IMenuManager manager) {
		manager.add(_back);
		manager.add(new Separator());
		manager.add(_forward);
	}
	private void fillLocalToolBar(final IToolBarManager manager) {
		_cymci = new CalendarYearMonthContributionItem(_calendarGraph);
		_calendarGraph.setYearMonthContributor(_cymci);
		manager.add(_cymci);
		manager.add(new Separator());
		manager.add(_back);
		manager.add(_forward);
		manager.add(_today);
		manager.add(new Separator());
		manager.add(_zoomIn);
		manager.add(_zoomOut);
		manager.add(new Separator());
		manager.add(_synced);
	}

	@Override
	public ArrayList<TourData> getSelectedTours() {
		
		final ArrayList<TourData> selectedTourData = new ArrayList<TourData>();
		final ArrayList<Long> tourIdSet = new ArrayList<Long>();
		tourIdSet.add(_calendarGraph.getSelectionTourId());
		for (final Long tourId : tourIdSet) {
			if (tourId > 0) { // < 0 means not selected
				selectedTourData.add(TourManager.getInstance().getTourData(tourId));
			}
		}
		return selectedTourData;
	}

	private void makeActions() {
		_back = new Action() {
			@Override
			public void run() {
				_calendarGraph.gotoPrevScreen();
			}
		};
		_back.setId("net.tourbook.calendar.back");
		_back.setText("Back");
		_back.setToolTipText("Back one screen");
		_back.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_BACK));
		
		_forward = new Action() {
			@Override
			public void run() {
				_calendarGraph.gotoNextScreen();
			}
		};
		_forward.setText("Forward");
		_forward.setToolTipText("Forward one screen");
		_forward.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_FORWARD));

		_zoomOut = new Action() {
			@Override
			public void run() {
				_calendarGraph.zoomOut();
			}
		};
		_zoomOut.setText("Zoom out");
		_zoomOut.setToolTipText("Show more weeks");
		_zoomOut.setImageDescriptor(Activator.getImageDescriptor(Messages.Image_zoom_out));
		// TODO: understand what I'm doing

		_zoomIn = new Action() {
			@Override
			public void run() {
				_calendarGraph.zoomIn();
			}
		};
		_zoomIn.setText("Zoom in");
		_zoomIn.setToolTipText("Show less weeks");
		_zoomIn.setImageDescriptor(Activator.getImageDescriptor(Messages.Image_zoom_in));
		// TODO: understand what I'm doing
		
		_synced = new Action(null, org.eclipse.jface.action.Action.AS_CHECK_BOX) {
			@Override
			public void run() {
				_calendarGraph.setSynced(_synced.isChecked());
			}};
		_synced.setText("Link with other views");
		_synced.setImageDescriptor(Activator.imageDescriptorFromPlugin("net.tourbook", "icons/synced.gif"));
		_synced.setChecked(true);
		
		_today = new Action() {
			@Override
			public void run() {
				_calendarGraph.gotoToday();
			}};
		_today.setText("Go to today");
		_today.setImageDescriptor(Activator.imageDescriptorFromPlugin("net.tourbook", "icons/zoom-centered.png"));
	}

	private void onSelectionChanged(final ISelection selection) {
		
		if (!_synced.isChecked()) {
			return;
		}

		// show and select the selected tour
		if (selection instanceof SelectionTourId) {
			_calendarGraph.gotoTourId(((SelectionTourId) selection).getTourId());
		}
		
	}
	
	private void refreshCalendar() {
		_calendarGraph.refreshCalendar();
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	@Override
	public void setFocus() {
		_calendarForm.setFocus();
	}

	private void showMessage(final String message) {
		MessageDialog.openInformation(_pageBook.getShell(), "%view_name_Calendar", message);
	}

}