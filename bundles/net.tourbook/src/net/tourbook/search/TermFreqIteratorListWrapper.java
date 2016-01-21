/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.tourbook.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.apache.lucene.search.spell.TermFreqIterator;
import org.apache.lucene.search.suggest.InputIterator;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefIterator;

/**
 * Wraps a list of BytesRefIterator's as a TermFreqIterator, with all weights
 * set to <code>1</code>. The resulting iterator can be viewed as a
 * concatenation of the individual iterators in the order in which they were
 * added.
 */
public class TermFreqIteratorListWrapper implements TermFreqIterator, InputIterator {

	/** List of iterators being wrapped. */
	private List<BytesRefIterator> iters;

	/** Index of the current iterator (to be used for a next() call. */
	private int curr;

    /**
     * Creates a new empty wrapper, to which iterators can be added. For each
     * iterator, a weight value of <code>1</code> is assigned for all terms.
     */
	public TermFreqIteratorListWrapper() {

		curr = -1;
		iters = new ArrayList<BytesRefIterator>();
	}

	/**
	 * Adds an iterator to the list.
	 */
	public void add(final BytesRefIterator wrapped) {

		iters.add(wrapped);

		if (curr == -1) {
			curr = 0;
		}
	}

	@Override
	public Set<BytesRef> contexts() {
		return null;
	}

	@Override
	public Comparator<BytesRef> getComparator() {

		// Assuming that all iterators in the list use the same Comparator.
		if (curr != -1) {
			return iters.get(0).getComparator();
		} else {
			return null;
		}
	}

	@Override
	public boolean hasContexts() {
		return false;
	}

	@Override
	public boolean hasPayloads() {
		return false;
	}

	@Override
	public BytesRef next() throws IOException {

		BytesRef term;

		for (; curr < iters.size(); curr++) {
			term = iters.get(curr).next();
			if (term != null) {
				return term;
			}
		}

		return null;
	}

	@Override
	public BytesRef payload() {
		return null;
	}

	@Override
	public long weight() {
		return 1;
	}
}
