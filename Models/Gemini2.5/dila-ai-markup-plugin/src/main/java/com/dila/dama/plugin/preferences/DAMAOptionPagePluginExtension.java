package com.dila.dama.plugin.preferences;

import com.dila.dama.plugin.util.PluginLogger;

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
public class DAMAOptionPagePluginExtension extends OptionPagePluginExtension {

  /**
   * The option page title for this preferences page.
   * Magic string matches the name from plugin.xml in order to get flat entry(no extra blank page root for single preference page plugin) right under plugins in preference tree.
   */
  public static final String DILA_DAMA_OPTIONS_PAGE_TITLE = "DILA AI Markup Assistant";

    /**
   * The option page key for this preferences page.
   */
  public static final String DILA_DAMA_OPTIONS_PAGE_KEY = "dila_ai_markup_options_page_key";

  /**
   * Obsolete OpenAI-era option keys (FR-004). No UI row reads or writes these any more; the
   * constants remain only so the retirement tasks (T056a-T056c) can prove nothing still reads
   * them. Values left in a user's storage from a previous version are ignored, never migrated
   * (FR-005).
   */
  public static final String KEY_DILA_DAMA_FT_PARSE_MODEL = "dila.dama.ft.parse.model";
  public static final String KEY_DILA_DAMA_FT_DETECT_MODEL = "dila.dama.ft.detect.model";
  public static final String KEY_DILA_DAMA_API_KEY = "dila.dama.api.key";
  public static final String KEY_DILA_DAMA_API_BASE_URL = "dila.dama.api.base.url";
  public static final String KEY_DILA_DAMA_CHAT_COMPLETIONS_PATH = "dila.dama.api.chat.path";
  public static final String KEY_DILA_DAMA_API_TIMEOUT_MS = "dila.dama.api.timeout";

  /**
   * CBRD (CBETA Reference Detection) API options for Ref-to-Link action.
   */
  public static final String KEY_CBRD_API_URL = "cbrd.api.url";
  public static final String KEY_CBRD_REFERER_HEADER = "cbrd.referer.header";
  public static final String KEY_CBRD_TIMEOUT_MS = "cbrd.timeout";

  /**
   * CBRD Parse API options for the AI Markup action (004-cbrd-parse-endpoint).
   * The token is stored via the secure option storage; the URL and timeout are plain options.
   */
  public static final String KEY_CBRD_PARSE_API_URL = "cbrd.parse.api.url";
  public static final String KEY_CBRD_PARSE_TOKEN = "cbrd.parse.token";
  public static final String KEY_CBRD_PARSE_TIMEOUT_MS = "cbrd.parse.timeout";

  public static final String DEFAULT_CBRD_PARSE_API_URL = "https://cbss.dila.edu.tw/cbrd/parse";
  /**
   * The parse endpoint runs a model transformation server-side, so it inherits the previous
   * AI Markup timeout rather than the shorter Ref-to-Link lookup timeout (FR-016).
   */
  public static final String DEFAULT_CBRD_PARSE_TIMEOUT_MS = "30000";

  private static final String DEFAULT_CBRD_API_URL = "https://cbss.dila.edu.tw/cbrd/link";
  private static final String DEFAULT_CBRD_REFERER_HEADER = "CBRD@dila.edu.tw";
  private static final String DEFAULT_CBRD_TIMEOUT_MS = "10000";

  /**
   * Safe fallback values to prevent null pointer exceptions in Oxygen's options system.
   */
  private static final String SAFE_KEY_FALLBACK = "dila_safe_fallback";
  private static final String SAFE_VALUE_FALLBACK = "";

  /**
   * The text field for the CBRD API URL.
   */
  private JTextField cbrdApiUrlTextField;

  /**
   * The text field for the CBRD Referer header value.
   */
  private JTextField cbrdRefererTextField;

  /**
   * The text field for the CBRD timeout (ms).
   */
  private JTextField cbrdTimeoutTextField;

