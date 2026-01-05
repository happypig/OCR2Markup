package com.dila.dama.plugin.domain.service;

import com.dila.dama.plugin.util.XmlDomUtils;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static org.assertj.core.api.Assertions.assertThat;

public class RefElementRewriterTest {

    private final RefElementRewriter rewriter = new RefElementRewriter();

    @Test
    public void rewrite_removesPtrElementsAndSetsCheckedAndHref() throws Exception {
        String xml = "<ref xml:id=\"ref001\" checked=\"0\">"
            + "<canon>T</canon>"
            + "<v>25</v>"
            + "<p>917</p>"
            + "<ptr href=\"https://old.example/1\"/>"
            + "<ptr href=\"https://old.example/2\"/>"
            + "</ref>";

        String url = "https://cbetaonline.dila.edu.tw/T1514";
        String rewritten = rewriter.rewrite(xml, url);

        Document doc = XmlDomUtils.parseXml(rewritten);
        Element root = doc.getDocumentElement();

        assertThat(root.getAttribute("checked")).isEqualTo("2");
        assertThat(root.getElementsByTagName("canon").item(0).getTextContent()).isEqualTo("T");

        NodeList ptrs = root.getElementsByTagName("ptr");
        assertThat(ptrs.getLength()).isEqualTo(1);
        assertThat(((Element) ptrs.item(0)).getAttribute("href")).isEqualTo(url);
        Node firstChild = root.getFirstChild();
        assertThat(firstChild).isInstanceOf(Element.class);
        assertThat(((Element) firstChild).getNodeName()).isEqualTo("ptr");
    }
}
