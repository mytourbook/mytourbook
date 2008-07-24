/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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

import java.util.ArrayList;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import net.tourbook.Messages;
import net.tourbook.colors.ColorDefinition;
import net.tourbook.colors.GraphColorItem;
import net.tourbook.colors.GraphColorProvider;
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.mapping.ILegendProvider;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.tour.TourManager;
import net.tourbook.util.TreeColumnLayout;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.RGB;
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

public class PrefPageTourTypes extends PreferencePage implements IWorkbenchPreferencePage, IColorTreeViewer {

	/**
	 * width for the tour type combo box
	 */
//	private static final int					TOUR_TYPE_WIDTH		= 30;
	TreeViewer									fColorViewer;
	private ColorDefinition						fExpandedItem;
	private GraphColorItem						fSelectedColor;

	private Button								fButtonAdd;
	private Button								fButtonDelete;
	private Button								fButtonRename;

	private ArrayList<TourType>					fTourTypes;
	private ArrayList<TourTypeColorDefinition>	fColorDefinitions;

	private IInputValidator						fTourNameValidator;

	private boolean								fIsModified	= false;

	private GraphColorLabelProvider				fColorLabelProvider;

	private ColorSelector						fColorSelector;

	private class ColorContentProvider implements ITreeContentProvider {

		public void dispose() {}

		public Object[] getChildren(final Object parentElement) {
			if (parentElement instanceof ColorDefinition) {
				final ColorDefinition graphDefinition = (ColorDefinition) parentElement;
				return graphDefinition.getGraphColorParts();
			}
			return null;
		}

		public Object[] getElements(final Object inputElement) {
			return fColorDefinitions.toArray(new Object[fColorDefinitions.size()]);
		}

		public Object getParent(final Object element) {
			return null;
		}

