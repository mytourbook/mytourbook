package net.tourbook.common.autocomplete;

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
