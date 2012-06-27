/**
 * Copyright (c) 2009-2012, Netbout.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are PROHIBITED without prior written permission from
 * the author. This product may NOT be used anywhere and on any computer
 * except the server platform of netBout Inc. located at www.netbout.com.
 * Federal copyright law prohibits unauthorized reproduction by any means
 * and imposes fines up to $25,000 for violation. If you received
 * this code occasionally and without intent to use it, please report this
 * incident to the author by email.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package com.netbout.inf.ray.imap;

import com.netbout.inf.Attribute;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Iterator;
import org.apache.commons.io.FilenameUtils;

/**
 * Catalog in a directory.
 *
 * <p>Class is thread-safe for reading and NOT thread-safe for writing
 * operations.
 *
 * @author Yegor Bugayenko (yegor@netbout.com)
 * @version $Id$
 */
final class Catalog {

    /**
     * Main file.
     */
    private final transient File file;

    /**
     * File with duplicates.
     */
    private final transient File dups;

    /**
     * Public ctor.
     * @param file The file to use
     * @throws IOException If some I/O problem inside
     */
    public Catalog(final File ctlg) throws IOException {
        this.file = ctlg;
        this.dups = new File(
            this.file.getParentFile(),
            String.format(
                "%s-dups.%s",
                FilenameUtils.getBaseName(this.file.getPath()),
                FilenameUtils.getExtension(this.file.getPath())
            )
        );
    }

    /**
     * One item.
     *
     * <p>The class is immutable and thread-safe;
     */
    public static final class Item implements Comparable<Item> {
        /**
         * Size of Item in bytes (INT + LONG).
         */
        public static final int SIZE = 4 + 8;
        /**
         * Value.
         */
        private final transient String val;
        /**
         * Position.
         */
        private final transient long pos;
        /**
         * Public ctor.
         * @param value The value
         * @param postn The position
         */
        public Item(final String value, final long postn) {
            this.val = value;
            this.pos = postn;
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public int compareTo(final Item item) {
            return new Integer(this.value().hashCode())
                .compareTo(new Integer(item.value().hashCode()));
        }
        /**
         * Get value.
         * @return The value
         */
        public String value() {
            return this.val;
        }
        /**
         * Get position.
         * @return The pos
         */
        public long position() {
            return this.pos;
        }
    }

    /**
     * Get position of numbers in data file, for the given value, or ZERO
     * if such a value is not found in catalog.
     *
     * <p>The method is thread-safe.
     *
     * @param value The value
     * @return Position in data file
     */
    public long seek(final String value) throws IOException {
        final int target = value.hashCode();
        final RandomAccessFile data = new RandomAccessFile(this.file, "r");
        long left = 0;
        long right = data.length() / Item.SIZE;
        long found = 0;
        while (left < right) {
            final long pos = left + (right - left) / 2;
            data.seek(pos * Item.SIZE);
            final int hash = data.readInt();
            if (hash == target) {
                found = data.readLong();
                break;
            }
            if (hash < target) {
                left = pos;
            } else {
                right = pos;
            }
        }
        return found;
    }

    /**
     * Create it from scratch, using the provided items.
     *
     * <p>The method is NOT thread-safe.
     *
     * @param items The items to use
     * @param position The position to register
     * @throws IOException If some I/O problem inside
     */
    public void create(final Iterator<Item> items) throws IOException {
        final OutputStream stream = new FileOutputStream(this.file);
        try {
            final DataOutputStream data = new DataOutputStream(stream);
            int previous = 0;
            while (items.hasNext()) {
                final Item item = items.next();
                final int hash = item.value().hashCode();
                if (hash < previous) {
                    throw new IllegalArgumentException("items are not ordered");
                }
                data.writeInt(hash);
                data.writeLong(item.position());
                previous = hash;
            }
            data.flush();
        } finally {
            stream.close();
        }
    }

}
