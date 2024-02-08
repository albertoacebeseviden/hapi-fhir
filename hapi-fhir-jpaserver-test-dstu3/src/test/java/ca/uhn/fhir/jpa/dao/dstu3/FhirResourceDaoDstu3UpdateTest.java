package ca.uhn.fhir.jpa.dao.dstu3;

import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.jpa.api.config.JpaStorageSettings;
import ca.uhn.fhir.jpa.model.dao.JpaPid;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.jpa.test.BaseJpaDstu3Test;
import ca.uhn.fhir.model.primitive.InstantDt;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceGoneException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.dstu3.model.CodeSystem;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Meta;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.UriType;
import org.hl7.fhir.instance.model.api.IIdType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.fail;

public class FhirResourceDaoDstu3UpdateTest extends BaseJpaDstu3Test {
	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(FhirResourceDaoDstu3UpdateTest.class);

	@AfterEach
	public void afterEach(){
		myStorageSettings.setResourceServerIdStrategy(JpaStorageSettings.IdStrategyEnum.SEQUENTIAL_NUMERIC);
	}

	@Test
	public void testReCreateMatchResource() {

		CodeSystem codeSystem = new CodeSystem();
		codeSystem.setUrl("http://foo");
		IIdType id = myCodeSystemDao.create(codeSystem, mySrd).getId().toUnqualifiedVersionless();

		myCodeSystemDao.delete(id, mySrd);

		codeSystem = new CodeSystem();
		codeSystem.setUrl("http://foo");
		IIdType id2 = myCodeSystemDao.update(codeSystem, "CodeSystem?url=http://foo", mySrd).getId();

		assertThat(id2.getIdPart()).isNotEqualTo(id.getIdPart());
		assertThat(id2.getVersionIdPart()).isEqualTo("1");
	}

	@Test
	public void testCreateAndUpdateWithoutRequest() {
		String methodName = "testUpdateByUrl";

		Patient p = new Patient();
		p.addIdentifier().setSystem("urn:system").setValue(methodName + "2");
		IIdType id = myPatientDao.create(p).getId().toUnqualified();

		p = new Patient();
		p.addIdentifier().setSystem("urn:system").setValue(methodName + "2");
		p.setActive(true);
		IIdType id2 = myPatientDao.create(p, "Patient?identifier=urn:system|" + methodName + "2").getId().toUnqualified();
		assertThat(id2.getValue()).isEqualTo(id.getValue());

		p = new Patient();
		p.setId(id);
		p.addIdentifier().setSystem("urn:system").setValue(methodName + "2");
		p.setActive(false);
		myPatientDao.update(p).getId();

		p.setActive(true);
		id2 = myPatientDao.update(p, "Patient?identifier=urn:system|" + methodName + "2").getId().toUnqualified();
		assertThat(id2.getIdPart()).isEqualTo(id.getIdPart());
		assertThat(id2.getVersionIdPart()).isEqualTo("3");

		Patient newPatient = myPatientDao.read(id);
		assertThat(newPatient.getIdElement().getVersionIdPart()).isEqualTo("1");

		newPatient = myPatientDao.read(id.toVersionless());
		assertThat(newPatient.getIdElement().getVersionIdPart()).isEqualTo("3");

		myPatientDao.delete(id.toVersionless());

		try {
			myPatientDao.read(id.toVersionless());
			fail("");
		} catch (ResourceGoneException e) {
			// nothing
		}

	}

	@Test
	public void testDuplicateProfilesIgnored() {
		String name = "testDuplicateProfilesIgnored";
		IIdType id;
		{
			Patient patient = new Patient();
			patient.addName().setFamily(name);

			List<IdType> tl = new ArrayList<IdType>();
			tl.add(new IdType("http://foo/bar"));
			tl.add(new IdType("http://foo/bar"));
			tl.add(new IdType("http://foo/bar"));
			patient.getMeta().getProfile().addAll(tl);

			id = myPatientDao.create(patient, mySrd).getId().toUnqualifiedVersionless();
		}

		// Do a read
		{
			Patient patient = myPatientDao.read(id, mySrd);
			List<UriType> tl = patient.getMeta().getProfile();
			assertThat(tl.size()).isEqualTo(1);
			assertThat(tl.get(0).getValue()).isEqualTo("http://foo/bar");
		}

	}

