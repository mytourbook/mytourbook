/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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
import java.util.Set;

import javax.persistence.EntityManager;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.ITreeViewer;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourTagCategory;
import net.tourbook.database.TourDatabase;
import net.tourbook.tag.TVIPrefTag;
import net.tourbook.tag.TVIPrefTagCategory;
import net.tourbook.tag.TVIPrefTagRoot;
import net.tourbook.tag.TagMenuManager;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
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
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
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
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
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

public class PrefPageTags extends PreferencePage implements IWorkbenchPreferencePage, ITourViewer, ITreeViewer {

	public static final String		ID				= "net.tourbook.preferences.PrefPageTags";			//$NON-NLS-1$

	private static final String		SORT_PROPERTY	= "sort";											//$NON-NLS-1$

	private final IPreferenceStore	_prefStore		= TourbookPlugin.getDefault().getPreferenceStore();

	private IPropertyChangeListener	_prefChangeListener;

	private TVIPrefTagRoot			_rootItem;

	private boolean					_isModified		= false;

	private long					_dragStartTime;

	/*
	 * image resources
	 */
	private Image					_imgTag			= TourbookPlugin
			.getImageDescriptor(Messages.Image__tag)
			.createImage();
	private Image					_imgTagRoot		= TourbookPlugin
			.getImageDescriptor(Messages.Image__tag_root)
			.createImage();
	private Image					_imgTagCategory	= TourbookPlugin
			.getImageDescriptor(Messages.Image__tag_category)
			.createImage();

	/*
	 * UI constrols
	 */
	private TreeViewer				_tagViewer;
	private ToolBar					_toolBar;

	private Button					_btnNewTag;
	private Button					_btnNewTagCategory;
	private Button					_btnRename;
	private Button					_btnReset;

	/*
	 * None UI controls
	 */

	/**
	 * Sort the tags and categories
	 */
	private final static class TagViewerComparator extends ViewerComparator {
		@Override
		public int compare(final Viewer viewer, final Object obj1, final Object obj2) {
			if (obj1 instanceof TVIPrefTag && obj2 instanceof TVIPrefTag) {

				// sort tags by name
				final TourTag tourTag1 = ((TVIPrefTag) (obj1)).getTourTag();
				final TourTag tourTag2 = ((TVIPrefTag) (obj2)).getTourTag();

				return tourTag1.getTagName().compareTo(tourTag2.getTagName());

			} else if (obj1 instanceof TVIPrefTag && obj2 instanceof TVIPrefTagCategory) {

				// sort category before tag
				return 1;

			} else if (obj2 instanceof TVIPrefTag && obj1 instanceof TVIPrefTagCategory) {

				// sort category before tag
				return -1;

			} else if (obj1 instanceof TVIPrefTagCategory && obj2 instanceof TVIPrefTagCategory) {

				// sort categories by name
				final TourTagCategory tourTagCat1 = ((TVIPrefTagCategory) (obj1)).getTourTagCategory();
				final TourTagCategory tourTagCat2 = ((TVIPrefTagCategory) (obj2)).getTourTagCategory();

				return tourTagCat1.getCategoryName().compareTo(tourTagCat2.getCategoryName());
			}

			return 0;
		}

		@Override
		public boolean isSorterProperty(final Object element, final String property) {
			// sort when the name has changed
			return true;
		}
	}

	private final class TagViewerContentProvicer implements ITreeContentProvider {

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

	public PrefPageTags() {}

	public PrefPageTags(final String title) {
		super(title);
	}

	public PrefPageTags(final String title, final ImageDescriptor image) {
		super(title, image);
	}

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

					_tagViewer.getTree().setLinesVisible(
							getPreferenceStore().getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

					_tagViewer.refresh();

					/*
					 * the tree must be redrawn because the styled text does not display the new
					 * color
					 */
					_tagViewer.getTree().redraw();
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
		_rootItem = new TVIPrefTagRoot(_tagViewer, true);

		updateTagViewer();
		enableButtons();
		addPrefListener();

		return ui;
	}

