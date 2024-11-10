package net.tourbook.common.autocomplete;

import org.eclipse.swt.widgets.Combo;

public class AutocompleteComboInputMT extends AutocompleteComboMT {

	public AutocompleteComboInputMT(final Combo combo) {
		super(combo);
	}

	@Override
   protected AutocompleteInputContentProposalProviderMT getContentProposalProvider(final String[] proposals) {

      return new AutocompleteInputContentProposalProviderMT(proposals);
	}

}
