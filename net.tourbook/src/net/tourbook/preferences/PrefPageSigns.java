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
package net.tourbook.preferences;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Set;

import javax.persistence.EntityManager;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.Hack;
import net.tourbook.common.UI;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.SQLUtils;
import net.tourbook.common.util.TreeColumnDefinition;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourSign;
import net.tourbook.data.TourSignCategory;
import net.tourbook.database.TourDatabase;
import net.tourbook.photo.ILoadCallBack;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoUI;
import net.tourbook.sign.SignManager;
import net.tourbook.sign.TVIPrefSign;
import net.tourbook.sign.TVIPrefSignCategory;
import net.tourbook.sign.TVIPrefSignRoot;
import net.tourbook.ui.action.ActionCollapseAll;
import net.tourbook.ui.action.ActionExpandSelection;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
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
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class PrefPageSigns extends PreferencePage implements IWorkbenchPreferencePage, ITourViewer {

	public static final String		ID					= "net.tourbook.preferences.PrefPageSigns"; //$NON-NLS-1$

	private static final String		SORT_PROPERTY		= "sort";									//$NON-NLS-1$

	private final IPreferenceStore	_prefStore			= TourbookPlugin.getPrefStore();
	private final IDialogSettings	_state				= TourbookPlugin.getState("PrefPageSigns"); //$NON-NLS-1$

	private TVIPrefSignRoot			_rootItem;

	private ColumnManager			_columnManager;
	private TreeColumnDefinition	_colDefImage;
	private TreeViewerItem			_expandedItem;

	private int						DEFAULT_IMAGE_WIDTH;
	private int						DEFAULT_ROW_HEIGHT;
	private int						MAX_ROW_HEIGHT;
	private int						_imageColumnWidth	= -1;

	/**
	 * Contains the tree column widget for the sign image.
	 */
	private TreeColumn				_tcSignImage;
	private int						_signImageColumn;

	private boolean					_isModified			= false;
	private int						_uiCounter;

	private long					_dragStartTime;

	/*
	 * UI controls
	 */
	private ToolBar					_toolBar;
	private TreeViewer				_signViewer;
	private Composite				_viewerContainer;

	private Button					_btnDelete;
	private Button					_btnNewSign;
	private Button					_btnNewSignCategory;
	private Button					_btnRename;
	private Button					_btnReset;

	/*
	 * None UI
	 */
	private PixelConverter			_pc;

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

			} else if (obj1 instanceof TVIPrefSign && obj2 instanceof TVIPrefSignCategory) {

				// sort category before sign
				return 1;

			} else if (obj2 instanceof TVIPrefSign && obj1 instanceof TVIPrefSignCategory) {

				// sort category before sign
				return -1;

			} else if (obj1 instanceof TVIPrefSignCategory && obj2 instanceof TVIPrefSignCategory) {

				// sort categories by name
				final TourSignCategory tourSignCat1 = ((TVIPrefSignCategory) (obj1)).getTourSignCategory();
				final TourSignCategory tourSignCat2 = ((TVIPrefSignCategory) (obj2)).getTourSignCategory();

				return tourSignCat1.getCategoryName().compareTo(tourSignCat2.getCategoryName());
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

	public PrefPageSigns() {}

	public PrefPageSigns(final String title) {
		super(title);
	}

	public PrefPageSigns(final String title, final ImageDescriptor image) {
		super(title, image);
	}

	@Override
	protected Control createContents(final Composite parent) {

// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//
// THIS IS ONLY FOR TESTING
//
// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//		PhotoImageCache.disposeAll();

		initUI(parent);

		final Composite ui = createUI(parent);

		fillToolbar();

		// set initial row height
		onResizeImageColumn();

		// set viewer root item
		_rootItem = new TVIPrefSignRoot(_signViewer);

		updateSignViewer();
		enableButtons();

		return ui;
	}

	/**
	 * create the views context menu
	 */
	private void createContextMenu() {

		final MenuManager menuMgr = new MenuManager();

		menuMgr.setRemoveAllWhenShown(true);

		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(final IMenuManager menuMgr2) {
//				fillContextMenu(menuMgr2);
			}
		});

		final Tree tree = _signViewer.getTree();
		final Menu treeContextMenu = menuMgr.createContextMenu(tree);

		tree.setMenu(treeContextMenu);

		_columnManager.createHeaderContextMenu(tree, treeContextMenu);
	}

	private Composite createUI(final Composite parent) {

		// container
		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.margins(0, 0)
//				.spacing(SWT.DEFAULT, 0)
				.numColumns(2)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			createUI_10_Title(container);

			createUI_20_SignViewer_Container(container);
			createUI_30_Buttons(container);

			createUI_40_Bottom(container);
		}

		// spacer
		new Label(parent, SWT.NONE);

		return container;
	}

	private void createUI_10_Title(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			// label: title
			final Label label = new Label(container, SWT.WRAP);
			label.setText(Messages.pref_tourtag_viewer_title);
			GridDataFactory.swtDefaults().grab(true, false).applyTo(label);

			// toolbar
			_toolBar = new ToolBar(container, SWT.FLAT);
		}

		// spacer
		new Label(parent, SWT.NONE);
	}

	private void createUI_20_SignViewer_Container(final Composite parent) {

		// define all columns for the viewer
		_columnManager = new ColumnManager(this, _state);
		defineAllColumns();

		_viewerContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_viewerContainer);
		GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
		{
			createUI_22_SignViewer_Table(_viewerContainer);
		}
	}

	private void createUI_22_SignViewer_Table(final Composite parent) {

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
		tree.setLinesVisible(getPreferenceStore().getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

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

					onRenameTourSign();

				} else if (selection instanceof TVIPrefSignCategory) {

					// expand/collapse current item
					final TVIPrefSignCategory treeItem = (TVIPrefSignCategory) selection;

					if (_signViewer.getExpandedState(treeItem)) {

						_signViewer.collapseToLevel(treeItem, 1);

					} else {

//						if (_expandedItem != null) {
//							_signViewer.collapseToLevel(_expandedItem, 1);
//						}

						_signViewer.expandToLevel(treeItem, 1);
						_expandedItem = treeItem;
					}
				}
			}
		});

		_signViewer.addTreeListener(new ITreeViewerListener() {

			@Override
			public void treeCollapsed(final TreeExpansionEvent event) {

				if (event.getElement() instanceof TVIPrefSignCategory) {
					_expandedItem = null;
				}
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
				enableButtons();
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

	private void createUI_30_Buttons(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.indent(5, 0)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().applyTo(container);
		{
			// button: new sign
			_btnNewSign = new Button(container, SWT.NONE);
			_btnNewSign.setText(Messages.pref_tourtag_btn_new_tag);
			setButtonLayoutData(_btnNewSign);
			_btnNewSign.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
//					onNewSign();
				}
			});

			// button: new sign category
			_btnNewSignCategory = new Button(container, SWT.NONE);
			_btnNewSignCategory.setText(Messages.pref_tourtag_btn_new_tag_category);
			setButtonLayoutData(_btnNewSignCategory);
			_btnNewSignCategory.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
//					onNewCategory();
				}
			});

			// button: rename
			_btnRename = new Button(container, SWT.NONE);
			_btnRename.setText(Messages.pref_tourtag_btn_rename);
			setButtonLayoutData(_btnRename);
			_btnRename.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onRenameTourSign();
				}
			});

			// button: delete
			_btnDelete = new Button(container, SWT.NONE);
			_btnDelete.setText(Messages.App_Action_Delete);
			setButtonLayoutData(_btnDelete);
			_btnDelete.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onDeleteTourSign();
				}
			});

			// button: reset
			_btnReset = new Button(container, SWT.NONE);
			_btnReset.setText(Messages.pref_tourtag_btn_reset);
			setButtonLayoutData(_btnReset);
			final GridData gd = (GridData) _btnReset.getLayoutData();
			gd.verticalIndent = 50;

			_btnReset.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
