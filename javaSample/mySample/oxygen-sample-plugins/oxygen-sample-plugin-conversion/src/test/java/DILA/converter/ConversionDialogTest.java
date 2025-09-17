package DILA.converter;


import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;

import javax.swing.JTextField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

import junit.framework.TestCase;
import ro.sync.basic.io.FileSystemUtil;
import ro.sync.exml.plugin.PluginManager;

/**
 *  Description of the Class
 *
 *@author     radu
 *@created    October 16, 2002
 *@version    $Revision: 1.14 $
 */
public class ConversionDialogTest extends TestCase {
    /**
     *  Logger for logging.
     */
    private static Logger logger = LoggerFactory.getLogger(ConversionDialogTest.class.getName());

    /**
     *  A unit test for JUnit
     * @author dan
     * @throws IOException 
     * @author dan
     * @throws UnsupportedFlavorException 
     */
    public void testCopy() throws UnsupportedFlavorException, IOException {
      ConversionDialog cd = new ConversionDialog(null, "cd");

      // Set s1 text field text
      cd.masterValueTextField = new JTextField("123");
      // Copy text of s 1 text field to clipboard
      cd.copyMasterValue();
      // Test result
      logger.debug("Paste");
      Clipboard clipboard = cd.getToolkit().getSystemClipboard();
      logger.debug("Getting content.");
      Transferable content = clipboard.getContents(this);
      logger.debug("Obtaining transfer data");
      String dstData = (String) (content.getTransferData(DataFlavor.stringFlavor));
      if(logger.isDebugEnabled()){
        logger.debug("Content is : " + dstData);
      }
      assertEquals("123", dstData);
      logger.debug("Test copy ok.");
    }


    /**
     *  A unit test for JUnit
     * @author dan
     */
    public void testExtractNumberStr() {
      ConversionDialog cd = new ConversionDialog(null, "cd");
      assertEquals("1.22", cd.extractNumberStr("1.22 mm"));
      assertEquals("1.23", cd.extractNumberStr("1.23"));
    }


    /**
     *  A unit test for JUnit
     * @author dan
     */
    public void testFormatNumberWithLabel() {
      ConversionDialog cd = new ConversionDialog(null, "cd");
      cd.includeLabelCheck = new javax.swing.JCheckBox("", true);
      assertEquals("1.22 mm", cd.formatNumberWithLabel(""+1.22, "mm"));
    }

    /**
     *  The JUnit setup method
     */
    protected void setUp() throws IOException {
      // Create a temp dir to store the current plugin. The compiled classes will
      // not be present in this folder, but will be added in the classpath.
      File tempDir = Files.createTempDir();
      FileSystemUtil.copyDir(new File("."), new File(tempDir, "crtPlugin"), true, true);
      FileSystemUtil.copyFile(new File("../plugin.dtd"), new File(tempDir, "plugin.dtd"), true);
      PluginManager.setPluginsDir(tempDir.getAbsolutePath());
      PluginManager.disableLateDelegationCLForTests(true);
      PluginManager.getInstance(); 
    }

    /**
     *  The teardown method for JUnit
     */
    protected void tearDown() {
      PluginManager.disableLateDelegationCLForTests(false);
    }

}