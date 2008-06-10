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
import net.tourbook.tag.TVIRootItem;
import net.tourbook.tag.TVITourTag;
import net.tourbook.tag.TVITourTagCategory;
import net.tourbook.tour.TreeViewerItem;
import net.tourbook.ui.ActionCollapseAll;
import net.tourbook.ui.ActionExpandAll;
import net.tourbook.ui.UI;

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

public class PrefPageTags extends PreferencePage implements IWorkbenchPreferencePage {

	private static final String	SORT_PROPERTY	= "sort";																//$NON-NLS-1$

	private TreeViewer			fTagViewer;
	private ToolBar				fToolBar;

	private Button				fBtnNewTag;
	private Button				fBtnNewTagCategory;
	private Button				fBtnRename;
	private Button				fBtnReset;

	private TVIRootItem			fRootItem;

	private Image				fImgTag			= TourbookPlugin.getImageDescriptor(Messages.Image__tag).createImage();
	private Image				fImgTagRoot		= TourbookPlugin.getImageDescriptor(Messages.Image__tag_root)
														.createImage();
	private Image				fImgTagCategory	= TourbookPlugin.getImageDescriptor(Messages.Image__tag_category)
														.createImage();

	private boolean				fIsModified		= false;

	private long				fDragStartTime;

	/**
	 * Sort the tags and categories
	 */
	private final class TagViewerComparator extends ViewerComparator {
		@Override
		public int compare(final Viewer viewer, final Object obj1, final Object obj2) {
			if (obj1 instanceof TVITourTag && obj2 instanceof TVITourTag) {

				// sort tags by name
				final TourTag tourTag1 = ((TVITourTag) (obj1)).getTourTag();
				final TourTag tourTag2 = ((TVITourTag) (obj2)).getTourTag();

				return tourTag1.getTagName().compareTo(tourTag2.getTagName());

			} else if (obj1 instanceof TVITourTag && obj2 instanceof TVITourTagCategory) {

				// sort category before tag
				return 1;

			} else if (obj2 instanceof TVITourTag && obj1 instanceof TVITourTagCategory) {

				// sort category before tag
				return -1;

			} else if (obj1 instanceof TVITourTagCategory && obj2 instanceof TVITourTagCategory) {

				// sort categories by name
				final TourTagCategory tourTagCat1 = ((TVITourTagCategory) (obj1)).getTourTagCategory();
				final TourTagCategory tourTagCat2 = ((TVITourTagCategory) (obj2)).getTourTagCategory();

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
		fRootItem = new TVIRootItem(fTagViewer);

		updateTagViewer();
		enableButtons();

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
		tree.setLinesVisible(true);

		fTagViewer = new TreeViewer(tree);
		fTagViewer.setContentProvider(new TagViewerContentProvicer());
		fTagViewer.setComparator(new TagViewerComparator());
		fTagViewer.setUseHashlookup(true);

		fTagViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(final DoubleClickEvent event) {

				final Object selection = ((IStructuredSelection) fTagViewer.getSelection()).getFirstElement();

				if (selection instanceof TVITourTag) {

					// tag is selected

					onRenameTourTag();

				} else if (selection instanceof TVITourTagCategory) {

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

		createTagViewerColumns(treeLayout);
	}

	private void createTagViewerColumns(final TreeColumnLayout treeLayout) {
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

				final Object element = cell.getElement();

				if (element instanceof TVITourTag) {

					final TourTag tourTag = ((TVITourTag) element).getTourTag();
					cell.setText(tourTag.getTagName()); //$NON-NLS-1$
					cell.setImage(tourTag.isRoot() ? fImgTagRoot : fImgTag);

				} else if (element instanceof TVITourTagCategory) {

					final TVITourTagCategory tourTagCategoryItem = (TVITourTagCategory) element;
					final TourTagCategory tourTagCategory = tourTagCategoryItem.getTourTagCategory();

					cell.setImage(fImgTagCategory);

					final StyledString styledString = new StyledString();

					styledString.append(tourTagCategory.getCategoryName());

					// get number of categories
					final int categoryCounter = tourTagCategory.getCategoryCounter();
					final int tagCounter = tourTagCategory.getTagCounter();
					if (categoryCounter == -1 && tagCounter == -1) {

//						styledString.append("  ...", StyledString.COUNTER_STYLER);

					} else {

						String categoryString = UI.EMPTY_STRING;
						if (categoryCounter > 0) {
							categoryString = "/" + categoryCounter;
						}
						styledString.append("   " + tagCounter + categoryString, StyledString.COUNTER_STYLER);
					}

					cell.setText(styledString.getString());
					cell.setStyleRanges(styledString.getStyleRanges());
				}
			}
		});
		treeLayout.setColumnData(tvcColumn, new ColumnWeightData(100, true));
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

		final Label label = new Label(container, SWT.WRAP);
		label.setText(Messages.pref_tourtag_viewer_title);
		GridDataFactory.swtDefaults()//
				.grab(true, false)
				.applyTo(label);

		// toolbar
		fToolBar = new ToolBar(container, SWT.FLAT);

		// spacer
		new Label(container, SWT.NONE);

		createTagViewer(container);
		createButtons(container);

		setToolbarActions();

		return container;
	}

