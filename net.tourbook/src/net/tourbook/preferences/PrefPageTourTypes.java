/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
import java.util.List;

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
import net.tourbook.ui.UI;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageTourTypes extends PreferencePage implements IWorkbenchPreferencePage, IColorTreeViewer {

	private TreeViewer							_colorViewer;
	private ColorDefinition						_expandedItem;
	private GraphColorItem						_selectedColor;

	private Button								_btnAdd;
	private Button								_btnDelete;
	private Button								_btnRename;

	private ArrayList<TourType>					_tourTypes;
	private ArrayList<TourTypeColorDefinition>	_colorDefinitions;

	private IInputValidator						_tourNameValidator;

	private boolean								_isModified	= false;

	private GraphColorLabelProvider				_colorLabelProvider;

	private ColorSelector						_colorSelector;
	private Button								_btnTourTypeImage;

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
			return _colorDefinitions.toArray(new Object[_colorDefinitions.size()]);
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

	/**
	 * create the different color names (childs) for the color definition
	 */
	private void createColorNames(final ColorDefinition colorDefinition) {

		// use the first three color, mapping color is not used in tour types
		final int graphNamesLength = GraphColorProvider.colorNames.length - 1;

		final GraphColorItem[] graphColors = new GraphColorItem[graphNamesLength];

		for (int nameIndex = 0; nameIndex < graphNamesLength; nameIndex++) {
			graphColors[nameIndex] = new GraphColorItem(
					colorDefinition,
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
		_tourTypes = TourDatabase.getAllTourTypes();

		// create the color definitions
		_colorDefinitions = new ArrayList<TourTypeColorDefinition>();

		if (_tourTypes != null) {
			for (final TourType tourType : _tourTypes) {

				final TourTypeColorDefinition colorDefinition = new TourTypeColorDefinition(
						tourType,
						"tourtype." + tourType.getTypeId(), //$NON-NLS-1$
						tourType.getName(),
						tourType.getRGBBright(),
						tourType.getRGBDark(),
						tourType.getRGBLine());

				_colorDefinitions.add(colorDefinition);

				createColorNames(colorDefinition);
			}
		}

		_tourNameValidator = new IInputValidator() {
			public String isValid(final String newText) {
				return null;
			}
		};

		enableButtons();

		_colorViewer.setInput(this);

		return viewerContainer;
	}

	private Composite createUI(final Composite parent) {

		final Label label = new Label(parent, SWT.WRAP);
		label.setText(Messages.Pref_TourTypes_Title);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		// container
		final Composite container = new Composite(parent, SWT.NONE);
		final GridLayout gl = new GridLayout(2, false);
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		container.setLayout(gl);
		container.setLayoutData(new GridData(SWT.NONE, SWT.FILL, true, true));

		createUITourTypeViewer(container);
		createUIButtons(container);

		return container;
	}

	private void createUIButtons(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
		final GridLayout gridLayout = new GridLayout();
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		container.setLayout(gridLayout);

		// button: add
		_btnAdd = new Button(container, SWT.NONE);
		_btnAdd.setText(Messages.Pref_TourTypes_Button_add);
		setButtonLayoutData(_btnAdd);
		_btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onAddTourType();
				enableButtons();
			}
		});

		// button: rename
		_btnRename = new Button(container, SWT.NONE);
		_btnRename.setText(Messages.Pref_TourTypes_Button_rename);
		setButtonLayoutData(_btnRename);
		_btnRename.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onRenameTourType();
			}
		});

		_colorSelector = new ColorSelector(container);
		_colorSelector.getButton().setLayoutData(new GridData());
		_colorSelector.setEnabled(false);
		setButtonLayoutData(_colorSelector.getButton());
		_colorSelector.addListener(new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent event) {
				onChangeColor(event);
			}
		});

