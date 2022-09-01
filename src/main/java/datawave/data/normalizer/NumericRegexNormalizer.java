package datawave.data.normalizer;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import datawave.data.type.util.NumericalEncoder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

public class NumericRegexNormalizer {
    
    private static final char NULL = '\u0000';
    private static final char ZERO = '0';
    private static final char ONE = '1';
    private static final char TWO = '2';
    private static final char THREE = '3';
    private static final char FOUR = '4';
    private static final char FIVE = '5';
    private static final char SIX = '6';
    private static final char SEVEN = '7';
    private static final char EIGHT = '8';
    private static final char NINE = '9';
    private static final char LOWERCASE_D = 'd';
    private static final char BACKSLASH = '\\';
    private static final char PERIOD = '.';
    private static final char HYPHEN = '-';
    private static final char STAR = '*';
    private static final char PLUS = '+';
    private static final char PIPE = '|';
    private static final char GROUP_START = '(';
    private static final char GROUP_END = ')';
    private static final char LIST_START = '[';
    private static final char LIST_END = ']';
    private static final char EXCLAMATION_POINT = '!';
    
    private static final String ESCAPED_BACKSLASH = "\\\\";
    private static final String CARET = "^";
    private static final String DOLLAR = "$";
    private static final String EMPTY_GROUP = "()";
    
    /**
     * Use base 10 when parsing characters to ints.
     */
    private static final int DECIMAL_RADIX = 10;
    /**
     * The set of all digits. This reflects all possible permutations for any \d found in the regex.
     */
    private static final List<Character> ALL_DIGITS = ImmutableList.of(ZERO, ONE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE);
    
    /**
     * The set of all digits and the dot. This reflects all possible permutations for any . wildcards found in the regex.
     */
    private static final List<Character> ALL_DIGITS_AND_PERIOD = ImmutableList.of(ZERO, ONE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, PERIOD);
    
    /**
     * Matches against any unescaped d characters, and any other letters. If \d is present, that indicates a digit and is allowed.
     */
    private static final Pattern RESTRICTED_LETTERS = Pattern.compile(".*[a-ce-zA-Z].*");
    
    private static final Predicate<String> IS_NOT_EMPTY = (str) -> !str.isEmpty();
    
    private static final Predicate<String> IS_NOT_PERIOD = (str) -> !str.equals(".");
    
    private static final Predicate<String> DOES_NOT_CONTAIN_MULTIPLE_PERIODS = (str) -> {
        int length = str.length();
        boolean encounteredPeriod = false;
        for (int i = 0; i < length; i++) {
            if (str.charAt(i) == PERIOD) {
                if (encounteredPeriod) {
                    return false;
                } else {
                    encounteredPeriod = true;
                }
            }
        }
        return true;
    };
    
    private final String regex;
    
    private List<PermutationGroup> permutations = new ArrayList<>();
    
    public static NumericRegexNormalizer of(String regex) {
        return new NumericRegexNormalizer(regex);
    }
    
    private NumericRegexNormalizer(String regex) {
        this.regex = regex;
    }
    
    public String normalize() {
        checkForQuickFailures();
        if (regexRequiresEncoding()) {
            parsePermutations();
            removeInvalidPermutations();
            return encodeAndRebuildPattern();
        } else {
            return regex;
        }
    }
    
    /**
     * Pre-validate the regex to quickly identify any indications that the regex is not valid for numerical expansion.
     */
    private void checkForQuickFailures() {
        checkForEmptyPattern();
        checkForInvalidPattern();
        checkForRestrictedLetters();
        checkForWhitespace();
        checkForEscapedBackslash();
        checkForLineAnchors();
        checkForEmptyGroups();
    }
    
    /**
     * Throw an exception if the regex pattern is empty.
     */
    private void checkForEmptyPattern() {
        if (regex.isEmpty()) {
            throw new IllegalArgumentException("Regex pattern may not be blank.");
        }
    }
    
    /**
     * Throw an exception if the regex cannot be compiled.
     */
    private void checkForInvalidPattern() {
        try {
            Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("Invalid numeric regex pattern provided: " + regex, e);
        }
    }
    
