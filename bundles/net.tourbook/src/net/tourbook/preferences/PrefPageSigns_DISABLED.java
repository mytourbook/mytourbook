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


public class PrefPageSigns_DISABLED /*
									 * extends PreferencePage implements IWorkbenchPreferencePage,
									 * ITourViewer
									 */{

//	public static final String		ID							= "net.tourbook.preferences.PrefPageSigns"; //$NON-NLS-1$
//
//	private static final String		SORT_PROPERTY				= "sort";									//$NON-NLS-1$
//
//	/**
//	 * The expanded sign items have these structure:
//	 * <p>
//	 * 1. Type<br>
//	 * 2. Id<br>
//	 * <br>
//	 * 3. Type<br>
//	 * 4. Id<br>
//	 * ...
//	 */
//	private static final String		STATE_EXPANDED_ITEMS		= "STATE_EXPANDED_ITEMS";					//$NON-NLS-1$
//
//	private static final int		STATE_ITEM_TYPE_SEPARATOR	= -1;
//	private static final int		STATE_ITEM_TYPE_CATEGORY	= 1;
//	private static final int		STATE_ITEM_TYPE_SIGN		= 2;
//
//	private final IPreferenceStore	_prefStore					= TourbookPlugin.getPrefStore();
//	private final IDialogSettings	_state						= TourbookPlugin.getState("PrefPageSigns"); //$NON-NLS-1$
//
//	private TVIPrefSignRoot			_rootItem;
//
//	private ColumnManager			_columnManager;
//	private TreeColumnDefinition	_colDefImage;
//	private TreeViewerItem			_expandedItem;
//
//	private int						DEFAULT_IMAGE_WIDTH;
//	private int						DEFAULT_ROW_HEIGHT;
//	private int						MAX_ROW_HEIGHT;
//	private int						_imageColumnWidth			= -1;
//
//	/**
//	 * Contains the tree column widget for the sign image.
//	 */
//	private TreeColumn				_tcSignImage;
//	private int						_signImageColumn;
//
//	private boolean					_isModified					= false;
//	private int						_uiCounter;
//
//	private long					_dragStartTime;
//
//	/*
//	 * UI controls
//	 */
//	private ToolBar					_toolBar;
//	private TreeViewer				_signViewer;
//	private Composite				_viewerContainer;
//
//	private Button					_btnDelete;
//	private Button					_btnNewSign;
//	private Button					_btnNewSignCategory;
//	private Button					_btnRename;
//	private Button					_btnReset;
//
//	/*
//	 * None UI
//	 */
//	private PixelConverter			_pc;
//
//	private class LoadImageCallback implements ILoadCallBack {
//
//		private TreeViewerItem	__viewerItem;
//
//		public LoadImageCallback(final TreeViewerItem viewerItem) {
//
//			__viewerItem = viewerItem;
//		}
//
//		@Override
//		public void callBackImageIsLoaded(final boolean isImageLoaded) {
//
//			if (isImageLoaded == false) {
//				return;
//			}
//
//			// run in UI thread
//			Display.getDefault().syncExec(new Runnable() {
//
//				@Override
//				public void run() {
//
//					if (_signViewer.getTree().isDisposed()) {
//						return;
//					}
//
//					// update sign image
//
//					// update image size in the viewer
//					_signViewer.update(__viewerItem, null);
//
//					// !!! refresh() and update() do not repaint a loaded image but a redraw() do
//					_signViewer.getTree().redraw();
//				}
//			});
//
//		}
//	}
//
//	/**
//	 * Sort the signs and categories
//	 */
//	private final class SignViewerComparator extends ViewerComparator {
//		@Override
//		public int compare(final Viewer viewer, final Object obj1, final Object obj2) {
//			if (obj1 instanceof TVIPrefSign && obj2 instanceof TVIPrefSign) {
//
//				// sort signs by name
//				final TourSign tourSign1 = ((TVIPrefSign) (obj1)).getTourSign();
//				final TourSign tourSign2 = ((TVIPrefSign) (obj2)).getTourSign();
//
//				return tourSign1.getSignName().compareTo(tourSign2.getSignName());
//
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
//			}
//
//			return 0;
//		}
//
//		@Override
//		public boolean isSorterProperty(final Object element, final String property) {
//			// sort when the name has changed
//			return true;
//		}
//	}
//
//	private final class SignViewerContentProvider implements ITreeContentProvider {
//
//		@Override
//		public void dispose() {}
//
//		@Override
//		public Object[] getChildren(final Object parentElement) {
//			return ((TreeViewerItem) parentElement).getFetchedChildrenAsArray();
//		}
//
//		@Override
//		public Object[] getElements(final Object inputElement) {
//			return _rootItem.getFetchedChildrenAsArray();
//		}
//
//		@Override
//		public Object getParent(final Object element) {
//			return ((TreeViewerItem) element).getParentItem();
//		}
//
////		public TreeViewerItem getRootItem() {
////			return _rootItem;
////		}
//
//		@Override
//		public boolean hasChildren(final Object element) {
//			return ((TreeViewerItem) element).hasChildren();
//		}
//
//		@Override
//		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
//	}
//
//	private class StateSegment {
//
//		private long	__itemType;
//		private long	__itemData;
//
//		public StateSegment(final long itemType, final long itemData) {
//
//			__itemType = itemType;
//			__itemData = itemData;
//		}
//	}
//
//	public PrefPageSigns() {}
//
//	public PrefPageSigns(final String title) {
//		super(title);
//	}
//
//	public PrefPageSigns(final String title, final ImageDescriptor image) {
//		super(title, image);
//	}
//
//	@Override
//	protected Control createContents(final Composite parent) {
//
//// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
////
//// THIS IS ONLY FOR TESTING
////
//// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
////		PhotoImageCache.disposeAll();
//
//		initUI(parent);
//
//		final Composite ui = createUI(parent);
//
//		fillToolbar();
//
//		// set initial row height
//		onResizeImageColumn();
//
//		// set viewer root item
//		_rootItem = new TVIPrefSignRoot(_signViewer);
//
//		// show contents in the viewers
//		_signViewer.setInput(this);
//
//		restoreState_Viewer();
//
//		enableControls();
//
//		return ui;
//	}
//
//	/**
//	 * create the views context menu
//	 */
//	private void createContextMenu() {
//
//		final MenuManager menuMgr = new MenuManager();
//
//		menuMgr.setRemoveAllWhenShown(true);
//
//		menuMgr.addMenuListener(new IMenuListener() {
//			@Override
//			public void menuAboutToShow(final IMenuManager menuMgr2) {
////				fillContextMenu(menuMgr2);
//			}
//		});
//
//		final Tree tree = _signViewer.getTree();
//		final Menu treeContextMenu = menuMgr.createContextMenu(tree);
//
//		tree.setMenu(treeContextMenu);
//
//		_columnManager.createHeaderContextMenu(tree, treeContextMenu);
//	}
//
//	private Composite createUI(final Composite parent) {
//
//		// container
//		final Composite container = new Composite(parent, SWT.NONE);
//		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
//		GridLayoutFactory.fillDefaults()//
//				.margins(0, 0)
////				.spacing(SWT.DEFAULT, 0)
//				.numColumns(2)
//				.applyTo(container);
////		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
//		{
//			createUI_10_Title(container);
//
//			createUI_20_SignViewer_Container(container);
//			createUI_30_Buttons(container);
//
//			createUI_40_Bottom(container);
//		}
//
//		// spacer
//		new Label(parent, SWT.NONE);
//
//		return container;
//	}
//
//	private void createUI_10_Title(final Composite parent) {
//
//		final Composite container = new Composite(parent, SWT.NONE);
//		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
//		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		{
//			// label: title
//			final Label label = new Label(container, SWT.WRAP);
//			label.setText(Messages.pref_tourtag_viewer_title);
//			GridDataFactory.swtDefaults().grab(true, false).applyTo(label);
//
//			// toolbar
//			_toolBar = new ToolBar(container, SWT.FLAT);
//		}
//
//		// spacer
//		new Label(parent, SWT.NONE);
//	}
//
//	private void createUI_20_SignViewer_Container(final Composite parent) {
//
//		// define all columns for the viewer
//		_columnManager = new ColumnManager(this, _state);
//		defineAllColumns();
//
//		_viewerContainer = new Composite(parent, SWT.NONE);
//		GridDataFactory.fillDefaults().grab(true, true).applyTo(_viewerContainer);
//		GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
//		{
//			createUI_22_SignViewer_Table(_viewerContainer);
//		}
//	}
//
//	private void createUI_22_SignViewer_Table(final Composite parent) {
//
//		/*
//		 * Create tree
//		 */
//		final Tree tree = new Tree(parent, //
//				SWT.H_SCROLL //
//						| SWT.V_SCROLL
////						| SWT.BORDER
//						| SWT.MULTI
//						| SWT.FULL_SELECTION);
//
//		GridDataFactory.fillDefaults()//
//				.grab(true, true)
//				.hint(_pc.convertWidthInCharsToPixels(60), _pc.convertHeightInCharsToPixels(30))
//				.applyTo(tree);
//
//		DEFAULT_ROW_HEIGHT = tree.getItemHeight();
//
//		tree.setHeaderVisible(true);
//		tree.setLinesVisible(getPreferenceStore().getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));
//
//		/*
//		 * NOTE: MeasureItem, PaintItem and EraseItem are called repeatedly. Therefore, it is
//		 * critical for performance that these methods be as efficient as possible.
//		 */
//		final Listener paintListener = new Listener() {
//			@Override
//			public void handleEvent(final Event event) {
//
//				if (event.index == _signImageColumn //
//						&& (event.type == SWT.MeasureItem || event.type == SWT.PaintItem)) {
//
//					onPaintViewer(event);
//				}
//			}
//		};
//		tree.addListener(SWT.MeasureItem, paintListener);
//		tree.addListener(SWT.PaintItem, paintListener);
//
//		tree.addKeyListener(new KeyAdapter() {
//			@Override
//			public void keyPressed(final KeyEvent e) {
//
//				if (e.keyCode == SWT.DEL) {
//					onDeleteTourSign();
//				}
//			}
//		});
//
//		/*
//		 * Create viewer
//		 */
//		_signViewer = new TreeViewer(tree);
//
//		_columnManager.createColumns(_signViewer);
//
//		_tcSignImage = _colDefImage.getTreeColumn();
//		_signImageColumn = _colDefImage.getCreateIndex();
//
//		_signViewer.setContentProvider(new SignViewerContentProvider());
//		_signViewer.setComparator(new SignViewerComparator());
//
//		_signViewer.setUseHashlookup(true);
//
//		_signViewer.addDoubleClickListener(new IDoubleClickListener() {
//			@Override
//			public void doubleClick(final DoubleClickEvent event) {
//
//				final Object selection = ((IStructuredSelection) _signViewer.getSelection()).getFirstElement();
//
//				if (selection instanceof TVIPrefSign) {
//
//					// sign is selected
//
//					onRenameTourSign();
//
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
//						_expandedItem = treeItem;
//					}
//				}
//			}
//		});
//
//		_signViewer.addTreeListener(new ITreeViewerListener() {
//
//			@Override
//			public void treeCollapsed(final TreeExpansionEvent event) {
//
//				if (event.getElement() instanceof TVIPrefSignCategory) {
//					_expandedItem = null;
//				}
//			}
//
//			@Override
//			public void treeExpanded(final TreeExpansionEvent event) {
//
////				final Object element = event.getElement();
////
////				if (element instanceof TVIPrefSignCategory) {
////
////					final TVIPrefSignCategory treeItem = (TVIPrefSignCategory) element;
////
////					if (_expandedItem != null) {
////						_signViewer.collapseToLevel(_expandedItem, 1);
////					}
////
////					Display.getCurrent().asyncExec(new Runnable() {
////						public void run() {
////							_signViewer.expandToLevel(treeItem, 1);
////							_expandedItem = treeItem;
////						}
////					});
////				}
//			}
//		});
//
//		_signViewer.addSelectionChangedListener(new ISelectionChangedListener() {
//			@Override
//			public void selectionChanged(final SelectionChangedEvent event) {
//				enableControls();
//			}
//		});
//
////		_signViewer.addDragSupport(
////				DND.DROP_MOVE,
////				new Transfer[] { LocalSelectionTransfer.getTransfer() },
////				new DragSourceListener() {
////
////					public void dragFinished(final DragSourceEvent event) {
////
////						final LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
////
////						if (event.doit == false) {
////							return;
////						}
////
////						transfer.setSelection(null);
////						transfer.setSelectionSetTime(0);
////					}
////
////					public void dragSetData(final DragSourceEvent event) {
////						// data are set in LocalSelectionTransfer
////					}
////
////					public void dragStart(final DragSourceEvent event) {
////
////						final LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
////						final ISelection selection = _signViewer.getSelection();
////
//////						System.out.println("dragStart");
////						transfer.setSelection(selection);
////						transfer.setSelectionSetTime(_dragStartTime = event.time & 0xFFFFFFFFL);
////
////						event.doit = !selection.isEmpty();
////					}
////				});
////
////		_signViewer.addDropSupport(
////				DND.DROP_MOVE,
////				new Transfer[] { LocalSelectionTransfer.getTransfer() },
////				new SignDropAdapter(this, _signViewer));
//
//		createContextMenu();
//
////		// set color for all controls
////		final ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
////		final Color fgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_FOREGROUND);
////		final Color bgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_BACKGROUND);
////
////		net.tourbook.common.UI.updateChildColors(tree, fgColor, bgColor);
//	}
//
//	private void createUI_30_Buttons(final Composite parent) {
//
//		final Composite container = new Composite(parent, SWT.NONE);
//		GridDataFactory.fillDefaults()//
//				.indent(5, 0)
//				.applyTo(container);
//		GridLayoutFactory.fillDefaults().applyTo(container);
//		{
//			// button: new sign
//			_btnNewSign = new Button(container, SWT.NONE);
//			_btnNewSign.setText(Messages.pref_tourtag_btn_new_tag);
//			setButtonLayoutData(_btnNewSign);
//			_btnNewSign.addSelectionListener(new SelectionAdapter() {
//				@Override
//				public void widgetSelected(final SelectionEvent e) {
////					onNewSign();
//				}
//			});
//
//			// button: new sign category
//			_btnNewSignCategory = new Button(container, SWT.NONE);
//			_btnNewSignCategory.setText(Messages.pref_tourtag_btn_new_tag_category);
//			setButtonLayoutData(_btnNewSignCategory);
//			_btnNewSignCategory.addSelectionListener(new SelectionAdapter() {
//				@Override
//				public void widgetSelected(final SelectionEvent e) {
////					onNewCategory();
//				}
//			});
//
//			// button: rename
//			_btnRename = new Button(container, SWT.NONE);
//			_btnRename.setText(Messages.pref_tourtag_btn_rename);
//			setButtonLayoutData(_btnRename);
//			_btnRename.addSelectionListener(new SelectionAdapter() {
//				@Override
//				public void widgetSelected(final SelectionEvent e) {
//					onRenameTourSign();
//				}
//			});
//
//			// button: delete
//			_btnDelete = new Button(container, SWT.NONE);
//			_btnDelete.setText(Messages.App_Action_Delete);
//			setButtonLayoutData(_btnDelete);
//			_btnDelete.addSelectionListener(new SelectionAdapter() {
//				@Override
//				public void widgetSelected(final SelectionEvent e) {
//					onDeleteTourSign();
//				}
//			});
//
//			// button: reset
//			_btnReset = new Button(container, SWT.NONE);
//			_btnReset.setText(Messages.pref_tourtag_btn_reset);
//			setButtonLayoutData(_btnReset);
//			final GridData gd = (GridData) _btnReset.getLayoutData();
//			gd.verticalIndent = 50;
//
//			_btnReset.addSelectionListener(new SelectionAdapter() {
//				@Override
//				public void widgetSelected(final SelectionEvent e) {
////					onReset();
//				}
//			});
//		}
//	}
//
//	private void createUI_40_Bottom(final Composite parent) {
//
//		final Composite container = new Composite(parent, SWT.NONE);
//		GridDataFactory.fillDefaults()//
//				.grab(true, false)
//				.span(2, 1)
////				.indent(0, _pc.convertVerticalDLUsToPixels(4))
//				.applyTo(container);
//		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//		{
//			final Label label = new Label(container, SWT.WRAP);
//			label.setText(Messages.pref_tourtag_hint);
//			GridDataFactory.swtDefaults().grab(true, false).applyTo(label);
//
//			final Link link = new Link(container, SWT.WRAP);
//			GridDataFactory.swtDefaults().grab(true, false).applyTo(link);
//			link.setText(Messages.Pref_TourTag_Link_AppearanceOptions);
//			link.addSelectionListener(new SelectionAdapter() {
//				@Override
//				public void widgetSelected(final SelectionEvent e) {
//					PreferencesUtil.createPreferenceDialogOn(getShell(), PrefPageAppearance.ID, null, null);
//				}
//			});
//		}
//	}
//
//	private void defineAllColumns() {
//
//		defineColumn_Name();
//		defineColumn_Image();
//		defineColumn_Dimension();
//		defineColumn_FilePathName();
//
//		defineColumn_Spacer();
//	}
//
//	/**
//	 * Column: Dimension
//	 */
//	private void defineColumn_Dimension() {
//
//		final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, "Dimension", SWT.TRAIL); //$NON-NLS-1$
//
//		colDef.setColumnLabel(Messages.SignImage_Viewer_Column_Dimension_Label);
//		colDef.setColumnHeaderText(Messages.SignImage_Viewer_Column_Dimension_Label);
//		colDef.setColumnHeaderToolTipText(Messages.SignImage_Viewer_Column_Dimension_Tooltip);
//		colDef.setIsDefaultColumn();
//		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(12));
//
//		colDef.setLabelProvider(new CellLabelProvider() {
//			@Override
//			public void update(final ViewerCell cell) {
//
//				final Object element = cell.getElement();
//				if (element instanceof TVIPrefSign) {
//
//					final TourSign tourSign = ((TVIPrefSign) element).getTourSign();
//
//					final Photo signImagePhoto = tourSign.getSignImagePhoto();
//
//					String dimensionText = signImagePhoto.getDimensionText();
//
//					if (dimensionText.length() == 0) {
//
//						/*
//						 * Dimension is not yet set, this case should not happen again, it appeared
//						 * during development but should be fixed now.
//						 */
//
//						dimensionText = UI.SYMBOL_QUESTION_MARK;
//					}
//
//					cell.setText(dimensionText);
//
//				} else {
//
//					cell.setText(UI.EMPTY_STRING);
//				}
//
//			}
//		});
//
//	}
//
//	/**
//	 * Column: File path name
//	 */
//	private void defineColumn_FilePathName() {
//
//		final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, "FilePathName", SWT.LEAD); //$NON-NLS-1$
//
//		colDef.setColumnLabel(Messages.SignImage_Viewer_Column_FilePathName_Label);
//		colDef.setColumnHeaderText(Messages.SignImage_Viewer_Column_FilePathName_Label);
//		colDef.setColumnHeaderToolTipText(Messages.SignImage_Viewer_Column_FilePathName_Tooltip);
//		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(100));
//
//		colDef.setLabelProvider(new CellLabelProvider() {
//			@Override
//			public void update(final ViewerCell cell) {
//
//				final Object element = cell.getElement();
//				if (element instanceof TVIPrefSign) {
//
//					final TourSign tourSign = ((TVIPrefSign) element).getTourSign();
//
//					cell.setText(tourSign.getImageFilePathName());
//
//				} else {
//
//					cell.setText(UI.EMPTY_STRING);
//				}
//
//			}
//		});
//
//	}
//
//	/**
//	 * Column: Sign image
//	 */
//	private void defineColumn_Image() {
//
//		final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, "Image", SWT.CENTER); //$NON-NLS-1$
//		_colDefImage = colDef;
//
//		colDef.setColumnLabel(Messages.SignImage_Viewer_Column_Image_Label);
//		colDef.setColumnHeaderText(Messages.SignImage_Viewer_Column_Image_Header);
//		colDef.setColumnHeaderToolTipText(Messages.SignImage_Viewer_Column_Image_Tooltip);
//
//		colDef.setDefaultColumnWidth(DEFAULT_IMAGE_WIDTH);
//		colDef.setIsDefaultColumn();
//		colDef.setIsColumnMoveable(false);
//		colDef.setCanModifyVisibility(false);
//
//		colDef.setLabelProvider(new CellLabelProvider() {
//			/*
//			 * !!! set dummy label provider, otherwise an error occures !!!
//			 */
//			@Override
//			public void update(final ViewerCell cell) {}
//		});
//
//		colDef.addControlListener(new ControlAdapter() {
//			@Override
//			public void controlResized(final ControlEvent e) {
//				onResizeImageColumn();
//			}
//		});
//	}
//
//	/**
//	 * Column: Sign name
//	 */
//	private void defineColumn_Name() {
//
//		final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, "Name", SWT.LEAD); //$NON-NLS-1$
//		colDef.setColumnLabel(Messages.SignImage_Viewer_Column_Name_Label);
//		colDef.setColumnHeaderText(Messages.SignImage_Viewer_Column_Name_Label);
//
//		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(30));
//		colDef.setIsDefaultColumn();
//		colDef.setIsColumnMoveable(false);
//		colDef.setCanModifyVisibility(false);
//
//		colDef.setLabelProvider(new StyledCellLabelProvider() {
//			@Override
//			public void update(final ViewerCell cell) {
//
//				final StyledString styledString = new StyledString();
//
//				final Object element = cell.getElement();
//				if (element instanceof TVIPrefSign) {
//
//					final TourSign tourSign = ((TVIPrefSign) element).getTourSign();
//
//					styledString.append(tourSign.getSignName(), null);
//
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
//				} else {
//					styledString.append(element.toString());
//				}
//
//				cell.setText(styledString.getString());
//				cell.setStyleRanges(styledString.getStyleRanges());
//			}
//		});
//	}
//
//	/**
//	 * Column: Spacer
//	 * <p>
//	 * This column is used for Linux that the last column is NOT the image column, otherwise the
//	 * image column width has a strange behaviour.
//	 */
//	private void defineColumn_Spacer() {
//
//		final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, "Spacer", SWT.LEAD); //$NON-NLS-1$
//
////		colDef.setColumnLabel(Messages.profileViewer_column_label_name);
////		colDef.setColumnHeader(Messages.profileViewer_column_label_name_header);
////		colDef.setColumnToolTipText(Messages.profileViewer_column_label_name_tooltip);
//
//		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(2));
//		colDef.setIsDefaultColumn();
//		colDef.setIsColumnMoveable(false);
//		colDef.setCanModifyVisibility(false);
//
//		colDef.setLabelProvider(new CellLabelProvider() {
//			@Override
//			public void update(final ViewerCell cell) {
//				cell.setText(UI.EMPTY_STRING);
//			}
//		});
//	}
//
//	@Override
//	public void dispose() {
//
//		super.dispose();
//	}
//
//	private void enableControls() {
//
//		final IStructuredSelection selectedSigns = (IStructuredSelection) _signViewer.getSelection();
//		final Object selection = selectedSigns.getFirstElement();
//
//		boolean isTourSign = false;
//		boolean isSignCategory = false;
//		final boolean isSelection = selection != null;
//
//		if (selection instanceof TVIPrefSign) {
//			isTourSign = true;
//		} else if (selection instanceof TVIPrefSignCategory) {
//			isSignCategory = true;
//		}
//
//		_btnNewSign.setEnabled(isSelection == false || isSignCategory == true && isTourSign == false);
//		_btnNewSignCategory.setEnabled(isSelection == false || isSignCategory == true && isTourSign == false);
//		_btnRename.setEnabled(selectedSigns.size() == 1);
//	}
//
//	/**
//	 * set the toolbar action after the {@link #_signViewer} is created
//	 */
//	private void fillToolbar() {
//
//		final ToolBarManager tbm = new ToolBarManager(_toolBar);
//
//		tbm.add(new ActionExpandSelection(this, TreeViewer.ALL_LEVELS));
//		tbm.add(new ActionCollapseAll(this));
//
//		tbm.update(true);
//	}
//
//	private void fireModifyEvent() {
//
////		if (_isModified) {
////
////			_isModified = false;
////
////			// remove old signs from cached tours
////			TourDatabase.clearTourSigns();
////
////			SignMenuManager.updateRecentSignNames();
////
////			TourManager.getInstance().clearTourDataCache();
////
////			// fire modify event
////			TourManager.fireEvent(TourEventId.TAG_STRUCTURE_CHANGED);
////		}
//	}
//
//	@Override
//	public ColumnManager getColumnManager() {
//		return _columnManager;
//	}
//
//	public long getDragStartTime() {
//		return _dragStartTime;
//	}
//
//	private int getImageColumnWidth() {
//
//		int width;
//
//		if (_tcSignImage == null) {
//			width = DEFAULT_IMAGE_WIDTH;
//		} else {
//			width = _tcSignImage.getWidth();
//		}
//
//		return width;
//	}
//
//	public TVIPrefSignRoot getRootItem() {
//		return _rootItem;
//	}
//
//	private int getRowHeight() {
//
//		return Math.min(MAX_ROW_HEIGHT, Math.max(DEFAULT_ROW_HEIGHT, _imageColumnWidth));
//	}
//
//	@Override
//	public ColumnViewer getViewer() {
//		return _signViewer;
//	}
//
//	@Override
//	public void init(final IWorkbench workbench) {
//		setPreferenceStore(TourbookPlugin.getDefault().getPreferenceStore());
//	}
//
//	private void initUI(final Composite parent) {
//
//		_pc = new PixelConverter(parent);
//
//		DEFAULT_IMAGE_WIDTH = TourMarker.getSignImageMaxSize(_pc);
//		MAX_ROW_HEIGHT = TourMarker.getSignImageMaxSize(_pc);
//	}
//
//	@Override
//	public boolean isValid() {
//
////		saveFilterList();
//
//		return true;
//	}
//
//	@Override
//	public boolean okToLeave() {
//
//		saveState_Viewer();
//
//		return super.okToLeave();
//	}
//
//	private void onDeleteTourSign() {
//
//		/*
//		 * Get a list with all tour signs which should be deleted
//		 */
//		final ArrayList<TVIPrefSign> removedSigns = new ArrayList<TVIPrefSign>();
//
//		final StructuredSelection selection = (StructuredSelection) _signViewer.getSelection();
//		for (final Object selectedItem : selection.toArray()) {
//
//			if (selectedItem instanceof TVIPrefSign) {
//				removedSigns.add((TVIPrefSign) selectedItem);
//			}
//		}
//
//		final int selectedSignImages = removedSigns.size();
//
//		// info: no selected sign image
//		if (selectedSignImages == 0) {
//			MessageDialog.openInformation(
//					Display.getCurrent().getActiveShell(),
//					Messages.Pref_SignImages_Dialog_Delete_Title,
//					Messages.Pref_SignImages_Dialog_NoSelectedSignImage_Message);
//			return;
//		}
//
//		// confirm deletion
//		if (MessageDialog.openConfirm(
//				Display.getCurrent().getActiveShell(),
//				Messages.Pref_SignImages_Dialog_Delete_Title,
//				NLS.bind(Messages.Pref_SignImages_Dialog_ConfirmDelete_Message, selectedSignImages)) == false) {
//			return;
//		}
//
//		/*
//		 * Delete selected sign images
//		 */
//		int firstItemIndex = -1;
//		TreeViewerItem reselectParent = null;
//
//		// update model
//		EntityManager em = null;
//		try {
//
//			em = TourDatabase.getInstance().getEntityManager();
//
//			for (final TVIPrefSign tviSign : removedSigns) {
//
//				final TourSign tourSign = tviSign.getTourSign();
//
//				// remove sign from parent (category)
//
//				final TreeViewerItem tviParent = tviSign.getParentItem();
//
//				if (tviParent instanceof TVIPrefSignCategory) {
//
//					final TVIPrefSignCategory tviCategory = (TVIPrefSignCategory) tviParent;
//					final TourSignCategory signCategory = tviCategory.getTourSignCategory();
//
//					// get the index for the first removed item
//					if (firstItemIndex == -1) {
//						for (final TreeViewerItem categoryChild : tviCategory.getChildren()) {
//
//							firstItemIndex++;
//
//							if (categoryChild == tviSign) {
//								reselectParent = tviParent;
//								break;
//							}
//						}
//					}
//
//					/*
//					 * Update model
//					 */
//
//					// update cache
//					SignManager.removeCachedCategory(signCategory.getCategoryId());
//
//					// update db model
//					final TourSignCategory savedCategory = updateModel_DeleteSign(tourSign, signCategory, em);
//
//					// update viewer model
//					tviCategory.removeChild(tviSign);
//					tviCategory.setTourSignCategory(savedCategory);
//
//				} else if (tviParent instanceof TVIPrefSignRoot) {
//
//					final TVIPrefSignRoot tviRoot = (TVIPrefSignRoot) tviParent;
//
//					/*
//					 * Update model
//					 */
//
//					// update cache
//					SignManager.removeCachedCategory(SignManager.ROOT_SIGN_ID);
//
//					// update db model
////					final TourSignCategory savedCategory = updateModel_DeleteSign(tourSign, signCategory, em);
//
//					// update viewer model
//					tviRoot.removeChild(tviSign);
//				}
//			}
//
//		} finally {
//
//			if (em != null) {
//				em.close();
//			}
//		}
//
//		/*
//		 * select the item which is before the removed items, this is not yet finished because there
//		 * are multiple possibilities
//		 */
//		TreeViewerItem nextSelectedTreeItem = null;
//
//		if (reselectParent != null) {
//
//			final ArrayList<TreeViewerItem> firstSelectedChildren = reselectParent.getChildren();
//			final int remainingChildren = firstSelectedChildren.size();
//
//			if (remainingChildren > 0) {
//
//				// there are children still available
//
//				if (firstItemIndex < remainingChildren) {
//					nextSelectedTreeItem = firstSelectedChildren.get(firstItemIndex);
//				} else {
//					nextSelectedTreeItem = firstSelectedChildren.get(remainingChildren - 1);
//				}
//
//			} else {
//
//				/*
//				 * it's possible that the parent does not have any children, then also this parent
//				 * must be removed (to be done later)
//				 */
//				nextSelectedTreeItem = reselectParent;
//			}
//		}
//
//		// update UI
//		_signViewer.remove(removedSigns.toArray());
//
//		// select next item
//		if (nextSelectedTreeItem == null) {
//
//			// select first
//
//			Object firstItem = null;
//
//			final ArrayList<TreeViewerItem> rootChildren = _rootItem.getChildren();
//			if (rootChildren.size() > 0) {
//				firstItem = rootChildren.get(0);
//			}
//
//			if (firstItem != null) {
//				_signViewer.setSelection(new StructuredSelection(firstItem), true);
//			}
//
//		} else {
//			_signViewer.setSelection(new StructuredSelection(nextSelectedTreeItem), true);
//		}
//
//		_signViewer.getTree().setFocus();
//	}
//
//	private void onPaintViewer(final Event event) {
//
//		final TreeItem item = (TreeItem) event.item;
//		final Object itemData = item.getData();
//		if (itemData instanceof TVIPrefSign) {
//
//			final TVIPrefSign prefSign = (TVIPrefSign) itemData;
//
//			final Photo signImagePhoto = prefSign.getSignImagePhoto();
//
//			boolean isUpdateDimension = false;
//			if (signImagePhoto.getPhotoImageWidth() == Integer.MIN_VALUE) {
//				isUpdateDimension = true;
//			}
//
//			final Image signImage = SignManager.getSignImage(signImagePhoto, new LoadImageCallback(prefSign));
//
//			if (signImage != null) {
//
//				final int photoPosX = event.x;
//				final int photoPosY = event.y;
//
//				switch (event.type) {
//				case SWT.MeasureItem:
//
//					// this is replaced with Hack.setTreeItemHeight() for win
//
////					event.width += imageRect.width;
////					event.height = Math.max(event.height, imageRect.height + 2);
//
//					if (UI.IS_WIN == false) {
//						event.height = getRowHeight();
//					}
//
//					break;
//
//				case SWT.PaintItem:
//
//					final GC gc = event.gc;
//					final Photo photo = signImagePhoto;
//
//					final int imageCanvasWidth = Math.max(DEFAULT_ROW_HEIGHT, _imageColumnWidth);
//					final int imageCanvasHeight = event.height;
//
//					PhotoUI.paintPhotoImage(
//							gc,
//							photo,
//							signImage,
//							photoPosX,
//							photoPosY,
//							imageCanvasWidth,
//							imageCanvasHeight,
//							SWT.CENTER,
//							null);
//
//					break;
//				}
//
//				if (isUpdateDimension) {
//
//					/*
//					 * This is a hack because png images do not contain EXIF data and the image
//					 * dimension can be invalid in the photo. Force to update dimension AFTER image
//					 * is loaded and the dimension is set.
//					 */
//					_viewerContainer.getDisplay().asyncExec(new Runnable() {
//						public void run() {
//							_signViewer.update(prefSign, null);
//						}
//					});
//				}
//			}
//		}
//	}
//
//	/**
//	 * Rename selected sign/category
//	 */
//	private void onRenameTourSign() {
//
//		final Object selection = ((StructuredSelection) _signViewer.getSelection()).getFirstElement();
//
//		String name = UI.EMPTY_STRING;
//		String dlgTitle = UI.EMPTY_STRING;
//		String dlgMessage = UI.EMPTY_STRING;
//
//		if (selection instanceof TVIPrefSign) {
//
//			dlgTitle = Messages.pref_tourtag_dlg_rename_title;
//			dlgMessage = Messages.pref_tourtag_dlg_rename_message;
//			name = ((TVIPrefSign) selection).getTourSign().getSignName();
//
//		} else if (selection instanceof TVIPrefSignCategory) {
//
//			dlgTitle = Messages.pref_tourtag_dlg_rename_title_category;
//			dlgMessage = Messages.pref_tourtag_dlg_rename_message_category;
//			name = ((TVIPrefSignCategory) selection).getTourSignCategory().getCategoryName();
//		}
//
//		final InputDialog inputDialog = new InputDialog(getShell(), dlgTitle, dlgMessage, name, null);
//
//		if (inputDialog.open() != Window.OK) {
//
//			setFocusToViewer();
//			return;
//		}
//
//		// save changed name
//
//		name = inputDialog.getValue().trim();
//
//		if (selection instanceof TVIPrefSign) {
//
//			// save sign
//
//			final TVIPrefSign signItem = ((TVIPrefSign) selection);
//			final TourSign tourSign = signItem.getTourSign();
//
//			tourSign.setSignName(name);
//
//			// persist sign
//			TourDatabase.saveEntity(tourSign, tourSign.getSignId(), TourSign.class);
//
//			_signViewer.update(signItem, new String[] { SORT_PROPERTY });
//
//		} else if (selection instanceof TVIPrefSignCategory) {
//
//			// save category
//
//			final TVIPrefSignCategory tourCategoryItem = ((TVIPrefSignCategory) selection);
//			final TourSignCategory tourCategory = tourCategoryItem.getTourSignCategory();
//
//			tourCategory.setName(name);
//
//			// persist category
//			TourDatabase.saveEntity(tourCategory, tourCategory.getCategoryId(), TourSignCategory.class);
//
//			_signViewer.update(tourCategoryItem, new String[] { SORT_PROPERTY });
//
//		}
//
//		_isModified = true;
//
//		setFocusToViewer();
//	}
//
//	//	private void onNewCategory() {
////
////		final InputDialog inputDialog = new InputDialog(
////				getShell(),
////				Messages.pref_tourtag_dlg_new_tag_category_title,
////				Messages.pref_tourtag_dlg_new_tag_category_message,
////				UI.EMPTY_STRING,
////				null);
////
////		if (inputDialog.open() != Window.OK) {
////			setFocusToViewer();
////			return;
////		}
////
////		// create sign category + tree item
////		final TourSignCategory newCategory = new TourSignCategory(inputDialog.getValue().trim());
////		final TVIPrefSignCategory newCategoryItem = new TVIPrefSignCategory(_signViewer, newCategory);
////
////		final Object parentElement = ((StructuredSelection) _signViewer.getSelection()).getFirstElement();
////		TourSignCategory savedNewCategory = null;
////
////		if (parentElement == null) {
////
////			// a parent is not selected, this will be a root category
////
////			newCategory.setRoot(true);
////
////			/*
////			 * update model
////			 */
////
////			_rootItem.getFetchedChildren().add(newCategoryItem);
////
////			// persist new category
////			savedNewCategory = TourDatabase
////					.saveEntity(newCategory, newCategory.getCategoryId(), TourSignCategory.class);
////			if (savedNewCategory != null) {
////
////				// update item
////				newCategoryItem.setTourSignCategory(savedNewCategory);
////
////				// update viewer
////				_signViewer.add(this, newCategoryItem);
////			}
////
////		} else if (parentElement instanceof TVIPrefSignCategory) {
////
////			// parent is a category
////
////			final TVIPrefSignCategory parentCategoryItem = (TVIPrefSignCategory) parentElement;
////			final TourSignCategory parentCategory = parentCategoryItem.getTourSignCategory();
////
////			/*
////			 * update model
////			 */
////
////			final EntityManager em = TourDatabase.getInstance().getEntityManager();
////
////			// persist new category
////			savedNewCategory = TourDatabase
////					.saveEntity(newCategory, newCategory.getCategoryId(), TourSignCategory.class);
////			if (savedNewCategory != null) {
////
////				// update item
////				newCategoryItem.setTourSignCategory(savedNewCategory);
////
////				/*
////				 * update parent category
////				 */
////				final TourSignCategory parentCategoryEntity = em.find(
////						TourSignCategory.class,
////						parentCategory.getCategoryId());
////
////				// set sign in parent category
////				final Set<TourSignCategory> lazyTourSignCategories = parentCategoryEntity.getSignCategories();
////				lazyTourSignCategories.add(savedNewCategory);
////
////				// update number of categories
////				parentCategoryEntity.setCategoryCounter(lazyTourSignCategories.size());
////
////				/*
////				 * persist parent category
////				 */
////				final TourSignCategory savedParentCategory = TourDatabase.saveEntity(
////						parentCategoryEntity,
////						parentCategoryEntity.getCategoryId(),
////						TourSignCategory.class);
////
////				if (savedParentCategory != null) {
////
////					// update item
////					parentCategoryItem.setTourSignCategory(savedParentCategory);
////
////					/*
////					 * update viewer
////					 */
////					parentCategoryItem.clearChildren();
////
//////					fSignViewer.update(parentCategoryItem, null);
////
////					_signViewer.add(parentCategoryItem, newCategoryItem);
////
////					_signViewer.expandToLevel(parentCategoryItem, 1);
////				}
////			}
////
////			em.close();
////
////		}
////
////		if (savedNewCategory != null) {
////
////			// reveal new sign in viewer
////			_signViewer.reveal(newCategoryItem);
////
////			_isModified = true;
////		}
////
////		setFocusToViewer();
////	}
////
////	/**
////	 * <pre>
////	 *
////	 * category	--- category
////	 * category	--- sign
////	 * 			+-- sign
////	 * category	--- category
////	 * 			+-- category --- sign
////	 * 						 +-- sign
////	 * 			+-- sign
////	 * 			+-- sign
////	 * 			+-- sign
////	 * sign
////	 * sign
////	 *
////	 * </pre>
////	 */
////	private void onNewSign() {
////
////		final InputDialog inputDialog = new InputDialog(
////				getShell(),
////				Messages.pref_tourtag_dlg_new_tag_title,
////				Messages.pref_tourtag_dlg_new_tag_message,
////				UI.EMPTY_STRING,
////				null);
////
////		if (inputDialog.open() != Window.OK) {
////			setFocusToViewer();
////			return;
////		}
////
////		TourSign savedSign = null;
////
////		// create new tour sign + item
////		final TourSign tourSign = new TourSign(inputDialog.getValue().trim());
////		final TVIPrefSign signItem = new TVIPrefSign(_signViewer, tourSign);
////
////		final Object parentItem = ((StructuredSelection) _signViewer.getSelection()).getFirstElement();
////		if (parentItem == null) {
////
////			// a parent is not selected, this will be a root sign
////
////			tourSign.setRoot(true);
////
////			/*
////			 * update model
////			 */
////			signItem.setParentItem(_rootItem);
////			_rootItem.getFetchedChildren().add(signItem);
////
////			// persist sign
////			savedSign = TourDatabase.saveEntity(tourSign, TourDatabase.ENTITY_IS_NOT_SAVED, TourSign.class);
////
////			if (savedSign != null) {
////
////				// update item
////				signItem.setTourSign(savedSign);
////
////				/*
////				 * update viewer
////				 */
////				_signViewer.add(this, signItem);
////			}
////
////		} else if (parentItem instanceof TVIPrefSignCategory) {
////
////			// parent is a category
////
////			final TVIPrefSignCategory parentCategoryItem = (TVIPrefSignCategory) parentItem;
////			TourSignCategory parentSignCategory = parentCategoryItem.getTourSignCategory();
////
////			/*
////			 * update model
////			 */
////
////			// set parent into sign
////			signItem.setParentItem(parentCategoryItem);
////
////			/*
////			 * persist sign without new category otherwise an exception "detached entity passed to
////			 * persist: net.tourbook.data.TourSignCategory" is raised
////			 */
////			savedSign = TourDatabase.saveEntity(tourSign, TourDatabase.ENTITY_IS_NOT_SAVED, TourSign.class);
////			if (savedSign != null) {
////
////				// update item
////				signItem.setTourSign(savedSign);
////
////				// update parent category
////				final EntityManager em = TourDatabase.getInstance().getEntityManager();
////				{
////
////					final TourSignCategory parentSignCategoryEntity = em.find(
////							TourSignCategory.class,
////							parentSignCategory.getCategoryId());
////
////					// set new entity
////					parentSignCategory = parentSignCategoryEntity;
////					parentCategoryItem.setTourSignCategory(parentSignCategoryEntity);
////
////					// set sign into parent category
////					final Set<TourSign> lazyTourSigns = parentSignCategoryEntity.getTourSigns();
////					lazyTourSigns.add(tourSign);
////
////					parentSignCategory.setSignCounter(lazyTourSigns.size());
////				}
////				em.close();
////
////				// persist parent category
////				final TourSignCategory savedParent = TourDatabase.saveEntity(
////						parentSignCategory,
////						parentSignCategory.getCategoryId(),
////						TourSignCategory.class);
////
////				if (savedParent != null) {
////
////					// update item
////					parentCategoryItem.setTourSignCategory(savedParent);
////
////					// set category in sign
////					tourSign.getSignCategories().add(parentSignCategory);
////
////					// persist sign with category
////					savedSign = TourDatabase.saveEntity(tourSign, tourSign.getSignId(), TourSign.class);
////
////				}
////
////			}
////
////			if (savedSign != null) {
////
////				// clear tour sign list
////				TourDatabase.clearTourSigns();
////
////				/*
////				 * update viewer
////				 */
////				parentCategoryItem.clearChildren();
////
////				_signViewer.add(parentCategoryItem, signItem);
////				_signViewer.update(parentCategoryItem, null);
////
////				_signViewer.expandToLevel(parentCategoryItem, 1);
////			}
////		}
////
////		if (savedSign != null) {
////
////			// show new sign in viewer
////			_signViewer.reveal(signItem);
////
////			_isModified = true;
////		}
////
////		setFocusToViewer();
////	}
//
//	private void onResizeImageColumn() {
//
//		final int imageColumnWidth = getImageColumnWidth();
//
//		// check if the width has changed
//		if (imageColumnWidth == _imageColumnWidth) {
//			return;
//		}
//
//		_imageColumnWidth = imageColumnWidth;
//
//		// update images
//		if (UI.IS_WIN) {
//			Hack.setTreeItemHeight(_signViewer.getTree(), getRowHeight());
//		}
//	}
//
//	@Override
//	public boolean performCancel() {
//
//		saveState_Viewer();
//		fireModifyEvent();
//
//		return true;
//	}
//
//	@Override
//	public boolean performOk() {
//
//		saveState();
//
//		fireModifyEvent();
//
//		return true;
//	}
//
//	@Override
//	public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {
//
//		_viewerContainer.setRedraw(false);
//		{
//			final Object[] expandedElements = _signViewer.getExpandedElements();
//			final ISelection selection = _signViewer.getSelection();
//
//			_signViewer.getTree().dispose();
//
//			createUI_22_SignViewer_Table(_viewerContainer);
//
//			_viewerContainer.layout();
//
//			// update the viewer
//			reloadViewer();
//
//			_signViewer.setExpandedElements(expandedElements);
//			_signViewer.setSelection(selection);
//		}
//		_viewerContainer.setRedraw(true);
//
//		onResizeImageColumn();
//
//		return _signViewer;
//	}
//
//	@Override
//	public void reloadViewer() {
//		_signViewer.setInput(new Object[0]);
//	}
//
//	/**
//	 * Restore viewer state after the viewer is loaded.
//	 */
//	private void restoreState_Viewer() {
//
//		/*
//		 * Expanded sign categories
//		 */
//		final long[] allStateItems = Util.getStateLongArray(_state, STATE_EXPANDED_ITEMS, null);
//		if (allStateItems != null) {
//
//			final ArrayList<TreePath> viewerTreePaths = new ArrayList<TreePath>();
//
//			final ArrayList<StateSegment[]> allStateSegments = restoreState_Viewer_GetSegments(allStateItems);
//			for (final StateSegment[] stateSegments : allStateSegments) {
//
//				final ArrayList<Object> pathSegments = new ArrayList<>();
//
//				// start tree items with the root and go deeper with every segment
//				ArrayList<TreeViewerItem> treeItems = _rootItem.getFetchedChildren();
//
//				for (final StateSegment stateSegment : stateSegments) {
//
//					/*
//					 * This is somehow recursive as it goes deeper into the child tree items until
//					 * there are no children
//					 */
//					treeItems = restoreState_Viewer_ExpandItem(pathSegments, treeItems, stateSegment);
//				}
//
//				if (pathSegments.size() > 0) {
//					viewerTreePaths.add(new TreePath(pathSegments.toArray()));
//				}
//			}
//
//			if (viewerTreePaths.size() > 0) {
//				_signViewer.setExpandedTreePaths(viewerTreePaths.toArray(new TreePath[viewerTreePaths.size()]));
//			}
//		}
//
//		// set selection to first item, this also makes the focus visible
//		final ArrayList<TreeViewerItem> treeItems = _rootItem.getChildren();
//		if (treeItems.size() > 0) {
//			_signViewer.setSelection(new StructuredSelection(treeItems.get(0)));
//		}
//
//		setFocusToViewer();
//	}
//
//	/**
//	 * @param pathSegments
//	 * @param treeItems
//	 * @param stateSegment
//	 * @return Returns children when it could be expanded otherwise <code>null</code>.
//	 */
//	private ArrayList<TreeViewerItem> restoreState_Viewer_ExpandItem(	final ArrayList<Object> pathSegments,
//																		final ArrayList<TreeViewerItem> treeItems,
//																		final StateSegment stateSegment) {
//
//		if (treeItems == null) {
//			return null;
//		}
//
//		final long stateData = stateSegment.__itemData;
//
//		if (stateSegment.__itemType == STATE_ITEM_TYPE_CATEGORY) {
//
//			for (final TreeViewerItem treeItem : treeItems) {
//
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
//			}
//
//		} else if (stateSegment.__itemType == STATE_ITEM_TYPE_SIGN) {
//
//			for (final TreeViewerItem treeItem : treeItems) {
//
//				if (treeItem instanceof TVIPrefSign) {
//
//					final TVIPrefSign viewerSign = (TVIPrefSign) treeItem;
//					final long viewerSignId = viewerSign.getTourSign().getSignId();
//
//					if (viewerSignId == stateData) {
//
//						pathSegments.add(treeItem);
//
//						return viewerSign.getFetchedChildren();
//					}
//				}
//			}
//		}
//
//		return null;
//	}
//
//	/**
//	 * Convert state structure into a 'segment' structure.
//	 */
//	private ArrayList<StateSegment[]> restoreState_Viewer_GetSegments(final long[] expandedItems) {
//
//		final ArrayList<StateSegment[]> allTreePathSegments = new ArrayList<StateSegment[]>();
//		final ArrayList<StateSegment> currentSegments = new ArrayList<StateSegment>();
//
//		for (int itemIndex = 0; itemIndex < expandedItems.length;) {
//
//			// ensure array bounds
//			if (itemIndex + 1 >= expandedItems.length) {
//				// this should not happen when data are not corrupted
//				break;
//			}
//
//			final long itemType = expandedItems[itemIndex++];
//			final long itemData = expandedItems[itemIndex++];
//
//			if (itemType == STATE_ITEM_TYPE_SEPARATOR) {
//
//				// a new tree path starts
//
//				if (currentSegments.size() > 0) {
//
//					// keep current tree path segments
//
//					allTreePathSegments.add(currentSegments.toArray(new StateSegment[currentSegments.size()]));
//
//					// start a new path
//					currentSegments.clear();
//				}
//
//			} else {
//
//				// a new segment is available
//
//				if (itemType == STATE_ITEM_TYPE_CATEGORY || itemType == STATE_ITEM_TYPE_SIGN) {
//
//					currentSegments.add(new StateSegment(itemType, itemData));
//				}
//			}
//		}
//
//		if (currentSegments.size() > 0) {
//			allTreePathSegments.add(currentSegments.toArray(new StateSegment[currentSegments.size()]));
//		}
//
//		return allTreePathSegments;
//	}
//
//	/**
//	 * save state of the pref page
//	 */
//	private void saveState() {
//
//		saveState_Viewer();
//	}
//
//	private void saveState_Viewer() {
//
//		// viewer state
//		_columnManager.saveState(_state);
//
//		saveState_Viewer_ExpandedItems();
//	}
//
//	/**
//	 * Save state for expanded tree items.
//	 */
//	private void saveState_Viewer_ExpandedItems() {
//
//		final Object[] visibleExpanded = _signViewer.getVisibleExpandedElements();
//
//		if (visibleExpanded.length == 0) {
//			Util.setState(_state, STATE_EXPANDED_ITEMS, new long[0]);
//			return;
//		}
//
//		final TLongArrayList expandedItems = new TLongArrayList();
//
//		final TreePath[] expandedOpenedTreePaths = UI.getExpandedOpenedItems(
//				visibleExpanded,
//				_signViewer.getExpandedTreePaths());
//
//		for (final TreePath expandedPath : expandedOpenedTreePaths) {
//
//			// start a new path, allways set it twice to have a even structure
//			expandedItems.add(STATE_ITEM_TYPE_SEPARATOR);
//			expandedItems.add(STATE_ITEM_TYPE_SEPARATOR);
//
//			for (int segmentIndex = 0; segmentIndex < expandedPath.getSegmentCount(); segmentIndex++) {
//
//				final Object segment = expandedPath.getSegment(segmentIndex);
//
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
//			}
//		}
//
//		Util.setState(_state, STATE_EXPANDED_ITEMS, expandedItems.toArray());
//	}
//
//	private void setFocusToViewer() {
//
//		// set focus back to the tree
//		_signViewer.getTree().setFocus();
//	}
//
//	public void setIsModified() {
//		_isModified = true;
//	}
//
//	/**
//	 * Removes a {@link TourSign} from a {@link TourSignCategory} and updates the model (database).
//	 *
//	 * @param tourSign
//	 * @param signCategory
//	 * @param em
//	 * @return Returns the saved entity
//	 */
//	private TourSignCategory updateModel_DeleteSign(final TourSign tourSign,
//													final TourSignCategory signCategory,
//													final EntityManager em) {
//
//		final TourSignCategory lazyCategory = em.find(TourSignCategory.class, signCategory.getCategoryId());
//
//		// remove sign
//		final Set<TourSign> lazyTourSigns = lazyCategory.getTourSigns();
//		lazyTourSigns.remove(tourSign);
//
//		// update counter
//		lazyCategory.setSignCounter(lazyTourSigns.size());
//		lazyCategory.setCategoryCounter(lazyCategory.getTourSignCategories().size());
//
//		return TourDatabase.saveEntity(lazyCategory, lazyCategory.getCategoryId(), TourSignCategory.class);
//	}

}
