/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.collateTours;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.util.SQL;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.common.util.Util;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.TourTypeSQLData;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import org.joda.time.DateTime;

public class TVICollatedTour_Root extends TVICollatedTour {

	TVICollatedTour_Root(final CollatedToursView view) {
		super(view);
	}

	@Override
	protected void fetchChildren() {

		collateToursView.setIsInUIUpdate(true);
		{
			// set the children for the root item
			final ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();
			setChildren(children);

			final TourTypeSQLData sqlData = collateToursView.getCollatedSQL();
			if (sqlData == null) {
				return;
			}

			final SQLFilter sqlFilter = new SQLFilter();

			final ArrayList<TVICollatedTour_Event> collateEvents = getCollateEvents(sqlData);

			children.addAll(collateEvents);

			getCollateTVI(collateEvents, sqlFilter);
		}
		collateToursView.setIsInUIUpdate(false);
	}

	/**
	 * Get all events/tours for the selected tour type filter.
	 * 
	 * @param sqlData
	 * @return
	 */
	private ArrayList<TVICollatedTour_Event> getCollateEvents(final TourTypeSQLData sqlData) {

		final ArrayList<TVICollatedTour_Event> collateEvents = new ArrayList<>();

		final String sql = "" // //$NON-NLS-1$

				+ "SELECT" //											//$NON-NLS-1$

				+ " tourID, " //									1	//$NON-NLS-1$
				+ " jTdataTtag.TourTag_tagId, "//					2	//$NON-NLS-1$

				+ " tourStartTime, " //								3	//$NON-NLS-1$
				+ " tourTitle" //									4	//$NON-NLS-1$

				+ " FROM " + TourDatabase.TABLE_TOUR_DATA + "\n" //$NON-NLS-1$ //$NON-NLS-2$

				// get tag id's
				+ (" LEFT OUTER JOIN " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " jTdataTtag") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" ON TourData.tourId = jTdataTtag.TourData_tourId") //$NON-NLS-1$

				+ (" WHERE 1=1" + sqlData.getWhereString()) // 			//$NON-NLS-1$

				+ " ORDER BY tourStartTime";//							//$NON-NLS-1$

		Connection conn = null;

		try {

			int eventCounter = 0;

			conn = TourDatabase.getInstance().getConnection();

			final PreparedStatement statement = conn.prepareStatement(sql);
			sqlData.setParameters(statement, 1);

			long prevTourId = -1;
			HashSet<Long> tagIds = null;

			final ResultSet result = statement.executeQuery();
			while (result.next()) {

				final long dbTourId = result.getLong(1);
				final Object dbTagId = result.getObject(2);

				if (dbTourId == prevTourId) {

					// additional result set's for the same tour

					// get tags from outer join
					if (dbTagId instanceof Long) {
						tagIds.add((Long) dbTagId);
					}

				} else {

					final long dbTourStartTime = result.getLong(3);
					final String dbTourTitle = result.getString(4);

					final TVICollatedTour_Event collateEvent = new TVICollatedTour_Event(collateToursView, this);
					collateEvents.add(collateEvent);

					final DateTime eventStart = new DateTime(dbTourStartTime);

					collateEvent.treeColumn = dbTourTitle == null ? UI.EMPTY_STRING : dbTourTitle;

					collateEvent.tourId = dbTourId;
					collateEvent.eventStart = eventStart;

					collateEvent.isFirstEvent = eventCounter++ == 0;

					collateEvent.colTourTitle = dbTourTitle;

					// get first tag id
					if (dbTagId instanceof Long) {

						tagIds = new HashSet<Long>();
						tagIds.add((Long) dbTagId);

						collateEvent.setTagIds(tagIds);
					}
				}

				prevTourId = dbTourId;
			}

		} catch (final SQLException e) {
			SQL.showException(e, sql);
		} finally {
			Util.closeSql(conn);
		}

		/*
		 * Add an additional event which shows the tours from the last event until today.
		 */
		final TVICollatedTour_Event collateEvent = new TVICollatedTour_Event(collateToursView, this);
		collateEvents.add(collateEvent);

		final DateTime eventStart = new DateTime();

		collateEvent.treeColumn = UI.EMPTY_STRING;
		collateEvent.eventStart = eventStart;
		collateEvent.isLastEvent = true;

