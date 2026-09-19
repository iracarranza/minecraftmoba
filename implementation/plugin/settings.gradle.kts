rootProject.name = "minecraft-moba"
include("probe")
// Test-only server event harness; never install in production.
include("acceptance-fixture")
project(":acceptance-fixture").projectDir = file("../../validation/plugin/server-fixture")
