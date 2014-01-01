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
import java.util.HashMap;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.color.IMapColorProvider;
import net.tourbook.common.color.Map3ColorDefinition;
import net.tourbook.common.color.Map3ColorManager;
import net.tourbook.common.color.Map3ColorProfile;
import net.tourbook.common.color.Map3GradientColorProvider;
import net.tourbook.common.color.MapGraphId;
import net.tourbook.common.color.RGBVertex;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.TreeColumnDefinition;
import net.tourbook.common.util.Util;
import net.tourbook.map.MapColorProvider;
import net.tourbook.map2.view.TourMapPainter;
import net.tourbook.map3.ui.DialogMap3ColorEditor;
import net.tourbook.map3.ui.IMap3ColorUpdater;
import net.tourbook.ui.UI;
import net.tourbook.ui.action.ActionCollapseAll;
import net.tourbook.ui.action.ActionExpandAll;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageMap3Color extends PreferencePage implements IWorkbenchPreferencePage, IMap3ColorUpdater,
		ITourViewer {

	private static final int							PROFILE_IMAGE_HEIGHT				= 30;
	private static final int							PROFILE_IMAGE_MIN_SIZE				= 30;
	private static final int							PROFILE_IMAGE_X_OFFSET				= 10;

	private static final String							STATE_EXPANDED_COLOR_DEFINITIONS	= "STATE_EXPANDED_COLOR_DEFINITIONS";				//$NON-NLS-1$

	private final IPreferenceStore						_prefStore							= TourbookPlugin
																									.getPrefStore();

	private final IDialogSettings						_state								= TourbookPlugin
																									.getState(this
																											.getClass()
																											.getName());

	private CheckboxTreeViewer							_colorProfileViewer;
	private ColumnManager								_columnManager;

	private PixelConverter								_pc;

	private boolean										_isTreeExpading;

	private int											_defaultImageWidth					= 300;
	private int											_oldImageWidth						= -1;

	/**
	 * contains the table column widget for the profile color
	 */
	private TreeColumn									_tcProfileImage;

	private TreeColumnDefinition						_colDefImage;

	/**
	 * index of the profile image, this can be changed when the columns are reordered with the mouse
	 * or the column manager
	 */
	private int											_profileImageColumn					= 0;

	private HashMap<Map3GradientColorProvider, Image>	_profileImages						= new HashMap<Map3GradientColorProvider, Image>();

	/*
	 * UI controls
	 */
	private Composite									_viewerContainer;

	private Button										_btnDuplicateProfile;
	private Button										_btnEditProfile;
	private Button										_btnNewProfile;
	private Button										_btnRemoveProfile;
	private ToolBar										_toolBar;

	/**
	 * the color content provider has the following structure<br>
	 * 
	 * <pre>
	 * {@link Map3ColorDefinition}
	 *    {@link Map3GradientColorProvider}
	 *    {@link Map3GradientColorProvider}
	 *    ...
	 *    {@link Map3GradientColorProvider}
	 * 
	 *    ...
	 * 
	 * {@link Map3ColorDefinition}
	 *    {@link Map3GradientColorProvider}
	 *    {@link Map3GradientColorProvider}
	 *    ...
	 *    {@link Map3GradientColorProvider}
	 * </pre>
	 */
	private static class ContentProvider implements ITreeContentProvider {

		public void dispose() {}

		public Object[] getChildren(final Object parentElement) {
			if (parentElement instanceof Map3ColorDefinition) {

				final Map3ColorDefinition colorDef = (Map3ColorDefinition) parentElement;

				final ArrayList<Map3GradientColorProvider> colorProvider = colorDef.getColorProviders();

				return colorProvider.toArray(new Map3GradientColorProvider[colorProvider.size()]);
			}

			return null;
		}

		public Object[] getElements(final Object inputElement) {

			if (inputElement instanceof PrefPageMap3Color) {
				return getColorDefinitions();
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

		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	private class ProfileComparator extends ViewerComparator {

		@Override
		public int compare(final Viewer viewer, final Object c1, final Object c2) {

			if (c1 instanceof Map3GradientColorProvider && c2 instanceof Map3GradientColorProvider) {

				// compare color profiles by name

				final Map3GradientColorProvider cp1 = (Map3GradientColorProvider) c1;
				final Map3GradientColorProvider cp2 = (Map3GradientColorProvider) c2;

				return cp1.getMap3ColorProfile().getProfileName().compareTo(cp2.getMap3ColorProfile().getProfileName());
			}

			return 0;
		}
	}

	public PrefPageMap3Color() {

		noDefaultAndApplyButton();
	}

	private static Object[] getColorDefinitions() {

		final ArrayList<Map3ColorDefinition> colorDefinitions = Map3ColorManager.getSortedColorDefinitions();

		return colorDefinitions.toArray(new Map3ColorDefinition[colorDefinitions.size()]);
	}

	@Override
	public void applyMapColors(	final Map3GradientColorProvider originalColorProvider,
								final Map3GradientColorProvider modifiedColorProvider,
								final boolean isNewProfile) {

		final MapGraphId originalGraphId = originalColorProvider.getGraphId();
		final MapGraphId modifiedGraphId = modifiedColorProvider.getGraphId();

		// update color provider
		final IMapColorProvider colorProvider = MapColorProvider.getMap3ColorProvider(modifiedGraphId);

		if (colorProvider instanceof Map3GradientColorProvider) {

			final Map3GradientColorProvider map3GradientColorProvider = (Map3GradientColorProvider) colorProvider;

			map3GradientColorProvider.setColorProfile(modifiedColorProvider.getColorProfile());

		} else {
			return;
		}

		// update model
		if (isNewProfile) {

			// a new profile is edited

			Map3ColorManager.addColorProvider(modifiedColorProvider);

		} else {

			// an existing profile is modified

			Map3ColorManager.replaceColorProvider(originalColorProvider, modifiedColorProvider);
		}

		// update UI
		_colorProfileViewer.refresh(Map3ColorManager.getColorDefinition(originalGraphId));

		if (originalGraphId != modifiedGraphId) {

			// both color definitions are modified
			_colorProfileViewer.refresh(Map3ColorManager.getColorDefinition(modifiedGraphId));
		}
		_colorProfileViewer.setSelection(new StructuredSelection(modifiedColorProvider));

		Map3ColorManager.saveColors();

		// fire event that color has changed
		TourbookPlugin.getDefault().getPreferenceStore()//
				.setValue(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED, Math.random());
	}

	@Override
	protected Control createContents(final Composite parent) {

		_pc = new PixelConverter(parent);

		final Composite ui = createUI(parent);
		fillToolbar();

		restoreState();
		enableControls();

		reloadViewer();
		restoreStateViewer();

		// expand all for doing easier navigation when only the default profiles are defined
//		_colorProfileViewer.expandAll();

		return ui;
	}

	/**
	 * create the views context menu
	 */
	private void createContextMenu() {

		final MenuManager menuMgr = new MenuManager();

		menuMgr.setRemoveAllWhenShown(true);

		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(final IMenuManager menuMgr2) {
//				fillContextMenu(menuMgr2);
			}
		});

		final Tree tree = _colorProfileViewer.getTree();
		final Menu treeContextMenu = menuMgr.createContextMenu(tree);

		tree.setMenu(treeContextMenu);

		_columnManager.createHeaderContextMenu(tree, treeContextMenu);
	}

	private Composite createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.spacing(LayoutConstants.getSpacing().x, 0)
				.numColumns(2)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			createUI_10_Title(container);

			createUI_20_ColorViewer(container);
			createUI_50_Actions(container);
		}

		return container;
	}

	private void createUI_10_Title(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			// label: title
			final Label label = new Label(container, SWT.WRAP);
			label.setText(Messages.Pref_Map3Color_Label_Title);
			GridDataFactory.fillDefaults()//
					.grab(true, true)
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(label);

			// toolbar
			_toolBar = new ToolBar(container, SWT.FLAT);
		}

		// spacer
		new Label(parent, SWT.NONE);
	}

	private void createUI_20_ColorViewer(final Composite parent) {

		// define all columns for the viewer
		_columnManager = new ColumnManager(this, _state);
		defineAllColumns();

		_viewerContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.applyTo(_viewerContainer);
		GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
//		_viewerContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			createUI_22_ColorViewer(_viewerContainer);
		}
	}

	private void createUI_22_ColorViewer(final Composite parent) {

		/*
		 * Create tree
		 */
		final Tree tree = new Tree(parent, SWT.CHECK | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tree);

		tree.setHeaderVisible(true);
		tree.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));
