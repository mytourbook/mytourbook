package net.tourbook.search;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.suggest.Lookup.LookupResult;
import org.apache.lucene.search.suggest.analyzing.AnalyzingSuggester;
import org.apache.lucene.store.FSDirectory;

public class TESTMySuggester {
	private AnalyzingSuggester	suggr;
	int							numSugg;

	public TESTMySuggester(final String indexPath, final Set<String> suggFields, final int numSugg) throws Exception {

		initialize(indexPath, suggFields, numSugg);
	}

	public TESTMySuggester(final String indexPath, final String field, final int numSugg) throws Exception {

		final Set<String> suggFields = new HashSet<String>();
		suggFields.add(field);

		initialize(indexPath, suggFields, numSugg);
	}

	/** A quick test. */
	public static void main(final String[] args) throws Exception {

		final TESTMySuggester suggr = new TESTMySuggester("./index", "contents", 5); // suggest single field

		//MySuggester suggr = new MySuggester("./index", new HashSet<String>(), 5); // suggest all fields

		final List<LookupResult> results = suggr.lookup("s");

		for (final LookupResult res : results) {
			System.out.println(res.key + " " + res.value);
		}
	}

	/**
	 * Initialize suggester with terms from several fields. If suggFields is empty, all fields are
	 * used.
	 */
	private void initialize(final String indexPath, final Set<String> suggFields, final int numSugg) throws Exception {

		this.numSugg = numSugg;

		// Get terms from index for given field
		final IndexReader ireader = DirectoryReader.open(FSDirectory.open(new File(indexPath)));
		final TermFreqIteratorListWrapper inputIterator = new TermFreqIteratorListWrapper();

		final List<AtomicReaderContext> leaves = ireader.leaves();

		for (final AtomicReaderContext readerContext : leaves) {

			final AtomicReader reader = readerContext.reader();
			final Fields fields = reader.fields();

			for (final String field : fields) {

				if (suggFields.size() > 0 && !suggFields.contains(field)) {
					continue;
				}

				final Terms terms = fields.terms(field);
				final TermsEnum termsEnum = terms.iterator(null);

				inputIterator.add(termsEnum);
			}
		}

		// Build suggester using terms
		final Analyzer analyzer = new StandardAnalyzer(new CharArraySet(0, true));

		suggr = new AnalyzingSuggester(analyzer);
		suggr.build(inputIterator);

		ireader.close();
	}

	/** Look up terms starting with 'query' in the index. */
	public List<LookupResult> lookup(final String query) throws Exception {
		return suggr.lookup(query, false, numSugg);
	}
}
