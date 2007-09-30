/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
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
package net.tourbook.ui.views.tourBook;

import java.util.ArrayList;
import java.util.Iterator;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ResizeableListDialog;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

public class ActionSetTourType extends Action {

	private static final String	MEMENTO_SELECTED_TOUR_TYPE_ID	= "action-save-tour-type.selected-tour-type-id";	//$NON-NLS-1$

	private TourBookView		fViewPart;

	private ArrayList<TourType>	fTourTypes;
	private TourType			fSelectedTourType;

	private boolean				fUseDialog;

	private class TourTypeContentProvider implements IStructuredContentProvider {

		public void dispose() {}

		public Object[] getElements(Object inputElement) {
			return fTourTypes.toArray();
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
	}

	private class TourTypeLabelProvider extends LabelProvider implements ITableLabelProvider {

		public String getColumnText(Object element, int columnIndex) {

			TourType tourType = (TourType) element;
			switch (columnIndex) {
			case 0:
				return tourType.getName();
			}
			return "?"; //$NON-NLS-1$
		}

		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}
	}

	public IDialogSettings getDialogSettings() {

		final String DIALOG_SETTINGS_SECTION = "DialogSelectTourType"; //$NON-NLS-1$

		IDialogSettings pluginSettings = TourbookPlugin.getDefault().getDialogSettings();
		IDialogSettings dialogSettings = pluginSettings.getSection(DIALOG_SETTINGS_SECTION);

		if (dialogSettings == null) {
			dialogSettings = pluginSettings.addNewSection(DIALOG_SETTINGS_SECTION);
		}
		return dialogSettings;
	}

	public ActionSetTourType(TourBookView tourBookView, boolean useDialog) {

		fViewPart = tourBookView;
		fUseDialog = useDialog;

		setText(Messages.Tour_Book_Action_set_tour_type_with_dlg);

		setEnabled(false);
	}

	public void run() {

		/*
		 * create a list with all tour types which does not contain the fake type id's
		 */

		ArrayList<TourType> pluginTourTypes = TourbookPlugin.getDefault().getTourTypes();
		fTourTypes = new ArrayList<TourType>(pluginTourTypes.size());

		for (TourType tourType : pluginTourTypes) {
			if (tourType.getTypeId() >= 0) {
				fTourTypes.add(tourType);
			}
		}

		final TourType tourType;

		if (fUseDialog) {

			tourType = selectTourType();

			// check if a tour type was selected
			if (tourType == null) {
				return;
			}
		} else {
			tourType = fSelectedTourType;
		}

		Runnable runnable = new Runnable() {

			public void run() {

				boolean isModified = false;

				// get selected tours
				final IStructuredSelection selectedTours = ((IStructuredSelection) fViewPart.getTourViewer()
						.getSelection());

				TourData firstSelectedTourData = null;

				// loop: all selected tours
				for (Iterator iter = selectedTours.iterator(); iter.hasNext();) {

					Object selObject = iter.next();

					if (selObject instanceof TVITourBookTour) {

						TVITourBookTour tviTour = ((TVITourBookTour) selObject);

						final TourData tourData = TourManager.getInstance()
								.getTourData(tviTour.getTourId());

						if (tourData != null) {

							tourData.setTourType(tourType);

							// save the tour type
							if (TourDatabase.saveTour(tourData)) {

								// update the tour type in the viewer data model
								tviTour.fTourTypeId = tourType.getTypeId();

								firstSelectedTourData = tourData;
								isModified = true;
							}
						}
					}
				}

				// update the tour viewer and statistics
				if (isModified) {

					fSelectedTourType = tourType;

					fViewPart.getTourViewer().update(selectedTours.toArray(), null);
//					fViewPart.refreshStatistics();

					if (firstSelectedTourData != null) {
//						fViewPart.refreshTour(firstSelectedTourData);
					}

					// reselect the first tour, to force the statistic to reselect the tour
					fViewPart.getTourViewer()
							.setSelection(new StructuredSelection(selectedTours.getFirstElement()));

					// update views which display the tour type (raw data view) 
					TourbookPlugin.getDefault()
							.getPreferenceStore()
							.setValue(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED,
									Math.random());
				}
			}
		};
		BusyIndicator.showWhile(Display.getCurrent(), runnable);

	}

	private TourType selectTourType() {

		ResizeableListDialog dialog = new ResizeableListDialog(fViewPart.getSite().getShell());

		dialog.setContentProvider(new TourTypeContentProvider());
		dialog.setLabelProvider(new TourTypeLabelProvider());

		dialog.setTitle(Messages.Tour_Book_Dlg_set_tour_type_title);
		dialog.setMessage(Messages.Tour_Book_Dlg_set_tour_type_msg);
		dialog.setDialogBoundsSettings(getDialogSettings(), Dialog.DIALOG_PERSISTLOCATION
				| Dialog.DIALOG_PERSISTSIZE);

		dialog.setInput(this);

		// select last tour type
		IDialogSettings settings = getDialogSettings();
		boolean isSelected = false;
		try {
			long typeId = settings.getLong(MEMENTO_SELECTED_TOUR_TYPE_ID);
			for (TourType tourType : fTourTypes) {
				if (tourType.getTypeId() == typeId) {
					dialog.setInitialSelections(new TourType[] { tourType });
					isSelected = true;
					break;
				}
			}
		} catch (NumberFormatException e) {}

		// select the first entry when nothing was selected before
		if (isSelected == false && fTourTypes.size() > 0) {
			dialog.setInitialSelections(new Object[] { fTourTypes.get(0) });
		}

		dialog.create();

		// disable ok button when no tour types are available
		if (fTourTypes.size() == 0) {
			dialog.getOkButton().setEnabled(false);
		}

		if (dialog.open() != Window.OK) {
			return null;
		}

		Object[] tourTypes = dialog.getResult();

		if (tourTypes != null && tourTypes.length > 0) {

			TourType selectedTourType = (TourType) tourTypes[0];

			settings.put(MEMENTO_SELECTED_TOUR_TYPE_ID, selectedTourType.getTypeId());

			return selectedTourType;
		} else {
			return null;
		}
	}

	TourType getSelectedTourType() {
		return fSelectedTourType;
	}

	void setSelectedTourType(TourType selectedTourType) {
		fSelectedTourType = selectedTourType;
	}
}