	@Override
	public void dispose() {

		fImgTag.dispose();
		fImgTagRoot.dispose();
		fImgTagCategory.dispose();

		super.dispose();
	}

	private void enableButtons() {

		final IStructuredSelection selectedTags = (IStructuredSelection) fTagViewer.getSelection();
		final Object selection = selectedTags.getFirstElement();

		boolean isTourTag = false;
		boolean isTagCategory = false;
		final boolean isSelection = selection != null;

		if (selection instanceof TVITourTag) {
			isTourTag = true;
		} else if (selection instanceof TVITourTagCategory) {
			isTagCategory = true;
		}

		fBtnNewTag.setEnabled(isSelection == false || isTagCategory == true && isTourTag == false);
		fBtnNewTagCategory.setEnabled(isSelection == false || isTagCategory == true && isTourTag == false);
		fBtnRename.setEnabled(selectedTags.size() == 1);
	}

	public long getDragStartTime() {
		return fDragStartTime;
	}

	public TVIRootItem getRootItem() {
		return fRootItem;
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
		final TreeViewerItem newCategoryItem = new TVITourTagCategory(fTagViewer, newTourTagCategory);

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
			isSaved = TourDatabase.saveEntity(newTourTagCategory,
					newTourTagCategory.getCategoryId(),
					TourTagCategory.class);

			// update viewer
			fTagViewer.add(this, newCategoryItem);

		} else if (parentElement instanceof TVITourTagCategory) {

			// parent is a category

			final TVITourTagCategory parentCategoryItem = (TVITourTagCategory) parentElement;
			TourTagCategory parentTagCategory = parentCategoryItem.getTourTagCategory();

			/*
			 * update model
			 */

			final EntityManager em = TourDatabase.getInstance().getEntityManager();

			// persist new category
			isSaved = TourDatabase.saveEntity(newTourTagCategory,
					newTourTagCategory.getCategoryId(),
					TourTagCategory.class);

			if (isSaved) {

				// update parent category
				{
					final TourTagCategory parentTourTagCategoryEntity = em.find(TourTagCategory.class,
							parentTagCategory.getCategoryId());

					// set new entity
					parentTagCategory = parentTourTagCategoryEntity;
					parentCategoryItem.setTourTagCategory(parentTourTagCategoryEntity);

					// set tag in parent category
					final Set<TourTagCategory> lazyTourTagCategories = parentTourTagCategoryEntity.getTagCategories();
					lazyTourTagCategories.add(newTourTagCategory);

					// update number of categories
					parentTourTagCategoryEntity.setCategoryCounter(lazyTourTagCategories.size());
				}

				// persist parent category
				isSaved = TourDatabase.saveEntity(parentTagCategory,
						parentTagCategory.getCategoryId(),
						TourTagCategory.class);

				if (isSaved) {

					/*
					 * update viewer
					 */
					parentCategoryItem.resetChildren();

					fTagViewer.update(parentCategoryItem, null);
					fTagViewer.add(parentCategoryItem, newCategoryItem);

					fTagViewer.expandToLevel(parentCategoryItem, 1);
				}
			}

			em.close();

		}

