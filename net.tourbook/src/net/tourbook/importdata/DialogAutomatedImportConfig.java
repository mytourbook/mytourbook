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
package net.tourbook.importdata;

import java.util.ArrayList;
import java.util.Collections;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;

/**
 * This is a template for a title area dialog
 */
public class DialogAutomatedImportConfig extends TitleAreaDialog {

	private static final String					ID						= "net.tourbook.importdata.DialogAutomatedImportConfig";	//$NON-NLS-1$
	//
	private static final String					DATA_KEY_TOUR_TYPE_ID	= "DATA_KEY_TOUR_TYPE_ID";									//$NON-NLS-1$
	private static final String					DATA_KEY_VERTEX_INDEX	= "DATA_KEY_VERTEX_INDEX";									//$NON-NLS-1$
	//
	private final IDialogSettings				_state					= TourbookPlugin.getState(ID);
	//
	private SelectionAdapter					_defaultSelectionListener;
	private SelectionAdapter					_vertexTourTypeListener;
	private MouseWheelListener					_vertexValueMouseWheelListener;
	//
	private ActionAddVertex						_action_VertexAdd;
	private ActionDeleteVertex[]				_actionVertex_Delete;
	private ActionOpenPrefDialog				_actionOpenTourTypePrefs;
	private ActionSortVertices					_action_VertexSort;
	//

	private PixelConverter						_pc;

	private final static SpeedVertex[]			DEFAULT_VERTICES;

	static {

		DEFAULT_VERTICES = new SpeedVertex[] {
			//
			new SpeedVertex(10),
			new SpeedVertex(30),
			new SpeedVertex(150),
			new SpeedVertex(300)
		//
		};
	}

	/** Model for all configs. */
	private ArrayList<AutomatedImportConfig>	_dialogConfigs;

	/** Model for the currently selected config. */
	private AutomatedImportConfig				_currentConfig;

	private TableViewer							_configViewer;

	/*
	 * UI controls
	 */
	private Composite							_parent;

	private Composite							_vertexOuterContainer;
	private Composite							_vertexContainer;
	private ScrolledComposite					_vertexScrolledContainer;

	private Button								_btnConfig_Add;
	private Button								_btnConfig_Remove;
	private Button								_btnSelectDeviceFolder;
	private Button								_chkTourType;

	private Combo								_comboDevicePath;

	private Label								_lblConfigName;
	private Label								_lblDeviceFolder;
	private Label[]								_labelVertex_SpeedUnit;
	private Label[]								_labelVertex_TourTypeIcon;

	private Link[]								_linkVertex_TourType;

	private Spinner[]							_spinnerVertex_AvgSpeed;

	private Text								_txtConfigName;

	private class ActionAddVertex extends Action {

		public ActionAddVertex() {

			super(null, AS_PUSH_BUTTON);

			setToolTipText(Messages.Dialog_AutoImportConfig_Action_AddSpeed_Tooltip);
			setImageDescriptor(TourbookPlugin.getImageDescriptor(net.tourbook.Messages.Image__App_Add));
		}

		@Override
		public void run() {
			onVertex_Add();
		}
	}

	private class ActionDeleteVertex extends Action {

		private int	_vertexIndex;

