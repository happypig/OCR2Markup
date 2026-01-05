package com.dila.dama.plugin.domain.service;

import com.dila.dama.plugin.domain.model.InvalidReferenceException;
import com.dila.dama.plugin.util.XmlDomUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

public class RefElementRewriter {

    public String rewrite(String refXml, String newUrl) throws InvalidReferenceException {
        if (refXml == null || refXml.trim().isEmpty()) {
            throw new InvalidReferenceException("error.no.selection");
        }
        if (newUrl == null || newUrl.trim().isEmpty()) {
            throw new InvalidReferenceException("error.no.results");
        }

        final Document doc;
        try {
            doc = XmlDomUtils.parseXml(refXml);
        } catch (Exception e) {
            throw new InvalidReferenceException("error.invalid.xml", e);
        }

        Element root = doc.getDocumentElement();
        if (root == null || !isRefElement(root)) {
            throw new InvalidReferenceException("error.not.ref.element");
        }

        removePtrElements(root);
        root.setAttribute("checked", "2");
        Element ptr = createPtrElement(doc, root, newUrl.trim());
        Node firstChild = root.getFirstChild();
        if (firstChild != null) {
            root.insertBefore(ptr, firstChild);
        } else {
            root.appendChild(ptr);
        }

        try {
            return XmlDomUtils.toXmlString(root);
        } catch (Exception e) {
            throw new InvalidReferenceException("error.invalid.xml", e);
        }
    }

    private static void removePtrElements(Element root) {
        List<Node> ptrNodes = new ArrayList<>();
        NodeList list = root.getElementsByTagNameNS("*", "ptr");
        if (list == null || list.getLength() == 0) {
            list = root.getElementsByTagName("ptr");
        }
        if (list != null) {
            for (int i = 0; i < list.getLength(); i++) {
                Node node = list.item(i);
                if (node != null) {
                    ptrNodes.add(node);
                }
            }
        }
        for (Node node : ptrNodes) {
            if (node.getParentNode() != null) {
                node.getParentNode().removeChild(node);
            }
        }
    }

    private static Element createPtrElement(Document doc, Element root, String url) {
        String namespace = root.getNamespaceURI();
        String prefix = root.getPrefix();
        Element ptr;
        if (namespace != null && !namespace.isEmpty()) {
            String qualified = (prefix != null && !prefix.isEmpty()) ? prefix + ":ptr" : "ptr";
            ptr = doc.createElementNS(namespace, qualified);
        } else {
            ptr = doc.createElement("ptr");
        }
        ptr.setAttribute("href", url);
        return ptr;
    }

    private static boolean isRefElement(Element element) {
        String local = element.getLocalName();
        if ("ref".equals(local)) {
            return true;
        }
        String name = element.getNodeName();
        return "ref".equals(name) || name.endsWith(":ref");
    }
}
