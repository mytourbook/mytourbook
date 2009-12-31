/* *****************************************************************************
 *  Copyright (C) 2008 Michael Kanis, Veit Edunjobi and others
 *  
 *  This file is part of Geoclipse.
 *
 *  Geoclipse is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Geoclipse is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Geoclipse.  If not, see <http://www.gnu.org/licenses/>. 
 *******************************************************************************/

package de.byteholder.geoclipse.poi;

import java.util.Collection;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.help.IWorkbenchHelpSystem;
import org.eclipse.ui.part.ViewPart;

import de.byteholder.gpx.ext.PointOfInterest;

/**
 * @author Michael Kanis
 * @author Veit Edunjobi
 */
public class PoiView extends ViewPart implements Observer {

	private static final String		DIALOG_SETTINGS_KEY_POIS			= "pois";									//$NON-NLS-1$

	private static final String		DIALOG_SETTINGS_SECTION_RECENT_POIS	= "recentPois";							//$NON-NLS-1$

	public final static String		ID									= "de.byteholder.geoclipse.poi.poiView";	//$NON-NLS-1$
////now using searchQueryCombo instead	
//	private Text					searchQueryText;
	private Combo					searchQueryCombo;
	private Button					searchButton;
	private TableViewer				poiTableViewer;

	private List<PointOfInterest>	pois;

	private PostSelectionProvider	postSelectionProvider;

	private IWorkbenchHelpSystem	workbenchHelpSystem;

	class ViewContentProvider implements IStructuredContentProvider {
		public void dispose() {}

		public Object[] getElements(final Object parent) {
			if (pois == null) {
				return new String[] {};
			} else {
				return pois.toArray();
			}
		}

		public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {}
	}

	class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {

		public Image getColumnImage(final Object obj, final int index) {
			switch (index) {
			case 0:
				return getImage(obj);
			default:
				return null;
			}
		}

		public String getColumnText(final Object obj, final int index) {
			final PointOfInterest poi = (PointOfInterest) obj;

			switch (index) {
			case 0:
				return poi.getCategory();
			case 1:
				final StringBuilder s = new StringBuilder(poi.getName());
				if (poi.getNearestPlaces() != null && poi.getNearestPlaces().size() > 0) {
					s.append(" (") //$NON-NLS-1$
							.append(Messages.PoiView_0)
							.append(" ") //$NON-NLS-1$
							.append(poi.getNearestPlaces().get(0).getName())
							.append(")"); //$NON-NLS-1$
				}
				return s.toString();
			default:
				return getText(obj);
			}
		}

		@Override
		public Image getImage(final Object obj) {

			if (obj instanceof PointOfInterest) {

				Image img;
				final PointOfInterest poi = (PointOfInterest) obj;

				// TODO find/make better matching icons

				if (poi.getCategory().equals("highway")) { //$NON-NLS-1$
					img = Activator.getDefault().getImageRegistry().get(Activator.IMG_CAR);
				} else if (poi.getCategory().equals("place")) { //$NON-NLS-1$
					img = Activator.getDefault().getImageRegistry().get(Activator.IMG_HOUSE);
				} else if (poi.getCategory().equals("waterway")) { //$NON-NLS-1$
					img = Activator.getDefault().getImageRegistry().get(Activator.IMG_ANCHOR);
				} else if (poi.getCategory().equals("amenity")) { //$NON-NLS-1$
					img = Activator.getDefault().getImageRegistry().get(Activator.IMG_CART);
				} else if (poi.getCategory().equals("leisure")) { //$NON-NLS-1$
					img = Activator.getDefault().getImageRegistry().get(Activator.IMG_STAR);
				} else if (poi.getCategory().equals("sport")) { //$NON-NLS-1$
					img = Activator.getDefault().getImageRegistry().get(Activator.IMG_SOCCER);
				} else {
					img = Activator.getDefault().getImageRegistry().get(Activator.IMG_FLAG);
				}

				return img;
			} else {
				return null;
			}
		}
	}

	public PoiView() {}

	public PoiView(final List<PointOfInterest> pois) {
		this.pois = pois;
	}

