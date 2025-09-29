package dila.workspace;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JPasswordField;

import ro.sync.exml.plugin.option.OptionPagePluginExtension;
import ro.sync.exml.workspace.api.PluginResourceBundle;
import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * Plugin option page extension Custom Workspace Access Plugin Extension.
 */
public class DAMAWorkspaceAccessOptionPagePluginExtension extends OptionPagePluginExtension {

  /**
   * The option page key for this preferences page.
   */
  public static final String DILA_DAMA_OPTIONS_PAGE_KEY = "dila_ai_markup_options";

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
   * Safe fallback values to prevent null pointer exceptions in Oxygen's options system.
   */
  private static final String SAFE_KEY_FALLBACK = "dila_safe_fallback";
  private static final String SAFE_VALUE_FALLBACK = "";
  
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
    try {
      System.out.println("DILA Plugin: Applying option page settings...");
      
      // Ultra-defensive checks for system-wide preferences saving
      if (pluginWorkspace == null) {
        System.out.println("DILA Plugin: PluginWorkspace is null in apply()");
        return; // Silently return without error
      }
      
      if (pluginWorkspace.getOptionsStorage() == null) {
        System.out.println("DILA Plugin: OptionsStorage is null in apply()");
        return; // Silently return without error
      }
      
      // Additional check: ensure text fields were initialized
      if (parseModelTextField == null || detectModelTextField == null || apiKeyTextField == null) {
        System.out.println("DILA Plugin: Text fields not initialized in apply()");
        return; // Silently return without error
      }
      
      // Save the new locations in the option storage.
      // Extra null safety checks to prevent NullPointerException in Oxygen's options system
      // Ensure text fields are not null before accessing them
      String parseModelText = (parseModelTextField != null && parseModelTextField.getText() != null) 
          ? parseModelTextField.getText().trim() : "";
      String detectModelText = (detectModelTextField != null && detectModelTextField.getText() != null) 
          ? detectModelTextField.getText().trim() : "";
      String apiKeyText = (apiKeyTextField != null && apiKeyTextField.getText() != null) 
          ? apiKeyTextField.getText().trim() : "";
      
      // Only set options if keys are not null
      // Ultra-defensive option setting to prevent ConcurrentHashMap.putVal NPE
      // CRITICAL: Both key and value must be non-null for ConcurrentHashMap.put()
      if (KEY_DILA_DAMA_FT_PARSE_MODEL != null && !KEY_DILA_DAMA_FT_PARSE_MODEL.trim().isEmpty() && parseModelText != null) {
        try {
          System.out.println("DILA Plugin: Setting parse model: " + parseModelText);
          pluginWorkspace.getOptionsStorage().setOption(KEY_DILA_DAMA_FT_PARSE_MODEL, parseModelText);
        } catch (Exception e) {
          System.out.println("[E] DILA Plugin: Failed to set parse model option: " + e.getMessage());
        }
      }
      if (KEY_DILA_DAMA_FT_DETECT_MODEL != null && !KEY_DILA_DAMA_FT_DETECT_MODEL.trim().isEmpty() && detectModelText != null) {
        try {
          System.out.println("DILA Plugin: Setting detect model: " + detectModelText);
          pluginWorkspace.getOptionsStorage().setOption(KEY_DILA_DAMA_FT_DETECT_MODEL, detectModelText);
        } catch (Exception e) {
          System.out.println("[E] DILA Plugin: Failed to set detect model option: " + e.getMessage());
        }
      }
      if (KEY_DILA_DAMA_API_KEY != null && !KEY_DILA_DAMA_API_KEY.trim().isEmpty() && apiKeyText != null) {
        try {
          System.out.println("DILA Plugin: Setting API key: " + (apiKeyText.isEmpty() ? "[empty]" : "[***]"));
          pluginWorkspace.getOptionsStorage().setSecretOption(KEY_DILA_DAMA_API_KEY, apiKeyText);
        } catch (Exception e) {
          System.out.println("[E] DILA Plugin: Failed to set API key option: " + e.getMessage());
        }
      }
      
      System.out.println("DILA Plugin: Options saved successfully");
    } catch (Exception e) {
      // Log the error to prevent crashes during option saving
      System.out.println("[E] DILA Plugin: Error saving options: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * @see ro.sync.exml.plugin.option.OptionPagePluginExtension#restoreDefaults()
   */
  @Override
  public void restoreDefaults() {
    try {
      // Reset the text fields values. Use safe fallback to prevent null issues.
      if (parseModelTextField != null) {
        parseModelTextField.setText(SAFE_VALUE_FALLBACK);
      }
      if (detectModelTextField != null) {
        detectModelTextField.setText(SAFE_VALUE_FALLBACK);
      }
      if (apiKeyTextField != null) {
        apiKeyTextField.setText(SAFE_VALUE_FALLBACK);
      }
    } catch (Exception e) {
      System.out.println("[E] DILA Plugin: Error restoring defaults: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * @see ro.sync.exml.plugin.option.OptionPagePluginExtension#getTitle()
   */
  @Override
  public String getTitle() {
    try {
      // Ensure we NEVER return null - this prevents NPE in Oxygen's options dialog
      String title = "DILA AI Markup Assistant Options";
      return (title != null && !title.trim().isEmpty()) ? title : "DILA Plugin";
    } catch (Exception e) {
      System.out.println("[E] DILA Plugin: Error in getTitle(): " + e.getMessage());
      e.printStackTrace();
      // Return a guaranteed non-null, non-empty string
      return "DILA Plugin";
    }
  }

  /**
   * @see ro.sync.exml.plugin.option.OptionPagePluginExtension#init(ro.sync.exml.workspace.api.PluginWorkspace)
   */
  @Override
  public JComponent init(final PluginWorkspace pluginWorkspace) {
    try {
      // Ultra-defensive initialization for system-wide preferences loading
      System.out.println("DILA Plugin: Initializing option page...");
      
      // Check for null workspace early
      if (pluginWorkspace == null) {
        System.out.println("DILA Plugin: PluginWorkspace is null in init()");
        return createSimplePanel("Plugin workspace not available");
      }
      
      // Check if options storage is available
      if (pluginWorkspace.getOptionsStorage() == null) {
        System.out.println("DILA Plugin: OptionsStorage is null in init()");
        return createSimplePanel("Options storage not available");
      }

      // Store the resource bundle as an instance variable
      // According to Oxygen SDK, cast to StandalonePluginWorkspace to access getResourceBundle()
      try {
        if (pluginWorkspace instanceof StandalonePluginWorkspace) {
          this.resources = ((StandalonePluginWorkspace) pluginWorkspace).getResourceBundle();
          if (this.resources != null) {
            System.out.println("DILA Plugin: Resource bundle loaded successfully");
          } else {
            System.out.println("DILA Plugin: Resource bundle is null - i18n translations may not work");
          }
        } else {
          System.out.println("DILA Plugin: PluginWorkspace is not StandalonePluginWorkspace - cannot access resource bundle");
          this.resources = null;
        }
      } catch (Exception e) {
        System.out.println("[E] DILA Plugin: Failed to load resource bundle: " + e.getMessage());
        this.resources = null;
      }

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

      apiKeyTextField = new JPasswordField();
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
      
      // Get stored options with extra null safety
      String ftParseModel = SAFE_VALUE_FALLBACK;
      String ftDetectModel = SAFE_VALUE_FALLBACK;
      String apiKey = SAFE_VALUE_FALLBACK;
      
      if (pluginWorkspace != null && pluginWorkspace.getOptionsStorage() != null) {
        if (KEY_DILA_DAMA_FT_PARSE_MODEL != null && !KEY_DILA_DAMA_FT_PARSE_MODEL.trim().isEmpty()) {
          try {
            String value = pluginWorkspace.getOptionsStorage().getOption(KEY_DILA_DAMA_FT_PARSE_MODEL, SAFE_VALUE_FALLBACK);
            ftParseModel = (value != null) ? value : SAFE_VALUE_FALLBACK;
          } catch (Exception e) {
            System.out.println("[E] DILA Plugin: Failed to get parse model option: " + e.getMessage());
            ftParseModel = SAFE_VALUE_FALLBACK;
          }
        }
        if (KEY_DILA_DAMA_FT_DETECT_MODEL != null && !KEY_DILA_DAMA_FT_DETECT_MODEL.trim().isEmpty()) {
          try {
            String value = pluginWorkspace.getOptionsStorage().getOption(KEY_DILA_DAMA_FT_DETECT_MODEL, SAFE_VALUE_FALLBACK);
            ftDetectModel = (value != null) ? value : SAFE_VALUE_FALLBACK;
          } catch (Exception e) {
            System.out.println("[E] DILA Plugin: Failed to get detect model option: " + e.getMessage());
            ftDetectModel = SAFE_VALUE_FALLBACK;
          }
        }
        if (KEY_DILA_DAMA_API_KEY != null && !KEY_DILA_DAMA_API_KEY.trim().isEmpty()) {
          try {
            String value = pluginWorkspace.getOptionsStorage().getSecretOption(KEY_DILA_DAMA_API_KEY, SAFE_VALUE_FALLBACK);
            apiKey = (value != null) ? value : SAFE_VALUE_FALLBACK;
          } catch (Exception e) {
            System.out.println("[E] DILA Plugin: Failed to get API key option: " + e.getMessage());
            apiKey = SAFE_VALUE_FALLBACK;
          }
        }
      }
      
      // Initialize the text fields with the stored options (guaranteed non-null)
      if (parseModelTextField != null) {
        parseModelTextField.setText((ftParseModel != null) ? ftParseModel : SAFE_VALUE_FALLBACK);
      }
      if (detectModelTextField != null) {
        detectModelTextField.setText((ftDetectModel != null) ? ftDetectModel : SAFE_VALUE_FALLBACK);
      }
      if (apiKeyTextField != null) {
        apiKeyTextField.setText((apiKey != null) ? apiKey : SAFE_VALUE_FALLBACK);
      }
      
      return panel;
    } catch (Exception e) {
      // Log the error and return a simple panel to prevent crashes
      System.out.println("[E] DILA Plugin: Error initializing options page: " + e.getMessage());
      e.printStackTrace();
      return new JPanel(); // Return empty panel as fallback
    }
  }

  /**
   * Creates a simple panel with a message for error cases.
   */
  private JPanel createSimplePanel(String message) {
    JPanel panel = new JPanel();
    if (message != null && !message.isEmpty()) {
      JLabel label = new JLabel(message);
      panel.add(label);
    }
    return panel;
  }


  /**
   * Gets a localized message by key.
   * Uses the properly loaded PluginResourceBundle from StandalonePluginWorkspace.
   * @param key the message key
   * @return the localized message
   */
  private String getMessage(String key) {
    // Try to get translation from resource bundle first
    if (resources != null) {
      try {
        String translation = resources.getMessage(key);
        // Check if we got a valid translation (not just the key back)
        if (translation != null && !key.equals(translation)) {
          return translation;
        }
      } catch (Exception e) {
        System.out.println("[E] DILA Plugin: Error getting message for key '" + key + "': " + e.getMessage());
      }
    }
    
    // Fallback translations matching the keys in our i18n/translation.xml
    switch (key) {
      case "ft.parse.model.label":
        return "Parsing model:";
      case "ft.detect.model.label":
        return "Detection model:";
      case "api.key.label":
        return "API Key*: ";
      default:
        System.out.println("DILA Plugin: Using key as fallback for: " + key);
        return key; // Return the key itself if no translation found
    }
  }

  /**
   * Gets a key of option page key.
   * @return the option page key - NEVER null to prevent ConcurrentHashMap.putVal NPE
   */
  @Override
  public String getKey() {
    try {
      // Ensure we NEVER return null - this is critical for Oxygen's options system
      String key = DILA_DAMA_OPTIONS_PAGE_KEY;
      if (key == null || key.trim().isEmpty()) {
        System.out.println("DILA Plugin: Option page key is null/empty, using fallback");
        return SAFE_KEY_FALLBACK;
      }
      return key;
    } catch (Exception e) {
      System.out.println("[E] DILA Plugin: Error in getKey(): " + e.getMessage());
      e.printStackTrace();
      // Return a guaranteed non-null, non-empty string
      return SAFE_KEY_FALLBACK;
    }
  }
}
