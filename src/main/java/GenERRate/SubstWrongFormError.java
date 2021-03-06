package GenERRate;

import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class SubstWrongFormError
 *
 * @author Jennifer Foster
 */
public class SubstWrongFormError extends SubstError {
    private static final String VOWEL = "aeiou";
    private static final String CONSONANT = "bcdfghjklmnpqrstvwxyz";
    private static final String PUNCTUATION = "!\"#$%&'()*+,./:;<=>?@[]^_`{|}~";

    private static final Pattern CONSONANT_CONSONANT_ING = Pattern.compile(
            ".*([" + CONSONANT + "])\\1ing", Pattern.CASE_INSENSITIVE);
    private static final Pattern VOWEL_VOWEL_M = Pattern.compile(
            ".*[aeiou][aeuio]m$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONSONANT_CONSONANT_ED = Pattern.compile(
            ".*([" + CONSONANT + "])\\1ed", Pattern.CASE_INSENSITIVE);
    private static final Pattern VOWEL_CONSONANT = Pattern.compile(
            "(.*[" + VOWEL + "])([" + CONSONANT + "])", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONSONANT_E = Pattern.compile(
            ".*[" + CONSONANT + "]e$", Pattern.CASE_INSENSITIVE);
    private static final Pattern VOWEL_VOWEL_CONSONANT = Pattern.compile(
            ".*[" + VOWEL + "][" + VOWEL + "][" + CONSONANT + "]$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONSONANT_VOWEL_CONSONANT = Pattern.compile(
            "(.*[" + CONSONANT + "][" + VOWEL + "])([" + CONSONANT + "])$", Pattern.CASE_INSENSITIVE);

    /**
     * The part-of-speech tag set in effect (WSJ, CLAWS).
     */
    private final PartOfSpeech tagSet;

    /**
     * The sourceTag of the word to be changed.
     */
    private final String sourceTag;

    /**
     * The nature of the change to the word. What targetTag the change can take will depend on the part-of-speech for the word.
     * For a verb, for example, this might be the tense or number. For an adjective or an adverb, the change will be between comparative,  superlative or normal forms.
     */
    private final String targetTag;

    private final List<String> extraWords;

    private final Set<String> dictionary;

    public SubstWrongFormError(Sentence sentence, PartOfSpeech tagSet, String sourceTag, String targetTag,
                               List<String> extraWords, Set<String> dictionary) {
        super(sentence);
        this.tagSet = tagSet;
        this.sourceTag = sourceTag;
        this.targetTag = targetTag;
        this.extraWords = extraWords;
        this.dictionary = dictionary;
        super.errorInfo = "errortype=\"SubstWrongForm" + this.sourceTag + this.targetTag + "Error\"";
    }

    /**
     * Selects a word of the given sourceTag from the sentence and changes the word based on
     * the value of the targetTag attribute.
     * Throws a CannotCreateErrorException if the sentence does not contain a sourceTag with
     * this targetTag.
     *
     * @return Sentence
     */
    public Sentence insertError() throws CannotCreateErrorException {
        // if the sentence itself is empty, nothing can be added
        // throw an exception
        if (sentence.size() < 1) {
            throw new CannotCreateErrorException("The sentence is empty. Cannot substitute one word for another");
        }
        //if the sentence is not tagged, this type of substitution error cannot be substituted
        if (!sentence.areTagsIncluded()) {
            throw new CannotCreateErrorException("Cannot substitute a word with sourceTag tag " + sourceTag + ". The input sentence is not tagged.");
        }
        Sentence newSentence = new Sentence(sentence.toString(), sentence.areTagsIncluded());
        //find all words in the sentence tagged as sourceTag
        List<Integer> listPOS = new ArrayList<Integer>();
        Word word;
        for (int i = 0; i < newSentence.size(); i++) {
            word = newSentence.getWord(i);
            if (word.getTag().equals(sourceTag) && sourceTag.equals(tagSet.INF)) {
                Word nextWord = newSentence.getWord(i + 1);
                if (nextWord != null && nextWord.getTag().equals(tagSet.VERB_BASE)) {
                    listPOS.add(i);
                }
            } else if (word.getTag().equals(sourceTag)) {
                listPOS.add(i);
            }
        }
        //throw an exception if there is no word of this sourceTag in the sentence
        if (listPOS.size() < 1) {
            throw new CannotCreateErrorException("Cannot substitute a word with sourceTag " + sourceTag + " because there is none in the sentence.");
        }
        Random random = new Random(newSentence.toString().hashCode());

        //randomly choose the position in the sentence where the word should be replaced
        int where = listPOS.get(random.nextInt(listPOS.size()));

        //delete the word which was at this position in the sentence
        Word oldWord = newSentence.getWord(where);
        Word newWord = null;
        Word anotherNewWord = null;
        int where2 = -1;

        //examine the sourceTag and the targetTag to see how to substitute the word
        if ((sourceTag.equals(tagSet.SINGULAR_NOUN)) && (targetTag.equals(tagSet.PLURAL_NOUN))) {
            newWord = makeNounPlural(oldWord);
        } else if ((sourceTag.equals(tagSet.PLURAL_NOUN)) && (targetTag.equals(tagSet.SINGULAR_NOUN))) {
            newWord = makeNounSingular(oldWord);
        } else if ((sourceTag.equals(tagSet.VERB_THIRD_SING)) && (targetTag.equals(tagSet.VERB_NON_THIRD_SING))) {
            newWord = thirdSingularToNonThirdSingular(oldWord);
        } else if ((sourceTag.equals(tagSet.VERB_NON_THIRD_SING)) && (targetTag.equals(tagSet.VERB_THIRD_SING))) {
            newWord = nonThirdSingularToThirdSingular(oldWord);
        } else if ((sourceTag.equals(tagSet.VERB_THIRD_SING)) && (targetTag.equals(tagSet.VERB_PRES_PART))) {
            newWord = thirdSingToPresP(oldWord);
        }
    /*else if ((sourceTag.equals(tagSet.VERB_NON_THIRD_SING)) && (targetTag.equals(tagSet.VERB_PAST_PART)))
    {
		newWord = nonThirdSingToPastP(oldWord);
	}*/
        else if ((sourceTag.equals(tagSet.VERB_PRES_PART)) && (targetTag.equals(tagSet.VERB_PAST_PART))) {
            newWord = presPToPastP(oldWord);
        } else if ((sourceTag.equals(tagSet.VERB_PRES_PART)) && (targetTag.equals(tagSet.VERB_THIRD_SING))) {
            newWord = presPToThirdSing(oldWord);
        } else if ((sourceTag.equals(tagSet.VERB_PRES_PART)) && (targetTag.equals(tagSet.VERB_NON_THIRD_SING))) {
            newWord = presPToNonThirdSing(oldWord);
        } else if ((sourceTag.equals(tagSet.VERB_PRES_PART)) && (targetTag.equals(tagSet.INF))) {
            newWord = presPToInf(oldWord);
            anotherNewWord = buildSubstitution("to", tagSet.INF, oldWord.getToken());
        } else if ((sourceTag.equals(tagSet.INF)) && (targetTag.equals(tagSet.VERB_PRES_PART))) {
            oldWord = newSentence.getWord(where + 1);
            newWord = baseToPresP(oldWord);
            where2 = where + 1;
        } else if ((sourceTag.equals(tagSet.VERB_BASE)) && (targetTag.equals(tagSet.VERB_PRES_PART))) {
            newWord = baseToPresP(oldWord);
        } else if ((sourceTag.equals(tagSet.VERB_NON_THIRD_SING)) && (targetTag.equals(tagSet.VERB_PRES_PART))) {
            newWord = nonThirdSingToPresP(oldWord);
        } else if ((sourceTag.equals(tagSet.VERB_PAST_PART)) && (targetTag.equals(tagSet.VERB_THIRD_SING))) {
            newWord = pastPToThirdSing(oldWord);
        } else if ((sourceTag.equals(tagSet.VERB_PAST_PART)) && (targetTag.equals(tagSet.VERB_PRES_PART))) {
            newWord = pastPToPresP(oldWord);
        } else if ((sourceTag.equals(tagSet.VERB_BASE)) && (targetTag.equals(tagSet.VERB_THIRD_SING))) {
            newWord = baseToThirdSing(oldWord);
        } else if ((sourceTag.equals(tagSet.ADJ)) && (targetTag.equals(tagSet.ADJ_COMP))) {
            newWord = regularAdjToComparative(oldWord);
        } else if ((sourceTag.equals(tagSet.ADJ)) && (targetTag.equals(tagSet.ADJ_SUP))) {
            newWord = regularAdjToSuperlative(oldWord);
        } else if ((sourceTag.equals(tagSet.ADJ_COMP)) && (targetTag.equals(tagSet.ADJ_SUP))) {
            newWord = comparativeAdjToSuperlative(oldWord);
        } else if ((sourceTag.equals(tagSet.ADJ_COMP)) && (targetTag.equals(tagSet.ADJ))) {
            newWord = comparativeAdjToRegular(oldWord);
        } else if ((sourceTag.equals(tagSet.ADJ_SUP)) && (targetTag.equals(tagSet.ADJ))) {
            newWord = superlativeAdjToRegular(oldWord);
        } else if ((sourceTag.equals(tagSet.ADJ_SUP)) && (targetTag.equals(tagSet.ADJ_COMP))) {
            newWord = superlativeAdjToComparative(oldWord);
        } else if ((sourceTag.equals(tagSet.ADV)) && (targetTag.equals(tagSet.ADJ)) && (oldWord.getToken().endsWith("ly"))) {
            newWord = adverbToAdj(oldWord);
        } else {
            //find all the words tagged as targetTag in the extra word list
            List<String> formList = new ArrayList<String>();
            String tokenTag;
            String pos;
            StringTokenizer tokens;
            for (int i = 0; i < extraWords.size(); i++) {
                tokenTag = extraWords.get(i);
                tokens = new StringTokenizer(tokenTag, " ");
                if (tokens.hasMoreTokens()) {
                    tokens.nextToken();
                }
                if (tokens.hasMoreTokens()) {
                    pos = tokens.nextToken();
                    if (pos.equals(targetTag)) {
                        formList.add(tokenTag);
                    }
                }

            }
            if (formList.size() == 0) {
                throw new CannotCreateErrorException("No word with the sourceTag " + targetTag + " in the extra word list. Cannot create an " + errorInfo);
            }

            //randomly select the replacing word
            int newWordPos = random.nextInt(formList.size());
            String newWordString = formList.get(newWordPos);
            tokens = new StringTokenizer(newWordString, " ");
            if (tokens.countTokens() == 2) {
                newWord = buildSubstitution(tokens.nextToken(), tokens.nextToken(), tokens.nextToken());
            }
        }

        validateReplacement(oldWord, newWord);

        if (where2 == -1) {
            newSentence.removeWord(where);
        } else {
            newSentence.removeWord(where);
            newSentence.removeWord(where);
        }

        if (anotherNewWord == null) {
            newSentence.insertWord(newWord, where);
        } else {
            newSentence.insertWord(anotherNewWord, where);
            newSentence.insertWord(newWord, where + 1);
        }

        newSentence.setErrorDescription(errorInfo + " details=\"" + oldWord.getToken() + "/" + newWord.getToken() + " at " + (where + 1) + "\"");

        return newSentence;

    }

    /**
     * Build a Word using the three-arg constructor, which preserves the case of the original token.
     *
     * @param replacement the token replacing the original token
     * @param tag         the part-of-speech tag of the replacement
     * @param original    the original token
     * @return
     */
    public Word buildSubstitution(String replacement, String tag, String original) {
        return new Word(replacement, tag, original);
    }

    public void validateReplacement(Word original, Word replacement) throws CannotCreateErrorException {
        if (replacement == null) {
            throw new CannotCreateErrorException(
                    "There was a problem inserting a SubstWrongFormError.");
        }

        if (dictionary != null && dictionary.size() != 0) {
            if (!dictionary.contains(replacement.getToken().toLowerCase())) {
                throw new CannotCreateErrorException(
                        "The replacement performed by SubstWrongFormError " +
                                "is invalid because the replacement word does not exist in the dictionary: '" +
                                original.getToken().toLowerCase() + "/" + original.getTag() + "' => '" +
                                replacement.getToken().toLowerCase() + "/" + replacement.getTag() + "'");
            }
        }
    }

    public Word makeNounSingular(Word word) {
        final String newTag = tagSet.SINGULAR_NOUN;
        final String token = word.getToken();

        if (token.endsWith("ies")) {
            if (token.equalsIgnoreCase("movies")) {
                return buildSubstitution(token.substring(0, token.length() - 1), newTag, token);
            } else {
                return buildSubstitution(token.substring(0, token.length() - 3) + "y", newTag, token);
            }
        } else if (token.endsWith("men")) {
            return buildSubstitution(token.substring(0, token.length() - 3) + "man", newTag, token);
        } else if (token.endsWith("a")) {
            return buildSubstitution(token.substring(0, token.length() - 1) + "um", newTag, token);
        } else if (token.endsWith("ches") || token.endsWith("sses") || token.endsWith("zes") || token.endsWith("shes") || token.endsWith("xes")) {
            return buildSubstitution(token.substring(0, token.length() - 2), newTag, token);
        } else if (token.length() > 0) {
            return buildSubstitution(token.substring(0, token.length() - 1), newTag, token);
        } else {
            return null;
        }
    }

    public Word makeNounPlural(Word word) {
        final String plural_noun = tagSet.PLURAL_NOUN;
        final String token = word.getToken();

        if (token.endsWith("man")) {
            return buildSubstitution(token.substring(0, token.length() - 3) + "men", plural_noun, token);
        } else if (token.endsWith("ch") || token.endsWith("s") || token.endsWith("z") || token.endsWith("sh") || token.endsWith("x")) {
            return buildSubstitution(token + "es", plural_noun, token);
        } else if (token.endsWith("y") && token.length() > 1 && !ErrorUtilities.isVowel(token.charAt(token.length() - 2))) {
            return buildSubstitution(token.substring(0, token.length() - 1) + "ies", plural_noun, token);
        } else if (token.endsWith("um")) {
            return buildSubstitution(token.substring(0, token.length() - 2) + "a", plural_noun, token);
        } else {
            return buildSubstitution(token + "s", plural_noun, token);
        }
    }

    public Word nonThirdSingularToThirdSingular(Word word) {
        final String tag = tagSet.VERB_THIRD_SING;
        final String token = word.getToken();

        if (token.equalsIgnoreCase("are") || token.equalsIgnoreCase("'re") || token.equalsIgnoreCase("'m") || token.equalsIgnoreCase("am")) {
            return buildSubstitution("is", tag, token);
        } else if (token.equalsIgnoreCase("have")) {
            return buildSubstitution("has", tag, token);
        } else if (token.equalsIgnoreCase("do")) {
            return buildSubstitution("does", tag, token);
        } else if (token.equalsIgnoreCase("go")) {
            return buildSubstitution("goes", tag, token);
        } else if (token.equalsIgnoreCase("shall")) {
            return buildSubstitution(token, tag, token);
        } else if (token.endsWith("y") && token.length() > 1 && !ErrorUtilities.isVowel(token.charAt(token.length() - 2))) {
            return buildSubstitution(token.substring(0, token.length() - 1) + "ies", tag, token);
        } else if (token.endsWith("ch") || token.endsWith("x") || token.endsWith("s") || token.endsWith("z") || token.endsWith("sh")) {
            return buildSubstitution(token + "es", tag, token);
        } else {
            return buildSubstitution(token + "s", tag, token);
        }
    }

    protected boolean isIES(String token) {
        token = token.toLowerCase();
        return token.equals("ties") ||
                token.equals("unties") ||
                token.equals("lies") ||
                token.equals("dies");
    }

    protected boolean isIESToY(String token) {
        token = token.toLowerCase();
        return token.endsWith("ies");
    }

    public Word thirdSingularToNonThirdSingular(Word word) {
        final String tag = tagSet.VERB_NON_THIRD_SING;
        final String token = word.getToken();

        if (token.equalsIgnoreCase("is") || token.equalsIgnoreCase("'s")) {
            return buildSubstitution("are", tag, token);
        } else if (token.equalsIgnoreCase("has")) {
            return buildSubstitution("have", tag, token);
        } else if (token.equalsIgnoreCase("does")) {
            return buildSubstitution("do", tag, token);
        } else if (token.equalsIgnoreCase("goes")) {
            return buildSubstitution("go", tag, token);
        } else if (token.equalsIgnoreCase("shall")) {
            return buildSubstitution(token, tag, token);
        } else if (token.toLowerCase().endsWith("iases")) {
            return buildSubstitution(token.substring(0, token.length() - 2), tag, token);
        } else if (isIES(token)) {
            return buildSubstitution(token.substring(0, token.length() - 1), tag, token);
        } else if (isIESToY(token)) {
            return buildSubstitution(token.substring(0, token.length() - 3) + "y", tag, token);
        } else if (token.endsWith("oes")) {
            return buildSubstitution(token.substring(0, token.length() - 2), tag, token);
        } else if (token.toLowerCase().startsWith("focus")) {
            return buildSubstitution("focus", tag, token);
        } else if (token.endsWith("sses") || token.endsWith("ches") || token.endsWith("xes") ||
                token.endsWith("zzes") || token.endsWith("shes")) {
            if (token.endsWith("uizzes")) {
                return buildSubstitution("quiz", tag, token);
            } else {
                return buildSubstitution(token.substring(0, token.length() - 2), tag, token);
            }
        } else if (token.length() > 0) {
            return buildSubstitution(token.substring(0, token.length() - 1), tag, token);
        } else {
            return null;
        }
    }

    protected boolean baseConvertCToCk(String token) {
        token = token.toLowerCase();
        return token.endsWith("anic") || token.endsWith("imic");
    }

    public Word thirdSingToBase(Word word) {
        final String tag = tagSet.VERB_BASE;
        final String token = word.getToken();

        if (token.equalsIgnoreCase("is")) {
            return buildSubstitution("be", tag, token);
        } else if (token.toLowerCase().endsWith("does") || token.toLowerCase().endsWith("goes")) {
            return buildSubstitution(token.substring(0, token.length() - 2), tag, token);
        } else if (token.equalsIgnoreCase("has")) {
            return buildSubstitution("have", tag, token);
        } else if (token.endsWith("ies")) {
            return buildSubstitution(token.substring(0, token.length() - 3) + "y", tag, token);
        } else if (token.equalsIgnoreCase("begat")) {
            return buildSubstitution(token.substring(0, token.length() - 2) + "et", tag, token);
        } else if (token.endsWith("yes") || token.endsWith("ys")) {
            // e.g. eyes -> eye, relays -> relay
            return buildSubstitution(token.substring(0, token.length() - 1), tag, token);
        } else if (token.endsWith("es")) {
            return buildSubstitution(token.substring(0, token.length() - 1), tag, token);
        } else if (token.length() > 0) {
            return buildSubstitution(token.substring(0, token.length() - 1), tag, token);
        } else {
            return null;
        }

    }

    protected boolean thirdSingToPresPDuplicateFinalConsonant(String token, Matcher matcher) {
        if (!matcher.matches()) {
            //System.out.println(token + " doesn't match CVC");
            return false;
        }

        token = token.toLowerCase();

        //System.out.println("matches CVC: " + token);
        // The word ends with consonant-vowel-consonant.
        if (token.endsWith("h") || token.endsWith("w") ||
                token.endsWith("x") || token.endsWith("y")) {
            // Words that end in these characters are never doubled.
            //System.out.println("doesn't end in a non-doubling character: " + token);
            return false;
        }

        if (token.length() == 3) {
            // e.g. set -> settting, get -> getting
            return true;
        }

        return !token.endsWith("nger") &&
                !token.endsWith("mber") &&
                !token.endsWith("eaten") &&
                !token.endsWith("aden") &&
                !token.endsWith("atten") &&
                !token.endsWith("avel") &&
                !token.endsWith("ider") &&
                !token.endsWith("nder") &&
                !token.endsWith("wer") &&
                !token.endsWith("ken") &&
                !token.endsWith("cit") &&
                !token.endsWith("rit") &&
                !token.endsWith("ven") &&
                !token.endsWith("avor") &&
                !token.endsWith("over") &&
                !token.endsWith("ther") &&
                !token.endsWith("rsen") &&
                !token.endsWith("mirror") &&
                !token.endsWith("outlaw") &&
                !token.endsWith("liken") &&
                !token.endsWith("visit") &&
                !token.endsWith("target") &&
                !token.endsWith("market") &&
                !token.endsWith("rival") &&
                !token.endsWith("caper") &&
                !token.endsWith("taper") &&
                !token.endsWith("chisel") &&
                !token.endsWith("counter") &&
                !token.endsWith("ghten") &&
                !token.endsWith("gthen") &&
                !token.endsWith("iden") &&
                !token.endsWith("pen") &&
                !token.endsWith("edit") &&
                !token.endsWith("ender") &&
                !token.endsWith("enter") &&
                !token.endsWith("onder") &&
                !token.endsWith("utter") &&
                !token.endsWith("order") &&
                !token.endsWith("osit") &&
                !token.endsWith("ffer") &&
                !token.endsWith("fit") &&
                !token.endsWith("gger") &&
                !token.endsWith("ister") &&
                !token.endsWith("isten") &&
                !token.endsWith("llel") &&
                !token.endsWith("otal") &&
                !token.endsWith("ander") &&
                !token.endsWith("sor") &&
                !token.endsWith("bit") &&
                !token.endsWith("met") &&
                !token.endsWith("pret") &&
                !token.endsWith("limit") &&
                !token.endsWith("eter") &&
                !token.endsWith("ndon") &&
                !token.endsWith("debut") &&
                !token.endsWith("cover") &&
                !token.endsWith("sper") &&
                !token.endsWith("mpet") &&
                !token.endsWith("itor") &&
                !token.endsWith("iver") &&
                !token.endsWith("uffer") &&
                !token.endsWith("ater") &&
                !token.endsWith("lter") &&
                !token.endsWith("ster") &&
                !token.endsWith("elop") &&
                !token.endsWith("pivot");
    }

    public Word thirdSingToPresP(Word word) {
        final String tag = tagSet.VERB_PRES_PART;
        final Word base = thirdSingToBase(word);
        final String token = base.getToken();
        final Matcher cvcMatcher = CONSONANT_VOWEL_CONSONANT.matcher(token);
        final Matcher ceMatcher = CONSONANT_E.matcher(token);

        //System.out.println("third sing " + word.getToken() + " => base " + token);

        if (token.equalsIgnoreCase("be")) {
            return buildSubstitution("being", tag, token);
        } else if (token.equalsIgnoreCase("have")) {
            return buildSubstitution("having", tag, token);
        } else if (token.endsWith("ue")) {
            // e.g. continue -> continuing
            return buildSubstitution(token.substring(0, token.length() - 1) + "ing", tag, token);
        } else if (token.equalsIgnoreCase("beget")) {
            return buildSubstitution("begetting", tag, token);
        } else if (token.endsWith("ye") || token.endsWith("y")) {
            //System.out.println("append ing");
            return buildSubstitution(token.substring(0, token.length()) + "ing", tag, token);
        } else if (baseConvertCToCk(token)) {
            return buildSubstitution(token + "king", tag, token);
        } else if (ceMatcher.matches()) {
            return buildSubstitution(token.substring(0, token.length() - 1) + "ing", tag, token);
        } else if (thirdSingToPresPDuplicateFinalConsonant(token, cvcMatcher)) {
            //System.out.println("append TRAILING CONSONANT ing: " + token);
            if (token.length() == 3) {
                Character last = token.charAt(token.length() - 1);
                return buildSubstitution(token + last.toString() + "ing", tag, token);
            } else {
                MatchResult result = cvcMatcher.toMatchResult();
                String newToken = result.group(1) + result.group(2) + result.group(2) + "ing";
                return buildSubstitution(newToken, tag, token);
            }
        } else if (token.toLowerCase().endsWith("quit") || token.toLowerCase().endsWith("quat") ||
                token.toLowerCase().endsWith("qual") || token.toLowerCase().endsWith("quip")) {
            Character last = token.charAt(token.length() - 1);
            return buildSubstitution(token + last.toString() + "ing", tag, token);
        } else if (token.length() > 0) {
            //System.out.println("DEFAULT: append ing");
            return buildSubstitution(token.substring(0, token.length()) + "ing", tag, token);
        } else {
            return null;
        }
    }

    public Word nonThirdSingToPresP(Word word) {
        final String tag = tagSet.VERB_PRES_PART;
        final String token = word.getToken();

        if (token.equalsIgnoreCase("are")) {
            return buildSubstitution("being", tag, token);
        } else if (token.endsWith("e") && !token.endsWith("ee")) {
            return buildSubstitution(token.substring(0, token.length() - 1) + "ing", tag, token);
        } else if (token.endsWith("t")
                && (!(token.substring(token.length() - 3, token.length() - 1).equals("ea")))
                && token.length() > 1
                && ErrorUtilities.isVowel(token.charAt(token.length() - 2))) {
            return buildSubstitution(token + "ting", tag, token);
        } else {
            return buildSubstitution(token + "ing", tag, token);
        }
    }

    public Word baseToPresP(Word word) {
        String tag = tagSet.VERB_PRES_PART;
        final String token = word.getToken();

        final Matcher vvcMatcher = VOWEL_VOWEL_CONSONANT.matcher(token);

        if (token.equalsIgnoreCase("be")) {
            return buildSubstitution("being", tag, token);
        } else if (token.toLowerCase().endsWith("saw")) {
            return buildSubstitution(token.substring(0, token.length() - 2) + "eeing", tag, token);
        } else if (vvcMatcher.matches()) {
            if (token.toLowerCase().endsWith("quit")) {
                return buildSubstitution(token + "ting", tag, token);
            } else {
                return buildSubstitution(token + "ing", tag, token);
            }
        } else if (token.endsWith("an")) {
            // e.g. man -> manning, pan -> panning
            return buildSubstitution(token + "ning", tag, token);
        } else if (token.endsWith("e") && !token.endsWith("ee")) {
            if (token.toLowerCase().endsWith("hoe") || token.endsWith("noe")) {
                // e.g. hoe -> hoeing, shoe -> shoeing, canoe -> canoeing
                return buildSubstitution(token + "ing", tag, token);
            } else if (token.endsWith("ie") /* && token.length() == 3 */) {
                // e.g. tie -> tying, die -> dying, underlie -> underlying
                return buildSubstitution(token.substring(0, token.length() - 2) + "ying", tag, token);
            } else {
                // e.g. hope -> hoping
                return buildSubstitution(token.substring(0, token.length() - 1) + "ing", tag, token);
            }
        } else if (token.endsWith("am") || token.endsWith("um")) {
            if (VOWEL_VOWEL_M.matcher(token).matches()) {
                // e.g. foam -> foaming
                return buildSubstitution(token + "ing", tag, token);
            } else {
                // e.g. dam -> damming, ram -> ramming,
                return buildSubstitution(token + "ming", tag, token);
            }
        } else if (token.endsWith("ise")) {
            // e.g. subsidise -> subsidising
            return buildSubstitution(token.substring(0, token.length() - 1) + "ing", tag, token);
        } else if (token.endsWith("er")) {
            if (token.endsWith("efer") || token.endsWith("nfer") || token.endsWith("sfer") ||
                    token.endsWith("deter")) {
                // e.g. refer -> referring, confer -> conferring, transfer -> transferring, deter -> deterring
                return buildSubstitution(token + "ring", tag, token);
            } else {
                return buildSubstitution(token + "ing", tag, token);
            }
        } else if (token.endsWith("eed") || token.endsWith("eem")) {
            // e.g. seed -> seeding, need -> needing, teem -> teeming, deem -> deeming
            return buildSubstitution(token + "ing", tag, token);
        } else if (token.endsWith("id") || token.endsWith("ed") || token.endsWith("ud")) {
            // e.g. forbid -> forbidding, stud -> studding
            return buildSubstitution(token + "ding", tag, token);
        } else if (token.endsWith("ab") || token.endsWith("ob") || token.endsWith("ub")) {
            // e.g. log -> lobbing, rub -> rubbing, grab -> grabbing
            return buildSubstitution(token + "bing", tag, token);
        } else if (token.endsWith("og") || token.endsWith("ag")) {
            // e.g. log -> logging, lag -> lagging
            return buildSubstitution(token + "ging", tag, token);
        } else if (token.endsWith("ek") || token.endsWith("ic")) {
            // e.g. trek -> trekking, mimic -> mimicking
            return buildSubstitution(token + "king", tag, token);
        } else if (token.endsWith("el") || token.endsWith("ul")) {
            if (token.endsWith("del") || token.endsWith("llel")) {
                // e.g. cancel -> canceling, model -> modeling, parallel -> paralleling, level -> leveling
                return buildSubstitution(token + "ing", tag, token);
            } else {
                // e.g. excel -> excelling
                return buildSubstitution(token + "ling", tag, token);
            }
        } else if (token.endsWith("ag") || token.endsWith("eg") || token.endsWith("ig") || token.endsWith("og") || token.endsWith("ug")) {
            // e.g. rig -> rigging, hug -> hugging
            return buildSubstitution(token + "ging", tag, token);
        } else if (token.endsWith("em") || token.endsWith("im")) {
            // e.g. swim -> swimming
            return buildSubstitution(token + "ming", tag, token);
        } else if (token.endsWith("in") || token.endsWith("on") || token.endsWith("un")) {
            if (token.toLowerCase().endsWith("ammon")) {
                // e.g. backgammon -> backgammoning
                return buildSubstitution(token + "ing", tag, token);
            } else {
                return buildSubstitution(token + "ning", tag, token);
            }
        } else if (token.endsWith("ap") || token.endsWith("ep") || token.endsWith("ip") || token.endsWith("op") || token.endsWith("up")) {
            if (token.toLowerCase().endsWith("velop")) {
                return buildSubstitution(token + "ing", tag, token);
            } else {
                // e.g. kidnap -> kidnapping, drop -> dropping
                return buildSubstitution(token + "ping", tag, token);
            }
        } else if (token.endsWith("ol")) {
            // e.g. control -> controlling
            return buildSubstitution(token + "ling", tag, token);
        } else if (token.endsWith("ir") || token.endsWith("or") || token.endsWith("ur")) {
            if (token.endsWith("our")) {
                // e.g. deliver -> delivering, favour -> favouring
                return buildSubstitution(token + "ing", tag, token);
            } else {
                // e.g. incur -> incurring, spur -> spurring, stir -> stirring
                return buildSubstitution(token + "ring", tag, token);
            }
        } else if (token.endsWith("at") || token.endsWith("et") || token.endsWith("it") || token.endsWith("ot") || token.endsWith("ut")) {
            // System.out.println(token + " ends with 't'");
            if (token.endsWith("mbat") || // e.g. combat -> combating
                    token.endsWith("abit") ||
                    token.endsWith("ibit") ||
                    token.endsWith("icit") ||
                    token.endsWith("ket") || // e.g. market -> marketing
                    token.toLowerCase().endsWith("efit") ||
                    token.toLowerCase().endsWith("profit") ||
                    token.toLowerCase().endsWith("rit") || // e.g. inherit -> inheriting
                    token.toLowerCase().endsWith("arget") || // e.g. target -> targeting
                    token.toLowerCase().endsWith("xit") || // e.g. exit -> exiting
                    token.toLowerCase().endsWith("rrot") || // e.g. parrot -> parroting
                    /* token.endsWith("nefit") ||  token.endsWith("rofit") || */ token.equalsIgnoreCase("debut") ||
                    token.toLowerCase().endsWith("visit") || token.toLowerCase().endsWith("osit") ||
                    token.toLowerCase().endsWith("imit") || token.toLowerCase().endsWith("pret") ||
                    token.toLowerCase().endsWith("ivet")) {
                // e.g. benefit -> benefiting, profit -> profiting, debut -> debuting, deposit -> depositing,
                // limit -> limiting, interpret -> interpreting, elicit -> eliciting, rivet -> riveting
                // System.out.println(token + " doesn't get an extra t");
                return buildSubstitution(token + "ing", tag, token);
            } else {
                // e.g. set -> setting, fret -> fretting, outwit -> outwitting
                // System.out.println(token + " gets an extra t");
                return buildSubstitution(token + "ting", tag, token);
            }
        } else {
            return buildSubstitution(token + "ing", tag, token);
        }
    }

    public Word presPToPastP(Word word) {
        final String tag = tagSet.VERB_PAST_PART;
        final String token = word.getToken();

        if (token.equalsIgnoreCase("being")) {
            return buildSubstitution("been", tag, token);
        } else if (token.equalsIgnoreCase("having")) {
            return buildSubstitution("had", tag, token);
        } else if (token.endsWith("coming")) {
            return buildSubstitution(token.substring(0, token.length() - 3) + "e", tag, token);
        } else if (token.equalsIgnoreCase("going")) {
            return buildSubstitution("gone", tag, token);
        } else if (token.equalsIgnoreCase("doing")) {
            return buildSubstitution("done", tag, token);
        } else if (token.equalsIgnoreCase("leaving")) {
            return buildSubstitution("left", tag, token);
        } else if (token.equalsIgnoreCase("taking")) {
            return buildSubstitution("taken", tag, token);
        } else if (token.equalsIgnoreCase("seeing")) {
            return buildSubstitution("seen", tag, token);
        } else if (token.equalsIgnoreCase("making")) {
            return buildSubstitution("made", tag, token);
        } else if (token.equalsIgnoreCase("bringing")) {
            return buildSubstitution("brought", tag, token);
        } else if (token.equalsIgnoreCase("teaching")) {
            return buildSubstitution("taught", tag, token);
        } else if (token.equalsIgnoreCase("reading")) {
            return buildSubstitution("read", tag, token);
        } else if (token.equalsIgnoreCase("letting")) {
            return buildSubstitution("let", tag, token);
        } else if (token.endsWith("wing") &&
                (token.charAt(token.length() - 5) == 'a' ||
                        token.charAt(token.length() - 5) == 'o')) {
            return buildSubstitution(token.substring(0, token.length() - 3) + "n", tag, token);
        } else if (token.endsWith("ying") && !ErrorUtilities.isVowel(token.charAt(token.length() - 5))) {
            return buildSubstitution(token.substring(0, token.length() - 4) + "ied", tag, token);
        } else if (token.endsWith("ing")) {
            return buildSubstitution(token.substring(0, token.length() - 3) + "ed", tag, token);
        } else {
            return null;
        }
    }

    public Word presPToThirdSing(Word word) {
        final String tag = tagSet.VERB_THIRD_SING;
        final Word infinitive = presPToInf(word);

        if (infinitive == null) {
            return infinitive;
        }

        //System.out.println(word.getToken() + " => " + infinitive.getToken());

        return baseToThirdSing(infinitive);
    }

    public Word presPToNonThirdSing(Word word) {
        final String tag = tagSet.VERB_NON_THIRD_SING;
        final String token = word.getToken();

        if (token.equalsIgnoreCase("being")) {
            return buildSubstitution("are", tag, token);
        } else if (token.equalsIgnoreCase("having")) {
            return buildSubstitution("have", tag, token);
        } else if (token.endsWith("ching")
                || token.endsWith("ssing") || token.endsWith("oing") || token.endsWith("dging")
                || token.endsWith("oting")) {
            return buildSubstitution(token.substring(0, token.length() - 3) + "e", tag, token);
        } else if (token.endsWith("ing")) {
            return buildSubstitution(token.substring(0, token.length() - 3), tag, token);
        } else {
            return null;
        }
    }

    protected boolean infEndsWithE(String token) {
        token = token.toLowerCase();
        return token.endsWith("ribing") || // e.g. proscribing -> proscribe
                token.endsWith("robing") || // e.g. probing -> probe
                token.endsWith("ubing") || // e.g. tubing -> tube

                token.endsWith("ancing") || // e.g. freelancing -> freelance
                token.endsWith("eecing") || // e.g. fleecing -> fleece
                token.endsWith("encing")    || // e.g. referencing -> reference
                token.endsWith("incing")    || // e.g. convincing -> convince
                token.endsWith("ouncing")   || // e.g. announcing -> announce
                token.endsWith("acing")     || // e.g. placing -> place
                token.endsWith("licing")    || // e.g. slicing -> slice
                token.endsWith("ourcing")   || // e.g. sourcing -> source
                token.endsWith("ercing")    || // e.g. piercing -> pierce
                token.endsWith("orcing")    || // e.g. divorcing -> divorce
                token.endsWith("scing")     || // e.g. producing -> produce
                token.endsWith("ucing")     || // e.g. convalescing -> convalesce
                token.endsWith("icing")     || // e.g. sacrificing -> sacrifice

                token.endsWith("arauding")  || // e.g. marauding -> maraude
                token.endsWith("cading")    || // e.g. cascading -> cascade
                token.endsWith("nading") || // e.g. serenading -> serenade
                token.endsWith("rading")    || // e.g. degrading -> degrade
                token.endsWith("uading")    || // e.g. persuading -> persuade
                token.endsWith("vading")    || // e.g. pervading -> pervade
                token.endsWith("ceding") || // e.g. conceding -> concede
                token.endsWith("peding") || // e.g. impeding -> impede
                token.endsWith("seding") || // e.g. superseding -> supersede
                token.endsWith("iding") ||
                //token.endsWith("ciding")     || // e.g. deciding -> decide
                //token.endsWith("hiding") || // e.g. chiding -> chide
                //token.endsWith("liding") || // e.g. sliding -> slide, eliding -> elide
                //token.endsWith("siding")    || // e.g. subsiding -> subside
                //token.endsWith("uiding") || // e.g. guiding -> guide
                //token.endsWith("viding") || // e.g. providing -> provide
                token.endsWith("oding")     || // e.g. eroding -> erode
                token.endsWith("luding") || // e.g. including -> include
                token.endsWith("ruding") || // e.g. protruding -> protrude

                token.endsWith("afing") || // e.g. chafing -> chafe

                token.endsWith("caching") || // e.g. caching -> cache


                token.endsWith("oking") || // e.g. joking -> joke, choking -> choke

                token.endsWith("haling") || // e.g. whaling -> whale
                token.equalsIgnoreCase("taling") || // e.g. taling -> tale
                token.endsWith("mbling")    || // e.g. rumbling -> rumble
                token.endsWith("abling")    || // e.g. disabling -> disable
                token.endsWith("cling")     || // e.g. circling -> circle
                token.endsWith("dling") || // e.g. waddling -> waddle
                token.endsWith("fling") || // e.g. stifling -> stifle
                token.endsWith("ggling")    || // e.g. wiggling -> wiggle
                token.endsWith("ngling")    || // e.g. singling -> single
                token.endsWith("ogling")    || // e.g. ogling -> ogle
                token.endsWith("biling") || // e.g. snowmobiling -> snowmobile
                token.endsWith("piling") || // e.g. compiling -> compile
                token.endsWith("ckling")    || // e.g. tickling -> tickle
                token.endsWith("inkling")   || // e.g. wrinkling -> wrinkle
                token.endsWith("ntling")    || // e.g. dismantling -> dismantle
                token.endsWith("ipling") || // e.g. tripling -> triple
                token.endsWith("mpling") || // e.g. trampling -> trample
                token.endsWith("upling") || // e.g. coupling -> couple
                token.endsWith("ppling") || // e.g. rippling -> ripple
                token.endsWith("rtling") || // e.g. wrestling -> wrestle
                token.endsWith("stling") || // e.g. hurtling -> hurtle
                token.endsWith("ttling")    || // e.g. settling -> settle
                token.endsWith("zzling")    || // e.g. embezzling -> embezzle
                token.endsWith("bling")     || // e.g. troubling -> trouble (overrides next rule)
                token.endsWith("bbling")    || // e.g. babbling -> babble
                token.endsWith("paling") || // e.g. impaling -> impale
                token.endsWith("ciling") || // e.g. reconciling -> reconcile
                token.endsWith("filing") || // e.g. filing -> file
                token.endsWith("miling") || // e.g. smiling -> smile
                token.endsWith("soling")    || // e.g. consoling -> console
                token.endsWith("itling")    || // e.g. titling -> title
                token.endsWith("istling")   || // e.g. whistling -> whistle
                token.endsWith("ycling")    || // e.g. cycling -> cycle
                token.endsWith("caling")    || // e.g. scaling -> scale
                token.endsWith("culing")    || // e.g. ridiculing -> ridicule
                token.endsWith("duling")    || // e.g. scheduling -> schedule
                token.endsWith("ruling")    || // e.g. ruling -> rule
                token.endsWith("yling")     || // e.g. styling -> style

                token.endsWith("aging")     || // e.g. managing -> manage
                token.endsWith("dging") || // e.g. abridge -> abridge
                token.endsWith("ieging")     || // e.g. besieging -> besiege
                token.endsWith("leging")     || // e.g. alleging -> allege
                token.endsWith("arging")    || // e.g. charging -> charge
                token.endsWith("erging")    || // e.g. emerging -> emerge
                token.endsWith("orging") || // e.g. forging -> forge
                token.endsWith("ulging")    || // e.g. indulging -> indulge
                token.endsWith("urging") || // e.g. purging -> purge
                token.endsWith("uging")     || // e.g. gouging -> gouge

                token.endsWith("rafing") || // e.g. strafing -> strafe

                token.endsWith("iking")     || // e.g. hiking -> hike
                token.endsWith("voking") || // e.g. invoking -> invoke
                token.endsWith("uking")     || // e.g. rebuking -> rebuke
                token.endsWith("making")    || // e.g. making -> make
                token.endsWith("taking")    || // e.g. taking -> take

                token.endsWith("uming") || // e.g. assuming -> assume
                token.endsWith("coming") || // e.g. unbecoming -> unbecome
                token.endsWith("aming") || // e.g. flaming -> flame, framing -> frame, naming -> name
                //token.endsWith("laming")     || // e.g. flaming -> flame
                // token.endsWith("raming")     || // e.g. framing -> frame
                token.endsWith("iming") || // e.g. chiming -> chime

                token.endsWith("laning") || // e.g. laning -> lane
                token.endsWith("waning") || // e.g. waning -> wane
                token.endsWith("bining")    || // e.g. combining -> combine
                token.endsWith("lining")    || // e.g. lining -> line
                token.endsWith("gining")    || // e.g. imagining -> imagine
                token.endsWith("hining") || // e.g. shining -> shine
                token.endsWith("pining")    || // e.g. opining -> opine
                token.endsWith("fining")    || // e.g. defining -> define
                token.endsWith("mining")    || // e.g. examining -> examine
                token.endsWith("twining")   || // e.g. intertwining -> intertwine
                token.endsWith("vining") || // e.g. divining -> diving
                token.endsWith("vening")    || // e.g. intervening -> intervene
                token.endsWith("boning") || // e.g. boning -> bones
                token.endsWith("doning")    || // e.g. condoning -> condone
                token.endsWith("honing")    || // e.g. phoning -> phone
                token.endsWith("loning") || // e.g. cloning -> clone
                token.endsWith("poning") || // e.g. postponing -> postpone
                token.endsWith("roning")    || // e.g. dethroning -> dethrone
                token.endsWith("toning") || // e.g. intoning -> intone
                token.endsWith("zoning")    || // e.g. zoning -> zone

                token.endsWith("iping")     || // e.g. wiping -> wipe
                token.endsWith("aping") || // e.g. taping -> tape
                token.endsWith("coping") || // e.g. scoping -> scope
                token.endsWith("doping") || // e.g. doping -> dope
                token.endsWith("roping") || // e.g. groping -> grope
                token.equals("eloping") || // e.g. eloping -> elope
                token.endsWith("yping")     || // e.g. genotyping -> genotype
                token.endsWith("caping")    || // e.g. escaping -> escape
                token.endsWith("haping") || // e.g. reshaping -> reshapes

                token.endsWith("tiring") || // e.g. retiring -> retire
                token.endsWith("uiring") || // e.g. enquiring -> enquire
                token.endsWith("faring") || // e.g. seafaring -> seafare
                token.endsWith("paring") || // e.g. comparing -> compare
                token.endsWith("fering") || // e.g. interfering -> interfere
                token.endsWith("rsevering") || // e.g. persevering -> persevere
                token.endsWith("firing") || // e.g. misfiring -> misfire
                token.endsWith("curing") || // e.g. securing -> secure
                token.endsWith("juring") || // e.g. injuring -> injure
                token.endsWith("suring") || // e.g. reinsuring -> reinsure
                token.endsWith("turing") || // e.g. manufacturing -> manufacture
                token.endsWith("ntring") || // e.g. centring -> centre

                token.endsWith("rsing") || // e.g. parsing -> parse
                token.endsWith("basing") || // e.g. rebasing -> rebase
                token.endsWith("casing") || // e.g. truecasing -> truecase
                token.endsWith("chasing") || // e.g. purchasing -> purchase
                token.endsWith("phrasing") || // e.g. paraphrasing -> paraphrase
                token.endsWith("ising") || // e.g. reorganising -> reorganise
                token.endsWith("eansing") || // e.g. cleansing -> cleanse
                token.endsWith("ensing") || // e.g. condensing -> condense
                token.endsWith("earsing") || // e.g. rehearsing -> rehearse
                token.endsWith("easing") || // e.g. releasing -> release
                token.endsWith("ersing") || // e.g. traversing -> traverse
                token.endsWith("ursing") || // e.g. coursing -> course
                token.endsWith("ulsing") || // e.g. pulsing -> pulse
                token.endsWith("oosing") || // e.g. choosing -> choose
                token.endsWith("orsing") || // e.g. endorsing -> endorse
                token.endsWith("using") || // e.g. reusing -> reuse
                token.endsWith("posing") || // e.g. supposing -> suppose
                token.endsWith("osing") || // e.g. closing -> close
                token.endsWith("psing") || // e.g. collapsing -> collapse
                token.endsWith("ysing") || // e.g. catalysing -> catalyse

                token.endsWith("cating") || // e.g. reciprocating -> reciprocate
                token.endsWith("dating") || // e.g. predating -> predate
                token.endsWith("creating") || // e.g. creating -> create
                token.endsWith("aseating") || // e.g. caseating -> caseate
                token.endsWith("iating") || // e.g. obviating -> obviate
                token.endsWith("gating") || // e.g. segregating -> segregate
                token.endsWith("kating") || // e.g. skating -> skate
                token.endsWith("lating") || // e.g. relating -> relate
                token.endsWith("ulating") || // e.g. articulating -> articulate
                token.endsWith("nating") || // e.g. designating -> designate
                token.endsWith("rating") || // e.g. rating -> rate
                token.endsWith("erating") || // e.g. operating -> operate
                token.endsWith("sating") || // e.g. compensating -> compensate
                token.endsWith("ctating") || // e.g. nictating -> nictate
                token.endsWith("itating") || // e.g. facilitating -> facilitate
                token.endsWith("otating") || // e.g. rotating -> rotate
                token.endsWith("uating") || // e.g. evaluating -> evaluate
                token.endsWith("vating") || // e.g. deactivating -> deactivate
                token.endsWith("leting") || // e.g. completing -> complete
                token.endsWith("peting") || // e.g. competing -> compete
                token.endsWith("nciting") || // e.g. inciting -> incite
                token.endsWith("xciting") || // e.g. exciting -> excite
                token.endsWith("niting") || // e.g. uniting -> unite
                token.endsWith("writing") || // e.g. writing -> write
                token.endsWith("moting") || // e.g. promoting -> promote
                token.endsWith("noting") || // e.g. denoting -> denote
                token.endsWith("buting") || // e.g. attributing -> attribute
                token.endsWith("iluting") || // e.g. diluting -> dilute
                token.endsWith("tuting") || // e.g. substituting -> substitute
                token.endsWith("wasting") || // e.g. wasting -> waste
                token.endsWith("uting") || // e.g. diluting -> dilute

                token.endsWith("buing") || // e.g. imbuing -> imbue
                token.endsWith("cuing")     || // e.g. rescuing -> rescue
                token.endsWith("duing")     || // e.g. subduing -> subdue
                token.endsWith("euing") || // e.g. queueing -> queue
                token.endsWith("guing")     || // e.g. arguing -> argue
                token.endsWith("aluing")    || // e.g. valuing -> value
                token.endsWith("inuing")    || // e.g. continuing -> continue
                token.endsWith("quing")     || // e.g. critiquing -> critique
                token.endsWith("suing") || // e.g. ensuing -> ensue

                token.endsWith("aving") || // e.g. saving -> save
                token.endsWith("ieving") || // e.g. relieving -> relieve
                token.endsWith("arving") || // e.g. carving -> carve
                token.endsWith("erving") || // e.g. serving -> serve
                token.endsWith("lving") || // e.g. halving -> halve
                token.endsWith("iving") || // e.g. receiving -> receive
                token.endsWith("oving") || // e.g. approving -> approve, moving -> move

                token.endsWith("owsing") || // e.g. browsing -> browse
                token.equalsIgnoreCase("owing") || // e.g. owing -> owe

                token.endsWith("azing") || // e.g. gazing -> gaze
                token.endsWith("izing") || // e.g. dualizing -> dualize
                token.endsWith("yzing"); // analyzing -> analyze
        // token.endsWith("Xing"); //|| // e.g. Xing -> X
        // token.endsWith("Xing"); //|| // e.g. Xing -> X
        // token.endsWith("Xing"); //|| // e.g. Xing -> X
        // token.endsWith("Xing"); //|| // e.g. Xing -> X
        // token.endsWith("Xing"); //|| // e.g. Xing -> X
        // token.endsWith("Xing"); //|| // e.g. Xing -> X
        // token.endsWith("Xing"); //|| // e.g. Xing -> X
    }

    protected boolean presPToInfConvertCkToC(String token) {
        token = token.toLowerCase();
        return token.endsWith("panicking") ||
                token.endsWith("mimicking") ||
                token.endsWith("picnicking");
    }

    public Word presPToInf(Word word) {
        final String tag = tagSet.VERB_NON_THIRD_SING;
        final String token = word.getToken();

        if (token.equalsIgnoreCase("being")) {
            return buildSubstitution("be", tag, token);
        } else if (presPToInfConvertCkToC(token)) {
            return buildSubstitution(token.substring(0, token.length() - 4), tag, token);
        } else if (token.equalsIgnoreCase("lying")) {
            return buildSubstitution(token.substring(0, token.length() - 4) + "ie", tag, token);
        } else if (infEndsWithE(token)) {
            //System.out.println(token + " matches[1] infEndsWithE");
            return buildSubstitution(token.substring(0, token.length() - 3) + "e", tag, token);
        } else if (CONSONANT_CONSONANT_ING.matcher(token).matches()) {
            //System.out.println(token + " matches[2] + " + CONSONANT_CONSONANT_ING);
            if (token.endsWith("ssing") || token.endsWith("uzzing") || token.endsWith("spelling") ||
                    token.endsWith("stalling") || token.endsWith("selling") || token.endsWith("welling") ||
                    token.endsWith("cotting") || token.endsWith("affing") || token.endsWith("uffing") ||
                    token.endsWith("yelling") || token.equalsIgnoreCase("rolling")) {
                return buildSubstitution(token.substring(0, token.length() - 3), tag, token);
            } else {
                return buildSubstitution(token.substring(0, token.length() - 4), tag, token);
            }
        } else if (token.endsWith("xying")) {
            return buildSubstitution(token.substring(0, token.length() - 4) + "i", tag, token);
        } else if (token.endsWith("ing")) {
            return buildSubstitution(token.substring(0, token.length() - 3), tag, token);
        } else {
            return null;
        }
    }

    protected boolean removeD(String token) {
        token = token.toLowerCase();

        return token.endsWith("ibed") || // e.g. enticed -> entice

                token.endsWith("aced") || // e.g. faced -> face
                token.endsWith("iced") || // e.g. sacrificed -> sacrifice
                //token.endsWith("oiced") || // e.g. voiced -> voice, rejoiced -> rejoice
                //token.endsWith("ticed") || // e.g. enticed -> entice
                //token.endsWith("viced") || // e.g. serviced -> service
                token.endsWith("nced") || // e.g. licenced -> licence
                //token.endsWith("enced") || // e.g. licenced -> licence
                //token.endsWith("fenced") || // e.g. fenced -> fence
                //token.endsWith("tenced") || // e.g. sentenced -> sentence
                //token.endsWith("vanced") || // e.g. advanced -> advance
                //token.endsWith("vinced") || // e.g. evinced -> evince
                //token.endsWith("ounced") || // e.g. pronounced -> pronounce
                token.endsWith("rced") || // e.g. sourced -> source
                token.endsWith("uced") || // e.g. introduced -> introduce

                token.endsWith("caded") || // e.g. barricaded -> barricade
                token.endsWith("ceded") || // e.g. conceded -> concede
                token.endsWith("raded") || // e.g. paraded -> parade
                token.endsWith("jaded") || // e.g. jaded -> jade
                token.endsWith("uaded") || // e.g. persuaded -> persuade
                token.endsWith("vaded") || // e.g. invaded -> invade
                token.endsWith("acceded") || // e.g. acceeded -> accede
                token.endsWith("peded") || // e.g. impeded -> impede
                token.endsWith("seded") || // e.g. superseded > supersede
                token.endsWith("llided") || // e.g. collided -> collide
                token.endsWith("cided") || // e.g. coincided
                token.endsWith("fided") || // e.g. confided
                token.endsWith("hided") || // e.g. chided -> chide
                token.endsWith("rided") || // e.g. debrided, prided
                token.endsWith("bsided") || // e.g. subsided -> subside
                token.endsWith("esided") || // e.g. presided -> preside
                token.endsWith("uided") || // e.g. guided -> guide
                token.endsWith("vided") || // e.g. provided -> provide
                token.endsWith("graded") || // e.g. down-graded -> down-grade
                token.equals("ceded") ||
                token.endsWith("eceded") || // e.g. preceded -> precede
                token.endsWith("coded") || // e.g. coded -> code
                token.endsWith("loded") || // e.g. exploded -> explode
                token.endsWith("roded") || // e.g. corroded -> corrode
                token.endsWith(("luded")) || // e.g. precluded -> preclude
                token.endsWith("nuded") || // e.g. denuded -> denude

                token.endsWith("emceed") || // e.g. emceed -> emcee
                token.endsWith("reed") || // e.g. freed -> free
                token.endsWith("teed") || // e.g. guaranteed -> guarantee

                token.endsWith("afed") || // e.g. chafed -> chafe

                token.equals("aged") ||
                token.endsWith("daged") || // e.g. bandaged -> bandage
                token.endsWith("riaged") || // e.g. triaged -> triage
                token.endsWith("ckaged") || // e.g. packaged -> package
                token.endsWith("gaged") || // e.g. disengaged -> disengage
                token.endsWith("laged") || // e.g. pillaged -> pillage, camoflaged -> camoflage
                token.endsWith("maged") || // e.g. damaged -> damage
                token.endsWith("naged") || // e.g. managed -> manage
                token.endsWith("paged") || // e.g. paged -> page
                token.endsWith("taged") || // e.g. staged -> stage
                token.endsWith("raged") || // e.g. averaged -> average
                token.endsWith("saged") || // e.g. envisaged -> envisage
                token.endsWith("vaged") || // e.g. salvaged -> salvage, ravaged -> ravage
                token.endsWith("yaged") || // e.g. voyaged -> voyage
                token.endsWith("dged") || // e.g. bridged -> bridge
                token.endsWith("ieged") || // e.g. besieged -> besiege
                token.endsWith("leged") || // e.g. alleged -> allege
                token.endsWith("liged") || // e.g. obliged -> oblige
                token.endsWith("lged") || // e.g. indulged -> indulge
                token.endsWith("mpinged") || // e.g. impinged -> impinge
                token.endsWith("changed") || // e.g. changed -> change
                token.endsWith("ranged") || // e.g. arranged -> arrange
                token.endsWith("lenged") || // e.g. challenged -> challenge
                token.endsWith("venged") || // e.g. avenged -> avenge
                token.endsWith("fringed") || // e.g. infringed -> infringe
                token.endsWith("unged") || // e.g. expunged -> expunge
                token.endsWith("rged") || // e.g. emerged -> emerge
                token.endsWith("auged") || // e.g. gauged -> gauge

                token.endsWith("reathed") || // e.g. breathed -> breathe
                token.endsWith("ythed") || // e.g. scythed -> scythe

                token.endsWith("faked") || // e.g. faked -> fake
                token.endsWith("raked") || // e.g. raked -> rake
                token.endsWith("taked") || // e.g. staked -> stake
                token.endsWith("iked") || // e.g. liked -> like
                token.endsWith("hoked") || // e.g. choked -> choke
                token.endsWith("moked") || // e.g. smoked -> smoke
                token.endsWith("roked") || // e.g. stroked -> stroke
                token.endsWith("toked") || // e.g. stoked -> stoke
                token.endsWith("voked") || // e.g. invoked -> invoke

                token.endsWith("caled") || // e.g. down-scales -> down-scale
                token.endsWith("haled") || // e.g. exhaled -> exhale
                token.endsWith("paled") || // e.g. impaled -> impale
                token.endsWith("saled") || // e.g. wholesaled -> wholesale
                token.endsWith("bled") || // e.g. assembled -> assemble, enabled -> enable
                token.endsWith("cled") || // e.g. bespectacled -> bespectacle
                token.endsWith("dled") || // e.g. puddled -> puddle
                token.endsWith("fled") || // e.g. baffled -> baffle
                token.endsWith("ggled") || // e.g. struggled -> struggle
                token.endsWith("ngled") || // e.g. mingled -> mingle
                token.endsWith("ogled") || // e.g. ogled -> ogle, googled -> google
                token.endsWith("rgled") || // e.g. burgled -> burgle
                token.endsWith("ciled") || // e.g. reconciled -> reconcile
                token.endsWith("filed") || // e.g. profiled -> profile
                token.endsWith("miled") || // e.g. smiled -> smile
                token.endsWith("piled") || // e.g. piled -> pile
                token.endsWith("xiled") || // e.g. exiled -> exile
                token.endsWith("kled") || // e.g. sprinkled -> sprinkle
                token.endsWith("joled") || // e.g. cajoled -> cajole
                token.endsWith("aroled") || // e.g. paroled -> parole
                token.endsWith("pled") || // e.g. coupled -> couple
                token.endsWith("tled") || // e.g. titled -> title
                token.endsWith("culed") || // e.g. ridiculed -> ridicule
                token.endsWith("duled") || // e.g. scheduled -> schedule
                token.endsWith("ruled") || // e.g. ruled -> rule
                token.endsWith("yled") || // e.g. styled -> style
                token.endsWith("zled") || // e.g. puzzled -> puzzle

                token.endsWith("famed") || // e.g. famed -> fame
                token.endsWith("hamed") || // e.g. shamed -> shame
                token.endsWith("lamed") || // e.g. flamed -> flame
                token.endsWith("named") || // e.g. codenamed -> codename
                token.endsWith("ramed") || // e.g. framed -> frame
                token.endsWith("hemed") || // e.g. themed -> theme, blasphemed -> blaspheme
                token.equals("mimed") ||
                token.endsWith("rimed") || // e.g. rimed -> rime
                token.endsWith("timed") || // e.g. timed -> time
                token.endsWith("comed") || // e.g. welcomed -> welcome
                token.endsWith("umed") || // e.g. consumed -> consume
                token.endsWith("rhymed") || // e.g. rhymed -> rhyme

                token.endsWith("paned") || // e.g. paned -> pane, waned -> wane
                token.endsWith("waned") || // e.g. paned -> pane, waned -> wane
                token.endsWith("vened") || // e.g. contravened -> contravene
                token.endsWith("bined") || // e.g. combined -> combine
                token.endsWith("fined") || // e.g. defined -> define
                token.endsWith("lined") || // e.g. lined -> line
                token.endsWith("mined") || // e.g. examined -> examine
                token.endsWith("pined") || // e.g. opined -> opine
                token.endsWith("rined") || // e.g. enshrined -> enshrine
                token.endsWith("tined") || // e.g. quarrantined -> quarrantine
                token.endsWith("wined") || // e.g. intertwined -> intertwine
                token.endsWith("condoned") || // e.g. condoned -> condone
                token.endsWith("boned") || // e.g. boned -> bone
                token.endsWith("honed") || // e.g. phoned -> phone
                token.endsWith("loned") || // e.g. cloned -> clone
                token.endsWith("poned") || // e.g. postponed -> postpone
                token.endsWith("roned") || // e.g. enthroned -> enthrone
                token.endsWith("toned") || // e.g. intoned -> intone, stoned -> stone
                token.equals("zoned") || // e.g. zoned -> zone
                token.equals("rezoned") || // e.g. zoned -> zone
                token.equals("re-zoned") || // e.g. zoned -> zone
                token.endsWith("gined") || // e.g. imagined -> imagine
                token.endsWith("pruned") || // e.g. pruned -> prune
                token.endsWith("tuned") || // e.g. tuned -> tune

                token.endsWith("caped") || // e.g. escaped -> escape
                token.endsWith("haped") || // e.g. shaped -> shape
                token.endsWith("raped") || // e.g. draped -> drape
                token.endsWith("taped") || // e.g. videotaped -> videotape
                token.endsWith("wiped") || // e.g. wiped -> wipe
                token.endsWith("roped") || // e.g. roped -> rope
                token.endsWith("duped") || // e.g. duped -> dupe
                token.endsWith("yped") || // e.g. typed -> type

                token.endsWith("dared") || // e.g. dared -> dare
                token.endsWith("hared") || // e.g. shared -> share
                token.endsWith("clared") || // e.g. declared -> declare
                token.endsWith("flared") || // e.g. flared -> flare
                token.endsWith("nared") || // e.g. ensnared -> ensnare
                token.endsWith("pared") || // e.g. compare -> compared
                token.endsWith("uared") || // e.g. squared -> square
                token.endsWith("cred") || // e.g. massacred -> massacre
                token.endsWith("dhered") || // e.g. adhered -> adhere
                token.endsWith("rfered") || // e.g. interfered -> interfere
                token.endsWith("bored") || // e.g. bored -> bore
                token.endsWith("dored") || // e.g. adored -> adore
                token.endsWith("plored") || // e.g. explored -> explore
                token.endsWith("gnored") || // e.g. ignored -> ignore
                token.endsWith("stored") || // e.g. restored -> restore
                token.endsWith("hired") || // e.g. hired -> hire
                token.endsWith("mired") || // e.g. mired -> mire
                token.endsWith("pired") || // e.g. umpired -> umpire
                token.endsWith("sired") || // e.g. sired -> sire, desired -> desire
                token.endsWith("tired") || // e.g. retired -> retire
                token.endsWith("uired") || // e.g. enquired -> enquire
                token.endsWith("wired") || // e.g. rewired -> rewire
                token.endsWith("tred") || // e.g. centred -> centre
                token.endsWith("cured") || // e.g. cured -> cure
                token.endsWith("dured") || // e.g. endured -> endure
                token.endsWith("gured") || // e.g. reconfigured -> reconfigure
                token.endsWith("jured") || // e.g. injured -> injure
                token.equals("lured") || // e.g. lured -> lure
                token.endsWith("nured") || // e.g. tenured -> tenure
                token.endsWith("sured") || // e.g. assured -> assure
                token.endsWith("tured") || // e.g. ventured -> venture

                token.endsWith("based") || // e.g. rebased -> rebase
                token.endsWith("cased") || // e.g. showcased -> showcase
                token.equals("eased") || // e.g. eased -> ease
                token.endsWith("ceased") || // e.g. deceased -> decease
                token.endsWith("leased") || // e.g. released -> release
                token.endsWith("reased") || // e.g. creased -> crease
                token.endsWith("hased") || // e.g. purchased -> purchase
                token.endsWith("iased") ||  // e.g. biased -> bias
                token.endsWith("rased") || // e.g. erased -> erase
                token.endsWith("ised") || // e.g. criminalised -> criminalise
                token.endsWith("nsed") || // e.g. licensed -> license
                token.endsWith("cored") || // e.g. scored -> score
                token.endsWith("ulsed") || // e.g. pulsed -> pulse
                token.endsWith("osed") || // e.g. opposed -> oppose
                token.endsWith("psed") || // e.g. lapsed -> lapse
                token.endsWith("rsed") || // e.g. accursed -> accurse, interspersed -> intersperse
                token.endsWith("essed") || // e.g. reprocessed -> reprocess
                token.endsWith("ncussed") || // e.g. concussed -> concuss
                token.equals("used") || // e.g. used -> use
                token.equals("reused") ||
                token.equals("misused") ||
                token.endsWith("-used") || // e.g. re-used
                token.endsWith("aused") || // e.g. caused -> cause
                token.endsWith("bused") || // e.g. abused -> abuse
                token.endsWith("fused") || // e.g. confused -> confuse
                token.endsWith("hused") || // e.g. enthused -> enthuse
                token.endsWith("mused") || // e.g. amused -> amuse
                token.endsWith("oused") || // e.g. aroused -> arouse, espoused -> espouse
                token.endsWith("ysed") || // e.g. catalysed -> catalyse

                token.endsWith("rrotted") || // e.g. garrotted -> garrottes
                token.equals("garrotted") || // e.g. garotted
                token.endsWith("bated") || // e.g. debated -> debate
                token.endsWith("cated") || // e.g. located -> locate
                token.endsWith("dated") || // e.g. consolidated -> consolidate
                token.endsWith("neated") || // e.g. lineated -> lineate
                token.endsWith("lated") || // e.g. dilated -> dilate
                token.endsWith("kated") || // e.g. skated -> skate
                token.endsWith("created") || // e.g. created -> create
                token.endsWith("gated") || // e.g. relegated -> relegate
                token.endsWith("iated") || // e.g. affiliated -> affiliate
                token.endsWith("ulated") || // e.g. regulated -> regulate
                token.endsWith("ylated") || // e.g. methylated -> methylate
                token.endsWith("nated") || // e.g. hyphenated -> hyphenate
                token.endsWith("mated") || // e.g. decimated -> decimate, desquamated -> desquamate
                token.endsWith("pated") || // e.g. dissipated -> disspate
                token.endsWith("rated") || // e.g. frustrated -> frustrate
                token.endsWith("sated") || // e.g. compensated -> compensate
                token.endsWith("tated") || // e.g. reinstated -> reinstate
                token.endsWith("uated") || // e.g. situated -> situate
                token.endsWith("vated") || // e.g. activated -> activate
                token.endsWith("eleted") || // e.g. deleted -> delete
                token.endsWith("oleted") || // e.g. obsoleted -> obsolete
                token.endsWith("pleted") || // e.g. completed -> complete
                token.equals("meted") || // e.g. meted -> mete
                token.endsWith("peted") || // e.g. competed -> compete
                token.equals("cited") || // e.g. cited -> cite
                token.endsWith("ecited") || // e.g. recited -> recite
                token.endsWith("ncited") || // e.g. incited -> incite
                token.endsWith("xcited") || // e.g. excited -> excite
                token.endsWith("adited") || // e.g. extradited -> extradite
                token.endsWith("nited") || // e.g. united -> unite
                token.endsWith("vited") || // e.g. disinvited -> disinvite
                token.endsWith("moted") || // e.g. promoted -> promote
                token.endsWith("noted") || // e.g. denoted -> denote
                token.endsWith("uoted") || // e.g. quoted -> quote
                token.endsWith("voted") || // e.g. voted -> vote
                token.endsWith("tasted") || // e.g. tasted -> taste
                token.endsWith("wasted") || // e.g. wasted -> waste
                token.endsWith("uetted") || // e.g. silhouetted -> silhouette
                token.endsWith("zetted") || // e.g. gazetted -> gazette
                token.endsWith("ibuted") || // e.g. misattributed -> misattribute
                token.endsWith("cuted") || // e.g. electrocuted -> electrocute, persecuted -> persecute
                token.endsWith("futed") || // e.g. refuted -> refute
                token.endsWith("luted") || // e.g. diluted -> dilute
                token.equals("routed") || // e.g. routed -> route
                token.equals("rerouted") || // e.g. routed -> route
                token.endsWith("-routed") || // e.g. routed -> route
                token.endsWith("puted") || // e.g. computed -> compute
                token.endsWith("tuted") || // e.g. constituted -> contitute
                token.endsWith("muted") || // e.g. commuted -> commute
                token.endsWith("sputed") || // e.g. disputed -> dispute

                token.endsWith("bued") || // e.g. imbued -> imbue
                token.endsWith("cued") || // e.g. rescued -> rescue
                token.endsWith("dued") || // e.g. subdued -> subdue
                token.endsWith("ueued") || // e.g. queued -> queue
                token.endsWith("gued") || // e.g. prologued -> prologue
                token.endsWith("lued") || // e.g. glued -> glue
                token.endsWith("nued") || // e.g. discontinued -> discontinue
                token.endsWith("qued") || // e.g. piqued -> pique
                token.endsWith("crued") || // e.g. accrued -> accrue
                token.endsWith("strued") || // e.g. construed -> construe
                token.endsWith("sued") || // e.g. sued -> sue, issued -> issue

                token.endsWith("aved") || // e.g. saved -> save
                token.endsWith("eved") || // e.g. relieved -> relieve
                token.endsWith("ived") || // e.g. outlived -> outlive
                token.endsWith("lved") || // e.g. revolved -> revolve
                token.endsWith("oved") || // e.g. loved -> love
                token.endsWith("rved") || // e.g. reserved -> reserve

                token.equals("owed") || // e.g. owed -> owe

                token.endsWith("xed") || // e.g. axed -> axe

                token.endsWith("dyed") || // e.g. dyed -> dye

                token.endsWith("dazed") || // e.g. dazed -> daze
                token.endsWith("lazed") || // e.g. glazed -> glaze
                token.endsWith("mazed") || // e.g. amazed -> amaze
                token.endsWith("razed") || // e.g. crazed -> craze
                token.endsWith("eezed") || // e.g. squeezed -> squeeze
                token.endsWith("ized") || // e.g. criminalized -> criminalize
                token.endsWith("ozed") || // e.g. dozed -> doze
                token.endsWith("tzed") || // e.g. waltzed -> waltz
                token.endsWith("yzed"); // || // e.g. criminalized -> criminalize
    }

    protected boolean removeEDAndConsonant(String token) {
        return CONSONANT_CONSONANT_ED.matcher(token).matches() &&
                !token.toLowerCase().endsWith("balled") && // e.g. blackballed
                !token.toLowerCase().endsWith("called") && // e.g. called
                !token.toLowerCase().endsWith("palled") && // e.g. appalled
                !token.toLowerCase().endsWith("ralled") && // e.g. enthralled
                !token.toLowerCase().endsWith("talled") && // e.g. installed
                !token.toLowerCase().endsWith("walled") && // e.g. walled
                !token.toLowerCase().endsWith("felled") && // e.g. felled
                !token.toLowerCase().endsWith("helled") && // e.g. shelled
                !token.toLowerCase().endsWith("melled") && // e.g. smelled
                !token.toLowerCase().endsWith("spelled") && // e.g. spelled
                !token.toLowerCase().endsWith("swelled") && // e.g. spelled
                !token.toLowerCase().endsWith("quelled") && // e.g. quelled
                !token.toLowerCase().endsWith("yelled") && // e.g. yelled
                !token.toLowerCase().endsWith("billed") && // e.g. billed
                !token.toLowerCase().endsWith("filled") && // e.g. filled
                !token.toLowerCase().endsWith("chilled") && // e.g. chilled
                !token.toLowerCase().endsWith("drilled") && // e.g. drilled
                !token.toLowerCase().endsWith("killed") && // e.g. killed
                !token.toLowerCase().endsWith("milled") && // e.g. milled
                !token.toLowerCase().endsWith("stilled") && // e.g. stilled
                !token.toLowerCase().endsWith("spilled") && // e.g. stilled
                !token.toLowerCase().endsWith("tilled") && // e.g. tilled
                !token.toLowerCase().endsWith("thrilled") && // e.g. thrilled
                !token.toLowerCase().endsWith("willed") && // e.g. willed
                !token.toLowerCase().endsWith("polled") && // e.g. polled
                !token.toLowerCase().equals("rolled") && // e.g. rolled
                !token.toLowerCase().endsWith("enrolled") && // e.g. enrolled
                !token.toLowerCase().equals("tolled") && // e.g. rolled
                !token.toLowerCase().equals("trolled") && // e.g. trolled
                !token.toLowerCase().endsWith("ffed") && // e.g. buffed
                !token.toLowerCase().endsWith("culled") && // e.g. culled
                !token.toLowerCase().endsWith("dulled") && // e.g. dulled
                !token.toLowerCase().endsWith("fulled") && // e.g. fulled
                !token.toLowerCase().endsWith("pulled") && // e.g. pulled
                !token.toLowerCase().endsWith("ossed") && // e.g. crossed
                !token.toLowerCase().endsWith("assed") && // e.g. passed, bypassed
                !token.toLowerCase().endsWith("cotted") && // e.g. boycotted
                !token.toLowerCase().endsWith("uzzed"); // e.g. buzzed
    }

    protected boolean replaceIEDWithY(String token) {
        return token.endsWith("ied");
    }

    protected boolean pastPToBaseNoChange(String token) {
        token = token.toLowerCase();
        return token.endsWith("cast") ||
                token.endsWith("spread") ||
                token.endsWith("become") ||
                token.endsWith("overcome") ||
                token.equals("set") ||
                token.equals("upset") ||
                token.equals("wed") ||
                token.equals("shed") ||
                token.equals("split") ||
                token.equals("rerun") ||
                token.equals("fit") ||
                token.equals("clad") ||
                token.equals("ironclad") ||
                token.equals("read") ||
                token.equals("misread") ||
                token.equals("offset") ||
                token.equals("hit") ||
                token.equals("quit") ||
                token.equals("bet") ||
                token.equals("bid") ||
                token.equals("rebid") ||
                token.equals("beset") ||
                token.equals("thrust") ||
                token.equals("inset") ||
                token.equals("beat") ||
                token.equals("overrun") ||
                token.equals("hurt") ||
                token.equals("knit") ||
                token.equals("shut");
    }

    protected boolean containsPunctuation(String token) {
        for (int i = 0; i < PUNCTUATION.length(); i++) {
            if (token.contains(PUNCTUATION.substring(i, i + 1))) {
                return true;
            }
        }
        return false;
    }

    protected boolean isBlackListedPastPToBase(String token) {
        token = token.toLowerCase();
        return token.equals("opinionated") ||
                token.endsWith("wrought") ||
                token.equals("sled") ||
                token.equals("bore") || // bore is past tense, not past participle
                token.equals("bed") ||
                token.equals("coalbed") ||
                token.equals("deathbed") ||
                token.equals("trackbed") ||
                token.equals("need") ||
                token.equals("seabed") ||
                token.equals("testbed") ||
                token.equals("riverbed") ||
                token.equals("linseed") ||
                token.equals("sinced") ||
                token.equals("infrared") ||
                containsPunctuation(token);
    }

    public Word pastPToBase(Word word) {
        final String tag = tagSet.VERB_BASE;
        final String token = word.getToken();

        if (isBlackListedPastPToBase(token)) {
            return null;
        } else if (pastPToBaseNoChange(token)) {
            return buildSubstitution(token, tag, token);
        } else if (token.equalsIgnoreCase("been")) {
            return buildSubstitution("be", tag, token);
        } else if (token.equalsIgnoreCase("had")) {
            return buildSubstitution("have", tag, token);
        } else if (token.equalsIgnoreCase("used")) {
            return buildSubstitution("use", tag, token);
        } else if (token.endsWith("done")) {
            return buildSubstitution(token.substring(0, token.length() - 2), tag, token);
        } else if (token.endsWith("gone")) {
            return buildSubstitution(token.substring(0, token.length() - 2), tag, token);
        } else if (token.equalsIgnoreCase("taken")) {
            return buildSubstitution("take", tag, token);
        } else if (token.equalsIgnoreCase("left")) {
            return buildSubstitution("leave", tag, token);
        } else if (token.toLowerCase().equals("got") || token.toLowerCase().endsWith("forgot")) {
            return buildSubstitution(token.substring(0, token.length() - 3) + "get", tag, token);
        } else if (token.toLowerCase().endsWith("gotten")) {
            // e.g. gotten -> get, forgotten -> forget
            return buildSubstitution(token.substring(0, token.length() - 5) + "et", tag, token);
        } else if (token.toLowerCase().endsWith("told") || token.toLowerCase().endsWith("sold")) {
            return buildSubstitution(token.substring(0, token.length() - 3) + "ell", tag, token);
        } else if (token.equalsIgnoreCase("took")) {
            return buildSubstitution("take", tag, token);
        } else if (token.endsWith("sat")) {
            return buildSubstitution(token.substring(0, token.length() - 2) + "it", tag, token);
        } else if (token.endsWith("saw")) {
            return buildSubstitution(token.substring(0, token.length() - 2) + "ee", tag, token);
        } else if (token.toLowerCase().endsWith("sewn") || token.toLowerCase().endsWith("hewn") ||
                token.toLowerCase().endsWith("strewn")) {
            return buildSubstitution(token.substring(0, token.length() - 1), tag, token);
        } else if (token.equalsIgnoreCase("fed") || token.equalsIgnoreCase("overfed") ||
                token.equalsIgnoreCase("bred") || token.equalsIgnoreCase("overbred") ||
                token.equalsIgnoreCase("sped")) {
            return buildSubstitution(token.substring(0, token.length() - 1) + "ed", tag, token);
        } else if (token.endsWith("torn")) {
            return buildSubstitution("tear", tag, token);
        } else if (token.equalsIgnoreCase("led") || token.equalsIgnoreCase("misled") || token.equalsIgnoreCase("co-led")) {
            return buildSubstitution(token.substring(0, token.length() - 1) + "ad", tag, token);
        } else if (token.endsWith("lit")) {
            return buildSubstitution(token.substring(0, token.length() - 2) + "ight", tag, token);
        } else if (token.endsWith("paid")) {
            return buildSubstitution(token.substring(0, token.length() - 2) + "y", tag, token);
        } else if (token.toLowerCase().endsWith("felt")) {
            return buildSubstitution(token.substring(0, token.length() - 3) + "eel", tag, token);
        } else if (token.toLowerCase().endsWith("dealt")) {
            return buildSubstitution(token.substring(0, token.length() - 4) + "eal", tag, token);
        } else if (token.toLowerCase().endsWith("built")) {
            return buildSubstitution(token.substring(0, token.length() - 1) + "d", tag, token);
        } else if (token.toLowerCase().endsWith("spelt") || token.toLowerCase().endsWith("spilt")) {
            return buildSubstitution(token.substring(0, token.length() - 1) + "l", tag, token);
        } else if (token.toLowerCase().endsWith("held")) {
            return buildSubstitution(token.substring(0, token.length() - 3) + "old", tag, token);
        } else if (token.toLowerCase().endsWith("rose")) {
            return buildSubstitution(token.substring(0, token.length() - 3) + "ise", tag, token);
        } else if (token.toLowerCase().endsWith("shrunk")) {
            return buildSubstitution(token.substring(0, token.length() - 3) + "ink", tag, token);
        } else if (token.toLowerCase().endsWith("stood")) {
            return buildSubstitution(token.substring(0, token.length() - 3) + "and", tag, token);
        } else if (token.toLowerCase().endsWith("slid")) {
            return buildSubstitution(token + "e", tag, token);
        } else if (token.toLowerCase().endsWith("broken")) {
            return buildSubstitution(token.substring(0, token.length() - 4) + "eak", tag, token);
        } else if (token.toLowerCase().endsWith("frozen")) {
            return buildSubstitution(token.substring(0, token.length() - 4) + "eeze", tag, token);
        } else if (token.toLowerCase().endsWith("chosen")) {
            return buildSubstitution(token.substring(0, token.length() - 4) + "oose", tag, token);
        } else if (token.toLowerCase().endsWith("woven")) {
            return buildSubstitution(token.substring(0, token.length() - 4) + "eave", tag, token);
        } else if (token.toLowerCase().endsWith("hidden")) {
            return buildSubstitution(token.substring(0, token.length() - 3) + "e", tag, token);
        } else if (token.toLowerCase().endsWith("bidden")) {
            // e.g. forbidden, bidden
            return buildSubstitution(token.substring(0, token.length() - 3), tag, token);
        } else if (token.toLowerCase().endsWith("ridden")) {
            return buildSubstitution(token.substring(0, token.length() - 3) + "e", tag, token);
        } else if (token.equalsIgnoreCase("risen") || token.equalsIgnoreCase("arisen")) {
            return buildSubstitution(token.substring(0, token.length() - 1), tag, token);
        } else if (token.toLowerCase().endsWith("given") || token.toLowerCase().endsWith("riven")) {
            return buildSubstitution(token.substring(0, token.length() - 1), tag, token);
        } else if (token.toLowerCase().endsWith("written") || token.toLowerCase().endsWith("bitten") ||
                token.toLowerCase().endsWith("smitten")) {
            return buildSubstitution(token.substring(0, token.length() - 3) + "e", tag, token);
        } else if (token.toLowerCase().endsWith("beaten")) {
            return buildSubstitution(token.substring(0, token.length() - 2), tag, token);
        } else if (token.toLowerCase().endsWith("shot")) {
            return buildSubstitution(token.substring(0, token.length() - 2) + "oot", tag, token);
        } else if (token.toLowerCase().endsWith("thought")) {
            return buildSubstitution(token.substring(0, token.length() - 5) + "ink", tag, token);
        } else if (token.toLowerCase().endsWith("fought")) {
            return buildSubstitution(token.substring(0, token.length() - 5) + "ight", tag, token);
        } else if (token.toLowerCase().endsWith("taught")) {
            return buildSubstitution(token.substring(0, token.length() - 5) + "each", tag, token);
        } else if (token.toLowerCase().endsWith("caught")) {
            return buildSubstitution(token.substring(0, token.length() - 5) + "atch", tag, token);
        } else if (token.toLowerCase().endsWith("flung") || token.toLowerCase().endsWith("stung")) {
            return buildSubstitution(token.substring(0, token.length() - 3) + "ing", tag, token);
        } else if (token.toLowerCase().endsWith("sent") || token.toLowerCase().endsWith("lent") ||
                token.toLowerCase().endsWith("spent")) {
            return buildSubstitution(token.substring(0, token.length() - 3) + "end", tag, token);
        } else if (token.toLowerCase().endsWith("panicked") || token.toLowerCase().endsWith("mimicked") ||
                token.toLowerCase().endsWith("afficked")) {
            return buildSubstitution(token.substring(0, token.length() - 3), tag, token);
        } else if (token.toLowerCase().endsWith("lain") || token.toLowerCase().endsWith("laid") || token.toLowerCase().endsWith("said")) {
            return buildSubstitution(token.substring(0, token.length() - 3) + "ay", tag, token);
        } else if (token.toLowerCase().endsWith("rung")) {
            return buildSubstitution(token.substring(0, token.length() - 3) + "ing", tag, token);
        } else if (token.toLowerCase().endsWith("sang") || token.toLowerCase().endsWith("sung")) {
            return buildSubstitution(token.substring(0, token.length() - 3) + "ing", tag, token);
        } else if (token.toLowerCase().endsWith("hung")) {
            return buildSubstitution(token.substring(0, token.length() - 3) + "ang", tag, token);
        } else if (token.toLowerCase().endsWith("sunk")) {
            return buildSubstitution(token.substring(0, token.length() - 3) + "ink", tag, token);
        } else if (token.toLowerCase().endsWith("borne")) {
            return buildSubstitution(token.substring(0, token.length() - 4) + "ear", tag, token);
        } else if (token.toLowerCase().endsWith("drawn") || token.endsWith("grown") ||
                token.toLowerCase().endsWith("seen") ||
                token.toLowerCase().endsWith("thrown") || token.toLowerCase().endsWith("blown")) {
            return buildSubstitution(token.substring(0, token.length() - 1), tag, token);
        } else if (token.toLowerCase().endsWith("flown")) {
            return buildSubstitution(token.substring(0, token.length() - 3) + "y", tag, token);
        } else if (token.toLowerCase().endsWith("sworn")) {
            return buildSubstitution(token.substring(0, token.length() - 3) + "ear", tag, token);
        } else if (token.toLowerCase().endsWith("heard")) {
            return buildSubstitution(token.substring(0, token.length() - 4) + "ear", tag, token);
        } else if (token.toLowerCase().endsWith("lost")) {
            return buildSubstitution(token.substring(0, token.length() - 1) + "e", tag, token);
        } else if (token.toLowerCase().endsWith("slept") || token.toLowerCase().endsWith("swept") ||
                token.toLowerCase().endsWith("kept")) {
            return buildSubstitution(token.substring(0, token.length() - 2) + "ep", tag, token);
        } else if (token.toLowerCase().endsWith("bound") || token.equalsIgnoreCase("wound")) {
            return buildSubstitution(token.substring(0, token.length() - 4) + "ind", tag, token);
        } else if (token.toLowerCase().endsWith("stuck")) {
            return buildSubstitution(token.substring(0, token.length() - 4) + "tick", tag, token);
        } else if (token.toLowerCase().endsWith("struck")) {
            return buildSubstitution(token.substring(0, token.length() - 3) + "ike", tag, token);
        } else if (token.toLowerCase().endsWith("stricken")) {
            return buildSubstitution(token.substring(0, token.length() - 5) + "ike", tag, token);
        } else if (token.toLowerCase().endsWith("spun")) {
            return buildSubstitution(token.substring(0, token.length() - 4) + "spin", tag, token);
        } else if (token.equalsIgnoreCase("dug")) {
            return buildSubstitution(token.substring(0, token.length() - 2) + "ig", tag, token);
        } else if (token.toLowerCase().endsWith("woken")) {
            return buildSubstitution(token.substring(0, token.length() - 4) + "ake", tag, token);
        } else if (token.equalsIgnoreCase("fled")) {
            return buildSubstitution(token.substring(0, token.length() - 1) + "e", tag, token);
        } else if (removeD(token)) {
            return buildSubstitution(token.substring(0, token.length() - 1), tag, token);
        } else if (removeEDAndConsonant(token)) {
            //System.out.println("removing ED and constant: '" + token + "'");
            return buildSubstitution(token.substring(0, token.length() - 3), tag, token);
        } else if (replaceIEDWithY(token)) {
            return buildSubstitution(token.substring(0, token.length() - 3) + "y", tag, token);
        } else if (token.endsWith("ed")) {
            //System.out.println("reached default remove -ed rule: " + token);
            return buildSubstitution(token.substring(0, token.length() - 2), tag, token);
        } else {
            // System.out.println("Probably not a past particple " + token);
            return null;
        }
    }

    protected boolean replaceYWithIES(String token) {
        return token.endsWith("y") &&
                !token.endsWith("oy") &&
                !token.endsWith("ay") &&
                !token.endsWith("ey");
    }

    protected boolean appendSES(String token) {
        token = token.toLowerCase();
        return token.equals("bus");
    }

    protected boolean appendZES(String token) {
        token = token.toLowerCase();
        return token.equals("quiz");
    }

    public Word pastPToThirdSing(Word word) {
        final String tag = tagSet.VERB_THIRD_SING;
        final Word replacement = pastPToBase(word);

        if (replacement == null) {
            return null;
        }

        final String token = replacement.getToken();

        //System.out.println(word.getToken() + " => " + token);

        if (token.equalsIgnoreCase("be")) {
            return buildSubstitution("is", tag, token);
        } else if (token.equalsIgnoreCase("have")) {
            return buildSubstitution("has", tag, token);
        } else if (token.endsWith("do")) {
            return buildSubstitution(token + "es", tag, token);
        } else if (token.endsWith("go")) {
            return buildSubstitution(token + "es", tag, token);
        } else if (token.equalsIgnoreCase("take")) {
            return buildSubstitution("takes", tag, token);
        } else if (token.equalsIgnoreCase("leave")) {
            return buildSubstitution("leaves", tag, token);
        } else if (replaceYWithIES(token)) {
            return buildSubstitution(token.substring(0, token.length() - 1) + "ies", tag, token);
        } else if (token.endsWith("ch") || token.endsWith("sh") || token.endsWith("ss") ||
                token.endsWith("es") || token.endsWith("cus") ||
                token.endsWith("zz") || token.endsWith("to") /* e.g. veto */ ||
                token.endsWith("ucco") /* e.g. stucco */) {
            return buildSubstitution(token + "es", tag, token);
        } else if (appendSES(token)) {
            return buildSubstitution(token + "ses", tag, token);
        } else if (appendZES(token)) {
            return buildSubstitution(token + "zes", tag, token);
        } else {
            return buildSubstitution(token + "s", tag, token);
        }
        /*
        }
        else if (token.length() > 5 && ErrorUtilities.isVowel(token.charAt(word.getToken().length() - 4))) {
            return buildSubstitution(token.substring(0, token.length() - 1) + "s", tag, token);
        } else if
                (token.endsWith("ied")
                        || token.endsWith("ched")
                        || token.endsWith("sed")
                        || token.endsWith("ated")
                        || token.endsWith("lved")
                        || token.endsWith("nced")
                ) {
            return buildSubstitution(token.substring(0, token.length() - 1) + "s", tag, token);

        } else if (token.length() > 1) {
            return buildSubstitution(token.substring(0, token.length() - 2) + "s", tag, token);

        } else {
            return null;
        }
                */
    }

    public Word pastPToPresP(Word word) {
        final String tag = tagSet.VERB_PRES_PART;
        final String token = word.getToken();

        if (token.equalsIgnoreCase("been")) {
            return buildSubstitution("being", tag, token);
        } else if (token.equalsIgnoreCase("had")) {
            return buildSubstitution("having", tag, token);
        } else if (token.equalsIgnoreCase("done")) {
            return buildSubstitution("doing", tag, token);
        } else if (token.equalsIgnoreCase("gone")) {
            return buildSubstitution("going", tag, token);
        } else if (token.equalsIgnoreCase("taken")) {
            return buildSubstitution("taking", tag, token);
        } else if (token.equalsIgnoreCase("left")) {
            return buildSubstitution("leaving", tag, token);
        } else if (token.endsWith("ied")) {
            return buildSubstitution(token.substring(0, token.length() - 3) + "ying", tag, token);
        } else if (token.length() > 2) {
            return buildSubstitution(token.substring(0, token.length() - 2) + "ing", tag, token);
        } else {
            return null;
        }
    }

    // VB -> VBZ
    public Word baseToThirdSing(Word word) {
        final String tag = tagSet.VERB_THIRD_SING;
        final String token = word.getToken();

        if (token.equalsIgnoreCase("be")) {
            return buildSubstitution("is", tag, token);
        } else if (token.toLowerCase().endsWith("do") || token.toLowerCase().endsWith("go")) {
            return buildSubstitution(token + "es", tag, token);
        } else if (token.equalsIgnoreCase("have")) {
            return buildSubstitution("has", tag, token);
        } else if (token.endsWith("y")) {
            if (token.length() == 3) {
                if (!ErrorUtilities.isVowel(token.charAt(token.length() - 2))) {
                    // try -> tries
                    return buildSubstitution(token.substring(0, token.length() - 1) + "ies", tag, token);
                } else {
                    // say -> says
                    return buildSubstitution(token + "s", tag, token);
                }
            }
            if (ErrorUtilities.isVowel(token.charAt(token.length() - 2))) {
                return buildSubstitution(token + "s", tag, token);
            } else {
                return buildSubstitution(token.substring(0, token.length() - 1) + "ies", tag, token);
            }
        } else if (token.endsWith("sh") || token.endsWith("ch") || token.endsWith("ss") ||
                token.endsWith("sso") ||
                token.endsWith("xi") || token.endsWith("x")) {
            return buildSubstitution(token + "es", tag, token);
        } else {
            return buildSubstitution(token + "s", tag, token);
        }
    }

    public Word regularAdjToComparative(Word word) {
        final String tag = tagSet.ADJ_COMP;
        final String token = word.getToken();

        if (token.equalsIgnoreCase("good")) {
            return buildSubstitution("better", tag, token);
        } else if (token.endsWith("y")) {
            return buildSubstitution(token.substring(0, token.length() - 1) + "ier", tag, token);
        } else if (token.endsWith("t") && ErrorUtilities.isVowel(token.charAt(token.length() - 2))) {
            return buildSubstitution(token + "ter", tag, token);
        } else if (token.endsWith("e")) {
            return buildSubstitution(token + "r", tag, token);
        } else {
            return buildSubstitution(token + "er", tag, token);
        }
    }

    public Word regularAdjToSuperlative(Word word) {
        final String tag = tagSet.ADJ_SUP;
        final String token = word.getToken();

        if (token.equalsIgnoreCase("good")) {
            return buildSubstitution("best", tag, token);
        } else if (token.endsWith("y")) {
            return buildSubstitution(token.substring(0, token.length() - 1) + "iest", tag, token);
        } else if (token.endsWith("t") && ErrorUtilities.isVowel(token.charAt(token.length() - 2))) {
            return buildSubstitution(token + "test", tag, token);
        } else if (token.endsWith("e")) {
            return buildSubstitution(token + "st", tag, token);
        } else {
            return buildSubstitution(token + "est", tag, token);
        }
    }

    public Word comparativeAdjToSuperlative(Word word) {
        final String tag = tagSet.ADJ_SUP;
        final String token = word.getToken();

        if (token.equalsIgnoreCase("better")) {
            return buildSubstitution("best", tag, token);
        } else if (token.length() > 0) {
            return buildSubstitution(token.substring(0, token.length() - 1) + "st", tag, token);
        } else {
            return null;
        }
    }

    public Word superlativeAdjToComparative(Word word) {
        final String tag = tagSet.ADJ_COMP;
        final String token = word.getToken();

        if (token.equalsIgnoreCase("best")) {
            return buildSubstitution("better", tag, token);
        } else if (token.length() > 1) {
            return buildSubstitution(token.substring(0, token.length() - 2) + "r", tag, token);
        } else {
            return null;
        }
    }

    //TODO: deal with cases such as "freer" to "free"
    public Word comparativeAdjToRegular(Word word) {
        final String tag = tagSet.ADJ;
        final String token = word.getToken();

        if (token.equalsIgnoreCase("better")) {
            return buildSubstitution("good", tag, token);
        } else if (token.endsWith("ier")) {
            return buildSubstitution(token.substring(0, token.length() - 3) + "y", tag, token);
        } else if (token.endsWith("tter")) {
            return buildSubstitution(token.substring(0, token.length() - 3), tag, token);
        } else if (token.endsWith("er")) {
            return buildSubstitution(token.substring(0, token.length() - 2), tag, token);
        } else {
            return null;
        }
    }

    public Word superlativeAdjToRegular(Word word) {
        final String tag = tagSet.ADJ;
        final String token = word.getToken();

        if (token.equalsIgnoreCase("best")) {
            return buildSubstitution("good", tag, token);
        } else if (token.endsWith("iest")) {
            return buildSubstitution(token.substring(0, token.length() - 4) + "y", tag, token);
        } else if (token.endsWith("ttest")) {
            return buildSubstitution(token.substring(0, token.length() - 4), tag, token);
        } else if (token.length() > 2) {
            return buildSubstitution(token.substring(0, token.length() - 3), tag, token);
        } else {
            return null;
        }
    }

    public Word adverbToAdj(Word word) {
        final String tag = tagSet.ADJ;
        final String token = word.getToken();

        if (token.equalsIgnoreCase("angrily")) {
            return buildSubstitution(token.substring(0, 1) + "ngry", tag, token);
        } else if (token.endsWith("ically")) {
            String lower = token.toLowerCase();
            if ((lower.endsWith("typically") && !lower.startsWith("phenotyp")) ||
                    lower.endsWith("identically") ||
                    lower.endsWith("topologically") ||
                    lower.endsWith("athologically") ||
                    lower.endsWith("theologically") ||
                    lower.endsWith("liturgically") ||
                    lower.endsWith("psychologically") ||
                    lower.endsWith("vertically") ||
                    lower.endsWith("chemically") ||
                    lower.endsWith("chronologically") ||
                    lower.endsWith("mathematically") ||
                    lower.endsWith("zygotically") ||
                    lower.endsWith("canonically") ||
                    lower.endsWith("spherically") ||
                    lower.endsWith("lexically") ||
                    lower.endsWith("practically") ||
                    lower.endsWith("technologically") ||
                    lower.endsWith("surgically") ||
                    lower.endsWith("technically") ||
                    lower.endsWith("theatrically") ||
                    lower.endsWith("categorically") ||
                    (lower.endsWith("radically") && !lower.startsWith("spor"))) {
                return buildSubstitution(token.substring(0, token.length() - 2), tag, token);
            } else {
                return buildSubstitution(token.substring(0, token.length() - 4), tag, token);
            }
        } else if (token.endsWith("ably") || token.endsWith("ibly")) {
            // e.g. unsustainably -> unsustainable
            return buildSubstitution(token.substring(0, token.length() - 1) + "e", tag, token);
            // } else if (token.endsWith("arily") || token.endsWith("dily")) {
        } else if (token.endsWith("ily")) {
            // e.g. extraordinarily -> extraordinary
            // e.g. Steadily -> Steady
            // e.g. derogatorily -> derogatory
            if (token.equalsIgnoreCase("eerily")) {
                return buildSubstitution(token.substring(0, 1) + "erie", tag, token);
            } else {
                return buildSubstitution(token.substring(0, token.length() - 3) + "y", tag, token);
            }
        } else if (token.endsWith("bly") || token.endsWith("btly") || token.endsWith("mply")) {
            // e.g. feebly -> feeble, subtly -> subtle
            return buildSubstitution(token.substring(0, token.length() - 1) + "e", tag, token);
        } else if (token.endsWith("uly")) {
            // e.g. unduly -> undue
            return buildSubstitution(token.substring(0, token.length() - 2) + "e", tag, token);
        } else if (token.endsWith("ully")) {
            // e.g. fully -> full, thankfully -> thankful
            if (token.toLowerCase().equals("fully")) {
                return buildSubstitution(token.substring(0, token.length() - 1), tag, token);
            } else {
                return buildSubstitution(token.substring(0, token.length() - 2), tag, token);
            }
        } else if (token.endsWith("olly")) {
            return buildSubstitution(token.substring(0, token.length() - 2) + "e", tag, token);
        } else if (token.endsWith("doubly")) {
            // e.g. doubly -> double (?)
            return buildSubstitution(token.substring(0, token.length() - 1) + "e", tag, token);
        } else if (token.length() > 1) {
            //strip the "ly" from the end of the word
            return buildSubstitution(token.substring(0, token.length() - 2), tag, token);
        } else {
            return null;
        }
    }
}
