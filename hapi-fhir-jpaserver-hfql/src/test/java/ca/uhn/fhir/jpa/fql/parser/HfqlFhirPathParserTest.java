package ca.uhn.fhir.jpa.fql.parser;

import ca.uhn.fhir.context.BaseRuntimeElementDefinition;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.RuntimePrimitiveDatatypeDefinition;
import ca.uhn.fhir.jpa.fql.executor.HfqlDataTypeEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class HfqlFhirPathParserTest {

	@ParameterizedTest
	@CsvSource(value = {
		// Good
		"Patient     , Patient.name.family                               ,   STRING",
		"Patient     , Patient.name.given.getValue().is(System.string)   ,   STRING",
		"Patient     , Patient.identifier.where(system='foo').system     ,   STRING",
		"Observation , Observation.value.ofType(Quantity).value          ,   DECIMAL",
		"Patient     , name.family                                       ,   STRING",
		"Patient     , name.given.getValue().is(System.string)           ,   STRING",
		"Patient     , identifier.where(system='foo').system             ,   STRING",
		"Patient     , identifier[0].where(system='foo').system          ,   STRING",
		"Observation , value.ofType(Quantity).value                      ,   DECIMAL",
		"Patient     , Patient.meta.versionId.toInteger()                ,   INTEGER",
		// Bad
		"Patient     , Patient.identifier                                ,   ",
		"Patient     , foo                                               ,   ",
	})
	void testDetermineDatatypeForPath(String theResourceType, String theFhirPath, HfqlDataTypeEnum theExpectedType) {
		HfqlFhirPathParser svc = new HfqlFhirPathParser(FhirContext.forR4Cached());
		HfqlDataTypeEnum actual = svc.determineDatatypeForPath(theResourceType, theFhirPath);
		assertEquals(theExpectedType, actual);
	}


	@Test
	void testAllFhirDataTypesHaveMappings() {
		FhirContext ctx = FhirContext.forR5Cached();
		int foundCount = 0;
		for (BaseRuntimeElementDefinition<?> next : ctx.getElementDefinitions()) {
			if (next instanceof RuntimePrimitiveDatatypeDefinition) {
				assertNotNull(HfqlFhirPathParser.getHfqlDataTypeForFhirType(next.getName()), () -> "No mapping for type: " + next.getName());
				foundCount++;
			}
		}
		assertEquals(21, foundCount);
	}


}
