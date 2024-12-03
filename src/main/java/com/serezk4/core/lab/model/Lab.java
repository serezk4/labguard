package com.serezk4.core.lab.model;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.antlr.v4.runtime.tree.ParseTree;

@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
@Getter
@Setter
public class Lab {
    final String isu;
    final int labNumber;
    ParseTree tree;
}
