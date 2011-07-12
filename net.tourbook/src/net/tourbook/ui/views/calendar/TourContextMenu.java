package net.tourbook.ui.views.calendar;

import java.util.ArrayList;

import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.extension.export.ActionExport;
import net.tourbook.printing.ActionPrint;
import net.tourbook.tag.TagMenuManager;
import net.tourbook.tour.ActionOpenAdjustAltitudeDialog;
import net.tourbook.tour.ActionOpenMarkerDialog;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TourTypeMenuManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.action.ActionComputeDistanceValuesFromGeoposition;
import net.tourbook.ui.action.ActionEditQuick;
import net.tourbook.ui.action.ActionEditTour;
import net.tourbook.ui.action.ActionJoinTours;
import net.tourbook.ui.action.ActionOpenTour;
import net.tourbook.ui.action.ActionSetAltitudeValuesFromSRTM;
import net.tourbook.ui.action.ActionSetPerson;
import net.tourbook.ui.action.ActionSetTourTypeMenu;
import net.tourbook.ui.views.rawData.ActionMergeTour;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;

public class TourContextMenu {

	private static TourContextMenu						_instance;
	private TagMenuManager								_tagMenuMgr;

	private ActionEditQuick								_actionEditQuick;

	// private ActionDeleteTourMenu						_actionDeleteTour;

	private ActionEditTour								_actionEditTour;
	private ActionOpenTour								_actionOpenTour;
	private ActionOpenMarkerDialog						_actionOpenMarkerDialog;
	private ActionOpenAdjustAltitudeDialog				_actionOpenAdjustAltitudeDialog;
	private ActionMergeTour								_actionMergeTour;
	private ActionJoinTours								_actionJoinTours;
	private ActionComputeDistanceValuesFromGeoposition	_actionComputeDistanceValuesFromGeoposition;
	private ActionSetAltitudeValuesFromSRTM				_actionSetAltitudeFromSRTM;
	private ActionSetTourTypeMenu						_actionSetTourType;

	private ActionSetPerson								_actionSetOtherPerson;

	private ActionExport								_actionExportTour;
	private ActionPrint									_actionPrintTour;

	private TourContextMenu() {}

	public static TourContextMenu getInstance() {
		if (_instance == null) {
			_instance = new TourContextMenu();
		}
		return _instance;
	}

	private void createActions(final ITourProvider tourProvider) {

		_actionEditQuick = new ActionEditQuick(tourProvider);
		_actionEditTour = new ActionEditTour(tourProvider);
		_actionOpenTour = new ActionOpenTour(tourProvider);
		// _actionDeleteTour = new ActionDeleteTourMenu(tourProvider);

		_actionOpenMarkerDialog = new ActionOpenMarkerDialog(tourProvider, true);
		_actionOpenAdjustAltitudeDialog = new ActionOpenAdjustAltitudeDialog(tourProvider);
		_actionMergeTour = new ActionMergeTour(tourProvider);
		_actionJoinTours = new ActionJoinTours(tourProvider);
		_actionComputeDistanceValuesFromGeoposition = new ActionComputeDistanceValuesFromGeoposition(tourProvider);
		_actionSetAltitudeFromSRTM = new ActionSetAltitudeValuesFromSRTM(tourProvider);
		_actionSetOtherPerson = new ActionSetPerson(tourProvider);

		_actionSetTourType = new ActionSetTourTypeMenu(tourProvider);

		_actionExportTour = new ActionExport(tourProvider);
		_actionPrintTour = new ActionPrint(tourProvider);

		_tagMenuMgr = new TagMenuManager(tourProvider, true);

	}

