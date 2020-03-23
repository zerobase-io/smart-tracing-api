# Zerobase API Server

This repository contains the back end for the Zerobase smart tracing platform. Refer to the [smart-tracing repo](https://github.com/zerobase-io/smart-tracing) for the front end. We are using Kotlin and Neo4j with cloud storage on Heroku. We are using Dropwizard for the REST framework. Kotlin requires Java and Maven.

# TODO

*

## Set up for the project

### Kotlin

* Download [IntelliJ](https://www.jetbrains.com/idea/download/index.html?_ga=2.137859766.761208892.1584829709-1795868819.1584829709#section=mac) community version to use as an IDE for Kotlin
* There is a dependency on Java 11 and Maven

### Java 11

* This project requires Java version 11 which can be dowloaded [here](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html)

### Maven

* Download Maven from [here](https://maven.apache.org/download.cgi)
* Follow these installation [instructions](https://maven.apache.org/install.html)
* Add the full path to your ~/.bash_profile 


### Dropwizard

* A library for building RESTful web services [here](https://github.com/dropwizard/dropwizard) 
* Maven is required to run successfully

### Docker

Docker is used to run Neo4j on this project 

*  Install Docker [here](https://www.docker.com/get-started?utm_source=google&utm_medium=cpc&utm_campaign=getstarted&utm_content=sitelink&utm_term=getstarted&utm_budget=growth&gclid=EAIaIQobChMIzsLmsdWu6AIVA4bICh3VWArbEAAYASABEgKP8_D_BwE)
* Pull the neo4j image
```sh
$ docker pull neo4j
```

### Project

```sh
$ git clone https://github.com/zerobase-io/smart-tracing.git
```
* Open the folder in intelliJ navigate to File/Project Structure. Update the JDK home path to Java 11
* In the file path src/main/kotlin/models/ open `Main.kt`
* In `Main.kt` right click on the run button next to main and click Create Run Configuration
* ![main](https://github.com/alh2202/smart-tracing-api/blob/master/main.png)

* Set the environment variable as `GRAPHENEDB_PASSWORD=zerobase`
And program arguments as `server src/main/resources/config.yml`
![env](https://github.com/alh2202/smart-tracing-api/blob/master/env.png)
 
*  From the command line navigate into the folder and run the following, it downloads all of the project dependencies
```sh
$ mvn clean install
```
* Run this to build
```sh
$ java -jar target/smart-tracing-api.jar server target/classes/config.yml
```

* In a different terminal window, run this to create and run a docker container
```sh
$ docker run -d -p7474:7474 -p7687:7687 --name=zerboase-db neo4j:latest
```
* The first port is http endpoint and the second is bolt port :7687


* Open http://localhost:7474/ in your browser
The default username and password for Neo4j is 
username: Neo4j 
Password: Neo4j
You will then be asked to set a new password, make this password “zerobase”

### Project structure





