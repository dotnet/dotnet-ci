// This file is invoked before the repo generator file is run.
// Within the same DSL step, the groovy files will see the same object model.
// So code in this file can affect the code in the netci.groovy.  For instance,
// this file could set a global indicating this is a pr test generation, which could
// be used to to alter any number of options (for instance, altering triggers so that
// generated jobs can be left un-disabled, but the default trigger options won't cause the
// job to run for everyone.

import jobs.generation.GenerationSettings

println("Running PreGen")

// Output the settings we're using
if (GenerationSettings.isTestGeneration()) {
    println("Setting up for a generation test")
}