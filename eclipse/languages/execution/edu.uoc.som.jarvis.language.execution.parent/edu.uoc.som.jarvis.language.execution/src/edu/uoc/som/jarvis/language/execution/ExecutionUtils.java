package edu.uoc.som.jarvis.language.execution;

import static java.util.Objects.nonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.eclipse.emf.ecore.EObject;

import edu.uoc.som.jarvis.execution.ExecutionModel;
import edu.uoc.som.jarvis.intent.EventDefinition;
import edu.uoc.som.jarvis.intent.Library;
import edu.uoc.som.jarvis.platform.EventProviderDefinition;
import edu.uoc.som.jarvis.platform.PlatformDefinition;
import edu.uoc.som.jarvis.utils.ImportRegistry;

/**
 * A set of utility methods to manipulate Execution models.
 */
public class ExecutionUtils {

	/**
	 * Returns the {@link ExecutionModel} containing the provided {@code element}.
	 * <p>
	 * This method returns the first {@link ExecutionModel} instance in the provided {@code element}'s
	 * {@code eContainer} hierarchy.
	 * 
	 * @param element the {@link EObject} to retrieve the containing {@link ExecutionModel} from
	 * @return the containing {@link ExecutionModel} if it exists, {@code null} otherwise
	 */
	public static ExecutionModel getContainingExecutionModel(EObject element) {
		EObject currentObject = element;
		while (nonNull(currentObject)) {
			currentObject = currentObject.eContainer();
			if (currentObject instanceof ExecutionModel) {
				return (ExecutionModel) currentObject;
			}
		}
		return null;
	}

	public static Collection<EventDefinition> getEventDefinitionsFromImports(ExecutionModel executionModel) {
		Collection<EventDefinition> result = getEventDefinitionsFromImportedLibraries(executionModel);
		result.addAll(getEventDefinitionsFromImportedPlatforms(executionModel));
		return result;
	}
	
	public static EventDefinition getEventDefinitionFromImportedLibraries(ExecutionModel executionModel,
			String eventDefinitionName) {
		Optional<EventDefinition> result = getEventDefinitionsFromImportedLibraries(executionModel).stream().filter(e -> e.getName().equals(eventDefinitionName)).findAny();
		return result.orElseGet(() -> null);
	}

	public static EventDefinition getEventDefinitionFromImportedPlatforms(ExecutionModel executionModel,
			String eventDefinitionName) {
		Optional<EventDefinition> result = getEventDefinitionsFromImportedPlatforms(executionModel).stream().filter(e -> e.getName().equals(eventDefinitionName)).findAny();
		return result.orElseGet(() -> null);
	}

	public static Collection<EventDefinition> getEventDefinitionsFromImportedLibraries(ExecutionModel executionModel) {
		List<EventDefinition> eventDefinitions = new ArrayList<>();
		Collection<Library> libraries = ImportRegistry.getInstance().getImportedLibraries(executionModel);
		for (Library library : libraries) {
			eventDefinitions.addAll(library.getEventDefinitions());
		}
		return eventDefinitions;
	}

	public static Collection<EventDefinition> getEventDefinitionsFromImportedPlatforms(ExecutionModel executionModel) {
		List<EventDefinition> eventDefinitions = new ArrayList<>();
		Collection<PlatformDefinition> platformDefinitions = ImportRegistry.getInstance()
				.getImportedPlatforms(executionModel);
		for (PlatformDefinition platformDefinition : platformDefinitions) {
			for (EventProviderDefinition eventProviderDefinition : platformDefinition.getEventProviderDefinitions()) {
				eventDefinitions.addAll(eventProviderDefinition.getEventDefinitions());
			}
		}
		return eventDefinitions;
	}

	public static Collection<EventProviderDefinition> getEventProviderDefinitionsFromImportedPlatforms(
			ExecutionModel executionModel) {
		List<EventProviderDefinition> eventProviderDefinitions = new ArrayList<>();
		Collection<PlatformDefinition> platformDefinitions = ImportRegistry.getInstance()
				.getImportedPlatforms(executionModel);
		for (PlatformDefinition platformDefinition : platformDefinitions) {
			eventProviderDefinitions.addAll(platformDefinition.getEventProviderDefinitions());
		}
		return eventProviderDefinitions;
	}
}
