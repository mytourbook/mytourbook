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
package net.tourbook.tour;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import net.tourbook.Messages;
import net.tourbook.application.TourTypeContributionItem;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.TourTypeFilterSet;

public class TourTypeFilterManager {

	private static final String					TAG_NAME					= "name";									//$NON-NLS-1$

	private static final String					TAG_FILTER_TYPE				= "filterType";							//$NON-NLS-1$
	private static final String					TAG_SYSTEM_ID				= "systemId";								//$NON-NLS-1$
	private static final String					TAG_TOUR_TYPE_ID			= "tourTypeId";							//$NON-NLS-1$

	private static final String					MEMENTO_ROOT_FILTER_LIST	= "filterlist";							//$NON-NLS-1$
	private static final String					MEMENTO_CHILD_FILTER		= "filter";								//$NON-NLS-1$
	private static final String					MEMENTO_CHILD_TOURTYPE		= "tourtype";								//$NON-NLS-1$

	private static final String					MEMENTO_FILTER_LIST_FILE	= "filterlist.xml";						//$NON-NLS-1$

	private static IPreferenceStore				_prefStore					= TourbookPlugin.getPrefStore();
	private static IDialogSettings				_state						= TourbookPlugin
																					.getState("TourTypeFilterManager"); //$NON-NLS-1$
	private static IPropertyChangeListener		_prefChangeListener;

	/**
	 * contains the tour type filters which are displayed in the combobox
	 */
	private static ArrayList<TourTypeFilter>	_tourTypeFilters;

	private static ArrayList<ActionTTFilter>	_ttFilterActions			= new ArrayList<>();

	private static double						_propertyValue;

	private static ActionOpenPrefDialog			_actionOpenTourTypePrefs;

	/**
	 * contains the action for the filter which is currently selected
	 */
	private static ActionTTFilter				_selectedFilterAction;

	private static TourTypeContributionItem		_tourTypeContribItem;

	static {

		addPrefListener();

		_actionOpenTourTypePrefs = new ActionOpenPrefDialog(
				Messages.Action_TourType_ModifyTourTypeFilter,
				ITourbookPreferences.PREF_PAGE_TOUR_TYPE_FILTER);
	}

	private static class ActionTTFilter extends Action {

		private TourTypeFilter	__ttFilter;
		private ImageDescriptor	__filterImageDescriptor;

		public ActionTTFilter(final TourTypeFilter ttFilter) {

			/*
			 * push button is used to see the image in a menu item in linux but a checkbox hides the
			 * image in ubuntu 10.4 therefore the check box is not used an the selected filter is
			 * marked in another way
			 */
			super(ttFilter.getFilterName(), AS_PUSH_BUTTON);

			__ttFilter = ttFilter;
			__filterImageDescriptor = TourTypeFilter.getFilterImageDescriptor(ttFilter);
		}

		@Override
		public void run() {

			_selectedFilterAction = this;

			_tourTypeContribItem.updateUI(__ttFilter);

			setActiveTourTypeFilter(__ttFilter, true);
		}

		@Override
		public String toString() {
			return __ttFilter.toString();
		}
	}

	public TourTypeFilterManager() {}

	/**
	 * listen for changes in the tour type
	 */
	private static void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {

			@Override
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)
						|| property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

					final String newValue = event.getNewValue().toString();
					final double propertyValue = Double.parseDouble(newValue);

					// check if the event was originated from this tour type
					// combobox
					if (_propertyValue != propertyValue) {

						/*
						 * reselect old tour type filter when it's still available
						 */
						final TourTypeFilter activeTourTypeFilter = TourbookPlugin.getActiveTourTypeFilter();

						updateTourTypeFilter();

						selectTourTypeFilter(activeTourTypeFilter, false);
					}
				}
			}
		};

		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	/**
	 * Fills the tour type filter context menu.
	 * 
	 * @param menuMgr
	 */
	public static void fillMenu(final IMenuManager menuMgr) {

		final TourTypeFilter activeTTFilter = TourbookPlugin.getActiveTourTypeFilter();

		for (final ActionTTFilter ttFilterAction : _ttFilterActions) {

			// check filter which is currently selected in the UI
			final boolean isChecked = activeTTFilter == ttFilterAction.__ttFilter;

			ttFilterAction.setChecked(isChecked);
			ttFilterAction.setEnabled(isChecked == false);

			String filterName;

			if (isChecked) {
				filterName = ">>> " + ttFilterAction.__ttFilter.getFilterName() + " <<<";//$NON-NLS-1$ //$NON-NLS-2$
			} else {
				filterName = ttFilterAction.__ttFilter.getFilterName();
			}

			ttFilterAction.setText(filterName);

			// disabled filter image is hidden because it look ugly on win32
			ttFilterAction.setImageDescriptor(isChecked ? null : ttFilterAction.__filterImageDescriptor);

			menuMgr.add(ttFilterAction);
		}

		menuMgr.add(new Separator());
		menuMgr.add(_actionOpenTourTypePrefs);
	}

	/**
	 * @return Returns a list with all available tour type filters.
	 *         <p>
	 *         <b>This list MUST NOT BE MODIFIED as it is synched with another list !!!</b>
	 */
	public static ArrayList<TourTypeFilter> getTourTypeFilters() {

		if (_tourTypeFilters == null) {
			updateTourTypeFilter();
		}

		return _tourTypeFilters;
	}

	private static XMLMemento getXMLMementoRoot() {

		try {

			final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

			final Element element = document.createElement(MEMENTO_ROOT_FILTER_LIST);
			element.setAttribute("version", "1"); //$NON-NLS-1$ //$NON-NLS-2$

			document.appendChild(element);

			return new XMLMemento(document, element);

		} catch (final ParserConfigurationException e) {
			throw new Error(e.getMessage());
		}
	}

