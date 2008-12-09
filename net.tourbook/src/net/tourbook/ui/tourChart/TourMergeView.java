/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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

package net.tourbook.ui.tourChart;

import java.util.ArrayList;

import net.tourbook.data.TourData;
import net.tourbook.tour.SelectionActiveEditor;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.TourEditor;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

public class TourMergeView extends ViewPart {

	public static final String	ID	= "net.tourbook.views.TourMergeView";	//$NON-NLS-1$

	private TourData			fTourData;

	private PageBook			fPageBook;
	private Label				fPageInvalidData;
	private Label				fPageNoTour;

	private Form				fPageTourMerger;

	private boolean				fIsSaving;

	private ISelectionListener	fPostSelectionListener;

	private IPartListener2		fPartListener;

	private ActionSaveTour		fActionSaveTour;

	

	void actionSaveTour() {
		saveTour();
	}

	private void addPartListener() {

		// set the part listener
		fPartListener = new IPartListener2() {
			public void partActivated(final IWorkbenchPartReference partRef) {}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourMergeView.this) {
					saveTour();
//					saveState();
					hideMergeLayer();
				}
			}

			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			public void partHidden(final IWorkbenchPartReference partRef) {}

			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			public void partOpened(final IWorkbenchPartReference partRef) {}

			public void partVisible(final IWorkbenchPartReference partRef) {}
		};

		getSite().getPage().addPartListener(fPartListener);
	}

	private void addSelectionListener() {

		fPostSelectionListener = new ISelectionListener() {
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

				if (part == TourMergeView.this) {
					return;
				}

				onSelectionChanged(selection);
			}
		};

		getSite().getPage().addPostSelectionListener(fPostSelectionListener);
	}

	private void addTourEventListener() {

//		fTourEventListener = new ITourEventListener() {
//			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {
//
//				if (fTourData == null || part == TourSegmenterView.this) {
//					return;
//				}
//
//				if (eventId == TourEventId.TOUR_CHANGED && eventData instanceof TourEvent) {
//
//					final TourEvent tourEvent = (TourEvent) eventData;
//					final ArrayList<TourData> modifiedTours = tourEvent.getModifiedTours();
//
//					if (modifiedTours == null || modifiedTours.size() == 0) {
//						return;
//					}
//
//					final TourData modifiedTourData = modifiedTours.get(0);
//					final long viewTourId = fTourData.getTourId();
//
//					if (modifiedTourData.getTourId() == viewTourId) {
//
//						// update existing tour
//
//						if (checkDataValidation(modifiedTourData)) {
//
//							if (tourEvent.isReverted) {
//
//								/*
//								 * tour is reverted, saving existing tour is not necessary, just
//								 * update the tour
//								 */
//								setTour(modifiedTourData);
//
//							} else {
//
//								createSegments();
//								reloadViewer();
//							}
//						}
//
//					} else {
//
//						// display new tour
//
//						onSelectionChanged(new SelectionTourData(null, modifiedTourData));
//					}
//
//					// removed old tour data from the selection provider
//					fPostSelectionProvider.clearSelection();
//				}
//			}
//		};
//
//		TourManager.getInstance().addPropertyListener(fTourEventListener);
	}


	@Override
	public void createPartControl(final Composite parent) {

		createUI(parent);
		createActions();

		addSelectionListener();
		addTourEventListener();
		addPartListener();

		// update viewer with current selection
		onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());
		if (fTourData == null) {
			Display.getCurrent().asyncExec(new Runnable() {
				public void run() {

					final ArrayList<TourData> selectedTours = TourManager.getSelectedTours();

					if (selectedTours != null && selectedTours.size() > 0) {
						onSelectionChanged(new SelectionTourData(null, selectedTours.get(0)));
					}
				}
			});
		}
	}

	@Override
	public void dispose() {

		final IWorkbenchPage wbPage = getSite().getPage();
		wbPage.removePostSelectionListener(fPostSelectionListener);
		wbPage.removePartListener(fPartListener);

		super.dispose();
	}

	

	/**
	 * handle a tour selection event
	 * 
	 * @param selection
	 */
	private void onSelectionChanged(final ISelection selection) {

		if (fIsSaving) {
			return;
		}

		/*
		 * run selection async because a tour could be modified and needs to be saved, modifications
		 * are not reported to the tour data editor, saving needs also to be asynch with the tour
		 * data editor
		 */
		Display.getCurrent().asyncExec(new Runnable() {
			public void run() {

				if (fPageBook.isDisposed()) {
					return;
				}

				TourData nextTourData = null;

				if (selection instanceof SelectionActiveEditor) {

					final IEditorPart editorPart = ((SelectionActiveEditor) selection).getEditor();
					if (editorPart instanceof TourEditor) {
						nextTourData = ((TourEditor) editorPart).getTourChart().getTourData();
					}

				} else if (selection instanceof SelectionTourData) {

					nextTourData = ((SelectionTourData) selection).getTourData();

				} else if (selection instanceof SelectionTourId) {

					final SelectionTourId tourIdSelection = (SelectionTourId) selection;

					if (fTourData != null) {
						if (fTourData.getTourId().equals(tourIdSelection.getTourId())) {
							// don't reload the same tour
							return;
						}
					}

					nextTourData = TourManager.getInstance().getTourData(tourIdSelection.getTourId());

				} else {
					return;
				}

				if (validateSelectedTour(nextTourData) == false) {
					return;
				}

				/*
				 * save previous tour when a new tour is selected
				 */
				if (fTourData != null && fTourData.getTourId() == nextTourData.getTourId()) {

					// nothing to do, it's the same tour

				} else {

//					saveTour();

					setTour(nextTourData);
				}
			}
		});
	}

	private TourData saveTour() {

		if (fIsTourDirty == false || fTourData == null) {
			// nothing to do
			return null;
		}

		TourData savedTour = null;
		fIsSaving = true;
		try {
			savedTour = TourManager.saveModifiedTour(fTourData);
		} catch (final Exception e) {

		} finally {
			fIsSaving = false;
		}

		fIsTourDirty = false;

		return savedTour;
	}

	@Override
	public void setFocus() {
		fPageBook.setFocus();
	}

	/**
	 * check if data for the segmenter is valid
	 */
	private boolean validateSelectedTour(final TourData tourData) {

		if (tourData == null) {

			fTourData = null;
			fPageBook.showPage(fPageNoTour);

			return false;

		} else if (fTourData != null && fTourData.getTourId() == tourData.getTourId()) {

			// nothing to do, it's the same tour

			return false;

		} else if (tourData.getMergeFromTourId() == null) {

			fTourData = null;
			fPageBook.showPage(fPageInvalidData);

			return false;

		} else {

			// fTourData is set later

			fPageBook.showPage(fPageTourMerger);

			return true;
		}

	}

}
