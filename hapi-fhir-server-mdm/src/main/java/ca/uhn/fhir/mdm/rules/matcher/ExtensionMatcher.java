package ca.uhn.fhir.mdm.rules.matcher;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.ExtensionUtil;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;

import java.util.List;

public class ExtensionMatcher implements IMdmFieldMatcher{
	@Override
	public boolean matches(FhirContext theFhirContext, IBase theLeftBase, IBase theRightBase, boolean theExact, String theIdentifierSystem) {
		List<? extends IBaseExtension<?, ?>> leftExtension = null;
		List<? extends IBaseExtension<?, ?>> rightExtension = null;
		if (theLeftBase instanceof IBaseHasExtensions && theRightBase instanceof IBaseHasExtensions){
			leftExtension = ((IBaseHasExtensions) theLeftBase).getExtension();
			rightExtension = ((IBaseHasExtensions) theRightBase).getExtension();
		}
		else{
			return false;
		}

		boolean match = false;

		for (IBaseExtension leftExtensionValue : leftExtension) {
			if (leftExtensionValue.getUrl().equals(theIdentifierSystem) || theIdentifierSystem == null) {
				for (IBaseExtension rightExtensionValue : rightExtension) {
					if (rightExtensionValue.getUrl().equals(theIdentifierSystem) || theIdentifierSystem == null) {
						match |= ExtensionUtil.equals(leftExtensionValue, rightExtensionValue);
					}
				}
			}
		}
		return match;
	}
}
