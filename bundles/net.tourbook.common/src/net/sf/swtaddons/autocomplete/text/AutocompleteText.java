package net.sf.swtaddons.autocomplete.text;

import net.sf.swtaddons.autocomplete.AutocompleteWidget;

import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.widgets.Text;

public abstract class AutocompleteText extends AutocompleteWidget {
	
	protected Text text = null;
	
	public AutocompleteText(Text text, String[] selectionItems) {
		if (text != null) {
			this.text = text;
			provider = getContentProposalProvider(selectionItems);
			adapter = new ContentProposalAdapter(text, new TextContentAdapter(), provider, getActivationKeystroke(), getAutoactivationChars());
			adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
		}
	}

}