// 2009-01-02 disabled because the tour data cache was cleared
// button: delete
		_btnDelete = new Button(container, SWT.NONE);
		_btnDelete.setText(Messages.Pref_TourTypes_Button_delete);
		final GridData gd = setButtonLayoutData(_btnDelete);
		gd.verticalIndent = 10;
		_btnDelete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onDeleteTourType();
				enableButtons();
			}
		});

		_btnTourTypeImage = new Button(container, SWT.NONE);
		_btnTourTypeImage.setImage(UI.getInstance().getTourTypeImage(-1));

	}

	private void createUITourTypeViewer(final Composite parent) {

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
		final Tree tree = new Tree(treeContainer, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);

		tree.setLinesVisible(false);
		final int colorColumnWidth = tree.getItemHeight() * 4 + 5;

		// tree columns
		TreeColumn tc;

		tc = new TreeColumn(tree, SWT.NONE);
		tc.setText(Messages.Pref_TourTypes_Column_Color);
		treeLayouter.addColumnData(new ColumnWeightData(3, true));

		tc = new TreeColumn(tree, SWT.NONE);
		treeLayouter.addColumnData(new ColumnPixelData(colorColumnWidth, true));

		tc = new TreeColumn(tree, SWT.NONE);
		treeLayouter.addColumnData(new ColumnPixelData(colorColumnWidth, true));

		_colorViewer = new TreeViewer(tree);
		_colorViewer.setContentProvider(new ColorContentProvider());

		_colorLabelProvider = new GraphColorLabelProvider(this);
		_colorViewer.setLabelProvider(_colorLabelProvider);

		_colorViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				onSelectColor();
				enableButtons();
			}
		});

		_colorViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(final DoubleClickEvent event) {

				final Object selection = ((IStructuredSelection) _colorViewer.getSelection()).getFirstElement();

				if (selection instanceof ColorDefinition) {

					// expand/collapse current item
					final ColorDefinition treeItem = (ColorDefinition) selection;

					if (_colorViewer.getExpandedState(treeItem)) {
						_colorViewer.collapseToLevel(treeItem, 1);
					} else {
						if (_expandedItem != null) {
							_colorViewer.collapseToLevel(_expandedItem, 1);
						}
						_colorViewer.expandToLevel(treeItem, 1);
						_expandedItem = treeItem;
					}
				} else if (selection instanceof GraphColorItem) {

					// open color dialog
					_colorSelector.open();
				}
			}
		});

		_colorViewer.addTreeListener(new ITreeViewerListener() {

			public void treeCollapsed(final TreeExpansionEvent event) {

				if (event.getElement() instanceof ColorDefinition) {
					_expandedItem = null;
				}
			}

			public void treeExpanded(final TreeExpansionEvent event) {

				final Object element = event.getElement();

				if (element instanceof ColorDefinition) {
					final ColorDefinition treeItem = (ColorDefinition) element;

					/*
					 * run not in the treeExpand method, this is blocked by the viewer with the
					 * message: Ignored reentrant call while viewer is busy
					 */
					Display.getCurrent().asyncExec(new Runnable() {
						public void run() {

							if (_expandedItem != null) {
								_colorViewer.collapseToLevel(_expandedItem, 1);
							}
							_colorViewer.expandToLevel(treeItem, 1);
							_expandedItem = treeItem;
						}
					});
				}
			}
		});

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

	private boolean deleteTourTypeFromTourData(final TourType tourType) {

		boolean returnResult = false;

		final EntityManager em = TourDatabase.getInstance().getEntityManager();

		if (em != null) {

			final Query query = em.createQuery(//
					//
					"SELECT tourData" //$NON-NLS-1$
							+ (" FROM TourData AS tourData") //$NON-NLS-1$
							+ (" WHERE tourData.tourType.typeId=" + tourType.getTypeId())); //$NON-NLS-1$

			final List<?> tourDataList = query.getResultList();
			if (tourDataList.size() > 0) {

				final EntityTransaction ts = em.getTransaction();

				try {

					ts.begin();

					// remove tour type from all tour data
					for (final Object listItem : tourDataList) {

						if (listItem instanceof TourData) {

							final TourData tourData = (TourData) listItem;

							tourData.setTourType(null);
							em.merge(tourData);
						}
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

		final ITreeSelection selection = (ITreeSelection) _colorViewer.getSelection();

		if (selection.isEmpty()) {
			_btnDelete.setEnabled(false);
			_btnRename.setEnabled(false);
		} else {
			_btnDelete.setEnabled(true);
			_btnRename.setEnabled(true);
		}
	}

	private void fireModifyEvent() {

		if (_isModified) {

			_isModified = false;

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

		final IStructuredSelection selection = (IStructuredSelection) _colorViewer.getSelection();
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
		return _colorViewer;
	}

	public void init(final IWorkbench workbench) {
		setPreferenceStore(TourbookPlugin.getDefault().getPreferenceStore());
//		noDefaultAndApplyButton();
	}

	@Override
	public boolean okToLeave() {

		fireModifyEvent();

		return super.okToLeave();
	}

	private void onAddTourType() {

		// ask for the tour type name
		final InputDialog dialog = new InputDialog(
				this.getShell(),
				Messages.Pref_TourTypes_Dlg_new_tour_type_title,
				Messages.Pref_TourTypes_Dlg_new_tour_type_msg,
				UI.EMPTY_STRING,
				_tourNameValidator);

		if (dialog.open() != Window.OK) {
			return;
		}

		// create new tour type
		final TourType newTourType = new TourType(dialog.getValue());

		final TourTypeColorDefinition newColorDefinition = new TourTypeColorDefinition(
				newTourType,
				Long.toString(newTourType.getTypeId()),
				newTourType.getName());

		newTourType.setColorBright(newColorDefinition.getDefaultGradientBright());
		newTourType.setColorDark(newColorDefinition.getDefaultGradientDark());
		newTourType.setColorLine(newColorDefinition.getDefaultLineColor());

		// add new entity to db
		final TourType saveTourType = TourDatabase.saveEntity(newTourType, newTourType.getTypeId(), TourType.class);
		if (saveTourType != null) {

			// overwrite tour type object
			newColorDefinition.setTourType(saveTourType);

			createColorNames(newColorDefinition);

			_colorDefinitions.add(newColorDefinition);

			_colorViewer.add(this, newColorDefinition);

			// update internal tour type list
			_tourTypes.add(saveTourType);

			_isModified = true;
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

		if (!oldValue.equals(newValue) && _selectedColor != null) {

			// color has changed

			// update the data model
			_selectedColor.setNewRGB(newValue);

			final ColorDefinition colorDefinition = _selectedColor.getColorDefinition();

			/*
			 * dispose the old color/image from the graph
			 */
			_colorLabelProvider.disposeResources(_selectedColor.getColorId(), colorDefinition.getImageId());

			/*
			 * update the tree viewer, the color images will be recreated in the label provider
			 */
			_colorViewer.update(_selectedColor, null);
			_colorViewer.update(colorDefinition, null);

			/*
			 * update the tour type in the db
			 */
			final TourTypeColorDefinition tourTypeColorDefinition = (TourTypeColorDefinition) colorDefinition;
			final TourType oldTourType = tourTypeColorDefinition.getTourType();

			oldTourType.setColorBright(colorDefinition.getNewGradientBright());
			oldTourType.setColorDark(colorDefinition.getNewGradientDark());
			oldTourType.setColorLine(colorDefinition.getNewLineColor());

			final TourType savedTourType = TourDatabase
					.saveEntity(oldTourType, oldTourType.getTypeId(), TourType.class);

			tourTypeColorDefinition.setTourType(savedTourType);

			// replace tour type with new one
			_tourTypes.remove(oldTourType);
			_tourTypes.add(savedTourType);

			UI.getInstance().setTourTypeImagesDirty();
			_btnTourTypeImage.setImage(UI.getInstance().getTourTypeImage(savedTourType.getTypeId()));

			_isModified = true;
		}
	}

	private void onDeleteTourType() {

		final TourTypeColorDefinition selectedColorDefinition = getSelectedColorDefinition();
		final TourType selectedTourType = selectedColorDefinition.getTourType();

		// confirm deletion
		final String[] buttons = new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL };

		final MessageDialog dialog = new MessageDialog(
				this.getShell(),
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
			_colorViewer.remove(selectedColorDefinition);

			// update import combo
			// fComboImportTourType.remove(getTourTypeIndex(selectedTourType));

			// update internal list
			_tourTypes.remove(selectedTourType);

			_isModified = true;
		}
	}

	private void onRenameTourType() {

		final TourTypeColorDefinition selectedColorDefinition = getSelectedColorDefinition();
		final TourType selectedTourType = selectedColorDefinition.getTourType();

		// ask for the tour type name
		final InputDialog dialog = new InputDialog(
				this.getShell(),
				Messages.Pref_TourTypes_Dlg_rename_tour_type_title,
				NLS.bind(Messages.Pref_TourTypes_Dlg_rename_tour_type_msg, selectedTourType.getName()),
				selectedTourType.getName(),
				_tourNameValidator);
		if (dialog.open() != Window.OK) {
			return;
		}

		// update tour type name
		final String newTourTypeName = dialog.getValue();

		selectedTourType.setName(newTourTypeName);
		selectedColorDefinition.setVisibleName(newTourTypeName);

		// update entity in the db
		final TourType saveTourType = TourDatabase.saveEntity(
				selectedTourType,
				selectedTourType.getTypeId(),
				TourType.class);

		if (saveTourType != null) {

			selectedColorDefinition.setTourType(saveTourType);

			// update viewer
			_colorViewer.update(selectedColorDefinition, null);

			_isModified = true;
		}
	}

	/**
	 * is called when the color in the color viewer was selected
	 */
	private void onSelectColor() {

		final IStructuredSelection selection = (IStructuredSelection) _colorViewer.getSelection();
		final Object firstElement = selection.getFirstElement();

		if (firstElement instanceof GraphColorItem) {

			final GraphColorItem graphColor = (GraphColorItem) firstElement;

			_selectedColor = graphColor;

			_colorSelector.setColorValue(graphColor.getNewRGB());
			_colorSelector.setEnabled(true);

			final TourType tourType = ((TourTypeColorDefinition) graphColor.getColorDefinition()).getTourType();

			_btnTourTypeImage.setImage(UI.getInstance().getTourTypeImage(tourType.getTypeId()));

		} else if (firstElement instanceof TourTypeColorDefinition) {

			final TourType tourType = ((TourTypeColorDefinition) firstElement).getTourType();

			_btnTourTypeImage.setImage(UI.getInstance().getTourTypeImage(tourType.getTypeId()));

		} else {
			_colorSelector.setEnabled(false);
		}
	}

	@Override
	public boolean performOk() {

		fireModifyEvent();

		return super.performOk();
	}
}