  /**
   * The text field for the CBRD Parse endpoint URL.
   */
  private JTextField cbrdParseApiUrlTextField;

  /**
   * The masked field for the shared DILA parse token. Masked and stored via the secure option
   * storage so the token is never displayed (FR-003, NFR-004).
   */
  private JPasswordField cbrdParseTokenTextField;

  /**
   * The text field for the CBRD Parse request timeout (ms).
   */
  private JTextField cbrdParseTimeoutTextField;

  /**
   * Resource bundle for i18n support.
   */
  private PluginResourceBundle resources;

  /**
   * Constructor - Initialize resource bundle early to ensure it's available for getTitle()
   */
  public DAMAOptionPagePluginExtension() {
    super();
  }

  /**
   * @see ro.sync.exml.plugin.option.OptionPagePluginExtension#apply(ro.sync.exml.workspace.api.PluginWorkspace)
   */
  @Override
  public void apply(PluginWorkspace pluginWorkspace) {
    try {
      PluginLogger.info(DAMAOptionPagePluginExtension.class, "Applying option page settings...");

      // Ultra-defensive checks for system-wide preferences saving
      if (pluginWorkspace == null) {
        PluginLogger.info(DAMAOptionPagePluginExtension.class, "PluginWorkspace is null in apply()");
        return; // Silently return without error
      }

      if (pluginWorkspace.getOptionsStorage() == null) {
        PluginLogger.info(DAMAOptionPagePluginExtension.class, "OptionsStorage is null in apply()");
        return; // Silently return without error
      }

      // Additional check: ensure text fields were initialized
      if (cbrdParseApiUrlTextField == null || cbrdParseTokenTextField == null || cbrdParseTimeoutTextField == null) {
        PluginLogger.info(DAMAOptionPagePluginExtension.class, "Text fields not initialized in apply()");
        return; // Silently return without error
      }

      // CBRD options (non-secret)
      String cbrdApiUrl = (cbrdApiUrlTextField != null && cbrdApiUrlTextField.getText() != null)
          ? cbrdApiUrlTextField.getText().trim()
          : DEFAULT_CBRD_API_URL;
      String cbrdReferer = (cbrdRefererTextField != null && cbrdRefererTextField.getText() != null)
          ? cbrdRefererTextField.getText().trim()
          : DEFAULT_CBRD_REFERER_HEADER;
      String cbrdTimeoutMs = (cbrdTimeoutTextField != null && cbrdTimeoutTextField.getText() != null)
          ? cbrdTimeoutTextField.getText().trim()
          : DEFAULT_CBRD_TIMEOUT_MS;

      if (cbrdApiUrl == null || cbrdApiUrl.isEmpty()) {
        cbrdApiUrl = DEFAULT_CBRD_API_URL;
      }
      if (cbrdReferer == null || cbrdReferer.isEmpty()) {
        cbrdReferer = DEFAULT_CBRD_REFERER_HEADER;
      }
      int timeoutParsed = parsePositiveIntOrDefault(cbrdTimeoutMs, DEFAULT_CBRD_TIMEOUT_MS);

      if (KEY_CBRD_API_URL != null && !KEY_CBRD_API_URL.trim().isEmpty()) {
        try {
          pluginWorkspace.getOptionsStorage().setOption(KEY_CBRD_API_URL, cbrdApiUrl);
        } catch (Exception e) {
          PluginLogger.error(DAMAOptionPagePluginExtension.class, "Failed to set CBRD API URL: " + e.getMessage());
        }
      }
      if (KEY_CBRD_REFERER_HEADER != null && !KEY_CBRD_REFERER_HEADER.trim().isEmpty()) {
        try {
          pluginWorkspace.getOptionsStorage().setOption(KEY_CBRD_REFERER_HEADER, cbrdReferer);
        } catch (Exception e) {
          PluginLogger.error(DAMAOptionPagePluginExtension.class, "Failed to set CBRD Referer header: " + e.getMessage());
        }
      }
      if (KEY_CBRD_TIMEOUT_MS != null && !KEY_CBRD_TIMEOUT_MS.trim().isEmpty()) {
        try {
          pluginWorkspace.getOptionsStorage().setOption(KEY_CBRD_TIMEOUT_MS, String.valueOf(timeoutParsed));
        } catch (Exception e) {
          PluginLogger.error(DAMAOptionPagePluginExtension.class, "Failed to set CBRD timeout: " + e.getMessage());
        }
      }

      // CBRD Parse options for AI Markup (004-cbrd-parse-endpoint)
      String cbrdParseApiUrl = (cbrdParseApiUrlTextField.getText() != null)
          ? cbrdParseApiUrlTextField.getText().trim()
          : DEFAULT_CBRD_PARSE_API_URL;
      if (cbrdParseApiUrl.isEmpty()) {
        cbrdParseApiUrl = DEFAULT_CBRD_PARSE_API_URL;
      }
      String cbrdParseToken = (cbrdParseTokenTextField.getPassword() != null)
          ? new String(cbrdParseTokenTextField.getPassword()).trim()
          : SAFE_VALUE_FALLBACK;
      String cbrdParseTimeoutText = (cbrdParseTimeoutTextField.getText() != null)
          ? cbrdParseTimeoutTextField.getText().trim()
          : DEFAULT_CBRD_PARSE_TIMEOUT_MS;
      int cbrdParseTimeoutParsed = parsePositiveIntOrDefault(cbrdParseTimeoutText, DEFAULT_CBRD_PARSE_TIMEOUT_MS);

      if (KEY_CBRD_PARSE_API_URL != null && !KEY_CBRD_PARSE_API_URL.trim().isEmpty()) {
        try {
          pluginWorkspace.getOptionsStorage().setOption(KEY_CBRD_PARSE_API_URL, cbrdParseApiUrl);
        } catch (Exception e) {
          PluginLogger.error(DAMAOptionPagePluginExtension.class, "Failed to set CBRD Parse endpoint URL: " + e.getMessage());
        }
      }
      if (KEY_CBRD_PARSE_TOKEN != null && !KEY_CBRD_PARSE_TOKEN.trim().isEmpty()) {
        try {
          // Never log the value itself - only whether one is present (FR-003, NFR-004).
          PluginLogger.debug(DAMAOptionPagePluginExtension.class,
              "Setting CBRD Parse token: " + (cbrdParseToken.isEmpty() ? "[empty]" : "[***]"));
          pluginWorkspace.getOptionsStorage().setSecretOption(KEY_CBRD_PARSE_TOKEN, cbrdParseToken);
        } catch (Exception e) {
          PluginLogger.error(DAMAOptionPagePluginExtension.class, "Failed to set CBRD Parse token: " + e.getMessage());
        }
      }
      if (KEY_CBRD_PARSE_TIMEOUT_MS != null && !KEY_CBRD_PARSE_TIMEOUT_MS.trim().isEmpty()) {
        try {
          pluginWorkspace.getOptionsStorage().setOption(KEY_CBRD_PARSE_TIMEOUT_MS, String.valueOf(cbrdParseTimeoutParsed));
        } catch (Exception e) {
          PluginLogger.error(DAMAOptionPagePluginExtension.class, "Failed to set CBRD Parse timeout: " + e.getMessage());
        }
      }

      PluginLogger.info(DAMAOptionPagePluginExtension.class, "Options saved successfully");
    } catch (Exception e) {
      // Log the error to prevent crashes during option saving
      PluginLogger.error(DAMAOptionPagePluginExtension.class, "Error saving options: " + e.getMessage(), e);
    }
  }

