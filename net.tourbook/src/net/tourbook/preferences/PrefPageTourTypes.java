/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.color.ColorDefinition;
import net.tourbook.common.color.GraphColorItem;
import net.tourbook.common.color.GraphColorManager;
import net.tourbook.common.color.IGradientColorProvider;
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.preference.IPreferenceStore;
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
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
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

	private static final String					COLOR_UNIQUE_ID_PREFIX	= "crId";														//$NON-NLS-1$

	private static final String[]				SORT_PROPERTY			= new String[] { "this property is needed for sorting !!!" };	//$NON-NLS-1$

	private final IPreferenceStore				_prefStore				= TourbookPlugin.getPrefStore();

	private GraphColorPainter					_graphColorPainter;

	private ColorDefinition						_expandedItem;
	private GraphColorItem						_selectedGraphColor;

	private ArrayList<TourType>					_dbTourTypes;
	private ArrayList<TourTypeColorDefinition>	_colorDefinitions;

	private boolean								_isModified				= false;

	private UI									_ui;

	/*
	 * UI controls
	 */
	private TreeViewer							_tourTypeViewer;

	private Button								_btnAdd;
	private Button								_btnDelete;
	private Button								_btnRename;

	private CLabel								_iconTourTypeImage;

	private ColorSelector						_colorSelector;

	private class ColorContentProvider implements ITreeContentProvider {

		@Override
		public void dispose() {}

		@Override
		public Object[] getChildren(final Object parentElement) {

			if (parentElement instanceof ColorDefinition) {
				return ((ColorDefinition) parentElement).getGraphColorItems();
			}

			return null;
		}

		@Override
		public Object[] getElements(final Object inputElement) {
			return _colorDefinitions.toArray(new Object[_colorDefinitions.size()]);
		}

		@Override
		public Object getParent(final Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(final Object element) {
			if (element instanceof ColorDefinition) {
				return true;
			}
			return false;
		}

		@Override
		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}

	}

	private static final class TourTypeComparator extends ViewerComparator {
		@Override
		public int compare(final Viewer viewer, final Object e1, final Object e2) {

			if (e1 instanceof TourTypeColorDefinition && e2 instanceof TourTypeColorDefinition) {

				final TourTypeColorDefinition ttcDef1 = (TourTypeColorDefinition) e1;
				final TourTypeColorDefinition ttcDef2 = (TourTypeColorDefinition) e2;

				return ttcDef1.compareTo(ttcDef2);
			}

			return 0;
		}

		@Override
		public boolean isSorterProperty(final Object element, final String property) {

			// sort when the name has changed
			return true;
		}
	}

	@Override
	protected Control createContents(final Composite parent) {

		initUI();

		final Composite viewerContainer = createUI(parent);

		// read tour types from the database
		_dbTourTypes = TourDatabase.getAllTourTypes();

		// create color definitions for all tour types
		_colorDefinitions = new ArrayList<TourTypeColorDefinition>();

		if (_dbTourTypes != null) {
			for (final TourType tourType : _dbTourTypes) {

				final long typeId = tourType.getTypeId();

				// create a unique name for each tour type
				final Object colorId = typeId == TourDatabase.ENTITY_IS_NOT_SAVED //
						? COLOR_UNIQUE_ID_PREFIX + tourType.getCreateId()
						: typeId;

				final String colorName = "tourtype." + colorId; //$NON-NLS-1$

				final TourTypeColorDefinition colorDefinition = new TourTypeColorDefinition(
						tourType,
						colorName,
						tourType.getName(),
						tourType.getRGBBright(),
						tourType.getRGBDark(),
						tourType.getRGBLine(),
						tourType.getRGBText());

				_colorDefinitions.add(colorDefinition);

				createGraphColorItems(colorDefinition);
			}
		}

		enableActions();

		_tourTypeViewer.setInput(this);

		return viewerContainer;
	}

	/**
	 * Create the different color names (childs) for the color definition.
	 */
	private void createGraphColorItems(final ColorDefinition colorDefinition) {

		// use the first 4 color, the mapping color is not used in tour types
		final int graphNamesLength = GraphColorManager.colorNames.length - 1;

		final GraphColorItem[] graphColors = new GraphColorItem[graphNamesLength];

		for (int nameIndex = 0; nameIndex < graphNamesLength; nameIndex++) {

			graphColors[nameIndex] = new GraphColorItem(
					colorDefinition,
					GraphColorManager.colorNames[nameIndex][0],
					GraphColorManager.colorNames[nameIndex][1],
					false);
		}

		colorDefinition.setColorNames(graphColors);
	}

	private Composite createUI(final Composite parent) {

		final Label label = new Label(parent, SWT.WRAP);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(label);
		label.setText(Messages.Pref_TourTypes_Title);

		// container
		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			createUI_10_ColorViewer(container);
			createUI_20_Actions(container);
		}

		// must be set after the viewer is created
		_graphColorPainter = new GraphColorPainter(this);

		return container;
	}

	private void createUI_10_ColorViewer(final Composite parent) {

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
		tree.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

		_tourTypeViewer = new TreeViewer(tree);
		defineAllColumns(treeLayout, tree);

		_tourTypeViewer.setContentProvider(new ColorContentProvider());

		_tourTypeViewer.setComparator(new TourTypeComparator());
		_tourTypeViewer.setUseHashlookup(true);

		_tourTypeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				onSelectColor();
				enableActions();
			}
		});

		_tourTypeViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(final DoubleClickEvent event) {

				final Object selection = ((IStructuredSelection) _tourTypeViewer.getSelection()).getFirstElement();

				if (selection instanceof ColorDefinition) {

					// expand/collapse current item
					final ColorDefinition treeItem = (ColorDefinition) selection;

					if (_tourTypeViewer.getExpandedState(treeItem)) {
						_tourTypeViewer.collapseToLevel(treeItem, 1);
					} else {
						if (_expandedItem != null) {
							_tourTypeViewer.collapseToLevel(_expandedItem, 1);
						}
						_tourTypeViewer.expandToLevel(treeItem, 1);
						_expandedItem = treeItem;

						// expanding the treeangle, the layout is correctly done but not with double click
						layoutContainer.layout(true, true);
					}
				} else if (selection instanceof GraphColorItem) {

					// open color dialog
					_colorSelector.open();
				}
			}
		});

		_tourTypeViewer.addTreeListener(new ITreeViewerListener() {

			@Override
			public void treeCollapsed(final TreeExpansionEvent event) {

				if (event.getElement() instanceof ColorDefinition) {
					_expandedItem = null;
				}
			}

			@Override
			public void treeExpanded(final TreeExpansionEvent event) {

				final Object element = event.getElement();

				if (element instanceof ColorDefinition) {
					final ColorDefinition treeItem = (ColorDefinition) element;

					/*
					 * run not in the treeExpand method, this is blocked by the viewer with the
					 * message: Ignored reentrant call while viewer is busy
					 */
					Display.getCurrent().asyncExec(new Runnable() {
						@Override
						public void run() {

							if (_expandedItem != null) {
								_tourTypeViewer.collapseToLevel(_expandedItem, 1);
							}
							_tourTypeViewer.expandToLevel(treeItem, 1);
							_expandedItem = treeItem;
						}
					});
				}
			}
		});
	}

	private void createUI_20_Actions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{
			/*
			 * Add
			 */
			{
				_btnAdd = new Button(container, SWT.NONE);
				_btnAdd.setText(Messages.Pref_TourTypes_Button_add);
				_btnAdd.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onAddTourType();
						enableActions();
					}
				});
				setButtonLayoutData(_btnAdd);
			}

			/*
			 * Rename
			 */
			{
				_btnRename = new Button(container, SWT.NONE);
				_btnRename.setText(Messages.Pref_TourTypes_Button_rename);
				_btnRename.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onRenameTourType();
					}
				});
				setButtonLayoutData(_btnRename);
			}

			/*
			 * Delete
			 */
			{
// 2009-01-02 disabled because the tour data cache was cleared
// button: delete
				_btnDelete = new Button(container, SWT.NONE);
				_btnDelete.setText(Messages.Pref_TourTypes_Button_delete);
				_btnDelete.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onDeleteTourType();
						enableActions();
					}
				});
				setButtonLayoutData(_btnDelete);
			}

			/*
			 * Color selector
			 */
			{
				_colorSelector = new ColorSelector(container);
				_colorSelector.getButton().setLayoutData(new GridData());
				_colorSelector.setEnabled(false);
				_colorSelector.addListener(new IPropertyChangeListener() {
					@Override
					public void propertyChange(final PropertyChangeEvent event) {
						onChangeColor(event);
					}
				});
				setButtonLayoutData(_colorSelector.getButton());
//				final GridData gd = setButtonLayoutData(_btnDelete);
//				gd.verticalIndent = 50;
			}

			/*
			 * Tour type image
			 */
			{
				_iconTourTypeImage = new CLabel(container, SWT.NONE);
				_iconTourTypeImage.setImage(_ui.getTourTypeImage(-1));
				GridDataFactory.fillDefaults()//
						.grab(true, true)
						.align(SWT.CENTER, SWT.CENTER)
						.applyTo(_iconTourTypeImage);
			}
		}
	}

	/**
	 * create columns
	 */
	private void defineAllColumns(final TreeColumnLayout treeLayout, final Tree tree) {

		final int numberOfHorizontalImages = 4;
		final int trailingOffset = 10;

		final int itemHeight = tree.getItemHeight();
		final int oneColorWidth = itemHeight + GraphColorPainter.GRAPH_COLOR_SPACING;

		final int colorImageWidth = (oneColorWidth * numberOfHorizontalImages) + trailingOffset;

		TreeColumn tc;
		TreeViewerColumn tvc;

		/*
		 * 1. column: color item/color definition
		 */
		{
			tvc = new TreeViewerColumn(_tourTypeViewer, SWT.TRAIL);
			tc = tvc.getColumn();
			tvc.setLabelProvider(new StyledCellLabelProvider() {
				@Override
				public void update(final ViewerCell cell) {

					final Object element = cell.getElement();

					if (element instanceof ColorDefinition) {

						cell.setText(((ColorDefinition) (element)).getVisibleName());

					} else if (element instanceof GraphColorItem) {

						cell.setText(((GraphColorItem) (element)).getName());

					} else {

						cell.setText(UI.EMPTY_STRING);
					}
				}
			});
			treeLayout.setColumnData(tc, new ColumnWeightData(1, true));
		}

		/*
		 * 2. column: color for definition/item
		 */
		{
			tvc = new TreeViewerColumn(_tourTypeViewer, SWT.TRAIL);
			tc = tvc.getColumn();
			tvc.setLabelProvider(new StyledCellLabelProvider() {
				@Override
				public void update(final ViewerCell cell) {

					final Object element = cell.getElement();

					if (element instanceof ColorDefinition) {

						final Image image = _graphColorPainter.drawColorDefinitionImage(
								(ColorDefinition) element,
								numberOfHorizontalImages);

						cell.setImage(image);

					} else if (element instanceof GraphColorItem) {

						final Image image = _graphColorPainter.drawGraphColorImage(//
								(GraphColorItem) element,
								numberOfHorizontalImages);

						cell.setImage(image);

					} else {

						cell.setImage(null);
					}
				}
			});
			treeLayout.setColumnData(tc, new ColumnPixelData(colorImageWidth, true));
		}
	}

	private boolean deleteTourType(final TourType tourType) {

		if (deleteTourType_FromTourData(tourType)) {
			if (deleteTourType_FromDb(tourType)) {
				return true;
			}
		}

		return false;
	}

	private boolean deleteTourType_FromDb(final TourType tourType) {

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

	private boolean deleteTourType_FromTourData(final TourType tourType) {

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

	@Override
	public void dispose() {

		_graphColorPainter.disposeAllResources();

		super.dispose();
	}

	private void enableActions() {

		final IStructuredSelection selection = (IStructuredSelection) _tourTypeViewer.getSelection();
		final Object selectedItem = selection.getFirstElement();

		boolean isSelected = false;
		boolean isGraphSelected = false;

		if (selectedItem instanceof GraphColorItem) {

			isGraphSelected = true;
			isSelected = true;

		} else if (selectedItem instanceof TourTypeColorDefinition) {

			isSelected = true;
		}

		_btnDelete.setEnabled(isSelected);
		_btnRename.setEnabled(isSelected);

		_iconTourTypeImage.setEnabled(isSelected);
		_colorSelector.setEnabled(isGraphSelected);
	}

	private void fireModifyEvent() {

		if (_isModified) {

			_isModified = false;

			TourManager.getInstance().clearTourDataCache();

			// fire modify event
			_prefStore.setValue(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED, Math.random());
		}
	}

	@Override
	public IGradientColorProvider getMapLegendColorProvider() {
		return null;
	}

	/**
	 * @return Returns the selected color definition in the color viewer
	 */
	private TourTypeColorDefinition getSelectedColorDefinition() {

		TourTypeColorDefinition selectedColorDefinition = null;

		final IStructuredSelection selection = (IStructuredSelection) _tourTypeViewer.getSelection();
		final Object selectedItem = selection.getFirstElement();

		if (selectedItem instanceof GraphColorItem) {

			final GraphColorItem graphColor = (GraphColorItem) selectedItem;

			selectedColorDefinition = (TourTypeColorDefinition) graphColor.getColorDefinition();

		} else if (selectedItem instanceof TourTypeColorDefinition) {

			selectedColorDefinition = (TourTypeColorDefinition) selectedItem;
		}

		return selectedColorDefinition;
	}

	@Override
	public TreeViewer getTreeViewer() {
		return _tourTypeViewer;
	}

	@Override
	public void init(final IWorkbench workbench) {
		setPreferenceStore(_prefStore);
//		noDefaultAndApplyButton();
	}

	private void initUI() {

		_ui = UI.getInstance();
	}

	@Override
	public boolean okToLeave() {

		fireModifyEvent();

		return super.okToLeave();
	}

	private void onAddTourType() {

		// ask for the tour type name
		final InputDialog inputDialog = new InputDialog(
				this.getShell(),
				Messages.Pref_TourTypes_Dlg_new_tour_type_title,
				Messages.Pref_TourTypes_Dlg_new_tour_type_msg,
				UI.EMPTY_STRING,
				null);

		if (inputDialog.open() != Window.OK) {
			setFocusToViewer();
			return;
		}

		final String tourTypeName = inputDialog.getValue();

		// create new tour type
		final TourType newTourType = new TourType(tourTypeName);

		final TourTypeColorDefinition newColorDefinition = new TourTypeColorDefinition(//
				newTourType,
				COLOR_UNIQUE_ID_PREFIX + newTourType.getCreateId(),
				tourTypeName);

		newTourType.setColorBright(newColorDefinition.getGradientBright_Default());
		newTourType.setColorDark(newColorDefinition.getGradientDark_Default());
		newTourType.setColorLine(newColorDefinition.getLineColor_Default());
		newTourType.setColorText(newColorDefinition.getTextColor_Default());

		// add new entity to db
		final TourType saveTourType = TourDatabase.saveEntity(//
				newTourType,
				newTourType.getTypeId(),
				TourType.class);

		if (saveTourType != null) {

			/*
			 * Invalidate all tourtypes, this fixes a VEEEEEEEEEEERY long existing bug that a new
			 * tour type is initially not displayed correctly.
			 */
//			TourDatabase.clearTourTypes();

			// overwrite tour type object
			newColorDefinition.setTourType(saveTourType);

			createGraphColorItems(newColorDefinition);

			// update model
			_colorDefinitions.add(newColorDefinition);

			// update internal tour type list
			_dbTourTypes.add(saveTourType);
			Collections.sort(_dbTourTypes);

			// update UI
			_tourTypeViewer.add(this, newColorDefinition);

			_tourTypeViewer.setSelection(new StructuredSelection(newColorDefinition), true);

			_tourTypeViewer.collapseAll();
			_tourTypeViewer.expandToLevel(newColorDefinition, 1);

			_isModified = true;
		}

		setFocusToViewer();

		return;
	}

	/**
	 * is called when the color in the color selector has changed
	 * 
	 * @param event
	 */
	private void onChangeColor(final PropertyChangeEvent event) {

		final RGB oldRGB = (RGB) event.getOldValue();
		final RGB newRGB = (RGB) event.getNewValue();

		if (_selectedGraphColor == null || oldRGB.equals(newRGB)) {
			return;
		}

		// color has changed

		// update model
		_selectedGraphColor.setRGB(newRGB);

		final ColorDefinition colorDefinition = _selectedGraphColor.getColorDefinition();

		/*
		 * dispose the old color/image from the graph and color definition to force the recreation
		 */
		_graphColorPainter.recreateResources(//
				_selectedGraphColor.getColorId(),
				colorDefinition.getColorDefinitionId());

		/*
		 * update the tree viewer, the color images will be recreated in the label provider
		 */
		_tourTypeViewer.update(_selectedGraphColor, null);
		_tourTypeViewer.update(colorDefinition, null);

2nd color modification do NOT work !!! propably tour id has changed when saving the entity

		/*
		 * update the tour type in the db
		 */
		final TourTypeColorDefinition tourTypeColorDefinition = (TourTypeColorDefinition) colorDefinition;
		final TourType oldTourType = tourTypeColorDefinition.getTourType();

		oldTourType.setColorBright(colorDefinition.getGradientBright_New());
		oldTourType.setColorDark(colorDefinition.getGradientDark_New());
		oldTourType.setColorLine(colorDefinition.getLineColor_New());
		oldTourType.setColorText(colorDefinition.getTextColor_New());
		final TourType savedTourType = TourDatabase.saveEntity(//
				oldTourType,
				oldTourType.getTypeId(),
				TourType.class);

		tourTypeColorDefinition.setTourType(savedTourType);

		// replace tour type with new one
		_dbTourTypes.remove(oldTourType);
		_dbTourTypes.add(savedTourType);
		Collections.sort(_dbTourTypes);

		// update UI
		_ui.setTourTypeImagesDirty();
//		TourDatabase.clearTourTypes();

		final long typeId = savedTourType.getTypeId();
		_iconTourTypeImage.setImage(_ui.getTourTypeImage(typeId));

		_isModified = true;
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
			setFocusToViewer();
			return;
		}

		// remove entity from the db
		if (deleteTourType(selectedTourType)) {

			// update model
			_dbTourTypes.remove(selectedTourType);

			_colorDefinitions.remove(selectedColorDefinition);

			// update UI
			_tourTypeViewer.remove(selectedColorDefinition);

			_isModified = true;
		}

		setFocusToViewer();
	}

	private void onRenameTourType() {

		final TourTypeColorDefinition selectedColorDefinition = getSelectedColorDefinition();
		final TourType selectedTourType = selectedColorDefinition.getTourType();

		// ask for the tour type name
		final InputDialog dialog = new InputDialog(
				getShell(),
				Messages.Pref_TourTypes_Dlg_rename_tour_type_title,
				NLS.bind(Messages.Pref_TourTypes_Dlg_rename_tour_type_msg, selectedTourType.getName()),
				selectedTourType.getName(),
				null);

		if (dialog.open() != Window.OK) {
			setFocusToViewer();
			return;
		}

		// update tour type name
		final String newTourTypeName = dialog.getValue();

		selectedTourType.setName(newTourTypeName);
		selectedColorDefinition.setVisibleName(newTourTypeName);

		// update entity in the db
		final TourType savedTourType = TourDatabase.saveEntity(
				selectedTourType,
				selectedTourType.getTypeId(),
				TourType.class);

		if (savedTourType != null) {

			// update model
			selectedColorDefinition.setTourType(savedTourType);

			// replace tour type with new one
			_dbTourTypes.remove(selectedTourType);
			_dbTourTypes.add(savedTourType);
			Collections.sort(_dbTourTypes);

			// update viewer, resort types when necessary
			_tourTypeViewer.update(selectedColorDefinition, SORT_PROPERTY);

			_isModified = true;
		}

		setFocusToViewer();
	}

	/**
	 * Is called when a color in the color viewer is selected.
	 */
	private void onSelectColor() {

		_selectedGraphColor = null;

		final IStructuredSelection selection = (IStructuredSelection) _tourTypeViewer.getSelection();
		final Object firstElement = selection.getFirstElement();

		if (firstElement instanceof GraphColorItem) {

			final GraphColorItem graphColor = (GraphColorItem) firstElement;

			_selectedGraphColor = graphColor;

			_colorSelector.setColorValue(graphColor.getRGB());
			_colorSelector.setEnabled(true);

			final TourType tourType = ((TourTypeColorDefinition) graphColor.getColorDefinition()).getTourType();
			final long typeId = tourType.getTypeId();

			_iconTourTypeImage.setImage(_ui.getTourTypeImage(typeId));

		} else if (firstElement instanceof TourTypeColorDefinition) {

			final TourType tourType = ((TourTypeColorDefinition) firstElement).getTourType();
			final long typeId = tourType.getTypeId();

			_iconTourTypeImage.setImage(_ui.getTourTypeImage(typeId));

		} else {

			_colorSelector.setEnabled(false);
			_iconTourTypeImage.setImage(null);
		}

		setFocusToViewer();
	}

	@Override
	public boolean performCancel() {

		fireModifyEvent();

		return super.performCancel();
	}

	@Override
	public boolean performOk() {

		fireModifyEvent();

		return super.performOk();
	}

	private void setFocusToViewer() {

		// set focus back to the tree
		_tourTypeViewer.getTree().setFocus();
	}
}
