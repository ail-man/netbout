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
 * this code accidentally and without intent to use it, please report this
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
package com.netbout.inf.ray.imap.dir;

import com.jcabi.log.Logger;
import com.netbout.inf.Notice;
import com.netbout.inf.Stash;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentSkipListSet;
import javax.validation.constraints.NotNull;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Default implementation of {@link Stash}.
 *
 * <p>Class is immutable and thread-safe.
 *
 * @author Yegor Bugayenko (yegor@netbout.com)
 * @version $Id$
 */
@SuppressWarnings("PMD.TooManyMethods")
final class DefaultStash implements Stash {

    /**
     * Extension of all stash files.
     */
    private static final String EXTENSION = "nser";

    /**
     * Lock on the directory.
     */
    private final transient Lock lock;

    /**
     * Processed notices, their file names.
     */
    private final transient Collection<File> done =
        new ConcurrentSkipListSet<File>();

    /**
     * Public ctor.
     * @param path The directory where to save files
     * @throws IOException If some I/O problem inside
     */
    public DefaultStash(@NotNull final File path) throws IOException {
        this.lock = new Lock(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        try {
            return String.format(
                "%d files in %s, %d of them are done",
                this.files().size(),
                this.lock,
                this.done.size()
            );
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        this.lock.close();
        Logger.info(
            this,
            "#close(..): closed with %d done notices",
            this.done.size()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(@NotNull final Notice notice) throws IOException {
        final Notice.SerializableNotice ser =
            new Notice.SerializableNotice(notice);
        final byte[] bytes = ser.serialize();
        final File file = this.file(ser);
        FileUtils.writeByteArrayToFile(file, bytes);
        Logger.debug(
            this,
            "#add('%s'): saved %d bytes into %s",
            ser,
            bytes.length,
            FilenameUtils.getName(file.getPath())
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(@NotNull final Notice notice) throws IOException {
        final Notice.SerializableNotice ser =
            new Notice.SerializableNotice(notice);
        final File file = this.file(new Notice.SerializableNotice(notice));
        this.done.add(file);
        Logger.debug(
            this,
            "#remove('%s'): deleted %s",
            ser,
            FilenameUtils.getName(file.getPath())
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copyTo(@NotNull final Stash stash) throws IOException {
        if (!(stash instanceof DefaultStash)) {
            throw new IllegalArgumentException("can copy only to DefaultStash");
        }
        int count = 0;
        for (File file : this.files()) {
            FileUtils.copyFileToDirectory(
                file,
                DefaultStash.class.cast(stash).lock.dir()
            );
            ++count;
        }
        this.done.clear();
        Logger.info(
            this,
            "#copyTo(..): copied %d files",
            count
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Notice> iterator() {
        Collection<File> files;
        try {
            files = this.files();
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        }
        final Iterator<File> iterator = files.iterator();
        // @checkstyle AnonInnerLength (50 lines)
        return new Iterator<Notice>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }
            @Override
            public Notice next() {
                Notice notice;
                byte[] bytes;
                final File file = iterator.next();
                try {
                    bytes = FileUtils.readFileToByteArray(file);
                    notice = Notice.SerializableNotice.deserialize(bytes);
                } catch (java.io.IOException ex) {
                    throw new IllegalStateException(ex);
                }
                Logger.debug(
                    this,
                    "#next(): loaded '%s' from %s (%d bytes)",
                    new Notice.SerializableNotice(notice),
                    FilenameUtils.getName(file.getPath()),
                    bytes.length
                );
                return notice;
            }
            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Get all files in the stash.
     * @return List of files
     * @throws IOException If some IO error
     */
    private Collection<File> files() throws IOException {
        final Collection<File> files = new ConcurrentSkipListSet<File>();
        final Collection<File> all = FileUtils.listFiles(
            this.lock.dir(), new String[] {DefaultStash.EXTENSION}, true
        );
        for (File file : all) {
            if (this.done.contains(file)) {
                continue;
            }
            files.add(file);
        }
        Logger.info(
            this,
            "#files(): %d file(s) found with '%s' extension",
            files.size(),
            DefaultStash.EXTENSION
        );
        return files;
    }

    /**
     * Create file name of the notice.
     * @param notice The notice
     * @return The file name
     * @throws IOException If some error
     */
    private File file(final Notice.SerializableNotice notice)
        throws IOException {
        final String key = new StringBuffer(
            StringUtils.strip(
                new Base32(true).encodeToString(
                    notice.toString().getBytes()
                ),
                "="
            )
        ).reverse().toString();
        return new File(
            this.lock.dir(),
            String.format(
                "%s/%s/%s.%s",
                key.substring(0, 2),
                // @checkstyle MagicNumber (2 lines)
                key.substring(2, 4),
                key.substring(4, key.length() - 4),
                DefaultStash.EXTENSION
            )
        );
    }

}