  /**
   * @see ro.sync.exml.plugin.option.OptionPagePluginExtension#restoreDefaults()
   */
  @Override
  public void restoreDefaults() {
    try {
      // Reset the text fields values. Use safe fallback to prevent null issues.
      if (cbrdApiUrlTextField != null) {
        cbrdApiUrlTextField.setText(DEFAULT_CBRD_API_URL);
      }
      if (cbrdRefererTextField != null) {
        cbrdRefererTextField.setText(DEFAULT_CBRD_REFERER_HEADER);
      }
      if (cbrdTimeoutTextField != null) {
        cbrdTimeoutTextField.setText(DEFAULT_CBRD_TIMEOUT_MS);
      }
      if (cbrdParseApiUrlTextField != null) {
        cbrdParseApiUrlTextField.setText(DEFAULT_CBRD_PARSE_API_URL);
      }
      if (cbrdParseTokenTextField != null) {
        // The token has no default: it is provisioned per team, never revived (FR-005).
        cbrdParseTokenTextField.setText(SAFE_VALUE_FALLBACK);
      }
      if (cbrdParseTimeoutTextField != null) {
        cbrdParseTimeoutTextField.setText(DEFAULT_CBRD_PARSE_TIMEOUT_MS);
      }
    } catch (Exception e) {
      PluginLogger.error(DAMAOptionPagePluginExtension.class, "Error restoring defaults: " + e.getMessage(), e);
    }
  }

