/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.calendar;

import net.tourbook.common.formatter.ValueFormat;
import net.tourbook.common.util.StatusUtil;

public class FormatterData implements Cloneable {

	boolean		isEnabled;

	FormatterID	id;
	ValueFormat	valueFormat;

	FormatterData(final boolean isEnabled, final FormatterID id, final ValueFormat valueFormat) {

		this.isEnabled = isEnabled;

		this.id = id;
		this.valueFormat = valueFormat;
	}

	@Override
	protected FormatterData clone() {

		FormatterData clonedData = null;

		try {

			clonedData = (FormatterData) super.clone();

		} catch (final CloneNotSupportedException e) {
			StatusUtil.log(e);
		}

		return clonedData;
	}

	@Override
	public String toString() {

		return "\n"

				+ "FormatterData ["

				+ "isEnabled=" + isEnabled + ", "
				+ "id=" + id + ", "
				+ "valueFormat=" + valueFormat

				+ "]";
	}
}
