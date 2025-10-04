package com.dila.dama.plugin.util;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

/**
 * Utility class for encoding operations and validation.
 * Provides robust UTF-8 validation and common encoding management.
 */
public class EncodingUtils {
    
    /**
     * Robustly checks if a file is valid UTF-8 using strict validation.
     * Uses CharsetDecoder with strict error reporting to ensure accurate detection.
     * 
     * @param path The path to the file to validate
     * @return true if the file is valid UTF-8, false otherwise
     */
    public static boolean isValidUtf8(Path path) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
            decoder.onMalformedInput(CodingErrorAction.REPORT);
            decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
            decoder.decode(ByteBuffer.wrap(bytes));
            return true;
        } catch (CharacterCodingException e) {
            return false; // Invalid UTF-8 encoding detected
        } catch (IOException e) {
            return false; // File I/O error
        } catch (Exception e) {
            return false; // Any other error
        }
    }
    
    /**
     * Get available character encodings for the dropdown selection.
     * Returns common encodings that users are likely to encounter.
     * 
     * @return Array of common encoding names
     */
    public static String[] getCommonEncodings() {
        return new String[] {
            "Windows-1252",
            "ISO-8859-1", 
            "GBK",
            "Big5",
            "Shift_JIS",
            "EUC-KR",
            "Windows-1251"
        };
    }
    
    /**
     * Checks if a charset name is valid and supported by the system.
     * 
     * @param charsetName The name of the charset to validate
     * @return true if the charset is supported, false otherwise
     */
    public static boolean isCharsetSupported(String charsetName) {
        try {
            return Charset.isSupported(charsetName);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get a display-friendly name for a charset.
     * Provides fallback to the original name if display name is unavailable.
     * 
     * @param charsetName The charset name
     * @return Display name for the charset
     */
    public static String getCharsetDisplayName(String charsetName) {
        try {
            Charset charset = Charset.forName(charsetName);
            String displayName = charset.displayName();
            return displayName != null ? displayName : charsetName;
        } catch (Exception e) {
            return charsetName;
        }
    }
}