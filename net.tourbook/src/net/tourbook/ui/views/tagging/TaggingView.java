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
package net.tourbook.ui.views.tagging;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageAppearanceView;
import net.tourbook.tag.ActionMenuSetAllTagStructures;
import net.tourbook.tag.ActionMenuSetTagStructure;
import net.tourbook.tag.ActionRemoveAllTags;
import net.tourbook.tag.ActionRenameTag;
import net.tourbook.tag.ActionSetTourTag;
import net.tourbook.tag.ChangedTags;
import net.tourbook.tag.TagManager;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.ITourItem;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ColumnManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.ITourViewer;
import net.tourbook.ui.TreeColumnDefinition;
import net.tourbook.ui.TreeColumnFactory;
import net.tourbook.ui.TreeViewerItem;
import net.tourbook.ui.UI;
import net.tourbook.ui.action.ActionCollapseAll;
import net.tourbook.ui.action.ActionCollapseOthers;
import net.tourbook.ui.action.ActionEditQuick;
import net.tourbook.ui.action.ActionEditTour;
import net.tourbook.ui.action.ActionExpandSelection;
import net.tourbook.ui.action.ActionModifyColumns;
import net.tourbook.ui.action.ActionOpenPrefDialog;
import net.tourbook.ui.action.ActionOpenTour;
import net.tourbook.ui.action.ActionRefreshView;
import net.tourbook.ui.action.ActionSetTourTypeMenu;
import net.tourbook.util.PixelConverter;
import net.tourbook.util.PostSelectionProvider;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

public class TaggingView extends ViewPart implements ITourProvider, ITourViewer {

	static public final String				ID								= "net.tourbook.views.tagViewID";		//$NON-NLS-1$

	final IDialogSettings					fViewState						= TourbookPlugin.getDefault()
																					.getDialogSettingsSection(ID);

	private static final String				MEMENTO_TAG_VIEW_LAYOUT			= "tagview.layout";					//$NON-NLS-1$

	static final int						TAG_VIEW_LAYOUT_FLAT			= 0;
	static final int						TAG_VIEW_LAYOUT_HIERARCHICAL	= 10;

	private int								fTagViewLayout					= TAG_VIEW_LAYOUT_HIERARCHICAL;

	private Composite						fViewerContainer;

	private TreeViewer						fTagViewer;
	private TVITagViewRoot					fRootItem;

	private ColumnManager					fColumnManager;

	private PostSelectionProvider			fPostSelectionProvider;

	private ITourEventListener				fTourEventListener;
	private ISelectionListener				fPostSelectionListener;

	private ActionSetTourTag				fActionAddTag;
	private ActionCollapseAll				fActionCollapseAll;
	private ActionCollapseOthers			fActionCollapseOthers;
	private ActionEditQuick					fActionEditQuick;
	private ActionEditTour					fActionEditTour;
	private ActionExpandSelection			fActionExpandSelection;
	private ActionOpenPrefDialog			fActionOpenTagPrefs;
	private ActionSetLayoutHierarchical		fActionSetLayoutHierarchical;
	private ActionSetLayoutFlat				fActionSetLayoutFlat;
	private ActionRefreshView				fActionRefreshView;
	private ActionRemoveAllTags				fActionRemoveAllTags;
	private ActionSetTourTag				fActionRemoveTag;
	private ActionRenameTag					fActionRenameTag;
	private ActionMenuSetAllTagStructures	fActionSetAllTagStructures;
	private ActionMenuSetTagStructure		fActionSetTagStructure;
	private ActionSetTourTypeMenu			fActionSetTourType;
	private ActionOpenTour					fActionOpenTour;

	private ActionModifyColumns				fActionModifyColumns;

	private IPropertyChangeListener			fPrefChangeListener;

	private final Image						fImgTagCategory					= TourbookPlugin.getImageDescriptor(Messages.Image__tag_category)
																					.createImage();
	private final Image						fImgTag							= TourbookPlugin.getImageDescriptor(Messages.Image__tag)
																					.createImage();
	private final Image						fImgTagRoot						= TourbookPlugin.getImageDescriptor(Messages.Image__tag_root)
																					.createImage();

	private IPartListener2					fPartListener;

	private boolean							fIsRecTimeFormat_hhmmss;
	private boolean							fIsDriveTimeFormat_hhmmss;
	private boolean							fIsPauseTimeFormat_hhmmss;

	private static final NumberFormat		fNF								= NumberFormat.getNumberInstance();

	/**
	 * comparatore is sorting the tree items
	 */
	private final class TagComparator extends ViewerComparator {
		@Override
		public int compare(final Viewer viewer, final Object obj1, final Object obj2) {

			if (obj1 instanceof TVITagViewTour && obj2 instanceof TVITagViewTour) {

				// sort tours by date
				final TVITagViewTour tourItem1 = (TVITagViewTour) (obj1);
				final TVITagViewTour tourItem2 = (TVITagViewTour) (obj2);

				return tourItem1.tourDate.compareTo(tourItem2.tourDate);
			}

			if (obj1 instanceof TVITagViewYear && obj2 instanceof TVITagViewYear) {
				final TVITagViewYear yearItem1 = (TVITagViewYear) (obj1);
				final TVITagViewYear yearItem2 = (TVITagViewYear) (obj2);

				return yearItem1.compareTo(yearItem2);
			}

			if (obj1 instanceof TVITagViewMonth && obj2 instanceof TVITagViewMonth) {
				final TVITagViewMonth monthItem1 = (TVITagViewMonth) (obj1);
				final TVITagViewMonth monthItem2 = (TVITagViewMonth) (obj2);

				return monthItem1.compareTo(monthItem2);
			}

			return 0;
		}
	}

