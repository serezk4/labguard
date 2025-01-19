package com.serezk4.core.lab.model;

import java.util.List;

/**
 * Represents a lab associated with a specific student and lab number.
 *
 * <p>
 * The {@code Lab} class encapsulates the metadata and content of a programming lab.
 * It includes:
 * <ul>
 *     <li>The student's ISU identifier.</li>
 *     <li>The lab number.</li>
 *     <li>A list of {@link Clazz} objects representing the classes contained in the lab.</li>
 * </ul>
 * This class is designed to serve as a container for lab-related data, enabling analysis and storage.
 * </p>
 *
 * @param isu       The ISU identifier of the student who owns the lab.
 *                  This is typically a unique 6-digit numeric identifier.
 * @param labNumber The number of the lab being represented.
 *                  This is a positive integer corresponding to a specific lab assignment.
 * @param clazzes   A list of {@link Clazz} objects representing the parsed Java classes in the lab.
 *                  May be {@code null} or empty if no classes are available.
 *
 * <p><b>Usage:</b></p>
 * <pre>{@code
 * Lab lab = new Lab("123456", 1, List.of(new Clazz("ExampleClass", parseTree, sourceCode, checkstyleResults)));
 * }</pre>
 *
 * <p>
 * Typical operations involve passing a {@code Lab} instance to services that analyze,
 * compare, or store lab data.
 * </p>
 *
 * @see Clazz
 *
 * @author serezk4
 * @version 1.0
 * @since 1.0
 */
public record Lab(
        String isu,
        int labNumber,
        List<Clazz> clazzes
) {
}
