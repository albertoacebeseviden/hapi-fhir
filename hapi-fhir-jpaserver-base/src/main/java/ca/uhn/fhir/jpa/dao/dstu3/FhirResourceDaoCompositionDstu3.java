package ca.uhn.fhir.jpa.dao.dstu3;

/*
 * #%L
 * HAPI FHIR JPA Server
 * %%
 * Copyright (C) 2014 - 2018 University Health Network
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

import ca.uhn.fhir.jpa.dao.IFhirResourceDaoComposition;
import ca.uhn.fhir.jpa.dao.ISearchParamRegistry;
import ca.uhn.fhir.jpa.dao.SearchParameterMap;
import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.StringParam;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Composition;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class FhirResourceDaoCompositionDstu3 extends FhirResourceDaoDstu3<Composition> implements IFhirResourceDaoComposition<Composition> {

	@Autowired
	private ISearchParamRegistry mySearchParamRegistry;

	@Override
	public Bundle getDocumentForComposition(HttpServletRequest theServletRequest, IIdType theId, IPrimitiveType<Integer> theCount, DateRangeParam theLastUpdate, SortSpec theSort, RequestDetails theRequestDetails) {
		SearchParameterMap paramMap = new SearchParameterMap();
		if (theCount != null) {
			paramMap.setCount(theCount.getValue());
		}

//		paramMap.setRevIncludes(Collections.singleton(IResource.INCLUDE_ALL.asRecursive()));
		paramMap.setIncludes(Collections.singleton(IResource.INCLUDE_ALL.asRecursive()));
//		paramMap.setEverythingMode(theId != null ? EverythingModeEnum.ENCOUNTER_INSTANCE : EverythingModeEnum.ENCOUNTER_TYPE);
		paramMap.setSort(theSort);
		paramMap.setLastUpdated(theLastUpdate);
		if (theId != null) {
			paramMap.add("_id", new StringParam(theId.getIdPart()));
		}
		IBundleProvider retVal = search(paramMap);
		List<IBaseResource> resourceList = retVal.getResources(0, retVal.size());

		Bundle bundle = new Bundle().setType(Bundle.BundleType.DOCUMENT);
		for (IBaseResource resource : resourceList)
			bundle.addEntry(new Bundle.BundleEntryComponent().setResource((Resource) resource));

		return bundle;
	}
}

