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
package net.tourbook.ui.views.tagging;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import javax.persistence.EntityManager;

import org.eclipse.jface.viewers.TreeViewer;

import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.UI;

public class TVITagView_Tag extends TVITagViewItem {

	long				tagId;

	String			name;

	private int		_expandType;

	public boolean	isRoot	= false;

	public TVITagView_Tag(final TVITagViewItem parentItem) {
		setParentItem(parentItem);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final TVITagView_Tag other = (TVITagView_Tag) obj;
		if (tagId != other.tagId) {
			return false;
		}
		return true;
	}

	@Override
	protected void fetchChildren() {

		switch (_expandType) {
		case TourTag.EXPAND_TYPE_FLAT:
			setChildren(readTagChildren_Tours(UI.EMPTY_STRING));
			break;

		case TourTag.EXPAND_TYPE_YEAR_MONTH_DAY:
			setChildren(readTagChildren_Years(true, UI.EMPTY_STRING));
			break;

		case TourTag.EXPAND_TYPE_YEAR_DAY:
			setChildren(readTagChildren_Years(false, UI.EMPTY_STRING));
			break;

		default:
			break;
		}

	}

	public int getExpandType() {
		return _expandType;
	}

	public String getName() {
		return name;
	}

	public long getTagId() {
		return tagId;
	}

	/**
	 * @param modifiedTours
	 * @return Returns an expression to select tour id's in the WHERE clause
	 */
	private String getTourIdWhereClause(final ArrayList<TourData> modifiedTours) {

		if (modifiedTours.size() == 0) {
			return UI.EMPTY_STRING;
		}

		final StringBuilder sb = new StringBuilder();
		boolean isFirst = true;

		sb.append(" AND TourData.tourId IN ("); //$NON-NLS-1$

		for (final TourData tourData : modifiedTours) {

			if (isFirst) {
				isFirst = false;
			} else {
				sb.append(',');
			}

			sb.append(Long.toString(tourData.getTourId()));
		}

		sb.append(')');

		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (tagId ^ (tagId >>> 32));
		return result;
	}

	/**
	 * get all tours for the tag Id of this tree item
	 */
	private ArrayList<TreeViewerItem> readTagChildren_Tours(final String whereClause) {

		final ArrayList<TreeViewerItem> children = new ArrayList<>();

		try {

			final SQLFilter sqlFilter = new SQLFilter();
			final StringBuilder sb = new StringBuilder();

			sb.append("SELECT"); //$NON-NLS-1$

			sb.append(" TourData.tourId,");//				1  //$NON-NLS-1$
			sb.append(" jTdataTtag2.TourTag_tagId,");//		2 //$NON-NLS-1$
			sb.append(TVITagView_Tour.SQL_TOUR_COLUMNS); //	3

			sb.append(" FROM " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " jTdataTtag"); //$NON-NLS-1$ //$NON-NLS-2$

			// get all tours for current tag
			sb.append(" LEFT OUTER JOIN " + TourDatabase.TABLE_TOUR_DATA + " TourData"); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(" ON jTdataTtag.TourData_tourId = TourData.tourId "); //$NON-NLS-1$

			// get all tag id's for one tour
			sb.append(" LEFT OUTER JOIN " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " jTdataTtag2"); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(" ON TourData.tourID = jTdataTtag2.TourData_tourId"); //$NON-NLS-1$

			sb.append(" WHERE jTdataTtag.TourTag_TagId = ?"); //$NON-NLS-1$
			sb.append(whereClause);
			sb.append(sqlFilter.getWhereClause());

			sb.append(" ORDER BY startYear, startMonth, startDay, startHour, startMinute"); //$NON-NLS-1$

			long previousTourId = -1;
			TVITagView_Tour tourItem = null;

			final Connection conn = TourDatabase.getInstance().getConnection();

			final PreparedStatement statement = conn.prepareStatement(sb.toString());
			statement.setLong(1, tagId);
			sqlFilter.setParameters(statement, 2);

			final ResultSet result = statement.executeQuery();
			while (result.next()) {

				final long tourId = result.getLong(1);
				final Object resultTagId = result.getObject(2);

				if (tourId == previousTourId) {

					// get tags from outer join

					if (resultTagId instanceof Long) {
						tourItem.tagIds.add((Long) resultTagId);
					}

				} else {

					tourItem = new TVITagView_Tour(this);
					children.add(tourItem);

					tourItem.tourId = tourId;
					tourItem.getTourColumnData(result, resultTagId, 3);

					tourItem.treeColumn = tourItem.tourDate.format(TimeTools.Formatter_Date_S);
				}

				previousTourId = tourId;
			}

			conn.close();

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}
		return children;
	}

