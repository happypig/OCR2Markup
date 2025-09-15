// This is called by Oxygen when the plugin is initialized.
function applicationStarted(pluginWorkspaceAccess) {

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
            UTF8PrintStream.println(message);
        }
    }

    getDebugMode()

    logDebug("Starting DILA AI markup plugin");
    logDebug("debug mode: " + getDebugMode());

    var resources = pluginWorkspaceAccess.getResourceBundle();

    function i18nFn(key) {
        return resources.getMessage(key);
    }

    // Add a view component customizer
    pluginWorkspaceAccess.addViewComponentCustomizer(new JavaAdapter(
        Packages.ro.sync.exml.workspace.api.standalone.ViewComponentCustomizer, {
            customizeView: function(viewInfo) {
                if ("dila.ai.markup.view" == viewInfo.getViewID()) {
          
                    // Create a simple UI for the view
                    var JMenuBar = Packages.javax.swing.JMenuBar;
                    var JMenu = Packages.javax.swing.JMenu;
                    var JMenuItem = Packages.javax.swing.JMenuItem;
                    var menuBar = new JMenuBar();
                    var menuActions = new JMenu(i18nFn("actions.menu")); // "Actions"
                    var menuItemActionAIMarkup = new JMenuItem(i18nFn("ai.markup.action")); // load action from local
                    var menuItemActionTagRemoval = new JMenuItem(i18nFn("tag.removal.action")); // load action from local
                    var menuOptions = new JMenu(i18nFn("options.menu")); // "Options"
                    var menuItemOption = new JMenuItem(i18nFn("preferences.action")); // go to options dialog

                    // Assemble the menu bar
                    menuBar.add(menuActions);
                    menuActions.add(menuItemActionAIMarkup);
                    menuActions.add(menuItemActionTagRemoval);
                    menuBar.add(menuOptions);
                    menuOptions.add(menuItemOption);

                    // Main panel with BorderLayout
                    var JPanel = Packages.javax.swing.JPanel;
                    var BorderLayout = Packages.java.awt.BorderLayout;
                    var mainPanel = new JPanel(new BorderLayout());
                    mainPanel.add(menuBar, BorderLayout.NORTH);

                    // Text area in the center
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
                    
                    var splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, 
                                                   new JScrollPane(infoArea), 
                                                   new JScrollPane(resultArea));
                    splitPane.setDividerLocation(0.5);
                    splitPane.setResizeWeight(0.5);

                    // Create options panel with model and API key configuration
                    var optionsPanel = new JPanel();
                    var GridBagLayout = Packages.java.awt.GridBagLayout;
                    var GridBagConstraints = Packages.java.awt.GridBagConstraints;
                    var JLabel = Packages.javax.swing.JLabel;
                    var JTextField = Packages.javax.swing.JTextField;
                    var Insets = Packages.java.awt.Insets;

                    optionsPanel.setLayout(new GridBagLayout());
                    var gbc = new GridBagConstraints();
                    gbc.insets = new Insets(5, 5, 5, 5);
                    gbc.anchor = GridBagConstraints.WEST;

                    // Model 1 configuration
                    gbc.gridx = 0; gbc.gridy = 0;
                    optionsPanel.add(new JLabel(i18nFn("model1.label")), gbc);
                    gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
                    var model1Field = new JTextField(20);
                    optionsPanel.add(model1Field, gbc);

                    // Model 2 configuration
                    gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
                    optionsPanel.add(new JLabel(i18nFn("model2.label")), gbc);
                    gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
                    var model2Field = new JTextField(20);
                    optionsPanel.add(model2Field, gbc);

                    // API Key configuration
                    gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
                    optionsPanel.add(new JLabel(i18nFn("api.key.label")), gbc);
                    gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
                    var apiKeyField = new JTextField(20);
                    optionsPanel.add(apiKeyField, gbc);

                    // Initially hide the options panel
                    // optionsPanel.setVisible(true);
                    
                    mainPanel.add(splitPane, BorderLayout.CENTER);

                    var JButton = Packages.javax.swing.JButton;
                    var buttonPanel = new JPanel();
                    var replaceButton = new JButton(i18nFn("replace.button")); // "Replace in Document"
                    buttonPanel.add(replaceButton);
                    buttonPanel.setComponentOrientation(Packages.java.awt.ComponentOrientation.RIGHT_TO_LEFT);
                    mainPanel.add(buttonPanel, BorderLayout.SOUTH);
                    buttonPanel.setVisible(false);

                    // Set the component and title for the view
                    viewInfo.setComponent(mainPanel);
                    viewInfo.setTitle(i18nFn("view.title")); // "DILA AI Markup Assistant"

                    // Create a container that can switch between splitPane and optionsPanel
                    var CardLayout = Packages.java.awt.CardLayout;
                    var cardPanel = new JPanel(new CardLayout());
                    cardPanel.add(splitPane, "MAIN");
                    cardPanel.add(optionsPanel, "OPTIONS");
                    
                    var cardLayout = cardPanel.getLayout();

                    // Add action listeners to menu items
                    menuItemActionAIMarkup.addActionListener(function(e) {
                        logDebug("AI Markup action triggered");

                                                // Switch back to main view if options panel is currently showing
                        if (optionsPanel.isVisible()) {
                            cardLayout.show(cardPanel, "MAIN");
                            mainPanel.remove(cardPanel);
                            mainPanel.add(splitPane, BorderLayout.CENTER);
                            mainPanel.revalidate();
                            mainPanel.repaint();
                        }

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

                                    var API_KEY = apiKeyField.getText().trim();

                                    connection.setRequestMethod("POST");
                                    connection.setRequestProperty("Content-Type", "application/json");
                                    connection.setRequestProperty("Authorization", "Bearer " + API_KEY); // API key
                                    connection.setDoOutput(true);
                                    
                                    // Define the system prompt to guide the AI's response.
                                    var systemPrompt = i18nFn("system.prompt.ai.markup");

                                    // Prepare request payload
                                    var requestBody = JSON.stringify({
                                        "model": "ft:gpt-4.1-mini-2025-04-14:dila::CBFXGs2r",
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
                                            logDebug("\nAPI call successful.");
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
                                        infoArea.setText(i18nFn("llm.error", error.message));
                                        logDebug("LLM API Error: " + error);
                                    });
                                }
                            });
                            networkThread.start(); // Start the background thread
                        }
                    });

                    // Add action listener to the replace button
                    replaceButton.addActionListener(function(e) {
                        var replacementText = resultArea.getText();
                        var replacementTextLength = replacementText.length();
                        logDebug("Replace button clicked with replacement: " + replacementText + 
                            "\n(length: " + replacementTextLength + ")");
                        var typeOfReplacementText = typeof replacementText;
                        if (typeof replacementTextLength != 'number') {
                            logDebug("\nType of replacementTextLength: " + typeOfReplacementText);
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

                    menuItemActionTagRemoval.addActionListener(function(e) {
                        logDebug("Tag Removal action triggered");

                        // Switch back to main view if options panel is currently showing
                        if (optionsPanel.isVisible()) {
                            cardLayout.show(cardPanel, "MAIN");
                            mainPanel.remove(cardPanel);
                            mainPanel.add(splitPane, BorderLayout.CENTER);
                            mainPanel.revalidate();
                            mainPanel.repaint();
                        }

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
                    menuItemOption.addActionListener(function(e) {
                        logDebug("Settings option triggered");
                        // infoArea.setText(i18nFn("action.settings.selected"));
                        // pluginWorkspaceAccess.showOptionsDialog("DILA AI Markup Assistant Settings", "dila.ai.markup.assistant");
                        buttonPanel.setVisible(false);

                        // Switch to options view only if main panel is currently shown
                        if (!optionsPanel.isVisible()) {
                            cardLayout.show(cardPanel, "OPTIONS");
                            mainPanel.remove(splitPane);
                            mainPanel.add(cardPanel, BorderLayout.CENTER);
                            mainPanel.revalidate();
                            mainPanel.repaint();
                        }

                   });
                }
            }
        }
    ))

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
            area.append(i18nFn("no.editor.ope"));
            return "";
        }
    }
}



// This is called by Oxygen when the plugin is being disposed.
function applicationClosing() {
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
            UTF8PrintStream.println(message);
        }
    }

    // Clean up resources if needed.
    getDebugMode()
    logDebug("\nClosing DILA AI Markup plugin.");
    logDebug("debug mode: " + getDebugMode());
}