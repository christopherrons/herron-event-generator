plugins {
    java
    id("org.springframework.boot") version "3.0.1"
    id("io.spring.dependency-management") version "1.1.0"
    id("org.hidetake.ssh") version "2.10.1"
}

springBoot {
    mainClass.set("com.herron.bitstamp.consumer.server.BitstampConsumerApplication")
}

// Project Configs
allprojects {
    repositories {
        mavenCentral()
    }

    apply(plugin = "maven-publish")
    apply(plugin = "java-library")

    group = "com.herron.bitstamp.consumer"
    version = "1.0.0-SNAPSHOT"
    if (project.hasProperty("releaseVersion")) {
        val releaseVersion: String by project
        version = releaseVersion
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    //Project Modules
    implementation(project(":bitstamp-consumer-server"))

    // External Libs
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.parent)
    implementation(libs.spring.kafka)
    implementation(libs.tyrus.standalone.client)
    implementation(libs.javax.json.api)
    implementation(libs.javax.json)
    implementation(libs.javafaker)

    // External Test Libs
    testImplementation(testlibs.junit.jupiter.api)
    testImplementation(testlibs.junit.jupiter.engine)
    testImplementation(testlibs.spring.boot.starter.test)
    testImplementation(testlibs.spring.kafka.test)
}

// Tasks
val releaseDirName = "releases"
tasks.register<Tar>("buildAndPackage") {
    dependsOn("clean")
    dependsOn("build")
    tasks.findByName("build")?.mustRunAfter("clean")
    compression = Compression.GZIP
    archiveExtension.set("tar.gz")
    destinationDirectory.set(layout.buildDirectory.dir(releaseDirName))
    from(layout.projectDirectory.dir("bitstamp-consumer-deploy/src/main/java/com/herron/bitstamp/consumer/deploy/scripts")) {
        exclude("**/*.md")
    }
    from(layout.buildDirectory.file("libs/${rootProject.name}-${version}.jar"))
}

tasks.register("deployToServer") {
    remotes {
        withGroovyBuilder {
            "create"("webServer") {
                    setProperty("host", "bitstamp-consumer-1")
                setProperty("user", "herron")
                setProperty("agent", true)
            }
        }
    }

    doLast {
        ssh.run(delegateClosureOf<org.hidetake.groovy.ssh.core.RunHandler> {
            session(remotes, delegateClosureOf<org.hidetake.groovy.ssh.session.SessionHandler> {
                put(
                    hashMapOf(
                        "from" to "${layout.buildDirectory.get()}/${releaseDirName}/${rootProject.name}-${version}.tar.gz",
                        "into" to "/home/herron/deploy"
                    )
                )
                execute("tar -xf /home/herron/deploy/bitstamp-consumer-${version}.tar.gz --directory /home/herron/deploy/")
                execute("rm /home/herron/deploy/bitstamp-consumer-${version}.tar.gz ")
                execute("cd /home/herron/deploy/ && bash /home/herron/deploy/bootstrap.sh")
            })
        })
    }
}

tasks.register("buildPackageDeploy") {
    dependsOn("buildAndPackage")
    dependsOn("deployToServer")
    tasks.findByName("deployToServer")?.mustRunAfter("buildAndPackage")
}

tasks.withType<Test> {
    useJUnitPlatform()
}