	/**
	 * The comparator is necessary to set and restore the expanded elements
	 */
	private class TagComparer implements IElementComparer {

		public boolean equals(final Object a, final Object b) {

			if (a == b) {

				return true;

			} else if (a instanceof TVITagViewYear && b instanceof TVITagViewYear) {

				final TVITagViewYear yearItem1 = (TVITagViewYear) a;
				final TVITagViewYear yearItem2 = (TVITagViewYear) b;

				return yearItem1.getTagId() == yearItem2.getTagId() //
						&& yearItem1.getYear() == yearItem2.getYear();

			} else if (a instanceof TVITagViewMonth && b instanceof TVITagViewMonth) {

				final TVITagViewMonth month1 = (TVITagViewMonth) a;
				final TVITagViewMonth month2 = (TVITagViewMonth) b;
				final TVITagViewYear yearItem1 = month1.getYearItem();
				final TVITagViewYear yearItem2 = month2.getYearItem();

				return yearItem1.getTagId() == yearItem2.getTagId()
						&& yearItem1.getYear() == yearItem2.getYear()
						&& month1.getMonth() == month2.getMonth();

			} else if (a instanceof TVITagViewTagCategory && b instanceof TVITagViewTagCategory) {

				return ((TVITagViewTagCategory) a).tagCategoryId == ((TVITagViewTagCategory) b).tagCategoryId;

			} else if (a instanceof TVITagViewTag && b instanceof TVITagViewTag) {

				return ((TVITagViewTag) a).getTagId() == ((TVITagViewTag) b).getTagId();

			}

			return false;
		}

		public int hashCode(final Object element) {
			return 0;
		}

	}

	private class TagContentProvider implements ITreeContentProvider {

		public void dispose() {}

		public Object[] getChildren(final Object parentElement) {

			if (parentElement instanceof TVITagViewItem) {
				return ((TVITagViewItem) parentElement).getFetchedChildrenAsArray();
			}

			return new Object[0];
		}

		public Object[] getElements(final Object inputElement) {
			return getChildren(inputElement);
		}

		public Object getParent(final Object element) {
			return ((TreeViewerItem) element).getParentItem();
		}

		public boolean hasChildren(final Object element) {
			return ((TreeViewerItem) element).hasChildren();
		}

		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {

			if (newInput == null) {
				return;
			}

			setTagViewTitle(newInput);
		}
	}

	private void addPartListener() {

		fPartListener = new IPartListener2() {
			public void partActivated(final IWorkbenchPartReference partRef) {}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TaggingView.this) {

					saveState();

//					TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR, null, TaggingView.this);
				}
			}

			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			public void partHidden(final IWorkbenchPartReference partRef) {}

			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			public void partOpened(final IWorkbenchPartReference partRef) {}

