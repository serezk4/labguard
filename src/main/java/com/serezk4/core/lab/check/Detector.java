package com.serezk4.core.lab.check;

import com.serezk4.core.lab.model.Clazz;

public interface Detector {
    double detect(Clazz source, Clazz target);
}
