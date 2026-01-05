package com.dila.dama.plugin.domain.service;

import com.dila.dama.plugin.domain.model.InvalidReferenceException;
import com.dila.dama.plugin.domain.model.TripitakaComponents;
import com.dila.dama.plugin.util.XmlDomUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ReferenceParser {

    public TripitakaComponents parseReference(String selectedXml) throws InvalidReferenceException {
        if (selectedXml == null || selectedXml.trim().isEmpty()) {
            throw new InvalidReferenceException("error.no.selection");
        }

        final Document doc;
        try {
            doc = XmlDomUtils.parseXml(selectedXml);
        } catch (Exception e) {
            throw new InvalidReferenceException("error.invalid.xml", e);
        }

        Element root = doc.getDocumentElement();
        if (root == null || !isRefElement(root)) {
            throw new InvalidReferenceException("error.not.ref.element");
        }

        String canon = getChildText(root, "canon");
        String volume = getChildText(root, "v");
        String work = getChildText(root, "w");
        String page = getChildText(root, "p");
        String column = getChildText(root, "c");
        String line = getChildText(root, "l");

        if (canon == null) {
            throw new InvalidReferenceException("error.missing.canon");
        }
        if (volume == null) {
            throw new InvalidReferenceException("error.missing.volume");
        }
        if (page == null) {
            throw new InvalidReferenceException("error.missing.page");
        }

        try {
            return new TripitakaComponents(canon, volume, work, page, column, line);
        } catch (IllegalArgumentException e) {
            // Map constructor validation back to i18n keys when possible.
            if (e.getMessage() != null && e.getMessage().contains("canon")) {
                throw new InvalidReferenceException("error.missing.canon", e);
            }
            if (e.getMessage() != null && e.getMessage().contains("volume")) {
                throw new InvalidReferenceException("error.missing.volume", e);
            }
            if (e.getMessage() != null && e.getMessage().contains("page")) {
                throw new InvalidReferenceException("error.missing.page", e);
            }
            throw new InvalidReferenceException("error.invalid.xml", e);
        }
    }

    private static boolean isRefElement(Element element) {
        String local = element.getLocalName();
        if ("ref".equals(local)) {
            return true;
        }
        String name = element.getNodeName();
        return "ref".equals(name) || name.endsWith(":ref");
    }

    private static String getChildText(Element root, String localName) {
        NodeList list = root.getElementsByTagNameNS("*", localName);
        if (list == null || list.getLength() == 0) {
            list = root.getElementsByTagName(localName);
        }
        if (list == null || list.getLength() == 0) {
            return null;
        }
        Node item = list.item(0);
        if (item == null) {
            return null;
        }
        String text = item.getTextContent();
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

