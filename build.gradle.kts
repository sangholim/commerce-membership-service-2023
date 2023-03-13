import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
    id("org.springframework.boot") version "2.7.5"
    id("io.spring.dependency-management") version "1.0.15.RELEASE"
    id("org.springdoc.openapi-gradle-plugin") version "1.6.0"

    kotlin("jvm") version "1.6.21"
    kotlin("plugin.spring") version "1.6.21"
    kotlin("kapt") version "1.6.21"
}

group = "io.commerce"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    kapt("org.springframework.boot:spring-boot-configuration-processor")
    kapt("com.github.therapi:therapi-runtime-javadoc-scribe:0.15.0")
    kapt("org.mapstruct:mapstruct-processor:1.5.3.Final")

    // Bill of Materials (BOM)
    implementation(platform("io.awspring.cloud:spring-cloud-aws-dependencies:2.4.2"))
    implementation(platform("io.kotest:kotest-bom:5.5.4"))
    implementation(platform("org.springdoc:springdoc-openapi:2.0.3"))
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:2021.0.5"))
    implementation(platform("org.testcontainers:testcontainers-bom:1.17.6"))

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.awspring.cloud:spring-cloud-starter-aws-secrets-manager-config")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.mapstruct:mapstruct:1.5.3.Final")
    implementation("org.springdoc:springdoc-openapi-javadoc")
    implementation("org.springdoc:springdoc-openapi-kotlin")
    implementation("org.springdoc:springdoc-openapi-security")
    implementation("org.springdoc:springdoc-openapi-webflux-core")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.cloud:spring-cloud-starter-bootstrap")
    implementation("org.springframework.cloud:spring-cloud-starter-stream-kafka")
    implementation("software.amazon.msk:aws-msk-iam-auth:1.1.5")

    runtimeOnly("com.nimbusds:oauth2-oidc-sdk:10.4")

    developmentOnly("org.springframework.boot:spring-boot-devtools")

    testImplementation("com.ninja-squad:springmockk:4.0.0")
    testImplementation("io.github.serpro69:kotlin-faker:1.13.0")
    testImplementation("io.kotest.extensions:kotest-extensions-spring:1.1.2")
    testImplementation("io.kotest.extensions:kotest-extensions-testcontainers:1.3.4")
    testImplementation("io.kotest:kotest-assertions-core")
    testImplementation("io.kotest:kotest-framework-datatest")
    testImplementation("io.kotest:kotest-runner-junit5")
    testImplementation("io.mockk:mockk:1.13.2")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:mongodb")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(module = "mockito-core")
        exclude(module = "mockito-junit-jupiter")
    }
    testImplementation("org.springframework.cloud:spring-cloud-stream") {
        artifact {
            name = "spring-cloud-stream"
            extension = "jar"
            type = "test-jar"
            classifier = "test-binder"
        }
    }
}

kapt {
    arguments {
        /**
         * Mapstruct Configurations
         */
        arg("mapstruct.defaultComponentModel", "spring")
        arg("mapstruct.unmappedSourcePolicy", "IGNORE")
        arg("mapstruct.unmappedTargetPolicy", "ERROR")
    }
    // Disable caching for annotation processor
    includeCompileClasspath = false
}

openApi {
    customBootRun {
        args.add("--spring.profiles.active=oas")
    }
    outputDir.set(file("./openapi"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<BootBuildImage>("bootBuildImage") {
    imageName = System.getenv("IMAGE_NAME")
    builder = "paketobuildpacks/builder:tiny"
    buildpacks = listOf(
        "docker://gcr.io/paketo-buildpacks/amazon-corretto",
        "docker://gcr.io/paketo-buildpacks/java",
        "docker://gcr.io/paketo-buildpacks/health-checker"
    )
    environment = mapOf(
        "BP_JVM_VERSION" to "17",
        "BP_HEALTH_CHECKER_DEPENDENCY" to "thc",
        "BP_HEALTH_CHECKER_ENABLED" to "true",
        "THC_PATH" to "/actuator/health/liveness"
    )

    docker {
        publishRegistry {
            url = System.getenv("DOCKER_REGISTRY")
            username = System.getenv("DOCKER_USERNAME")
            password = System.getenv("DOCKER_PASSWORD")
        }
    }
}
