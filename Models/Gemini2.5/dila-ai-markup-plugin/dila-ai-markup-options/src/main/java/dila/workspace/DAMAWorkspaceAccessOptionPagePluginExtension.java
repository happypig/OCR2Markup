package dila.workspace;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import ro.sync.exml.plugin.option.OptionPagePluginExtension;
import ro.sync.exml.workspace.api.PluginWorkspace;

/**
 * Plugin option page extension Custom Workspace Access Plugin Extension.
 */
public class DAMAWorkspaceAccessOptionPagePluginExtension extends OptionPagePluginExtension {

  /**
   * The option page key for this preferences page.
   */
  public static final String DILA_DAMA_OPTIONS_PAGE_KEY = "dila.dama.options.page.key";

    /**
   * The option key describing the ft parse model.
   */
  public static final String KEY_DILA_DAMA_FT_PARSE_MODEL = "dila.dama.ft.parse.model";

  /**
   * The option key describing the ft detect model.
   */
  public static final String KEY_DILA_DAMA_FT_DETECT_MODEL = "dila.dama.ft.detect.model";

  /**
   * The option key describing the API key.
   */
  public static final String KEY_DILA_DAMA_API_KEY = "dila.dama.api.key";
  
  /**
   * The text field for the ft parse model.
   */
  private JTextField parseModelTextField;

  /**
   * The text field for the ft detect model.
   */
  private JTextField detectModelTextField;

  /**
   * The text field for the API key.
   */
  private JTextField apiKeyTextField;

  /**
   * Resource bundle for i18n support.
   */
  private PluginResourceBundle resources;

  /**
   * @see ro.sync.exml.plugin.option.OptionPagePluginExtension#apply(ro.sync.exml.workspace.api.PluginWorkspace)
   */
  @Override
  public void apply(PluginWorkspace pluginWorkspace) {
    // Save the new locations in the option storage.
    pluginWorkspace.getOptionsStorage().setOption(KEY_DILA_DAMA_FT_PARSE_MODEL,
        !"".equals(parseModelTextField.getText()) ? parseModelTextField.getText() : null);
    pluginWorkspace.getOptionsStorage().setOption(KEY_DILA_DAMA_FT_DETECT_MODEL,
        !"".equals(detectModelTextField.getText()) ? detectModelTextField.getText() : null);
    pluginWorkspace.getOptionsStorage().setOption(KEY_DILA_DAMA_API_KEY,
        !"".equals(apiKeyTextField.getText()) ? apiKeyTextField.getText() : null);
  }

  /**
   * @see ro.sync.exml.plugin.option.OptionPagePluginExtension#restoreDefaults()
   */
  @Override
  public void restoreDefaults() {
    // Reset the text fields values. Empty string is used to map the <null> default values of the options.
    parseModelTextField.setText("");
    detectModelTextField.setText("");
    apiKeyTextField.setText("");
  }

  /**
   * @see ro.sync.exml.plugin.option.OptionPagePluginExtension#getTitle()
   */
  @Override
  public String getTitle() {
    return "DILA AI Markup Assistant Options";
  }

  /**
   * @see ro.sync.exml.plugin.option.OptionPagePluginExtension#init(ro.sync.exml.workspace.api.PluginWorkspace)
   */
  @Override
  public JComponent init(final PluginWorkspace pluginWorkspace) {
    
    // Store the resource bundle as an instance variable
    this.resources = pluginWorkspace.getResourceBundle();

    GridBagConstraints c = new GridBagConstraints();
    JPanel panel = new JPanel(new GridBagLayout());

    JLabel parseModelLabel = new JLabel(getMessage("ft.parse.model.label"));
    c.gridx = 0;
    c.gridy = 0;
    c.weightx = 0;
    c.weighty = 0;
    c.anchor = GridBagConstraints.WEST;
    panel.add(parseModelLabel, c);

    parseModelTextField = new JTextField();
    c.gridx ++;
    c.weightx = 1;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.insets = new Insets(0, 5, 0, 5);
    panel.add(parseModelTextField, c);
    
    c.gridx = 0;
    c.gridy ++;
    JLabel detectModelLabel = new JLabel(getMessage("ft.detect.model.label"));
    panel.add(detectModelLabel, c);

    detectModelTextField = new JTextField();
    c.gridx ++;
    c.weightx = 1;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.insets = new Insets(0, 5, 0, 5);
    panel.add(detectModelTextField, c);
    
    c.gridx = 0;
    c.gridy ++;
    JLabel apiKeyLabel = new JLabel(getMessage("api.key.label"));
    panel.add(apiKeyLabel, c);

    apiKeyTextField = new JTextField();
    c.gridx ++;
    c.weightx = 1;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.insets = new Insets(0, 5, 0, 5);
    panel.add(apiKeyTextField, c);

    c.gridx = 0;
    c.gridy ++;
    c.gridwidth = 3;
    c.weightx = 1;
    c.weighty = 1;
    c.fill = GridBagConstraints.BOTH;
    panel.add(new JPanel(), c);
    
    String ftParseModel = pluginWorkspace.getOptionsStorage().getOption(
        KEY_DILA_DAMA_FT_PARSE_MODEL, 
        null);

    String ftDetectModel = pluginWorkspace.getOptionsStorage().getOption(
        KEY_DILA_DAMA_FT_DETECT_MODEL,
        null);

    String apiKey = pluginWorkspace.getOptionsStorage().getOption(
        KEY_DILA_DAMA_API_KEY,
        null);
        null);
    
    // Initialize the text fields with the stored options.
    parseModelTextField.setText(ftParseModel != null ? ftParseModel : "");
    detectModelTextField.setText(ftDetectModel != null ? ftDetectModel : "");
    apiKeyTextField.setText(apiKey != null ? apiKey : "");
    
    return panel;
  }

  /**
   * Gets a localized message by key.
   * @param key the message key
   * @return the localized message
   */
  private String getMessage(String key) {
    return resources != null ? resources.getMessage(key) : key;
  }

  /**
   * Gets a key of option page key.
   * @return the option page key
   */
  @Override
  public String getKey() {
    return DILA_DAMA_OPTIONS_PAGE_KEY;
  }
}
