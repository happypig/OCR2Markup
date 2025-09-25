package dila.formatpreserve;


import java.net.MalformedURLException;
import java.net.URL;

import junit.framework.TestCase;
import ro.sync.exml.plugin.selection.SelectionPluginContextImpl;

/**
 * Tests 
 * @author radu_coravu
 */
public class FormatPreserveTextPluginExtensionTest extends TestCase {

  /**
   * TC for process method.
   * 
   *@author radu_coravu
   *@throws MalformedURLException If URL is malformed.
   */
  public void testProcess() throws MalformedURLException {
    String toTest = "<root><a><b></b><c></c></a></root>";
    String result = "<root\n" + 
    		"><a\n" + 
    		"><b\n" + 
    		"></b\n" + 
    		"><c\n" + 
    		"></c\n" + 
    		"></a\n" + 
    		"></root>";

    URL url = new URL("ftp://www.a.com/test/a.xml");

    SelectionPluginContextImpl pluginContext = new SelectionPluginContextImpl(toTest, null, url, null);
    FormatPreserveTextPluginExtension obj = new FormatPreserveTextPluginExtension();

    assertEquals(
        " Should be equal : " + obj.process(pluginContext).getProcessedSelection(),
        result, obj.process(pluginContext).getProcessedSelection());
    assertEquals(url, pluginContext.getDocumentURL());
    
    toTest = "<root><a><b>1 > 2</b><c></c></a></root>";
    result = "<root\n" + 
    		"><a\n" + 
    		"><b\n" + 
    		">1 > 2</b\n" + 
    		"><c\n" + 
    		"></c\n" + 
    		"></a\n" + 
    		"></root>";

    pluginContext = new SelectionPluginContextImpl(toTest, null, url, null);
    obj = new FormatPreserveTextPluginExtension();

    assertEquals(
        " Should be equal : " + obj.process(pluginContext).getProcessedSelection(),
        result, obj.process(pluginContext).getProcessedSelection());
    assertEquals(url, pluginContext.getDocumentURL());
    
    toTest = result;

    pluginContext = new SelectionPluginContextImpl(toTest, null, url, null);
    obj = new FormatPreserveTextPluginExtension();

    assertEquals(
        " Should be equal : " + obj.process(pluginContext).getProcessedSelection(),
        result, obj.process(pluginContext).getProcessedSelection());
    assertEquals(url, pluginContext.getDocumentURL());
    
    toTest = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!--comment--><root><a><b>1 > 2</b><d/><c></c></a></root>";
    result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!--comment--><root\n" + 
    		"><a\n" + 
    		"><b\n" + 
    		">1 > 2</b\n" + 
    		"><d\n" + 
    		"/><c\n" + 
    		"></c\n" + 
    		"></a\n" + 
    		"></root>";

    pluginContext = new SelectionPluginContextImpl(toTest, null, url, null);
    obj = new FormatPreserveTextPluginExtension();

    assertEquals(
        " Should be equal : " + obj.process(pluginContext).getProcessedSelection(),
        result, obj.process(pluginContext).getProcessedSelection());
    assertEquals(url, pluginContext.getDocumentURL());
  }
}