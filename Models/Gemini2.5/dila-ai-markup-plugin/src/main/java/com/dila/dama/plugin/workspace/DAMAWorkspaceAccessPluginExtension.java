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
import com.dila.dama.plugin.application.command.RunAiMarkupDiagnosticsCommand;
import com.dila.dama.plugin.application.command.ConvertReferenceCommand;
import com.dila.dama.plugin.application.command.ConvertReferenceResult;
import com.dila.dama.plugin.application.query.BuildDiagnosticExportQuery;
import com.dila.dama.plugin.application.query.LoadReleaseNotesQuery;
import com.dila.dama.plugin.domain.model.AiMarkupDiagnosticSession;
import com.dila.dama.plugin.domain.model.CbrdParseConfiguration;
import com.dila.dama.plugin.domain.model.InvalidReferenceException;
import com.dila.dama.plugin.domain.model.SanitizedTroubleshootingRecord;
import com.dila.dama.plugin.infrastructure.export.DiagnosticExportWriter;
import com.dila.dama.plugin.domain.service.DocumentLanguageResolver;
import com.dila.dama.plugin.domain.service.RefElementRewriter;
import com.dila.dama.plugin.domain.service.ReferenceParser;
import com.dila.dama.plugin.domain.service.RequestValidationService;
import com.dila.dama.plugin.infrastructure.api.CBRDAPIClient;
import com.dila.dama.plugin.infrastructure.api.CbrdParseRequest;
import com.dila.dama.plugin.infrastructure.api.HttpUrlConnectionFactory;
import com.dila.dama.plugin.preferences.DAMAOptionPagePluginExtension;
import com.dila.dama.plugin.util.PluginLogger;
import com.dila.dama.plugin.util.XmlDomUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.URI;
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
    static final String USER_MANUAL_URL = "https://docs.google.com/document/d/1JHWAu4KJ6eb-UZhh-uYW8HbzsKc6fD5i_lVKTQWj9HQ/edit?usp=sharing";
    static final List<String> OPTIONS_MENU_ITEM_KEYS = Collections.unmodifiableList(Arrays.asList(
        "menuItem.preferences",
        "menuItem.user.manual",
        "menuItem.about"
    ));
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
    private JButton exportButton;

    // Ref-to-Link workflow state (in-memory only)
    private String currentRefToLinkUrl;
    private String currentRefToLinkSelection;
    
    // UTF-8 workflow state
    private List<Path> currentNonUtf8Files = null;
    private volatile boolean aiMarkupInProgress = false;

    /** The selection the in-flight operation is processing, shown when a second one is refused (FR-015). */
    private volatile String inFlightAiMarkupSelection = "";

    /** Cancel handle for the in-flight parse request (FR-020). */
    private volatile CompletableFuture<?> inFlightAiMarkupFuture;

    /** Set when an in-flight operation was abandoned, so a late result is discarded silently (FR-020). */
    private volatile boolean aiMarkupCancelled = false;

    private AiMarkupDiagnosticSession lastDiagnosticSession;
    private SanitizedTroubleshootingRecord lastTroubleshootingRecord;

    private final RequestValidationService requestValidationService = new RequestValidationService();
    private final DocumentLanguageResolver documentLanguageResolver = new DocumentLanguageResolver();

    private RunAiMarkupDiagnosticsCommand aiMarkupDiagnosticsCommand = new RunAiMarkupDiagnosticsCommand();
    private BuildDiagnosticExportQuery diagnosticExportQuery = new BuildDiagnosticExportQuery();
    private LoadReleaseNotesQuery loadReleaseNotesQuery = new LoadReleaseNotesQuery();
    private DiagnosticExportWriter diagnosticExportWriter = new DiagnosticExportWriter();
    private ExternalUrlOpener externalUrlOpener = new DesktopExternalUrlOpener();
    private AboutDialogPresenter aboutDialogPresenter = new SwingAboutDialogPresenter();
    private String pluginVersionOverrideForTests;
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

    interface ExternalUrlOpener {
        void open(String url) throws Exception;
    }

    interface AboutDialogPresenter {
        void show(String title, String html);
    }

    private static class DesktopExternalUrlOpener implements ExternalUrlOpener {
        @Override
        public void open(String url) throws Exception {
            if (!Desktop.isDesktopSupported()) {
                throw new UnsupportedOperationException("Desktop browsing is not supported");
            }
            Desktop.getDesktop().browse(new URI(url));
        }
    }

    private static class SwingAboutDialogPresenter implements AboutDialogPresenter {
        @Override
        public void show(String title, String html) {
            JEditorPane editorPane = new JEditorPane("text/html", html);
            editorPane.setEditable(false);
            editorPane.setCaretPosition(0);
            JScrollPane scrollPane = new JScrollPane(editorPane);
            scrollPane.setPreferredSize(new Dimension(520, 420));
            JOptionPane.showMessageDialog(null, scrollPane, title, JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
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
        JMenuItem menuItemUserManual = new JMenuItem(i18n("menuItem.user.manual"));
        JMenuItem menuItemAbout = new JMenuItem(i18n("menuItem.about"));
        menuOptions.add(menuItemOption);
        menuOptions.add(menuItemUserManual);
        menuOptions.add(menuItemAbout);
        menuBar.add(menuOptions);
        
        // Add action listeners
        menuItemActionAIMarkup.addActionListener(new AIMarkupActionListener());
        menuItemActionTagRemoval.addActionListener(new TagRemovalActionListener());
        menuItemRefToLink.addActionListener(new RefToLinkActionListener());
        menuItemUtf8Check.addActionListener(new UTF8CheckActionListener());
        menuItemOption.addActionListener(new OptionsActionListener());
        menuItemUserManual.addActionListener(new UserManualActionListener());
        menuItemAbout.addActionListener(new AboutActionListener());
        
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
        exportButton = new JButton(i18n("button.export.diagnostics"));

        buttonPanel.add(replaceButton);
        buttonPanel.add(convertButton);
        buttonPanel.add(exportButton);
        buttonPanel.add(transferButton);
        buttonPanel.add(cancelButton);
        
        // Initially hide all buttons
        buttonPanel.setVisible(false);
        
        // Add action listeners
        replaceButton.addActionListener(new ReplaceButtonActionListener());
        convertButton.addActionListener(new ConvertButtonActionListener());
        exportButton.addActionListener(new ExportDiagnosticsButtonActionListener());
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

            // A second invocation is ignored, and the editor is told which selection is still
            // being processed (FR-015). Checked before anything is cleared so the in-flight
            // result area is not wiped by the ignored click.
            if (aiMarkupInProgress) {
                showAiMarkupAlreadyInProgress();
                return;
            }

            // Set operation context
            currentOperation = OperationType.AI_MARKUP;
            lastDiagnosticSession = null;
            lastTroubleshootingRecord = null;

            // Clear previous results
            infoArea.setText(i18n("action.ai.markup.selected") + "\n\n"); // "Action selected: AI Markup (Use AI for reference tagging)"
            resultArea.setText("");
            hideAllButtons();

            // Get selected text from current editor
            String selectedText = fetchSelectedText(resultArea);
            if (selectedText.isEmpty()) {
                // fetchSelectedText already reported "no text selected".
                return;
            }

            // Pre-flight: missing token (FR-010), unusable endpoint URL (FR-021), empty or
            // over-long selection (FR-019). Each names its own cause and sends no request.
            String preflightGuidance = aiMarkupPreflightGuidance(selectedText);
            if (preflightGuidance != null) {
                PluginLogger.warn("[AIMarkupActionListener]Pre-flight refused the request");
                resultArea.setText(preflightGuidance);
                hideAllButtons();
                return;
            }

            if (!tryStartAiMarkupOperation(selectedText)) {
                showAiMarkupAlreadyInProgress();
                return;
            }

            infoArea.append(i18n("selected.text", selectedText) + "\n" // "Selected text: "
                            + i18n("text.with.length", selectedText.length()) + "\n\n"); // "Text length: {0} characters"
            resultArea.setText(i18n("ai.markup.diagnostic.processing"));

            // Process AI markup in background
            CompletableFuture<RunAiMarkupDiagnosticsCommand.Result> inFlight =
                CompletableFuture.supplyAsync(() -> runAiMarkup(selectedText), executor);
            inFlightAiMarkupFuture = inFlight;
            inFlight
                .thenAccept(result -> SwingUtilities.invokeLater(() -> {
                    completeAiMarkupOperation(result);
                }))
                .exceptionally(throwable -> {
                    SwingUtilities.invokeLater(() -> {
                        if (!aiMarkupCancelled) {
                            resultArea.setText(i18n("error.processing.ai.markup", throwable.getMessage()));
                            hideAllButtons();
                        }
                        finishAiMarkupOperation();
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
        String apiUrl = "https://cbss.dila.edu.tw/cbrd/link";
        String referer = "CBRD@dila.edu.tw";
        int timeoutMs = 10000;
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
                    timeoutMs = 10000;
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

    private class UserManualActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            PluginLogger.info("[UserManualActionListener]User manual action triggered");
            try {
                externalUrlOpener.open(USER_MANUAL_URL);
            } catch (Exception ex) {
                PluginLogger.error("[UserManualActionListener]Error opening user manual: " + ex.getMessage(), ex);
            }
        }
    }

    private class AboutActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            PluginLogger.info("[AboutActionListener]About action triggered");
            aboutDialogPresenter.show(i18n("dialog.about.title"), buildAboutDialogHtml());
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

    private String buildAboutDialogHtml() {
        LoadReleaseNotesQuery.Result releaseNotes = loadReleaseNotesQuery.execute(
            resolvePluginVersion(),
            i18n("about.release.notes.unavailable")
        );

        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family:sans-serif; padding:8px;'>");
        html.append("<h2>").append(escapeHtml(i18n("dialog.about.title"))).append("</h2>");
        html.append("<p><strong>")
            .append(escapeHtml(i18n("about.version.label")))
            .append("</strong> ")
            .append(escapeHtml(releaseNotes.getPluginVersion()))
            .append("</p>");
        if (releaseNotes.isFallbackUsed()) {
            html.append("<h3>").append(escapeHtml(i18n("about.release.notes.heading"))).append("</h3>");
            html.append("<p>").append(escapeHtml(releaseNotes.getReleaseNotesMarkup())).append("</p>");
        } else {
            html.append(releaseNotes.getReleaseNotesMarkup());
        }
        html.append("</body></html>");
        return html.toString();
    }

    private String resolvePluginVersion() {
        if (pluginVersionOverrideForTests != null && !pluginVersionOverrideForTests.trim().isEmpty()) {
            return pluginVersionOverrideForTests.trim();
        }

        Package pluginPackage = getClass().getPackage();
        if (pluginPackage != null) {
            String implementationVersion = pluginPackage.getImplementationVersion();
            if (implementationVersion != null && !implementationVersion.trim().isEmpty()) {
                return implementationVersion.trim();
            }
        }

        InputStream stream = getClass().getClassLoader()
            .getResourceAsStream("META-INF/maven/dila/dila-ai-markup-plugin/pom.properties");
        if (stream != null) {
            Properties properties = new Properties();
            try {
                properties.load(stream);
                String version = properties.getProperty("version");
                if (version != null && !version.trim().isEmpty()) {
                    return version.trim();
                }
            } catch (Exception e) {
                PluginLogger.warn("[resolvePluginVersion]Failed to load pom.properties: " + e.getMessage());
            }
        }

        return "unknown";
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
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
        exportButton.setVisible(false);
        transferButton.setVisible(false);
        cancelButton.setVisible(false);
        replaceButton.setVisible(true);
        buttonPanel.setVisible(true);
        buttonPanel.revalidate();
        buttonPanel.repaint();
    }

    private void showConvertButton() {
        replaceButton.setVisible(false);
        exportButton.setVisible(false);
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
        exportButton.setVisible(false);
        transferButton.setVisible(false);
        cancelButton.setVisible(false);
        convertButton.setVisible(true);
        convertButton.setEnabled(false);
        buttonPanel.setVisible(true);
        buttonPanel.revalidate();
        buttonPanel.repaint();
    }

    private void showConvertAndReplaceButtons() {
        exportButton.setVisible(false);
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

    private void showExportButton() {
        replaceButton.setVisible(false);
        convertButton.setVisible(false);
        transferButton.setVisible(false);
        cancelButton.setVisible(false);
        exportButton.setVisible(true);
        exportButton.setEnabled(true);
        buttonPanel.setVisible(true);
        buttonPanel.revalidate();
        buttonPanel.repaint();
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
            // CBRD Parse guidance (004-cbrd-parse-endpoint) - these must be recognised as
            // errors so a failure message never gets offered for replacement.
            "ai.markup.error.text_is_required",
            "ai.markup.error.text_is_too_long",
            "ai.markup.error.unsupported_language",
            "ai.markup.error.unauthorized",
            "ai.markup.error.parse_api_not_configured",
            "ai.markup.error.openai_credentials_unavailable",
            "ai.markup.error.openai_rate_limited",
            "ai.markup.error.openai_unavailable",
            "ai.markup.error.invalid_model_output",
            "ai.markup.error.unexpected",
            "ai.markup.error.connectivity",
            "ai.markup.error.token_not_configured",
            "ai.markup.error.endpoint_url_invalid",
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
        exportButton.setVisible(false);
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
        exportButton.setVisible(false);
        buttonPanel.setVisible(false);
        buttonPanel.revalidate();
        buttonPanel.repaint();
    }
    
    /**
     * Hide all buttons
     */
    private void hideAllButtons() {
        exportButton.setVisible(false);
        buttonPanel.setVisible(false);
        buttonPanel.revalidate();
        buttonPanel.repaint();
    }
    
    // ========================================
    // Processing Methods (Phase 2-4 implementation stubs)
    // ========================================

    private synchronized boolean tryStartAiMarkupOperation() {
        return tryStartAiMarkupOperation("");
    }

    /**
     * Single-flight guard. Records the selection being processed so a refused second invocation
     * can show the editor which one is in flight (FR-015).
     */
    private synchronized boolean tryStartAiMarkupOperation(String selectedText) {
        if (aiMarkupInProgress) {
            return false;
        }
        aiMarkupInProgress = true;
        aiMarkupCancelled = false;
        inFlightAiMarkupSelection = selectedText == null ? "" : selectedText;
        return true;
    }

    private synchronized void finishAiMarkupOperation() {
        aiMarkupInProgress = false;
        inFlightAiMarkupFuture = null;
    }

    /**
     * Reports that AI Markup is already running, together with the selection being processed,
     * so the editor can tell which one it is (FR-015).
     */
    void showAiMarkupAlreadyInProgress() {
        String message = i18n("ai.markup.diagnostic.in.progress");
        String inFlight = inFlightAiMarkupSelection;
        resultArea.setText(inFlight == null || inFlight.isEmpty() ? message : message + "\n\n" + inFlight);
        hideAllButtons();
    }

    /**
     * Cancels the in-flight parse request and marks its result for silent discard, so nothing is
     * written into a document the editor has closed (FR-020).
     */
    void cancelInFlightAiMarkup() {
        CompletableFuture<?> inFlight = inFlightAiMarkupFuture;
        if (inFlight != null && !inFlight.isDone()) {
            PluginLogger.info("[cancelInFlightAiMarkup]Cancelling in-flight AI Markup request");
            inFlight.cancel(true);
        }
        if (aiMarkupInProgress) {
            aiMarkupCancelled = true;
        }
        finishAiMarkupOperation();
    }

    RunAiMarkupDiagnosticsCommand.Result runAiMarkup(String text) {
        CbrdParseConfiguration configuration = buildAiMarkupConfiguration();
        CbrdParseRequest request = new CbrdParseRequest(text, resolveAiMarkupLanguage());
        return aiMarkupDiagnosticsCommand.execute(request, configuration, determinePlatform());
    }

    /**
     * Returns the guidance to show instead of sending a request, or null when the request may
     * proceed (FR-010, FR-019, FR-021).
     */
    String aiMarkupPreflightGuidance(String selectedText) {
        RequestValidationService.ValidationResult result =
            requestValidationService.validate(buildAiMarkupConfiguration(), selectedText);
        if (result.isValid()) {
            return null;
        }
        return resolveGuidanceMessage(result.getGuidanceMessageKey());
    }

    void completeAiMarkupOperation(RunAiMarkupDiagnosticsCommand.Result result) {
        if (aiMarkupCancelled) {
            // The document or the editor closed while this was in flight: discard silently.
            PluginLogger.info("[completeAiMarkupOperation]Discarding result for a cancelled operation");
            aiMarkupCancelled = false;
            // Withdraw any offered action too: a Replace button left over from the abandoned
            // operation would apply stale markup.
            hideAllButtons();
            finishAiMarkupOperation();
            return;
        }
        try {
            lastDiagnosticSession = result == null ? null : result.getSession();
            lastTroubleshootingRecord = result == null ? null : result.getTroubleshootingRecord();
            if (result != null && result.isSuccess()) {
                setResultWithReplaceButton(result.getMarkupResult());
            } else if (result != null) {
                resultArea.setText(resolveGuidanceMessage(result.getSummaryMessageKey()));
                if (lastTroubleshootingRecord != null) {
                    showExportButton();
                } else {
                    hideAllButtons();
                }
            } else {
                resultArea.setText(i18n("ai.markup.diagnostic.unknown"));
                hideAllButtons();
            }
        } finally {
            finishAiMarkupOperation();
        }
    }

    private class ExportDiagnosticsButtonActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (lastTroubleshootingRecord == null || lastDiagnosticSession == null) {
                resultArea.setText(i18n("ai.markup.diagnostic.export.unavailable"));
                hideAllButtons();
                return;
            }

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle(i18n("ai.markup.diagnostic.export.dialog.title"));
            fileChooser.setSelectedFile(new File("ai-markup-diagnostics-" + lastDiagnosticSession.getSessionId() + ".json"));

            int result = fileChooser.showSaveDialog(null);
            if (result != JFileChooser.APPROVE_OPTION) {
                return;
            }

            final File outputFile = fileChooser.getSelectedFile();
            resultArea.setText(i18n("ai.markup.diagnostic.export.processing"));
            hideAllButtons();
            CompletableFuture.supplyAsync(() -> exportDiagnostics(outputFile, "manual_support_export"), executor)
                .thenAccept(success -> SwingUtilities.invokeLater(() -> {
                    resultArea.setText(success
                        ? i18n("ai.markup.diagnostic.export.success", outputFile.getAbsolutePath())
                        : i18n("ai.markup.diagnostic.export.failure"));
                    if (!success && lastTroubleshootingRecord != null) {
                        showExportButton();
                    }
                }))
                .exceptionally(throwable -> {
                    SwingUtilities.invokeLater(() -> {
                        resultArea.setText(i18n("ai.markup.diagnostic.export.failure.detail", throwable.getMessage()));
                        if (lastTroubleshootingRecord != null) {
                            showExportButton();
                        }
                    });
                    return null;
                });
        }
    }

    /**
     * Builds the AI Markup configuration from the CBRD Parse preferences (FR-002, FR-003, FR-016).
     *
     * The obsolete {@code dila.dama.*} OpenAI keys are never read: values left over from a
     * previous version are ignored, never migrated (FR-004, FR-005).
     */
    CbrdParseConfiguration buildAiMarkupConfiguration() {
        String endpointUrl = CbrdParseConfiguration.resolveEndpointUrl(
            optionValue(DAMAOptionPagePluginExtension.KEY_CBRD_PARSE_API_URL, CbrdParseConfiguration.DEFAULT_ENDPOINT_URL));
        int timeoutMs = CbrdParseConfiguration.resolveTimeoutMs(
            optionValue(DAMAOptionPagePluginExtension.KEY_CBRD_PARSE_TIMEOUT_MS, ""));
        String sharedToken = optionValueSecret(DAMAOptionPagePluginExtension.KEY_CBRD_PARSE_TOKEN, "");
        return new CbrdParseConfiguration(endpointUrl, timeoutMs, sharedToken);
    }

    /**
     * Resolves the request language from the document root element's {@code xml:lang} (FR-007).
     * Anything unavailable or unusable yields the Chinese default rather than blocking the call.
     */
    String resolveAiMarkupLanguage() {
        return documentLanguageResolver.resolveFromXml(fetchCurrentDocumentXml());
    }

    /**
     * Returns the current editor's full document text, or null when there is no text-mode
     * editor to read (headless tests, Author mode, no open file).
     */
    String fetchCurrentDocumentXml() {
        try {
            if (pluginWorkspaceAccess == null) {
                return null;
            }
            WSEditor editorAccess = pluginWorkspaceAccess.getCurrentEditorAccess(PluginWorkspace.MAIN_EDITING_AREA);
            if (editorAccess == null) {
                return null;
            }
            WSEditorPage pageAccess = editorAccess.getCurrentPage();
            if (!(pageAccess instanceof WSTextEditorPage)) {
                return null;
            }
            javax.swing.text.Document document = ((WSTextEditorPage) pageAccess).getDocument();
            if (document == null) {
                return null;
            }
            return document.getText(0, document.getLength());
        } catch (Exception e) {
            PluginLogger.warn("[fetchCurrentDocumentXml]Could not read the document: " + e.getMessage());
            return null;
        }
    }

    private String optionValue(String key, String defaultValue) {
        if (optionStorage == null) {
            return defaultValue;
        }
        try {
            return optionStorage.getOption(key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String optionValueSecret(String key, String defaultValue) {
        if (optionStorage == null) {
            return defaultValue;
        }
        try {
            return optionStorage.getSecretOption(key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String determinePlatform() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.contains("mac")) {
            return "macos";
        }
        if (osName.contains("win")) {
            return "windows";
        }
        return "generic";
    }

    private String resolveGuidanceMessage(String guidanceKey) {
        if (guidanceKey == null || guidanceKey.trim().isEmpty()) {
            return i18n("ai.markup.diagnostic.unknown");
        }
        String direct = i18n(guidanceKey);
        if (!guidanceKey.equals(direct)) {
            return direct;
        }
        if (guidanceKey.endsWith(".windows") || guidanceKey.endsWith(".macos")) {
            String baseKey = guidanceKey.substring(0, guidanceKey.lastIndexOf('.'));
            String fallback = i18n(baseKey);
            if (!baseKey.equals(fallback)) {
                return fallback;
            }
        }
        return direct;
    }

    boolean exportDiagnostics(File outputFile, String exportReason) {
        if (outputFile == null || lastTroubleshootingRecord == null || lastDiagnosticSession == null) {
            return false;
        }
        try {
            diagnosticExportWriter.write(
                diagnosticExportQuery.build(lastDiagnosticSession.getSessionId(), lastTroubleshootingRecord, exportReason),
                outputFile
            );
            lastDiagnosticSession.exported(
                diagnosticExportQuery.build(lastDiagnosticSession.getSessionId(), lastTroubleshootingRecord, exportReason)
            );
            return true;
        } catch (Exception e) {
            PluginLogger.error("[exportDiagnostics]Failed to export diagnostics: " + e.getMessage(), e);
            return false;
        }
    }

    void initializeUiForTests() {
        createTextAreas();
        createButtonPanel();
    }

    void setAiMarkupDiagnosticsCommandForTests(RunAiMarkupDiagnosticsCommand command) {
        this.aiMarkupDiagnosticsCommand = command;
    }

    void setDiagnosticExportQueryForTests(BuildDiagnosticExportQuery query) {
        this.diagnosticExportQuery = query;
    }

    void setDiagnosticExportWriterForTests(DiagnosticExportWriter writer) {
        this.diagnosticExportWriter = writer;
    }

    void setLoadReleaseNotesQueryForTests(LoadReleaseNotesQuery query) {
        this.loadReleaseNotesQuery = query;
    }

    void setExternalUrlOpenerForTests(ExternalUrlOpener opener) {
        this.externalUrlOpener = opener;
    }

    void setAboutDialogPresenterForTests(AboutDialogPresenter presenter) {
        this.aboutDialogPresenter = presenter;
    }

    void setPluginVersionForTests(String pluginVersion) {
        this.pluginVersionOverrideForTests = pluginVersion;
    }

    void setOptionStorageForTests(WSOptionsStorage storage) {
        this.optionStorage = storage;
    }

    JMenuBar createMenuBarForTests() {
        return createMenuBar();
    }

    String getUserManualUrlForTests() {
        return USER_MANUAL_URL;
    }

    List<String> getOptionsMenuItemKeysForTests() {
        return OPTIONS_MENU_ITEM_KEYS;
    }

    JTextArea getResultAreaForTests() {
        return resultArea;
    }

    JTextArea getInfoAreaForTests() {
        return infoArea;
    }

    JButton getReplaceButtonForTests() {
        return replaceButton;
    }

    /**
     * The button panel is the real "are any actions offered" signal: {@code hideAllButtons}
     * hides the panel, and individual buttons keep their own flags.
     */
    boolean isButtonPanelVisibleForTests() {
        return buttonPanel.isVisible();
    }

    JButton getExportButtonForTests() {
        return exportButton;
    }

    boolean tryStartAiMarkupOperationForTests(String selectedText) {
        return tryStartAiMarkupOperation(selectedText);
    }

    void finishAiMarkupOperationForTests() {
        finishAiMarkupOperation();
    }

    void setInFlightAiMarkupFutureForTests(CompletableFuture<?> future) {
        this.inFlightAiMarkupFuture = future;
    }

    boolean tryStartAiMarkupOperationForTests() {
        return tryStartAiMarkupOperation();
    }

    boolean isAiMarkupInProgressForTests() {
        return aiMarkupInProgress;
    }

    void setAiMarkupOperationContextForTests() {
        currentOperation = OperationType.AI_MARKUP;
    }

    boolean isExecutorShutdownForTests() {
        return executor.isShutdown();
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
        // Interrupt any in-flight parse request first so its result is discarded rather than
        // written into a document that is going away (FR-020).
        cancelInFlightAiMarkup();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        return true;
    }
}