		if (isSaved) {

			// reveal new tag in viewer
			fTagViewer.reveal(newCategoryItem);

			setIsModified(true);
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
		final TVITourTag tourTagItem = new TVITourTag(fTagViewer, tourTag);

		final Object parentElement = ((StructuredSelection) fTagViewer.getSelection()).getFirstElement();
		if (parentElement == null) {

			// a parent is not selected, this will be a root tag

			tourTag.setRoot(true);

			/*
			 * update model
			 */
			fRootItem.getFetchedChildren().add(tourTagItem);

			// persist tag
			isSaved = TourDatabase.saveEntity(tourTag, tourTag.getTagId(), TourTag.class);

			if (isSaved) {

				/*
				 * update viewer
				 */
				fTagViewer.add(this, tourTagItem);
			}

		} else if (parentElement instanceof TVITourTagCategory) {

			// parent is a category

			final TVITourTagCategory parentCategoryItem = (TVITourTagCategory) parentElement;
			TourTagCategory parentCategory = parentCategoryItem.getTourTagCategory();

			tourTagItem.setParentItem(parentCategoryItem);

			/*
			 * update model
			 */

			/*
			 * persist tag without new category otherwise an exception "detached entity passed to
			 * persist: net.tourbook.data.TourTagCategory" is raised
			 */
			isSaved = TourDatabase.saveEntity(tourTag, tourTag.getTagId(), TourTag.class);
			if (isSaved) {

				// update parent category
				final EntityManager em = TourDatabase.getInstance().getEntityManager();
				{

					final TourTagCategory parentTourTagCategoryEntity = em.find(TourTagCategory.class,
							parentCategory.getCategoryId());

					// set new entity
					parentCategory = parentTourTagCategoryEntity;
					parentCategoryItem.setTourTagCategory(parentTourTagCategoryEntity);

					// set tag in parent category
					final Set<TourTag> lazyTourTags = parentTourTagCategoryEntity.getTourTags();
					lazyTourTags.add(tourTag);

				}
				em.close();

				// persist parent category
				isSaved = TourDatabase.saveEntity(parentCategory, parentCategory.getCategoryId(), TourTagCategory.class);

				if (isSaved) {

					// set category in tag
					tourTag.getTagCategories().add(parentCategory);

					// persist tag with category
					isSaved = TourDatabase.saveEntity(tourTag, tourTag.getTagId(), TourTag.class);
				}

			}

			if (isSaved) {

				// update list which contains all tour tags
				TourDatabase.getTourTags().add(tourTag);

				/*
				 * update viewer
				 */
				parentCategoryItem.resetChildren();

				fTagViewer.add(parentCategoryItem, tourTagItem);

				fTagViewer.expandToLevel(parentCategoryItem, 1);
			}

		} else if (parentElement instanceof TVITourTag) {

			// parent is a tag

//			final TVITourTag tviTourTag = (TVITourTag) parentElement;

		}

		if (isSaved) {

			// show new tag in viewer
			fTagViewer.reveal(tourTagItem);

			setIsModified(true);
		}
	}

