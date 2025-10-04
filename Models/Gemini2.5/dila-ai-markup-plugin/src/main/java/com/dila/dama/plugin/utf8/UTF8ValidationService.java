package com.dila.dama.plugin.utf8;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Enhanced UTF-8 validation and conversion service using Java for superior accuracy.
 * This class provides comprehensive UTF-8 detection and conversion capabilities
 * with better accuracy than JavaScript implementations.
 */
public class UTF8ValidationService {
    
    private static final int MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final int BUFFER_SIZE = 8192;
    
    /**
     * Enhanced UTF-8 validation with superior accuracy.
     * Uses Java CharsetDecoder for strict validation.
     */
    public static boolean isValidUtf8(Path filePath) {
        if (filePath == null || !Files.exists(filePath)) {
            return true; // Assume valid if we can't validate
        }
        
        if (Files.isDirectory(filePath)) {
            return true; // Directories are considered "valid"
        }
        
        if (!Files.isReadable(filePath)) {
            return true; // Assume valid if we can't read it
        }
        
        try {
            long fileSize = Files.size(filePath);
            
            // Skip very large files to prevent memory issues
            if (fileSize > MAX_FILE_SIZE) {
                return true;
            }
            
            if (fileSize == 0) {
                return true; // Empty files are valid UTF-8
            }
            
            // Check for BOM and encoding patterns first
            byte[] firstBytes = readFirstBytes(filePath, 4);
            if (firstBytes.length >= 2) {
                // Check for UTF-16 BOM patterns
                int byte0 = firstBytes[0] & 0xFF;
                int byte1 = firstBytes[1] & 0xFF;
                
                // UTF-16 Little Endian BOM: FF FE
                if (byte0 == 0xFF && byte1 == 0xFE) {
                    return false;
                }
                
                // UTF-16 Big Endian BOM: FE FF
                if (byte0 == 0xFE && byte1 == 0xFF) {
                    return false;
                }
                
                // Check for UTF-16 without BOM (alternating null bytes pattern)
                if (firstBytes.length >= 4) {
                    int byte2 = firstBytes[2] & 0xFF;
                    int byte3 = firstBytes[3] & 0xFF;
                    
                    // Pattern for UTF-16 LE: non-null, null, non-null, null
                    if (byte1 == 0 && byte3 == 0 && byte0 != 0 && byte2 != 0) {
                        return false;
                    }
                    
                    // Pattern for UTF-16 BE: null, non-null, null, non-null
                    if (byte0 == 0 && byte2 == 0 && byte1 != 0 && byte3 != 0) {
                        return false;
                    }
                }
            }
            
            // Use CharsetDecoder for strict UTF-8 validation
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
            
            try (InputStream is = Files.newInputStream(filePath);
                 BufferedInputStream bis = new BufferedInputStream(is);
                 InputStreamReader reader = new InputStreamReader(bis, decoder)) {
                
                char[] buffer = new char[BUFFER_SIZE];
                while (reader.read(buffer) != -1) {
                    // Reading with strict decoder will throw exception if not valid UTF-8
                }
                return true;
                
            } catch (IOException e) {
                // If we get an exception during reading, it's likely not valid UTF-8
                return false;
            }
            
        } catch (Exception e) {
            return true; // Assume valid for unexpected errors
        }
    }
    
    /**
     * Read the first few bytes of a file for BOM detection.
     */
    private static byte[] readFirstBytes(Path filePath, int numBytes) {
        try (InputStream is = Files.newInputStream(filePath)) {
            byte[] buffer = new byte[numBytes];
            int bytesRead = is.read(buffer);
            if (bytesRead < numBytes) {
                return Arrays.copyOf(buffer, bytesRead);
            }
            return buffer;
        } catch (IOException e) {
            return new byte[0];
        }
    }
    
