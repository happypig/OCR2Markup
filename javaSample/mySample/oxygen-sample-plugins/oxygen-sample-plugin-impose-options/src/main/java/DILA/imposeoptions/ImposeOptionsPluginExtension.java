package DILA.imposeoptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import ro.sync.exml.options.PerspectivesLayoutInfo;
import ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension;
import ro.sync.exml.workspace.api.Platform;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * Plugin extension - workspace access extension.
 */
public class ImposeOptionsPluginExtension implements WorkspaceAccessPluginExtension {

  /**
   * Key for remembering that options have been imposed
   */
  private final static String OPTIONS_IMPOSED_KEY = "imposed.options";

  /**
   * @see ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension#applicationStarted(ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace)
   */
  @Override
  public void applicationStarted(final StandalonePluginWorkspace pluginWorkspaceAccess) {
	  // Prevent 'UnsupportedOprationException'
	  if(!pluginWorkspaceAccess.getPlatform().equals(Platform.WEBAPP)) {

		  boolean impose = true;
		  File baseDir = ImposeOptionsPlugin.getInstance().getDescriptor().getBaseDir();
		  String imposedTS = pluginWorkspaceAccess.getOptionsStorage().getOption(OPTIONS_IMPOSED_KEY,
				  null);
		  if (imposedTS != null) {
			  if (imposedTS.equals(ImposeOptionsPlugin.getInstance().getDescriptor().getVersion())) {
				  //We have already imposed them.
				  impose = false;
			  }
		  }
		  //We remember we have already set certain options for this version of the plugin.
		  pluginWorkspaceAccess.getOptionsStorage().setOption(OPTIONS_IMPOSED_KEY,
				  ImposeOptionsPlugin.getInstance().getDescriptor().getVersion());
		  if (impose) {
			  //Global options file.
			  File optionsFile = new File(baseDir, "options.xml");
			  if (optionsFile.exists()) {
				  //One approach with having an options.xml would be the issue of having references to certain resources.
				  //These references should be somehow redirected to the current plugin's installation folder. 
				  try {
					  StringBuilder content = new StringBuilder(Math.max((int) optionsFile.length(), 100));
					  InputStreamReader reader = new InputStreamReader(new FileInputStream(optionsFile),
							  "UTF-8");
					  char[] buf = new char[1024];
					  int len = -1;
					  while ((len = reader.read(buf)) != -1) {
						  content.append(buf, 0, len);
					  }
					  reader.close();
					  if (content.indexOf("OXY_PLUGIN_REPLACEMENT_PATH") != -1) {
						  String replacedContent = content.toString();
						  replacedContent = replacedContent.replaceAll("OXY_PLUGIN_REPLACEMENT_PATH_DIR",
								  baseDir.getAbsolutePath());
						  replacedContent = replacedContent.replaceAll("OXY_PLUGIN_REPLACEMENT_PATH_URL", baseDir
								  .toURI().toASCIIString());

						  //And use it with the variables replaced.
						  optionsFile = File.createTempFile("tempOxygenOptions", ".xml");
						  optionsFile.deleteOnExit();
						  OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(optionsFile),
								  "UTF-8");
						  osw.write(replacedContent);
						  osw.close();
					  }
				  } catch (Exception e) {
					  e.printStackTrace();
				  }
				  pluginWorkspaceAccess.importGlobalOptions(optionsFile);
			  }

			  File templatesDir = new File(baseDir, "templates");
			  if (templatesDir.exists()) {
				  //Impose folder for templates dir
				  pluginWorkspaceAccess.setGlobalObjectProperty("user.custom.templates.directories",
						  new String[] { templatesDir.getAbsolutePath() });
			  }
			  //Application layout.
			  File layoutFile = new File(baseDir, "application.layout");
			  if (layoutFile.exists()) {
				  PerspectivesLayoutInfo info = new PerspectivesLayoutInfo(true, false, "",
						  layoutFile.getAbsolutePath());
				  pluginWorkspaceAccess.setGlobalObjectProperty("perspectives.layout.info", info);
				  if(!"false".equals(System.getProperty("com.oxygenxml.impose.showRestartDialog"))){
					  pluginWorkspaceAccess.showInformationMessage("Predefined window layout was loaded. Please restart the application to apply it.");
				  } else {
					  //EXM-36512 Avoid showing the dialog.
				  }
			  }

		  }
	  }
  }

  @Override
  public boolean applicationClosing() {
    return true;
  }
}