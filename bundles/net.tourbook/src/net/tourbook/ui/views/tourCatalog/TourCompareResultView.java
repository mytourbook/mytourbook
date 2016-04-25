/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.TreeColumnDefinition;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tag.TagMenuManager;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TourTypeMenuManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.TreeColumnFactory;
import net.tourbook.ui.action.ActionCollapseAll;
import net.tourbook.ui.action.ActionEditQuick;
import net.tourbook.ui.action.ActionEditTour;
import net.tourbook.ui.action.ActionModifyColumns;
import net.tourbook.ui.action.ActionOpenTour;
import net.tourbook.ui.action.ActionSetTourTypeMenu;
import net.tourbook.ui.views.TourInfoToolTipCellLabelProvider;
import net.tourbook.ui.views.TreeViewerTourInfoToolTip;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnViewer;
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.part.ViewPart;

public class TourCompareResultView extends ViewPart implements ITourViewer, ITourProvider {

	public static final String					ID					= "net.tourbook.views.tourCatalog.CompareResultView";	//$NON-NLS-1$

	private final IPreferenceStore				_prefStore			= TourbookPlugin.getDefault().getPreferenceStore();
	private final IDialogSettings				_state				= TourbookPlugin.getDefault() //
																			.getDialogSettingsSection(ID);

	private TVICompareResultRootItem			_rootItem;

	private PostSelectionProvider				_postSelectionProvider;

	private ISelectionListener					_postSelectionListener;
	private IPartListener2						_partListener;
	private IPropertyChangeListener				_prefChangeListener;
	private ITourEventListener					_tourPropertyListener;
	private ITourEventListener					_compareTourPropertyListener;

	private boolean								_isToolbarCreated;

	private ColumnManager						_columnManager;
	private TagMenuManager						_tagMenuMgr;

	private SelectionRemovedComparedTours		_oldRemoveSelection	= null;

	private final NumberFormat					_nf1				= NumberFormat.getNumberInstance();
	private final NumberFormat					_nf2				= NumberFormat.getNumberInstance();
	{
		_nf1.setMinimumFractionDigits(1);
		_nf1.setMaximumFractionDigits(1);

		_nf2.setMinimumFractionDigits(2);
		_nf2.setMaximumFractionDigits(2);
	}

	/*
	 * resources
	 */
	private Image								_dbImage			= TourbookPlugin.getImageDescriptor(//
																			Messages.Image__database).createImage(true);

	private TreeViewerTourInfoToolTip			_tourInfoToolTip;
	private PixelConverter						_pc;

	/*
	 * UI controls
	 */
	private Composite							_viewerContainer;
	private CheckboxTreeViewer					_tourViewer;

	private ActionSaveComparedTours				_actionSaveComparedTours;
	private ActionRemoveComparedTourSaveStatus	_actionRemoveComparedTourSaveStatus;
	private ActionCheckTours					_actionCheckTours;
	private ActionUncheckTours					_actionUncheckTours;
	private ActionModifyColumns					_actionModifyColumns;
	private ActionCollapseAll					_actionCollapseAll;

	private ActionEditQuick						_actionEditQuick;
	private ActionEditTour						_actionEditTour;
	private ActionSetTourTypeMenu				_actionSetTourType;
	private ActionOpenTour						_actionOpenTour;

	class ResultContentProvider implements ITreeContentProvider {

		@Override
		public void dispose() {}

		@Override
		public Object[] getChildren(final Object parentElement) {
			return ((TreeViewerItem) parentElement).getFetchedChildrenAsArray();
		}

