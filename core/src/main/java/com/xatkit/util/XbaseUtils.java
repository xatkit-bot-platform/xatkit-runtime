package com.xatkit.util;

import com.xatkit.core.RuntimePlatformRegistry;
import org.eclipse.xtext.xbase.XMemberFeatureCall;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static fr.inria.atlanmod.commons.Preconditions.checkState;

/**
 * An utility class providing helpers for Xbase objects.
 */
public class XbaseUtils {

    /**
     * Returns {@code true} if the provided {@code featureCall} is a platform's action call, {@code false} otherwise.
     *
     * @param featureCall the {@link XMemberFeatureCall} to check
     * @param registry    the {@link RuntimePlatformRegistry} containing the loaded
     *                    {@link com.xatkit.platform.PlatformDefinition}s.
     * @return {@code true} if the provided {@code featureCall} is a platform's action call, {@code false} otherwise
     */
    public static boolean isPlatformActionCall(XMemberFeatureCall featureCall, RuntimePlatformRegistry registry) {
        String platformName;
        try {
            platformName = getPlatformName(featureCall);
        } catch (IllegalStateException e) {
            /*
             * The qualified name doesn't match the Platform.Action pattern, the feature call cannot be a platform
             * action call.
             */
            return false;
        }
        return registry.getLoadedPlatformDefinitions().stream().anyMatch(p -> p.getName().equals(platformName));
    }

    /**
     * Returns the name of the platform corresponding to the provided {@code actionCall}.
     * <p>
     * The qualified name of the provided {@code actionCall} matches the following pattern: {@code Platform.Action}.
     * This method extracts the name of the platform and returns it as a {@link String}.
     *
     * @param actionCall the {@link XMemberFeatureCall} representing the action call to retrieve the platform name from
     * @return the platform name
     * @throws NullPointerException  if the provided {@code actionCall} is {@code null}
     * @throws IllegalStateException if the {@code actionCall}'s qualified name doesn't follow the {@code Platform
     *                               .Action} template
     */
    public static String getPlatformName(XMemberFeatureCall actionCall) {
        checkNotNull(actionCall, "Cannot retrieve the platform name of the provided %s %s",
                XMemberFeatureCall.class.getSimpleName(), actionCall);
        String[] splittedActionName = splitActionCallName(actionCall);
        return splittedActionName[0];
    }

    /**
     * Returns the name of the action corresponding to the provided {@code actionCall}.
     * <p>
     * The qualified name of the provided {@code actionCall} matches the following pattern: {@code Platform.Action}.
     * This method extracts the name of the action and returns it as a {@link String}.
     *
     * @param actionCall the {@link XMemberFeatureCall} representing the action call to retrieve the action name from
     * @return the action name
     * @throws NullPointerException  if the provided {@code actionCall} is {@code null}
     * @throws IllegalStateException if the {@code actionCall}'s qualified name doesn't follow the {@code Platform
     *                               .Action} template
     */
    public static String getActionName(XMemberFeatureCall actionCall) {
        checkNotNull(actionCall, "Cannot retrieve the action name of the provided %s %s",
                XMemberFeatureCall.class.getSimpleName(), actionCall);
        String[] splittedActionName = splitActionCallName(actionCall);
        return splittedActionName[1];
    }

    /**
     * Returns an array of {@link String}s containing the {@code actionCall}'s qualified name parts.
     *
     * @param actionCall the {@link XMemberFeatureCall} representing the action to compute the qualified name parts
     * @return an array of {@link String}s containing the {@code actionCall}'s qualified name parts
     * @throws NullPointerException  if the provided {@code actionCall} is {@code null}
     * @throws IllegalStateException if the computed array does not contain exactly two elements (the {@code
     *                               actionCall} should follow the {@code Platform.Action} template)
     */
    private static String[] splitActionCallName(XMemberFeatureCall actionCall) {
        checkNotNull(actionCall, "Cannot split the name of the provided %s %s",
                XMemberFeatureCall.class.getSimpleName(), actionCall);
        String qualifiedName = actionCall.getFeature().getQualifiedName();
        String[] splittedQualifiedName = qualifiedName.split("\\.");
        checkState(splittedQualifiedName.length == 2, "An error occurred when splitting the name of the provided %s " +
                        "expected the following syntax: PlatformName.ActionName, found %s",
                XMemberFeatureCall.class.getSimpleName(), qualifiedName);
        return splittedQualifiedName;
    }
}