//		tree.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));

		/*
		 * NOTE: MeasureItem, PaintItem and EraseItem are called repeatedly. Therefore, it is
		 * critical for performance that these methods be as efficient as possible.
		 */
		final Listener paintListener = new Listener() {
			public void handleEvent(final Event event) {

				if (event.index == _profileImageColumn) {

					// paint image at image column

					if (event.type == SWT.MeasureItem || event.type == SWT.PaintItem) {

						onViewerPaint(event);
					}
				}
			}
		};
		tree.addListener(SWT.MeasureItem, paintListener);
		tree.addListener(SWT.PaintItem, paintListener);

		/*
		 * Create tree viewer
		 */
		_colorProfileViewer = new CheckboxTreeViewer(tree);
		_columnManager.createColumns(_colorProfileViewer);

		_tcProfileImage = _colDefImage.getTreeColumn();
		_profileImageColumn = _colDefImage.getCreateIndex();

		_colorProfileViewer.setContentProvider(new ContentProvider());
		_colorProfileViewer.setComparator(new ProfileComparator());

		_colorProfileViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				onViewerSelectColor();
			}
		});

		_colorProfileViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(final DoubleClickEvent event) {
				onViewerDoubleClick();
			}
		});

		_colorProfileViewer.setCheckStateProvider(new ICheckStateProvider() {

			@Override
			public boolean isChecked(final Object element) {
				return onViewerIsChecked(element);
			}

			@Override
			public boolean isGrayed(final Object element) {
				return onViewerIsGrayed(element);
			}
		});

		_colorProfileViewer.addCheckStateListener(new ICheckStateListener() {

			@Override
			public void checkStateChanged(final CheckStateChangedEvent event) {
				onViewerCheckStateChange(event);
			}
		});

		createContextMenu();
	}

	private void createUI_50_Actions(final Composite parent) {

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
						_columnManager.openColumnDialog();
					}
				});
				setButtonLayoutData(btnAdjustColumns);
				final GridData gd = (GridData) btnAdjustColumns.getLayoutData();
				gd.verticalIndent = 20;
			}
		}
	}

	private void defineAllColumns() {

		defineColumn_10_ProfileName();
		defineColumn_20_ColorImage();
		defineColumn_99_ProfileId();
	}

	/**
	 * column: profile name
	 */
	private void defineColumn_10_ProfileName() {

		final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, "profileName", SWT.LEAD); //$NON-NLS-1$

		colDef.setColumnLabel(Messages.Pref_Map3Color_Column_ProfileName);
		colDef.setColumnHeader(Messages.Pref_Map3Color_Column_ProfileName);
		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(20));
		colDef.setIsDefaultColumn();
		colDef.setIsColumnMoveable(false);
		colDef.setCanModifyVisibility(false);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				if (element instanceof Map3ColorDefinition) {
					cell.setText(((Map3ColorDefinition) (element)).getVisibleName());
				} else if (element instanceof Map3GradientColorProvider) {
					cell.setText(((Map3GradientColorProvider) (element)).getMap3ColorProfile().getProfileName());
				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});
	}

	/**
	 * Column: Color image
	 */
	private void defineColumn_20_ColorImage() {

		final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, "colorImage", SWT.LEAD); //$NON-NLS-1$
		_colDefImage = colDef;

		colDef.setColumnLabel(Messages.Pref_Map3Color_Column_Colors);
		colDef.setColumnHeader(Messages.Pref_Map3Color_Column_Colors);
		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(30));
		colDef.setIsDefaultColumn();
		colDef.setIsColumnMoveable(false);
		colDef.setCanModifyVisibility(false);
		colDef.setLabelProvider(new CellLabelProvider() {

			// !!! set dummy label provider, otherwise an error occures !!!
			@Override
			public void update(final ViewerCell cell) {}
		});

		colDef.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				onResizeImageColumn();
			}
		});
	}

	/**
	 * column: id
	 */
	private void defineColumn_99_ProfileId() {

		final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, "profileId", SWT.LEAD); //$NON-NLS-1$

