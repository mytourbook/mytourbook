/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
import net.tourbook.colors.ColorDefinition;
import net.tourbook.colors.GraphColorItem;
import net.tourbook.colors.GraphColorProvider;
import net.tourbook.mapping.DialogMappingColor;
import net.tourbook.mapping.ILegendProvider;
import net.tourbook.mapping.LegendColor;
import net.tourbook.mapping.LegendConfig;
import net.tourbook.mapping.LegendProvider;
import net.tourbook.mapping.ValueColor;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.util.TreeColumnLayout;
 
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
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
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
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageAppearanceColors extends PreferencePage implements IWorkbenchPreferencePage, IColorTreeViewer {

	private static final List<Integer>	fUnitValues		= Arrays.asList(10, 50, 100, 150, 190);
	private static final List<String>	fUnitLabels		= Arrays.asList(Messages.Pref_ChartColors_unit_min,
																Messages.Pref_ChartColors_unit_low,
																Messages.Pref_ChartColors_unit_mid,
																Messages.Pref_ChartColors_unit_high,
																Messages.Pref_ChartColors_unit_max);

	private ValueColor[]				fValueColors	= new ValueColor[] {
			new ValueColor(10, 255, 0, 0),
			new ValueColor(50, 100, 100, 0),
			new ValueColor(100, 0, 255, 0),
			new ValueColor(150, 0, 100, 100),
			new ValueColor(190, 0, 0, 255)				};

	TreeViewer							fColorViewer;

	private ColorSelector				fColorSelector;
	private Button						fBtnLegend;

	private GraphColorItem				fSelectedColor;
	private boolean						fIsColorChanged;

	private ColorDefinition				fExpandedItem;

	private GraphColorLabelProvider		fColorLabelProvider;

	private LegendProvider				fLegendProvider;
	private DialogMappingColor			_dialogMappingColor;

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

		updateColorsFromDialog(fSelectedColor.getColorDefinition());
		updateAndSaveColors();
	}

	/**
	 * Create the color selection control.
	 * 
	 * @param parent
	 */
	private void createColorControl(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);

		final GridLayout layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		container.setLayout(layout);

		final GridData gd = new GridData();
		gd.verticalAlignment = SWT.TOP;
		gd.horizontalAlignment = SWT.LEAD;
		container.setLayoutData(gd);

		// container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));

		fColorSelector = new ColorSelector(container);
		fColorSelector.getButton().setLayoutData(new GridData());
		fColorSelector.setEnabled(false);
		setButtonLayoutData(fColorSelector.getButton());
		fColorSelector.addListener(new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent event) {
				onSelectColorInColorSelector(event);
			}
		});

		/*
		 * button: mapping color
		 */
		fBtnLegend = new Button(container, SWT.NONE);
		fBtnLegend.setText(Messages.Pref_ChartColors_btn_legend);
		setButtonLayoutData(fBtnLegend);

		fBtnLegend.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelectMappingColor();
			}
		});
		fBtnLegend.setEnabled(false);
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
						graphColors.add(new GraphColorItem(colorDefinition,
								GraphColorProvider.colorNames[nameIndex][0],
								GraphColorProvider.colorNames[nameIndex][1],
								true));
					}
				} else {
					graphColors.add(new GraphColorItem(colorDefinition,
							GraphColorProvider.colorNames[nameIndex][0],
							GraphColorProvider.colorNames[nameIndex][1],
							false));
				}
			}

			colorDefinition.setColorNames(graphColors.toArray(new GraphColorItem[graphColors.size()]));
		}
	}

	private void createColorViewer(final Composite parent) {

		// parent.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));

		GridData gd;
		GridLayout gl;

		// viewer container
		final Composite viewerContainer = new Composite(parent, SWT.NONE);
		gl = new GridLayout();
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		viewerContainer.setLayout(gl);

		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		viewerContainer.setLayoutData(gd);

		// tree container
		final Composite treeContainer = new Composite(viewerContainer, SWT.NONE);
		gl = new GridLayout();
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		treeContainer.setLayout(gl);

		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		treeContainer.setLayoutData(gd);

		final TreeColumnLayout treeLayouter = new TreeColumnLayout();
		treeContainer.setLayout(treeLayouter);

		// color tree
		final Tree tree = new Tree(treeContainer, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		tree.setLinesVisible(false);

		// tree columns
		TreeColumn tc;

		final int colorWidth = (tree.getItemHeight() + 5) * 4 + 0;

		tc = new TreeColumn(tree, SWT.NONE);
		tc.setText(Messages.Pref_ChartColors_Column_color);
		treeLayouter.addColumnData(new ColumnWeightData(30, true));

		tc = new TreeColumn(tree, SWT.NONE);
		treeLayouter.addColumnData(new ColumnPixelData(colorWidth, true));

		tc = new TreeColumn(tree, SWT.NONE);
		treeLayouter.addColumnData(new ColumnPixelData(colorWidth, true));

		tc = new TreeColumn(tree, SWT.NONE);
		treeLayouter.addColumnData(new ColumnPixelData(10, false));

		fColorViewer = new TreeViewer(tree);
		fColorViewer.setContentProvider(new ColorContentProvider());

		fColorLabelProvider = new GraphColorLabelProvider(this);
		fColorViewer.setLabelProvider(fColorLabelProvider);

		fColorViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				onSelectColorInColorViewer();
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

					final GraphColorItem graphColor = (GraphColorItem) selection;

					if (graphColor.isLegend()) {

						// legend color is selected

						onSelectMappingColor();

					} else {
						// open color selection dialog
						fColorSelector.open();
					}
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
					
					Display.getCurrent().asyncExec(new Runnable() {
						public void run() {
							fColorViewer.expandToLevel(treeItem, 1);
							fExpandedItem = treeItem;
						}
					});
				}
			}
		});
	}

	@Override
	protected Control createContents(final Composite parent) {

		createColorDefinitions();

		// container
		final Composite container = new Composite(parent, SWT.NONE);
		final GridLayout gridLayout = new GridLayout(2, false);
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		container.setLayout(gridLayout);

		createColorViewer(container);
		createColorControl(container);

		initializeLegend();

		fColorViewer.setInput(this);

		return container;
	}

	public ILegendProvider getLegendProvider() {
		return fLegendProvider;
	}

	public TreeViewer getTreeViewer() {
		return fColorViewer;
	}

	public void init(final IWorkbench workbench) {
		setPreferenceStore(TourbookPlugin.getDefault().getPreferenceStore());
	}

	/**
	 * setup legend
	 */
	private void initializeLegend() {

		final LegendConfig legendConfig = new LegendConfig();
		legendConfig.units = fUnitValues;
		legendConfig.unitLabels = fUnitLabels;
		legendConfig.legendMinValue = 0;
		legendConfig.legendMaxValue = 200;
		legendConfig.unitText = ""; //$NON-NLS-1$

		final LegendColor legendColor = new LegendColor();
		legendColor.valueColors = fValueColors;

		fLegendProvider = new LegendProvider(legendConfig, legendColor, 0);
		_dialogMappingColor = new DialogMappingColor(Display.getCurrent().getActiveShell(), fLegendProvider,this);
	}

	/**
	 * is called when the color in the color selector has changed
	 * 
	 * @param event
	 */
	private void onSelectColorInColorSelector(final PropertyChangeEvent event) {

		final RGB oldValue = (RGB) event.getOldValue();
		final RGB newValue = (RGB) event.getNewValue();

		if (!oldValue.equals(newValue) && fSelectedColor != null) {

			// color has changed

			// update the data model
			fSelectedColor.setNewRGB(newValue);

			/*
			 * dispose the old color/image from the graph
			 */
			fColorLabelProvider.disposeResources(fSelectedColor.getColorId(), fSelectedColor.getColorDefinition()
					.getImageId());

			/*
			 * update the tree viewer, the color images will then be recreated
			 */
			fColorViewer.update(fSelectedColor, null);
			fColorViewer.update(fSelectedColor.getColorDefinition(), null);

			fIsColorChanged = true;
		}
	}

	/**
	 * is called when the color in the color viewer was selected
	 */
	private void onSelectColorInColorViewer() {

		final IStructuredSelection selection = (IStructuredSelection) fColorViewer.getSelection();

		fBtnLegend.setEnabled(false);
		fColorSelector.setEnabled(false);

		if (selection.getFirstElement() instanceof GraphColorItem) {

			// graph color is selected

			final GraphColorItem graphColor = (GraphColorItem) selection.getFirstElement();

			// keep selected color
			fSelectedColor = graphColor;

			if (graphColor.isLegend()) {

				// legend color is selected

				fBtnLegend.setEnabled(true);

			} else {

				// 'normal' color is selected

				// prepare color selector
				fColorSelector.setColorValue(graphColor.getNewRGB());
				fColorSelector.setEnabled(true);
			}

		} else {

			// color definition is selected

		}
	}

	/**
	 * modify the colors of the legend
	 */
	private void onSelectMappingColor() {

		final ColorDefinition selectedColorDefinition = fSelectedColor.getColorDefinition();

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

		fColorLabelProvider.disposeGraphImages();
		fColorViewer.refresh();

		fIsColorChanged = true;

		super.performDefaults();
	}

	@Override
	public boolean performOk() {

		if (fIsColorChanged) {
			updateAndSaveColors();
		}

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

			PreferenceConverter.setValue(prefStore, prefGraphName + GraphColorProvider.PREF_COLOR_DARK, graphDefinition
					.getNewGradientDark());

			PreferenceConverter.setValue(prefStore, prefGraphName + GraphColorProvider.PREF_COLOR_LINE, graphDefinition
					.getNewLineColor());
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
		fColorLabelProvider.disposeResources(fSelectedColor.getColorId(), selectedColorDefinition.getImageId());

		/*
		 * update the tree viewer, the color images will be recreated
		 */
		fColorViewer.update(fSelectedColor, null);
		fColorViewer.update(selectedColorDefinition, null);

		fIsColorChanged = true;
	}

}
