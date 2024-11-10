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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;

public class AutoComplete_ContentProposalProviderMT implements IContentProposalProvider {

   /*
    * The proposals provided.
    */
   private String[] _allProposals;

   /**
    * Construct an IContentProposalProvider whose content proposals are
    * the specified array of Objects.
    *
    * @param proposals
    *           the array of Strings to be returned whenever proposals are
    *           requested.
    */
   public AutoComplete_ContentProposalProviderMT(final String[] proposals) {

      super();

      _allProposals = proposals;
   }

   /**
    * Return a subset List of the proposal objects that
    * match the input string
    *
    * @param allProposals
    *           the array of Strings to be used as proposals.
    * @param searchText
    *           the string to try and match among the propostals
    *
    * @return
    *         the proposals that match the given string
    */
   protected List<IContentProposal> getMatchingProposals(final String[] allProposals, final String searchText) {

      final List<IContentProposal> allContentProposals = new ArrayList<>();
      final String[] matchingProposals = matches(allProposals, searchText);

      for (final String proposal : matchingProposals) {

         allContentProposals.add(new IContentProposal() {

            @Override
            public String getContent() {
               return proposal;
            }

            @Override
            public int getCursorPosition() {
               return proposal.length();
            }

            @Override
            public String getDescription() {
               return null;
            }

            @Override
            public String getLabel() {
               return null;
            }
         });
      }

      return allContentProposals;
   }

   /**
    * Return an array of Objects representing the valid content proposals for a
    * field. Ignore the current contents of the field.
    *
    * @param contents
    *           the current contents of the field (ignored)
    * @param position
    *           the current cursor position within the field (ignored)
    *
    * @return the array of Objects that represent valid proposals for the field
    *         given its current content.
    */
   @Override
   public IContentProposal[] getProposals(final String contents, final int position) {

      final List<IContentProposal> allContentProposals = getMatchingProposals(_allProposals, contents);

      return allContentProposals.toArray(new IContentProposal[allContentProposals.size()]);
   }

   /**
    * Returns an array of Strings within the input array that
    * match the input test string
    *
    * @param allTexts
    *           the String array of possible completions
    * @param searchText
    *           the incomplete String to try and match
    *
    * @return
    *         the array of possible completions to the input string
    */
   private String[] matches(final String[] allTexts, final String searchText) {

      final TextMatcher textMatcher = new TextMatcher(searchText.trim(), true, false);

      final List<String> allMatches = new ArrayList<>();

      for (final String text : allTexts) {

         if (textMatcher.match(text)) {

            allMatches.add(text);
         }
      }

      return allMatches.toArray(new String[allMatches.size()]);
   }

   /**
    * Set the Strings to be used as content proposals.
    *
    * @param items
    *           the array of Strings to be used as proposals.
    */
   public void setProposals(final String[] items) {

      _allProposals = items;
   }

}
