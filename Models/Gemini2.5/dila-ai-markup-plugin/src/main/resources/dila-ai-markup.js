// Global variable for our logger instance.
var UTF8PrintStream = null;
var DEBUG = false; // default value

// Function to determine debug mode from environment variable or system property
function getDebugMode() {
    // Try to get DEBUG from environment variable first
    try {
        var debugEnv = Packages.java.lang.System.getenv("DILA_DEBUG");
        if (debugEnv != null) {
            DEBUG = String(debugEnv).toLowerCase() === "true";
        }
    } catch (e) {
        // Fallback to system property
        try {
            var debugProp = Packages.java.lang.System.getProperty("dila.debug");
            if (debugProp != null) {
                DEBUG = String(debugProp).toLowerCase() === "true";
            }
            } catch (e) {
            // Keep default value
            Packages.java.lang.System.err.println("Keep default value: " + DEBUG);
        }
    }
    return DEBUG;
}

/**
 * Logs a message to the System.err stream if in DEBUG mode.
 * Initializes the stream on first use.
 * @param {string} message The message to log.
 */
function logDebug(message) {
    if (DEBUG) {
        if (UTF8PrintStream === null) {
            // Initialize the stream.
            var PrintStream = Packages.java.io.PrintStream;
            UTF8PrintStream = new PrintStream(Packages.java.lang.System.err, true, "UTF-8");
        }
        var now = new Date();
        var timestamp = now.toLocaleString();
        UTF8PrintStream.println("[" + timestamp + "] " + message);
    }
}

