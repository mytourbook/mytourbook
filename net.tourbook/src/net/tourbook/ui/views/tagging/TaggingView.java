/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
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
import net.tourbook.tour.TourDoubleClickState;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TourTypeMenuManager;
import net.tourbook.ui.ITourProvider;
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
import net.tourbook.ui.views.TourInfoToolTipCellLabelProvider;
import net.tourbook.ui.views.TourInfoToolTipStyledCellLabelProvider;
import net.tourbook.ui.views.TreeViewerTourInfoToolTip;
import net.tourbook.util.ColumnManager;
import net.tourbook.util.ITourViewer;
import net.tourbook.util.PixelConverter;
import net.tourbook.util.PostSelectionProvider;
import net.tourbook.util.TreeColumnDefinition;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
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

	static public final String				ID								= "net.tourbook.views.tagViewID";			//$NON-NLS-1$

	private static final String				MEMENTO_TAG_VIEW_LAYOUT			= "tagview.layout";						//$NON-NLS-1$

	static final int						TAG_VIEW_LAYOUT_FLAT			= 0;
	static final int						TAG_VIEW_LAYOUT_HIERARCHICAL	= 10;

	private final IPreferenceStore			_prefStore						= TourbookPlugin
																					.getDefault()
																					.getPreferenceStore();

	private final IDialogSettings			_state							= TourbookPlugin
																					.getDefault()
																					.getDialogSettingsSection(ID);

	private int								_tagViewLayout					= TAG_VIEW_LAYOUT_HIERARCHICAL;

	private boolean							_isRecTimeFormat_hhmmss;
	private boolean							_isDriveTimeFormat_hhmmss;

	private boolean							_isToolTipInTag;
	private boolean							_isToolTipInTitle;
	private boolean							_isToolTipInTags;

	private TourDoubleClickState			_tourDoubleClickState			= new TourDoubleClickState();

	private static final NumberFormat		_nf1							= NumberFormat.getNumberInstance();
	{
		_nf1.setMinimumFractionDigits(1);
		_nf1.setMaximumFractionDigits(1);
	}

	/*
	 * resources
	 */
	private final Image						_imgTagCategory					= TourbookPlugin
																					.getImageDescriptor(
																							Messages.Image__tag_category)
																					.createImage();
	private final Image						_imgTag							= TourbookPlugin.getImageDescriptor(
																					Messages.Image__tag).createImage();
	private final Image						_imgTagRoot						= TourbookPlugin
																					.getImageDescriptor(
																							Messages.Image__tag_root)
																					.createImage();
	/*
	 * UI controls
	 */
	private Composite						_viewerContainer;

	private TreeViewer						_tagViewer;
	private TVITagViewRoot					_bootItem;

	private ColumnManager					_columnManager;

	private PostSelectionProvider			_postSelectionProvider;

	private ITourEventListener				_tourEventListener;
	private ISelectionListener				_postSelectionListener;
	private IPropertyChangeListener			_prefChangeListener;
	private IPartListener2					_partListener;

	private ActionSetTourTag				_actionAddTag;
	private ActionCollapseAll				_actionCollapseAll;
	private ActionCollapseOthers			_actionCollapseOthers;
	private ActionEditQuick					_actionEditQuick;
	private ActionEditTour					_actionEditTour;
	private ActionExpandSelection			_actionExpandSelection;
	private ActionOpenPrefDialog			_actionOpenTagPrefs;
	private ActionSetLayoutHierarchical		_actionSetLayoutHierarchical;
	private ActionSetLayoutFlat				_actionSetLayoutFlat;
	private ActionRefreshView				_actionRefreshView;
	private ActionRemoveAllTags				_actionRemoveAllTags;
	private ActionSetTourTag				_actionRemoveTag;
	private ActionRenameTag					_actionRenameTag;
	private ActionMenuSetAllTagStructures	_actionSetAllTagStructures;
	private ActionMenuSetTagStructure		_actionSetTagStructure;
	private ActionSetTourTypeMenu			_actionSetTourType;
	private ActionOpenTour					_actionOpenTour;
	private ActionModifyColumns				_actionModifyColumns;

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
	 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!<br>
	 * <br>
	 * The comparator is necessary to set and restore the expanded elements <br>
	 * <br>
	 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!<br>
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

		_partListener = new IPartListener2() {
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

		getViewSite().getPage().addPartListener(_partListener);
	}

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)) {
					reloadViewer();

				} else if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

					// update viewer

					_tagViewer.refresh();

				} else if (property.equals(ITourbookPreferences.VIEW_TOOLTIP_IS_MODIFIED)) {

					updateToolTipState();

				} else if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// measurement system has changed

					UI.updateUnits();

					_columnManager.saveState(_state);
					_columnManager.clearColumns();
					defineAllColumns(_viewerContainer);

					_tagViewer = (TreeViewer) recreateViewer(_tagViewer);

				} else if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

					readDisplayFormats();

					_tagViewer.getTree().setLinesVisible(
							_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

					_tagViewer.refresh();

					/*
					 * the tree must be redrawn because the styled text does not show with the new
					 * color
					 */
					_tagViewer.getTree().redraw();
				}
			}
		};

		// register the listener
		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	private void addSelectionListener() {

		// this view part is a selection listener
		_postSelectionListener = new ISelectionListener() {

			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

				if (selection instanceof SelectionDeletedTours) {
					final SelectionDeletedTours deletedTourSelection = (SelectionDeletedTours) selection;

					updateViewerAfterTourIsDeleted(_bootItem, deletedTourSelection.removedTours);
				}
			}
		};

		// register selection listener in the page
		getSite().getPage().addPostSelectionListener(_postSelectionListener);
	}

	private void addTourEventListener() {

		_tourEventListener = new ITourEventListener() {
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if (part == TaggingView.this) {
					return;
				}

				if (eventId == TourEventId.NOTIFY_TAG_VIEW) {
					if (eventData instanceof ChangedTags) {

						final ChangedTags changedTags = (ChangedTags) eventData;

						final boolean isAddMode = changedTags.isAddMode();

						// get a clone of the modified tours/tags because the tours are removed from the list
						final ChangedTags changedTagsClone = new ChangedTags(
								changedTags.getModifiedTags(),
								changedTags.getModifiedTours(),
								isAddMode);

						updateViewerAfterTagStructureIsModified(_bootItem, changedTagsClone, isAddMode);
					}

				} else if (eventId == TourEventId.TAG_STRUCTURE_CHANGED || eventId == TourEventId.UPDATE_UI) {

					reloadViewer();

				} else if (eventId == TourEventId.TOUR_CHANGED && eventData instanceof TourEvent) {

					final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();
					if (modifiedTours != null) {
						updateViewerAfterTourIsModified(_bootItem, modifiedTours);
					}
				}
			}
		};

		TourManager.getInstance().addTourEventListener(_tourEventListener);
	}

	private void createActions() {

		_actionEditQuick = new ActionEditQuick(this);
		_actionEditTour = new ActionEditTour(this);
		_actionOpenTour = new ActionOpenTour(this);

		_actionSetTourType = new ActionSetTourTypeMenu(this);

		_actionAddTag = new ActionSetTourTag(this, true);
		_actionRemoveTag = new ActionSetTourTag(this, false);
		_actionRemoveAllTags = new ActionRemoveAllTags(this);

		_actionRefreshView = new ActionRefreshView(this);
		_actionSetTagStructure = new ActionMenuSetTagStructure(this);
		_actionSetAllTagStructures = new ActionMenuSetAllTagStructures(this);
		_actionRenameTag = new ActionRenameTag(this);

		_actionExpandSelection = new ActionExpandSelection(this);
		_actionCollapseAll = new ActionCollapseAll(this);
		_actionCollapseOthers = new ActionCollapseOthers(this);

		_actionOpenTagPrefs = new ActionOpenPrefDialog(
				Messages.action_tag_open_tagging_structure,
				ITourbookPreferences.PREF_PAGE_TAGS);

		_actionSetLayoutFlat = new ActionSetLayoutFlat(this);
		_actionSetLayoutHierarchical = new ActionSetLayoutHierarchical(this);

		_actionModifyColumns = new ActionModifyColumns(this);
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
		final Control tourViewer = _tagViewer.getControl();
		final Menu menu = menuMgr.createContextMenu(tourViewer);
		tourViewer.setMenu(menu);
	}

	@Override
	public void createPartControl(final Composite parent) {

		// define all columns
		_columnManager = new ColumnManager(this, _state);
		defineAllColumns(parent);

		_viewerContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);

		createActions();
		fillViewMenu();

		// viewer must be created after the action are created
		createTagViewer(_viewerContainer);

		// set selection provider
		getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider());

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
		tree.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

		_tagViewer = new TreeViewer(tree);
		_columnManager.createColumns(_tagViewer);

		_tagViewer.setContentProvider(new TagContentProvider());
		_tagViewer.setComparer(new TagComparer());
		_tagViewer.setComparator(new TagComparator());
		_tagViewer.setUseHashlookup(true);

		_tagViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {

				final IStructuredSelection selectedTours = (IStructuredSelection) (event.getSelection());
				final Object selectedItem = ((IStructuredSelection) (event.getSelection())).getFirstElement();

				if (selectedItem instanceof TVITagViewTour && selectedTours.size() == 1) {

					// one tour is selected

					final TVITagViewTour tourItem = (TVITagViewTour) selectedItem;
					_postSelectionProvider.setSelection(new SelectionTourId(tourItem.getTourId()));

				} else {

					// multiple tours are selected

					final ArrayList<Long> tourIds = new ArrayList<Long>();

					for (final Iterator<?> tourIterator = selectedTours.iterator(); tourIterator.hasNext();) {
						final Object viewItem = tourIterator.next();
						if (viewItem instanceof TVITagViewTour) {
							tourIds.add(((TVITagViewTour) viewItem).getTourId());
						}
					}

					_postSelectionProvider.setSelection(new SelectionTourIds(tourIds));
				}

				enableActions();
			}
		});

		_tagViewer.addDoubleClickListener(new IDoubleClickListener() {

			public void doubleClick(final DoubleClickEvent event) {

				final Object selection = ((IStructuredSelection) _tagViewer.getSelection()).getFirstElement();

				if (selection instanceof TVITagViewTour) {

					TourManager.getInstance().tourDoubleClickAction(TaggingView.this, _tourDoubleClickState);

				} else if (selection != null) {

					// expand/collapse current item

					final TreeViewerItem tourItem = (TreeViewerItem) selection;

					if (_tagViewer.getExpandedState(tourItem)) {
						_tagViewer.collapseToLevel(tourItem, 1);
					} else {
						_tagViewer.expandToLevel(tourItem, 1);
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

		// set tour info tooltip provider
		new TreeViewerTourInfoToolTip(_tagViewer);
	}

	/**
	 * Defines all columns for the table viewer in the column manager
	 * 
	 * @param parent
	 */
	private void defineAllColumns(final Composite parent) {

		final PixelConverter pc = new PixelConverter(parent);

		defineColumn1stColumn(pc);
		defineColumnTitle(pc);
		defineColumnTags(pc);
		defineColumnTimeRecording(pc);
		defineColumnTimeDriving(pc);
		defineColumnTimeBreak(pc);
		defineColumnDistance(pc);
		defineColumnAltitudeUp(pc);
		defineColumnAltitudeDown(pc);
		defineColumnMaxAltitude(pc);
		defineColumnMaxSpeed(pc);
		defineColumnMaxPulse(pc);
		defineColumnAvgSpeed(pc);
		defineColumnAvgPace(pc);
		defineColumnAvgPulse(pc);
		defineColumnAvgCadence(pc);
		defineColumnAvgTemperature(pc);
	}

	/**
	 * tree column: category/tag/year/month/tour
	 */
	private void defineColumn1stColumn(final PixelConverter pc) {

		final TreeColumnDefinition colDef = TreeColumnFactory.TAG.createColumn(_columnManager, pc);
		colDef.setIsDefaultColumn();
		colDef.setCanModifyVisibility(false);
		colDef.setLabelProvider(new TourInfoToolTipStyledCellLabelProvider() {

			@Override
			public Long getTourId(final ViewerCell cell) {

				if (_isToolTipInTag == false) {
					return null;
				}

				final Object element = cell.getElement();
				final TVITagViewItem viewItem = (TVITagViewItem) element;

				if (viewItem instanceof TVITagViewTour) {
					return ((TVITagViewTour) viewItem).tourId;
				}

				return null;
			}

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
					styledString.append("   " + viewItem.colTourCounter, StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
					cell.setImage(tagItem.isRoot ? _imgTagRoot : _imgTag);

				} else if (viewItem instanceof TVITagViewTagCategory) {

					styledString.append(viewItem.treeColumn, UI.TAG_CATEGORY_STYLER);
					cell.setImage(_imgTagCategory);

				} else if (viewItem instanceof TVITagViewYear || viewItem instanceof TVITagViewMonth) {

					styledString.append(viewItem.treeColumn);
					styledString.append("   " + viewItem.colTourCounter, StyledString.QUALIFIER_STYLER); //$NON-NLS-1$

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
	}

	/**
	 * column: altitude down (m)
	 */
	private void defineColumnAltitudeDown(final PixelConverter pc) {

		final TreeColumnDefinition colDef = TreeColumnFactory.ALTITUDE_DOWN.createColumn(_columnManager, pc);
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
	}

	/**
	 * column: altitude up (m)
	 */
	private void defineColumnAltitudeUp(final PixelConverter pc) {

		final TreeColumnDefinition colDef = TreeColumnFactory.ALTITUDE_UP.createColumn(_columnManager, pc);
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
	}

	/**
	 * column: avg cadence
	 */
	private void defineColumnAvgCadence(final PixelConverter pc) {

		final TreeColumnDefinition colDef = TreeColumnFactory.AVG_CADENCE.createColumn(_columnManager, pc);
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
	}

	/**
	 * column: avg pace min/km - min/mi
	 */
	private void defineColumnAvgPace(final PixelConverter pc) {

		final TreeColumnDefinition colDef = TreeColumnFactory.AVG_PACE.createColumn(_columnManager, pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final float pace = ((TVITagViewItem) element).colAvgPace * UI.UNIT_VALUE_DISTANCE;

				if (pace == 0.0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(UI.format_mm_ss((long) pace));
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: avg pulse
	 */
	private void defineColumnAvgPulse(final PixelConverter pc) {

		final TreeColumnDefinition colDef = TreeColumnFactory.AVG_PULSE.createColumn(_columnManager, pc);
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
		});
	}

	/**
	 * column: avg speed km/h - mph
	 */
	private void defineColumnAvgSpeed(final PixelConverter pc) {

		final TreeColumnDefinition colDef = TreeColumnFactory.AVG_SPEED.createColumn(_columnManager, pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				final float colAvgSpeed = ((TVITagViewItem) element).colAvgSpeed / UI.UNIT_VALUE_DISTANCE;
				if (colAvgSpeed != 0) {

					cell.setText(_nf1.format(colAvgSpeed));
					setCellColor(cell, element);
				}
			}
		});
	}

	/**
	 * column: avg temperature
	 */
	private void defineColumnAvgTemperature(final PixelConverter pc) {

		final TreeColumnDefinition colDef = TreeColumnFactory.AVG_TEMPERATURE.createColumn(_columnManager, pc);
		colDef.setLabelProvider(new CellLabelProvider() {

			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				if (element instanceof TVITagViewTagCategory) {
					return;
				}

				final TVITagViewItem tviTagViewItem = (TVITagViewItem) element;
				float temperature = tviTagViewItem.colAvgTemperature;
				if (temperature != 0) {

					if (UI.UNIT_VALUE_TEMPERATURE != 1) {
						temperature = temperature * UI.UNIT_FAHRENHEIT_MULTI + UI.UNIT_FAHRENHEIT_ADD;
					}

					cell.setText(_nf1.format(temperature));
					setCellColor(cell, element);
				}
			}
		});
	}

	/**
	 * column: distance (km/miles)
	 */
	private void defineColumnDistance(final PixelConverter pc) {

		final TreeColumnDefinition colDef = TreeColumnFactory.DISTANCE.createColumn(_columnManager, pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				if (element instanceof TVITagViewTagCategory) {
					return;
				}

				final long colDistance = ((TVITagViewItem) element).colDistance;
				if (colDistance != 0) {

					final String distance = _nf1.format(((float) colDistance) / 1000 / UI.UNIT_VALUE_DISTANCE);

					cell.setText(distance);
					setCellColor(cell, element);
				}
			}
		});
	}

	/**
	 * column: max altitude
	 */
	private void defineColumnMaxAltitude(final PixelConverter pc) {

		final TreeColumnDefinition colDef = TreeColumnFactory.MAX_ALTITUDE.createColumn(_columnManager, pc);
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
	}

	/**
	 * column: max pulse
	 */
	private void defineColumnMaxPulse(final PixelConverter pc) {

		final TreeColumnDefinition colDef = TreeColumnFactory.MAX_PULSE.createColumn(_columnManager, pc);
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
	}

	/**
	 * column: max speed
	 */
	private void defineColumnMaxSpeed(final PixelConverter pc) {

		final TreeColumnDefinition colDef = TreeColumnFactory.MAX_SPEED.createColumn(_columnManager, pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				if (element instanceof TVITagViewTagCategory) {
					return;
				}

				final float colMaxSpeed = ((TVITagViewItem) element).colMaxSpeed;
				if (colMaxSpeed != 0) {

					cell.setText(_nf1.format(colMaxSpeed / UI.UNIT_VALUE_DISTANCE));
					setCellColor(cell, element);
				}
			}
		});
	}

	/**
	 * column: tags
	 */
	private void defineColumnTags(final PixelConverter pc) {

		final TreeColumnDefinition colDef = TreeColumnFactory.TOUR_TAGS.createColumn(_columnManager, pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new TourInfoToolTipCellLabelProvider() {

			@Override
			public Long getTourId(final ViewerCell cell) {

				if (_isToolTipInTags == false) {
					return null;
				}

				final Object element = cell.getElement();
				if (element instanceof TVITagViewTour) {
					return ((TVITagViewTour) element).tourId;
				}

				return null;
			}

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
	}

	/**
	 * column: paused time (h)
	 */
	private void defineColumnTimeBreak(final PixelConverter pc) {

		final TreeColumnDefinition colDef = TreeColumnFactory.PAUSED_TIME.createColumn(_columnManager, pc);
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

				cell.setText(_nf1.format(relativePausedTime));

				setCellColor(cell, element);

			}
		});
	}

	/**
	 * column: driving time (h)
	 */
	private void defineColumnTimeDriving(final PixelConverter pc) {

		final TreeColumnDefinition colDef = TreeColumnFactory.DRIVING_TIME.createColumn(_columnManager, pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				if (element instanceof TVITagViewTagCategory) {
					return;
				}

				final long drivingTime = ((TVITagViewItem) element).colDrivingTime;

				if (element instanceof TVITagViewTour) {
					if (_isDriveTimeFormat_hhmmss) {
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
	}

	/**
	 * column: recording time (h)
	 */
	private void defineColumnTimeRecording(final PixelConverter pc) {

		final TreeColumnDefinition colDef = TreeColumnFactory.RECORDING_TIME.createColumn(_columnManager, pc);
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
					if (_isRecTimeFormat_hhmmss) {
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
	}

	/**
	 * column: title
	 */
	private void defineColumnTitle(final PixelConverter pc) {

		final TreeColumnDefinition colDef = TreeColumnFactory.TITLE.createColumn(_columnManager, pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new TourInfoToolTipCellLabelProvider() {

			@Override
			public Long getTourId(final ViewerCell cell) {

				if (_isToolTipInTitle == false) {
					return null;
				}

				final Object element = cell.getElement();
				if (element instanceof TVITagViewTour) {
					return ((TVITagViewTour) element).tourId;
				}

				return null;
			}

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
	}

	@Override
	public void dispose() {

		getSite().getPage().removePostSelectionListener(_postSelectionListener);
		TourManager.getInstance().removeTourEventListener(_tourEventListener);
		_prefStore.removePropertyChangeListener(_prefChangeListener);

		_imgTag.dispose();
		_imgTagRoot.dispose();
		_imgTagCategory.dispose();

		super.dispose();
	}

	private void enableActions() {

		final StructuredSelection selection = (StructuredSelection) _tagViewer.getSelection();
		final int treeItems = _tagViewer.getTree().getItemCount();

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
		final boolean isOneTour = tourItems == 1;

		final int selectedItems = selection.size();
		final TVITagViewItem firstElement = (TVITagViewItem) selection.getFirstElement();
		final boolean firstElementHasChildren = firstElement == null ? false : firstElement.hasChildren();

		_tourDoubleClickState.canEditTour = isOneTour;
		_tourDoubleClickState.canOpenTour = isOneTour;
		_tourDoubleClickState.canQuickEditTour = isOneTour;
		_tourDoubleClickState.canEditMarker = isOneTour;
		_tourDoubleClickState.canAdjustAltitude = isOneTour;

		_actionEditTour.setEnabled(isOneTour);
		_actionOpenTour.setEnabled(isOneTour);
		_actionEditQuick.setEnabled(isOneTour);

		// action: set tour type
		final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();
		_actionSetTourType.setEnabled(isTourSelected && tourTypes.size() > 0);

		// action: add tag
		_actionAddTag.setEnabled(isTourSelected);

		ArrayList<Long> existingTagIds = null;
		long existingTourTypeId = TourDatabase.ENTITY_IS_NOT_SAVED;
		if (firstTour != null && isOneTour) {

			// one tour is selected

			existingTagIds = firstTour.tagIds;
			existingTourTypeId = firstTour.tourTypeId;

			if (existingTagIds != null && existingTagIds.size() > 0) {

				// at least one tag is within the tour

				_actionRemoveAllTags.setEnabled(true);
				_actionRemoveTag.setEnabled(true);
			} else {
				// tags are not available
				_actionRemoveAllTags.setEnabled(false);
				_actionRemoveTag.setEnabled(false);
			}
		} else {

			// multiple tours are selected

			_actionRemoveTag.setEnabled(isTourSelected);
			_actionRemoveAllTags.setEnabled(isTourSelected);
		}

		// enable rename action
		if (selectedItems == 1) {
			if (isTagSelected) {
				_actionRenameTag.setText(Messages.action_tag_rename_tag);
				_actionRenameTag.setEnabled(true);
			} else if (isCategorySelected) {
				_actionRenameTag.setText(Messages.action_tag_rename_tag_category);
				_actionRenameTag.setEnabled(true);

			} else {
				_actionRenameTag.setEnabled(false);
			}
		} else {
			_actionRenameTag.setEnabled(false);
		}

		/*
		 * tree expand type can be set if only tags are selected or when an item is selected which
		 * is not a category
		 */
		_actionSetTagStructure.setEnabled(isTagSelected || (items == 1 && categoryItems == 0));
		_actionSetAllTagStructures.setEnabled(treeItems > 0);

		_actionExpandSelection.setEnabled(firstElement == null ? false : //
				selectedItems == 1 ? firstElementHasChildren : //
						true);

		_actionCollapseOthers.setEnabled(selectedItems == 1 && firstElementHasChildren);
		_actionCollapseAll.setEnabled(treeItems > 0);

		// enable/disable actions for tags/tour types
		TagManager.enableRecentTagActions(isTourSelected, existingTagIds);
		TourTypeMenuManager.enableRecentTourTypeActions(isTourSelected, existingTourTypeId);
	}

	private void fillContextMenu(final IMenuManager menuMgr) {

		menuMgr.add(_actionCollapseOthers);
		menuMgr.add(_actionExpandSelection);
		menuMgr.add(_actionCollapseAll);

		menuMgr.add(new Separator());
		menuMgr.add(_actionRenameTag);
		menuMgr.add(_actionSetTagStructure);
		menuMgr.add(_actionSetAllTagStructures);

		menuMgr.add(new Separator());
		menuMgr.add(_actionEditQuick);
		menuMgr.add(_actionEditTour);
		menuMgr.add(_actionOpenTour);

		// tour type actions
		menuMgr.add(new Separator());
		menuMgr.add(_actionSetTourType);
		TourTypeMenuManager.fillMenuRecentTourTypes(menuMgr, this, true);

		// tour tag actions
		menuMgr.add(new Separator());
		menuMgr.add(_actionAddTag);
		TagManager.fillMenuRecentTags(menuMgr, this, true, true);
		menuMgr.add(_actionRemoveTag);
		menuMgr.add(_actionRemoveAllTags);
		menuMgr.add(_actionOpenTagPrefs);

		enableActions();
	}

	private void fillToolBar() {
		/*
		 * action in the view toolbar
		 */
		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

		// recreate the toolbar
		tbm.removeAll();

		tbm.add(_actionExpandSelection);
		tbm.add(_actionCollapseAll);

		tbm.add(_actionRefreshView);

		tbm.update(true);
	}

	private void fillViewMenu() {

		/*
		 * fill view menu
		 */
		final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();

		menuMgr.add(_actionSetLayoutFlat);
		menuMgr.add(_actionSetLayoutHierarchical);

		menuMgr.add(new Separator());
		menuMgr.add(_actionModifyColumns);

	}

	public ColumnManager getColumnManager() {
		return _columnManager;
	}

//	@SuppressWarnings("unchecked")
//	@Override
//	public Object getAdapter(final Class adapter) {
//
//		if (adapter == ColumnViewer.class) {
//			return _tagViewer;
//		}
//
//		return Platform.getAdapterManager().getAdapter(this, adapter);
//	}

	public ArrayList<TourData> getSelectedTours() {

		// get selected tours
		final IStructuredSelection selectedTours = ((IStructuredSelection) _tagViewer.getSelection());

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
		return _tagViewer;
	}

	private void readDisplayFormats() {

		_isRecTimeFormat_hhmmss = _prefStore.getString(ITourbookPreferences.VIEW_LAYOUT_RECORDING_TIME_FORMAT).equals(
				PrefPageAppearanceView.VIEW_TIME_LAYOUT_HH_MM_SS);

		_isDriveTimeFormat_hhmmss = _prefStore.getString(ITourbookPreferences.VIEW_LAYOUT_DRIVING_TIME_FORMAT).equals(
				PrefPageAppearanceView.VIEW_TIME_LAYOUT_HH_MM_SS);
	}

	public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

		_viewerContainer.setRedraw(false);
		{
			final Object[] expandedElements = _tagViewer.getExpandedElements();
			final ISelection selection = _tagViewer.getSelection();

			_tagViewer.getTree().dispose();

			createTagViewer(_viewerContainer);
			_viewerContainer.layout();

			_tagViewer.setInput(_bootItem = new TVITagViewRoot(this, _tagViewLayout));

			_tagViewer.setExpandedElements(expandedElements);
			_tagViewer.setSelection(selection);
		}
		_viewerContainer.setRedraw(true);

		return _tagViewer;
	}

	/**
	 * reload the content of the tag viewer
	 */
	public void reloadViewer() {

		final Tree tree = _tagViewer.getTree();
		tree.setRedraw(false);
		{
			final Object[] expandedElements = _tagViewer.getExpandedElements();

			_tagViewer.setInput(_bootItem = new TVITagViewRoot(this, _tagViewLayout));
			_tagViewer.setExpandedElements(expandedElements);
		}
		tree.setRedraw(true);
	}

	private void restoreState() {

		_tagViewLayout = TAG_VIEW_LAYOUT_HIERARCHICAL;

		// restore view layout
		try {

			final int viewLayout = _state.getInt(MEMENTO_TAG_VIEW_LAYOUT);
			switch (viewLayout) {

			case TAG_VIEW_LAYOUT_FLAT:

				_tagViewLayout = viewLayout;
				_actionSetLayoutFlat.setChecked(true);
				break;

			case TAG_VIEW_LAYOUT_HIERARCHICAL:

				_tagViewLayout = viewLayout;
				_actionSetLayoutHierarchical.setChecked(true);
				break;

			default:
				break;
			}

		} catch (final NumberFormatException e) {

			// set default tag view layout
			_tagViewLayout = TAG_VIEW_LAYOUT_HIERARCHICAL;
			_actionSetLayoutHierarchical.setChecked(true);
		}

		updateToolTipState();
	}

	private void saveState() {

		_columnManager.saveState(_state);

		// save view layout
		_state.put(MEMENTO_TAG_VIEW_LAYOUT, _tagViewLayout);

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
//		} else if (element instanceof TVITagViewTour) {
//			cell.setForeground(JFaceResources.getColorRegistry().get(UI.VIEW_COLOR_TOUR));
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
		_tagViewLayout = tagViewStructure;
		reloadViewer();
	}

	private void updateToolTipState() {

		_isToolTipInTag = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TAGGING_TAG);
		_isToolTipInTitle = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TAGGING_TITLE);
		_isToolTipInTags = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TAGGING_TAGS);
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
						tagItem.refresh(_tagViewer, changedTags.getModifiedTours(), changedTags.isAddMode());

						// update tag totals
						TVITagViewItem.readTagTotals(tagItem);

						// update viewer
						_tagViewer.refresh(tagItem);

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
			_tagViewer.remove(deletedTourItems.toArray());
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
							_tagViewer.update(tourItem, null);

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
