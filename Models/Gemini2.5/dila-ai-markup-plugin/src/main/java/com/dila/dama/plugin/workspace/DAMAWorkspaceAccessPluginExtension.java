package com.dila.dama.plugin.workspace;

import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.options.WSOptionsStorage;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.ViewComponentCustomizer;
import ro.sync.exml.workspace.api.standalone.ViewInfo;
import ro.sync.exml.workspace.api.editor.WSEditor;
import ro.sync.exml.workspace.api.editor.page.WSEditorPage;
import ro.sync.exml.workspace.api.editor.page.text.WSTextEditorPage;

import ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension;

import com.dila.dama.plugin.utf8.UTF8ValidationService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Complete Java implementation of DILA AI Markup Plugin
 * Migrated from JavaScript to eliminate JS-Java bridge issues
 */
public class DAMAWorkspaceAccessPluginExtension implements WorkspaceAccessPluginExtension {
    
    private static final boolean DEBUG = getDebugMode();
    private static final String OPTIONS_PAGE_KEY = "dila_ai_markup_options_page_key";
    private StandalonePluginWorkspace pluginWorkspaceAccess;
    private Object resources; // PluginResourceBundle - using Object to avoid type issues
    private WSOptionsStorage optionStorage;
    
    // UI Components
    private JTextArea infoArea;
    private JTextArea resultArea;
    private JPanel buttonPanel;
    private JButton replaceButton;
    private JButton transferButton;
    private JButton cancelButton;
    
    // UTF-8 workflow state
    private List<Path> currentNonUtf8Files = null;
    
    // Thread pool for background operations
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    
    /**
     * Called when the application starts - implements WorkspaceAccessPluginExtension
     */
    @Override
    public void applicationStarted(StandalonePluginWorkspace workspace) {
        try {
            if (workspace == null) {
                logDebug("Workspace is null, skipping initialization");
                return;
            }
            
            this.pluginWorkspaceAccess = workspace;
            this.resources = workspace.getResourceBundle();
            this.optionStorage = workspace.getOptionsStorage();
            
            logDebug("Starting DILA AI markup plugin (Pure Java Implementation)");
            logDebug("Debug mode: " + DEBUG);
            
            if (optionStorage != null) {
                logDebug("Options storage available.");
                // // Handle API key logging safely
                // String apiKey = optionStorage.getSecretOption("dila.dama.api.key", "");
                // String maskedApiKey = "";
                // if (apiKey != null && apiKey.length() > 0) {
                //     maskedApiKey = apiKey.replaceAll(".(?=.{4})", "*");
                // }
                // logDebug("API Key (masked except last 4 chars): " + maskedApiKey);
                
                // // Load model configuration
                // String model = optionStorage.getOption("dila.dama.llm.model", "");
                // logDebug("LLM Model: " + model);
            } else {
                logDebug("No options storage available.");
            }
            
            // Add view component customizer
            workspace.addViewComponentCustomizer(new DAMAViewComponentCustomizer());
        } catch (Exception e) {
            // Handle exceptions gracefully during startup
            logDebug("Exception during startup: " + e.getMessage());
            // Don't propagate exceptions from startup
        }
    }
    
    /**
     * Custom view component customizer for the DILA AI Markup view
     */
    private class DAMAViewComponentCustomizer implements ViewComponentCustomizer {
        
        @Override
        public void customizeView(ViewInfo viewInfo) {
            if ("dila.ai.markup.view".equals(viewInfo.getViewID())) {
                logDebug("Customizing DILA AI Markup view with pure Java implementation");
                
                // Create the main plugin panel
                JPanel pluginPanel = createMainPanel();
                
                // Set component and title
                viewInfo.setComponent(pluginPanel);
                viewInfo.setTitle(i18n("view.title")); // "DILA AI Markup Assistant", with translation
                
                logDebug("DILA AI Markup view customization completed");
            }
        }
    }
    
    /**
     * Create the main plugin panel with all UI components
     */
    private JPanel createMainPanel() {
        // Main panel with BorderLayout
        JPanel pluginPanel = new JPanel(new BorderLayout());
        
        // Set panel size to 1/5 of screen width
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int panelWidth = screenSize.width / 5;
        int panelHeight = screenSize.height;
        pluginPanel.setPreferredSize(new Dimension(panelWidth, panelHeight));
        
        // Create and add menu bar
        JMenuBar menuBar = createMenuBar();
        pluginPanel.add(menuBar, BorderLayout.NORTH);
        
        // Create text areas and button panel
        createTextAreas();
        createButtonPanel();
        
        // Create split pane layout
        JScrollPane infoScrollPane = new JScrollPane(infoArea);
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(new JScrollPane(resultArea), BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, infoScrollPane, bottomPanel);
        splitPane.setResizeWeight(0.5);
        
        // The split pane is added to the center of the main panel, dividing info and result areas.
        // Layout: [MenuBar (NORTH)] [SplitPane (CENTER: infoArea above, resultArea+buttons below)]
        pluginPanel.add(splitPane, BorderLayout.CENTER);
        
        return pluginPanel;
    }
    