	private ArrayList<TreeViewerItem> readTagChildren_Years(final boolean isMonth, final String whereClause) {

		/*
		 * get the children for the tag item
		 */
		final ArrayList<TreeViewerItem> children = new ArrayList<>();

		try {

			/*
			 * get all tours for the tag Id of this tree item
			 */
			final StringBuilder sb = new StringBuilder();
			final SQLFilter sqlFilter = new SQLFilter();

			sb.append("SELECT "); //$NON-NLS-1$

			sb.append(" startYear,"); //		// 1 //$NON-NLS-1$
			sb.append(SQL_SUM_COLUMNS);

			sb.append(" FROM " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " jTdataTtag"); //$NON-NLS-1$ //$NON-NLS-2$

			// get all tours for current tag
			sb.append(" LEFT OUTER JOIN " + TourDatabase.TABLE_TOUR_DATA + " TourData"); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(" ON jTdataTtag.TourData_tourId = TourData.tourId "); //$NON-NLS-1$

			sb.append(" WHERE jTdataTtag.TourTag_TagId = ?"); //$NON-NLS-1$
			sb.append(whereClause);
			sb.append(sqlFilter.getWhereClause());

			sb.append(" GROUP BY startYear"); //$NON-NLS-1$
			sb.append(" ORDER BY startYear"); //$NON-NLS-1$

			final Connection conn = TourDatabase.getInstance().getConnection();

			final PreparedStatement statement = conn.prepareStatement(sb.toString());
			statement.setLong(1, tagId);
			sqlFilter.setParameters(statement, 2);

			final ResultSet result = statement.executeQuery();
			while (result.next()) {

				final int dbYear = result.getInt(1);

				final TVITagView_Year yearItem = new TVITagView_Year(this, dbYear, isMonth);
				children.add(yearItem);

				yearItem.treeColumn = Integer.toString(dbYear);
				yearItem.readSumColumnData(result, 2);
			}

			conn.close();

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}

		return children;
	}

	/**
	 * This tag was added or removed from tours. According to the expand type, the structure of the
	 * tag will be modified for the added or removed tours
	 *
	 * @param tagViewer
	 * @param modifiedTours
	 * @param isAddMode
	 */
	public void refresh(final TreeViewer tagViewer, final ArrayList<TourData> modifiedTours, final boolean isAddMode) {

		switch (_expandType) {
		case TourTag.EXPAND_TYPE_FLAT:
			refreshFlatTours(tagViewer, modifiedTours, isAddMode);
			break;

		case TourTag.EXPAND_TYPE_YEAR_MONTH_DAY:
			refreshYearItems(tagViewer, true);
			break;

		case TourTag.EXPAND_TYPE_YEAR_DAY:
			refreshYearItems(tagViewer, false);
			break;

		default:
			break;
		}
	}

	private void refreshFlatTours(final TreeViewer tagViewer,
											final ArrayList<TourData> modifiedTours,
											final boolean isAddMode) {

		final ArrayList<TreeViewerItem> unfetchedChildren = getUnfetchedChildren();
		if (unfetchedChildren == null) {
			// children are not fetched
			return;
		}

		if (isAddMode) {

			// this tag was added to tours

			final ArrayList<TreeViewerItem> tagChildren = readTagChildren_Tours(getTourIdWhereClause(modifiedTours));

			// update model
			unfetchedChildren.addAll(tagChildren);

			// update viewer
			tagViewer.add(this, tagChildren.toArray());

		} else {

			// this tag was remove from tours

			final HashMap<Long, TVITagView_Tour> removedTours = new HashMap<>();

			// loop all tour items
			for (final TreeViewerItem treeItem : unfetchedChildren) {

				if (treeItem instanceof TVITagView_Tour) {

					final TVITagView_Tour tourItem = (TVITagView_Tour) treeItem;
					final long itemTourId = tourItem.getTourId();

					// find tour item in the modified tours
					for (final TourData tourData : modifiedTours) {
						if (tourData.getTourId().longValue() == itemTourId) {

							// tree tour item was found in the modified tours

							// remove the item outside of the for loop
							removedTours.put(itemTourId, tourItem);

							break;
						}
					}
				}
			}

			final Collection<TVITagView_Tour> removedTourItems = removedTours.values();

			// update model
			unfetchedChildren.removeAll(removedTours.values());

			// update viewer
			tagViewer.remove(removedTourItems.toArray());
		}
	}

	/**
	 * Read the year totals, this will read all year items for the tag and removes the years which do
	 * not have any tour items
	 *
	 * @param tagViewer
	 * @param isMonth
	 */
	private void refreshYearItems(final TreeViewer tagViewer, final boolean isMonth) {

		final ArrayList<TreeViewerItem> allYearItems = readTagChildren_Years(isMonth, UI.EMPTY_STRING);

		// update model
		setChildren(allYearItems);

		// update viewer
		tagViewer.update(allYearItems.toArray(), null);
	}

	public void setExpandType(final int expandType) {
		_expandType = expandType;
	}

	public String setName(final String name) {
		this.name = name;
		return name;
	}

	/**
	 * Set the expand type for the item and save the changed model in the database
	 *
	 * @param expandType
	 */
	public void setNewExpandType(final int expandType) {

		final EntityManager em = TourDatabase.getInstance().getEntityManager();

		try {

			final TourTag tagInDb = em.find(TourTag.class, tagId);

			if (tagInDb != null) {

				tagInDb.setExpandType(expandType);

				TourDatabase.saveEntity(tagInDb, tagId, TourTag.class);
			}

		} catch (final Exception e) {
			e.printStackTrace();
		} finally {

			em.close();

			_expandType = expandType;
		}

	}

	@Override
	public String toString() {
		return "TVITagView_Tag " + System.identityHashCode(this) + " [tagId=" + tagId + ", name=" + name + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

}