	@AfterEach
	public void afterResetDao() {
		myStorageSettings.setResourceMetaCountHardLimit(new JpaStorageSettings().getResourceMetaCountHardLimit());
	}
	
	@Test
	public void testHardMetaCapIsEnforcedOnCreate() {
		myStorageSettings.setResourceMetaCountHardLimit(3);

		IIdType id;
		{
			Patient patient = new Patient();
			patient.getMeta().addTag().setSystem("http://foo").setCode("1");
			patient.getMeta().addTag().setSystem("http://foo").setCode("2");
			patient.getMeta().addTag().setSystem("http://foo").setCode("3");
			patient.getMeta().addTag().setSystem("http://foo").setCode("4");
			patient.setActive(true);
			try {
				id = myPatientDao.create(patient, mySrd).getId().toUnqualifiedVersionless();
				fail("");
			} catch (UnprocessableEntityException e) {
				assertThat(e.getMessage()).isEqualTo(Msg.code(932) + "Resource contains 4 meta entries (tag/profile/security label), maximum is 3");
			}
		}
	}
	
	@Test
	public void testHardMetaCapIsEnforcedOnMetaAdd() {
		myStorageSettings.setResourceMetaCountHardLimit(3);

		IIdType id;
		{
			Patient patient = new Patient();
			patient.setActive(true);
			id = myPatientDao.create(patient, mySrd).getId().toUnqualifiedVersionless();
		}
		
		{
			Meta meta = new Meta();
			meta.addTag().setSystem("http://foo").setCode("1");
			meta.addTag().setSystem("http://foo").setCode("2");
			meta.addTag().setSystem("http://foo").setCode("3");
			meta.addTag().setSystem("http://foo").setCode("4");
			try {
				myPatientDao.metaAddOperation(id, meta, null);
				fail("");
			} catch (UnprocessableEntityException e) {
				assertThat(e.getMessage()).isEqualTo(Msg.code(932) + "Resource contains 4 meta entries (tag/profile/security label), maximum is 3");
			}

		}
	}
	
	@Test
	public void testDuplicateTagsOnAddTagsIgnored() {
		IIdType id;
		{
			Patient patient = new Patient();
			patient.setActive(true);
			id = myPatientDao.create(patient, mySrd).getId().toUnqualifiedVersionless();
		}

		Meta meta = new Meta();
		meta.addTag().setSystem("http://foo").setCode("bar").setDisplay("Val1");
		meta.addTag().setSystem("http://foo").setCode("bar").setDisplay("Val2");
		meta.addTag().setSystem("http://foo").setCode("bar").setDisplay("Val3");
		myPatientDao.metaAddOperation(id, meta, null);
		
		// Do a read
		{
			Patient patient = myPatientDao.read(id, mySrd);
			List<Coding> tl = patient.getMeta().getTag();
			assertThat(tl.size()).isEqualTo(1);
			assertThat(tl.get(0).getSystem()).isEqualTo("http://foo");
			assertThat(tl.get(0).getCode()).isEqualTo("bar");
		}

	}