		@Override
		public Object[] getElements(final Object inputElement) {
			return _rootItem.getFetchedChildrenAsArray();
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
		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	public TourCompareResultView() {}

	private void addCompareTourPropertyListener() {

		_compareTourPropertyListener = new ITourEventListener() {
			@Override
			public void tourChanged(final IWorkbenchPart part, final TourEventId propertyId, final Object propertyData) {

				if (propertyId == TourEventId.COMPARE_TOUR_CHANGED
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

//							resultItem.movedStartIndex = compareTourProperty.startIndex;
//							resultItem.movedEndIndex = compareTourProperty.endIndex;

							resultItem.movedSpeed = compareTourProperty.speed;

							// update viewer
							_tourViewer.update(comparedTourItem, null);
						}

					} else {

						// compare result is saved

						// find compared tour in the viewer
						final ArrayList<TVICompareResultComparedTour> comparedTours = new ArrayList<TVICompareResultComparedTour>();
						getComparedTours(comparedTours, _rootItem, compareIds);

						if (comparedTours.size() > 0) {

							final TVICompareResultComparedTour compareTourItem = comparedTours.get(0);

							if (compareTourProperty.isDataSaved) {

								// compared tour was saved

								compareTourItem.dbStartIndex = compareTourProperty.startIndex;
								compareTourItem.dbEndIndex = compareTourProperty.endIndex;

								compareTourItem.dbSpeed = compareTourProperty.speed;

							} else {

//								compareTourItem.movedStartIndex = compareTourProperty.startIndex;
//								compareTourItem.movedEndIndex = compareTourProperty.endIndex;

								compareTourItem.movedSpeed = compareTourProperty.speed;
							}

							// update viewer
							_tourViewer.update(compareTourItem, null);
						}
					}
				}
			}
		};

		TourManager.getInstance().addTourEventListener(_compareTourPropertyListener);
	}

	/**
	 * set the part listener to save the view settings, the listeners are called before the controls
	 * are disposed
	 */
	private void addPartListeners() {

		_partListener = new IPartListener2() {

			@Override
			public void partActivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourCompareResultView.this) {

					saveState();

					clearCompareResult();
				}
			}

			@Override
			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partHidden(final IWorkbenchPartReference partRef) {}

			@Override
			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			@Override
			public void partOpened(final IWorkbenchPartReference partRef) {
				/*
				 * add the actions in the part open event so they are appended AFTER the actions
				 * which are defined in the plugin.xml
				 */
				fillToolbar();
			}

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

				if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// measurement system has changed

					net.tourbook.ui.UI.updateUnits();

					_columnManager.saveState(_state);
					_columnManager.clearColumns();
					defineAllColumns(_viewerContainer);

