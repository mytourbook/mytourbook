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
package net.tourbook.tag;

import java.util.HashMap;

import javax.persistence.EntityManager;

import net.tourbook.Messages;
import net.tourbook.data.TourTag;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.views.tagging.TagView;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

public class ActionMenuSetAllTagStructures extends Action implements IMenuCreator {

	private Menu	fMenu;

	private TagView	fTagView;

	private class ActionSetTagStructure extends Action {

		private int	fExpandType;

		private ActionSetTagStructure(final int expandType, final String name) {

			super(name, AS_CHECK_BOX);
			fExpandType = expandType;
		}

		@Override
		public void run() {

			final Runnable runnable = new Runnable() {

				public void run() {

					final EntityManager em = TourDatabase.getInstance().getEntityManager();
					try {

						/*
						 * update all tags which has not the current expand type
						 */

						final HashMap<Long, TourTag> allTourTags = TourDatabase.getAllTourTags();
						for (final TourTag tourTag : allTourTags.values()) {

							if (tourTag.getExpandType() != fExpandType) {

								// set new expand type

								final Long tagId = tourTag.getTagId();
								final TourTag tagInDb = em.find(TourTag.class, tagId);
								if (tagInDb != null) {

									tagInDb.setExpandType(fExpandType);

									final TourTag savedEntity = TourDatabase.saveEntity(tagInDb,
											tagId,
											TourTag.class,
											em);

									if (savedEntity != null) {

										// set entity from the database into the all tag list

										allTourTags.put(tagId, savedEntity);
									}
								}
							}
						}

					} catch (final Exception e) {
						e.printStackTrace();
					} finally {

						em.close();
					}

					fTagView.reloadViewer();
				}

			};
			BusyIndicator.showWhile(Display.getCurrent(), runnable);
		}
	}

	public ActionMenuSetAllTagStructures(final TagView tagView) {

		super(Messages.app_action_tag_set_all_tag_structures, AS_DROP_DOWN_MENU);
		setMenuCreator(this);

		fTagView = tagView;
	}

	private void addActionToMenu(final Action action) {

		final ActionContributionItem item = new ActionContributionItem(action);
		item.fill(fMenu, -1);
	}

	public void dispose() {
		if (fMenu != null) {
			fMenu.dispose();
			fMenu = null;
		}
	}

	public Menu getMenu(final Control parent) {
		return null;
	}

	public Menu getMenu(final Menu parent) {

		dispose();
		fMenu = new Menu(parent);

		// Add listener to repopulate the menu each time
		fMenu.addMenuListener(new MenuAdapter() {
			@Override
			public void menuShown(final MenuEvent e) {

				final Menu menu = (Menu) e.widget;

				// dispose old items
				for (final MenuItem menuItem : menu.getItems()) {
					menuItem.dispose();
				}

				/*
				 * create all expand types
				 */
				int typeIndex = 0;
				for (final int expandType : TagManager.EXPAND_TYPES) {

					final ActionSetTagStructure actionTagStructure = new ActionSetTagStructure(expandType,
							TagManager.EXPAND_TYPE_NAMES[typeIndex++]);

					addActionToMenu(actionTagStructure);
				}
			}
		});

		return fMenu;
	}

}