	@Test
	public void testDuplicateTagsOnUpdateIgnored() {
		IIdType id;
		{
			Patient patient = new Patient();
			patient.setActive(true);
			id = myPatientDao.create(patient, mySrd).getId().toUnqualifiedVersionless();
		}

		{
			Patient patient = new Patient();
			patient.setId(id.getValue());
			patient.setActive(true);
			patient.getMeta().addTag().setSystem("http://foo").setCode("bar").setDisplay("Val1");
			patient.getMeta().addTag().setSystem("http://foo").setCode("bar").setDisplay("Val2");
			patient.getMeta().addTag().setSystem("http://foo").setCode("bar").setDisplay("Val3");
			myPatientDao.update(patient, mySrd).getId().toUnqualifiedVersionless();
		}
		
		// Do a read on second version
		{
			Patient patient = myPatientDao.read(id, mySrd);
			List<Coding> tl = patient.getMeta().getTag();
			assertThat(tl.size()).isEqualTo(1);
			assertThat(tl.get(0).getSystem()).isEqualTo("http://foo");
			assertThat(tl.get(0).getCode()).isEqualTo("bar");
		}

		// Do a read on first version
		{
			Patient patient = myPatientDao.read(id.withVersion("1"), mySrd);
			List<Coding> tl = patient.getMeta().getTag();
			assertThat(tl.size()).isEqualTo(0);
		}

		Meta meta = new Meta();
		meta.addTag().setSystem("http://foo").setCode("bar").setDisplay("Val1");
		meta.addTag().setSystem("http://foo").setCode("bar").setDisplay("Val2");
		meta.addTag().setSystem("http://foo").setCode("bar").setDisplay("Val3");
		myPatientDao.metaAddOperation(id.withVersion("1"), meta, null);

		// Do a read on first version
		{
			Patient patient = myPatientDao.read(id.withVersion("1"), mySrd);
			List<Coding> tl = patient.getMeta().getTag();
			assertThat(tl.size()).isEqualTo(1);
			assertThat(tl.get(0).getSystem()).isEqualTo("http://foo");
			assertThat(tl.get(0).getCode()).isEqualTo("bar");
		}

	}

	@Test
	public void testMultipleUpdatesWithNoChangesDoesNotResultInAnUpdateForDiscreteUpdates() {

		// First time
		Patient p = new Patient();
		p.setActive(true);
		p.setId("Patient/A");
		String id = myPatientDao.update(p).getId().getValue();
		assertThat(id).endsWith("Patient/A/_history/1");
		assertThat(myPatientDao.read(new IdType("Patient/A")).getIdElement().getVersionIdPart()).isEqualTo("1");

		// Second time should not result in an update
		p = new Patient();
		p.setActive(true);
		p.setId("Patient/A");
		id = myPatientDao.update(p).getId().getValue();
		assertThat(id).endsWith("Patient/A/_history/1");
		assertThat(myPatientDao.read(new IdType("Patient/A")).getIdElement().getVersionIdPart()).isEqualTo("1");

		// And third time should not result in an update
		p = new Patient();
		p.setActive(true);
		p.setId("Patient/A");
		id = myPatientDao.update(p).getId().getValue();
		assertThat(id).endsWith("Patient/A/_history/1");
		assertThat(myPatientDao.read(new IdType("Patient/A")).getIdElement().getVersionIdPart()).isEqualTo("1");

		myPatientDao.read(new IdType("Patient/A"));
		myPatientDao.read(new IdType("Patient/A/_history/1"));
		try {
			myPatientDao.read(new IdType("Patient/A/_history/2"));
			fail("");
		} catch (ResourceNotFoundException e) {
			// good
		}
		try {
			myPatientDao.read(new IdType("Patient/A/_history/3"));
			fail("");
		} catch (ResourceNotFoundException e) {
			// good
		}

		// Create one more
		p = new Patient();
		p.setActive(false);
		p.setId("Patient/A");
		id = myPatientDao.update(p).getId().getValue();
		assertThat(id).endsWith("Patient/A/_history/2");

	}



	@Test
	public void testUpdateByUrl() {
		String methodName = "testUpdateByUrl";

		Patient p = new Patient();
		p.addIdentifier().setSystem("urn:system").setValue(methodName);
		IIdType id = myPatientDao.create(p, mySrd).getId();
		ourLog.info("Created patient, got it: {}", id);

		p = new Patient();
		p.addIdentifier().setSystem("urn:system").setValue(methodName);
		p.addName().setFamily("Hello");
		p.setId("Patient/" + methodName);

		myPatientDao.update(p, "Patient?identifier=urn%3Asystem%7C" + methodName, mySrd);

		p = myPatientDao.read(id.toVersionless(), mySrd);
		assertThat(p.getIdElement().toVersionless().toString()).doesNotContain("test");
		assertThat(p.getIdElement().toVersionless()).isEqualTo(id.toVersionless());
		assertThat(p.getIdElement()).isNotEqualTo(id);
		assertThat(p.getIdElement().toString()).endsWith("/_history/2");

	}

