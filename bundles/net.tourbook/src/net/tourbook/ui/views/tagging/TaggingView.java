/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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

import gnu.trove.list.array.TLongArrayList;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.TreeColumnDefinition;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageTags;
import net.tourbook.preferences.PrefPageViewColors;
import net.tourbook.tag.ActionMenuSetAllTagStructures;
import net.tourbook.tag.ActionMenuSetTagStructure;
import net.tourbook.tag.ActionRenameTag;
import net.tourbook.tag.ChangedTags;
import net.tourbook.tag.TagMenuManager;
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
import net.tourbook.ui.UI;
import net.tourbook.ui.action.ActionCollapseAll;
import net.tourbook.ui.action.ActionCollapseOthers;
import net.tourbook.ui.action.ActionEditQuick;
import net.tourbook.ui.action.ActionEditTour;
import net.tourbook.ui.action.ActionExpandSelection;
import net.tourbook.ui.action.ActionModifyColumns;
import net.tourbook.ui.action.ActionOpenTour;
import net.tourbook.ui.action.ActionRefreshView;
import net.tourbook.ui.action.ActionSetTourTypeMenu;
import net.tourbook.ui.views.TourInfoToolTipCellLabelProvider;
import net.tourbook.ui.views.TourInfoToolTipStyledCellLabelProvider;
import net.tourbook.ui.views.TreeViewerTourInfoToolTip;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
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
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

public class TaggingView extends ViewPart implements ITourProvider, ITourViewer {

	static public final String				ID								= "net.tourbook.views.tagViewID";					//$NON-NLS-1$

	private static final String				MEMENTO_TAG_VIEW_LAYOUT			= "tagview.layout";								//$NON-NLS-1$

	/**
	 * The expanded tag items have these structure:
	 * <p>
	 * 1. Type<br>
	 * 2. id/year/month<br>
	 * <br>
	 * 3. Type<br>
	 * 4. id/year/month<br>
	 * ...
	 */
	private static final String				STATE_EXPANDED_ITEMS			= "STATE_EXPANDED_ITEMS";							//$NON-NLS-1$

	private static final int				STATE_ITEM_TYPE_SEPARATOR		= -1;

	private static final int				STATE_ITEM_TYPE_CATEGORY		= 1;
	private static final int				STATE_ITEM_TYPE_TAG				= 2;
	private static final int				STATE_ITEM_TYPE_YEAR			= 3;
	private static final int				STATE_ITEM_TYPE_MONTH			= 4;

	static final int						TAG_VIEW_LAYOUT_FLAT			= 0;
	static final int						TAG_VIEW_LAYOUT_HIERARCHICAL	= 10;
	private static final NumberFormat		_nf1							= NumberFormat.getNumberInstance();

	{
		_nf1.setMinimumFractionDigits(1);
		_nf1.setMaximumFractionDigits(1);
	}

	private final IPreferenceStore			_prefStore						= TourbookPlugin.getPrefStore();

	private final IDialogSettings			_state							= TourbookPlugin.getState(ID);
	private int								_tagViewLayout					= TAG_VIEW_LAYOUT_HIERARCHICAL;

	private boolean							_isRecTimeFormat_hhmmss;

	private boolean							_isDriveTimeFormat_hhmmss;
	private TreeViewerTourInfoToolTip		_tourInfoToolTip;

	private boolean							_isToolTipInTag;
	private boolean							_isToolTipInTitle;
	private boolean							_isToolTipInTags;
	private TagMenuManager					_tagMenuMgr;

	private TourDoubleClickState			_tourDoubleClickState			= new TourDoubleClickState();
	/*
	 * resources
	 */
	private final Image						_imgTagCategory					= TourbookPlugin
																					.getImage(Messages.Image__tag_category);

	private final Image						_imgTag							= TourbookPlugin
																					.getImage(Messages.Image__tag);
	private final Image						_imgTagRoot						= TourbookPlugin
																					.getImage(Messages.Image__tag_root);
	private PixelConverter					_pc;
	/*
	 * UI controls
	 */
	private Composite						_viewerContainer;

