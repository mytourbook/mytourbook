/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import net.tourbook.Messages;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.UI;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

public class ActionHandlerDbConsistencyCheck extends AbstractHandler {

	private void checkConsistency() {

		final String[] resultInfo = new String[1];
		final String[] error = new String[1];

		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {

				final StringBuilder sbSQL = new StringBuilder();

				sbSQL.append("SELECT"); //$NON-NLS-1$
				//				
				sbSQL.append(" schemaname || '.' || tablename as TableName,"); //$NON-NLS-1$
				sbSQL.append(" SYSCS_UTIL.SYSCS_CHECK_TABLE(schemaname, tablename) AS OK"); //$NON-NLS-1$
				//				
				sbSQL.append(" FROM sys.sysschemas s, sys.systables t"); //$NON-NLS-1$
				//				
				sbSQL.append(" WHERE s.schemaid = t.schemaid"); //$NON-NLS-1$
				sbSQL.append("   and t.tabletype = 'T'"); //$NON-NLS-1$

				try {

					final StringBuilder sbResult = new StringBuilder();
					sbResult.append(Messages.app_db_consistencyCheck_checkIsOK);

					final Connection conn = TourDatabase.getInstance().getConnection();
					final PreparedStatement statement = conn.prepareStatement(sbSQL.toString());

					final ResultSet result = statement.executeQuery();

					while (result.next()) {
						sbResult.append(result.getString(1));
						sbResult.append(UI.COLON_SPACE);
						sbResult.append(result.getString(2));
						sbResult.append(UI.NEW_LINE);
					}

					resultInfo[0] = sbResult.toString();

					conn.close();

				} catch (final Exception e) {
					error[0] = NLS.bind(Messages.app_db_consistencyCheck_checkFailed, e.getMessage());
				}
			}
		});

		if (error[0] == null) {

			MessageDialog.openInformation(
					Display.getCurrent().getActiveShell(),
					Messages.app_db_consistencyCheck_dlgTitle,
					resultInfo[0]);
		} else {

			MessageDialog.openError(
					Display.getCurrent().getActiveShell(),
					Messages.app_db_consistencyCheck_dlgTitle,
					error[0]);
		}
	}

	public Object execute(final ExecutionEvent event) throws ExecutionException {

		checkConsistency();

		return null;
	}

}
