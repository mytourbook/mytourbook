/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourCatalog;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tag.ActionRemoveAllTags;
import net.tourbook.tag.ActionSetTourTag;
import net.tourbook.tag.TagManager;
import net.tourbook.tour.ActionEditQuick;
import net.tourbook.tour.ITourPropertyListener;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TreeViewerItem;
import net.tourbook.ui.ActionCollapseAll;
import net.tourbook.ui.ActionEditTour;
import net.tourbook.ui.ActionModifyColumns;
import net.tourbook.ui.ActionOpenPrefDialog;
import net.tourbook.ui.ActionSetTourType;
import net.tourbook.ui.ColumnManager;
import net.tourbook.ui.ISelectedTours;
import net.tourbook.ui.ITourViewer;
import net.tourbook.ui.TreeColumnDefinition;
import net.tourbook.ui.TreeColumnFactory;
import net.tourbook.ui.UI;
import net.tourbook.util.PixelConverter;
import net.tourbook.util.PostSelectionProvider;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.part.ViewPart;

public class TourCompareResultView extends ViewPart implements ITourViewer, ISelectedTours {

	public static final String					ID					= "net.tourbook.views.tourCatalog.CompareResultView";	//$NON-NLS-1$

	/**
	 * This memento allows this view to save and restore state when it is closed and opened within a
	 * session. A different memento is supplied by the platform for persistance at workbench
	 * shutdown.
	 */
	private static IMemento						fSessionMemento		= null;

	private Composite							fViewerContainer;
	private CheckboxTreeViewer					fTourViewer;
	private TVICompareResultRootItem			fRootItem;

	private ISelectionListener					fPostSelectionListener;
	private IPartListener2						fPartListener;
	private IPropertyChangeListener				fPrefChangeListener;
	private ITourPropertyListener				fTourPropertyListener;
	private PostSelectionProvider				fPostSelectionProvider;

	private ActionSaveComparedTours				fActionSaveComparedTours;
	private ActionRemoveComparedTourSaveStatus	fActionRemoveComparedTourSaveStatus;
	private ActionCheckTours					fActionCheckTours;
	private ActionUncheckTours					fActionUncheckTours;
	private ActionModifyColumns					fActionModifyColumns;
	private ActionCollapseAll					fActionCollapseAll;
	private ActionSetTourTag					fActionAddTag;
	private ActionSetTourTag					fActionRemoveTag;
	private ActionRemoveAllTags					fActionRemoveAllTags;
	private ActionOpenPrefDialog				fActionOpenTagPrefs;
	private ActionEditQuick						fActionEditQuick;
	private ActionEditTour						fActionEditTour;
	private ActionSetTourType					fActionSetTourType;

	private boolean								fIsToolbarCreated;

	/**
	 * resource manager for images
	 */
	private Image								dbImage				= TourbookPlugin.getImageDescriptor(Messages.Image__database)
																			.createImage(true);

	private final NumberFormat					nf					= NumberFormat.getNumberInstance();

	private ITourPropertyListener				fCompareTourPropertyListener;

	private ColumnManager						fColumnManager;

	SelectionRemovedComparedTours				fOldRemoveSelection	= null;

	class ResultContentProvider implements ITreeContentProvider {

		public void dispose() {}

		public Object[] getChildren(final Object parentElement) {
			return ((TreeViewerItem) parentElement).getFetchedChildrenAsArray();
		}

		public Object[] getElements(final Object inputElement) {
			return fRootItem.getFetchedChildrenAsArray();
		}

		public Object getParent(final Object element) {
			return ((TreeViewerItem) element).getParentItem();
		}

		public boolean hasChildren(final Object element) {
			return ((TreeViewerItem) element).hasChildren();
		}

		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	public TourCompareResultView() {}