    /**
     * Create the menu bar with Actions, Tools, and Options menus
     */
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // Actions Menu
        JMenu menuActions = new JMenu(i18n("menu.actions")); // "Actions"
        JMenuItem menuItemActionAIMarkup = new JMenuItem(i18n("menuItem.ai.markup")); // "AI Markup"
        JMenuItem menuItemActionTagRemoval = new JMenuItem(i18n("menuItem.tag.removal")); // "Tag Removal"

        menuActions.add(menuItemActionAIMarkup);
        menuActions.add(menuItemActionTagRemoval);
        menuBar.add(menuActions);
        
        // Tools Menu  
        JMenu menuTools = new JMenu(i18n("menu.tools")); // "Tools"
        JMenuItem menuItemUtf8Check = new JMenuItem(i18n("menuItem.utf8.check.convert")); // "UTF-8 Check/Convert"
        menuTools.add(menuItemUtf8Check);
        menuBar.add(menuTools);
        
        // Add horizontal glue to push Options to the right
        menuBar.add(Box.createHorizontalGlue());
        
        // Options Menu with icon
        JMenu menuOptions = createOptionsMenu();
        JMenuItem menuItemOption = new JMenuItem(i18n("menuItem.preferences")); // "Preferences..."
        menuOptions.add(menuItemOption);
        menuBar.add(menuOptions);
        
        // Add action listeners
        menuItemActionAIMarkup.addActionListener(new AIMarkupActionListener());
        menuItemActionTagRemoval.addActionListener(new TagRemovalActionListener());
        menuItemUtf8Check.addActionListener(new UTF8CheckActionListener());
        menuItemOption.addActionListener(new OptionsActionListener());
        
