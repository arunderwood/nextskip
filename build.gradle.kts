import com.github.spotbugs.snom.SpotBugsTask
import de.undercouch.gradle.tasks.download.Download

plugins {
    id("java")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.vaadin)
    alias(libs.plugins.download)

    // Code quality plugins
    id("checkstyle")
    id("pmd")
    alias(libs.plugins.spotbugs)
    id("jacoco")
    alias(libs.plugins.delta.coverage)
    alias(libs.plugins.pitest)
}

group = "io.nextskip"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

// Enable reproducible builds
tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

// Enable dependency locking for all configurations
// This creates gradle.lockfile which Renovate uses to trigger postUpgradeTasks
dependencyLocking {
    lockAllConfigurations()
}

// Disable plain JAR generation - we only need the Spring Boot executable JAR
tasks.named<Jar>("jar") {
    enabled = false
}

// Workaround: Delta coverage plugin + Vaadin causes bootJar to look for 'generate' task
// which doesn't exist. Create an alias to hillaGenerate to fix the conflict.
// See: https://github.com/gw-kit/delta-coverage-plugin/issues/172
tasks.register("generate") {
    dependsOn("hillaGenerate")
    description = "Alias for hillaGenerate (workaround for delta-coverage plugin compatibility)"
}

// Ensure Vaadin production resources are included in bootJar
// The vaadinBuildFrontend task writes to build/resources/main/ AFTER processResources,
// so we need to explicitly include these files in the bootJar
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    dependsOn("vaadinBuildFrontend", "downloadOtelAgent", "downloadPyroscopeAgent")

    // Explicitly copy Vaadin production files into the JAR
    into("BOOT-INF/classes") {
        from("build/resources/main") {
            include("META-INF/VAADIN/**")
            include("META-INF/resources/**")
        }
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}

repositories {
    mavenCentral()  // Primary repository - fastest and most reliable

    // Vaadin addons repository (required for Vaadin components)
    maven {
        url = uri("https://maven.vaadin.com/vaadin-addons")
        content {
            includeGroup("com.vaadin")
        }
    }

    // Pre-release repositories (only for SNAPSHOT builds)
    if (version.toString().contains("SNAPSHOT") || version.toString().contains("rc")) {
        maven {
            url = uri("https://maven.vaadin.com/vaadin-prereleases")
            content {
                includeGroup("com.vaadin")
            }
        }
        maven {
            url = uri("https://repo.spring.io/milestone")
            content {
                includeGroupByRegex("org\\.springframework.*")
            }
        }
    }
}

the<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension>().apply {
    imports {
        mavenBom("com.vaadin:vaadin-bom:${libs.versions.vaadin.get()}")
    }
}

dependencies {
    // Vaadin with Hilla for React integration
    implementation(libs.bundles.vaadin)
    developmentOnly(libs.vaadin.dev)

    // Spring Boot starters (versions managed by Spring Boot BOM)
    implementation(libs.bundles.spring.boot.web)
    implementation(libs.spring.boot.starter.webflux)

    // Caching
    implementation(libs.caffeine)

    // Resilience4j for circuit breakers and retry logic
    implementation(libs.bundles.resilience4j)

    // Jackson XML
    implementation(libs.jackson.dataformat.xml)

    // iCal4j for calendar/event parsing
    implementation(libs.ical4j)

    // Jsoup for HTML parsing
    implementation(libs.jsoup)

    // MQTT client for PSKReporter
    implementation(libs.paho.mqttv5.client)

    // Pekko Streams for high-volume ETL
    implementation(libs.pekko.stream)

    // Database
    implementation(libs.spring.boot.starter.data.jpa)
    runtimeOnly(libs.postgresql)
    implementation(libs.spring.boot.starter.liquibase)

    // Database observability
    implementation("net.ttddyy.observation:datasource-micrometer-spring-boot:2.0.1")
    implementation("org.hibernate.orm:hibernate-micrometer")

    // Scheduling
    implementation(libs.db.scheduler.spring.boot.starter)

    // SpotBugs annotations (for @SuppressFBWarnings)
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.9.8")

    // Testing
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.reactor.test)
    testImplementation(libs.wiremock.standalone)

    // Testcontainers for database integration tests
    testImplementation(libs.bundles.testcontainers)

    // Java 25 compatibility - override Mockito and ByteBuddy
    testImplementation(libs.mockito.core)
    testImplementation(libs.byte.buddy)

    // Property-based testing
    testImplementation(libs.jqwik)

    // Pekko Streams testkit
    testImplementation(libs.pekko.stream.testkit)
}

