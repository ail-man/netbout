/**
 * Copyright (c) 2009-2011, NetBout.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 1) Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the following
 * disclaimer. 2) Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution. 3) Neither the name of the NetBout.com nor
 * the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.netbout.spi.client;

import com.sun.jersey.api.client.ClientResponse;
import com.ymock.util.Logger;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.UriBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Response generated by a client, on top of Jersey {@link ClientResponse}.
 *
 * @author Yegor Bugayenko (yegor@netbout.com)
 * @version $Id$
 */
final class JerseyRestResponse implements RestResponse {

    /**
     * Document builder factory, DOM.
     */
    private static final DocumentBuilderFactory DFACTORY =
        DocumentBuilderFactory.newInstance();

    /**
     * XPath factory, DOM.
     */
    private static final XPathFactory XFACTORY =
        XPathFactory.newInstance();

    /**
     * DOM transformer factory, DOM.
     */
    private static final TransformerFactory TFACTORY =
        TransformerFactory.newInstance();

    /**
     * Original resource where this response.
     */
    private final transient RestClient client;

    /**
     * The response.
     */
    private final transient ClientResponse response;

    /**
     * The DOM document from parsed output.
     */
    private transient Document doc;

    /**
     * Public ctor.
     * @param clnt The client
     * @param resp The response
     */
    public JerseyRestResponse(final RestClient clnt,
        final ClientResponse resp) {
        this.client = clnt;
        this.response = resp;
        final String error = resp.getHeaders().getFirst("Netbout-error");
        if (error != null) {
            throw new AssertionError(
                String.format(
                    "Error header detected: '%s'",
                    error
                )
            );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RestResponse assertStatus(final int code) {
        if (this.response.getStatus() != code) {
            throw new AssertionError(
                String.format(
                    "Status code %d is not equal to %d",
                    this.response.getStatus(),
                    code
                )
            );
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RestResponse assertXPath(final String xpath) {
        if (this.xpath(xpath).isEmpty()) {
            throw new AssertionError(
                String.format(
                    "Document doesn't match XPath '%s':%n%s",
                    xpath,
                    this.asText(this.document())
                )
            );
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> xpath(final String query) {
        final XPath xpath = this.XFACTORY.newXPath();
        NodeList nodes;
        try {
            nodes = (NodeList) xpath.evaluate(
                query,
                this.document(),
                XPathConstants.NODESET
            );
        } catch (javax.xml.xpath.XPathExpressionException ex) {
            throw new IllegalArgumentException(ex);
        }
        final List<String> items = new ArrayList<String>();
        for (int idx = 0; idx < nodes.getLength(); idx += 1) {
            items.add(nodes.item(idx).getNodeValue());
        }
        return items;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URI location() {
        final URI uri = this.response.getLocation();
        Logger.debug(
            this,
            "#location(): returned '%s'",
            uri
        );
        return uri;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String header(final String name) {
        final String hdr = this.response.getHeaders().getFirst(name);
        if (hdr == null) {
            throw new AssertionError(
                String.format(
                    "Header '%s' not found in [%s]",
                    name,
                    StringUtils.join(this.response.getHeaders().keySet(), ", ")
                )
            );
        }
        return hdr;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RestClient rel(final String rel) {
        String xpath = rel;
        if (xpath.charAt(0) != '/') {
            xpath = String.format("/page/links/link[@rel='%s']/@href", rel);
        }
        final URI uri = UriBuilder.fromUri(this.xpath(xpath).get(0)).build();
        Logger.debug(
            this,
            "#rel('%s'): going to '%s'",
            rel,
            uri
        );
        return this.client.copy(uri);
    }

    /**
     * Get DOM document.
     * @return The document
     */
    private Document document() {
        synchronized (this) {
            if (this.doc == null) {
                this.DFACTORY.setNamespaceAware(true);
                try {
                    this.doc = this.DFACTORY
                        .newDocumentBuilder()
                        .parse(
                            new InputSource(
                                new StringReader(
                                    this.response.getEntity(String.class)
                                )
                            )
                        );
                } catch (javax.xml.parsers.ParserConfigurationException ex) {
                    throw new IllegalStateException(ex);
                } catch (org.xml.sax.SAXException ex) {
                    throw new IllegalStateException(ex);
                } catch (java.io.IOException ex) {
                    throw new IllegalStateException(ex);
                }
            }
            return this.doc;
        }
    }

    /**
     * Convert {@link Document} to {@link String}.
     * @param document The document to transform
     * @return The string as a result
     */
    private String asText(final Document document) {
        try {
            final Transformer trans = this.TFACTORY.newTransformer();
            trans.setOutputProperty(OutputKeys.INDENT, "yes");
            final StringWriter writer = new StringWriter();
            trans.transform(new DOMSource(document), new StreamResult(writer));
            return writer.toString();
        } catch (javax.xml.transform.TransformerConfigurationException ex) {
            throw new IllegalStateException(ex);
        } catch (javax.xml.transform.TransformerException ex) {
            throw new IllegalStateException(ex);
        }
    }

}
