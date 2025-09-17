package simple.documentation.framework.operations;

import java.io.File;
import java.io.StringWriter;
import java.net.MalformedURLException;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.InputSource;

import ro.sync.ecss.extensions.api.ArgumentDescriptor;
import ro.sync.ecss.extensions.api.ArgumentsMap;
import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.ecss.extensions.api.AuthorOperation;
import ro.sync.ecss.extensions.api.AuthorOperationException;
import ro.sync.ecss.extensions.api.access.AuthorXMLUtilAccess;

/**
 * Custom transform operation using Saxon EE transformer.
 */
public class TrasformOperation implements AuthorOperation {

  public void doOperation(AuthorAccess authorAccess, ArgumentsMap arguments)
  throws IllegalArgumentException, AuthorOperationException {
    // Choose the XSLT used in the transform operation
    File xsltFile = authorAccess.getWorkspaceAccess().chooseFile(
        // Title
        "Choose the XSLT", 
        // Extensions
        new String[] {"xsl", "xslt"}, 
        // Filter description
        "XSLT files",
        // Open for save
        false);

    if (xsltFile != null) {
      try {
        String xslSystemId = xsltFile.toURI().toURL().toString();
        // Create the XSLT source used in transform operation
        Source xslSource = new SAXSource(new InputSource(xslSystemId));
        // Create a XSL transformer without Oxygen options
        Transformer xslTransformer = authorAccess.getXMLUtilAccess().createXSLTTransformer(xslSource, null, AuthorXMLUtilAccess.TRANSFORMER_SAXON_ENTERPRISE_EDITION, false);
        Source editorSource = new SAXSource(new InputSource(authorAccess.getEditorAccess().createContentReader()));
        StringWriter result = new StringWriter();
        // Transform the current document using the specified XSLT
        xslTransformer.transform(editorSource, new StreamResult(result));
        StringBuffer resultBuffer = result.getBuffer();
        // Display the result
        authorAccess.getWorkspaceAccess().showInformationMessage("Transformation result: " + resultBuffer.toString());
        authorAccess.getWorkspaceAccess().showStatusMessage("Transformation finished");
      } catch (TransformerConfigurationException e) {
        // Display the error message
        authorAccess.getWorkspaceAccess().showErrorMessage(e.getMessage());
      } catch (TransformerException e) {
        // Display the error message
        authorAccess.getWorkspaceAccess().showErrorMessage(e.getMessage());
      } catch (MalformedURLException e) {
        // Display the error message
        authorAccess.getWorkspaceAccess().showErrorMessage(e.getMessage());
      }
    }
  }

  /**
   * Arguments
   */
  public ArgumentDescriptor[] getArguments() {
    return null;
  }

  /**
   * Description
   */
  public String getDescription() {
    return "Custom transform operation using Saxon EE transformer.";
  }
}
