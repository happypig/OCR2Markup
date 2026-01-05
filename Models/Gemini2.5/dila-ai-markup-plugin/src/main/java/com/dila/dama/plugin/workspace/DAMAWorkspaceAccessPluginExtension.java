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
import com.dila.dama.plugin.application.command.ConvertReferenceCommand;
import com.dila.dama.plugin.application.command.ConvertReferenceResult;
import com.dila.dama.plugin.domain.model.InvalidReferenceException;
import com.dila.dama.plugin.domain.service.RefElementRewriter;
import com.dila.dama.plugin.domain.service.ReferenceParser;
import com.dila.dama.plugin.infrastructure.api.CBRDAPIClient;
import com.dila.dama.plugin.infrastructure.api.HttpUrlConnectionFactory;
import com.dila.dama.plugin.preferences.DAMAOptionPagePluginExtension;
import com.dila.dama.plugin.util.PluginLogger;
import com.dila.dama.plugin.util.XmlDomUtils;

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
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Complete Java implementation of DILA AI Markup Plugin
 * Migrated from JavaScript to eliminate JS-Java bridge issues
 */
public class DAMAWorkspaceAccessPluginExtension implements WorkspaceAccessPluginExtension {
    
    private static final String OPTIONS_PAGE_KEY = "dila_ai_markup_options_page_key";
    private StandalonePluginWorkspace pluginWorkspaceAccess;
    private Object resources; // PluginResourceBundle - using Object to avoid type issues
    private WSOptionsStorage optionStorage;
    
    // UI Components
    private JTextArea infoArea;
    private JTextArea resultArea;
    private JPanel buttonPanel;
    private JButton replaceButton;
    private JButton convertButton;
    private JButton transferButton;
    private JButton cancelButton;

    // Ref-to-Link workflow state (in-memory only)
    private String currentRefToLinkUrl;
    private String currentRefToLinkSelection;
    
    // UTF-8 workflow state
    private List<Path> currentNonUtf8Files = null;
    // private int currentTotalFilesScanned = 0;
    
    // Thread pool for background operations
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    
    /**
     * Operation type enum for tracking current operation context
     */
    private enum OperationType {
        NONE,           // No operation in progress
        AI_MARKUP,      // AI Markup operation
        TAG_REMOVAL,    // Tag Removal operation
        UTF8_CHECK,     // UTF-8 Check operation
        UTF8_CONVERT,   // UTF-8 Conversion operation
        REF_TO_LINK     // <ref> to link operation
    }
    
    // Current operation context
    private OperationType currentOperation = OperationType.NONE;
    
    /**
     * Result class for UTF-8 check operation
     */
    private static class Utf8CheckResult {
        private final List<Path> nonUtf8Files;
        private final int totalFiles;
        
        public Utf8CheckResult(List<Path> nonUtf8Files, int totalFiles) {
            this.nonUtf8Files = nonUtf8Files;
            this.totalFiles = totalFiles;
        }
        
        public List<Path> getNonUtf8Files() {
            return nonUtf8Files;
        }
        
        public int getTotalFiles() {
            return totalFiles;
        }
    }
    
    /**
     * Called when the application starts - implements WorkspaceAccessPluginExtension
     */
    @Override
    public void applicationStarted(StandalonePluginWorkspace workspace) {
        try {
            if (workspace == null) {
                PluginLogger.warn("[applicationStarted]Workspace is null, skipping initialization");
                return;
            }
            
            this.pluginWorkspaceAccess = workspace;
            this.resources = workspace.getResourceBundle();
            this.optionStorage = workspace.getOptionsStorage();

            if (System.getProperty("java.net.useSystemProxies") == null) {
                System.setProperty("java.net.useSystemProxies", "true");
                PluginLogger.debug("[applicationStarted]Enabled system proxy discovery for HTTP connections");
            }
            
            PluginLogger.info("[applicationStarted]Starting DILA AI markup plugin (Pure Java Implementation)");
            PluginLogger.debug("[applicationStarted]PluginLogger.isDebugEnabled() mode: " + PluginLogger.isDebugEnabled());
            
            if (optionStorage != null) {
                PluginLogger.debug("[applicationStarted]Options storage available.");
                // Leave the checking of API key & model to the action handlers
            } else {
                PluginLogger.warn("[applicationStarted]No options storage available.");
            }
            
            // Add view component customizer
            workspace.addViewComponentCustomizer(new DAMAViewComponentCustomizer());
        } catch (Exception e) {
            // Handle exceptions gracefully during startup
            PluginLogger.error("[applicationStarted]Exception during startup: " + e.getMessage());
            // Don't propagate exceptions from startup
        }
    }
    
    /**
     * Custom view component customizer for the DILA AI Markup view(i18n)
     */
    private class DAMAViewComponentCustomizer implements ViewComponentCustomizer {
        
