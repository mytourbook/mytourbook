/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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
package net.tourbook.tag.tour.filter;

import gnu.trove.set.hash.TLongHashSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.form.SashLeftFixedForm;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.common.util.ITreeViewer;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourTagCategory;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.PrefPageTags;
import net.tourbook.tag.TVIPrefTag;
import net.tourbook.tag.TVIPrefTagCategory;
import net.tourbook.tag.TVIPrefTagRoot;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.action.ActionCollapseAll;
import net.tourbook.ui.action.ActionExpandAll;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;

/**
 * Slideout for the tour tag filter
 */
public class SlideoutTourTagFilter extends AdvancedSlideout implements ITreeViewer {

	private static final String		STATE_IS_HIERARCHICAL_LAYOUT	= "STATE_IS_HIERARCHICAL_LAYOUT";	//$NON-NLS-1$
	private static final String		STATE_IS_LIVE_UPDATE			= "STATE_IS_LIVE_UPDATE";			//$NON-NLS-1$
	private static final String		STATE_SASH_WIDTH_CONTAINER		= "STATE_SASH_WIDTH_CONTAINER";		//$NON-NLS-1$
	private static final String		STATE_SASH_WIDTH_TAG_CONTAINER	= "STATE_SASH_WIDTH_TAG_CONTAINER";	//$NON-NLS-1$

	private static final Object[]	EMPTY_LIST						= new Object[] {};
	private static final long[]		NO_TAGS							= new long[] {};

	private static IDialogSettings	_state;

	{}

	private final ArrayList<TourTagFilterProfile>	_profiles		= TourTagFilterManager.getProfiles();

	private TableViewer								_profileViewer;
	private TourTagFilterProfile					_selectedProfile;

	private ContainerCheckedTreeViewer				_tagViewer;
	private TVIPrefTagRoot							_tagViewerRootItem;

	private CheckboxTableViewer						_tagCloudViewer;
	private ArrayList<TagCloud>						_tagCloudItems	= new ArrayList<>();

	private ToolItem								_tourTagFilterItem;

