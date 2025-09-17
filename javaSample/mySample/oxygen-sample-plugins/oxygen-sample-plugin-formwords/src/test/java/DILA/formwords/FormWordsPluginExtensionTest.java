package DILA.formwords;

import java.net.MalformedURLException;
import java.net.URL;

import junit.framework.TestCase;
import ro.sync.exml.plugin.selection.SelectionPluginContextImpl;

/**
 * @author iulian_velea
 * @created Feb 16, 2004
 * 
 */
public class FormWordsPluginExtensionTest extends TestCase {

  /**
   * Constructor for FormWordsPluginExtensionTest.
   * @param arg0
   */
  public FormWordsPluginExtensionTest(String arg0) {
    super(arg0);
  }

  /**
   * Test process method. 
   * 
   *@author iulian_velea
   *@throws MalformedURLException If URL is malformed.
   */
  public void testProcess() throws MalformedURLException {
    String toTest = "  this is a sample.    this is a Sample   !";
    String result = "  This Is A Sample.    This Is A Sample   !";

    URL url = new URL("ftp://www.a.com/test/a.xml");

    SelectionPluginContextImpl pluginContext = new SelectionPluginContextImpl(toTest, null, url, null);
    FormWordsPluginExtension obj = new FormWordsPluginExtension();

    assertTrue(
      " Should be equal : " + obj.process(pluginContext).getProcessedSelection(),
      result.equals(obj.process(pluginContext).getProcessedSelection()));
    assertEquals(url, pluginContext.getDocumentURL());

    toTest = "tHis i\n" + 
             "s  <> # & ^   \n" + 
             "    ! aaa A sample.";
    result = "THis I\n" + 
             "S  <> # & ^   \n" + 
             "    ! Aaa A Sample.";
    pluginContext = new SelectionPluginContextImpl(toTest, null, url, null);
    assertEquals(url, pluginContext.getDocumentURL());

    assertTrue(
      " Should be equal : " + obj.process(pluginContext).getProcessedSelection(),
      result.equals(obj.process(pluginContext).getProcessedSelection()));
    
    toTest = "a\na";
    result = "A\nA";
    
    pluginContext = new SelectionPluginContextImpl(toTest, null, url, null);

    assertTrue(
        " Should be equal : " + obj.process(pluginContext).getProcessedSelection(),
        result.equals(obj.process(pluginContext).getProcessedSelection()));
    assertEquals(url, pluginContext.getDocumentURL());
  }
}