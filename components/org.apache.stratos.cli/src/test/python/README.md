### CLI Integration Tests

#### Running the tests

The CLI integration tests can be run from maven using:

```
mvn -P cli-test integration-test
```

You will need python installed and be running a unix like operating system

#### Creating new tests

Run 'mvn -P cli-test integration-test' to download the test dependencies.

Set the environment variables CLI_JAR, PYTHONPATH and WIREMOCK_JAR. For example:

```
# the stratos CLI_JAR
export CLI_JAR=~/incubator-stratos/components/org.apache.stratos.cli/target/org.apache.stratos.cli-4.0.0-SNAPSHOT.jar

# set the PYTHONPATH to include pexpect
export PYTHONPATH=$PYTHONPATH:~/incubator-stratos/components/org.apache.stratos.cli/target/pexpect-3.2

# the wiremock application
export WIREMOCK_JAR=~/incubator-stratos/components/org.apache.stratos.cli/target/dependency/wiremock-1.46-standalone.jar
```

The tests are split into three areas:

- test_common.py : tests that are common to interactive and non-interactive CLI usage
- test_interactive.py : tests for CLI in interactive mode
- test_noninteractive.py : tests for CLI in non-interactive mode

After setting the above environment variables, the tests can be executed like this:

```
./test_common.py
```

The tests have class methods that start wiremock at the start of the tests (setUpClass) and stop it at the end of the test (tearDownClass).

The tests use pexpect to execute the CLI and interact with the CLI's input and output. See the existing tests for examples of pexpect.

The tests depend on wiremock (www.wiremock.org) for simulating the Statos REST services.

The python class wiremock.py is a simple wrapper to start/stop wiremock and provides utility methods so the tests can call wiremock's APIs to retrieve information about the requests that wiremock received.

The response that wiremock chooses to return for a particular request is defined in the 'mappings' folder.  A mapping usually has a response body that can be found in the '__files' folder
