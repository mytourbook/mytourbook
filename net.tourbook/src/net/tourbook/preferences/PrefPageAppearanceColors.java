/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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
import java.util.Arrays;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.colors.ColorDefinition;
import net.tourbook.colors.GraphColorItem;
import net.tourbook.colors.GraphColorProvider;
import net.tourbook.mapping.DialogMappingColor;
import net.tourbook.mapping.ILegendProviderGradientColors;
import net.tourbook.mapping.LegendColor;
import net.tourbook.mapping.LegendConfig;
import net.tourbook.mapping.LegendProviderMinMax;
import net.tourbook.mapping.ValueColor;
import net.tourbook.ui.UI;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
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
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageAppearanceColors extends PreferencePage implements IWorkbenchPreferencePage, IColorTreeViewer {

	private static final List<Integer>	_unitValues		= Arrays.asList(10, 50, 100, 150, 190);
	private static final List<String>	_unitLabels		= Arrays.asList(
																Messages.Pref_ChartColors_unit_min,
																Messages.Pref_ChartColors_unit_low,
																Messages.Pref_ChartColors_unit_mid,
																Messages.Pref_ChartColors_unit_high,
																Messages.Pref_ChartColors_unit_max);

	private ValueColor[]				_valueColors	= new ValueColor[] {
			new ValueColor(10, 255, 0, 0),
			new ValueColor(50, 100, 100, 0),
			new ValueColor(100, 0, 255, 0),
			new ValueColor(150, 0, 100, 100),
			new ValueColor(190, 0, 0, 255)				};

	TreeViewer							_colorViewer;

	private ColorSelector				_colorSelector;
	private Button						_btnLegend;

	private GraphColorItem				_selectedColor;
	private boolean						_isColorChanged;

	private ColorDefinition				_expandedItem;

	private LegendProviderMinMax				_legendProvider;
	private DialogMappingColor			_dialogMappingColor;
	private GraphColorPainter			_graphColorPainter;

	/**
	 * the color content provider has the following structure<br>
	 * 
	 * <pre>
	 * {@link ColorDefinition}
	 *    {@link GraphColorItem}
	 *    {@link GraphColorItem}
	 *    ...
	 *    {@link GraphColorItem}
	 * 
	 *    ...
	 * 
	 * {@link ColorDefinition}
	 *    {@link GraphColorItem}
	 *    {@link GraphColorItem}
	 *    ...
	 *    {@link GraphColorItem}
	 * </pre>
	 */
	private static class ColorContentProvider implements ITreeContentProvider {

		public void dispose() {}

		public Object[] getChildren(final Object parentElement) {
			if (parentElement instanceof ColorDefinition) {
				final ColorDefinition graphDefinition = (ColorDefinition) parentElement;
				return graphDefinition.getGraphColorParts();
			}
			return null;
		}

		public Object[] getElements(final Object inputElement) {
			if (inputElement instanceof PrefPageAppearanceColors) {
				return GraphColorProvider.getInstance().getGraphColorDefinitions();
			}
			return null;
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

	public void actionApplyColors() {

		updateColorsFromDialog(_selectedColor.getColorDefinition());
		updateAndSaveColors();
	}

	/**
	 * create color objects for every graph definition
	 */
	private void createColorDefinitions() {

		final ColorDefinition[] graphColorDefinitions = GraphColorProvider.getInstance().getGraphColorDefinitions();

		for (final ColorDefinition colorDefinition : graphColorDefinitions) {

			final ArrayList<GraphColorItem> graphColors = new ArrayList<GraphColorItem>();

			final boolean isLegendColorAvailable = colorDefinition.getLegendColor() != null;

			for (int nameIndex = 0; nameIndex < GraphColorProvider.colorNames.length; nameIndex++) {

				if (nameIndex == 3) {
					if (isLegendColorAvailable) {
						// create legend color
						graphColors.add(new GraphColorItem(
								colorDefinition,
								GraphColorProvider.colorNames[nameIndex][0],
								GraphColorProvider.colorNames[nameIndex][1],
								true));
					}
				} else {
					graphColors.add(new GraphColorItem(
							colorDefinition,
							GraphColorProvider.colorNames[nameIndex][0],
							GraphColorProvider.colorNames[nameIndex][1],
							false));
				}
			}

			colorDefinition.setColorNames(graphColors.toArray(new GraphColorItem[graphColors.size()]));
		}
	}

	@Override
	protected Control createContents(final Composite parent) {

		createColorDefinitions();

		final Composite ui = createUI(parent);

		initializeLegend();

		_colorViewer.setInput(this);

		return ui;
	}

	private Composite createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			createUI10ColorViewer(container);
			createUI20ColorControl(container);
		}

		return container;
	}

	private void createUI10ColorViewer(final Composite parent) {

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

		_colorViewer = new TreeViewer(tree);
		defineAllColumns(treeLayout, tree);

		_colorViewer.setContentProvider(new ColorContentProvider());

		_graphColorPainter = new GraphColorPainter(this);

		_colorViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				onSelectColorInColorViewer();
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

						// expanding the treeangle, the layout is correctly done but not with double click
						layoutContainer.layout(true, true);
					}
				} else if (selection instanceof GraphColorItem) {

					final GraphColorItem graphColor = (GraphColorItem) selection;

					if (graphColor.isLegend()) {

						// legend color is selected

						onSelectMappingColor();

					} else {
						// open color selection dialog
						_colorSelector.open();
					}
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

					if (_expandedItem != null) {
						_colorViewer.collapseToLevel(_expandedItem, 1);
					}

					Display.getCurrent().asyncExec(new Runnable() {
						public void run() {
							_colorViewer.expandToLevel(treeItem, 1);
							_expandedItem = treeItem;
						}
					});
				}
			}
		});
	}

	/**
	 * Create the color selection control.
	 * 
	 * @param parent
	 */
	private void createUI20ColorControl(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		// container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			/*
			 * button: color selector:
			 */
			_colorSelector = new ColorSelector(container);
			_colorSelector.getButton().setLayoutData(new GridData());
			_colorSelector.setEnabled(false);
			setButtonLayoutData(_colorSelector.getButton());
			_colorSelector.addListener(new IPropertyChangeListener() {
				public void propertyChange(final PropertyChangeEvent event) {
					onSelectColorInColorSelector(event);
				}
			});

			/*
			 * button: mapping color
			 */
			_btnLegend = new Button(container, SWT.NONE);
			_btnLegend.setText(Messages.Pref_ChartColors_btn_legend);
			setButtonLayoutData(_btnLegend);

			_btnLegend.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectMappingColor();
				}
			});
			_btnLegend.setEnabled(false);
		}
	}


	/**
	 * create columns
	 */
	private void defineAllColumns(final TreeColumnLayout treeLayout, final Tree tree) {

		TreeViewerColumn tvc;
		TreeColumn tc;
		final int colorWidth = (tree.getItemHeight() + 0) * 4 + 10;

		// 1. column: color item/color definition
		tvc = new TreeViewerColumn(_colorViewer, SWT.TRAIL);
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

		// 2. column: color for definition/item
		tvc = new TreeViewerColumn(_colorViewer, SWT.TRAIL);
		tc = tvc.getColumn();
		tvc.setLabelProvider(new StyledCellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				if (element instanceof ColorDefinition) {
					cell.setImage(_graphColorPainter.drawDefinitionImage((ColorDefinition) element));
				} else if (element instanceof GraphColorItem) {
					cell.setImage(_graphColorPainter.drawColorImage((GraphColorItem) element));
				} else {
					cell.setImage(null);
				}
			}
		});
		treeLayout.setColumnData(tc, new ColumnPixelData(colorWidth, true));
	}

	public ILegendProviderGradientColors getLegendProvider() {
		return _legendProvider;
	}

	public TreeViewer getTreeViewer() {
		return _colorViewer;
	}

	public void init(final IWorkbench workbench) {
		setPreferenceStore(TourbookPlugin.getDefault().getPreferenceStore());
	}

	/**
	 * setup legend
	 */
	private void initializeLegend() {

		final LegendConfig legendConfig = new LegendConfig();
		legendConfig.units = _unitValues;
		legendConfig.unitLabels = _unitLabels;
		legendConfig.legendMinValue = 0;
		legendConfig.legendMaxValue = 200;
		legendConfig.unitText = UI.EMPTY_STRING;

		final LegendColor legendColor = new LegendColor();
		legendColor.valueColors = _valueColors;

		_legendProvider = new LegendProviderMinMax(legendConfig, legendColor, 0);
		_dialogMappingColor = new DialogMappingColor(Display.getCurrent().getActiveShell(), _legendProvider, this);
	}

	/**
	 * is called when the color in the color selector has changed
	 * 
	 * @param event
	 */
	private void onSelectColorInColorSelector(final PropertyChangeEvent event) {

		final RGB oldValue = (RGB) event.getOldValue();
		final RGB newValue = (RGB) event.getNewValue();

		if (!oldValue.equals(newValue) && _selectedColor != null) {

			// color has changed

			// update the data model
			_selectedColor.setNewRGB(newValue);

			/*
			 * dispose the old color/image from the graph
			 */
			_graphColorPainter.disposeResources(_selectedColor.getColorId(), _selectedColor
					.getColorDefinition()
					.getImageId());

			/*
			 * update the tree viewer, the color images will then be recreated
			 */
			_colorViewer.update(_selectedColor, null);
			_colorViewer.update(_selectedColor.getColorDefinition(), null);

			_isColorChanged = true;
		}
	}

	/**
	 * is called when the color in the color viewer was selected
	 */
	private void onSelectColorInColorViewer() {

		final IStructuredSelection selection = (IStructuredSelection) _colorViewer.getSelection();

		_btnLegend.setEnabled(false);
		_colorSelector.setEnabled(false);

		if (selection.getFirstElement() instanceof GraphColorItem) {

			// graph color is selected

			final GraphColorItem graphColor = (GraphColorItem) selection.getFirstElement();

			// keep selected color
			_selectedColor = graphColor;

			if (graphColor.isLegend()) {

				// legend color is selected

				_btnLegend.setEnabled(true);

			} else {

				// 'normal' color is selected

				// prepare color selector
				_colorSelector.setColorValue(graphColor.getNewRGB());
				_colorSelector.setEnabled(true);
			}

		} else {

			// color definition is selected

		}
	}

	/**
	 * modify the colors of the legend
	 */
	private void onSelectMappingColor() {

		final ColorDefinition selectedColorDefinition = _selectedColor.getColorDefinition();

		// set the color which should be modified in the dialog
		_dialogMappingColor.setLegendColor(selectedColorDefinition);

		final int returnValue = _dialogMappingColor.open();

		if (returnValue != Window.OK) {
			return;
		}

		updateColorsFromDialog(selectedColorDefinition);
	}

	@Override
	public boolean performCancel() {

		resetColors();
		_graphColorPainter.disposeAllResources();

		return super.performCancel();
	}

	@Override
	protected void performDefaults() {

		final ColorDefinition[] graphColorDefinitions = GraphColorProvider.getInstance().getGraphColorDefinitions();

		// update current colors
		for (final ColorDefinition graphDefinition : graphColorDefinitions) {

			graphDefinition.setNewGradientBright(graphDefinition.getDefaultGradientBright());
			graphDefinition.setNewGradientDark(graphDefinition.getDefaultGradientDark());
			graphDefinition.setNewLineColor(graphDefinition.getDefaultLineColor());

			final LegendColor defaultLegendColor = graphDefinition.getDefaultLegendColor();
			if (defaultLegendColor != null) {
				graphDefinition.setNewLegendColor(defaultLegendColor.getCopy());
			}
		}

		_graphColorPainter.disposeAllResources();
		_colorViewer.refresh();

		_isColorChanged = true;

		super.performDefaults();
	}

	@Override
	public boolean performOk() {

		if (_isColorChanged) {
			updateAndSaveColors();
		}

		_graphColorPainter.disposeAllResources();

		return super.performOk();
	}

	private void resetColors() {

		for (final ColorDefinition graphDefinition : GraphColorProvider.getInstance().getGraphColorDefinitions()) {

			graphDefinition.setNewGradientBright(graphDefinition.getGradientBright());
			graphDefinition.setNewGradientDark(graphDefinition.getGradientDark());
			graphDefinition.setNewLineColor(graphDefinition.getLineColor());

			graphDefinition.setNewLegendColor(graphDefinition.getLegendColor());
		}
	}

	/**
	 * save the colors in the pref store and the legendcolor in a xml file
	 */
	private void saveGraphColors() {

		final IPreferenceStore prefStore = getPreferenceStore();

		for (final ColorDefinition graphDefinition : GraphColorProvider.getInstance().getGraphColorDefinitions()) {

			final String prefGraphName = ITourbookPreferences.GRAPH_COLORS + graphDefinition.getPrefName() + "."; //$NON-NLS-1$

			PreferenceConverter.setValue(
					prefStore,
					prefGraphName + GraphColorProvider.PREF_COLOR_BRIGHT,
					graphDefinition.getNewGradientBright());

			PreferenceConverter.setValue(
					prefStore,
					prefGraphName + GraphColorProvider.PREF_COLOR_DARK,
					graphDefinition.getNewGradientDark());

			PreferenceConverter.setValue(
					prefStore,
					prefGraphName + GraphColorProvider.PREF_COLOR_LINE,
					graphDefinition.getNewLineColor());
		}

		GraphColorProvider.saveLegendData();
	}

	private void updateAndSaveColors() {

		saveGraphColors();

		// update current colors
		for (final ColorDefinition graphDefinition : GraphColorProvider.getInstance().getGraphColorDefinitions()) {

			graphDefinition.setGradientBright(graphDefinition.getNewGradientBright());
			graphDefinition.setGradientDark(graphDefinition.getNewGradientDark());
			graphDefinition.setLineColor(graphDefinition.getNewLineColor());

			graphDefinition.setLegendColor(graphDefinition.getNewLegendColor());
		}

		// force to change the status
		getPreferenceStore().setValue(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED, Math.random());
	}

	private void updateColorsFromDialog(final ColorDefinition selectedColorDefinition) {

		// set new legend color
		selectedColorDefinition.setNewLegendColor(_dialogMappingColor.getLegendColor());

		/*
		 * show java code for the selected color, this code can copy/pasted into GraphColorProvider
		 */
//		System.out.println(fSelectedColor.getColorDefinition().getLegendColor().createConstructorString());
//
//
		/*
		 * dispose old color and image for the graph
		 */
		_graphColorPainter.disposeResources(_selectedColor.getColorId(), selectedColorDefinition.getImageId());

		/*
		 * update the tree viewer, the color images will be recreated
		 */
		_colorViewer.update(_selectedColor, null);
		_colorViewer.update(selectedColorDefinition, null);

		_isColorChanged = true;
	}

}