// OpenTelemetry agent configuration
val otelAgentVersion = libs.versions.grafana.otel.agent.get()
val otelAgentDir = layout.buildDirectory.dir("otel-agent")

tasks.register<Download>("downloadOtelAgent") {
    description = "Downloads the Grafana OpenTelemetry Java agent"
    group = "observability"

    src("https://github.com/grafana/grafana-opentelemetry-java/releases/download/v${otelAgentVersion}/grafana-opentelemetry-java.jar")
    dest(otelAgentDir.map { it.file("grafana-opentelemetry-java.jar") })
    overwrite(false)
    retries(3)
}

// Pyroscope profiling agent configuration
val pyroscopeAgentVersion = libs.versions.pyroscope.agent.get()
val pyroscopeAgentDir = layout.buildDirectory.dir("pyroscope-agent")

tasks.register<Download>("downloadPyroscopeAgent") {
    description = "Downloads the Pyroscope Java profiling agent"
    group = "observability"

    src("https://github.com/grafana/pyroscope-java/releases/download/v${pyroscopeAgentVersion}/pyroscope.jar")
    dest(pyroscopeAgentDir.map { it.file("pyroscope.jar") })
    overwrite(false)
    retries(3)
}

tasks.named<Test>("test") {
    useJUnitPlatform()

    // Parallel test execution for faster CI builds
    // GitHub runners have 4 cores, so run up to 4 test JVMs in parallel
    maxParallelForks = Runtime.getRuntime().availableProcessors()
    // Fork new JVM every 50 tests to prevent memory leaks
    setForkEvery(50)

    // Required for Java 25 compatibility with Mockito/ByteBuddy
    // --add-opens: Allows reflective access to JDK internals
    // --enable-native-access: Allows JNA native library loading
    // -XX:+EnableDynamicAgentLoading: Allows ByteBuddy agent attachment
    // -Dnet.bytebuddy.experimental=true: Enables ByteBuddy's experimental Java 25 support
    jvmArgs = listOf(
        "--add-opens", "java.base/java.lang=ALL-UNNAMED",
        "--add-opens", "java.base/java.util=ALL-UNNAMED",
        "--add-opens", "java.base/java.io=ALL-UNNAMED",
        "--enable-native-access=ALL-UNNAMED",
        "-XX:+EnableDynamicAgentLoading",
        "-Dnet.bytebuddy.experimental=true"
    )
}

// Code quality plugin configurations

// NOTE: Using direct string literals instead of version catalog accessors (libs.versions.X.get())
// to enable Renovate detection until native support is added.
// See: https://github.com/renovatebot/renovate/discussions/40147
// Can be reverted to catalog accessors if Renovate adds native support.
checkstyle {
    toolVersion = "12.3.1"
    configFile = file("${rootDir}/config/checkstyle/checkstyle.xml")
    isIgnoreFailures = false  // Quality violations fail the build
}

pmd {
    toolVersion = "7.20.0"
    ruleSets = emptyList()  // Empty to use custom ruleset file
    ruleSetFiles = files("${rootDir}/config/pmd/ruleset.xml")
    isIgnoreFailures = false  // Quality violations fail the build
}

