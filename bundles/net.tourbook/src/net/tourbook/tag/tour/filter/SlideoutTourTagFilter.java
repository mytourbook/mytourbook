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

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.form.SashLeftFixedForm;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourTagCategory;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tag.TVIPrefTag;
import net.tourbook.tag.TVIPrefTagCategory;
import net.tourbook.tag.TVIPrefTagRoot;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;

/**
 * Slideout for the tour tag filter
 */
public class SlideoutTourTagFilter extends AdvancedSlideout {

	private static final String						STATE_IS_LIVE_UPDATE	= "STATE_IS_LIVE_UPDATE";				//$NON-NLS-1$
	private static final String						STATE_SASH_WIDTH		= "STATE_SASH_WIDTH";					//$NON-NLS-1$

	private IDialogSettings							_state;
	private final IPreferenceStore					_prefStore				= TourbookPlugin.getPrefStore();

	private TableViewer								_profileViewer;
	private ContainerCheckedTreeViewer				_tagViewer;

	private TVIPrefTagRoot							_rootItem;

	private final ArrayList<TourTagFilterProfile>	_filterProfiles			= TourTagFilterManager.getProfiles();
	private TourTagFilterProfile					_selectedProfile;

	private PixelConverter							_pc;

	private ToolItem								_tourTagFilterItem;

	private boolean									_isLiveUpdate;

