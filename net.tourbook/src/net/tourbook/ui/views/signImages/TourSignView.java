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
package net.tourbook.ui.views.signImages;

import gnu.trove.list.array.TLongArrayList;

import java.text.NumberFormat;
import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.Hack;
import net.tourbook.common.UI;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.TreeColumnDefinition;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourSign;
import net.tourbook.data.TourSignCategory;
import net.tourbook.photo.ILoadCallBack;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoUI;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.sign.SignManager;
import net.tourbook.sign.TVIPrefSign;
import net.tourbook.sign.TVIPrefSignRoot;
import net.tourbook.ui.action.ActionCollapseAll;
import net.tourbook.ui.action.ActionExpandSelection;
import net.tourbook.ui.action.ActionModifyColumns;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
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
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

public class TourSignView extends ViewPart implements ITourViewer {

	public static final String		ID							= "net.tourbook.ui.views.signImages.TourSignView";	//$NON-NLS-1$

	/**
	 * The expanded sign items have these structure:
	 * <p>
	 * 1. Type<br>
	 * 2. Id<br>
	 * <br>
	 * 3. Type<br>
	 * 4. Id<br>
	 * ...
	 */
	private static final String		STATE_EXPANDED_ITEMS		= "STATE_EXPANDED_ITEMS";							//$NON-NLS-1$

	private static final int		STATE_ITEM_TYPE_SEPARATOR	= -1;

	private static final int		STATE_ITEM_TYPE_CATEGORY	= 1;
	private static final int		STATE_ITEM_TYPE_SIGN		= 2;

	private static final String		STATE_LAST_IMPORT_FILE_PATH	= "STATE_LAST_IMPORT_FILE_PATH";					//$NON-NLS-1$

	private final IPreferenceStore	_prefStore					= TourbookPlugin.getPrefStore();

	private final IDialogSettings	_state						= TourbookPlugin.getState(ID);

	private IPartListener2			_partListener;
	private ISelectionListener		_postSelectionListener;
	private IPropertyChangeListener	_prefChangeListener;

	private PostSelectionProvider	_postSelectionProvider;

	private ActionModifyColumns		_actionModifyColumns;
	private ActionCreateSignImage	_actionCreateSign;

	private PixelConverter			_pc;

	private TreeViewer				_signViewer;

	private TVIPrefSignRoot			_rootItem;
	private ColumnManager			_columnManager;
	private TreeColumnDefinition	_colDefImage;
	/**
	 * Contains the tree column widget for the sign image.
	 */
	private TreeColumn				_tcSignImage;

	private int						_signImageColumn;
	private int						DEFAULT_IMAGE_WIDTH;
	private int						DEFAULT_ROW_HEIGHT;
	private int						MAX_ROW_HEIGHT;
	private int						_imageColumnWidth			= -1;
	private final NumberFormat		_nf_3_3						= NumberFormat.getNumberInstance();

	{
		_nf_3_3.setMinimumFractionDigits(3);
		_nf_3_3.setMaximumFractionDigits(3);
	}
	/*
	 * UI controls
	 */
	private Composite				_viewerContainer;

	public class ActionCreateSignCategory extends Action {

		private ActionCreateSignCategory() {
			setText(Messages.SignImage_View_Action_CreateSignCategory);
		}

		@Override
		public void run() {
			onActionCreateSignCategory();
		}
	}

	public class ActionCreateSignImage extends Action {

		private ActionCreateSignImage() {
			setText(Messages.SignImage_View_Action_ImportSignImage);
		}

		@Override
		public void run() {
			onActionImportSignImage();
		}
	}

	private class LoadImageCallback implements ILoadCallBack {

		private TreeViewerItem	__viewerItem;

		public LoadImageCallback(final TreeViewerItem viewerItem) {

			__viewerItem = viewerItem;
		}