	@Test
	public void testUpdateConditionalByLastUpdated() throws Exception {
		String methodName = "testUpdateByUrl";

		Patient p = new Patient();
		p.addIdentifier().setSystem("urn:system").setValue(methodName + "2");
		myPatientDao.create(p, mySrd).getId();

		InstantDt start = InstantDt.withCurrentTime();
		ourLog.info("First time: {}", start.getValueAsString());
		Thread.sleep(100);

		p = new Patient();
		p.addIdentifier().setSystem("urn:system").setValue(methodName);
		IIdType id = myPatientDao.create(p, mySrd).getId();
		ourLog.info("Created patient, got ID: {}", id);

		Thread.sleep(100);

		p = new Patient();
		p.addIdentifier().setSystem("urn:system").setValue(methodName);
		p.addName().setFamily("Hello");
		p.setId("Patient/" + methodName);

		String matchUrl = "Patient?_lastUpdated=gt" + start.getValueAsString();
		ourLog.info("URL is: {}", matchUrl);
		myPatientDao.update(p, matchUrl, mySrd);

		p = myPatientDao.read(id.toVersionless(), mySrd);
		assertThat(p.getIdElement().toVersionless().toString()).doesNotContain("test");
		assertThat(p.getIdElement().toVersionless()).isEqualTo(id.toVersionless());
		assertThat(p.getIdElement()).isNotEqualTo(id);
		assertThat(p.getIdElement().toString()).endsWith("/_history/2");

	}

	@Test
	public void testUpdateConditionalByLastUpdatedWithWrongTimezone() throws Exception {
		TimeZone def = TimeZone.getDefault();
		try {
			TimeZone.setDefault(TimeZone.getTimeZone("GMT-0:00"));
			String methodName = "testUpdateByUrl";

			Patient p = new Patient();
			p.addIdentifier().setSystem("urn:system").setValue(methodName + "2");
			myPatientDao.create(p, mySrd).getId();

			InstantDt start = InstantDt.withCurrentTime();
			Thread.sleep(100);

			p = new Patient();
			p.addIdentifier().setSystem("urn:system").setValue(methodName);
			IIdType id = myPatientDao.create(p, mySrd).getId();
			ourLog.info("Created patient, got it: {}", id);

			Thread.sleep(100);

			p = new Patient();
			p.addIdentifier().setSystem("urn:system").setValue(methodName);
			p.addName().setFamily("Hello");
			p.setId("Patient/" + methodName);

			myPatientDao.update(p, "Patient?_lastUpdated=gt" + start.getValueAsString(), mySrd);

			p = myPatientDao.read(id.toVersionless(), mySrd);
			assertThat(p.getIdElement().toVersionless().toString()).doesNotContain("test");
			assertThat(p.getIdElement().toVersionless()).isEqualTo(id.toVersionless());
			assertThat(p.getIdElement()).isNotEqualTo(id);
			assertThat(p.getIdElement().toString()).endsWith("/_history/2");
		} finally {
			TimeZone.setDefault(def);
		}
	}

	@Test
	public void testUpdateCreatesTextualIdIfItDoesntAlreadyExist() {
		Patient p = new Patient();
		String methodName = "testUpdateCreatesTextualIdIfItDoesntAlreadyExist";
		p.addIdentifier().setSystem("urn:system").setValue(methodName);
		p.addName().setFamily("Hello");
		p.setId("Patient/" + methodName);

		IIdType id = myPatientDao.update(p, mySrd).getId();
		assertThat(id.toUnqualifiedVersionless().getValue()).isEqualTo("Patient/" + methodName);

		p = myPatientDao.read(id, mySrd);
		assertThat(p.getIdentifier().get(0).getValue()).isEqualTo(methodName);
	}

	@Test
	public void testUpdateDoesntFailForUnknownIdWithNumberThenText() {
		String methodName = "testUpdateFailsForUnknownIdWithNumberThenText";
		Patient p = new Patient();
		p.setId("0" + methodName);
		p.addName().setFamily(methodName);

		myPatientDao.update(p, mySrd);
	}

