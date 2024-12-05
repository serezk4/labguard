package com.serezk4.core.lab.model;

public record Plagiarist(Clazz targetClazz, Clazz plagiarizedClazz, double similarity) {
}
