# Zerobase API Server

[![Build Status](https://dev.azure.com/zerobase-io/smart-tracing-api/_apis/build/status/zerobase-io.smart-tracing-api?branchName=azure-pipelines-testing)](https://dev.azure.com/zerobase-io/smart-tracing-api/_build/latest?definitionId=3&branchName=azure-pipelines-testing)

This repository contains the back end for the Zerobase smart tracing platform. Refer to the [smart-tracing repo](https://github.com/zerobase-io/smart-tracing) for the front end. We are using Kotlin and Neo4j with cloud storage on Heroku. We are using Dropwizard for the REST framework. Kotlin requires Java and Maven.

Any commands shown in this read-me are written in bash. If you are on Windows or use an alternate shell, like fish, please adjust the commands
accordingly.

There are other README files throughout the project. They are located in the source folders that most closely align with the
readme contents.

## Architecture Notes
* We are leveraging the type system as much as possible. We don't want stringly typed things and why write our own validations when we can
  just use what's already there?
  * Inline Types - These are mostly just string-wrapper classes provide some context as they are passed around
  * Unsigned Numbers - There are a whole bunch of things that want numbers no less than 0
* We are trying to avoid coupling as much as possible. To this end, the rest endpoints don't do anything but
  execute database operations and then fire events to trigger any additional behaviors. This design allows us to add
  new behaviors without having to constant touch and bloat the rest resources.
* Dependency injection is your friend. Learn to love it. It's a huge win on testing, among other things.
* There are 2 levels of automated testing relevant to this project, and I'm going to use Google's terms: small and medium. Both have their
  place.
  * **Small Tests** - These are usually referred to as "unit tests". They narrowly test a specific class or method. Everything is mocked.
    These tests are great for complex pieces and validating individual behaviors, but has at least 2 main problems:are limited in verifying the app is functioning
    - They don't verify the service is working as it should. You can have extremely robust small test suites that validate each piece and
      still have a broken app that won't start.
    - They make refactoring the internals painful. Since all interactions are mocked in small tests, if you change any of the things that
      were mocked, you have to rewire all the tests. Sometimes, that's easy using IDEs and tooling and sometimes it's extremely painful
      because the refactor isn't that simple.
  * **Medium Tests** - These are service level tests. Medium tests, unlike small tests, have access to resources on localhost. This allows
    for a local database to be used. This type of test provides a greater confidence that the app will work when deployed because to run,
    the app must be able to turn on, wire up all the pieces (like database connections), and produce the correct API outputs based on the
    inputs. This type of test allows easy refactoring of the internals because it verifies that the API contract is maintained, regardless
    of internal implementation details.

## Development Setup

### Kotlin
The backend is written in Kotlin. While you can work on it in any editor, such as vim or VS Code, it is significantly easier to use an IDE. We recommend [IntelliJ](https://www.jetbrains.com/idea/download/index.html).
If you are using Intellij, the Kotlin plugin must be **at least 1.3.70**.

### Java 11

This project targets the current LTS version of Java: 11. You are welcome to use any of the JDK implementations locally, but deployments
will be done using an AdoptOpenJDK build. If you want to use Oracle's official JDK, it can be dowloaded [here](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html).
We recommend, however, that you install the JDK using a package manager, such as `brew` on macOS, to easily stay up to date.

### Maven
Maven is a dependency management and build tool that is commonly used in the Java world. It is available for installation via package
managers, such as `brew` on macOS, for easy updating (recommended way) but can also be installed manually.

#### Install manually
* Download Maven from [here](https://maven.apache.org/download.cgi)
* Follow these installation instructions [here](https://maven.apache.org/install.html)
* Add the full path to your ~/.bash_profile, ~/.zshrc, or similar.

### Docker
Docker is used to run external services locally for front-end development or tests instead of requiring access to a cloud environment.
Some of the resources can be run without docker, but some don't really have local install options. Install docker manually by following
this [guide](https://www.docker.com/get-started) or via a package manager.

## Running Locally

### Docker Compose
We have a docker compose file that will spin up all the pieces necessary and expose the API on your local machine. There are several
environment variables that can be used to configure it:

* `DB_PORT`: Used if you want to connect to the gremlin server manually and play with the graph.
* `APP_VERSION`: By default, it will use latest. If you need to run a specific version, or one that isn't in DockerHub yet, set this to the version tag you want.
* `APP_PORT**: By default, it will be 9000.

#### Not changing the backend
To run the backend when not intending to work on it (useful if you are working only on the front end or testing it) you can pull it in from dockerhub with

    docker-compose -f docker-compose-full-stack.yml upp app

Alternately, run *just dependencies* from docker-compose and build/develop on your machine

    $ mvn clean install
    $ docker-compose -f ./docker-compose-dependencies.yml up database aws

### Manually with Docker
#### Database - Gremlin
* Pull the Gremlin server image
    ```sh
    $ docker pull tinkerpop/gremlin-server
    ```
* Run the gremlin image. By default, the Gremlin server does not support the string ids we are using with Neptune. As such we need
  to override the settings so it will.
    ```sh
    $ docker run -d \
            -p 8182:8182 \
            -v $(pwd)/src/test/resources/tinkergraph-overrides.properties:/opt/gremlin-server/conf/tinkergraph-empty.properties \
            --name=zerobase-db \
            tinkerpop/gremlin-server:latest
    ```
By default, there are no credentials for the local install

#### Localstack - AWS Fakes
Follow their startup documentation: https://github.com/localstack/localstack.

#### App
```sh
$ docker run -d --name=zerobase-api \
    -e WRITE_ENDPOINT=<gremlin-api host> \
    -e DB_PORT=<gremlin mapped port> \
    -e AWS_SES_ENDPOINT=<localstack ses http url (including port)>
    zerobaseio/smart-tracing-api:<version>
```

### Manually without Docker
#### Run Gremlin without Docker
If you're unable to use Docker to run Gremlin, there is an alternative available. The desktop version can be downloaded
from [here](https://www.apache.org/dyn/closer.lua/tinkerpop/3.4.6/apache-tinkerpop-gremlin-server-3.4.6-bin.zip). The configuration
instructions are found [here](http://tinkerpop.apache.org/docs/3.4.6/reference/#gremlin-server). The default configuration should
suffice.

#### AWS SES
Follow the documentation for a non-Docker SES fake. Here's one: https://github.com/csi-lk/aws-ses-local

#### Project
After cloning the project there are multiple ways to deploy it locally: from dockerhub, using an IDE or via the command line. There is also a docker-compose for setting up all dependencies. By default, the app listens on
port 9000. You can override that with an environment variable of `PORT` if you need to. The `local-config.yml` defaults to `localhost`
and `8182` for the database. Both can be overriden with environment variables, using `WRITE_ENDPOINT` and `DB_PORT` respectively.

**Running in an IDE**

The following directions use Intellij as the IDE, but the steps should be similar if you are using a different IDE.

* If necessary, navigate to File/Project Structure. Update the SDK for the project to JDK11.
* Open `Main.kt`
* In `Main.kt`, click on the run button next to main and select `Create Main.kt` to create a run configuration.

![main](./images/main.png)

* Set the name to something reasonable. If running locally, it's recommended to use the `local-config.yml` instead of
`config.yml`. The configuration will target running locally against Gremlin, while the normal config will target AWS Neptune.
* Set the program arguments as `server <the config file you want to use>`. Options for config files are:
    * `src/main/resources/config.yml`
    * `src/main/resources/local-config.yml`
* By default, the `local-config.yml` expects the database to be running at `localhost:8182`. If you need to override the port,
set an environment variable of `DB_PORT` to the port your gremlin server is running on.

![env](./images/env.png)

* Click `OK` and run the configuration you just made.

**Running from the command line.**
* From the project's root directory, build the project.
    ```sh
    $ mvn clean install
    ```

    Alternately, to do a build with fewer checks
    ```sh
      mvn package -Dbasepom.check.skip-all
    ```

* Set the environment variables (if needed), either as an export or inline, and run the jar.

  You should not have to set environment variables at all if running the default configuration for dependencies via `docker-compose-dependencies`
    * Export
      ```sh
-       $ export DB_PORT=12345
        $ java -jar target/smart-tracing-api.jar server target/classes/local-config.yml
      ```
    * Inline
      ```sh
        $ DB_PORT=12345 java -jar target/smart-tracing-api.jar server target/classes/local-config.yml
      ```

### Debugging / Calling end points
* Once you're running the project, you can double check if all is well by visiting `http://localhost:8081` in your browser. You
  shoudl see a simple page that exposes metrics and healthcheck results. You can also curl `http://localhost:8081/healthchecks` if
  you prefer the command line.
* App endpoints are available at `http://localhost:9000`

## Database Model
![Database Model](https://www.lucidchart.com/publicSegments/view/5946562a-26d8-4b1a-9054-e1692c23afe7/image.png)
