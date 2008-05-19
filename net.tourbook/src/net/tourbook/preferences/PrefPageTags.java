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

import java.util.Set;

import javax.persistence.EntityManager;

import net.tourbook.Messages;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourTagCategory;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.tag.TVIRootItem;
import net.tourbook.tag.TVITourTag;
import net.tourbook.tag.TVITourTagCategory;
import net.tourbook.tour.TreeViewerItem;
import net.tourbook.ui.UI;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageTags extends PreferencePage implements IWorkbenchPreferencePage {

	private TreeViewer	fTagViewer;

	private Button		fBtnNewTag;
	private Button		fBtnRename;

	private TVIRootItem	fRootItem;

	private Image		fImgFolderClosed	= TourbookPlugin.getImageDescriptor(Messages.Image__folder_closed)
													.createImage();
	private Image		fImgFolderOpened	= TourbookPlugin.getImageDescriptor(Messages.Image__folder_opened)
													.createImage();

	class TagViewerContentProvicer implements ITreeContentProvider {

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

	/**
	 * Sort the tags and categories
	 */
	private class TagViewerSorter extends ViewerSorter {

		@Override
		public int compare(final Viewer viewer, final Object obj1, final Object obj2) {

			if (obj1 instanceof TVITourTag && obj2 instanceof TVITourTag) {

				final TourTag tourTag1 = ((TVITourTag) (obj1)).getTourTag();
				final TourTag tourTag2 = ((TVITourTag) (obj2)).getTourTag();

				return tourTag1.getTagName().compareTo(tourTag2.getTagName());

			} else if (obj1 instanceof TVITourTag && obj2 instanceof TVITourTagCategory) {

				return 1;

			} else if (obj1 instanceof TVITourTagCategory && obj2 instanceof TVITourTagCategory) {

				final TourTagCategory tourTagCat1 = ((TVITourTagCategory) (obj1)).getTourTagCategory();
				final TourTagCategory tourTagCat2 = ((TVITourTagCategory) (obj2)).getTourTagCategory();

				return tourTagCat1.getCategoryName().compareTo(tourTagCat2.getCategoryName());
			}

			return 0;
		}
	}

	public PrefPageTags() {}

	public PrefPageTags(final String title) {
		super(title);
	}

	public PrefPageTags(final String title, final ImageDescriptor image) {
		super(title, image);
	}

	private void createButtons(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
		final GridLayout gl = new GridLayout();
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		container.setLayout(gl);

		// button: new tag
		fBtnNewTag = new Button(container, SWT.NONE);
		fBtnNewTag.setText(Messages.pref_tourtag_button_new_tag);
		setButtonLayoutData(fBtnNewTag);
		fBtnNewTag.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onNewTag();
			}
		});

		// button: new tag category
		fBtnNewTag = new Button(container, SWT.NONE);
		fBtnNewTag.setText(Messages.pref_tourtag_btn_new_tag_category);
		setButtonLayoutData(fBtnNewTag);
		fBtnNewTag.addSelectionListener(new SelectionAdapter() {
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

	}

	@Override
	protected Control createContents(final Composite parent) {

		final Composite viewerContainer = createUI(parent);

		// set root item
		fRootItem = new TVIRootItem();

		updateViewers();

		return viewerContainer;
	}

	private void createTagViewer(final Composite parent) {

		/*
		 * create tree layout
		 */

		final Composite layoutContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(layoutContainer);

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
		// tree.setLinesVisible(true);

		fTagViewer = new TreeViewer(tree);
		fTagViewer.setContentProvider(new TagViewerContentProvicer());
		fTagViewer.setSorter(new TagViewerSorter());
		fTagViewer.setUseHashlookup(true);

		fTagViewer.addDoubleClickListener(new IDoubleClickListener() {

			public void doubleClick(final DoubleClickEvent event) {

				final Object selection = ((IStructuredSelection) fTagViewer.getSelection()).getFirstElement();

				if (selection instanceof TVITourTag) {

					// tag is selected

				} else if (selection instanceof TVITourTagCategory) {

					// expand/collapse current item

					final TreeViewerItem tourItem = (TVITourTagCategory) selection;

					if (fTagViewer.getExpandedState(tourItem)) {
						fTagViewer.collapseToLevel(tourItem, 1);
					} else {
						fTagViewer.expandToLevel(tourItem, 1);
					}
				}
			}
		});

		/*
		 * create columns
		 */
		TreeViewerColumn tvc;
		TreeColumn tvcColumn;

		// column: tags + tag categories
		tvc = new TreeViewerColumn(fTagViewer, SWT.TRAIL);
		tvcColumn = tvc.getColumn();
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				if (element instanceof TVITourTag) {

					cell.setText(((TVITourTag) element).getTourTag().getTagName()); //$NON-NLS-1$

				} else if (element instanceof TVITourTagCategory) {

					final TVITourTagCategory tourTagCategoryItem = (TVITourTagCategory) element;
					final TourTagCategory tourTagCategory = tourTagCategoryItem.getTourTagCategory();

					cell.setText(tourTagCategory.getCategoryName()); //$NON-NLS-1$

					cell.setImage(fTagViewer.getExpandedState(tourTagCategoryItem)
							? fImgFolderOpened
							: fImgFolderClosed);
				}
			}
		});
		treeLayout.setColumnData(tvcColumn, new ColumnWeightData(100, true));

	}

	private Composite createUI(final Composite parent) {

		final Label label = new Label(parent, SWT.WRAP);
		label.setText(Messages.pref_tourtag_viewer_title);
		label.setLayoutData(new GridData(SWT.NONE, SWT.NONE, true, false));

		// container
		final Composite viewerContainer = new Composite(parent, SWT.NONE);
		final GridLayout gl = new GridLayout(3, false);
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		viewerContainer.setLayout(gl);
		viewerContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		createTagViewer(viewerContainer);
		createButtons(viewerContainer);

		// spacer
		new Label(parent, SWT.WRAP);

		return viewerContainer;
	}

	@Override
	public void dispose() {

		fImgFolderOpened.dispose();
		fImgFolderClosed.dispose();

		super.dispose();
	}

	private void enableButtons() {

		final IStructuredSelection selection = (IStructuredSelection) fTagViewer.getSelection();

		final TourTag tourTag = (TourTag) selection.getFirstElement();

		fBtnRename.setEnabled(tourTag != null);
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

		// create tour tag category + tree item
		final TourTagCategory newTourTagCategory = new TourTagCategory(inputDialog.getValue().trim());
		final TVITourTagCategory newCategoryItem = new TVITourTagCategory(newTourTagCategory);

		boolean isSaved = false;

		final Object parentElement = ((StructuredSelection) fTagViewer.getSelection()).getFirstElement();
		if (parentElement == null) {

			// a parent is not selected, this will be a root category

			newTourTagCategory.setRoot(true);

			/*
			 * update model
			 */

			fRootItem.getFetchedChildren().add(newCategoryItem);

			// persist new category
			isSaved = TourDatabase.persistEntity(newTourTagCategory,
					newTourTagCategory.getCategoryId(),
					TourTagCategory.class);

			// update viewer
			fTagViewer.add(this, newCategoryItem);

		} else if (parentElement instanceof TVITourTagCategory) {

			// parent is a category

			final TVITourTagCategory parentCategoryItem = (TVITourTagCategory) parentElement;
			TourTagCategory parentTourTagCategory = parentCategoryItem.getTourTagCategory();

			/*
			 * update model
			 */

			final EntityManager em = TourDatabase.getInstance().getEntityManager();

			// persist new category
			isSaved = TourDatabase.persistEntity(newTourTagCategory,
					newTourTagCategory.getCategoryId(),
					TourTagCategory.class);

			if (isSaved) {

				// update parent category
				{
					final TourTagCategory parentTourTagCategoryEntity = em.find(TourTagCategory.class,
							parentTourTagCategory.getCategoryId());

					// set new entity
					parentTourTagCategory = parentTourTagCategoryEntity;
					parentCategoryItem.setTourTagCategory(parentTourTagCategoryEntity);

					// set tag in parent category
					final Set<TourTagCategory> lazyTourTagCategories = parentTourTagCategoryEntity.getTagCategories();
					lazyTourTagCategories.add(newTourTagCategory);
				}

				// persist parent category
				isSaved = TourDatabase.persistEntity(parentTourTagCategory,
						parentTourTagCategory.getCategoryId(),
						TourTagCategory.class);

				// update viewer
				fTagViewer.add(parentCategoryItem, newCategoryItem);
			}

			em.close();

		}

		if (isSaved) {

			// reveal new tag in viewer
			fTagViewer.reveal(newCategoryItem);

		} else {
			MessageDialog.openError(getShell(), "Error", "Error occured when saving new tag/category");//$NON-NLS-1$ //$NON-NLS-2$
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

		boolean isSaved = false;

		// create new tour tag + item
		final TourTag tourTag = new TourTag(inputDialog.getValue().trim());
		final TVITourTag tourTagItem = new TVITourTag(tourTag);

		final Object parentElement = ((StructuredSelection) fTagViewer.getSelection()).getFirstElement();
		if (parentElement == null) {

			// a parent is not selected, this will be a root tag

			tourTag.setRoot(true);

			/*
			 * update model
			 */
			fRootItem.getFetchedChildren().add(tourTagItem);

			// persist tag
			isSaved = TourDatabase.persistEntity(tourTag, tourTag.getTagId(), TourTag.class);

			if (isSaved) {

				/*
				 * update viewer
				 */
				fTagViewer.add(this, tourTagItem);
			}

		} else if (parentElement instanceof TVITourTagCategory) {

			// parent is a category

			final TVITourTagCategory parentCategoryItem = (TVITourTagCategory) parentElement;
			TourTagCategory parentTourTagCategory = parentCategoryItem.getTourTagCategory();

			/*
			 * update model
			 */

			/*
			 * persist tag without new category otherwise an exception "detached entity passed to
			 * persist: net.tourbook.data.TourTagCategory" is raised
			 */
			isSaved = TourDatabase.persistEntity(tourTag, tourTag.getTagId(), TourTag.class);
			if (isSaved) {

				// update parent category
				final EntityManager em = TourDatabase.getInstance().getEntityManager();
				{

					final TourTagCategory parentTourTagCategoryEntity = em.find(TourTagCategory.class,
							parentTourTagCategory.getCategoryId());

					// set new entity
					parentTourTagCategory = parentTourTagCategoryEntity;
					parentCategoryItem.setTourTagCategory(parentTourTagCategoryEntity);

					// set tag in parent category
					final Set<TourTag> lazyTourTags = parentTourTagCategoryEntity.getTourTags();
					lazyTourTags.add(tourTag);

				}
				em.close();

				// persist parent category
				isSaved = TourDatabase.persistEntity(parentTourTagCategory,
						parentTourTagCategory.getCategoryId(),
						TourTagCategory.class);

				if (isSaved) {

					// set category in tag
					tourTag.getTagCategories().add(parentTourTagCategory);

					// persist tag with category
					isSaved = TourDatabase.persistEntity(tourTag, tourTag.getTagId(), TourTag.class);
				}
			}

			if (isSaved) {

				// update list which contains all tour tags
				TourDatabase.getTourTags().add(tourTag);

				/*
				 * update viewer
				 */
				fTagViewer.add(parentCategoryItem, tourTagItem);
			}

		} else if (parentElement instanceof TVITourTag) {

			// parent is a tag

//			final TVITourTag tviTourTag = (TVITourTag) parentElement;

		}

		if (isSaved) {

			// show new tag in viewer
			fTagViewer.reveal(tourTagItem);

			// fTagViewer.setSelection(new StructuredSelection(tourTagItem), true);

		} else {
			MessageDialog.openError(getShell(), "Error", "Error occured when saving new tag"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * Rename selected tag
	 */
	private void onRenameTourTag() {

		final TourTag tourTag = (TourTag) ((StructuredSelection) fTagViewer.getSelection()).getFirstElement();

		final InputDialog inputDialog = new InputDialog(getShell(),
				Messages.pref_tourtag_dlg_rename_title,
				Messages.pref_tourtag_dlg_rename_message,
				tourTag.getTagName(),
				null);

		inputDialog.open();

		if (inputDialog.getReturnCode() != Window.OK) {
			return;
		}

		// update model
		tourTag.setTagName(inputDialog.getValue().trim());

		// update entity in the db
		if (TourDatabase.persistEntity(tourTag, tourTag.getTagId(), TourTag.class)) {

			// update viewer
			fTagViewer.update(tourTag, null);

//			fIsModified = true;
		}
	}

	@Override
	public boolean performOk() {

//		saveFilterList();

		return true;
	}

	private void updateViewers() {

		// show contents in the viewers
		fTagViewer.setInput(this);

		enableButtons();
	}

}