	private Composite createUI(final Composite parent) {

		// container
		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory
				.fillDefaults()//
				.margins(0, 0)
				//				.spacing(SWT.DEFAULT, 0)
				.numColumns(2)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			createUI_10_Title(container);

			createUI_20_TagViewer(container);
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

	private void createUI_20_TagViewer(final Composite parent) {

		/*
		 * create tree layout
		 */

		final Composite layoutContainer = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()//
				.grab(true, true)
				.hint(200, 100)
				.applyTo(layoutContainer);

		final TreeColumnLayout treeLayout = new TreeColumnLayout();
		layoutContainer.setLayout(treeLayout);

		/*
		 * create viewer
		 */
		final Tree tree = new Tree(layoutContainer, SWT.H_SCROLL | SWT.V_SCROLL
//				| SWT.BORDER
				| SWT.MULTI
				| SWT.FULL_SELECTION);

		tree.setHeaderVisible(false);
		tree.setLinesVisible(getPreferenceStore().getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

		_tagViewer = new TreeViewer(tree);

		_tagViewer.setContentProvider(new TagViewerContentProvicer());
		_tagViewer.setComparator(new TagViewerComparator());
		_tagViewer.setUseHashlookup(true);

		_tagViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(final DoubleClickEvent event) {

				final Object selection = ((IStructuredSelection) _tagViewer.getSelection()).getFirstElement();

				if (selection instanceof TVIPrefTag) {

					// tag is selected

					onRenameTourTag();

				} else if (selection instanceof TVIPrefTagCategory) {

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

		_tagViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				enableButtons();
			}
		});

		_tagViewer.addDragSupport(
				DND.DROP_MOVE,
				new Transfer[] { LocalSelectionTransfer.getTransfer() },
				new DragSourceListener() {

					@Override
					public void dragFinished(final DragSourceEvent event) {

						final LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();

						if (event.doit == false) {
							return;
						}

						transfer.setSelection(null);
						transfer.setSelectionSetTime(0);
					}

					@Override
					public void dragSetData(final DragSourceEvent event) {
						// data are set in LocalSelectionTransfer
					}

					@Override
					public void dragStart(final DragSourceEvent event) {

						final LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
						final ISelection selection = _tagViewer.getSelection();

						//						System.out.println("dragStart");
						transfer.setSelection(selection);
						transfer.setSelectionSetTime(_dragStartTime = event.time & 0xFFFFFFFFL);

						event.doit = !selection.isEmpty();
					}
				});

		_tagViewer.addDropSupport(
				DND.DROP_MOVE,
				new Transfer[] { LocalSelectionTransfer.getTransfer() },
				new TagDropAdapter(this, _tagViewer));

		/*
		 * create columns
		 */
		TreeViewerColumn tvc;
		TreeColumn tvcColumn;

		// column: tags + tag categories
		tvc = new TreeViewerColumn(_tagViewer, SWT.TRAIL);
		tvcColumn = tvc.getColumn();
		tvc.setLabelProvider(new StyledCellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final StyledString styledString = new StyledString();

				final Object element = cell.getElement();
				if (element instanceof TVIPrefTag) {

					final TourTag tourTag = ((TVIPrefTag) element).getTourTag();

					styledString.append(tourTag.getTagName(), UI.TAG_STYLER);
					cell.setImage(tourTag.isRoot() ? _imgTagRoot : _imgTag);

				} else if (element instanceof TVIPrefTagCategory) {

					final TVIPrefTagCategory tourTagCategoryItem = (TVIPrefTagCategory) element;
					final TourTagCategory tourTagCategory = tourTagCategoryItem.getTourTagCategory();

					cell.setImage(_imgTagCategory);

					styledString.append(tourTagCategory.getCategoryName(), UI.TAG_CATEGORY_STYLER);

					// get number of categories
					final int categoryCounter = tourTagCategory.getCategoryCounter();
					final int tagCounter = tourTagCategory.getTagCounter();
					if (categoryCounter == -1 && tagCounter == -1) {

//						styledString.append("  ...", StyledString.COUNTER_STYLER);

					} else {

						String categoryString = UI.EMPTY_STRING;
						if (categoryCounter > 0) {
							categoryString = "/" + categoryCounter; //$NON-NLS-1$
						}
						styledString.append("   " + tagCounter + categoryString, StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
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
		GridDataFactory
				.fillDefaults()//
				.indent(5, 0)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().applyTo(container);
		{
			// button: new tag
			_btnNewTag = new Button(container, SWT.NONE);
			_btnNewTag.setText(Messages.pref_tourtag_btn_new_tag);
			setButtonLayoutData(_btnNewTag);
			_btnNewTag.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onNewTag();
				}
			});

			// button: new tag category
			_btnNewTagCategory = new Button(container, SWT.NONE);
			_btnNewTagCategory.setText(Messages.pref_tourtag_btn_new_tag_category);
			setButtonLayoutData(_btnNewTagCategory);
			_btnNewTagCategory.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onNewCategory();
				}
			});

			// button: rename
			_btnRename = new Button(container, SWT.NONE);
			_btnRename.setText(Messages.pref_tourtag_btn_rename);
			setButtonLayoutData(_btnRename);
			_btnRename.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onRenameTourTag();
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
		GridDataFactory
				.fillDefaults()//
				.grab(true, false)
				.span(2, 1)
				//				.indent(0, _pc.convertVerticalDLUsToPixels(4))
				.applyTo(container);
		GridLayoutFactory
				.fillDefaults()//
				.numColumns(1)
				//				.extendedMargins(0, 0, top, bottom)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
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

		_imgTag.dispose();
		_imgTagRoot.dispose();
		_imgTagCategory.dispose();

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		super.dispose();
	}