//	public static TourTypeFilterManager instance() {
//
//		if (_instance == null) {
//			_instance = new TourTypeFilterManager();
//		}
//		return _instance;
//	}

	/**
	 * @return Returns a list with all tour type filters retrieved from the filter xml file
	 */
	public static ArrayList<TourTypeFilter> readTourTypeFilters() {

		final ArrayList<TourTypeFilter> filterList = readXMLFilterFile();

		final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();
		final ArrayList<?> tourTypesNotDisplayed = (ArrayList<?>) tourTypes.clone();

		/*
		 * check if all system filters are visible
		 */
		boolean isSysFilterAll = false;
		boolean isSysFilterNotDefined = false;

		for (final TourTypeFilter tourTypeFilter : filterList) {
			if (tourTypeFilter.getFilterType() == TourTypeFilter.FILTER_TYPE_SYSTEM) {
				switch (tourTypeFilter.getSystemFilterId()) {
				case TourTypeFilter.SYSTEM_FILTER_ID_ALL:

					isSysFilterAll = true;

					// set correct locale string
					tourTypeFilter.setName(Messages.App_Tour_type_item_all_types);
					break;

				case TourTypeFilter.SYSTEM_FILTER_ID_NOT_DEFINED:

					isSysFilterNotDefined = true;

					// set correct locale string
					tourTypeFilter.setName(Messages.App_Tour_type_item_not_defined);
					break;
				default:
					break;
				}
			}
		}

		if (isSysFilterAll == false) {
			filterList.add(new TourTypeFilter(
					TourTypeFilter.SYSTEM_FILTER_ID_ALL,
					Messages.App_Tour_type_item_all_types));
		}
		if (isSysFilterNotDefined == false) {
			filterList.add(new TourTypeFilter(
					TourTypeFilter.SYSTEM_FILTER_ID_NOT_DEFINED,
					Messages.App_Tour_type_item_not_defined));
		}

		/*
		 * ensure that all available tour types are visible
		 */
		for (final TourTypeFilter tourTypeFilter : filterList) {
			final TourType tourType = tourTypeFilter.getTourType();
			if (tourType != null) {
				tourTypesNotDisplayed.remove(tourType);
			}
		}

		for (final Object tourType : tourTypesNotDisplayed) {
			filterList.add(new TourTypeFilter((TourType) tourType));
		}

		return filterList;
	}

	/**
	 * Read filter list from xml file
	 * 
	 * @return Returns a list with all filters from the xml file
	 */
	private static ArrayList<TourTypeFilter> readXMLFilterFile() {

		final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();
		final ArrayList<TourTypeFilter> filterList = new ArrayList<>();

		final IPath stateLocation = Platform.getStateLocation(TourbookPlugin.getDefault().getBundle());
		final String filename = stateLocation.append(MEMENTO_FILTER_LIST_FILE).toFile().getAbsolutePath();

		// check if filter file is available
		final File inputFile = new File(filename);
		if (inputFile.exists() == false) {
			return filterList;
		}

		InputStreamReader reader = null;
		long tourTypeId;

		try {
			reader = new InputStreamReader(new FileInputStream(inputFile), "UTF-8"); //$NON-NLS-1$
			final XMLMemento mementoFilterList = XMLMemento.createReadRoot(reader);

			final IMemento[] mementoFilters = mementoFilterList.getChildren(MEMENTO_CHILD_FILTER);

			for (final IMemento mementoFilter : mementoFilters) {

				final Integer filterType = mementoFilter.getInteger(TAG_FILTER_TYPE);
				final String filterName = mementoFilter.getString(TAG_NAME);

				if (filterType == null || filterName == null) {
					continue;
				}

				switch (filterType) {
				case TourTypeFilter.FILTER_TYPE_SYSTEM:
					final Integer systemId = mementoFilter.getInteger(TAG_SYSTEM_ID);
					if (systemId == null) {
						continue;
					}

					filterList.add(new TourTypeFilter(systemId, filterName));

					break;

				case TourTypeFilter.FILTER_TYPE_DB:

					String tourTypeIdString = mementoFilter.getString(TAG_TOUR_TYPE_ID);

					if (tourTypeIdString == null) {
						continue;
					}

					tourTypeId = Long.parseLong(tourTypeIdString);

					// find the tour type in the available tour types
					for (final TourType tourType : tourTypes) {
						if (tourType.getTypeId() == tourTypeId) {
							filterList.add(new TourTypeFilter(tourType));
							break;
						}
					}

					break;

				case TourTypeFilter.FILTER_TYPE_TOURTYPE_SET:

					final ArrayList<TourType> tourTypesInFilter = new ArrayList<>();
					final IMemento[] mementoTourTypes = mementoFilter.getChildren(MEMENTO_CHILD_TOURTYPE);

					// get all tour types
					for (final IMemento mementoTourType : mementoTourTypes) {

						tourTypeIdString = mementoTourType.getString(TAG_TOUR_TYPE_ID);
						if (tourTypeIdString == null) {
							continue;
						}

						tourTypeId = Long.parseLong(tourTypeIdString);

						// find the tour type in the available tour types
						for (final TourType tourType : tourTypes) {
							if (tourType.getTypeId() == tourTypeId) {
								tourTypesInFilter.add(tourType);
								break;
							}
						}
					}

					final TourTypeFilterSet filterSet = new TourTypeFilterSet();
					filterSet.setName(filterName);
					filterSet.setTourTypes(tourTypesInFilter.toArray());

					filterList.add(new TourTypeFilter(filterSet));

					break;

				default:
					break;
				}
			}

		} catch (final UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final WorkbenchException e) {
			e.printStackTrace();
		} catch (final NumberFormatException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
		}

		return filterList;
	}

	public static void restoreState() {

		final ArrayList<TourTypeFilter> tourTypeFilters = getTourTypeFilters();

		final String lastTourTypeFilterName = _state.get(ITourbookPreferences.APP_LAST_SELECTED_TOUR_TYPE_FILTER);

		TourTypeFilter selectTourTypeFilter = null;

		if (lastTourTypeFilterName != null) {

			// find the name in the filter list

			for (final TourTypeFilter tourTypeFilter : tourTypeFilters) {
				if (tourTypeFilter.getFilterName().equals(lastTourTypeFilterName)) {
					selectTourTypeFilter = tourTypeFilter;
					break;
				}
			}
		}

		if (selectTourTypeFilter == null) {

			// old filter is not available, get the filter which selects all tour types

			for (final TourTypeFilter tourTypeFilter : tourTypeFilters) {
				if (tourTypeFilter.getFilterType() == TourTypeFilter.FILTER_TYPE_SYSTEM
						&& tourTypeFilter.getSystemFilterId() == TourTypeFilter.SYSTEM_FILTER_ID_ALL) {
					selectTourTypeFilter = tourTypeFilter;
					break;
				}
			}
		}

		// try to reselect the last tour type filter
		selectTourTypeFilter(selectTourTypeFilter, false);
	}

	public static void saveState() {

		final TourTypeFilter activeTourTypeFilter = TourbookPlugin.getActiveTourTypeFilter();

		_state.put(ITourbookPreferences.APP_LAST_SELECTED_TOUR_TYPE_FILTER, activeTourTypeFilter.getFilterName());
	}

	/**
	 * @param isNext
	 *            is <code>true</code> when the next filter should be selected, is
	 *            <code>false</code> when the previous filter should be selected
	 * @return Returns <code>true</code> when a new filter is selected
	 */
	public static boolean selectNextFilter(final boolean isNext) {

		int selectedFilterIndex = 0;

		// get filter which is currently selected
		for (final ActionTTFilter filterAction : _ttFilterActions) {

			if (filterAction == _selectedFilterAction) {
				break;
			}

			selectedFilterIndex++;
		}

		if (isNext && selectedFilterIndex < _ttFilterActions.size() - 1) {

			// select next filter

			selectTourTypeFilter(_tourTypeFilters.get(++selectedFilterIndex), true);

		} else if (isNext == false && selectedFilterIndex > 0) {

			// select previous filter

			selectTourTypeFilter(_tourTypeFilters.get(--selectedFilterIndex), true);

		} else {
			return false;
		}

		return true;
	}

	/**
	 * reselect the tour type in the combo box and set the active tour type in the plugin activator
	 * 
	 * @param isFireEvent
	 * @param tourTypeFilter
	 */
	private static TourTypeFilter selectTourTypeFilter(	final TourTypeFilter requestedTourTypeFilter,
														final boolean isFireEvent) {

		ActionTTFilter selectedTTFilterAction = null;

		// uncheck all actions
		for (final ActionTTFilter ttFilterAction : _ttFilterActions) {
			ttFilterAction.setChecked(false);
		}

		// get requested filter action
		for (final ActionTTFilter ttFilterAction : _ttFilterActions) {
			if (ttFilterAction.__ttFilter.equals(requestedTourTypeFilter)) {
				selectedTTFilterAction = ttFilterAction;
				break;
			}
		}

		// ensure a filter is selected
		if (selectedTTFilterAction == null) {
			selectedTTFilterAction = _ttFilterActions.get(0);
		}

		selectedTTFilterAction.setChecked(true);

		_selectedFilterAction = selectedTTFilterAction;

		final TourTypeFilter selectedFilter = selectedTTFilterAction.__ttFilter;

		_tourTypeContribItem.updateUI(selectedFilter);

		setActiveTourTypeFilter(selectedFilter, isFireEvent);

		return selectedFilter;
	}

	private static void setActiveTourTypeFilter(final TourTypeFilter ttFilter, final boolean isFireEvent) {

		TourbookPlugin.setActiveTourTypeFilter(ttFilter);

		if (isFireEvent == false) {
			return;
		}

		/*
		 * fire as asynch that the combo box drop down is hidden and the combo text ist displayed
		 */
		Display.getCurrent().asyncExec(new Runnable() {
			@Override
			public void run() {

				// fire change event
				_propertyValue = Math.random();
				_prefStore.setValue(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED, _propertyValue);
			}
		});
	}

	public static void setToolBarContribItem(final TourTypeContributionItem tourTypeContribItem) {
		_tourTypeContribItem = tourTypeContribItem;
	}

	private static void updateTourTypeFilter() {

		if (_tourTypeFilters != null) {
			_tourTypeFilters.clear();
		}

		_tourTypeFilters = readTourTypeFilters();

		_ttFilterActions.clear();

		for (final TourTypeFilter tourTypeFilter : _tourTypeFilters) {
			_ttFilterActions.add(new ActionTTFilter(tourTypeFilter));
		}
	}

	/**
	 * write the filters from the viewer into an xml file
	 * 
	 * @param filterViewer
	 */
	public static void writeXMLFilterFile(final TableViewer filterViewer) {

		BufferedWriter writer = null;

		try {

			final IPath stateLocation = Platform.getStateLocation(TourbookPlugin.getDefault().getBundle());
			final File file = stateLocation.append(MEMENTO_FILTER_LIST_FILE).toFile();

			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8")); //$NON-NLS-1$

			final XMLMemento xmlMemento = getXMLMementoRoot();

			for (final TableItem tableItem : filterViewer.getTable().getItems()) {

				final Object itemData = tableItem.getData();
				if (itemData instanceof TourTypeFilter) {

					final TourTypeFilter filter = (TourTypeFilter) itemData;

					final IMemento mementoFilter = xmlMemento.createChild(MEMENTO_CHILD_FILTER);

					final int filterType = filter.getFilterType();

					mementoFilter.putInteger(TAG_FILTER_TYPE, filterType);
					mementoFilter.putString(TAG_NAME, filter.getFilterName());

					switch (filterType) {
					case TourTypeFilter.FILTER_TYPE_SYSTEM:
						mementoFilter.putInteger(TAG_SYSTEM_ID, filter.getSystemFilterId());
						break;

					case TourTypeFilter.FILTER_TYPE_DB:
						mementoFilter.putString(TAG_TOUR_TYPE_ID, Long.toString(filter.getTourType().getTypeId()));
						break;

					case TourTypeFilter.FILTER_TYPE_TOURTYPE_SET:

						final TourTypeFilterSet filterSet = filter.getTourTypeSet();

						final Object[] tourTypes = filterSet.getTourTypes();
						if (tourTypes != null) {
							for (final Object item : tourTypes) {

								final TourType tourType = (TourType) item;
								final IMemento mementoTourType = mementoFilter.createChild(MEMENTO_CHILD_TOURTYPE);
								{
									mementoTourType.putString(TAG_TOUR_TYPE_ID, Long.toString(tourType.getTypeId()));
								}
							}
						}
						break;

					default:
						break;
					}
				}
			}

			xmlMemento.save(writer);

		} catch (final IOException e) {
			e.printStackTrace();
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
