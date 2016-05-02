package org.jboss.staxmapper;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayDeque;
import java.util.Deque;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * @author Jason T. Greene
 */
public class IncludingXMLStreamReader implements XMLStreamReader {
    private Deque<XMLStreamReader> readers = new ArrayDeque<XMLStreamReader>();

    public IncludingXMLStreamReader(XMLStreamReader reader) {
        readers.push(reader);
    }

    private XMLStreamReader getReader() {
        if (readers.size() < 1) {
            throw new IllegalStateException("Stream already closed");
        }
        return readers.getFirst();
    }

    @Override
    public void close() throws XMLStreamException {
        for (XMLStreamReader reader : readers) {
            safeClose(reader);
        }
    }

    private void safeClose(XMLStreamReader c) {
        try {
            if (c != null) {
                c.close();
            }
        } catch (Throwable t) {
        }
    }

    @Override
    public int getAttributeCount() {
        return getReader().getAttributeCount();
    }

    @Override
    public String getAttributeLocalName(int index) {
        return getReader().getAttributeLocalName(index);
    }

    @Override
    public QName getAttributeName(int index) {
        return getReader().getAttributeName(index);
    }

    @Override
    public String getAttributeNamespace(int index) {
        return getReader().getAttributeNamespace(index);
    }

    @Override
    public String getAttributePrefix(int index) {
        return getReader().getAttributePrefix(index);
    }

    @Override
    public String getAttributeType(int index) {
        return getReader().getAttributeType(index);
    }

    @Override
    public String getAttributeValue(int index) {
        return getReader().getAttributeValue(index);
    }

    @Override
    public String getAttributeValue(String namespaceURI, String localName) {
        return getReader().getAttributeValue(namespaceURI, localName);
    }

    @Override
    public String getCharacterEncodingScheme() {
        return getReader().getCharacterEncodingScheme();
    }

    @Override
    public String getElementText() throws XMLStreamException {
        return getReader().getElementText();
    }

    @Override
    public String getEncoding() {
        return getReader().getEncoding();
    }

    @Override
    public int getEventType() {
        return getReader().getEventType();
    }

    @Override
    public String getLocalName() {
        return getReader().getLocalName();
    }

    @Override
    public Location getLocation() {
        return getReader().getLocation();
    }

    @Override
    public QName getName() {
        return getReader().getName();
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return getReader().getNamespaceContext();
    }

    @Override
    public int getNamespaceCount() {
        return getReader().getNamespaceCount();
    }

    @Override
    public String getNamespacePrefix(int index) {
        return getReader().getNamespacePrefix(index);
    }

    @Override
    public String getNamespaceURI() {
        return getReader().getNamespaceURI();
    }

    @Override
    public String getNamespaceURI(int index) {
        return getReader().getNamespaceURI(index);
    }

    @Override
    public String getNamespaceURI(String prefix) {
        return getReader().getNamespaceURI(prefix);
    }

    @Override
    public String getPIData() {
        return getReader().getPIData();
    }

    @Override
    public String getPITarget() {
        return getReader().getPITarget();
    }

    @Override
    public String getPrefix() {
        return getReader().getPrefix();
    }

    @Override
    public Object getProperty(String name) throws IllegalArgumentException {
        return getReader().getProperty(name);
    }

    @Override
    public String getText() {
        return getReader().getText();
    }

    @Override
    public char[] getTextCharacters() {
        return getReader().getTextCharacters();
    }

    @Override
    public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length) throws XMLStreamException {
        return getReader().getTextCharacters(sourceStart, target, targetStart, length);
    }

    @Override
    public int getTextLength() {
        return getReader().getTextLength();
    }

    @Override
    public int getTextStart() {
        return getReader().getTextStart();
    }

    @Override
    public String getVersion() {
        return getReader().getVersion();
    }

    @Override
    public boolean hasName() {
        return getReader().hasName();
    }

    @Override
    public boolean hasNext() throws XMLStreamException {
        return getReader().hasNext();
    }

    @Override
    public boolean hasText() {
        return getReader().hasText();
    }

    @Override
    public boolean isAttributeSpecified(int index) {
        return getReader().isAttributeSpecified(index);
    }

    @Override
    public boolean isCharacters() {
        return getReader().isCharacters();
    }

    @Override
    public boolean isEndElement() {
        return getReader().isEndElement();
    }

    @Override
    public boolean isStandalone() {
        return getReader().isStandalone();
    }

    @Override
    public boolean isStartElement() {
        return getReader().isStartElement();
    }

    @Override
    public boolean isWhiteSpace() {
        return getReader().isWhiteSpace();
    }

    @Override
    public int next() throws XMLStreamException {
        XMLStreamReader reader = getReader();
        int next = reader.next();
        if (next == END_DOCUMENT && readers.size() > 1) {
            readers.pop();
            return next();
        }

        return checkInclude(reader, false, next);
    }

    private int checkInclude(XMLStreamReader reader, boolean nextTag, int orig) throws XMLStreamException {
        if (reader.isStartElement() && "include".equals(reader.getLocalName()) && "http://www.w3.org/2001/XInclude".equals(reader.getNamespaceURI())) {
            String location = null;
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                if ("href".equals(reader.getAttributeLocalName(i))) {
                    location = reader.getAttributeValue(i);
                }
            }

            if (location == null) {
                throw new XMLStreamException("Include attribute did not specify an href", reader.getLocation());
            }

            while (reader.next() != END_ELEMENT) {}

            try {
                XMLStreamReader inner = XMLInputFactory.newInstance().createXMLStreamReader(new FileInputStream(location));
                int ret = nextTag ? inner.nextTag() :inner.next();
                readers.push(inner);
                return ret;
            } catch (FileNotFoundException e) {
                throw new XMLStreamException("Could not find included file: " + location, reader.getLocation());
            }
        }

        return orig;
    }

    @Override
    public int nextTag() throws XMLStreamException {
        XMLStreamReader reader = getReader();

        int next = next();
        while((next == CHARACTERS && isWhiteSpace())
                || (next == CDATA && isWhiteSpace())
                || next == SPACE
                || next == PROCESSING_INSTRUCTION
                || next == COMMENT) {
            next = next();
        }

        if (next == END_DOCUMENT && readers.size() > 1) {
            readers.pop();
            return nextTag();
        }

        if (next != START_ELEMENT && next != END_ELEMENT) {
            throw new XMLStreamException("Expected start or end tag", getLocation());
        }

        return checkInclude(reader, true, next);
    }

    @Override
    public void require(int type, String namespaceURI, String localName) throws XMLStreamException {
        getReader().require(type, namespaceURI, localName);
    }

    @Override
    public boolean standaloneSet() {
        return getReader().standaloneSet();
    }
}
