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

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import net.tourbook.Messages;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourTagCategory;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;
import net.tourbook.ui.views.tourTag.TVITagViewTag;
import net.tourbook.ui.views.tourTag.TVITagViewTagCategory;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

public class ActionRenameTag extends Action {

	private TreeViewer	fTreeViewer;

	private static boolean updateCategory(final long id, final String tagName) {
		
		final EntityManager em = TourDatabase.getInstance().getEntityManager();
		
		boolean isSaved = false;
		final EntityTransaction ts = em.getTransaction();
		
		try {
			
			ts.begin();
			{
				final TourTagCategory categoryInDb = em.find(TourTagCategory.class, id);
				if (categoryInDb != null) {
					
					categoryInDb.setName(tagName);
					em.merge(categoryInDb);
				}
			}
			ts.commit();
			
		} catch (final Exception e) {
			e.printStackTrace();
		} finally {
			if (ts.isActive()) {
				ts.rollback();
			} else {
				isSaved = true;
			}
			em.close();
		}
		
		if (isSaved == false) {
			MessageDialog.openError(Display.getCurrent().getActiveShell(),//
					"Error", "Error occured when saving an entity"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		return isSaved;
	}

	private static boolean updateTag(final long id, final String tagName) {

		final EntityManager em = TourDatabase.getInstance().getEntityManager();

		boolean isSaved = false;
		final EntityTransaction ts = em.getTransaction();

		try {

			ts.begin();
			{
				final TourTag tagInDb = em.find(TourTag.class, id);
				if (tagInDb != null) {

					tagInDb.setTagName(tagName);
					em.merge(tagInDb);
				}
			}
			ts.commit();

		} catch (final Exception e) {
			e.printStackTrace();
		} finally {
			if (ts.isActive()) {
				ts.rollback();
			} else {
				isSaved = true;
			}
			em.close();
		}

		if (isSaved == false) {
			MessageDialog.openError(Display.getCurrent().getActiveShell(),//
					"Error", "Error occured when saving an entity"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		return isSaved;
	}

	public ActionRenameTag(final TreeViewer treeViewer) {

		super(Messages.action_tag_rename_tag, AS_PUSH_BUTTON);

		fTreeViewer = treeViewer;
	}

	/**
	 * Rename selected tag/category
	 */
	private void onRenameTourTag() {

		final Object selection = ((StructuredSelection) fTreeViewer.getSelection()).getFirstElement();

		String name = UI.EMPTY_STRING;
		String dlgTitle = UI.EMPTY_STRING;
		String dlgMessage = UI.EMPTY_STRING;

		if (selection instanceof TVITagViewTag) {

			name = ((TVITagViewTag) selection).getName();
			dlgTitle = Messages.action_tag_dlg_rename_title;
			dlgMessage = Messages.action_tag_dlg_rename_message;

		} else if (selection instanceof TVITagViewTagCategory) {

			name = ((TVITagViewTagCategory) selection).getName();
			dlgTitle = Messages.action_tagcategory_dlg_rename_title;
			dlgMessage = Messages.action_tagcategory_dlg_rename_message;
		}

		final InputDialog inputDialog = new InputDialog(Display.getCurrent().getActiveShell(),
				dlgTitle,
				dlgMessage,
				name,
				null);

		inputDialog.open();

		if (inputDialog.getReturnCode() != Window.OK) {
			return;
		}

		// save changed name

		name = inputDialog.getValue().trim();

		if (selection instanceof TVITagViewTag) {

			// save tag

			final TVITagViewTag tourTagItem = ((TVITagViewTag) selection);

			// persist tag
			updateTag(tourTagItem.getTagId(), name);

			fTreeViewer.update(tourTagItem, null);

		} else if (selection instanceof TVITagViewTagCategory) {

			// save category

			final TVITagViewTagCategory tourCategoryItem = ((TVITagViewTagCategory) selection);

			// persist category
			updateCategory(tourCategoryItem.getCategoryId(), name);

			fTreeViewer.update(tourCategoryItem, null);

		}

		TourDatabase.clearTourTags();

		// fire modify event
		TourManager.firePropertyChange(TourManager.TAG_STRUCTURE_CHANGED, null);
	}

	@Override
	public void run() {

		final Runnable runnable = new Runnable() {
			public void run() {
				onRenameTourTag();
			}
		};
		BusyIndicator.showWhile(Display.getCurrent(), runnable);
	}
}
