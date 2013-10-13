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
package com.netbout.hub;

import com.jcabi.urn.URN;
import com.jcabi.urn.URNMocker;
import org.mockito.Mockito;

/**
 * Mocker of {@link ParticipantDt}.
 * @author Yegor Bugayenko (yegor@netbout.com)
 * @version $Id$
 */
public final class ParticipantDtMocker {

    /**
     * The object.
     */
    private final transient ParticipantDt participant =
        Mockito.mock(ParticipantDt.class);

    /**
     * Public ctor.
     */
    public ParticipantDtMocker() {
        this.withIdentity(new URNMocker().mock());
        this.confirmed();
    }

    /**
     * With this identity name.
     * @param identity The name
     * @return This object
     */
    public ParticipantDtMocker withIdentity(final String identity) {
        return this.withIdentity(URN.create(identity));
    }

    /**
     * With this identity name.
     * @param name The name
     * @return This object
     */
    public ParticipantDtMocker withIdentity(final URN name) {
        Mockito.doReturn(name).when(this.participant).getIdentity();
        return this;
    }

    /**
     * Should be confirmed.
     * @return This object
     */
    public ParticipantDtMocker confirmed() {
        Mockito.doReturn(true).when(this.participant).isConfirmed();
        return this;
    }

    /**
     * Build it.
     * @return The participant data type
     */
    public ParticipantDt mock() {
        return this.participant;
    }

}