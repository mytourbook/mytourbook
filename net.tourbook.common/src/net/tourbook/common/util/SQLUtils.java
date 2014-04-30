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
package net.tourbook.common.util;

import java.sql.SQLException;

import net.tourbook.common.UI;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

/**
 * SQL utilities.
 */
public final class SQLUtils {

	public static final String	SQL_STRING_SEPARATOR	= "'";	//$NON-NLS-1$

	/**
	 * @param text
	 * @return Returns a text with all removed string separators {@value #SQL_STRING_SEPARATOR}.
	 */
	public static String getCleanString(final String text) {

		return text.replace(SQLUtils.SQL_STRING_SEPARATOR, UI.EMPTY_STRING);
	}

	/**
	 * @param string
	 * @return Returns a string with leading/trailing string separators
	 *         {@value #SQL_STRING_SEPARATOR}, this separator {@link #SQL_STRING_SEPARATOR}
	 *         <strong>must</strong> not be contained in the string which can be done with
	 *         {@link #getCleanString(String)}.
	 */
	public static String getSqlString(final String string) {
		return SQLUtils.SQL_STRING_SEPARATOR + string + SQLUtils.SQL_STRING_SEPARATOR;
	}

	public static void showSQLException(SQLException exception) {

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

}