        @Override
        public void customizeView(ViewInfo viewInfo) {
            if ("dila.ai.markup.view".equals(viewInfo.getViewID())) {
                PluginLogger.info("[DAMAViewComponentCustomizer]Customizing DILA AI Markup view with pure Java implementation");
                
                // Create the main plugin panel
                JPanel pluginPanel = createMainPanel();
                
                // Set component and title
                viewInfo.setComponent(pluginPanel);
                viewInfo.setTitle(i18n("view.title")); // "DILA AI Markup Assistant", with translation

                PluginLogger.info("[DAMAViewComponentCustomizer]DILA AI Markup view customization completed");
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
     * Create the menu bar with Actions, Tools, and Options menus(i18n)
     */
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // Actions Menu
        JMenu menuActions = new JMenu(i18n("menu.actions")); // "Actions"
        JMenuItem menuItemActionAIMarkup = new JMenuItem(i18n("menuItem.ai.markup")); // "AI Markup"
        JMenuItem menuItemActionTagRemoval = new JMenuItem(i18n("menuItem.tag.removal")); // "Tag Removal"
        JMenuItem menuItemRefToLink = new JMenuItem(i18n("menuItem.ref.to.link")); // "<ref> to link"

        menuActions.add(menuItemActionAIMarkup);
        menuActions.add(menuItemActionTagRemoval);
        menuActions.add(menuItemRefToLink);
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
        menuItemRefToLink.addActionListener(new RefToLinkActionListener());
        menuItemUtf8Check.addActionListener(new UTF8CheckActionListener());
        menuItemOption.addActionListener(new OptionsActionListener());
        
        return menuBar;
    }
    
    /**
     * Create options menu with theme-appropriate icon(i18n)
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
                    PluginLogger.error("[createOptionsMenu]Could not determine theme, using light theme");
                }
            }
            
            // Use consistent naming with JavaScript version
            String iconPath = darkTheme ? "images/options_dark.png" : "images/options.png";
            PluginLogger.debug("[createOptionsMenu]Loading options icon: " + iconPath + " (dark theme: " + darkTheme + ")");
            
            // Try multiple approaches to load the icon
            ImageIcon icon = loadPluginIcon(iconPath);
            
            if (icon != null && icon.getIconWidth() > 0 && icon.getIconHeight() > 0) {
                // Scale icon to appropriate size for menu
                Image img = icon.getImage();
                Image scaledImg = img.getScaledInstance(16, 16, Image.SCALE_SMOOTH);
                menuOptions.setIcon(new ImageIcon(scaledImg));
                PluginLogger.debug("[createOptionsMenu]Options icon loaded successfully (size: " + icon.getIconWidth() + "x" + icon.getIconHeight() + ")");
            } else {
                PluginLogger.debug("[createOptionsMenu]Options icon not found or invalid, using text label");
                menuOptions.setText(i18n("menu.options"));  // "Options"
            }
            
        } catch (Exception e) {
            PluginLogger.error("[createOptionsMenu]Error loading options icon: " + e.getMessage());
            menuOptions.setText(i18n("menu.options"));  // "Options"
        }
        
        menuOptions.setToolTipText(i18n("menu.options")); // "Options"
        return menuOptions;
    }
    
    /**
     * Load plugin icon by class loader resource
     */
    private ImageIcon loadPluginIcon(String iconPath) {
        try {
            URL iconURL = getClass().getClassLoader().getResource(iconPath);
            if (iconURL != null) {
                PluginLogger.debug("[loadPluginIcon]Icon found via class loader: " + iconURL);
                return new ImageIcon(iconURL);
            }
        } catch (Exception e) {
            PluginLogger.error("[loadPluginIcon]Class loader approach failed: " + e.getMessage());
        }
        
        PluginLogger.warn("[loadPluginIcon]All icon loading methods failed for: " + iconPath);
        return null;
    }
    
    /**
     * Create text areas for info and results(i18n)
     */
    private void createTextAreas() {
        infoArea = new JTextArea(i18n("initial.info"), 4, 0); // "Please select any function from the Actions/Tools menu, or select Preferences from the Gear icon(Options menu) to configure parameters."
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setEditable(false);
        
        resultArea = new JTextArea("", 8, 0);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setEditable(false);
    }
    
    /**
     * Create button panel with replace, transfer, and cancel buttons(i18n)
     */
    private void createButtonPanel() {
        buttonPanel = new JPanel();
        buttonPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        
        replaceButton = new JButton(i18n("button.replace")); // "Replace", with translation
        convertButton = new JButton(i18n("button.convert")); // "Convert", with translation
        transferButton = new JButton(i18n("button.transfer.utf8")); // "Transfer to UTF-8", with translation
        cancelButton = new JButton(i18n("button.cancel")); // "Cancel", with translation
        
        buttonPanel.add(replaceButton);
        buttonPanel.add(convertButton);
        buttonPanel.add(transferButton);
        buttonPanel.add(cancelButton);
        
        // Initially hide all buttons
        buttonPanel.setVisible(false);
        
        // Add action listeners
        replaceButton.addActionListener(new ReplaceButtonActionListener());
        convertButton.addActionListener(new ConvertButtonActionListener());
        transferButton.addActionListener(new TransferButtonActionListener());
        cancelButton.addActionListener(new CancelButtonActionListener());
    }
    
    // ========================================
    // Action Listeners
    // ========================================
    
    /**
     * AI Markup action listener(i18n)
     */
    private class AIMarkupActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            PluginLogger.info("[AIMarkupActionListener]AI Markup action triggered");
            
            // Set operation context
            currentOperation = OperationType.AI_MARKUP;
            
            // Clear previous results
            infoArea.setText(i18n("action.ai.markup.selected") + "\n\n"); // "Action selected: AI Markup (Use AI for reference tagging)"
            resultArea.setText("");
            hideAllButtons();
            
            // Get selected text from current editor
            String selectedText = fetchSelectedText(resultArea);
            if (selectedText.isEmpty()) {
                // infoArea.append(i18n("no.text.selected")); // "No text selected in the editor."
                return;
            }
            
            infoArea.append(i18n("selected.text", selectedText) + "\n" // "Selected text: "
                            + i18n("text.with.length", selectedText.length()) + "\n\n"); // "Text length: {0} characters"
            
            // Process AI markup in background
            CompletableFuture.supplyAsync(() -> processAIMarkup(selectedText), executor)
                .thenAccept(result -> SwingUtilities.invokeLater(() -> {
                    setResultWithReplaceButton(result);
                }))
                .exceptionally(throwable -> {
                    SwingUtilities.invokeLater(() -> {
                        setResultWithReplaceButton(i18n("error.processing.ai.markup", throwable.getMessage())); // "Error processing AI Markup: {0}"
                    });
                    return null;
                });
        }
    }
    