	private TreeViewer						_tagViewer;

	private TVITagViewRoot					_rootItem;
	private ColumnManager					_columnManager;

	private PostSelectionProvider			_postSelectionProvider;

	private ITourEventListener				_tourEventListener;

	private ISelectionListener				_postSelectionListener;
	private IPropertyChangeListener			_prefChangeListener;
	private IPartListener2					_partListener;
	private ActionCollapseAll				_actionCollapseAll;

	private ActionCollapseOthers			_actionCollapseOthers;
	private ActionEditQuick					_actionEditQuick;
	private ActionEditTour					_actionEditTour;
	private ActionExpandSelection			_actionExpandSelection;
	private ActionMenuSetAllTagStructures	_actionSetAllTagStructures;
	private ActionMenuSetTagStructure		_actionSetTagStructure;
	private ActionModifyColumns				_actionModifyColumns;
	private ActionOpenTour					_actionOpenTour;
	private ActionOpenPrefDialog			_actionOpenTagPrefs;
	private ActionRefreshView				_actionRefreshView;
	private ActionRenameTag					_actionRenameTag;
	private ActionSetLayoutHierarchical		_actionSetLayoutHierarchical;
	private ActionSetLayoutFlat				_actionSetLayoutFlat;
	private ActionSetTourTypeMenu			_actionSetTourType;

	private class StateSegment {

		private long	__itemType;
		private long	__itemData;

		public StateSegment(final long itemType, final long itemData) {

			__itemType = itemType;
			__itemData = itemData;
		}
	}

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

		@Override
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

