package com.dila.dama.plugin.utf8;

import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import static org.junit.Assert.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Comprehensive test suite for UTF8ValidationService
 * Tests UTF-8 validation, encoding detection, and file conversion
 */
public class UTF8ValidationServiceTest {

    private Path tempDir;
    private Path utf8File;
    private Path utf16File;
    private Path invalidFile;
    private Path binaryFile;

    @Before
    public void setUp() throws IOException {
        // Create temporary directory for test files
        tempDir = Files.createTempDirectory("utf8-test-");
        
        // Create test files with different encodings
        createTestFiles();
    }

    @After
    public void tearDown() throws IOException {
        // Clean up test files
        if (Files.exists(tempDir)) {
            Files.walk(tempDir)
                .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        // Ignore cleanup errors
                    }
                });
        }
    }

    private void createTestFiles() throws IOException {
        // UTF-8 valid file
        utf8File = tempDir.resolve("valid-utf8.txt");
        String utf8Content = "Hello World\nUTF-8 Content: 中文 日本語 한국어\n";
        Files.write(utf8File, utf8Content.getBytes(StandardCharsets.UTF_8));

        // UTF-16 LE file (should be detected as non-UTF-8)
        utf16File = tempDir.resolve("utf16-le.txt");
        String utf16Content = "UTF-16 LE Content\nWith special chars: 中文";
        Files.write(utf16File, utf16Content.getBytes(StandardCharsets.UTF_16LE));

        // Invalid UTF-8 file (manual byte sequence)
        invalidFile = tempDir.resolve("invalid-utf8.txt");
        byte[] invalidBytes = {
            (byte) 0x48, (byte) 0x65, (byte) 0x6C, (byte) 0x6C, (byte) 0x6F, // "Hello"
            (byte) 0xFF, (byte) 0xFE, // Invalid UTF-8 sequence
            (byte) 0x57, (byte) 0x6F, (byte) 0x72, (byte) 0x6C, (byte) 0x64  // "World"
        };
        Files.write(invalidFile, invalidBytes);

        // Binary file (should be ignored)
        binaryFile = tempDir.resolve("binary.jpg");
        byte[] binaryBytes = {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, // JPEG header
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46  // JFIF marker
        };
        Files.write(binaryFile, binaryBytes);
    }

    // ========================================
    // UTF-8 Validation Tests
    // ========================================

    @Test
    public void testValidUtf8File() {
        boolean result = UTF8ValidationService.isValidUtf8(utf8File);
        assertTrue("Valid UTF-8 file should be detected as valid", result);
    }

    @Test
    public void testInvalidUtf8File() {
        boolean result = UTF8ValidationService.isValidUtf8(utf16File);
        assertFalse("UTF-16 file should be detected as invalid UTF-8", result);
    }

    @Test
    public void testInvalidByteSequence() {
        boolean result = UTF8ValidationService.isValidUtf8(invalidFile);
        assertFalse("File with invalid byte sequence should be detected as invalid", result);
    }

    @Test
    public void testBinaryFileValidation() {
        boolean result = UTF8ValidationService.isValidUtf8(binaryFile);
        assertFalse("Binary file should be detected as invalid UTF-8", result);
    }

    @Test
    public void testNonExistentFile() {
        Path nonExistent = tempDir.resolve("does-not-exist.txt");
        boolean result = UTF8ValidationService.isValidUtf8(nonExistent);
        assertTrue("Non-existent file should return true (assume valid)", result);
    }

    @Test
    public void testDirectoryValidation() throws IOException {
        Path directory = tempDir.resolve("test-dir");
        Files.createDirectory(directory);
        
        boolean result = UTF8ValidationService.isValidUtf8(directory);
        assertTrue("Directory should be considered valid", result);
    }

    // ========================================
    // File Scanning Tests
    // ========================================

    @Test
    public void testScanSingleFile() {
        Path[] files = {utf8File};
        List<Path> nonUtf8Files = UTF8ValidationService.scanForNonUtf8Files(files);
        
        assertThat(nonUtf8Files).isEmpty();
    }

    @Test
    public void testScanMultipleFiles() {
        Path[] files = {utf8File, utf16File, invalidFile};
        List<Path> nonUtf8Files = UTF8ValidationService.scanForNonUtf8Files(files);
        
        assertThat(nonUtf8Files)
            .hasSize(2)
            .contains(utf16File, invalidFile)
            .doesNotContain(utf8File);
    }

    @Test
    public void testScanDirectory() throws IOException {
        Path[] directories = {tempDir};
        List<Path> nonUtf8Files = UTF8ValidationService.scanForNonUtf8Files(directories);
        
        // Should find utf16File and invalidFile, but not binaryFile (non-text)
        assertThat(nonUtf8Files)
            .hasSize(2)
            .contains(utf16File, invalidFile)
            .doesNotContain(utf8File, binaryFile);
    }

    @Test
    public void testScanEmptyArray() {
        Path[] emptyArray = {};
        List<Path> result = UTF8ValidationService.scanForNonUtf8Files(emptyArray);
        
        assertThat(result).isEmpty();
    }

    // ========================================
    // File Conversion Tests
    // ========================================

    @Test
    public void testConvertUtf16ToUtf8() throws IOException {
        List<Path> filesToConvert = Arrays.asList(utf16File);
        
        UTF8ValidationService.ConversionResult result = 
            UTF8ValidationService.convertFilesToUtf8(filesToConvert, "UTF-16LE");
        
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailureCount()).isEqualTo(0);
        
        // Verify backup was created
        Path backupFile = Paths.get(utf16File.toString() + ".utf8backup");
        assertThat(Files.exists(backupFile)).isTrue();
        
        // Verify conversion worked
        assertTrue("Converted file should now be valid UTF-8", 
                  UTF8ValidationService.isValidUtf8(utf16File));
    }

    @Test
    public void testConvertWithAutoDetection() throws IOException {
        List<Path> filesToConvert = Arrays.asList(utf16File);
        
        UTF8ValidationService.ConversionResult result = 
            UTF8ValidationService.convertFilesToUtf8(filesToConvert, null); // Auto-detect
        
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailureCount()).isEqualTo(0);
    }

    @Test
    public void testConvertInvalidFile() throws IOException {
        List<Path> filesToConvert = Arrays.asList(invalidFile);
        
        UTF8ValidationService.ConversionResult result = 
            UTF8ValidationService.convertFilesToUtf8(filesToConvert, "UTF-8");
        
        // Should fail to convert invalid file
        assertThat(result.getFailureCount()).isEqualTo(1);
        assertThat(result.getSuccessCount()).isEqualTo(0);
        
        assertThat(result.getFailures()).hasSize(1);
        UTF8ValidationService.ConversionFailure failure = result.getFailures().get(0);
        assertThat(failure.getFilePath()).isEqualTo(invalidFile);
        assertThat(failure.getError()).isNotBlank();
    }

    @Test
    public void testConvertNonExistentFile() throws IOException {
        Path nonExistent = tempDir.resolve("does-not-exist.txt");
        List<Path> filesToConvert = Arrays.asList(nonExistent);
        
        UTF8ValidationService.ConversionResult result = 
            UTF8ValidationService.convertFilesToUtf8(filesToConvert, "UTF-8");
        
        assertThat(result.getFailureCount()).isEqualTo(1);
        assertThat(result.getSuccessCount()).isEqualTo(0);
    }

    // ========================================
    // Edge Case Tests
    // ========================================

    @Test
    public void testEmptyFile() throws IOException {
        Path emptyFile = tempDir.resolve("empty.txt");
        Files.createFile(emptyFile);
        
        boolean result = UTF8ValidationService.isValidUtf8(emptyFile);
        assertTrue("Empty file should be considered valid UTF-8", result);
    }

    @Test
    public void testLargeFile() throws IOException {
        Path largeFile = tempDir.resolve("large.txt");
        StringBuilder content = new StringBuilder();
        
        // Create a 1MB file with valid UTF-8 content
        String pattern = "Line {0}: Hello World with UTF-8 chars: 中文 日本語 한국어\n";
        for (int i = 0; i < 10000; i++) {
            content.append(pattern.replace("{0}", String.valueOf(i)));
        }
        
        Files.write(largeFile, content.toString().getBytes(StandardCharsets.UTF_8));
        
        boolean result = UTF8ValidationService.isValidUtf8(largeFile);
        assertTrue("Large valid UTF-8 file should be detected correctly", result);
    }

    @Test
    public void testFileWithBOM() throws IOException {
        Path bomFile = tempDir.resolve("with-bom.txt");
        
        // Create file with UTF-8 BOM
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        String content = "Content after BOM";
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        
        byte[] fileBytes = new byte[bom.length + contentBytes.length];
        System.arraycopy(bom, 0, fileBytes, 0, bom.length);
        System.arraycopy(contentBytes, 0, fileBytes, bom.length, contentBytes.length);
        
        Files.write(bomFile, fileBytes);
        
        boolean result = UTF8ValidationService.isValidUtf8(bomFile);
        assertTrue("File with UTF-8 BOM should be valid", result);
    }

    // ========================================
    // Performance Tests
    // ========================================

    @Test(timeout = 5000) // 5 second timeout
    public void testValidationPerformance() throws IOException {
        // Create multiple test files
        for (int i = 0; i < 100; i++) {
            Path testFile = tempDir.resolve("perf-test-" + i + ".txt");
            String content = "Performance test file " + i + " with UTF-8: 测试";
            Files.write(testFile, content.getBytes(StandardCharsets.UTF_8));
        }
        
        long startTime = System.currentTimeMillis();
        
        Path[] files = {tempDir};
        List<Path> result = UTF8ValidationService.scanForNonUtf8Files(files);
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Should complete within 5 seconds (enforced by timeout)
        assertTrue("Performance test should complete quickly", duration < 5000);
        assertThat(result).isEmpty(); // All files should be valid UTF-8
    }
}