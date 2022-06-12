/*
 * Copyright 2013 Hannes Janetzek
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.tourbook.map25.renderer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * The Class AssetAdapter.
 */
public abstract class AssetAdapterMT {

    /**
     * The instance provided by backend
     */
    static AssetAdapterMT g;

    public static void init(final AssetAdapterMT adapter) {
        g = adapter;
    }

    public static InputStream readFileAsStream(final String file) {
        return g.openFileAsStream(file);
    }

    public static String readTextFile(final String file) {

        final StringBuilder sb = new StringBuilder();

        final InputStream is = g.openFileAsStream(file);
        if (is == null) {
         return null;
      }

        final BufferedReader r = new BufferedReader(new InputStreamReader(is));
        String line;
        try {
            while ((line = r.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }

        return sb.toString();

    }

    /**
     * Open file from asset path as stream.
     */
    protected abstract InputStream openFileAsStream(String file);
}
