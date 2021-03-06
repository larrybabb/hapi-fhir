package ca.uhn.fhir.jpa.provider;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.rp.dstu.ObservationResourceProvider;
import ca.uhn.fhir.jpa.rp.dstu.OrganizationResourceProvider;
import ca.uhn.fhir.jpa.rp.dstu.PatientResourceProvider;
import ca.uhn.fhir.jpa.testutil.RandomServerPortProvider;
import ca.uhn.fhir.model.dstu.resource.Observation;
import ca.uhn.fhir.model.dstu.resource.Organization;
import ca.uhn.fhir.model.dstu.resource.Patient;
import ca.uhn.fhir.model.dstu.resource.Questionnaire;
import ca.uhn.fhir.rest.client.IGenericClient;
import ca.uhn.fhir.rest.server.RestfulServer;

public class SystemProviderTest {

	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(SystemProviderTest.class);
	private static Server ourServer;
	private static ClassPathXmlApplicationContext ourAppCtx;
	private static FhirContext ourCtx;
	private static IGenericClient ourClient;

	@Test
	public void testTransactionFromBundle() throws Exception {

		InputStream bundleRes = SystemProviderTest.class.getResourceAsStream("/transaction_link_patient_eve.xml");
		String bundle = IOUtils.toString(bundleRes);
		String response = ourClient.transaction().withBundle(bundle).prettyPrint().execute();
		ourLog.info(response);
	}
	
	
	@AfterClass
	public static void afterClass() throws Exception {
		ourServer.stop();
		ourAppCtx.stop();
	}

	@SuppressWarnings("unchecked")
	@BeforeClass
	public static void beforeClass() throws Exception {
		ourAppCtx = new ClassPathXmlApplicationContext("fhir-jpabase-spring-test-config.xml", "hapi-fhir-server-resourceproviders-dstu2.xml");

		IFhirResourceDao<Patient> patientDao = (IFhirResourceDao<Patient>) ourAppCtx.getBean("myPatientDaoDstu2", IFhirResourceDao.class);
		PatientResourceProvider patientRp = new PatientResourceProvider();
		patientRp.setDao(patientDao);

		IFhirResourceDao<Questionnaire> questionnaireDao = (IFhirResourceDao<Questionnaire>) ourAppCtx.getBean("myQuestionnaireDaoDstu2", IFhirResourceDao.class);
		QuestionnaireResourceProvider questionnaireRp = new QuestionnaireResourceProvider();
		questionnaireRp.setDao(questionnaireDao);

		IFhirResourceDao<Observation> observationDao = (IFhirResourceDao<Observation>) ourAppCtx.getBean("myObservationDaoDstu2", IFhirResourceDao.class);
		ObservationResourceProvider observationRp = new ObservationResourceProvider();
		observationRp.setDao(observationDao);

		IFhirResourceDao<Organization> organizationDao = (IFhirResourceDao<Organization>) ourAppCtx.getBean("myOrganizationDaoDstu2", IFhirResourceDao.class);
		OrganizationResourceProvider organizationRp = new OrganizationResourceProvider();
		organizationRp.setDao(organizationDao);

		RestfulServer restServer = new RestfulServer();
		restServer.setResourceProviders(patientRp, questionnaireRp, observationRp, organizationRp);

		JpaSystemProviderDstu2 systemProv = ourAppCtx.getBean(JpaSystemProviderDstu2.class, "mySystemProviderDstu2");
		restServer.setPlainProviders(systemProv);

		int myPort = RandomServerPortProvider.findFreePort();
		ourServer = new Server(myPort);

		ServletContextHandler proxyHandler = new ServletContextHandler();
		proxyHandler.setContextPath("/");

		String serverBase = "http://localhost:" + myPort + "/fhir/context";

		ServletHolder servletHolder = new ServletHolder();
		servletHolder.setServlet(restServer);
		proxyHandler.addServlet(servletHolder, "/fhir/context/*");

		ourCtx = FhirContext.forDstu2();
		restServer.setFhirContext(ourCtx);
		
		ourServer.setHandler(proxyHandler);
		ourServer.start();


		ourCtx.getRestfulClientFactory().setSocketTimeout(600 * 1000);
		ourClient = ourCtx.newRestfulGenericClient(serverBase);
		ourClient.setLogRequestAndResponse(true);
	}
	
}
