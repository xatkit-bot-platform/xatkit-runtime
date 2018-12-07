package edu.uoc.som.jarvis.language.execution;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.linking.impl.DefaultLinkingService;
import org.eclipse.xtext.linking.impl.IllegalNodeException;
import org.eclipse.xtext.nodemodel.INode;

import edu.uoc.som.jarvis.execution.ActionInstance;
import edu.uoc.som.jarvis.execution.ExecutionModel;
import edu.uoc.som.jarvis.execution.ExecutionPackage;
import edu.uoc.som.jarvis.execution.ExecutionRule;
import edu.uoc.som.jarvis.execution.ParameterValue;
import edu.uoc.som.jarvis.intent.EventDefinition;
import edu.uoc.som.jarvis.intent.Library;
import edu.uoc.som.jarvis.platform.ActionDefinition;
import edu.uoc.som.jarvis.platform.EventProviderDefinition;
import edu.uoc.som.jarvis.platform.Parameter;
import edu.uoc.som.jarvis.platform.PlatformDefinition;
import edu.uoc.som.jarvis.utils.ImportRegistry;

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
			if (ref.equals(ExecutionPackage.eINSTANCE.getExecutionModel_EventProviderDefinitions())) {
				/*
				 * Trying to retrieve an InputProvider from a loaded platform
				 */
				Collection<PlatformDefinition> platformDefinitions = ImportRegistry.getInstance()
						.getLoadedPlatforms((ExecutionModel) context);
				System.out.println("found " + platformDefinitions.size() + " platforms");
				for (PlatformDefinition platformDefinition : platformDefinitions) {
					for (EventProviderDefinition eventProviderDefinition : platformDefinition.getEventProviderDefinitions()) {
						System.out.println("comparing EventProvider " + eventProviderDefinition.getName());
						System.out.println("Node text: " + node.getText());
						if (eventProviderDefinition.getName().equals(node.getText())) {
							return Arrays.asList(eventProviderDefinition);
						}
					}
				}
				return Collections.emptyList();
			} else {
				return super.getLinkedObjects(context, ref, node);
			}
		} else if (context instanceof ExecutionRule) {
			if (ref.equals(ExecutionPackage.eINSTANCE.getExecutionRule_Event())) {
				/*
				 * Trying to retrieve an Event from a loaded Library
				 */
				Collection<Library> libraries = ImportRegistry.getInstance().getLoadedLibraries((ExecutionModel) context.eContainer());
				System.out.println("Found " + libraries.size() + "libraries");
				for(Library library : libraries) {
					for(EventDefinition eventDefinition : library.getEventDefinitions()) {
						System.out.println("Comparing Event " + eventDefinition.getName());
						System.out.println("Node text: " + node.getText());
						if(eventDefinition.getName().equals(node.getText())) {
							return Arrays.asList(eventDefinition);
						}
					}
				}
				/*
				 * Trying to retrieve an Event from a loaded platform
				 */
				Collection<PlatformDefinition> platformDefinitions = ImportRegistry.getInstance()
						.getLoadedPlatforms((ExecutionModel) context.eContainer());
				System.out.println("Found " + platformDefinitions.size() + "platforms");
				for (PlatformDefinition platformDefinition : platformDefinitions) {
					for (EventProviderDefinition eventProviderDefinition : platformDefinition.getEventProviderDefinitions()) {
						for (EventDefinition eventDefinition : eventProviderDefinition.getEventDefinitions()) {
							System.out.println("comparing Event " + eventDefinition.getName());
							System.out.println("Node text: " + node.getText());
							if (eventDefinition.getName().equals(node.getText())) {
								return Arrays.asList(eventDefinition);
							}
						}
					}
				}
				return Collections.emptyList();
			} else {
				return super.getLinkedObjects(context, ref, node);
			}
		} else if (context instanceof ActionInstance) {
			if (ref.equals(ExecutionPackage.eINSTANCE.getActionInstance_Action())) {
				/*
				 * Trying to retrieve an Action from a loaded platform
				 */
				ExecutionModel executionModel = null;
				EObject currentObject = context;
				while (isNull(executionModel)) {
					currentObject = currentObject.eContainer();
					if (currentObject instanceof ExecutionModel) {
						executionModel = (ExecutionModel) currentObject;
					}
				}
				String[] splittedActionName = node.getText().trim().split("\\.");
				if (splittedActionName.length != 2) {
					System.out.println(MessageFormat.format(
							"Cannot handle the action {0}, expecting a qualified name <Platform>.<Action>",
							node.getText().trim()));
					return Collections.emptyList();
				}
				String platformName = splittedActionName[0];
				String actionName = splittedActionName[1];
				Collection<PlatformDefinition> platformDefinitions = ImportRegistry.getInstance()
						.getLoadedPlatforms(executionModel);
				for (PlatformDefinition platformDefinition : platformDefinitions) {
					if(platformDefinition.getName().equals(platformName)) {
						for (ActionDefinition actionDefinition : platformDefinition.getActions()) {
							if (actionDefinition.getName().equals(actionName)) {
								return Arrays.asList(actionDefinition);
							}
						}
					}
				}
				return Collections.emptyList();
			} else {
				return super.getLinkedObjects(context, ref, node);
			}
		} else if (context instanceof ParameterValue) {
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
				 * First look for the parameters in the defined containing Action. For platform containing multiple
				 * Actions with the same name (i.e. same JarvisAction but different constructors) this iteration can
				 * fail, because the inferred Action was not right.
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
					 * The platform may be null if there is an issue when loading the import. In that case we can ignore
					 * the linking, the model is false anyway
					 */
					return Collections.emptyList();
				}
				Parameter result = null;
				for (ActionDefinition platformAction : platformDefinition.getActions()) {
					if (!platformAction.equals(actionDefinition) && platformAction.getName().equals(actionDefinition.getName())) {
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
							 * The Parameter was found in another Action, trying to reset all the ParameterValues'
							 * Parameter with the Action Parameters.
							 */
							ActionDefinition foundActionDefinition = (ActionDefinition) result.eContainer();
							boolean validAction = true;
							for (ParameterValue actionInstanceValue : actionInstance.getValues()) {
								Parameter actionInstanceParameter = actionInstanceValue.getParameter();
								boolean found = false;
								/*
								 * Check that each Parameter associated to the ActionInstance ParameterValues has a
								 * variant in the found action and update its reference. If all the Parameters have been
								 * updated the found Action variant is returned. Otherwise the loop searches for another
								 * Action variant.
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
		} else {
			return super.getLinkedObjects(context, ref, node);
		}
	}

}