	private void enableButtons() {

		final IStructuredSelection selectedTags = (IStructuredSelection) _tagViewer.getSelection();
		final Object selection = selectedTags.getFirstElement();

		boolean isTourTag = false;
		boolean isTagCategory = false;
		final boolean isSelection = selection != null;

		if (selection instanceof TVIPrefTag) {
			isTourTag = true;
		} else if (selection instanceof TVIPrefTagCategory) {
			isTagCategory = true;
		}

		_btnNewTag.setEnabled(isSelection == false || isTagCategory == true && isTourTag == false);
		_btnNewTagCategory.setEnabled(isSelection == false || isTagCategory == true && isTourTag == false);
		_btnRename.setEnabled(selectedTags.size() == 1);
	}

	/**
	 * set the toolbar action after the {@link #_tagViewer} is created
	 */
	private void fillToolbar() {

		final ToolBarManager tbm = new ToolBarManager(_toolBar);

		tbm.add(new ActionExpandSelection(this));
		tbm.add(new ActionCollapseAll(this));

		tbm.update(true);
	}

	private void fireModifyEvent() {

		if (_isModified) {
			_isModified = false;

			// remove old tags from cached tours
			TourDatabase.clearTourTags();

			TagMenuManager.updateRecentTagNames();

			TourManager.getInstance().clearTourDataCache();

			// fire modify event
			TourManager.fireEvent(TourEventId.TAG_STRUCTURE_CHANGED);
		}
	}

	@Override
	public ColumnManager getColumnManager() {
		return null;
	}

	public long getDragStartTime() {
		return _dragStartTime;
	}

	public TVIPrefTagRoot getRootItem() {
		return _rootItem;
	}

	@Override
	public TreeViewer getTreeViewer() {
		return _tagViewer;
	}

	@Override
	public ColumnViewer getViewer() {
		return _tagViewer;
	}

	@Override
	public void init(final IWorkbench workbench) {
		setPreferenceStore(TourbookPlugin.getDefault().getPreferenceStore());
	}

	@Override
	public boolean isValid() {

//		saveFilterList();

		return true;
	}