		@Override
		public void callBackImageIsLoaded(final boolean isImageLoaded) {

			if (isImageLoaded == false) {
				return;
			}

			// run in UI thread
			Display.getDefault().syncExec(new Runnable() {

				@Override
				public void run() {

					if (_signViewer.getTree().isDisposed()) {
						return;
					}

					// update sign image

					// update image size in the viewer
					_signViewer.update(__viewerItem, null);

					// !!! refresh() and update() do not repaint a loaded image but a redraw() do
					_signViewer.getTree().redraw();
				}
			});

		}
	}

	/**
	 * Sort the signs and categories
	 */
	private final class SignViewerComparator extends ViewerComparator {
		@Override
		public int compare(final Viewer viewer, final Object obj1, final Object obj2) {
			if (obj1 instanceof TVIPrefSign && obj2 instanceof TVIPrefSign) {

				// sort signs by name
				final TourSign tourSign1 = ((TVIPrefSign) (obj1)).getTourSign();
				final TourSign tourSign2 = ((TVIPrefSign) (obj2)).getTourSign();

				return tourSign1.getSignName().compareTo(tourSign2.getSignName());

//			} else if (obj1 instanceof TVIPrefSign && obj2 instanceof TVIPrefSignCategory) {
//
//				// sort category before sign
//				return 1;
//
//			} else if (obj2 instanceof TVIPrefSign && obj1 instanceof TVIPrefSignCategory) {
//
//				// sort category before sign
//				return -1;
//
//			} else if (obj1 instanceof TVIPrefSignCategory && obj2 instanceof TVIPrefSignCategory) {
//
//				// sort categories by name
//				final TourSignCategory tourSignCat1 = ((TVIPrefSignCategory) (obj1)).getTourSignCategory();
//				final TourSignCategory tourSignCat2 = ((TVIPrefSignCategory) (obj2)).getTourSignCategory();
//
//				return tourSignCat1.getCategoryName().compareTo(tourSignCat2.getCategoryName());
			}

			return 0;
		}

		@Override
		public boolean isSorterProperty(final Object element, final String property) {
			// sort when the name has changed
			return true;
		}
	}

	private final class SignViewerContentProvider implements ITreeContentProvider {

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

//		public TreeViewerItem getRootItem() {
//			return _rootItem;
//		}

		@Override
		public boolean hasChildren(final Object element) {
			return ((TreeViewerItem) element).hasChildren();
		}

		@Override
		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	private class StateSegment {

		private long	__itemType;
		private long	__itemData;

		public StateSegment(final long itemType, final long itemData) {

			__itemType = itemType;
			__itemData = itemData;
		}
	}

	public TourSignView() {
		super();
	}

