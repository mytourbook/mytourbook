/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.color.IMapColorProvider;
import net.tourbook.common.color.Map3ColorDefinition;
import net.tourbook.common.color.Map3ColorManager;
import net.tourbook.common.color.Map3ColorProfile;
import net.tourbook.common.color.Map3GradientColorProvider;
import net.tourbook.common.color.MapGraphId;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.TreeColumnDefinition;
import net.tourbook.map.MapColorProvider;
import net.tourbook.map3.ui.DialogMap3ColorEditor;
import net.tourbook.map3.ui.IMap3ColorUpdater;
import net.tourbook.ui.UI;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageMap3Color extends PreferencePage implements IWorkbenchPreferencePage, IMap3ColorUpdater,
		ITourViewer {

	private final IPreferenceStore	_prefStore	= TourbookPlugin.getDefault().getPreferenceStore();
	private final IDialogSettings	_state		= TourbookPlugin.getDefault().getDialogSettingsSection(
														this.getClass().getName());

	private TreeViewer				_colorProfileViewer;
	private ColumnManager			_columnManager;

	private PixelConverter			_pc;

	private boolean					_isTreeExpading;

	/*
	 * UI controls
	 */
	private Composite				_viewerContainer;

	private Button					_btnDuplicateProfile;
	private Button					_btnEditProfile;
	private Button					_btnNewProfile;
	private Button					_btnRemoveProfile;

	/**
	 * the color content provider has the following structure<br>
	 * 
	 * <pre>
	 * {@link Map3ColorDefinition}
	 *    {@link Map3ColorProfile}
	 *    {@link Map3ColorProfile}
	 *    ...
	 *    {@link Map3ColorProfile}
	 * 
	 *    ...
	 * 
	 * {@link Map3ColorDefinition}
	 *    {@link Map3ColorProfile}
	 *    {@link Map3ColorProfile}
	 *    ...
	 *    {@link Map3ColorProfile}
	 * </pre>
	 */
	private static class ColorContentProvider implements ITreeContentProvider {

		public void dispose() {}

		public Object[] getChildren(final Object parentElement) {
			if (parentElement instanceof Map3ColorDefinition) {

				final ArrayList<Map3ColorProfile> colorProfiles = ((Map3ColorDefinition) parentElement)
						.getColorProfiles();

				return colorProfiles.toArray(new Map3ColorProfile[colorProfiles.size()]);
			}

			return null;
		}

		public Object[] getElements(final Object inputElement) {

			if (inputElement instanceof PrefPageMap3Color) {

				final ArrayList<Map3ColorDefinition> colorDefinitions = Map3ColorManager.getSortedColorDefinitions();

				return colorDefinitions.toArray(new Map3ColorDefinition[colorDefinitions.size()]);
			}

			return null;
		}

		public Object getParent(final Object element) {
			return null;
		}

		public boolean hasChildren(final Object element) {

			if (element instanceof Map3ColorDefinition) {
				return true;
			}

			return false;
		}

		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {

//			System.out.println(System.currentTimeMillis() + " [" + getClass().getSimpleName() + "] \tinputchanged");
//			// remove SYSTEM.OUT.PRINTLN
		}
	}

	public class ContentComparator extends ViewerComparator {

		@Override
		public int compare(final Viewer viewer, final Object e1, final Object e2) {

			if (e1 instanceof Map3ColorProfile && e2 instanceof Map3ColorProfile) {

				// compare color profiles by name

				final Map3ColorProfile p1 = (Map3ColorProfile) e1;
				final Map3ColorProfile p2 = (Map3ColorProfile) e2;

				return p1.getProfileName().compareTo(p2.getProfileName());
			}

			return 0;
		}

	}

	public PrefPageMap3Color() {

		noDefaultAndApplyButton();
	}

	@Override
	public void applyMapColors(	final Map3ColorProfile originalProfile,
								final Map3ColorProfile modifiedProfile,
								final boolean isNewProfile) {

		final MapGraphId originalGraphId = originalProfile.getGraphId();
		final MapGraphId modifiedGraphId = modifiedProfile.getGraphId();

		// update color provider
		final IMapColorProvider colorProvider = MapColorProvider.getMap3ColorProvider(modifiedGraphId);
		if (colorProvider instanceof Map3GradientColorProvider) {
			((Map3GradientColorProvider) colorProvider).setColorProfile(modifiedProfile);
		} else {
			return;
		}

		// update model
		if (isNewProfile) {

			// a new profile is edited

			// get all color profiles for the modified graph id
			final ArrayList<Map3ColorProfile> colorProfiles = Map3ColorManager.getColorProfiles(modifiedProfile
					.getGraphId());

			colorProfiles.add(modifiedProfile);

		} else {

			// an existing profile is modified

			Map3ColorManager.replaceColorProfile(originalProfile, modifiedProfile);
		}

		// update UI
		_colorProfileViewer.refresh(Map3ColorManager.getColorDefinition(originalGraphId));

		if (originalGraphId != modifiedGraphId) {

			// both color definitions are modified
			_colorProfileViewer.refresh(Map3ColorManager.getColorDefinition(modifiedGraphId));
		}

		Map3ColorManager.saveColors();

		// force to change the status
		TourbookPlugin.getDefault().getPreferenceStore()//
				.setValue(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED, Math.random());
	}

	@Override
	protected Control createContents(final Composite parent) {

		_pc = new PixelConverter(parent);

		final Composite ui = createUI(parent);

		restoreState();

		enableControls();

		reloadViewer();

		// expand all for doing easier navigation when only the default profiles are defined
		_colorProfileViewer.expandAll();

		return ui;
	}

	private Composite createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
//			final Composite testContainer = new Composite(container, SWT.NONE);
//			GridDataFactory.fillDefaults().grab(true, true).applyTo(testContainer);
//			GridLayoutFactory.fillDefaults().numColumns(1).applyTo(testContainer);
//			testContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
//			{
//				final Label label = new Label(testContainer, SWT.NONE);
//				GridDataFactory.fillDefaults().applyTo(label);
//				label.setText("1");
//
//
//				final Label label2 = new Label(testContainer, SWT.NONE);
//				GridDataFactory.fillDefaults().applyTo(label2);
//				label2.setText("2");
//
//			}
			createUI_10_ColorViewer(container);

			createUI_20_Actions(container);
		}

		return container;
	}

	private void createUI_10_ColorViewer(final Composite parent) {

		// define all columns for the viewer
		_columnManager = new ColumnManager(this, _state);
		defineAllColumns();

		_viewerContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
//				.hint(200, 100)
				.applyTo(_viewerContainer);
		GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
//		_viewerContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
//		_viewerContainer.setLayout(new TreeColumnLayout());
		{
			createUI_12_ColorViewer(_viewerContainer);
		}
	}

	private void createUI_12_ColorViewer(final Composite parent) {

		/*
		 * Create tree
		 */
		final Tree tree = new Tree(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
//		GridDataFactory.fillDefaults().grab(true, true).applyTo(tree);
//		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

//		tree.setLayout(new TreeColumnLayout());

		GridDataFactory.fillDefaults().grab(true, true).applyTo(tree);

		tree.setHeaderVisible(true);
		tree.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));