	private void onNewCategory() {

		final InputDialog inputDialog = new InputDialog(
				getShell(),
				Messages.pref_tourtag_dlg_new_tag_category_title,
				Messages.pref_tourtag_dlg_new_tag_category_message,
				UI.EMPTY_STRING,
				null);

		if (inputDialog.open() != Window.OK) {
			setFocusToViewer();
			return;
		}

		// create tag category + tree item
		final TourTagCategory newCategory = new TourTagCategory(inputDialog.getValue().trim());
		final TVIPrefTagCategory newCategoryItem = new TVIPrefTagCategory(_tagViewer, newCategory);

		final Object parentElement = ((StructuredSelection) _tagViewer.getSelection()).getFirstElement();
		TourTagCategory savedNewCategory = null;

		if (parentElement == null) {

			// a parent is not selected, this will be a root category

			newCategory.setRoot(true);

			/*
			 * update model
			 */

			_rootItem.getFetchedChildren().add(newCategoryItem);

			// persist new category
			savedNewCategory = TourDatabase.saveEntity(newCategory, newCategory.getCategoryId(), TourTagCategory.class);
			if (savedNewCategory != null) {

				// update item
				newCategoryItem.setTourTagCategory(savedNewCategory);

				// update viewer
				_tagViewer.add(this, newCategoryItem);
			}

		} else if (parentElement instanceof TVIPrefTagCategory) {

			// parent is a category

			final TVIPrefTagCategory parentCategoryItem = (TVIPrefTagCategory) parentElement;
			final TourTagCategory parentCategory = parentCategoryItem.getTourTagCategory();

			/*
			 * update model
			 */

			final EntityManager em = TourDatabase.getInstance().getEntityManager();

			// persist new category
			savedNewCategory = TourDatabase.saveEntity(newCategory, newCategory.getCategoryId(), TourTagCategory.class);
			if (savedNewCategory != null) {

				// update item
				newCategoryItem.setTourTagCategory(savedNewCategory);

				/*
				 * update parent category
				 */
				final TourTagCategory parentCategoryEntity = em.find(
						TourTagCategory.class,
						parentCategory.getCategoryId());

				// set tag in parent category
				final Set<TourTagCategory> lazyTourTagCategories = parentCategoryEntity.getTagCategories();
				lazyTourTagCategories.add(savedNewCategory);

				// update number of categories
				parentCategoryEntity.setCategoryCounter(lazyTourTagCategories.size());

				/*
				 * persist parent category
				 */
				final TourTagCategory savedParentCategory = TourDatabase.saveEntity(
						parentCategoryEntity,
						parentCategoryEntity.getCategoryId(),
						TourTagCategory.class);

				if (savedParentCategory != null) {

					// update item
					parentCategoryItem.setTourTagCategory(savedParentCategory);

					/*
					 * update viewer
					 */
					parentCategoryItem.clearChildren();

//					fTagViewer.update(parentCategoryItem, null);

					_tagViewer.add(parentCategoryItem, newCategoryItem);

					_tagViewer.expandToLevel(parentCategoryItem, 1);
				}
			}

			em.close();

		}

		if (savedNewCategory != null) {

			// reveal new tag in viewer
			_tagViewer.reveal(newCategoryItem);

			_isModified = true;
		}

		setFocusToViewer();
	}

