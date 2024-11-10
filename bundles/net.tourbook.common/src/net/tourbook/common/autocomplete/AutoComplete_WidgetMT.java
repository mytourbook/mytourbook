package net.tourbook.common.autocomplete;

import org.eclipse.jface.fieldassist.ContentProposalAdapter;

public abstract class AutoComplete_WidgetMT {

   protected AutoComplete_ContentProposalProviderMT provider;
   protected ContentProposalAdapter                 adapter;

   protected abstract AutoComplete_ContentProposalProviderMT getContentProposalProvider(String[] proposals);

}
