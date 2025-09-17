package DILA;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JApplet;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;

import ro.sync.ecss.extensions.api.component.AuthorComponentException;
import ro.sync.ecss.extensions.api.component.AuthorComponentFactory;

/**
 * Demonstrates the use of the author component as an applet.
 */
@SuppressWarnings("serial")
public class AuthorComponentSampleApplet extends JApplet {
	
	/**
	 * The sample component wrapper
	 */
	private AuthorComponentSample sample;

	/**
	 * @see java.applet.Applet#init()
	 */
	@Override
	public void init() {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					try {
						// No restrictions to this signed applet
						//If you encounter any security problems running the applet uncomment the line below: 
//						System.setSecurityManager(null);
						
						// When using many small images, the performance increases if the disc cache is disabled. 
						ImageIO.setUseCache(false);
						

						// THIS IS THE WAY IN WHICH YOU CAN REGISTER YOUR OWN PROTOCOL HANDLER TO THE JVM.
						// THEN YOU CAN OPEN YOUR CUSTOM URLs IN THE APPLET AND THE APPLET WILL USE YOUR HANDLER
						// URL.setURLStreamHandlerFactory(new MyCustomURLStreamHandlerFactory());
						
						// Use a custom protocol handler when integrating the applet into a SharePoint site.
						if ("true".equals(getParameter("isOnSharePoint"))){
							URL.setURLStreamHandlerFactory(new DILA.net.protocol.http.handlers.CustomURLStreamHandlerFactory(AuthorComponentSampleApplet.this));
						}
						
						//EXM-28635 Avoid having Xerces connect back to the codebase for each created parser.
						System.setProperty(
						  "org.apache.xerces.xni.parser.XMLParserConfiguration",
				            "org.apache.xerces.parsers.XIncludeAwareParserConfiguration");
						
						// System look and feel
						UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

						// A gray to match the web page.
						Color gray = new Color(240,240,240);
						UIManager.put("Panel.background", gray);
						UIManager.put("ToolBar.background", gray);
						UIManager.put("SplitPaneDivider.background", gray);
						UIManager.put("ScrollBar.background", gray);
						
						if(new File(System.getProperty("user.home")).listFiles() == null) {
							//Problems with Safari 7 on MAC, applet by default runs in safe mode and cannot create files in user home.
							JOptionPane.showMessageDialog(AuthorComponentSampleApplet.this,
									"Applet does not have permissions to access the user home folder to store its resources cache.\n" + 
									"If you want to remove this warning and you are running the Applet using Safari on MAC OS X \n" +
									"please go to the 'Safari->Preferences->Security page', click on the 'Manage Website Settings' \n" +
									"button then select Java and for the corresponding applet site entry choose the 'Run in Unsafe mode' option.", 
									"Cannot write applet resources to user home", JOptionPane.WARNING_MESSAGE);
							//EXM-28624 If we have trouble accessing the user home we'll use the temporary files directory instead.
							System.setProperty("user.home", System.getProperty("java.io.tmpdir"));
						}
						//The document base
						URL docBase = getDocumentBase();
						System.out.println("[OXYGEN] Doc base is " + docBase);
						startAppletInit();
					} catch(Throwable t) {
						t.printStackTrace();
					}
				}});
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		super.init();
	}

	/**
	 * @see java.applet.Applet#stop()
	 */
	@Override
	public void stop() {
		super.stop();
	}

	/**
	 * @see java.applet.Applet#start()
	 */
	@Override
	public void start() {
		super.start();
	}

	/**
	 * Getter used from JavaScript
	 * @return The author component sample. Can be <code>null</code> if the 
	 * AuthorComponent did not initialized propertly, for instance a licens problem.
	 */
	public AuthorComponentSample getAuthorComponentSample() {
		return sample;
	}

	/**
	 * Setter used from JavaScript
	 * 
	 * @param width The new applet width
	 * @param height The new applet height
	 */
	public void changeSizeTo(final int width, final int height) {
		AuthorComponentSample.invokeOnAWT(new Runnable() {
			public void run() {
				AuthorComponentSampleApplet.super
				.setPreferredSize(new Dimension(width, height));
				AuthorComponentSampleApplet.super.setSize(width, height);
				AuthorComponentSampleApplet.super.validate();
			}
		});
	}
	
	
	/**
	 * Initialize the main applet components on a thread.
	 */
	private void startAppletInit() {
		final JPanel progressPanel = new JPanel(new GridLayout(2, 1));
		progressPanel.setBorder(new EmptyBorder(5, 15, 0, 15));
		progressPanel.add(new JLabel("Initializing..."));
		JProgressBar progressBar = new JProgressBar();
		progressBar.setIndeterminate(true);
		progressPanel.add(progressBar);
		add(progressPanel, BorderLayout.NORTH);
		new Thread() {
			public void run() {
				try {
					// Create the array of ZIPPED document type URLs 
					List<URL> frameworksURLs = new ArrayList<URL>();
					ClassLoader classLoader = AuthorComponentSampleApplet.class.getClassLoader();
					
					// Get the array of ZIPs from the applet
					URL frameworks = classLoader.getResource("frameworks.zip");
					if (frameworks != null) {
						frameworksURLs.add(frameworks);
					}
					
					// Get the ZIP pointing to fixed options if available.
					URL optionsZipURL = classLoader.getResource("options.zip");
					
					// The applet ID
					String appletID = getParameter("AppletID");
					
					String addToolbar = getParameter("addToolbar");
					String addHelperViews = getParameter("addHelperViews");
					String addBreadCrumb = getParameter("addBreadCrumb");
					
					
					// Check if the applet should show the "Open" action
					boolean showOpenLocal = false;
					
					URL documentBase = getDocumentBase();
					String query = documentBase.getQuery();
					if (query != null && query.contains("open_local")) {
						showOpenLocal = true;
					}
					
					boolean showToolbar = true;
					if ("false".equals(addToolbar)) {
					  showToolbar = false;
					}
					
					boolean showViews = true;
          if ("false".equals(addHelperViews)) {
            showViews = false;
          }
          
          boolean showBreadCrumb = true;
          if ("false".equals(addBreadCrumb)) {
            showBreadCrumb = false;
          }
					
					//Enable debug from JNLP or HTML.
					initLogger();
					
					// Create the sample panel
					sample = new AuthorComponentSample(
							frameworksURLs.toArray(new URL[0]), optionsZipURL, getCodeBase(), appletID, 
							showToolbar, showViews, showBreadCrumb, showOpenLocal);
					if (showToolbar) {
					  sample.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
					}
					//No side views by default for the applet.
					if (showViews) {
					  sample.setVisibleSideView("outline", false);
					  sample.setVisibleSideView("attributes", false);
					  sample.setVisibleSideView("elements", false);
					  sample.setVisibleSideView("review", false);
					  sample.setVisibleSideView("validationProblems", false);
					}
					
					//Signal to Javascript loading has ended.
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							try {
								//Load default content if necessary
								String loadDefaultContent = getParameter("loadDefaultContent");
								if("yes".equals(loadDefaultContent)) {
									sample
									.setDocumentContent(
											null,
											AuthorComponentSample.DEFAULT_DOC_CONTENT);
								} else {
									//Maybe load a remote URL from some place
									String loadLocationURL = getParameter("loadLocationURL");
									if(loadLocationURL != null && loadLocationURL.length() > 0) {
										try {
											URL toLoad = new URL(getDocumentBase(), loadLocationURL);
											sample.setDocumentContent(toLoad.toString(), null);
										} catch(MalformedURLException ex) {
											ex.printStackTrace();
											JOptionPane.showMessageDialog(AuthorComponentSampleApplet.this, ex.getMessage());
										} catch(AuthorComponentException ex) {
											JOptionPane.showMessageDialog(AuthorComponentSampleApplet.this, ex.getMessage());
											ex.printStackTrace();
										}
									} else {
										//Call the javascript method to let it know the applet has loaded.
										try {
											getAppletContext().showDocument(new URL("javascript:onLoad()"));
										} catch (MalformedURLException e) {
											JOptionPane.showMessageDialog(AuthorComponentSampleApplet.this, e.getMessage());
											//
										}
									}
								}
								//Remove the progress label
								remove(progressPanel);
								//Add the applet component
								add(sample);
								//Reconfigure
								invalidate();
								validate();
							} catch(Throwable t) {
								t.printStackTrace();
								JOptionPane.showMessageDialog(AuthorComponentSampleApplet.this, t.getMessage());
							}
						}
					});
				} catch (AuthorComponentException e) {
					JOptionPane.showMessageDialog(
							AuthorComponentSampleApplet.this,
							"AuthorComponent problem: " + e.getMessage(),
							"Cannot create author editor",
							JOptionPane.ERROR_MESSAGE);
					e.printStackTrace();
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}

		}.start();
	}
	
	
	/**
	 * Init the logger from HTML or JNLP parameters 
	 */
	private void initLogger() {
	  Logger logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
	  if(logger instanceof ch.qos.logback.classic.Logger) {
	    String severity = getParameter("loggerLevel");
	    if("debug".equals(severity)) {
	      ((ch.qos.logback.classic.Logger) logger).setLevel(Level.DEBUG);
	    } else if("error".equals(severity)) {
	      ((ch.qos.logback.classic.Logger) logger).setLevel(Level.ERROR);
	    } else if("warn".equals(severity)) {
	      ((ch.qos.logback.classic.Logger) logger).setLevel(Level.WARN);
	    } else if("fatal".equals(severity)) {
	      ((ch.qos.logback.classic.Logger) logger).setLevel(Level.ERROR);
	    } else if("all".equals(severity)) {
	      ((ch.qos.logback.classic.Logger) logger).setLevel(Level.ALL);
	    }
	  }
	}
	
	/**
	 * @see java.applet.Applet#destroy()
	 */
	@Override
	public void destroy() {
		AuthorComponentFactory factory = AuthorComponentFactory.getInstance();
		factory.dispose();
		super.destroy();
	}
}