	/**
	 * <pre>
	 * 
	 * category	--- category
	 * category	--- tag
	 * 			+-- tag
	 * category	--- category
	 * 			+-- category --- tag
	 * 						 +-- tag
	 * 			+-- tag
	 * 			+-- tag
	 * 			+-- tag
	 * tag
	 * tag
	 * </pre>
	 */
	private void onNewTag() {

		final InputDialog inputDialog = new InputDialog(
				getShell(),
				Messages.pref_tourtag_dlg_new_tag_title,
				Messages.pref_tourtag_dlg_new_tag_message,
				UI.EMPTY_STRING,
				null);

		if (inputDialog.open() != Window.OK) {
			setFocusToViewer();
			return;
		}

		TourTag savedTag = null;

		// create new tour tag + item
		final TourTag tourTag = new TourTag(inputDialog.getValue().trim());
		final TVIPrefTag tagItem = new TVIPrefTag(_tagViewer, tourTag);

		final Object parentItem = ((StructuredSelection) _tagViewer.getSelection()).getFirstElement();
		if (parentItem == null) {

			// a parent is not selected, this will be a root tag

			tourTag.setRoot(true);

			/*
			 * update model
			 */
			tagItem.setParentItem(_rootItem);
			_rootItem.getFetchedChildren().add(tagItem);

			// persist tag
			savedTag = TourDatabase.saveEntity(tourTag, TourDatabase.ENTITY_IS_NOT_SAVED, TourTag.class);

			if (savedTag != null) {

				// update item
				tagItem.setTourTag(savedTag);

				/*
				 * update viewer
				 */
				_tagViewer.add(this, tagItem);
			}

		} else if (parentItem instanceof TVIPrefTagCategory) {

			// parent is a category

			final TVIPrefTagCategory parentCategoryItem = (TVIPrefTagCategory) parentItem;
			TourTagCategory parentTagCategory = parentCategoryItem.getTourTagCategory();

			/*
			 * update model
			 */

			// set parent into tag
			tagItem.setParentItem(parentCategoryItem);

			/*
			 * persist tag without new category otherwise an exception "detached entity passed to
			 * persist: net.tourbook.data.TourTagCategory" is raised
			 */
			savedTag = TourDatabase.saveEntity(tourTag, TourDatabase.ENTITY_IS_NOT_SAVED, TourTag.class);
			if (savedTag != null) {

				// update item
				tagItem.setTourTag(savedTag);

				// update parent category
				final EntityManager em = TourDatabase.getInstance().getEntityManager();
				{

					final TourTagCategory parentTagCategoryEntity = em.find(
							TourTagCategory.class,
							parentTagCategory.getCategoryId());

					// set new entity
					parentTagCategory = parentTagCategoryEntity;
					parentCategoryItem.setTourTagCategory(parentTagCategoryEntity);

					// set tag into parent category
					final Set<TourTag> lazyTourTags = parentTagCategoryEntity.getTourTags();
					lazyTourTags.add(tourTag);

					parentTagCategory.setTagCounter(lazyTourTags.size());
				}
				em.close();

				// persist parent category
				final TourTagCategory savedParent = TourDatabase.saveEntity(
						parentTagCategory,
						parentTagCategory.getCategoryId(),
						TourTagCategory.class);

				if (savedParent != null) {

					// update item
					parentCategoryItem.setTourTagCategory(savedParent);

					// set category in tag,
// this seems to be not necessary
//					tourTag.setTagCategory(parentTagCategory);

					// persist tag with category
					savedTag = TourDatabase.saveEntity(tourTag, tourTag.getTagId(), TourTag.class);

				}

			}

			if (savedTag != null) {

				// clear tour tag list
				TourDatabase.clearTourTags();

				/*
				 * update viewer
				 */
				parentCategoryItem.clearChildren();

				_tagViewer.add(parentCategoryItem, tagItem);
				_tagViewer.update(parentCategoryItem, null);

				_tagViewer.expandToLevel(parentCategoryItem, 1);
			}
		}

		if (savedTag != null) {

			// show new tag in viewer
			_tagViewer.reveal(tagItem);

			_isModified = true;
		}

		setFocusToViewer();
	}

