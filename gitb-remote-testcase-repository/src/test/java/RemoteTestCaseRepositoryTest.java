import com.gitb.repository.ITestCaseRepository;
import com.gitb.repository.RemoteTestCaseRepository;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Created by serbay on 10/21/14.
 */

public class RemoteTestCaseRepositoryTest {

	public static final String NULL_RESOURCE_ID = "Sample/Actor1/artifacts/Schema2.xsd";
	public static final String NULL_TEST_CASE_ID = "Sample/Actor1/testcases/TestCase2";
	public static final String AVAILABLE_RESOURCE_ID = "Sample/Actor1/artifacts/Schema1.xsd";
	public static final String AVAILABLE_TEST_CASE_ID = "Sample/Actor1/testcases/TestCase1";
	private static ITestCaseRepository repository;

	@BeforeClass
	public static void init() {
		repository = new RemoteTestCaseRepository();
	}

	@Test
	public void testResourceAvailablity() {
		assertTrue(repository.isTestArtifactAvailable(AVAILABLE_RESOURCE_ID));
		assertFalse(repository.isTestArtifactAvailable(NULL_RESOURCE_ID));

		assertNotNull(repository.getTestArtifact(AVAILABLE_RESOURCE_ID));
		assertNull(repository.getTestArtifact(NULL_RESOURCE_ID));
	}

	@Test
	public void testTestCaseAvailablity() {
		assertTrue(repository.isTestCaseAvailable(AVAILABLE_TEST_CASE_ID));
		assertFalse(repository.isTestCaseAvailable(NULL_TEST_CASE_ID));

		assertNotNull(repository.getTestCase(AVAILABLE_TEST_CASE_ID));
		assertNull(repository.getTestCase(NULL_TEST_CASE_ID));
	}
}
