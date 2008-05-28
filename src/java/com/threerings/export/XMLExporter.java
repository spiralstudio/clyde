//
// $Id$

package com.threerings.export;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

import com.threerings.util.ReflectionUtil;

import static java.util.logging.Level.*;
import static com.threerings.export.Log.*;

/**
 * Exports to an XML format.
 */
public class XMLExporter extends Exporter
{
    /** Identifies the format version. */
    public static final String VERSION = "1.0";

    /**
     * Creates an exporter to write to the specified stream.
     */
    public XMLExporter (OutputStream out)
    {
        _out = out;
    }

    @Override // documentation inherited
    public void writeObject (Object object)
        throws IOException
    {
        if (_document == null) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            try {
                DocumentBuilder builder = factory.newDocumentBuilder();
                _document = builder.newDocument();
            } catch (ParserConfigurationException e) {
                throw new IOException("Error creating XML document [error=" + e + "].");
            }
            _document.appendChild(_element = _document.createElement("java"));
            _element.setAttribute("version", VERSION);
            _element.setAttribute("class", XMLImporter.class.getName());
            appendln();
        }
        write("object", object, Object.class);
    }

    @Override // documentation inherited
    public void write (String name, boolean value)
        throws IOException
    {
        setValue(name, Boolean.toString(value));
    }

    @Override // documentation inherited
    public void write (String name, byte value)
        throws IOException
    {
        setValue(name, Byte.toString(value));
    }

    @Override // documentation inherited
    public void write (String name, char value)
        throws IOException
    {
        setValue(name, Character.toString(value));
    }

    @Override // documentation inherited
    public void write (String name, double value)
        throws IOException
    {
        setValue(name, Double.toString(value));
    }

    @Override // documentation inherited
    public void write (String name, float value)
        throws IOException
    {
        setValue(name, Float.toString(value));
    }

    @Override // documentation inherited
    public void write (String name, int value)
        throws IOException
    {
        setValue(name, Integer.toString(value));
    }

    @Override // documentation inherited
    public void write (String name, long value)
        throws IOException
    {
        setValue(name, Long.toString(value));
    }

    @Override // documentation inherited
    public void write (String name, short value)
        throws IOException
    {
        setValue(name, Short.toString(value));
    }

    @Override // documentation inherited
    public <T> void write (String name, T value, Class<T> clazz)
        throws IOException
    {
        // replace any existing value
        for (Node node = _element.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element && node.getNodeName().equals(name)) {
                // remove any children
                Element child = (Element)node;
                child.setTextContent(null);
                write(child, value, clazz);
                return;
            }
        }
        append(name, value, clazz);
    }

    @Override // documentation inherited
    public void close ()
        throws IOException
    {
        // finish up, then close the underlying stream
        finish();
        _out.close();
    }

    @Override // documentation inherited
    public void finish ()
        throws IOException
    {
        // create the output object
        DOMImplementationLS impl = (DOMImplementationLS)_document.getImplementation();
        LSOutput lsout = impl.createLSOutput();
        lsout.setByteStream(_out);

        // write out the document
        LSSerializer serializer = impl.createLSSerializer();
        serializer.write(_document, lsout);
    }

    /**
     * Appends a new element with the supplied object.
     */
    protected <T> void append (String name, T value, Class<T> clazz)
        throws IOException
    {
        appendIndent();
        Element child = _document.createElement(name);
        _element.appendChild(child);
        write(child, value, clazz);
        appendln();
    }

    /**
     * Writes an object to the specified element.
     */
    protected void write (Element element, Object value, Class clazz)
        throws IOException
    {
        if (value != null) {
            Element previous = _elements.get(value);
            if (previous != null) {
                String id = previous.getAttribute("id");
                if (id.length() == 0) {
                    previous.setAttribute("id", id = Integer.toString(++_lastObjectId));
                }
                element.setAttribute("ref", id);

            } else {
                _elements.put(value, element);
                writeValue(element, value, clazz);
            }
        }
    }

    /**
     * Writes the value of an object to the specified element.
     */
    protected void writeValue (Element element, Object value, Class clazz)
        throws IOException
    {
        // write the class unless we can determine that implicitly
        Class cclazz = getClass(value);
        if (cclazz != clazz) {
            element.setAttribute("class", cclazz.getName());
        }
        // see if we can convert the value to a string
        @SuppressWarnings("unchecked") Stringifier<Object> stringifier =
            (Stringifier<Object>)Stringifier.getStringifier(cclazz);
        if (stringifier != null) {
            // because empty text nodes are removed, we must include a comment to
            // signify an empty string
            String str = stringifier.toString(value);
            element.appendChild(str.length() == 0 ?
                _document.createComment("empty") : _document.createTextNode(str));
            return;
        }
        // write the array dimension, if applicable
        if (cclazz.isArray()) {
            element.setAttribute("length", Integer.toString(Array.getLength(value)));
        }
        String oindent = _indent;
        Element oelement = _element;
        _element = element;
        _indent = _indent + "  ";
        try {
            appendln();
            // write the outer class information, if applicable
            Object outer = ReflectionUtil.getOuter(value);
            if (outer != null) {
                @SuppressWarnings("unchecked") Class<Object> eclazz =
                    (Class<Object>)cclazz.getEnclosingClass();
                write("outer", outer, eclazz);
            }
            if (value instanceof Exportable) {
                writeFields((Exportable)value);
            } else if (value instanceof Object[]) {
                @SuppressWarnings("unchecked") Class<Object> ctype =
                    (Class<Object>)cclazz.getComponentType();
                writeEntries((Object[])value, ctype);
            } else if (value instanceof Collection) {
                writeEntries((Collection)value);
            } else if (value instanceof Map) {
                writeEntries((Map)value);
            } else {
                throw new IOException("Value is not exportable [class=" + cclazz + "].");
            }
        } finally {
            _indent = oindent;
            appendIndent();
            _element = oelement;
        }
    }

    /**
     * Writes out the entries of an array.
     */
    protected <T> void writeEntries (T[] array, Class<T> ctype)
        throws IOException
    {
        for (T entry : array) {
            append("entry", entry, ctype);
        }
    }

    /**
     * Writes out the entries of a collection.
     */
    protected void writeEntries (Collection collection)
        throws IOException
    {
        for (Object entry : collection) {
            append("entry", entry, Object.class);
        }
    }

    /**
     * Writes out the entries of a map.
     */
    protected void writeEntries (Map<?, ?> map)
        throws IOException
    {
        for (Map.Entry entry : map.entrySet()) {
            append("key", entry.getKey(), Object.class);
            append("value", entry.getValue(), Object.class);
        }
    }

    /**
     * Appends a simple value element to the current element.
     */
    protected void setValue (String name, String value)
    {
        // replace any existing value
        for (Node node = _element.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element && node.getNodeName().equals(name)) {
                // remove any children and replace with a single text node (which may be empty,
                // hence calling setTextContent with null rather than the value)
                Element child = (Element)node;
                child.setTextContent(null);
                child.appendChild(_document.createTextNode(value));
                return;
            }
        }
        appendIndent();
        Element child = _document.createElement(name);
        _element.appendChild(child);
        child.appendChild(_document.createTextNode(value));
        appendln();
    }

    /**
     * Appends the current indentation to the current element.
     */
    protected void appendIndent ()
    {
        _element.appendChild(_document.createTextNode(_indent));
    }

    /**
     * Appends a newline to the current element.
     */
    protected void appendln ()
    {
        _element.appendChild(_document.createTextNode("\n"));
    }

    /** The output stream. */
    protected OutputStream _out;

    /** The document under construction. */
    protected Document _document;

    /** The element corresponding to the current object. */
    protected Element _element;

    /** The current indentation string. */
    protected String _indent = "";

    /** Maps exported objects to their corresponding elements. */
    protected IdentityHashMap<Object, Element> _elements = new IdentityHashMap<Object, Element>();

    /** The last object id assigned. */
    protected int _lastObjectId;
}
