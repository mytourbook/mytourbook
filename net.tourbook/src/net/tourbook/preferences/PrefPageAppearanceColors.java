/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
import net.tourbook.common.CommonActivator;
import net.tourbook.common.color.ColorDefinition;
import net.tourbook.common.color.ColorValue;
import net.tourbook.common.color.GradientColorProvider;
import net.tourbook.common.color.GraphColorItem;
import net.tourbook.common.color.GraphColorManager;
import net.tourbook.common.color.IGradientColors;
import net.tourbook.common.color.MapColor;
import net.tourbook.common.color.MapColorId;
import net.tourbook.common.color.MapLegendImageConfig;
import net.tourbook.map2.view.DialogMappingColor;
import net.tourbook.map2.view.IMapColorUpdater;
import net.tourbook.ui.UI;

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
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
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

public class PrefPageAppearanceColors extends PreferencePage implements IWorkbenchPreferencePage, IColorTreeViewer,
		IMapColorUpdater {

	private static ColorValue[]			_legendImageColors		= new ColorValue[] {
			new ColorValue(10, 255, 0, 0),
			new ColorValue(50, 100, 100, 0),
			new ColorValue(100, 0, 255, 0),
			new ColorValue(150, 0, 100, 100),
			new ColorValue(190, 0, 0, 255)						};

	private static final List<Float>	_legendImageUnitValues	= Arrays.asList(10f, 50f, 100f, 150f, 190f);
	private static final List<String>	_legendImageUnitLabels	= Arrays.asList(
																		Messages.Pref_ChartColors_unit_min,
																		Messages.Pref_ChartColors_unit_low,
																		Messages.Pref_ChartColors_unit_mid,
																		Messages.Pref_ChartColors_unit_high,
																		Messages.Pref_ChartColors_unit_max);

	private final IPreferenceStore		_prefStore				= TourbookPlugin.getDefault().getPreferenceStore();
	private final IPreferenceStore		_commonPrefStore		= CommonActivator.getPrefStore();

	TreeViewer							_colorViewer;

	private ColorSelector				_colorSelector;
	private Button						_btnLegend;

	private GraphColorItem				_selectedColor;
	private boolean						_isColorChanged;

	private ColorDefinition				_expandedItem;

	private IGradientColors				_legendImageColorProvider;
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
				return GraphColorManager.getInstance().getGraphColorDefinitions();
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

	public static GradientColorProvider createLegendImageColorProvider() {

		final MapLegendImageConfig legendConfig = new MapLegendImageConfig();

		legendConfig.units = _legendImageUnitValues;
		legendConfig.unitLabels = _legendImageUnitLabels;
		legendConfig.unitText = UI.EMPTY_STRING;
		legendConfig.legendMinValue = 0;
		legendConfig.legendMaxValue = 200;

		final MapColor legendColor = new MapColor();
		legendColor.colorValues = _legendImageColors;

		final GradientColorProvider legendImageColorProvider = new GradientColorProvider(
				MapColorId.Altitude,
				legendConfig,
				legendColor);

		return legendImageColorProvider;
	}

	@Override
	public void applyMapColors(final MapColor newMapColor) {

		updateColorsFromDialog(_selectedColor.getColorDefinition(), newMapColor);
		updateAndSaveColors();
	}

	/**
	 * create color objects for every graph definition
	 */
	private void createColorDefinitions() {

		final String[][] colorNames = GraphColorManager.colorNames;
		final ColorDefinition[] graphColorDefinitions = GraphColorManager.getInstance().getGraphColorDefinitions();

		for (final ColorDefinition colorDefinition : graphColorDefinitions) {

			final ArrayList<GraphColorItem> graphColors = new ArrayList<GraphColorItem>();

			final boolean isMapColorAvailable = colorDefinition.getMapColor() != null;

			for (final String[] colorName : colorNames) {

				if (colorName[0] == GraphColorManager.PREF_COLOR_MAPPING) {
					if (isMapColorAvailable) {
						// create map color
						graphColors.add(new GraphColorItem(colorDefinition, colorName[0], colorName[1], true));
					}
				} else {
					graphColors.add(new GraphColorItem(colorDefinition, colorName[0], colorName[1], false));
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
		tree.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

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

					if (graphColor.isMapColor()) {

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
		final int colorWidth = (tree.getItemHeight() + 0) * 5 + 10;

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

	public IGradientColors getMapLegendColorProvider() {
		return _legendImageColorProvider;
	}

	public TreeViewer getTreeViewer() {
		return _colorViewer;
	}

	public void init(final IWorkbench workbench) {
		setPreferenceStore(_commonPrefStore);
	}

	/**
	 * Setup legend and create a dummy color provider.
	 */
	private void initializeLegend() {

		_legendImageColorProvider = createLegendImageColorProvider();

		_dialogMappingColor = new DialogMappingColor(
				Display.getCurrent().getActiveShell(),
				_legendImageColorProvider,
				this);
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

			if (graphColor.isMapColor()) {

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

		// new colors will be set with applyMapColors
		_dialogMappingColor.open();
	}

	@Override
	protected void performApply() {

		if (_isColorChanged) {
			updateAndSaveColors();
		}
	}

	@Override
	public boolean performCancel() {

		resetColors();
		_graphColorPainter.disposeAllResources();

		return super.performCancel();
	}

	@Override
	protected void performDefaults() {

		final ColorDefinition[] graphColorDefinitions = GraphColorManager.getInstance().getGraphColorDefinitions();

		// update current colors
		for (final ColorDefinition graphDefinition : graphColorDefinitions) {

			graphDefinition.setNewGradientBright(graphDefinition.getDefaultGradientBright());
			graphDefinition.setNewGradientDark(graphDefinition.getDefaultGradientDark());
			graphDefinition.setNewLineColor(graphDefinition.getDefaultLineColor());
			graphDefinition.setNewTextColor(graphDefinition.getDefaultTextColor());

			final MapColor defaultLegendColor = graphDefinition.getDefaultMapColor();
			if (defaultLegendColor != null) {
				graphDefinition.setNewMapColor(defaultLegendColor.getCopy());
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

		for (final ColorDefinition graphDefinition : GraphColorManager.getInstance().getGraphColorDefinitions()) {

			graphDefinition.setNewGradientBright(graphDefinition.getGradientBright());
			graphDefinition.setNewGradientDark(graphDefinition.getGradientDark());
			graphDefinition.setNewLineColor(graphDefinition.getLineColor());
			graphDefinition.setNewTextColor(graphDefinition.getTextColor());

			graphDefinition.setNewMapColor(graphDefinition.getMapColor());
		}
	}

	private void updateAndSaveColors() {

		GraphColorManager.saveNewColors();

		// force to change the status
		TourbookPlugin.getDefault().getPreferenceStore()//
				.setValue(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED, Math.random());
	}

	private void updateColorsFromDialog(final ColorDefinition selectedColorDefinition, final MapColor newMapColor) {

		// set new legend color
		selectedColorDefinition.setNewMapColor(newMapColor);

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
