/**
 * UTF-8 Check/Convert Tool Module
 * This module adds UTF-8 validation and conversion functionality to the DILA AI Markup Plugin.
 * 
 * Features:
 * - Robust UTF-8 validation using Java CharsetDecoder
 * - User-driven encoding selection via dropdown dialog
 * - Automatic backup creation before conversion
 * - Recursive directory scanning with file type filtering
 * - Comprehensive error handling and progress reporting
 * - Multi-language support
 */
(function() {
    'use strict';
    
    // Ensure required globals are available from main plugin
    if (typeof menuBar === 'undefined' || typeof i18nFn === 'undefined') {
        throw new Error('UTF-8 tool requires main plugin to be loaded first');
    }
    
    logDebug("Loading UTF-8 Check/Convert tool module");
    
    // Import Java classes
    var JFileChooser = Packages.javax.swing.JFileChooser;
    var JMenu = Packages.javax.swing.JMenu;
    var JMenuItem = Packages.javax.swing.JMenuItem;
    var JOptionPane = Packages.javax.swing.JOptionPane;
    var JComboBox = Packages.javax.swing.JComboBox;
    var JLabel = Packages.javax.swing.JLabel;
    var JPanel = Packages.javax.swing.JPanel;
    var BorderLayout = Packages.java.awt.BorderLayout;
    var Files = Packages.java.nio.file.Files;
    var StandardCopyOption = Packages.java.nio.file.StandardCopyOption;
    var EncodingUtils = Packages.com.dila.dama.plugin.util.EncodingUtils;
    
    // Create Tools menu and add to menu bar
    var menuTools = new JMenu(i18nFn("menu.tools"));
    var menuItemUtf8Check = new JMenuItem(i18nFn("menu.tools.utf8.check"));
    menuTools.add(menuItemUtf8Check);
    menuBar.add(menuTools);
    
    logDebug("UTF-8 tool menu added to menu bar");
    
    // Add action listener for UTF-8 check/convert
    menuItemUtf8Check.addActionListener(function() {
        logDebug("UTF-8 check/convert tool activated");
        
        // Clear areas and prepare UI
        infoArea.setText(i18nFn("utf8.check.select.files") + "\n");
        resultArea.setText("");
        
        // Hide button panel if visible
        if (typeof buttonPanel !== 'undefined') {
            buttonPanel.setVisible(false);
        }
        
        // Create and configure file chooser
        var fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setDialogTitle(i18nFn("utf8.check.dialog.title"));
        
        // Add file filter for common text files (optional)
        var FileNameExtensionFilter = Packages.javax.swing.filechooser.FileNameExtensionFilter;
        var textFilter = new FileNameExtensionFilter(
            "Text files (*.xml, *.txt, *.html, *.css, *.js)", 
            "xml", "txt", "html", "htm", "xhtml", "css", "js", "json", "md", "properties"
        );
        fileChooser.addChoosableFileFilter(textFilter);
        
        var result = fileChooser.showOpenDialog(customViewPanel);
        
        if (result === JFileChooser.APPROVE_OPTION) {
            var selectedFiles = fileChooser.getSelectedFiles();
            processSelectedFiles(selectedFiles);
        } else {
            logDebug("User cancelled file selection");
            infoArea.setText(i18nFn("utf8.check.cancelled"));
        }
    });
    
    /**
     * Process selected files and folders for UTF-8 validation
     */
    function processSelectedFiles(selectedFiles) {
        var nonUtf8Files = [];
        var totalFiles = 0;
        
        infoArea.append(i18nFn("utf8.scanning.files") + "\n");
        logDebug("Starting file scan for " + selectedFiles.length + " selected items");
        
        // Scan all selected files/folders
        for (var i = 0; i < selectedFiles.length; i++) {
            var scannedCount = scanFileOrDirectory(selectedFiles[i], nonUtf8Files);
            totalFiles += scannedCount;
        }
        
        logDebug("Scan completed. Total files: " + totalFiles + ", Non-UTF-8: " + nonUtf8Files.length);
        
        // Display results
        if (nonUtf8Files.length > 0) {
            var messageKey = "utf8.check.found.non.utf8";
            var message = i18nFn(messageKey);
            // Simple string replacement for {0} placeholder
            message = message.replace("{0}", nonUtf8Files.length);
            infoArea.append("\n" + message + "\n\n");
            
            // List non-UTF-8 files (show max 10, then summary)
            var maxDisplay = Math.min(nonUtf8Files.length, 10);
            for (var j = 0; j < maxDisplay; j++) {
                infoArea.append("• " + nonUtf8Files[j].getPath() + "\n");
            }
            
            if (nonUtf8Files.length > 10) {
                var moreMessage = i18nFn("utf8.check.more.files");
                moreMessage = moreMessage.replace("{0}", (nonUtf8Files.length - 10));
                infoArea.append("... " + moreMessage + "\n");
            }
            
            // Show encoding selection dialog
            showEncodingSelectionDialog(nonUtf8Files);
        } else {
            infoArea.append("\n" + i18nFn("utf8.check.all.valid"));
            logDebug("All files are already UTF-8 encoded");
        }
    }
    
    /**
     * Recursively scan files and directories
     * @param file The file or directory to scan
     * @param nonUtf8Files Array to collect non-UTF-8 files
     * @return Number of files scanned
     */
    function scanFileOrDirectory(file, nonUtf8Files) {
        var count = 0;
        
        if (file.isDirectory()) {
            logDebug("Scanning directory: " + file.getPath());
            var files = file.listFiles();
            if (files !== null) {
                for (var i = 0; i < files.length; i++) {
                    count += scanFileOrDirectory(files[i], nonUtf8Files);
                }
            }
        } else if (file.isFile() && isTextFile(file)) {
            count = 1;
            try {
                if (!EncodingUtils.isValidUtf8(file.toPath())) {
                    nonUtf8Files.push(file);
                    logDebug("Non-UTF-8 file found: " + file.getPath());
                }
            } catch (e) {
                logDebug("Error checking file " + file.getPath() + ": " + e);
                // Don't add to nonUtf8Files if we can't check it
            }
        }
        
        return count;
    }
    
    /**
     * Check if file is likely a text file based on extension
     * @param file The file to check
     * @return true if likely a text file
     */
    function isTextFile(file) {
        var name = file.getName().toLowerCase();
        var textExtensions = ['.xml', '.txt', '.html', '.htm', '.xhtml', '.css', '.js', '.json', '.md', '.properties', '.java', '.py', '.php', '.rb', '.go', '.rs', '.c', '.cpp', '.h', '.hpp'];
        
        for (var i = 0; i < textExtensions.length; i++) {
            if (name.endsWith(textExtensions[i])) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Show dialog for encoding selection
     * @param nonUtf8Files Array of files to convert
     */
    function showEncodingSelectionDialog(nonUtf8Files) {
        try {
            var panel = new JPanel(new BorderLayout());
            var label = new JLabel(i18nFn("utf8.check.select.encoding"));
            var encodingComboBox = new JComboBox(EncodingUtils.getCommonEncodings());
            
            // Set default selection to Windows-1252 (most common)
            encodingComboBox.setSelectedIndex(0);
            
            panel.add(label, BorderLayout.NORTH);
            panel.add(encodingComboBox, BorderLayout.CENTER);
            
            var options = [i18nFn("button.convert"), i18nFn("button.cancel")];
            var dialogResult = JOptionPane.showOptionDialog(
                customViewPanel,
                panel,
                i18nFn("utf8.check.dialog.title"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
            );
            
            if (dialogResult === 0) { // Convert button clicked
                var selectedEncoding = encodingComboBox.getSelectedItem().toString();
                logDebug("User selected encoding: " + selectedEncoding);
                convertFilesWithBackup(nonUtf8Files, selectedEncoding);
            } else {
                logDebug("User cancelled conversion");
                infoArea.append("\n" + i18nFn("utf8.conversion.cancelled"));
            }
        } catch (e) {
            logDebug("Error in encoding selection dialog: " + e);
            infoArea.append("\n" + i18nFn("utf8.conversion.error") + e.getMessage());
        }
    }
    
    /**
     * Convert files to UTF-8 with automatic backup
     * @param files Array of files to convert
     * @param sourceEncoding The source encoding selected by user
     */
    function convertFilesWithBackup(files, sourceEncoding) {
        var startMessage = i18nFn("utf8.conversion.started");
        startMessage = startMessage.replace("{0}", sourceEncoding);
        resultArea.setText(startMessage + "\n\n");
        
        var successCount = 0;
        var failCount = 0;
        
        logDebug("Starting conversion of " + files.length + " files from " + sourceEncoding + " to UTF-8");
        
        for (var i = 0; i < files.length; i++) {
            var file = files[i];
            var originalPath = file.toPath();
            var backupPath = originalPath.getParent().resolve(file.getName() + ".bak");
            
            try {
                // Step 1: Create backup
                Files.copy(originalPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
                resultArea.append(i18nFn("utf8.conversion.backing.up") + backupPath.getFileName() + "\n");
                logDebug("Backup created: " + backupPath.toString());
                
                // Step 2: Read with specified encoding
                var contentBytes = Files.readAllBytes(originalPath);
                var content = new Packages.java.lang.String(contentBytes, sourceEncoding);
                
                // Step 3: Write as UTF-8
                Files.write(originalPath, content.getBytes("UTF-8"));
                resultArea.append(i18nFn("utf8.conversion.success") + file.getName() + "\n\n");
                successCount++;
                logDebug("Successfully converted: " + file.getName());
                
            } catch (e) {
                resultArea.append(i18nFn("utf8.conversion.failed") + file.getName() + ": " + e.getMessage() + "\n\n");
                failCount++;
                logDebug("Conversion failed for " + file.getName() + ": " + e);
            }
        }
        
        // Final summary
        resultArea.append("═══════════════════════════════════\n");
        resultArea.append(i18nFn("utf8.conversion.summary") + "\n");
        
        var successMessage = i18nFn("utf8.conversion.success.count");
        successMessage = successMessage.replace("{0}", successCount);
        resultArea.append(successMessage + "\n");
        
        var failMessage = i18nFn("utf8.conversion.fail.count");
        failMessage = failMessage.replace("{0}", failCount);
        resultArea.append(failMessage + "\n");
        
        if (successCount > 0) {
            resultArea.append("\n" + i18nFn("utf8.conversion.backup.note") + "\n");
        }
        
        logDebug("Conversion completed. Success: " + successCount + ", Failed: " + failCount);
    }
    
    logDebug("UTF-8 Check/Convert tool module loaded successfully");
    
})();