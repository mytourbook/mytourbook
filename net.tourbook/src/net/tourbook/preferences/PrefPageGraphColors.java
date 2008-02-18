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

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.colors.ColorDefinition;
import net.tourbook.colors.GraphColor;
import net.tourbook.colors.GraphColorDefaults;
import net.tourbook.colors.LegendColorDialog;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.util.TreeColumnLayout;

import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageGraphColors extends PreferencePage implements IWorkbenchPreferencePage, IColorTreeViewer {

	TreeViewer					fColorViewer;

	private ColorSelector		fColorSelector;

	private GraphColor			fSelectedColor;
	private boolean				fIsColorChanged;

	private ColorDefinition		fExpandedItem;

	private ColorLabelProvider	fColorLabelProvider;

	private LegendColorDialog	fLegendColorDialog;

	/**
	 * the color content provider has the following structure<br>
	 * 
	 * <pre>
	 * {@link ColorDefinition}
	 *    {@link GraphColor}
	 *    {@link GraphColor}
	 *    ...
	 *    {@link GraphColor}
	 *    
	 *    ...
	 *    
	 * {@link ColorDefinition}
	 *    {@link GraphColor}
	 *    {@link GraphColor}
	 *    ...
	 *    {@link GraphColor}
	 * </pre>
	 */
	private class ColorContentProvider implements ITreeContentProvider {

		public void dispose() {}

		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof ColorDefinition) {
				ColorDefinition graphDefinition = (ColorDefinition) parentElement;
				return graphDefinition.getGraphColorParts();
			}
			return null;
		}

		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof PrefPageGraphColors) {
				return GraphColorDefaults.getInstance().getGraphDefinitionList();
			}
			return null;
		}

		public Object getParent(Object element) {
			return null;
		}

		public boolean hasChildren(Object element) {
			if (element instanceof ColorDefinition) {
				return true;
			}
			return false;
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}

	}

	/**
	 * Create the color selection control.
	 * 
	 * @param parent
	 */
	private void createColorControl(Composite parent) {

		Composite container = new Composite(parent, SWT.NONE);

		GridLayout layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		container.setLayout(layout);

		GridData gd = new GridData();
		gd.verticalAlignment = SWT.TOP;
		gd.horizontalAlignment = SWT.LEAD;
		container.setLayoutData(gd);

		// container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));

		fColorSelector = new ColorSelector(container);
		fColorSelector.getButton().setLayoutData(new GridData());
		fColorSelector.setEnabled(false);
		setButtonLayoutData(fColorSelector.getButton());
		fColorSelector.addListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				onColorSelectorSelectColor(event);
			}
		});
	}

	/**
	 * create color objects for every graph definition
	 */
	private void createColorDefinitions() {

		for (ColorDefinition colorDefinition : GraphColorDefaults.getInstance().getGraphDefinitionList()) {

			ArrayList<GraphColor> graphColors = new ArrayList<GraphColor>();

			boolean isLegendColorAvailable = colorDefinition.getLegendColor() != null;

			for (int nameIndex = 0; nameIndex < GraphColorDefaults.colorNames.length; nameIndex++) {

				if (nameIndex == 3) {
					if (isLegendColorAvailable) {
						// create legend color
						graphColors.add(new GraphColor(colorDefinition,
								GraphColorDefaults.colorNames[nameIndex][0],
								GraphColorDefaults.colorNames[nameIndex][1],
								true));
					}
				} else {
					graphColors.add(new GraphColor(colorDefinition,
							GraphColorDefaults.colorNames[nameIndex][0],
							GraphColorDefaults.colorNames[nameIndex][1],
							false));
				}
			}

			colorDefinition.setColorNames(graphColors.toArray(new GraphColor[graphColors.size()]));
		}
	}

	private void createColorViewer(Composite parent) {

		// parent.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));

		GridData gd;
		GridLayout gl;

		// viewer container
		Composite viewerContainer = new Composite(parent, SWT.NONE);
		gl = new GridLayout();
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		viewerContainer.setLayout(gl);

		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		viewerContainer.setLayoutData(gd);

		// tree container
		Composite treeContainer = new Composite(viewerContainer, SWT.NONE);
		gl = new GridLayout();
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		treeContainer.setLayout(gl);

		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		treeContainer.setLayoutData(gd);

		TreeColumnLayout treeLayouter = new TreeColumnLayout();
		treeContainer.setLayout(treeLayouter);

		// color tree
		final Tree tree = new Tree(treeContainer, SWT.H_SCROLL | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		tree.setLinesVisible(false);

		// tree columns
		TreeColumn tc;

		tc = new TreeColumn(tree, SWT.NONE);
		tc.setText(Messages.Pref_ChartColors_Column_color);
		treeLayouter.addColumnData(new ColumnWeightData(30, true));

		tc = new TreeColumn(tree, SWT.NONE);
		treeLayouter.addColumnData(new ColumnWeightData(10, true));

		tc = new TreeColumn(tree, SWT.NONE);
		treeLayouter.addColumnData(new ColumnWeightData(10, true));

		tc = new TreeColumn(tree, SWT.NONE);
		treeLayouter.addColumnData(new ColumnWeightData(5, true));

		fColorViewer = new TreeViewer(tree);
		fColorViewer.setContentProvider(new ColorContentProvider());

		fColorLabelProvider = new ColorLabelProvider(this);
		fColorViewer.setLabelProvider(fColorLabelProvider);

		fColorViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				onColorViewerSelectColor();
			}
		});

		fColorViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {

				Object selection = ((IStructuredSelection) fColorViewer.getSelection()).getFirstElement();

				if (selection instanceof ColorDefinition) {

					// expand/collapse current item
					ColorDefinition treeItem = (ColorDefinition) selection;

					if (fColorViewer.getExpandedState(treeItem)) {
						fColorViewer.collapseToLevel(treeItem, 1);
					} else {
						if (fExpandedItem != null) {
							fColorViewer.collapseToLevel(fExpandedItem, 1);
						}
						fColorViewer.expandToLevel(treeItem, 1);
						fExpandedItem = treeItem;
					}
				} else if (selection instanceof GraphColor) {

					GraphColor graphColor = (GraphColor) selection;

					if (graphColor.isLegend()) {
						fLegendColorDialog.open();
					} else {
						fColorSelector.open();
					}
				}
			}
		});

		fColorViewer.addTreeListener(new ITreeViewerListener() {

			public void treeCollapsed(TreeExpansionEvent event) {

				if (event.getElement() instanceof ColorDefinition) {
					fExpandedItem = null;
				}
			}

			public void treeExpanded(TreeExpansionEvent event) {

				Object element = event.getElement();

				if (element instanceof ColorDefinition) {
					ColorDefinition treeItem = (ColorDefinition) element;

					if (fExpandedItem != null) {
						fColorViewer.collapseToLevel(fExpandedItem, 1);
					}
					fColorViewer.expandToLevel(treeItem, 1);
					fExpandedItem = treeItem;
				}
			}
		});
	}

	@Override
	protected Control createContents(Composite parent) {

		createColorDefinitions();

		// container
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout(2, false);
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		container.setLayout(gridLayout);

		Label label = new Label(container, SWT.WRAP);
		label.setText(Messages.Pref_ChartColors_Label_title);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);

		createColorViewer(container);
		createColorControl(container);

		fLegendColorDialog = new LegendColorDialog(Display.getCurrent().getActiveShell());

		fColorViewer.setInput(this);

		return container;
	}

	public TreeViewer getTreeViewer() {
		return fColorViewer;
	}

	public void init(IWorkbench workbench) {
		setPreferenceStore(TourbookPlugin.getDefault().getPreferenceStore());
	}

	/**
	 * is called when the color in the color selector has changed
	 * 
	 * @param event
	 */
	private void onColorSelectorSelectColor(PropertyChangeEvent event) {

		RGB oldValue = (RGB) event.getOldValue();
		RGB newValue = (RGB) event.getNewValue();

		if (!oldValue.equals(newValue) && fSelectedColor != null) {

			// color has changed

			// update the data model
			fSelectedColor.setNewRGB(newValue);

			/*
			 * dispose the old color/image from the graph
			 */
			fColorLabelProvider.disposeColor(fSelectedColor.getColorId(), fSelectedColor.getColorDefinition()
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
	private void onColorViewerSelectColor() {

		IStructuredSelection selection = (IStructuredSelection) fColorViewer.getSelection();

		if (selection.getFirstElement() instanceof GraphColor) {

			// graph color is selected

			GraphColor graphColor = (GraphColor) selection.getFirstElement();
			fSelectedColor = graphColor;

			if (graphColor.isLegend()) {

				// legend color is selected

			} else {

				// 'normal' color is selected

				// prepare color selector
				fColorSelector.setColorValue(graphColor.getNewRGB());
				fColorSelector.setEnabled(true);
			}

		} else {

			// color definition is selected

			fColorSelector.setEnabled(false);
		}
	}

	@Override
	public boolean performCancel() {
		resetColors();
		return super.performCancel();
	}

	@Override
	protected void performDefaults() {

		// update current colors
		for (ColorDefinition graphDefinition : GraphColorDefaults.getInstance().getGraphDefinitionList()) {

			graphDefinition.setNewGradientBright(graphDefinition.getDefaultGradientBright());
			graphDefinition.setNewGradientDark(graphDefinition.getDefaultGradientDark());
			graphDefinition.setNewLineColor(graphDefinition.getDefaultLineColor());
		}

		fColorLabelProvider.disposeGraphImages();
		fColorViewer.refresh();

		fIsColorChanged = true;

		super.performDefaults();
	}

	@Override
	public boolean performOk() {

		if (fIsColorChanged) {

			updatePrefColors();

			// update current colors
			for (ColorDefinition graphDefinition : GraphColorDefaults.getInstance().getGraphDefinitionList()) {

				graphDefinition.setGradientBright(graphDefinition.getNewGradientBright());
				graphDefinition.setGradientDark(graphDefinition.getNewGradientDark());
				graphDefinition.setLineColor(graphDefinition.getNewLineColor());
			}

			// force to change the status
			getPreferenceStore().setValue(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED, Math.random());
		}

		return super.performOk();
	}

	private void resetColors() {

		for (ColorDefinition graphDefinition : GraphColorDefaults.getInstance().getGraphDefinitionList()) {

			graphDefinition.setNewGradientBright(graphDefinition.getGradientBright());
			graphDefinition.setNewGradientDark(graphDefinition.getGradientDark());
			graphDefinition.setNewLineColor(graphDefinition.getLineColor());
		}
	}

	/**
	 * save the colors in the pref store
	 */
	private void updatePrefColors() {

		IPreferenceStore prefStore = getPreferenceStore();

		for (ColorDefinition graphDefinition : GraphColorDefaults.getInstance().getGraphDefinitionList()) {

			String prefGraphName = ITourbookPreferences.GRAPH_COLORS + graphDefinition.getPrefName() + "."; //$NON-NLS-1$

			PreferenceConverter.setValue(prefStore,
					prefGraphName + GraphColorDefaults.PREF_COLOR_BRIGHT,
					graphDefinition.getNewGradientBright());

			PreferenceConverter.setValue(prefStore,
					prefGraphName + GraphColorDefaults.PREF_COLOR_DARK,
					graphDefinition.getNewGradientDark());

			PreferenceConverter.setValue(prefStore,
					prefGraphName + GraphColorDefaults.PREF_COLOR_LINE,
					graphDefinition.getNewLineColor());
		}
	}

}