	@Test
	@Disabled
	public void testUpdateIgnoresIdenticalVersions() {
		String methodName = "testUpdateIgnoresIdenticalVersions";

		Patient p1 = new Patient();
		p1.addIdentifier().setSystem("urn:system").setValue(methodName);
		p1.addName().setFamily("Tester").addGiven(methodName);
		IIdType p1id = myPatientDao.create(p1, mySrd).getId();

		IIdType p1id2 = myPatientDao.update(p1, mySrd).getId();
		assertThat(p1id2.getValue()).isEqualTo(p1id.getValue());

		p1.addName().addGiven("NewGiven");
		IIdType p1id3 = myPatientDao.update(p1, mySrd).getId();
		assertThat(p1id3.getValue()).isNotEqualTo(p1id.getValue());

	}

	@Test
	public void testUpdateMaintainsSearchParams() {
		Patient p1 = new Patient();
		p1.addIdentifier().setSystem("urn:system").setValue("testUpdateMaintainsSearchParamsDstu2AAA");
		p1.addName().setFamily("Tester").addGiven("testUpdateMaintainsSearchParamsDstu2AAA");
		IIdType p1id = myPatientDao.create(p1, mySrd).getId();

		Patient p2 = new Patient();
		p2.addIdentifier().setSystem("urn:system").setValue("testUpdateMaintainsSearchParamsDstu2BBB");
		p2.addName().setFamily("Tester").addGiven("testUpdateMaintainsSearchParamsDstu2BBB");
		myPatientDao.create(p2, mySrd).getId();

		List<JpaPid> ids = myPatientDao.searchForIds(new SearchParameterMap(Patient.SP_GIVEN, new StringParam("testUpdateMaintainsSearchParamsDstu2AAA")), null);
		assertThat(ids.size()).isEqualTo(1);
		assertThat(JpaPid.toLongList(ids)).containsExactly(p1id.getIdPartAsLong());

		// Update the name
		p1.getName().get(0).getGiven().get(0).setValue("testUpdateMaintainsSearchParamsDstu2BBB");
		MethodOutcome update2 = myPatientDao.update(p1, mySrd);
		IIdType p1id2 = update2.getId();

		ids = myPatientDao.searchForIds(new SearchParameterMap(Patient.SP_GIVEN, new StringParam("testUpdateMaintainsSearchParamsDstu2AAA")), null);
		assertThat(ids.size()).isEqualTo(0);

		ids = myPatientDao.searchForIds(new SearchParameterMap(Patient.SP_GIVEN, new StringParam("testUpdateMaintainsSearchParamsDstu2BBB")), null);
		assertThat(ids.size()).isEqualTo(2);

		// Make sure vreads work
		p1 = myPatientDao.read(p1id, mySrd);
		assertThat(p1.getName().get(0).getGivenAsSingleString()).isEqualTo("testUpdateMaintainsSearchParamsDstu2AAA");

		p1 = myPatientDao.read(p1id2, mySrd);
		assertThat(p1.getName().get(0).getGivenAsSingleString()).isEqualTo("testUpdateMaintainsSearchParamsDstu2BBB");

	}