    /**
     * Throw an exceptions if the regex contains any letters other than \d.
     */
    private void checkForRestrictedLetters() {
        if (RESTRICTED_LETTERS.matcher(regex).matches() || containsUnescapedLowercaseD()) {
            throw new IllegalArgumentException(
                            "Regex pattern may not contain any letters other than \\d to indicate a member of the digit character class 0-9.");
        }
    }
    
    /**
     * Return whether the regex contains an unescaped d.
     */
    private boolean containsUnescapedLowercaseD() {
        int pos = regex.indexOf(LOWERCASE_D);
        while (pos != -1) {
            if (pos == 0 || regex.charAt(pos - 1) != BACKSLASH) {
                return true;
            }
            pos = regex.indexOf(LOWERCASE_D, pos + 1);
        }
        return false;
    }
    
    /**
     * Throw an exception if the regex contains any whitespace.
     */
    private void checkForWhitespace() {
        if (CharMatcher.whitespace().matchesAnyOf(regex)) {
            throw new IllegalArgumentException("Regex pattern may not contain any whitespace.");
        }
    }
    
    /**
     * Throw an exception if the regex contains an escaped backslash.
     */
    private void checkForEscapedBackslash() {
        if (regex.contains(ESCAPED_BACKSLASH)) {
            throw new IllegalArgumentException("Regex pattern may not contain any escaped backslashes.");
        }
    }
    
    /**
     * Throw an exception if the regex contains any line anchors ^ or $.
     */
    private void checkForLineAnchors() {
        if (regex.contains(CARET) || regex.contains(DOLLAR)) {
            throw new IllegalArgumentException("Regex pattern may not contain line anchors ^ or $.");
        }
    }
    
    /**
     * Throw an exception if the regex contains any empty groups.
     */
    private void checkForEmptyGroups() {
        if (regex.contains(EMPTY_GROUP)) {
            throw new IllegalArgumentException("Regex pattern may not contain empty groups '()'.");
        }
    }
    
    /**
     * Return whether the regex requires any numerical encoding.
     */
    private boolean regexRequiresEncoding() {
        // todo - finish implementing checks here
        return true;
    }
    
    /**
     * Parse the various numerical permutations for the regex.
     */
    private void parsePermutations() {
        RegexParser parser = new RegexParser(regex);
        this.permutations = parser.parse();
    }
    
    /**
     * Remove any numerical permutations that are not valid numbers.
     */
    private void removeInvalidPermutations() {
        Iterator<PermutationGroup> iterator = this.permutations.iterator();
        while (iterator.hasNext()) {
            PermutationGroup group = iterator.next();
            // @formatter:off
            List<String> validSequences = group.sequences.stream()
                            .filter(IS_NOT_EMPTY)
                            .filter(IS_NOT_PERIOD)
                            .filter(DOES_NOT_CONTAIN_MULTIPLE_PERIODS)
                            .collect(Collectors.toList());
            // @formatter: on
    
            // Keep any remaining valid sequences, or otherwise delete the permutation group.
            // Todo - maybe implement a strict flag to throw an error upon any bad permutations?
            if(!validSequences.isEmpty()){
                group.sequences = validSequences;
            } else {
                iterator.remove();
            }
        }
        
        // Check if we have any valid permutations remaining.
        if (this.permutations.isEmpty()){
            throw new IllegalStateException("No valid numerical sequences could be generated by the pattern " + regex);
        }
    }
    
    /**
     * Encode the numerical permutations, and return the rebuilt regex pattern with these encodings.
     */
    private String encodeAndRebuildPattern() {
        StringBuilder sb = new StringBuilder();
        boolean multipleGroups = permutations.size() > 1;
        for (PermutationGroup group : permutations) {
            boolean hasTrailingModifiers = group.qualifiers != null;
            boolean multipleSequences = group.sequences.size() > 1;
            
            // Separate multiple permutations with |.
            if (sb.length() != 0) {
                sb.append(PIPE);
            }
            // If there are multiple sequences here, and multiple groups, we want to start a new group.
            if (multipleSequences && multipleGroups) {
                sb.append(GROUP_START);
            }
            // Additionally, if there are any trailing modifiers like .*, we want to start a new group that the modifiers will be placed afterwards.
            if (hasTrailingModifiers) {
                sb.append(GROUP_START);
            }
            
            // Append the encoded, escaped numerical sequences, separated by |.
            // @formatter:off
            sb.append(group.sequences.stream()
                            .map(NumericalEncoder::encode)
                            .map(this::escapeSpecialCharacters)
                            .collect(Collectors.joining("|")));
            
            // @formatter:on
            
            // Close the groups.
            if (hasTrailingModifiers) {
                sb.append(GROUP_END);
            }
            if (multipleSequences && multipleGroups) {
                sb.append(GROUP_END);
            }
        }
        return sb.toString();
    }
    
