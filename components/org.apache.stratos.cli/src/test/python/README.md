### CLI Integration Tests

#### Running the tests

The CLI integration tests can be run from maven using:

```
mvn -P cli-test integration-test
```

You will need python installed and be running a unix like operating system

#### Creating new tests

Run 'mvn -P cli-test integration-test' to download the test dependencies.

Set the environment variables CLI_JAR, PYTHONPATH and WIREMOCK_JAR, WIREMOCK_HTTP_PORT, WIREMOCK_HTTPS_PORT. For example:

```
# the stratos CLI_JAR
export CLI_JAR=~/stratos/components/org.apache.stratos.cli/target/org.apache.stratos.cli-4.1.0-SNAPSHOT.jar

# set the PYTHONPATH to include pexpect
export PYTHONPATH=$PYTHONPATH:~/stratos/components/org.apache.stratos.cli/target/pexpect-3.2

# the wiremock application
export WIREMOCK_JAR=~/stratos/components/org.apache.stratos.cli/target/dependency/wiremock-1.46-standalone.jar

# wiremock's http port 
export WIREMOCK_HTTP_PORT=8080

# wiremock's https port 
export WIREMOCK_HTTPS_PORT=9443
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

#### Wiremock REST recording

Wiremock can record REST requests and responses between the CLI and Stratos.  To setup recording, run wiremock wth the following command line:

```
java -jar $WIREMOCK_JAR --record-mappings --proxy-all="https://stratos_address:stratos_port" --port 8181 --https-port 9441
```

Choose values for the http and https ports that are free on your system.

Set the STRATOS_URL:

```
export STRATOS_URL="https://localhost:9441"  # use the https-port you ran wiremock with
```

Then run the CLI, and perform the action that you want wiremock to record:

```
java -jar $CLI_JAR
```

Wiremock will have recorded the requests and responses from the CLI to Stratos in the 'mappings' and '__files' folders.

You can stop wiremock on the command line and create a test case using pexpect.