	private ModifyListener							_defaultModifyListener;
	private ITourEventListener						_tourEventListener;
	{
		_defaultModifyListener = new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent e) {
				onProfile_Modify();
			}
		};
	}

	private boolean								_tagViewerItem_IsChecked;
	private boolean								_tagViewerItem_IsKeyPressed;
	private Object								_tagViewerItem_Data;

	private boolean								_tagCloudViewerItem_IsChecked;
	private boolean								_tagCloudViewerItem_IsKeyPressed;
	private Object								_tagCloudViewerItem_Data;

	private long								_expandRunnableCounter;
	private boolean								_isExpandingSelection;
	private boolean								_isBehaviourSingleExpandedOthersCollapse	= true;
	private boolean								_isBehaviourAutoExpandCollapse				= true;
	private boolean								_isInCollapseAll;
	private boolean								_isHierarchicalLayout;
	private boolean								_isLiveUpdate;

	private PixelConverter						_pc;

	private ActionCollapseAllWithoutSelection	_actionCollapseAll;
	private ActionExpandAll						_actionExpandAll;
	private ActionOpenPrefDialog				_actionOpenPrefTags;
	private ActionTag_LayoutFlat				_actionTag_LayoutFlat;
	private ActionTag_LayoutHierarchical		_actionTag_LayoutHierarchical;
	private ActionTagCloud_CheckAllTags			_actionTagCloud_CheckAll;
	private ActionTagCloud_UncheckAllTags		_actionTagCloud_UncheckAll;

	/*
	 * UI controls
	 */
	private Button								_btnApply;
	private Button								_btnCopyProfile;
	private Button								_btnDeleteProfile;
	private Button								_btnNewProfile;
	private Button								_chkLiveUpdate;

	private Label								_lblAllTags;
	private Label								_lblProfileName;
	private Label								_lblSelectTags;

	private Image								_imgTag;
	private Image								_imgTagRoot;
	private Image								_imgTagCategory;

	private Text								_txtProfileName;

	private ToolBar								_toolBarAllTags;
	private ToolBar								_toolBarTagCloud;

	private class ActionCollapseAllWithoutSelection extends ActionCollapseAll {

		public ActionCollapseAllWithoutSelection(final ITreeViewer treeViewerProvider) {
			super(treeViewerProvider);
		}

		@Override
		public void run() {

			_isInCollapseAll = true;
			{
				super.run();
			}
			_isInCollapseAll = false;
		}

	}

	private class ActionTag_LayoutFlat extends Action {

		ActionTag_LayoutFlat() {

			super(Messages.action_tagView_flat_layout, AS_RADIO_BUTTON);

			setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__layout_flat));
		}

		@Override
		public void run() {
			onTag_Layout(false);
		}
	}

	private class ActionTag_LayoutHierarchical extends Action {

		ActionTag_LayoutHierarchical() {

			super(Messages.action_tagView_flat_hierarchical, AS_RADIO_BUTTON);

			setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__layout_hierarchical));
		}

		@Override
		public void run() {
			onTag_Layout(true);
		}
	}

	private class ActionTagCloud_CheckAllTags extends Action {

		public ActionTagCloud_CheckAllTags() {

			super();

			setToolTipText(Messages.Slideout_TourTagFilter_Action_CheckAllTags_Tooltip);

			setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_Checkbox_Checked));
			setDisabledImageDescriptor(
					TourbookPlugin.getImageDescriptor(Messages.Image__App_Checkbox_Checked_Disabled));
		}

		@Override
		public void run() {
			onTagCloud_Checkbox_CheckAll();
		}
	}

	private class ActionTagCloud_UncheckAllTags extends Action {

		public ActionTagCloud_UncheckAllTags() {

			super();

			setToolTipText(Messages.Slideout_TourTagFilter_Action_UncheckAllTags_Tooltip);

			setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_Checkbox_Uncheck));
			setDisabledImageDescriptor(
					TourbookPlugin.getImageDescriptor(Messages.Image__App_Checkbox_Uncheck_Disabled));
		}

		@Override
		public void run() {
			onTagCloud_Checkbox_UncheckAll();
		}
	}

	private class ProfileComparator extends ViewerComparator {

		@Override
		public int compare(final Viewer viewer, final Object e1, final Object e2) {

			if (e1 == null || e2 == null) {
				return 0;
			}

			final TourTagFilterProfile profile1 = (TourTagFilterProfile) e1;
			final TourTagFilterProfile profile2 = (TourTagFilterProfile) e2;

			return profile1.name.compareTo(profile2.name);
		}

		@Override
		public boolean isSorterProperty(final Object element, final String property) {

			// force resorting when a name is renamed
			return true;
		}
	}

	private class ProfileProvider implements IStructuredContentProvider {

		@Override
		public void dispose() {}

		@Override
		public Object[] getElements(final Object inputElement) {
			return _profiles.toArray();
		}

		@Override
		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	private class TagCloud {

		long	tagId;
		String	tagName;

		TagCloud(final long tagId, final String tagName) {

			this.tagId = tagId;
			this.tagName = tagName;
		}
	}

	private class TagCloudComparator extends ViewerComparator {

		@Override
		public int compare(final Viewer viewer, final Object e1, final Object e2) {

			if (e1 == null || e2 == null) {
				return 0;
			}

			final TagCloud tagCloud1 = (TagCloud) e1;
			final TagCloud tagCloud2 = (TagCloud) e2;

			return tagCloud1.tagName.compareTo(tagCloud2.tagName);
		}

		@Override
		public boolean isSorterProperty(final Object element, final String property) {

			// force resorting when a name is renamed
			return true;
		}
	}

	private class TagCloudProvider implements IStructuredContentProvider {

		@Override
		public void dispose() {}

		@Override
		public Object[] getElements(final Object inputElement) {
			return _tagCloudItems.toArray();
		}

		@Override
		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	/**
	 * Sort the tags and categories
	 */
	private final static class TagViewerComparator extends ViewerComparator {
		@Override
		public int compare(final Viewer viewer, final Object obj1, final Object obj2) {
			if (obj1 instanceof TVIPrefTag && obj2 instanceof TVIPrefTag) {

				// sort tags by name
				final TourTag tourTag1 = ((TVIPrefTag) (obj1)).getTourTag();
				final TourTag tourTag2 = ((TVIPrefTag) (obj2)).getTourTag();

				return tourTag1.getTagName().compareTo(tourTag2.getTagName());

			} else if (obj1 instanceof TVIPrefTag && obj2 instanceof TVIPrefTagCategory) {

				// sort category before tag
				return 1;

			} else if (obj2 instanceof TVIPrefTag && obj1 instanceof TVIPrefTagCategory) {

				// sort category before tag
				return -1;

			} else if (obj1 instanceof TVIPrefTagCategory && obj2 instanceof TVIPrefTagCategory) {

				// sort categories by name
				final TourTagCategory tourTagCat1 = ((TVIPrefTagCategory) (obj1)).getTourTagCategory();
				final TourTagCategory tourTagCat2 = ((TVIPrefTagCategory) (obj2)).getTourTagCategory();

				return tourTagCat1.getCategoryName().compareTo(tourTagCat2.getCategoryName());
			}

			return 0;
		}

		@Override
		public boolean isSorterProperty(final Object element, final String property) {
			// sort when the name has changed
			return true;
		}
	}

	private final class TagViewerContentProvicer implements ITreeContentProvider {

		@Override
		public void dispose() {}

		@Override
		public Object[] getChildren(final Object parentElement) {
			return ((TreeViewerItem) parentElement).getFetchedChildrenAsArray();
		}

		@Override
		public Object[] getElements(final Object inputElement) {
			return _tagViewerRootItem.getFetchedChildrenAsArray();
		}

		@Override
		public Object getParent(final Object element) {
			return ((TreeViewerItem) element).getParentItem();
		}

		@Override
		public boolean hasChildren(final Object element) {
			return ((TreeViewerItem) element).hasChildren();
		}

		@Override
		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	/**
	 * @param toolItem
	 * @param state
	 */
	public SlideoutTourTagFilter(	final ToolItem toolItem,
									final IDialogSettings state) {

		super(
				toolItem.getParent(),
				state,
				new int[] { 700, 400, 700, 400 });

		_tourTagFilterItem = toolItem;
		_state = state;

		setShellFadeOutDelaySteps(30);
		setTitleText(Messages.Slideout_TourTagFilter_Label_Title);
	}

	private void addTourEventListener() {

		_tourEventListener = new ITourEventListener() {
			@Override
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if (eventId == TourEventId.TAG_STRUCTURE_CHANGED) {

					updateTagModel();

					// reselect profile
					onProfile_Select(false);
				}
			}
		};

		TourManager.getInstance().addTourEventListener(_tourEventListener);
	}

	private void createActions() {

		_actionExpandAll = new ActionExpandAll(this);
		_actionCollapseAll = new ActionCollapseAllWithoutSelection(this);
		_actionOpenPrefTags = new ActionOpenPrefDialog(Messages.action_tag_open_tagging_structure, PrefPageTags.ID);
		_actionTag_LayoutFlat = new ActionTag_LayoutFlat();
		_actionTag_LayoutHierarchical = new ActionTag_LayoutHierarchical();
		_actionTagCloud_CheckAll = new ActionTagCloud_CheckAllTags();
		_actionTagCloud_UncheckAll = new ActionTagCloud_UncheckAllTags();

	}

	@Override
	protected void createSlideoutContent(final Composite parent) {

		// reset to a valid state when the slideout is opened again
		_selectedProfile = null;

		initUI(parent);

		createUI(parent);

		createActions();
		fillToolbar();

		addTourEventListener();

		restoreStateBeforeUI();

		// load profile viewer
		_profileViewer.setInput(new Object());

		// load tag viewer
		updateTagModel();

		restoreState();
		enableControls();
	}

	private void createUI(final Composite parent) {

		final Composite shellContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(shellContainer);
		GridLayoutFactory.fillDefaults().applyTo(shellContainer);
		{
			final Composite sashContainer = new Composite(shellContainer, SWT.NONE);
			GridDataFactory
					.fillDefaults()//
					.grab(true, true)
					.applyTo(sashContainer);
			GridLayoutFactory.swtDefaults().applyTo(sashContainer);
			{
				// left part
				final Composite containerProfiles = createUI_200_Profiles(sashContainer);

				// sash
				final Sash sash = new Sash(sashContainer, SWT.VERTICAL);

				// right part
				final Composite containerTags = createUI_300_Tags(sashContainer);

				new SashLeftFixedForm(//
						sashContainer,
						containerProfiles,
						sash,
						containerTags,
						_state,
						STATE_SASH_WIDTH_CONTAINER,
						30);
			}

			createUI_800_Actions(shellContainer);
		}
	}

	private Composite createUI_200_Profiles(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()
				//				.hint(_pc.convertWidthInCharsToPixels(30), SWT.DEFAULT)
				.applyTo(container);
		GridLayoutFactory
				.fillDefaults()//
				.numColumns(1)
				.extendedMargins(0, 3, 0, 0)
				.applyTo(container);
		{
			{
				// label: Profiles

				final Label label = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults().applyTo(label);
				label.setText(Messages.Slideout_TourFilter_Label_Profiles);
			}

			createUI_210_ProfileViewer(container);
		}

		return container;
	}

	private void createUI_210_ProfileViewer(final Composite parent) {

		final Composite layoutContainer = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()
				.grab(true, true)
				.hint(_pc.convertWidthInCharsToPixels(15), _pc.convertHeightInCharsToPixels(8))
				.applyTo(layoutContainer);

		final TableColumnLayout tableLayout = new TableColumnLayout();
		layoutContainer.setLayout(tableLayout);

		/*
		 * create table
		 */
		final Table table = new Table(layoutContainer, SWT.FULL_SELECTION);

		table.setLayout(new TableLayout());

		// !!! this prevents that the horizontal scrollbar is displayed, but is not always working :-(
		table.setHeaderVisible(true);

		_profileViewer = new TableViewer(table);

		/*
		 * create columns
		 */
		TableViewerColumn tvc;
		TableColumn tc;

		{
			// Column: Profile name

			tvc = new TableViewerColumn(_profileViewer, SWT.LEAD);
			tc = tvc.getColumn();
			tc.setText(Messages.Slideout_TourFilter_Column_ProfileName);
			tvc.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(final ViewerCell cell) {

					final TourTagFilterProfile profile = (TourTagFilterProfile) cell.getElement();

					cell.setText(profile.name);
				}
			});
			tableLayout.setColumnData(tc, new ColumnWeightData(1, false));
		}
		{
			// Column: Number of properties

			tvc = new TableViewerColumn(_profileViewer, SWT.LEAD);
			tc = tvc.getColumn();
			tc.setText(Messages.Slideout_TourFilter_Column_Properties);
			tc.setToolTipText(Messages.Slideout_TourTagFilter_Column_Properties_Tooltip);
			tvc.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(final ViewerCell cell) {

					final TourTagFilterProfile profile = (TourTagFilterProfile) cell.getElement();

					cell.setText(Integer.toString(profile.tagFilterIds.size()));
				}
			});
			tableLayout.setColumnData(tc, new ColumnPixelData(_pc.convertWidthInCharsToPixels(6), false));
		}

		/*
		 * create table viewer
		 */
		_profileViewer.setContentProvider(new ProfileProvider());
		_profileViewer.setComparator(new ProfileComparator());

		_profileViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				onProfile_Select(true);
			}
		});

		_profileViewer.addDoubleClickListener(new IDoubleClickListener() {

			@Override
			public void doubleClick(final DoubleClickEvent event) {

				// set focus to  profile name
				_txtProfileName.setFocus();
				_txtProfileName.selectAll();
			}
		});

		_profileViewer.getTable().addKeyListener(new KeyListener() {

			@Override
			public void keyPressed(final KeyEvent e) {

				if (e.keyCode == SWT.DEL) {
					onProfile_Delete();
				}
			}

			@Override
			public void keyReleased(final KeyEvent e) {}
		});
	}

	private Composite createUI_300_Tags(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()//
				.grab(true, true)
				.applyTo(container);
		GridLayoutFactory
				.fillDefaults()//
				.numColumns(1)
				.extendedMargins(3, 0, 0, 0)
				.applyTo(container);
		{
			createUI_310_ProfileName(container);
			createUI_320_TagContainer(container);
		}

//		/**
//		 * Very Important !
//		 * <p>
//		 * Do a layout NOW, otherwise the initial profile container is using the whole width of the
//		 * slideout :-(
//		 */
//		container.layout(true, true);

		return container;
	}

	private void createUI_310_ProfileName(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()//
				.grab(true, false)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			{
				// Label: Profile name
				_lblProfileName = new Label(container, SWT.NONE);
				_lblProfileName.setText(Messages.Slideout_TourFilter_Label_ProfileName);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(_lblProfileName);
			}
			{
				// Text: Profile name
				_txtProfileName = new Text(container, SWT.BORDER);
				_txtProfileName.addModifyListener(_defaultModifyListener);
				GridDataFactory
						.fillDefaults()//
						.grab(true, false)
						.hint(_pc.convertWidthInCharsToPixels(30), SWT.DEFAULT)
						.applyTo(_txtProfileName);
			}
		}
	}

	private void createUI_320_TagContainer(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()
				.grab(true, true)
				.indent(0, 10)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{
			// left part
			final Composite containerTagList = createUI_330_TagCloud(container);

			// sash
			final Sash sash = new Sash(container, SWT.VERTICAL);

			// right part
			final Composite containerTagViewer = createUI_340_AllTags(container);

			new SashLeftFixedForm(//
					container,
					containerTagList,
					sash,
					containerTagViewer,
					_state,
					STATE_SASH_WIDTH_TAG_CONTAINER,
					40);
		}
	}

	private Composite createUI_330_TagCloud(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory
				.fillDefaults()
				.numColumns(1)
				.spacing(0, 2)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			createUI_332_TagCloud_Header(container);
			createUI_334_TagCloud_Viewer(container);
		}

		return container;
	}

	private void createUI_332_TagCloud_Header(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			{
				// Label: Selected Tags

				_lblSelectTags = new Label(container, SWT.NONE);
				_lblSelectTags.setText(Messages.Slideout_TourTagFilter_Label_SelectedTags);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.grab(true, false)
						.applyTo(_lblSelectTags);
			}
			{
				// toolbar
				_toolBarTagCloud = new ToolBar(container, SWT.FLAT);
			}
		}
	}

	private void createUI_334_TagCloud_Viewer(final Composite parent) {

		final Composite layoutContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(layoutContainer);

		final TableColumnLayout tableLayout = new TableColumnLayout();
		layoutContainer.setLayout(tableLayout);

		/*
		 * Create table
		 */
		final Table table = new Table(layoutContainer, SWT.FULL_SELECTION | SWT.CHECK);

		table.setLayout(new TableLayout());

		table.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(final SelectionEvent event) {

				/*
				 * The tag cloud viewer selection event can have another selection !!!
				 */

				_tagCloudViewerItem_IsChecked = event.detail == SWT.CHECK;
				_tagCloudViewerItem_Data = event.item.getData();
			}
		});

		table.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(final KeyEvent e) {
				_tagCloudViewerItem_IsKeyPressed = true;
			}
		});

		layoutContainer.addTraverseListener(new TraverseListener() {

			@Override
			public void keyTraversed(final TraverseEvent event) {
				onTraverse_TagCloudContainer(table, event);
			}
		});

		_tagCloudViewer = new CheckboxTableViewer(table);

		/*
		 * Create columns
		 */
		TableViewerColumn tvc;
		TableColumn tc;

		{
			// Column: Tag name

			tvc = new TableViewerColumn(_tagCloudViewer, SWT.LEAD);
			tc = tvc.getColumn();
			tc.setText(Messages.Slideout_TourFilter_Column_ProfileName);
			tvc.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(final ViewerCell cell) {

					final TagCloud tagCloud = (TagCloud) cell.getElement();

					cell.setText(tagCloud.tagName);
				}
			});
			tableLayout.setColumnData(tc, new ColumnWeightData(1, false));
		}

		/*
		 * create table viewer
		 */
		_tagCloudViewer.setContentProvider(new TagCloudProvider());
		_tagCloudViewer.setComparator(new TagCloudComparator());

		_tagCloudViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				onTagCloud_Select(event);
			}
		});
	}

	private Composite createUI_340_AllTags(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory
				.fillDefaults()
				.spacing(0, 2)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			createUI_342_AllTags_Header(container);
			createUI_344_AllTags_Viewer(container);
		}

		return container;
	}

	private void createUI_342_AllTags_Header(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		containerTag.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			{
				// Label: All Tags
				_lblAllTags = new Label(container, SWT.NONE);
				_lblAllTags.setText(Messages.Slideout_TourTagFilter_Label_AllTags);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.grab(true, false)
						.applyTo(_lblAllTags);
			}
			{
				// toolbar
				_toolBarAllTags = new ToolBar(container, SWT.FLAT);
			}
		}
	}

	private void createUI_344_AllTags_Viewer(final Composite parent) {

		/*
		 * create tree layout
		 */

		final Composite layoutContainer = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()//
				.grab(true, true)
				.hint(200, 100)
				.applyTo(layoutContainer);

		final TreeColumnLayout treeLayout = new TreeColumnLayout();
		layoutContainer.setLayout(treeLayout);

		/*
		 * create viewer tree
		 */
		final Tree tree = new Tree(
				layoutContainer,
				SWT.H_SCROLL | SWT.V_SCROLL
						| SWT.MULTI
						| SWT.CHECK
						| SWT.FULL_SELECTION);

		tree.setHeaderVisible(false);

		tree.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(final SelectionEvent event) {

				/*
				 * The tag treeviewer selection event can have another selection !!!
				 */

				_tagViewerItem_IsChecked = event.detail == SWT.CHECK;

				if (_tagViewerItem_IsChecked) {

					/*
					 * Item can be null when <ctrl>+A is pressed !!!
					 */
					final Widget item = event.item;

					_tagViewerItem_Data = item.getData();
				}
			}
		});

		tree.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(final KeyEvent e) {
				_tagViewerItem_IsKeyPressed = true;
			}
		});

		layoutContainer.addTraverseListener(new TraverseListener() {

			@Override
			public void keyTraversed(final TraverseEvent event) {
				onTraverse_TagContainer(tree, event);
			}
		});

		/*
		 * Create viewer
		 */
		_tagViewer = new ContainerCheckedTreeViewer(tree);

		_tagViewer.setUseHashlookup(true);
		_tagViewer.setContentProvider(new TagViewerContentProvicer());
		_tagViewer.setComparator(new TagViewerComparator());

		_tagViewer.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(final CheckStateChangedEvent event) {
				update_FromTagViewer();
			}
		});

		_tagViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				onTag_Select(event);
			}
		});

		/*
		 * create columns
		 */
		TreeViewerColumn tvc;
		TreeColumn tvcColumn;

		// column: tags + tag categories
		tvc = new TreeViewerColumn(_tagViewer, SWT.TRAIL);
		tvcColumn = tvc.getColumn();
		tvc.setLabelProvider(new StyledCellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final StyledString styledString = new StyledString();

				final Object element = cell.getElement();

				if (element instanceof TVIPrefTag) {

					final TourTag tourTag = ((TVIPrefTag) element).getTourTag();

					styledString.append(tourTag.getTagName(), net.tourbook.ui.UI.TAG_STYLER);
					cell.setImage(tourTag.isRoot() ? _imgTagRoot : _imgTag);

				} else if (element instanceof TVIPrefTagCategory) {

					final TVIPrefTagCategory tourTagCategoryItem = (TVIPrefTagCategory) element;
					final TourTagCategory tourTagCategory = tourTagCategoryItem.getTourTagCategory();

					cell.setImage(_imgTagCategory);

					styledString.append(tourTagCategory.getCategoryName(), net.tourbook.ui.UI.TAG_CATEGORY_STYLER);

					// get number of categories
					final int categoryCounter = tourTagCategory.getCategoryCounter();
					final int tagCounter = tourTagCategory.getTagCounter();
					if (categoryCounter == -1 && tagCounter == -1) {

//						styledString.append("  ...", StyledString.COUNTER_STYLER);

					} else {

						String categoryString = UI.EMPTY_STRING;
						if (categoryCounter > 0) {
							categoryString = "/" + categoryCounter; //$NON-NLS-1$
						}
						styledString.append("   " + tagCounter + categoryString, StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
					}

				} else {
					styledString.append(element.toString());
				}

				cell.setText(styledString.getString());
				cell.setStyleRanges(styledString.getStyleRanges());
			}
		});
		treeLayout.setColumnData(tvcColumn, new ColumnWeightData(100, true));
	}

	private void createUI_800_Actions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);