		public ActionDeleteVertex() {

			super(null, AS_PUSH_BUTTON);

			setToolTipText(Messages.Dialog_AutoImportConfig_Action_RemoveSpeed_Tooltip);

			setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_Trash));
			setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_Trash_Disabled));
		}

		@Override
		public void run() {
			onVertex_Remove(_vertexIndex);
		}

		public void setData(final String key, final int vertexIndex) {

			_vertexIndex = vertexIndex;
		}
	}

	private class ActionSetTourType extends Action {

		private int			_vertexIndex;
		private TourType	_tourType;

		/**
		 * @param tourType
		 * @param vertexIndex
		 * @param isSaveTour
		 *            when <code>true</code> the tour will be saved and a
		 *            {@link TourManager#TOUR_CHANGED} event is fired, otherwise the
		 *            {@link TourData} from the tour provider is only updated
		 */
		public ActionSetTourType(final TourType tourType, final boolean isChecked, final int vertexIndex) {

			super(tourType.getName(), AS_CHECK_BOX);

			_vertexIndex = vertexIndex;

			if (isChecked == false) {

				// show image when tour type can be selected, disabled images look ugly on win
				final Image tourTypeImage = net.tourbook.ui.UI.getInstance().getTourTypeImage(tourType.getTypeId());
				setImageDescriptor(ImageDescriptor.createFromImage(tourTypeImage));
			}

			setChecked(isChecked);
			setEnabled(isChecked == false);

			_tourType = tourType;
		}

		@Override
		public void run() {
			onVertex_SelectTourType(_vertexIndex, _tourType);
		}
	}

	private class ActionSortVertices extends Action {

		public ActionSortVertices() {

			super(null, AS_PUSH_BUTTON);

			setToolTipText(Messages.Dialog_AutoImportConfig_Action_SortVertices_Tooltip);

			setImageDescriptor(TourbookPlugin.getImageDescriptor(net.tourbook.Messages.Image__App_Sort));
			setDisabledImageDescriptor(TourbookPlugin
					.getImageDescriptor(net.tourbook.Messages.Image__App_Sort_Disabled));
		}

		@Override
		public void run() {
			onVertex_Sort();
		}
	}

	private class ClientsContentProvider implements IStructuredContentProvider {

		public ClientsContentProvider() {}

		@Override
		public void dispose() {}

		@Override
		public Object[] getElements(final Object parent) {
			return _dialogConfigs.toArray(new AutomatedImportConfig[_dialogConfigs.size()]);
		}

		@Override
		public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {

		}
	}

	DialogAutomatedImportConfig(final Shell parentShell, final ArrayList<AutomatedImportConfig> importConfigs) {

		super(parentShell);

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE);

		setupImportConfigs(importConfigs);
	}

	private void addPrefChangeListener() {

//		_prefChangeListener = new IPropertyChangeListener() {
//			@Override
//			public void propertyChange(final PropertyChangeEvent event) {
//				final String property = event.getProperty();
//
//				if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {
//					checkTourTypes();
//				}
//			}
//		};
//
//		// add pref listener
//		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	private void addVertex(final int vertexPosition, final SpeedVertex speedVertex) {

		_currentConfig.speedVertices.add(vertexPosition, speedVertex);

		// sort vertices by value
		Collections.sort(_currentConfig.speedVertices);
	}

	private void checkTourTypes() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean close() {

//		_prefStore.removePropertyChangeListener(_prefChangeListener);

		return super.close();
	}

	@Override
	protected void configureShell(final Shell shell) {

		super.configureShell(shell);

		shell.setText(Messages.Dialog_AutoImportConfig_Dialog_Title);

		shell.addListener(SWT.Resize, new Listener() {
			@Override
			public void handleEvent(final Event event) {

				// ensure that the dialog is not smaller than the default size

				final Point shellDefaultSize = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT);

				final Point shellSize = shell.getSize();

				shellSize.x = shellSize.x < shellDefaultSize.x ? shellDefaultSize.x : shellSize.x;
				shellSize.y = shellSize.y < shellDefaultSize.y ? shellDefaultSize.y : shellSize.y;

				shell.setSize(shellSize);
			}
		});
	}

	@Override
	public void create() {

		super.create();

		setTitle(Messages.Dialog_AutoImportConfig_Dialog_Title);
		setMessage(Messages.Dialog_AutoImportConfig_Dialog_Message);

		addPrefChangeListener();

		restoreState();

		_configViewer.setInput(new Object());

		// prevent that the horizontal scrollbar is visible
		_configViewer.getTable().getParent().layout();

		_configViewer.setSelection(new StructuredSelection(_dialogConfigs.get(0)));
	}

	private void createActions() {

		_action_VertexAdd = new ActionAddVertex();
		_action_VertexSort = new ActionSortVertices();

		_actionOpenTourTypePrefs = new ActionOpenPrefDialog(
				Messages.action_tourType_modify_tourTypes,
				ITourbookPreferences.PREF_PAGE_TOUR_TYPE);
	}

	/**
	 * Creates a configuration from the {@link #DEFAULT_VERTICES}.
	 * 
	 * @return Returns the created config.
	 */
	private AutomatedImportConfig createDefaultConfig() {

		final AutomatedImportConfig defaultConfig = new AutomatedImportConfig();
		final ArrayList<SpeedVertex> speedVertices = defaultConfig.speedVertices;

		for (final SpeedVertex speedVertex : DEFAULT_VERTICES) {
			speedVertices.add(speedVertex.clone());
		}

		_dialogConfigs.add(defaultConfig);

		return defaultConfig;
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		_parent = parent;

		final Composite ui = (Composite) super.createDialogArea(parent);

		initUI(ui);
		createActions();

		createUI(ui);

		return ui;
	}

	/**
	 * create the drop down menus, this must be created after the parent control is created
	 */

	private void createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.swtDefaults()//
				.numColumns(2)
