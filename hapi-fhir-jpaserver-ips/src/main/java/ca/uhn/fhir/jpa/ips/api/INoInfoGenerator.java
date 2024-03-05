/*-
 * #%L
 * HAPI FHIR JPA Server - International Patient Summary (IPS)
 * %%
 * Copyright (C) 2014 - 2024 Smile CDR, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package ca.uhn.fhir.jpa.ips.api;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

/**
 * This interface is invoked when a section has no resources found, and should generate
 * a "stub" resource explaining why. Typically this would be content such as "no information
 * is available for this section", and might indicate for example that the absence of
 * AllergyIntolerance resources only indicates that the allergy status is not known, not that
 * the patient has no allergies.
 */
public interface INoInfoGenerator {

	/**
	 * Generate an appropriate no-info resource. The resource does not need to have an ID populated,
	 * although it can if it is a resource found in the repository.
	 */
	IBaseResource generate(IIdType theSubjectId);
}