	private void addCompareTourPropertyListener() {

		fCompareTourPropertyListener = new ITourPropertyListener() {
			public void propertyChanged(final int propertyId, final Object propertyData) {

				if (propertyId == TourManager.TOUR_PROPERTY_COMPARE_TOUR_CHANGED
						&& propertyData instanceof TourPropertyCompareTourChanged) {

					final TourPropertyCompareTourChanged compareTourProperty = (TourPropertyCompareTourChanged) propertyData;

					final long compareId = compareTourProperty.compareId;

					final ArrayList<Long> compareIds = new ArrayList<Long>();
					compareIds.add(compareId);

					if (compareId == -1) {

						// compare result is not saved

						final Object comparedTourItem = compareTourProperty.comparedTourItem;

						if (comparedTourItem instanceof TVICompareResultComparedTour) {
							final TVICompareResultComparedTour resultItem = (TVICompareResultComparedTour) comparedTourItem;

							resultItem.movedStartIndex = compareTourProperty.startIndex;
							resultItem.movedEndIndex = compareTourProperty.endIndex;
							resultItem.movedSpeed = compareTourProperty.speed;

							// update viewer
							fTourViewer.update(comparedTourItem, null);
						}

					} else {

						// compare result is saved

						// find compared tour in the viewer
						final ArrayList<TVICompareResultComparedTour> comparedTours = new ArrayList<TVICompareResultComparedTour>();
						getComparedTours(comparedTours, fRootItem, compareIds);

						if (comparedTours.size() > 0) {

							final TVICompareResultComparedTour compareTourItem = comparedTours.get(0);

							if (compareTourProperty.isDataSaved) {

								// compared tour was saved

								compareTourItem.dbStartIndex = compareTourProperty.startIndex;
								compareTourItem.dbEndIndex = compareTourProperty.endIndex;
								compareTourItem.dbSpeed = compareTourProperty.speed;

							} else {

								compareTourItem.movedStartIndex = compareTourProperty.startIndex;
								compareTourItem.movedEndIndex = compareTourProperty.endIndex;
								compareTourItem.movedSpeed = compareTourProperty.speed;
							}

							// update viewer
							fTourViewer.update(compareTourItem, null);
						}
					}
				}
			}
		};

		TourManager.getInstance().addPropertyListener(fCompareTourPropertyListener);
	}

	/**
	 * set the part listener to save the view settings, the listeners are called before the controls
	 * are disposed
	 */
	private void addPartListeners() {

		fPartListener = new IPartListener2() {

			public void partActivated(final IWorkbenchPartReference partRef) {}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {
				if (ID.equals(partRef.getId())) {
					saveSettings();
				}
			}

			public void partDeactivated(final IWorkbenchPartReference partRef) {
				if (ID.equals(partRef.getId())) {
					saveSettings();
				}
			}

			public void partHidden(final IWorkbenchPartReference partRef) {}

			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			public void partOpened(final IWorkbenchPartReference partRef) {
				/*
				 * add the actions in the part open event so they are appended AFTER the actions
				 * which are defined in the plugin.xml
				 */
				fillToolbar();
			}

			public void partVisible(final IWorkbenchPartReference partRef) {}
		};

		getViewSite().getPage().addPartListener(fPartListener);
	}

