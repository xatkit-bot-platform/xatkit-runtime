package fr.zelus.jarvis.language;

import static java.util.Objects.isNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.linking.impl.DefaultLinkingService;
import org.eclipse.xtext.linking.impl.IllegalNodeException;
import org.eclipse.xtext.nodemodel.INode;

import fr.zelus.jarvis.intent.EventDefinition;
import fr.zelus.jarvis.intent.IntentDefinition;
import fr.zelus.jarvis.language.util.ModuleRegistry;
import fr.zelus.jarvis.module.Action;
import fr.zelus.jarvis.module.EventProviderDefinition;
import fr.zelus.jarvis.module.Module;
import fr.zelus.jarvis.module.Parameter;
import fr.zelus.jarvis.orchestration.ActionInstance;
import fr.zelus.jarvis.orchestration.OrchestrationLink;
import fr.zelus.jarvis.orchestration.OrchestrationModel;
import fr.zelus.jarvis.orchestration.OrchestrationPackage;
import fr.zelus.jarvis.orchestration.ParameterValue;

public class OrchestrationLinkingService extends DefaultLinkingService {

	public OrchestrationLinkingService() {
		super();
		System.out.println("Created OLS");
	}

	@Override
	public List<EObject> getLinkedObjects(EObject context, EReference ref, INode node) throws IllegalNodeException {
		System.out.println("Linking context: " + context);
		System.out.println("Linking reference: " + ref);
		if (context instanceof OrchestrationModel) {
			if (ref.equals(OrchestrationPackage.eINSTANCE.getOrchestrationModel_EventProviderDefinitions())) {
				/*
				 * Trying to retrieve an InputProvider from a loaded module
				 */
				try {
					Collection<Module> modules = ModuleRegistry.getInstance()
							.loadOrchestrationModelModules((OrchestrationModel) context);
					System.out.println("found " + modules.size() + " modules");
					for (Module module : modules) {
						for (EventProviderDefinition eventProviderDefinition : module.getEventProviderDefinitions()) {
							System.out.println("comparing EventProvider " + eventProviderDefinition.getName());
							System.out.println("Node text: " + node.getText());
							if (eventProviderDefinition.getName().equals(node.getText())) {
								return Arrays.asList(eventProviderDefinition);
							}
						}
					}
					return Collections.emptyList();
				} catch (IOException e) {
					System.out.println("Cannot retrieve the linked object");
					return Collections.emptyList();
				}
			} else {
				return super.getLinkedObjects(context, ref, node);
			}
		} else if (context instanceof OrchestrationLink) {
			if (ref.equals(OrchestrationPackage.eINSTANCE.getOrchestrationLink_Event())) {
				/*
				 * Trying to retrieve an Event from a loaded module
				 */
				try {
					Collection<Module> modules = ModuleRegistry.getInstance()
							.loadOrchestrationModelModules((OrchestrationModel) context.eContainer());
					System.out.println("found " + modules.size() + "modules");
					for (Module module : modules) {
						for (IntentDefinition intentDefinition : module.getIntentDefinitions()) {
							System.out.println("comparing Intent " + intentDefinition.getName());
							System.out.println("Node text: " + node.getText());
							if (intentDefinition.getName().equals(node.getText())) {
								return Arrays.asList(intentDefinition);
							}
						}
						for(EventProviderDefinition eventProviderDefinition : module.getEventProviderDefinitions()) {
							for(EventDefinition eventDefinition : eventProviderDefinition.getEventDefinitions()) {
								System.out.println("comparing Event " + eventDefinition.getName());
								System.out.println("Note text: " + node.getText());
								if(eventDefinition.getName().equals(node.getText())) {
									return Arrays.asList(eventDefinition);
								}
							}
						}
					}
					return Collections.emptyList();
				} catch (IOException e) {
					System.out.println("Cannot retrieve the linked object");
					return Collections.emptyList();
				}
			} else {
				return super.getLinkedObjects(context, ref, node);
			}
		} else if (context instanceof ActionInstance) {
			if (ref.equals(OrchestrationPackage.eINSTANCE.getActionInstance_Action())) {
				/*
				 * Trying to retrieve an Action from a loaded module
				 */
				try {
					Collection<Module> modules = ModuleRegistry.getInstance()
							.loadOrchestrationModelModules((OrchestrationModel) context.eContainer().eContainer());
					System.out.println("found " + modules.size() + " modules");
					for (Module module : modules) {
						for (Action action : module.getActions()) {
							System.out.println("comparing Action " + action.getName());
							System.out.println("Node text: " + node.getText());
							if (action.getName().equals(node.getText())) {
								return Arrays.asList(action);
							}
						}
					}
					return Collections.emptyList();
				} catch (IOException e) {
					System.out.println("Cannot retrieve the linked object");
					return Collections.emptyList();
				}
			} else {
				return super.getLinkedObjects(context, ref, node);
			}
		} else if (context instanceof ParameterValue) {
			if (ref.equals(OrchestrationPackage.eINSTANCE.getParameterValue_Parameter())) {
				/*
				 * Trying to retrieve the Parameter of the containing Action
				 */
				ActionInstance actionInstance = (ActionInstance) context.eContainer();
				Action action = actionInstance.getAction();
				if (isNull(action)) {
					/*
					 * TODO We should reload all the actions if this is not set
					 */
					System.out.println("Cannot retrieve the Action associated to " + actionInstance);
				}
				for (Parameter p : action.getParameters()) {
					System.out.println("comparing Parameter " + p.getKey());
					System.out.println("Node text: " + node.getText());
					if (p.getKey().equals(node.getText())) {
						return Arrays.asList(p);
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
