package net.tourbook.ui.views.tourTag;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.ArrayList;

import javax.persistence.EntityManager;

import net.tourbook.data.TourTag;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TreeViewerItem;
import net.tourbook.ui.TourTypeSQL;
import net.tourbook.ui.UI;

public class TVITagViewTag extends TVITagViewItem {

	private static final DateFormat	fDF	= DateFormat.getDateInstance(DateFormat.SHORT);

	long							tagId;
	String							name;

	private int						fExpandType;

	public TVITagViewTag(final TVITagViewItem parentItem) {
		setParentItem(parentItem);
	}

	@Override
	protected void fetchChildren() {

		switch (fExpandType) {
		case TourTag.EXPAND_TYPE_FLAT:
			getChildrenFlat();
			break;

		case TourTag.EXPAND_TYPE_YEAR_MONTH_DAY:
			getChildrenYearMonthDay(true);
			break;

		case TourTag.EXPAND_TYPE_YEAR_DAY:
			getChildrenYearMonthDay(false);
			break;

		default:
			break;
		}

	}

	/**
	 * get all tours for the tag Id of this tree item
	 */
	private void getChildrenFlat() {

		final ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();
		setChildren(children);

		try {

			final TourTypeSQL sqlTourTypes = UI.sqlTourTypes();
			final StringBuilder sb = new StringBuilder();

			sb.append("SELECT");

			sb.append(" tourId,");//						1 
			sb.append(" jTdataTtag2.TourTag_tagId,");//		2
			sb.append(TVITagViewTour.SQL_TOUR_COLUMNS); //	3

			sb.append(" FROM " + TourDatabase.JOINTABLE_TOURDATA__TOURTAG + " jTdataTtag");

			// get all tours for current tag
			sb.append(" LEFT OUTER JOIN " + TourDatabase.TABLE_TOUR_DATA + " TourData");
			sb.append(" ON jTdataTtag.TourData_tourId = TourData.tourId ");

			// get all tag id's for one tour 
			sb.append(" LEFT OUTER JOIN " + TourDatabase.JOINTABLE_TOURDATA__TOURTAG + " jTdataTtag2");
			sb.append(" ON TourData.tourID = jTdataTtag2.TourData_tourId");

			sb.append(" WHERE jTdataTtag.TourTag_TagId = ?");// + tagId);
			sb.append(UI.sqlTourPersonId());
			sb.append(sqlTourTypes.getWhereClause());

			sb.append(" ORDER BY startYear, startMonth, startDay, startHour, startMinute"); //$NON-NLS-1$

			long previousTourId = -1;
			TVITagViewTour tourItem = null;

			final Connection conn = TourDatabase.getInstance().getConnection();

			final PreparedStatement statement = conn.prepareStatement(sb.toString());
			statement.setLong(1, tagId);
			sqlTourTypes.setSQLParameters(statement, 2);

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

					tourItem = new TVITagViewTour(this);
					children.add(tourItem);

					tourItem.tourId = tourId;
					tourItem.getTourColumnData(result, resultTagId, 3);

					tourItem.treeColumn = fDF.format(tourItem.tourDate.toDate());
				}

				previousTourId = tourId;
			}

			conn.close();

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}
	}

	private void getChildrenYearMonthDay(final boolean isMonth) {

		/*
		 * set the children for the root item, these are year items
		 */
		final ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();
		setChildren(children);

		try {

			/*
			 * get all tours for the tag Id of this tree item
			 */
			final TourTypeSQL sqlTourTypes = UI.sqlTourTypes();
			final StringBuilder sb = new StringBuilder();

			sb.append("SELECT ");

			sb.append(" startYear,"); //		// 1
			sb.append(SQL_SUM_COLUMNS);

			sb.append(" FROM " + TourDatabase.JOINTABLE_TOURDATA__TOURTAG + " jTdataTtag");

			// get all tours for current tag
			sb.append(" LEFT OUTER JOIN " + TourDatabase.TABLE_TOUR_DATA + " TourData");
			sb.append(" ON jTdataTtag.TourData_tourId = TourData.tourId ");

			sb.append(" WHERE jTdataTtag.TourTag_TagId = ?");
			sb.append(UI.sqlTourPersonId());
			sb.append(sqlTourTypes.getWhereClause());

			sb.append(" GROUP BY startYear");
			sb.append(" ORDER BY startYear");

			final Connection conn = TourDatabase.getInstance().getConnection();

			final PreparedStatement statement = conn.prepareStatement(sb.toString());
			statement.setLong(1, tagId);
			sqlTourTypes.setSQLParameters(statement, 2);

			final ResultSet result = statement.executeQuery();
			while (result.next()) {

				final int dbYear = result.getInt(1);

				final TVITagViewYear tourItem = new TVITagViewYear(this, dbYear, isMonth);
				children.add(tourItem);

				tourItem.treeColumn = Integer.toString(dbYear);
				tourItem.getSumColumnData(result, 2);
			}

			conn.close();

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}
	}

	public int getExpandType() {
		return fExpandType;
	}

	@Override
	protected void remove() {}

	public void setExpandType(final int expandType) {
		fExpandType = expandType;
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

				TourDatabase.saveEntity(tagInDb, tagId, TourTag.class, em);
			}

		} catch (final Exception e) {
			e.printStackTrace();
		} finally {

			em.close();

			fExpandType = expandType;
		}

	}

}