	private void addPartListener() {

		_partListener = new IPartListener2() {

			public void partActivated(final IWorkbenchPartReference partRef) {}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourSignView.this) {

//					TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR, null, TourMarkerView.this);
					saveState();
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

				if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// measurement system has changed

					_columnManager.saveState(_state);
					_columnManager.clearColumns();

					defineAllColumns();

					_signViewer = (TreeViewer) recreateViewer(_signViewer);

				} else if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

					_signViewer.getTree().setLinesVisible(
							_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

					_signViewer.refresh();

					/*
					 * the tree must be redrawn because the styled text does not show with the new
					 * color
					 */
					_signViewer.getTree().redraw();

				}
			}
		};
		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	/**
	 * listen for events when a tour is selected
	 */
	private void addSelectionListener() {

		_postSelectionListener = new ISelectionListener() {
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
				if (part == TourSignView.this) {
					return;
				}
				onSelectionChanged(selection);
			}
		};
		getSite().getPage().addPostSelectionListener(_postSelectionListener);
	}

	private void createActions() {

		_actionModifyColumns = new ActionModifyColumns(this);

		_actionCreateSign = new ActionCreateSignImage();
	}

	/**
	 * create the views context menu
	 */
	private void createContextMenu() {

		final MenuManager treeMenuMgr = new MenuManager();

		treeMenuMgr.setRemoveAllWhenShown(true);

		treeMenuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(final IMenuManager menuMgr) {
				fillContextMenu(menuMgr);
			}
		});

		final Tree tree = _signViewer.getTree();
		final Menu treeContextMenu = treeMenuMgr.createContextMenu(tree);

		tree.setMenu(treeContextMenu);

		_columnManager.createHeaderContextMenu(tree, treeContextMenu);
	}

	@Override
	public void createPartControl(final Composite parent) {

		// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		//
		// THIS IS ONLY FOR TESTING
		//
		// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//		PhotoImageCache.disposeAll();

		initUI(parent);

		createUI(parent);

		addSelectionListener();
		addPrefListener();
		addPartListener();

		createActions();
		fillToolbar();

		// set initial row height
		onResizeImageColumn();

		// set viewer root item
		_rootItem = new TVIPrefSignRoot(_signViewer);

		// show contents in the viewers
		_signViewer.setInput(this);

		restoreState_Viewer();

		enableControls();

		// this part is a selection provider
		getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));
	}

	private Composite createUI(final Composite parent) {

		// container
		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.margins(0, 0)
//				.spacing(SWT.DEFAULT, 0)
				.numColumns(1)
				.applyTo(container);
		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			createUI_20_SignViewer_Container(container);
		}

		return container;
	}

	private void createUI_20_SignViewer_Container(final Composite parent) {

		// define all columns for the viewer
		_columnManager = new ColumnManager(this, _state);
		defineAllColumns();

		_viewerContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_viewerContainer);
		GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
		{
			createUI_22_SignViewer_Tree(_viewerContainer);
		}
	}

	private void createUI_22_SignViewer_Tree(final Composite parent) {

		/*
		 * Create tree
		 */
		final Tree tree = new Tree(parent, //
				SWT.H_SCROLL //
						| SWT.V_SCROLL
//						| SWT.BORDER
						| SWT.MULTI
						| SWT.FULL_SELECTION);

		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.hint(_pc.convertWidthInCharsToPixels(60), _pc.convertHeightInCharsToPixels(30))
				.applyTo(tree);

		DEFAULT_ROW_HEIGHT = tree.getItemHeight();

		tree.setHeaderVisible(true);
		tree.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

		/*
		 * NOTE: MeasureItem, PaintItem and EraseItem are called repeatedly. Therefore, it is
		 * critical for performance that these methods be as efficient as possible.
		 */
		final Listener paintListener = new Listener() {
			@Override
			public void handleEvent(final Event event) {

				if (event.index == _signImageColumn //
						&& (event.type == SWT.MeasureItem || event.type == SWT.PaintItem)) {

					onPaintViewer(event);
				}
			}
		};
		tree.addListener(SWT.MeasureItem, paintListener);
		tree.addListener(SWT.PaintItem, paintListener);

		tree.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(final KeyEvent e) {

				if (e.keyCode == SWT.DEL) {
//					onDeleteTourSign();
				}
			}
		});

		/*
		 * Create viewer
		 */
		_signViewer = new TreeViewer(tree);

		_columnManager.createColumns(_signViewer);

		_tcSignImage = _colDefImage.getTreeColumn();
		_signImageColumn = _colDefImage.getCreateIndex();

		_signViewer.setContentProvider(new SignViewerContentProvider());
		_signViewer.setComparator(new SignViewerComparator());

		_signViewer.setUseHashlookup(true);

		_signViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(final DoubleClickEvent event) {

				final Object selection = ((IStructuredSelection) _signViewer.getSelection()).getFirstElement();

				if (selection instanceof TVIPrefSign) {

					// sign is selected

//					onRenameTourSign();

//				} else if (selection instanceof TVIPrefSignCategory) {
//
//					// expand/collapse current item
//					final TVIPrefSignCategory treeItem = (TVIPrefSignCategory) selection;
//
//					if (_signViewer.getExpandedState(treeItem)) {
//
//						_signViewer.collapseToLevel(treeItem, 1);
//
//					} else {
//
////						if (_expandedItem != null) {
////							_signViewer.collapseToLevel(_expandedItem, 1);
////						}
//
//						_signViewer.expandToLevel(treeItem, 1);
////						_expandedItem = treeItem;
//					}
				}
			}
		});

		_signViewer.addTreeListener(new ITreeViewerListener() {

			@Override
			public void treeCollapsed(final TreeExpansionEvent event) {

//				if (event.getElement() instanceof TVIPrefSignCategory) {
////					_expandedItem = null;
//				}
			}

			@Override
			public void treeExpanded(final TreeExpansionEvent event) {

//				final Object element = event.getElement();
//
//				if (element instanceof TVIPrefSignCategory) {
//
//					final TVIPrefSignCategory treeItem = (TVIPrefSignCategory) element;
//
//					if (_expandedItem != null) {
//						_signViewer.collapseToLevel(_expandedItem, 1);
//					}
//
//					Display.getCurrent().asyncExec(new Runnable() {
//						public void run() {
//							_signViewer.expandToLevel(treeItem, 1);
//							_expandedItem = treeItem;
//						}
//					});
//				}
			}
		});

		_signViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				enableControls();
			}
		});

