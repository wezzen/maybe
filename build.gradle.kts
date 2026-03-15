plugins {
    id("java")
    id("maven-publish")
    id("org.jreleaser") version "1.17.0"
}

group = "io.github.wezzen"
version = "0.1.0"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    compileOnly("org.jetbrains:annotations:26.1.0")

    testImplementation("org.junit.jupiter:junit-jupiter:6.0.3")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<Javadoc> {
    (options as StandardJavadocDocletOptions).apply {
        encoding = "UTF-8"
        addBooleanOption("html5", true)
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name = "maybe"
                description = "A null-safe Maybe container for Java — no get() without handling both cases"
                url = "https://github.com/wezzen/maybe"

                licenses {
                    license {
                        name = "Apache License 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0"
                    }
                }

                developers {
                    developer {
                        id = "wezzen"
                        name = "Dmitry Wezen"
                        email = "dimariqqq@gmail.com"
                    }
                }

                scm {
                    connection = "scm:git:git://github.com/wezzen/maybe.git"
                    developerConnection = "scm:git:ssh://github.com/wezzen/maybe.git"
                    url = "https://github.com/wezzen/maybe"
                }
            }
        }
    }

    repositories {
        maven {
            url = uri(layout.buildDirectory.dir("staging-deploy"))
        }
    }
}

jreleaser {
    gitRootSearch = true

    project {
        copyright = "2026 Dmitry Wezen"
    }

    signing {
        active = org.jreleaser.model.Active.ALWAYS
        armored = true
    }

    deploy {
        maven {
            mavenCentral {
                create("sonatype") {
                    active = org.jreleaser.model.Active.ALWAYS
                    url = "https://central.sonatype.com/api/v1/publisher"
                    stagingRepository("build/staging-deploy")
                }
            }
        }
    }

    release {
        github {
            skipRelease = true
            skipTag = true
            token = "none"
        }
    }
}