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
package net.tourbook.application;

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

import net.tourbook.Messages;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.CustomControlContribution;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.TourTypeFilterSet;
import net.tourbook.ui.UI;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class TourTypeContributionItem extends CustomControlContribution {

	private static final String			ID							= "net.tourbook.tourtypefilter";	//$NON-NLS-1$

	private static final String			TAG_NAME					= "name";							//$NON-NLS-1$
	private static final String			TAG_FILTER_TYPE				= "filterType";					//$NON-NLS-1$
	private static final String			TAG_SYSTEM_ID				= "systemId";						//$NON-NLS-1$
	private static final String			TAG_TOUR_TYPE_ID			= "tourTypeId";					//$NON-NLS-1$

	private static final String			MEMENTO_ROOT_FILTER_LIST	= "filterlist";					//$NON-NLS-1$
	private static final String			MEMENTO_CHILD_FILTER		= "filter";						//$NON-NLS-1$
	private static final String			MEMENTO_CHILD_TOURTYPE		= "tourtype";						//$NON-NLS-1$

	private static final String			MEMENTO_FILTER_LIST_FILE	= "filterlist.xml";				//$NON-NLS-1$

	static TourbookPlugin				plugin						= TourbookPlugin.getDefault();

	private IPropertyChangeListener		fPrefChangeListener;

	private TourTypeCombo				fComboTourType;

	/**
	 * contains the tour type filters which are displayed in the combobox
	 */
	private ArrayList<TourTypeFilter>	fTourTypeFilters;

	protected double					fPropertyValue;

	public TourTypeContributionItem() {
		this(ID);
	}

	protected TourTypeContributionItem(String id) {
		super(id);
	}

	/**
	 * @return Returns a list with all tour type filters
	 */
	@SuppressWarnings("unchecked")
	public static ArrayList<TourTypeFilter> getTourTypeFilters() {

		ArrayList<TourTypeFilter> filterList = readXMLFilterFile();

		ArrayList<TourType> tourTypes = TourDatabase.getTourTypes();
		ArrayList<TourType> tourTypesNotDisplayed = (ArrayList<TourType>) tourTypes.clone();

		/*
		 * check if all system filters are visible
		 */
		boolean isSysFilterAll = false;
		boolean isSysFilterNotDefined = false;

		for (TourTypeFilter tourTypeFilter : filterList) {
			if (tourTypeFilter.getFilterType() == TourTypeFilter.FILTER_TYPE_SYSTEM) {
				switch (tourTypeFilter.getSystemFilterId()) {
				case TourTypeFilter.SYSTEM_FILTER_ID_ALL:
					isSysFilterAll = true;
					break;
				case TourTypeFilter.SYSTEM_FILTER_ID_NOT_DEFINED:
					isSysFilterNotDefined = true;
					break;
				default:
					break;
				}
			}
		}

		if (isSysFilterAll == false) {
			filterList.add(new TourTypeFilter(TourTypeFilter.SYSTEM_FILTER_ID_ALL,
					Messages.App_Tour_type_item_all_types));
		}
		if (isSysFilterNotDefined == false) {
			filterList.add(new TourTypeFilter(TourTypeFilter.SYSTEM_FILTER_ID_NOT_DEFINED,
					Messages.App_Tour_type_item_not_defined));
		}

		/*
		 * ensure that all available tour types are visible
		 */
		for (TourTypeFilter tourTypeFilter : filterList) {
			final TourType tourType = tourTypeFilter.getTourType();
			if (tourType != null) {
				tourTypesNotDisplayed.remove(tourType);
			}
		}

		for (TourType tourType : tourTypesNotDisplayed) {
			filterList.add(new TourTypeFilter(tourType));
		}

		return filterList;
	}

	private static XMLMemento getXMLMementoRoot() {
		Document document;
		try {
			document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			Element element = document.createElement(MEMENTO_ROOT_FILTER_LIST);
			element.setAttribute("version", "1"); //$NON-NLS-1$ //$NON-NLS-2$
			document.appendChild(element);

			return new XMLMemento(document, element);

		} catch (ParserConfigurationException e) {
			throw new Error(e.getMessage());
		}
	}

	/**
	 * Read filter list from xml file
	 * 
	 * @return Returns a list with all filters from the xml file
	 */
	private static ArrayList<TourTypeFilter> readXMLFilterFile() {

		ArrayList<TourType> tourTypes = TourDatabase.getTourTypes();
		ArrayList<TourTypeFilter> filterList = new ArrayList<TourTypeFilter>();

		IPath stateLocation = Platform.getStateLocation(TourbookPlugin.getDefault().getBundle());
		String filename = stateLocation.append(MEMENTO_FILTER_LIST_FILE).toFile().getAbsolutePath();

		// check if filter file is available
		File inputFile = new File(filename);
		if (inputFile.exists() == false) {
			return filterList;
		}

		InputStreamReader reader = null;
		long tourTypeId;

		try {
			reader = new InputStreamReader(new FileInputStream(inputFile), "UTF-8"); //$NON-NLS-1$
			XMLMemento mementoFilterList = XMLMemento.createReadRoot(reader);

			IMemento[] mementoFilters = mementoFilterList.getChildren(MEMENTO_CHILD_FILTER);

			for (IMemento mementoFilter : mementoFilters) {

				Integer filterType = mementoFilter.getInteger(TAG_FILTER_TYPE);
				String filterName = mementoFilter.getString(TAG_NAME);

				if (filterType == null || filterName == null) {
					continue;
				}

				switch (filterType) {
				case TourTypeFilter.FILTER_TYPE_SYSTEM:
					Integer systemId = mementoFilter.getInteger(TAG_SYSTEM_ID);
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
					for (TourType tourType : tourTypes) {
						if (tourType.getTypeId() == tourTypeId) {
							filterList.add(new TourTypeFilter(tourType));
							break;
						}
					}

					break;

				case TourTypeFilter.FILTER_TYPE_TOURTYPE_SET:

					ArrayList<TourType> tourTypesInFilter = new ArrayList<TourType>();
					IMemento[] mementoTourTypes = mementoFilter.getChildren(MEMENTO_CHILD_TOURTYPE);

					// get all tour types
					for (IMemento mementoTourType : mementoTourTypes) {

						tourTypeIdString = mementoTourType.getString(TAG_TOUR_TYPE_ID);
						if (tourTypeIdString == null) {
							continue;
						}

						tourTypeId = Long.parseLong(tourTypeIdString);

						// find the tour type in the available tour types
						for (TourType tourType : tourTypes) {
							if (tourType.getTypeId() == tourTypeId) {
								tourTypesInFilter.add(tourType);
								break;
							}
						}
					}

					TourTypeFilterSet filterSet = new TourTypeFilterSet();
					filterSet.setName(filterName);
					filterSet.setTourTypes(tourTypesInFilter.toArray());

					filterList.add(new TourTypeFilter(filterSet));

					break;

				default:
					break;
				}
			}

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (WorkbenchException e) {
			e.printStackTrace();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return filterList;
	}

	/**
	 * write the filters from the viewer into an xml file
	 * 
	 * @param filterViewer
	 */
	public static void writeXMLFilterFile(TableViewer filterViewer) {

		BufferedWriter writer = null;

		try {

			IPath stateLocation = Platform.getStateLocation(TourbookPlugin.getDefault().getBundle());
			File file = stateLocation.append(MEMENTO_FILTER_LIST_FILE).toFile();

			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8")); //$NON-NLS-1$

			XMLMemento xmlMemento = getXMLMementoRoot();

			for (TableItem tableItem : filterViewer.getTable().getItems()) {

				Object itemData = tableItem.getData();
				if (itemData instanceof TourTypeFilter) {

					TourTypeFilter filter = (TourTypeFilter) itemData;

					IMemento mementoFilter = xmlMemento.createChild(MEMENTO_CHILD_FILTER);

					final int filterType = filter.getFilterType();

					mementoFilter.putInteger(TAG_FILTER_TYPE, filterType);
					mementoFilter.putString(TAG_NAME, filter.getFilterName());

					switch (filterType) {
					case TourTypeFilter.FILTER_TYPE_SYSTEM:
						mementoFilter.putInteger(TAG_SYSTEM_ID, filter.getSystemFilterId());
						break;

					case TourTypeFilter.FILTER_TYPE_DB:
						mementoFilter.putString(TAG_TOUR_TYPE_ID,
								Long.toString(filter.getTourType().getTypeId()));
						break;

					case TourTypeFilter.FILTER_TYPE_TOURTYPE_SET:

						TourTypeFilterSet filterSet = filter.getTourTypeSet();

						final Object[] tourTypes = filterSet.getTourTypes();
						if (tourTypes != null) {
							for (Object item : tourTypes) {

								TourType tourType = (TourType) item;
								IMemento mementoTourType = mementoFilter.createChild(MEMENTO_CHILD_TOURTYPE);
								{
									mementoTourType.putString(TAG_TOUR_TYPE_ID,
											Long.toString(tourType.getTypeId()));
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

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * listen for changes in the person list
	 */
	private void addPrefListener() {

		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(Preferences.PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)
						|| property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

					double propertyValue = Double.valueOf(event.getNewValue().toString())
							.doubleValue();

					// check if the event was originated from this tour type
					// combobox
					if (fPropertyValue != propertyValue) {

						fillFilterComboBox();
						reselectTourType(plugin.getActiveTourTypeFilter());
					}
				}
			}

		};
		// register the listener
		plugin.getPluginPreferences().addPropertyChangeListener(fPrefChangeListener);
	}

	@Override
	protected Control createControl(Composite parent) {

		if (PlatformUI.getWorkbench().isClosing()) {
			return new Label(parent, SWT.NONE);
		}

		Composite container = createUI(parent);
		addPrefListener();

		fillFilterComboBox();

		reselectLastTourType();

		return container;
	}

	private Composite createUI(Composite parent) {

		fComboTourType = new TourTypeCombo(parent, SWT.BORDER | SWT.FLAT | SWT.READ_ONLY);

		fComboTourType.setVisibleItemCount(20);
		fComboTourType.setToolTipText(Messages.App_Tour_type_tooltip);

		fComboTourType.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				plugin.getPluginPreferences().removePropertyChangeListener(fPrefChangeListener);
			}
		});

		fComboTourType.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				setActiveTourTypeFilter();

				// fire change event
				fPropertyValue = Math.random();
				plugin.getPreferenceStore()
						.setValue(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED, fPropertyValue);

			}
		});

		return fComboTourType.getControl();
	}

	private void fillFilterComboBox() {

		fTourTypeFilters = getTourTypeFilters();

		fComboTourType.removeAll();

		for (TourTypeFilter tourTypeFilter : fTourTypeFilters) {
			fComboTourType.add(tourTypeFilter.getFilterName(), getFilterImage(tourTypeFilter));
		}
	}

	private Image getFilterImage(TourTypeFilter filter) {

		int filterType = filter.getFilterType();

		Image filterImage = null;

		// set filter name/image
		switch (filterType) {
		case TourTypeFilter.FILTER_TYPE_DB:
			final TourType tourType = filter.getTourType();
			filterImage = UI.getInstance().getTourTypeImage(tourType.getTypeId());
			break;

		case TourTypeFilter.FILTER_TYPE_SYSTEM:
			filterImage = UI.IMAGE_REGISTRY.get(UI.IMAGE_TOUR_TYPE_FILTER_SYSTEM);
			break;

		case TourTypeFilter.FILTER_TYPE_TOURTYPE_SET:
			filterImage = UI.IMAGE_REGISTRY.get(UI.IMAGE_TOUR_TYPE_FILTER);
			break;

		default:
			break;
		}

		return filterImage;
	}

	private void reselectLastTourType() {

		String lastTourTypeFilterName = plugin.getDialogSettings()
				.get(ITourbookPreferences.APP_LAST_SELECTED_TOUR_TYPE_FILTER);

		TourTypeFilter selectTourTypeFilter = null;

		if (lastTourTypeFilterName != null) {

			// find the name in the filter list

			for (TourTypeFilter tourTypeFilter : fTourTypeFilters) {
				if (tourTypeFilter.getFilterName().equals(lastTourTypeFilterName)) {
					selectTourTypeFilter = tourTypeFilter;
					break;
				}
			}
		}

		if (selectTourTypeFilter == null) {

			// get the filter which selects all tour types

			for (TourTypeFilter tourTypeFilter : fTourTypeFilters) {
				if (tourTypeFilter.getFilterType() == TourTypeFilter.FILTER_TYPE_SYSTEM
						&& tourTypeFilter.getSystemFilterId() == TourTypeFilter.SYSTEM_FILTER_ID_ALL) {
					selectTourTypeFilter = tourTypeFilter;
					break;
				}
			}
		}

		// try to reselect the last person
		reselectTourType(selectTourTypeFilter);
	}

	/**
	 * reselect the tour type in the combo box and set the active tour type in the plugin
	 * 
	 * @param tourTypeFilter
	 */
	private void reselectTourType(TourTypeFilter selectTourTypeFilter) {

		TourTypeFilter activeTourTypeFilter = null;

		// find the tour type filter in the combobox
		int tourTypeIndex = 0;
		for (TourTypeFilter tourTypeFilter : fTourTypeFilters) {
			if (tourTypeFilter.getFilterName().equals(selectTourTypeFilter.getFilterName())) {
				activeTourTypeFilter = tourTypeFilter;
				fComboTourType.select(tourTypeIndex);
				break;
			}

			tourTypeIndex++;
		}

		if (activeTourTypeFilter == null) {
			// filter was not found, set first filter as active filter
			fComboTourType.select(0);
			activeTourTypeFilter = fTourTypeFilters.get(0);
		}

		plugin.setActiveTourTypeFilter(activeTourTypeFilter);
	}

	void saveState(IMemento memento) {

		// save selected tour type filter
		int selectionIndex = fComboTourType.getSelectionIndex();

		if (selectionIndex != -1) {
			plugin.getDialogSettings().put(ITourbookPreferences.APP_LAST_SELECTED_TOUR_TYPE_FILTER,
					fTourTypeFilters.get(selectionIndex).getFilterName());
		}
	}

	/**
	 * set the selected tour type
	 */
	private void setActiveTourTypeFilter() {

		int selectionIndex = fComboTourType.getSelectionIndex();
		if (selectionIndex == -1) {
			// nothing is selected, select first entry
			selectionIndex = 0;
			fComboTourType.select(selectionIndex);
		}

		plugin.setActiveTourTypeFilter(fTourTypeFilters.get(selectionIndex));
	}
}
