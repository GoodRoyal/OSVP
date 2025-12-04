import spacy
import re
from typing import List, Dict, Any

class ClaimExtractor:
    """
    A simple claim extractor for scientific text.
    Identifies sentences containing potential claims based on patterns and NLP features.
    """
    
    def __init__(self, model_name: str = "en_core_web_sm"):
        """
        Initialize the extractor with a spaCy model.
        
        :param model_name: Name of the spaCy model to load.
        """
        self.nlp = spacy.load(model_name)
        # Common patterns for claims in scientific text
        self.claim_patterns = [
            r'.*(?:shows|indicates|demonstrates|reveals|finds?\s+that).*',  # e.g., "The study shows that..."
            r'.*(?:results\s+(?:suggest|indicate)).*',  # e.g., "Results indicate..."
            r'.*(?:we\s+(?:propose|hypothesize|claim)).*',  # e.g., "We propose that..."
            r'.*(?:evidence\s+(?:supports|contradicts)).*',  # e.g., "Evidence supports..."
        ]
    
    def extract_claims(self, text: str) -> List[Dict[str, Any]]:
        """
        Extract claims from the input text.
        
        :param text: Input scientific text (e.g., abstract or paper section).
        :return: List of dictionaries, each containing a claim with metadata.
        """
        doc = self.nlp(text)
        claims = []
        
        for sent in doc.sents:
            sent_text = sent.text.strip()
            # Check if sentence matches claim patterns
            if any(re.search(pattern, sent_text, re.IGNORECASE) for pattern in self.claim_patterns):
                # Extract entities for context
                entities = {ent.label_: ent.text for ent in sent.ents}
                # Simple claim strength based on modals (tentative if 'may', 'might', etc.)
                modals = ['may', 'might', 'could', 'suggest', 'indicate']
                strength = 'tentative' if any(modal in sent_text.lower() for modal in modals) else 'assertive'
                
                claim = {
                    'text': sent_text,
                    'entities': entities,
                    'strength': strength,
                    'confidence': 0.8  # Placeholder; could be enhanced with ML scoring
                }
                claims.append(claim)
        
        return claims

# Example usage
if __name__ == "__main__":
    extractor = ClaimExtractor()
    sample_text = """
    The experiment demonstrates that the new algorithm reduces processing time by 40%.
    Results suggest that further validation is needed in larger datasets.
    We propose a model where quantum effects may influence the outcome.
    """
    claims = extractor.extract_claims(sample_text)
    for claim in claims:
        print(f"Claim: {claim['text']}")
        print(f"Entities: {claim['entities']}")
        print(f"Strength: {claim['strength']}\n")
