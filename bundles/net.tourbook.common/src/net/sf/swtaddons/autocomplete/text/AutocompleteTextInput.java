package net.sf.swtaddons.autocomplete.text;

import net.sf.swtaddons.autocomplete.AutocompleteContentProposalProvider;
import net.sf.swtaddons.autocomplete.AutocompleteInputContentProposalProvider;

import org.eclipse.swt.widgets.Text;

public class AutocompleteTextInput extends AutocompleteText {
	
	public AutocompleteTextInput(Text text, String[] selectionItems) {
		super(text, selectionItems);
	}

	protected AutocompleteContentProposalProvider getContentProposalProvider(String[] proposals) {
		return new AutocompleteInputContentProposalProvider(proposals);
	}

}