		public boolean hasChildren(final Object element) {
			if (element instanceof ColorDefinition) {
				return true;
			}
			return false;
		}

		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}

	}

	private class TourTypeColorDefinition extends ColorDefinition {

		TourType	fTourType;

		TourTypeColorDefinition(final TourType tourType,
								final String prefName,
								final String visibleName,
								final RGB defaultGradientBright,
								final RGB defaultGradientDark,
								final RGB defaultLineColor) {

			super(prefName, visibleName, defaultGradientBright, defaultGradientDark, defaultLineColor, null);

			fTourType = tourType;
		}
	}

	private void createButtons(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
		final GridLayout gridLayout = new GridLayout();
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		container.setLayout(gridLayout);

		// button: add
		fButtonAdd = new Button(container, SWT.NONE);
		fButtonAdd.setText(Messages.Pref_TourTypes_Button_add);
		setButtonLayoutData(fButtonAdd);
		fButtonAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onAddTourType();
				enableButtons();
			}
		});

		// button: rename
		fButtonRename = new Button(container, SWT.NONE);
		fButtonRename.setText(Messages.Pref_TourTypes_Button_rename);
		setButtonLayoutData(fButtonRename);
		fButtonRename.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onRenameTourType();
			}
		});

		fColorSelector = new ColorSelector(container);
		fColorSelector.getButton().setLayoutData(new GridData());
		fColorSelector.setEnabled(false);
		setButtonLayoutData(fColorSelector.getButton());
		fColorSelector.addListener(new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent event) {
				onChangeColor(event);
			}
		});

		// button: delete
		fButtonDelete = new Button(container, SWT.NONE);
		fButtonDelete.setText(Messages.Pref_TourTypes_Button_delete);
		final GridData gd = setButtonLayoutData(fButtonDelete);
		gd.verticalIndent = 10;
		fButtonDelete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onDeleteTourType();
				enableButtons();
			}
		});

	}

	/**
	 * create the different color names (childs) for the color definition
	 */
	private void createColorNames(final ColorDefinition colorDefinition) {

		// use the first three color, mapping color is not used in tour types
		final int graphNamesLength = GraphColorProvider.colorNames.length - 1;

		final GraphColorItem[] graphColors = new GraphColorItem[graphNamesLength];

		for (int nameIndex = 0; nameIndex < graphNamesLength; nameIndex++) {
			graphColors[nameIndex] = new GraphColorItem(colorDefinition,
					GraphColorProvider.colorNames[nameIndex][0],
					GraphColorProvider.colorNames[nameIndex][1],
					false);
		}

		colorDefinition.setColorNames(graphColors);
	}

	@Override
	protected Control createContents(final Composite parent) {

		final Composite viewerContainer = createUI(parent);

		// read tour typed from the database
		fTourTypes = TourDatabase.getTourTypes();

		// fill the color definitions
		fColorDefinitions = new ArrayList<TourTypeColorDefinition>();

		if (fTourTypes != null) {
			for (final TourType tourType : fTourTypes) {

				final TourTypeColorDefinition colorDefinition = new TourTypeColorDefinition(tourType,
						"tourtype." + tourType.getTypeId(), //$NON-NLS-1$
						tourType.getName(),
						tourType.getRGBBright(),
						tourType.getRGBDark(),
						tourType.getRGBLine());

				fColorDefinitions.add(colorDefinition);

				createColorNames(colorDefinition);
			}
		}

		createButtons(viewerContainer);

		fTourNameValidator = new IInputValidator() {
			public String isValid(final String newText) {
				return null;
			}
		};

		enableButtons();

		fColorViewer.setInput(this);

		return viewerContainer;
	}

	private void createTreeViewer(final Composite parent) {

		// tree container
		final Composite treeContainer = new Composite(parent, SWT.NONE);
		final GridLayout gl = new GridLayout();
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		treeContainer.setLayout(gl);

		final GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = 100;
		treeContainer.setLayoutData(gd);

		final TreeColumnLayout treeLayouter = new TreeColumnLayout();
		treeContainer.setLayout(treeLayouter);

		// tour tree
		final Tree tree = new Tree(treeContainer, SWT.H_SCROLL | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);

		tree.setLinesVisible(false);

		// tree columns
		TreeColumn tc;

		tc = new TreeColumn(tree, SWT.NONE);
		tc.setText(Messages.Pref_TourTypes_Column_Color);
		treeLayouter.addColumnData(new ColumnWeightData(3, true));

		tc = new TreeColumn(tree, SWT.NONE);
		treeLayouter.addColumnData(new ColumnPixelData(tree.getItemHeight() * 4, true));

		tc = new TreeColumn(tree, SWT.NONE);
		treeLayouter.addColumnData(new ColumnPixelData(tree.getItemHeight() * 4, true));

		fColorViewer = new TreeViewer(tree);
		fColorViewer.setContentProvider(new ColorContentProvider());

		fColorLabelProvider = new GraphColorLabelProvider(this);
		fColorViewer.setLabelProvider(fColorLabelProvider);

		fColorViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				onSelectColor();
				enableButtons();
			}
		});

		fColorViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(final DoubleClickEvent event) {

				final Object selection = ((IStructuredSelection) fColorViewer.getSelection()).getFirstElement();

				if (selection instanceof ColorDefinition) {
					// expand/collapse current item
					final ColorDefinition treeItem = (ColorDefinition) selection;

					if (fColorViewer.getExpandedState(treeItem)) {
						fColorViewer.collapseToLevel(treeItem, 1);
					} else {
						if (fExpandedItem != null) {
							fColorViewer.collapseToLevel(fExpandedItem, 1);
						}
						fColorViewer.expandToLevel(treeItem, 1);
						fExpandedItem = treeItem;
					}
				} else if (selection instanceof GraphColorItem) {
					fColorSelector.open();
				}
			}
		});

		fColorViewer.addTreeListener(new ITreeViewerListener() {

			public void treeCollapsed(final TreeExpansionEvent event) {

				if (event.getElement() instanceof ColorDefinition) {
					fExpandedItem = null;
				}
			}

			public void treeExpanded(final TreeExpansionEvent event) {

				final Object element = event.getElement();

				if (element instanceof ColorDefinition) {
					final ColorDefinition treeItem = (ColorDefinition) element;

					if (fExpandedItem != null) {
						fColorViewer.collapseToLevel(fExpandedItem, 1);
					}
					fColorViewer.expandToLevel(treeItem, 1);
					fExpandedItem = treeItem;
				}
			}
		});

	}

	private Composite createUI(final Composite parent) {

		final Label label = new Label(parent, SWT.WRAP);
		label.setText(Messages.Pref_TourTypes_Title);
		label.setLayoutData(new GridData(SWT.NONE, SWT.NONE, true, false));

		// container
		final Composite viewerContainer = new Composite(parent, SWT.NONE);
		final GridLayout gl = new GridLayout(2, false);
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		viewerContainer.setLayout(gl);
		viewerContainer.setLayoutData(new GridData(SWT.NONE, SWT.FILL, true, true));
//		viewerContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));

		createTreeViewer(viewerContainer);

		return viewerContainer;
	}

	private boolean deleteTourType(final TourType tourType) {

		if (deleteTourTypeFromTourData(tourType)) {
			if (deleteTourTypeFromDb(tourType)) {
				return true;
			}
		}

		return false;
	}

	private boolean deleteTourTypeFromDb(final TourType tourType) {

		boolean returnResult = false;

		final EntityManager em = TourDatabase.getInstance().getEntityManager();
		final EntityTransaction ts = em.getTransaction();

		try {
			final TourType tourTypeEntity = em.find(TourType.class, tourType.getTypeId());

			if (tourTypeEntity != null) {
				ts.begin();
				em.remove(tourTypeEntity);
				ts.commit();
			}

		} catch (final Exception e) {
			e.printStackTrace();
		} finally {
			if (ts.isActive()) {
				ts.rollback();
			} else {
				returnResult = true;
			}
			em.close();
		}

		return returnResult;
	}

	@SuppressWarnings("unchecked")
	private boolean deleteTourTypeFromTourData(final TourType tourType) {

		boolean returnResult = false;

		final EntityManager em = TourDatabase.getInstance().getEntityManager();

		if (em != null) {

			final Query query = em.createQuery("SELECT TourData " //$NON-NLS-1$
					+ ("FROM " + TourDatabase.TABLE_TOUR_DATA + " TourData ") //$NON-NLS-1$ //$NON-NLS-2$
					+ (" WHERE TourData.tourType.typeId=" + tourType.getTypeId())); //$NON-NLS-1$

			final ArrayList<TourData> tourDataList = (ArrayList<TourData>) query.getResultList();

			if (tourDataList.size() > 0) {

				final EntityTransaction ts = em.getTransaction();

				try {

					ts.begin();

					// remove tour type from all tour data
					for (final TourData tourData : tourDataList) {
						tourData.setTourType(null);
						em.merge(tourData);
					}

					ts.commit();

				} catch (final Exception e) {
					e.printStackTrace();
				} finally {
					if (ts.isActive()) {
						ts.rollback();
					}
				}
			}

			returnResult = true;
			em.close();
		}

		return returnResult;
	}

	private void enableButtons() {

		final ITreeSelection selection = (ITreeSelection) fColorViewer.getSelection();

		if (selection.isEmpty()) {
			fButtonDelete.setEnabled(false);
			fButtonRename.setEnabled(false);
		} else {
			fButtonDelete.setEnabled(true);
			fButtonRename.setEnabled(true);
		}
	}

	private void fireModifyEvent() {

		if (fIsModified) {

			fIsModified = false;

			TourDatabase.clearTourTypes();
			TourManager.getInstance().clearTourDataCache();

			// fire modify event
			getPreferenceStore().setValue(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED, Math.random());
		}
	}

	public ILegendProvider getLegendProvider() {
		return null;
	}

	/**
	 * @return Returns the selected color definition in the color viewer
	 */
	private TourTypeColorDefinition getSelectedColorDefinition() {

		final IStructuredSelection selection = (IStructuredSelection) fColorViewer.getSelection();
		final Object selectedItem = selection.getFirstElement();

		TourTypeColorDefinition selectedColorDefinition = null;

		if (selectedItem instanceof GraphColorItem) {
			selectedColorDefinition = ((TourTypeColorDefinition) ((GraphColorItem) selectedItem).getColorDefinition());
		} else if (selectedItem instanceof TourTypeColorDefinition) {
			selectedColorDefinition = ((TourTypeColorDefinition) selectedItem);
		}
		return selectedColorDefinition;
	}

	public TreeViewer getTreeViewer() {
		return fColorViewer;
	}

	public void init(final IWorkbench workbench) {
		setPreferenceStore(TourbookPlugin.getDefault().getPreferenceStore());
	}

	@Override
	public boolean okToLeave() {

		fireModifyEvent();

		return super.okToLeave();
	}

	private void onAddTourType() {

		// ask for the reference tour name
		final InputDialog dialog = new InputDialog(this.getShell(),
				Messages.Pref_TourTypes_Dlg_new_tour_type_title,
				Messages.Pref_TourTypes_Dlg_new_tour_type_msg,
				"", //$NON-NLS-1$
				fTourNameValidator);

		if (dialog.open() != Window.OK) {
			return;
		}

		// create new tour type
		final TourType newTourType = new TourType(dialog.getValue());

		final TourTypeColorDefinition newColorDefinition = new TourTypeColorDefinition(newTourType,
				Long.toString(newTourType.getTypeId()),
				newTourType.getName(),
				new RGB(255, 255, 255),
				new RGB(255, 167, 199),
				new RGB(232, 152, 180));

		newTourType.setColorBright(newColorDefinition.getDefaultGradientBright());
		newTourType.setColorDark(newColorDefinition.getDefaultGradientDark());
		newTourType.setColorLine(newColorDefinition.getDefaultLineColor());

		// add new entity to db
		if (persistTourType(newTourType)) {

			createColorNames(newColorDefinition);

			fColorDefinitions.add(newColorDefinition);

			fColorViewer.add(this, newColorDefinition);

			// update the import combo
			// fComboImportTourType.add(newTourType.getName());

			// update internal tour type list
			fTourTypes.add(newTourType);

			fIsModified = true;
		}
	}

	/**
	 * is called when the color in the color selector has changed
	 * 
	 * @param event
	 */
	private void onChangeColor(final PropertyChangeEvent event) {

		final RGB oldValue = (RGB) event.getOldValue();
		final RGB newValue = (RGB) event.getNewValue();

		if (!oldValue.equals(newValue) && fSelectedColor != null) {

			// color has changed

			// update the data model
			fSelectedColor.setNewRGB(newValue);

			final ColorDefinition colorDefinition = fSelectedColor.getColorDefinition();
			/*
			 * dispose the old color/image from the graph
			 */
			fColorLabelProvider.disposeResources(fSelectedColor.getColorId(), colorDefinition.getImageId());

			/*
			 * update the tree viewer, the color images will be recreated in the label provider
			 */
			fColorViewer.update(fSelectedColor, null);
			fColorViewer.update(colorDefinition, null);

			/*
			 * update the tour type in the db
			 */
			final TourType tourType = ((TourTypeColorDefinition) colorDefinition).fTourType;

			tourType.setColorBright(colorDefinition.getNewGradientBright());
			tourType.setColorDark(colorDefinition.getNewGradientDark());
			tourType.setColorLine(colorDefinition.getNewLineColor());

			persistTourType(tourType);

			fIsModified = true;
		}
	}

	private void onDeleteTourType() {

		final TourTypeColorDefinition selectedColorDefinition = getSelectedColorDefinition();
		final TourType selectedTourType = selectedColorDefinition.fTourType;

		// confirm deletion
		final String[] buttons = new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL };

		final MessageDialog dialog = new MessageDialog(this.getShell(),
				Messages.Pref_TourTypes_Dlg_delete_tour_type_title,
				null,
				NLS.bind(Messages.Pref_TourTypes_Dlg_delete_tour_type_msg, selectedTourType.getName()),
				MessageDialog.QUESTION,
				buttons,
				1);

		if (dialog.open() != Window.OK) {
			return;
		}

		// remove entity from the db
		if (deleteTourType(selectedTourType)) {

			// update color viewer
			fColorViewer.remove(selectedColorDefinition);

			// update import combo
			// fComboImportTourType.remove(getTourTypeIndex(selectedTourType));

			// update internal list
			fTourTypes.remove(selectedTourType);

			fIsModified = true;
		}
	}

	private void onRenameTourType() {

		final TourTypeColorDefinition selectedColorDefinition = getSelectedColorDefinition();
		final TourType selectedTourType = selectedColorDefinition.fTourType;

		// ask for the tour type name
		final InputDialog dialog = new InputDialog(this.getShell(),
				Messages.Pref_TourTypes_Dlg_rename_tour_type_title,
				NLS.bind(Messages.Pref_TourTypes_Dlg_rename_tour_type_msg, selectedTourType.getName()),
				selectedTourType.getName(),
				fTourNameValidator);
		if (dialog.open() != Window.OK) {
			return;
		}

		// update tour type name
		final String newTourTypeName = dialog.getValue();

		selectedTourType.setName(newTourTypeName);
		selectedColorDefinition.setVisibleName(newTourTypeName);

		// update entity in the db
		if (persistTourType(selectedTourType)) {

			// update viewer
			fColorViewer.update(selectedColorDefinition, null);

			fIsModified = true;
		}
	}

	/**
	 * is called when the color in the color viewer was selected
	 */
	private void onSelectColor() {

		final IStructuredSelection selection = (IStructuredSelection) fColorViewer.getSelection();

		if (selection.getFirstElement() instanceof GraphColorItem) {
			final GraphColorItem graphColor = (GraphColorItem) selection.getFirstElement();
			fSelectedColor = graphColor;
			fColorSelector.setColorValue(graphColor.getNewRGB());
			fColorSelector.setEnabled(true);
		} else {
			fColorSelector.setEnabled(false);
		}
	}

	@Override
	public boolean performOk() {

		fireModifyEvent();

		return super.performOk();
	}

	private boolean persistTourType(final TourType tourType) {

		boolean isSaved = false;

		final EntityManager em = TourDatabase.getInstance().getEntityManager();
		final EntityTransaction ts = em.getTransaction();

		try {

			if (tourType.getTypeId() == -1) {
				// entity is new
				ts.begin();
				em.persist(tourType);
				ts.commit();
			} else {
				// update entity
				ts.begin();
				em.merge(tourType);
				ts.commit();
			}

		} catch (final Exception e) {
			e.printStackTrace();
		} finally {
			if (ts.isActive()) {
				ts.rollback();
			} else {
				isSaved = true;
			}
			em.close();
		}
		return isSaved;
	}

}
