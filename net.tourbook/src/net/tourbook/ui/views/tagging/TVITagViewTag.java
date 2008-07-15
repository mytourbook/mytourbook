package net.tourbook.ui.views.tagging;

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

	private static final DateFormat	fDF		= DateFormat.getDateInstance(DateFormat.SHORT);

	long							tagId;

	String							name;

	private int						fExpandType;

	public boolean					isRoot	= false;

	public TVITagViewTag(final TVITagViewItem parentItem) {
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
		final TVITagViewTag other = (TVITagViewTag) obj;
		if (tagId != other.tagId) {
			return false;
		}
		return true;
	}

	@Override
	protected void fetchChildren() {

		switch (fExpandType) {
		case TourTag.EXPAND_TYPE_FLAT:
			setChildren(readTagChildrenTours(UI.EMPTY_STRING));
			break;

		case TourTag.EXPAND_TYPE_YEAR_MONTH_DAY:
			setChildren(readTagChildrenYears(true, UI.EMPTY_STRING));
			break;

		case TourTag.EXPAND_TYPE_YEAR_DAY:
			setChildren(readTagChildrenYears(false, UI.EMPTY_STRING));
			break;

		default:
			break;
		}

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
	 * @return Returns an expression to select tour id's in the WHERE clause
	 */
	private String getTourIdWhereClause(final ArrayList<TourData> modifiedTours) {

		if (modifiedTours.size() == 0) {
			return UI.EMPTY_STRING;
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
	private ArrayList<TreeViewerItem> readTagChildrenTours(final String whereClause) {

		final ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();

		try {

			final SQLFilter sqlFilter = new SQLFilter();
			final StringBuilder sb = new StringBuilder();

			sb.append("SELECT");

			sb.append(" TourData.tourId,");//				1 
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
			sb.append(whereClause);
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

	private ArrayList<TreeViewerItem> readTagChildrenYears(final boolean isMonth, final String whereClause) {

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
			sb.append(whereClause);
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

				final TVITagViewYear yearItem = new TVITagViewYear(this, dbYear, isMonth);
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

//	/**
//	 * Read the year totals, this will read all year items for the tag and removes the years which
//	 * do not have any tour items
//	 * 
//	 * @param tagViewer
//	 */
//	private void readTotalsMonth(final TreeViewer tagViewer) {
//
////		final ArrayList<TreeViewerItem> allMonthItems = readYearChildrenMonths();
////
////		// update model
////		setChildren(allMonthItems);
////
////		// update viewer
////		tagViewer.update(allMonthItems.toArray(), null);
//	}

	/**
	 * Read the year totals, this will read all year items for the tag and removes the years which
	 * do not have any tour items
	 * 
	 * @param tagViewer
	 * @param isMonth
	 */
	private void readTotalsYear(final TreeViewer tagViewer, final boolean isMonth) {

		final ArrayList<TreeViewerItem> allYearItems = readTagChildrenYears(isMonth, UI.EMPTY_STRING);

		// update model
		setChildren(allYearItems);

		// update viewer
		tagViewer.update(allYearItems.toArray(), null);
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

			// this tag was added to tours

			final ArrayList<TreeViewerItem> tagChildren = readTagChildrenTours(getTourIdWhereClause(modifiedTours));

			// update model
			unfetchedChildren.addAll(tagChildren);

			// update viewer
			tagViewer.add(this, tagChildren.toArray());

		} else {

			// this tag was remove from tours

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

							// remove the item outside of the for loop
							removedTours.put(itemTourId, tourItem);

							break;
						}
					}
				}
			}

			final Collection<TVITagViewTour> removedTourItems = removedTours.values();

			// update model
			unfetchedChildren.removeAll(removedTours.values());

			// update viewer
			tagViewer.remove(removedTourItems.toArray());
		}
	}

	private void refreshYearMonthTours(	final TreeViewer tagViewer,
										final ArrayList<TourData> modifiedTours,
										final boolean isAddMode) {

//		final ArrayList<TreeViewerItem> unfetchedTagChildren = getUnfetchedChildren();
//		if (unfetchedTagChildren == null) {
//			// children are not fetched, nothing to do
//			return;
//		}
//
//		if (isAddMode) {
//
//			// this tag was added to tours
//
//		} else {
//
//			/*
//			 * this tag was removed from tours, remove the tour from this tag and remove the
//			 * year/month item when it does not contain tours
//			 */
//
//			final HashMap<Integer, TVITagViewYear> removedYears = new HashMap<Integer, TVITagViewYear>();
//
//			// loop: all year items for the current tag
//			for (final TreeViewerItem tagChildItem : unfetchedTagChildren) {
//				if (tagChildItem instanceof TVITagViewYear) {
//
//					final TVITagViewYear yearItem = (TVITagViewYear) tagChildItem;
//					final ArrayList<TreeViewerItem> unfetchedYearChildren = yearItem.getUnfetchedChildren();
//
//					if (unfetchedYearChildren == null) {
//
//						/*
//						 * The months for the year item are not fetched but the tag for such a tour
//						 * could have been removed. The year item will be removed if necessary in
//						 * the method readYearTotals()
//						 */
//
//						continue;
//
//					} else {
//
//						/*
//						 * the months for the current year item are fetched
//						 */
//
//						final HashMap<Integer, TVITagViewMonth> removedMonths = new HashMap<Integer, TVITagViewMonth>();
//
//						// loop: all month items for the current year item
//						for (final TreeViewerItem yearChildItem : unfetchedYearChildren) {
//							if (yearChildItem instanceof TVITagViewMonth) {
//
//								final TVITagViewMonth monthItem = (TVITagViewMonth) yearChildItem;
//								final ArrayList<TreeViewerItem> unfetchedMonthChildren = monthItem.getUnfetchedChildren();
//
//								if (unfetchedMonthChildren == null) {
//
//									/*
//									 * tours for the months are not fetched
//									 */
//
//									continue;
//
//								} else {
//
//									final HashMap<Long, TVITagViewTour> removedTours = new HashMap<Long, TVITagViewTour>();
//
//									// loop: all tour items
//									for (final TreeViewerItem monthChildItem : unfetchedMonthChildren) {
//										if (monthChildItem instanceof TVITagViewTour) {
//
//											final TVITagViewTour tourItem = (TVITagViewTour) monthChildItem;
//											final long tourItemTourId = tourItem.tourId;
//
//											// loop: all modified tours
//											for (final TourData tourData : modifiedTours) {
//												if (tourData.getTourId() == tourItemTourId) {
//
//													/*
//													 * the modified tour was found in the tree,
//													 * remove the tour from the year item
//													 */
//
//													removedTours.put(tourItemTourId, tourItem);
//
//													break;
//												}
//											}
//										}
//									}
//
//									if (removedTours.size() > 0) {
//
//										final Collection<TVITagViewTour> removedTourItems = removedTours.values();
//
//										// update month model
//										unfetchedMonthChildren.removeAll(removedTourItems);
//
//										if (unfetchedMonthChildren.size() == 0) {
//
//											// month item does not contain any tours, month will be removed
//
//											removedMonths.put(monthItem.hashCode(), monthItem);
//
//										} else {
//
//											// update viewer, remove tours from month
//											tagViewer.remove(removedTourItems.toArray());
//										}
//									}
//								}
//							}
//						}
//
//						if (removedMonths.size() > 0) {
//
//							final Collection<TVITagViewMonth> removedMonthItems = removedMonths.values();
//
//							// update year model
//							unfetchedYearChildren.removeAll(removedMonthItems);
//
//							if (unfetchedYearChildren.size() == 0) {
//
//								// year item does not contain any tours, year will be removed
//
//								removedYears.put(yearItem.hashCode(), yearItem);
//
//							} else {
//
//								// update viewer
//								tagViewer.remove(removedMonthItems.toArray());
//							}
//						}
//					}
//
//					readTotalsMonth(tagViewer);
//				}
//			}
//
//			if (removedYears.size() > 0) {
//
//				final Collection<TVITagViewYear> removedYearItems = removedYears.values();
//
//				// update tag model
//				unfetchedTagChildren.removeAll(removedYearItems);
//
//				// update viewer
//				tagViewer.remove(removedYearItems.toArray());
//			}
//		}

		readTotalsYear(tagViewer, true);
	}

	/**
	 * Refresh the children of the tag, these are year items
	 * 
	 * @param tagViewer
	 * @param modifiedTours
	 * @param isAddMode
	 */
	private void refreshYearTours(	final TreeViewer tagViewer,
									final ArrayList<TourData> modifiedTours,
									final boolean isAddMode) {

//		final ArrayList<TreeViewerItem> unfetchedTagChildren = getUnfetchedChildren();
//		if (unfetchedTagChildren == null) {
//			// children are not fetched, nothing to do
//			return;
//		}
//
//		if (isAddMode) {
//
//			// this tag was added to tours
//
//			// get year items for the modified tours
//			final ArrayList<TreeViewerItem> modifiedYearItems = readTagChildrenYears(false,
//					getTourIdWhereClause(modifiedTours));
//
//			/*
//			 * update model
//			 */
//
//			final ArrayList<TVITagViewYear> addedYearItems = new ArrayList<TVITagViewYear>();
//			final ArrayList<TVITagViewYear> updatedYearItems = new ArrayList<TVITagViewYear>();
//
//			// loop: all modified year items, add the year item or replace a year item
//			for (final TreeViewerItem treeItem : modifiedYearItems) {
//				if (treeItem instanceof TVITagViewYear) {
//
//					final TVITagViewYear newYearItem = (TVITagViewYear) treeItem;
//
//					final int oldYearItemIndex = unfetchedTagChildren.indexOf(newYearItem);
//					if (oldYearItemIndex == -1) {
//
//						// add new year item
//
//						unfetchedTagChildren.add(newYearItem);
//
//						addedYearItems.add(newYearItem);
//
//					} else {
//
//						// replace existing year item to display the corrected sum totals
//
//						unfetchedTagChildren.remove(oldYearItemIndex);
//						unfetchedTagChildren.add(newYearItem);
//
//						updatedYearItems.add(newYearItem);
//					}
//				}
//			}
//
//			/*
//			 * update viewer
//			 */
//			if (addedYearItems.size() > 0) {
//				tagViewer.add(this, addedYearItems.toArray());
//			}
//			if (updatedYearItems.size() > 0) {
//				tagViewer.update(updatedYearItems.toArray(), null);
//			}
//
//		} else {
//
//			/*
//			 * this tag was removed from tours, remove the tour from this tag and remove the year
//			 * item when it does not contain tours
//			 */
//
//			final HashMap<Integer, TVITagViewYear> removedYears = new HashMap<Integer, TVITagViewYear>();
//
//			// loop: all year items
//			for (final TreeViewerItem tagChildItem : unfetchedTagChildren) {
//				if (tagChildItem instanceof TVITagViewYear) {
//
//					final TVITagViewYear tagYearItem = (TVITagViewYear) tagChildItem;
//					final ArrayList<TreeViewerItem> unfetchedYearChildren = tagYearItem.getUnfetchedChildren();
//
//					if (unfetchedYearChildren == null) {
//
//						/*
//						 * the tours for the year item are not fetched but the tag for such a tour
//						 * could have been removed. Because the tours are not loaded for this year
//						 * item, it cannot be checked if the year item could have been removed
//						 */
//
//						continue;
//
//					} else {
//
//						/*
//						 * the tours for the current year item are fetched
//						 */
//						final HashMap<Long, TVITagViewTour> removedTours = new HashMap<Long, TVITagViewTour>();
//
//						// loop: all tour items in the current year item
//						for (final TreeViewerItem yearChildItem : unfetchedYearChildren) {
//							if (yearChildItem instanceof TVITagViewTour) {
//
//								final TVITagViewTour tourItem = (TVITagViewTour) yearChildItem;
//								final long tourItemTourId = tourItem.tourId;
//
//								for (final TourData tourData : modifiedTours) {
//									if (tourData.getTourId() == tourItemTourId) {
//
//										/*
//										 * the modified tour was found in the tree, remove the tour
//										 * from the year item
//										 */
//
//										removedTours.put(tourItemTourId, tourItem);
//
//										break;
//									}
//								}
//							}
//						}
//
//						if (removedTours.size() > 0) {
//
//							final Collection<TVITagViewTour> removedTourItems = removedTours.values();
//
//							// update year model
//							unfetchedYearChildren.removeAll(removedTourItems);
//
//							if (unfetchedYearChildren.size() == 0) {
//
//								// year item does not contain any tours, year will be removed
//
//								removedYears.put(tagYearItem.hashCode(), tagYearItem);
//
//							} else {
//
//								// update viewer
//								tagViewer.remove(removedTourItems.toArray());
//							}
//						}
//					}
//				}
//			}
//
//			if (removedYears.size() > 0) {
//
//				final Collection<TVITagViewYear> removedYearItems = removedYears.values();
//
//				// update tag model
//				unfetchedTagChildren.removeAll(removedYearItems);
//
//				// update viewer
//				tagViewer.remove(removedYearItems.toArray());
//			}
//		}

		readTotalsYear(tagViewer, false);
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
