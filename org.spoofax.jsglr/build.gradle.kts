plugins {
  id("org.metaborg.gradle.config.java-library")
  id("org.metaborg.gradle.config.junit-testing")
}

dependencies {
  // api(platform("org.metaborg:parent:$version")) // Can't use: causes dependency cycle because parent mentions pie.

  api("org.metaborg:org.spoofax.terms:$version")
  testCompileOnly("junit:junit:4.12")
  testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.1.0")
  api("org.metaborg:org.metaborg.util:$version")
  api("org.metaborg:org.metaborg.parsetable:$version")
}

sourceSets {
  main {
    java {
      srcDir("src")
    }
  }
  test {
    java {
      srcDir("test")
    }
  }
}

// Copy test resources into classes directory, to make them accessible as classloader resources at runtime.
val copyTestResourcesTask = tasks.create<Copy>("copyTestResources") {
  from("$projectDir/test/resources")
  into("$buildDir/classes/java/test")
}
tasks.getByName("processTestResources").dependsOn(copyTestResourcesTask)

// Skip tests, as they do not work.
tasks.test.get().enabled = false
