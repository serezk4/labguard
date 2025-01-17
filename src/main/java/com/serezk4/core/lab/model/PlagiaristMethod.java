package com.serezk4.core.lab.model;

public record PlagiaristMethod(Method targetMethod, Method plagiarizedMethod, double similarity) {
}
