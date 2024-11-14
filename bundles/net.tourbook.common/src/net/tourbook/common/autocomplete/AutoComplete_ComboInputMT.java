/*******************************************************************************
 * Copyright (C) 2024 Wolfgang Schramm and Contributors
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
package net.tourbook.common.autocomplete;

// Original: net.sf.swtaddons.autocomplete

import org.eclipse.swt.widgets.Combo;

public class AutoComplete_ComboInputMT extends AutoComplete_ComboMT {

	public AutoComplete_ComboInputMT(final Combo combo) {
		super(combo);
	}

	@Override
   protected AutoComplete_ContentProposalProviderMT getContentProposalProvider(final String[] proposals) {

      return new AutoComplete_ContentProposalProviderMT(proposals);
	}

}