  /**
   * @see ro.sync.exml.plugin.option.OptionPagePluginExtension#getTitle()
   */
  @Override
  public String getTitle() {
    String title = DILA_DAMA_OPTIONS_PAGE_TITLE;
    PluginLogger.debug(DAMAOptionPagePluginExtension.class, "getTitle() returning: " + title);

    return title;
  }

  /**
   * Initialize the resource bundle for i18n support.
   * This method should be called early to ensure translations are available.
   */
  private void initializeResourceBundle(PluginWorkspace pluginWorkspace) {
    // Store the resource bundle as an instance variable
    // According to Oxygen SDK, cast to StandalonePluginWorkspace to access getResourceBundle()
    try {
      if (pluginWorkspace instanceof StandalonePluginWorkspace) {
        this.resources = ((StandalonePluginWorkspace) pluginWorkspace).getResourceBundle();
        if (this.resources != null) {
          PluginLogger.info(DAMAOptionPagePluginExtension.class, "Resource bundle loaded successfully");
          // Test the resource bundle with a known key
          try {
            String testMessage = this.resources.getMessage("language");
            PluginLogger.debug(DAMAOptionPagePluginExtension.class, "Test message for 'language': " + testMessage);
          } catch (Exception e) {
            PluginLogger.debug(DAMAOptionPagePluginExtension.class, "Test message failed: " + e.getMessage());
          }
        } else {
          PluginLogger.info(DAMAOptionPagePluginExtension.class, "Resource bundle is null - i18n translations may not work");
        }
      } else {
        PluginLogger.info(DAMAOptionPagePluginExtension.class, "PluginWorkspace is not StandalonePluginWorkspace - cannot access resource bundle");
        this.resources = null;
      }
    } catch (Exception e) {
      PluginLogger.error(DAMAOptionPagePluginExtension.class, "Failed to load resource bundle: " + e.getMessage(), e);
      this.resources = null;
    }
  }

