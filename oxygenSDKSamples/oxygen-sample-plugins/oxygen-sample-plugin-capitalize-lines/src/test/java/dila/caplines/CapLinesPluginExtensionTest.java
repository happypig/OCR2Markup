package dila.caplines;

import java.net.MalformedURLException;
import java.net.URL;

import junit.framework.TestCase;
import ro.sync.exml.plugin.selection.SelectionPluginContextImpl;

/**
 * @author iulian_velea
 * @created Feb 16, 2004
 * 
 */
public class CapLinesPluginExtensionTest extends TestCase {

  /**
   * Constructor for CapLinesPluginExtensionTest.
   * @param arg0
   */
  public CapLinesPluginExtensionTest(String arg0) {
    super(arg0);
  }

  /**
   * TC for process method.
   * 
   *@author iulian_velea
   *@throws MalformedURLException If URL is malformed.
   */
  public void testProcess() throws MalformedURLException {
    String toTest = "  this is a sample.\n this IS a Sample ?  \n ?   this is a Sample   !";
    String result = "  This is a sample.\n This IS a Sample ?  \n ?   this is a Sample   !";

    URL url = new URL("ftp://www.a.com/test/a.xml");

    SelectionPluginContextImpl pluginContext = new SelectionPluginContextImpl(toTest, null, url, null);
    CapLinesPluginExtension obj = new CapLinesPluginExtension();

    assertTrue(
        " Should be equal : " + obj.process(pluginContext).getProcessedSelection(),
        result.equals(obj.process(pluginContext).getProcessedSelection()));
    assertEquals(url, pluginContext.getDocumentURL());

    toTest = "tHis i?\n" + 
    "s  <> # \n " +
    "new& ^   !\n" + 
    "    ! aaa A sample.";
    result = "THis i?\n" + 
    "S  <> # \n " +
    "New& ^   !\n" + 
    "    ! aaa A sample.";
    pluginContext = new SelectionPluginContextImpl(toTest, null, url, null);

    assertEquals(
        " Should be equal : ",
        result, obj.process(pluginContext).getProcessedSelection());
    assertEquals(url, pluginContext.getDocumentURL());
  }

}