	private void createActions() {

		searchQueryCombo.addListener(SWT.DefaultSelection, new Listener() {
			public void handleEvent(final Event e) {
				searchQueryCombo.setEnabled(false);
				searchButton.setEnabled(false);
				final GeoQuery finder = new GeoQuery(searchQueryCombo.getText());
				finder.addObserver(PoiView.this);
				finder.asyncFind();
			}
		});

		searchButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
//				searchQueryText.setEnabled(false);
				searchQueryCombo.setEnabled(false);
				searchButton.setEnabled(false);
//				final GeoQuery finder = new GeoQuery(searchQueryText.getText());
				final GeoQuery finder = new GeoQuery(searchQueryCombo.getText());
				finder.addObserver(PoiView.this);
				finder.asyncFind();
			}
		});

		poiTableViewer.addPostSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(final SelectionChangedEvent e) {

				final ISelection selection = e.getSelection();
				final Object obj = ((IStructuredSelection) selection).getFirstElement();
				final PointOfInterest selectedPoi = (PointOfInterest) obj;

				postSelectionProvider.setSelection(selectedPoi);
			}
		});

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(final Composite parent) {

		createUI(parent);
		createActions();

		// this part is a selection provider
		getSite().setSelectionProvider(postSelectionProvider = new PostSelectionProvider());

	}

	private void createUI(final Composite parent) {

		//contextHelp for this view. 
		//Clicking F1 while this view has Focus will jump to the associated
		//helpContext, see de.byteholder.geoclipse.help
		workbenchHelpSystem = PlatformUI.getWorkbench().getHelpSystem();
		//TODO this "forces" of this plug-in to de.byteholder.geoclipse.help ?!
		final String contextId = "de.byteholder.geoclipse.help.places_view"; //$NON-NLS-1$
		workbenchHelpSystem.setHelp(parent, contextId);

		final GridLayout layout = new GridLayout();
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 5;
		layout.numColumns = 2;
		parent.setLayout(layout);

		final Object[] recentPois = loadPois();
//		searchQueryText = new Text(parent, SWT.SINGLE | SWT.BORDER);
		searchQueryCombo = new Combo(parent, SWT.NONE);

		if (recentPois != null) {
			searchQueryCombo.setItems((String[]) recentPois);
		}

		searchButton = new Button(parent, SWT.PUSH);

		//TODO VEI SWT CONSTANTS
//		table fuer den tableviewer anlegen
		final Table poiTable = new Table(parent, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		poiTable.setLinesVisible(true);
		poiTable.setHeaderVisible(true);

		//Spalte category
		final TableColumn columnCategory = new TableColumn(poiTable, SWT.LEFT);
		columnCategory.setText("Category"); //$NON-NLS-1$
		columnCategory.setWidth(75);

		//Spalte name
		final TableColumn columnName = new TableColumn(poiTable, SWT.LEFT);
		columnName.setText("Name"); //$NON-NLS-1$
		columnName.setWidth(300);

		//tableviewer anlegen und table uebergeben
		poiTableViewer = new TableViewer(poiTable);

		poiTableViewer.setContentProvider(new ViewContentProvider());
		poiTableViewer.setLabelProvider(new ViewLabelProvider());

		//input uebergeben
		if (recentPois == null) {
			poiTableViewer.setInput(null);
		} else {
			poiTableViewer.setInput(recentPois);
		}
		//poiTableViewer.setInput (new Object[] {new Integer(1)});

		final GridData queryTextGridData = new GridData();
		queryTextGridData.horizontalAlignment = GridData.FILL;
		queryTextGridData.grabExcessHorizontalSpace = true;
//		searchQueryText.setLayoutData(queryTextGridData);
		searchQueryCombo.setLayoutData(queryTextGridData);

		searchButton.setText("Search"); //$NON-NLS-1$
		parent.getShell().setDefaultButton(searchButton);

		final GridData poiTableGridData = new GridData();
		poiTableGridData.horizontalSpan = 2;
		poiTableGridData.grabExcessHorizontalSpace = true;
		poiTableGridData.grabExcessVerticalSpace = true;
		poiTableGridData.horizontalAlignment = GridData.FILL;
		poiTableGridData.verticalAlignment = GridData.FILL;
		poiTableViewer.getTable().setLayoutData(poiTableGridData);
	}

	public Collection<PointOfInterest> getPois() {
		return pois;
	}

	/**
	 * Loads recently found {@link PointOfInterest} from the dialog settings.
	 * 
	 * @return an array of objects or null if none were found.
	 */
	private Object[] loadPois() {
		final IDialogSettings dialogSettings = Activator.getDefault()
				.getDialogSettings()
				.getSection(DIALOG_SETTINGS_SECTION_RECENT_POIS);

		if (dialogSettings == null) {
			return null;
		}
		final String recentPois = dialogSettings.get(DIALOG_SETTINGS_KEY_POIS);

		if (recentPois == null) {
			return null;
		}
		final Object[] pois = recentPois.split(","); //$NON-NLS-1$
		Activator.getDefault().getLog().log(new Status(Status.INFO, Activator.PLUGIN_ID, "Loaded Pois: " + pois)); //$NON-NLS-1$
		return pois;
	}

	/**
	 * Saves the given list of {@link PointOfInterest} to the dialogsettings.
	 * 
	 * @param pois
	 */

	private void savePois(final List<PointOfInterest> pois) {

		IDialogSettings dialogSettings = Activator.getDefault()
				.getDialogSettings()
				.getSection(DIALOG_SETTINGS_SECTION_RECENT_POIS);
		if (dialogSettings == null) {
			dialogSettings = Activator.getDefault()
					.getDialogSettings()
					.addNewSection(DIALOG_SETTINGS_SECTION_RECENT_POIS);
		}

		final StringBuilder sb = new StringBuilder();
		for (final PointOfInterest pointOfInterest : pois) {

			sb.append(pointOfInterest);
			sb.append(","); //$NON-NLS-1$
		}

		dialogSettings.put(DIALOG_SETTINGS_KEY_POIS, sb.toString());
		Activator.getDefault()
				.getLog()
				.log(new Status(Status.INFO, Activator.PLUGIN_ID, "Saved Pois: " + sb.toString())); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus() {
		searchButton.getShell().setDefaultButton(searchButton);
//		searchQueryText.setFocus();
		searchQueryCombo.setFocus();
	}

	public void setPois(final List<PointOfInterest> pois) {
		this.pois = pois;

		savePois(pois);
	}

	/**
	 * implements update from interface observer
	 */
	public void update(final Observable observable, final Object arg) {

		if (observable instanceof GeoQuery) {
			final GeoQuery finder = (GeoQuery) observable;

			if (finder.getSearchResult() != null) {
				setPois(finder.getSearchResult());
			}

			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					poiTableViewer.refresh();
					searchQueryCombo.setEnabled(true);
					searchButton.setEnabled(true);
				}
			});

			if (finder.getException() != null) {
				throw new RuntimeException(finder.getException());
			}
		}
	}
}
