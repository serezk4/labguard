package com.serezk4.core.lab.check.pattern;

import com.serezk4.core.lab.check.Checker;
import com.serezk4.core.lab.model.Method;

import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PatternMatchingChecker implements Checker {
    @Override
    public double detect(Method source, Method target) {
        Pattern sourcePattern = generatePattern(source.source());
        Pattern targetPattern = generatePattern(target.source());

        int commonMatches = (int) sourcePattern.matcher(target.source()).results().count();
        int sourceMatches = (int) sourcePattern.matcher(source.source()).results().count();
        int targetMatches = (int) targetPattern.matcher(target.source()).results().count();

        return (double) commonMatches / Math.max(sourceMatches, targetMatches);
    }

    private Pattern generatePattern(String sourceCode) {
        String cleanedSource = sourceCode.replaceAll("\\s+", " ").trim();
        String patternString = extractKeywords(cleanedSource);
        return Pattern.compile(patternString, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    }

    private String extractKeywords(String sourceCode) {
        String keywordsRegex = "\\b(int|double|String|if|else|while|for|return|public|private|protected|static|final)\\b";
        String methodsRegex = "\\b(\\w+)\\s*\\(";

        String keywords = Pattern.compile(keywordsRegex)
                .matcher(sourceCode)
                .results()
                .map(MatchResult::group)
                .collect(Collectors.joining("|"));

        String methods = Pattern.compile(methodsRegex)
                .matcher(sourceCode)
                .results()
                .map(match -> match.group(1))
                .collect(Collectors.joining("|"));

        return String.format("(%s|%s)", keywords, methods);
    }
}