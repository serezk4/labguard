package com.serezk4.core.lab.check.graph;

public class DataFlowNode {
    String variable; // Имя переменной
    String operation; // Описание операции (например, "assignment", "addition", "method call")

    DataFlowNode(String variable, String operation) {
        this.variable = variable;
        this.operation = operation;
    }

    @Override
    public String toString() {
        return "DataFlowNode{variable='" + variable + "', operation='" + operation + "'}";
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DataFlowNode)) return false;
        DataFlowNode other = (DataFlowNode) obj;
        return variable.equals(other.variable) && operation.equals(other.operation);
    }
}