					recreateViewer(null);

				} else if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

					_tourViewer.getTree().setLinesVisible(
							_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

					_tourViewer.refresh();

					/*
					 * the tree must be redrawn because the styled text does not show with the new
					 * color
					 */
					_tourViewer.getTree().redraw();
				}
			}
		};

		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	/**
	 * Listen to post selections
	 */
	private void addSelectionListeners() {

		_postSelectionListener = new ISelectionListener() {

			@Override
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
				onSelectionChanged(part, selection);
			}
		};

		// register selection listener in the page
		getSite().getPage().addPostSelectionListener(_postSelectionListener);

	}

	private void addTourEventListener() {

		_tourPropertyListener = new ITourEventListener() {
			@Override
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if (part == TourCompareResultView.this) {
					return;
				}

				if (eventId == TourEventId.TOUR_CHANGED && eventData instanceof TourEvent) {

					final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();
					if (modifiedTours != null) {
						updateTourViewer(_rootItem, modifiedTours);
					}

				} else if (eventId == TourEventId.TAG_STRUCTURE_CHANGED) {

					reloadViewer();
				}
			}
		};
		TourManager.getInstance().addTourEventListener(_tourPropertyListener);
	}

	private void clearCompareResult() {

		TourCompareManager.getInstance().clearCompareResult();
	}

	private void createActions() {

		_actionSaveComparedTours = new ActionSaveComparedTours(this);
		_actionRemoveComparedTourSaveStatus = new ActionRemoveComparedTourSaveStatus(this);

		_actionCheckTours = new ActionCheckTours(this);
		_actionUncheckTours = new ActionUncheckTours(this);

		_actionSetTourType = new ActionSetTourTypeMenu(this);

		_tagMenuMgr = new TagMenuManager(this, true);

		_actionEditQuick = new ActionEditQuick(this);
		_actionEditTour = new ActionEditTour(this);
		_actionOpenTour = new ActionOpenTour(this);

		_actionModifyColumns = new ActionModifyColumns(this);
		_actionCollapseAll = new ActionCollapseAll(this);
	}

	@Override
	public void createPartControl(final Composite parent) {

		_pc = new PixelConverter(parent);

		// define all columns for the viewer
		_columnManager = new ColumnManager(this, _state);
		defineAllColumns(parent);

		createUI(parent);

		addPartListeners();
		addSelectionListeners();
		addCompareTourPropertyListener();
		addPrefListener();
		addTourEventListener();

		createActions();
		fillViewMenu();

		getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));

		_tourViewer.setInput(_rootItem = new TVICompareResultRootItem());
	}

	private void createUI(final Composite parent) {

		_viewerContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
		{
			createUI_10_TourViewer(_viewerContainer);
		}
	}

	private Control createUI_10_TourViewer(final Composite parent) {

		// tour tree
		final Tree tree = new Tree(parent, SWT.H_SCROLL
				| SWT.V_SCROLL
				| SWT.BORDER
				| SWT.MULTI
				| SWT.FULL_SELECTION
				| SWT.CHECK);

		GridDataFactory.fillDefaults().grab(true, true).applyTo(tree);

		tree.setHeaderVisible(true);
		tree.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

		_tourViewer = new ContainerCheckedTreeViewer(tree);
		_columnManager.createColumns(_tourViewer);

		_tourViewer.setContentProvider(new ResultContentProvider());
		_tourViewer.setUseHashlookup(true);

		_tourViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				onSelect(event);
			}
		});

		_tourViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(final DoubleClickEvent event) {

				// expand/collapse current item

				final Object treeItem = ((IStructuredSelection) event.getSelection()).getFirstElement();

				if (_tourViewer.getExpandedState(treeItem)) {
					_tourViewer.collapseToLevel(treeItem, 1);
				} else {
					_tourViewer.expandToLevel(treeItem, 1);
				}
			}
		});

		_tourViewer.getTree().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(final KeyEvent keyEvent) {
				if (keyEvent.keyCode == SWT.DEL) {
					removeComparedTourFromDb();
				}
			}
		});

		_tourViewer.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(final CheckStateChangedEvent event) {

				if (event.getElement() instanceof TVICompareResultComparedTour) {

					final TVICompareResultComparedTour compareResult = (TVICompareResultComparedTour) event
							.getElement();

					if (event.getChecked() && compareResult.isSaved()) {
						/*
						 * uncheck elements which are already stored for the reftour, it would be
						 * better to disable them, but this is not possible because this is a
						 * limitation by the OS
						 */
						_tourViewer.setChecked(compareResult, false);
					} else {
						enableActions();
					}
				} else {
					// uncheck all other tree items
					_tourViewer.setChecked(event.getElement(), false);
				}
			}
		});

		createUI_20_ContextMenu();

		// set tour info tooltip provider
		_tourInfoToolTip = new TreeViewerTourInfoToolTip(_tourViewer);

		return tree;
	}

	/**
	 * create the views context menu
	 */
	private void createUI_20_ContextMenu() {

		final MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(final IMenuManager manager) {
				fillContextMenu(manager);
			}
		});

		final Tree tree = (Tree) _tourViewer.getControl();
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

	private void defineAllColumns(final Composite parent) {

		defineColumn_ComparedTour();
		defineColumn_Diff();
		defineColumn_SpeedComputed();
		defineColumn_SpeedSaved();
		defineColumn_SpeedMoved();
		defineColumn_Distance();
		defineColumn_TimeInterval();
		defineColumn_TourType();
		defineColumn_Title();
		defineColumn_Tags();
	}

	/**
	 * tree column: reference tour/date
	 */
	private void defineColumn_ComparedTour() {

		final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, "comparedTour", SWT.LEAD); //$NON-NLS-1$

		colDef.setIsDefaultColumn();
		colDef.setColumnLabel(Messages.Compare_Result_Column_tour);
		colDef.setColumnHeaderText(Messages.Compare_Result_Column_tour);
		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(25) + 16);
		colDef.setCanModifyVisibility(false);
		colDef.setLabelProvider(new TourInfoToolTipCellLabelProvider() {

			@Override
			public Long getTourId(final ViewerCell cell) {

				final Object element = cell.getElement();
				if (element instanceof TVICompareResultReferenceTour) {

					return ((TVICompareResultReferenceTour) element).tourId;

				} else if (element instanceof TVICompareResultComparedTour) {

					return ((TVICompareResultComparedTour) element).comparedTourData.getTourId();
				}

				return null;
			}

			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				if (element instanceof TVICompareResultReferenceTour) {

					final TVICompareResultReferenceTour refItem = (TVICompareResultReferenceTour) element;
					cell.setText(refItem.label);

				} else if (element instanceof TVICompareResultComparedTour) {

					final TVICompareResultComparedTour compareItem = (TVICompareResultComparedTour) element;
					cell.setText(TourManager.getTourDateShort(compareItem.comparedTourData));

					// display an image when a tour is saved
					if (compareItem.isSaved()) {
						cell.setImage(_dbImage);
					} else {
						cell.setImage(null);
					}
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: altitude difference
	 */
	private void defineColumn_Diff() {

		final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, "diff", SWT.TRAIL); //$NON-NLS-1$

		colDef.setIsDefaultColumn();
		colDef.setColumnHeaderText(Messages.Compare_Result_Column_diff);
		colDef.setColumnHeaderToolTipText(Messages.Compare_Result_Column_diff_tooltip);
		colDef.setColumnLabel(Messages.Compare_Result_Column_diff_label);
		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(8));
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVICompareResultComparedTour) {

					final TVICompareResultComparedTour compareItem = (TVICompareResultComparedTour) element;

					final float value = (compareItem.minAltitudeDiff * 100)
							/ (compareItem.normalizedEndIndex - compareItem.normalizedStartIndex);

					cell.setText(Integer.toString((int) value));

					setCellColor(cell, element);
				}
			}
		});
	}

	/**
	 * column: distance
	 */
	private void defineColumn_Distance() {

		final TreeColumnDefinition colDef = TreeColumnFactory.MOTION_DISTANCE.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVICompareResultComparedTour) {

					final TVICompareResultComparedTour compareItem = (TVICompareResultComparedTour) element;

					cell.setText(_nf2.format(compareItem.compareDistance
							/ (1000 * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE)));
					setCellColor(cell, element);
				}
			}
		});
	}

	/**
	 * column: speed computed
	 */
	private void defineColumn_SpeedComputed() {

		final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, "speedComputed", SWT.TRAIL); //$NON-NLS-1$

		colDef.setIsDefaultColumn();
		colDef.setColumnHeaderText(UI.UNIT_LABEL_SPEED);
		colDef.setColumnUnit(UI.UNIT_LABEL_SPEED);
		colDef.setColumnHeaderToolTipText(Messages.Compare_Result_Column_kmh_tooltip);
		colDef.setColumnLabel(Messages.Compare_Result_Column_kmh_label);
		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(8));
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVICompareResultComparedTour) {

					final TVICompareResultComparedTour compareItem = (TVICompareResultComparedTour) element;

					cell.setText(_nf2.format(compareItem.compareSpeed / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE));
					setCellColor(cell, element);
				}
			}
		});
	}

	/**
	 * column: speed moved
	 */
	private void defineColumn_SpeedMoved() {

		final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, "speedMoved", SWT.TRAIL); //$NON-NLS-1$

		colDef.setColumnHeaderText(UI.UNIT_LABEL_SPEED);
		colDef.setColumnUnit(UI.UNIT_LABEL_SPEED);
		colDef.setColumnHeaderToolTipText(Messages.Compare_Result_Column_kmh_moved_tooltip);
		colDef.setColumnLabel(Messages.Compare_Result_Column_kmh_moved_label);
		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(8));
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVICompareResultComparedTour) {

					final TVICompareResultComparedTour compareItem = (TVICompareResultComparedTour) element;

					cell.setText(_nf2.format(compareItem.movedSpeed / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE));
					setCellColor(cell, element);
				}
			}
		});
	}

	/**
	 * column: speed saved
	 */
	private void defineColumn_SpeedSaved() {

		final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, "speedSaved", SWT.TRAIL); //$NON-NLS-1$

		colDef.setColumnHeaderText(UI.UNIT_LABEL_SPEED);
		colDef.setColumnUnit(UI.UNIT_LABEL_SPEED);
		colDef.setColumnHeaderToolTipText(Messages.Compare_Result_Column_kmh_db_tooltip);
		colDef.setColumnLabel(Messages.Compare_Result_Column_kmh_db_label);
		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(8));
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVICompareResultComparedTour) {

					final TVICompareResultComparedTour compareItem = (TVICompareResultComparedTour) element;

					cell.setText(_nf2.format(compareItem.dbSpeed / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE));
					setCellColor(cell, element);
				}
			}
		});
	}

	/**
	 * column: tags
	 */
	private void defineColumn_Tags() {

		final TreeColumnDefinition colDef = TreeColumnFactory.TOUR_TAGS.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVICompareResultComparedTour) {

					final Set<TourTag> tourTags = ((TVICompareResultComparedTour) element).comparedTourData
							.getTourTags();
					if (tourTags.size() == 0) {

						// the tags could have been removed, set empty field

						cell.setText(UI.EMPTY_STRING);

					} else {

						cell.setText(TourDatabase.getTagNames(tourTags));
						setCellColor(cell, element);
					}
				}
			}
		});
	}

	/**
	 * column: time interval
	 */
	private void defineColumn_TimeInterval() {

		final TreeColumnDefinition colDef = TreeColumnFactory.DATA_TIME_INTERVAL.createColumn(_columnManager, _pc);
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
	}

	/**
	 * column: title
	 */
	private void defineColumn_Title() {

		final TreeColumnDefinition colDef = TreeColumnFactory.TOUR_TITLE.createColumn(_columnManager, _pc);
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
	}

	/**
	 * column: tour type
	 */
	private void defineColumn_TourType() {

		final TreeColumnDefinition colDef = TreeColumnFactory.TOUR_TYPE.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVICompareResultComparedTour) {
					final TourData comparedTourData = ((TVICompareResultComparedTour) element).comparedTourData;
					final TourType tourType = comparedTourData.getTourType();
					if (tourType != null) {
						cell.setImage(net.tourbook.ui.UI.getInstance().getTourTypeImage(tourType.getTypeId()));
					}
				}
			}
		});
	}

	@Override
	public void dispose() {

		getSite().getPage().removePostSelectionListener(_postSelectionListener);
		getSite().getPage().removePartListener(_partListener);
		TourManager.getInstance().removeTourEventListener(_compareTourPropertyListener);
		TourManager.getInstance().removeTourEventListener(_tourPropertyListener);
		_prefStore.removePropertyChangeListener(_prefChangeListener);

		_dbImage.dispose();

		super.dispose();
	}

	private void enableActions() {

		final ITreeSelection selection = (ITreeSelection) _tourViewer.getSelection();

		int tourItems = 0;
		int otherItems = 0;
		int savedTourItems = 0;
		int unsavedTourItems = 0;
		TVICompareResultComparedTour firstTourItem = null;
		TVICompareResultComparedTour firstCheckedItem = null;
		TVICompareResultComparedTour firstSelectedItem = null;

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
					firstSelectedItem = comparedTourItem;
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
		for (final Object checkedElement : _tourViewer.getCheckedElements()) {

			if (checkedElement instanceof TVICompareResultComparedTour) {
				final TVICompareResultComparedTour comparedTourItem = (TVICompareResultComparedTour) checkedElement;

				// count tours
				if (tourItems <= 1) {
					firstTourItem = comparedTourItem;
					firstCheckedItem = comparedTourItem;
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
		boolean isOneTour = tourItems == 1 && otherItems == 0;
		boolean isOneTourSelected = selectedTours == 1;

		// check if the same tour is selected and/or checked
		if (tourItems == 2 && otherItems == 0 && firstSelectedItem == firstCheckedItem) {
			isOneTour = true;
			isOneTourSelected = true;
		}

		_actionCheckTours.setEnabled(unsavedTourItems > 0);
		_actionUncheckTours.setEnabled(checkedTours > 0);

		// action: save compare result
		_actionSaveComparedTours.setEnabled(unsavedTourItems > 0);

		// action: remove tour from saved compare result, currently only one tour item is supported
		_actionRemoveComparedTourSaveStatus.setEnabled(savedTourItems > 0);

		// actions: edit tour
		_actionEditQuick.setEnabled(isOneTourSelected);
		_actionEditTour.setEnabled(isOneTourSelected);
		_actionOpenTour.setEnabled(isOneTourSelected);

		// action: tour type
		final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();
		_actionSetTourType.setEnabled(isTourSelected && tourTypes.size() > 0);

		// tags: add/remove/remove all
		Set<TourTag> allExistingTags = null;
		long existingTourTypeId = TourDatabase.ENTITY_IS_NOT_SAVED;

		if (isOneTour) {

			// one tour is selected

			allExistingTags = firstTourItem.comparedTourData.getTourTags();

			final TourType tourType = firstTourItem.comparedTourData.getTourType();
			existingTourTypeId = tourType == null ? TourDatabase.ENTITY_IS_NOT_SAVED : tourType.getTypeId();
		}
		_tagMenuMgr.enableTagActions(isTourSelected, isOneTour, allExistingTags);

		TourTypeMenuManager.enableRecentTourTypeActions(isTourSelected, existingTourTypeId);
	}

	private void fillContextMenu(final IMenuManager menuMgr) {

		menuMgr.add(_actionSaveComparedTours);
		menuMgr.add(_actionRemoveComparedTourSaveStatus);
		menuMgr.add(_actionCheckTours);
		menuMgr.add(_actionUncheckTours);

		menuMgr.add(new Separator());
		menuMgr.add(_actionEditQuick);
		menuMgr.add(_actionEditTour);
		menuMgr.add(_actionOpenTour);

		// tour tag actions
		_tagMenuMgr.fillTagMenu(menuMgr);

		// tour type actions
		menuMgr.add(new Separator());
		menuMgr.add(_actionSetTourType);
		TourTypeMenuManager.fillMenuWithRecentTourTypes(menuMgr, this, true);

		enableActions();
	}

	private void fillToolbar() {

		// check if toolbar is created
		if (_isToolbarCreated) {
			return;
		}
		_isToolbarCreated = true;

		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

		tbm.add(_actionCollapseAll);

		tbm.update(true);
	}

	private void fillViewMenu() {

		/*
		 * fill view menu
		 */
		final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();
		menuMgr.add(_actionModifyColumns);
	}

	@Override
	public ColumnManager getColumnManager() {
		return _columnManager;
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

	private TVICompareResultComparedTour getSelectedComparedTour() {

		final TreeSelection selection = (TreeSelection) _tourViewer.getSelection();
		for (final Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {

			final Object treeItem = iterator.next();
			if (treeItem instanceof TVICompareResultComparedTour) {

				return (TVICompareResultComparedTour) treeItem;
			}
		}

		return null;
	}

	private TVICompareResultReferenceTour getSelectedRefTour() {

		final TreeSelection selection = (TreeSelection) _tourViewer.getSelection();
		for (final Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {

			final Object treeItem = iterator.next();
			if (treeItem instanceof TVICompareResultReferenceTour) {

				return (TVICompareResultReferenceTour) treeItem;
			}
		}

		return null;
	}

	@Override
	public ArrayList<TourData> getSelectedTours() {

		// get selected tours

		final IStructuredSelection selectedTours = ((IStructuredSelection) _tourViewer.getSelection());
		final ArrayList<TourData> selectedTourData = new ArrayList<TourData>();

		// loop: all selected tours
		for (final Iterator<?> iter = selectedTours.iterator(); iter.hasNext();) {

			final Object treeItem = iter.next();

			if (treeItem instanceof TVICompareResultComparedTour) {

				final TVICompareResultComparedTour compareItem = ((TVICompareResultComparedTour) treeItem);
				final TourData tourData = TourManager.getInstance().getTourData(
						compareItem.comparedTourData.getTourId());
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
	@Override
	public CheckboxTreeViewer getViewer() {
		return _tourViewer;
	}

	TVICompareResultComparedTour navigateTour(final boolean isNextTour) {

		final TVICompareResultComparedTour selectedCompareResult = getSelectedComparedTour();
		if (selectedCompareResult != null) {

			// compared tour is selected

			final TreeViewerItem parentItem = selectedCompareResult.getParentItem();
			if (parentItem instanceof TVICompareResultReferenceTour) {

				final TVICompareResultReferenceTour refTourItem = (TVICompareResultReferenceTour) parentItem;

				final ArrayList<TreeViewerItem> children = refTourItem.getChildren();
				for (int childIndex = 0; childIndex < children.size(); childIndex++) {

					final TreeViewerItem refTourChild = children.get(childIndex);
					if (refTourChild == selectedCompareResult) {

						if (isNextTour) {

							// navigate next

							if (childIndex < children.size() - 1) {

								// next tour is available

								final TreeViewerItem nextRefTourChild = children.get(childIndex + 1);
								if (nextRefTourChild instanceof TVICompareResultComparedTour) {
									return (TVICompareResultComparedTour) nextRefTourChild;
								}

							} else {

								// return first tour

								final TreeViewerItem nextRefTourChild = children.get(0);
								if (nextRefTourChild instanceof TVICompareResultComparedTour) {
									return (TVICompareResultComparedTour) nextRefTourChild;
								}
							}

						} else {

							// navigate previous

							if (childIndex > 0) {

								// previous tour is available

								final TreeViewerItem nextChild = children.get(childIndex - 1);
								if (nextChild instanceof TVICompareResultComparedTour) {
									return (TVICompareResultComparedTour) nextChild;
								}

							} else {

								// return last tour

								final TreeViewerItem prevChild = children.get(children.size() - 1);
								if (prevChild instanceof TVICompareResultComparedTour) {
									return (TVICompareResultComparedTour) prevChild;
								}
							}
						}
					}
				}
			}

		} else {

			final TVICompareResultReferenceTour selectedRefTour = getSelectedRefTour();
			if (selectedRefTour != null) {

				// ref tour is selected

				final ArrayList<TreeViewerItem> refChildren = selectedRefTour.getFetchedChildren();
				if (refChildren.size() > 0) {

					// navigate to the first child and ignore direction

					_tourViewer.expandToLevel(selectedRefTour, 1);

					// return first tour
					final TreeViewerItem nextRefTourChild = refChildren.get(0);
					if (nextRefTourChild instanceof TVICompareResultComparedTour) {
						return (TVICompareResultComparedTour) nextRefTourChild;
					}
				}
			}
		}

		return null;
	}

	private void onSelect(final SelectionChangedEvent event) {

		final IStructuredSelection selection = (IStructuredSelection) event.getSelection();

		final Object treeItem = selection.getFirstElement();

		if (treeItem instanceof TVICompareResultReferenceTour) {

			final TVICompareResultReferenceTour refItem = (TVICompareResultReferenceTour) treeItem;

			_postSelectionProvider.setSelection(new SelectionTourCatalogView(refItem.refTourItem.refId));

		} else if (treeItem instanceof TVICompareResultComparedTour) {

			final TVICompareResultComparedTour resultItem = (TVICompareResultComparedTour) treeItem;

			_postSelectionProvider.setSelection(new StructuredSelection(resultItem));
		}
	}

	private void onSelectionChanged(final IWorkbenchPart part, final ISelection selection) {

		if (part == TourCompareResultView.this) {
			return;
		}

		if (selection instanceof StructuredSelection) {

			final Object firstElement = ((StructuredSelection) selection).getFirstElement();

			if (firstElement instanceof TVICompareResultComparedTour) {

				_tourViewer.setSelection(selection, true);
			}

		} else if (selection instanceof SelectionRemovedComparedTours) {

			removeComparedToursFromViewer(selection);

		} else if (selection instanceof SelectionPersistedCompareResults) {

			final SelectionPersistedCompareResults selectionPersisted = (SelectionPersistedCompareResults) selection;

			final ArrayList<TVICompareResultComparedTour> persistedCompareResults = selectionPersisted.persistedCompareResults;

			if (persistedCompareResults.size() > 0) {

				final TVICompareResultComparedTour comparedTourItem = persistedCompareResults.get(0);

				// uncheck persisted tours
				_tourViewer.setChecked(comparedTourItem, false);

				// update changed item
				_tourViewer.update(comparedTourItem, null);

			}
		}
	}

	@Override
	public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

		_viewerContainer.setRedraw(false);
		{
			final Object[] expandedElements = _tourViewer.getExpandedElements();
			final ISelection selection = _tourViewer.getSelection();

			_tourViewer.getTree().dispose();

			createUI_10_TourViewer(_viewerContainer);
			_viewerContainer.layout();

			_tourViewer.setInput(_rootItem = new TVICompareResultRootItem());

			_tourViewer.setExpandedElements(expandedElements);
			_tourViewer.setSelection(selection);
		}
		_viewerContainer.setRedraw(true);

		return _tourViewer;
	}

	@Override
	public void reloadViewer() {

		final Tree tree = _tourViewer.getTree();
		tree.setRedraw(false);
		{
			final Object[] expandedElements = _tourViewer.getExpandedElements();
			final ISelection selection = _tourViewer.getSelection();

			_tourViewer.setInput(_rootItem = new TVICompareResultRootItem());

			_tourViewer.setExpandedElements(expandedElements);
			_tourViewer.setSelection(selection);
		}
		tree.setRedraw(true);
	}

	/**
	 * Remove compared tour from the database
	 */
	void removeComparedTourFromDb() {

		final StructuredSelection selection = (StructuredSelection) _tourViewer.getSelection();
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

			_postSelectionProvider.setSelection(selectionRemovedCompareTours);

			removeComparedToursFromViewer(selectionRemovedCompareTours);
		}
	}

	private void removeComparedToursFromViewer(final ISelection selection) {

		final SelectionRemovedComparedTours removedTourSelection = (SelectionRemovedComparedTours) selection;
		final ArrayList<Long> removedTourCompareIds = removedTourSelection.removedComparedTours;

		/*
		 * return when there are no removed tours or when the selection has not changed
		 */
		if (removedTourCompareIds.size() == 0 || removedTourSelection == _oldRemoveSelection) {
			return;
		}

		_oldRemoveSelection = removedTourSelection;

		/*
		 * find/update the removed compared tours in the viewer
		 */

		final ArrayList<TVICompareResultComparedTour> comparedTourItems = new ArrayList<TVICompareResultComparedTour>();
		getComparedTours(comparedTourItems, _rootItem, removedTourCompareIds);

		// reset entity for the removed compared tours
		for (final TVICompareResultComparedTour removedTourItem : comparedTourItems) {

			removedTourItem.compId = -1;

			removedTourItem.dbStartIndex = -1;
			removedTourItem.dbEndIndex = -1;
			removedTourItem.dbSpeed = 0;

//			removedTourItem.movedStartIndex = -1;
//			removedTourItem.movedEndIndex = -1;
			removedTourItem.movedSpeed = 0;
		}

		// update viewer
		_tourViewer.update(comparedTourItems.toArray(), null);
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
				for (final Object checkedItem : _tourViewer.getCheckedElements()) {
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
				final TreeSelection selection = (TreeSelection) _tourViewer.getSelection();
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
				_tourViewer.setCheckedElements(new Object[0]);

				// update persistent status
				_tourViewer.update(updatedItems.toArray(), null);

				// fire post selection to update the tour catalog view
				_postSelectionProvider.setSelection(compareResultSelection);

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

	private void saveState() {

		_columnManager.saveState(_state);
	}

	private void setCellColor(final ViewerCell cell, final Object element) {

		if (element instanceof TVICompareResultReferenceTour) {

			cell.setForeground(JFaceResources.getColorRegistry().get(net.tourbook.ui.UI.VIEW_COLOR_TITLE));

		} else if (element instanceof TVICompareResultComparedTour) {

			// show the saved tours in a different color

			if (((TVICompareResultComparedTour) (element)).isSaved()) {
				cell.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_GRAY));
			} else {
				// show the text with tour color
				cell.setForeground(JFaceResources.getColorRegistry().get(net.tourbook.ui.UI.VIEW_COLOR_TOUR));
//				cell.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));
			}
		}
	}

	@Override
	public void setFocus() {

		_tourViewer.getTree().setFocus();
	}

	@Override
	public void updateColumnHeader(final ColumnDefinition colDef) {
		// TODO Auto-generated method stub
		
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
						_tourViewer.update(compareItem, null);

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