//		tree.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));

		/*
		 * Create tree viewer
		 */
		_colorProfileViewer = new TreeViewer(tree);
		_columnManager.createColumns(_colorProfileViewer);

		_colorProfileViewer.setContentProvider(new ColorContentProvider());
		_colorProfileViewer.setComparator(new ContentComparator());

		_colorProfileViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				onSelectColorViewer();
			}
		});

		_colorProfileViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(final DoubleClickEvent event) {
				onDoubleClickColorViewer();
			}

		});
	}

	private void createUI_20_Actions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults().applyTo(container);
		{

			{
				/*
				 * Button: New
				 */
				_btnNewProfile = new Button(container, SWT.NONE);
				_btnNewProfile.setText(Messages.App_Action_New);
				_btnNewProfile.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onActionAddProfile();
					}
				});
				setButtonLayoutData(_btnNewProfile);
			}

			{
				/*
				 * button: Edit
				 */
				_btnEditProfile = new Button(container, SWT.NONE);
				_btnEditProfile.setText(Messages.App_Action_Edit);
				_btnEditProfile.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onActionEditProfile();
					}
				});
				setButtonLayoutData(_btnEditProfile);
			}

			{
				/*
				 * Button: Duplicate
				 */
				_btnDuplicateProfile = new Button(container, SWT.NONE);
				_btnDuplicateProfile.setText(Messages.App_Action_Duplicate);
				_btnDuplicateProfile.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onActionDuplicateProfile();
					}
				});
				setButtonLayoutData(_btnDuplicateProfile);
			}

			{
				/*
				 * Button: Remove
				 */
				_btnRemoveProfile = new Button(container, SWT.NONE);
				_btnRemoveProfile.setText(Messages.App_Action_Remove);
				_btnRemoveProfile.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onActionRemoveProfile();
					}
				});
				setButtonLayoutData(_btnRemoveProfile);
			}

			{
				/*
				 * Button: Columns
				 */
				final Button btnAdjustColumns = new Button(container, SWT.NONE);
				btnAdjustColumns.setText(Messages.App_Action_Columns);
				btnAdjustColumns.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
//					_columnManager.openColumnDialog();
					}
				});
				setButtonLayoutData(btnAdjustColumns);
				final GridData gd = (GridData) btnAdjustColumns.getLayoutData();
				gd.verticalIndent = 20;
			}
		}
	}

	private void defineAllColumns() {

		defineColumn_ProfileName();
	}

	/**
	 * column: profile name
	 */
	private void defineColumn_ProfileName() {

		final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, "profileName", SWT.LEAD); //$NON-NLS-1$

