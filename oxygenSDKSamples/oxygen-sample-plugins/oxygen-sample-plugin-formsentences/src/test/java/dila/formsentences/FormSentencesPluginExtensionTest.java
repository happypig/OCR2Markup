package dila.formsentences;

import java.net.MalformedURLException;
import java.net.URL;

import junit.framework.TestCase;
import ro.sync.exml.plugin.selection.SelectionPluginContextImpl;

/**
 * @author iulian_velea
 * @created Feb 16, 2004
 * 
 */
public class FormSentencesPluginExtensionTest extends TestCase {

  /**
   * Constructor for FormSentencesPluginExtensionTest.
   * @param arg0
   */
  public FormSentencesPluginExtensionTest(String arg0) {
    super(arg0);
  }

  /**
   * Test process.
   * 
   *@author iulian_velea
   *@throws MalformedURLException If URL is malformed.
   */
  public void testProcess() throws MalformedURLException {
    String toTest = "  this is a sample. this IS a Sample ?  ?   this is a Sample   !";
    String result = "  This is a sample. This IS a Sample ?  ?   This is a Sample   !";

    URL url = new URL("ftp://www.a.com/test/a.xml");

    SelectionPluginContextImpl pluginContext = new SelectionPluginContextImpl(toTest, null, url, null);
    FormSentencesPluginExtension obj = new FormSentencesPluginExtension();

    assertTrue(
        " Should be equal : " + obj.process(pluginContext).getProcessedSelection(),
        result.equals(obj.process(pluginContext).getProcessedSelection()));
    assertEquals(url, pluginContext.getDocumentURL());

    toTest = "tHis i?\n" + 
    "s  <> # & ^   !\n" + 
    "    ! aaa A sample.";
    result = "THis i?\n" + 
    "S  <> # & ^   !\n" + 
    "    ! Aaa A sample.";
    pluginContext = new SelectionPluginContextImpl(toTest, null, url, null);

    assertTrue(
        " Should be equal : " + obj.process(pluginContext).getProcessedSelection(),
        result.equals(obj.process(pluginContext).getProcessedSelection()));
    assertEquals(url, pluginContext.getDocumentURL());
  }

}