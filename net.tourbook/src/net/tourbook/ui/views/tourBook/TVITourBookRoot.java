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
package net.tourbook.ui.views.tourBook;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

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

public class TVITourBookRoot extends TVITourBookItem {

	TVITourBookRoot(final TourBookView view) {
		super(view);
	}

	@Override
	protected void fetchChildren() {

		tourBookView.setIsInUIUpdate(true);

		if (tourBookView.getViewType() == ViewType.COLLATE_BY_TOUR_TYPE) {
			fetchChildren_ByTourType();
		} else {
			fetchChildren_ByYear();
		}

		tourBookView.setIsInUIUpdate(false);
	}

	private void fetchChildren_ByTourType() {

		/*
		 * set the children for the root item
		 */
		final ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();
		setChildren(children);

		final TourTypeSQLData sqlData = tourBookView.getCollatedSQL();
		if (sqlData == null) {
			return;
		}

		final ArrayList<TVITourBookCollateEvent> collateEvents = getCollateEvents(sqlData);

		children.addAll(collateEvents);

		getCollateTVI(collateEvents);
	}

	private void fetchChildren_ByYear() {

		/*
		 * set the children for the root item, these are year items
		 */
		final ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();
		setChildren(children);

		final SQLFilter sqlFilter = new SQLFilter();

		final String sql = "" //$NON-NLS-1$
				//
				+ "SELECT" //						 //$NON-NLS-1$
				+ " startYear," // //$NON-NLS-1$
				+ SQL_SUM_COLUMNS

				+ " FROM " + TourDatabase.TABLE_TOUR_DATA + UI.NEW_LINE //$NON-NLS-1$

				+ (" WHERE 1=1" + sqlFilter.getWhereClause()) // //$NON-NLS-1$

				+ " GROUP BY startYear" //			//$NON-NLS-1$
				+ " ORDER BY startYear";//			//$NON-NLS-1$

		Connection conn = null;

		try {

			conn = TourDatabase.getInstance().getConnection();

			final PreparedStatement statement = conn.prepareStatement(sql);
			sqlFilter.setParameters(statement, 1);

//			final long time = System.currentTimeMillis();
//			System.out.println(System.currentTimeMillis() - time + "ms");

			final ResultSet result = statement.executeQuery();
			while (result.next()) {

				final int dbYear = result.getInt(1);

				final TVITourBookYear yearItem = new TVITourBookYear(tourBookView, this);
				children.add(yearItem);

				yearItem.treeColumn = Integer.toString(dbYear);
				yearItem.tourYear = dbYear;

				calendar.set(dbYear, 0, 1);
				yearItem.colTourDate = calendar.getTimeInMillis();

				yearItem.addSumColumns(result, 2);
			}

		} catch (final SQLException e) {
			SQL.showException(e, sql);
		} finally {
			Util.closeSql(conn);
		}
	}