//					onReset();
				}
			});
		}
	}

	private void createUI_40_Bottom(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.span(2, 1)
//				.indent(0, _pc.convertVerticalDLUsToPixels(4))
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{
			final Label label = new Label(container, SWT.WRAP);
			label.setText(Messages.pref_tourtag_hint);
			GridDataFactory.swtDefaults().grab(true, false).applyTo(label);

			final Link link = new Link(container, SWT.WRAP);
			GridDataFactory.swtDefaults().grab(true, false).applyTo(link);
			link.setText(Messages.Pref_TourTag_Link_AppearanceOptions);
			link.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					PreferencesUtil.createPreferenceDialogOn(getShell(), PrefPageAppearance.ID, null, null);
				}
			});
		}
	}

	private void defineAllColumns() {

		defineColumn_Name();
		defineColumn_Image();
		defineColumn_Dimension();
		defineColumn_FilePathName();

		defineColumn_Spacer();
	}

	/**
	 * Column: Dimension
	 */
	private void defineColumn_Dimension() {

		final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, "Dimension", SWT.TRAIL); //$NON-NLS-1$

		colDef.setColumnLabel(Messages.SignImage_Viewer_Column_Dimension_Label);
		colDef.setColumnHeader(Messages.SignImage_Viewer_Column_Dimension_Label);
		colDef.setColumnToolTipText(Messages.SignImage_Viewer_Column_Dimension_Tooltip);
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

						// dimension is not yet set

						dimensionText = "?";

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
		colDef.setColumnHeader(Messages.SignImage_Viewer_Column_FilePathName_Label);
		colDef.setColumnToolTipText(Messages.SignImage_Viewer_Column_FilePathName_Tooltip);
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

		final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, "image", SWT.LEAD); //$NON-NLS-1$
		_colDefImage = colDef;

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

		final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, "name", SWT.LEAD); //$NON-NLS-1$

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

				} else if (element instanceof TVIPrefSignCategory) {

					final TVIPrefSignCategory tourSignCategoryItem = (TVIPrefSignCategory) element;
					final TourSignCategory tourSignCategory = tourSignCategoryItem.getTourSignCategory();

					styledString.append(tourSignCategory.getCategoryName(), null);

					// get number of categories
					final int categoryCounter = tourSignCategory.getCategoryCounter();
					final int signCounter = tourSignCategory.getSignCounter();
					if (categoryCounter == -1 && signCounter == -1) {

//						styledString.append("  ...", StyledString.COUNTER_STYLER);

					} else {

						String categoryString = UI.EMPTY_STRING;
						if (categoryCounter > 0) {
							categoryString = "/" + categoryCounter; //$NON-NLS-1$
						}
						styledString.append("   " + signCounter + categoryString, StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
					}

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

		final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, "spacer", SWT.LEAD); //$NON-NLS-1$

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

		super.dispose();
	}

	private void enableButtons() {

		final IStructuredSelection selectedSigns = (IStructuredSelection) _signViewer.getSelection();
		final Object selection = selectedSigns.getFirstElement();

		boolean isTourSign = false;
		boolean isSignCategory = false;
		final boolean isSelection = selection != null;

		if (selection instanceof TVIPrefSign) {
			isTourSign = true;
		} else if (selection instanceof TVIPrefSignCategory) {
			isSignCategory = true;
		}

		_btnNewSign.setEnabled(isSelection == false || isSignCategory == true && isTourSign == false);
		_btnNewSignCategory.setEnabled(isSelection == false || isSignCategory == true && isTourSign == false);
		_btnRename.setEnabled(selectedSigns.size() == 1);
	}

	/**
	 * set the toolbar action after the {@link #_signViewer} is created
	 */
	private void fillToolbar() {

		final ToolBarManager tbm = new ToolBarManager(_toolBar);

		tbm.add(new ActionExpandSelection(this, TreeViewer.ALL_LEVELS));
		tbm.add(new ActionCollapseAll(this));

		tbm.update(true);
	}

	private void fireModifyEvent() {

//		if (_isModified) {
//
//			_isModified = false;
//
//			// remove old signs from cached tours
//			TourDatabase.clearTourSigns();
//
//			SignMenuManager.updateRecentSignNames();
//
//			TourManager.getInstance().clearTourDataCache();
//
//			// fire modify event
//			TourManager.fireEvent(TourEventId.TAG_STRUCTURE_CHANGED);
//		}
	}

	@Override
	public ColumnManager getColumnManager() {
		return _columnManager;
	}

	public long getDragStartTime() {
		return _dragStartTime;
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

	public TVIPrefSignRoot getRootItem() {
		return _rootItem;
	}

	private int getRowHeight() {

		return Math.min(MAX_ROW_HEIGHT, Math.max(DEFAULT_ROW_HEIGHT, _imageColumnWidth));
	}

	@Override
	public ColumnViewer getViewer() {
		return _signViewer;
	}

	@Override
	public void init(final IWorkbench workbench) {
		setPreferenceStore(TourbookPlugin.getDefault().getPreferenceStore());
	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);

//		DEFAULT_IMAGE_WIDTH = _pc.convertWidthInCharsToPixels(6);
//		MAX_ROW_HEIGHT = _pc.convertVerticalDLUsToPixels(50);
		DEFAULT_IMAGE_WIDTH = TourMarker.getSignImageMaxSize(_pc);
		MAX_ROW_HEIGHT = TourMarker.getSignImageMaxSize(_pc);
	}

	@Override
	public boolean isValid() {

//		saveFilterList();

		return true;
	}

	@Override
	public boolean okToLeave() {

		saveViewerState();

		return super.okToLeave();
	}

	private void onDeleteTourSign() {

		/*
		 * Get a list with all tour signs which should be removed
		 */
		final ArrayList<TVIPrefSign> removedSigns = new ArrayList<TVIPrefSign>();

		final StructuredSelection selection = (StructuredSelection) _signViewer.getSelection();
		for (final Object selectedItem : selection.toArray()) {

			if (selectedItem instanceof TVIPrefSign) {
				removedSigns.add((TVIPrefSign) selectedItem);
			}
		}

		if (removedSigns.size() > 0) {

			int firstItemIndex = -1;
			TreeViewerItem reselectParent = null;

			// update model
			EntityManager em = null;
			try {

				em = TourDatabase.getInstance().getEntityManager();

				for (final TVIPrefSign tviSign : removedSigns) {

					final TourSign tourSign = tviSign.getTourSign();

					// remove sign from parent (category)

					final TreeViewerItem tviParent = tviSign.getParentItem();
					if (tviParent instanceof TVIPrefSignCategory) {

						final TVIPrefSignCategory tviCategory = (TVIPrefSignCategory) tviParent;
						final TourSignCategory signCategory = tviCategory.getTourSignCategory();

						// get the index for the first removed item
						if (firstItemIndex == -1) {
							for (final TreeViewerItem categoryChild : tviCategory.getChildren()) {

								firstItemIndex++;

								if (categoryChild == tviSign) {
									reselectParent = tviParent;
									break;
								}
							}
						}

						/*
						 * Update model
						 */
						tviCategory.removeChild(tviSign);

						final TourSignCategory newlySavedCategory = updateModel_RemoveSign(tourSign, signCategory, em);
						tviCategory.setTourSignCategory(newlySavedCategory);

						SignManager.resetSignEntries(signCategory.getCategoryId());
					}
				}

			} finally {

				if (em != null) {
					em.close();
				}
			}

			/*
			 * select the item which is before the removed items, this is not yet finished because
			 * there are multiple possibilities
			 */
			TreeViewerItem nextSelectedTreeItem = null;

			if (reselectParent != null) {

				final ArrayList<TreeViewerItem> firstSelectedChildren = reselectParent.getChildren();
				final int remainingChildren = firstSelectedChildren.size();

				if (remainingChildren > 0) {

					// there are children still available

					if (firstItemIndex < remainingChildren) {
						nextSelectedTreeItem = firstSelectedChildren.get(firstItemIndex);
					} else {
						nextSelectedTreeItem = firstSelectedChildren.get(remainingChildren - 1);
					}

				} else {

					/*
					 * it's possible that the parent does not have any children, then also this
					 * parent must be removed (to be done later)
					 */
					nextSelectedTreeItem = reselectParent;
				}
			}

			// update UI
			_signViewer.remove(removedSigns.toArray());

			// select next item
			if (nextSelectedTreeItem == null) {

				// select first

				Object firstItem = null;

				final ArrayList<TreeViewerItem> rootChildren = _rootItem.getChildren();
				if (rootChildren.size() > 0) {
					firstItem = rootChildren.get(0);
				}

				if (firstItem != null) {
					_signViewer.setSelection(new StructuredSelection(firstItem), true);
				}

			} else {
				_signViewer.setSelection(new StructuredSelection(nextSelectedTreeItem), true);
			}

			_signViewer.getTree().setFocus();
		}
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

	/**
	 * Rename selected sign/category
	 */
	private void onRenameTourSign() {

		final Object selection = ((StructuredSelection) _signViewer.getSelection()).getFirstElement();

		String name = UI.EMPTY_STRING;
		String dlgTitle = UI.EMPTY_STRING;
		String dlgMessage = UI.EMPTY_STRING;

		if (selection instanceof TVIPrefSign) {

			dlgTitle = Messages.pref_tourtag_dlg_rename_title;
			dlgMessage = Messages.pref_tourtag_dlg_rename_message;
			name = ((TVIPrefSign) selection).getTourSign().getSignName();

		} else if (selection instanceof TVIPrefSignCategory) {

			dlgTitle = Messages.pref_tourtag_dlg_rename_title_category;
			dlgMessage = Messages.pref_tourtag_dlg_rename_message_category;
			name = ((TVIPrefSignCategory) selection).getTourSignCategory().getCategoryName();
		}

		final InputDialog inputDialog = new InputDialog(getShell(), dlgTitle, dlgMessage, name, null);

		if (inputDialog.open() != Window.OK) {

			setFocusToViewer();
			return;
		}

		// save changed name

		name = inputDialog.getValue().trim();

		if (selection instanceof TVIPrefSign) {

			// save sign

			final TVIPrefSign signItem = ((TVIPrefSign) selection);
			final TourSign tourSign = signItem.getTourSign();

			tourSign.setSignName(name);

			// persist sign
			TourDatabase.saveEntity(tourSign, tourSign.getSignId(), TourSign.class);

			_signViewer.update(signItem, new String[] { SORT_PROPERTY });

		} else if (selection instanceof TVIPrefSignCategory) {

			// save category

			final TVIPrefSignCategory tourCategoryItem = ((TVIPrefSignCategory) selection);
			final TourSignCategory tourCategory = tourCategoryItem.getTourSignCategory();

			tourCategory.setName(name);

			// persist category
			TourDatabase.saveEntity(tourCategory, tourCategory.getCategoryId(), TourSignCategory.class);

			_signViewer.update(tourCategoryItem, new String[] { SORT_PROPERTY });

		}

		_isModified = true;

		setFocusToViewer();
	}

	//	private void onNewCategory() {
//
//		final InputDialog inputDialog = new InputDialog(
//				getShell(),
//				Messages.pref_tourtag_dlg_new_tag_category_title,
//				Messages.pref_tourtag_dlg_new_tag_category_message,
//				UI.EMPTY_STRING,
//				null);
//
//		if (inputDialog.open() != Window.OK) {
//			setFocusToViewer();
//			return;
//		}
//
//		// create sign category + tree item
//		final TourSignCategory newCategory = new TourSignCategory(inputDialog.getValue().trim());
//		final TVIPrefSignCategory newCategoryItem = new TVIPrefSignCategory(_signViewer, newCategory);
//
//		final Object parentElement = ((StructuredSelection) _signViewer.getSelection()).getFirstElement();
//		TourSignCategory savedNewCategory = null;
//
//		if (parentElement == null) {
//
//			// a parent is not selected, this will be a root category
//
//			newCategory.setRoot(true);
//
//			/*
//			 * update model
//			 */
//
//			_rootItem.getFetchedChildren().add(newCategoryItem);
//
//			// persist new category
//			savedNewCategory = TourDatabase
//					.saveEntity(newCategory, newCategory.getCategoryId(), TourSignCategory.class);
//			if (savedNewCategory != null) {
//
//				// update item
//				newCategoryItem.setTourSignCategory(savedNewCategory);
//
//				// update viewer
//				_signViewer.add(this, newCategoryItem);
//			}
//
//		} else if (parentElement instanceof TVIPrefSignCategory) {
//
//			// parent is a category
//
//			final TVIPrefSignCategory parentCategoryItem = (TVIPrefSignCategory) parentElement;
//			final TourSignCategory parentCategory = parentCategoryItem.getTourSignCategory();
//
//			/*
//			 * update model
//			 */
//
//			final EntityManager em = TourDatabase.getInstance().getEntityManager();
//
//			// persist new category
//			savedNewCategory = TourDatabase
//					.saveEntity(newCategory, newCategory.getCategoryId(), TourSignCategory.class);
//			if (savedNewCategory != null) {
//
//				// update item
//				newCategoryItem.setTourSignCategory(savedNewCategory);
//
//				/*
//				 * update parent category
//				 */
//				final TourSignCategory parentCategoryEntity = em.find(
//						TourSignCategory.class,
//						parentCategory.getCategoryId());
//
//				// set sign in parent category
//				final Set<TourSignCategory> lazyTourSignCategories = parentCategoryEntity.getSignCategories();
//				lazyTourSignCategories.add(savedNewCategory);
//
//				// update number of categories
//				parentCategoryEntity.setCategoryCounter(lazyTourSignCategories.size());
//
//				/*
//				 * persist parent category
//				 */
//				final TourSignCategory savedParentCategory = TourDatabase.saveEntity(
//						parentCategoryEntity,
//						parentCategoryEntity.getCategoryId(),
//						TourSignCategory.class);
//
//				if (savedParentCategory != null) {
//
//					// update item
//					parentCategoryItem.setTourSignCategory(savedParentCategory);
//
//					/*
//					 * update viewer
//					 */
//					parentCategoryItem.clearChildren();
//
////					fSignViewer.update(parentCategoryItem, null);
//
//					_signViewer.add(parentCategoryItem, newCategoryItem);
//
//					_signViewer.expandToLevel(parentCategoryItem, 1);
//				}
//			}
//
//			em.close();
//
//		}
//
//		if (savedNewCategory != null) {
//
//			// reveal new sign in viewer
//			_signViewer.reveal(newCategoryItem);
//
//			_isModified = true;
//		}
//
//		setFocusToViewer();
//	}
//
//	/**
//	 * <pre>
//	 *
//	 * category	--- category
//	 * category	--- sign
//	 * 			+-- sign
//	 * category	--- category
//	 * 			+-- category --- sign
//	 * 						 +-- sign
//	 * 			+-- sign
//	 * 			+-- sign
//	 * 			+-- sign
//	 * sign
//	 * sign
//	 *
//	 * </pre>
//	 */
//	private void onNewSign() {
//
//		final InputDialog inputDialog = new InputDialog(
//				getShell(),
//				Messages.pref_tourtag_dlg_new_tag_title,
//				Messages.pref_tourtag_dlg_new_tag_message,
//				UI.EMPTY_STRING,
//				null);
//
//		if (inputDialog.open() != Window.OK) {
//			setFocusToViewer();
//			return;
//		}
//
//		TourSign savedSign = null;
//
//		// create new tour sign + item
//		final TourSign tourSign = new TourSign(inputDialog.getValue().trim());
//		final TVIPrefSign signItem = new TVIPrefSign(_signViewer, tourSign);
//
//		final Object parentItem = ((StructuredSelection) _signViewer.getSelection()).getFirstElement();
//		if (parentItem == null) {
//
//			// a parent is not selected, this will be a root sign
//
//			tourSign.setRoot(true);
//
//			/*
//			 * update model
//			 */
//			signItem.setParentItem(_rootItem);
//			_rootItem.getFetchedChildren().add(signItem);
//
//			// persist sign
//			savedSign = TourDatabase.saveEntity(tourSign, TourDatabase.ENTITY_IS_NOT_SAVED, TourSign.class);
//
//			if (savedSign != null) {
//
//				// update item
//				signItem.setTourSign(savedSign);
//
//				/*
//				 * update viewer
//				 */
//				_signViewer.add(this, signItem);
//			}
//
//		} else if (parentItem instanceof TVIPrefSignCategory) {
//
//			// parent is a category
//
//			final TVIPrefSignCategory parentCategoryItem = (TVIPrefSignCategory) parentItem;
//			TourSignCategory parentSignCategory = parentCategoryItem.getTourSignCategory();
//
//			/*
//			 * update model
//			 */
//
//			// set parent into sign
//			signItem.setParentItem(parentCategoryItem);
//
//			/*
//			 * persist sign without new category otherwise an exception "detached entity passed to
//			 * persist: net.tourbook.data.TourSignCategory" is raised
//			 */
//			savedSign = TourDatabase.saveEntity(tourSign, TourDatabase.ENTITY_IS_NOT_SAVED, TourSign.class);
//			if (savedSign != null) {
//
//				// update item
//				signItem.setTourSign(savedSign);
//
//				// update parent category
//				final EntityManager em = TourDatabase.getInstance().getEntityManager();
//				{
//
//					final TourSignCategory parentSignCategoryEntity = em.find(
//							TourSignCategory.class,
//							parentSignCategory.getCategoryId());
//
//					// set new entity
//					parentSignCategory = parentSignCategoryEntity;
//					parentCategoryItem.setTourSignCategory(parentSignCategoryEntity);
//
//					// set sign into parent category
//					final Set<TourSign> lazyTourSigns = parentSignCategoryEntity.getTourSigns();
//					lazyTourSigns.add(tourSign);
//
//					parentSignCategory.setSignCounter(lazyTourSigns.size());
//				}
//				em.close();
//
//				// persist parent category
//				final TourSignCategory savedParent = TourDatabase.saveEntity(
//						parentSignCategory,
//						parentSignCategory.getCategoryId(),
//						TourSignCategory.class);
//
//				if (savedParent != null) {
//
//					// update item
//					parentCategoryItem.setTourSignCategory(savedParent);
//
//					// set category in sign
//					tourSign.getSignCategories().add(parentSignCategory);
//
//					// persist sign with category
//					savedSign = TourDatabase.saveEntity(tourSign, tourSign.getSignId(), TourSign.class);
//
//				}
//
//			}
//
//			if (savedSign != null) {
//
//				// clear tour sign list
//				TourDatabase.clearTourSigns();
//
//				/*
//				 * update viewer
//				 */
//				parentCategoryItem.clearChildren();
//
//				_signViewer.add(parentCategoryItem, signItem);
//				_signViewer.update(parentCategoryItem, null);
//
//				_signViewer.expandToLevel(parentCategoryItem, 1);
//			}
//		}
//
//		if (savedSign != null) {
//
//			// show new sign in viewer
//			_signViewer.reveal(signItem);
//
//			_isModified = true;
//		}
//
//		setFocusToViewer();
//	}

	private void onReset() {

		final MessageDialog dialog = new MessageDialog(
				Display.getCurrent().getActiveShell(),
				Messages.pref_tourtag_dlg_reset_title,
				null,
				Messages.pref_tourtag_dlg_reset_message,
				MessageDialog.QUESTION,
				new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL },
				1);

		if (dialog.open() != Window.OK) {
			setFocusToViewer();
			return;
		}

		try {

			System.out.println("RESET TAG STRUCTURE"); //$NON-NLS-1$

			final StringBuilder sb = new StringBuilder();
			final Connection conn = TourDatabase.getInstance().getConnection();

			/*
			 * remove join table sign->category
			 */
			sb.append("DELETE FROM "); //$NON-NLS-1$
			sb.append(TourDatabase.JOINTABLE_TOURTAGCATEGORY_TOURTAG);
			int result = conn.createStatement().executeUpdate(sb.toString());
			System.out.println("Deleted " //$NON-NLS-1$
					+ result
					+ " entries from " //$NON-NLS-1$
					+ TourDatabase.JOINTABLE_TOURTAGCATEGORY_TOURTAG);

			/*
			 * remove jointable category<->category
			 */
			sb.setLength(0);
			sb.append("DELETE FROM "); //$NON-NLS-1$
			sb.append(TourDatabase.JOINTABLE_TOURTAGCATEGORY_TOURTAGCATEGORY);
			result = conn.createStatement().executeUpdate(sb.toString());
			System.out.println("Deleted " //$NON-NLS-1$
					+ result
					+ " entries from " //$NON-NLS-1$
					+ TourDatabase.JOINTABLE_TOURTAGCATEGORY_TOURTAGCATEGORY);

			/*
			 * set signs to root
			 */
			sb.setLength(0);
			sb.append("UPDATE "); //$NON-NLS-1$
			sb.append(TourDatabase.TABLE_TOUR_TAG);
			sb.append(" SET isRoot=1"); //$NON-NLS-1$
			result = conn.createStatement().executeUpdate(sb.toString());
			System.out.println("Set " + result + " tour signs to root"); //$NON-NLS-1$ //$NON-NLS-2$

			/*
			 * set categories to root
			 */
			sb.setLength(0);
			sb.append("UPDATE "); //$NON-NLS-1$
			sb.append(TourDatabase.TABLE_TOUR_TAG_CATEGORY);
			sb.append(" SET isRoot=1"); //$NON-NLS-1$
			result = conn.createStatement().executeUpdate(sb.toString());
			System.out.println("Set " + result + " tour categories to root"); //$NON-NLS-1$ //$NON-NLS-2$

			conn.close();

			// update the sign viewer
			_rootItem = new TVIPrefSignRoot(_signViewer);
			updateSignViewer();

			_isModified = true;

		} catch (final SQLException e) {
			SQLUtils.showSQLException(e);
		}

		setFocusToViewer();
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

	@Override
	public boolean performCancel() {

		saveViewerState();
		fireModifyEvent();

		return true;
	}

	@Override
	public boolean performOk() {

		saveState();

		fireModifyEvent();

		return true;
	}

	@Override
	public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

		_viewerContainer.setRedraw(false);
		{
			final Object[] expandedElements = _signViewer.getExpandedElements();
			final ISelection selection = _signViewer.getSelection();

			_signViewer.getTree().dispose();

			createUI_22_SignViewer_Table(_viewerContainer);

			_viewerContainer.layout();

			// update the viewer
			reloadViewer();

			_signViewer.setExpandedElements(expandedElements);
			_signViewer.setSelection(selection);
		}
		_viewerContainer.setRedraw(true);

		onResizeImageColumn();

		return _signViewer;
	}

	@Override
	public void reloadViewer() {
		_signViewer.setInput(new Object[0]);
	}

	/**
	 * save state of the pref page
	 */
	private void saveState() {

		saveViewerState();
	}

	private void saveViewerState() {

		// viewer state
		_columnManager.saveState(_state);
	}

	private void setFocusToViewer() {

		// set focus back to the tree
		_signViewer.getTree().setFocus();
	}

	public void setIsModified() {
		_isModified = true;
	}

	/**
	 * Removes a {@link TourSign} from a {@link TourSignCategory} and updates the model (database).
	 * 
	 * @param tourSign
	 * @param signCategory
	 * @param em
	 * @return Returns the saved entity
	 */
	private TourSignCategory updateModel_RemoveSign(final TourSign tourSign,
													final TourSignCategory signCategory,
													final EntityManager em) {

		final TourSignCategory lazyCategory = em.find(TourSignCategory.class, signCategory.getCategoryId());

		// remove tag
		final Set<TourSign> lazyTourSigns = lazyCategory.getTourSigns();
		lazyTourSigns.remove(tourSign);

		// update counter
		lazyCategory.setSignCounter(lazyTourSigns.size());
		lazyCategory.setCategoryCounter(lazyCategory.getTourSignCategories().size());

		return TourDatabase.saveEntity(lazyCategory, lazyCategory.getCategoryId(), TourSignCategory.class);
	}

	private void updateSignViewer() {

		// show contents in the viewers
		_signViewer.setInput(this);

		enableButtons();
	}

}
