package net.tourbook.ui.views.calendar;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.util.PostSelectionProvider;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

public class CalendarView extends ViewPart {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String	ID	= "net.tourbook.views.calendar.CalendarView";
	private final IDialogSettings	_state		= TourbookPlugin.getDefault().getDialogSettingsSection(
														"TourCalendarView");						//$NON-NLS-1$
	private Action					_back;
	private Action					_forward;

	private PageBook				_pageBook;
	private CalendarForm			_calendarForm;

	private PostSelectionProvider	_postSelectionProvider;
	private IPartListener2			_partListener;
	private IPropertyChangeListener	_prefChangeListener;
	private ITourEventListener		_tourEventListener;
	private ISelectionListener		_postSelectionListener;

	public CalendarView() {}

	private void contributeToActionBars() {
		final IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	@Override
	public void createPartControl(final Composite parent) {
		
		// this view is a selection provider, set it before the container is created
		getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider());

//		addPartListener();
//		addPrefListener();
//		addSelectionListener();
//		addTourEventListener();

		// _statContainer.restoreStatistics(_state, _activePerson, _activeTourTypeFilter);
		createUI(parent);
		
		// final TourTimeData _tourTypeData = DataProviderTourTime.getInstance.getTourTimeData();

		// Create the help context id for the viewer's control
		makeActions();
		contributeToActionBars();
	}
	
	private void createUI(final Composite parent) {
		
		_pageBook = new PageBook(parent, SWT.NONE);

		_calendarForm = new CalendarForm(_pageBook, SWT.NORMAL);

		_pageBook.showPage(_calendarForm);

	}

	private void fillLocalPullDown(final IMenuManager manager) {
		manager.add(_back);
		manager.add(new Separator());
		manager.add(_forward);
	}

	private void fillLocalToolBar(final IToolBarManager manager) {
		manager.add(_back);
		manager.add(_forward);
	}
	
	private void makeActions() {
		_back = new Action() {
			@Override
			public void run() {
				_calendarForm.back();
			}
		};
		_back.setText("Back");
		_back.setToolTipText("Back one month");
		_back.setImageDescriptor(PlatformUI
				.getWorkbench()
				.getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_TOOL_BACK));
		
		_forward = new Action() {
			@Override
			public void run() {
				_calendarForm.forward();
			}
		};
		_forward.setText("Forward");
		_forward.setToolTipText("Forward one Month");
		_forward.setImageDescriptor(PlatformUI
				.getWorkbench()
				.getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_TOOL_FORWARD));
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