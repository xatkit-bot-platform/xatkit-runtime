/*
 * ge
 * nerated by Xtext 2.12.0
 */
package com.xatkit.language.execution.ui.contentassist

import com.xatkit.execution.ExecutionModel
import com.xatkit.intent.EventDefinition
import com.xatkit.intent.Library
import com.xatkit.language.execution.ExecutionUtils
import com.xatkit.platform.PlatformDefinition
import com.xatkit.utils.ImportRegistry
import org.eclipse.emf.ecore.EObject
import org.eclipse.xtext.Assignment
import org.eclipse.xtext.ui.editor.contentassist.ContentAssistContext
import org.eclipse.xtext.ui.editor.contentassist.ICompletionProposalAcceptor

import static java.util.Objects.nonNull

/**
 * See https://www.eclipse.org/Xtext/documentation/304_ide_concepts.html#content-assist
 * on how to customize the content assistant.
 */
class ExecutionProposalProvider extends AbstractExecutionProposalProvider {

	override completeExecutionModel_EventProviderDefinitions(EObject model, Assignment assignment,
		ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		var platforms = ImportRegistry.getInstance.getImportedPlatforms(model as ExecutionModel)
		for (PlatformDefinition platform : platforms) {
			platform.eventProviderDefinitions.map[i|i.name].forEach [ iName |
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
		libraries.map[m|m.eventDefinitions.map[e|e.name]].flatten.forEach [ eName |
			acceptor.accept(createCompletionProposal(eName, context))
		]
		/*
		 * Intents stored in used EventProviders
		 */
		var executionModel = model.eContainer as ExecutionModel
		executionModel.eventProviderDefinitions.map[e|e.eventDefinitions.map[ed|ed.name]].flatten.forEach [ edName |
			acceptor.accept(createCompletionProposal(edName, context))
		];
		super.completeExecutionRule_Event(model, assignment, context, acceptor)
	}

	override completeActionInstance_Action(EObject model, Assignment assignment, ContentAssistContext context,
		ICompletionProposalAcceptor acceptor) {
		/*
		 * Retrieve the ExecutionModel, it can be different than the direct parent in case of nested on error ActionInstances.
		 */
		var ExecutionModel executionModel = ExecutionUtils.getContainingExecutionModel(model);
		val platforms = ImportRegistry.getInstance.getImportedPlatforms(executionModel)
		for (PlatformDefinition platform : platforms) {
			platform.actions.forEach [ a |
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

	override completeContextAccess_ContextName(EObject model, Assignment assignment, ContentAssistContext context,
		ICompletionProposalAcceptor acceptor) {
		var ExecutionModel executionModel = ExecutionUtils.getContainingExecutionModel(model);
		ExecutionUtils.getOutContextsFromImports(executionModel).forEach[ outContext | 
			val EventDefinition containingEvent = ExecutionUtils.getContainingEventDefinition(outContext)
			val Library containingLibrary = ExecutionUtils.getContainingLibrary(containingEvent)
			var String containingName;
			if(nonNull(containingLibrary)) {
				containingName = containingLibrary.name + '.' + containingEvent.name
			} else {
				val PlatformDefinition containingPlatform = ExecutionUtils.getContainingPlatform(containingEvent)
				if(nonNull(containingPlatform)) {
					containingName = containingPlatform.name + '.' + containingEvent.name
				}
			}
			acceptor.accept(createCompletionProposal(outContext.name, outContext.name + '- from ' + containingName , null, context))
		]
	}

}
