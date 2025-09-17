package DILA.reviewer;

import java.net.URL;

import javax.swing.JFrame;
import javax.swing.UIManager;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * A stand alone simple reviewer application based on the AuthorComponent.
 * 
 * It allows the user to change the editing mode between: read-only, text editing only and 
 * full editing.
 * 
 * @author cristi_talau
 */
public class Reviewer {
	/**
	 * The default document content.
	 */
	private final static String DEFAULT_DOC_CONTENT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<!DOCTYPE concept PUBLIC \"-//OASIS//DTD DITA Concept//EN\" \"http://docs.oasis-open.org/dita/v1.1/OS/dtd/concept.dtd\">\n"
			+ "<concept id=\"conceptId\">\n"
			+ "    <title>Winter Flowers</title>\n"
			+ "    <conbody>\n"
			+ "        <p>Winter is the season of cold weather. The season occurs during December - February in\n"
			+ "            Northern hemisphere . In the Southern hemisphere winter occurs during June - August. </p>\n"
			+ "        <p>Some of the flowers blooming in winter are: Acashia, Alstromeria, Amaryllis, Carnation,\n"
			+ "            Chrysanthemums, Cyclamen, Evergreens, Gerbera Daisy, Ginger, Helleborus, Holly berry,\n"
			+ "            Lily, Asiatic Lily, Casa Blanca Lily, Narcissus, Orchid, Pansy, Pepperberry, Phlox,\n"
			+ "            Protea, Queen Ann's Lace, Roses, Star of Bethlehem, Statice. </p>\n"
			+ "    </conbody>\n" + "</concept>\n" + "";

	

	/**
	 * Logger.
	 */
	static Logger logger = LoggerFactory.getLogger(Reviewer.class.getName());

	/**
	 * Brings up a frame with the component sample.
	 * 
	 * @param args
	 *            ignored.
	 */
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

			// These two zips are stored inside two jar artifacts referred by the POM.
			URL frameworksZipURL = Reviewer.class.getResource("/frameworks.zip");  
			URL optionsZipURL = Reviewer.class.getResource("/options.zip");

			logger.info("Loading options from: " + optionsZipURL);
			logger.info("Loading frameworks from: " + frameworksZipURL);

			JFrame frame = new JFrame("Author Component Sample Reviewer Application");
			AuthorComponentReviewerSample sample = new AuthorComponentReviewerSample(
			// Frameworks Zip
					new URL[] { frameworksZipURL },
					// Options Zip
					optionsZipURL,
					// TODO set here licensing details
					"http://licenseServletURL/", "user", "password");

			sample.setDocumentContent(null, DEFAULT_DOC_CONTENT);

			frame.setSize(1000, 700);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.getContentPane().add(sample);
			frame.setVisible(true);

		} catch (Exception e) {
			logger.error(e, e);
		}
	}

}
