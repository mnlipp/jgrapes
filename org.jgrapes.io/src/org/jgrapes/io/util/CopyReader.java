/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2018 Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.io.util;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

/**
 * A filter that copies all data read into a buffer. The buffer has to be
 * cleared regularly (see {@link #copied} else heap space will eventually
 * become exhausted. 
 */
public class CopyReader extends FilterReader {

    @SuppressWarnings("PMD.AvoidStringBufferField")
    private final StringBuilder copied = new StringBuilder();
    private int copyBufferSize = 1024 * 10;
    private boolean overflow;

    /**
     * Instantiates a new copy reader.
     *
     * @param source the source
     */
    public CopyReader(Reader source) {
        super(source);
    }

    /**
     * Sets the copy buffer size, defaults to 10240.
     *
     * @param size the size
     * @return the copy reader
     */
    @SuppressWarnings("PMD.LinguisticNaming")
    public CopyReader setCopyBufferSize(int size) {
        copyBufferSize = size;
        return this;
    }

    @Override
    public int read() throws IOException {
        char[] buf = new char[1];
        if (read(buf, 0, 1) == -1) {
            return -1;
        } else {
            return buf[0];
        }
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        int count = in.read(cbuf, off, len);
        if (count > 0 && !overflow) {
            if (copied.length() > copyBufferSize) {
                copied.append("...");
                overflow = true;
            } else {
                copied.append(cbuf, off, count);
            }
        }
        return count;
    }

    /**
     * Returns all chars and resets the copy buffer.
     *
     * @return the string
     */
    public String copied() {
        int count = copied.length();
        String res = copied.substring(0, count);
        copied.delete(0, count);
        overflow = false;
        return res;

    }
}