	/**
	 * Rename selected tag/category
	 */
	private void onRenameTourTag() {

		final Object selection = ((StructuredSelection) fTagViewer.getSelection()).getFirstElement();
		String name = UI.EMPTY_STRING;

		if (selection instanceof TVITourTag) {
			name = ((TVITourTag) selection).getTourTag().getTagName();
		} else if (selection instanceof TVITourTagCategory) {
			name = ((TVITourTagCategory) selection).getTourTagCategory().getCategoryName();
		}

		final InputDialog inputDialog = new InputDialog(getShell(),
				Messages.pref_tourtag_dlg_rename_title,
				Messages.pref_tourtag_dlg_rename_message,
				name,
				null);

		inputDialog.open();

		if (inputDialog.getReturnCode() != Window.OK) {
			return;
		}

		// save changed name

		name = inputDialog.getValue().trim();

		if (selection instanceof TVITourTag) {

			final TVITourTag tourTagItem = ((TVITourTag) selection);
			final TourTag tourTag = tourTagItem.getTourTag();

			tourTag.setTagName(name);

			// persist tag
			TourDatabase.saveEntity(tourTag, tourTag.getTagId(), TourTag.class);

			fTagViewer.update(tourTagItem, new String[] { SORT_PROPERTY });

		} else if (selection instanceof TVITourTagCategory) {

			final TVITourTagCategory tourCategoryItem = ((TVITourTagCategory) selection);
			final TourTagCategory tourCategory = tourCategoryItem.getTourTagCategory();

			tourCategory.setName(name);

			// persist category
			TourDatabase.saveEntity(tourCategory, tourCategory.getCategoryId(), TourTagCategory.class);

			fTagViewer.update(tourCategoryItem, new String[] { SORT_PROPERTY });

		}

		setIsModified(true);
	}

	private void onReset() {

		final MessageDialog dialog = new MessageDialog(Display.getCurrent().getActiveShell(),
				"Emergency Reset",
				null,
				"Are you sure to reset the structure of the tags?\n\n"
						+ "Reseting the structure will not delete the tags or categories,\n"
						+ "they will be set to a main tag or main category.",
				MessageDialog.QUESTION,
				new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL },
				1);

		if ((dialog.open()) == Window.OK) {

			try {

				System.out.println("RESET TAG STRUCTURE");

				final StringBuilder sb = new StringBuilder();
				final Connection conn = TourDatabase.getInstance().getConnection();

				/*
				 * remove join table tag->category
				 */
				sb.append("DELETE FROM ");
				sb.append(TourDatabase.JOINTABLE_TOURTAGCATEGORY_TOURTAG);
				int result = conn.createStatement().executeUpdate(sb.toString());
				System.out.println("Deleted "
						+ result
						+ " entries from "
						+ TourDatabase.JOINTABLE_TOURTAGCATEGORY_TOURTAG);

				/*
				 * remove jointable category<->category
				 */
				sb.setLength(0);
				sb.append("DELETE FROM ");
				sb.append(TourDatabase.JOINTABLE_TOURTAGCATEGORY_TOURTAGCATEGORY);
				result = conn.createStatement().executeUpdate(sb.toString());
				System.out.println("Deleted "
						+ result
						+ " entries from "
						+ TourDatabase.JOINTABLE_TOURTAGCATEGORY_TOURTAGCATEGORY);

				/*
				 * set tags to root
				 */
				sb.setLength(0);
				sb.append("UPDATE ");
				sb.append(TourDatabase.TABLE_TOUR_TAG);
				sb.append(" SET isRoot=1");
				result = conn.createStatement().executeUpdate(sb.toString());
				System.out.println("Set " + result + " tour tags to root");

				/*
				 * set categories to root
				 */
				sb.setLength(0);
				sb.append("UPDATE ");
				sb.append(TourDatabase.TABLE_TOUR_TAG_CATEGORY);
				sb.append(" SET isRoot=1");
				result = conn.createStatement().executeUpdate(sb.toString());
				System.out.println("Set " + result + " tour categories to root");

				conn.close();

				// update the tag viewer
				fRootItem = new TVIRootItem(fTagViewer);
				updateTagViewer();

			} catch (final SQLException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public boolean performOk() {

		if (fIsModified) {

			TourDatabase.cleanTourTags();

			// fire modify event
			getPreferenceStore().setValue(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED, Math.random());
		}

		return true;
	}

	public void setIsModified(final boolean fIsModified) {
		this.fIsModified = fIsModified;
	}

	/**
	 * set the toolbar action after the {@link #fTagViewer} is created
	 */
	private void setToolbarActions() {

		final ToolBarManager tbm = new ToolBarManager(fToolBar);

		tbm.add(new ActionExpandAll(fTagViewer));
		tbm.add(new ActionCollapseAll(fTagViewer));

		tbm.update(true);
	}

	private void updateTagViewer() {

		// show contents in the viewers
		fTagViewer.setInput(this);

		enableButtons();
	}

}
