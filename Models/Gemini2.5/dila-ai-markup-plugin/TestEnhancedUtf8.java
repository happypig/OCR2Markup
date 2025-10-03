import java.nio.file.Paths;
import com.dila.dama.plugin.preferences.DAMAOptionPagePluginExtension;

public class TestEnhancedUtf8 {
    public static void main(String[] args) {
        // Test UTF-8 file
        System.out.println("Testing UTF-8 file:");
        boolean utf8Result = DAMAOptionPagePluginExtension.isValidUtf8(
            Paths.get("test-files/test-utf8.txt")
        );
        System.out.println("UTF-8 file validation result: " + utf8Result);
        
        // Test UTF-16 file
        System.out.println("\nTesting UTF-16 file:");
        boolean utf16Result = DAMAOptionPagePluginExtension.isValidUtf8(
            Paths.get("test-files/test-utf16-le.txt")
        );
        System.out.println("UTF-16 file validation result: " + utf16Result);
        
        // Test encodings list
        System.out.println("\nTesting enhanced encoding list:");
        String[] encodings = DAMAOptionPagePluginExtension.getCommonEncodings();
        System.out.println("Number of encodings: " + encodings.length);
        for (String encoding : encodings) {
            System.out.println("  - " + encoding);
        }
    }
}