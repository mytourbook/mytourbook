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
import java.util.HashMap;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.database.TourDatabase;
import net.tourbook.tag.TagMenuManager;
import net.tourbook.ui.ITourProvider2;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolItem;

/**
 * Slideout for the tour tag filter
 */
public class SlideoutTourTagFilter extends AdvancedSlideout implements ITourProvider2 {

	private PixelConverter		_pc;

	private ToolItem			_tourTagFilterItem;

	private TagMenuManager		_tagMenuMgr;

	private TourData			_dummyTourData;
	private ArrayList<TourData>	_dummyTourDataList;

	/*
	 * UI controls
	 */
	private Composite			_innerContainer;

	private Label				_lblTags;
	private Link				_linkTag;

	public SlideoutTourTagFilter(	final ToolItem toolItem,
									final IDialogSettings state) {

		super(toolItem.getParent(), state, new int[] { 400, 300, 400, 400 });

		_tourTagFilterItem = toolItem;

		setShellFadeOutDelaySteps(30);
		setTitleText(Messages.Slideout_TourTagFilter_Label_Title);

		/*
		 * Create dummy tour data to handle setting of the tags
		 */
		_dummyTourData = new TourData();

		/*
		 * create a dummy tour id because setting of the tags and tour type works requires it
		 * otherwise it would cause a NPE when a tour has no id
		 */
		_dummyTourData.createTourIdDummy();

		_dummyTourDataList = new ArrayList<>();
		_dummyTourDataList.add(_dummyTourData);
	}

	/**
	 * create the drop down menus, this must be created after the parent control is created
	 */
	private void createMenus() {

		MenuManager menuMgr;

		/*
		 * Tag menu
		 */
		menuMgr = new MenuManager();

		menuMgr.setRemoveAllWhenShown(true);

		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(final IMenuManager menuMgr) {

				final Set<TourTag> tourTags = _dummyTourData.getTourTags();
				final boolean isTagInTour = tourTags.size() > 0;

				_tagMenuMgr.fillTagMenu(menuMgr);
				_tagMenuMgr.enableTagActions(true, isTagInTour, tourTags);
			}
		});

		// set menu for the tag item

		final Menu tagContextMenu = menuMgr.createContextMenu(_linkTag);
		tagContextMenu.addMenuListener(new MenuAdapter() {
			@Override
			public void menuHidden(final MenuEvent e) {
				_tagMenuMgr.onHideMenu();
			}

			@Override
			public void menuShown(final MenuEvent menuEvent) {

				final Rectangle rect = _linkTag.getBounds();
				Point pt = new Point(rect.x, rect.y + rect.height);
				pt = _linkTag.getParent().toDisplay(pt);

				_tagMenuMgr.onShowMenu(menuEvent, _linkTag, pt, null);
			}
		});

		_linkTag.setMenu(tagContextMenu);
	}

	@Override
	protected void createSlideoutContent(final Composite parent) {

		initUI(parent);

		createUI(parent);
		createTagActions();
		createMenus();

		restoreState();
		enableControls();

		updateUI_Tags();
	}

	private void createTagActions() {

		_tagMenuMgr = new TagMenuManager(this, false);
	}

	private void createUI(final Composite parent) {

		final Composite shellContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(shellContainer);
		GridLayoutFactory.fillDefaults().applyTo(shellContainer);
		{
			_innerContainer = new Composite(shellContainer, SWT.NONE);
			GridDataFactory
					.fillDefaults()//
					.grab(true, true)
					.applyTo(_innerContainer);
			GridLayoutFactory
					.swtDefaults()
					.numColumns(2)
					.spacing(20, 5)
					.applyTo(_innerContainer);
			{
				{
					/*
					 * tags
					 */
					_linkTag = new Link(_innerContainer, SWT.NONE);
					_linkTag.setText(Messages.tour_editor_label_tour_tag);
					GridDataFactory
							.fillDefaults()//
							.align(SWT.BEGINNING, SWT.BEGINNING)
							.applyTo(_linkTag);
					_linkTag.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(final SelectionEvent e) {
							net.tourbook.common.UI.openControlMenu(_linkTag);
						}
					});

					_lblTags = new Label(_innerContainer, SWT.WRAP);
					GridDataFactory
							.fillDefaults()//
							.grab(true, true)
							/*
							 * hint is necessary that the width is not expanded when the text is
							 * long
							 */
							.hint(_pc.convertWidthInCharsToPixels(100), SWT.DEFAULT)
							.applyTo(_lblTags);
				}
			}
		}
	}

	private void enableControls() {

	}

	@Override
	protected Rectangle getParentBounds() {

		final Rectangle itemBounds = _tourTagFilterItem.getBounds();
		final Point itemDisplayPosition = _tourTagFilterItem.getParent().toDisplay(itemBounds.x, itemBounds.y);

		itemBounds.x = itemDisplayPosition.x;
		itemBounds.y = itemDisplayPosition.y;

		return itemBounds;
	}

	@Override
	public ArrayList<TourData> getSelectedTours() {
		return _dummyTourDataList;
	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);

	}

	@Override
	protected void onFocus() {

	}

	private void restoreState() {

		final Set<TourTag> filterTags = _dummyTourData.getTourTags();
		filterTags.clear();

		final HashMap<Long, TourTag> allTags = TourDatabase.getAllTourTags();

		final long[] tourTagFilterIds = TourTagFilterManager.getTourTagFilterIds();

		for (final long filterTagId : tourTagFilterIds) {

			final TourTag tag = allTags.get(filterTagId);
			if (tag != null) {
				filterTags.add(tag);
			}
		}
	}

	private void saveStateTags() {

		TourTagFilterManager.updateTourTagFilter(_dummyTourData.getTourTags());
	}

	@Override
	public void toursAreModified(final ArrayList<TourData> modifiedTours) {

		if ((modifiedTours != null) && (modifiedTours.size() > 0)) {

			// check if it's the correct tour
			if (_dummyTourData == modifiedTours.get(0)) {

				updateUI_Tags();

				saveStateTags();

				TourTagFilterManager.fireFilterModifyEvent();
			}
		}
	}

	private void updateUI_Tags() {

		net.tourbook.ui.UI.updateUI_Tags(_dummyTourData, _lblTags, true);

		// reflow layout that the tags are aligned correctly
		_innerContainer.layout(true);
	}

}
