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

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.data.TourSign;
import net.tourbook.data.TourSignCategory;
import net.tourbook.database.TourDatabase;
import net.tourbook.sign.TVIPrefSign;
import net.tourbook.sign.TVIPrefSignCategory;
import net.tourbook.sign.TVIPrefSignRoot;
import net.tourbook.ui.UI;
import net.tourbook.ui.action.ActionCollapseAll;
import net.tourbook.ui.action.ActionExpandSelection;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class PrefPageSigns extends PreferencePage implements IWorkbenchPreferencePage, ITourViewer {

	private static final String		SORT_PROPERTY	= "sort";							//$NON-NLS-1$

	private final IPreferenceStore	_prefStore		= TourbookPlugin.getPrefStore();

	private IPropertyChangeListener	_prefChangeListener;

	private TVIPrefSignRoot			_rootItem;

	private boolean					_isModified		= false;

	private long					_dragStartTime;

	/*
	 * UI constrols
	 */
	private TreeViewer				_signViewer;
	private ToolBar					_toolBar;

	private Button					_btnNewSign;
	private Button					_btnNewSignCategory;
	private Button					_btnRename;
	private Button					_btnReset;

	/**
	 * Sort the signs and categories
	 */
	private final static class SignViewerComparator extends ViewerComparator {
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

	private final class SignViewerContentProvicer implements ITreeContentProvider {

		public void dispose() {}

		public Object[] getChildren(final Object parentElement) {
			return ((TreeViewerItem) parentElement).getFetchedChildrenAsArray();
		}

		public Object[] getElements(final Object inputElement) {
			return _rootItem.getFetchedChildrenAsArray();
		}

		public Object getParent(final Object element) {
			return ((TreeViewerItem) element).getParentItem();
		}

//		public TreeViewerItem getRootItem() {
//			return _rootItem;
//		}

		public boolean hasChildren(final Object element) {
			return ((TreeViewerItem) element).hasChildren();
		}

		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	public PrefPageSigns() {}

	public PrefPageSigns(final String title) {
		super(title);
	}

	public PrefPageSigns(final String title, final ImageDescriptor image) {
		super(title, image);
	}

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

					_signViewer.getTree().setLinesVisible(
							getPreferenceStore().getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

					_signViewer.refresh();

					/*
					 * the tree must be redrawn because the styled text does not display the new
					 * color
					 */
					_signViewer.getTree().redraw();
				}
			}
		};

		// register the listener
		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	@Override
	protected Control createContents(final Composite parent) {

		final Composite ui = createUI(parent);

		fillToolbar();

		// set root item
		_rootItem = new TVIPrefSignRoot(_signViewer);

		updateSignViewer();
		enableButtons();
		addPrefListener();

		return ui;
	}

	private Composite createUI(final Composite parent) {

		// container
		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.margins(0, 0)
				.spacing(SWT.DEFAULT, 0)
				.numColumns(2)
				.applyTo(container);
		{
			createUI_10_Title(container);

			createUI_20_SignViewer(container);
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

	private void createUI_20_SignViewer(final Composite parent) {

		/*
		 * create tree layout
		 */

		final Composite layoutContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.hint(200, 100)
				.applyTo(layoutContainer);

		final TreeColumnLayout treeLayout = new TreeColumnLayout();
		layoutContainer.setLayout(treeLayout);

		/*
		 * create viewer
		 */
		final Tree tree = new Tree(layoutContainer, SWT.H_SCROLL
				| SWT.V_SCROLL
				| SWT.BORDER
				| SWT.MULTI
				| SWT.FULL_SELECTION);

		tree.setHeaderVisible(false);
		tree.setLinesVisible(getPreferenceStore().getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

		_signViewer = new TreeViewer(tree);

		_signViewer.setContentProvider(new SignViewerContentProvicer());
		_signViewer.setComparator(new SignViewerComparator());
		_signViewer.setUseHashlookup(true);

		_signViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(final DoubleClickEvent event) {

				final Object selection = ((IStructuredSelection) _signViewer.getSelection()).getFirstElement();

				if (selection instanceof TVIPrefSign) {

					// sign is selected

					onRenameTourSign();

				} else if (selection instanceof TVIPrefSignCategory) {

					// expand/collapse current item

					final TreeViewerItem tourItem = (TreeViewerItem) selection;

					if (_signViewer.getExpandedState(tourItem)) {
						_signViewer.collapseToLevel(tourItem, 1);
					} else {
						_signViewer.expandToLevel(tourItem, 1);
					}
				}
			}
		});

		_signViewer.addSelectionChangedListener(new ISelectionChangedListener() {
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

		/*
		 * create columns
		 */
		TreeViewerColumn tvc;
		TreeColumn tvcColumn;

		// column: signs + sign categories
		tvc = new TreeViewerColumn(_signViewer, SWT.TRAIL);
		tvcColumn = tvc.getColumn();
		tvc.setLabelProvider(new StyledCellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final StyledString styledString = new StyledString();

				final Object element = cell.getElement();
				if (element instanceof TVIPrefSign) {

					final TourSign tourSign = ((TVIPrefSign) element).getTourSign();

					styledString.append(tourSign.getSignName(), UI.TAG_STYLER);

				} else if (element instanceof TVIPrefSignCategory) {

					final TVIPrefSignCategory tourSignCategoryItem = (TVIPrefSignCategory) element;
					final TourSignCategory tourSignCategory = tourSignCategoryItem.getTourSignCategory();

					styledString.append(tourSignCategory.getCategoryName(), UI.TAG_CATEGORY_STYLER);

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
		treeLayout.setColumnData(tvcColumn, new ColumnWeightData(100, true));
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

			// button: reset
			_btnReset = new Button(container, SWT.NONE);
			_btnReset.setText(Messages.pref_tourtag_btn_reset);
			setButtonLayoutData(_btnReset);
			final GridData gd = (GridData) _btnReset.getLayoutData();
			gd.verticalIndent = 50;

			_btnReset.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onReset();
				}
			});
		}
	}

	private void createUI_40_Bottom(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(container);
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

	@Override
	public void dispose() {

		_prefStore.removePropertyChangeListener(_prefChangeListener);

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

		tbm.add(new ActionExpandSelection(this));
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

	public ColumnManager getColumnManager() {
		return null;
	}

	public long getDragStartTime() {
		return _dragStartTime;
	}

	public TVIPrefSignRoot getRootItem() {
		return _rootItem;
	}

	public ColumnViewer getViewer() {
		return _signViewer;
	}

	public void init(final IWorkbench workbench) {
		setPreferenceStore(TourbookPlugin.getDefault().getPreferenceStore());
	}

	@Override
	public boolean isValid() {

//		saveFilterList();

		return true;
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

			final TVIPrefSign tourSignItem = ((TVIPrefSign) selection);
			final TourSign tourSign = tourSignItem.getTourSign();

			tourSign.setSignName(name);

			// persist sign
			TourDatabase.saveEntity(tourSign, tourSign.getSignId(), TourSign.class);

			_signViewer.update(tourSignItem, new String[] { SORT_PROPERTY });

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
			UI.showSQLException(e);
		}

		setFocusToViewer();
	}

	@Override
	public boolean performCancel() {
		fireModifyEvent();
		return true;
	}

	@Override
	public boolean performOk() {
		fireModifyEvent();
		return true;
	}

	public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {
		return null;
	}

	public void reloadViewer() {}

	private void setFocusToViewer() {

		// set focus back to the tree
		_signViewer.getTree().setFocus();
	}

	public void setIsModified() {
		_isModified = true;
	}

	private void updateSignViewer() {

		// show contents in the viewers
		_signViewer.setInput(this);

		enableButtons();
	}

}
