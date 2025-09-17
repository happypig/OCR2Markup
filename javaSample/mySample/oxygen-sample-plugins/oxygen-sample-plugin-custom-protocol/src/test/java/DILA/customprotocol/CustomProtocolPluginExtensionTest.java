package DILA.customprotocol;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Optional;

import com.google.common.io.Files;

import junit.framework.TestCase;
import ro.sync.basic.io.FileSystemUtil;
import ro.sync.basic.util.URLUtil;
import ro.sync.exml.plugin.PluginManager;
import ro.sync.net.protocol.Installer;

/**
 * Tests the "cproto" protocol by writing the line numbers in the file indicated by the URL.
 * 
 * @author dan
 */
public class CustomProtocolPluginExtensionTest extends TestCase {
  /**
   * Temporary dir to store plugins.
   */
  private File tempDir;

  /**
   * Sets up the plugins dir.
   */
	@Override
	protected void setUp() throws Exception {
    // Create a temp dir to store the current plugin. The compiled classes will
    // not be present in this folder, but will be added in the classpath.
    tempDir = Files.createTempDir();
    FileSystemUtil.copyDir(new File("."), new File(tempDir, "crtPlugin"), true, true);
    FileSystemUtil.copyFile(new File("../plugin.dtd"), new File(tempDir, "plugin.dtd"), true);
    PluginManager.setPluginsDir(tempDir.getAbsolutePath());
		super.setUp();
	}
	
	/**
   *  The teardown method for JUnit
   */
  protected void tearDown() {
    FileSystemUtil.deleteRecursivelly(tempDir);
  }

  /**
   * Uses the file2 protocol to write line numbers in the file indicated by the URL
   * 
   * @param url
   *          The URL of the file
   * @throws Exception
   */
  public static void rewriteFile(URL url) throws Exception {

    URLConnection connection = url.openConnection();
    // The input stream of the connection
    BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
    // The new content of the file
    String newContent = "";
    // A line in the file
    String line = "";
    int i = 1;
    while ((line = input.readLine()) != null) {
      // Add the line numbers
      newContent += i + ". " + line + "\n";
      i++;
    }
    // The output stream of the connection
    BufferedWriter output = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
    output.write(newContent);
    output.flush();
    //Close streams
    input.close();
    output.close();
  }

  /**
   * Test file2 protocol
   * @author dan
   * @throws Exception 
   */
  public void testWriteInFile() throws Exception {
    //Create a new file in test dir
	File testDir = new File("test");	  
	testDir.mkdir();
	
    File file = new File(testDir, "file2.txt");
    FileWriter writer = new FileWriter(file);
    String content = "Method Summary\n"
        + "int available()\n"
        + "Returns the number of bytes that can be read from this file input stream without blocking.\n"
        + "void close()\n"
        + "Closes this file input stream and releases any system resources associated with the stream.\n"
        + "protected  void finalize()\n"
        + "Ensures that the close method of this file input stream is called when there are no more references to it.";
    //Write it's content
    writer.write(content);
    writer.flush();
    writer.close();

    // Install the protocols
    Installer.installProtocols();
    //Get the URL of the file using file2 protocol
    URL url = new URL("cproto:" + URLUtil.correct(file.getAbsolutePath(), false));
    
    //Test the protocol
    rewriteFile(url);
    
    //Check the new content of the file
    //The lines from the file must be numbered
    BufferedReader reader= new BufferedReader(new FileReader(file));
    String line = reader.readLine();
    assertTrue("content of the file not modified "+line, line.equals("1. Method Summary"));
    line = reader.readLine();
    assertTrue("content of the file not modified "+line, line.equals("2. int available()"));
    line = reader.readLine();
    assertTrue("content of the file not modified "+line, line.equals("3. Returns the number of bytes that can be read from this file input stream without blocking."));
    reader.close();
    
    //Delete the file
    file.delete();
    testDir.delete();
  }
  
  /**
   * <p><b>Description:</b> Make sure only the protocol is corrected when the canonical file is retrieved.</p>
   * <p><b>Bug ID:</b> EXM-48610</p>
   *
   * @author adrian_sorop
   * @throws Exception
   */
  public void testCorrectOnlyProtocol() throws Exception {

    File testDir = new File("cproto");    
    testDir.mkdir();
    try {
      File file = new File(testDir, "cproto_fileName.txt");
      FileWriter writer = new FileWriter(file);
      String content = "something";
      //Write it's content
      writer.write(content);
      writer.flush();
      writer.close();

      // Install the protocols
      Installer.installProtocols();
      //Get the URL of the file using file2 protocol
      URL url = new URL("cproto:" + URLUtil.correct(file.getAbsolutePath(), false));

      File canonicalFileFromFileUrl = CustomProtocolHandler.getCanonicalFileFromFileUrl(url);
      assertEquals(file.getAbsolutePath(), canonicalFileFromFileUrl.getAbsolutePath());
    } finally {
      Optional.ofNullable(testDir).ifPresent(t -> FileSystemUtil.deleteRecursivelly(t));
    }
  }

}