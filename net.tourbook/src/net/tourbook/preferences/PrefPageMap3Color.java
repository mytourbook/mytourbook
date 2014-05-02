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

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.color.ColorProviderConfig;
import net.tourbook.common.color.Map3ColorDefinition;
import net.tourbook.common.color.Map3ColorProfile;
import net.tourbook.common.color.Map3GradientColorManager;
import net.tourbook.common.color.Map3GradientColorProvider;
import net.tourbook.common.color.Map3ProfileComparator;
import net.tourbook.common.color.MapGraphId;
import net.tourbook.common.color.ProfileImage;
import net.tourbook.common.color.RGBVertex;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.TreeColumnDefinition;
import net.tourbook.common.util.Util;
import net.tourbook.map2.view.TourMapPainter;
import net.tourbook.map3.Messages;
import net.tourbook.map3.ui.DialogMap3ColorEditor;
import net.tourbook.map3.ui.IMap3ColorUpdater;
import net.tourbook.photo.IPhotoPreferences;
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
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
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
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
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
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageMap3Color extends PreferencePage implements IWorkbenchPreferencePage, IMap3ColorUpdater,
		ITourViewer {

	public static final String							ID									= "net.tourbook.preferences.PrefPageMap3Color";	//$NON-NLS-1$

	private static final String							APP_ACTION_COLUMNS					= net.tourbook.Messages.App_Action_Columns;
	private static final String							APP_ACTION_DUPLICATE				= net.tourbook.Messages.App_Action_Duplicate;
	private static final String							APP_ACTION_EDIT						= net.tourbook.Messages.App_Action_Edit;
	private static final String							APP_ACTION_NEW						= net.tourbook.Messages.App_Action_New;
	private static final String							APP_ACTION_REMOVE					= net.tourbook.Messages.App_Action_Remove;

	private static int									PROFILE_IMAGE_HEIGHT;
	private static final int							PROFILE_IMAGE_MIN_SIZE				= 30;

	private static final String							STATE_EXPANDED_COLOR_DEFINITIONS	= "STATE_EXPANDED_COLOR_DEFINITIONS";				//$NON-NLS-1$

	private final IPreferenceStore						_prefStore							= TourbookPlugin
																									.getPrefStore();

	private final IDialogSettings						_state								= TourbookPlugin
																									.getState(this
																											.getClass()
																											.getName());

	/**
	 * Displays all {@link Map3ColorDefinition}s and all {@link Map3GradientColorProvider}s which
	 * are managed by the {@link Map3GradientColorManager}.
	 */
	private CheckboxTreeViewer							_colorProfileViewer;
	private ColumnManager								_columnManager;

	private PixelConverter								_pc;

	private boolean										_isInUIUpdate;

	private int											_defaultImageWidth					= 300;
	private int											_oldImageWidth						= -1;

	/**
	 * Contains the table column widget for the profile image.
	 */
	private TreeColumn									_tcProfileImage;

	private TreeColumnDefinition						_colDefGraphImage;
	private TreeColumnDefinition						_colDefProfileImage;

	/**
	 * index of the profile image, this can be changed when the columns are reordered with the mouse
	 * or the column manager
	 */
	private int											_columnIndexGraphImage				= 0;
	private int											_columnIndexProfileImage			= 0;

	private HashMap<Map3GradientColorProvider, Image>	_profileImages						= new HashMap<Map3GradientColorProvider, Image>();

	private MouseWheelListener							_defaultMouseWheelListener;

	/*
	 * UI controls
	 */
	private Composite									_viewerContainer;

	private Button										_btnDuplicateProfile;
	private Button										_btnEditProfile;
	private Button										_btnNewProfile;
	private Button										_btnRemoveProfile;

	private Button										_chkShowColorSelector;

	private Label										_lblNumberOfColors;

	private Spinner										_spinNumberOfColors;

	private ToolBar										_toolBar;

	{
		_defaultMouseWheelListener = new MouseWheelListener() {
			public void mouseScrolled(final MouseEvent event) {
				net.tourbook.common.UI.adjustSpinnerValueOnMouseScroll(event);
			}
		};
	}

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

	public PrefPageMap3Color() {

//		noDefaultAndApplyButton();
	}

	/**
	 * @return Returns all sorted {@link Map3ColorDefinition}s.
	 */
	private static Object[] getColorDefinitions() {

		final ArrayList<Map3ColorDefinition> colorDefinitions = Map3GradientColorManager.getSortedColorDefinitions();

		return colorDefinitions.toArray(new Map3ColorDefinition[colorDefinitions.size()]);
	}

	private void actionAddProfile() {

		// get graph id from currently selected item
		final Object selection = ((IStructuredSelection) _colorProfileViewer.getSelection()).getFirstElement();
		MapGraphId graphId = MapGraphId.Altitude;

		if (selection instanceof Map3ColorDefinition) {

			graphId = ((Map3ColorDefinition) selection).getGraphId();

		} else if (selection instanceof Map3GradientColorProvider) {

			graphId = ((Map3GradientColorProvider) selection).getGraphId();
		}

		final Map3ColorProfile newColorProfile = Map3GradientColorManager.getDefaultColorProfile(graphId);

		// set profile name
		newColorProfile.setProfileName(Map3ColorProfile.PROFILE_NAME_NEW);

		final Map3GradientColorProvider newColorProvider = new Map3GradientColorProvider(graphId, newColorProfile);

		new DialogMap3ColorEditor(//
				Display.getCurrent().getActiveShell(),
				newColorProvider,
				this,
				true).open();

		_colorProfileViewer.getTree().setFocus();
	}

	private void actionDuplicateProfile() {

		// get graph id from currently selected item
		final Object selection = ((IStructuredSelection) _colorProfileViewer.getSelection()).getFirstElement();

		if ((selection instanceof Map3GradientColorProvider) == false) {
			return;
		}

		final Map3GradientColorProvider selectedColorProvider = (Map3GradientColorProvider) selection;

		final Map3GradientColorProvider duplicatedColorProvider = selectedColorProvider.clone();

		// create a new profile name by setting it to the profile id which is unique
		duplicatedColorProvider.getMap3ColorProfile().setDuplicatedName();

		new DialogMap3ColorEditor(//
				Display.getCurrent().getActiveShell(),
				duplicatedColorProvider,
				this,
				true).open();

		_colorProfileViewer.getTree().setFocus();
	}

	private void actionEditProfile() {

		final Object firstElement = ((StructuredSelection) _colorProfileViewer.getSelection()).getFirstElement();
		if (firstElement instanceof Map3GradientColorProvider) {

			final Map3GradientColorProvider colorProvider = (Map3GradientColorProvider) firstElement;

			new DialogMap3ColorEditor(//
					Display.getCurrent().getActiveShell(),
					colorProvider,
					this,
					false).open();

			_colorProfileViewer.getTree().setFocus();
		}
	}

	private void actionRemoveProfile() {

		final Object firstElement = ((StructuredSelection) _colorProfileViewer.getSelection()).getFirstElement();
		if (firstElement instanceof Map3GradientColorProvider) {

			final Map3GradientColorProvider selectedColorProvider = (Map3GradientColorProvider) firstElement;
			final Map3ColorProfile colorProfile = selectedColorProvider.getMap3ColorProfile();

			final String message = NLS.bind(Messages.Pref_Map3Color_Dialog_RemoveProfile_Message, //
					colorProfile.getProfileName());

			if (MessageDialog.openQuestion(
					Display.getCurrent().getActiveShell(),
					Messages.Pref_Map3Color_Dialog_RemoveProfile_Title,
					message)) {

				// update model
				final Map3ColorDefinition colorDef = Map3GradientColorManager.getColorDefinition(selectedColorProvider
						.getGraphId());

				colorDef.removeColorProvider(selectedColorProvider);

//				Map3GradientColorManager.saveColors();

				// update UI
				_colorProfileViewer.refresh(colorDef);

				updateUI_SelectColorProvider(Map3GradientColorManager.getActiveMap3ColorProvider(colorDef.getGraphId()));
			}
		}
	}

	@Override
	public void applyData(final Object data) {

		if (data instanceof MapGraphId) {

			// expand color definition for the graph id

			final MapGraphId graphId = (MapGraphId) data;
			final Map3ColorDefinition expandedColorDefinition = Map3GradientColorManager.getColorDefinition(graphId);
			final Map3GradientColorProvider activeColorProvider = Map3GradientColorManager
					.getActiveMap3ColorProvider(graphId);

			_viewerContainer.setRedraw(false);
			{
				_colorProfileViewer.collapseAll();

				_colorProfileViewer.setExpandedElements(new Object[] { expandedColorDefinition });
				_colorProfileViewer.setSelection(new StructuredSelection(activeColorProvider));

				_colorProfileViewer.getTree().setFocus();
			}
			_viewerContainer.setRedraw(true);
		}
	}

	@Override
	public void applyMapColors(	final Map3GradientColorProvider originalCP,
								final Map3GradientColorProvider modifiedCP,
								final boolean isNewColorProvider) {

		/*
		 * Update model
		 */
		if (isNewColorProvider) {

			// a new profile is edited
			Map3GradientColorManager.addColorProvider(modifiedCP);

		} else {

			// an existing profile is modified
			Map3GradientColorManager.replaceColorProvider(originalCP, modifiedCP);
		}
//		Map3GradientColorManager.saveColors();

		/*
		 * Update UI
		 */
		final MapGraphId originalGraphId = originalCP.getGraphId();
		final MapGraphId modifiedGraphId = modifiedCP.getGraphId();

		_colorProfileViewer.refresh(Map3GradientColorManager.getColorDefinition(originalGraphId));

		if (originalGraphId != modifiedGraphId) {

			// both color definitions are modified
			_colorProfileViewer.refresh(Map3GradientColorManager.getColorDefinition(modifiedGraphId));
		}

		updateUI_SelectColorProvider(modifiedCP);
	}

	@Override
	protected Control createContents(final Composite parent) {

		_pc = new PixelConverter(parent);

		PROFILE_IMAGE_HEIGHT = (int) (_pc.convertHeightInCharsToPixels(1) * 1.0);

		final Composite ui = createUI(parent);

		fillToolbar();

		restorePrefStore();
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
				.spacing(LayoutConstants.getSpacing().x, 2)
				.numColumns(2)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			createUI_10_Title(container);

			createUI_20_ColorViewer_Container(container);
			createUI_30_Actions(container);

			createUI_40_Options(container);
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

	private void createUI_20_ColorViewer_Container(final Composite parent) {

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
			createUI_22_ColorViewer_Table(_viewerContainer);
		}
	}

	private void createUI_22_ColorViewer_Table(final Composite parent) {

		/*
		 * Create tree
		 */
		final Tree tree = new Tree(parent, //
				SWT.CHECK //
						| SWT.H_SCROLL
						| SWT.V_SCROLL
//						| SWT.BORDER
						| SWT.FULL_SELECTION);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tree);

		tree.setHeaderVisible(true);
//		tree.setLinesVisible(false);
//		tree.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));

		/*
		 * NOTE: MeasureItem, PaintItem and EraseItem are called repeatedly. Therefore, it is
		 * critical for performance that these methods be as efficient as possible.
		 */
		final Listener paintListener = new Listener() {
			public void handleEvent(final Event event) {

				if (event.type == SWT.MeasureItem || event.type == SWT.PaintItem) {

					onViewerPaint(event);
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

		_tcProfileImage = _colDefProfileImage.getTreeColumn();

		_columnIndexProfileImage = _colDefProfileImage.getCreateIndex();
		_columnIndexGraphImage = _colDefGraphImage.getCreateIndex();

		_colorProfileViewer.setContentProvider(new ContentProvider());
		_colorProfileViewer.setComparator(new Map3ProfileComparator());

		_colorProfileViewer.setUseHashlookup(true);

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

		// set color for all controls
		final ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
		final Color fgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_FOREGROUND);
		final Color bgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_BACKGROUND);

		net.tourbook.common.UI.updateChildColors(tree, fgColor, bgColor);
	}

	private void createUI_30_Actions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults().applyTo(container);
		{

			{
				/*
				 * Button: New
				 */
				_btnNewProfile = new Button(container, SWT.NONE);
				_btnNewProfile.setText(APP_ACTION_NEW);
				_btnNewProfile.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						actionAddProfile();
					}
				});
				setButtonLayoutData(_btnNewProfile);
			}

			{
				/*
				 * button: Edit
				 */
				_btnEditProfile = new Button(container, SWT.NONE);
				_btnEditProfile.setText(APP_ACTION_EDIT);
				_btnEditProfile.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						actionEditProfile();
					}
				});
				setButtonLayoutData(_btnEditProfile);
			}

			{
				/*
				 * Button: Duplicate
				 */
				_btnDuplicateProfile = new Button(container, SWT.NONE);
				_btnDuplicateProfile.setText(APP_ACTION_DUPLICATE);
				_btnDuplicateProfile.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						actionDuplicateProfile();
					}
				});
				setButtonLayoutData(_btnDuplicateProfile);
			}

			{
				/*
				 * Button: Remove
				 */
				_btnRemoveProfile = new Button(container, SWT.NONE);
				_btnRemoveProfile.setText(APP_ACTION_REMOVE);
				_btnRemoveProfile.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						actionRemoveProfile();
					}
				});
				setButtonLayoutData(_btnRemoveProfile);
			}

			{
				/*
				 * Button: Columns
				 */
				final Button btnAdjustColumns = new Button(container, SWT.NONE);
				btnAdjustColumns.setText(APP_ACTION_COLUMNS);
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

	private void createUI_40_Options(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults()//
//				.margins(0, 5)
				.extendedMargins(0, 0, 5, 20)
				.numColumns(2)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			{
				/*
				 * Checkbox: Show drop down color selector
				 */
				_chkShowColorSelector = new Button(container, SWT.CHECK);
				_chkShowColorSelector.setText(Messages.Pref_Map3Color_Checkbox_ShowDropDownColorSelector);
				_chkShowColorSelector
						.setToolTipText(Messages.Pref_Map3Color_Checkbox_ShowDropDownColorSelector_Tooltip);
				_chkShowColorSelector.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						enableControls();
					}
				});
			}

			final Composite rowContainer = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(rowContainer);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(rowContainer);
