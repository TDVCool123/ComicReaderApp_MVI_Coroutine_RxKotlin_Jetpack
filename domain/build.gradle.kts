plugins {
  `comic-app-plugin`
  kotlin
}

dependencies {
  implementation(deps.reactiveX.java)
  implementation(deps.kotlin.coroutinesCore)
  implementation(deps.arrow.core)
  //implementation("com.chrynan.uri:uri-core-iossimulatorarm64:0.4.0")
  api(deps.uri.core)
}
