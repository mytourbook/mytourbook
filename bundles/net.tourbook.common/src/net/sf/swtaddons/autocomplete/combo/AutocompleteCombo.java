package net.sf.swtaddons.autocomplete.combo;

import net.sf.swtaddons.autocomplete.AutocompleteWidget;

import org.eclipse.jface.fieldassist.ComboContentAdapter;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

public abstract class AutocompleteCombo extends AutocompleteWidget {
	
	private final class ProposalUpdateFocusListener implements FocusListener {
		public void focusGained(FocusEvent e) {
			provider.setProposals(combo.getItems());
		}

		public void focusLost(FocusEvent e) {
			// do nothing
		}
	}
	
	private final class ProposalUpdateResizeListener implements Listener {
		@Override
		public void handleEvent(Event event) {
			if (combo != null) {
				Point size = combo.getSize();
				size.y *= 8;
				adapter.setPopupSize(size);
			}
		}
	}

	protected Combo combo = null;
	
	public AutocompleteCombo(Combo aCombo) {
		this.combo = aCombo;
		
		if (combo != null) {
			this.combo.addFocusListener(new ProposalUpdateFocusListener());
			this.combo.addListener(SWT.Resize, new ProposalUpdateResizeListener());
			
			provider = getContentProposalProvider(combo.getItems());
			adapter = new ContentProposalAdapter(combo, new ComboContentAdapter(), provider, getActivationKeystroke(), getAutoactivationChars());
			adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
		}
	}
}
