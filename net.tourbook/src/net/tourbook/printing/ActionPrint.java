package net.tourbook.printing;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.printing.PrintTourExtension;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.ITourProviderAll;
import net.tourbook.ui.UI;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

public class ActionPrint extends Action implements IMenuCreator {

	private static ArrayList<PrintTourExtension>	fPrintExtensionPoints;
 
	private Menu									fMenu;
	private ArrayList<ActionPrintTour>				fPrintTourActions;

	private ITourProvider							fTourProvider;

	private int										fTourStartIndex	= -1;
	private int										fTourEndIndex	= -1;

	private class ActionPrintTour extends Action {

		private PrintTourExtension	fPrintTourExtension;

		public ActionPrintTour(final PrintTourExtension printTourExtension) {

			super(printTourExtension.getVisibleName());
			fPrintTourExtension = printTourExtension;
		}

		ActionPrintTour(final String visibleName, final String fileExtension) {}

		@Override
		public void run() {
			final ArrayList<TourData> selectedTours;
			if (fTourProvider instanceof ITourProviderAll) {
				selectedTours = ((ITourProviderAll) fTourProvider).getAllSelectedTours();
			} else {
				selectedTours = fTourProvider.getSelectedTours();
			}

			if (selectedTours == null || selectedTours.size() == 0) {
				return;
			}

			fPrintTourExtension.printTours(selectedTours, fTourStartIndex, fTourEndIndex);
		}

	}

	/**
	 * @param tourProvider
	 * @param isAddMode
	 * @param isSaveTour
	 *            when <code>true</code> the tour will be saved and a
	 *            {@link TourManager#TOUR_CHANGED} event is fired, otherwise the {@link TourData}
	 *            from the tour provider is only updated
	 */
	public ActionPrint(final ITourProvider tourProvider) {

		super(UI.IS_NOT_INITIALIZED, AS_DROP_DOWN_MENU);

		fTourProvider = tourProvider;

		setText(Messages.action_print_tour);
		setMenuCreator(this);

		getExtensionPoints();
		createActions();
	}

	private void addActionToMenu(final Action action) {
		final ActionContributionItem item = new ActionContributionItem(action);
		item.fill(fMenu, -1);
	}

	private void createActions() {

		if (fPrintTourActions != null) {
			return;
		}

		fPrintTourActions = new ArrayList<ActionPrintTour>();

		// create action for each extension point
		for (final PrintTourExtension printTourExtension : fPrintExtensionPoints) {
			fPrintTourActions.add(new ActionPrintTour(printTourExtension));
		}
	}

	public void dispose() {
		if (fMenu != null) {
			fMenu.dispose();
			fMenu = null;
		}
	}

	/**
	 * read extension points {@link TourbookPlugin#EXT_POINT_PRINT_TOUR}
	 */
	private ArrayList<PrintTourExtension> getExtensionPoints() {

		if (fPrintExtensionPoints != null) {
			return fPrintExtensionPoints;
		}

		fPrintExtensionPoints = new ArrayList<PrintTourExtension>();

		final IExtensionPoint extPoint = Platform.getExtensionRegistry().getExtensionPoint(TourbookPlugin.PLUGIN_ID,
				TourbookPlugin.EXT_POINT_PRINT_TOUR);

		if (extPoint != null) {

			for (final IExtension extension : extPoint.getExtensions()) {
				for (final IConfigurationElement configElement : extension.getConfigurationElements()) {

					if (configElement.getName().equalsIgnoreCase("print")) { //$NON-NLS-1$
						try {
							final Object object = configElement.createExecutableExtension("class"); //$NON-NLS-1$
							if (object instanceof PrintTourExtension) {

								final PrintTourExtension printTourItem = (PrintTourExtension) object;

								printTourItem.setPrintId(configElement.getAttribute("id")); //$NON-NLS-1$
								printTourItem.setVisibleName(configElement.getAttribute("name")); //$NON-NLS-1$

								fPrintExtensionPoints.add(printTourItem);
							}
						} catch (final CoreException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}

		return fPrintExtensionPoints;
	}

	public Menu getMenu(final Control parent) {
		return null;
	}

	public Menu getMenu(final Menu parent) {

		dispose();
		fMenu = new Menu(parent);

		for (final ActionPrintTour action : fPrintTourActions) {
			addActionToMenu(action);
		}

		return fMenu;
	}

	public void setTourRange(final int tourStartIndex, final int tourEndIndex) {
		fTourStartIndex = tourStartIndex;
		fTourEndIndex = tourEndIndex;
	}

}
