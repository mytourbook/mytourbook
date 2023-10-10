package net.sf.swtaddons.autocomplete.combo;

import net.sf.swtaddons.autocomplete.AutocompleteContentProposalProvider;
import net.sf.swtaddons.autocomplete.AutocompleteInputContentProposalProvider;

import org.eclipse.swt.widgets.Combo;

public class AutocompleteComboInput extends AutocompleteCombo {
	
	public AutocompleteComboInput(Combo combo) {
		super(combo);
	}

	protected AutocompleteContentProposalProvider getContentProposalProvider(String[] proposals) {
		return new AutocompleteInputContentProposalProvider(proposals);
	}

}
