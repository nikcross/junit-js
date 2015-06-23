package org.onestonesoup.junitjs;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.ScriptableObject;

/**
 * An abstract class extended to create javascript unit tests
 * 
 * @author Nik Cross
 *
 */
public abstract class JavascriptUnitTest {

	public class Errors {

		private StringBuilder log = new StringBuilder();

		public void log(Object message) {
			log.append(message.toString());
			log.append("\n");
		}

		public void clear() {
			log = new StringBuilder();
		}

		public String toString() {
			return log.toString();
		}
	}

	private Map<String, Object> resources = new HashMap<String, Object>();;

	private Map<String, Object> mocks = new HashMap<String, Object>();

	private Errors errors = new Errors();

	/**
	 * Add a javascript resource for use in javascript tests
	 * 
	 * @param alias
	 *            the reference alias for the scripted object when called in javascript
	 * @param scriptFile
	 *            the file to evaluate to create a callable javascript object
	 */
	public void addResource(String alias, String scriptFile) throws IOException {
		resources.put(alias, evaluateResource(scriptFile));
	}

	/**
	 * Add a mock javascript object for use in javascript tests Mock object scripts are compiled after resources so can
	 * reference resource objects
	 * 
	 * @param alias
	 *            the reference alias for the scripted object when called in javascript
	 * @param scriptFile
	 *            the file to evaluate to create a callable javascript mock object
	 */
	public void addMock(String alias, String scriptFile) throws IOException {
		mocks.put(alias, evaluateResource(scriptFile));
	}

	/**
	 * Evaluate a target script and test script as a unit test The test script is appended to the target script and the
	 * result returned cast to a string
	 * 
	 * @param targetScript
	 *            the script file under test
	 * @param testScript
	 *            the script file applying the tests to the target script
	 * @return a string representation of the test result
	 */
	public String evaluateToString(String targetScript, String testScript) throws IOException {
		return Context.toString(evaluateTest(targetScript, testScript));
	}

	/**
	 * Evaluate a target script and test script as a unit test The test script is appended to the target script and the
	 * result returned cast to a Double
	 * 
	 * @param targetScript
	 *            the script file under test
	 * @param testScript
	 *            the script file applying the tests to the target script
	 * @return a Double representation of the test result
	 */
	public Double evaluateToDouble(String targetScript, String testScript) throws IOException {
		return Context.toNumber(evaluateTest(targetScript, testScript));
	}

	/**
	 * Evaluate a target script and test script as a unit test The test script is appended to the target script and the
	 * result returned cast to a Boolean
	 * 
	 * @param targetScript
	 *            the script file under test
	 * @param testScript
	 *            the script file applying the tests to the target script
	 * @return a Boolean representation of the test result
	 */
	public Boolean evaluateToBoolean(String targetScript, String testScript) throws IOException {
		return Context.toBoolean(evaluateTest(targetScript, testScript));
	}

	public void clearErrors() {
		errors.clear();
	}

	public String getErrors() {
		return errors.toString();
	}

	/**
	 * Evaluate a target script and test script as a unit test The test script is appended to the target script and the
	 * result returned
	 * 
	 * This method populates all of the javascript resources and mock objects then loads the target and test scripts as
	 * strings, appending the test script to the target script finally evaluating the script and returning the result
	 * 
	 * @param targetScript
	 * @param testScript
	 * @return
	 * @throws IOException
	 */
	private Object evaluateTest(String targetScript, String testScript) throws IOException {
		Context js = ContextFactory.getGlobal().enterContext();
		ScriptableObject scope = js.initStandardObjects();
		errors.log("Runnning test " + testScript + " against " + targetScript);

		ScriptableObject.putProperty(scope, "out", Context.toObject(System.out, scope));
		ScriptableObject.putProperty(scope, "errors", Context.toObject(errors, scope));

		for (String alias : resources.keySet()) {
			Object resource = resources.get(alias);
			Object wrappedOut = Context.toObject(resource, scope);
			ScriptableObject.putProperty(scope, alias, wrappedOut);
		}

		for (String alias : mocks.keySet()) {
			Object mock = mocks.get(alias);
			Object wrappedOut = Context.toObject(mock, scope);
			ScriptableObject.putProperty(scope, alias, wrappedOut);
		}

		InputStream inputStream = new FileInputStream("src/main/webapp/js/" + targetScript);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		IOUtils.copy(inputStream, outputStream);
		String script = outputStream.toString();

		inputStream = new FileInputStream("src/test/js/" + testScript);
		outputStream = new ByteArrayOutputStream();
		IOUtils.copy(inputStream, outputStream);
		script += outputStream.toString();

		return js.evaluateString(scope, script, testScript, 0, null);
	}

	private Object evaluateResource(String scriptFileName) throws IOException {
		Context js = ContextFactory.getGlobal().enterContext();
		ScriptableObject scope = js.initStandardObjects();

		ScriptableObject.putProperty(scope, "out", Context.toObject(System.out, scope));
		ScriptableObject.putProperty(scope, "errors", Context.toObject(errors, scope));

		InputStream inputStream = new FileInputStream(scriptFileName);
		Reader reader = new InputStreamReader(inputStream);

		return js.evaluateReader(scope, reader, scriptFileName, 0, null);
	}
}
