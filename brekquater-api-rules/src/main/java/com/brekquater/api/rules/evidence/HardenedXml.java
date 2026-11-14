// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.evidence;

import java.io.InputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Streams the elements of an XML report with DTDs and external entities turned off. JaCoCo reports declare a
 * DOCTYPE, so a reader that honoured it could be made to fetch URLs or read local files (XXE).
 */
final class HardenedXml {

    private HardenedXml() {}

    /** Calls {@code handler} for every start and end tag of the document, in order. */
    static void walk(InputStream input, ElementHandler handler) {
        try {
            XMLStreamReader xml = factory().createXMLStreamReader(input);
            try {
                walk(xml, handler);
            } finally {
                xml.close();
            }
        } catch (XMLStreamException exception) {
            throw new UnreadableReportException("The file is not well-formed XML.", exception);
        }
    }

    private static void walk(XMLStreamReader xml, ElementHandler handler) throws XMLStreamException {
        while (xml.hasNext()) {
            int event = xml.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                handler.start(xml);
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                handler.end(xml.getLocalName());
            }
        }
    }

    // The JDK's own implementation is requested, so a StAX library on the classpath cannot weaken these settings.
    private static XMLInputFactory factory() {
        XMLInputFactory factory = XMLInputFactory.newDefaultFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        return factory;
    }

    /** Receives the tags of a document; the reader is positioned on the start tag, so attributes can be read. */
    interface ElementHandler {

        void start(XMLStreamReader element);

        void end(String name);
    }
}
