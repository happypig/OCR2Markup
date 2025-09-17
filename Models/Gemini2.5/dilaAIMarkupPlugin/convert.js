var apiKey = ""

// var Base64 = Packages.java.util.Base64;
// var encodedApiKey = Base64.getEncoder().encodeToString(apiKey.getPassword().join("").getBytes());
// packages.java.lang.System.err.println("Encoded API Key: " + encodedApiKey);

// Node.js version
var encodedApiKey = Buffer.from(apiKey, 'utf8').toString('base64');
console.error("Encoded API Key: " + encodedApiKey);