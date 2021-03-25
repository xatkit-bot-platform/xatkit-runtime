package com.xatkit.util;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;

/**
 * An utility class providing helpers for EMF objects.
 */
public class EMFUtils {

    /**
     * Disables the default constructor, this class only provides static methods and should not be constructed.
     */
    private EMFUtils() { }

    /**
     * Computes the name of the provided {@code eObject}.
     * <p>
     * This method searches for a <i>name</i> attribute associated to the provided {@code eObject} and returns its
     * value. If there is no such attribute the value of the {@code toString()} method is returned.
     *
     * @param eObject the {@link EObject} to retrieve the name of
     * @return the name of the {@link EObject}
     */
    public static String getName(EObject eObject) {
        EStructuralFeature feature = eObject.eClass().getEStructuralFeature("name");
        if (feature instanceof EAttribute) {
            if (((EAttribute) feature).getEAttributeType().getInstanceClass().equals(String.class)) {
                return (String) eObject.eGet(feature);
            }
        }
        return eObject.toString();
    }
}