        return menuBar;
    }
    
    /**
     * Create options menu with theme-appropriate icon
     */
    private JMenu createOptionsMenu() {
        JMenu menuOptions = new JMenu();
        
        try {
            // Determine if dark theme
            boolean darkTheme = false;
            if (pluginWorkspaceAccess != null) {
                Object colorTheme = pluginWorkspaceAccess.getColorTheme();
                try {
                    // Use reflection to call isDarkTheme() if available
                    if (colorTheme != null) {
                        java.lang.reflect.Method isDarkMethod = colorTheme.getClass().getMethod("isDarkTheme");
                        darkTheme = (Boolean) isDarkMethod.invoke(colorTheme);
                    }
                } catch (Exception e) {
                    logDebug("Could not determine theme, using light theme");
                }
            }
            
            // Use consistent naming with JavaScript version
            String iconPath = darkTheme ? "images/options_dark.png" : "images/options.png";
            logDebug("Loading options icon: " + iconPath + " (dark theme: " + darkTheme + ")");
            
            // Try multiple approaches to load the icon
            ImageIcon icon = loadPluginIcon(iconPath);
            
            if (icon != null && icon.getIconWidth() > 0 && icon.getIconHeight() > 0) {
                // Scale icon to appropriate size for menu
                Image img = icon.getImage();
                Image scaledImg = img.getScaledInstance(16, 16, Image.SCALE_SMOOTH);
                menuOptions.setIcon(new ImageIcon(scaledImg));
                logDebug("Options icon loaded successfully (size: " + icon.getIconWidth() + "x" + icon.getIconHeight() + ")");
            } else {
                logDebug("Options icon not found or invalid, using text label");
                menuOptions.setText(i18n("menu.options"));  // "Options"
            }
            
        } catch (Exception e) {
            logDebug("Error loading options icon: " + e.getMessage());
            menuOptions.setText(i18n("menu.options"));  // "Options"
        }
        
        menuOptions.setToolTipText(i18n("menu.options")); // "Options"
        return menuOptions;
    }
    
    /**
     * Load plugin icon with multiple fallback approaches
     */
    private ImageIcon loadPluginIcon(String iconPath) {
        // Method 1: Try class loader resource
        try {
            URL iconURL = getClass().getClassLoader().getResource(iconPath);
            if (iconURL != null) {
                logDebug("Icon found via class loader: " + iconURL);
                return new ImageIcon(iconURL);
            }
        } catch (Exception e) {
            logDebug("Class loader approach failed: " + e.getMessage());
        }
        
        // // Method 2: Try direct class resource
        // try {
        //     URL iconURL = getClass().getResource("/" + iconPath);
        //     if (iconURL != null) {
        //         logDebug("Icon found via class resource: " + iconURL);
        //         return new ImageIcon(iconURL);
        //     }
        // } catch (Exception e) {
        //     logDebug("Class resource approach failed: " + e.getMessage());
        // }
        
        // // Method 3: Try using plugin workspace to get plugin directory
        // try {
        //     if (pluginWorkspaceAccess != null) {
        //         // Get plugin directory from workspace access
        //         String pluginDirPath = getPluginDirectory();
        //         if (pluginDirPath != null) {
        //             File iconFile = new File(pluginDirPath, iconPath);
        //             if (iconFile.exists() && iconFile.canRead()) {
        //                 logDebug("Icon found in plugin directory: " + iconFile.getAbsolutePath());
        //                 return new ImageIcon(iconFile.getAbsolutePath());
        //             }
        //         }
        //     }
        // } catch (Exception e) {
        //     logDebug("Plugin directory approach failed: " + e.getMessage());
        // }
        
        // // Method 4: Try alternative naming patterns
        // try {
        //     String[] alternativeNames = {
        //         iconPath,
        //         iconPath.replace("_", "-"),  // options_dark.png -> options-dark.png
        //         iconPath.replace("-", "_"),  // options-dark.png -> options_dark.png
        //         "icons/" + iconPath,         // Try icons/ subdirectory
        //         "img/" + iconPath            // Try img/ subdirectory
        //     };
            
        //     for (String altPath : alternativeNames) {
        //         URL iconURL = getClass().getClassLoader().getResource(altPath);
        //         if (iconURL != null) {
        //             logDebug("Icon found with alternative path: " + altPath);
        //             return new ImageIcon(iconURL);
        //         }
        //     }
        // } catch (Exception e) {
        //     logDebug("Alternative naming approach failed: " + e.getMessage());
        // }
        
        logDebug("All icon loading methods failed for: " + iconPath);
        return null;
    }
    
    // /**
    //  * Get plugin directory path
    //  */
    // private String getPluginDirectory() {
    //     try {
    //         // Try to get plugin directory using reflection on workspace access
    //         java.lang.reflect.Method getPluginDirMethod = pluginWorkspaceAccess.getClass().getMethod("getPluginDirectory");
    //         Object pluginDir = getPluginDirMethod.invoke(pluginWorkspaceAccess);
    //         if (pluginDir != null) {
    //             return pluginDir.toString();
    //         }
    //     } catch (Exception e) {
    //         logDebug("Could not get plugin directory via reflection: " + e.getMessage());
    //     }
        
    //     // Fallback: try to determine from code source
    //     try {
    //         java.security.CodeSource codeSource = getClass().getProtectionDomain().getCodeSource();
    //         if (codeSource != null) {
    //             URL location = codeSource.getLocation();
    //             if (location != null) {
    //                 File jarFile = new File(location.toURI());
    //                 File pluginDir = jarFile.getParentFile();
    //                 if (pluginDir != null && pluginDir.exists()) {
    //                     return pluginDir.getAbsolutePath();
    //                 }
    //             }
    //         }
    //     } catch (Exception e) {
    //         logDebug("Could not determine plugin directory from code source: " + e.getMessage());
    //     }
        
    //     return null;
    // }
    
    /**
     * Create text areas for info and results
     */
    private void createTextAreas() {
        infoArea = new JTextArea(i18n("initial.info"), 4, 0); // "Please select a function from the Actions/Tools menu, or select Preferences from the Options menu to configure parameters."
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setEditable(false);
        
        resultArea = new JTextArea("", 8, 0);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setEditable(false);
    }
    
    /**
     * Create button panel with replace, transfer, and cancel buttons
     */
    private void createButtonPanel() {
        buttonPanel = new JPanel();
        buttonPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        
        replaceButton = new JButton(i18n("button.replace")); // "Replace", with translation
        transferButton = new JButton(i18n("button.transfer.utf8")); // "Transfer to UTF-8", with translation
        cancelButton = new JButton(i18n("button.cancel")); // "Cancel", with translation
        
        buttonPanel.add(replaceButton);
        buttonPanel.add(transferButton);
        buttonPanel.add(cancelButton);
        
        // Initially hide all buttons
        buttonPanel.setVisible(false);
        
        // Add action listeners
        replaceButton.addActionListener(new ReplaceButtonActionListener());
        transferButton.addActionListener(new TransferButtonActionListener());
        cancelButton.addActionListener(new CancelButtonActionListener());
    }
    
    // ========================================
    // Action Listeners
    // ========================================
    
    /**
     * AI Markup action listener
     */
    private class AIMarkupActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            logDebug("AI Markup action triggered");
            
            // Clear previous results
            infoArea.setText("");
            resultArea.setText("");
            hideAllButtons();
            
            // Get selected text from current editor
            String selectedText = fetchSelectedText(infoArea);
            if (selectedText.isEmpty()) {
                infoArea.setText(i18n("no.text.selected")); // "No text selected in the editor."
                return;
            }
            
            infoArea.setText(i18n("action.ai.markup.selected") + // "Action selected: AI Markup (Use AI for reference tagging)\n"
                            i18n("selected.text", selectedText) + // "\nSelected text: "
                            i18n("text.with.length", selectedText.length())); // "Text length: {0} characters"
            
            // Process AI markup in background
            CompletableFuture.supplyAsync(() -> processAIMarkup(selectedText), executor)
                .thenAccept(result -> SwingUtilities.invokeLater(() -> {
                    resultArea.setText(result);
                    showReplaceButton();
                }))
                .exceptionally(throwable -> {
                    SwingUtilities.invokeLater(() -> {
                        resultArea.setText(i18n("error.processing.ai.markup", throwable.getMessage())); // "Error processing AI Markup: {0}"
                    });
                    return null;
                });
        }
    }
    
    /**
     * Tag Removal action listener
     */
    private class TagRemovalActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            logDebug("Tag Removal action triggered");
            
            // Clear previous results
            infoArea.setText("");
            resultArea.setText("");
            hideAllButtons();
            
            // Get selected text from current editor
            String selectedText = fetchSelectedText(infoArea);
            if (selectedText.isEmpty()) {
                infoArea.setText(i18n("no.text.selected")); // "No text selected in the editor."
                return;
            }

            infoArea.setText(i18n("action.tag.removal.selected") + // "Action selected: Tag Removal (Remove tags from selected text).\n"
                            i18n("selected.text", selectedText) + // "\nSelected text: "
                            i18n("text.with.length", selectedText.length())); // "Text length: {0} characters"

            // Process tag removal
            String result = processTagRemoval(selectedText);
            resultArea.setText(result);
            showReplaceButton();
        }
    }
    
    /**
     * UTF-8 Check action listener
     */
    private class UTF8CheckActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            logDebug("UTF-8 Check action triggered");
            
            // Clear previous results
            infoArea.setText("");
            resultArea.setText("");
            hideAllButtons();
            currentNonUtf8Files = null;
            
            // Show file chooser
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            fileChooser.setMultiSelectionEnabled(true);
            fileChooser.setDialogTitle(i18n("dialog.directories.files.selection")); // "Select files or directories to check UTF-8 encoding"
            
            int result = fileChooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                File[] selectedFiles = fileChooser.getSelectedFiles();
                if (selectedFiles != null && selectedFiles.length > 0) {
                    infoArea.setText(i18n("utf8.scanning.files")); // "Scanning files for UTF-8 compliance..."
                    // Check files in background
                    CompletableFuture.supplyAsync(() -> checkUtf8Files(selectedFiles), executor)
                        .thenAccept(nonUtf8Files -> SwingUtilities.invokeLater(() -> 
                            displayUtf8CheckResults(nonUtf8Files)))
                        .exceptionally(throwable -> {
                            SwingUtilities.invokeLater(() -> {
                                resultArea.setText(i18n("error.checking.utf8", throwable.getMessage())); // "Error checking UTF-8 files: {0}"
                            });
                            return null;
                        });
                }
            }
        }
    }
    
    /**
     * Options action listener
     */
    private class OptionsActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            logDebug("Options action triggered");
            
            try {
                if (pluginWorkspaceAccess != null) {
                    // Open options page using the same key as defined in plugin configuration
                    pluginWorkspaceAccess.showPreferencesPages(new String[]{OPTIONS_PAGE_KEY}, OPTIONS_PAGE_KEY, true);
                } else {
                    logDebug("Plugin workspace access is null, cannot open options");
                }
            } catch (Exception ex) {
                logDebug("Error opening options page: " + ex.getMessage());
            }
        }
    }
    
    /**
     * Replace button action listener
     */
    private class ReplaceButtonActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            logDebug("Replace button triggered");
            
            String resultText = resultArea.getText();
            if (resultText == null || resultText.trim().isEmpty()) {
                infoArea.setText(i18n("no.text.to.replace")); // "\nNo text to replace."
                return;
            }
            
            // Check if we have selected text - reuse existing validation logic
            String currentSelection = fetchSelectedText(infoArea);
            if (currentSelection.isEmpty()) {
                // Error message already set by fetchSelectedText
                return;
            }
            
            // Replace text in current editor
            try {
                WSEditor editorAccess = pluginWorkspaceAccess.getCurrentEditorAccess(PluginWorkspace.MAIN_EDITING_AREA);
                if (editorAccess != null) {
                    WSEditorPage pageAccess = editorAccess.getCurrentPage();
                    if (pageAccess instanceof WSTextEditorPage) {
                        WSTextEditorPage textPage = (WSTextEditorPage) pageAccess;
                        
                        // Get current selection bounds
                        int selectionStart = textPage.getSelectionStart();
                        int selectionEnd = textPage.getSelectionEnd();
                        
                        if (selectionStart != selectionEnd) {
                            // Replace selected text using Oxygen's API
                            try {
                                // First delete the selected text
                                textPage.deleteSelection();
                                
                                // Then insert the new text at the cursor position
                                int currentOffset = textPage.getCaretOffset();
                                
                                // Use reflection to call insertTextAtOffset if available
                                try {
                                    java.lang.reflect.Method insertMethod = textPage.getClass().getMethod(
                                        "insertTextAtOffset", String.class, int.class);
                                    insertMethod.invoke(textPage, resultText, currentOffset);
                                    logDebug("Text replaced by insertTextAtOffset successfully");
                                } catch (NoSuchMethodException nsme) {
                                    // Fallback: try using document insertion
                                    javax.swing.text.Document doc = textPage.getDocument();
                                    if (doc != null) {
                                        doc.insertString(currentOffset, resultText, null);
                                        logDebug("Text replaced by document insertion successfully");
                                    } else {
                                        infoArea.setText(i18n("no.editor.open")); // "\nNo open Text mode editor."
                                        logDebug("No open Text mode editor.");
                                        return;
                                    }
                                }
                                
                                infoArea.setText(i18n("text.replaced")); // "\nSelected text has been replaced."
                                hideAllButtons();
                                resultArea.setText(""); // Clear result area
                                
                            } catch (Exception ex) {
                                logDebug("Error during text replacement: " + ex.getMessage());
                                infoArea.setText(i18n("error.replacing.text", ex.getMessage())); // "\nError during text replacement: "
                            }
                        } else {
                            infoArea.setText(i18n("no.text.selected"));  // "No text selected in the editor."
                        }
                    }
                    // Note: fetchSelectedText already handles non-text mode case
                }
                // Note: fetchSelectedText already handles no editor case
            } catch (Exception ex) {
                logDebug("Error accessing editor: " + ex.getMessage());
                infoArea.setText(i18n("error.accessing.editor", ex.getMessage())); // "Error accessing editor: {0}"
            }
        }
    }
    
    /**
     * Transfer button action listener for UTF-8 conversion
     */
    private class TransferButtonActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            logDebug("Transfer button triggered");
            
            if (currentNonUtf8Files == null || currentNonUtf8Files.isEmpty()) {
                infoArea.setText(i18n("no.files.to.convert")); // "\nNo files to convert."
                return;
            }
            
            infoArea.setText(i18n("utf8.Converting")); // "Converting files to UTF-8..."
            hideTransferButtons();
            
            // Convert files in background
            CompletableFuture.supplyAsync(() -> convertFilesToUtf8(currentNonUtf8Files), executor)
                .thenAccept(conversionResults -> SwingUtilities.invokeLater(() -> 
                    displayConversionResults(conversionResults)))
                .exceptionally(throwable -> {
                    SwingUtilities.invokeLater(() -> {
                        infoArea.setText(i18n("error.converting.files", throwable.getMessage())); // "Error converting files: {0}"
                    });
                    return null;
                });
        }
    }
    
    /**
     * Cancel button action listener
     */
    private class CancelButtonActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            logDebug("Cancel button triggered");
            
            currentNonUtf8Files = null;
            hideTransferButtons();
            infoArea.setText(i18n("utf8.conversion.cancelled")); // "UTF-8 conversion cancelled."
            resultArea.setText("");
        }
    }
    
    // ========================================
    // Helper Methods
    // ========================================
    
    /**
     * Get internationalized string
     */
    private String i18n(String key) {
        try {
            if (resources != null) {
                // Use reflection to call getMessage if available
                java.lang.reflect.Method getMessageMethod = resources.getClass().getMethod("getMessage", String.class);
                return (String) getMessageMethod.invoke(resources, key);
            }
            logDebug("Resources not available for i18n key: " + key);
            return key;
        } catch (Exception e) {
            logDebug("Error getting i18n message for key " + key + ": " + e.getMessage());
            return key;
        }
    }
    
    /**
     * Get internationalized string with parameter substitution
     */
    private String i18n(String key, Object... params) {
        try {
            String message = i18n(key);
            if (params != null && params.length > 0) {
                // Simple parameter substitution using {0}, {1}, etc.
                for (int i = 0; i < params.length; i++) {
                    message = message.replace("{" + i + "}", String.valueOf(params[i]));
                }
            }
            return message;
        } catch (Exception e) {
            logDebug("Error formatting i18n message for key " + key + ": " + e.getMessage());
            return key;
        }
    }
    
    /**
     * Fetch selected text from current editor
     */
    private String fetchSelectedText(JTextArea area) {
        try {
            WSEditor editorAccess = pluginWorkspaceAccess.getCurrentEditorAccess(PluginWorkspace.MAIN_EDITING_AREA);
            if (editorAccess != null) {
                WSEditorPage pageAccess = editorAccess.getCurrentPage();
                if (pageAccess instanceof WSTextEditorPage) {
                    WSTextEditorPage textPage = (WSTextEditorPage) pageAccess;
                    return textPage.getSelectedText();
                } else {
                    area.setText(i18n("not.text.mode")); // "\nCurrent page is not Text mode."
                    return ""; // Non-text mode
                }
            }
            area.setText(i18n("no.editor.open"));  // "\nNo open Text mode editor."
            return ""; // No editor open
        } catch (Exception e) {
            logDebug("Error fetching selected text: " + e.getMessage()); // "Error accessing editor: {0}"
            return "";
        }
    }
    
    /**
     * Show only replace button
     */
    private void showReplaceButton() {
        transferButton.setVisible(false);
        cancelButton.setVisible(false);
        replaceButton.setVisible(true);
        buttonPanel.setVisible(true);
        buttonPanel.revalidate();
        buttonPanel.repaint();
    }
    
    /**
     * Show transfer and cancel buttons
     */
    private void showTransferButtons() {
        replaceButton.setVisible(false);
        transferButton.setVisible(true);
        cancelButton.setVisible(true);
        buttonPanel.setVisible(true);
        buttonPanel.revalidate();
        buttonPanel.repaint();
    }
    
    /**
     * Hide transfer and cancel buttons
     */
    private void hideTransferButtons() {
        transferButton.setVisible(false);
        cancelButton.setVisible(false);
        buttonPanel.setVisible(false);
        buttonPanel.revalidate();
        buttonPanel.repaint();
    }
    
    /**
     * Hide all buttons
     */
    private void hideAllButtons() {
        buttonPanel.setVisible(false);
        buttonPanel.revalidate();
        buttonPanel.repaint();
    }
    
    // ========================================
    // Processing Methods (Phase 2-4 implementation stubs)
    // ========================================
    
    /**
     * Process AI markup with LLM API integration
     */
    private String processAIMarkup(String text) {
        try {
            // Get API configuration from options storage
            String apiKey = optionStorage.getSecretOption("dila.dama.api.key", "");
            String parseModel = optionStorage.getOption("dila.dama.ft.parse.model", "");
            
            if (apiKey == null || apiKey.trim().isEmpty()) {
                logDebug("API key not configured");
                return i18n("error.no.APIKey"); // "Error: API key not configured. Please set up your API key in Preferences."
            }
            
            if (parseModel == null || parseModel.trim().isEmpty()) {
                logDebug("Parse model not configured");
                return i18n("error.no.parse.model"); // "Error: Parse model not configured. Please set up your model in Preferences."
            }
            
            logDebug("Making AI Markup API call with model: " + parseModel);
            
            // Create HTTP connection
            URL url = new URL("https://api.openai.com/v1/chat/completions");
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setDoOutput(true);
            
            // Get system prompt from i18n
            String systemPrompt = i18n("system.prompt.ai.markup"); // "Please add XML tags for all sutra, volume, page, column, and line references related to "sutra references" in the selected text."
            
            // Create request body
            String requestBody = createJSONRequest(parseModel, systemPrompt, text);
            
            // Send request
            try (java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(
                    connection.getOutputStream(), "UTF-8")) {
                writer.write(requestBody);
                writer.flush();
            }
            
            // Handle response
            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                // Success response
                StringBuilder response = new StringBuilder();
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }
                
                // Parse JSON response
                String llmResponse = parseOpenAIResponse(response.toString());
                return "<ref>" + llmResponse + "</ref>";
                
            } else {
                // Error response
                StringBuilder errorResponse = new StringBuilder();
                try (java.io.BufferedReader errorReader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(connection.getErrorStream(), "UTF-8"))) {
                    String errorLine;
                    while ((errorLine = errorReader.readLine()) != null) {
                        errorResponse.append(errorLine);
                    }
                }
                logDebug("AI Markup HTTP error response: " + errorResponse.toString());
                return "HTTP Error " + responseCode + ": " + errorResponse.toString();
            }
            
        } catch (Exception e) {
            logDebug("AI Markup error: " + e.getMessage());
            return i18n("llm.error", e.getMessage()); // "\nError calling language model: \n{0}"
        }
    }
    
    /**
     * Create JSON request for OpenAI API
     */
    private String createJSONRequest(String model, String systemPrompt, String userContent) {
        // Simple JSON creation without external dependencies
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"model\":\"").append(escapeJSON(model)).append("\",");
        json.append("\"messages\":[");
        json.append("{\"role\":\"system\",\"content\":\"").append(escapeJSON(systemPrompt)).append("\"},");
        json.append("{\"role\":\"user\",\"content\":\"").append(escapeJSON(userContent)).append("\"}");
        json.append("],");
        json.append("\"max_tokens\":1000");
        json.append("}");
        return json.toString();
    }
    
    /**
     * Escape JSON string values
     */
    private String escapeJSON(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    /**
     * Parse OpenAI API response
     */
    private String parseOpenAIResponse(String jsonResponse) {
        try {
            // Simple JSON parsing for OpenAI response format
            // Looking for: {"choices":[{"message":{"content":"response text"}}]}
            
            int choicesIndex = jsonResponse.indexOf("\"choices\":");
            if (choicesIndex == -1) {
                return "Error parsing response: no choices found";
            }
            
            int contentIndex = jsonResponse.indexOf("\"content\":", choicesIndex);
            if (contentIndex == -1) {
                return "Error parsing response: no content found";
            }
            
            // Find the start and end of the content string
            int contentStart = jsonResponse.indexOf("\"", contentIndex + 10) + 1;
            int contentEnd = findJSONStringEnd(jsonResponse, contentStart);
            
            if (contentStart > 0 && contentEnd > contentStart) {
                String content = jsonResponse.substring(contentStart, contentEnd);
                return unescapeJSON(content);
            } else {
                return "Error parsing response: could not extract content";
            }
            
        } catch (Exception e) {
            logDebug("Error parsing OpenAI response: " + e.getMessage());
            return "Error parsing response: " + e.getMessage();
        }
    }
    
    /**
     * Find the end of a JSON string value
     */
    private int findJSONStringEnd(String json, int start) {
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Unescape JSON string values
     */
    private String unescapeJSON(String text) {
        if (text == null) return "";
        return text.replace("\\\"", "\"")
                  .replace("\\\\", "\\")
                  .replace("\\n", "\n")
                  .replace("\\r", "\r")
                  .replace("\\t", "\t");
    }
    
    /**
     * Process tag removal - removes XML/HTML tags from text
     */
    private String processTagRemoval(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        
        logDebug("Processing tag removal for text length: " + text.length());
        
        // Remove XML/HTML tags using regex
        String cleanedText = text.replaceAll("<[^>]*>", "");
        
        logDebug("Tag removal completed. Original length: " + text.length() + 
                ", cleaned length: " + cleanedText.length());
        
        return cleanedText;
    }
    
    /**
     * Check UTF-8 encoding of files using Java service
     */
    private List<Path> checkUtf8Files(File[] selectedFiles) {
        List<Path> nonUtf8Files = new ArrayList<>();
        int totalFiles = 0;
        
        for (File file : selectedFiles) {
            totalFiles += scanFileOrDirectory(file.toPath(), nonUtf8Files);
        }
        
        logDebug("UTF-8 check completed. Total files: " + totalFiles + ", Non-UTF-8: " + nonUtf8Files.size());
        return nonUtf8Files;
    }
    
    /**
     * Recursively scan files and directories
     */
    private int scanFileOrDirectory(Path path, List<Path> nonUtf8Files) {
        int count = 0;
        
        try {
            if (Files.isDirectory(path)) {
                try (java.util.stream.Stream<Path> stream = Files.walk(path)) {
                    stream.filter(Files::isRegularFile)
                          .filter(this::isTextFile)
                          .forEach(file -> {
                              if (!UTF8ValidationService.isValidUtf8(file)) {
                                  nonUtf8Files.add(file);
                              }
                          });
                    count = (int) stream.filter(Files::isRegularFile).count();
                }
            } else if (Files.isRegularFile(path) && isTextFile(path)) {
                count = 1;
                if (!UTF8ValidationService.isValidUtf8(path)) {
                    nonUtf8Files.add(path);
                }
            }
        } catch (IOException e) {
            logDebug("Error scanning path " + path + ": " + e.getMessage());
        }
        
        return count;
    }
    
    /**
     * Check if file is likely a text file
     */
    private boolean isTextFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        String[] textExtensions = {".xml", ".txt", ".html", ".htm", ".xhtml", ".css", ".js", 
                                  ".json", ".md", ".properties", ".java", ".py", ".php", ".rb", 
                                  ".go", ".rs", ".c", ".cpp", ".h", ".hpp", ".sql", ".sh", ".bat", ".csv"};
        
        for (String ext : textExtensions) {
            if (fileName.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Display UTF-8 check results
     */
    private void displayUtf8CheckResults(List<Path> nonUtf8Files) {
        if (nonUtf8Files.isEmpty()) {
            infoArea.setText(i18n("utf8.all.valid")); // "All files are valid UTF-8!"
            return;
        }
        
        currentNonUtf8Files = nonUtf8Files;
        
        StringBuilder info = new StringBuilder();
        info.append(i18n("utf8.non.utf8.found", nonUtf8Files.size())); // "Non-UTF-8 files found: {0}\n\n"
        
        int displayCount = Math.min(10, nonUtf8Files.size());
        for (int i = 0; i < displayCount; i++) {
            info.append("• ").append(nonUtf8Files.get(i).toString()).append("\n");
        }
        
        if (nonUtf8Files.size() > 10) {
            info.append(i18n("more.files.to.convert", nonUtf8Files.size() - 10)); // "... and {0} more files\n"
        }
        
        infoArea.setText(info.toString());
        showTransferButtons();
    }
    
    /**
     * Convert files to UTF-8
     */
    private String convertFilesToUtf8(List<Path> files) {
        StringBuilder results = new StringBuilder();
        int successCount = 0;
        int failureCount = 0;
        
        // Use the existing method from UTF8ValidationService
        UTF8ValidationService.ConversionResult conversionResult = UTF8ValidationService.convertFilesToUtf8(files, null);
        
        for (UTF8ValidationService.ConversionSuccess success : conversionResult.getSuccesses()) {
            successCount++;
            results.append("✓ ").append(success.getFilePath().getFileName())
                   .append(" (").append(success.getSourceEncoding()).append(")\n");
        }
        
        for (UTF8ValidationService.ConversionFailure failure : conversionResult.getFailures()) {
            failureCount++;
            results.append("✗ ").append(failure.getFilePath().getFileName())
                   .append(" - ").append(failure.getError()).append("\n");
        }
        logDebug("UTF-8 conversion summary: Successes=" + successCount + ", Failures=" + failureCount);        
        logDebug("UTF-8 conversion results:\n" + results.toString());
        return i18n("utf8.conversion.summary", successCount, failureCount, results.toString()); // "Conversion summary:\nSuccessfully converted {0} files\nFailed conversions {1} files\n\nDetails:\n{2}"
     } 
    
    /**
     * Display conversion results
     */
    private void displayConversionResults(String results) {
        resultArea.setText(results);
        infoArea.setText(i18n("utf8.conversion.completed")); // "UTF-8 conversion completed."
        currentNonUtf8Files = null;
    }
    
    // ========================================
    // Static utility methods
    // ========================================
    
    /**
     * Determine debug mode from environment or system properties
     */
    private static boolean getDebugMode() {
        try {
            String debugEnv = System.getenv("DILA_DEBUG");
            if (debugEnv != null) {
                return "true".equalsIgnoreCase(debugEnv);
            }
        } catch (Exception e) {
            // Continue to system property check
        }
        
        try {
            String debugProp = System.getProperty("dila.debug");
            if (debugProp != null) {
                return "true".equalsIgnoreCase(debugProp);
            }
        } catch (Exception e) {
            // Use default
        }
        
        return false; // Default value
    }
    
    /**
     * Log debug messages
     */
    private static void logDebug(String message) {
        if (DEBUG) {
            System.err.println("[" + new Date() + "] " + message);
        }
    }
    
    /**
     * Called when the application is closing - implements WorkspaceAccessPluginExtension
     */
    @Override
    public boolean applicationClosing() {
        logDebug("Closing DILA AI Markup plugin (Pure Java Implementation)");
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        return true;
    }
}