    /**
     * Return an encoded number with all of its special characters escaped.
     */
    private String escapeSpecialCharacters(String str) {
        StringBuilder sb = new StringBuilder();
        char[] strChars = str.toCharArray();
        for (char ch : strChars) {
            if (ch == PERIOD || ch == EXCLAMATION_POINT || ch == PLUS) {
                sb.append(BACKSLASH);
            }
            sb.append(ch);
        }
        return sb.toString();
    }
    
    public static class RegexParser {
        
        /**
         * Holds the length of the pattern string.
         */
        private final int patternLength;
        
        /**
         * Holds the contents of the pattern as a char array for convenience.
         */
        private final char[] patternArray;
        
        /**
         * The index into the pattern string that keeps track of how much has been parsed.
         */
        private int cursor = 0;
        
        RegexParser(String pattern) {
            this.patternLength = pattern.length();
            this.patternArray = pattern.toCharArray();
        }
        
        /**
         * Parse the regex pattern and return its numerical permutations.
         */
        public List<PermutationGroup> parse() {
            return parseExpression(false);
        }
        
        /**
         * Parse the next expression in the pattern where the cursor currently points.
         * 
         * @param currentlyParsingGroup
         *            whether a group is currently being parsed, indicated by encountering a {@code (}. If so, parsing the current expression will stop once a
         *            {@code )} is found.
         */
        private List<PermutationGroup> parseExpression(boolean currentlyParsingGroup) {
            List<PermutationGroup> permutationGroups = new ArrayList<>();
            PermutationGroup currPermutations = new PermutationGroup();
            CharacterGroup currCharGroup;
            char currChar;
            while (cursor < patternLength) {
                currChar = current();
                switch (currChar) {
                    case HYPHEN:
                    case ZERO:
                    case ONE:
                    case TWO:
                    case THREE:
                    case FOUR:
                    case FIVE:
                    case SIX:
                    case SEVEN:
                    case EIGHT:
                    case NINE:
                        // Parse a numerical sequence and add it to the current permutation group.
                        currCharGroup = parseSequence();
                        currPermutations.mergeWith(currCharGroup);
                        break;
                    case PIPE:
                        // A pipe here indicates a top-level OR. There is nothing left to add to the current permutation group. Add it to our list and create
                        // a new permutation group for the next regex sequence.
                        permutationGroups.add(currPermutations);
                        currPermutations = new PermutationGroup();
                        cursor++;
                        break;
                    case BACKSLASH:
                        // A backslash indicates an escaped decimal point or a \d. Add the resulting char group to the current permutations.
                        currCharGroup = parseBackslash();
                        currPermutations.mergeWith(currCharGroup);
                        break;
                    case LIST_START:
                        // Parse the list of numerical characters from a character list and add it to the current permutation group.
                        currCharGroup = parseList();
                        currPermutations.mergeWith(currCharGroup);
                        break;
                    case PERIOD:
                        if (hasNext()) {
                            char next = peek();
                            if (next == STAR || next == PLUS) {
                                currPermutations.qualifiers = parseQualifiers();
                                permutationGroups.add(currPermutations);
                                currPermutations = new PermutationGroup();
                                break;
                            }
                        }
                        currCharGroup = parseWildcard();
                        currPermutations.mergeWith(currCharGroup);
                        break;
                    case GROUP_START:
                        // Do not allow nested groups.
                        if (currentlyParsingGroup) {
                            throw new IllegalArgumentException("Nested regex groups are not supported.");
                        }
                        cursor++;
                        List<PermutationGroup> groupPermutations = parseExpression(true);
                        currPermutations.mergeWith(groupPermutations);
                        break;
                    case GROUP_END:
                        // If a group is currently being parsed, this is the end of the group, and there is nothing left to add to the current permutations. Add
                        // it to our list and create a new permutation group for the next regex sequence.
                        if (currentlyParsingGroup) {
                            cursor++;
                            permutationGroups.add(currPermutations);
                            return permutationGroups;
                        }
                    default:
                        // We've encountered a regex operation that we don't support.
                        throw new UnsupportedOperationException("Encountered unsupported regex operation " + currChar + " at position " + cursor);
                    
                }
            }
            // Ensure the last permutation group we parsed is added.
            permutationGroups.add(currPermutations);
            return permutationGroups;
        }
        
