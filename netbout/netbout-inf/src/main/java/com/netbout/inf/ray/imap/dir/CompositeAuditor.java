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
import java.io.IOException;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang.StringUtils;

/**
 * Composite auditor of a baseline.
 *
 * <p>Class is immutable and thread-safe.
 *
 * @author Yegor Bugayenko (yegor@netbout.com)
 * @version $Id$
 */
final class CompositeAuditor implements Auditor {

    /**
     * {@inheritDoc}
     */
    @Override
    public void audit(@NotNull final Baseline base,
        @NotNull final Audit audit) throws IOException {
        final long start = System.currentTimeMillis();
        final Auditor[] auditors = new Auditor[] {
            new NumbersAuditor(),
            new ReversiveAuditor(),
        };
        if (StringUtils.equals(System.getProperty("netbout.prof"), "true")) {
            Logger.warn(
                this,
                "#audit('%s'): skipped because of 'netbout.prof' system var",
                base
            );
        } else {
            for (Auditor auditor : auditors) {
                auditor.audit(base, audit);
            }
            Logger.debug(
                this,
                "#audit('%s', ..): done in %[ms]s",
                base,
                System.currentTimeMillis() - start
            );
        }
    }

}
