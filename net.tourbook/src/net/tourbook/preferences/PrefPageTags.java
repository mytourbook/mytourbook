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
package net.tourbook.preferences;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

import javax.persistence.EntityManager;

import net.tourbook.Messages;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourTagCategory;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.tag.TVIPrefTag;
import net.tourbook.tag.TVIPrefTagCategory;
import net.tourbook.tag.TVIPrefTagRoot;
import net.tourbook.tag.TagManager;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TourProperty;
import net.tourbook.ui.ColumnManager;
import net.tourbook.ui.ITourViewer;
import net.tourbook.ui.TreeViewerItem;
import net.tourbook.ui.UI;
import net.tourbook.ui.action.ActionCollapseAll;
import net.tourbook.ui.action.ActionExpandSelection;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.LocalSelectionTransfer;
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
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageTags extends PreferencePage implements IWorkbenchPreferencePage, ITourViewer {

	private static final String		SORT_PROPERTY	= "sort";				//$NON-NLS-1$

	private TreeViewer				fTagViewer;
	private ToolBar					fToolBar;

	private Button					fBtnNewTag;
	private Button					fBtnNewTagCategory;
	private Button					fBtnRename;
	private Button					fBtnReset;

	private TVIPrefTagRoot			fRootItem;

	private Image					fImgTag			= TourbookPlugin.getImageDescriptor(Messages.Image__tag)
															.createImage();
	private Image					fImgTagRoot		= TourbookPlugin.getImageDescriptor(Messages.Image__tag_root)
															.createImage();
	private Image					fImgTagCategory	= TourbookPlugin.getImageDescriptor(Messages.Image__tag_category)
															.createImage();

	private boolean					fIsModified		= false;

	private long					fDragStartTime;

	private IPropertyChangeListener	fPrefChangeListener;

	/**
	 * Sort the tags and categories
	 */
	private final class TagViewerComparator extends ViewerComparator {
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

		public TreeViewerItem getRootItem() {
			return fRootItem;
		}

		public boolean hasChildren(final Object element) {
			return ((TreeViewerItem) element).hasChildren();
		}

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

		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(final Preferences.PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

					fTagViewer.getTree()
							.setLinesVisible(getPreferenceStore().getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

					fTagViewer.refresh();

					/*
					 * the tree must be redrawn because the styled text does not display the new
					 * color
					 */
					fTagViewer.getTree().redraw();
				}
			}
		};

		// register the listener
		TourbookPlugin.getDefault().getPluginPreferences().addPropertyChangeListener(fPrefChangeListener);
	}

	private void createButtons(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.indent(5, 0)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().applyTo(container);

		// button: new tag
		fBtnNewTag = new Button(container, SWT.NONE);
		fBtnNewTag.setText(Messages.pref_tourtag_btn_new_tag);
		setButtonLayoutData(fBtnNewTag);
		fBtnNewTag.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onNewTag();
			}
		});

		// button: new tag category
		fBtnNewTagCategory = new Button(container, SWT.NONE);
		fBtnNewTagCategory.setText(Messages.pref_tourtag_btn_new_tag_category);
		setButtonLayoutData(fBtnNewTagCategory);
		fBtnNewTagCategory.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onNewCategory();
			}
		});

		// button: rename
		fBtnRename = new Button(container, SWT.NONE);
		fBtnRename.setText(Messages.pref_tourtag_btn_rename);
		setButtonLayoutData(fBtnRename);
		fBtnRename.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onRenameTourTag();
			}
		});

		// button: reset
		fBtnReset = new Button(container, SWT.NONE);
		fBtnReset.setText(Messages.pref_tourtag_btn_reset);
		setButtonLayoutData(fBtnReset);
		final GridData gd = (GridData) fBtnReset.getLayoutData();
		gd.verticalIndent = 50;

		fBtnReset.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onReset();
			}
		});

	}

	@Override
	protected Control createContents(final Composite parent) {

		final Composite viewerContainer = createUI(parent);

		// set root item
		fRootItem = new TVIPrefTagRoot(fTagViewer);

		updateTagViewer();
		enableButtons();
		addPrefListener();

		return viewerContainer;
	}

	private void createTagViewer(final Composite parent) {

		/*
		 * create tree layout
		 */

		final Composite layoutContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.hint(200, 100)
				.span(2, 1)
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

		fTagViewer = new TreeViewer(tree);
		fTagViewer.setContentProvider(new TagViewerContentProvicer());
		fTagViewer.setComparator(new TagViewerComparator());
		fTagViewer.setUseHashlookup(true);

		fTagViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(final DoubleClickEvent event) {

				final Object selection = ((IStructuredSelection) fTagViewer.getSelection()).getFirstElement();

				if (selection instanceof TVIPrefTag) {

					// tag is selected

					onRenameTourTag();

				} else if (selection instanceof TVIPrefTagCategory) {

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

		fTagViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				enableButtons();
			}
		});

		fTagViewer.addDragSupport(DND.DROP_MOVE,
				new Transfer[] { LocalSelectionTransfer.getTransfer() },
				new DragSourceListener() {

					public void dragFinished(final DragSourceEvent event) {

						final LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();

						if (event.doit == false) {
							return;
						}

						transfer.setSelection(null);
						transfer.setSelectionSetTime(0);
					}

					public void dragSetData(final DragSourceEvent event) {
					// data are set in LocalSelectionTransfer
					}

					public void dragStart(final DragSourceEvent event) {

						final LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
						final ISelection selection = fTagViewer.getSelection();

//						System.out.println("dragStart");
						transfer.setSelection(selection);
						transfer.setSelectionSetTime(fDragStartTime = event.time & 0xFFFFFFFFL);

						event.doit = !selection.isEmpty();
					}
				});

		fTagViewer.addDropSupport(DND.DROP_MOVE,
				new Transfer[] { LocalSelectionTransfer.getTransfer() },
				new TagDropAdapter(this, fTagViewer));

		/*
		 * create columns
		 */
		TreeViewerColumn tvc;
		TreeColumn tvcColumn;

		// column: tags + tag categories
		tvc = new TreeViewerColumn(fTagViewer, SWT.TRAIL);
		tvcColumn = tvc.getColumn();
		tvc.setLabelProvider(new StyledCellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final StyledString styledString = new StyledString();

				final Object element = cell.getElement();
				if (element instanceof TVIPrefTag) {

					final TourTag tourTag = ((TVIPrefTag) element).getTourTag();

					styledString.append(tourTag.getTagName(), UI.TAG_STYLER);
					cell.setImage(tourTag.isRoot() ? fImgTagRoot : fImgTag);

				} else if (element instanceof TVIPrefTagCategory) {

					final TVIPrefTagCategory tourTagCategoryItem = (TVIPrefTagCategory) element;
					final TourTagCategory tourTagCategory = tourTagCategoryItem.getTourTagCategory();

					cell.setImage(fImgTagCategory);

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

	/**
	 * set the toolbar action after the {@link #fTagViewer} is created
	 */
	private void createToolbarActions() {

		final ToolBarManager tbm = new ToolBarManager(fToolBar);

		tbm.add(new ActionExpandSelection(this));
		tbm.add(new ActionCollapseAll(this));

		tbm.update(true);
	}

	private Composite createUI(final Composite parent) {

		// container
		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.margins(0, 0)
				.spacing(SWT.DEFAULT, 0)
				.numColumns(3)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));

		Label label = new Label(container, SWT.WRAP);
		label.setText(Messages.pref_tourtag_viewer_title);
		GridDataFactory.swtDefaults().grab(true, false).applyTo(label);

		// toolbar
		fToolBar = new ToolBar(container, SWT.FLAT);

		// spacer
		new Label(container, SWT.NONE);

		createTagViewer(container);
		createButtons(container);

		label = new Label(container, SWT.WRAP);
		label.setText(Messages.pref_tourtag_hint);
		GridDataFactory.swtDefaults().grab(true, false).span(3, 1).applyTo(label);

		// spacer
		new Label(container, SWT.NONE);

		createToolbarActions();

		return container;
	}

	@Override
	public void dispose() {

		fImgTag.dispose();
		fImgTagRoot.dispose();
		fImgTagCategory.dispose();

		TourbookPlugin.getDefault().getPluginPreferences().removePropertyChangeListener(fPrefChangeListener);

		super.dispose();
	}

	private void enableButtons() {

		final IStructuredSelection selectedTags = (IStructuredSelection) fTagViewer.getSelection();
		final Object selection = selectedTags.getFirstElement();

		boolean isTourTag = false;
		boolean isTagCategory = false;
		final boolean isSelection = selection != null;

		if (selection instanceof TVIPrefTag) {
			isTourTag = true;
		} else if (selection instanceof TVIPrefTagCategory) {
			isTagCategory = true;
		}

		fBtnNewTag.setEnabled(isSelection == false || isTagCategory == true && isTourTag == false);
		fBtnNewTagCategory.setEnabled(isSelection == false || isTagCategory == true && isTourTag == false);
		fBtnRename.setEnabled(selectedTags.size() == 1);
	}

	private void fireModifyEvent() {

		if (fIsModified) {
			fIsModified = false;

			// remove old tags from internal list
			TourDatabase.clearTourTags();

			TagManager.updateTagNames();

			TourManager.getInstance().clearTourDataCache();

			// fire modify event
			TourManager.firePropertyChange(TourProperty.TAG_STRUCTURE_CHANGED, null);
		}
	}

	public ColumnManager getColumnManager() {
		return null;
	}

	public long getDragStartTime() {
		return fDragStartTime;
	}

	public TVIPrefTagRoot getRootItem() {
		return fRootItem;
	}

	public ColumnViewer getViewer() {
		return fTagViewer;
	}

	public void init(final IWorkbench workbench) {
		setPreferenceStore(TourbookPlugin.getDefault().getPreferenceStore());
	}

	@Override
	public boolean isValid() {

//		saveFilterList();

		return true;
	}

	private void onNewCategory() {

		final InputDialog inputDialog = new InputDialog(getShell(),
				Messages.pref_tourtag_dlg_new_tag_category_title,
				Messages.pref_tourtag_dlg_new_tag_category_message,
				UI.EMPTY_STRING,
				null);

		inputDialog.open();

		if (inputDialog.getReturnCode() != Window.OK) {
			return;
		}

		// create tag category + tree item
		final TourTagCategory newCategory = new TourTagCategory(inputDialog.getValue().trim());
		final TVIPrefTagCategory newCategoryItem = new TVIPrefTagCategory(fTagViewer, newCategory);

		final Object parentElement = ((StructuredSelection) fTagViewer.getSelection()).getFirstElement();
		TourTagCategory savedNewCategory = null;

		if (parentElement == null) {

			// a parent is not selected, this will be a root category

			newCategory.setRoot(true);

			/*
			 * update model
			 */

			fRootItem.getFetchedChildren().add(newCategoryItem);

			// persist new category
			savedNewCategory = TourDatabase.saveEntity(newCategory, newCategory.getCategoryId(), TourTagCategory.class);
			if (savedNewCategory != null) {

				// update item
				newCategoryItem.setTourTagCategory(savedNewCategory);

				// update viewer
				fTagViewer.add(this, newCategoryItem);
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
				final TourTagCategory parentCategoryEntity = em.find(TourTagCategory.class,
						parentCategory.getCategoryId());

				// set tag in parent category
				final Set<TourTagCategory> lazyTourTagCategories = parentCategoryEntity.getTagCategories();
				lazyTourTagCategories.add(savedNewCategory);

				// update number of categories
				parentCategoryEntity.setCategoryCounter(lazyTourTagCategories.size());

				/*
				 * persist parent category
				 */
				final TourTagCategory savedParentCategory = TourDatabase.saveEntity(parentCategoryEntity,
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

					fTagViewer.add(parentCategoryItem, newCategoryItem);

					fTagViewer.expandToLevel(parentCategoryItem, 1);
				}
			}

			em.close();

		}

		if (savedNewCategory != null) {

			// reveal new tag in viewer
			fTagViewer.reveal(newCategoryItem);

			fIsModified = true;
		}
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
	 * 
	 * </pre>
	 */
	private void onNewTag() {

		final InputDialog inputDialog = new InputDialog(getShell(),
				Messages.pref_tourtag_dlg_new_tag_title,
				Messages.pref_tourtag_dlg_new_tag_message,
				UI.EMPTY_STRING,
				null);

		inputDialog.open();

		if (inputDialog.getReturnCode() != Window.OK) {
			return;
		}

		TourTag savedTag = null;

		// create new tour tag + item
		final TourTag tourTag = new TourTag(inputDialog.getValue().trim());
		final TVIPrefTag tagItem = new TVIPrefTag(fTagViewer, tourTag);

		final Object parentItem = ((StructuredSelection) fTagViewer.getSelection()).getFirstElement();
		if (parentItem == null) {

			// a parent is not selected, this will be a root tag

			tourTag.setRoot(true);

			/*
			 * update model
			 */
			tagItem.setParentItem(fRootItem);
			fRootItem.getFetchedChildren().add(tagItem);

			// persist tag
			savedTag = TourDatabase.saveEntity(tourTag, TourDatabase.ENTITY_IS_NOT_SAVED, TourTag.class);

			if (savedTag != null) {

				// update item
				tagItem.setTourTag(savedTag);

				/*
				 * update viewer
				 */
				fTagViewer.add(this, tagItem);
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

					final TourTagCategory parentTagCategoryEntity = em.find(TourTagCategory.class,
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
				final TourTagCategory savedParent = TourDatabase.saveEntity(parentTagCategory,
						parentTagCategory.getCategoryId(),
						TourTagCategory.class);

				if (savedParent != null) {

					// update item
					parentCategoryItem.setTourTagCategory(savedParent);

					// set category in tag
					tourTag.getTagCategories().add(parentTagCategory);

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

				fTagViewer.add(parentCategoryItem, tagItem);
				fTagViewer.update(parentCategoryItem, null);

				fTagViewer.expandToLevel(parentCategoryItem, 1);
			}
		}

		if (savedTag != null) {

			// show new tag in viewer
			fTagViewer.reveal(tagItem);

			fIsModified = true;
		}
	}

	/**
	 * Rename selected tag/category
	 */
	private void onRenameTourTag() {

		final Object selection = ((StructuredSelection) fTagViewer.getSelection()).getFirstElement();

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

		inputDialog.open();

		if (inputDialog.getReturnCode() != Window.OK) {
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

			fTagViewer.update(tourTagItem, new String[] { SORT_PROPERTY });

		} else if (selection instanceof TVIPrefTagCategory) {

			// save category

			final TVIPrefTagCategory tourCategoryItem = ((TVIPrefTagCategory) selection);
			final TourTagCategory tourCategory = tourCategoryItem.getTourTagCategory();

			tourCategory.setName(name);

			// persist category
			TourDatabase.saveEntity(tourCategory, tourCategory.getCategoryId(), TourTagCategory.class);

			fTagViewer.update(tourCategoryItem, new String[] { SORT_PROPERTY });

		}

		fIsModified = true;
	}

	private void onReset() {

		final MessageDialog dialog = new MessageDialog(Display.getCurrent().getActiveShell(),
				Messages.pref_tourtag_dlg_reset_title,
				null,
				Messages.pref_tourtag_dlg_reset_message,
				MessageDialog.QUESTION,
				new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL },
				1);

		if ((dialog.open()) == Window.OK) {

			try {

				System.out.println("RESET TAG STRUCTURE"); //$NON-NLS-1$

				final StringBuilder sb = new StringBuilder();
				final Connection conn = TourDatabase.getInstance().getConnection();

				/*
				 * remove join table tag->category
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
				fRootItem = new TVIPrefTagRoot(fTagViewer);
				updateTagViewer();

				fIsModified = true;

			} catch (final SQLException e) {
				UI.showSQLException(e);
			}
		}
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

	public void setIsModified() {
		fIsModified = true;
	}

	private void updateTagViewer() {

		// show contents in the viewers
		fTagViewer.setInput(this);

		enableButtons();
	}

}