	public Menu createContextMenu(final ITourProvider tourProvider, final Control control) {

		createActions(tourProvider);

		// final MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		final MenuManager menuMgr = new MenuManager();
		final TagMenuManager tagMenuMgr = new TagMenuManager(tourProvider, true);

		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(final IMenuManager manager) {
				fillContextMenu(manager, tourProvider);
			}
		});

		final Menu contextMenu = menuMgr.createContextMenu(control);

		contextMenu.addMenuListener(new MenuAdapter() {
			@Override
			public void menuHidden(final MenuEvent e) {
				tagMenuMgr.onHideMenu();
			}

			@Override
			public void menuShown(final MenuEvent menuEvent) {
				// tagMenuMgr.onShowMenu(menuEvent, _control, Display.getCurrent().getCursorLocation(), _tourInfoToolTip);
				tagMenuMgr.onShowMenu(menuEvent, control, Display.getCurrent().getCursorLocation(), null);
			}
		});

		menuMgr.add(_actionEditQuick);
		menuMgr.add(_actionEditTour);
		menuMgr.add(_actionOpenMarkerDialog);
		menuMgr.add(_actionOpenAdjustAltitudeDialog);
		menuMgr.add(_actionOpenTour);
		menuMgr.add(_actionMergeTour);
		// menuMgr.add(_actionJoinTours); // until now we only allow single tour selection
		menuMgr.add(_actionComputeDistanceValuesFromGeoposition);
		menuMgr.add(_actionSetAltitudeFromSRTM);

		tagMenuMgr.fillTagMenu(menuMgr);

		// tour type actions
		menuMgr.add(new Separator());
		menuMgr.add(_actionSetTourType);
		TourTypeMenuManager.fillMenuWithRecentTourTypes(menuMgr, tourProvider, true);

		menuMgr.add(new Separator());
		menuMgr.add(_actionExportTour);
		menuMgr.add(_actionPrintTour);

		menuMgr.add(new Separator());
		menuMgr.add(_actionSetOtherPerson);

		return contextMenu;

	}

	private void enableActions(final ITourProvider tourProvider) {

		/*
		 * count number of selected items
		 */
		final ArrayList<TourData> selectedTours = tourProvider.getSelectedTours();

		final int tourItems = selectedTours.size();
		final boolean isTourSelected = tourItems > 0;
		final boolean isOneTour = tourItems == 1;
		boolean isDeviceTour = false;

		TourData firstSavedTour = null;

		if (isOneTour) {
			firstSavedTour = TourManager.getInstance().getTourData(selectedTours.get(0).getTourId());
			isDeviceTour = firstSavedTour.isManualTour() == false;
		}

		/*
		 * enable actions
		 */
		// _tourDoubleClickState.canEditTour = isOneTour;
		// _tourDoubleClickState.canOpenTour = isOneTour;
		// _tourDoubleClickState.canQuickEditTour = isOneTour;
		// _tourDoubleClickState.canEditMarker = isOneTour;
		// _tourDoubleClickState.canAdjustAltitude = isOneTour;

		_actionEditTour.setEnabled(isOneTour);
		_actionOpenTour.setEnabled(isOneTour);
		_actionEditQuick.setEnabled(isOneTour);
		_actionOpenMarkerDialog.setEnabled(isOneTour && isDeviceTour);
		_actionOpenAdjustAltitudeDialog.setEnabled(isOneTour && isDeviceTour);

		_actionMergeTour.setEnabled(isOneTour
				&& isDeviceTour
				&& firstSavedTour != null
				&& firstSavedTour.getMergeSourceTourId() != null);
		_actionComputeDistanceValuesFromGeoposition.setEnabled(isTourSelected);
		_actionSetAltitudeFromSRTM.setEnabled(isTourSelected);

		// enable delete ation when at least one tour is selected
//		if (isTourSelected) {
//			_actionDeleteTour.setEnabled(true);
//		} else {
//			_actionDeleteTour.setEnabled(false);
//		}

		_actionJoinTours.setEnabled(tourItems > 1);
		_actionSetOtherPerson.setEnabled(isTourSelected);

		_actionExportTour.setEnabled(isTourSelected);
		_actionPrintTour.setEnabled(isTourSelected);

		final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();
		_actionSetTourType.setEnabled(isTourSelected && tourTypes.size() > 0);

		if (null != firstSavedTour) {
			final ArrayList<Long> tagIds = new ArrayList<Long>();
			for (final TourTag tag : firstSavedTour.getTourTags()) {
				tagIds.add(tag.getTagId());
			}
			_tagMenuMgr.enableTagActions(isTourSelected, isOneTour, tagIds);
			TourTypeMenuManager.enableRecentTourTypeActions(isTourSelected, isOneTour ? firstSavedTour
					.getTourType()
					.getTypeId() : TourDatabase.ENTITY_IS_NOT_SAVED);
		} else {
			_tagMenuMgr.enableTagActions(isTourSelected, isOneTour, new ArrayList<Long>());
			TourTypeMenuManager.enableRecentTourTypeActions(isTourSelected, isOneTour
					? new Long(-1)
					: TourDatabase.ENTITY_IS_NOT_SAVED);
		}
	}

	private void fillContextMenu(final IMenuManager menuMgr, final ITourProvider tourProvider) {

		menuMgr.add(_actionEditQuick);
		menuMgr.add(_actionEditTour);
		menuMgr.add(_actionOpenMarkerDialog);
		menuMgr.add(_actionOpenAdjustAltitudeDialog);
		menuMgr.add(_actionOpenTour);
		menuMgr.add(_actionMergeTour);
		menuMgr.add(_actionJoinTours);
		menuMgr.add(_actionComputeDistanceValuesFromGeoposition);
		menuMgr.add(_actionSetAltitudeFromSRTM);

		_tagMenuMgr.fillTagMenu(menuMgr);

		// tour type actions
		menuMgr.add(new Separator());
		menuMgr.add(_actionSetTourType);
		TourTypeMenuManager.fillMenuWithRecentTourTypes(menuMgr, tourProvider, true);

		menuMgr.add(new Separator());
		menuMgr.add(_actionExportTour);
		menuMgr.add(_actionPrintTour);

		menuMgr.add(new Separator());
		menuMgr.add(_actionSetOtherPerson);
//		menuMgr.add(_actionDeleteTour);

		enableActions(tourProvider);
	}

}