//				.spacing(10, 8)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		{
			final Composite configContainer = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.span(2, 1)
					.hint(convertWidthInCharsToPixels(30), convertVerticalDLUsToPixels(66))
					.applyTo(configContainer);
			GridLayoutFactory.fillDefaults()//
					.numColumns(2)
					.applyTo(configContainer);
			{
				createUI_22_ConfigViewer(configContainer);
				createUI_24_ConfigActions(configContainer);
			}

			createUI_40_Name(container);
			createUI_50_DeviceFolder(container);
			createUI_80_TourType(container);
		}
	}

	private void createUI_22_ConfigViewer(final Composite parent) {

		final TableColumnLayout tableLayout = new TableColumnLayout();

		final Composite layoutContainer = new Composite(parent, SWT.NONE);
		layoutContainer.setLayout(tableLayout);
		GridDataFactory.fillDefaults() //
				.grab(true, true)
//				.hint(SWT.DEFAULT, convertHeightInCharsToPixels(5))
				.applyTo(layoutContainer);

		/*
		 * create table
		 */
		final Table table = new Table(layoutContainer, (/* SWT.H_SCROLL | */SWT.V_SCROLL
				| SWT.BORDER
				| SWT.FULL_SELECTION | SWT.MULTI));

		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		_configViewer = new TableViewer(table);
		defineAllColumns(tableLayout);

		_configViewer.setUseHashlookup(true);
		_configViewer.setContentProvider(new ClientsContentProvider());

		_configViewer.setComparator(new ViewerComparator() {
			@Override
			public int compare(final Viewer viewer, final Object e1, final Object e2) {

				// compare by name

				final AutomatedImportConfig p1 = (AutomatedImportConfig) e1;
				final AutomatedImportConfig p2 = (AutomatedImportConfig) e2;

				return p1.name.compareTo(p2.name);
			}
		});

		_configViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				onConfig_Select(event.getSelection());
			}
		});

		_configViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(final DoubleClickEvent event) {
//				_tabFolderPerson.setSelection(0);
//				_txtFirstName.setFocus();
//				_txtFirstName.selectAll();
			}
		});

	}

	private void createUI_24_ConfigActions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
