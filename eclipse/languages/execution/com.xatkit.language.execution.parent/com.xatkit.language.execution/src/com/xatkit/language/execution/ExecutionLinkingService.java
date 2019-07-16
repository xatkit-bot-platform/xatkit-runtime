package com.xatkit.language.execution;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.linking.impl.DefaultLinkingService;
import org.eclipse.xtext.linking.impl.IllegalNodeException;
import org.eclipse.xtext.nodemodel.INode;

import com.xatkit.execution.ActionInstance;
import com.xatkit.execution.ExecutionModel;
import com.xatkit.execution.ExecutionPackage;
import com.xatkit.execution.ExecutionRule;
import com.xatkit.execution.ParameterValue;
import com.xatkit.intent.EventDefinition;
import com.xatkit.platform.ActionDefinition;
import com.xatkit.platform.EventProviderDefinition;
import com.xatkit.platform.Parameter;
import com.xatkit.platform.PlatformDefinition;
import com.xatkit.utils.ImportRegistry;

public class ExecutionLinkingService extends DefaultLinkingService {

	public ExecutionLinkingService() {
		super();
		System.out.println("Created Execution Linking Service");
	}

	@Override
	public List<EObject> getLinkedObjects(EObject context, EReference ref, INode node) throws IllegalNodeException {
		System.out.println("Linking context: " + context);
		System.out.println("Linking reference: " + ref);
		if (context instanceof ExecutionModel) {
			return getLinkedObjectsForExecutionModel((ExecutionModel) context, ref, node);
		} else if (context instanceof ExecutionRule) {
			return getLinkedObjectsForExecutionRule((ExecutionRule) context, ref, node);
		} else if (context instanceof ActionInstance) {
			return getLinkedObjectsForActionInstance((ActionInstance) context, ref, node);
		} else if (context instanceof ParameterValue) {
			return getLinkedObjectsForParameterValue((ParameterValue) context, ref, node);
		} else {
			return super.getLinkedObjects(context, ref, node);
		}
	}

	private List<EObject> getLinkedObjectsForExecutionModel(ExecutionModel context, EReference ref, INode node) {
		if (ref.equals(ExecutionPackage.eINSTANCE.getExecutionModel_EventProviderDefinitions())) {
			QualifiedName qualifiedName = getQualifiedName(node.getText());
			if (nonNull(qualifiedName)) {
				String platformName = qualifiedName.getQualifier();
				String eventProviderName = qualifiedName.getLocalName();
				PlatformDefinition platformDefinition = ImportRegistry.getInstance()
						.getImportedPlatform((ExecutionModel) context, platformName);
				if (nonNull(platformDefinition)) {
					EventProviderDefinition eventProviderDefinition = platformDefinition
							.getEventProviderDefinition(eventProviderName);
					if (nonNull(eventProviderDefinition)) {
						return Arrays.asList(eventProviderDefinition);
					}
				}
			}
			return Collections.emptyList();
		} else {
			return super.getLinkedObjects(context, ref, node);
		}
	}

	private List<EObject> getLinkedObjectsForExecutionRule(ExecutionRule context, EReference ref, INode node) {
		if (ref.equals(ExecutionPackage.eINSTANCE.getExecutionRule_Event())) {
			ExecutionModel executionModel = (ExecutionModel) context.eContainer();
			/*
			 * Trying to retrieve an Event from a loaded Library
			 */
			EventDefinition foundEvent = ExecutionUtils.getEventDefinitionFromImportedLibraries(executionModel,
					node.getText());
			if (isNull(foundEvent)) {
				/*
				 * Cannot retrieve the Event from a loaded Library, trying to retrieve it from a loaded Platform
				 */
				foundEvent = ExecutionUtils.getEventDefinitionFromImportedPlatforms(executionModel, node.getText());
			}
			if (nonNull(foundEvent)) {
				return Arrays.asList(foundEvent);
			} else {
				/*
				 * Cannot retrieve the Event from the loaded Libraries or Platforms
				 */
				return Collections.emptyList();
			}
		} else {
			return super.getLinkedObjects(context, ref, node);
		}
	}

