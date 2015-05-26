/**
 * Copyright (c) 2009-2015, netbout.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are PROHIBITED without prior written permission from
 * the author. This product may NOT be used anywhere and on any computer
 * except the server platform of netbout Inc. located at www.netbout.com.
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
package com.netbout.rest;

import com.jcabi.urn.URN;
import com.netbout.mock.MkBase;
import java.net.HttpURLConnection;
import java.util.regex.Pattern;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;
import org.takes.Take;
import org.takes.facets.hamcrest.HmRsStatus;
import org.takes.rq.RqFake;
import org.takes.rq.RqMethod;
import org.takes.rq.RqWithHeader;
import org.takes.rs.RsPrint;

/**
 * Test case for {@link TkApp}.
 * @author Yegor Bugayenko (yegor@teamed.io)
 * @version $Id$
 * @since 2.14
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class TkAppTest {

    /**
     * TkApp can render front page.
     * @throws Exception If there is some problem inside
     */
    @Test
    public void rendersFrontPage() throws Exception {
        MatcherAssert.assertThat(
            new TkApp(new MkBase()).act(new RqFake(RqMethod.GET, "/")),
            new HmRsStatus(
                Matchers.equalTo(HttpURLConnection.HTTP_OK)
            )
        );
    }

    /**
     * TkApp can render front page.
     * @throws Exception If there is some problem inside
     */
    @Test
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public void rendersStaticResources() throws Exception {
        final String[] pages = {
            "/robots.txt",
            "/css/style.css",
            "/js/bout.js",
            "/xsl/login.xsl",
            "/xsl/login-layout.xsl",
            "/xsl/bout.xsl",
            "/lang/en.xml",
            "/favicon.ico",
        };
        final Take app = new TkApp(new MkBase());
        for (final String page : pages) {
            MatcherAssert.assertThat(
                page,
                app.act(new RqFake(RqMethod.GET, page)),
                new HmRsStatus(
                    Matchers.equalTo(HttpURLConnection.HTTP_OK)
                )
            );
        }
    }

    /**
     * TkApp can redirect unauthenticated users to login page
     * and store return location in RsReturn cookie.
     * @todo #609:30min Currently there is no way to send unauthorized
     *  request from a test, as there is a PsFake(TkAppAuth.TESTING)
     *  in TkAppAuth, hence all requests are authorized.
     * @throws Exception If there is some problem inside
     */
    @Test
    @Ignore
    public void redirectsToLoginAndSetsCookie() throws Exception {
        final String head = new RsPrint(
            new TkApp(new MkBase()).act(new RqFake(RqMethod.GET, "/whatever"))
        ).printHead();
        MatcherAssert.assertThat(
            // @checkstyle MultipleStringLiteralsCheck (2 lines)
            "Incorrect status code",
            Integer.parseInt(head.split("\\s")[1]),
            Matchers.equalTo(HttpURLConnection.HTTP_SEE_OTHER)
        );
        MatcherAssert.assertThat(
            // @checkstyle MultipleStringLiteralsCheck (1 line)
            "Incorrect Location header",
            Pattern.compile(
                "^Location: /$",
                Pattern.MULTILINE
            ).matcher(head).find()
        );
        MatcherAssert.assertThat(
            // @checkstyle MultipleStringLiteralsCheck (1 line)
            "Incorrect Set-Cookie header",
            Pattern.compile(
                "^Set-Cookie: RsReturn=.*%2Fwhatever;Expires=.*$",
                Pattern.MULTILINE
            ).matcher(head).find()
        );
    }

    /**
     * TkApp can redirect authenticated users from home location
     * to the location of RsReturn cookie.
     * @throws Exception If there is some problem inside
     */
    @Test
    public void redirectsToReturnCookie() throws Exception {
        final String head = new RsPrint(
            new TkApp(new MkBase()).act(
                new RqWithHeader(
                    new RqWithTester(new URN("urn:test:1")),
                    "Cookie: RsReturn=http://example.com/whatever"
                )
            )
        ).printHead();
        MatcherAssert.assertThat(
            // @checkstyle MultipleStringLiteralsCheck (3 lines)
            "Incorrect status code",
            Integer.parseInt(head.split("\\s")[1]),
            Matchers.equalTo(HttpURLConnection.HTTP_SEE_OTHER)
        );
        MatcherAssert.assertThat(
            // @checkstyle MultipleStringLiteralsCheck (1 line)
            "Incorrect Location header",
            Pattern.compile(
                "^Location: http://example.com/whatever$",
                Pattern.MULTILINE
            ).matcher(head).find()
        );
        MatcherAssert.assertThat(
            // @checkstyle MultipleStringLiteralsCheck (1 line)
            "Incorrect Set-Cookie header",
            Pattern.compile(
                "^Set-Cookie: RsReturn=$",
                Pattern.MULTILINE
            ).matcher(head).find()
        );
    }
}
