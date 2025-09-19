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

    // Now we can safely call getDescriptor()
    var optionsXMLUrl = jsDirURL.toString().substring(6) + "DAMAOptions.xml"; 
    logDebug("jsDirURL: " + jsDirURL);
    logDebug("jsDirURL 2 string: " + jsDirURL.toString());
    logDebug("clean jsDirURL: " + jsDirURL.toString().substring(6));
    logDebug("optionsDirPath: " + optionsXMLUrl);
    var optionsFile = new Packages.java.io.File(optionsXMLUrl);
    logDebug("optionsFile: " + optionsFile);

    /**
     * Imports options from an XML file and sets them in the options panel fields.
     * @param {java.io.File} optionsFile The XML file containing the options.
     * @param {javax.swing.JTextField} parseModelField The text field for the parse model.
     * @param {javax.swing.JTextField} detectModelField The text field for the detect model.
     * @param {javax.swing.JPasswordField} apiKeyField The password field for the API key.
     */
    function importDAMAOptions(optionsFile, parseModelField, detectModelField, apiKeyField) {
        logDebug("\nimportDAMAOptions");
        if (optionsFile != null && optionsFile.exists()) {
            try {
                var DocumentBuilderFactory = Packages.javax.xml.parsers.DocumentBuilderFactory;
                var factory = DocumentBuilderFactory.newInstance();
                var builder = factory.newDocumentBuilder();
                var doc = builder.parse(optionsFile);
                doc.getDocumentElement().normalize();

                var fields = doc.getElementsByTagName("field");
                for (var i = 0; i < fields.getLength(); i++) {
                    var field = fields.item(i);
                    var fieldName = field.getAttribute("name");
                    var valueNode = field.getElementsByTagName("string").item(0);
                    if (valueNode) {
                        var fieldValue = valueNode.getTextContent();
                        if (fieldName == "ft.parse.model") {
                            parseModelField.setText(fieldValue);
                        } else if (fieldName == "ft.detect.model") {
                            detectModelField.setText(fieldValue);
                        } else if (fieldName == "api.key") {
                            // The API key is stored in Base64 for simple obfuscation. Decode it.
                            // WARNING: This is not true encryption.
                            var Base64 = Packages.java.util.Base64;
                            logDebug("fieldValue: " + fieldValue);
                            var decodedBytes = Base64.getDecoder().decode(fieldValue);
                            logDebug("decodedBytes: " + decodedBytes);
                            apiKeyField.setText(new Packages.java.lang.String(decodedBytes));
                            logDebug("apiKeyField after convert: " + apiKeyField.getText());
                        }
                    }
                }
                logDebug("Successfully imported options from DAMAOptions.xml");
            } catch (e) {
                logDebug("Error importing DAMAOptions.xml: " + e);
            }
        } else {
            logDebug("DAMAOptions.xml not found, using default values.");
        }
    }

    /**
     * Saves the current options to the XML file.
     * @param {java.io.File} optionsFile The XML file to save the options to.
     * @param {javax.swing.JTextField} parseModelField The text field for the parse model.
     * @param {javax.swing.JTextField} detectModelField The text field for the detect model.
     * @param {javax.swing.JPasswordField} apiKeyField The password field for the API key.
     */
    function saveDAMAOptions(optionsFile, parseModelField, detectModelField, apiKeyField) {
        try {
            // Ensure parent directory exists
            var parentDir = optionsFile.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            logDebug("\nsaveDAMAOptions");

            var DocumentBuilderFactory = Packages.javax.xml.parsers.DocumentBuilderFactory;
            var DocumentBuilder = Packages.javax.xml.parsers.DocumentBuilder;
            var TransformerFactory = Packages.javax.xml.transform.TransformerFactory;
            var Transformer = Packages.javax.xml.transform.Transformer;
            var DOMSource = Packages.javax.xml.transform.dom.DOMSource;
            var StreamResult = Packages.javax.xml.transform.stream.StreamResult;
            var OutputKeys = Packages.javax.xml.transform.OutputKeys;

            var docFactory = DocumentBuilderFactory.newInstance();
            var docBuilder = docFactory.newDocumentBuilder();

            // Create the root element
            var doc = docBuilder.newDocument();
            var serialized = doc.createElement("serialized");
            serialized.setAttribute("version", "18.0");
            serialized.setAttribute("xml:space", "preserve");
            doc.appendChild(serialized);

            var map = doc.createElement("map");
            serialized.appendChild(map);

            var entry = doc.createElement("entry");
            map.appendChild(entry);

            var stringKey = doc.createElement("String");
            stringKey.appendChild(doc.createTextNode("dila.ai.markup.assistant.options"));
            entry.appendChild(stringKey);

            var options = doc.createElement("dilaAIMarkupAssistantOptions");
            entry.appendChild(options);

            // Helper to create a field element
            function createField(name, value) {
                var field = doc.createElement("field");
                field.setAttribute("name", name);
                var stringValue = doc.createElement("string");
                stringValue.appendChild(doc.createTextNode(value));
                field.appendChild(stringValue);
                return field;
            }

            // Get values and create fields
            options.appendChild(createField("ft.parse.model", parseModelField.getText()));
            options.appendChild(createField("ft.detect.model", detectModelField.getText()));

            // WARNING: Storing API keys, even obfuscated, in a config file is a security risk.
            // Here we use Base64 as a simple deterrent, not as a secure encryption method.
            var Base64 = Packages.java.util.Base64;
            var passwordChars = apiKeyField.getPassword();
            logDebug("class name of passwordChars: " + passwordChars.getClass().getName());
            logDebug("passwordChars: " + passwordChars);
            var javaString = new Packages.java.lang.String(passwordChars);
            var encodedApiKey = Base64.getEncoder().encodeToString(javaString.getBytes("UTF-8"));
            logDebug("encodedApiKey: " + encodedApiKey);

            options.appendChild(createField("api.key", encodedApiKey));

            // Write the content into xml file
            var transformerFactory = TransformerFactory.newInstance();
            var transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            var source = new DOMSource(doc);
            var result = new StreamResult(optionsFile);
            transformer.transform(source, result);

            logDebug("Successfully saved options to DAMAOptions.xml");
            return true;
        } catch (e) {
            logDebug("Error saving DAMAOptions.xml: " + e);
            return false;
        }
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

                    // Plugin panel of BorderLayout with menu bar
                    var JPanel = Packages.javax.swing.JPanel;
                    var BorderLayout = Packages.java.awt.BorderLayout;
                    var pluginPanel = new JPanel(new BorderLayout());
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

                    // Options panel with model and API key configuration
                    var optionsPanel = new JPanel();
                    var BorderFactory = Packages.javax.swing.BorderFactory;
                    var TitledBorder = Packages.javax.swing.border.TitledBorder;
                    var titledBorder = BorderFactory.createTitledBorder(i18nFn("options.panel.title"));
                    titledBorder.setTitlePosition(TitledBorder.BELOW_TOP);
                    optionsPanel.setBorder(titledBorder);

                    var GridBagLayout = Packages.java.awt.GridBagLayout;
                    var GridBagConstraints = Packages.java.awt.GridBagConstraints;
                    var JLabel = Packages.javax.swing.JLabel;
                    var JTextField = Packages.javax.swing.JTextField;
                    var Insets = Packages.java.awt.Insets;

                    optionsPanel.setLayout(new GridBagLayout());
                    var gbc = new GridBagConstraints();
                    gbc.insets = new Insets(5, 5, 5, 5);
                    gbc.anchor = GridBagConstraints.WEST;

                    // ft parse model configuration
                    gbc.gridx = 0; gbc.gridy = 0;
                    optionsPanel.add(new JLabel(i18nFn("ft.parse.model.label")), gbc); // "解析參照用預訓練模型"
                    gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
                    var parseModelField = new JTextField(20);
                    parseModelField.setName("ft.parse.model");
                    optionsPanel.add(parseModelField, gbc);

                    // ft detect model configuration
                    gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
                    optionsPanel.add(new JLabel(i18nFn("ft.detect.model.label")), gbc); // "偵測參照用預訓練模型"
                    gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
                    var detectModelField = new JTextField(20);
                    detectModelField.setName("ft.detect.model");
                    optionsPanel.add(detectModelField, gbc);

                    // API Key configuration
                    gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
                    optionsPanel.add(new JLabel(i18nFn("api.key.label")), gbc); // "API Key"
                    gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
                    var JPasswordField = Packages.javax.swing.JPasswordField;
                    var apiKeyField = new JPasswordField(40);
                    apiKeyField.setName("api.key");
                    optionsPanel.add(apiKeyField, gbc);

                    // Save button
                    gbc.gridy = 3;
                    gbc.gridx = 1;
                    gbc.fill = GridBagConstraints.NONE;
                    gbc.anchor = GridBagConstraints.EAST;
                    var saveButton = new JButton(i18nFn("save.button"));
                    optionsPanel.add(saveButton, gbc);

                    // Filler panel to push everything to the top
                    gbc.gridy = 4;
                    gbc.gridx = 0;
                    gbc.gridwidth = 2; // Span across both columns
                    gbc.weightx = 1.0; // Take full horizontal space
                    gbc.weighty = 1.0;
                    gbc.fill = GridBagConstraints.BOTH; // Fill both horizontal and vertical space
                    
                    var optionInfoArea = new JTextArea(i18nFn("options.info"), 4, 20);
                    optionInfoArea.setLineWrap(true);
                    optionInfoArea.setWrapStyleWord(true);
                    optionInfoArea.setEditable(false);
                    optionsPanel.add(optionInfoArea, gbc);

                    
                    // Card container that can switch between splitPane and optionsPanel
                    var CardLayout = Packages.java.awt.CardLayout;
                    var cardPanel = new JPanel(new CardLayout());
                    cardPanel.add(splitPane, "MAIN");
                    cardPanel.add(optionsPanel, "OPTIONS");
                    
                    var cardLayout = cardPanel.getLayout();

                    pluginPanel.add(cardPanel, BorderLayout.CENTER);

                    // Set the component and title for the view
                    viewInfo.setComponent(pluginPanel);
                    viewInfo.setTitle(i18nFn("view.title")); // "DILA AI Markup Assistant"

                    importDAMAOptions(optionsFile, parseModelField, detectModelField, apiKeyField);
                    logDebug("apiKeyField: " + apiKeyField.getText());

                    // Add action listener for the save button
                    saveButton.addActionListener(function() {
                        var saved = saveDAMAOptions(optionsFile, parseModelField, detectModelField, apiKeyField);
                        if (saved) {
                            optionInfoArea.setText(i18nFn("options.saved.successfully")); // "Options saved successfully."
                        } else {
                            optionInfoArea.setText(i18nFn("options.saved.failed")); // "Failed to save options."
                        }
                    });

                    // Add action listeners to menu items
                    menuItemActionAIMarkup.addActionListener(function() {
                        logDebug("AI Markup action triggered");

                        // Switch back to main view if options panel is currently showing
                        cardLayout.show(cardPanel, "MAIN");

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
                            logDebug("\nClass name of replacementTextLength: " + classNameOfReplacementText);
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

                    menuItemActionTagRemoval.addActionListener(function() {
                        logDebug("Tag Removal action triggered");

                        // Switch back to main view if options panel is currently showing
                        cardLayout.show(cardPanel, "MAIN");

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
                    menuItemOption.addActionListener(function() {
                        logDebug("Settings option triggered");
                        buttonPanel.setVisible(false);

                        // Switch to options view
                        cardLayout.show(cardPanel, "OPTIONS");
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
            area.append(i18nFn("no.editor.open"));
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