	private ModifyListener							_defaultModifyListener;
	{
		_defaultModifyListener = new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent e) {
				onProfile_Modify();
			}
		};
	}

	/*
	 * UI controls
	 */
	private Composite			_containerFilter;
	private Composite			_containerProfiles;

	private Button				_btnApply;
	private Button				_btnCopyProfile;
	private Button				_btnDeleteProfile;
	private Button				_chkLiveUpdate;

	private Label				_lblProfileName;

	private Text				_txtProfileName;

	private SashLeftFixedForm	_sashForm;

	private Image				_imgTag;
	private Image				_imgTagRoot;
	private Image				_imgTagCategory;

	private class FilterProfileComparator extends ViewerComparator {

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

	private class FilterProfileProvider implements IStructuredContentProvider {

		@Override
		public void dispose() {}

		@Override
		public Object[] getElements(final Object inputElement) {
			return _filterProfiles.toArray();
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
			return _rootItem.getFetchedChildrenAsArray();
		}

		@Override
		public Object getParent(final Object element) {
			return ((TreeViewerItem) element).getParentItem();
		}

//		public TreeViewerItem getRootItem() {
//			return _rootItem;
//		}

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

		super(toolItem.getParent(), state, new int[] { 400, 300, 400, 400 });

		_tourTagFilterItem = toolItem;
		_state = state;

		setShellFadeOutDelaySteps(30);
		setTitleText(Messages.Slideout_TourTagFilter_Label_Title);
	}

	@Override
	protected void createSlideoutContent(final Composite parent) {

		/*
		 * Reset to a valid state when the slideout is opened again
		 */
		_selectedProfile = null;

		initUI(parent);

		createUI(parent);

		// load profile viewer
		_profileViewer.setInput(new Object());

		// load tag viewer and show content
		_rootItem = new TVIPrefTagRoot(_tagViewer);
		_tagViewer.setInput(this);

		restoreState();
		enableControls();
	}

	private void createUI(final Composite parent) {

		final Composite shellContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(shellContainer);
		GridLayoutFactory.fillDefaults().applyTo(shellContainer);
		{
			final Composite container = new Composite(shellContainer, SWT.NONE);
			GridDataFactory
					.fillDefaults()//
					.grab(true, true)
					.applyTo(container);
			GridLayoutFactory.swtDefaults().applyTo(container);
			{
				// left part
				_containerProfiles = createUI_200_Profiles(container);

				// sash
				final Sash sash = new Sash(container, SWT.VERTICAL);
				{
					UI.addSashColorHandler(sash);

					// save sash width
					sash.addMouseListener(new MouseAdapter() {
						@Override
						public void mouseUp(final MouseEvent e) {
							_state.put(STATE_SASH_WIDTH, _containerProfiles.getSize().x);
						}
					});
				}

				// right part
				_containerFilter = createUI_300_Filter(container);

				_sashForm = new SashLeftFixedForm(//
						container,
						_containerProfiles,
						sash,
						_containerFilter,
						30);
			}
		}
	}

	private Composite createUI_200_Profiles(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()//
				.applyTo(container);
		GridLayoutFactory
				.fillDefaults()//
				.numColumns(1)
				.extendedMargins(0, 3, 0, 0)
				.applyTo(container);
		{
			{
				final Label label = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults().applyTo(label);
				label.setText(Messages.Slideout_TourFilter_Label_Profiles);
			}

			createUI_210_ProfileViewer(container);
			createUI_220_ProfileActions(container);
		}

		return container;
	}

	private void createUI_210_ProfileViewer(final Composite parent) {

		final Composite layoutContainer = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()//
				.grab(true, true)
				.applyTo(layoutContainer);

		final TableColumnLayout tableLayout = new TableColumnLayout();
		layoutContainer.setLayout(tableLayout);

		/*
		 * create table
		 */
		final Table table = new Table(layoutContainer, SWT.FULL_SELECTION);

		table.setLayout(new TableLayout());

		// !!! this prevents that the horizontal scrollbar is displayed, but is not always working :-(
		table.setHeaderVisible(false);
//		table.setHeaderVisible(true);

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

			tvc = new TableViewerColumn(_profileViewer, SWT.TRAIL);
			tc = tvc.getColumn();
			tc.setText(Messages.Slideout_TourFilter_Column_Properties);
			tc.setToolTipText(Messages.Slideout_TourFilter_Column_Properties_Tooltip);
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
		_profileViewer.setContentProvider(new FilterProfileProvider());
		_profileViewer.setComparator(new FilterProfileComparator());

		_profileViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				onProfile_Select();
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

	private void createUI_220_ProfileActions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
		{
			{
				/*
				 * Button: New
				 */
				final Button button = new Button(container, SWT.PUSH);
				button.setText(Messages.Slideout_TourFilter_Action_AddProfile);
				button.setToolTipText(Messages.Slideout_TourFilter_Action_AddProfile_Tooltip);
				button.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onProfile_Add();
					}
				});

				// set button default width
				UI.setButtonLayoutData(button);
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

	private Composite createUI_300_Filter(final Composite parent) {

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
			createUI_310_FilterName(container);
//			createUI_400_FilterOuterContainer(container);
			createUI_450_TagViewer(container);
			createUI_500_FilterActions(container);
		}

		return container;
	}

	private void createUI_310_FilterName(final Composite parent) {

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

	private void createUI_450_TagViewer(final Composite parent) {

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
		 * create viewer
		 */
		final Tree tree = new Tree(layoutContainer, SWT.H_SCROLL | SWT.V_SCROLL
//				| SWT.BORDER
				| SWT.MULTI
				| SWT.FULL_SELECTION);

		tree.setHeaderVisible(false);
		tree.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

		_tagViewer = new ContainerCheckedTreeViewer(tree);

		_tagViewer.setContentProvider(new TagViewerContentProvicer());
		_tagViewer.setComparator(new TagViewerComparator());

		_tagViewer.setUseHashlookup(true);

		_tagViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(final DoubleClickEvent event) {

				final Object selection = ((IStructuredSelection) event.getSelection()).getFirstElement();

				if (selection instanceof TVIPrefTag) {

					// tag is selected

//					onRenameTourTag();

				} else if (selection instanceof TVIPrefTagCategory) {

					// expand/collapse current item

					final TreeViewerItem tourItem = (TreeViewerItem) selection;

					if (_tagViewer.getExpandedState(tourItem)) {
						_tagViewer.collapseToLevel(tourItem, 1);
					} else {
						_tagViewer.expandToLevel(tourItem, 1);
					}
				}
			}
		});

		_tagViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {

				final Object selection = ((IStructuredSelection) event.getSelection()).getFirstElement();

				if (selection instanceof TVIPrefTag) {

					// tag is selected

//					onRenameTourTag();

				} else if (selection instanceof TVIPrefTagCategory) {

					// expand/collapse current item

					final TreeViewerItem tourItem = (TreeViewerItem) selection;

					if (_tagViewer.getExpandedState(tourItem)) {
						_tagViewer.collapseToLevel(tourItem, 1);
					} else {
						_tagViewer.expandToLevel(tourItem, 1);
					}
				}
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

	private void createUI_500_FilterActions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(5).applyTo(container);
		{
			{
				/*
				 * Checkbox: live update
				 */
				_chkLiveUpdate = new Button(container, SWT.CHECK);
				_chkLiveUpdate.setText(Messages.Slideout_TourFilter_Checkbox_IsLiveUpdate);
				_chkLiveUpdate.setToolTipText(Messages.Slideout_TourFilter_Checkbox_IsLiveUpdate_Tooltip);
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
						doApply();
					}
				});

				// set button default width
				UI.setButtonLayoutData(_btnApply);
			}
		}
	}

	private void doApply() {

		TourTagFilterManager.fireFilterModifyEvent();
	}

	private void doLiveUpdate() {

		_isLiveUpdate = _chkLiveUpdate.getSelection();

		_state.put(STATE_IS_LIVE_UPDATE, _isLiveUpdate);

		enableControls();

		fireModifyEvent();
	}

	private void enableControls() {

	}

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

	private void onDisposeSlideout() {

		_imgTag.dispose();
		_imgTagRoot.dispose();
		_imgTagCategory.dispose();
	}

	@Override
	protected void onFocus() {

		if (_selectedProfile != null
				&& _selectedProfile.name != null
				&& _selectedProfile.name.equals(Messages.Tour_Filter_Default_ProfileName)) {

			// default profile is selected, make it easy to rename it

			_txtProfileName.selectAll();
			_txtProfileName.setFocus();

		} else {

			_profileViewer.getTable().setFocus();
		}
	}

	private void onProfile_Add() {

		final TourTagFilterProfile filterProfile = new TourTagFilterProfile();

		// update model
		_filterProfiles.add(filterProfile);

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
		_filterProfiles.add(filterProfile);

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
		_filterProfiles.remove(_selectedProfile);
		TourTagFilterManager.setSelectedProfile(null);

		// update UI
		_profileViewer.remove(_selectedProfile);

		/*
		 * Select another filter at the same position
		 */
		final int numFilters = _filterProfiles.size();
		final int nextFilterIndex = Math.min(numFilters - 1, lastIndex);

		final Object nextSelectedProfile = _profileViewer.getElementAt(nextFilterIndex);
		if (nextSelectedProfile == null) {

			_selectedProfile = null;

		} else {

			selectProfile((TourTagFilterProfile) nextSelectedProfile);
		}

		enableControls();

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

	private void onProfile_Select() {

		TourTagFilterProfile selectedProfile = null;

		// get selected profile from viewer
		final StructuredSelection selection = (StructuredSelection) _profileViewer.getSelection();
		final Object firstElement = selection.getFirstElement();
		if (firstElement != null) {
			selectedProfile = (TourTagFilterProfile) firstElement;
		}

		if (_selectedProfile != null && _selectedProfile == selectedProfile) {
			// a new profile is not selected
			return;
		}

		_selectedProfile = selectedProfile;

		// update model
		TourTagFilterManager.setSelectedProfile(_selectedProfile);

		// update UI
		if (_selectedProfile == null) {

			_txtProfileName.setText(UI.EMPTY_STRING);

		} else {

			_txtProfileName.setText(_selectedProfile.name);

			if (_selectedProfile.name.equals(Messages.Tour_Filter_Default_ProfileName)) {

				// a default profile is selected, make is easy to rename it

				_txtProfileName.selectAll();
				_txtProfileName.setFocus();
			}
		}

		fireModifyEvent();
	}

//	private void onResizeFilterContent() {
//
//		if (_filterScrolled_Content == null || _filterScrolled_Content.isDisposed()) {
//			return;
//		}
//
//		final Point contentSize = _filterScrolled_Content.computeSize(SWT.DEFAULT, SWT.DEFAULT);
//
//		_filterScrolled_Container.setMinSize(contentSize);
//	}

	private void restoreState() {

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
		 * Other states
		 */
		_isLiveUpdate = Util.getStateBoolean(_state, STATE_IS_LIVE_UPDATE, false);
		_chkLiveUpdate.setSelection(_isLiveUpdate);

		// restore width for the profile list
		final int leftPartWidth = Util.getStateInt(_state, STATE_SASH_WIDTH, _pc.convertWidthInCharsToPixels(50));
		_sashForm.setViewerWidth(leftPartWidth);
	}

	private void selectProfile(final TourTagFilterProfile selectedProfile) {

		_profileViewer.setSelection(new StructuredSelection(selectedProfile));

		final Table table = _profileViewer.getTable();
		table.setSelection(table.getSelectionIndices());
	}

}