//		_signViewer.addDragSupport(
//				DND.DROP_MOVE,
//				new Transfer[] { LocalSelectionTransfer.getTransfer() },
//				new DragSourceListener() {
//
//					public void dragFinished(final DragSourceEvent event) {
//
//						final LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
//
//						if (event.doit == false) {
//							return;
//						}
//
//						transfer.setSelection(null);
//						transfer.setSelectionSetTime(0);
//					}
//
//					public void dragSetData(final DragSourceEvent event) {
//						// data are set in LocalSelectionTransfer
//					}
//
//					public void dragStart(final DragSourceEvent event) {
//
//						final LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
//						final ISelection selection = _signViewer.getSelection();
//
////						System.out.println("dragStart");
//						transfer.setSelection(selection);
//						transfer.setSelectionSetTime(_dragStartTime = event.time & 0xFFFFFFFFL);
//
//						event.doit = !selection.isEmpty();
//					}
//				});
//
//		_signViewer.addDropSupport(
//				DND.DROP_MOVE,
//				new Transfer[] { LocalSelectionTransfer.getTransfer() },
//				new SignDropAdapter(this, _signViewer));

		createContextMenu();

//		// set color for all controls
//		final ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
//		final Color fgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_FOREGROUND);
//		final Color bgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_BACKGROUND);
//
//		net.tourbook.common.UI.updateChildColors(tree, fgColor, bgColor);
	}

	private void defineAllColumns() {

		defineColumn_Name();
		defineColumn_Image();
		defineColumn_Dimension();
		defineColumn_FilePathName();

//		defineColumn_Spacer();
	}

	/**
	 * Column: Dimension
	 */
	private void defineColumn_Dimension() {

		final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, "Dimension", SWT.TRAIL); //$NON-NLS-1$

		colDef.setColumnLabel(Messages.SignImage_Viewer_Column_Dimension_Label);
		colDef.setColumnHeaderText(Messages.SignImage_Viewer_Column_Dimension_Label);
		colDef.setColumnHeaderToolTipText(Messages.SignImage_Viewer_Column_Dimension_Tooltip);
		colDef.setIsDefaultColumn();
		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(12));

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				if (element instanceof TVIPrefSign) {

					final TourSign tourSign = ((TVIPrefSign) element).getTourSign();

					final Photo signImagePhoto = tourSign.getSignImagePhoto();

					String dimensionText = signImagePhoto.getDimensionText();

					if (dimensionText.length() == 0) {

						/*
						 * Dimension is not yet set, this case should not happen again, it appeared
						 * during development but should be fixed now.
						 */

						dimensionText = UI.SYMBOL_QUESTION_MARK;
					}

					cell.setText(dimensionText);

				} else {

					cell.setText(UI.EMPTY_STRING);
				}

			}
		});

	}

	/**
	 * Column: File path name
	 */
	private void defineColumn_FilePathName() {

		final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, "FilePathName", SWT.LEAD); //$NON-NLS-1$

		colDef.setColumnLabel(Messages.SignImage_Viewer_Column_FilePathName_Label);
		colDef.setColumnHeaderText(Messages.SignImage_Viewer_Column_FilePathName_Label);
		colDef.setColumnHeaderToolTipText(Messages.SignImage_Viewer_Column_FilePathName_Tooltip);
		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(100));

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				if (element instanceof TVIPrefSign) {

					final TourSign tourSign = ((TVIPrefSign) element).getTourSign();

					cell.setText(tourSign.getImageFilePathName());

				} else {

					cell.setText(UI.EMPTY_STRING);
				}

			}
		});

	}

	/**
	 * Column: Sign image
	 */
	private void defineColumn_Image() {

		final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, "Image", SWT.CENTER); //$NON-NLS-1$
		_colDefImage = colDef;

		colDef.setColumnLabel(Messages.SignImage_Viewer_Column_Image_Label);
		colDef.setColumnHeaderText(Messages.SignImage_Viewer_Column_Image_Header);
		colDef.setColumnHeaderToolTipText(Messages.SignImage_Viewer_Column_Image_Tooltip);

		colDef.setDefaultColumnWidth(DEFAULT_IMAGE_WIDTH);
		colDef.setIsDefaultColumn();
		colDef.setIsColumnMoveable(false);
		colDef.setCanModifyVisibility(false);

		colDef.setLabelProvider(new CellLabelProvider() {
			/*
			 * !!! set dummy label provider, otherwise an error occures !!!
			 */
			@Override
			public void update(final ViewerCell cell) {}
		});

		colDef.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				onResizeImageColumn();
			}
		});
	}

	/**
	 * Column: Sign name
	 */
	private void defineColumn_Name() {

		final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, "Name", SWT.LEAD); //$NON-NLS-1$
		colDef.setColumnLabel(Messages.SignImage_Viewer_Column_Name_Label);
		colDef.setColumnHeaderText(Messages.SignImage_Viewer_Column_Name_Label);

		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(30));
		colDef.setIsDefaultColumn();
		colDef.setIsColumnMoveable(false);
		colDef.setCanModifyVisibility(false);

		colDef.setLabelProvider(new StyledCellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final StyledString styledString = new StyledString();

				final Object element = cell.getElement();
				if (element instanceof TVIPrefSign) {

					final TourSign tourSign = ((TVIPrefSign) element).getTourSign();

					styledString.append(tourSign.getSignName(), null);

//				} else if (element instanceof TVIPrefSignCategory) {
//
//					final TVIPrefSignCategory tourSignCategoryItem = (TVIPrefSignCategory) element;
//					final TourSignCategory tourSignCategory = tourSignCategoryItem.getTourSignCategory();
//
//					styledString.append(tourSignCategory.getCategoryName(), null);
//
//					// get number of categories
//					final int categoryCounter = tourSignCategory.getCategoryCounter();
//					final int signCounter = tourSignCategory.getSignCounter();
//					if (categoryCounter == -1 && signCounter == -1) {
//
////						styledString.append("  ...", StyledString.COUNTER_STYLER);
//
//					} else {
//
//						String categoryString = UI.EMPTY_STRING;
//						if (categoryCounter > 0) {
//							categoryString = "/" + categoryCounter; //$NON-NLS-1$
//						}
//						styledString.append("   " + signCounter + categoryString, StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
//					}
//
				} else {
					styledString.append(element.toString());
				}

				cell.setText(styledString.getString());
				cell.setStyleRanges(styledString.getStyleRanges());
			}
		});
	}

	/**
	 * Column: Spacer
	 * <p>
	 * This column is used for Linux that the last column is NOT the image column, otherwise the
	 * image column width has a strange behaviour.
	 */
	private void defineColumn_Spacer() {

		final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, "Spacer", SWT.LEAD); //$NON-NLS-1$