spotbugs {
    toolVersion = "4.9.8"
    effort = com.github.spotbugs.snom.Effort.MAX
    reportLevel = com.github.spotbugs.snom.Confidence.MEDIUM
    ignoreFailures = false  // Quality violations fail the build
    excludeFilter = file("${rootDir}/config/spotbugs/spotbugs-exclude.xml")
}

tasks.withType<SpotBugsTask>().configureEach {
    reports {
        create("html") {
            required = true
        }
        create("xml") {
            required = true  // Enable for CI integration
        }
    }
}

jacoco {
    toolVersion = "0.8.14"
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
    reports {
        xml.required = true
        html.required = true
    }
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    violationRules {
        rule {
            limit {
                minimum = "0.75".toBigDecimal()  // 75% instruction coverage
            }
        }
        rule {
            limit {
                counter = "BRANCH"
                minimum = "0.65".toBigDecimal()  // 65% branch coverage
            }
        }
    }
}

// Delta coverage: enforce coverage on changed code (PR patch coverage)
configure<io.github.surpsg.deltacoverage.gradle.DeltaCoverageConfiguration> {
    diffSource.git.compareWith("refs/remotes/origin/main")

    // Exclude infrastructure classes that require integration tests
    // These classes involve Pekko Streams, MQTT connections, and Spring configuration
    // that cannot be meaningfully unit tested without full infrastructure
    excludeClasses.value(listOf(
        "**/SpotStreamProcessor.class",
        "**/SpotStreamConfig.class",
        "**/PskReporterMqttSource.class",
        "**/SpotCleanupTask.class"
    ))

    reportViews.named("test") {
        violationRules {
            failIfCoverageLessThan(0.80)
            failOnViolation.set(true)
        }
    }

    // Report generation
    reports {
        html.set(true)
        xml.set(true)
        markdown.set(true)
    }
}

pitest {
    targetClasses = setOf("io.nextskip.*")
    targetTests = setOf("io.nextskip.*")
    mutators = setOf("DEFAULTS")
    // Dynamic thread allocation: use 75% of available cores, minimum 2
    val availableCores = Runtime.getRuntime().availableProcessors()
    val pitestThreads = maxOf(2, (availableCores * 0.75).toInt())
    threads = pitestThreads
    outputFormats = setOf("HTML", "XML")
    timestampedReports = false
    junit5PluginVersion = "1.2.1"
    // Incremental analysis: reuse results for unchanged code
    historyInputLocation = file("${rootDir}/.pitest-history")
    historyOutputLocation = file("${rootDir}/.pitest-history")
    // Fail build if mutation score drops below threshold
    mutationThreshold = 75
    // Timeout to prevent hangs when mutations cause infinite waits (e.g., in backoff/retry logic)
    timeoutConstInMillis = 15000
    jvmArgs = listOf(
        "--add-opens", "java.base/java.lang=ALL-UNNAMED",
        "--enable-native-access=ALL-UNNAMED",
        "-XX:+EnableDynamicAgentLoading",
        "-Dnet.bytebuddy.experimental=true"
    )
}

// Enable coverage verification in check task for local feedback
// Skip locally if needed: ./gradlew check -x jacocoTestCoverageVerification -x deltaCoverage -x pitest
tasks.named("check") {
    dependsOn("jacocoTestCoverageVerification")
    dependsOn("deltaCoverage")
    dependsOn(tasks.named("pitest"))
}

// Vaadin's vaadinPrepareFrontend runs npm install but doesn't declare
// package-lock.json as an input. This causes Gradle's build cache to
// restore stale outputs when new npm dependencies are added.
// Adding package-lock.json as an input ensures the cache invalidates
// when dependencies change.
plugins.withId("com.vaadin") {
    tasks.named("vaadinPrepareFrontend") {
        inputs.file("package-lock.json").withPathSensitivity(PathSensitivity.RELATIVE)
    }
}
