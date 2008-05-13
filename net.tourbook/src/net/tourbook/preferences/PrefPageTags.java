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

import net.tourbook.Messages;
import net.tourbook.data.TourTag;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.ui.UI;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageTags extends PreferencePage implements IWorkbenchPreferencePage {

	private TableViewer	fTagViewer;

	private Button		fBtnNew;
	private Button		fBtnRename;

//	private boolean		fIsModified;

	private class TagViewerContentProvicer implements IStructuredContentProvider {

		public void dispose() {}

		public Object[] getElements(final Object inputElement) {
			return TourDatabase.getTourTags().toArray();
		}

		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	/**
	 * Sort the markers by time
	 */
	private class TagViewerSorter extends ViewerSorter {

		@Override
		public int compare(final Viewer viewer, final Object obj1, final Object obj2) {
			return ((TourTag) (obj1)).getTagName().compareTo(((TourTag) (obj2)).getTagName());
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

		// button: new
		fBtnNew = new Button(container, SWT.NONE);
		fBtnNew.setText(Messages.Pref_TourTypeFilter_button_new);
		setButtonLayoutData(fBtnNew);
		fBtnNew.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onNewTag();
			}
		});

		// button: rename
		fBtnRename = new Button(container, SWT.NONE);
		fBtnRename.setText(Messages.Pref_TourTypeFilter_button_rename);
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

		updateViewers();

		return viewerContainer;
	}

	private void createTagViewer(final Composite parent) {

		final TableColumnLayout tableLayout = new TableColumnLayout();

		final Composite layoutContainer = new Composite(parent, SWT.NONE);
		layoutContainer.setLayout(tableLayout);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(layoutContainer);

		/*
		 * create table
		 */
		final Table table = new Table(layoutContainer, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);

		table.setLayout(new TableLayout());
		table.setHeaderVisible(false);
		table.setLinesVisible(false);

//		table.addKeyListener(new KeyAdapter() {
//			@Override
//			public void keyPressed(KeyEvent e) {
//
//				IStructuredSelection selection = (IStructuredSelection) fTagViewer.getSelection();
//
//				if (selection.size() > 0) {
//					if (e.keyCode == SWT.CR) {
//						if (e.stateMask == SWT.CONTROL) {
//							// edit visual position
//							fTagViewer.editElement(selection.getFirstElement(), COLUMN_VISUAL_POSITION);
//						} else {
//							if (fTagViewer.isCellEditorActive() == false) {
//								fTagViewer.editElement(selection.getFirstElement(), COLUMN_REMARK);
//							}
//						}
//					}
//				}
//			}
//		});

		fTagViewer = new TableViewer(table);

		/*
		 * create columns
		 */
		TableViewerColumn tvc;
		TableColumn tvcColumn;

		// column: time
		tvc = new TableViewerColumn(fTagViewer, SWT.TRAIL);
		tvcColumn = tvc.getColumn();
		tvcColumn.setText(Messages.Tag_Viewer_column_name);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourTag tag = (TourTag) cell.getElement();

				cell.setText(tag.getTagName());
			}
		});
		tableLayout.setColumnData(tvcColumn, new ColumnWeightData(100, true));

		/*
		 * create table viewer
		 */

		fTagViewer.setContentProvider(new TagViewerContentProvicer());
		fTagViewer.setSorter(new TagViewerSorter());

		fTagViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				final StructuredSelection selection = (StructuredSelection) event.getSelection();
				if (selection != null) {
					enableButtons();
				}
			}
		});

		fTagViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(final DoubleClickEvent event) {
				final StructuredSelection selection = (StructuredSelection) event.getSelection();
				if (selection != null) {
					onRenameTourTag();
				}
			}
		});
	}

	private Composite createUI(final Composite parent) {

		final Label label = new Label(parent, SWT.WRAP);
		label.setText(Messages.Pref_TourTypes_root_title);
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

	private void onNewTag() {

		final InputDialog inputDialog = new InputDialog(getShell(),
				Messages.Tag_Viewer_column_dlg_new_title,
				Messages.Tag_Viewer_column_dlg_new_nessage,
				UI.EMPTY_STRING,
				null);

		inputDialog.open();

		if (inputDialog.getReturnCode() != Window.OK) {
			return;
		}

		// create new tour tag
		final TourTag tourTag = new TourTag(inputDialog.getValue().trim());

		// add new entity to db
		if (TourDatabase.persistEntity(tourTag, tourTag.getTagId(), TourTag.class)) {

			// update model
			TourDatabase.getTourTags().add(tourTag);

			// update viewer
			fTagViewer.add(tourTag);

			// select new tag
			fTagViewer.setSelection(new StructuredSelection(tourTag), true);

			fTagViewer.getTable().setFocus();

//			fIsModified = true;
		}
	}

	/**
	 * Rename selected tag
	 */
	private void onRenameTourTag() {

		final TourTag tourTag = (TourTag) ((StructuredSelection) fTagViewer.getSelection()).getFirstElement();

		final InputDialog inputDialog = new InputDialog(getShell(),
				Messages.Tag_Viewer_column_dlg_rename_title,
				Messages.Tag_Viewer_column_dlg_rename_nessage,
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

//	private void saveFilterList() {
//
//		if (fIsModified) {
//
//			fIsModified = false;
//
//			TourTypeContributionItem.writeXMLFilterFile(fTagViewer);
//
//			// fire modify event
//			getPreferenceStore().setValue(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED, Math.random());
//		}
//	}

	private void updateViewers() {

		// show contents in the viewers
		fTagViewer.setInput(new Object());

		enableButtons();
	}

}
