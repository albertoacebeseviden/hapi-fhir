package ca.uhn.fhir.jpa.fql.parser;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class HfqlLexerTest {

	@Test
	public void testSimpleStatement() {
		String input = """
					from Patient
					select
					   name.given[0],
					   name.family
			""";
		List<String> allTokens = new HfqlLexer(input).allTokens();
		assertThat(allTokens, contains(
			"from", "Patient", "select", "name.given[0]", ",", "name.family"
		));
	}

	@Test
	public void testSelectStar() {
		String input = """
					from Patient
					select
					   *
			""";
		List<String> allTokens = new HfqlLexer(input).allTokens();
		assertThat(allTokens, contains(
			"from", "Patient", "select", "*"
		));
	}

	@Test
	public void testQuotedString() {
		String input = """
			from
			  Patient
			where
			  name.given = 'Foo \\' Chalmers'
			select
			  name.given[0],\s
			  name.family
			  """;
		List<String> allTokens = new HfqlLexer(input).allTokens();
		assertThat(allTokens, contains(
			"from", "Patient", "where",
			"name.given", "=", "'Foo ' Chalmers'",
			"select", "name.given[0]",
			",", "name.family"
		));

	}

	@Test
	public void testSearchParamWithQualifiers() {
		String input = """
			from
			  Patient
			search
			  _has:Observation:subject:device.identifier='1234-5'
			select
			  name.family
			  """;
		HfqlLexer hfqlLexer = new HfqlLexer(input);

		assertEquals("from", hfqlLexer.getNextToken(HfqlLexerOptions.HFQL_TOKEN).getToken());
		assertEquals("Patient", hfqlLexer.getNextToken(HfqlLexerOptions.HFQL_TOKEN).getToken());
		assertEquals("search", hfqlLexer.getNextToken(HfqlLexerOptions.HFQL_TOKEN).getToken());
		assertEquals("_has:Observation:subject:device.identifier", hfqlLexer.getNextToken(HfqlLexerOptions.SEARCH_PARAMETER_NAME).getToken());
		assertEquals("=", hfqlLexer.getNextToken(HfqlLexerOptions.HFQL_TOKEN).getToken());
		assertEquals("'1234-5'", hfqlLexer.getNextToken(HfqlLexerOptions.HFQL_TOKEN).getToken());
		assertEquals("select", hfqlLexer.getNextToken(HfqlLexerOptions.HFQL_TOKEN).getToken());
		assertEquals("name.family", hfqlLexer.getNextToken(HfqlLexerOptions.HFQL_TOKEN).getToken());

	}

	@Test
	public void testInList() {
		String input = """
			from StructureDefinition
			    where url in ('foo' | 'bar')
			select
			    Name: name,
			    URL: url
			""";
		List<String> allTokens = new HfqlLexer(input).allTokens();
		assertThat(allTokens, contains(
			"from", "StructureDefinition", "where",
			"url", "in", "(", "'foo'", "|", "'bar'", ")",
			"select",
			"Name", ":", "name", ",",
			"URL", ":", "url"
		));
	}

	@Test
	public void testFhirPathSelector() {
		String input = """
					from Patient
					select 
						( Observation.value.ofType ( Quantity ) ).unit,
						name.family.length()
			""";
		HfqlLexer lexer = new HfqlLexer(input);
		assertEquals("from", lexer.getNextToken().getToken());
		assertEquals("Patient", lexer.getNextToken().getToken());
		assertEquals("select", lexer.getNextToken().getToken());
		assertEquals("( Observation.value.ofType ( Quantity ) ).unit", lexer.getNextToken(HfqlLexerOptions.FHIRPATH_EXPRESSION).getToken());
		assertEquals(",", lexer.getNextToken().getToken());
		assertEquals("name.family.length()", lexer.getNextToken(HfqlLexerOptions.FHIRPATH_EXPRESSION).getToken());
	}


	@Test
	public void testOptionChangeIsRespected() {
		// Setup
		String input = """
					from Patient
					select 
						( Observation.value.ofType ( Quantity ) ).unit,
						name.family.length()
			""";
		HfqlLexer lexer = new HfqlLexer(input);
		assertEquals("from", lexer.getNextToken().getToken());
		assertEquals("Patient", lexer.getNextToken().getToken());
		assertEquals("select", lexer.getNextToken().getToken());

		// Test + Verify
		assertEquals("(", lexer.peekNextToken(HfqlLexerOptions.HFQL_TOKEN).getToken());
		assertEquals("( Observation.value.ofType ( Quantity ) ).unit", lexer.peekNextToken(HfqlLexerOptions.FHIRPATH_EXPRESSION).getToken());
		assertEquals("(", lexer.peekNextToken(HfqlLexerOptions.HFQL_TOKEN).getToken());
		assertEquals("( Observation.value.ofType ( Quantity ) ).unit", lexer.getNextToken(HfqlLexerOptions.FHIRPATH_EXPRESSION).getToken());
	}

}