  /**
   * @see ro.sync.exml.plugin.option.OptionPagePluginExtension#init(ro.sync.exml.workspace.api.PluginWorkspace)
   */
  @Override
  public JComponent init(final PluginWorkspace pluginWorkspace) {
    try {
      // Ultra-defensive initialization for system-wide preferences loading
      PluginLogger.info(DAMAOptionPagePluginExtension.class, "Initializing option page...");

      // Check for null workspace early
      if (pluginWorkspace == null) {
        PluginLogger.info(DAMAOptionPagePluginExtension.class, "PluginWorkspace is null in init()");
        return createSimplePanel("Plugin workspace not available");
      }

      // Check if options storage is available
      if (pluginWorkspace.getOptionsStorage() == null) {
        PluginLogger.info(DAMAOptionPagePluginExtension.class, "OptionsStorage is null in init()");
        return createSimplePanel("Options storage not available");
      }

      // Initialize resource bundle for i18n support
      initializeResourceBundle(pluginWorkspace);

      GridBagConstraints c = new GridBagConstraints();
      JPanel panel = new JPanel(new GridBagLayout());

      JLabel cbrdApiUrlLabel = new JLabel(getMessage("cbrd.api.url.label"));
      c.gridx = 0;
      c.gridy = 0;
      c.weightx = 0;
      c.weighty = 0;
      c.anchor = GridBagConstraints.WEST;
      panel.add(cbrdApiUrlLabel, c);

      cbrdApiUrlTextField = new JTextField();
      c.gridx ++;
      c.weightx = 1;
      c.fill = GridBagConstraints.HORIZONTAL;
      c.insets = new Insets(0, 5, 0, 5);
      panel.add(cbrdApiUrlTextField, c);

      c.gridx = 0;
      c.gridy ++;
      JLabel cbrdRefererLabel = new JLabel(getMessage("cbrd.referer.label"));
      panel.add(cbrdRefererLabel, c);

      cbrdRefererTextField = new JTextField();
      c.gridx ++;
      c.weightx = 1;
      c.fill = GridBagConstraints.HORIZONTAL;
      c.insets = new Insets(0, 5, 0, 5);
      panel.add(cbrdRefererTextField, c);

      c.gridx = 0;
      c.gridy ++;
      JLabel cbrdTimeoutLabel = new JLabel(getMessage("cbrd.timeout.ms.label"));
      panel.add(cbrdTimeoutLabel, c);

      cbrdTimeoutTextField = new JTextField();
      c.gridx ++;
      c.weightx = 1;
      c.fill = GridBagConstraints.HORIZONTAL;
      c.insets = new Insets(0, 5, 0, 5);
      panel.add(cbrdTimeoutTextField, c);

      c.gridx = 0;
      c.gridy ++;
      JLabel cbrdParseApiUrlLabel = new JLabel(getMessage("cbrd.parse.api.url.label"));
      panel.add(cbrdParseApiUrlLabel, c);

      cbrdParseApiUrlTextField = new JTextField();
      c.gridx ++;
      c.weightx = 1;
      c.fill = GridBagConstraints.HORIZONTAL;
      c.insets = new Insets(0, 5, 0, 5);
      panel.add(cbrdParseApiUrlTextField, c);

      c.gridx = 0;
      c.gridy ++;
      JLabel cbrdParseTokenLabel = new JLabel(getMessage("cbrd.parse.token.label"));
      panel.add(cbrdParseTokenLabel, c);

      cbrdParseTokenTextField = new JPasswordField();
      c.gridx ++;
      c.weightx = 1;
      c.fill = GridBagConstraints.HORIZONTAL;
      c.insets = new Insets(0, 5, 0, 5);
      panel.add(cbrdParseTokenTextField, c);

      c.gridx = 0;
      c.gridy ++;
      JLabel cbrdParseTimeoutLabel = new JLabel(getMessage("cbrd.parse.timeout.ms.label"));
      panel.add(cbrdParseTimeoutLabel, c);

      cbrdParseTimeoutTextField = new JTextField();
      c.gridx ++;
      c.weightx = 1;
      c.fill = GridBagConstraints.HORIZONTAL;
      c.insets = new Insets(0, 5, 0, 5);
      panel.add(cbrdParseTimeoutTextField, c);

      c.gridx = 0;
      c.gridy ++;
      c.gridwidth = 3;
      c.weightx = 1;
      c.weighty = 1;
      c.fill = GridBagConstraints.BOTH;
      panel.add(new JPanel(), c);

      // Get stored options with extra null safety
      String cbrdApiUrl = DEFAULT_CBRD_API_URL;
      String cbrdReferer = DEFAULT_CBRD_REFERER_HEADER;
      String cbrdTimeoutMs = DEFAULT_CBRD_TIMEOUT_MS;
      String cbrdParseApiUrl = DEFAULT_CBRD_PARSE_API_URL;
      String cbrdParseToken = SAFE_VALUE_FALLBACK;
      String cbrdParseTimeoutMs = DEFAULT_CBRD_PARSE_TIMEOUT_MS;

      if (pluginWorkspace.getOptionsStorage() != null) {
        if (KEY_CBRD_API_URL != null && !KEY_CBRD_API_URL.trim().isEmpty()) {
          try {
            String value = pluginWorkspace.getOptionsStorage().getOption(KEY_CBRD_API_URL, DEFAULT_CBRD_API_URL);
            cbrdApiUrl = (value != null && !value.trim().isEmpty()) ? value.trim() : DEFAULT_CBRD_API_URL;
          } catch (Exception e) {
            PluginLogger.error(DAMAOptionPagePluginExtension.class, "Failed to get CBRD API URL: " + e.getMessage());
            cbrdApiUrl = DEFAULT_CBRD_API_URL;
          }
        }

        if (KEY_CBRD_REFERER_HEADER != null && !KEY_CBRD_REFERER_HEADER.trim().isEmpty()) {
          try {
            String value = pluginWorkspace.getOptionsStorage().getOption(KEY_CBRD_REFERER_HEADER, DEFAULT_CBRD_REFERER_HEADER);
            cbrdReferer = (value != null && !value.trim().isEmpty()) ? value.trim() : DEFAULT_CBRD_REFERER_HEADER;
          } catch (Exception e) {
            PluginLogger.error(DAMAOptionPagePluginExtension.class, "Failed to get CBRD Referer header: " + e.getMessage());
            cbrdReferer = DEFAULT_CBRD_REFERER_HEADER;
          }
        }

        if (KEY_CBRD_TIMEOUT_MS != null && !KEY_CBRD_TIMEOUT_MS.trim().isEmpty()) {
          try {
            String value = pluginWorkspace.getOptionsStorage().getOption(KEY_CBRD_TIMEOUT_MS, DEFAULT_CBRD_TIMEOUT_MS);
            cbrdTimeoutMs = (value != null && !value.trim().isEmpty()) ? value.trim() : DEFAULT_CBRD_TIMEOUT_MS;
          } catch (Exception e) {
            PluginLogger.error(DAMAOptionPagePluginExtension.class, "Failed to get CBRD timeout: " + e.getMessage());
            cbrdTimeoutMs = DEFAULT_CBRD_TIMEOUT_MS;
          }
        }

        if (KEY_CBRD_PARSE_API_URL != null && !KEY_CBRD_PARSE_API_URL.trim().isEmpty()) {
          try {
            String value = pluginWorkspace.getOptionsStorage().getOption(KEY_CBRD_PARSE_API_URL, DEFAULT_CBRD_PARSE_API_URL);
            cbrdParseApiUrl = (value != null && !value.trim().isEmpty()) ? value.trim() : DEFAULT_CBRD_PARSE_API_URL;
          } catch (Exception e) {
            PluginLogger.error(DAMAOptionPagePluginExtension.class, "Failed to get CBRD Parse endpoint URL: " + e.getMessage());
            cbrdParseApiUrl = DEFAULT_CBRD_PARSE_API_URL;
          }
        }

        if (KEY_CBRD_PARSE_TOKEN != null && !KEY_CBRD_PARSE_TOKEN.trim().isEmpty()) {
          try {
            String value = pluginWorkspace.getOptionsStorage().getSecretOption(KEY_CBRD_PARSE_TOKEN, SAFE_VALUE_FALLBACK);
            cbrdParseToken = (value != null) ? value : SAFE_VALUE_FALLBACK;
          } catch (Exception e) {
            PluginLogger.error(DAMAOptionPagePluginExtension.class, "Failed to get CBRD Parse token: " + e.getMessage());
            cbrdParseToken = SAFE_VALUE_FALLBACK;
          }
        }

        if (KEY_CBRD_PARSE_TIMEOUT_MS != null && !KEY_CBRD_PARSE_TIMEOUT_MS.trim().isEmpty()) {
          try {
            String value = pluginWorkspace.getOptionsStorage().getOption(KEY_CBRD_PARSE_TIMEOUT_MS, DEFAULT_CBRD_PARSE_TIMEOUT_MS);
            cbrdParseTimeoutMs = (value != null && !value.trim().isEmpty()) ? value.trim() : DEFAULT_CBRD_PARSE_TIMEOUT_MS;
          } catch (Exception e) {
            PluginLogger.error(DAMAOptionPagePluginExtension.class, "Failed to get CBRD Parse timeout: " + e.getMessage());
            cbrdParseTimeoutMs = DEFAULT_CBRD_PARSE_TIMEOUT_MS;
          }
        }
      }

      // Initialize the text fields with the stored options (guaranteed non-null)
      if (cbrdApiUrlTextField != null) {
        cbrdApiUrlTextField.setText(cbrdApiUrl);
      }
      if (cbrdRefererTextField != null) {
        cbrdRefererTextField.setText(cbrdReferer);
      }
      if (cbrdTimeoutTextField != null) {
        cbrdTimeoutTextField.setText(cbrdTimeoutMs);
      }
      if (cbrdParseApiUrlTextField != null) {
        cbrdParseApiUrlTextField.setText(cbrdParseApiUrl);
      }
      if (cbrdParseTokenTextField != null) {
        cbrdParseTokenTextField.setText(cbrdParseToken);
      }
      if (cbrdParseTimeoutTextField != null) {
        cbrdParseTimeoutTextField.setText(cbrdParseTimeoutMs);
      }

      return panel;
    } catch (Exception e) {
      // Log the error and return a simple panel to prevent crashes
      PluginLogger.error(DAMAOptionPagePluginExtension.class, "Error initializing options page: " + e.getMessage(), e);
      return new JPanel(); // Return empty panel as fallback
    }
  }

