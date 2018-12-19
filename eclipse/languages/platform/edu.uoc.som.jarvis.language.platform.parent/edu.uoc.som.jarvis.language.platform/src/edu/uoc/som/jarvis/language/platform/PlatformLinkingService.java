package edu.uoc.som.jarvis.language.platform;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.linking.impl.DefaultLinkingService;
import org.eclipse.xtext.linking.impl.IllegalNodeException;
import org.eclipse.xtext.nodemodel.INode;

import edu.uoc.som.jarvis.platform.PlatformDefinition;
import edu.uoc.som.jarvis.platform.PlatformPackage;
import edu.uoc.som.jarvis.utils.ImportRegistry;

public class PlatformLinkingService extends DefaultLinkingService {

	public PlatformLinkingService() {
		super();
		System.out.println("Created Platform Linking Service");
	}

	@Override
	public List<EObject> getLinkedObjects(EObject context, EReference ref, INode node) throws IllegalNodeException {
		if (context instanceof PlatformDefinition) {
			PlatformDefinition platformDefinition = (PlatformDefinition) context;
			if (ref.equals(PlatformPackage.eINSTANCE.getPlatformDefinition_Extends())) {
				Collection<PlatformDefinition> importedPlatformDefinitions = ImportRegistry.getInstance()
						.getImportedPlatforms(platformDefinition);
				System.out.println("Found " + importedPlatformDefinitions.size() + " platforms");
				for(PlatformDefinition importedPlatform : importedPlatformDefinitions) {
					if(importedPlatform.getName().equals(node.getText())) {
						return Arrays.asList(importedPlatform);
					}
				}
			}
		}
		return super.getLinkedObjects(context, ref, node);
	}

}