	/**
	 * Per the spec, update should preserve tags and security labels but not profiles
	 */
	@Test
	public void testUpdateMaintainsTagsAndSecurityLabels() {
		String methodName = "testUpdateMaintainsTagsAndSecurityLabels";

		IIdType p1id;
		{
			Patient p1 = new Patient();
			p1.addName().setFamily(methodName);

			p1.getMeta().addTag("tag_scheme1", "tag_term1", null);
			p1.getMeta().addSecurity("sec_scheme1", "sec_term1", null);
			p1.getMeta().addProfile("http://foo1");

			p1id = myPatientDao.create(p1, mySrd).getId().toUnqualifiedVersionless();
		}
		{
			Patient p1 = new Patient();
			p1.setId(p1id.getValue());
			p1.addName().setFamily(methodName);

			p1.getMeta().addTag("tag_scheme2", "tag_term2", null);
			p1.getMeta().addSecurity("sec_scheme2", "sec_term2", null);
			p1.getMeta().addProfile("http://foo2");

			myPatientDao.update(p1, mySrd);
		}
		{
			Patient p1 = myPatientDao.read(p1id, mySrd);
			List<Coding> tagList = p1.getMeta().getTag();
			Set<String> secListValues = new HashSet<>();
			for (Coding next : tagList) {
				secListValues.add(next.getSystemElement().getValue() + "|" + next.getCodeElement().getValue());
			}
			assertThat(secListValues).containsExactlyInAnyOrder("tag_scheme1|tag_term1", "tag_scheme2|tag_term2");
			List<Coding> secList = p1.getMeta().getSecurity();
			secListValues = new HashSet<>();
			for (Coding next : secList) {
				secListValues.add(next.getSystemElement().getValue() + "|" + next.getCodeElement().getValue());
			}
			assertThat(secListValues).containsExactlyInAnyOrder("sec_scheme1|sec_term1", "sec_scheme2|sec_term2");
			List<UriType> profileList = p1.getMeta().getProfile();
			assertThat(profileList.size()).isEqualTo(1);
			assertThat(profileList.get(0).getValueAsString()).isEqualTo("http://foo2"); // no foo1
		}
	}

	@Test
	public void testUpdateModifiesProfiles() {
		String name = "testUpdateModifiesProfiles";
		IIdType id;
		{
			Patient patient = new Patient();
			patient.addName().setFamily(name);

			List<IdType> tl = new ArrayList<>();
			tl.add(new IdType("http://foo/bar"));
			patient.getMeta().getProfile().addAll(tl);

			id = myPatientDao.create(patient, mySrd).getId().toUnqualifiedVersionless();
		}

		// Do a read
		{
			Patient patient = myPatientDao.read(id, mySrd);
			List<UriType> tl = patient.getMeta().getProfile();
			assertThat(tl.size()).isEqualTo(1);
			assertThat(tl.get(0).getValue()).isEqualTo("http://foo/bar");
		}

		// Update
		{
			Patient patient = new Patient();
			patient.setId(id);
			patient.addName().setFamily(name);

			List<IdType> tl = new ArrayList<>();
			tl.add(new IdType("http://foo/baz"));
			patient.getMeta().getProfile().clear();
			patient.getMeta().getProfile().addAll(tl);

			id = myPatientDao.update(patient, mySrd).getId().toUnqualifiedVersionless();
		}

		// Do a read
		{
			Patient patient = myPatientDao.read(id, mySrd);
			List<UriType> tl = patient.getMeta().getProfile();
			assertThat(tl.size()).isEqualTo(1);
			assertThat(tl.get(0).getValue()).isEqualTo("http://foo/baz");
		}

	}

	@Test
	public void testUpdateRejectsInvalidTypes() {
		Patient p1 = new Patient();
		p1.addIdentifier().setSystem("urn:system").setValue("testUpdateRejectsInvalidTypes");
		p1.addName().setFamily("Tester").addGiven("testUpdateRejectsInvalidTypes");
		IIdType p1id = myPatientDao.create(p1, mySrd).getId();

		Organization p2 = new Organization();
		p2.getNameElement().setValue("testUpdateRejectsInvalidTypes");
		try {
			p2.setId(new IdType("Organization/" + p1id.getIdPart()));
			myOrganizationDao.update(p2, mySrd);
			fail("");
		} catch (UnprocessableEntityException e) {
			assertThat(e.getMessage()).isEqualTo(Msg.code(930) + "Existing resource ID[Patient/" + p1id.getIdPartAsLong() + "] is of type[Patient] - Cannot update with [Organization]");
		}

		try {
			p2.setId(new IdType("Patient/" + p1id.getIdPart()));
			myOrganizationDao.update(p2, mySrd);
			fail("");
		} catch (InvalidRequestException e) {
			assertThat(e.getMessage()).isEqualTo(Msg.code(996) + "Incorrect resource type (Patient) for this DAO, wanted: Organization");
		}

	}

