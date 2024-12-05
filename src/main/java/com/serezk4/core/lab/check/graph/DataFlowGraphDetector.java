package com.serezk4.core.lab.check.graph;

import com.serezk4.core.lab.check.Detector;
import com.serezk4.core.lab.model.Clazz;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataFlowGraphDetector implements Detector {
    private static final Pattern VARIABLE_ASSIGNMENT = Pattern.compile("(\\w+)\\s*=\\s*(.*);");
    private static final Pattern METHOD_CALL = Pattern.compile("(\\w+)\\.(\\w+)\\((.*)\\);");

    private List<DataFlowNode> extractDataFlowGraph(Clazz clazz) {
        List<DataFlowNode> dataFlowGraph = new ArrayList<>();
        String[] lines = clazz.source().split("\\n");

        for (String line : lines) {
            line = line.trim();

            Matcher assignmentMatcher = VARIABLE_ASSIGNMENT.matcher(line);
            if (assignmentMatcher.matches()) {
                String variable = assignmentMatcher.group(1);
                String operation = "assignment: " + assignmentMatcher.group(2).trim();
                dataFlowGraph.add(new DataFlowNode(variable, operation));
                continue;
            }

            Matcher methodMatcher = METHOD_CALL.matcher(line);
            if (methodMatcher.matches()) {
                String variable = methodMatcher.group(1);
                String operation = "method call: " + methodMatcher.group(2);
                dataFlowGraph.add(new DataFlowNode(variable, operation));
            }
        }

        return dataFlowGraph;
    }

    @Override
    public double detect(Clazz source, Clazz target) {
        List<DataFlowNode> graph1 = extractDataFlowGraph(source);
        List<DataFlowNode> graph2 = extractDataFlowGraph(target);

        Set<DataFlowNode> set1 = new HashSet<>(graph1);
        Set<DataFlowNode> set2 = new HashSet<>(graph2);

        int intersectionSize = 0;

        for (DataFlowNode node : set1) {
            if (set2.contains(node)) {
                intersectionSize++;
            }
        }

        int unionSize = set1.size() + set2.size() - intersectionSize;

        return unionSize == 0 ? 0.0 : (double) intersectionSize / unionSize;
    }
}