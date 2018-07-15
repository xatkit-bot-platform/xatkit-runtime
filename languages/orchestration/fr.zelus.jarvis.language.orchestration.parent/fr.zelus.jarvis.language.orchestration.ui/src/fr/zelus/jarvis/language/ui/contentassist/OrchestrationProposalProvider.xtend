/*
 * ge
 * nerated by Xtext 2.12.0
 */
package fr.zelus.jarvis.language.ui.contentassist

import org.eclipse.emf.ecore.EObject
import org.eclipse.xtext.RuleCall
import org.eclipse.xtext.ui.editor.contentassist.ContentAssistContext
import org.eclipse.xtext.ui.editor.contentassist.ICompletionProposalAcceptor
import org.eclipse.xtext.Assignment
import fr.zelus.jarvis.language.util.ModuleRegistry
import fr.zelus.jarvis.orchestration.OrchestrationModel

/**
 * See https://www.eclipse.org/Xtext/documentation/304_ide_concepts.html#content-assist
 * on how to customize the content assistant.
 */
class OrchestrationProposalProvider extends AbstractOrchestrationProposalProvider {

	override completeOrchestrationModel_EventProviderDefinitions(EObject model, Assignment assignment,
		ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		var modules = ModuleRegistry.instance.loadOrchestrationModelModules(model as OrchestrationModel)
		modules.map[m|m.eventProviderDefinitions.map[i|i.name]].flatten.forEach [ iName |
			acceptor.accept(createCompletionProposal(iName, context))
		]
		super.completeOrchestrationModel_EventProviderDefinitions(model, assignment, context, acceptor)
	}

	override completeOrchestrationLink_Event(EObject model, Assignment assignment, ContentAssistContext context,
		ICompletionProposalAcceptor acceptor) {
		var modules = ModuleRegistry.instance.loadOrchestrationModelModules(model.eContainer as OrchestrationModel)
		modules.map[m|m.intentDefinitions.map[i|i.name]].flatten.forEach [ iName |
			acceptor.accept(createCompletionProposal(iName, context))
		]
		modules.map[m|m.eventProviderDefinitions.map[e|e.eventDefinitions.map[ed|ed.name]].flatten].flatten.forEach [ edName |
			acceptor.accept(createCompletionProposal(edName, context))
		]
		super.completeOrchestrationLink_Event(model, assignment, context, acceptor)
	}

	override completeActionInstance_Action(EObject model, Assignment assignment, ContentAssistContext context,
		ICompletionProposalAcceptor acceptor) {
		println("completion")
		println(model)
		var modules = ModuleRegistry.instance.loadOrchestrationModelModules(model.eContainer as OrchestrationModel)
		modules.map[m|m.actions].flatten.forEach [ a |
			var parameterString = ""
			if(!a.parameters.empty) {
				parameterString += '('
				parameterString += a.parameters.map[p|p.key + " : \"\""].join(", ")
				parameterString += ')'
			}
			acceptor.accept(createCompletionProposal(a.name + parameterString, context))
		]
		super.completeActionInstance_Action(model, assignment, context, acceptor)
	}

}