	@Test
	public void testUpdateUnknownNumericIdFails() {
		Patient p = new Patient();
		p.addIdentifier().setSystem("urn:system").setValue("testCreateNumericIdFails");
		p.addName().setFamily("Hello");
		p.setId("Patient/9999999999999999");
		try {
			myPatientDao.update(p, mySrd);
			fail("");
		} catch (InvalidRequestException e) {
			assertThat(e.getMessage()).contains("Can not create resource with ID[9999999999999999], no resource with this ID exists and clients may only");
		}
	}

	@Test
	public void testUpdateWithInvalidIdFails() {
		Patient p = new Patient();
		p.addIdentifier().setSystem("urn:system").setValue("testCreateNumericIdFails");
		p.addName().setFamily("Hello");
		p.setId("Patient/123:456");
		try {
			myPatientDao.update(p, mySrd);
			fail("");
		} catch (InvalidRequestException e) {
			assertThat(e.getMessage()).isEqualTo(Msg.code(521) + "Can not process entity with ID[123:456], this is not a valid FHIR ID");
		}
	}

	@Test
	public void testUpdateWithNoChangeDetectionDisabledUpdateUnchanged() {
		myStorageSettings.setSuppressUpdatesWithNoChange(false);

		String name = "testUpdateUnchanged";
		IIdType id1, id2;
		{
			Patient patient = new Patient();
			patient.addName().setFamily(name);
			id1 = myPatientDao.create(patient, mySrd).getId().toUnqualified();
		}

		// Update
		{
			Patient patient = new Patient();
			patient.setId(id1);
			patient.addName().setFamily(name);
			id2 = myPatientDao.update(patient, mySrd).getId().toUnqualified();
		}

		assertThat(id2.getValue()).isNotEqualTo(id1.getValue());
	}

	@Test
	public void testUpdateWithNoChangeDetectionUpdateTagAdded() {
		String name = "testUpdateUnchanged";
		IIdType id1, id2;
		{
			Patient patient = new Patient();
			patient.addName().setFamily(name);
			id1 = myPatientDao.create(patient, mySrd).getId().toUnqualified();
		}

		// Update
		{
			Patient patient = new Patient();
			patient.getMeta().addTag().setCode("CODE");
			patient.setId(id1);
			patient.addName().setFamily(name);
			id2 = myPatientDao.update(patient, mySrd).getId().toUnqualified();
		}

		assertThat(id2.getValue()).isNotEqualTo(id1.getValue());
	}

	@Test
	public void testUpdateWithNoChangeDetectionUpdateTagMetaRemoved() {
		String name = "testUpdateUnchanged";
		IIdType id1, id2;
		{
			Patient patient = new Patient();
			patient.getMeta().addTag().setCode("CODE");
			patient.addName().setFamily(name);
			id1 = myPatientDao.create(patient, mySrd).getId().toUnqualified();
		}

		Meta meta = new Meta();
		meta.addTag().setCode("CODE");
		myPatientDao.metaDeleteOperation(id1, meta, null);

		meta = myPatientDao.metaGetOperation(Meta.class, id1, null);
		assertThat(meta.getTag().size()).isEqualTo(0);

		// Update
		{
			Patient patient = new Patient();
			patient.setId(id1);
			patient.addName().setFamily(name);
			id2 = myPatientDao.update(patient, mySrd).getId().toUnqualified();
		}

		assertThat(id2.getValue()).isEqualTo(id1.getValue());

		meta = myPatientDao.metaGetOperation(Meta.class, id2, null);
		assertThat(meta.getTag().size()).isEqualTo(0);
	}

	@Test
	public void testUpdateWithNoChangeDetectionUpdateTagNoChange() {
		String name = "testUpdateUnchanged";
		IIdType id1, id2;
		{
			Patient patient = new Patient();
			patient.addName().setFamily(name);
			id1 = myPatientDao.create(patient, mySrd).getId().toUnqualified();
		}

		// Add tag
		Meta meta = new Meta();
		meta.addTag().setCode("CODE");
		myPatientDao.metaAddOperation(id1, meta, null);

		// Update with tag
		{
			Patient patient = new Patient();
			patient.getMeta().addTag().setCode("CODE");
			patient.setId(id1);
			patient.addName().setFamily(name);
			id2 = myPatientDao.update(patient, mySrd).getId().toUnqualified();
		}

		assertThat(id2.getValue()).isEqualTo(id1.getValue());

		meta = myPatientDao.metaGetOperation(Meta.class, id2, null);
		assertThat(meta.getTag().size()).isEqualTo(1);
		assertThat(meta.getTag().get(0).getCode()).isEqualTo("CODE");
	}