			public void partVisible(final IWorkbenchPartReference partRef) {}
		};

		getViewSite().getPage().addPartListener(fPartListener);
	}

	private void addPrefListener() {

		final Preferences prefStore = TourbookPlugin.getDefault().getPluginPreferences();

		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(final Preferences.PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)) {
					reloadViewer();

				} else if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

					// update viewer

					fTagViewer.refresh();

				} else if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// measurement system has changed

					UI.updateUnits();

					fColumnManager.saveState(fViewState);
					fColumnManager.clearColumns();
					defineViewerColumns(fViewerContainer);

					fTagViewer = (TreeViewer) recreateViewer(fTagViewer);

				} else if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

					readDisplayFormats();

					fTagViewer.getTree()
							.setLinesVisible(prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

					fTagViewer.refresh();

					/*
					 * the tree must be redrawn because the styled text does not show with the new
					 * color
					 */
					fTagViewer.getTree().redraw();
				}
			}
		};

		// register the listener
		prefStore.addPropertyChangeListener(fPrefChangeListener);
	}

	private void addSelectionListener() {

		// this view part is a selection listener
		fPostSelectionListener = new ISelectionListener() {

			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

				if (selection instanceof SelectionDeletedTours) {
					final SelectionDeletedTours deletedTourSelection = (SelectionDeletedTours) selection;

					updateViewerAfterTourIsDeleted(fRootItem, deletedTourSelection.removedTours);
				}
			}
		};

		// register selection listener in the page
		getSite().getPage().addPostSelectionListener(fPostSelectionListener);
	}

	private void addTourEventListener() {

		fTourEventListener = new ITourEventListener() {
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if (part == TaggingView.this) {
					return;
				}

				if (eventId == TourEventId.NOTIFY_TAG_VIEW) {
					if (eventData instanceof ChangedTags) {

						final ChangedTags changedTags = (ChangedTags) eventData;

						final boolean isAddMode = changedTags.isAddMode();

						// get a clone of the modified tours/tags because the tours are removed from the list
						final ChangedTags changedTagsClone = new ChangedTags(changedTags.getModifiedTags(),
								changedTags.getModifiedTours(),
								isAddMode);

						updateViewerAfterTagStructureIsModified(fRootItem, changedTagsClone, isAddMode);
					}

				} else if (eventId == TourEventId.TAG_STRUCTURE_CHANGED || eventId == TourEventId.UPDATE_UI) {

					reloadViewer();

				} else if (eventId == TourEventId.TOUR_CHANGED && eventData instanceof TourEvent) {

					final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();
					if (modifiedTours != null) {
						updateViewerAfterTourIsModified(fRootItem, modifiedTours);
					}
				}
			}
		};

		TourManager.getInstance().addTourEventListener(fTourEventListener);
	}

	private void createActions() {

		fActionEditQuick = new ActionEditQuick(this);
		fActionEditTour = new ActionEditTour(this);
		fActionOpenTour = new ActionOpenTour(this);

		fActionSetTourType = new ActionSetTourTypeMenu(this);

		fActionAddTag = new ActionSetTourTag(this, true);
		fActionRemoveTag = new ActionSetTourTag(this, false);
		fActionRemoveAllTags = new ActionRemoveAllTags(this);

		fActionRefreshView = new ActionRefreshView(this);
		fActionSetTagStructure = new ActionMenuSetTagStructure(this);
		fActionSetAllTagStructures = new ActionMenuSetAllTagStructures(this);
		fActionRenameTag = new ActionRenameTag(this);

		fActionExpandSelection = new ActionExpandSelection(this);
		fActionCollapseAll = new ActionCollapseAll(this);
		fActionCollapseOthers = new ActionCollapseOthers(this);

		fActionOpenTagPrefs = new ActionOpenPrefDialog(Messages.action_tag_open_tagging_structure,
				ITourbookPreferences.PREF_PAGE_TAGS);

		fActionSetLayoutFlat = new ActionSetLayoutFlat(this);
		fActionSetLayoutHierarchical = new ActionSetLayoutHierarchical(this);

		fActionModifyColumns = new ActionModifyColumns(this);
	}

	/**
	 * create the views context menu
	 */
	private void createContextMenu() {

		final MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(final IMenuManager manager) {
				fillContextMenu(manager);
			}
		});

		// add the context menu to the table viewer
		final Control tourViewer = fTagViewer.getControl();
		final Menu menu = menuMgr.createContextMenu(tourViewer);
		tourViewer.setMenu(menu);
	}

	@Override
	public void createPartControl(final Composite parent) {

		// define all columns
		fColumnManager = new ColumnManager(this, fViewState);
		defineViewerColumns(parent);

		fViewerContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(fViewerContainer);

		createActions();
		fillViewMenu();

		// viewer must be created after the action are created
		createTagViewer(fViewerContainer);

		// set selection provider
		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());

		addTourEventListener();
		addPrefListener();
		addPartListener();
		addSelectionListener();

		enableActions();

		restoreState();
		readDisplayFormats();

		reloadViewer();
	}

	private void createTagViewer(final Composite parent) {

		// tour tree
		final Tree tree = new Tree(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FLAT | SWT.FULL_SELECTION | SWT.MULTI);

		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		tree.setHeaderVisible(true);
		tree.setLinesVisible(TourbookPlugin.getDefault()
				.getPluginPreferences()
				.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

		fTagViewer = new TreeViewer(tree);
		fColumnManager.createColumns(fTagViewer);

		fTagViewer.setContentProvider(new TagContentProvider());
		fTagViewer.setComparer(new TagComparer());
		fTagViewer.setComparator(new TagComparator());
		fTagViewer.setUseHashlookup(true);

		fTagViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {

				final IStructuredSelection selectedTours = (IStructuredSelection) (event.getSelection());
				final Object selectedItem = ((IStructuredSelection) (event.getSelection())).getFirstElement();

				if (selectedItem instanceof TVITagViewTour && selectedTours.size() == 1) {

					// one tour is selected

					final TVITagViewTour tourItem = (TVITagViewTour) selectedItem;
					fPostSelectionProvider.setSelection(new SelectionTourId(tourItem.getTourId()));

				} else {

					// multiple tours are selected

					final ArrayList<Long> tourIds = new ArrayList<Long>();

					for (final Iterator<?> tourIterator = selectedTours.iterator(); tourIterator.hasNext();) {
						final Object viewItem = tourIterator.next();
						if (viewItem instanceof TVITagViewTour) {
							tourIds.add(((TVITagViewTour) viewItem).getTourId());
						}
					}

					fPostSelectionProvider.setSelection(new SelectionTourIds(tourIds));
				}

				enableActions();
			}
		});

		fTagViewer.addDoubleClickListener(new IDoubleClickListener() {

			public void doubleClick(final DoubleClickEvent event) {

				final Object selection = ((IStructuredSelection) fTagViewer.getSelection()).getFirstElement();

				if (selection instanceof TVITagViewTour) {

					// open tour in the tour editor

					TourManager.getInstance().openTourInEditor(((TVITagViewTour) selection).getTourId());

				} else if (selection != null) {

					// expand/collapse current item

					final TreeViewerItem tourItem = (TreeViewerItem) selection;

					if (fTagViewer.getExpandedState(tourItem)) {
						fTagViewer.collapseToLevel(tourItem, 1);
					} else {
						fTagViewer.expandToLevel(tourItem, 1);
					}
				}
			}
		});

		/*
		 * the context menu must be created AFTER the viewer is created which is also done after the
		 * measurement system has changed, if not, the context menu is not displayed because it
		 * belongs to the old viewer
		 */
		createContextMenu();

		fillToolBar();
	}

	/**
	 * Defines all columns for the table viewer in the column manager
	 * 
	 * @param parent
	 */
	private void defineViewerColumns(final Composite parent) {

		final PixelConverter pixelConverter = new PixelConverter(parent);
		TreeColumnDefinition colDef;

		/*
		 * tree column: category/tag/year/month/tour
		 */
		colDef = TreeColumnFactory.TAG.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setCanModifyVisibility(false);
		colDef.setLabelProvider(new StyledCellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final TVITagViewItem viewItem = (TVITagViewItem) element;
				final StyledString styledString = new StyledString();

				if (viewItem instanceof TVITagViewTour) {

					styledString.append(viewItem.treeColumn);

					cell.setImage(UI.getInstance().getTourTypeImage(((TVITagViewTour) viewItem).tourTypeId));
					setCellColor(cell, element);

				} else if (viewItem instanceof TVITagViewTag) {

					final TVITagViewTag tagItem = (TVITagViewTag) viewItem;

					styledString.append(viewItem.treeColumn, UI.TAG_STYLER);
					styledString.append("   " + viewItem.colItemCounter, StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
					cell.setImage(tagItem.isRoot ? fImgTagRoot : fImgTag);

				} else if (viewItem instanceof TVITagViewTagCategory) {

					styledString.append(viewItem.treeColumn, UI.TAG_CATEGORY_STYLER);
					cell.setImage(fImgTagCategory);

				} else if (viewItem instanceof TVITagViewYear || viewItem instanceof TVITagViewMonth) {

					styledString.append(viewItem.treeColumn);
					styledString.append("   " + viewItem.colItemCounter, StyledString.QUALIFIER_STYLER); //$NON-NLS-1$

					if (viewItem instanceof TVITagViewMonth) {
						cell.setForeground(JFaceResources.getColorRegistry().get(UI.VIEW_COLOR_SUB_SUB));
					} else {
						cell.setForeground(JFaceResources.getColorRegistry().get(UI.VIEW_COLOR_SUB));
					}

				} else {
					styledString.append(viewItem.treeColumn);
				}

				cell.setText(styledString.getString());
				cell.setStyleRanges(styledString.getStyleRanges());
			}
		});

		/*
		 * column: title
		 */
		colDef = TreeColumnFactory.TITLE.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				if (element instanceof TVITagViewTour) {
					cell.setText(((TVITagViewTour) element).tourTitle);
					setCellColor(cell, element);
				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

		/*
		 * column: tags
		 */
		colDef = TreeColumnFactory.TOUR_TAGS.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				if (element instanceof TVITagViewTour) {
					TourDatabase.getInstance();
					cell.setText(TourDatabase.getTagNames(((TVITagViewTour) element).tagIds));
					setCellColor(cell, element);
				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

		/*
		 * column: recording time (h)
		 */
		colDef = TreeColumnFactory.RECORDING_TIME.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				if (element instanceof TVITagViewTagCategory) {
					return;
				}

				final long recordingTime = ((TVITagViewItem) element).colRecordingTime;

				if (element instanceof TVITagViewTour) {
					if (fIsRecTimeFormat_hhmmss) {
						cell.setText(UI.format_hh_mm_ss(recordingTime).toString());
					} else {
						cell.setText(UI.format_hh_mm(recordingTime + 30).toString());
					}
				} else {
					cell.setText(UI.format_hh_mm(recordingTime + 30).toString());
				}

				setCellColor(cell, element);
			}
		});

		/*
		 * column: driving time (h)
		 */
		colDef = TreeColumnFactory.DRIVING_TIME.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				if (element instanceof TVITagViewTagCategory) {
					return;
				}

				final long drivingTime = ((TVITagViewItem) element).colDrivingTime;

				if (element instanceof TVITagViewTour) {
					if (fIsDriveTimeFormat_hhmmss) {
						cell.setText(UI.format_hh_mm_ss(drivingTime).toString());
					} else {
						cell.setText(UI.format_hh_mm(drivingTime + 30).toString());
					}
				} else {
					cell.setText(UI.format_hh_mm(drivingTime + 30).toString());
				}

				setCellColor(cell, element);
			}
		});

		/*
		 * column: paused time (h)
		 */
		colDef = TreeColumnFactory.PAUSED_TIME.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				if (element instanceof TVITagViewTagCategory) {
					return;
				}

				/*
				 * display paused time relative to the recording time
				 */

				final TVITagViewItem item = (TVITagViewItem) element;

				final long dbPausedTime = item.colPausedTime;
				final long dbRecordingTime = item.colRecordingTime;

				final float relativePausedTime = dbRecordingTime == 0 ? 0 : (float) dbPausedTime
						/ dbRecordingTime
						* 100;

				fNF.setMinimumFractionDigits(1);
				fNF.setMaximumFractionDigits(1);
				cell.setText(fNF.format(relativePausedTime));

				setCellColor(cell, element);

			}
		});

		/*
		 * column: distance (km/miles)
		 */
		colDef = TreeColumnFactory.DISTANCE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				if (element instanceof TVITagViewTagCategory) {
					return;
				}

				final long colDistance = ((TVITagViewItem) element).colDistance;
				if (colDistance != 0) {

					// set distance
					fNF.setMinimumFractionDigits(1);
					fNF.setMaximumFractionDigits(1);

					final String distance = fNF.format(((float) colDistance) / 1000 / UI.UNIT_VALUE_DISTANCE);

					cell.setText(distance);
					setCellColor(cell, element);
				}
			}
		});

		/*
		 * column: altitude up (m)
		 */
		colDef = TreeColumnFactory.ALTITUDE_UP.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				if (element instanceof TVITagViewTagCategory) {
					return;
				}

				final long colAltitudeUp = ((TVITagViewItem) element).colAltitudeUp;
				if (colAltitudeUp != 0) {

					cell.setText(Long.toString((long) (colAltitudeUp / UI.UNIT_VALUE_ALTITUDE)));
					setCellColor(cell, element);
				}
			}
		});

		/*
		 * column: altitude down (m)
		 */
		colDef = TreeColumnFactory.ALTITUDE_DOWN.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				if (element instanceof TVITagViewTagCategory) {
					return;
				}

				final long colAltitudeDown = ((TVITagViewItem) element).colAltitudeDown;
				if (colAltitudeDown != 0) {

					cell.setText(Long.toString((long) (colAltitudeDown / UI.UNIT_VALUE_ALTITUDE)));
					setCellColor(cell, element);
				}
			}
		});

		/*
		 * column: max altitude
		 */
		colDef = TreeColumnFactory.MAX_ALTITUDE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				if (element instanceof TVITagViewTagCategory) {
					return;
				}

				final long colMaxAltitude = ((TVITagViewItem) element).colMaxAltitude;
				if (colMaxAltitude != 0) {

					cell.setText(Long.toString(colMaxAltitude));
					setCellColor(cell, element);
				}
			}
		});

		/*
		 * column: max speed
		 */
		colDef = TreeColumnFactory.MAX_SPEED.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				if (element instanceof TVITagViewTagCategory) {
					return;
				}

				final float colMaxSpeed = ((TVITagViewItem) element).colMaxSpeed;
				if (colMaxSpeed != 0) {

					fNF.setMinimumFractionDigits(1);
					fNF.setMaximumFractionDigits(1);

					cell.setText(fNF.format(colMaxSpeed / UI.UNIT_VALUE_DISTANCE));
					setCellColor(cell, element);
				}
			}
		});

		/*
		 * column: max pulse
		 */
		colDef = TreeColumnFactory.MAX_PULSE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				if (element instanceof TVITagViewTagCategory) {
					return;
				}

				final long colMaxPulse = ((TVITagViewItem) element).colMaxPulse;
				if (colMaxPulse != 0) {

					cell.setText(Long.toString(colMaxPulse));
					setCellColor(cell, element);
				}
			}
		});

		/*
		 * column: avg speed km/h - mph
		 */
		colDef = TreeColumnFactory.AVG_SPEED.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				fNF.setMinimumFractionDigits(1);
				fNF.setMaximumFractionDigits(1);

				final float colAvgSpeed = ((TVITagViewItem) element).colAvgSpeed / UI.UNIT_VALUE_DISTANCE;
				if (colAvgSpeed != 0) {

					cell.setText(fNF.format(colAvgSpeed));
					setCellColor(cell, element);
				}
			}
		});

		/*
		 * column: avg pace min/km - min/mi
		 */
		colDef = TreeColumnFactory.AVG_PACE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final float pace = ((TVITagViewItem) element).colAvgPace * UI.UNIT_VALUE_DISTANCE;

				if (pace == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(UI.format_mm_ss((long) pace).toString());
				}

				setCellColor(cell, element);
			}
		});

		/*
		 * column: avg pulse
		 */
		colDef = TreeColumnFactory.AVG_PULSE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				if (element instanceof TVITagViewTagCategory) {
					return;
				}

				final long colAvgPulse = ((TVITagViewItem) element).colAvgPulse;
				if (colAvgPulse != 0) {

					cell.setText(Long.toString(colAvgPulse));
					setCellColor(cell, element);
				}
			}
		}); /*
			 * column: avg cadence
			 */
		colDef = TreeColumnFactory.AVG_CADENCE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				if (element instanceof TVITagViewTagCategory) {
					return;
				}

				final long colAvgCadence = ((TVITagViewItem) element).colAvgCadence;
				if (colAvgCadence != 0) {

					cell.setText(Long.toString(colAvgCadence));
					setCellColor(cell, element);
				}
			}
		});

		/*
		 * column: avg temperature
		 */
		colDef = TreeColumnFactory.AVG_TEMPERATURE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {

			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				if (element instanceof TVITagViewTagCategory) {
					return;
				}

				long temperature = ((TVITagViewItem) element).colAvgTemperature;
				if (temperature != 0) {

					if (UI.UNIT_VALUE_TEMPERATURE != 1) {
						temperature = (long) (temperature * UI.UNIT_FAHRENHEIT_MULTI + UI.UNIT_FAHRENHEIT_ADD);
					}

					cell.setText(Long.toString(temperature));
					setCellColor(cell, element);
				}
			}
		});
	}

	@Override
	public void dispose() {

		getSite().getPage().removePostSelectionListener(fPostSelectionListener);
		TourManager.getInstance().removeTourEventListener(fTourEventListener);
		TourbookPlugin.getDefault().getPluginPreferences().removePropertyChangeListener(fPrefChangeListener);

		fImgTag.dispose();
		fImgTagRoot.dispose();
		fImgTagCategory.dispose();

		super.dispose();
	}

	private void enableActions() {

		final StructuredSelection selection = (StructuredSelection) fTagViewer.getSelection();
		final int treeItems = fTagViewer.getTree().getItemCount();

		/*
		 * count number of selected items
		 */
		int tourItems = 0;
		int tagItems = 0;
		int categoryItems = 0;
		int items = 0;
		int otherItems = 0;
		TVITagViewTour firstTour = null;

		for (final Iterator<?> iter = selection.iterator(); iter.hasNext();) {
			final Object treeItem = iter.next();
			if (treeItem instanceof TVITagViewTour) {
				if (tourItems == 0) {
					firstTour = (TVITagViewTour) treeItem;
				}
				tourItems++;
			} else if (treeItem instanceof TVITagViewTag) {
				tagItems++;
			} else if (treeItem instanceof TVITagViewTagCategory) {
				categoryItems++;
			} else {
				otherItems++;
			}
			items++;
		}

		final boolean isTourSelected = tourItems > 0;
		final boolean isTagSelected = tagItems > 0 && tourItems == 0 && categoryItems == 0 && otherItems == 0;
		final boolean isCategorySelected = categoryItems > 0 && tourItems == 0 && tagItems == 0 && otherItems == 0;

		final int selectedItems = selection.size();
		final TVITagViewItem firstElement = (TVITagViewItem) selection.getFirstElement();
		final boolean firstElementHasChildren = firstElement == null ? false : firstElement.hasChildren();

		fActionEditTour.setEnabled(tourItems == 1);
		fActionOpenTour.setEnabled(tourItems == 1);
		fActionEditQuick.setEnabled(tourItems == 1);

		// action: set tour type
		final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();
		fActionSetTourType.setEnabled(isTourSelected && tourTypes.size() > 0);

		// action: add tag
		fActionAddTag.setEnabled(isTourSelected);

		// actions: remove tags
		if (firstTour != null && tourItems == 1) {

			// one tour is selected

			final ArrayList<Long> tagIds = firstTour.tagIds;
			if (tagIds != null && tagIds.size() > 0) {

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

		// enable rename action
		if (selectedItems == 1) {
			if (isTagSelected) {
				fActionRenameTag.setText(Messages.action_tag_rename_tag);
				fActionRenameTag.setEnabled(true);
			} else if (isCategorySelected) {
				fActionRenameTag.setText(Messages.action_tag_rename_tag_category);
				fActionRenameTag.setEnabled(true);

			} else {
				fActionRenameTag.setEnabled(false);
			}
		} else {
			fActionRenameTag.setEnabled(false);
		}

		/*
		 * tree expand type can be set if only tags are selected or when an item is selected which
		 * is not a category
		 */
		fActionSetTagStructure.setEnabled(isTagSelected || (items == 1 && categoryItems == 0));
		fActionSetAllTagStructures.setEnabled(treeItems > 0);

		fActionExpandSelection.setEnabled(firstElement == null ? false : //
				selectedItems == 1 ? firstElementHasChildren : //
						true);

		fActionCollapseOthers.setEnabled(selectedItems == 1 && firstElementHasChildren);
		fActionCollapseAll.setEnabled(treeItems > 0);

		// enable/disable actions for the recent tags
		TagManager.enableRecentTagActions(isTourSelected);
	}

	private void fillContextMenu(final IMenuManager menuMgr) {

		menuMgr.add(fActionCollapseOthers);
		menuMgr.add(fActionExpandSelection);
		menuMgr.add(fActionCollapseAll);

		menuMgr.add(new Separator());
		menuMgr.add(fActionRenameTag);
		menuMgr.add(fActionSetTagStructure);
		menuMgr.add(fActionSetAllTagStructures);

		menuMgr.add(new Separator());
		menuMgr.add(fActionEditQuick);
		menuMgr.add(fActionEditTour);
		menuMgr.add(fActionOpenTour);

		menuMgr.add(new Separator());
		menuMgr.add(fActionSetTourType);
		menuMgr.add(fActionAddTag);
		menuMgr.add(fActionRemoveTag);
		menuMgr.add(fActionRemoveAllTags);
		TagManager.fillRecentTagsIntoMenu(menuMgr, this, true, true);
		menuMgr.add(fActionOpenTagPrefs);

		enableActions();
	}

	private void fillToolBar() {
		/*
		 * action in the view toolbar
		 */
		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

		// recreate the toolbar
		tbm.removeAll();

		tbm.add(fActionExpandSelection);
		tbm.add(fActionCollapseAll);

		tbm.add(fActionRefreshView);

		tbm.update(true);
	}

	private void fillViewMenu() {

		/*
		 * fill view menu
		 */
		final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();

		menuMgr.add(fActionSetLayoutFlat);
		menuMgr.add(fActionSetLayoutHierarchical);

		menuMgr.add(new Separator());
		menuMgr.add(fActionModifyColumns);

	}

	@SuppressWarnings("unchecked")
	@Override
	public Object getAdapter(final Class adapter) {

		if (adapter == ColumnViewer.class) {
			return fTagViewer;
		}

		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	public ColumnManager getColumnManager() {
		return fColumnManager;
	}

	public ArrayList<TourData> getSelectedTours() {

		// get selected tours
		final IStructuredSelection selectedTours = ((IStructuredSelection) fTagViewer.getSelection());

		final TourManager tourManager = TourManager.getInstance();
		final ArrayList<TourData> selectedTourData = new ArrayList<TourData>();

		// loop: all selected tours
		for (final Iterator<?> iter = selectedTours.iterator(); iter.hasNext();) {

			final Object treeItem = iter.next();
			if (treeItem instanceof TVITagViewTour) {

				final TourData tourData = tourManager.getTourData(((TVITagViewTour) treeItem).getTourId());

				if (tourData != null) {
					selectedTourData.add(tourData);
				}
			}
		}

		return selectedTourData;
	}

	public ColumnViewer getViewer() {
		return fTagViewer;
	}

	private void readDisplayFormats() {

		final Preferences prefStore = TourbookPlugin.getDefault().getPluginPreferences();

		fIsRecTimeFormat_hhmmss = prefStore.getString(ITourbookPreferences.VIEW_LAYOUT_RECORDING_TIME_FORMAT)
				.equals(PrefPageAppearanceView.VIEW_TIME_LAYOUT_HH_MM_SS);

		fIsDriveTimeFormat_hhmmss = prefStore.getString(ITourbookPreferences.VIEW_LAYOUT_DRIVING_TIME_FORMAT)
				.equals(PrefPageAppearanceView.VIEW_TIME_LAYOUT_HH_MM_SS);
	}

	public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

		fViewerContainer.setRedraw(false);
		{
			final Object[] expandedElements = fTagViewer.getExpandedElements();
			final ISelection selection = fTagViewer.getSelection();

			fTagViewer.getTree().dispose();

			createTagViewer(fViewerContainer);
			fViewerContainer.layout();

			fTagViewer.setInput(fRootItem = new TVITagViewRoot(this, fTagViewLayout));

			fTagViewer.setExpandedElements(expandedElements);
			fTagViewer.setSelection(selection);
		}
		fViewerContainer.setRedraw(true);

		return fTagViewer;
	}

	/**
	 * reload the content of the tag viewer
	 */
	public void reloadViewer() {

		final Tree tree = fTagViewer.getTree();
		tree.setRedraw(false);
		{
			final Object[] expandedElements = fTagViewer.getExpandedElements();

			fTagViewer.setInput(fRootItem = new TVITagViewRoot(this, fTagViewLayout));
			fTagViewer.setExpandedElements(expandedElements);
		}
		tree.setRedraw(true);
	}

	private void restoreState() {

		fTagViewLayout = TAG_VIEW_LAYOUT_HIERARCHICAL;

		// restore view layout
		try {

			final int viewLayout = fViewState.getInt(MEMENTO_TAG_VIEW_LAYOUT);
			switch (viewLayout) {

			case TAG_VIEW_LAYOUT_FLAT:

				fTagViewLayout = viewLayout;
				fActionSetLayoutFlat.setChecked(true);
				break;

			case TAG_VIEW_LAYOUT_HIERARCHICAL:

				fTagViewLayout = viewLayout;
				fActionSetLayoutHierarchical.setChecked(true);
				break;

			default:
				break;
			}

		} catch (final NumberFormatException e) {

			// set default tag view layout
			fTagViewLayout = TAG_VIEW_LAYOUT_HIERARCHICAL;
			fActionSetLayoutHierarchical.setChecked(true);
		}
	}

	private void saveState() {

		fColumnManager.saveState(fViewState);

		// save view layout
		fViewState.put(MEMENTO_TAG_VIEW_LAYOUT, fTagViewLayout);

//		final Object[] expandedElements = fTagViewer.getExpandedElements();
//		final Object[] visibleExpandedElements = fTagViewer.getVisibleExpandedElements();
//		final TreePath[] expandedTreePaths = fTagViewer.getExpandedTreePaths();
//		fTagViewer.setExpandedTreePaths(expandedTreePaths);
//		fTagViewer.setExpandedElements(expandedElements);
	}

	private void setCellColor(final ViewerCell cell, final Object element) {
		// set color
		if (element instanceof TVITagViewTag) {
			cell.setForeground(JFaceResources.getColorRegistry().get(UI.VIEW_COLOR_TITLE));
		} else if (element instanceof TVITagViewYear) {
			cell.setForeground(JFaceResources.getColorRegistry().get(UI.VIEW_COLOR_SUB));
		} else if (element instanceof TVITagViewMonth) {
			cell.setForeground(JFaceResources.getColorRegistry().get(UI.VIEW_COLOR_SUB_SUB));
		} else if (element instanceof TVITagViewTour) {
			cell.setForeground(JFaceResources.getColorRegistry().get(UI.VIEW_COLOR_TOUR));
		}
	}

	@Override
	public void setFocus() {

	}

	private void setTagViewTitle(final Object newInput) {

		String description = UI.EMPTY_STRING;

		if (newInput instanceof TVITagViewTag) {
			description = Messages.tag_view_title_tag + ((TVITagViewTag) newInput).getName();
		} else if (newInput instanceof TVITagViewTagCategory) {
			description = Messages.tag_view_title_tag_category + ((TVITagViewTagCategory) newInput).name;
		}

		setContentDescription(description);
	}

	public void setViewLayout(final int tagViewStructure) {
		fTagViewLayout = tagViewStructure;
		reloadViewer();
	}

	/**
	 * !!! Recursive !!! method to update the tags in the viewer, this method handles changes in the
	 * tag structure
	 * 
	 * @param rootItem
	 * @param changedTags
	 * @param isAddMode
	 */
	private void updateViewerAfterTagStructureIsModified(	final TreeViewerItem parentItem,
															final ChangedTags changedTags,
															final boolean isAddMode) {

		final ArrayList<TreeViewerItem> children = parentItem.getUnfetchedChildren();

		if (children == null) {
			return;
		}

		// loop: all children of the current parent item 
		for (final Object object : children) {

			if (object instanceof TVITagViewTag) {

				final TVITagViewTag tagItem = (TVITagViewTag) object;
				final long viewerTagId = tagItem.getTagId();

				final HashMap<Long, TourTag> modifiedTags = changedTags.getModifiedTags();
				final ArrayList<Long> removedIds = new ArrayList<Long>();

				for (final Long modifiedTagId : modifiedTags.keySet()) {
					if (viewerTagId == modifiedTagId.longValue()) {

						/*
						 * current tag was modified
						 */

						// add/remove tours from the tag
						tagItem.refresh(fTagViewer, changedTags.getModifiedTours(), changedTags.isAddMode());

						// update tag totals
						TVITagViewItem.readTagTotals(tagItem);

						// update viewer
						fTagViewer.refresh(tagItem);

						removedIds.add(modifiedTagId);
					}
				}

				/*
				 * modified tag id exists only once in the tree viewer, remove the id's outside of
				 * the foreach loop to avid the exception ConcurrentModificationException
				 */
				for (final Long removedId : removedIds) {
					modifiedTags.remove(removedId);
				}

				// optimize
				if (modifiedTags.size() == 0) {
					return;
				}

			} else {
				if (object instanceof TreeViewerItem) {
					updateViewerAfterTagStructureIsModified((TreeViewerItem) object, changedTags, isAddMode);
				}
			}
		}
	}

	/**
	 * !!!Recursive !!! delete tour items
	 * 
	 * @param rootItem
	 * @param deletedTourIds
	 */
	private void updateViewerAfterTourIsDeleted(final TreeViewerItem parentItem,
												final ArrayList<ITourItem> deletedTourIds) {

		final ArrayList<TreeViewerItem> parentChildren = parentItem.getUnfetchedChildren();

		if (parentChildren == null) {
			return;
		}

		final ArrayList<TVITagViewTour> deletedTourItems = new ArrayList<TVITagViewTour>();

		// loop: all tree children items
		for (final Object object : parentChildren) {
			if (object instanceof TreeViewerItem) {

				final TreeViewerItem childItem = (TreeViewerItem) object;
				if (childItem instanceof TVITagViewTour) {

					final TVITagViewTour tourItem = (TVITagViewTour) childItem;
					final long tourItemId = tourItem.getTourId();

					// loop: all deleted tour id's
					for (final ITourItem deletedTourItem : deletedTourIds) {
						if (deletedTourItem.getTourId().longValue() == tourItemId) {

							// keep deleted tour item
							deletedTourItems.add(tourItem);

							break;
						}
					}

				} else {
					// update children
					updateViewerAfterTourIsDeleted(childItem, deletedTourIds);
				}
			}
		}

		if (deletedTourItems.size() > 0) {

			// update model
			parentChildren.removeAll(deletedTourItems);

			// update viewer
			fTagViewer.remove(deletedTourItems.toArray());
		}
	}

	/**
	 * !!!Recursive !!! update the data for all tour items
	 * 
	 * @param rootItem
	 * @param modifiedTours
	 */
	private void updateViewerAfterTourIsModified(	final TreeViewerItem parentItem,
													final ArrayList<TourData> modifiedTours) {

		final ArrayList<TreeViewerItem> children = parentItem.getUnfetchedChildren();

		if (children == null) {
			return;
		}

		// loop: all children
		for (final Object object : children) {
			if (object instanceof TreeViewerItem) {

				final TreeViewerItem treeItem = (TreeViewerItem) object;
				if (treeItem instanceof TVITagViewTour) {

					final TVITagViewTour tourItem = (TVITagViewTour) treeItem;
					final long tourItemId = tourItem.getTourId();

					for (final TourData modifiedTourData : modifiedTours) {
						if (modifiedTourData.getTourId().longValue() == tourItemId) {

							// update tree item

							final TourType tourType = modifiedTourData.getTourType();
							if (tourType != null) {
								tourItem.tourTypeId = tourType.getTypeId();
							}

							// update item title
							tourItem.tourTitle = modifiedTourData.getTourTitle();

							// update item tags
							final Set<TourTag> tourTags = modifiedTourData.getTourTags();
							final ArrayList<Long> tagIds;

							tourItem.tagIds = tagIds = new ArrayList<Long>();
							for (final TourTag tourTag : tourTags) {
								tagIds.add(tourTag.getTagId());
							}

							// update item in the viewer
							fTagViewer.update(tourItem, null);

							// a tour exists only once as a child in a tree item
							break;
						}
					}

				} else {
					// update children
					updateViewerAfterTourIsModified(treeItem, modifiedTours);
				}
			}
		}
	}

}