//		colDef.setColumnLabel(Messages.profileViewer_column_label_name);
//		colDef.setColumnHeader(Messages.profileViewer_column_label_name_header);
//		colDef.setColumnToolTipText(Messages.profileViewer_column_label_name_tooltip);
		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(20));
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				if (element instanceof Map3ColorDefinition) {
					cell.setText(((Map3ColorDefinition) (element)).getVisibleName());
				} else if (element instanceof Map3ColorProfile) {
					cell.setText(((Map3ColorProfile) (element)).getProfileName());
				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});
	}

	private void enableControls() {

		final IStructuredSelection selection = (IStructuredSelection) _colorProfileViewer.getSelection();

		final Object firstSelectedItem = selection.getFirstElement();

		boolean isColorProfileSelected = false;
		boolean canRemoveProfiles = false;

		if (firstSelectedItem instanceof Map3ColorProfile) {

			final Map3ColorProfile colorProfile = (Map3ColorProfile) firstSelectedItem;

			isColorProfileSelected = true;

			final MapGraphId graphId = colorProfile.getGraphId();
			final ArrayList<Map3ColorProfile> colorProfiles = Map3ColorManager.getColorProfiles(graphId);

			// profiles can only be removed when more than one profile is available for a graph type
			canRemoveProfiles = colorProfiles.size() > 1;
		}

		_btnEditProfile.setEnabled(isColorProfileSelected);
		_btnDuplicateProfile.setEnabled(isColorProfileSelected);
		_btnRemoveProfile.setEnabled(canRemoveProfiles);
	}

	private void expandCollapseTreeItem(final Map3ColorDefinition treeItem) {

		if (_isTreeExpading) {

			// prevent runtime exception: Ignored reentrant call while viewer is busy.
			return;
		}

		if (_colorProfileViewer.getExpandedState(treeItem)) {

			_colorProfileViewer.collapseToLevel(treeItem, 1);

		} else {

			_colorProfileViewer.expandToLevel(treeItem, 1);

			// expanding the treeangle, the layout is correctly done but not with double click
			_viewerContainer.layout(true, true);
		}
	}

	@Override
	public ColumnManager getColumnManager() {
		return _columnManager;
	}

	@Override
	public ColumnViewer getViewer() {
		return _colorProfileViewer;
	}

	public void init(final IWorkbench workbench) {}

	private void onActionAddProfile() {

		// get graph id from currently selected item
		final Object selection = ((IStructuredSelection) _colorProfileViewer.getSelection()).getFirstElement();
		MapGraphId graphId = MapGraphId.Altitude;

		if (selection instanceof Map3ColorDefinition) {
			graphId = ((Map3ColorDefinition) selection).getGraphId();
		} else if (selection instanceof Map3ColorProfile) {
			graphId = ((Map3ColorProfile) selection).getGraphId();
		}

		final Map3ColorProfile newColorProfile = Map3ColorManager.getDefaultColorProfile(graphId);

		// set profile name
		newColorProfile.setProfileName(Map3ColorProfile.PROFILE_NAME_NEW);

		new DialogMap3ColorEditor(//
				Display.getCurrent().getActiveShell(),
				newColorProfile,
				this,
				true).open();
	}

	private void onActionDuplicateProfile() {

		// get graph id from currently selected item
		final Object selection = ((IStructuredSelection) _colorProfileViewer.getSelection()).getFirstElement();

		if ((selection instanceof Map3ColorProfile) == false) {
			return;
		}

		final Map3ColorProfile selectedProfile = (Map3ColorProfile) selection;

		final Map3ColorProfile duplicatedProfile = selectedProfile.clone();

		// create a profile name
		duplicatedProfile.setProfileName(//
				selectedProfile.getProfileName() + UI.SPACE + duplicatedProfile.getProfileId());

		new DialogMap3ColorEditor(//
				Display.getCurrent().getActiveShell(),
				duplicatedProfile,
				this,
				true).open();
	}

	private void onActionEditProfile() {

		final Object firstElement = ((StructuredSelection) _colorProfileViewer.getSelection()).getFirstElement();
		if (firstElement instanceof Map3ColorProfile) {

			final Map3ColorProfile originalProfile = (Map3ColorProfile) firstElement;

			new DialogMap3ColorEditor(//
					Display.getCurrent().getActiveShell(),
					originalProfile,
					this,
					false).open();
		}
	}

	private void onActionRemoveProfile() {

		final Object firstElement = ((StructuredSelection) _colorProfileViewer.getSelection()).getFirstElement();
		if (firstElement instanceof Map3ColorProfile) {

			if (MessageDialog.openQuestion(
					Display.getCurrent().getActiveShell(),
					Messages.Pref_Map3Color_Dialog_RemoveProfile_Title,
					Messages.Pref_Map3Color_Dialog_RemoveProfile_Message)) {

				final Map3ColorProfile selectedProfile = (Map3ColorProfile) firstElement;

//				Map3ColorManager.removeColorProfile(selectedProfile);
			}
		}
	}

	private void onDoubleClickColorViewer() {

		final Object selection = ((IStructuredSelection) _colorProfileViewer.getSelection()).getFirstElement();

		if (selection instanceof Map3ColorDefinition) {

			// expand/collapse current item

			expandCollapseTreeItem((Map3ColorDefinition) selection);

		} else if (selection instanceof Map3ColorProfile) {

			// edit selected color

			onActionEditProfile();
		}
	}

	/**
	 * Is called when acolor in the color viewer is selected.
	 */
	private void onSelectColorViewer() {

		enableControls();
	}

	@Override
	public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

		_viewerContainer.setRedraw(false);
		{
			_colorProfileViewer.getTree().dispose();

			createUI_12_ColorViewer(_viewerContainer);
			_viewerContainer.layout();

			// update the viewer
			reloadViewer();
		}
		_viewerContainer.setRedraw(true);

		return _colorProfileViewer;
	}

	@Override
	public void reloadViewer() {

		_colorProfileViewer.setInput(this);
	}

	private void restoreState() {

	}

	private void saveState() {

	}

}
