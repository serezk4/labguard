# 🛡️ Labguard 

## Overview

This project is an advanced plagiarism detection system for Java programming assignments. It analyzes both the **structure** and **semantics** of code to identify potential plagiarism. Built using **Java 23** and **Project Loom**, it supports high concurrency through lightweight virtual threads. The system generates comprehensive and interactive HTML reports, making it easy to review and understand the analysis results.

---

## Features

- **Tree-Based Structural Analysis**: Represents Java source code as syntax trees for precise comparison.
- **Semantic Analysis**: Uses Levenshtein distance to calculate similarity between code elements.
- **Custom Weighted Cost Model**: Adjusts tree edit distance computations with configurable penalties for structural and semantic differences.
- **Checkstyle Integration**: Reports coding style violations.
- **HTML Reports**: Interactive, sortable, and easy-to-read reports with detailed comparisons and warnings.
- **High Concurrency with Project Loom**: Leverages virtual threads to efficiently process large datasets.

---

## Algorithms and Implementation

### **Tree-Based Structural Analysis**

The system relies on **Abstract Syntax Trees (ASTs)** to represent the structure of Java source code. Each AST captures the hierarchical relationships between code elements, such as classes, methods, statements, and expressions.

#### Process:
1. **Parsing**:
   - **ANTLR** is used to parse Java source files into `ParseTree` objects.
   - Each node in the `ParseTree` corresponds to a syntactic construct.
   - Example: A `for` loop is parsed into a node with child nodes representing initialization, condition, and update.

2. **Tree Transformation**:
   - The `ParseTree` is converted into a custom node structure (`Node<StringNodeData>`).
   - This custom structure is optimized for tree edit distance calculations.

#### Code Reference:
- [Node Conversion](src/main/java/com/serezk4/core/apted/util/NodeUtil.java)

---

### **APTED Algorithm**

The core of the structural analysis is the **APTED (All Path Tree Edit Distance)** algorithm. It calculates the minimum cost to transform one tree into another, measuring structural similarity.

#### **Key Operations**:
1. **Insertion**:
   - Adds a node to a tree.
   - Example: Adding a missing statement inside a method block.

2. **Deletion**:
   - Removes a node from a tree.
   - Example: Removing an unused method.

3. **Renaming**:
   - Changes the label of a node.
   - Example: Renaming a variable or method.

#### **Weighted Cost Model**:
APTED relies on a custom cost model to determine the cost of each operation:
- **Base Costs**:
  - Each node type (e.g., `Class`, `Method`, `Field`) has a predefined cost.
  - Example: Renaming a class costs more than renaming a local variable.
  - [Implementation: WeightedCostModel.java](src/main/java/com/serezk4/core/apted/costmodel/WeightedCostModel.java)

- **Structure Penalty**:
  - Differences in the number of child nodes between two trees incur penalties.
  - Example: A method with more statements is penalized when compared to a shorter method.

- **Semantic Similarity**:
  - Labels of nodes (e.g., variable names, method names) are compared using Levenshtein distance.
  - High similarity reduces renaming costs.

#### Formula for Renaming Cost:
```plaintext
renamingCost = baseCost + structurePenalty + (1 - semanticSimilarity) * scalingFactor;
```

#### Example Workflow:
1. Two trees representing Java classes are compared.
2. Insertion, deletion, and renaming costs are calculated for each node.
3. The **tree edit distance** is computed as the total transformation cost.

#### Similarity Calculation:
To normalize the tree edit distance:
```plaintext
similarity = 1.0 - (editDistance / maxSubtreeSize)
```

#### Code References:
- [APTED Algorithm](src/main/java/com/serezk4/core/apted/distance/APTED.java)
- [Cost Model Implementation](src/main/java/com/serezk4/core/apted/costmodel/WeightedCostModel.java)

---

### **Semantic Analysis**

Semantic analysis enhances structural analysis by comparing the labels of tree nodes.

#### Levenshtein Distance:
- Measures the minimum number of edits (insertions, deletions, substitutions) required to transform one string into another.
- Example:
  - Comparing `for` and `foreach` results in a smaller distance than comparing `for` and `while`.

#### Precomputed Similarity Map:
- To optimize performance, Levenshtein distances are normalized and stored in a map.
- Example: A distance of 32 (out of 128) corresponds to a similarity of 0.75.

#### Code Reference:
- [Levenshtein Implementation](src/main/java/com/serezk4/core/apted/costmodel/WeightedCostModel.java)

---

### **Checkstyle Integration**

Checkstyle ensures compliance with coding standards. The system integrates Checkstyle to detect and report violations.

#### How It Works:
1. Each file is analyzed using the **CheckstyleAnalyzer**.
2. Warnings are collected and included in the HTML report.

#### Configuration:
- Custom rules are defined in `checkstyle.xml`.

#### Code Reference:
- [Checkstyle Integration](src/main/java/com/serezk4/core/lab/analyze/checkstyle/CheckstyleAnalyzer.java)

---

### **HTML Report Generation**

The system generates an HTML report summarizing the analysis results. The report includes:
- **Plagiarism Comparisons**:
  - Displays side-by-side comparisons of code.
  - Highlights differences between classes, methods, and statements.

- **Similarity Scores**:
  - Ranges from 0.0 (no similarity) to 1.0 (identical).

- **Checkstyle Warnings**:
  - Lists all coding style violations.

#### Interactive Features:
- **Collapsible Code Blocks**:
  - Allows toggling of detailed code comparisons.
- **Sorting**:
  - Enables sorting results by similarity scores.

#### Code Reference:
- [HTML Report Generator](src/main/java/com/serezk4/core/html/HtmlGenerator.java)

---

## Workflow

### Step-by-Step Process:
1. **Input**:
   - The system accepts:
     - Student ISU (identifier).
     - Lab number.
     - Path to Java source files.

2. **Preprocessing**:
   - Each file is parsed into a `ParseTree` using ANTLR.
   - Trees are transformed into a custom structure for analysis.

3. **Similarity Detection**:
   - Structural analysis is performed using the APTED algorithm.
   - Semantic similarity is incorporated into renaming costs.

4. **Report Generation**:
   - Results are summarized in an interactive HTML report.

---

## Usage

### 0. Requirements

```shell
❯ java -version
openjdk version "23" 2024-09-17
OpenJDK Runtime Environment (build 23+37-2369)
OpenJDK 64-Bit Server VM (build 23+37-2369, mixed mode, sharing)

❯ gradle -version

------------------------------------------------------------
Gradle 8.11.1
------------------------------------------------------------

Build time:    2024-11-20 16:56:46 UTC
Revision:      481cb05a490e0ef9f8620f7873b83bd8a72e7c39

Kotlin:        2.0.20
Groovy:        3.0.22
Ant:           Apache Ant(TM) version 1.10.14 compiled on August 16 2023

```

### 1. Build project
```shell
gradle build
```

### 2. Start
```shell
java --enable-preview --add-opens java.base/java.lang=ALL-UNNAMED
 \ -XX:+UseG1GC -jar build/libs/core-1.0-SNAPSHOT.jar
 \ 123456          # isu 
 \ 6               # lab number
 \ /usr/lab/source # path to lab files
```

### 3. Open HTML report