	private ArrayList<TVITourBookCollateEvent> getCollateEvents(final TourTypeSQLData sqlData) {

		final ArrayList<TVITourBookCollateEvent> collateEvents = new ArrayList<>();

		final String sql = "" // //$NON-NLS-1$

				+ "SELECT" //						 //$NON-NLS-1$
				+ " tourStartTime," //			1 //$NON-NLS-1$
				+ " tourTitle" //				2 //$NON-NLS-1$

				+ " FROM " + TourDatabase.TABLE_TOUR_DATA + "\n" //$NON-NLS-1$ //$NON-NLS-2$

				+ (" WHERE 1=1" + sqlData.getWhereString()) // //$NON-NLS-1$

				+ " ORDER BY tourStartTime";//			//$NON-NLS-1$

		Connection conn = null;

		try {

			conn = TourDatabase.getInstance().getConnection();

			final PreparedStatement statement = conn.prepareStatement(sql);
			sqlData.setParameters(statement, 1);

//			final long time = System.currentTimeMillis();
//			System.out.println(System.currentTimeMillis() - time + "ms");

			final ResultSet result = statement.executeQuery();
			while (result.next()) {

				final long dbTourStartTime = result.getLong(1);
				final String dbTourTitle = result.getString(2);

				final TVITourBookCollateEvent collateEvent = new TVITourBookCollateEvent(tourBookView, this);
				collateEvents.add(collateEvent);

				final DateTime eventStart = new DateTime(dbTourStartTime);
				final String eventStartText = UI.DateFormatterShort.format(dbTourStartTime);

				collateEvent.treeColumn = dbTourTitle == null ? UI.EMPTY_STRING : dbTourTitle;
				collateEvent.eventStart = eventStart;
				collateEvent.eventStartText = eventStartText;
			}

		} catch (final SQLException e) {
			SQL.showException(e, sql);
		} finally {
			Util.closeSql(conn);
		}

		/*
		 * Add an additional event which shows the tours from the last event until today.
		 */
		final TVITourBookCollateEvent collateEvent = new TVITourBookCollateEvent(tourBookView, this);
		collateEvents.add(collateEvent);

		final DateTime eventStart = new DateTime();
		final String eventStartText = UI.DateFormatterShort.format(eventStart.getMillis());

		collateEvent.treeColumn = "Today"; //$NON-NLS-1$
		collateEvent.eventStart = eventStart;
		collateEvent.eventStartText = eventStartText;

		return collateEvents;
	}

	private void getCollateTVI(final ArrayList<TVITourBookCollateEvent> collatedEvents) {

		try {

			/*
			 * Run with a monitor because it can take a long time until all is computed.
			 */

			final IRunnableWithProgress runnable = new IRunnableWithProgress() {
				@Override
				public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

					getCollateTVI_Runnable(collatedEvents, monitor);
				}
			};

			/*
			 * Use the shell of the main app that the tooltip is not covered with the monitor,
			 * otherwise it would be centered of the active shell.
			 */
			new ProgressMonitorDialog(tourBookView.getShell()).run(true, true, runnable);

		} catch (final InvocationTargetException e) {
			StatusUtil.showStatus(e);
		} catch (final InterruptedException e) {
			StatusUtil.showStatus(e);
		}

	}

	private void getCollateTVI_Runnable(final ArrayList<TVITourBookCollateEvent> collatedEvents,
										final IProgressMonitor monitor) {

		int eventCounter = 0;
		final int eventSize = collatedEvents.size();

		monitor.beginTask(Messages.Tour_Book_Monitor_CollateTask, eventSize);

		Connection conn = null;

		final SQLFilter sqlFilter = new SQLFilter();

		final String sql = "" //$NON-NLS-1$
				//
				+ "SELECT " //						 //$NON-NLS-1$
				+ SQL_SUM_COLUMNS

				+ " FROM " + TourDatabase.TABLE_TOUR_DATA + UI.NEW_LINE //$NON-NLS-1$

				+ (" WHERE TourStartTime >= ? AND TourStartTime < ?") //$NON-NLS-1$
				+ sqlFilter.getWhereClause();

		try {

			conn = TourDatabase.getInstance().getConnection();

			final PreparedStatement statement = conn.prepareStatement(sql);
			sqlFilter.setParameters(statement, 3);

			long prevStart = 0;

			for (int eventIndex = 0; eventIndex < eventSize; eventIndex++) {

				if (monitor.isCanceled()) {
					break;
				}

				final TVITourBookCollateEvent collateEvent = collatedEvents.get(eventIndex);

				final long eventStart = eventIndex == 0 ? Long.MIN_VALUE : prevStart;
				final long eventEnd = collateEvent.eventStart.getMillis();

				prevStart = eventEnd;

				statement.setLong(1, eventStart);
				statement.setLong(2, eventEnd);

				final ResultSet result = statement.executeQuery();

				while (result.next()) {
					collateEvent.addSumColumns(result, 1);
				}

				monitor.subTask(NLS.bind(Messages.Tour_Book_Monitor_CollateSubtask, ++eventCounter, eventSize));
				monitor.worked(1);
			}

		} catch (final SQLException e) {
			SQL.showException(e, sql);
		} finally {
			Util.closeSql(conn);
		}
	}
}
