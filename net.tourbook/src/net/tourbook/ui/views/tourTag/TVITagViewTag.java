package net.tourbook.ui.views.tourTag;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.persistence.EntityManager;

import net.tourbook.data.TourTag;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TreeViewerItem;

import org.joda.time.DateTime;

public class TVITagViewTag extends TVITagViewItem {

	public long	tagId;

	private int	fExpandType;

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
			getChildrenYearMonthDay();
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

		final StringBuilder sb = new StringBuilder();

		try {

			sb.append("SELECT ");

			sb.append(" Tdata.tourId,");//					// 1 
			sb.append(" Tdata.startYear,"); //				// 2 
			sb.append(" Tdata.startMonth,");//				// 3
			sb.append(" Tdata.startDay,");//				// 4
			sb.append(" Tdata.tourTitle,");//				// 5
			sb.append(" Tdata.tourType_typeId,");//			// 6

			sb.append(" jTdataTtag2.TourTag_tagId");//		// 7 

			sb.append(" FROM " + TourDatabase.JOINTABLE_TOURDATA__TOURTAG + " jTdataTtag");

			// get all tours for current tag
			sb.append(" LEFT OUTER JOIN " + TourDatabase.TABLE_TOUR_DATA + " Tdata");
			sb.append(" ON jTdataTtag.TourData_tourId=Tdata.tourId ");

			// get all tag id's for one tour 
			sb.append(" LEFT OUTER JOIN " + TourDatabase.JOINTABLE_TOURDATA__TOURTAG + " jTdataTtag2");
			sb.append(" ON Tdata.tourID = jTdataTtag2.TourData_tourId");

			sb.append(" WHERE jTdataTtag.TourTag_TagId = ?");// + tagId);
			sb.append(" ORDER BY startYear, startMonth, startDay, startHour, startMinute"); //$NON-NLS-1$

			long lastTourId = -1;
			ArrayList<Long> tagIds = null;

			final Connection conn = TourDatabase.getInstance().getConnection();

			final PreparedStatement statement = conn.prepareStatement(sb.toString());
			statement.setLong(1, tagId);

			final ResultSet result = statement.executeQuery();
			while (result.next()) {

				long tourId = result.getLong(1);

				if (tourId == lastTourId) {

					// get tags from outer join

					final Object resultTagId = result.getObject(7);
					if (resultTagId instanceof Long) {
						tagIds.add((Long) resultTagId);
					}

				} else {

					final TVITagViewTour tourItem = new TVITagViewTour(this);
					children.add(tourItem);

					tourItem.tourId = tourId = result.getLong(1);
					tourItem.tourDate = new DateTime(result.getInt(2), result.getInt(3), result.getInt(4), 0, 0, 0, 0);
					tourItem.tourTitle = result.getString(5);

					final Object tourTypeId = result.getObject(6);
					tourItem.tourTypeId = (tourTypeId == null ? TourDatabase.ENTITY_IS_NOT_SAVED : (Long) tourTypeId);

					final Object resultTagId = result.getObject(7);
					if (resultTagId instanceof Long) {
						tourItem.tagIds = tagIds = new ArrayList<Long>();
						tagIds.add((Long) resultTagId);
					}
				}

				lastTourId = tourId;
			}

			conn.close();

		} catch (final SQLException e) {
			e.printStackTrace();
		}
	}

	private void getChildrenYearMonthDay() {

		/*
		 * set the children for the root item, these are year items
		 */
		final ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();
		setChildren(children);

		final StringBuilder sb = new StringBuilder();

		try {

			/*
			 * get all tours for the tag Id of this tree item
			 */
			sb.append("SELECT ");

			sb.append(" startYear,"); //		// 1
			sb.append(SQL_SUM_COLUMNS);

			sb.append(" FROM " + TourDatabase.JOINTABLE_TOURDATA__TOURTAG + " jTdataTtag");

			// get all tours for current tag
			sb.append(" LEFT OUTER JOIN " + TourDatabase.TABLE_TOUR_DATA + " Tdata");
			sb.append(" ON jTdataTtag.TourData_tourId=Tdata.tourId ");

			sb.append(" WHERE jTdataTtag.TourTag_TagId = ?");
			sb.append(sqlTourPersonId());
			sb.append(sqlTourTypeId());

			sb.append(" GROUP BY startYear");
			sb.append(" ORDER BY startYear");

			final Connection conn = TourDatabase.getInstance().getConnection();

			final PreparedStatement statement = conn.prepareStatement(sb.toString());
			statement.setLong(1, tagId);

			final ResultSet result = statement.executeQuery();
			while (result.next()) {

				final int dbYear = result.getInt(1);

				final TVITagViewYear tourItem = new TVITagViewYear(this, dbYear);
				children.add(tourItem);

				tourItem.treeColumn = Integer.toString(dbYear);
				tourItem.addSumData(result, 1);
			}

			conn.close();

		} catch (final SQLException e) {
			e.printStackTrace();
		}
	}

	public int getExpandType() {
		return fExpandType;
	}

	@Override
	protected void remove() {}

	/**
	 * Set the expand type for the item and save the changed model in the database
	 * 
	 * @param expandType
	 */
	public void setExpandType(final int expandType) {

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
