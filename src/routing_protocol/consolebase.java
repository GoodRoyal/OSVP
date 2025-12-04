import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ClaimExtractor {
    private SentenceDetectorME sentenceDetector;
    private TokenizerME tokenizer;
    private List<Pattern> claimPatterns;

    /**
     * Initialize the extractor with OpenNLP models.
     * Assumes models are in /models/ directory.
     */
    public ClaimExtractor() throws Exception {
        InputStream sentStream = new FileInputStream("models/en-sent.bin");
        InputStream tokenStream = new FileInputStream("models/en-token.bin");
        SentenceModel sentModel = new SentenceModel(sentStream);
        TokenizerModel tokenModel = new TokenizerModel(tokenStream);
        
        this.sentenceDetector = new SentenceDetectorME(sentModel);
        this.tokenizer = new TokenizerME(tokenModel);
        
        // Common patterns for claims in scientific text
        this.claimPatterns = new ArrayList<>();
        claimPatterns.add(Pattern.compile(".*(?:shows|indicates|demonstrates|reveals|finds\\s+that).*", Pattern.CASE_INSENSITIVE));
        claimPatterns.add(Pattern.compile(".*(?:results\\s+(?:suggest|indicate)).*", Pattern.CASE_INSENSITIVE));
        claimPatterns.add(Pattern.compile(".*(?:we\\s+(?:propose|hypothesize|claim)).*", Pattern.CASE_INSENSITIVE));
        claimPatterns.add(Pattern.compile(".*(?:evidence\\s+(?:supports|contradicts)).*", Pattern.CASE_INSENSITIVE));
    }

    /**
     * Extract claims from the input text.
     *
     * @param text Input scientific text (e.g., abstract or paper section).
     * @return List of maps, each containing a claim with metadata.
     */
    public List<Map<String, Object>> extractClaims(String text) {
        List<Map<String, Object>> claims = new ArrayList<>();
        
        String[] sentences = sentenceDetector.sentDetect(text);
        
        for (String sentText : sentences) {
            sentText = sentText.trim();
            // Check if sentence matches claim patterns
            boolean isClaim = false;
            for (Pattern pattern : claimPatterns) {
                if (pattern.matcher(sentText).matches()) {
                    isClaim = true;
                    break;
                }
            }
            
            if (isClaim) {
                // Simple entity extraction via tokenization (placeholder; enhance with NER model)
                String[] tokens = tokenizer.tokenize(sentText);
                Map<String, String> entities = new HashMap<>();
                // Basic entity tagging (e.g., look for percentages, organizations)
                for (String token : tokens) {
                    if (token.matches("\\d+%")) {
                        entities.put("PERCENT", token);
                    } else if (token.matches("[A-Z][a-z]+\\s+[A-Z][a-z]+")) {  // Simple org-like
                        entities.put("ORG", token);
                    }
                }
                
                // Simple claim strength based on modals
                String[] modals = {"may", "might", "could", "suggest", "indicate"};
                String strength = "assertive";
                for (String modal : modals) {
                    if (sentText.toLowerCase().contains(modal)) {
                        strength = "tentative";
                        break;
                    }
                }
                
                Map<String, Object> claim = new HashMap<>();
                claim.put("text", sentText);
                claim.put("entities", entities);
                claim.put("strength", strength);
                claim.put("confidence", 0.8);  // Placeholder
                claims.add(claim);
            }
        }
        
        return claims;
    }

    // Example usage
    public static void main(String[] args) throws Exception {
        ClaimExtractor extractor = new ClaimExtractor();
        String sampleText = """
            The experiment demonstrates that the new algorithm reduces processing time by 40%.
            Results suggest that further validation is needed in larger datasets.
            We propose a model where quantum effects may influence the outcome.
            """;
        
        List<Map<String, Object>> claims = extractor.extractClaims(sampleText);
        for (Map<String, Object> claim : claims) {
            System.out.println("Claim: " + claim.get("text"));
            System.out.println("Entities: " + claim.get("entities"));
            System.out.println("Strength: " + claim.get("strength") + "\n");
        }
    }
}
