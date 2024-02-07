package ca.uhn.fhir.rest.server;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.test.utilities.HttpClientExtension;
import ca.uhn.fhir.test.utilities.server.RestfulServerExtension;
import ca.uhn.fhir.util.TestUtil;
import ca.uhn.fhir.util.UrlUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PagingUsingNamedPagesR4Test {

	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(PagingUsingNamedPagesR4Test.class);
	private static final FhirContext ourCtx = FhirContext.forR4Cached();
	private static IBundleProvider ourNextBundleProvider;
	private IPagingProvider myPagingProvider;

	@RegisterExtension
	public RestfulServerExtension ourServer = new RestfulServerExtension(ourCtx)
		 .registerProvider(new DummyPatientResourceProvider())
		 .withPagingProvider(new FifoMemoryPagingProvider(100))
		 .setDefaultResponseEncoding(EncodingEnum.JSON)
		 .setDefaultPrettyPrint(false);

	@RegisterExtension
	private HttpClientExtension ourClient = new HttpClientExtension();

	@BeforeEach
	public void before() {
		myPagingProvider = mock(IPagingProvider.class);
		when(myPagingProvider.canStoreSearchResults()).thenReturn(true);
		ourServer.getRestfulServer().setPagingProvider(myPagingProvider);
		ourNextBundleProvider = null;
	}

	private List<IBaseResource> createPatients(int theLow, int theHigh) {
		List<IBaseResource> patients = new ArrayList<>();
		for (int id = theLow; id <= theHigh; id++) {
			Patient pt = new Patient();
			pt.setId("Patient/" + id);
			pt.addName().setFamily("FAM" + id);
			patients.add(pt);
		}
		return patients;
	}

	private Bundle executeAndReturnBundle(HttpGet httpGet, EncodingEnum theExpectEncoding) throws IOException {
		Bundle bundle;
		try (CloseableHttpResponse status = ourClient.execute(httpGet)) {
			String responseContent = IOUtils.toString(status.getEntity().getContent(), StandardCharsets.UTF_8);
			ourLog.info(responseContent);
			assertEquals(200, status.getStatusLine().getStatusCode());
			EncodingEnum ct = EncodingEnum.forContentType(status.getEntity().getContentType().getValue().replaceAll(";.*", "").trim());
			assertEquals(theExpectEncoding, ct);
			assert ct != null;
			bundle = ct.newParser(ourCtx).parseResource(Bundle.class, responseContent);
			assertEquals(10, bundle.getEntry().size());
		}
		return bundle;
	}

	@Test
	public void testPaging() throws Exception {

		List<IBaseResource> patients0 = createPatients(0, 9);
		BundleProviderWithNamedPages provider0 = new BundleProviderWithNamedPages(patients0, "SEARCHID0", "PAGEID0", 1000);
		provider0.setNextPageId("PAGEID1");
		when(myPagingProvider.retrieveResultList(any(), eq("SEARCHID0"), eq("PAGEID0"))).thenReturn(provider0);

		List<IBaseResource> patients1 = createPatients(10, 19);
		BundleProviderWithNamedPages provider1 = new BundleProviderWithNamedPages(patients1, "SEARCHID0", "PAGEID1", 1000);
		provider1.setPreviousPageId("PAGEID0");
		provider1.setNextPageId("PAGEID2");
		when(myPagingProvider.retrieveResultList(any(), eq("SEARCHID0"), eq("PAGEID1"))).thenReturn(provider1);

		List<IBaseResource> patients2 = createPatients(20, 29);
		BundleProviderWithNamedPages provider2 = new BundleProviderWithNamedPages(patients2, "SEARCHID0", "PAGEID2", 1000);
		provider2.setPreviousPageId("PAGEID1");
		when(myPagingProvider.retrieveResultList(any(), eq("SEARCHID0"), eq("PAGEID2"))).thenReturn(provider2);

		ourNextBundleProvider = provider0;

		HttpGet httpGet;
		String linkSelf;
		String linkNext;
		String linkPrev;
		Bundle bundle;

		// Initial search
		httpGet = new HttpGet(ourServer.getBaseUrl() + "/Patient?_format=xml");
		bundle = executeAndReturnBundle(httpGet, EncodingEnum.XML);
		linkSelf = bundle.getLink(Constants.LINK_SELF).getUrl();
		assertEquals(ourServer.getBaseUrl() + "/Patient?_format=xml", linkSelf);
		linkNext = bundle.getLink(Constants.LINK_NEXT).getUrl();
		assertEquals(ourServer.getBaseUrl() + "?_getpages=SEARCHID0&_pageId=PAGEID1&_format=xml&_bundletype=searchset", linkNext);
		assertNull(bundle.getLink(Constants.LINK_PREVIOUS));

		// Fetch the next page
		httpGet = new HttpGet(linkNext);
		bundle = executeAndReturnBundle(httpGet, EncodingEnum.XML);
		linkSelf = bundle.getLink(Constants.LINK_SELF).getUrl();
		assertEquals(ourServer.getBaseUrl() + "?_getpages=SEARCHID0&_pageId=PAGEID1&_format=xml&_bundletype=searchset", linkSelf);
		linkNext = bundle.getLink(Constants.LINK_NEXT).getUrl();
		assertEquals(ourServer.getBaseUrl() + "?_getpages=SEARCHID0&_pageId=PAGEID2&_format=xml&_bundletype=searchset", linkNext);
		linkPrev = bundle.getLink(Constants.LINK_PREVIOUS).getUrl();
		assertEquals(ourServer.getBaseUrl() + "?_getpages=SEARCHID0&_pageId=PAGEID0&_format=xml&_bundletype=searchset", linkPrev);

		// Fetch the next page
		httpGet = new HttpGet(linkNext);
		bundle = executeAndReturnBundle(httpGet, EncodingEnum.XML);
		linkSelf = bundle.getLink(Constants.LINK_SELF).getUrl();
		assertEquals(ourServer.getBaseUrl() + "?_getpages=SEARCHID0&_pageId=PAGEID2&_format=xml&_bundletype=searchset", linkSelf);
		assertNull(bundle.getLink(Constants.LINK_NEXT));
		linkPrev = bundle.getLink(Constants.LINK_PREVIOUS).getUrl();
		assertEquals(ourServer.getBaseUrl() + "?_getpages=SEARCHID0&_pageId=PAGEID1&_format=xml&_bundletype=searchset", linkPrev);
	}

	@Test
	public void testPagingLinkUnknownPage() throws Exception {

		when(myPagingProvider.retrieveResultList(any(), nullable(String.class), any())).thenReturn(null);
		when(myPagingProvider.retrieveResultList(any(), nullable(String.class), nullable(String.class))).thenReturn(null);

		// With ID
		HttpGet httpGet = new HttpGet(ourServer.getBaseUrl() + "?_getpages=SEARCHID0&_pageId=PAGEID0&_format=xml&_bundletype=FOO" + UrlUtil.escapeUrlParam("\""));
		try (CloseableHttpResponse status = ourClient.execute(httpGet)) {
			String responseContent = IOUtils.toString(status.getEntity().getContent(), StandardCharsets.UTF_8);
			ourLog.info(responseContent);
			assertThat(responseContent).doesNotContain("FOO\"");
			assertEquals(410, status.getStatusLine().getStatusCode());
		}

		// Without ID
		httpGet = new HttpGet(ourServer.getBaseUrl() + "?_getpages=SEARCHID0&_format=xml&_bundletype=FOO" + UrlUtil.escapeUrlParam("\""));
		try (CloseableHttpResponse status = ourClient.execute(httpGet)) {
			String responseContent = IOUtils.toString(status.getEntity().getContent(), StandardCharsets.UTF_8);
			ourLog.info(responseContent);
			assertThat(responseContent).doesNotContain("FOO\"");
			assertEquals(410, status.getStatusLine().getStatusCode());
		}

	}

	@Test
	public void testPagingLinksSanitizeBundleType() throws Exception {

		List<IBaseResource> patients0 = createPatients(0, 9);
		BundleProviderWithNamedPages provider0 = new BundleProviderWithNamedPages(patients0, "SEARCHID0", "PAGEID0", 1000);
		provider0.setNextPageId("PAGEID1");
		when(myPagingProvider.retrieveResultList(any(), eq("SEARCHID0"), eq("PAGEID0"))).thenReturn(provider0);

		// Initial search
		HttpGet httpGet = new HttpGet(ourServer.getBaseUrl() + "?_getpages=SEARCHID0&_pageId=PAGEID0&_format=xml&_bundletype=FOO" + UrlUtil.escapeUrlParam("\""));
		try (CloseableHttpResponse status = ourClient.execute(httpGet)) {
			String responseContent = IOUtils.toString(status.getEntity().getContent(), StandardCharsets.UTF_8);
			ourLog.info(responseContent);
			assertThat(responseContent).doesNotContain("FOO\"");
			assertEquals(200, status.getStatusLine().getStatusCode());
			EncodingEnum ct = EncodingEnum.forContentType(status.getEntity().getContentType().getValue().replaceAll(";.*", "").trim());
			assert ct != null;
			Bundle bundle = EncodingEnum.XML.newParser(ourCtx).parseResource(Bundle.class, responseContent);
			assertEquals(10, bundle.getEntry().size());
		}

	}

	@AfterAll
	public static void afterClassClearContext() throws Exception {
		TestUtil.randomizeLocaleAndTimezone();
	}

	private static class DummyPatientResourceProvider implements IResourceProvider {


		@Override
		public Class<? extends IBaseResource> getResourceType() {
			return Patient.class;
		}

		@SuppressWarnings("rawtypes")
		@Search()
		public IBundleProvider search() {
			IBundleProvider retVal = ourNextBundleProvider;
			Validate.notNull(retVal);
			ourNextBundleProvider = null;
			return retVal;
		}

	}


}
