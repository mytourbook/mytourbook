package net.tourbook.common.autocomplete;

public class AutocompleteInputContentProposalProviderMT extends AutocompleteContentProposalProviderMT {

	/**
	 * Construct a ContentProposalProvider whose content proposals are
	 * the specified array of Objects.  This ContentProposalProvider will
	 * SUGGEST a completion for the input but will not force the input
	 * to be one of the proposals
	 *
	 * @param proposals
	 *            the array of Strings to be returned whenever proposals are
	 *            requested.
	 */
	public AutocompleteInputContentProposalProviderMT(final String[] proposals) {
		super(proposals);
	}

}
