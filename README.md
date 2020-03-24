# Zerobase API Server

This repository contains the back end for the Zerobase smart tracing platform. Refer to the [smart-tracing repo](https://github.com/zerobase-io/smart-tracing) for the front end. We are using Kotlin and Neo4j with cloud storage on Heroku. We are using Dropwizard for the REST framework. Kotlin requires Java and Maven. 

We use Kotlin version 1.3.70, OpenJDK version 9, any version of maven should work.

# TODO

*

## Set up for the project

### Kotlin

* The backend is written in Kotlin. Make sure you have a compatible IDE such as [IntelliJ](https://www.jetbrains.com/idea/download/index.html?_ga=2.137859766.761208892.1584829709-1795868819.1584829709).


### Java 11

* This project requires Java version 11 which can be dowloaded [here](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html)
*These are best installed with a package manager, for example using brew on Mac

### Maven

* Download Maven from [here](https://maven.apache.org/download.cgi)
* Follow these installation instructions [here](https://maven.apache.org/install.html)
* Add the full path to your ~/.bash_profile 


### Dropwizard

* A library for building RESTful web services [here](https://github.com/dropwizard/dropwizard) 
* Maven is required to run successfully

### Docker

Docker is used to run Neo4j on this project 

*  Install docker [here](https://www.docker.com/get-started?utm_source=google&utm_medium=cpc&utm_campaign=getstarted&utm_content=sitelink&utm_term=getstarted&utm_budget=growth&gclid=EAIaIQobChMIzsLmsdWu6AIVA4bICh3VWArbEAAYASABEgKP8_D_BwE)
* Pull the Neo4j image

```sh
$ docker pull neo4j
```

### Project

* After cloning the project there are many ways to deploy it locally. The following directions pertain to deployment via the IntelliJ IDE. In whichever environment you choose, ensure you're using Java 11 and set the environment variable `GRAPHENEDB_PASSWORD=zerobase`. Set the program argument `server src/main/resources/config.yml`


*In IntelliJ
	* Navigate to File/Project Structure. Update the JDK home path to Java 11.
	* In the file path src/main/kotlin/models/ open `Main.kt`
	* In `Main.kt` right click on the run button next to main and click Edit Run Configuration
	* ![main](https://github.com/alh2202/smart-tracing-api/blob/master/main.png)

	* Set the environment variable `GRAPHENEDB_PASSWORD=zerobase`
And program arguments as `server src/main/resources/config.yml`
![env](https://github.com/alh2202/smart-tracing-api/blob/master/env.png)

*If you're working with Arch, you can set the jdk to java-11 by running the following
```

```
 
*  From the command line navigate into the folder and run the following, it downloads all of the project dependencies
```sh
$ mvn install
```
* Run this to start the server
```sh
$ java -jar target/smart-tracing-api.jar server target/classes/config.yml
```

* In a different terminal window, first port is http endpoint, second is bolt port :7687

```sh
$ docker run -d -p7474:7474 -p7687:7687 --name=zerboase-db neo4j:latest
```

* Open http://localhost:7474/ in your browser
The default username and password for Neo4j is 
username: neo4j 
Password: neo4j

You will then be asked to set a new password