//		colDef.setColumnLabel(Messages.profileViewer_column_label_name);
//		colDef.setColumnHeader(Messages.profileViewer_column_label_name_header);
//		colDef.setColumnToolTipText(Messages.profileViewer_column_label_name_tooltip);

		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(2));
		colDef.setIsDefaultColumn();
		colDef.setIsColumnMoveable(false);
		colDef.setCanModifyVisibility(false);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				cell.setText(UI.EMPTY_STRING);
			}
		});
	}

	@Override
	public void dispose() {

		getSite().getPage().removePostSelectionListener(_postSelectionListener);
		getViewSite().getPage().removePartListener(_partListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		super.dispose();
	}

	private void enableControls() {

		final IStructuredSelection selectedSigns = (IStructuredSelection) _signViewer.getSelection();
		final Object selection = selectedSigns.getFirstElement();

		boolean isTourSign = false;
		final boolean isSignCategory = false;
		final boolean isSelection = selection != null;

		if (selection instanceof TVIPrefSign) {
			isTourSign = true;
//		} else if (selection instanceof TVIPrefSignCategory) {
//			isSignCategory = true;
		}

//		_btnNewSign.setEnabled(isSelection == false || isSignCategory == true && isTourSign == false);
//		_btnNewSignCategory.setEnabled(isSelection == false || isSignCategory == true && isTourSign == false);
//		_btnRename.setEnabled(selectedSigns.size() == 1);
	}

	private void fillContextMenu(final IMenuManager menuMgr) {

		menuMgr.add(_actionCreateSign);
	}

	private void fillToolbar() {

		final IActionBars actionBars = getViewSite().getActionBars();

		/*
		 * Fill view menu
		 */
		final IMenuManager menuMgr = actionBars.getMenuManager();

		menuMgr.add(new Separator());
		menuMgr.add(_actionModifyColumns);

		/*
		 * Fill toolbar
		 */
		final IToolBarManager tbm = actionBars.getToolBarManager();

		tbm.add(new ActionExpandSelection(this, 2));
		tbm.add(new ActionCollapseAll(this));

	}

	@Override
	public ColumnManager getColumnManager() {
		return _columnManager;
	}

	private int getImageColumnWidth() {

		int width;

		if (_tcSignImage == null) {
			width = DEFAULT_IMAGE_WIDTH;
		} else {
			width = _tcSignImage.getWidth();
		}

		return width;
	}

	public Object getMarkerViewer() {
		return _signViewer;
	}

	private int getRowHeight() {

		return Math.min(MAX_ROW_HEIGHT, Math.max(DEFAULT_ROW_HEIGHT, _imageColumnWidth));
	}

	/**
	 * @return Returns the selected sign category or <code>null</code> when the root is selected.
	 */
	private TourSignCategory getSelectedSignCategory() {

		final StructuredSelection selection = (StructuredSelection) _signViewer.getSelection();

		final Object firstElement = selection.getFirstElement();
		if (firstElement instanceof TVIPrefSign) {

			final TVIPrefSign tviTourSign = (TVIPrefSign) firstElement;
			final TreeViewerItem parentItem = tviTourSign.getParentItem();

			System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") //$NON-NLS-1$ //$NON-NLS-2$
					+ ("\tparent: " + parentItem)); //$NON-NLS-1$
			// TODO remove SYSTEM.OUT.PRINTLN

		}

		return null;
	}

	@Override
	public ColumnViewer getViewer() {
		return _signViewer;
	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);

		DEFAULT_IMAGE_WIDTH = TourMarker.getSignImageMaxSize(_pc);
		MAX_ROW_HEIGHT = TourMarker.getSignImageMaxSize(_pc);
	}

	private void onActionCreateSignCategory() {

	}

	private void onActionImportSignImage() {

		final FileDialog fileDialog = new FileDialog(_viewerContainer.getShell(), SWT.OPEN | SWT.MULTI);
		fileDialog.setFilterPath(_state.get(STATE_LAST_IMPORT_FILE_PATH));

		// open file dialog
		final String firstFilePathName = fileDialog.open();

		// check if user canceled the dialog
		if (firstFilePathName == null) {
			return;
		}

		/*
		 * Get images from a file dialog
		 */
		final IPath filePath = new Path(firstFilePathName).removeLastSegments(1);

		// keep last selected path
		_state.put(STATE_LAST_IMPORT_FILE_PATH, filePath.makeAbsolute().toString());

		// create path for each file
		final ArrayList<IPath> selectedFilePaths = new ArrayList<IPath>();
		final String[] selectedFileNames = fileDialog.getFileNames();

		for (final String fileName : selectedFileNames) {

			// replace filename, keep the directory path
			final IPath filePathWithName = filePath.append(fileName);
			final IPath absolutePath = filePathWithName.makeAbsolute();
			final String filePathName = absolutePath.toString();

			selectedFilePaths.add(new Path(filePathName));
		}

		/*
		 * Get selected sign category
		 */

		final TourSignCategory signCategory = getSelectedSignCategory();

		SignManager.createSignImages(selectedFilePaths, signCategory);
	}

	private void onPaintViewer(final Event event) {

		final TreeItem item = (TreeItem) event.item;
		final Object itemData = item.getData();
		if (itemData instanceof TVIPrefSign) {

			final TVIPrefSign prefSign = (TVIPrefSign) itemData;

			final Photo signImagePhoto = prefSign.getSignImagePhoto();

			boolean isUpdateDimension = false;
			if (signImagePhoto.getPhotoImageWidth() == Integer.MIN_VALUE) {
				isUpdateDimension = true;
			}

			final Image signImage = SignManager.getSignImage(signImagePhoto, new LoadImageCallback(prefSign));

			if (signImage != null) {

				final int photoPosX = event.x;
				final int photoPosY = event.y;

				switch (event.type) {
				case SWT.MeasureItem:

					// this is replaced with Hack.setTreeItemHeight() for win

//					event.width += imageRect.width;
//					event.height = Math.max(event.height, imageRect.height + 2);

					if (UI.IS_WIN == false) {
						event.height = getRowHeight();
					}

					break;

				case SWT.PaintItem:

					final GC gc = event.gc;
					final Photo photo = signImagePhoto;

					final int imageCanvasWidth = Math.max(DEFAULT_ROW_HEIGHT, _imageColumnWidth);
					final int imageCanvasHeight = event.height;

					PhotoUI.paintPhotoImage(
							gc,
							photo,
							signImage,
							photoPosX,
							photoPosY,
							imageCanvasWidth,
							imageCanvasHeight,
							SWT.CENTER,
							null);

					break;
				}

				if (isUpdateDimension) {

					/*
					 * This is a hack because png images do not contain EXIF data and the image
					 * dimension can be invalid in the photo. Force to update dimension AFTER image
					 * is loaded and the dimension is set.
					 */
					_viewerContainer.getDisplay().asyncExec(new Runnable() {
						public void run() {
							_signViewer.update(prefSign, null);
						}
					});
				}
			}
		}
	}

	private void onResizeImageColumn() {

		final int imageColumnWidth = getImageColumnWidth();

		// check if the width has changed
		if (imageColumnWidth == _imageColumnWidth) {
			return;
		}

		_imageColumnWidth = imageColumnWidth;

		// update images
		if (UI.IS_WIN) {
			Hack.setTreeItemHeight(_signViewer.getTree(), getRowHeight());
		}
	}

	private void onSelectionChanged(final ISelection selection) {

	}

	@Override
	public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

		_viewerContainer.setRedraw(false);
		{
			final Object[] expandedElements = _signViewer.getExpandedElements();
			final ISelection selection = _signViewer.getSelection();

			_signViewer.getTree().dispose();

			createUI_22_SignViewer_Tree(_viewerContainer);

			_viewerContainer.layout();

			// update the viewer
			reloadViewer();

			_signViewer.setExpandedElements(expandedElements);
			_signViewer.setSelection(selection);

			// force a resize
			_imageColumnWidth = -1;
			onResizeImageColumn();
		}
		_viewerContainer.setRedraw(true);

		return _signViewer;
	}

	@Override
	public void reloadViewer() {

		_signViewer.setInput(new Object[0]);
	}

	/**
	 * Restore viewer state after the viewer is loaded.
	 */
	private void restoreState_Viewer() {

		/*
		 * Expanded sign categories
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
				_signViewer.setExpandedTreePaths(viewerTreePaths.toArray(new TreePath[viewerTreePaths.size()]));
			}
		}

		// set selection to first item, this also makes the focus visible
		final ArrayList<TreeViewerItem> treeItems = _rootItem.getChildren();
		if (treeItems.size() > 0) {
			_signViewer.setSelection(new StructuredSelection(treeItems.get(0)));
		}

		setFocus();
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

//				if (treeItem instanceof TVIPrefSignCategory) {
//
//					final TVIPrefSignCategory viewerCat = (TVIPrefSignCategory) treeItem;
//					final long viewerCatId = viewerCat.getTourSignCategory().getCategoryId();
//
//					if (viewerCatId == stateData) {
//
//						pathSegments.add(treeItem);
//
//						return viewerCat.getFetchedChildren();
//					}
//				}
			}

		} else if (stateSegment.__itemType == STATE_ITEM_TYPE_SIGN) {

			for (final TreeViewerItem treeItem : treeItems) {

				if (treeItem instanceof TVIPrefSign) {

					final TVIPrefSign viewerSign = (TVIPrefSign) treeItem;
					final long viewerSignId = viewerSign.getTourSign().getSignId();

					if (viewerSignId == stateData) {

						pathSegments.add(treeItem);

						return viewerSign.getFetchedChildren();
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

				if (itemType == STATE_ITEM_TYPE_CATEGORY || itemType == STATE_ITEM_TYPE_SIGN) {

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

		saveState_Viewer();
	}

	private void saveState_Viewer() {

		// viewer state
		_columnManager.saveState(_state);

		saveState_Viewer_ExpandedItems();
	}

	/**
	 * Save state for expanded tree items.
	 */
	private void saveState_Viewer_ExpandedItems() {

		final Object[] visibleExpanded = _signViewer.getVisibleExpandedElements();

		if (visibleExpanded.length == 0) {
			Util.setState(_state, STATE_EXPANDED_ITEMS, new long[0]);
			return;
		}

		final TLongArrayList expandedItems = new TLongArrayList();

		final TreePath[] expandedOpenedTreePaths = UI.getExpandedOpenedItems(
				visibleExpanded,
				_signViewer.getExpandedTreePaths());

		for (final TreePath expandedPath : expandedOpenedTreePaths) {

			// start a new path, allways set it twice to have a even structure
			expandedItems.add(STATE_ITEM_TYPE_SEPARATOR);
			expandedItems.add(STATE_ITEM_TYPE_SEPARATOR);

			for (int segmentIndex = 0; segmentIndex < expandedPath.getSegmentCount(); segmentIndex++) {

				final Object segment = expandedPath.getSegment(segmentIndex);

//				if (segment instanceof TVIPrefSignCategory) {
//
//					expandedItems.add(STATE_ITEM_TYPE_CATEGORY);
//					expandedItems.add(((TVIPrefSignCategory) segment).getTourSignCategory().getCategoryId());
//
//				} else if (segment instanceof TVIPrefSign) {
//
//					expandedItems.add(STATE_ITEM_TYPE_SIGN);
//					expandedItems.add(((TVIPrefSign) segment).getTourSign().getSignId());
//				}
			}
		}

		Util.setState(_state, STATE_EXPANDED_ITEMS, expandedItems.toArray());
	}

	@Override
	public void setFocus() {
		_signViewer.getTree().setFocus();
	}
}
