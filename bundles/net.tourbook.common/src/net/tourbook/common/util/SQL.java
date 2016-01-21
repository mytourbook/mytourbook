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
package net.tourbook.common.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import net.tourbook.common.UI;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

/**
 * SQL utilities.
 */
public final class SQL {

	public static final String	SQL_STRING_SEPARATOR	= "'";	//$NON-NLS-1$

	public static void close(final Connection conn) {

		if (conn != null) {
			try {
				conn.close();
			} catch (final SQLException e) {
				SQL.showException(e);
			}
		}
	}

	public static void close(final ResultSet result) {

		if (result != null) {
			try {
				result.close();
			} catch (final SQLException e) {
				SQL.showException(e);
			}
		}
	}

	public static void close(final Statement stmt) {

		if (stmt != null) {
			try {
				stmt.close();
			} catch (final SQLException e) {
				SQL.showException(e);
			}
		}
	}

	/**
	 * @param text
	 * @return Returns a text with all removed string separators {@value #SQL_STRING_SEPARATOR}.
	 */
	public static String getCleanString(final String text) {

		return text.replace(SQL.SQL_STRING_SEPARATOR, UI.EMPTY_STRING);
	}

	/**
	 * @param string
	 * @return Returns a string with leading/trailing string separators
	 *         {@value #SQL_STRING_SEPARATOR}, this separator {@link #SQL_STRING_SEPARATOR}
	 *         <strong>must</strong> not be contained in the string which can be done with
	 *         {@link #getCleanString(String)}.
	 */
	public static String getSqlString(final String string) {

		return SQL.SQL_STRING_SEPARATOR + string + SQL.SQL_STRING_SEPARATOR;
	}

	public static void logParameterMetaData(final PreparedStatement statement) {

//		try {
//
//			if (statement instanceof org.apache.derby.impl.jdbc.EmbedPreparedStatement) {
//				org.apache.derby.impl.jdbc.EmbedPreparedStatement new_name = (org.apache.derby.impl.jdbc.EmbedPreparedStatement) statement;
//
//			}
//
//
//			final ParameterMetaData paraMeta = statement.getParameterMetaData();
//
//
//			for (int paraIndex = 1; paraIndex <= paraMeta.getParameterCount(); paraIndex++) {
//
//				final int type = paraMeta.getParameterType(paraIndex);
//				final String typeName = paraMeta.getParameterTypeName(paraIndex);
//
//				System.out.println((UI.timeStampNano() + " [" + "] ") + ("\t" + paraMeta));
//				// TODO remove SYSTEM.OUT.PRINTLN
//			}
//
//		} catch (final SQLException e) {
//			showException(e);
//		}
	}

	public static void showException(SQLException exception) {

		while (exception != null) {

			final String sqlExceptionText = Util.getSQLExceptionText(exception);

			System.out.println(sqlExceptionText);
			exception.printStackTrace();

			MessageDialog.openError(Display.getCurrent().getActiveShell(), //
					"SQL Error",//$NON-NLS-1$
					sqlExceptionText);

			exception = exception.getNextException();
		}
	}

	public static void showException(final SQLException exception, final String sqlStatement) {

		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {

				final String message = "SQL statement: " + UI.NEW_LINE2 // //$NON-NLS-1$
						+ sqlStatement
						+ Util.getSQLExceptionText(exception);

				MessageDialog.openError(Display.getDefault().getActiveShell(), //
						"SQL Error", //$NON-NLS-1$
						message);

				StatusUtil.log(message);
				StatusUtil.log(exception);
			}
		});
	}
}