	@Test
	public void testUpdateWithNoChangeDetectionUpdateTagRemoved() {
		String name = "testUpdateUnchanged";
		IIdType id1, id2;
		{
			Patient patient = new Patient();
			patient.getMeta().addTag().setCode("CODE");
			patient.addName().setFamily(name);
			id1 = myPatientDao.create(patient, mySrd).getId().toUnqualified();
		}

		// Update
		{
			Patient patient = new Patient();
			patient.setId(id1);
			patient.addName().setFamily(name);
			id2 = myPatientDao.update(patient, mySrd).getId().toUnqualified();
		}

		assertThat(id2.getValue()).isEqualTo(id1.getValue());

		Meta meta = myPatientDao.metaGetOperation(Meta.class, id2, null);
		assertThat(meta.getTag().size()).isEqualTo(1);
		assertThat(meta.getTag().get(0).getCode()).isEqualTo("CODE");
	}

	@Test
	public void testUpdateWithNoChangeDetectionUpdateUnchanged() {
		String name = "testUpdateUnchanged";
		IIdType id1, id2;
		{
			Patient patient = new Patient();
			patient.addName().setFamily(name);
			id1 = myPatientDao.create(patient, mySrd).getId().toUnqualified();
		}

		// Update
		{
			Patient patient = new Patient();
			patient.setId(id1);
			patient.addName().setFamily(name);
			id2 = myPatientDao.update(patient, mySrd).getId().toUnqualified();
		}

		assertThat(id2.getValue()).isEqualTo(id1.getValue());
	}

	@Test
	public void testUpdateWithNumericIdFails() {
		Patient p = new Patient();
		p.addIdentifier().setSystem("urn:system").setValue("testCreateNumericIdFails");
		p.addName().setFamily("Hello");
		p.setId("Patient/123");
		try {
			myPatientDao.update(p, mySrd);
			fail("");
		} catch (InvalidRequestException e) {
			assertThat(e.getMessage()).contains("clients may only assign IDs which contain at least one non-numeric");
		}
	}

	@Test
	public void testUpdateWithNumericThenTextIdSucceeds() {
		Patient p = new Patient();
		p.addIdentifier().setSystem("urn:system").setValue("testCreateNumericIdFails");
		p.addName().setFamily("Hello");
		p.setId("Patient/123abc");
		IIdType id = myPatientDao.update(p, mySrd).getId();
		assertThat(id.getIdPart()).isEqualTo("123abc");
		assertThat(id.getVersionIdPart()).isEqualTo("1");

		p = myPatientDao.read(id.toUnqualifiedVersionless(), mySrd);
		assertThat(p.getIdElement().toUnqualifiedVersionless().getValue()).isEqualTo("Patient/123abc");
		assertThat(p.getName().get(0).getFamily()).isEqualTo("Hello");

	}

	@Test
	void testCreateWithConditionalUpdate_withUuidAsServerResourceStrategyAndNoIdProvided_uuidAssignedAsResourceId() {
		// setup
		myStorageSettings.setResourceServerIdStrategy(JpaStorageSettings.IdStrategyEnum.UUID);
		Patient p = new Patient();
		p.addIdentifier().setSystem("http://my-lab-system").setValue("123");
		p.addName().setFamily("FAM");
		String theMatchUrl = "Patient?identifier=http://my-lab-system|123";

		// execute
		String result = myPatientDao.update(p, theMatchUrl, mySrd).getId().getIdPart();

		// verify
		try {
			UUID.fromString(result);
		} catch (IllegalArgumentException exception){
			fail("", "Result id is not a UUID. Instead, it was: " + result);
		}
	}

}
