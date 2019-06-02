/*
 * ge
 * nerated by Xtext 2.12.0
 */
package edu.uoc.som.jarvis.language.execution.ui.contentassist

import org.eclipse.emf.ecore.EObject
import org.eclipse.xtext.ui.editor.contentassist.ContentAssistContext
import org.eclipse.xtext.ui.editor.contentassist.ICompletionProposalAcceptor
import org.eclipse.xtext.Assignment
import static java.util.Objects.isNull
import edu.uoc.som.jarvis.execution.ExecutionModel
import edu.uoc.som.jarvis.platform.PlatformDefinition
import edu.uoc.som.jarvis.utils.ImportRegistry

/**
 * See https://www.eclipse.org/Xtext/documentation/304_ide_concepts.html#content-assist
 * on how to customize the content assistant.
 */
class ExecutionProposalProvider extends AbstractExecutionProposalProvider {

	override completeExecutionModel_EventProviderDefinitions(EObject model, Assignment assignment,
		ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		var platforms = ImportRegistry.getInstance.getImportedPlatforms(model as ExecutionModel)
		for(PlatformDefinition platform : platforms) {
			platform.eventProviderDefinitions.map[i | i.name].forEach[iName |
				acceptor.accept(createCompletionProposal(platform.name + '.' + iName, context))
			]
		}
	}

	override completeExecutionRule_Event(EObject model, Assignment assignment, ContentAssistContext context,
		ICompletionProposalAcceptor acceptor) {
		/*
		 * Intents from libraries.
		 */
		var libraries = ImportRegistry.getInstance.getImportedLibraries(model.eContainer as ExecutionModel)
		libraries.map[m|m.eventDefinitions.map[e|e.name]].flatten.forEach[eName |
			acceptor.accept(createCompletionProposal(eName, context))
		]
		/*
		 * Intents stored in used EventProviders
		 */
		var executionModel = model.eContainer as ExecutionModel
		executionModel.eventProviderDefinitions.map[e | e.eventDefinitions.map[ed | ed.name]].flatten.forEach[
			edName |
				acceptor.accept(createCompletionProposal(edName, context))
		];
		super.completeExecutionRule_Event(model, assignment, context, acceptor)
	}

	override completeActionInstance_Action(EObject model, Assignment assignment, ContentAssistContext context,
		ICompletionProposalAcceptor acceptor) {
		println("completion")
		println(model)
		/*
		 * Retrieve the ExecutionModel, it can be different than the direct parent in case of nested on error ActionInstances.
		 */
		var ExecutionModel executionModel = null
		var currentObject = model
		while(isNull(executionModel)) {
			currentObject = currentObject.eContainer
			if(currentObject instanceof ExecutionModel) {
				executionModel = currentObject
			}
		}
		val platforms = ImportRegistry.getInstance.getImportedPlatforms(executionModel)
		for(PlatformDefinition platform : platforms) {
			platform.actions.forEach[a | 
				var String prefix = platform.name + "."
				var parameterString = ""
				if(!a.parameters.empty) {
					parameterString += '('
					parameterString += a.parameters.map[p|p.key + " : \"\""].join(", ")
					parameterString += ')'
				}
				acceptor.accept(createCompletionProposal(prefix + a.name + parameterString, context))
			]
		}
	}

}