//		profileViewer_column_label_id                     = internal ID
//		profileViewer_column_label_id_header              = ID
//		profileViewer_column_label_id_tooltip             = internal unique profile Id

		colDef.setColumnLabel(Messages.Pref_Map3Color_Column_Id_Label);
		colDef.setColumnHeader(Messages.Pref_Map3Color_Column_Id_Header);
		colDef.setColumnToolTipText(Messages.Pref_Map3Color_Column_Id_Tooltip);
		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(10));

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				if (element instanceof Map3ColorDefinition) {

					cell.setText(((Map3ColorDefinition) (element)).getGraphId().name());

				} else if (element instanceof Map3GradientColorProvider) {

					cell.setText(Integer.toString(((Map3GradientColorProvider) (element))
							.getMap3ColorProfile()
							.getProfileId()));

				} else {

					cell.setText(UI.EMPTY_STRING);
				}
			}
		});
	}

	@Override
	public void dispose() {

		disposeProfileImages();

		super.dispose();
	}

	private void disposeProfileImages() {

		for (final Image profileImage : _profileImages.values()) {
			profileImage.dispose();
		}

		_profileImages.clear();
	}

	private void enableControls() {

		final IStructuredSelection selection = (IStructuredSelection) _colorProfileViewer.getSelection();

		final Object firstSelectedItem = selection.getFirstElement();

		boolean isColorProfileSelected = false;
		boolean canRemoveProfiles = false;

		if (firstSelectedItem instanceof Map3GradientColorProvider) {

			final Map3GradientColorProvider colorProvider = (Map3GradientColorProvider) firstSelectedItem;

			isColorProfileSelected = true;

			final MapGraphId graphId = colorProvider.getGraphId();

			// profiles can only be removed when more than one profile is available for a graph type
			final ArrayList<Map3GradientColorProvider> graphIdColorProviders = Map3ColorManager
					.getColorProviders(graphId);
			canRemoveProfiles = graphIdColorProviders.size() > 1;
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

	/**
	 * set the toolbar action after the {@link #_tagViewer} is created
	 */
	private void fillToolbar() {

		final ToolBarManager tbm = new ToolBarManager(_toolBar);

		tbm.add(new ActionExpandAll(this));
		tbm.add(new ActionCollapseAll(this));

		tbm.update(true);
	}

	@Override
	public ColumnManager getColumnManager() {
		return _columnManager;
	}

	private int getImageSize() {

		int width;

		if (_tcProfileImage == null) {
			width = _defaultImageWidth;
		} else {
			width = _tcProfileImage.getWidth();
		}

		// ensure min size
		if (width < PROFILE_IMAGE_MIN_SIZE) {
			width = PROFILE_IMAGE_MIN_SIZE;
		}

		return width + PROFILE_IMAGE_X_OFFSET;
	}

	private Image getProfileImage(final Map3GradientColorProvider colorProvider) {

		Image image = _profileImages.get(colorProvider);

		if (isProfileImageValid(image) == false) {

			final int imageSize = getImageSize();
			final int legendSize = imageSize - PROFILE_IMAGE_X_OFFSET;

			final Map3ColorProfile colorProfile = colorProvider.getMap3ColorProfile();
			final ArrayList<RGBVertex> rgbVertices = colorProfile.getProfileImage().getRgbVertices();

			colorProvider.configureColorProvider(legendSize, rgbVertices);

			image = TourMapPainter.createMapLegendImage(//
					Display.getCurrent(),
					colorProvider,
					legendSize,
					PROFILE_IMAGE_HEIGHT);

			final Image oldImage = _profileImages.put(colorProvider, image);

			Util.disposeResource(oldImage);
		}

		return image;
	}

	@Override
	public ColumnViewer getViewer() {
		return _colorProfileViewer;
	}

	public void init(final IWorkbench workbench) {}

	/**
	 * @param image
	 * @return Returns <code>true</code> when the image is valid, returns <code>false</code> when
	 *         the profile image must be created,
	 */
	private boolean isProfileImageValid(final Image image) {

		if (image == null || image.isDisposed()) {

			return false;

		}

		if (image.getBounds().width != getImageSize()) {

			image.dispose();

			return false;
		}

		return true;
	}

	@Override
	public boolean okToLeave() {

		saveState();

		return super.okToLeave();
	}

	private void onActionAddProfile() {

		// get graph id from currently selected item
		final Object selection = ((IStructuredSelection) _colorProfileViewer.getSelection()).getFirstElement();
		MapGraphId graphId = MapGraphId.Altitude;

		if (selection instanceof Map3ColorDefinition) {

			graphId = ((Map3ColorDefinition) selection).getGraphId();

		} else if (selection instanceof Map3GradientColorProvider) {

			graphId = ((Map3GradientColorProvider) selection).getGraphId();
		}

		final Map3ColorProfile newColorProfile = Map3ColorManager.getDefaultColorProfile(graphId);

		// set profile name
		newColorProfile.setProfileName(Map3ColorProfile.PROFILE_NAME_NEW);

		final Map3GradientColorProvider newColorProvider = new Map3GradientColorProvider(graphId, newColorProfile);

		new DialogMap3ColorEditor(//
				Display.getCurrent().getActiveShell(),
				newColorProvider,
				this,
				true).open();
	}

	private void onActionDuplicateProfile() {

		// get graph id from currently selected item
		final Object selection = ((IStructuredSelection) _colorProfileViewer.getSelection()).getFirstElement();

		if ((selection instanceof Map3GradientColorProvider) == false) {
			return;
		}

		final Map3GradientColorProvider selectedColorProvider = (Map3GradientColorProvider) selection;

		final Map3GradientColorProvider duplicatedColorProvider = selectedColorProvider.clone();

		// create a profile name
		duplicatedColorProvider.getMap3ColorProfile().setProfileName(
				selectedColorProvider.getMap3ColorProfile().getProfileName()
						+ UI.SPACE
						+ duplicatedColorProvider.getMap3ColorProfile().getProfileId());

		new DialogMap3ColorEditor(//
				Display.getCurrent().getActiveShell(),
				duplicatedColorProvider,
				this,
				true).open();
	}

	private void onActionEditProfile() {

		final Object firstElement = ((StructuredSelection) _colorProfileViewer.getSelection()).getFirstElement();
		if (firstElement instanceof Map3GradientColorProvider) {

			final Map3GradientColorProvider originalProfile = (Map3GradientColorProvider) firstElement;

			new DialogMap3ColorEditor(//
					Display.getCurrent().getActiveShell(),
					originalProfile,
					this,
					false).open();
		}
	}

	private void onActionRemoveProfile() {

		final Object firstElement = ((StructuredSelection) _colorProfileViewer.getSelection()).getFirstElement();
		if (firstElement instanceof Map3GradientColorProvider) {

			final Map3GradientColorProvider selectedColorProvider = (Map3GradientColorProvider) firstElement;
			final Map3ColorProfile colorProfile = selectedColorProvider.getMap3ColorProfile();

			final String message = NLS.bind(Messages.Pref_Map3Color_Dialog_RemoveProfile_Message, //
					colorProfile.getProfileName(),
					colorProfile.getProfileId());

			if (MessageDialog.openQuestion(
					Display.getCurrent().getActiveShell(),
					Messages.Pref_Map3Color_Dialog_RemoveProfile_Title,
					message)) {

				// update model
				final Map3ColorDefinition colorDef = Map3ColorManager.getColorDefinition(selectedColorProvider
						.getGraphId());

				colorDef.removeColorProvider(selectedColorProvider);

				// update UI
				_colorProfileViewer.refresh(colorDef);

				// select active color provider
				_colorProfileViewer.setSelection(new StructuredSelection(//
						Map3ColorManager.getActiveColorProvider(colorDef.getGraphId())));

				_colorProfileViewer.getTree().setFocus();
			}
		}
	}

	private void onResizeImageColumn() {

		final int newImageWidth = getImageSize();

		// check if the width has changed
		if (newImageWidth == _oldImageWidth) {
			return;
		}

		// recreate images
		disposeProfileImages();
	}

	private void onViewerCheckStateChange(final CheckStateChangedEvent event) {

		final Object viewerItem = event.getElement();

		if (viewerItem instanceof Map3ColorDefinition) {

			final Map3ColorDefinition colorDef = (Map3ColorDefinition) viewerItem;

			if (event.getChecked()) {
				_colorProfileViewer.setChecked(colorDef, false);
			}

		} else if (viewerItem instanceof Map3GradientColorProvider) {

			final Map3GradientColorProvider colorProvider = (Map3GradientColorProvider) viewerItem;

			if (event.getChecked()) {

				// set as active color provider

				setActiveColorProvider(colorProvider);

			} else {

				// color provider cannot be unchecked because at least one color provider must be checked

				_colorProfileViewer.setChecked(colorProvider, true);
			}
		}

	}

	private void onViewerDoubleClick() {

		final Object selection = ((IStructuredSelection) _colorProfileViewer.getSelection()).getFirstElement();

		if (selection instanceof Map3ColorDefinition) {

			// expand/collapse current item

			expandCollapseTreeItem((Map3ColorDefinition) selection);

		} else if (selection instanceof Map3GradientColorProvider) {

			// edit selected color

			onActionEditProfile();
		}
	}

	private boolean onViewerIsChecked(final Object element) {

		if (element instanceof Map3GradientColorProvider) {

			// set checked only active color providers

			final Map3GradientColorProvider colorProvider = (Map3GradientColorProvider) element;
			final boolean isActiveColorProfile = colorProvider.getMap3ColorProfile().isActiveColorProfile();

			return isActiveColorProfile;
		}

		return false;
	}

	private boolean onViewerIsGrayed(final Object element) {

		if (element instanceof Map3ColorDefinition) {
			return true;
		}

		return false;
	}

	private void onViewerPaint(final Event event) {

		switch (event.type) {
		case SWT.MeasureItem:

			/*
			 * Set height also for color def, when not set and all is collapsed, the color def size
			 * will be adjusted when an item is expanded.
			 */

			event.width += getImageSize();
			event.height = PROFILE_IMAGE_HEIGHT;

			break;

		case SWT.PaintItem:

			final TreeItem item = (TreeItem) event.item;
			final Object itemData = item.getData();

			if (itemData instanceof Map3ColorDefinition) {

				final Map3ColorDefinition colorDef = (Map3ColorDefinition) itemData;

				final Image image = UI.getGraphImage(colorDef.getGraphId());

				if (image != null) {

					final Rectangle rect = image.getBounds();

					final int x = event.x + event.width;
					final int yOffset = Math.max(0, (event.height - rect.height) / 2);

					event.gc.drawImage(image, x, event.y + yOffset);
				}

			} else if (itemData instanceof Map3GradientColorProvider) {

				final Map3GradientColorProvider colorProvider = (Map3GradientColorProvider) itemData;

				final Image image = getProfileImage(colorProvider);

				if (image != null) {

					final Rectangle rect = image.getBounds();

					final int x = event.x + event.width + PROFILE_IMAGE_X_OFFSET;
					final int yOffset = Math.max(0, (event.height - rect.height) / 2);

					event.gc.drawImage(image, x, event.y + yOffset);
				}
			}

			break;
		}
	}

	/**
	 * Is called when a color in the color viewer is selected.
	 */
	private void onViewerSelectColor() {

		final IStructuredSelection selection = (IStructuredSelection) _colorProfileViewer.getSelection();

		final Object selectedItem = selection.getFirstElement();

		if (selectedItem instanceof Map3GradientColorProvider) {

			final Map3GradientColorProvider selectedColorProvider = (Map3GradientColorProvider) selectedItem;

			setActiveColorProvider(selectedColorProvider);
		}

		enableControls();
	}

	@Override
	public boolean performCancel() {

		saveState();

		return super.performCancel();
	}

	@Override
	public boolean performOk() {

		saveState();

		return super.performOk();
	}

	@Override
	public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

		_viewerContainer.setRedraw(false);
		{
			final Object[] expandedElements = _colorProfileViewer.getExpandedElements();
			final ISelection selection = _colorProfileViewer.getSelection();

			_colorProfileViewer.getTree().dispose();

			createUI_22_ColorViewer(_viewerContainer);
			_viewerContainer.layout();

			// update viewer
			reloadViewer();

			_colorProfileViewer.setExpandedElements(expandedElements);
			_colorProfileViewer.setSelection(selection);
		}
		_viewerContainer.setRedraw(true);

		return _colorProfileViewer;
	}

	@Override
	public void reloadViewer() {

		_colorProfileViewer.setInput(this);

		_colorProfileViewer.setGrayedElements(getColorDefinitions());
	}

	private void restoreState() {

	}

	private void restoreStateViewer() {

		final String[] expandedColorDefIds = Util.getStateArray(_state, STATE_EXPANDED_COLOR_DEFINITIONS, null);
		if (expandedColorDefIds != null) {

			final ArrayList<Map3ColorDefinition> expandedColorDefs = new ArrayList<Map3ColorDefinition>();

			for (final String graphIdValue : expandedColorDefIds) {

				final MapGraphId graphId = MapGraphId.valueOf(graphIdValue);

				if (graphId != null) {
					expandedColorDefs.add(Map3ColorManager.getColorDefinition(graphId));
				}
			}

			_colorProfileViewer.setExpandedElements(expandedColorDefs.toArray());
		}
	}

	private void saveState() {

		_columnManager.saveState(_state);

		/*
		 * save state for expanded color defs
		 */
		final ArrayList<String> expandedColorDefIds = new ArrayList<String>();

		for (final Object expandedElement : _colorProfileViewer.getExpandedElements()) {
			if (expandedElement instanceof Map3ColorDefinition) {
				expandedColorDefIds.add(((Map3ColorDefinition) expandedElement).getGraphId().name());
			}
		}

		_state.put(
				STATE_EXPANDED_COLOR_DEFINITIONS,
				expandedColorDefIds.toArray(new String[expandedColorDefIds.size()]));
	}

	private void setActiveColorProvider(final Map3GradientColorProvider colorProvider) {

		final MapGraphId graphId = colorProvider.getGraphId();
		final Map3ColorDefinition colorDefinition = Map3ColorManager.getColorDefinition(graphId);

		final ArrayList<Map3GradientColorProvider> allGraphIdColorProvider = colorDefinition.getColorProviders();

		if (allGraphIdColorProvider.size() < 2) {

			// this case should need no attention

		} else {

			// set as active color provider

			if (colorProvider.getMap3ColorProfile().isActiveColorProfile()) {
				return;
			}

			// reset state for previous color provider
			final Map3GradientColorProvider currentActiveColorProvider = Map3ColorManager
					.getActiveColorProvider(graphId);
			_colorProfileViewer.setChecked(currentActiveColorProvider, false);

			// set state for selected color provider
			Map3ColorManager.setActiveColorProvider(colorProvider);
			_colorProfileViewer.setChecked(colorProvider, true);

			// also select a checked color provider
			_colorProfileViewer.setSelection(new StructuredSelection(colorProvider));
		}
	}

}
