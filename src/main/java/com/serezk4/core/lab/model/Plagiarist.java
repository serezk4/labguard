package com.serezk4.core.lab.model;

import java.util.List;

public record Plagiarist(Clazz targetClazz, Clazz plagiarizedClazz, List<PlagiaristMethod> plagiaristMethodList) {
}
