package net.sf.swtaddons.autocomplete;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;

public class AutocompleteContentProposalProvider implements IContentProposalProvider {

	/*
	 * The proposals provided.
	 */
	protected String[] proposals = null;

	/**
	 * Construct an IContentProposalProvider whose content proposals are
	 * the specified array of Objects.
	 * 
	 * @param proposals
	 *            the array of Strings to be returned whenever proposals are
	 *            requested.
	 */
	public AutocompleteContentProposalProvider(String[] proposals) {
		super();
		this.proposals = proposals;
	}

	/**
	 * Return an array of Objects representing the valid content proposals for a
	 * field. Ignore the current contents of the field.
	 * 
	 * @param contents
	 *            the current contents of the field (ignored)
	 * @param position
	 *            the current cursor position within the field (ignored)
	 * @return the array of Objects that represent valid proposals for the field
	 *         given its current content.
	 */
	public IContentProposal [] getProposals(String contents, int position) {
		List<IContentProposal> contentProposals = getMatchingProposals(this.proposals, contents);
		return (IContentProposal[]) contentProposals.toArray(new IContentProposal[contentProposals.size()]);
	}

	/**
	 * Set the Strings to be used as content proposals.
	 * 
	 * @param items
	 *            the array of Strings to be used as proposals.
	 */
	public void setProposals(String[] items) {
		this.proposals = items;
	}
	
	
	/**
	 * Return a subset List of the proposal objects that
	 * match the input string
	 * 
	 * @param proposals
	 *			the array of Strings to be used as proposals.
	 * @param contents
	 * 			the string to try and match among the propostals
	 * @return
	 * 			the proposals that match the given string
	 */
	protected List<IContentProposal> getMatchingProposals(String[] proposals, String contents) {
		List<IContentProposal> contentProposals = new ArrayList<IContentProposal>();
		String[] matchingProposals = matches(proposals, contents);
		
		for (int i=0; i<matchingProposals.length; i++) {
			final String proposal = matchingProposals[i];
			contentProposals.add(new IContentProposal() {
				public String getContent() {
					return proposal;
				}
				public String getDescription() {
					return null;
				}
				public String getLabel() {
					return null;
				}
				public int getCursorPosition() {
					return proposal.length();
				}
			});
		}
		return contentProposals;
	}

	/**
	 * Returns an array of Strings within the input array that
	 * match the input test string
	 * 
	 * @param items
	 * 			the String array of possible completions
	 * @param prefix
	 * 			the incomplete String to try and match
	 * @return
	 * 			the array of possible completions to the input string
	 */
	private String[] matches (String[] items, String prefix) {
		List<String> matches = new ArrayList<String>();
		for (int i = 0; i < items.length; ++i) {
            if (startsWithIgnoreCase(items[i], prefix)) {
                matches.add(items[i]);
            }
        }
        return (String[]) matches.toArray(new String[matches.size()]);
	}
	
	/**
     * This is a method in the style of to {@link String#startsWith(String)}
     * but does a case insensitive match.
     * 
     * @param string - string within which to search
     * @param prefix - prefix to match in the string
     * 
     * @return - boolean value of the success of a match
     */
    private boolean startsWithIgnoreCase(String string, String prefix) {
    	return string.toLowerCase().startsWith(prefix.toLowerCase());
    }
}