//				.grab(false, true)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			/*
			 * button: add
			 */
			_btnConfig_Add = new Button(container, SWT.NONE);
			_btnConfig_Add.setText(Messages.App_Action_Add);
			setButtonLayoutData(_btnConfig_Add);
			_btnConfig_Add.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onConfig_Add();
				}
			});

			/*
			 * button: remove
			 */
			_btnConfig_Remove = new Button(container, SWT.NONE);
			_btnConfig_Remove.setText(Messages.App_Action_Remove);
			setButtonLayoutData(_btnConfig_Remove);
			_btnConfig_Remove.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onConfig_Remove();
				}
			});

		}
	}

	private void createUI_40_Name(final Composite parent) {

		/*
		 * Config name
		 */

		// label
		_lblConfigName = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.align(SWT.FILL, SWT.CENTER)
				.applyTo(_lblConfigName);
		_lblConfigName.setText(Messages.Dialog_AutoImportConfig_Label_ConfigName);

		// text
		_txtConfigName = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.applyTo(_txtConfigName);
	}

	private void createUI_50_DeviceFolder(final Composite parent) {

		/*
		 * Label: device folder
		 */
		_lblDeviceFolder = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.align(SWT.FILL, SWT.CENTER)
				.applyTo(_lblDeviceFolder);
		_lblDeviceFolder.setText(Messages.Dialog_AutoImportConfig_Label_DeviceFolder);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			/*
			 * Combo: path
			 */
			_comboDevicePath = new Combo(container, SWT.SINGLE | SWT.BORDER);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.align(SWT.FILL, SWT.CENTER)
//					.hint(_pc.convertWidthInCharsToPixels(40), SWT.DEFAULT)
					.applyTo(_comboDevicePath);
			_comboDevicePath.setVisibleItemCount(20);
			_comboDevicePath.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(final ModifyEvent e) {
					validateFields();
				}
			});
			_comboDevicePath.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					validateFields();
				}
			});

			/*
			 * Button: browse...
			 */
			_btnSelectDeviceFolder = new Button(container, SWT.PUSH);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(_btnSelectDeviceFolder);
			_btnSelectDeviceFolder.setText(Messages.app_btn_browse);
			_btnSelectDeviceFolder.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectBrowseDirectory();
					validateFields();
				}
			});
			setButtonLayoutData(_btnSelectDeviceFolder);
		}
	}

	private void createUI_80_TourType(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.span(2, 1)
				.hint(SWT.DEFAULT, convertVerticalDLUsToPixels(92))
				.applyTo(group);
		GridLayoutFactory.swtDefaults()//
				.margins(5, 0)
				.applyTo(group);
//		group.setText(Messages.Dialog_AutoImportConfig_Group_TourType);
//		group.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
		{
			createUI_82_EnableTourType(group);
			createUI_84_VertexFields(group);
		}
	}

	private void createUI_82_EnableTourType(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.numColumns(2)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
		{
			// ckeckbox: enable min/max
			_chkTourType = new Button(container, SWT.CHECK);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.applyTo(_chkTourType);
			_chkTourType.setText(Messages.Dialog_AutoImportConfig_Checkbox_SetTourType);
			_chkTourType.addSelectionListener(_defaultSelectionListener);

			createUI_86_VertexActions(container);
		}
	}

	private void createUI_84_VertexFields(final Composite parent) {

		/*
		 * vertex fields container
		 */
		_vertexOuterContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.indent(_pc.convertHorizontalDLUsToPixels(10), 0)
				.applyTo(_vertexOuterContainer);

		GridLayoutFactory.fillDefaults().applyTo(_vertexOuterContainer);
//		_vertexOuterContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));

		createUI_88_VertexFields();
	}

	private void createUI_86_VertexActions(final Composite parent) {

		final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);

		final ToolBarManager tbm = new ToolBarManager(toolbar);

		tbm.add(_action_VertexAdd);
		tbm.add(_action_VertexSort);

		tbm.update(true);
	}

	/**
	 * Create the vertex fields from the vertex list
	 * 
	 * @param parent
	 */
	private void createUI_88_VertexFields() {

		if (_currentConfig == null) {

			if (_vertexScrolledContainer != null) {

				_vertexScrolledContainer.dispose();
				_vertexScrolledContainer = null;

				_actionVertex_Delete = null;
				_labelVertex_TourTypeIcon = null;
				_labelVertex_SpeedUnit = null;
				_linkVertex_TourType = null;
				_spinnerVertex_AvgSpeed = null;
			}

			return;
		}

		final int vertexSize = _currentConfig.speedVertices.size();

		// check if required vertex fields are already available
		if (_spinnerVertex_AvgSpeed != null && _spinnerVertex_AvgSpeed.length == vertexSize) {
			return;
		}

		Point scrollOrigin = null;

		// dispose previous content
		if (_vertexScrolledContainer != null) {

			// get current scroll position
			scrollOrigin = _vertexScrolledContainer.getOrigin();

			_vertexScrolledContainer.dispose();
		}

		_vertexContainer = createUI_89_VertexScrolledContainer(_vertexOuterContainer);
//		_vertexContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));

		/*
		 * fields
		 */
		_actionVertex_Delete = new ActionDeleteVertex[vertexSize];
		_labelVertex_TourTypeIcon = new Label[vertexSize];
		_labelVertex_SpeedUnit = new Label[vertexSize];
		_linkVertex_TourType = new Link[vertexSize];
		_spinnerVertex_AvgSpeed = new Spinner[vertexSize];

		_vertexContainer.setRedraw(false);
		{
			for (int vertexIndex = 0; vertexIndex < vertexSize; vertexIndex++) {

				/*
				 * Spinner: Speed value
				 */
				final Spinner spinnerValue = new Spinner(_vertexContainer, SWT.BORDER);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(spinnerValue);
				spinnerValue.setMinimum(RawDataManager.CONFIG_SPEED_MIN);
				spinnerValue.setMaximum(RawDataManager.CONFIG_SPEED_MAX);
				spinnerValue.setToolTipText(Messages.Dialog_AutoImportConfig_Spinner_Speed_Tooltip);
				spinnerValue.addMouseWheelListener(_vertexValueMouseWheelListener);

				/*
				 * Label: Speed unit
				 */
				final Label lblUnit = new Label(_vertexContainer, SWT.NONE);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(lblUnit);
				lblUnit.setText(UI.UNIT_LABEL_SPEED);

				/*
				 * Label with icon: Tour type (CLabel cannot be disabled !!!)
				 */
				final Label lblTourTypeIcon = new Label(_vertexContainer, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.hint(16, 16)
						.applyTo(lblTourTypeIcon);

				/*
				 * Link: Tour type
				 */
				final Link linkTourType = new Link(_vertexContainer, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.grab(true, false)
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(linkTourType);
				linkTourType.setText(Messages.tour_editor_label_tour_type);
				linkTourType.addSelectionListener(_vertexTourTypeListener);
//				linkTourType.setBackground(bgColor);

				/*
				 * Context menu: Tour type
				 */
				final MenuManager menuMgr = new MenuManager();
				menuMgr.setRemoveAllWhenShown(true);
				menuMgr.addMenuListener(new IMenuListener() {
					@Override
					public void menuAboutToShow(final IMenuManager menuMgr) {
						fillTourTypeMenu(menuMgr, linkTourType);
					}
				});
				final Menu ttContextMenu = menuMgr.createContextMenu(linkTourType);
				linkTourType.setMenu(ttContextMenu);

				/*
				 * Action: Delete vertex
				 */
				final ActionDeleteVertex actionDeleteVertex = new ActionDeleteVertex();
				createUI_ActionButton(_vertexContainer, actionDeleteVertex);

				/*
				 * Keep vertex controls
				 */
				_actionVertex_Delete[vertexIndex] = actionDeleteVertex;
				_labelVertex_TourTypeIcon[vertexIndex] = lblTourTypeIcon;
				_labelVertex_SpeedUnit[vertexIndex] = lblUnit;
				_linkVertex_TourType[vertexIndex] = linkTourType;
				_spinnerVertex_AvgSpeed[vertexIndex] = spinnerValue;
			}
		}
		_vertexContainer.setRedraw(true);

		_vertexOuterContainer.layout(true);

		// set scroll position to previous position
		if (scrollOrigin != null) {
			_vertexScrolledContainer.setOrigin(scrollOrigin);
		}
	}

	private Composite createUI_89_VertexScrolledContainer(final Composite parent) {

		// scrolled container
		_vertexScrolledContainer = new ScrolledComposite(parent, SWT.V_SCROLL);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_vertexScrolledContainer);
		_vertexScrolledContainer.setExpandVertical(true);
		_vertexScrolledContainer.setExpandHorizontal(true);

		// vertex container
		final Composite vertexContainer = new Composite(_vertexScrolledContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(vertexContainer);
		GridLayoutFactory.fillDefaults()//
				.numColumns(5)
				.applyTo(vertexContainer);
//		vertexContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));

		_vertexScrolledContainer.setContent(vertexContainer);
		_vertexScrolledContainer.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				_vertexScrolledContainer.setMinSize(vertexContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
		});

		return vertexContainer;
	}

	/**
	 * Creates an action in a toolbar.
	 * 
	 * @param parent
	 * @param action
	 * @return
	 */
	private ToolBarManager createUI_ActionButton(final Composite parent, final Action action) {

		final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);

		final ToolBarManager tbm = new ToolBarManager(toolbar);
		tbm.add(action);
		tbm.update(true);

		return tbm;
	}

	private void defineAllColumns(final TableColumnLayout tableLayout) {

		TableViewerColumn tvc;
		TableColumn tc;

		/*
		 * column: name
		 */
		tvc = new TableViewerColumn(_configViewer, SWT.LEAD);
		tc = tvc.getColumn();
		tc.setText(Messages.Dialog_AutoImportConfig_Column_Name);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				cell.setText(((AutomatedImportConfig) cell.getElement()).name);
			}
		});
		tableLayout.setColumnData(tc, new ColumnWeightData(5, convertWidthInCharsToPixels(5)));

	}

	private void enableControls() {

		final Object selectedConfig = ((StructuredSelection) _configViewer.getSelection()).getFirstElement();

		final boolean isConfigSelected = selectedConfig != null;
		final boolean isTourTypeSelected = isConfigSelected && _chkTourType.getSelection();

		if (_actionVertex_Delete != null) {

			for (final ActionDeleteVertex action : _actionVertex_Delete) {
				action.setEnabled(isTourTypeSelected);
			}

			for (final Spinner spinner : _spinnerVertex_AvgSpeed) {
				spinner.setEnabled(isTourTypeSelected);
			}

			for (final Link link : _linkVertex_TourType) {
				link.setEnabled(isTourTypeSelected);
			}

			for (final Label label : _labelVertex_SpeedUnit) {
				label.setEnabled(isTourTypeSelected);
			}

			for (final Label label : _labelVertex_TourTypeIcon) {

				if (isTourTypeSelected) {

					final Integer vertexIndex = (Integer) label.getData(DATA_KEY_VERTEX_INDEX);

					final SpeedVertex vertex = _currentConfig.speedVertices.get(vertexIndex);
					final long tourTypeId = vertex.tourTypeId;

					label.setImage(net.tourbook.ui.UI.getInstance().getTourTypeImage(tourTypeId));

				} else {

					// the disabled image looks very ugly
					label.setImage(null);
				}
			}
		}

		_action_VertexAdd.setEnabled(isTourTypeSelected);
		_action_VertexSort.setEnabled(isTourTypeSelected);

		_btnConfig_Remove.setEnabled(isConfigSelected);
		_btnSelectDeviceFolder.setEnabled(isConfigSelected);
		_chkTourType.setEnabled(isConfigSelected);
		_comboDevicePath.setEnabled(isConfigSelected);
		_lblConfigName.setEnabled(isConfigSelected);
		_txtConfigName.setEnabled(isConfigSelected);
	}

	private void fillTourTypeMenu(final IMenuManager menuMgr, final Link linkTourType) {

		// get tour type which will be checked in the menu
		final TourType checkedTourType = null;

		final int vertexIndex = (int) linkTourType.getData(DATA_KEY_VERTEX_INDEX);

		// add all tour types to the menu
		final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();

		for (final TourType tourType : tourTypes) {

			boolean isChecked = false;
			if (checkedTourType != null && checkedTourType.getTypeId() == tourType.getTypeId()) {
				isChecked = true;
			}

			final ActionSetTourType action = new ActionSetTourType(tourType, isChecked, vertexIndex);

			menuMgr.add(action);
		}

		menuMgr.add(new Separator());
		menuMgr.add(_actionOpenTourTypePrefs);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		// keep window size and position
		return _state;
//		return null;
	}

	ArrayList<AutomatedImportConfig> getModifiedConfigs() {

		return _dialogConfigs;
	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);

		/*
		 * Field listener
		 */
		_defaultSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelectDefault();
			}
		};

		/*
		 * Vertex listener
		 */
		_vertexValueMouseWheelListener = new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
			}
		};

		_vertexTourTypeListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				UI.openControlMenu((Link) event.widget);
			}
		};
	}

	@Override
	protected void okPressed() {

		saveState();

		super.okPressed();
	}

	private void onConfig_Add() {

		// keep modifications
		update_Model_From_UI();

		// update model

		AutomatedImportConfig newConfig;
		if (_dialogConfigs.size() == 0) {

			/*
			 * Setup default config
			 */

			newConfig = createDefaultConfig();

		} else {

			newConfig = new AutomatedImportConfig();

			_dialogConfigs.add(newConfig);
		}

		// update UI
		_configViewer.refresh();

		// prevent that the horizontal scrollbar is visible
		_configViewer.getTable().getParent().layout();

		_configViewer.setSelection(new StructuredSelection(newConfig), true);

		_txtConfigName.setFocus();
	}

	private void onConfig_Remove() {

		final StructuredSelection selection = (StructuredSelection) _configViewer.getSelection();
		final AutomatedImportConfig selectedConfig = (AutomatedImportConfig) selection.getFirstElement();

		int selectedIndex = 0;

		// get index of the selected config
		for (int configIndex = 0; configIndex < _dialogConfigs.size(); configIndex++) {

			final AutomatedImportConfig config = _dialogConfigs.get(configIndex);

			if (config.equals(selectedConfig)) {
				selectedIndex = configIndex;
				break;
			}
		}

		// update model
		_dialogConfigs.remove(selectedIndex);

		// update UI
		_configViewer.refresh();

		// select config at the same position

		if (_dialogConfigs.size() == 0) {

			// all configs are removed

			_currentConfig = null;

			/*
			 * Update UI
			 */

			// remove vertex fields
			createUI_88_VertexFields();

			enableControls();

		} else {

			if (selectedIndex >= _dialogConfigs.size()) {
				selectedIndex--;
			}

			final AutomatedImportConfig nextConfig = _dialogConfigs.get(selectedIndex);

			_configViewer.setSelection(new StructuredSelection(nextConfig), true);
		}
	}

	private void onConfig_Select(final ISelection selection) {

		final AutomatedImportConfig selectedConfig = (AutomatedImportConfig) ((StructuredSelection) selection)
				.getFirstElement();

		if (_currentConfig == selectedConfig) {
			// this is already selected
			return;
		}

		// update model from the old selected config
		update_Model_From_UI();

		// set model
		_currentConfig = selectedConfig;

		update_UI_From_Model();

		enableControls();
	}

	private void onSelectBrowseDirectory() {

		final DirectoryDialog dialog = new DirectoryDialog(_parent.getShell(), SWT.SAVE);
		dialog.setText(Messages.dialog_export_dir_dialog_text);
		dialog.setMessage(Messages.dialog_export_dir_dialog_message);

//		dialog.setFilterPath(getExportPathName());

		final String selectedDirectoryName = dialog.open();

		if (selectedDirectoryName != null) {
			setErrorMessage(null);
			_comboDevicePath.setText(selectedDirectoryName);
		}
	}

	private void onSelectDefault() {

		enableControls();
	}

	private void onVertex_Add() {

		// update model
		addVertex(0, new SpeedVertex());

		// update UI + model
		update_UI_From_Model();
		update_Model_From_UI();
	}

	private void onVertex_Remove(final int vertexIndex) {

		final ArrayList<SpeedVertex> speedVertices = _currentConfig.speedVertices;

		// update model
		final SpeedVertex removedVertex = speedVertices.get(vertexIndex);

		speedVertices.remove(removedVertex);

		// update UI + model
		update_UI_From_Model();
		update_Model_From_UI();
	}

	private void onVertex_SelectTourType(final int vertexIndex, final TourType tourType) {

		// update model
		_currentConfig.speedVertices.get(vertexIndex).tourTypeId = tourType.getTypeId();

		/*
		 * Update UI
		 */
		final Image image = net.tourbook.ui.UI.getInstance().getTourTypeImage(tourType.getTypeId());

		final Label ttIcon = _labelVertex_TourTypeIcon[vertexIndex];
		final Link ttLink = _linkVertex_TourType[vertexIndex];

		ttIcon.setImage(image);
		ttLink.setText(UI.LINK_TAG_START + tourType.getName() + UI.LINK_TAG_END);

		_vertexOuterContainer.layout();
	}

	private void onVertex_Sort() {

		update_Model_From_UI();
		update_UI_From_Model();
	}

	private void restoreState() {

	}

	private void saveState() {

		update_Model_From_UI();
	}

	private void setupImportConfigs(final ArrayList<AutomatedImportConfig> importConfigs) {

		/*
		 * Clone original configs, only the backup will be modified in the dialog.
		 */
		_dialogConfigs = new ArrayList<>();

		for (final AutomatedImportConfig importConfig : importConfigs) {
			_dialogConfigs.add(importConfig.clone());
		}

		if (_dialogConfigs.size() == 0) {

			/*
			 * Setup default config
			 */

			createDefaultConfig();
		}
	}

	/**
	 * Get vertices from UI and sort them.
	 */
	private void update_Model_From_UI() {

		if (_currentConfig == null) {
			return;
		}

		_currentConfig.name = _txtConfigName.getText();
		_currentConfig.isSetTourType = _chkTourType.getSelection();

		final ArrayList<SpeedVertex> speedVertices = _currentConfig.speedVertices;
		final ArrayList<SpeedVertex> newVertices = new ArrayList<SpeedVertex>();

		for (int vertexIndex = 0; vertexIndex < speedVertices.size(); vertexIndex++) {

			/*
			 * create vertices from UI controls
			 */
			final Spinner spinnerAvgSpeed = _spinnerVertex_AvgSpeed[vertexIndex];
			final Link linkTourType = _linkVertex_TourType[vertexIndex];

			final SpeedVertex speedVertex = new SpeedVertex();

			speedVertex.avgSpeed = spinnerAvgSpeed.getSelection();
			speedVertex.tourTypeId = (long) linkTourType.getData(DATA_KEY_TOUR_TYPE_ID);

			newVertices.add(speedVertex);
		}

		// sort vertices by value
		Collections.sort(newVertices);

		// update model
		speedVertices.clear();
		speedVertices.addAll(newVertices);
	}

	private void update_UI_From_Model() {

		if (_currentConfig == null) {
			return;
		}

		_configViewer.update(_currentConfig, null);

		_chkTourType.setSelection(_currentConfig.isSetTourType);
		_txtConfigName.setText(_currentConfig.name);

		// check and create vertex fields
		createUI_88_VertexFields();

		final ArrayList<SpeedVertex> speedVertices = _currentConfig.speedVertices;

		final int vertexSize = speedVertices.size();

		final net.tourbook.ui.UI uiInstance = net.tourbook.ui.UI.getInstance();

		for (int vertexIndex = 0; vertexIndex < vertexSize; vertexIndex++) {

			final SpeedVertex vertex = speedVertices.get(vertexIndex);
			final long tourTypeId = vertex.tourTypeId;

			final Spinner spinnerAvgSpeed = _spinnerVertex_AvgSpeed[vertexIndex];
			final Link linkTourType = _linkVertex_TourType[vertexIndex];
			final Label labelTourTypeIcon = _labelVertex_TourTypeIcon[vertexIndex];

			// update UI
			spinnerAvgSpeed.setSelection(vertex.avgSpeed);
			linkTourType.setText(UI.LINK_TAG_START + uiInstance.getTourTypeLabel(tourTypeId) + UI.LINK_TAG_END);
			labelTourTypeIcon.setImage(uiInstance.getTourTypeImage(tourTypeId));

			// keep vertex references
			labelTourTypeIcon.setData(DATA_KEY_VERTEX_INDEX, vertexIndex);
			linkTourType.setData(DATA_KEY_VERTEX_INDEX, vertexIndex);
			spinnerAvgSpeed.setData(DATA_KEY_VERTEX_INDEX, vertexIndex);
			_actionVertex_Delete[vertexIndex].setData(DATA_KEY_VERTEX_INDEX, vertexIndex);

			linkTourType.setData(DATA_KEY_TOUR_TYPE_ID, tourTypeId);
		}
	}

	private void validateFields() {
		// TODO Auto-generated method stub

	}

}
