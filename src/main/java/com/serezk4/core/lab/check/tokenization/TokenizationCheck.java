package com.serezk4.core.lab.check.tokenization;

import com.serezk4.core.lab.check.Detector;
import com.serezk4.core.lab.model.Clazz;

import java.util.Arrays;
import java.util.List;

public class TokenizationCheck implements Detector {
    @Override
    public double detect(Clazz source, Clazz target) {
        List<String> tokens1 = tokenize(source.source());
        List<String> tokens2 = tokenize(target.source());
        long commonTokens = tokens1.stream().filter(tokens2::contains).count();
        return (double) commonTokens / Math.max(tokens1.size(), tokens2.size());
    }

    private static List<String> tokenize(String sourceCode) {
        return Arrays.asList(sourceCode.split("\\s+"));
    }
}
