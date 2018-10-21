/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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
package net.tourbook.extension.export;

import java.util.ArrayList;
import java.util.Collections;

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

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourData;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.ITourProviderAll;
import net.tourbook.ui.UI;

/**
 * Submenu for exporting tours
 */
public class ActionExport extends Action implements IMenuCreator {

	private static ArrayList<ExportTourExtension>	_exportExtensionPoints;

	private Menu												_menu;
	private ArrayList<ActionExportTour>					_exportTourActions;

	private final ITourProvider							_tourProvider;

	private int													_tourStartIndex	= -1;
	private int													_tourEndIndex		= -1;

	private class ActionExportTour extends Action {

		private final ExportTourExtension _exportTourExtension;

		public ActionExportTour(final ExportTourExtension exportTourExtension) {

			super(exportTourExtension.getVisibleName());

			_exportTourExtension = exportTourExtension;
		}

		@Override
		public void run() {

			final ArrayList<TourData> selectedTours;

			if (_tourProvider instanceof ITourProviderAll) {
				selectedTours = ((ITourProviderAll) _tourProvider).getAllSelectedTours();
			} else {
				selectedTours = _tourProvider.getSelectedTours();
			}

			if (selectedTours == null || selectedTours.size() == 0) {
				return;
			}

			// sort by date/time
			Collections.sort(selectedTours);

			_exportTourExtension.exportTours(selectedTours, _tourStartIndex, _tourEndIndex);
		}

	}

	/**
	 * @param tourProvider
	 * @param isAddMode
	 * @param isSaveTour
	 *           when <code>true</code> the tour will be saved and a {@link TourManager#TOUR_CHANGED}
	 *           event is fired, otherwise the {@link TourData} from the tour provider is only
	 *           updated
	 */
	public ActionExport(final ITourProvider tourProvider) {

		super(UI.IS_NOT_INITIALIZED, AS_DROP_DOWN_MENU);

		_tourProvider = tourProvider;

		setText(Messages.action_export_tour);
		setMenuCreator(this);

		getExtensionPoints();
		createActions();
	}

	private void addActionToMenu(final Action action) {
		final ActionContributionItem item = new ActionContributionItem(action);
		item.fill(_menu, -1);
	}

	private void createActions() {

		if (_exportTourActions != null) {
			return;
		}

		_exportTourActions = new ArrayList<>();

		// create action for each extension point
		for (final ExportTourExtension exportTourExtension : _exportExtensionPoints) {
			_exportTourActions.add(new ActionExportTour(exportTourExtension));
		}
	}

	@Override
	public void dispose() {
		if (_menu != null) {
			_menu.dispose();
			_menu = null;
		}
	}

	/**
	 * read extension points {@link TourbookPlugin#EXT_POINT_EXPORT_TOUR}
	 */
	private ArrayList<ExportTourExtension> getExtensionPoints() {

		if (_exportExtensionPoints != null) {
			return _exportExtensionPoints;
		}

		_exportExtensionPoints = new ArrayList<>();

		final IExtensionPoint extPoint = Platform.getExtensionRegistry()
				.getExtensionPoint(
						TourbookPlugin.PLUGIN_ID,
						TourbookPlugin.EXT_POINT_EXPORT_TOUR);

		if (extPoint != null) {

			for (final IExtension extension : extPoint.getExtensions()) {
				for (final IConfigurationElement configElement : extension.getConfigurationElements()) {

					if (configElement.getName().equalsIgnoreCase("export")) { //$NON-NLS-1$
						try {
							final Object object = configElement.createExecutableExtension("class"); //$NON-NLS-1$
							if (object instanceof ExportTourExtension) {

								final ExportTourExtension exportTourItem = (ExportTourExtension) object;

								exportTourItem.setExportId(configElement.getAttribute("id")); //$NON-NLS-1$
								exportTourItem.setVisibleName(configElement.getAttribute("name")); //$NON-NLS-1$
								exportTourItem.setFileExtension(configElement.getAttribute("fileextension")); //$NON-NLS-1$

								_exportExtensionPoints.add(exportTourItem);
							}
						} catch (final CoreException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}

		return _exportExtensionPoints;
	}

	@Override
	public Menu getMenu(final Control parent) {
		return null;
	}

	@Override
	public Menu getMenu(final Menu parent) {

		dispose();
		_menu = new Menu(parent);

		for (final ActionExportTour action : _exportTourActions) {
			addActionToMenu(action);
		}

		return _menu;
	}

	public void setNumberOfTours(final int numTours) {

		setText(Messages.action_export_tour + String.format(" (%d)", numTours)); //$NON-NLS-1$
	}

	public void setTourRange(final int tourStartIndex, final int tourEndIndex) {
		_tourStartIndex = tourStartIndex;
		_tourEndIndex = tourEndIndex;
	}

}