        /**
         * Retrieve the next character, and advance the cursor by one.
         */
        private char next() {
            return patternArray[++cursor];
        }
        
        /**
         * Return whether {@link #next()} without an exception.
         */
        private boolean hasNext() {
            return cursor < patternLength - 1;
        }
        
        /**
         * Return the character after the next one, and advance the cursor by two.
         */
        private char skip() {
            cursor++;
            return next();
        }
        
        /**
         * Peek the next character, and do not advance the cursor.
         */
        private char peek() {
            return patternArray[cursor + 1];
        }
        
        /**
         * Return the character at the position of the cursor.
         */
        private char current() {
            return patternArray[cursor];
        }
        
        /**
         * Parse and return the next numerical sequence.
         */
        public CharacterGroup parseSequence() {
            CharacterGroup group = CharacterGroup.newSequence();
            char curr = current();
            for (;;) {
                switch (curr) {
                    case HYPHEN:
                    case ZERO:
                    case ONE:
                    case TWO:
                    case THREE:
                    case FOUR:
                    case FIVE:
                    case SIX:
                    case SEVEN:
                    case EIGHT:
                    case NINE:
                        // Add any numeric characters as part of the sequence. If we encounter a period, we were directed here from parseBackslash() where an
                        // escaped decimal point was identified.
                        group.add(curr);
                        break;
                    default:
                        // Any other characters indicate the end of this particular numeric sequence.
                        return group;
                }
                if (!hasNext()) {
                    cursor++; // Increment the cursor to 'move' it past the end to ensure we will break out of all loops.
                    break;
                } else {
                    curr = next();
                }
            }
            return group;
        }
        
        /**
         * Return a character list that are valid numeric permutations for a . wildcard. This consists of the numbers 0-9, and a period for a decimal point.
         * However, if the wildcard is followed by * or +, and it is not definitively for the absolute end of a numerical sequence, an exception will be thrown.
         */
        private CharacterGroup parseWildcard() {
            cursor++;
            return CharacterGroup.newList(ALL_DIGITS_AND_PERIOD);
        }
        
        // todo finish implementing handling qualifiers
        private String parseQualifiers() {
            return "";
        }
        
        /**
         * Parse the group of characters relevant for the next escaped character.
         */
        private CharacterGroup parseBackslash() {
            char next = next();
            switch (next) {
                case PERIOD:
                    // If we found an escaped decimal point, capture it and any following numbers as a numerical sequence.
                    cursor++;
                    return CharacterGroup.newList(Collections.singleton(PERIOD));
                case LOWERCASE_D:
                    // If we found a lowercase d, i.e. \d, this
                    cursor++;
                    return CharacterGroup.newList(ALL_DIGITS);
                default:
                    throw new UnsupportedOperationException("Encountered unsupported escaped character " + next + " at position " + (cursor));
            }
        }
        
