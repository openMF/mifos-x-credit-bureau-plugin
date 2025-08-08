plugins {
	java
	id("org.springframework.boot") version "3.5.4"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "org.mifos"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}
java {
	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = JavaVersion.VERSION_21
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
	maven {
		url = uri("https://mifos.jfrog.io/artifactory/libs-release-local")
	}
}
ext {
	set("springCloudVersion", "2024.0.1")
	set("fineractVersion", "0.0.1195-dd90938")
}

dependencies {
	// Spring Boot Starters
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-web")

	// API Documentation
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9")

	// Jakarta REST / Jersey (JAX-RS)
	implementation("jakarta.ws.rs:jakarta.ws.rs-api:4.0.0")
	implementation("org.glassfish.jersey.containers:jersey-container-servlet:3.1.11")
	implementation("org.glassfish.jersey.core:jersey-server:3.1.11")
	implementation("org.glassfish.jersey.inject:jersey-hk2:3.1.11")

	// Database & ORM
	implementation("org.liquibase:liquibase-core")
	runtimeOnly("org.mariadb.jdbc:mariadb-java-client")

	// Fineract
	implementation("org.apache.fineract:fineract-core:${project.ext["fineractVersion"]}")
	// Optional alternative:
	// implementation("org.apache.fineract:fineract-provider:${project.ext["fineractVersion"]}:plain")

	// Mapping / Code Generation
	implementation("org.mapstruct:mapstruct:1.6.3")
	annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")

	// Cryptography
	implementation("org.bouncycastle:bcprov-jdk18on:1.81")

	// Lombok
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	// Testing
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("com.h2database:h2")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