// This is called by Oxygen when the plugin is initialized.
function applicationStarted(pluginWorkspaceAccess) {

    getDebugMode()

    logDebug("Starting DILA AI markup plugin");
    logDebug("debug mode: " + getDebugMode());

    var resources = pluginWorkspaceAccess.getResourceBundle();
    var optionsPageKey = "dila_ai_markup_options_page_key"; // should match the key in DAMAWorkspaceAccessOptionPagePluginExtension since there's no way to get it

    function i18nFn(key) {
        return resources.getMessage(key);
    }

    var optionStorage = pluginWorkspaceAccess.getOptionsStorage();
    if (optionStorage == null) {
        logDebug("No options storage available.");
    } else {
        logDebug("Options storage available.");
        
        // Fix: Handle Java String properly without conversion that breaks length() method
        var apiKey = optionStorage.getSecretOption("dila.dama.api.key", "");
        var maskedApiKey = "";
        if (apiKey != null && apiKey.length() > 0) {
            // Convert to JavaScript string only for the regex operation
            var jsApiKey = String(apiKey);
            maskedApiKey = jsApiKey.replace(/.(?=.{4})/g, '*');
        }
        logDebug("API Key (masked except last 4 chars): " + maskedApiKey);
    }

    // Add a view component customizer
    pluginWorkspaceAccess.addViewComponentCustomizer(new JavaAdapter(
        Packages.ro.sync.exml.workspace.api.standalone.ViewComponentCustomizer, {
            customizeView: function(viewInfo) {
                if ("dila.ai.markup.view" == viewInfo.getViewID()) {
          
                    // Menu for the view
                    var JMenuBar = Packages.javax.swing.JMenuBar;
                    var JMenu = Packages.javax.swing.JMenu;
                    var JMenuItem = Packages.javax.swing.JMenuItem;
                    var ImageIcon = Packages.javax.swing.ImageIcon;
                    var Box = Packages.javax.swing.Box;
                    
                    var menuBar = new JMenuBar();
                    var menuActions = new JMenu(i18nFn("actions.menu")); // "Actions"
                    var menuItemActionAIMarkup = new JMenuItem(i18nFn("ai.markup.action")); // load action from local
                    var menuItemActionTagRemoval = new JMenuItem(i18nFn("tag.removal.action")); // load action from local

                    // Create options menu with comprehensive icon loading
                    var menuOptions = new JMenu();
                    
                    // Ensure menuOptions is properly initialized before proceeding
                    if (menuOptions != null) {
                        var colorTheme = pluginWorkspaceAccess.getColorTheme();
                        logDebug("Color theme: " + colorTheme);
                        var darkTheme = (colorTheme != null && colorTheme.isDarkTheme());
                        logDebug("Dark theme: " + darkTheme);
                        var iconPath = "images/options.png"; // Default icon path
                        if (darkTheme) {
                            iconPath = "images/options_dark.png"; // Dark theme icon path
                        }
                        logDebug("jsDirURL: " + jsDirURL);
                        
                        try {
                            var iconURI = new Packages.java.net.URI(jsDirURL + iconPath);
                            logDebug("Icon URI: " + iconURI);
                            var iconFile = new Packages.java.io.File(iconURI);
                            
                            if (iconFile.exists()) {
                                // Load icon directly without color manipulation
                                var originalIcon = new ImageIcon(iconFile.getAbsolutePath());
                                
                                // Verify the icon loaded properly
                                if (originalIcon != null && originalIcon.getIconWidth() > 0 && originalIcon.getIconHeight() > 0) {
                                    menuOptions.setIcon(originalIcon);
                                    logDebug("Options icon loaded successfully from file: " + iconFile.getAbsolutePath() + 
                                            " (Size: " + originalIcon.getIconWidth() + "x" + originalIcon.getIconHeight() + ")");
                                } else {
                                    menuOptions.setText(i18nFn("preferences.action"));
                                    logDebug("Icon file exists but failed to load properly, using text fallback");
                                }
                            } else {
                                menuOptions.setText(i18nFn("preferences.action"));
                                logDebug("Icon file does not exist at path: " + iconFile.getAbsolutePath());
                            }
                        } catch (e) {
                            menuOptions.setText(i18nFn("preferences.action"));
                            logDebug("Error loading icon from file: " + e.message);
                        }

                        // Set tooltip text for accessibility - with null safety check
                        menuOptions.setToolTipText(i18nFn("preferences.action"));
                    } else {
                        logDebug("Error: menuOptions is null, cannot proceed with menu creation");
                        // Create a new instance if somehow it became null
                        menuOptions = new JMenu(i18nFn("preferences.action"));
                        if (menuOptions != null) {
                            menuOptions.setToolTipText(i18nFn("preferences.action"));
                        }
                    }
                    
                    var menuItemOption = new JMenuItem(i18nFn("preferences.action")); // go to options dialog

                    // Assemble the menu bar - Actions on left, Options on right
                    menuBar.add(menuActions);
                    menuActions.add(menuItemActionAIMarkup);
                    menuActions.add(menuItemActionTagRemoval);
                    
                    // Add horizontal glue to push Options menu to the right
                    menuBar.add(Box.createHorizontalGlue());
                    
                    menuBar.add(menuOptions);
                    menuOptions.add(menuItemOption);

                    // Plugin panel of BorderLayout with menu bar
                    var JPanel = Packages.javax.swing.JPanel;
                    var BorderLayout = Packages.java.awt.BorderLayout;
                    var Dimension = Packages.java.awt.Dimension;
                    var Toolkit = Packages.java.awt.Toolkit;
                    
                    var pluginPanel = new JPanel(new BorderLayout());
                    
                    // Set the width to 1/5 of the application width
                    var screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                    var panelWidth = Math.floor(screenSize.width / 5);
                    var panelHeight = screenSize.height; // Use full height
                    pluginPanel.setPreferredSize(new Dimension(panelWidth, panelHeight));
                    
                    pluginPanel.add(menuBar, BorderLayout.NORTH);

                    // Text areas for info and results
                    var JTextArea = Packages.javax.swing.JTextArea;
                    var JScrollPane = Packages.javax.swing.JScrollPane;
                    var JSplitPane = Packages.javax.swing.JSplitPane;

                    var infoArea = new JTextArea(i18nFn("initial.info"), 4, 0);
                    infoArea.setLineWrap(true);
                    infoArea.setWrapStyleWord(true);
                    infoArea.setEditable(false);

                    var resultArea = new JTextArea("", 8, 0);
                    resultArea.setLineWrap(true);
                    resultArea.setWrapStyleWord(true);
                    resultArea.setEditable(false);

                    // Button panel for the replace button
                    var JButton = Packages.javax.swing.JButton;
                    var buttonPanel = new JPanel();
                    var replaceButton = new JButton(i18nFn("replace.button")); // "Replace in Document"
                    buttonPanel.add(replaceButton);
                    buttonPanel.setComponentOrientation(Packages.java.awt.ComponentOrientation.RIGHT_TO_LEFT);
                    buttonPanel.setVisible(false);

                    // Bottom panel to hold the text areas and the button panel
                    var bottomPanel = new JPanel(new BorderLayout());
                    bottomPanel.add(new JScrollPane(resultArea), BorderLayout.CENTER);
                    bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
                    
                    // Split pane to divide info area and bottom panel
                    var splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, 
                                                   new JScrollPane(infoArea), 
                                                   bottomPanel);
                    splitPane.setDividerLocation(0.5);
                    splitPane.setResizeWeight(0.5);

                    // pluginPanel.add(cardPanel, BorderLayout.CENTER);
                    pluginPanel.add(splitPane, BorderLayout.CENTER); // modification for OptionPagePluginExtension

                    // Set the component and title for the view
                    viewInfo.setComponent(pluginPanel);
                    viewInfo.setTitle(i18nFn("view.title")); // "DILA AI Markup Assistant"

                    // Add action listeners to AI Markup menu item
                    menuItemActionAIMarkup.addActionListener(function() {
                        logDebug("AI Markup action triggered");

                        // Switch back to main view if options panel is currently showing
                        // cardLayout.show(cardPanel, "MAIN"); // Commented out - cardLayout not used

                        infoArea.setText(i18nFn("action.ai.markup.selected"));
                        var selectedText = fetchAndDisplaySelectedText(infoArea);

                        // Send selectedText to LLM and handle response
                        if (selectedText && selectedText.length() > 0) {
                             // Create and start a new background thread for the network request
                            var Thread = Packages.java.lang.Thread;
                            var SwingUtilities = Packages.javax.swing.SwingUtilities;

                            var networkThread = new Thread(function() {
                                try {
                                    // Make HTTP request to LLM API
                                    var URL = Packages.java.net.URL;
                                    var HttpURLConnection = Packages.java.net.HttpURLConnection;
                                    var BufferedReader = Packages.java.io.BufferedReader;
                                    var InputStreamReader = Packages.java.io.InputStreamReader;
                                    var OutputStreamWriter = Packages.java.io.OutputStreamWriter;
                                    
                                    var url = new URL("https://api.openai.com/v1/chat/completions"); // ft LLM endpoint
                                    var connection = url.openConnection();

                                    var apiKey = optionStorage.getSecretOption("dila.dama.api.key", "");
                                    var parseModel = optionStorage.getOption("dila.dama.ft.parse.model", "");

                                    connection.setRequestMethod("POST");
                                    connection.setRequestProperty("Content-Type", "application/json");
                                    connection.setRequestProperty("Authorization", "Bearer " + apiKey); // API key
                                    connection.setDoOutput(true);
                                    
                                    // Define the system prompt to guide the AI's response.
                                    var systemPrompt = i18nFn("system.prompt.ai.markup");

                                    // Prepare request payload
                                    var requestBody = JSON.stringify({
                                        "model": parseModel,
                                        "messages": [
                                            {"role": "system", "content": systemPrompt},
                                            {"role": "user", "content": selectedText}
                                        ],
                                        "max_tokens": 1000
                                    });
                                    
                                    // Send request
                                    var writer = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
                                    writer.write(requestBody);
                                    writer.flush();
                                    writer.close();
                                    
                                    // Check for non-successful response codes
                                    var responseCode = connection.getResponseCode();
                                    if (responseCode >= 200 && responseCode < 300) {
                                        // Read response on success
                                        var reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                                        var response = "";
                                        var line;
                                        while ((line = reader.readLine()) != null) {
                                            response += line;
                                        }
                                        reader.close();
                                        
                                        // Parse and display response
                                        var responseObj = JSON.parse(response);
                                        var llmResponse = responseObj.choices[0].message.content;

                                        // --- Schedule UI update back on the EDT ---
                                        SwingUtilities.invokeLater(function() {
                                            resultArea.setText("<ref>" + llmResponse + "</ref>");
                                            buttonPanel.setVisible(true);
                                            logDebug("API call successful.");
                                        });
                                        
                                } else {
                                        // Read error stream for non-successful responses
                                        var errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "UTF-8"));
                                        var errorResponse = "";
                                        var errorLine;
                                        while ((errorLine = errorReader.readLine()) != null) {
                                            errorResponse += errorLine;
                                        }
                                        errorReader.close();
                                        throw new Error("HTTP " + responseCode + ": " + errorResponse);
                                    }
                                    
                                } catch (error) {
                                    // --- Schedule error message update back on the EDT ---
                                    SwingUtilities.invokeLater(function() {
                                        infoArea.setText(i18nFn("llm.error") + "\n" + error.message);
                                        logDebug("LLM API Error: " + error);
                                    });
                                }
                            });
                            networkThread.start(); // Start the background thread
                        }
                    });

                    // Add action listener to the replace button
                    replaceButton.addActionListener(function() {
                        var replacementText = resultArea.getText();
                        var replacementTextLength = replacementText.length();
                        logDebug("Replace button clicked with replacement: " + replacementText + 
                            "\n(length: " + replacementTextLength + ")");
                        var classNameOfReplacementText = replacementText.getClass().getName();
                        if (classNameOfReplacementText != 'number') {
                            logDebug("Class name of replacementTextLength: " + classNameOfReplacementText);
                        }

                        if (replacementText && replacementTextLength > 0) {
                            // Get the current editor access
                            logDebug("With replacement: " + replacementText);
                            var editorAccess = pluginWorkspaceAccess.getCurrentEditorAccess(Packages.ro.sync.exml.workspace.api.PluginWorkspace.MAIN_EDITING_AREA);
                            if (editorAccess != null) {
                                var pageAccess = editorAccess.getCurrentPage();
                                logDebug("With page access: " + pageAccess);
                                if (pageAccess != null && pageAccess instanceof Packages.ro.sync.exml.workspace.api.editor.page.text.WSTextEditorPage) {

                                    try {
                                        // Get the current selection range
                                        var selectionStart = pageAccess.getSelectionStart();
                                        var selectionEnd = pageAccess.getSelectionEnd();
                                        var theDocument = pageAccess.getDocument();
                                        logDebug("Start: " + selectionStart + ", End: " + selectionEnd + ", Document: " + theDocument);
                                        //perform the replacement on the EDT
                                        if (selectionStart != selectionEnd) {
                                            pageAccess.beginCompoundUndoableEdit();
                                            try {
                                                logDebug("Replacing text in document...");
                                                pageAccess.deleteSelection();
                                                // theDocument.remove(selectionStart, selectionEnd);
                                                logDebug("Original text removed");
                                                // theDocument.insertString(selectionStart, replacementText, null);
                                                theDocument.insertString(selectionStart, replacementText, null);
                                                logDebug("Text replaced in document.");
                                                infoArea.append(i18nFn("text.replaced"));
                                            } finally {
                                                pageAccess.endCompoundUndoableEdit();
                                                buttonPanel.setVisible(false);
                                            }
                                        } else {
                                            infoArea.append(i18nFn("text.mode.not.active.for.replace"));
                                        }
                                    } catch (error) {
                                        infoArea.append(i18nFn("error.replacing.text", error.message));
                                        logDebug("Error during text replacement: " + error);
                                    }
                                }
                            } else {
                                infoArea.append(i18nFn("text.editor.not.open"));
                            }
                        } else {
                            infoArea.append(i18nFn("no.text.to.replace"));
                        }
                    });

                    // Add action listener to the Tag Removal menu item
                    menuItemActionTagRemoval.addActionListener(function() {
                        logDebug("Tag Removal action triggered");

                        infoArea.setText(i18nFn("action.tag.removal.selected"));
                        var selectedText = fetchAndDisplaySelectedText(infoArea);
                        if (selectedText && selectedText.length() > 0) {
                            // Convert the Java String to a JavaScript string to use the regex-capable replace() method.
                            var cleanedText = new String(selectedText).replace(/<[^>]*>/g, '');
                            resultArea.setText(cleanedText);
                            buttonPanel.setVisible(true);
                        } else {
                            // Hide button panel if no text selected
                            buttonPanel.setVisible(false);
                        }
                    });

                    // Add action listener to the Preferences menu item
                    menuItemOption.addActionListener(function() {
                        try {
                            logDebug("Settings option triggered");
                            buttonPanel.setVisible(false);

                            // Use proper Java array creation instead of JavaScript array syntax
                            var StringArray = Packages.java.lang.reflect.Array.newInstance(Packages.java.lang.String, 1);
                            StringArray[0] = optionsPageKey;
                            pluginWorkspaceAccess.showPreferencesPages(StringArray, optionsPageKey, true);
                        } catch (e) {
                            logDebug("Error opening preferences: " + e);
                        }
                   });
                }
            }
        }
    ));

    function fetchAndDisplaySelectedText(area) {
        var editorAccess = pluginWorkspaceAccess.getCurrentEditorAccess(Packages.ro.sync.exml.workspace.api.PluginWorkspace.MAIN_EDITING_AREA);
        if (editorAccess != null) {
            var pageAccess = editorAccess.getCurrentPage();
            if (pageAccess != null && pageAccess instanceof Packages.ro.sync.exml.workspace.api.editor.page.text.WSTextEditorPage) {
                var textPageAccess = pageAccess;
                var selectedText = textPageAccess.getSelectedText();
                if (selectedText != null && selectedText.length() > 0) {
                    area.append(i18nFn("selected.text.label") + selectedText);
                    return selectedText;
                } else {
                    area.append(i18nFn("no.text.selected"));
                    return "";
                }
            } else {
                area.append(i18nFn("not.text.mode"));
                return "";
            }
        } else {
            area.append(i18nFn("no.editor.open"));
            return "";
        }
    }

}

// This is called by Oxygen when the plugin is being disposed.
function applicationClosing() {

    // Clean up resources if needed.
    getDebugMode()
    logDebug("Closing DILA AI Markup plugin.");
    logDebug("debug mode: " + getDebugMode());
}