	/**
	 * Rename selected tag/category
	 */
	private void onRenameTourTag() {

		final Object selection = ((StructuredSelection) _tagViewer.getSelection()).getFirstElement();

		String name = UI.EMPTY_STRING;
		String dlgTitle = UI.EMPTY_STRING;
		String dlgMessage = UI.EMPTY_STRING;

		if (selection instanceof TVIPrefTag) {
			dlgTitle = Messages.pref_tourtag_dlg_rename_title;
			dlgMessage = Messages.pref_tourtag_dlg_rename_message;
			name = ((TVIPrefTag) selection).getTourTag().getTagName();
		} else if (selection instanceof TVIPrefTagCategory) {
			dlgTitle = Messages.pref_tourtag_dlg_rename_title_category;
			dlgMessage = Messages.pref_tourtag_dlg_rename_message_category;
			name = ((TVIPrefTagCategory) selection).getTourTagCategory().getCategoryName();
		}

		final InputDialog inputDialog = new InputDialog(getShell(), dlgTitle, dlgMessage, name, null);

		if (inputDialog.open() != Window.OK) {

			setFocusToViewer();
			return;
		}

		// save changed name

		name = inputDialog.getValue().trim();

		if (selection instanceof TVIPrefTag) {

			// save tag

			final TVIPrefTag tourTagItem = ((TVIPrefTag) selection);
			final TourTag tourTag = tourTagItem.getTourTag();

			tourTag.setTagName(name);

			// persist tag
			TourDatabase.saveEntity(tourTag, tourTag.getTagId(), TourTag.class);

			_tagViewer.update(tourTagItem, new String[] { SORT_PROPERTY });

		} else if (selection instanceof TVIPrefTagCategory) {

			// save category

			final TVIPrefTagCategory tourCategoryItem = ((TVIPrefTagCategory) selection);
			final TourTagCategory tourCategory = tourCategoryItem.getTourTagCategory();

			tourCategory.setName(name);

			// persist category
			TourDatabase.saveEntity(tourCategory, tourCategory.getCategoryId(), TourTagCategory.class);

			_tagViewer.update(tourCategoryItem, new String[] { SORT_PROPERTY });

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
			 * remove join table tag->category
			 */
			sb.append("DELETE FROM "); //$NON-NLS-1$
			sb.append(TourDatabase.JOINTABLE__TOURTAGCATEGORY_TOURTAG);
			int result = conn.createStatement().executeUpdate(sb.toString());
			System.out.println(
					"Deleted " //$NON-NLS-1$
							+ result
							+ " entries from " //$NON-NLS-1$
							+ TourDatabase.JOINTABLE__TOURTAGCATEGORY_TOURTAG);

			/*
			 * remove jointable category<->category
			 */
			sb.setLength(0);
			sb.append("DELETE FROM "); //$NON-NLS-1$
			sb.append(TourDatabase.JOINTABLE__TOURTAGCATEGORY_TOURTAGCATEGORY);
			result = conn.createStatement().executeUpdate(sb.toString());
			System.out.println(
					"Deleted " //$NON-NLS-1$
							+ result
							+ " entries from " //$NON-NLS-1$
							+ TourDatabase.JOINTABLE__TOURTAGCATEGORY_TOURTAGCATEGORY);

			/*
			 * set tags to root
			 */
			sb.setLength(0);
			sb.append("UPDATE "); //$NON-NLS-1$
			sb.append(TourDatabase.TABLE_TOUR_TAG);
			sb.append(" SET isRoot=1"); //$NON-NLS-1$
			result = conn.createStatement().executeUpdate(sb.toString());
			System.out.println("Set " + result + " tour tags to root"); //$NON-NLS-1$ //$NON-NLS-2$

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

			// update the tag viewer
			_rootItem = new TVIPrefTagRoot(_tagViewer, true);
			updateTagViewer();

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

	@Override
	public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {
		return null;
	}

	@Override
	public void reloadViewer() {}

	private void setFocusToViewer() {

		// set focus back to the tree
		_tagViewer.getTree().setFocus();
	}

	public void setIsModified() {
		_isModified = true;
	}

	@Override
	public void updateColumnHeader(final ColumnDefinition colDef) {
		// TODO Auto-generated method stub

	}

	private void updateTagViewer() {

		// show contents in the viewers
		_tagViewer.setInput(this);

		enableButtons();
	}

}