    /**
     * Check if a file is likely a text file based on extension.
     */
    public static boolean isTextFile(Path filePath) {
        if (filePath == null) {
            return false;
        }
        
        String fileName = filePath.getFileName().toString().toLowerCase();
        String[] textExtensions = {
            ".xml", ".txt", ".html", ".htm", ".xhtml", ".css", ".js", ".json", 
            ".md", ".properties", ".java", ".py", ".php", ".rb", ".go", ".rs", 
            ".c", ".cpp", ".h", ".hpp", ".sql", ".sh", ".bat", ".csv"
        };
        
        for (String ext : textExtensions) {
            if (fileName.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Recursively scan files and directories for UTF-8 validation.
     */
    public static List<Path> scanForNonUtf8Files(Path[] selectedPaths) {
        List<Path> nonUtf8Files = new ArrayList<>();
        
        for (Path path : selectedPaths) {
            scanPathRecursively(path, nonUtf8Files);
        }
        
        return nonUtf8Files;
    }
    
    /**
     * Recursively scan a single path.
     */
    private static void scanPathRecursively(Path path, List<Path> nonUtf8Files) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        
        try {
            if (Files.isDirectory(path)) {
                Files.list(path).forEach(child -> scanPathRecursively(child, nonUtf8Files));
            } else if (Files.isRegularFile(path) && isTextFile(path)) {
                if (!isValidUtf8(path)) {
                    nonUtf8Files.add(path);
                }
            }
        } catch (IOException e) {
            // Skip files we can't access
        }
    }
    
    /**
     * Convert files to UTF-8 with automatic backup.
     */
    public static ConversionResult convertFilesToUtf8(List<Path> filesToConvert, String sourceEncoding) {
        ConversionResult result = new ConversionResult();
        
        for (Path filePath : filesToConvert) {
            try {
                // Create backup
                Path backupPath = createBackup(filePath);
                
                // Detect source encoding if not provided
                String detectedEncoding = sourceEncoding;
                if (detectedEncoding == null || detectedEncoding.isEmpty()) {
                    detectedEncoding = detectEncoding(filePath);
                }
                
                // Convert file
                convertFileToUtf8(filePath, detectedEncoding);
                
                result.addSuccess(filePath, backupPath, detectedEncoding);
                
            } catch (Exception e) {
                result.addFailure(filePath, e.getMessage());
            }
        }
        
        return result;
    }
    
    /**
     * Create a backup of the original file.
     */
    private static Path createBackup(Path originalFile) throws IOException {
        Path backupPath = originalFile.resolveSibling(originalFile.getFileName() + ".utf8backup");
        Files.copy(originalFile, backupPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return backupPath;
    }
    
    /**
     * Simple encoding detection based on common patterns.
     */
    private static String detectEncoding(Path filePath) {
        // Try common encodings in order of likelihood
        String[] commonEncodings = {
            "Windows-1252", "ISO-8859-1", "GBK", "GB2312", "Big5", 
            "Shift_JIS", "EUC-KR", "Windows-1251", "ISO-8859-2"
        };
        
        for (String encoding : commonEncodings) {
            if (canDecodeWith(filePath, encoding)) {
                return encoding;
            }
        }
        
        return "ISO-8859-1"; // Fallback
    }
    
    /**
     * Test if a file can be decoded with a specific encoding.
     */
    private static boolean canDecodeWith(Path filePath, String encoding) {
        try {
            Charset charset = Charset.forName(encoding);
            CharsetDecoder decoder = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
            
            try (InputStream is = Files.newInputStream(filePath);
                 BufferedInputStream bis = new BufferedInputStream(is);
                 InputStreamReader reader = new InputStreamReader(bis, decoder)) {
                
                char[] buffer = new char[BUFFER_SIZE];
                while (reader.read(buffer) != -1) {
                    // Reading without exception means it can decode
                }
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Convert a single file to UTF-8.
     */
    private static void convertFileToUtf8(Path filePath, String sourceEncoding) throws IOException {
        // Read content with source encoding
        String content;
        try (InputStream is = Files.newInputStream(filePath);
             BufferedInputStream bis = new BufferedInputStream(is);
             InputStreamReader reader = new InputStreamReader(bis, sourceEncoding)) {
            
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[BUFFER_SIZE];
            int charsRead;
            while ((charsRead = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, charsRead);
            }
            content = sb.toString();
        }
        
        // Write content with UTF-8 encoding
        try (OutputStream os = Files.newOutputStream(filePath);
             BufferedOutputStream bos = new BufferedOutputStream(os);
             OutputStreamWriter writer = new OutputStreamWriter(bos, StandardCharsets.UTF_8)) {
            
            writer.write(content);
        }
    }
    
    /**
     * Result class for conversion operations.
     */
    public static class ConversionResult {
        private final List<ConversionSuccess> successes = new ArrayList<>();
        private final List<ConversionFailure> failures = new ArrayList<>();
        
        public void addSuccess(Path filePath, Path backupPath, String sourceEncoding) {
            successes.add(new ConversionSuccess(filePath, backupPath, sourceEncoding));
        }
        
        public void addFailure(Path filePath, String error) {
            failures.add(new ConversionFailure(filePath, error));
        }
        
        public List<ConversionSuccess> getSuccesses() { return successes; }
        public List<ConversionFailure> getFailures() { return failures; }
        public int getSuccessCount() { return successes.size(); }
        public int getFailureCount() { return failures.size(); }
    }
    
    public static class ConversionSuccess {
        private final Path filePath;
        private final Path backupPath;
        private final String sourceEncoding;
        
        public ConversionSuccess(Path filePath, Path backupPath, String sourceEncoding) {
            this.filePath = filePath;
            this.backupPath = backupPath;
            this.sourceEncoding = sourceEncoding;
        }
        
        public Path getFilePath() { return filePath; }
        public Path getBackupPath() { return backupPath; }
        public String getSourceEncoding() { return sourceEncoding; }
    }
    
    public static class ConversionFailure {
        private final Path filePath;
        private final String error;
        
        public ConversionFailure(Path filePath, String error) {
            this.filePath = filePath;
            this.error = error;
        }
        
        public Path getFilePath() { return filePath; }
        public String getError() { return error; }
    }
}