		@Override
		public int hashCode(final Object element) {
			return 0;
		}

	}

	private class TagContentProvider implements ITreeContentProvider {

		@Override
		public void dispose() {}

		@Override
		public Object[] getChildren(final Object parentElement) {

			if (parentElement instanceof TVITagViewItem) {
				return ((TVITagViewItem) parentElement).getFetchedChildrenAsArray();
			}

			return new Object[0];
		}

		@Override
		public Object[] getElements(final Object inputElement) {
			return getChildren(inputElement);
		}

		@Override
		public Object getParent(final Object element) {
			return ((TreeViewerItem) element).getParentItem();
		}

		@Override
		public boolean hasChildren(final Object element) {
			return ((TreeViewerItem) element).hasChildren();
		}

		@Override
		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {

			if (newInput == null) {
				return;
			}

			setTagViewTitle(newInput);
		}
	}

	private void addPartListener() {

		_partListener = new IPartListener2() {
			@Override
			public void partActivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TaggingView.this) {

					saveState();

//					TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR, null, TaggingView.this);
				}
			}

			@Override
			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partHidden(final IWorkbenchPartReference partRef) {}

			@Override
			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			@Override
			public void partOpened(final IWorkbenchPartReference partRef) {}

			@Override
			public void partVisible(final IWorkbenchPartReference partRef) {}
		};

		getViewSite().getPage().addPartListener(_partListener);
	}

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {
			@Override
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
					defineAllColumns();

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

			@Override
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

				if (selection instanceof SelectionDeletedTours) {
					final SelectionDeletedTours deletedTourSelection = (SelectionDeletedTours) selection;

					updateViewerAfterTourIsDeleted(_rootItem, deletedTourSelection.removedTours);
				}
			}
		};

		// register selection listener in the page
		getSite().getPage().addPostSelectionListener(_postSelectionListener);
	}

	private void addTourEventListener() {

		_tourEventListener = new ITourEventListener() {
			@Override
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

						updateViewerAfterTagStructureIsModified(_rootItem, changedTagsClone, isAddMode);
					}

				} else if (eventId == TourEventId.TAG_STRUCTURE_CHANGED || eventId == TourEventId.UPDATE_UI) {

					reloadViewer();

				} else if (eventId == TourEventId.TOUR_CHANGED && eventData instanceof TourEvent) {

					final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();
					if (modifiedTours != null) {
						updateViewerAfterTourIsModified(_rootItem, modifiedTours);
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

		_tagMenuMgr = new TagMenuManager(this, true);

		_actionRefreshView = new ActionRefreshView(this);
		_actionSetTagStructure = new ActionMenuSetTagStructure(this);
		_actionSetAllTagStructures = new ActionMenuSetAllTagStructures(this);
		_actionRenameTag = new ActionRenameTag(this);
		_actionOpenTagPrefs = new ActionOpenPrefDialog(Messages.action_tag_open_tagging_structure, PrefPageTags.ID);

		_actionExpandSelection = new ActionExpandSelection(this);
		_actionCollapseAll = new ActionCollapseAll(this);
		_actionCollapseOthers = new ActionCollapseOthers(this);

		_actionSetLayoutFlat = new ActionSetLayoutFlat(this);
		_actionSetLayoutHierarchical = new ActionSetLayoutHierarchical(this);

		_actionModifyColumns = new ActionModifyColumns(this);
	}

	@Override
	public void createPartControl(final Composite parent) {

		_pc = new PixelConverter(parent);

		// define all columns
		_columnManager = new ColumnManager(this, _state);
		defineAllColumns();

		createActions();
		fillViewMenu();

		// viewer must be created after the action are created
		createUI(parent);

		// set selection provider
		getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));

		addTourEventListener();
		addPrefListener();
		addPartListener();
		addSelectionListener();

		enableActions();

		restoreState();
		readDisplayFormats();

		reloadViewer();

		restoreState_Viewer();
	}

	private void createUI(final Composite parent) {

		_viewerContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
		{
			createUI_10_TagViewer(_viewerContainer);
		}
	}

	private void createUI_10_TagViewer(final Composite parent) {

		/*
		 * Create tree
		 */
		final Tree tree = new Tree(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FLAT | SWT.FULL_SELECTION | SWT.MULTI);

		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		tree.setHeaderVisible(true);
		tree.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

		/*
		 * Create viewer
		 */
		_tagViewer = new TreeViewer(tree);
		_columnManager.createColumns(_tagViewer);

		_tagViewer.setContentProvider(new TagContentProvider());
		_tagViewer.setComparer(new TagComparer());
		_tagViewer.setComparator(new TagComparator());
		_tagViewer.setUseHashlookup(true);

		_tagViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
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

			@Override
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
		createUI_20_ContextMenu();

		fillToolBar();

		// set tour info tooltip provider
		_tourInfoToolTip = new TreeViewerTourInfoToolTip(_tagViewer);
	}

	/**
	 * create the views context menu
	 */
	private void createUI_20_ContextMenu() {

		final Tree tree = (Tree) _tagViewer.getControl();

		final MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(final IMenuManager manager) {
				fillContextMenu(manager);
			}
		});

		// add the context menu to the tree viewer

		final Menu treeContextMenu = menuMgr.createContextMenu(tree);
		treeContextMenu.addMenuListener(new MenuAdapter() {
			@Override
			public void menuHidden(final MenuEvent e) {
				_tagMenuMgr.onHideMenu();
			}

			@Override
			public void menuShown(final MenuEvent menuEvent) {
				_tagMenuMgr.onShowMenu(menuEvent, tree, Display.getCurrent().getCursorLocation(), _tourInfoToolTip);
			}
		});

		_columnManager.createHeaderContextMenu(tree, treeContextMenu);
	}

	/**
	 * Defines all columns for the table viewer in the column manager
	 */
	private void defineAllColumns() {

		defineColumn1stColumn();
		defineColumnTitle();
		defineColumnTags();
		defineColumnTimeRecording();
		defineColumnTimeDriving();
		defineColumnTimeBreak();
		defineColumnDistance();
		defineColumnAltitudeUp();
		defineColumnAltitudeDown();
		defineColumnMaxAltitude();
		defineColumnMaxSpeed();
		defineColumnMaxPulse();
		defineColumnAvgSpeed();
		defineColumnAvgPace();
		defineColumnAvgPulse();
		defineColumnAvgCadence();
		defineColumnAvgTemperature();
	}

	/**
	 * tree column: category/tag/year/month/tour
	 */
	private void defineColumn1stColumn() {

		final TreeColumnDefinition colDef = TreeColumnFactory.TAG.createColumn(_columnManager, _pc);
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
	private void defineColumnAltitudeDown() {

		final TreeColumnDefinition colDef = TreeColumnFactory.ALTITUDE_DOWN.createColumn(_columnManager, _pc);
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
	private void defineColumnAltitudeUp() {

		final TreeColumnDefinition colDef = TreeColumnFactory.ALTITUDE_UP.createColumn(_columnManager, _pc);
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
	private void defineColumnAvgCadence() {

		final TreeColumnDefinition colDef = TreeColumnFactory.POWERTRAIN_AVG_CADENCE.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				if (element instanceof TVITagViewTagCategory) {
					return;
				}

				final float colAvgCadence = ((TVITagViewItem) element).colAvgCadence;
				if (colAvgCadence != 0) {

					cell.setText(_nf1.format(colAvgCadence));
					setCellColor(cell, element);
				}
			}
		});
	}

	/**
	 * column: avg pace min/km - min/mi
	 */
	private void defineColumnAvgPace() {

		final TreeColumnDefinition colDef = TreeColumnFactory.MOTION_AVG_PACE.createColumn(_columnManager, _pc);
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
	private void defineColumnAvgPulse() {

		final TreeColumnDefinition colDef = TreeColumnFactory.BODY_PULSE_AVG.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				if (element instanceof TVITagViewTagCategory) {
					return;
				}

				final float colAvgPulse = ((TVITagViewItem) element).colAvgPulse;
				if (colAvgPulse != 0) {

					cell.setText(_nf1.format(colAvgPulse));
					setCellColor(cell, element);
				}
			}
		});
	}

	/**
	 * column: avg speed km/h - mph
	 */
	private void defineColumnAvgSpeed() {

		final TreeColumnDefinition colDef = TreeColumnFactory.MOTION_AVG_SPEED.createColumn(_columnManager, _pc);
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
	private void defineColumnAvgTemperature() {

		final TreeColumnDefinition colDef = TreeColumnFactory.WEATHER_AVG_TEMPERATURE.createColumn(_columnManager, _pc);
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
	private void defineColumnDistance() {

		final TreeColumnDefinition colDef = TreeColumnFactory.MOTION_DISTANCE.createColumn(_columnManager, _pc);
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
	private void defineColumnMaxAltitude() {

		final TreeColumnDefinition colDef = TreeColumnFactory.ALTITUDE_MAX.createColumn(_columnManager, _pc);
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
	private void defineColumnMaxPulse() {

		final TreeColumnDefinition colDef = TreeColumnFactory.BODY_PULSE_MAX.createColumn(_columnManager, _pc);
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
	private void defineColumnMaxSpeed() {

		final TreeColumnDefinition colDef = TreeColumnFactory.MOTION_MAX_SPEED.createColumn(_columnManager, _pc);
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
	private void defineColumnTags() {

		final TreeColumnDefinition colDef = TreeColumnFactory.TOUR_TAGS.createColumn(_columnManager, _pc);
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
	private void defineColumnTimeBreak() {

		final TreeColumnDefinition colDef = TreeColumnFactory.TIME_PAUSED_TIME.createColumn(_columnManager, _pc);
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
	private void defineColumnTimeDriving() {

		final TreeColumnDefinition colDef = TreeColumnFactory.TIME_DRIVING_TIME.createColumn(_columnManager, _pc);
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
	private void defineColumnTimeRecording() {

		final TreeColumnDefinition colDef = TreeColumnFactory.TIME_RECORDING_TIME.createColumn(_columnManager, _pc);
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
	private void defineColumnTitle() {

		final TreeColumnDefinition colDef = TreeColumnFactory.TOUR_TITLE.createColumn(_columnManager, _pc);
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
		final boolean isItemsAvailable = treeItems > 0;

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
		_actionSetAllTagStructures.setEnabled(isItemsAvailable);

		_actionExpandSelection.setEnabled(firstElement == null ? false : //
				selectedItems == 1 ? firstElementHasChildren : //
						true);

		_actionCollapseOthers.setEnabled(selectedItems == 1 && firstElementHasChildren);
		_actionCollapseAll.setEnabled(isItemsAvailable);

		_tagMenuMgr.enableTagActions(isTourSelected, isOneTour, firstTour == null ? null : firstTour.tagIds);

		TourTypeMenuManager.enableRecentTourTypeActions(isTourSelected, isOneTour
				? firstTour.tourTypeId
				: TourDatabase.ENTITY_IS_NOT_SAVED);
	}

	private void fillContextMenu(final IMenuManager menuMgr) {

		menuMgr.add(_actionCollapseOthers);
		menuMgr.add(_actionExpandSelection);
		menuMgr.add(_actionCollapseAll);

		menuMgr.add(new Separator());
		menuMgr.add(_actionRenameTag);
		menuMgr.add(_actionSetTagStructure);
		menuMgr.add(_actionSetAllTagStructures);
		menuMgr.add(_actionOpenTagPrefs);

		menuMgr.add(new Separator());
		menuMgr.add(_actionEditQuick);
		menuMgr.add(_actionEditTour);
		menuMgr.add(_actionOpenTour);

		_tagMenuMgr.fillTagMenu(menuMgr);

		// tour type actions
		menuMgr.add(new Separator());
		menuMgr.add(_actionSetTourType);
		TourTypeMenuManager.fillMenuWithRecentTourTypes(menuMgr, this, true);

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

		tbm.add(_actionSetLayoutFlat);
		tbm.add(_actionSetLayoutHierarchical);

		tbm.add(_actionRefreshView);

		tbm.update(true);
	}

	private void fillViewMenu() {

		/*
		 * fill view menu
		 */
		final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();

		menuMgr.add(new Separator());
		menuMgr.add(_actionModifyColumns);

	}

	@Override
	public ColumnManager getColumnManager() {
		return _columnManager;
	}

	@Override
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

	@Override
	public ColumnViewer getViewer() {
		return _tagViewer;
	}

	private void readDisplayFormats() {

		_isRecTimeFormat_hhmmss = _prefStore.getString(ITourbookPreferences.VIEW_LAYOUT_RECORDING_TIME_FORMAT).equals(
				PrefPageViewColors.VIEW_TIME_LAYOUT_HH_MM_SS);

		_isDriveTimeFormat_hhmmss = _prefStore.getString(ITourbookPreferences.VIEW_LAYOUT_DRIVING_TIME_FORMAT).equals(
				PrefPageViewColors.VIEW_TIME_LAYOUT_HH_MM_SS);
	}

	@Override
	public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

		_viewerContainer.setRedraw(false);
		{
			final Object[] expandedElements = _tagViewer.getExpandedElements();
			final ISelection selection = _tagViewer.getSelection();

			_tagViewer.getTree().dispose();

			createUI_10_TagViewer(_viewerContainer);
			_viewerContainer.layout();

			_tagViewer.setInput(_rootItem = new TVITagViewRoot(this, _tagViewLayout));

			_tagViewer.setExpandedElements(expandedElements);
			_tagViewer.setSelection(selection);
		}
		_viewerContainer.setRedraw(true);

		return _tagViewer;
	}

	/**
	 * reload the content of the tag viewer
	 */
	@Override
	public void reloadViewer() {

		final Tree tree = _tagViewer.getTree();
		tree.setRedraw(false);
		{
			final Object[] expandedElements = _tagViewer.getExpandedElements();

			_tagViewer.setInput(_rootItem = new TVITagViewRoot(this, _tagViewLayout));
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

	/**
	 * Restore viewer state after the viewer is loaded.
	 */
	private void restoreState_Viewer() {

		/*
		 * Expanded tag categories
		 */
		final long[] allStateItems = Util.getStateLongArray(_state, STATE_EXPANDED_ITEMS, null);
		if (allStateItems != null) {

			final ArrayList<TreePath> viewerTreePaths = new ArrayList<TreePath>();

			final ArrayList<StateSegment[]> allStateSegments = restoreState_Viewer_GetSegments(allStateItems);
			for (final StateSegment[] stateSegments : allStateSegments) {

				final ArrayList<Object> pathSegments = new ArrayList<>();

				// start tree items with the root and go deeper with every segment
				ArrayList<TreeViewerItem> treeItems = _rootItem.getFetchedChildren();

				for (final StateSegment stateSegment : stateSegments) {

					/*
					 * This is somehow recursive as it goes deeper into the child tree items until
					 * there are no children
					 */
					treeItems = restoreState_Viewer_ExpandItem(pathSegments, treeItems, stateSegment);
				}

				if (pathSegments.size() > 0) {
					viewerTreePaths.add(new TreePath(pathSegments.toArray()));
				}
			}

			if (viewerTreePaths.size() > 0) {
				_tagViewer.setExpandedTreePaths(viewerTreePaths.toArray(new TreePath[viewerTreePaths.size()]));
			}
		}
	}

	/**
	 * @param pathSegments
	 * @param treeItems
	 * @param stateSegment
	 * @return Returns children when it could be expanded otherwise <code>null</code>.
	 */
	private ArrayList<TreeViewerItem> restoreState_Viewer_ExpandItem(	final ArrayList<Object> pathSegments,
																		final ArrayList<TreeViewerItem> treeItems,
																		final StateSegment stateSegment) {

		if (treeItems == null) {
			return null;
		}

		final long stateData = stateSegment.__itemData;

		if (stateSegment.__itemType == STATE_ITEM_TYPE_CATEGORY) {

			for (final TreeViewerItem treeItem : treeItems) {

				if (treeItem instanceof TVITagViewTagCategory) {

					final TVITagViewTagCategory viewerCat = (TVITagViewTagCategory) treeItem;
					final long viewerCatId = viewerCat.getCategoryId();

					if (viewerCatId == stateData) {

						pathSegments.add(treeItem);

						return viewerCat.getFetchedChildren();
					}
				}
			}

		} else if (stateSegment.__itemType == STATE_ITEM_TYPE_TAG) {

			for (final TreeViewerItem treeItem : treeItems) {

				if (treeItem instanceof TVITagViewTag) {

					final TVITagViewTag viewerTag = (TVITagViewTag) treeItem;
					final long viewerTagId = viewerTag.tagId;

					if (viewerTagId == stateData) {

						pathSegments.add(treeItem);

						return viewerTag.getFetchedChildren();
					}
				}
			}

		} else if (stateSegment.__itemType == STATE_ITEM_TYPE_YEAR) {

			for (final TreeViewerItem treeItem : treeItems) {

				if (treeItem instanceof TVITagViewYear) {

					final TVITagViewYear viewerTagYear = (TVITagViewYear) treeItem;
					final long viewerYear = viewerTagYear.getYear();

					if (viewerYear == stateData) {

						pathSegments.add(treeItem);

						return viewerTagYear.getFetchedChildren();
					}
				}
			}

		} else if (stateSegment.__itemType == STATE_ITEM_TYPE_MONTH) {

			for (final TreeViewerItem treeItem : treeItems) {

				if (treeItem instanceof TVITagViewMonth) {

					final TVITagViewMonth viewerTagMonth = (TVITagViewMonth) treeItem;
					final long viewerYear = viewerTagMonth.getMonth();

					if (viewerYear == stateData) {

						pathSegments.add(treeItem);

						return viewerTagMonth.getFetchedChildren();
					}
				}
			}
		}

		return null;
	}

	/**
	 * Convert state structure into a 'segment' structure.
	 */
	private ArrayList<StateSegment[]> restoreState_Viewer_GetSegments(final long[] expandedItems) {

		final ArrayList<StateSegment[]> allTreePathSegments = new ArrayList<StateSegment[]>();
		final ArrayList<StateSegment> currentSegments = new ArrayList<StateSegment>();

		for (int itemIndex = 0; itemIndex < expandedItems.length;) {

			// ensure array bounds
			if (itemIndex + 1 >= expandedItems.length) {
				// this should not happen when data are not corrupted
				break;
			}

			final long itemType = expandedItems[itemIndex++];
			final long itemData = expandedItems[itemIndex++];

			if (itemType == STATE_ITEM_TYPE_SEPARATOR) {

				// a new tree path starts

				if (currentSegments.size() > 0) {

					// keep current tree path segments

					allTreePathSegments.add(currentSegments.toArray(new StateSegment[currentSegments.size()]));

					// start a new path
					currentSegments.clear();
				}

			} else {

				// a new segment is available

				if (itemType == STATE_ITEM_TYPE_CATEGORY
						|| itemType == STATE_ITEM_TYPE_TAG
						|| itemType == STATE_ITEM_TYPE_YEAR
						|| itemType == STATE_ITEM_TYPE_MONTH) {

					currentSegments.add(new StateSegment(itemType, itemData));
				}
			}
		}

		if (currentSegments.size() > 0) {
			allTreePathSegments.add(currentSegments.toArray(new StateSegment[currentSegments.size()]));
		}

		return allTreePathSegments;
	}

	private void saveState() {

		_columnManager.saveState(_state);

		// save view layout
		_state.put(MEMENTO_TAG_VIEW_LAYOUT, _tagViewLayout);

		saveState_ExpandedItems();
	}

	/**
	 * Save state for expanded tree items.
	 */
	private void saveState_ExpandedItems() {

		final Object[] visibleExpanded = _tagViewer.getVisibleExpandedElements();

		if (visibleExpanded.length == 0) {
			Util.setState(_state, STATE_EXPANDED_ITEMS, new long[0]);
			return;
		}

		final TLongArrayList expandedItems = new TLongArrayList();

		final TreePath[] expandedOpenedTreePaths = net.tourbook.common.UI.getExpandedOpenedItems(
				visibleExpanded,
				_tagViewer.getExpandedTreePaths());

		for (final TreePath expandedPath : expandedOpenedTreePaths) {

			// start a new path, allways set it twice to have a even structure
			expandedItems.add(STATE_ITEM_TYPE_SEPARATOR);
			expandedItems.add(STATE_ITEM_TYPE_SEPARATOR);

			for (int segmentIndex = 0; segmentIndex < expandedPath.getSegmentCount(); segmentIndex++) {

				final Object segment = expandedPath.getSegment(segmentIndex);

				if (segment instanceof TVITagViewTagCategory) {

					expandedItems.add(STATE_ITEM_TYPE_CATEGORY);
					expandedItems.add(((TVITagViewTagCategory) segment).tagCategoryId);

				} else if (segment instanceof TVITagViewTag) {

					expandedItems.add(STATE_ITEM_TYPE_TAG);
					expandedItems.add(((TVITagViewTag) segment).tagId);

				} else if (segment instanceof TVITagViewYear) {

					expandedItems.add(STATE_ITEM_TYPE_YEAR);
					expandedItems.add(((TVITagViewYear) segment).getYear());

				} else if (segment instanceof TVITagViewMonth) {

					expandedItems.add(STATE_ITEM_TYPE_MONTH);
					expandedItems.add(((TVITagViewMonth) segment).getMonth());
				}
			}
		}

		Util.setState(_state, STATE_EXPANDED_ITEMS, expandedItems.toArray());
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

		_tagViewer.getTree().setFocus();
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