        /**
         * Parse the contents of a regex character list, e.g. [123-8].
         */
        private CharacterGroup parseList() {
            // Maintain a sorted set of any characters we parse from a list for easier testing and to prevent duplicates.
            SortedSet<Character> chars = new TreeSet<>();
            char curr = next();
            char next = peek();
            while (cursor < patternLength) {
                switch (curr) {
                    case ZERO:
                    case ONE:
                    case TWO:
                    case THREE:
                    case FOUR:
                    case FIVE:
                    case SIX:
                    case SEVEN:
                    case EIGHT:
                    case NINE:
                        // If the next character is a hyphen, this indicates a character list with a possible numeric range such as [1-4].
                        if (next == HYPHEN) {
                            char charAfterNext = skip();
                            // Verify the end of the range is a number, and that we don't have a character list that looks like [4-] or [2-.].
                            if (!ALL_DIGITS.contains(charAfterNext)) {
                                throw new IllegalArgumentException("Encountered invalid end to numeric range in character list at position " + cursor);
                            }
                            int startOfRange = Character.getNumericValue(curr);
                            int endOfRange = Character.getNumericValue(charAfterNext);
                            // Add all characters within the defined numeric range.
                            for (int i = startOfRange; i <= endOfRange; i++) {
                                chars.add(Character.forDigit(i, DECIMAL_RADIX));
                            }
                        } else {
                            // Just a straightforward numeric character, add it.
                            chars.add(curr);
                        }
                        break;
                    case HYPHEN:
                        // If we've reached this, we have a list that contains a hyphen that is not part of a numeric range, such as [-34].
                        throw new IllegalArgumentException("Encountered invalid start to numeric range in character list at position " + cursor);
                    case PERIOD:
                        // Allow periods to be specified as part of a character list for decimal points.
                        chars.add(curr);
                        break;
                    case LIST_END:
                        // We've reached the end of the list. No more characters can be added.
                        cursor++;
                        return CharacterGroup.newList(chars);
                    default:
                        throw new IllegalArgumentException("Encountered invalid option in character list " + curr + " at position " + cursor);
                }
                curr = next();
                if (hasNext()) {
                    next = peek();
                } else {
                    next = NULL;
                }
            }
            // Theoretically we should never reach this statement since a valid regex character list will always have a terminating ] that will be handled by
            // the switch above.
            return CharacterGroup.newList();
        }
    }
    
    private static class PermutationGroup {
        private String qualifiers;
        private List<String> sequences = new ArrayList<>();
        
        public List<String> getSequences() {
            return sequences;
        }
        
        private void mergeWith(CharacterGroup group) {
            List<String> permutations = group.getPermutations();
            if (sequences.isEmpty()) {
                sequences = permutations;
            } else {
                // @formatter:off
                sequences = sequences.stream()
                                .flatMap((str) -> permutations.stream().map(charStr -> str + charStr)) // Intersect each permutation.
                                .distinct() // Remove any duplicates.
                                .collect(Collectors.toList());
                // @formatter:on
            }
        }
        
        private void mergeWith(List<PermutationGroup> groups) {
            // @formatter:off
            List<String> flattenedSequences = groups.stream()
                            .map(PermutationGroup::getSequences)
                            .flatMap(List::stream)
                            .distinct()
                            .collect(Collectors.toList());
            // @formatter:on
            if (sequences.isEmpty()) {
                sequences = flattenedSequences;
            } else {
                // @formatter:off
                sequences = sequences.stream()
                                .flatMap((str) -> flattenedSequences.stream().map(seq -> str + seq))
                                .distinct()
                                .collect(Collectors.toList());
                // @formatter:on
            }
        }
    }
    
    private static class CharacterGroup {
        private enum Type {
            /**
             * Represents a list of characters that do not make up a numerical sequence, but are instead each possible options for a single position in a
             * numeric sequence.
             */
            LIST {
                public List<String> getPermutations(List<Character> characters) {
            // @formatter:off
                    return characters.stream()
                                    .map(String::valueOf)
                                    .collect(Collectors.toList());
                    // @formatter:on
                }
            },
            
            /**
             * Represents a list of characters that make up a single numerical sequence.
             */
            SEQUENCE {
                public List<String> getPermutations(List<Character> characters) {
                    StringBuilder sb = new StringBuilder();
                    characters.forEach(sb::append);
                    String sequence = sb.toString();
                    
                    return Lists.newArrayList(sequence);
                }
            };
            
            abstract List<String> getPermutations(List<Character> characters);
            
        }
        
        private final Type type;
        private final List<Character> characters;
        
        private static CharacterGroup newList() {
            return new CharacterGroup(Type.LIST);
        }
        
        private static CharacterGroup newList(Collection<Character> characters) {
            return new CharacterGroup(Type.LIST, characters);
        }
        
        private static CharacterGroup newSequence() {
            return new CharacterGroup(Type.SEQUENCE);
        }
        
        private CharacterGroup(Type type) {
            this.type = type;
            this.characters = new ArrayList<>();
        }
        
        private CharacterGroup(Type type, Collection<Character> characters) {
            this(type);
            this.characters.addAll(characters);
        }
        
        private void add(char character) {
            this.characters.add(character);
        }
        
        private List<String> getPermutations() {
            return type.getPermutations(characters);
        }
    }
}