  /**
   * Parses a timeout preference, falling back to the supplied default when the value is
   * missing, unparseable, or non-positive.
   */
  private int parsePositiveIntOrDefault(String value, String defaultValue) {
    int fallback = Integer.parseInt(defaultValue);
    if (value == null || value.trim().isEmpty()) {
      return fallback;
    }
    try {
      int parsed = Integer.parseInt(value.trim());
      return parsed > 0 ? parsed : fallback;
    } catch (Exception e) {
      return fallback;
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
        PluginLogger.error(DAMAOptionPagePluginExtension.class, "Error getting message for key '" + key + "': " + e.getMessage(), e);
      }
    }

    // Fallback translations matching the keys in our i18n/translation.xml
    switch (key) {
      case "cbrd.api.url.label":
        return "CBRD API URL:";
      case "cbrd.referer.label":
        return "CBRD Referer header:";
      case "cbrd.timeout.ms.label":
        return "CBRD timeout (ms):";
      case "cbrd.parse.api.url.label":
        return "CBRD Parse endpoint URL:";
      case "cbrd.parse.token.label":
        return "CBRD Parse token*:";
      case "cbrd.parse.timeout.ms.label":
        return "CBRD Parse timeout (ms):";
      case "preferences.page.title":
        return "(Fallback)DILA AI Markup Assistant Options";
      default:
        PluginLogger.debug(DAMAOptionPagePluginExtension.class, "Using key as fallback for: " + key);
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
        PluginLogger.info(DAMAOptionPagePluginExtension.class, "Option page key is null/empty, using fallback");
        return SAFE_KEY_FALLBACK;
      }
      return key;
    } catch (Exception e) {
      PluginLogger.error(DAMAOptionPagePluginExtension.class, "Error in getKey(): " + e.getMessage(), e);
      // Return a guaranteed non-null, non-empty string
      return SAFE_KEY_FALLBACK;
    }
  }
}
