package dila.openredirect;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import ro.sync.exml.plugin.openredirect.OpenRedirectExtension;
import ro.sync.exml.plugin.openredirect.OpenRedirectInformation;

/**
 * Custom extension to redirect open for some URLs
 */
public class CustomOpenRedirectExtension implements OpenRedirectExtension {

  /**
   * @see ro.sync.exml.plugin.openredirect.OpenRedirectExtension#redirect(java.net.URL)
   */
  public OpenRedirectInformation[] redirect(URL url) {
    if(url != null) {
      if(url.toString().endsWith("exe")) {
        //Executable file, run as a separate process, reject default open in Oxygen
        try {
          Runtime.getRuntime().exec(url.getFile().replaceAll("%20", " "));
        } catch (IOException e) {
          e.printStackTrace();
        }
        return new OpenRedirectInformation[0];
      } else if(url.toString().endsWith("xlsx")) {
        //XML Excel version, open all sheets in the main editor and the zip
        //in the archive browser
        List<URL> sheetURLs = getURLsFromInsideZip(url, "sheet", ".xml");
        List<OpenRedirectInformation> openInfo = new ArrayList<OpenRedirectInformation>();
        openInfo.add( //Open the main ZIP in the archive browser
            new OpenRedirectInformation(url, null));
        for (int i = 0; i < sheetURLs.size(); i++) {
          openInfo.add( //Open the sheet
              new OpenRedirectInformation(sheetURLs.get(i), null));
        }
        return openInfo.toArray(new OpenRedirectInformation[0]);
      } else if(url.toString().endsWith("docx")) {
        //XML Word version, open all documents in the main editor and the zip
        //in the archive browser
        List<URL> docURLs = getURLsFromInsideZip(url, "document", ".xml");
        List<OpenRedirectInformation> openInfo = new ArrayList<OpenRedirectInformation>();
        openInfo.add( //Open the main ZIP in the archive browser
            new OpenRedirectInformation(url, null));
        for (int i = 0; i < docURLs.size(); i++) {
          openInfo.add( //Open the doc
              new OpenRedirectInformation(docURLs.get(i), null));
        }
        return openInfo.toArray(new OpenRedirectInformation[0]);
      } else if(url.toString().endsWith("dxp")) {
        //DITA XML Project, open all documents in the main editor and the zip
        //in the archive browser
        List<URL> mapURLs = getURLsFromInsideZip(url, "", ".ditamap");
        List<OpenRedirectInformation> openInfo = new ArrayList<OpenRedirectInformation>();
        openInfo.add( //Open the main ZIP in the archive browser
            new OpenRedirectInformation(url, OpenRedirectInformation.ZIP_CONTENT_TYPE));
        for (int i = 0; i < mapURLs.size(); i++) {
          openInfo.add( //Open the dita map
              new OpenRedirectInformation(mapURLs.get(i), null));
        }
        return openInfo.toArray(new OpenRedirectInformation[0]);
      } else if(url.toString().endsWith("svg")) {
        //Open the SVG both in the main editor (default) and in the image previewer
        return new OpenRedirectInformation[] {
            //Open the main ZIP in the archive browser
            new OpenRedirectInformation(url, null),
            //Open the first sheet
            new OpenRedirectInformation(url, OpenRedirectInformation.IMAGE_CONTENT_TYPE)
        };
      } else if(url.toString().endsWith(".xml.zip")) {
        //Zipped XML files. We delegate this type of file to a special sample protocol implementation.
        //The idea is to open Zipped XML files seamlessly using the file chooser.
        String str = url.toString();
        int protoIndex = str.indexOf(":/");
        if(protoIndex != -1){
          str = "filexmlzip" + str.substring(protoIndex);
          try {
            return new OpenRedirectInformation[] {new OpenRedirectInformation(new URL(str), "text/xml")};
          } catch (MalformedURLException e) {
            e.printStackTrace();
          }
        }
      } 
    }
    //Default behavior of Oxygen
    return null;
  }
  
  private static List<URL> getURLsFromInsideZip(URL zipURL, String fileNamePrefix, String fileNameSuffix) {
    List<URL> urls = new ArrayList<URL>();
    try {
      File zipFile = new File(URLDecoder.decode(zipURL.getFile(), "UTF8"));
      if(zipFile.exists()) {
        ZipFile dxpZip = new ZipFile(zipFile);
        try {
          Enumeration<? extends ZipEntry> entries = dxpZip.entries();
          while(entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String entryFileName = entry.getName();
            int pathIndex = entryFileName.lastIndexOf("/");
            if(pathIndex  != -1 && pathIndex < entryFileName.length() - 1) {
              entryFileName = entryFileName.substring(pathIndex + 1);
            }
            if(entryFileName.startsWith(fileNamePrefix) && entryFileName.endsWith(fileNameSuffix)) {
              URL entryURL = new URL("zip:" + zipURL.toString() + "!/" + entry.getName());
              urls.add(entryURL);
            }
          }
        } finally {
          dxpZip.close();
        }
      }
    } catch(Exception ex) {
      ex.printStackTrace();
    }
    return urls;
  }
}