	private void addPrefListener() {

		final Preferences prefStore = TourbookPlugin.getDefault().getPluginPreferences();

		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(final Preferences.PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// measurement system has changed

					UI.updateUnits();

					fColumnManager.saveState(fSessionMemento);

					fColumnManager.clearColumns();
					defineViewerColumns(fViewerContainer);

					recreateViewer();

				} else if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

					fTourViewer.getTree()
							.setLinesVisible(prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

					fTourViewer.refresh();

					/*
					 * the tree must be redrawn because the styled text does not show with the new
					 * color
					 */
					fTourViewer.getTree().redraw();
				}
			}
		};
		prefStore.addPropertyChangeListener(fPrefChangeListener);
	}

	/**
	 * Listen to post selections
	 */
	private void addSelectionListeners() {

		fPostSelectionListener = new ISelectionListener() {

			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

				if (selection instanceof SelectionRemovedComparedTours) {

					removeComparedToursFromViewer(selection);

				} else if (selection instanceof SelectionPersistedCompareResults) {

					final SelectionPersistedCompareResults selectionPersisted = (SelectionPersistedCompareResults) selection;

					final ArrayList<TVICompareResultComparedTour> persistedCompareResults = selectionPersisted.persistedCompareResults;

					if (persistedCompareResults.size() > 0) {

						final TVICompareResultComparedTour comparedTourItem = persistedCompareResults.get(0);

						// uncheck persisted tours
						fTourViewer.setChecked(comparedTourItem, false);

						// update changed item
						fTourViewer.update(comparedTourItem, null);

					}
				}
			}
		};

		// register selection listener in the page
		getSite().getPage().addPostSelectionListener(fPostSelectionListener);

	}

	private void addTourPropertyListener() {

		fTourPropertyListener = new ITourPropertyListener() {
			@SuppressWarnings("unchecked")
			public void propertyChanged(final int propertyId, final Object propertyData) {
				if (propertyId == TourManager.TOUR_PROPERTIES_CHANGED) {

					// get a clone of the modified tours because the tours are removed from the list
					final ArrayList<TourData> modifiedTours = (ArrayList<TourData>) ((ArrayList<TourData>) propertyData).clone();

					updateTourViewer(fRootItem, modifiedTours);

				} else if (propertyId == TourManager.TAG_STRUCTURE_CHANGED) {

					reloadViewer();
				}
			}
		};
		TourManager.getInstance().addPropertyListener(fTourPropertyListener);
	}

	private void createActions() {

		fActionSaveComparedTours = new ActionSaveComparedTours(this);
		fActionRemoveComparedTourSaveStatus = new ActionRemoveComparedTourSaveStatus(this);

		fActionCheckTours = new ActionCheckTours(this);
		fActionUncheckTours = new ActionUncheckTours(this);

		fActionAddTag = new ActionSetTourTag(this, true);
		fActionRemoveTag = new ActionSetTourTag(this, false);
		fActionRemoveAllTags = new ActionRemoveAllTags(this);

		fActionOpenTagPrefs = new ActionOpenPrefDialog(Messages.action_tag_open_tagging_structure,
				ITourbookPreferences.PREF_PAGE_TAGS);

		fActionEditQuick = new ActionEditQuick(this);
		fActionEditTour = new ActionEditTour(this);
		fActionSetTourType = new ActionSetTourType(this);

		fActionModifyColumns = new ActionModifyColumns(this);
		fActionCollapseAll = new ActionCollapseAll(this);
	}

	/**
	 * create the views context menu
	 */
	private void createContextMenu() {

		final MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(final IMenuManager manager) {
				TourCompareResultView.this.fillContextMenu(manager);
			}
		});

		// add the context menu to the table viewer
		final Control tourViewer = fTourViewer.getControl();
		final Menu menu = menuMgr.createContextMenu(tourViewer);
		tourViewer.setMenu(menu);
	}

	@Override
	public void createPartControl(final Composite parent) {

		// define all columns for the viewer
		fColumnManager = new ColumnManager(this, fSessionMemento);
		defineViewerColumns(parent);

		fViewerContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(fViewerContainer);

		createTourViewer(fViewerContainer);

		addPartListeners();
		addSelectionListeners();
		addCompareTourPropertyListener();
		addPrefListener();
		addTourPropertyListener();

		createActions();
		fillViewMenu();

		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());

		fTourViewer.setInput(fRootItem = new TVICompareResultRootItem());

		restoreState(fSessionMemento);
	}

	private Control createTourViewer(final Composite parent) {

		// tour tree
		final Tree tree = new Tree(parent, SWT.H_SCROLL
				| SWT.V_SCROLL
				| SWT.BORDER
				| SWT.MULTI
				| SWT.FULL_SELECTION
				| SWT.CHECK);

		GridDataFactory.fillDefaults().grab(true, true).applyTo(tree);

		tree.setHeaderVisible(true);
		tree.setLinesVisible(TourbookPlugin.getDefault()
				.getPluginPreferences()
				.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

		fTourViewer = new ContainerCheckedTreeViewer(tree);
		fColumnManager.createColumns();

		fTourViewer.setContentProvider(new ResultContentProvider());
		fTourViewer.setUseHashlookup(true);

		fTourViewer.setSorter(new ViewerSorter() {
			@Override
			public int compare(final Viewer viewer, final Object obj1, final Object obj2) {

				if (obj1 instanceof TVICompareResultComparedTour) {
					return ((TVICompareResultComparedTour) obj1).minAltitudeDiff
							- ((TVICompareResultComparedTour) obj2).minAltitudeDiff;
				}

				return 0;
			}
		});

		fTourViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				onSelectionChanged(event);
			}
		});

		fTourViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(final DoubleClickEvent event) {

				// expand/collapse current item

				final Object treeItem = ((IStructuredSelection) event.getSelection()).getFirstElement();

				if (fTourViewer.getExpandedState(treeItem)) {
					fTourViewer.collapseToLevel(treeItem, 1);
				} else {
					fTourViewer.expandToLevel(treeItem, 1);
				}
			}
		});

		fTourViewer.getTree().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(final KeyEvent keyEvent) {
				if (keyEvent.keyCode == SWT.DEL) {
					removeComparedTourFromDb();
				}
			}
		});

		fTourViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(final CheckStateChangedEvent event) {

				if (event.getElement() instanceof TVICompareResultComparedTour) {
					final TVICompareResultComparedTour compareResult = (TVICompareResultComparedTour) event.getElement();
					if (event.getChecked() && compareResult.isSaved()) {
						/*
						 * uncheck elements which are already stored for the reftour, it would be
						 * better to disable them, but this is not possible because this is a
						 * limitation by the OS
						 */
						fTourViewer.setChecked(compareResult, false);
					} else {
						enableActions();
					}
				} else {
					// uncheck all other tree items
					fTourViewer.setChecked(event.getElement(), false);
				}
			}
		});

		createContextMenu();

		return tree;
	}

	private void defineViewerColumns(final Composite parent) {

		final PixelConverter pixelConverter = new PixelConverter(parent);
		TreeColumnDefinition colDef;

		/*
		 * tree column: reference tour/date
		 */
		colDef = new TreeColumnDefinition("comparedTour", SWT.LEAD); //$NON-NLS-1$
		fColumnManager.addColumn(colDef);

		colDef.setColumnLabel(Messages.Compare_Result_Column_tour);
		colDef.setColumnText(Messages.Compare_Result_Column_tour);
		colDef.setColumnWidth(pixelConverter.convertWidthInCharsToPixels(25) + 16);
		colDef.setCanModifyVisibility(false);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();

				if (element instanceof TVICompareResultReferenceTour) {

					final TVICompareResultReferenceTour refItem = (TVICompareResultReferenceTour) element;
					cell.setText(refItem.label);

				} else if (element instanceof TVICompareResultComparedTour) {

					final TVICompareResultComparedTour compareItem = (TVICompareResultComparedTour) element;
					cell.setText(TourManager.getTourDate(compareItem.comparedTourData));

					// display an image when a tour is saved
					if (compareItem.isSaved()) {
						cell.setImage(dbImage);
					} else {
						cell.setImage(null);
					}
				}

				setCellColor(cell, element);
			}
		});

		/*
		 * column: altitude difference
		 */
		colDef = new TreeColumnDefinition("diff", SWT.TRAIL); //$NON-NLS-1$
		fColumnManager.addColumn(colDef);

		colDef.setColumnText(Messages.Compare_Result_Column_diff);
		colDef.setColumnToolTipText(Messages.Compare_Result_Column_diff_tooltip);
		colDef.setColumnLabel(Messages.Compare_Result_Column_diff_label);
		colDef.setColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVICompareResultComparedTour) {

					final TVICompareResultComparedTour compareItem = (TVICompareResultComparedTour) element;

					cell.setText(Integer.toString((compareItem.minAltitudeDiff * 100)
							/ (compareItem.normalizedEndIndex - compareItem.normalizedStartIndex)));

					setCellColor(cell, element);
				}
			}
		});

		/*
		 * column: speed computed
		 */
		colDef = new TreeColumnDefinition("speedComputed", SWT.TRAIL); //$NON-NLS-1$
		fColumnManager.addColumn(colDef);

		colDef.setColumnText(UI.UNIT_LABEL_SPEED);
		colDef.setColumnToolTipText(Messages.Compare_Result_Column_kmh_tooltip);
		colDef.setColumnLabel(Messages.Compare_Result_Column_kmh_label);
		colDef.setColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVICompareResultComparedTour) {

					final TVICompareResultComparedTour compareItem = (TVICompareResultComparedTour) element;

					nf.setMinimumFractionDigits(1);
					nf.setMaximumFractionDigits(1);
					cell.setText(nf.format(compareItem.compareSpeed / UI.UNIT_VALUE_DISTANCE));
					setCellColor(cell, element);
				}
			}
		});

		/*
		 * column: speed saved
		 */
		colDef = new TreeColumnDefinition("speedSaved", SWT.TRAIL); //$NON-NLS-1$
		fColumnManager.addColumn(colDef);

		colDef.setColumnText(UI.UNIT_LABEL_SPEED);
		colDef.setColumnToolTipText(Messages.Compare_Result_Column_kmh_db_tooltip);
		colDef.setColumnLabel(Messages.Compare_Result_Column_kmh_db_label);
		colDef.setColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVICompareResultComparedTour) {

					final TVICompareResultComparedTour compareItem = (TVICompareResultComparedTour) element;

					nf.setMinimumFractionDigits(1);
					nf.setMaximumFractionDigits(1);
					cell.setText(nf.format(compareItem.dbSpeed / UI.UNIT_VALUE_DISTANCE));
					setCellColor(cell, element);
				}
			}
		});

		/*
		 * column: speed moved
		 */
		colDef = new TreeColumnDefinition("speedMoved", SWT.TRAIL); //$NON-NLS-1$
		fColumnManager.addColumn(colDef);

		colDef.setColumnText(UI.UNIT_LABEL_SPEED);
		colDef.setColumnToolTipText(Messages.Compare_Result_Column_kmh_moved_tooltip);
		colDef.setColumnLabel(Messages.Compare_Result_Column_kmh_moved_label);
		colDef.setColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVICompareResultComparedTour) {

					final TVICompareResultComparedTour compareItem = (TVICompareResultComparedTour) element;

					nf.setMinimumFractionDigits(1);
					nf.setMaximumFractionDigits(1);
					cell.setText(nf.format(compareItem.movedSpeed / UI.UNIT_VALUE_DISTANCE));
					setCellColor(cell, element);
				}
			}
		});

		/*
		 * column: distance
		 */
		colDef = TreeColumnFactory.DISTANCE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVICompareResultComparedTour) {

					final TVICompareResultComparedTour compareItem = (TVICompareResultComparedTour) element;

					nf.setMinimumFractionDigits(2);
					nf.setMaximumFractionDigits(2);
					cell.setText(nf.format(compareItem.compareDistance / (1000 * UI.UNIT_VALUE_DISTANCE)));
					setCellColor(cell, element);
				}
			}
		});

		/*
		 * column: time interval
		 */
		colDef = TreeColumnFactory.TIME_INTERVAL.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVICompareResultComparedTour) {

					cell.setText(Integer.toString(((TVICompareResultComparedTour) element).timeIntervall));
					setCellColor(cell, element);
				}
			}
		});

		/*
		 * column: tour type
		 */
		colDef = TreeColumnFactory.TOUR_TYPE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVICompareResultComparedTour) {
					final TourData comparedTourData = ((TVICompareResultComparedTour) element).comparedTourData;
					final TourType tourType = comparedTourData.getTourType();
					if (tourType != null) {
						cell.setImage(UI.getInstance().getTourTypeImage(tourType.getTypeId()));
					}
				}
			}
		});

		/*
		 * column: title
		 */
		colDef = TreeColumnFactory.TITLE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVICompareResultComparedTour) {
					cell.setText(((TVICompareResultComparedTour) element).comparedTourData.getTourTitle());
					setCellColor(cell, element);
				}
			}
		});

		/*
		 * column: tags
		 */
		colDef = TreeColumnFactory.TOUR_TAGS.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVICompareResultComparedTour) {

					final Set<TourTag> tourTags = ((TVICompareResultComparedTour) element).comparedTourData.getTourTags();
					if (tourTags.size() == 0) {

						// the tags could have been removed, set empty field

						cell.setText(UI.EMPTY_STRING);

					} else {
						// convert the tags into a list of tag ids 
						final ArrayList<Long> tagIds = new ArrayList<Long>();
						for (final TourTag tourTag : tourTags) {
							tagIds.add(tourTag.getTagId());
						}

						cell.setText(TourDatabase.getTagNames(tagIds));
						setCellColor(cell, element);
					}
				}
			}
		});

	}

	@Override
	public void dispose() {

		getSite().getPage().removePostSelectionListener(fPostSelectionListener);
		getSite().getPage().removePartListener(fPartListener);
		TourManager.getInstance().removePropertyListener(fCompareTourPropertyListener);
		TourbookPlugin.getDefault().getPluginPreferences().removePropertyChangeListener(fPrefChangeListener);
		TourManager.getInstance().removePropertyListener(fTourPropertyListener);

		dbImage.dispose();

		super.dispose();
	}

	private void enableActions() {

		final ITreeSelection selection = (ITreeSelection) fTourViewer.getSelection();

		int tourItems = 0;
		int otherItems = 0;
		int savedTourItems = 0;
		int unsavedTourItems = 0;
		TVICompareResultComparedTour firstTourItem = null;

		/*
		 * count selected items
		 */
		int selectedTours = 0;
		for (final Iterator<?> iter = selection.iterator(); iter.hasNext();) {

			final Object treeItem = iter.next();

			if (treeItem instanceof TVICompareResultComparedTour) {
				final TVICompareResultComparedTour comparedTourItem = (TVICompareResultComparedTour) treeItem;

				// count tours
				if (tourItems == 0) {
					firstTourItem = comparedTourItem;
				}
				tourItems++;

				// count saved tours
				if (comparedTourItem.isSaved()) {
					savedTourItems++;
				} else {
					unsavedTourItems++;
				}

				selectedTours++;

			} else {
				otherItems++;
			}
		}

		/*
		 * count checked items
		 */
		int checkedTours = 0;
		for (final Object checkedElement : fTourViewer.getCheckedElements()) {

			if (checkedElement instanceof TVICompareResultComparedTour) {
				final TVICompareResultComparedTour comparedTourItem = (TVICompareResultComparedTour) checkedElement;

				// count tours
				if (tourItems == 0) {
					firstTourItem = comparedTourItem;
				}
				tourItems++;

				// count saved tours
				if (comparedTourItem.isSaved()) {
					savedTourItems++;
				} else {
					unsavedTourItems++;
				}

				checkedTours++;
			}
		}

		final boolean isTourSelected = tourItems > 0 && otherItems == 0;
		final boolean isOneTour = tourItems == 1 && otherItems == 0;
		final boolean isOneTourSelected = selectedTours == 1;

		fActionCheckTours.setEnabled(unsavedTourItems > 0);
		fActionUncheckTours.setEnabled(checkedTours > 0);

		// action: save compare result
		fActionSaveComparedTours.setEnabled(unsavedTourItems > 0);

		// action: remove tour from saved compare result, currently only one tour item is supported
		fActionRemoveComparedTourSaveStatus.setEnabled(savedTourItems > 0);

		/*
		 * add/remove/remove all
		 */
		fActionAddTag.setEnabled(isTourSelected);

		if (isOneTour) {

			// one tour is selected

			final Set<TourTag> tourTags = firstTourItem.comparedTourData.getTourTags();
			if (tourTags != null && tourTags.size() > 0) {

				// at least one tag is within the tour

				fActionRemoveAllTags.setEnabled(true);
				fActionRemoveTag.setEnabled(true);
			} else {
				// tags are not available
				fActionRemoveAllTags.setEnabled(false);
				fActionRemoveTag.setEnabled(false);
			}
		} else {

			// multiple tours are selected

			fActionRemoveTag.setEnabled(isTourSelected);
			fActionRemoveAllTags.setEnabled(isTourSelected);
		}

		// action: recent tags
		TagManager.enableRecentTagActions(isTourSelected);

		// actions: edit tour
		fActionEditTour.setEnabled(isOneTourSelected);
		fActionEditQuick.setEnabled(isOneTourSelected);

		// action: tour type
		final ArrayList<TourType> tourTypes = TourDatabase.getTourTypes();
		fActionSetTourType.setEnabled(isTourSelected && tourTypes.size() > 0);
	}

	private void fillContextMenu(final IMenuManager menuMgr) {

		menuMgr.add(fActionSaveComparedTours);
		menuMgr.add(fActionRemoveComparedTourSaveStatus);
		menuMgr.add(fActionCheckTours);
		menuMgr.add(fActionUncheckTours);

		menuMgr.add(new Separator());
		menuMgr.add(fActionAddTag);
		menuMgr.add(fActionRemoveTag);
		menuMgr.add(fActionRemoveAllTags);

		TagManager.fillRecentTagsIntoMenu(menuMgr, this, true);

		menuMgr.add(new Separator());
		menuMgr.add(fActionOpenTagPrefs);

		menuMgr.add(new Separator());
		menuMgr.add(fActionEditQuick);
		menuMgr.add(fActionSetTourType);
		menuMgr.add(fActionEditTour);

		enableActions();
	}

	private void fillToolbar() {

		// check if toolbar is created
		if (fIsToolbarCreated) {
			return;
		}
		fIsToolbarCreated = true;

		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

		tbm.add(fActionCollapseAll);

		tbm.update(true);
	}

	private void fillViewMenu() {

		/*
		 * fill view menu
		 */
		final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();
		menuMgr.add(fActionModifyColumns);
	}

	public ColumnManager getColumnManager() {
		return fColumnManager;
	}

	/**
	 * Recursive method to walk down the tour tree items and find the compared tours
	 * 
	 * @param parentItem
	 * @param CompareIds
	 */
	private void getComparedTours(	final ArrayList<TVICompareResultComparedTour> comparedTours,
									final TreeViewerItem parentItem,
									final ArrayList<Long> CompareIds) {

		final ArrayList<TreeViewerItem> unfetchedChildren = parentItem.getUnfetchedChildren();

		if (unfetchedChildren != null) {

			// children are available

			for (final TreeViewerItem treeItem : unfetchedChildren) {

				if (treeItem instanceof TVICompareResultComparedTour) {
					final TVICompareResultComparedTour ttiCompResult = (TVICompareResultComparedTour) treeItem;
					final long compId = ttiCompResult.compId;
					for (final Long removedCompId : CompareIds) {
						if (compId == removedCompId) {
							comparedTours.add(ttiCompResult);
						}
					}
				} else {
					// this is a child which can be the parent for other childs
					getComparedTours(comparedTours, treeItem, CompareIds);
				}
			}
		}
	}

	public ArrayList<TourData> getSelectedTours() {

		// get selected tours

		final IStructuredSelection selectedTours = ((IStructuredSelection) fTourViewer.getSelection());
		final ArrayList<TourData> selectedTourData = new ArrayList<TourData>();

		// loop: all selected tours
		for (final Iterator<?> iter = selectedTours.iterator(); iter.hasNext();) {

			final Object treeItem = iter.next();

			if (treeItem instanceof TVICompareResultComparedTour) {

				final TVICompareResultComparedTour compareItem = ((TVICompareResultComparedTour) treeItem);
				final TourData tourData = TourManager.getInstance()
						.getTourData(compareItem.comparedTourData.getTourId());
				if (tourData != null) {
					selectedTourData.add(tourData);
				}
			}
		}

		return selectedTourData;
	}

	/**
	 * @return Returns the tour viewer
	 */
	public CheckboxTreeViewer getViewer() {
		return fTourViewer;
	}

	@Override
	public void init(final IViewSite site, final IMemento memento) throws PartInitException {
		super.init(site, memento);

		// set the session memento if it's net yet set
		if (fSessionMemento == null) {
			fSessionMemento = memento;
		}
	}

	public boolean isFromTourEditor() {
		return false;
	}

	private void onSelectionChanged(final SelectionChangedEvent event) {

		final IStructuredSelection selection = (IStructuredSelection) event.getSelection();

		final Object treeItem = selection.getFirstElement();

		if (treeItem instanceof TVICompareResultReferenceTour) {

			final TVICompareResultReferenceTour refItem = (TVICompareResultReferenceTour) treeItem;

			fPostSelectionProvider.setSelection(new SelectionTourCatalogView(refItem.refTour.getRefId()));

		} else if (treeItem instanceof TVICompareResultComparedTour) {

			final TVICompareResultComparedTour resultItem = (TVICompareResultComparedTour) treeItem;

			fPostSelectionProvider.setSelection(new StructuredSelection(resultItem));
		}
	}

	public void recreateViewer() {

		fViewerContainer.setRedraw(false);
		{
			final Object[] expandedElements = fTourViewer.getExpandedElements();
			final ISelection selection = fTourViewer.getSelection();

			fTourViewer.getTree().dispose();

			createTourViewer(fViewerContainer);
			fViewerContainer.layout();

			fTourViewer.setInput(fRootItem = new TVICompareResultRootItem());

			fTourViewer.setExpandedElements(expandedElements);
			fTourViewer.setSelection(selection);
		}
		fViewerContainer.setRedraw(true);
	}

	public void reloadViewer() {

		final Tree tree = fTourViewer.getTree();
		tree.setRedraw(false);
		{
			final Object[] expandedElements = fTourViewer.getExpandedElements();
			final ISelection selection = fTourViewer.getSelection();

			fTourViewer.setInput(fRootItem = new TVICompareResultRootItem());

			fTourViewer.setExpandedElements(expandedElements);
			fTourViewer.setSelection(selection);
		}
		tree.setRedraw(true);
	}

	/**
	 * Remove compared tour from the database
	 */
	void removeComparedTourFromDb() {

		final StructuredSelection selection = (StructuredSelection) fTourViewer.getSelection();
		final SelectionRemovedComparedTours selectionRemovedCompareTours = new SelectionRemovedComparedTours();
		final ArrayList<Long> removedComparedTours = selectionRemovedCompareTours.removedComparedTours;

		for (final Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {

			final Object selectedElement = iterator.next();

			if (selectedElement instanceof TVICompareResultComparedTour) {
				final TVICompareResultComparedTour compareItem = (TVICompareResultComparedTour) selectedElement;

				if (TourCompareManager.removeComparedTourFromDb(compareItem.compId)) {
					removedComparedTours.add(compareItem.compId);
				}
			}
		}

		if (removedComparedTours.size() > 0) {
			// this viewer is also updated by the remove selection
			fPostSelectionProvider.setSelection(selectionRemovedCompareTours);
		}

	}

	private void removeComparedToursFromViewer(final ISelection selection) {

		final SelectionRemovedComparedTours removedTourSelection = (SelectionRemovedComparedTours) selection;
		final ArrayList<Long> removedTourCompareIds = removedTourSelection.removedComparedTours;

		/*
		 * return when there are no removed tours or when the selection has not changed
		 */
		if (removedTourCompareIds.size() == 0 || removedTourSelection == fOldRemoveSelection) {
			return;
		}

		fOldRemoveSelection = removedTourSelection;

		/*
		 * find/update the removed compared tours in the viewer
		 */

		final ArrayList<TVICompareResultComparedTour> comparedTourItems = new ArrayList<TVICompareResultComparedTour>();
		getComparedTours(comparedTourItems, fRootItem, removedTourCompareIds);

		// reset entity for the removed compared tours
		for (final TVICompareResultComparedTour removedTourItem : comparedTourItems) {

			removedTourItem.compId = -1;

			removedTourItem.dbStartIndex = -1;
			removedTourItem.dbEndIndex = -1;
			removedTourItem.dbSpeed = 0;

			removedTourItem.movedStartIndex = -1;
			removedTourItem.movedEndIndex = -1;
			removedTourItem.movedSpeed = 0;
		}

		// update viewer
		fTourViewer.update(comparedTourItems.toArray(), null);
	}

	private void restoreState(final IMemento memento) {

		if (memento != null) {

		}
	}

	/**
	 * Persist the compared tours which are checked or selected
	 */
	@SuppressWarnings("unchecked")
	void saveCompareResults() {

		final EntityManager em = TourDatabase.getInstance().getEntityManager();
		if (em != null) {

			final EntityTransaction ts = em.getTransaction();

			try {

				final ArrayList<TVICompareResultComparedTour> updatedItems = new ArrayList<TVICompareResultComparedTour>();
				final SelectionPersistedCompareResults compareResultSelection = new SelectionPersistedCompareResults();
				final ArrayList<TVICompareResultComparedTour> persistedCompareResults = compareResultSelection.persistedCompareResults;

				/*
				 * save checked items
				 */
				for (final Object checkedItem : fTourViewer.getCheckedElements()) {
					if (checkedItem instanceof TVICompareResultComparedTour) {

						final TVICompareResultComparedTour checkedCompareItem = (TVICompareResultComparedTour) checkedItem;
						if (checkedCompareItem.isSaved() == false) {
							TourCompareManager.saveComparedTourItem(checkedCompareItem, em, ts);

							persistedCompareResults.add(checkedCompareItem);

							updatedItems.add(checkedCompareItem);
						}
					}
				}

				/*
				 * save selected items which are not checked
				 */
				final TreeSelection selection = (TreeSelection) fTourViewer.getSelection();
				for (final Iterator<Object> iterator = selection.iterator(); iterator.hasNext();) {

					final Object treeItem = iterator.next();
					if (treeItem instanceof TVICompareResultComparedTour) {

						final TVICompareResultComparedTour selectedComparedItem = (TVICompareResultComparedTour) treeItem;
						if (selectedComparedItem.isSaved() == false) {

							TourCompareManager.saveComparedTourItem(selectedComparedItem, em, ts);

							persistedCompareResults.add(selectedComparedItem);

							updatedItems.add(selectedComparedItem);
						}
					}
				}

				// uncheck all
				fTourViewer.setCheckedElements(new Object[0]);

				// update persistent status
				fTourViewer.update(updatedItems.toArray(), null);

				// fire post selection to update the tour catalog view
				fPostSelectionProvider.setSelection(compareResultSelection);

			} catch (final Exception e) {
				e.printStackTrace();
			} finally {
				if (ts.isActive()) {
					ts.rollback();
				}
				em.close();
			}
		}
	}

	private void saveSettings() {
		fSessionMemento = XMLMemento.createWriteRoot("CompareResultView"); //$NON-NLS-1$
		saveState(fSessionMemento);
	}

	@Override
	public void saveState(final IMemento memento) {

		fColumnManager.saveState(memento);
	}

	private void setCellColor(final ViewerCell cell, final Object element) {

		if (element instanceof TVICompareResultReferenceTour) {

			cell.setForeground(JFaceResources.getColorRegistry().get(UI.VIEW_COLOR_TITLE));

		} else if (element instanceof TVICompareResultComparedTour) {

			// show the saved tours in a different color

			if (((TVICompareResultComparedTour) (element)).isSaved()) {
				cell.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_GRAY));
			} else {
				// show the text with tour color
				cell.setForeground(JFaceResources.getColorRegistry().get(UI.VIEW_COLOR_TOUR));
//				cell.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));
			}
		}
	}

	@Override
	public void setFocus() {
		fTourViewer.getControl().setFocus();
	}

	/**
	 * !!!Recursive !!! update all tour items with new data
	 * 
	 * @param rootItem
	 * @param modifiedTours
	 */
	private void updateTourViewer(final TreeViewerItem parentItem, final ArrayList<TourData> modifiedTours) {

		final ArrayList<TreeViewerItem> children = parentItem.getUnfetchedChildren();

		if (children == null) {
			return;
		}

		// loop: all children
		for (final Object object : children) {

			final TreeViewerItem treeItem = (TreeViewerItem) object;

			if (object instanceof TVICompareResultComparedTour) {

				// update compared items

				final TVICompareResultComparedTour compareItem = (TVICompareResultComparedTour) treeItem;
				final TourData comparedTourData = compareItem.comparedTourData;
				final long tourItemId = comparedTourData.getTourId();

				for (final TourData modifiedTourData : modifiedTours) {
					if (modifiedTourData.getTourId().longValue() == tourItemId) {

						comparedTourData.setTourType(modifiedTourData.getTourType());
						comparedTourData.setTourTitle(modifiedTourData.getTourTitle());
						comparedTourData.setTourTags(modifiedTourData.getTourTags());

						// update item in the viewer
						fTourViewer.update(compareItem, null);

						break;
					}
				}

			} else {

				// update children

				updateTourViewer(treeItem, modifiedTours);
			}
		}
	}

}