		return collateEvents;
	}

	private void getCollateTVI(final ArrayList<TVICollatedTour_Event> collatedEvents, final SQLFilter sqlFilter) {

		final int eventSize = collatedEvents.size();

		Connection conn = null;

		final String sql = "" //$NON-NLS-1$
				//
				+ "SELECT " //						 //$NON-NLS-1$
				+ SQL_SUM_COLUMNS

				+ " FROM " + TourDatabase.TABLE_TOUR_DATA + "\n" //$NON-NLS-1$ //$NON-NLS-2$

				+ (" WHERE TourStartTime >= ? AND TourStartTime < ?") //$NON-NLS-1$
				+ sqlFilter.getWhereClause();

		try {

			conn = TourDatabase.getInstance().getConnection();

			final PreparedStatement statement = conn.prepareStatement(sql);
			sqlFilter.setParameters(statement, 3);

			final long[] prevStart = { 0 };
			final int[] eventCounter = { 0 };
			final int[] eventIndex = { 0 };

			final long start = System.currentTimeMillis();

			boolean isLongDuration = false;

			for (; eventIndex[0] < eventSize;) {

				final int currentEventIndex = eventIndex[0]++;

				final boolean isFirstEvent = currentEventIndex == 0;
				final TVICollatedTour_Event collateEvent = collatedEvents.get(currentEventIndex);

				final long eventStart = isFirstEvent ? Long.MIN_VALUE : prevStart[0];
				final long eventEnd = collateEvent.eventStart.getMillis();

				prevStart[0] = eventEnd;

				/*
				 * This is a highly complicated algorithim that the eventStart is overwritten again
				 */
				collateEvent.eventStart = new DateTime(eventStart);
				collateEvent.eventEnd = new DateTime(eventEnd);
				collateEvent.isFirstEvent = isFirstEvent;

				statement.setLong(1, eventStart);
				statement.setLong(2, eventEnd);

				final ResultSet result = statement.executeQuery();

				while (result.next()) {
					collateEvent.addSumColumns(result, 1);
				}

				/*
				 * Check if this is a long duration, run in progress monitor
				 */
				final long runDuration = System.currentTimeMillis() - start;
				if (runDuration > 500) {
					isLongDuration = true;
					break;
				}

				++eventCounter[0];
			}

			if (isLongDuration) {

				try {

					/*
					 * Run with a monitor because it can take a longer time until all is computed.
					 */

					final IRunnableWithProgress runnable = new IRunnableWithProgress() {
						@Override
						public void run(final IProgressMonitor monitor) throws InvocationTargetException,
								InterruptedException {

							try {
								monitor.beginTask(Messages.Tour_Book_Monitor_CollateTask, eventSize);
								monitor.worked(eventCounter[0]);

								for (; eventIndex[0] < eventSize;) {

									if (monitor.isCanceled()) {
										break;
									}

									final int currentEventIndex = eventIndex[0]++;

									final boolean isFirstEvent = currentEventIndex == 0;
									final TVICollatedTour_Event collateEvent = collatedEvents.get(currentEventIndex);

									final long eventStart = isFirstEvent ? Long.MIN_VALUE : prevStart[0];
									final long eventEnd = collateEvent.eventStart.getMillis();

									prevStart[0] = eventEnd;

									/*
									 * This is a highly complicated algorithim that the eventStart
									 * is overwritten again
									 */
									collateEvent.eventStart = new DateTime(eventStart);
									collateEvent.eventEnd = new DateTime(eventEnd);
									collateEvent.isFirstEvent = isFirstEvent;

									statement.setLong(1, eventStart);
									statement.setLong(2, eventEnd);

									final ResultSet result = statement.executeQuery();

									while (result.next()) {
										collateEvent.addSumColumns(result, 1);
									}

									monitor.subTask(NLS.bind(
											Messages.Tour_Book_Monitor_CollateSubtask,
											++eventCounter[0],
											eventSize));
									monitor.worked(1);
								}
							} catch (final SQLException e) {
								SQL.showException(e, sql);
							}
						}
					};

					/*
					 * Use the shell of the main app that the tooltip is not covered with the
					 * monitor, otherwise it would be centered of the active shell.
					 */
					new ProgressMonitorDialog(collateToursView.getShell()).run(true, true, runnable);

				} catch (final InvocationTargetException e) {
					StatusUtil.showStatus(e);
				} catch (final InterruptedException e) {
					StatusUtil.showStatus(e);
				}
			}

		} catch (final SQLException e) {
			SQL.showException(e, sql);
		} finally {
			Util.closeSql(conn);
		}

	}

}
