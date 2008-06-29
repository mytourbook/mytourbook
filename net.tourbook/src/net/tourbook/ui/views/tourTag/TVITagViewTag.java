package net.tourbook.ui.views.tourTag;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import javax.persistence.EntityManager;

import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TreeViewerItem;
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.UI;

import org.eclipse.jface.viewers.TreeViewer;

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
			setChildren(getChildrenFlat(null));
			break;

		case TourTag.EXPAND_TYPE_YEAR_MONTH_DAY:
			setChildren(getChildrenYearMonthDay(true));
			break;

		case TourTag.EXPAND_TYPE_YEAR_DAY:
			setChildren(getChildrenYearMonthDay(false));
			break;

		default:
			break;
		}

	}

	/**
	 * get all tours for the tag Id of this tree item
	 */
	private ArrayList<TreeViewerItem> getChildrenFlat(final String whereClause) {

		final ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();

		try {

			final SQLFilter sqlFilter = new SQLFilter();
			final StringBuilder sb = new StringBuilder();

			sb.append("SELECT");

			sb.append(" TourData.tourId,");//						1 
			sb.append(" jTdataTtag2.TourTag_tagId,");//		2
			sb.append(TVITagViewTour.SQL_TOUR_COLUMNS); //	3

			sb.append(" FROM " + TourDatabase.JOINTABLE_TOURDATA__TOURTAG + " jTdataTtag");

			// get all tours for current tag
			sb.append(" LEFT OUTER JOIN " + TourDatabase.TABLE_TOUR_DATA + " TourData");
			sb.append(" ON jTdataTtag.TourData_tourId = TourData.tourId ");

			// get all tag id's for one tour 
			sb.append(" LEFT OUTER JOIN " + TourDatabase.JOINTABLE_TOURDATA__TOURTAG + " jTdataTtag2");
			sb.append(" ON TourData.tourID = jTdataTtag2.TourData_tourId");

			sb.append(" WHERE jTdataTtag.TourTag_TagId = ?");

			if (whereClause != null) {
				sb.append(whereClause);
			}

			sb.append(sqlFilter.getWhereClause());

			sb.append(" ORDER BY startYear, startMonth, startDay, startHour, startMinute"); //$NON-NLS-1$

			long previousTourId = -1;
			TVITagViewTour tourItem = null;

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
		return children;
	}

	private ArrayList<TreeViewerItem> getChildrenYearMonthDay(final boolean isMonth) {

		/*
		 * get the children for the tag item
		 */
		final ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();

		try {

			/*
			 * get all tours for the tag Id of this tree item
			 */
			final StringBuilder sb = new StringBuilder();
			final SQLFilter sqlFilter = new SQLFilter();

			sb.append("SELECT ");

			sb.append(" startYear,"); //		// 1
			sb.append(SQL_SUM_COLUMNS);

			sb.append(" FROM " + TourDatabase.JOINTABLE_TOURDATA__TOURTAG + " jTdataTtag");

			// get all tours for current tag
			sb.append(" LEFT OUTER JOIN " + TourDatabase.TABLE_TOUR_DATA + " TourData");
			sb.append(" ON jTdataTtag.TourData_tourId = TourData.tourId ");

			sb.append(" WHERE jTdataTtag.TourTag_TagId = ?");
			sb.append(sqlFilter.getWhereClause());

			sb.append(" GROUP BY startYear");
			sb.append(" ORDER BY startYear");

			final Connection conn = TourDatabase.getInstance().getConnection();

			final PreparedStatement statement = conn.prepareStatement(sb.toString());
			statement.setLong(1, tagId);
			sqlFilter.setParameters(statement, 2);

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

		return children;
	}

	public int getExpandType() {
		return fExpandType;
	}

	public String getName() {
		return name;
	}

	public long getTagId() {
		return tagId;
	}

	/**
	 * @param modifiedTours
	 * @return Returns an expression to select tour id's in the WHERE clause or <code>null</code>
	 *         when tour id's are not available
	 */
	private String getTourIdWhereClause(final ArrayList<TourData> modifiedTours) {

		if (modifiedTours.size() == 0) {
			return null;
		}

		final StringBuilder sb = new StringBuilder();
		boolean isFirst = true;

		sb.append(" AND TourData.tourId IN (");

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

	/**
	 * According to the expand type, the structure of the tag will be modified for the added or
	 * removed tours
	 * 
	 * @param tagViewer
	 * @param modifiedTours
	 * @param isAddMode
	 */
	public void refresh(final TreeViewer tagViewer, final ArrayList<TourData> modifiedTours, final boolean isAddMode) {

		switch (fExpandType) {
		case TourTag.EXPAND_TYPE_FLAT:
			refreshFlatTours(tagViewer, modifiedTours, isAddMode);
			break;

		case TourTag.EXPAND_TYPE_YEAR_MONTH_DAY:
			refreshYearMonthTours(tagViewer, modifiedTours, isAddMode);
			break;

		case TourTag.EXPAND_TYPE_YEAR_DAY:
			refreshYearTours(tagViewer, modifiedTours, isAddMode);
			break;

		default:
			break;
		}
	}

	private void refreshFlatTours(	final TreeViewer tagViewer,
									final ArrayList<TourData> modifiedTours,
									final boolean isAddMode) {

		final ArrayList<TreeViewerItem> unfetchedChildren = getUnfetchedChildren();
		if (unfetchedChildren == null) {
			// children are not fetched
			return;
		}

		if (isAddMode) {

			// add tours

			final ArrayList<TreeViewerItem> tagChildren = getChildrenFlat(getTourIdWhereClause(modifiedTours));

			// update model
			unfetchedChildren.addAll(tagChildren);

			// update viewer
			tagViewer.add(this, tagChildren.toArray());

		} else {

			// remove tours

			final HashMap<Long, TVITagViewTour> removedTours = new HashMap<Long, TVITagViewTour>();

			// loop all tour items
			for (final TreeViewerItem treeItem : unfetchedChildren) {

				if (treeItem instanceof TVITagViewTour) {

					final TVITagViewTour tourItem = (TVITagViewTour) treeItem;
					final long itemTourId = tourItem.getTourId();

					// find tour item in the modified tours
					for (final TourData tourData : modifiedTours) {
						if (tourData.getTourId().longValue() == itemTourId) {

							// tree tour item was found in the modified tours

							removedTours.put(itemTourId, tourItem);

							break;
						}
					}
				}
			}

			final Collection<TVITagViewTour> removedTourItems = removedTours.values();

			// update model
			for (final TVITagViewTour removedTourItem : removedTourItems) {
				unfetchedChildren.remove(removedTourItem);
			}

			// update viewer
			tagViewer.remove(removedTourItems.toArray());
		}
	}

	private void refreshYearMonthTours(	final TreeViewer tagViewer,
										final ArrayList<TourData> modifiedTours,
										final boolean isAddMode) {

	}

	private void refreshYearTours(	final TreeViewer tagViewer,
									final ArrayList<TourData> modifiedTours,
									final boolean isAddMode) {

	}

	@Override
	protected void remove() {}

	public void setExpandType(final int expandType) {
		fExpandType = expandType;
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