	private List<EObject> getLinkedObjectsForActionInstance(ActionInstance context, EReference ref, INode node) {
		if (ref.equals(ExecutionPackage.eINSTANCE.getActionInstance_Action())) {
			/*
			 * Trying to retrieve an Action from a loaded platform
			 */
			ExecutionModel executionModel = ExecutionUtils.getContainingExecutionModel(context);
			QualifiedName qualifiedName = getQualifiedName(node.getText());
			if (nonNull(qualifiedName)) {
				String platformName = qualifiedName.getQualifier();
				String actionName = qualifiedName.getLocalName();
				PlatformDefinition platformDefinition = ImportRegistry.getInstance().getImportedPlatform(executionModel,
						platformName);
				if (nonNull(platformDefinition)) {
					/*
					 * If there are multiple actions with the same name (e.g. multiple constructors for the same action)
					 * we first try to find the one with the same amount of parameters. If this fails we return the
					 * first one, it will be updated later.
					 */
					List<ActionDefinition> actionDefinitions = platformDefinition.getActions(actionName);
					if (!actionDefinitions.isEmpty()) {
						if (actionDefinitions.size() > 1) {
							for (ActionDefinition actionDefinition : actionDefinitions) {
								if (actionDefinition.getParameters().size() == context.getValues().size()) {
									return Arrays.asList(actionDefinition);
								}
							}
						}
						return Arrays.asList(actionDefinitions.get(0));
					}
				}
			}
			return Collections.emptyList();
		} else {
			return super.getLinkedObjects(context, ref, node);
		}
	}

	private List<EObject> getLinkedObjectsForParameterValue(ParameterValue context, EReference ref, INode node) {
		if (ref.equals(ExecutionPackage.eINSTANCE.getParameterValue_Parameter())) {
			/*
			 * Trying to retrieve the Parameter of the containing Action
			 */
			ActionInstance actionInstance = (ActionInstance) context.eContainer();
			ActionDefinition actionDefinition = actionInstance.getAction();
			if (isNull(actionDefinition)) {
				/*
				 * TODO We should reload all the actions if this is not set
				 */
				System.out.println("Cannot retrieve the Action associated to " + actionInstance);
			}
			/*
			 * First look for the parameters in the defined containing Action. For platform containing multiple Actions
			 * with the same name (i.e. same XatkitAction but different constructors) this iteration can fail, because
			 * the inferred Action was not right.
			 */
			for (Parameter p : actionDefinition.getParameters()) {
				System.out.println("comparing Parameter " + p.getKey());
				System.out.println("Node text: " + node.getText());
				if (p.getKey().equals(node.getText())) {
					return Arrays.asList(p);
				}
			}
			/*
			 * Unable to find the Parameter in the inferred Action, trying to find alternative Actions with the same
			 * name and check their parameters. If such Action is found all the defined ParameterValues of this
			 * ActionInstance are processed and updated to fit the new parent Action.
			 */
			PlatformDefinition platformDefinition = (PlatformDefinition) actionDefinition.eContainer();
			if (isNull(platformDefinition)) {
				/*
				 * The platform may be null if there is an issue when loading the import. In that case we can ignore the
				 * linking, the model is false anyway
				 */
				return Collections.emptyList();
			}
			Parameter result = null;
			for (ActionDefinition platformAction : platformDefinition.getActions()) {
				if (!platformAction.equals(actionDefinition)
						&& platformAction.getName().equals(actionDefinition.getName())) {
					for (Parameter p : platformAction.getParameters()) {
						if (p.getKey().equals(node.getText())) {
							System.out.println("Found the parameter " + p.getKey() + " in a variant "
									+ actionDefinition.getName() + " returning it and updating the action instance");
							actionInstance.setAction(platformAction);
							result = p;
						}
					}
					if (nonNull(result)) {
						/*
						 * The Parameter was found in another Action, trying to reset all the ParameterValues' Parameter
						 * with the Action Parameters.
						 */
						ActionDefinition foundActionDefinition = (ActionDefinition) result.eContainer();
						boolean validAction = true;
						for (ParameterValue actionInstanceValue : actionInstance.getValues()) {
							Parameter actionInstanceParameter = actionInstanceValue.getParameter();
							boolean found = false;
							/*
							 * Check that each Parameter associated to the ActionInstance ParameterValues has a variant
							 * in the found action and update its reference. If all the Parameters have been updated the
							 * found Action variant is returned. Otherwise the loop searches for another Action variant.
							 */
							for (Parameter foundActionParameter : foundActionDefinition.getParameters()) {
								if (foundActionParameter.getKey().equals(actionInstanceParameter.getKey())) {
									actionInstanceValue.setParameter(foundActionParameter);
									found = true;
								}
							}
							validAction &= found;
						}
						if (validAction) {
							return Arrays.asList(result);
						}
					}
				}
			}
			return Collections.emptyList();
		} else {
			return super.getLinkedObjects(context, ref, node);
		}
	}

	private QualifiedName getQualifiedName(String from) {
		String trimmed = from.trim();
		String[] splitted = trimmed.split("\\.");
		if (splitted.length != 2) {
			/*
			 * We don't handle qualified name that contain multiple or no qualifier.
			 */
			System.out.println(
					MessageFormat.format("Cannot compute a qualified name from the provided String {0}", from));
			return null;
		} else {
			return new QualifiedName(splitted[0], splitted[1]);
		}
	}
}
