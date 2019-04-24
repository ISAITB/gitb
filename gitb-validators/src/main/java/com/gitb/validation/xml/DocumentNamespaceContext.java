package com.gitb.validation.xml;

import org.w3c.dom.*;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Implementation based on source found here: http://www.ibm.com/developerworks/library/x-nmspccontext/
 *
 * Created by simatosc on 09/08/2016.
 */
public class DocumentNamespaceContext implements NamespaceContext {

    public static final String DEFAULT_NS = "default";
    private Map<String, String> prefix2Uri = new HashMap<>();
    private Map<String, String> uri2Prefix = new HashMap<>();

    /**
     * This constructor parses the document and stores all namespaces it can
     * find. If toplevelOnly is true, only namespaces in the root are used.
     *
     * @param document
     *            source document
     * @param toplevelOnly
     *            restriction of the search to enhance performance
     */
    public DocumentNamespaceContext(Document document, boolean toplevelOnly) {
        examineNode(document.getFirstChild(), toplevelOnly);
    }

    /**
     * A single node is read, the namespace attributes are extracted and stored.
     *
     * @param node
     *            to examine
     * @param attributesOnly,
     *            if true no recursion happens
     */
    private void examineNode(Node node, boolean attributesOnly) {
        NamedNodeMap attributes = node.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            storeAttribute((Attr) attribute);
        }
        if (!attributesOnly) {
            NodeList chields = node.getChildNodes();
            for (int i = 0; i < chields.getLength(); i++) {
                Node chield = chields.item(i);
                if (chield.getNodeType() == Node.ELEMENT_NODE)
                    examineNode(chield, false);
            }
        }
    }

    /**
     * This method looks at an attribute and stores it, if it is a namespace
     * attribute.
     *
     * @param attribute
     *            to examine
     */
    private void storeAttribute(Attr attribute) {
        // examine the attributes in namespace xmlns
        if (attribute.getNodeName().equals(XMLConstants.XMLNS_ATTRIBUTE)) {
            putInCache(DEFAULT_NS, attribute.getNodeValue());
        }
        if (attribute.getNamespaceURI() != null && attribute.getNamespaceURI().equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {
            putInCache(attribute.getLocalName(), attribute.getNodeValue());
        } else if (attribute.getName().startsWith(XMLConstants.XMLNS_ATTRIBUTE+":")) {
            // Extract manually local name.
            int pos = attribute.getName().indexOf(XMLConstants.XMLNS_ATTRIBUTE+":");
            int len = XMLConstants.XMLNS_ATTRIBUTE.length()+1;
            if (attribute.getName().length() >= (pos + len)) {
                putInCache(attribute.getName().substring(pos+len), attribute.getNodeValue());
            }
        }
    }

    private void putInCache(String prefix, String uri) {
        prefix2Uri.put(prefix, uri);
        uri2Prefix.put(uri, prefix);
    }

    /**
     * This method is called by XPath. It returns the default namespace, if the
     * prefix is null or "".
     *
     * @param prefix
     *            to search for
     * @return uri
     */
    public String getNamespaceURI(String prefix) {
        if (prefix == null || prefix.equals(XMLConstants.DEFAULT_NS_PREFIX) || prefix.equals(DEFAULT_NS)) {
            return prefix2Uri.get(DEFAULT_NS);
        } else {
            return prefix2Uri.get(prefix);
        }
    }

    /**
     * This method is not needed in this context, but can be implemented in a
     * similar way.
     */
    public String getPrefix(String namespaceURI) {
        return uri2Prefix.get(namespaceURI);
    }

    public Iterator getPrefixes(String namespaceURI) {
        // Not implemented
        return null;
    }

}
