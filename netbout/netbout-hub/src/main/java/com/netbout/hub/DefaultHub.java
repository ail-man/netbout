/**
 * Copyright (c) 2009-2011, netBout.com
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
package com.netbout.hub;

import com.netbout.bus.Bus;
import com.netbout.spi.Helper;
import com.netbout.spi.Identity;
import com.ymock.util.Logger;
import java.net.URL;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentSkipListSet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Entry point to Hub.
 *
 * @author Yegor Bugayenko (yegor@netbout.com)
 * @version $Id$
 */
public final class DefaultHub implements Hub {

    /**
     * The bus.
     */
    private final transient Bus bus;

    /**
     * All identities known for us at the moment.
     */
    private final transient NavigableSet all =
        new ConcurrentSkipListSet<Identity>();

    /**
     * Manager of bouts.
     */
    private final transient BoutMgr manager;

    /**
     * Identity finder.
     */
    private final transient IdentityFinder finder;

    /**
     * Public ctor.
     * @param ibus The bus
     */
    public DefaultHub(final Bus ibus) {
        this.bus = ibus;
        this.manager = new DefaultBoutMgr(this.bus);
        this.finder = new DefaultIdentityFinder(
            this, this.bus, this.all
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Identity identity(final Urn name) {
        final DefaultHub.Token token = new DefaultHub.Token(name);
        Identity identity;
        if (this.all.containsKey(token)) {
            identity = (Identity) this.all.floor(token);
        } else {
            identity = new HubIdentity(
                this.bus,
                this,
                this.manager,
                name
            );
            this.save(name, identity);
            Logger.debug(
                this,
                "#identity('%s'): created new (%d total)",
                name,
                this.all.size()
            );
        }
        return identity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element stats(final Document doc) {
        final Element root = doc.createElement("hub");
        final Element identities = doc.createElement("identities");
        root.appendChild(identities);
        for (String name : this.all.keySet()) {
            final Element identity = doc.createElement("identity");
            identities.appendChild(identity);
            identity.appendChild(doc.createTextNode(name));
        }
        root.appendChild(this.manager.stats(doc));
        return root;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void promote(final Identity identity, final Helper helper) {
        this.bus.register(helper);
        final Identity existing = this.identity(identity.name());
        this.all.remove(existing);
        this.save(helper);
        Logger.info(
            this,
            "#promote('%s', '%s'): replaced existing identity (%s)",
            identity.name(),
            helper.getClass().getName(),
            existing.getClass().getName()
        );
        this.bus.make("identity-promoted")
            .synchronously()
            .arg(identity.name())
            .arg(helper.location())
            .asDefault(true)
            .exec();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Identity> findByKeyword(final String keyword) {
        final Set<Identity> found = new HashSet<Identity>();
        final List<Urn> names = this.bus
            .make("find-identities-by-keyword")
            .synchronously()
            .arg(keyword)
            .asDefault(new ArrayList<Urn>())
            .exec();
        for (Urn name : names) {
            try {
                found.add(this.identity(name));
            } catch (com.netbout.spi.UnreachableIdentityException ex) {
                Logger.warn(
                    this,
                    // @checkstyle LineLength (1 line)
                    "#findByKeyword('%s'): some helper returned '%s' identity that is not reachable",
                    keyword,
                    name
                );
            }
        }
        return found;
    }

    /**
     * Save identity to storage.
     * @param identity The identity
     */
    private void save(final Identity identity) {
        this.all.add(identity);
        this.bus.make("identity-mentioned")
            .synchronously()
            .arg(identity.name())
            .asDefault(true)
            .exec();
    }

    /**
     * Token for searching of identities in storage.
     */
    private static final class Token {
        /**
         * Name of identity.
         */
        private final transient name;
        /**
         * Public ctor.
         * @param urn The name of identity
         */
        public Token(final Urn urn) {
            this.name = urn;
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object obj) {
            return obj.hashCode() == this.hashCode();
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return this.name.hashCode();
        }
    }

}
