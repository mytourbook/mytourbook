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
package net.tourbook.sign;

import java.util.ArrayList;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import net.tourbook.data.TourSign;
import net.tourbook.data.TourSignCategory;
import net.tourbook.database.TourDatabase;

import org.eclipse.jface.viewers.TreeViewer;

public class TVIPrefSignRoot extends TVIPrefSignItem {

	public TVIPrefSignRoot(final TreeViewer signViewer) {
		super(signViewer);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void fetchChildren() {

		final EntityManager em = TourDatabase.getInstance().getEntityManager();

		if (em != null) {

			/*
			 * read tour signs from db
			 */
			Query query = em.createQuery(//
					//
					"SELECT tourSign" //$NON-NLS-1$
							+ (" FROM " + TourDatabase.TABLE_TOUR_SIGN + " AS tourSign ") //$NON-NLS-1$
							+ (" WHERE tourSign.isRoot = 1")); //$NON-NLS-1$

			final ArrayList<TourSign> tourSigns = (ArrayList<TourSign>) query.getResultList();

			for (final TourSign tourSign : tourSigns) {
				final TVIPrefSign signItem = new TVIPrefSign(getSignViewer(), tourSign);
				addChild(signItem);
			}

			/*
			 * read sign categories from db
			 */
			query = em.createQuery(//
					//
					"SELECT tourSignCategory" //$NON-NLS-1$
							+ (" FROM " + TourDatabase.TABLE_TOUR_SIGN_CATEGORY + " AS tourSignCategory") //$NON-NLS-1$
							+ (" WHERE tourSignCategory.isRoot = 1")); //$NON-NLS-1$

			final ArrayList<TourSignCategory> tourSignCategories = (ArrayList<TourSignCategory>) query.getResultList();

			for (final TourSignCategory tourSignCategory : tourSignCategories) {

				final TVIPrefSignCategory categoryItem = new TVIPrefSignCategory(getSignViewer(), tourSignCategory);
				addChild(categoryItem);
			}

			em.close();
		}
	}
}
