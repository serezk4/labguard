package com.serezk4.core.lab.check.metric;

import com.serezk4.core.lab.check.Detector;
import com.serezk4.core.lab.model.Clazz;

public class CodeMetricsDetector implements Detector {
    @Override
    public double detect(Clazz source, Clazz target) {
        int distance = levenshteinDistance(source.source(), target.source());
        int maxLength = Math.max(source.source().length(), target.source().length());
        return 1.0 - ((double) distance / maxLength);
    }

    private int levenshteinDistance(String code1, String code2) {
        int[][] dp = new int[code1.length() + 1][code2.length() + 1];
        for (int i = 0; i <= code1.length(); i++) {
            for (int j = 0; j <= code2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(Math.min(
                                    dp[i - 1][j] + 1,
                                    dp[i][j - 1] + 1),
                            dp[i - 1][j - 1] + (code1.charAt(i - 1) == code2.charAt(j - 1) ? 0 : 1));
                }
            }
        }
        return dp[code1.length()][code2.length()];
    }
}