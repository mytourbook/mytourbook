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
package net.tourbook.ui.action;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import net.tourbook.Messages;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.database.TourDatabase;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

public class ActionHandlerDb_CompressDB extends AbstractHandler {

	private void checkConsistency() {

		final String[] resultInfo = new String[1];
		final String[] error = new String[1];

		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {

			private void logTableSize(final Connection conn) throws SQLException {

				String sqlSize = ""
						+ "SELECT "

						+ " TableName,"

						+ " (SELECT"
						+ "    SUM(NumAllocatedPages * PageSize)"
						+ "    FROM new org.apache.derby.diag.SpaceTable('APP', t.TableName) x) AS Size"

						+ " FROM SYS.SYSTABLES t"

				//	+ " ORDER BY Size DESC"
				;

				sqlSize = "select"

						+ " tablename,"

						+ " (select sum(numallocatedpages*pagesize)"
						+ "   from new org.apache.derby.diag.SpaceTable('APP',t.tablename) x),"

						+ " (select sum(estimspacesaving) "
						+ "   from new org.apache.derby.diag.SpaceTable('APP',t.tablename) x)"

						+ " from SYS.SYSTABLES t";

				sqlSize = ""
						+ "select"
						+ " v.* from SYS.SYSSCHEMAS s,"
						+ " SYS.SYSTABLES t,"
						+ " new org.apache.derby.diag.SpaceTable(SCHEMANAME,TABLENAME) v"
						+ " where s.SCHEMAID = t.SCHEMAID";

				sqlSize = ""

						+ "    select v.*"

						+ "    from SYS.SYSSCHEMAS s,"
						+ "         SYS.SYSTABLES t,"
						+ "         TABLE(SYSCS_DIAG.SPACE_TABLE(SCHEMANAME, TABLENAME)) v"

						+ "    where s.SCHEMAID = t.SCHEMAID"

//						+ " ORDER BY v"

				;

				sqlSize = ""
						+ "SELECT"
						+ " v.*,"
						+ " ((NUMALLOCATEDPAGES + NUMFREEPAGES) * PAGESIZE) as USEDSPACE,"
						+ " s.*"
						+ "   from SYS.SYSSCHEMAS s, SYS.SYSTABLES t, new org.apache.derby.diag.SpaceTable(SCHEMANAME,TABLENAME) v"

						+ " where s.SCHEMAID = t.SCHEMAID"
						+ " order by USEDSPACE desc";

				final Statement stmtSize = conn.createStatement();
				final ResultSet resultSetSize = stmtSize.executeQuery(sqlSize);

				while (resultSetSize.next()) {

					System.out.println(String.format("%-35s %10d %10d",

							resultSetSize.getString(1),
							resultSetSize.getInt(2),
							resultSetSize.getInt(3),
							resultSetSize.getInt(4)

					));
				}
			}

			@Override
			public void run() {

				Connection conn = null;
				try {

					final StringBuilder sbResult = new StringBuilder();
					sbResult.append(Messages.app_db_consistencyCheck_checkIsOK);

					conn = TourDatabase.getInstance().getConnection();

					// log size BEFORE compression
					logTableSize(conn);
//
//					/*
//					 * Get all table name,schema
//					 */
//					final String sql = ""
//							+ "SELECT"
//							+ " SCHEMANAME, TABLENAME"
//							+ " FROM Sys.SysSchemas s, Sys.SysTables t"
//							+ " WHERE s.SchemaId = t.SchemaId AND t.TableType = 'T'";
//
//					final Statement stmt = conn.createStatement();
//					final ResultSet resultSet = stmt.executeQuery(sql);
//
//					/*
//					 * Compress all tables
//					 */
//					final CallableStatement cs = conn.prepareCall("CALL SYSCS_UTIL.SYSCS_COMPRESS_TABLE(?, ?, ?)");
//
//					// loop all tables
//					while (resultSet.next()) {
//
//						final String schema = resultSet.getString(1);
//						final String table = resultSet.getString(2);
//
//						System.out.println("Now compressing " + schema + " " + table);
//
//						// compress
//						cs.setString(1, schema);
//						cs.setString(2, table);
//						cs.setShort(3, (short) 1);
//
//						cs.execute();
//
//					}
//
//					/*
//					 * Get size AFTER compression
//					 */
//					// log size BEFORE compression
//					logTableSize(conn);

				} catch (final Exception e) {

					StatusUtil.log(e);

					String message = e.getMessage();

					final int maxLength = 3000;
					if (message.length() > maxLength) {
						message = message.substring(0, maxLength) + "\n...\n...\n..."; //$NON-NLS-1$
					}

					error[0] = NLS.bind(Messages.app_db_consistencyCheck_checkFailed, message);

				} finally {

					if (conn != null) {

						try {
							conn.close();
						} catch (final SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		});

		if (error[0] == null) {

//			MessageDialog.openInformation(
//					Display.getCurrent().getActiveShell(),
//					Messages.app_db_consistencyCheck_dlgTitle,
//					resultInfo[0]);
		} else {

			MessageDialog.openError(
					Display.getCurrent().getActiveShell(),
					Messages.app_db_consistencyCheck_dlgTitle,
					error[0]);
		}
	}

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {

		checkConsistency();

		return null;
	}

}
