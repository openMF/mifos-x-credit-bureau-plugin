plugins {
	java
	id("org.springframework.boot") version "3.5.3"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "org.mifos"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
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
	set("fineractVersion", "0.0.1000-b80930b")
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("jakarta.ws.rs:jakarta.ws.rs-api:3.1.0")
	implementation("org.glassfish.jersey.core:jersey-server:3.1.5")
	implementation("org.glassfish.jersey.containers:jersey-container-servlet:3.1.5")
	implementation("org.glassfish.jersey.inject:jersey-hk2:3.1.5")
	implementation("org.liquibase:liquibase-core")
	implementation("org.mapstruct:mapstruct:1.6.3")

	implementation("org.apache.fineract:fineract-core:${project.ext["fineractVersion"]}")
//	implementation("org.apache.fineract:fineract-provider:${project.ext["fineractVersion"]}:plain")

	runtimeOnly("org.mariadb.jdbc:mariadb-java-client")
	compileOnly("org.projectlombok:lombok")

	annotationProcessor("org.projectlombok:lombok")
	annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testRuntimeOnly("com.h2database:h2")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