//			rowContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
			{
				{
					/*
					 * Label: Number of colors
					 */
					_lblNumberOfColors = new Label(rowContainer, SWT.NONE);
					GridDataFactory.fillDefaults()//
//							.indent(16, 0)
//							.grab(true, false)
//							.align(SWT.END, SWT.CENTER)
							.indent(20, 0)
							.align(SWT.FILL, SWT.CENTER)
							.applyTo(_lblNumberOfColors);
					_lblNumberOfColors.setText(Messages.Pref_Map3Color_Label_NumberOfColors);
					_lblNumberOfColors.setToolTipText(Messages.Pref_Map3Color_Label_NumberOfColors_Tooltip);
				}

				{
					/*
					 * Spinner: Number of colors
					 */
					_spinNumberOfColors = new Spinner(rowContainer, SWT.BORDER);
					_spinNumberOfColors.setMinimum(2);
					_spinNumberOfColors.setMaximum(50);
					_spinNumberOfColors.setPageIncrement(5);
					_spinNumberOfColors.addMouseWheelListener(_defaultMouseWheelListener);
				}
			}
		}
	}

	private void defineAllColumns() {

		defineColumn_10_ProfileName();
		defineColumn_20_GraphImage();

		defineColumn_32_MinValue();
		defineColumn_30_ColorImage();
		defineColumn_35_MaxValue();

		defineColumn_40_ValueMarker();
		defineColumn_42_LegendMarker();

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

					final Map3ColorDefinition colorDefinition = (Map3ColorDefinition) (element);

					cell.setText(colorDefinition.getVisibleName());

				} else if (element instanceof Map3GradientColorProvider) {

					cell.setText(((Map3GradientColorProvider) (element)).getMap3ColorProfile().getProfileName());

				} else {

					cell.setText(UI.EMPTY_STRING);
				}
			}
		});
	}

	/**
	 * Column: Graph image
	 */
	private void defineColumn_20_GraphImage() {

		final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, "graphImage", SWT.LEAD); //$NON-NLS-1$
		_colDefGraphImage = colDef;

		colDef.setColumnLabel(Messages.Pref_Map3Color_Column_GraphImage);
		colDef.setDefaultColumnWidth(20);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {

			// !!! set dummy label provider, otherwise an error occures !!!
			@Override
			public void update(final ViewerCell cell) {}
		});
	}

	/**
	 * Column: Color image
	 */
	private void defineColumn_30_ColorImage() {

		final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, "colorImage", SWT.LEAD); //$NON-NLS-1$
		_colDefProfileImage = colDef;

		colDef.setColumnLabel(Messages.Pref_Map3Color_Column_Colors);
		colDef.setColumnHeader(Messages.Pref_Map3Color_Column_Colors);
		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(20));
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
	 * Column: Min value
	 */
	private void defineColumn_32_MinValue() {

		final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, "minValue", SWT.TRAIL); //$NON-NLS-1$

		colDef.setColumnLabel(Messages.Pref_Map3Color_Column_MinValue_Label);
		colDef.setColumnHeader(Messages.Pref_Map3Color_Column_MinValue_Header);
		colDef.setColumnToolTipText(Messages.Pref_Map3Color_Column_MinValue_Label);
		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(8));
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				if (element instanceof Map3GradientColorProvider) {

					final Map3ColorProfile colorProfile = ((Map3GradientColorProvider) (element)).getMap3ColorProfile();

					final ProfileImage profileImage = colorProfile.getProfileImage();
					final ArrayList<RGBVertex> vertices = profileImage.getRgbVertices();
					final RGBVertex firstVertex = vertices.get(0);

					cell.setText(Integer.toString(firstVertex.getValue()));

				} else {

					cell.setText(UI.EMPTY_STRING);
				}
			}
		});
	}

	/**
	 * Column: Max value
	 */
	private void defineColumn_35_MaxValue() {

		final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, "maxValue", SWT.LEAD); //$NON-NLS-1$

		colDef.setColumnLabel(Messages.Pref_Map3Color_Column_MaxValue_Label);
		colDef.setColumnHeader(Messages.Pref_Map3Color_Column_MaxValue_Header);
		colDef.setColumnToolTipText(Messages.Pref_Map3Color_Column_MaxValue_Label);
		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(8));
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				if (element instanceof Map3GradientColorProvider) {

					final Map3ColorProfile colorProfile = ((Map3GradientColorProvider) (element)).getMap3ColorProfile();

					final ProfileImage profileImage = colorProfile.getProfileImage();
					final ArrayList<RGBVertex> vertices = profileImage.getRgbVertices();
					final RGBVertex lastVertex = vertices.get(vertices.size() - 1);

					cell.setText(Integer.toString(lastVertex.getValue()));

				} else {

					cell.setText(UI.EMPTY_STRING);
				}
			}
		});
	}

	/**
	 * Column: Relative value marker
	 */
	private void defineColumn_40_ValueMarker() {

		final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, "relativeMarker", SWT.CENTER); //$NON-NLS-1$

		colDef.setColumnLabel(Messages.Pref_Map3Color_Column_AbsoluteRelativValue_Label);
		colDef.setColumnToolTipText(Messages.Pref_Map3Color_Column_AbsoluteRelativValue_Tooltip);
		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(3));
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				if (element instanceof Map3GradientColorProvider) {

					final Map3ColorProfile colorProfile = ((Map3GradientColorProvider) (element)).getMap3ColorProfile();

					if (colorProfile.isAbsoluteValues()) {
						cell.setText(Messages.Pref_Map3Color_Column_ValueMarker_Absolute);
					} else {
						cell.setText(Messages.Pref_Map3Color_Column_ValueMarker_Relative);
					}

				} else {

					cell.setText(UI.EMPTY_STRING);
				}
			}
		});
	}

	/**
	 * Column: Legend overwrite marker
	 */
	private void defineColumn_42_LegendMarker() {

		final TreeColumnDefinition colDef = new TreeColumnDefinition(
				_columnManager,
				"legendMinMaxOverwrite", SWT.CENTER); //$NON-NLS-1$

		colDef.setColumnLabel(Messages.Pref_Map3Color_Column_OverwriteLegendMinMax_Label);
		colDef.setColumnToolTipText(Messages.Pref_Map3Color_Column_OverwriteLegendMinMax_Label_Tooltip);
		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(3));
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				if (element instanceof Map3GradientColorProvider) {

					final Map3ColorProfile colorProfile = ((Map3GradientColorProvider) (element)).getMap3ColorProfile();

					if (colorProfile.isAbsoluteValues() && colorProfile.isOverwriteLegendValues()) {
						cell.setText(Messages.Pref_Map3Color_Column_Legend_Marker);
					} else {
						cell.setText(UI.EMPTY_STRING);
					}

				} else {

					cell.setText(UI.EMPTY_STRING);
				}
			}
		});
	}

	/**
	 * column: id
	 */
	private void defineColumn_99_ProfileId() {

		final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, "profileId", SWT.LEAD); //$NON-NLS-1$

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
		final boolean isColorSelectorDisplayed = _chkShowColorSelector.getSelection();

		if (firstSelectedItem instanceof Map3GradientColorProvider) {

			final Map3GradientColorProvider colorProvider = (Map3GradientColorProvider) firstSelectedItem;

			isColorProfileSelected = true;

			final MapGraphId graphId = colorProvider.getGraphId();

			// profiles can only be removed when more than one profile is available for a graph type
			final ArrayList<Map3GradientColorProvider> graphIdColorProviders = Map3GradientColorManager
					.getColorProviders(graphId);
			canRemoveProfiles = graphIdColorProviders.size() > 1;
		}

		_btnEditProfile.setEnabled(isColorProfileSelected);
		_btnDuplicateProfile.setEnabled(isColorProfileSelected);
		_btnRemoveProfile.setEnabled(canRemoveProfiles);

		_lblNumberOfColors.setEnabled(isColorSelectorDisplayed);
		_spinNumberOfColors.setEnabled(isColorSelectorDisplayed);
	}

	private void expandCollapseTreeItem(final Map3ColorDefinition treeItem) {

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

	private int getImageColumnWidth() {

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

		return width;
	}

	private Image getProfileImage(final Map3GradientColorProvider colorProvider) {

		Image image = _profileImages.get(colorProvider);

		if (isProfileImageValid(image) == false) {

			/*
			 * This offset prevents that the color is painted just beside the vertical scollbar when
			 * default size is used.
			 */
			final int trailingOffset = 0;//10;

			final int columnWidth = getImageColumnWidth();

			final int imageWidth = columnWidth - trailingOffset;
			final int imageHeight = PROFILE_IMAGE_HEIGHT - 1;

			final Map3ColorProfile colorProfile = colorProvider.getMap3ColorProfile();
			final ArrayList<RGBVertex> rgbVertices = colorProfile.getProfileImage().getRgbVertices();

			colorProvider.configureColorProvider(ColorProviderConfig.MAP3_PROFILE, imageWidth, rgbVertices, false);

			image = TourMapPainter.createMapLegendImage(//
					colorProvider,
					ColorProviderConfig.MAP3_PROFILE,
					imageWidth,
					imageHeight,
					false,
					false,
					false);

			final Image oldImage = _profileImages.put(colorProvider, image);

			Util.disposeResource(oldImage);

			_oldImageWidth = imageWidth;
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

		if (image.getBounds().width != getImageColumnWidth()) {

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

	private void onResizeImageColumn() {

		final int newImageWidth = getImageColumnWidth();

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

			// prevent to check a color definition

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

				// a color provider cannot be unchecked, to be unckecked, another color provider must be checked

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

			actionEditProfile();
		}
	}

	private boolean onViewerIsChecked(final Object element) {

		if (element instanceof Map3GradientColorProvider) {

			// set checked only active color providers

			final Map3GradientColorProvider mgrColorProvider = (Map3GradientColorProvider) element;
			final boolean isActiveColorProfile = mgrColorProvider.getMap3ColorProfile().isActiveColorProfile();

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

		// paint images at the correct column

		final int columnIndex = event.index;

		if (columnIndex == _columnIndexProfileImage) {

			onViewerPaint_ProfileImage(event);

		} else if (columnIndex == _columnIndexGraphImage) {

			onViewerPaint_GraphImage(event);
		}

	}

	private void onViewerPaint_GraphImage(final Event event) {

		switch (event.type) {
		case SWT.MeasureItem:

			/*
			 * Set height also for color def, when not set and all is collapsed, the color def size
			 * will be adjusted when an item is expanded.
			 */

//			event.width += getImageColumnWidth();
//			event.height = PROFILE_IMAGE_HEIGHT;

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

					// center vertical
					final int yOffset = Math.max(0, (event.height - rect.height) / 2);

					event.gc.drawImage(image, x, event.y + yOffset);
				}
			}

			break;
		}
	}

	private void onViewerPaint_ProfileImage(final Event event) {

		switch (event.type) {
		case SWT.MeasureItem:

			/*
			 * Set height also for color def, when not set and all is collapsed, the color def size
			 * will be adjusted when an item is expanded.
			 */

			event.width += getImageColumnWidth();
//			event.height = PROFILE_IMAGE_HEIGHT;

			break;

		case SWT.PaintItem:

			final TreeItem item = (TreeItem) event.item;
			final Object itemData = item.getData();

			if (itemData instanceof Map3GradientColorProvider) {

				final Map3GradientColorProvider colorProvider = (Map3GradientColorProvider) itemData;

				final Image image = getProfileImage(colorProvider);

				if (image != null) {

					final Rectangle rect = image.getBounds();

					final int x = event.x + event.width;
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

		if (_isInUIUpdate) {
			return;
		}

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
	protected void performDefaults() {

		_chkShowColorSelector.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.MAP3_IS_COLOR_SELECTOR_DISPLAYED));

		_spinNumberOfColors.setSelection(//
				_prefStore.getDefaultInt(ITourbookPreferences.MAP3_NUMBER_OF_COLOR_SELECTORS));

		enableControls();

		super.performDefaults();
	}

	@Override
	public boolean performOk() {

		savePrefStore();
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

			createUI_22_ColorViewer_Table(_viewerContainer);
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

		// prevent to check color defintions
		_colorProfileViewer.setGrayedElements(getColorDefinitions());
	}

	private void restorePrefStore() {

		_chkShowColorSelector.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.MAP3_IS_COLOR_SELECTOR_DISPLAYED));

		_spinNumberOfColors.setSelection(//
				_prefStore.getInt(ITourbookPreferences.MAP3_NUMBER_OF_COLOR_SELECTORS));
	}

	private void restoreStateViewer() {

		final String[] expandedColorDefIds = Util.getStateArray(_state, STATE_EXPANDED_COLOR_DEFINITIONS, null);
		if (expandedColorDefIds != null) {

			final ArrayList<Map3ColorDefinition> expandedColorDefs = new ArrayList<Map3ColorDefinition>();

			for (final String graphIdValue : expandedColorDefIds) {

				final MapGraphId graphId = MapGraphId.valueOf(graphIdValue);

				if (graphId != null) {
					expandedColorDefs.add(Map3GradientColorManager.getColorDefinition(graphId));
				}
			}

			_colorProfileViewer.setExpandedElements(expandedColorDefs.toArray());
		}
	}

	private void savePrefStore() {

		_prefStore.setValue(//
				ITourbookPreferences.MAP3_IS_COLOR_SELECTOR_DISPLAYED,
				_chkShowColorSelector.getSelection());

		_prefStore.setValue(//
				ITourbookPreferences.MAP3_NUMBER_OF_COLOR_SELECTORS,
				_spinNumberOfColors.getSelection());
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

	/**
	 * @param selectedColorProvider
	 * @return Returns <code>true</code> when a new color provider is set, otherwise
	 *         <code>false</code>.
	 */
	private boolean setActiveColorProvider(final Map3GradientColorProvider selectedColorProvider) {

		final Map3ColorProfile selectedColorProfile = selectedColorProvider.getMap3ColorProfile();

		// check if the selected color provider is already the active color provider
		if (selectedColorProfile.isActiveColorProfile()) {
			return false;
		}

		final MapGraphId graphId = selectedColorProvider.getGraphId();
		final Map3ColorDefinition colorDefinition = Map3GradientColorManager.getColorDefinition(graphId);

		final ArrayList<Map3GradientColorProvider> allGraphIdColorProvider = colorDefinition.getColorProviders();

		if (allGraphIdColorProvider.size() < 2) {

			// this case should need no attention

		} else {

			// set selected color provider as active color provider

			// reset state for previous color provider
			final Map3GradientColorProvider oldActiveColorProvider = Map3GradientColorManager
					.getActiveMap3ColorProvider(graphId);
			_colorProfileViewer.setChecked(oldActiveColorProvider, false);

			// set state for selected color provider
			_colorProfileViewer.setChecked(selectedColorProvider, true);

			// set new active color provider
			Map3GradientColorManager.setActiveColorProvider(selectedColorProvider);

			updateUI_SelectColorProvider(selectedColorProvider);

			return true;
		}

		return false;
	}

	private void updateUI_SelectColorProvider(final Map3GradientColorProvider selectedColorProvider) {

		_isInUIUpdate = true;
		{
			// select checked color provider
			_colorProfileViewer.setSelection(new StructuredSelection(selectedColorProvider));
		}
		_isInUIUpdate = false;

		_colorProfileViewer.getTree().setFocus();

		// Fire event that 3D map colors have changed.
		TourbookPlugin.getPrefStore().setValue(ITourbookPreferences.MAP3_COLOR_IS_MODIFIED, Math.random());
	}

}