    /**
     * Tag Removal action listener(i18n)
     */
    private class TagRemovalActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            PluginLogger.info("[TagRemovalActionListener]Tag Removal action triggered");
            
            // Set operation context
            currentOperation = OperationType.TAG_REMOVAL;
            
            // Clear previous results
            infoArea.setText(i18n("action.tag.removal.selected") + "\n\n"); // "Action selected: Tag Removal (Remove tags from selected text)."
            resultArea.setText("");
            hideAllButtons();
            
            // Get selected text from current editor
            String selectedText = fetchSelectedText(resultArea);
            if (selectedText.isEmpty()) {
                // infoArea.append(i18n("no.text.selected")); // "No text selected in the editor."
                return;
            }

            infoArea.append(i18n("selected.text", selectedText) + "\n" // "\nSelected text: "
                            + i18n("text.with.length", selectedText.length()) + "\n\n"); // "Text length: {0} characters"

            // Process tag removal
            String result = processTagRemoval(selectedText);
            setResultWithReplaceButton(result);
        }
    }

    /**
     * Ref to Link action listener (001-ref-to-link-action)
     */
    private class RefToLinkActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            PluginLogger.info("[RefToLinkActionListener]Ref to Link action triggered");

            currentOperation = OperationType.REF_TO_LINK;
            currentRefToLinkUrl = null;
            currentRefToLinkSelection = null;

            infoArea.setText(i18n("action.ref.to.link.selected") + "\n\n");
            resultArea.setText("");
            hideAllButtons();

            String selectedText = fetchSelectedRefToLinkText();
            if (selectedText.isEmpty()) {
                return;
            }
            if (currentRefToLinkSelection != null && !currentRefToLinkSelection.equals(selectedText)) {
                PluginLogger.warn("[ReplaceButtonActionListener]Ref selection changed since Convert");
            }
            currentRefToLinkSelection = selectedText;

            infoArea.append(i18n("selected.text", selectedText) + "\n"
                + i18n("text.with.length", selectedText.length()) + "\n\n");

            executeRefToLinkConversion(selectedText);
        }
    }

    /**
     * Convert button action listener (CBRD API) (001-ref-to-link-action)
     */
    private class ConvertButtonActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (currentOperation != OperationType.REF_TO_LINK) {
                return;
            }

            PluginLogger.info("[ConvertButtonActionListener]Convert button triggered");

            String selectedText = fetchSelectedRefToLinkText();
            if (selectedText.isEmpty()) {
                return;
            }

            executeRefToLinkConversion(selectedText);
        }
    }

    private void executeRefToLinkConversion(String selectedText) {
        if (currentOperation != OperationType.REF_TO_LINK) {
            return;
        }
        if (selectedText == null || selectedText.trim().isEmpty()) {
            return;
        }

        resultArea.setText(i18n("calling.cbrd.api"));
        showRefToLinkConvertingState();

        // Read CBRD configuration from options storage (fallback to defaults)
        String apiUrl = "https://cbss.dila.edu.tw/dev/cbrd/link";
        String referer = "CBRD@dila.edu.tw";
        int timeoutMs = 3000;
        try {
            if (optionStorage != null) {
                String optUrl = optionStorage.getOption(DAMAOptionPagePluginExtension.KEY_CBRD_API_URL, apiUrl);
                String optReferer = optionStorage.getOption(DAMAOptionPagePluginExtension.KEY_CBRD_REFERER_HEADER, referer);
                String optTimeout = optionStorage.getOption(DAMAOptionPagePluginExtension.KEY_CBRD_TIMEOUT_MS, String.valueOf(timeoutMs));
                if (optUrl != null && !optUrl.trim().isEmpty()) {
                    apiUrl = optUrl.trim();
                }
                if (optReferer != null && !optReferer.trim().isEmpty()) {
                    referer = optReferer.trim();
                }
                try {
                    timeoutMs = Integer.parseInt(optTimeout);
                } catch (Exception ignored) {
                    timeoutMs = 3000;
                }
            }
        } catch (Exception ex) {
            PluginLogger.warn("[executeRefToLinkConversion]Failed to read CBRD options, using defaults: " + ex.getMessage());
        }

        ConvertReferenceCommand command = new ConvertReferenceCommand(
            new ReferenceParser(),
            new CBRDAPIClient(apiUrl, referer, timeoutMs, new HttpUrlConnectionFactory())
        );

        CompletableFuture.supplyAsync(() -> command.execute(selectedText), executor)
            .thenAccept(result -> SwingUtilities.invokeLater(() -> {
                handleRefToLinkConvertResult(result);
            }))
            .exceptionally(throwable -> {
                SwingUtilities.invokeLater(() -> {
                    currentRefToLinkUrl = null;
                    resultArea.setText(i18n("error.api.connection"));
                    showConvertButton();
                });
                return null;
            });
    }

    private void handleRefToLinkConvertResult(ConvertReferenceResult result) {
        if (result == null) {
            currentRefToLinkUrl = null;
            resultArea.setText(i18n("error.api.response"));
            showConvertButton();
            return;
        }

        if (result.isSuccess()) {
            currentRefToLinkUrl = result.getUrl();
            resultArea.setText(currentRefToLinkUrl != null ? currentRefToLinkUrl : "");
            infoArea.append(i18n("success.link.generated") + "\n");
            showConvertAndReplaceButtons();
        } else {
            currentRefToLinkUrl = null;
            resultArea.setText(i18n(result.getMessageKey(), result.getMessageParams()));
            showConvertButton();
        }
    }
    
    /**
     * UTF-8 Check action listener(i18n)
     */
    private class UTF8CheckActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            PluginLogger.info("[UTF8CheckActionListener]UTF-8 Check action triggered");
            
            // Set operation context
            currentOperation = OperationType.UTF8_CHECK;
            
            // Clear previous results
            infoArea.setText(i18n("action.utf8.check.transfer.selected") + "\n\n"); // "Action selected: UTF-8 Check/Convert (Check and convert files to UTF-8 encoding)."
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
                    infoArea.append(i18n("utf8.check.scanning.files") + "\n\n"); // "Scanning files for UTF-8 compliance..."
                    // Check files in background
                    CompletableFuture.supplyAsync(() -> checkUtf8Files(selectedFiles), executor)
                        .thenAccept(checkResult -> SwingUtilities.invokeLater(() -> {
                            displayUtf8CheckResults(checkResult);
                        }))
                        .exceptionally(throwable -> {
                            SwingUtilities.invokeLater(() -> {
                                setResultInformational(i18n("error.checking.utf8", throwable.getMessage())); // "Error checking UTF-8 files: {0}"
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
            PluginLogger.info("[OptionsActionListener]Options action triggered");
            
            try {
                if (pluginWorkspaceAccess != null) {
                    // Open options page using the same key as defined in plugin configuration
                    pluginWorkspaceAccess.showPreferencesPages(new String[]{OPTIONS_PAGE_KEY}, OPTIONS_PAGE_KEY, true);
                } else {
                    PluginLogger.warn("[OptionsActionListener]Plugin workspace access is null, cannot open options");
                }
            } catch (Exception ex) {
                PluginLogger.error("[OptionsActionListener]Error opening options page: " + ex.getMessage());
            }
        }
    }
    
    /**
     * Replace button action listener(i18n)
     */
    private class ReplaceButtonActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            PluginLogger.info("[ReplaceButtonActionListener]Replace button triggered");

            if (currentOperation == OperationType.REF_TO_LINK) {
                handleRefToLinkReplace();
                return;
            }
            
            String resultText = resultArea.getText();
            if (resultText == null || resultText.trim().isEmpty()) {
                PluginLogger.warn("[ReplaceButtonActionListener]No text to replace");
                infoArea.append("\n" + i18n("no.text.to.replace") + "\n"); // "No text to replace."
                return;
            }
            
            // Check if we have selected text - reuse existing validation logic
            String currentSelection = fetchSelectedText(resultArea);
            if (currentSelection.isEmpty()) {
                PluginLogger.warn("[ReplaceButtonActionListener]No selected text in editor");
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
                                
                                // Use document insertion to add the new text (document should never be null for valid text page)
                                javax.swing.text.Document doc = textPage.getDocument();
                                doc.insertString(currentOffset, resultText, null);
                                PluginLogger.info("[ReplaceButtonActionListener]Text replaced successfully via document insertion");
                                
                                // Reset operation context after successful replacement
                                currentOperation = OperationType.NONE;
                                
                                infoArea.append(i18n("text.replaced")); // "\nSelected text has been replaced."
                                hideAllButtons();
                                // resultArea.setText(""); // Clear result area
                                
                            } catch (Exception ex) {
                                PluginLogger.error("[ReplaceButtonActionListener]Error during text replacement: " + ex.getMessage());
                                infoArea.append("\n" + i18n("error.replacing.text", ex.getMessage()) + "\n"); // "\nError during text replacement: "
                            }
                        }
                        // Note: Selection validation already handled by fetchSelectedText() at method start
                    }
                    // Note: fetchSelectedText already handles non-text mode case
                }
                // Note: fetchSelectedText already handles no editor case
            } catch (Exception ex) {
                PluginLogger.error("[ReplaceButtonActionListener]Error accessing editor: " + ex.getMessage());
                setResultInformational(i18n("error.accessing.editor", ex.getMessage())); // "Error accessing editor: {0}"
            }
        }

        private void handleRefToLinkReplace() {
            if (currentRefToLinkUrl == null || currentRefToLinkUrl.trim().isEmpty()) {
                resultArea.setText(i18n("error.no.results"));
                showConvertButton();
                return;
            }

            String selectedText = fetchSelectedRefToLinkText();
            if (selectedText.isEmpty()) {
                return;
            }

            String rewritten;
            try {
                rewritten = new RefElementRewriter().rewrite(selectedText, currentRefToLinkUrl);
            } catch (InvalidReferenceException ex) {
                resultArea.setText(i18n(ex.getMessageKey(), ex.getParams()));
                showConvertButton();
                return;
            }

            try {
                if (replaceSelectionWithText(rewritten)) {
                    currentOperation = OperationType.NONE;
                    currentRefToLinkUrl = null;
                    currentRefToLinkSelection = null;
                    infoArea.append(i18n("success.replacement.complete") + "\n");
                    hideAllButtons();
                }
            } catch (Exception ex) {
                PluginLogger.error("[ReplaceButtonActionListener]Ref-to-link replacement error: " + ex.getMessage());
                setResultInformational(i18n("error.replacing.text", ex.getMessage()));
            }
        }
    }
    
    /**
     * Transfer button action listener for UTF-8 conversion(i18n)
     */
    private class TransferButtonActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            PluginLogger.info("[TransferButtonActionListener]Transfer button triggered");
            
            if (currentNonUtf8Files == null || currentNonUtf8Files.isEmpty()) {
                infoArea.append(i18n("no.files.to.convert") + "\n"); // "\nNo files to convert."
                return;
            }
            
            // Set operation context
            currentOperation = OperationType.UTF8_CONVERT;
            
            infoArea.append(i18n("utf8.Converting") + "\n"); // "Converting files to UTF-8..."
            hideTransferButtons();
            
            // Convert files in background
            CompletableFuture.supplyAsync(() -> convertFilesToUtf8(currentNonUtf8Files), executor)
                .thenAccept(conversionResults -> SwingUtilities.invokeLater(() -> 
                    displayConversionResults(conversionResults)))
                .exceptionally(throwable -> {
                    SwingUtilities.invokeLater(() -> {
                        setResultInformational(i18n("error.converting.files", throwable.getMessage())); // "Error converting files: {0}"
                    });
                    return null;
                });
        }
    }

    /**
     * Cancel button action listener(i18n)
     */
    private class CancelButtonActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            PluginLogger.info("[CancelButtonActionListener]Cancel button triggered");
            
            // Reset operation context
            currentOperation = OperationType.NONE;
            
            currentNonUtf8Files = null;
            hideTransferButtons();
            infoArea.append(i18n("utf8.conversion.cancelled")); // "UTF-8 conversion cancelled."
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
            PluginLogger.warn("[i18n]Resources not available for i18n key: " + key);
            return key;
        } catch (Exception e) {
            PluginLogger.error("[i18n]Error getting i18n message for key " + key + ": " + e.getMessage());
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
            PluginLogger.error("[i18n]Error formatting i18n message for key " + key + ": " + e.getMessage());
            return key;
        }
    }
    
    /**
     * Fetch selected text from current editor(i18n)
     */
    private String fetchSelectedRefToLinkText() {
        try {
            WSEditor editorAccess = pluginWorkspaceAccess.getCurrentEditorAccess(PluginWorkspace.MAIN_EDITING_AREA);
            if (editorAccess != null) {
                WSEditorPage pageAccess = editorAccess.getCurrentPage();
                if (pageAccess instanceof WSTextEditorPage) {
                    WSTextEditorPage textPage = (WSTextEditorPage) pageAccess;
                    String selectedText = textPage.getSelectedText();
                    if (selectedText != null && !selectedText.trim().isEmpty()) {
                        try {
                            Document doc = XmlDomUtils.parseXml(selectedText.trim());
                            Element root = doc.getDocumentElement();
                            if (root == null || !isRefElement(root)) {
                                resultArea.setText(i18n("error.not.ref.element"));
                                return "";
                            }
                        } catch (Exception ex) {
                            resultArea.setText(i18n("error.invalid.xml"));
                            return "";
                        }
                        return selectedText;
                    }
                    resultArea.setText(i18n("error.no.selection"));
                    return "";
                }
                resultArea.setText(i18n("not.text.mode") + "\n");
                return "";
            }
            resultArea.setText(i18n("no.editor.open") + "\n");
            return "";
        } catch (Exception e) {
            resultArea.setText(i18n("error.fetching.selected.text", e.getMessage()) + "\n");
            return "";
        }
    }

    private static boolean isRefElement(Element element) {
        String local = element.getLocalName();
        if ("ref".equals(local)) {
            return true;
        }
        String name = element.getNodeName();
        return "ref".equals(name) || name.endsWith(":ref");
    }

    private String fetchSelectedText(JTextArea area) {
        try {
            WSEditor editorAccess = pluginWorkspaceAccess.getCurrentEditorAccess(PluginWorkspace.MAIN_EDITING_AREA);
            if (editorAccess != null) {
                WSEditorPage pageAccess = editorAccess.getCurrentPage();
                if (pageAccess instanceof WSTextEditorPage) {
                    WSTextEditorPage textPage = (WSTextEditorPage) pageAccess;
                    String selectedText = textPage.getSelectedText();
                    if (selectedText != null && !selectedText.trim().isEmpty()) {
                        PluginLogger.info("[fetchSelectedText]Current page is Text mode: " + selectedText);
                        return selectedText;
                    } else {
                        area.append(i18n("no.text.selected") + "\n"); // [shared] "No text selected in the editor."
                        PluginLogger.warn("[fetchSelectedText]No text selected in the editor");
                        return ""; // No text selected
                    }
                } else {
                    area.append(i18n("not.text.mode") + "\n"); // "Current page is not Text mode."
                    PluginLogger.warn("[fetchSelectedText]Current page is not Text mode");
                    return ""; // Non-text mode
                }
            }
            area.append(i18n("no.editor.open") + "\n");  // [shared] "No open Text mode editor."
            PluginLogger.warn("[fetchSelectedText]No open editor");
            return ""; // No editor open
        } catch (Exception e) {
            area.append(i18n("error.fetching.selected.text", e.getMessage()) + "\n");  // "Error fetching selected text: {0}"
            PluginLogger.error("[fetchSelectedText]Error fetching selected text: " + e.getMessage());
            return "";
        }
    }

    private boolean replaceSelectionWithText(String replacementText) throws Exception {
        WSEditor editorAccess = pluginWorkspaceAccess.getCurrentEditorAccess(PluginWorkspace.MAIN_EDITING_AREA);
        if (editorAccess == null) {
            infoArea.append(i18n("no.editor.open") + "\n");
            return false;
        }

        WSEditorPage pageAccess = editorAccess.getCurrentPage();
        if (!(pageAccess instanceof WSTextEditorPage)) {
            infoArea.append(i18n("not.text.mode") + "\n");
            return false;
        }

        return replaceSelectionText((WSTextEditorPage) pageAccess, replacementText);
    }

    boolean replaceSelectionText(WSTextEditorPage textPage, String replacementText) throws Exception {
        int selectionStart = textPage.getSelectionStart();
        int selectionEnd = textPage.getSelectionEnd();

        if (selectionStart == selectionEnd) {
            return false;
        }

        textPage.deleteSelection();

        int currentOffset = textPage.getCaretOffset();
        javax.swing.text.Document doc = textPage.getDocument();
        doc.insertString(currentOffset, replacementText, null);
        return true;
    }
    
    /**
     * Show only replace button
     */
    private void showReplaceButton() {
        convertButton.setVisible(false);
        transferButton.setVisible(false);
        cancelButton.setVisible(false);
        replaceButton.setVisible(true);
        buttonPanel.setVisible(true);
        buttonPanel.revalidate();
        buttonPanel.repaint();
    }

    private void showConvertButton() {
        replaceButton.setVisible(false);
        transferButton.setVisible(false);
        cancelButton.setVisible(false);
        convertButton.setVisible(true);
        convertButton.setEnabled(true);
        buttonPanel.setVisible(true);
        buttonPanel.revalidate();
        buttonPanel.repaint();
    }

    private void showRefToLinkConvertingState() {
        replaceButton.setVisible(false);
        transferButton.setVisible(false);
        cancelButton.setVisible(false);
        convertButton.setVisible(true);
        convertButton.setEnabled(false);
        buttonPanel.setVisible(true);
        buttonPanel.revalidate();
        buttonPanel.repaint();
    }

    private void showConvertAndReplaceButtons() {
        transferButton.setVisible(false);
        cancelButton.setVisible(false);
        convertButton.setVisible(true);
        replaceButton.setVisible(true);
        convertButton.setEnabled(true);
        replaceButton.setEnabled(true);
        buttonPanel.setVisible(true);
        buttonPanel.revalidate();
        buttonPanel.repaint();
    }
    
    /**
     * Set result text and show replace button only if result is valid (non-empty and not an error)
    * 
    * Usage: Call after AI Markup or Tag Removal operations
    * - Shows Replace button if result contains valid markup/text
    * - Hides all buttons if result is error or empty
    * 
    * @param result The processed text or error message
    */
    private void setResultWithReplaceButton(String result) {
        String safeResult = (result != null) ? result : "";
        PluginLogger.info("[setResultWithReplaceButton]Setting result (length: " + safeResult.length() + ")");
        boolean isValid = isValidResultForReplacement(safeResult);
        resultArea.setText(safeResult);
        if (isValid) {
            PluginLogger.info("[setResultWithReplaceButton]Showing replace button");
            showReplaceButton();
        } else {
            PluginLogger.info("[setResultWithReplaceButton]Hiding all buttons");
            hideAllButtons();
        }
    }
    
    /**
     * Set result text and show conversion buttons if result indicates files need conversion
     * 
     * Usage: Call after UTF-8 Check operation completes
     * - Shows Transfer/Cancel buttons if non-UTF-8 files were found
     * - Hides all buttons if all files are valid UTF-8 or error occurred
     * 
     * @param result The UTF-8 check results or error message
     */
    private void setResultWithConversionButtons(String result) {
        String safeResult = (result != null) ? result : "";
        PluginLogger.info("[setResultWithConversionButtons]Setting result (length: " + safeResult.length() + ")");
        boolean isValid = isValidResultForConversion(safeResult);
        resultArea.setText(safeResult);
        if (isValid) {
            PluginLogger.info("[setResultWithConversionButtons]Showing transfer buttons");
            showTransferButtons();
        } else {
            PluginLogger.info("[setResultWithConversionButtons]Hiding all buttons");
            hideAllButtons();
        }
    }
    
    /**
     * Display informational result without action buttons
     * 
     * Usage: Call for completion messages or informational status
     * - UTF-8 conversion completed summaries
     * - Error messages that don't require user action
     * - Final operation results
     * 
     * Note: Currently appends to resultArea instead of replacing.
     * This is intentional for showing conversion summaries after file lists.
     * 
     * @param result The informational message to display
     */
    private void setResultInformational(String result) {
        String safeResult = (result != null) ? result : "";
        PluginLogger.info("[setResultInformational]Setting informational result (length: " + safeResult.length() + ")");
        resultArea.append(safeResult);
        hideAllButtons();
    }
    
    /**
     * Check if result is valid for replacement (not empty, null, or error message)
     * Only AI Markup and Tag Removal results should be valid for replacement
     * Uses operation context to determine validity
     */
    private boolean isValidResultForReplacement(String result) {
        if (result == null || result.trim().isEmpty()) {
            return false;
        }
        
        // Check for error patterns in both English and i18n messages
        if (isErrorMessage(result)) {
            return false;
        }
        
        // Use operation context: only AI_MARKUP and TAG_REMOVAL should show replace button
        if (currentOperation == OperationType.AI_MARKUP || 
            currentOperation == OperationType.TAG_REMOVAL) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if result is valid for UTF-8 conversion (shows non-UTF-8 files that need conversion)
     * Uses operation context to determine validity, avoiding fragile string matching
     */
    private boolean isValidResultForConversion(String result) {
        if (result == null || result.trim().isEmpty()) {
            return false;
        }
        
        // Error messages should not show conversion buttons
        if (isErrorMessage(result)) {
            return false;
        }
        
        // Use operation context: only UTF8_CHECK should show conversion buttons
        // and only if there are files that need conversion
        if (currentOperation == OperationType.UTF8_CHECK 
            && currentNonUtf8Files != null 
            && !currentNonUtf8Files.isEmpty()) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if a result string represents an error message (considering both English fallback and i18n)
     */
    private boolean isErrorMessage(String result) {
        if (result == null) {
            return false;
        }
        
        // Check for i18n error messages by comparing with known error keys
        String[] errorKeys = {
            "error.processing.ai.markup",
            "error.checking.utf8", 
            "error.converting.files",
            "error.no.APIKey",
            "error.no.parse.model",
            "llm.error",
            "error.replacing.text",
            "error.accessing.editor",

            // Ref to Link errors (001-ref-to-link-action)
            "error.no.selection",
            "error.invalid.xml",
            "error.not.ref.element",
            "error.missing.canon",
            "error.missing.volume",
            "error.missing.page",
            "error.unknown.canon",
            "error.invalid.numerals",
            "error.unknown.column",
            "error.api.timeout",
            "error.api.http",
            "error.api.connection",
            "error.api.response",
            "error.no.results"
        };
        
        for (String errorKey : errorKeys) {
            String errorMessage = i18n(errorKey, "").split("\\{")[0].trim(); // Get message without parameter placeholders
            if (!errorMessage.equals(errorKey) && result.startsWith(errorMessage)) {
                return true;
            }
        }
        
        // Fallback: Check for common English error patterns only if i18n failed to load
        String lowerResult = result.toLowerCase();
        if (lowerResult.startsWith("error") || 
            lowerResult.startsWith("http error") || 
            lowerResult.contains("exception") ||
            lowerResult.contains("failed")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Show transfer and cancel buttons
     */
    private void showTransferButtons() {
        replaceButton.setVisible(false);
        convertButton.setVisible(false);
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
     * Process AI markup with LLM API integration(i18n)
     */
    private String processAIMarkup(String text) {
        try {
            // Get API configuration from options storage
            String apiKey = optionStorage.getSecretOption("dila.dama.api.key", "");
            String parseModel = optionStorage.getOption("dila.dama.ft.parse.model", "");
            

            StringBuilder errors = new StringBuilder();
            
            if (apiKey == null || apiKey.trim().isEmpty()) {
                PluginLogger.warn("[processAIMarkup]API key not configured");
                errors.append(i18n("error.no.APIKey")); // "Error: API key not configured. Please set up your API key in Preferences."
            }
            
            if (parseModel == null || parseModel.trim().isEmpty()) {
                PluginLogger.warn("[processAIMarkup]Parse model not configured");
                if (errors.length() > 0) {
                    errors.append("\n");
                }
                errors.append(i18n("error.no.parse.model")); // "Error: Parse model not configured. Please set up your model in Preferences."
            }
            
            if (errors.length() > 0) {
                return errors.toString();
            }
            
            PluginLogger.info("[processAIMarkup]Making AI Markup API call with model: " + parseModel);
            
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
                PluginLogger.error("[processAIMarkup]AI Markup HTTP error response: " + errorResponse.toString());
                // return "HTTP Error " + responseCode + ": " + errorResponse.toString();
                return i18n("http.error", responseCode, errorResponse.toString()); // "HTTP Error {0}: {1}"
            }
            
        } catch (Exception e) {
            PluginLogger.error("[processAIMarkup]AI Markup error: " + e.getMessage());
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
            PluginLogger.error("[processAIMarkup]Error parsing OpenAI response: " + e.getMessage());
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

        PluginLogger.info("[processTagRemoval]Processing tag removal for text length: " + text.length());

        // Remove XML/HTML tags using regex
        String cleanedText = text.replaceAll("<[^>]*>", "");

        PluginLogger.info("[processTagRemoval]Tag removal completed. Original length: " + text.length() +
                ", cleaned length: " + cleanedText.length());
        
        return cleanedText;
    }
    
    /**
     * Check UTF-8 encoding of files using Java service
     */
    private Utf8CheckResult checkUtf8Files(File[] selectedFiles) {
        List<Path> nonUtf8Files = new ArrayList<>();
        int totalFiles = 0;
        
        for (File file : selectedFiles) {
            totalFiles += scanFileOrDirectory(file.toPath(), nonUtf8Files);
        }
        
        PluginLogger.info("[checkUtf8Files]UTF-8 check completed. Total files: " + totalFiles + ", Non-UTF-8: " + nonUtf8Files.size());
        return new Utf8CheckResult(nonUtf8Files, totalFiles);
    }
    
    /**
     * Recursively scan files and directories
     */
    private int scanFileOrDirectory(Path path, List<Path> nonUtf8Files) {
        int count = 0;
        
        try {
            if (Files.isDirectory(path)) {
                // Collect all text files first to avoid stream reuse issues
                List<Path> textFiles;
                try (java.util.stream.Stream<Path> stream = Files.walk(path)) {
                    textFiles = stream
                        .filter(Files::isRegularFile)
                        .filter(this::isTextFile)
                        .collect(java.util.stream.Collectors.toList());
                }
                
                // Check each file for UTF-8 encoding
                count = textFiles.size();
                for (Path file : textFiles) {
                    if (!UTF8ValidationService.isValidUtf8(file)) {
                        nonUtf8Files.add(file);
                    }
                }
            } else if (Files.isRegularFile(path) && isTextFile(path)) {
                count = 1;
                if (!UTF8ValidationService.isValidUtf8(path)) {
                    nonUtf8Files.add(path);
                }
            }
        } catch (IOException e) {
            PluginLogger.error("[scanFileOrDirectory]Error scanning path " + path + ": " + e.getMessage());
        }
        
        return count;
    }
    
    /**
     * Check if file is likely a text file
     */
    private boolean isTextFile(Path path) {
        PluginLogger.info("[isTextFile]Checking if file is a text file: " + path);
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
     * Display UTF-8 check results(i18n)
     */
    private void displayUtf8CheckResults(Utf8CheckResult result) {
        List<Path> nonUtf8Files = result.getNonUtf8Files();
        int totalFiles = result.getTotalFiles();
        
        if (nonUtf8Files.isEmpty()) {
            // infoArea.append(i18n("utf8.all.valid") + "\n"); // "All files are valid UTF-8!"
            setResultInformational(i18n("utf8.all.valid")); // "All files are valid UTF-8!"
            PluginLogger.info("[displayUtf8CheckResults]All files are valid UTF-8 (Total scanned: " + totalFiles + ")");
            return;
        }
        
        currentNonUtf8Files = nonUtf8Files;
        
        StringBuilder info = new StringBuilder();
        info.append(i18n("utf8.check.found.non.utf8", nonUtf8Files.size(), totalFiles)).append("\n"); // "Found {0} files that are not valid UTF-8 out of {1} files:"
        
        int displayCount = Math.min(10, nonUtf8Files.size());
        for (int i = 0; i < displayCount; i++) {
            info.append("• ").append(nonUtf8Files.get(i).toString()).append("\n");
        }
        
        if (nonUtf8Files.size() > 10) {
            info.append(i18n("more.files.to.convert", nonUtf8Files.size() - 10)); // "... and {0} more files"
        }
        
        // infoArea.setText(info.toString());
        PluginLogger.info("[displayUtf8CheckResults]UTF-8 check results: " + info.toString());
        setResultWithConversionButtons(info.toString()); // This will show transfer/cancel buttons if validation passes
    }
    
    /**
     * Convert files to UTF-8(i18n)
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
        
        PluginLogger.info("[convertFilesToUtf8]UTF-8 conversion summary: Successes=" + successCount + ", Failures=" + failureCount);        
        PluginLogger.info("[convertFilesToUtf8]UTF-8 conversion results:\n" + results.toString());
        
        // Fix: Handle empty results case and ensure proper formatting
        String detailsText = results.length() > 0 ? results.toString() : i18n("utf8.conversion.no.details");
        
        // Fix: Use a more structured approach to build the summary message
        StringBuilder summaryBuilder = new StringBuilder();
        summaryBuilder.append("\n").append(i18n("utf8.conversion.summary.header")).append("\n\n");
        summaryBuilder.append(i18n("utf8.conversion.success.count", String.valueOf(successCount))).append("\n");
        summaryBuilder.append(i18n("utf8.conversion.failure.count", String.valueOf(failureCount))).append("\n\n");
        summaryBuilder.append(i18n("utf8.conversion.details.header")).append("\n");
        summaryBuilder.append(detailsText);
        
        return summaryBuilder.toString();
    } 
    
    /**
     * Display conversion results
     */
    private void displayConversionResults(String results) {
        PluginLogger.info("[displayConversionResults]Displaying conversion results: " + results);
        
        // Reset operation context after conversion completes
        currentOperation = OperationType.NONE;
        
        setResultInformational(results); // Conversion results are informational, no action buttons needed
        infoArea.append("\n" + i18n("utf8.conversion.completed") + "\n\n"); // "UTF-8 conversion completed."
        resultArea.append("\n" + i18n("utf8.conversion.backing.up") + "\n"); // "The original files are backed up under the same directory:"
        currentNonUtf8Files = null;
    }
    
    /**
     * Called when the application is closing - implements WorkspaceAccessPluginExtension
     */
    @Override
    public boolean applicationClosing() {
        PluginLogger.info("[applicationClosing]Closing DILA AI Markup plugin (Pure Java Implementation)");
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        return true;
    }
}
