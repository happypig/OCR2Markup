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
        try {
            if (UTF8PrintStream === null) {
                // Initialize the stream.
                var PrintStream = Packages.java.io.PrintStream;
                UTF8PrintStream = new PrintStream(Packages.java.lang.System.err, true, "UTF-8");
            }
            var now = new Date();
            var timestamp = now.toLocaleString();
            UTF8PrintStream.println("[" + timestamp + "] " + message);
        } catch (e) {
            // Fallback to System.err if UTF8PrintStream fails
            Packages.java.lang.System.err.println("[ERROR] Failed to initialize UTF8PrintStream: " + e);
            Packages.java.lang.System.err.println("[" + new Date().toLocaleString() + "] " + message);
        }
    }
}

/**
 * UTF-8 tools are always available with enhanced Java implementation
 */
function isEncodingUtilsAvailable() {
    logDebug("Using enhanced Java UTF-8 validation service");
    return true;
}

/**
 * Enhanced UTF-8 validation using Java implementation for superior accuracy.
 * This bridges to the Java UTF8ValidationService class for enhanced validation.
 */
function isValidUtf8(file) {
    try {
        // Validate input
        if (!file || !file.exists || !file.exists()) {
            logDebug("File is null or does not exist: " + (file ? file.getPath() : "null"));
            return true; // Assume valid if we can't validate
        }
        
        if (file.isDirectory()) {
            logDebug("Path is a directory: " + file.getPath());
            return true; // Directories are considered "valid"
        }
        
        if (!file.canRead()) {
            logDebug("File cannot be read: " + file.getPath());
            return true; // Assume valid if we can't read it
        }
        
        logDebug("Using enhanced Java UTF-8 validation for: " + file.getPath());
        
        try {
            // Convert Java File to Path for the enhanced Java method
            var Paths = Packages.java.nio.file.Paths;
            var path = Paths.get(file.getAbsolutePath());
            
            // Call the enhanced Java implementation
            var UTF8ValidationService = Packages.com.dila.dama.plugin.utf8.UTF8ValidationService;
            var isValid = UTF8ValidationService.isValidUtf8(path);
            
            logDebug("Enhanced Java validation result for " + file.getPath() + ": " + isValid);
            return isValid;
            
        } catch (e) {
            logDebug("Error calling enhanced Java UTF-8 validation for " + file.getPath() + ": " + e);
            
            // Fallback to self-contained JavaScript validation if Java method fails
            logDebug("Falling back to JavaScript validation for: " + file.getPath());
            return isValidUtf8Fallback(file);
        }
        
    } catch (e) {
        logDebug("Unexpected error in UTF-8 validation for " + (file ? file.getPath() : "null") + ": " + e);
        return true; // Assume valid for unexpected errors
    }
}

/**
 * Fallback self-contained UTF-8 validation (original JavaScript implementation).
 * Used when the enhanced Java method is unavailable.
 */
