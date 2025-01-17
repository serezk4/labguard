package com.serezk4.core.lab.check;

import com.serezk4.core.lab.model.Clazz;
import com.serezk4.core.lab.model.Method;

public interface Checker {
    double detect(Method source, Method target);
}