//		container.addControlListener(new ControlAdapter() {
//
//			@Override
//			public void controlResized(final ControlEvent e) {
//
//				final Rectangle containerSize = container.getClientArea();
//
//				System.out.println(
//						(UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") //$NON-NLS-1$ //$NON-NLS-2$
//								+ ("\tcontainerSize: " + containerSize) //$NON-NLS-1$
////						+ ("\t: " + )
//				);
//// TODO remove SYSTEM.OUT.PRINTLN
//
//			}
//		});

		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			createUI_810_ProfileActions(container);
			createUI_820_FilterActions(container);
		}
	}

	private void createUI_810_ProfileActions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
		{
			{
				/*
				 * Button: New
				 */
				_btnNewProfile = new Button(container, SWT.PUSH);
				_btnNewProfile.setText(Messages.Slideout_TourFilter_Action_AddProfile);
				_btnNewProfile.setToolTipText(Messages.Slideout_TourTagFilter_Action_AddProfile_Tooltip);
				_btnNewProfile.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onProfile_Add();
					}
				});

				// set button default width
				UI.setButtonLayoutData(_btnNewProfile);
			}
			{
				/*
				 * Button: Copy
				 */
				_btnCopyProfile = new Button(container, SWT.PUSH);
				_btnCopyProfile.setText(Messages.Slideout_TourFilter_Action_CopyProfile);
				_btnCopyProfile.setToolTipText(Messages.Slideout_TourFilter_Action_CopyProfile_Tooltip);
				_btnCopyProfile.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onProfile_Copy();
					}
				});

				// set button default width
				UI.setButtonLayoutData(_btnCopyProfile);
			}
			{
				/*
				 * Button: Delete
				 */
				_btnDeleteProfile = new Button(container, SWT.PUSH);
				_btnDeleteProfile.setText(Messages.Slideout_TourFilter_Action_DeleteProfile);
				_btnDeleteProfile.setToolTipText(Messages.Slideout_TourFilter_Action_DeleteProfile_Tooltip);
				_btnDeleteProfile.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onProfile_Delete();
					}
				});

				// set button default width
				UI.setButtonLayoutData(_btnDeleteProfile);
			}
		}
	}

	private void createUI_820_FilterActions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			{
				/*
				 * Checkbox: live update
				 */
				_chkLiveUpdate = new Button(container, SWT.CHECK);
				_chkLiveUpdate.setText(Messages.Slideout_TourFilter_Checkbox_IsLiveUpdate);
				_chkLiveUpdate.setToolTipText(Messages.Slideout_TourTagFilter_Checkbox_IsLiveUpdate_Tooltip);
				_chkLiveUpdate.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						doLiveUpdate();
					}
				});

				GridDataFactory
						.fillDefaults()//
						.grab(true, false)
						.align(SWT.END, SWT.CENTER)
						.applyTo(_chkLiveUpdate);
			}
			{
				/*
				 * Button: Apply
				 */
				_btnApply = new Button(container, SWT.PUSH);
				_btnApply.setText(Messages.Slideout_TourFilter_Action_Apply);
				_btnApply.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						TourTagFilterManager.fireFilterModifyEvent();
					}
				});

				// set button default width
				UI.setButtonLayoutData(_btnApply);
			}
		}
	}

	private void doLiveUpdate() {

		_isLiveUpdate = _chkLiveUpdate.getSelection();

		enableControls();

		fireModifyEvent();
	}

	private void enableControls() {

		final int numCheckedTagCloudItems = _tagCloudViewer.getCheckedElements().length;
		final int numTagCloudItems = _tagCloudItems.size();

		final boolean isProfileSelected = _selectedProfile != null;
		final boolean canCheckTags = numTagCloudItems > 0 && numCheckedTagCloudItems < numTagCloudItems;
		final boolean canUncheckTags = numTagCloudItems > 0 && numCheckedTagCloudItems > 0;

		_btnApply.setEnabled(isProfileSelected && _isLiveUpdate == false);
		_btnCopyProfile.setEnabled(isProfileSelected);
		_btnDeleteProfile.setEnabled(isProfileSelected);

		_actionCollapseAll.setEnabled(isProfileSelected);
		_actionExpandAll.setEnabled(isProfileSelected);
		_actionTag_LayoutFlat.setEnabled(isProfileSelected);
		_actionTag_LayoutHierarchical.setEnabled(isProfileSelected);
		_actionTagCloud_CheckAll.setEnabled(isProfileSelected && canCheckTags);
		_actionTagCloud_UncheckAll.setEnabled(isProfileSelected && canUncheckTags);

		_chkLiveUpdate.setEnabled(isProfileSelected);

		_lblAllTags.setEnabled(isProfileSelected);
		_lblProfileName.setEnabled(isProfileSelected);
		_lblSelectTags.setEnabled(isProfileSelected);

		_tagCloudViewer.getTable().setEnabled(isProfileSelected);
		_tagViewer.getTree().setEnabled(isProfileSelected);

		_txtProfileName.setEnabled(isProfileSelected);
	}

	private void expandCollapseFolder(final TVIPrefTagCategory treeItem) {

		if (_tagViewer.getExpandedState(treeItem)) {

			// collapse folder

			_tagViewer.collapseToLevel(treeItem, 1);
		}
	}

	/**
	 * set the toolbar action after the {@link #_tagViewer} is created
	 */
	private void fillToolbar() {

		/*
		 * Toolbar: Tag cloud
		 */
		final ToolBarManager tbmTagCloud = new ToolBarManager(_toolBarTagCloud);

		tbmTagCloud.add(_actionTagCloud_CheckAll);
		tbmTagCloud.add(_actionTagCloud_UncheckAll);

		tbmTagCloud.update(true);

		/*
		 * Toolbar: All tags
		 */
		final ToolBarManager tbmAllTags = new ToolBarManager(_toolBarAllTags);

		tbmAllTags.add(_actionTag_LayoutFlat);
		tbmAllTags.add(_actionTag_LayoutHierarchical);
		tbmAllTags.add(_actionExpandAll);
		tbmAllTags.add(_actionCollapseAll);
		tbmAllTags.add(_actionOpenPrefTags);

		tbmAllTags.update(true);
	}

	/**
	 * Fire modify event only when live update is selected
	 */
	private void fireModifyEvent() {

		if (_isLiveUpdate) {
			TourTagFilterManager.fireFilterModifyEvent();
		}
	}

	@Override
	protected Rectangle getParentBounds() {

		final Rectangle itemBounds = _tourTagFilterItem.getBounds();
		final Point itemDisplayPosition = _tourTagFilterItem.getParent().toDisplay(itemBounds.x, itemBounds.y);

		itemBounds.x = itemDisplayPosition.x;
		itemBounds.y = itemDisplayPosition.y;

		return itemBounds;
	}

	private long[] getTagIds_FromTagCloud() {

		final TLongHashSet tagIds = new TLongHashSet();

		final Object[] checkedElements = _tagCloudViewer.getCheckedElements();

		for (final Object object : checkedElements) {

			if (object instanceof TagCloud) {
				tagIds.add(((TagCloud) object).tagId);
			}
		}

		return tagIds.toArray();
	}

	private long[] getTagIds_FromTagViewer() {

		final TLongHashSet tagIds = new TLongHashSet();

		final Object[] checkedElements = _tagViewer.getCheckedElements();

		for (final Object object : checkedElements) {

			if (object instanceof TVIPrefTag) {

				final TVIPrefTag tagItem = (TVIPrefTag) object;
				final long tagId = tagItem.getTourTag().getTagId();

				tagIds.add(tagId);
			}
		}

		return tagIds.toArray();
	}

	/**
	 * Traverses all tag viewer items until a tag items is found Recursive !
	 * 
	 * @param parentItems
	 * @param tagItems
	 * @param tagId
	 * @return Returns <code>true</code> when the tag id is found
	 */
	private boolean getTagItems(final ArrayList<TreeViewerItem> parentItems,
								final ArrayList<TVIPrefTag> tagItems,
								final long tagId) {

		for (final TreeViewerItem tvItem : parentItems) {

			if (tvItem instanceof TVIPrefTagCategory) {

				final TVIPrefTagCategory tagCategory = (TVIPrefTagCategory) tvItem;
				final ArrayList<TreeViewerItem> tagCategoryChildren = tagCategory.getFetchedChildren();

				if (tagCategoryChildren.size() > 0) {

					final boolean isTagFound = getTagItems(tagCategoryChildren, tagItems, tagId);

					if (isTagFound) {
						return true;
					}
				}

			} else if (tvItem instanceof TVIPrefTag) {

				final TVIPrefTag tagItem = (TVIPrefTag) tvItem;

				if (tagId == tagItem.getTourTag().getTagId()) {

					tagItems.add(tagItem);

					return true;
				}
			}
		}

		return false;
	}

	@Override
	public TreeViewer getTreeViewer() {
		return _tagViewer;
	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);

		_imgTag = TourbookPlugin.getImageDescriptor(Messages.Image__tag).createImage();
		_imgTagRoot = TourbookPlugin.getImageDescriptor(Messages.Image__tag_root).createImage();
		_imgTagCategory = TourbookPlugin.getImageDescriptor(Messages.Image__tag_category).createImage();

		parent.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				onDisposeSlideout();
			}
		});
	}

	/**
	 * Load all tag items that the categories do show the number of items
	 */
	private void loadAllTagItems() {

		final HashMap<Long, TourTag> allTourTags = TourDatabase.getAllTourTags();

		final Set<Long> tagIds = allTourTags.keySet();

		final ArrayList<TVIPrefTag> tagItems = new ArrayList<>(tagIds.size());

		if (tagIds.size() > 0) {

			// get all tag viewer items which should be checked

			final ArrayList<TreeViewerItem> rootItems = _tagViewerRootItem.getFetchedChildren();

			for (final long tagId : tagIds) {

				// Is recursive !!!
				getTagItems(rootItems, tagItems, tagId);
			}
		}
	}

	private void onDisposeSlideout() {

		_imgTag.dispose();
		_imgTagRoot.dispose();
		_imgTagCategory.dispose();

		saveState();
	}

	@Override
	protected void onFocus() {

		if (_selectedProfile != null
				&& _selectedProfile.name != null
				&& _selectedProfile.name.startsWith(Messages.Tour_Filter_Default_ProfileName)) {

			// default profile is selected, make it easy to rename it

			_txtProfileName.selectAll();
			_txtProfileName.setFocus();

		} else if (_selectedProfile == null) {

			_btnNewProfile.setFocus();
		}
	}

	private void onProfile_Add() {

		final TourTagFilterProfile filterProfile = new TourTagFilterProfile();

		// update model
		_profiles.add(filterProfile);

		// update viewer
		_profileViewer.refresh();

		// select new profile
		selectProfile(filterProfile);

		_txtProfileName.setFocus();
	}

	private void onProfile_Copy() {

		if (_selectedProfile == null) {
			// ignore
			return;
		}

		final TourTagFilterProfile filterProfile = _selectedProfile.clone();

		// update model
		_profiles.add(filterProfile);

		// update viewer
		_profileViewer.refresh();

		// select new profile
		selectProfile(filterProfile);

		_txtProfileName.setFocus();
	}

	private void onProfile_Delete() {

		if (_selectedProfile == null) {
			// ignore
			return;
		}

		/*
		 * Confirm deletion
		 */
		boolean isDeleteProfile;
		setIsKeepOpenInternally(true);
		{
			isDeleteProfile = MessageDialog.openConfirm(
					Display.getCurrent().getActiveShell(),
					Messages.Slideout_TourFilter_Confirm_DeleteProfile_Title,
					NLS.bind(Messages.Slideout_TourFilter_Confirm_DeleteProfile_Message, _selectedProfile.name));
		}
		setIsKeepOpenInternally(false);

		if (isDeleteProfile == false) {
			return;
		}

		// keep currently selected position
		final int lastIndex = _profileViewer.getTable().getSelectionIndex();

		// update model
		_profiles.remove(_selectedProfile);
		TourTagFilterManager.setSelectedProfile(null);

		// update UI
		_profileViewer.remove(_selectedProfile);

		/*
		 * Select another filter at the same position
		 */
		final int numFilters = _profiles.size();
		final int nextFilterIndex = Math.min(numFilters - 1, lastIndex);

		final Object nextSelectedProfile = _profileViewer.getElementAt(nextFilterIndex);
		if (nextSelectedProfile == null) {

			// all profiles are deleted

			_selectedProfile = null;

			updateTags_TagViewer(NO_TAGS);
			updateTags_TagCloud(NO_TAGS);

			enableControls();

			fireModifyEvent();

		} else {

			selectProfile((TourTagFilterProfile) nextSelectedProfile);
		}

		// set focus back to the viewer
		_profileViewer.getTable().setFocus();
	}

	private void onProfile_Modify() {

		if (_selectedProfile == null) {
			return;
		}

		final String profileName = _txtProfileName.getText();

		_selectedProfile.name = profileName;

		_profileViewer.refresh();
	}

	/**
	 * @param isCheckOldProfile
	 *            When <code>true</code> then the old profile is checked if it is already selected
	 */
	private void onProfile_Select(final boolean isCheckOldProfile) {

		TourTagFilterProfile selectedProfile = null;

		// get selected profile from viewer
		final StructuredSelection selection = (StructuredSelection) _profileViewer.getSelection();
		final Object firstElement = selection.getFirstElement();
		if (firstElement != null) {
			selectedProfile = (TourTagFilterProfile) firstElement;
		}

		if (isCheckOldProfile && _selectedProfile != null && _selectedProfile == selectedProfile) {
			// a new profile is not selected
			return;
		}

		_selectedProfile = selectedProfile;

		// update model
		TourTagFilterManager.setSelectedProfile(_selectedProfile);

		// update UI
		if (_selectedProfile == null) {

			// no profile

			_txtProfileName.setText(UI.EMPTY_STRING);

		} else {

			// a profile is selected

			_txtProfileName.setText(_selectedProfile.name);

			if (_selectedProfile.name.startsWith(Messages.Tour_Filter_Default_ProfileName)) {

				// a default profile is selected, make is easy to rename it

				_txtProfileName.selectAll();
				_txtProfileName.setFocus();
			}

			update_FromProfile();
		}

		fireModifyEvent();
	}

	/**
	 * @param isHierarchicalLayout
	 *            Is <code>true</code> when the layout is flat, otherwise it is hierarchical
	 */
	private void onTag_Layout(final boolean isHierarchicalLayout) {

		_isHierarchicalLayout = isHierarchicalLayout;

		updateTagModel();

		// reselect profile
		onProfile_Select(false);
	}

	private void onTag_Select(final SelectionChangedEvent event) {

		if (_tagViewerItem_IsKeyPressed) {

			// ignore when selected with keyboard

			// reset state
			_tagViewerItem_IsKeyPressed = false;

			return;
		}

		Object selection;

		if (_tagViewerItem_IsChecked) {

			// a checkbox is checked

			selection = _tagViewerItem_Data;

		} else {

			selection = ((IStructuredSelection) event.getSelection()).getFirstElement();
		}

		if (selection instanceof TVIPrefTag) {

			// tag is selected

			final TVIPrefTag tviTag = (TVIPrefTag) selection;

			// toggle tag
			if (_tagViewerItem_IsChecked == false) {

				// tag is selected and NOT the checkbox !!!

				final boolean isChecked = _tagViewer.getChecked(tviTag);

				_tagViewer.setChecked(tviTag, !isChecked);
			}

			update_FromTagViewer();

		} else if (selection instanceof TVIPrefTagCategory) {

			// expand/collapse current item

			if (_tagViewerItem_IsChecked == false) {

				// category is selected and NOT the checkbox !!!

				final TreeSelection treeSelection = (TreeSelection) event.getSelection();

				onTag_SelectCategory(treeSelection);
			}
		}
	}

	private void onTag_SelectCategory(final TreeSelection treeSelection) {

		if (_isExpandingSelection) {
			// prevent entless loops
			return;
		}

		final TreePath[] selectedTreePaths = treeSelection.getPaths();
		if (selectedTreePaths.length == 0) {
			return;
		}
		final TreePath selectedTreePath = selectedTreePaths[0];
		if (selectedTreePath == null) {
			return;
		}

		final TVIPrefTagCategory tviFolder = (TVIPrefTagCategory) selectedTreePath.getLastSegment();

		onTag_SelectCategory_10_AutoExpandCollapse(treeSelection, selectedTreePath, tviFolder);
	}

	/**
	 * This is not yet working thoroughly because the expanded position moves up or down and all
	 * expanded childrens are not visible (but they could) like when the triangle (+/-) icon in the
	 * tree is clicked.
	 * 
	 * @param treeSelection
	 * @param selectedTreePath
	 * @param tviFolder
	 */
	private void onTag_SelectCategory_10_AutoExpandCollapse(final ITreeSelection treeSelection,
															final TreePath selectedTreePath,
															final TVIPrefTagCategory tviFolder) {

		if (_isInCollapseAll) {

			// prevent auto expand
			return;
		}

		if (_isBehaviourSingleExpandedOthersCollapse) {

			/*
			 * run async because this is doing a reselection which cannot be done within the current
			 * selection event
			 */
			Display.getCurrent().asyncExec(new Runnable() {

				private long				__expandRunnableCounter	= ++_expandRunnableCounter;

				private TVIPrefTagCategory	__selectedFolderItem	= tviFolder;
				private ITreeSelection		__treeSelection			= treeSelection;
				private TreePath			__selectedTreePath		= selectedTreePath;

				@Override
				public void run() {

					// check if a newer expand event occured
					if (__expandRunnableCounter != _expandRunnableCounter) {
						return;
					}

					onTag_SelectCategory_20_AutoExpandCollapse_Runnable(
							__selectedFolderItem,
							__treeSelection,
							__selectedTreePath);
				}
			});

		} else {

			if (_isBehaviourAutoExpandCollapse) {

				// expand folder with one mouse click but not with the keyboard
				expandCollapseFolder(tviFolder);
			}
		}
	}

	/**
	 * This behavior is complex and still have possible problems.
	 * 
	 * @param selectedFolderItem
	 * @param treeSelection
	 * @param selectedTreePath
	 */
	private void onTag_SelectCategory_20_AutoExpandCollapse_Runnable(	final TVIPrefTagCategory selectedFolderItem,
																		final ITreeSelection treeSelection,
																		final TreePath selectedTreePath) {
		_isExpandingSelection = true;
		{
			final Tree tree = _tagViewer.getTree();

			tree.setRedraw(false);
			{
				final TreeItem topItem = tree.getTopItem();

				final boolean isExpanded = _tagViewer.getExpandedState(selectedTreePath);

				/*
				 * collapse all tree paths
				 */
				final TreePath[] allExpandedTreePaths = _tagViewer.getExpandedTreePaths();
				for (final TreePath treePath : allExpandedTreePaths) {
					_tagViewer.setExpandedState(treePath, false);
				}

				/*
				 * expand and select selected folder
				 */
				_tagViewer.setExpandedTreePaths(new TreePath[] { selectedTreePath });
				_tagViewer.setSelection(treeSelection, true);

				if (_isBehaviourAutoExpandCollapse && isExpanded) {

					// auto collapse expanded folder
					_tagViewer.setExpandedState(selectedTreePath, false);
				}

				/**
				 * set top item to the previous top item, otherwise the expanded/collapse item is
				 * positioned at the bottom and the UI is jumping all the time
				 * <p>
				 * win behaviour: when an item is set to top which was collapsed bevore, it will be
				 * expanded
				 */
				if (topItem.isDisposed() == false) {
					tree.setTopItem(topItem);
				}
			}
			tree.setRedraw(true);
		}
		_isExpandingSelection = false;
	}

	private void onTagCloud_Checkbox_CheckAll() {

		_tagCloudViewer.setCheckedElements(_tagCloudItems.toArray());

		update_FromTagCloud();
	}

	private void onTagCloud_Checkbox_UncheckAll() {

		_tagCloudViewer.setCheckedElements(EMPTY_LIST);

		update_FromTagCloud();
	}

	private void onTagCloud_Select(final SelectionChangedEvent event) {

		if (_tagCloudViewerItem_IsKeyPressed && _tagCloudViewerItem_IsChecked == false) {

			// ignore when only selected with keyboard and not checked

			// reset state
			_tagCloudViewerItem_IsKeyPressed = false;

			return;
		}

		Object selection;

		if (_tagCloudViewerItem_IsChecked) {

			// a checkbox is checked

			selection = _tagCloudViewerItem_Data;

		} else {

			selection = ((IStructuredSelection) event.getSelection()).getFirstElement();
		}

		if (selection instanceof TagCloud) {

			// tag is selected

			final TagCloud tagCloud = (TagCloud) selection;

			// toggle tag
			if (_tagCloudViewerItem_IsChecked == false) {

				// tag is selected and NOT the checkbox !!!

				final boolean isChecked = _tagCloudViewer.getChecked(tagCloud);

				_tagCloudViewer.setChecked(tagCloud, !isChecked);
			}

			update_FromTagCloud();
		}
	}

	/**
	 * Terrible solution to traverse to a table
	 * 
	 * @param table
	 * @param event
	 */
	private void onTraverse_TagCloudContainer(final Table table, final TraverseEvent event) {

		if (event.detail == SWT.TRAVERSE_TAB_NEXT) {

			table.setFocus();

			final TableItem[] selection = table.getSelection();
			if (selection == null || selection.length == 0) {

				if (table.getItemCount() > 0) {
					table.setSelection(table.getItem(0));
				}

			} else {

				table.setSelection(selection);
			}
		}
	}

	/**
	 * Terrible solution to traverse to a tree
	 * 
	 * @param tree
	 * @param event
	 */
	private void onTraverse_TagContainer(final Tree tree, final TraverseEvent event) {

		if (event.detail == SWT.TRAVERSE_TAB_NEXT) {

			tree.setFocus();

			final TreeItem[] selection = tree.getSelection();
			if (selection == null || selection.length == 0) {

				if (tree.getItemCount() > 0) {
					tree.setSelection(tree.getItem(0));
				}

			} else {

				tree.setSelection(selection);
			}
		}
	}

	private void restoreState() {

		// live update
		_isLiveUpdate = Util.getStateBoolean(_state, STATE_IS_LIVE_UPDATE, false);
		_chkLiveUpdate.setSelection(_isLiveUpdate);

		/*
		 * Get previous selected profile
		 */
		TourTagFilterProfile selectedProfile = TourTagFilterManager.getSelectedProfile();
		if (selectedProfile == null) {

			// select first profile

			selectedProfile = (TourTagFilterProfile) _profileViewer.getElementAt(0);
		}

		if (selectedProfile != null) {
			selectProfile(selectedProfile);
		}

		/*
		 * Set layout actions after the UI is created
		 */
		if (_isHierarchicalLayout) {
			_actionTag_LayoutHierarchical.setChecked(true);
		} else {
			_actionTag_LayoutFlat.setChecked(true);
		}
	}

	private void restoreStateBeforeUI() {

		// layout
		_isHierarchicalLayout = Util.getStateBoolean(_state, STATE_IS_HIERARCHICAL_LAYOUT, true);
	}

	@Override
	protected void saveState() {

		_state.put(STATE_IS_HIERARCHICAL_LAYOUT, _isHierarchicalLayout);
		_state.put(STATE_IS_LIVE_UPDATE, _isLiveUpdate);

		super.saveState();
	}

	private void selectProfile(final TourTagFilterProfile selectedProfile) {

		_profileViewer.setSelection(new StructuredSelection(selectedProfile));

		final Table table = _profileViewer.getTable();
		table.setSelection(table.getSelectionIndices());
	}

	private void update_FromProfile() {

		final long[] tagIds = _selectedProfile.tagFilterIds.toArray();

		updateTags_TagCloud(tagIds);
		updateTags_TagViewer(tagIds);

		enableControls();
	}

	private void update_FromTagCloud() {

		if (_selectedProfile == null) {
			return;
		}

		final long[] tagIds = getTagIds_FromTagCloud();

		updateTags_TagProfile(_selectedProfile, tagIds);
		updateTags_TagViewer(tagIds);

		enableControls();

		fireModifyEvent();
	}

	private void update_FromTagViewer() {

		if (_selectedProfile == null) {
			return;
		}

		final long[] tagIds = getTagIds_FromTagViewer();

		updateTags_TagProfile(_selectedProfile, tagIds);
		updateTags_TagCloud(tagIds);

		enableControls();

		fireModifyEvent();
	}

	private void updateTagModel() {

		_tagViewerRootItem = new TVIPrefTagRoot(_tagViewer, _isHierarchicalLayout);
		_tagViewer.setInput(this);

		loadAllTagItems();
	}

	private void updateTags_TagCloud(final long[] tagIds) {

		/*
		 * Update model
		 */
		_tagCloudItems.clear();

		final HashMap<Long, TourTag> allTourTags = TourDatabase.getAllTourTags();

		for (final long tagId : tagIds) {

			final TourTag tourTag = allTourTags.get(tagId);

			_tagCloudItems.add(new TagCloud(tagId, tourTag.getTagName()));
		}

		/*
		 * Update UI
		 */
		// reload viewer
		_tagCloudViewer.setInput(EMPTY_LIST);

		// check all
		_tagCloudViewer.setCheckedElements(_tagCloudItems.toArray());
	}

	private void updateTags_TagProfile(final TourTagFilterProfile profile, final long[] tagIds) {

		// update model
		final TLongHashSet profileTagFilterIds = profile.tagFilterIds;
		profileTagFilterIds.clear();
		profileTagFilterIds.addAll(tagIds);

		// update UI
		_profileViewer.update(profile, null);
	}

	private void updateTags_TagViewer(final long[] tagIds) {

		/*
		 * Update UI
		 */
		final ArrayList<TVIPrefTag> tagItems = new ArrayList<>(tagIds.length);

		if (tagIds.length > 0) {

			// get all tag viewer items which should be checked

			final ArrayList<TreeViewerItem> rootItems = _tagViewerRootItem.getFetchedChildren();

			for (final long tagId : tagIds) {

				// Is recursive !!!
				getTagItems(rootItems, tagItems, tagId);
			}
		}

		_tagViewer.setCheckedElements(tagItems.toArray());
	}

}