function isValidUtf8Fallback(file) {
    logDebug("Using fallback JavaScript UTF-8 validation for: " + file.getPath());
    
    try {
        var FileInputStream = Packages.java.io.FileInputStream;
        var fis = null;
        
        try {
            fis = new FileInputStream(file);
            
            // Check for BOM (Byte Order Mark) first
            var firstFewBytes = Packages.java.lang.reflect.Array.newInstance(Packages.java.lang.Byte.TYPE, 4);
            var bytesRead = fis.read(firstFewBytes);
            
            if (bytesRead >= 2) {
                // Check for UTF-16 BOM patterns
                var byte0 = firstFewBytes[0] & 0xFF;
                var byte1 = firstFewBytes[1] & 0xFF;
                
                // UTF-16 Little Endian BOM: FF FE
                if (byte0 === 0xFF && byte1 === 0xFE) {
                    logDebug("UTF-16 LE BOM detected in file: " + file.getPath());
                    return false;
                }
                
                // UTF-16 Big Endian BOM: FE FF
                if (byte0 === 0xFE && byte1 === 0xFF) {
                    logDebug("UTF-16 BE BOM detected in file: " + file.getPath());
                    return false;
                }
                
                // Check for UTF-16 without BOM (common pattern: alternating null bytes)
                if (bytesRead >= 4) {
                    var byte2 = firstFewBytes[2] & 0xFF;
                    var byte3 = firstFewBytes[3] & 0xFF;
                    
                    // Pattern for UTF-16 LE: non-null, null, non-null, null
                    if (byte1 === 0 && byte3 === 0 && byte0 !== 0 && byte2 !== 0) {
                        logDebug("UTF-16 LE pattern detected (no BOM) in file: " + file.getPath());
                        return false;
                    }
                    
                    // Pattern for UTF-16 BE: null, non-null, null, non-null
                    if (byte0 === 0 && byte2 === 0 && byte1 !== 0 && byte3 !== 0) {
                        logDebug("UTF-16 BE pattern detected (no BOM) in file: " + file.getPath());
                        return false;
                    }
                }
            }
            
            // Reset stream and do full UTF-8 validation
            fis.close();
            fis = new FileInputStream(file);
            
            // Read entire file as bytes for strict UTF-8 validation
            var buffer = Packages.java.lang.reflect.Array.newInstance(Packages.java.lang.Byte.TYPE, 8192);
            var totalBytesRead = 0;
            
            while ((bytesRead = fis.read(buffer)) !== -1) {
                // Validate UTF-8 byte sequences
                for (var i = 0; i < bytesRead; i++) {
                    var currentByte = buffer[i] & 0xFF;
                    
                    // Check for null bytes (common in UTF-16 but not typical in UTF-8 text)
                    if (currentByte === 0) {
                        logDebug("Null byte found at position " + (totalBytesRead + i) + " in file: " + file.getPath());
                        return false;
                    }
                    
                    // ASCII range (0-127) - always valid in UTF-8
                    if (currentByte <= 0x7F) {
                        continue;
                    }
                    
                    // Multi-byte UTF-8 sequence validation
                    var sequenceLength = 0;
                    
                    if ((currentByte & 0xE0) === 0xC0) {
                        // 2-byte sequence: 110xxxxx 10xxxxxx
                        sequenceLength = 1;
                    } else if ((currentByte & 0xF0) === 0xE0) {
                        // 3-byte sequence: 1110xxxx 10xxxxxx 10xxxxxx
                        sequenceLength = 2;
                    } else if ((currentByte & 0xF8) === 0xF0) {
                        // 4-byte sequence: 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
                        sequenceLength = 3;
                    } else {
                        // Invalid UTF-8 start byte
                        logDebug("Invalid UTF-8 start byte 0x" + currentByte.toString(16) + " at position " + (totalBytesRead + i) + " in file: " + file.getPath());
                        return false;
                    }
                    
                    // Validate continuation bytes
                    for (var j = 1; j <= sequenceLength; j++) {
                        if (i + j >= bytesRead) {
                            // Need to read more bytes - this is getting complex, so we'll use a simpler approach
                            break;
                        }
                        var continuationByte = buffer[i + j] & 0xFF;
                        if ((continuationByte & 0xC0) !== 0x80) {
                            logDebug("Invalid UTF-8 continuation byte 0x" + continuationByte.toString(16) + " at position " + (totalBytesRead + i + j) + " in file: " + file.getPath());
                            return false;
                        }
                    }
                    
                    // Skip the continuation bytes we just validated
                    i += sequenceLength;
                }
                totalBytesRead += bytesRead;
            }
            
            logDebug("Fallback UTF-8 validation passed for: " + file.getPath());
            return true;
            
        } catch (e) {
            logDebug("Error during fallback byte-level UTF-8 validation for " + file.getPath() + ": " + e);
            return false; // Assume invalid if we can't properly validate
        } finally {
            // Clean up resources
            try { if (fis) fis.close(); } catch (e) { /* ignore */ }
        }
        
    } catch (e) {
        logDebug("Unexpected error in fallback UTF-8 validation for " + (file ? file.getPath() : "null") + ": " + e);
        return true; // Assume valid for unexpected errors
    }
}

/**
 * Get common encodings using enhanced Java implementation with fallback.
 * Tries to use the Java method first, falls back to self-contained list.
 */
