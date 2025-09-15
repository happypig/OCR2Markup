// Global variable for our logger instance.
var UTF8PrintStream = null;
var DEBUG = false; // default value

// Function to determine debug mode from environment variable or system property
function getDebugMode() {
    // Try to get DEBUG from system property first
    try {
        var debugProp = Packages.java.lang.System.getProperty("dila.debug");
        if (debugProp != null) {
            DEBUG = debugProp.toLowerCase() === "true";
        }
    } catch (e) {
        // Fallback to environment variable
        try {
            var debugEnv = Packages.java.lang.System.getenv("DILA_DEBUG");
            if (debugEnv != null) {
                DEBUG = debugEnv.toLowerCase() === "true";
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

// This is called by Oxygen when the plugin is initialized.
function applicationStarted(pluginWorkspaceAccess) {


    // Check for DEBUG setting from environment variable or system property
    var DEBUG = getDebugMode();

    // Add a view component customizer
    pluginWorkspaceAccess.addViewComponentCustomizer(new JavaAdapter(
        Packages.ro.sync.exml.workspace.api.standalone.ViewComponentCustomizer, {
            customizeView: function(viewInfo) {
                if ("dila.modified.view" == viewInfo.getViewID()) {
          
                    // Create a simple UI for the view
                    var JMenuBar = Packages.javax.swing.JMenuBar;
                    var JMenu = Packages.javax.swing.JMenu;
                    var JMenuItem = Packages.javax.swing.JMenuItem;
                    var menuBar = new JMenuBar();
                    var menuActions = new JMenu("Actions");
                    var menuItemActionAIMarkup = new JMenuItem("AI Markup"); // load action from local
                    var menuItemActionTagRemoval = new JMenuItem("Tag Removal"); // load action from local
                    var menuOptions = new JMenu("Options");
                    var menuItemOption = new JMenuItem("Settings"); // go to options dialog

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

                    var infoArea = new JTextArea("請從Actions菜單選擇功能，或從Options菜單選擇Settings以設定參數。", 4, 0);
                    infoArea.setLineWrap(true);
                    infoArea.setWrapStyleWord(true);
                    infoArea.setEditable(false);
                    
                    var resultArea = new JTextArea("",8, 0);
                    resultArea.setLineWrap(true);
                    resultArea.setWrapStyleWord(true);
                    resultArea.setEditable(false);
                    
                    var splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, 
                                                   new JScrollPane(infoArea), 
                                                   new JScrollPane(resultArea));
                    splitPane.setDividerLocation(0.5);
                    splitPane.setResizeWeight(0.5);
                    
                    mainPanel.add(splitPane, BorderLayout.CENTER);

                    var JButton = Packages.javax.swing.JButton;
                    var buttonPanel = new JPanel();
                    var replaceButton = new JButton("Replace");
                    buttonPanel.add(replaceButton);
                    buttonPanel.setComponentOrientation(Packages.java.awt.ComponentOrientation.RIGHT_TO_LEFT);
                    mainPanel.add(buttonPanel, BorderLayout.SOUTH);
                    buttonPanel.setVisible(false);

                    // Set the component and title for the view
                    viewInfo.setComponent(mainPanel);
                    viewInfo.setTitle("DILA AI Markup Assistant");

                    // Add action listeners to menu items
                    menuItemActionAIMarkup.addActionListener(function(e) {
                        logDebug("AI Markup action triggered");
                        infoArea.setText("選取Action: AI Markup (用 AI 作參照標記)\n");
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
                                    connection.setRequestMethod("POST");
                                    connection.setRequestProperty("Content-Type", "application/json");
                                    connection.setRequestProperty("Authorization", "Bearer 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"); // API key
                                    connection.setDoOutput(true);
                                    
                                    // Define the system prompt to guide the AI's response.
                                    var systemPrompt = "請將選取文字中，只要與「藏經參照」相關的藏經、冊、頁、欄、行等均標上 XML 標籤。";

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
                                        infoArea.setText("呼叫語言模型發生錯誤: " + error.message);
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
                                                infoArea.append("\n選取文字已替換。");
                                            } finally {
                                                pageAccess.endCompoundUndoableEdit();
                                                buttonPanel.setVisible(false);
                                            }
                                        } else {
                                            infoArea.append("\n文字模式編輯器未開啟，請切換到文字模式再使用。");
                                        }
                                    } catch (error) {
                                        infoArea.append("\n替換文字時發生錯誤: " + error.message);
                                        logDebug("Error during text replacement: " + error);
                                    }
                                }
                            } else {
                                infoArea.append("Text 模式編輯器未開啟。");
                            }
                        } else {
                            infoArea.append("無文字可替換。");
                        }
                    });

                    menuItemActionTagRemoval.addActionListener(function(e) {
                        logDebug("Tag Removal action triggered");
                        infoArea.setText("選取Action: Tag Removal (移除選取文字的標記)。\n");
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
                        infoArea.setText("選取 Option: Settings (設定參數)。\n\n此為待完成功能");
                        // pluginWorkspaceAccess.showOptionsDialog("DILA AI Markup Assistant Settings", "dila.ai.markup.assistant");
                        buttonPanel.setVisible(false);
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
                    area.append("\n選取文字: " + selectedText);
                    return selectedText;
                } else {
                    area.append("[FND]編輯器內未選取文字.");
                    return "";
                }
            } else {
                area.append("[FND]當前頁面不是Text模式編輯器.");
                return "";
            }
        } else {
            area.append("[FND]目前沒有開啟的Text模式編輯器.");
            return "";
        }
    }
}



// This is called by Oxygen when the plugin is being disposed.
function applicationClosing() {
    // Clean up resources if needed.
    var PrintStream = Packages.java.io.PrintStream;
    var UTF8PrintStream = new PrintStream(Packages.java.lang.System.err, true, "UTF-8");
    logDebug("\nClosing AI Markup plugin.");
}