function getCommonEncodings() {
    try {
        logDebug("Using enhanced Java encoding list");
        
        // Try to call the enhanced Java implementation
        var DAMAExtension = Packages.com.dila.dama.plugin.preferences.DAMAOptionPagePluginExtension;
        var javaEncodings = DAMAExtension.getCommonEncodings();
        
        // Convert Java array to JavaScript ArrayList for compatibility
        var ArrayList = Packages.java.util.ArrayList;
        var encodings = new ArrayList();
        
        for (var i = 0; i < javaEncodings.length; i++) {
            encodings.add(javaEncodings[i]);
        }
        
        logDebug("Enhanced Java encoding list loaded with " + encodings.size() + " items");
        return encodings;
        
    } catch (e) {
        logDebug("Error calling enhanced Java encoding list: " + e);
        logDebug("Falling back to self-contained encoding list");
        
        // Fallback to self-contained list
        var ArrayList = Packages.java.util.ArrayList;
        var encodings = new ArrayList();
        
        // Common encodings ordered by likelihood
        var commonEncodings = [
            "Windows-1252",
            "ISO-8859-1",
            "GBK", 
            "GB2312",
            "Big5",
            "Shift_JIS",
            "EUC-KR",
            "Windows-1251",
            "ISO-8859-2",
            "UTF-16",
            "UTF-16BE",
            "UTF-16LE"
        ];
        
        for (var i = 0; i < commonEncodings.length; i++) {
            encodings.add(commonEncodings[i]);
        }
        
        logDebug("Fallback encoding list loaded with " + encodings.size() + " items");
        return encodings;
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
                    var JFileChooser = Packages.javax.swing.JFileChooser;
                    
                    var menuBar = new JMenuBar();
                    var menuActions = new JMenu(i18nFn("actions.menu")); // "Actions"
                    var menuItemActionAIMarkup = new JMenuItem(i18nFn("ai.markup.action")); // load action from local
                    var menuItemActionTagRemoval = new JMenuItem(i18nFn("tag.removal.action")); // load action from local

                    // UTF-8 tools are now always available with self-contained implementation
                    var utf8ToolsAvailable = isEncodingUtilsAvailable();
                    
                    // Add Tools menu for UTF-8 Check/Convert
                    var menuTools = new JMenu(i18nFn("menu.tools"));
                    var menuItemUtf8Check = new JMenuItem(i18nFn("menu.tools.utf8.check"));
                    menuTools.add(menuItemUtf8Check);
                    logDebug("UTF-8 tool menu item added (self-contained implementation)");

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

                    // Assemble the menu bar - Actions on left, Tools in middle, Options on right
                    menuBar.add(menuActions);
                    menuActions.add(menuItemActionAIMarkup);
                    menuActions.add(menuItemActionTagRemoval);
                    
                    // Always add Tools menu now
                    menuBar.add(menuTools);
                    
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

                    // ========================================
                    // UTF-8 Helper Functions - Self-contained implementation
                    // ========================================
                    
                    /**
                     * Check if file is likely a text file based on extension
                     */
                    function isTextFile(file) {
                        if (!file || !file.getName) {
                            return false;
                        }
                        
                        var name = file.getName().toLowerCase();
                        var textExtensions = ['.xml', '.txt', '.html', '.htm', '.xhtml', '.css', '.js', '.json', '.md', '.properties', '.java', '.py', '.php', '.rb', '.go', '.rs', '.c', '.cpp', '.h', '.hpp', '.sql', '.sh', '.bat', '.csv'];
                        
                        for (var i = 0; i < textExtensions.length; i++) {
                            if (name.endsWith(textExtensions[i])) {
                                return true;
                            }
                        }
                        return false;
                    }
                    
                    /**
                     * Recursively scan files and directories
                     */
                    function scanFileOrDirectory(file, nonUtf8Files) {
                        var count = 0;
                        
                        if (!file) {
                            logDebug("scanFileOrDirectory: file is null");
                            return 0;
                        }
                        
                        try {
                            if (file.isDirectory()) {
                                logDebug("Scanning directory: " + file.getPath());
                                var files = file.listFiles();
                                if (files !== null && files.length > 0) {
                                    for (var i = 0; i < files.length; i++) {
                                        try {
                                            if (files[i]) {
                                                count += scanFileOrDirectory(files[i], nonUtf8Files);
                                            }
                                        } catch (e) {
                                            logDebug("Error scanning individual file/directory " + (files[i] ? files[i].getPath() : "null") + ": " + e);
                                            // Continue with next file
                                        }
                                    }
                                } else {
                                    logDebug("Directory is empty or not accessible: " + file.getPath());
                                }
                            } else if (file.isFile() && isTextFile(file)) {
                                count = 1;
                                try {
                                    if (!isValidUtf8(file)) {
                                        nonUtf8Files.push(file);
                                        logDebug("Non-UTF-8 file found: " + file.getPath());
                                    } else {
                                        logDebug("UTF-8 file confirmed: " + file.getPath());
                                    }
                                } catch (e) {
                                    logDebug("Error checking file " + file.getPath() + ": " + e);
                                    // Don't add to non-UTF-8 list if check failed
                                }
                            } else {
                                logDebug("Skipping non-text file: " + file.getPath());
                            }
                        } catch (e) {
                            logDebug("Error in scanFileOrDirectory for " + (file ? file.getPath() : "null") + ": " + e);
                        }
                        
                        return count;
                    }
                    
                    /**
                     * Convert files to UTF-8 with automatic backup
                     */
                    function convertFilesWithBackup(files, sourceEncoding) {
                        if (!files || files.length === 0) {
                            resultArea.setText("No files to convert.\n");
                            return;
                        }
                        
                        try {
                            var FileInputStream = Packages.java.io.FileInputStream;
                            var FileOutputStream = Packages.java.io.FileOutputStream;
                            var InputStreamReader = Packages.java.io.InputStreamReader;
                            var OutputStreamWriter = Packages.java.io.OutputStreamWriter;
                            var BufferedReader = Packages.java.io.BufferedReader;
                            var BufferedWriter = Packages.java.io.BufferedWriter;
                            var File = Packages.java.io.File;
                            
                            var startMessage = i18nFn("utf8.conversion.started");
                            // FIX: Convert string to string before replace (sourceEncoding is already a string)
                            startMessage = startMessage.replace("{0}", String(sourceEncoding));
                            resultArea.setText(startMessage + "\n\n");
                            
                            var successCount = 0;
                            var failCount = 0;
                            
                            logDebug("Starting conversion of " + files.length + " files from " + sourceEncoding + " to UTF-8");
                            
                            for (var i = 0; i < files.length; i++) {
                                var file = files[i];
                                
                                if (!file) {
                                    logDebug("Skipping null file at index " + i);
                                    failCount++;
                                    continue;
                                }
                                
                                try {
                                    // Validate file before processing
                                    if (!file.exists()) {
                                        resultArea.append(i18nFn("utf8.conversion.failed") + file.getName() + ": File does not exist\n\n");
                                        failCount++;
                                        continue;
                                    }
                                    
                                    if (!file.canRead()) {
                                        resultArea.append(i18nFn("utf8.conversion.failed") + file.getName() + ": Cannot read file\n\n");
                                        failCount++;
                                        continue;
                                    }
                                    
                                    if (!file.canWrite()) {
                                        resultArea.append(i18nFn("utf8.conversion.failed") + file.getName() + ": Cannot write to file\n\n");
                                        failCount++;
                                        continue;
                                    }
                                    
                                    var backupFile = new File(file.getParent(), file.getName() + ".bak");
                                    
                                    // Step 1: Create backup by copying original file
                                    var originalFis = new FileInputStream(file);
                                    var backupFos = new FileOutputStream(backupFile);
                                    var buffer = Packages.java.lang.reflect.Array.newInstance(Packages.java.lang.Byte.TYPE, 4096);
                                    var bytesRead;
                                    
                                    while ((bytesRead = originalFis.read(buffer)) !== -1) {
                                        backupFos.write(buffer, 0, bytesRead);
                                    }
                                    originalFis.close();
                                    backupFos.close();
                                    
                                    resultArea.append(i18nFn("utf8.conversion.backing.up") + backupFile.getName() + "\n");
                                    logDebug("Backup created: " + backupFile.getPath());
                                    
                                    // Step 2: Read file with source encoding
                                    var reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), sourceEncoding));
                                    var content = "";
                                    var line;
                                    while ((line = reader.readLine()) !== null) {
                                        content += line + "\n";
                                    }
                                    reader.close();
                                    
                                    // Step 3: Write as UTF-8
                                    var writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
                                    writer.write(content);
                                    writer.close();
                                    
                                    resultArea.append(i18nFn("utf8.conversion.success") + file.getName() + "\n\n");
                                    successCount++;
                                    logDebug("Successfully converted: " + file.getName());
                                    
                                } catch (e) {
                                    var errorMsg = e.getMessage ? e.getMessage() : String(e);
                                    resultArea.append(i18nFn("utf8.conversion.failed") + file.getName() + ": " + errorMsg + "\n\n");
                                    failCount++;
                                    logDebug("Conversion failed for " + file.getName() + ": " + e);
                                }
                            }
                            
                            // Final summary
                            resultArea.append("═══════════════════════════════════\n");
                            resultArea.append(i18nFn("utf8.conversion.summary") + "\n");
                            
                            var successMessage = i18nFn("utf8.conversion.success.count");
                            // FIX: Convert number to string before replace
                            successMessage = successMessage.replace("{0}", String(successCount));
                            resultArea.append(successMessage + "\n");
                            
                            var failMessage = i18nFn("utf8.conversion.fail.count");
                            // FIX: Convert number to string before replace
                            failMessage = failMessage.replace("{0}", String(failCount));
                            resultArea.append(failMessage + "\n");
                            
                            if (successCount > 0) {
                                resultArea.append("\n" + i18nFn("utf8.conversion.backup.note") + "\n");
                            }
                            
                            logDebug("Conversion completed. Success: " + successCount + ", Failed: " + failCount);
                        } catch (e) {
                            logDebug("Error in convertFilesWithBackup: " + e);
                            var errorMsg = e.message ? e.message : String(e);
                            resultArea.append("\n" + i18nFn("utf8.conversion.error") + errorMsg);
                        }
                    }
                    
                    /**
                     * Show dialog for encoding selection
                     */
                    function showEncodingSelectionDialog(nonUtf8Files) {
                        if (!nonUtf8Files || nonUtf8Files.length === 0) {
                            logDebug("No files to convert");
                            return;
                        }
                        
                        try {
                            var JOptionPane = Packages.javax.swing.JOptionPane;
                            var JComboBox = Packages.javax.swing.JComboBox;
                            var JLabel = Packages.javax.swing.JLabel;
                            
                            // Get common encodings
                            var commonEncodings = getCommonEncodings();
                            
                            var panel = new JPanel(new BorderLayout());
                            var label = new JLabel(i18nFn("utf8.check.select.encoding"));
                            var encodingComboBox = new JComboBox(commonEncodings.toArray());
                            
                            encodingComboBox.setSelectedIndex(0);
                            
                            panel.add(label, BorderLayout.NORTH);
                            panel.add(encodingComboBox, BorderLayout.CENTER);
                            
                            var options = [i18nFn("button.convert"), i18nFn("button.cancel")];
                            var dialogResult = JOptionPane.showOptionDialog(
                                pluginPanel,
                                panel,
                                i18nFn("utf8.check.dialog.title"),
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE,
                                null,
                                options,
                                options[0]
                            );
                            
                            if (dialogResult === 0) {
                                var selectedEncoding = encodingComboBox.getSelectedItem();
                                if (selectedEncoding) {
                                    var encodingStr = selectedEncoding.toString();
                                    logDebug("User selected encoding: " + encodingStr);
                                    convertFilesWithBackup(nonUtf8Files, encodingStr);
                                } else {
                                    logDebug("No encoding selected");
                                    infoArea.append("\n" + i18nFn("utf8.conversion.cancelled"));
                                }
                            } else {
                                logDebug("User cancelled conversion");
                                infoArea.append("\n" + i18nFn("utf8.conversion.cancelled"));
                            }
                        } catch (e) {
                            logDebug("Error in encoding selection dialog: " + e);
                            var errorMsg = e.message ? e.message : String(e);
                            infoArea.append("\n" + i18nFn("utf8.conversion.error") + errorMsg);
                        }
                    }
                    
                    /**
                     * Process selected files and folders for UTF-8 validation using enhanced Java service
                     */
                    function processSelectedFiles(selectedFiles) {
                        if (!selectedFiles || selectedFiles.length === 0) {
                            infoArea.setText("No files selected for processing.\n");
                            return;
                        }
                        
                        infoArea.append(i18nFn("utf8.scanning.files") + "\n");
                        logDebug("Starting enhanced Java file scan for " + selectedFiles.length + " selected items");
                        
                        try {
                            // Convert File objects to Path objects for Java service
                            var Paths = Packages.java.nio.file.Paths;
                            var selectedPaths = Packages.java.lang.reflect.Array.newInstance(Packages.java.nio.file.Path, selectedFiles.length);
                            
                            for (var i = 0; i < selectedFiles.length; i++) {
                                infoArea.append("Scanning: " + selectedFiles[i].getName() + "\n");
                                selectedPaths[i] = Paths.get(selectedFiles[i].getAbsolutePath());
                            }
                            
                            // Use Java service for superior scanning accuracy
                            var UTF8ValidationService = Packages.com.dila.dama.plugin.utf8.UTF8ValidationService;
                            var nonUtf8PathsList = UTF8ValidationService.scanForNonUtf8Files(selectedPaths);
                            
                            // Convert Path objects back to File objects for UI compatibility
                            var nonUtf8Files = [];
                            var File = Packages.java.io.File;
                            for (var j = 0; j < nonUtf8PathsList.size(); j++) {
                                var path = nonUtf8PathsList.get(j);
                                nonUtf8Files.push(new File(path.toString()));
                            }
                            
                            logDebug("Enhanced Java scan completed. Non-UTF-8 files found: " + nonUtf8Files.length);
                            
                            // Display results
                            if (nonUtf8Files.length > 0) {
                                var messageKey = "utf8.check.found.non.utf8";
                                var message = i18nFn(messageKey);
                                // FIX: Convert number to string before replace
                                message = message.replace("{0}", String(nonUtf8Files.length));
                                infoArea.append("\n" + message + "\n\n");
                                
                                // List non-UTF-8 files (show max 10, then summary)
                                var maxDisplay = Math.min(nonUtf8Files.length, 10);
                                for (var k = 0; k < maxDisplay; k++) {
                                    if (nonUtf8Files[k]) {
                                        infoArea.append("• " + nonUtf8Files[k].getPath() + "\n");
                                    }
                                }
                                
                                if (nonUtf8Files.length > 10) {
                                    var moreMessage = i18nFn("utf8.check.more.files");
                                    // FIX: Convert number to string before replace
                                    moreMessage = moreMessage.replace("{0}", String(nonUtf8Files.length - 10));
                                    infoArea.append("... " + moreMessage + "\n");
                                }
                                
                                // Show enhanced conversion dialog
                                showEnhancedConversionDialog(nonUtf8Files);
                            } else {
                                infoArea.append("\n" + i18nFn("utf8.check.all.valid") + "\n");
                                logDebug("All files are already UTF-8 encoded (verified by enhanced Java service)");
                            }
                        } catch (e) {
                            logDebug("Error in enhanced processSelectedFiles: " + e);
                            var errorMsg = e.message ? e.message : String(e);
                            infoArea.append("\nError during enhanced file processing: " + errorMsg + "\n");
                            
                            // Fallback to original JavaScript scanning if Java service fails
                            logDebug("Falling back to original JavaScript scanning");
                            processSelectedFilesFallback(selectedFiles);
                        }
                    }
                    
                    /**
                     * Enhanced conversion dialog without encoding selection (Java auto-detects)
                     */
                    function showEnhancedConversionDialog(nonUtf8Files) {
                        if (!nonUtf8Files || nonUtf8Files.length === 0) {
                            logDebug("No files to convert");
                            return;
                        }
                        
                        try {
                            var JOptionPane = Packages.javax.swing.JOptionPane;
                            var JLabel = Packages.javax.swing.JLabel;
                            var JPanel = Packages.javax.swing.JPanel;
                            var BorderLayout = Packages.java.awt.BorderLayout;
                            
                            var panel = new JPanel(new BorderLayout());
                            var message = i18nFn("utf8.check.found.non.utf8").replace("{0}", String(nonUtf8Files.length)) + 
                                         "\n\nThe system will automatically detect source encodings and convert to UTF-8.";
                            var label = new JLabel("<html>" + message.replace(/\n/g, "<br>") + "</html>");
                            
                            panel.add(label, BorderLayout.CENTER);
                            
                            var options = [i18nFn("button.convert"), i18nFn("button.cancel")];
                            var dialogResult = JOptionPane.showOptionDialog(
                                pluginPanel,
                                panel,
                                i18nFn("utf8.check.dialog.title"),
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE,
                                null,
                                options,
                                options[0]
                            );
                            
                            if (dialogResult === 0) {
                                logDebug("User approved enhanced conversion");
                                convertFilesWithEnhancedJava(nonUtf8Files);
                            } else {
                                logDebug("User cancelled enhanced conversion");
                                infoArea.append("\n" + i18nFn("utf8.conversion.cancelled") + "\n");
                            }
                        } catch (e) {
                            logDebug("Error in enhanced conversion dialog: " + e);
                            var errorMsg = e.message ? e.message : String(e);
                            infoArea.append("\nError in conversion dialog: " + errorMsg + "\n");
                        }
                    }
                    
                    /**
                     * Convert files using enhanced Java service with automatic encoding detection
                     */
                    function convertFilesWithEnhancedJava(nonUtf8Files) {
                        if (!nonUtf8Files || nonUtf8Files.length === 0) {
                            infoArea.append("\nNo files to convert.\n");
                            return;
                        }
                        
                        try {
                            infoArea.append("\nStarting enhanced UTF-8 conversion...\n");
                            
                            // Convert File objects to Path objects for Java service
                            var Paths = Packages.java.nio.file.Paths;
                            var ArrayList = Packages.java.util.ArrayList;
                            var pathsList = new ArrayList();
                            
                            for (var i = 0; i < nonUtf8Files.length; i++) {
                                var path = Paths.get(nonUtf8Files[i].getAbsolutePath());
                                pathsList.add(path);
                            }
                            
                            // Use Java service for superior conversion with auto-detection
                            var UTF8ValidationService = Packages.com.dila.dama.plugin.utf8.UTF8ValidationService;
                            var conversionResult = UTF8ValidationService.convertFilesToUtf8(pathsList, null); // null = auto-detect
                            
                            // Display conversion results
                            var successCount = conversionResult.getSuccessCount();
                            var failureCount = conversionResult.getFailureCount();
                            
                            resultArea.setText(""); // Clear previous results
                            resultArea.append("=== Enhanced UTF-8 Conversion Results ===\n\n");
                            resultArea.append("Successfully converted: " + successCount + " files\n");
                            resultArea.append("Failed conversions: " + failureCount + " files\n\n");
                            
                            // Show successful conversions
                            if (successCount > 0) {
                                resultArea.append("✓ Successfully converted files:\n");
                                var successes = conversionResult.getSuccesses();
                                for (var j = 0; j < successes.size(); j++) {
                                    var success = successes.get(j);
                                    resultArea.append("  • " + success.getFilePath().getFileName() + 
                                                    " (from " + success.getSourceEncoding() + ")\n");
                                    resultArea.append("    Backup: " + success.getBackupPath().getFileName() + "\n");
                                }
                            }
                            
                            // Show failed conversions
                            if (failureCount > 0) {
                                resultArea.append("\n✗ Failed conversions:\n");
                                var failures = conversionResult.getFailures();
                                for (var k = 0; k < failures.size(); k++) {
                                    var failure = failures.get(k);
                                    resultArea.append("  • " + failure.getFilePath().getFileName() + 
                                                    " - " + failure.getError() + "\n");
                                }
                            }
                            
                            resultArea.append("\nConversion completed using enhanced Java service.\n");
                            logDebug("Enhanced Java conversion completed: " + successCount + " success, " + failureCount + " failures");
                            
                        } catch (e) {
                            logDebug("Error in enhanced Java conversion: " + e);
                            var errorMsg = e.message ? e.message : String(e);
                            resultArea.append("Error during enhanced conversion: " + errorMsg + "\n");
                            
                            // Fallback to original conversion method
                            logDebug("Falling back to original conversion method");
                            showEncodingSelectionDialog(nonUtf8Files);
                        }
                    }
                    
                    /**
                     * Fallback to original JavaScript file scanning if Java service fails
                     */
                    function processSelectedFilesFallback(selectedFiles) {
                        var nonUtf8Files = [];
                        var totalFiles = 0;
                        
                        infoArea.append("Using fallback JavaScript scanning...\n");
                        
                        try {
                            // Scan all selected files/folders using original method
                            for (var i = 0; i < selectedFiles.length; i++) {
                                var selectedFile = selectedFiles[i];
                                if (!selectedFile) {
                                    continue;
                                }
                                
                                try {
                                    var scannedCount = scanFileOrDirectory(selectedFile, nonUtf8Files);
                                    totalFiles += scannedCount;
                                } catch (e) {
                                    logDebug("Error in fallback scan: " + e);
                                }
                            }
                            
                            // Display results
                            if (nonUtf8Files.length > 0) {
                                var messageKey = "utf8.check.found.non.utf8";
                                var message = i18nFn(messageKey);
                                message = message.replace("{0}", String(nonUtf8Files.length));
                                infoArea.append("\n" + message + " (fallback scan)\n\n");
                                
                                // Show original encoding selection dialog
                                showEncodingSelectionDialog(nonUtf8Files);
                            } else {
                                infoArea.append("\n" + i18nFn("utf8.check.all.valid") + " (fallback scan)\n");
                            }
                        } catch (e) {
                            logDebug("Error in fallback processing: " + e);
                            infoArea.append("\nError in fallback processing: " + e + "\n");
                        }
                    }
                    
                    // ========================================
                    // UTF-8 Check/Convert Tool Implementation
                    // ========================================
                    
                    // Add action listener for UTF-8 check/convert
                    menuItemUtf8Check.addActionListener(function() {
                        logDebug("UTF-8 check/convert tool activated (self-contained implementation)");
                        
                        try {
                            infoArea.setText(i18nFn("utf8.check.select.files") + "\n");
                            resultArea.setText("");
                            buttonPanel.setVisible(false);
                            
                            var fileChooser = new JFileChooser();
                            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                            fileChooser.setMultiSelectionEnabled(true);
                            fileChooser.setDialogTitle(i18nFn("utf8.check.dialog.title"));
                            
                            // Add file filters
                            try {
                                var FileNameExtensionFilter = Packages.javax.swing.filechooser.FileNameExtensionFilter;
                                var textFilter = new FileNameExtensionFilter(
                                    "Text files (*.xml, *.txt, *.html, *.css, *.js)", 
                                    "xml", "txt", "html", "htm", "xhtml", "css", "js", "json", "md", "properties"
                                );
                                fileChooser.addChoosableFileFilter(textFilter);
                            } catch (e) {
                                logDebug("Could not add file filter: " + e);
                                // Continue without filter
                            }
                            
                            var result = fileChooser.showOpenDialog(pluginPanel);
                            
                            if (result === JFileChooser.APPROVE_OPTION) {
                                var selectedFiles = fileChooser.getSelectedFiles();
                                if (selectedFiles && selectedFiles.length > 0) {
                                    processSelectedFiles(selectedFiles);
                                } else {
                                    infoArea.setText(i18nFn("utf8.check.no.files.selected"));
                                }
                            } else {
                                logDebug("User cancelled file selection");
                                infoArea.setText(i18nFn("utf8.check.cancelled"));
                            }
                        } catch (e) {
                            logDebug("Error in UTF-8 check tool: " + e);
                            var errorMsg = e.message ? e.message : String(e);
                            infoArea.setText("Error in UTF-8 tool: " + errorMsg);
                        }
                    });
                    
                    logDebug("UTF-8 Check/Convert tool integrated successfully (self-contained implementation)");

                    // Add action listeners to AI Markup menu item
                    menuItemActionAIMarkup.addActionListener(function() {
                        logDebug("AI Markup action triggered");

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
                                    connection.setRequestProperty("Authorization", "Bearer " + apiKey);
                                    connection.setDoOutput(true);
                                    
                                    var systemPrompt = i18nFn("system.prompt.ai.markup");

                                    var requestBody = JSON.stringify({
                                        "model": parseModel,
                                        "messages": [
                                            {"role": "system", "content": systemPrompt},
                                            {"role": "user", "content": selectedText}
                                        ],
                                        "max_tokens": 1000
                                    });
                                    
                                    var writer = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
                                    writer.write(requestBody);
                                    writer.flush();
                                    writer.close();
                                    
                                    var responseCode = connection.getResponseCode();
                                    if (responseCode >= 200 && responseCode < 300) {
                                        var reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                                        var response = "";
                                        var line;
                                        while ((line = reader.readLine()) != null) {
                                            response += line;
                                        }
                                        reader.close();
                                        
                                        var responseObj = JSON.parse(response);
                                        var llmResponse = responseObj.choices[0].message.content;

                                        SwingUtilities.invokeLater(function() {
                                            resultArea.setText("<ref>" + llmResponse + "</ref>");
                                            buttonPanel.setVisible(true);
                                            logDebug("API call successful.");
                                        });
                                        
                                    } else {
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
                                    SwingUtilities.invokeLater(function() {
                                        infoArea.setText(i18nFn("llm.error") + "\n" + error.message);
                                        logDebug("LLM API Error: " + error);
                                    });
                                }
                            });
                            networkThread.start();
                        }
                    });

                    // Add action listener to the replace button
                    replaceButton.addActionListener(function() {
                        var replacementText = resultArea.getText();
                        var replacementTextLength = replacementText.length();
                        logDebug("Replace button clicked with replacement: " + replacementText + 
                            "\n(length: " + replacementTextLength + ")");

                        if (replacementText && replacementTextLength > 0) {
                            logDebug("With replacement: " + replacementText);
                            var editorAccess = pluginWorkspaceAccess.getCurrentEditorAccess(Packages.ro.sync.exml.workspace.api.PluginWorkspace.MAIN_EDITING_AREA);
                            if (editorAccess != null) {
                                var pageAccess = editorAccess.getCurrentPage();
                                logDebug("With page access: " + pageAccess);
                                if (pageAccess != null && pageAccess instanceof Packages.ro.sync.exml.workspace.api.editor.page.text.WSTextEditorPage) {

                                    try {
                                        var selectionStart = pageAccess.getSelectionStart();
                                        var selectionEnd = pageAccess.getSelectionEnd();
                                        var theDocument = pageAccess.getDocument();
                                        logDebug("Start: " + selectionStart + ", End: " + selectionEnd + ", Document: " + theDocument);
                                        
                                        if (selectionStart != selectionEnd) {
                                            pageAccess.beginCompoundUndoableEdit();
                                            try {
                                                logDebug("Replacing text in document...");
                                                pageAccess.deleteSelection();
                                                logDebug("Original text removed");
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
                        if (selectedText && selectedText.length > 0) {
                            var cleanedText = new String(selectedText).replace(/<[^>]*>/g, '');
                            resultArea.setText(cleanedText);
                            buttonPanel.setVisible(true);
                        } else {
                            buttonPanel.setVisible(false);
                        }
                    });

                    // Add action listener to the Preferences menu item
                    menuItemOption.addActionListener(function() {
                        try {
                            logDebug("Settings option triggered");
                            buttonPanel.setVisible(false);

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
    getDebugMode()
    logDebug("Closing DILA AI Markup plugin.");
    logDebug("debug mode: " + getDebugMode());
}
