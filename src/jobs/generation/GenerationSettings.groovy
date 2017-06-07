package jobs.generation;

import hudson.EnvVars;
import hudson.model.Executor;

class GenerationSettings {
    // Internal, used for CI SDK testing purposes generation.
    // CI SDK tests are run under pipelines, which don't have the same
    // execution context as traditional DSL job steps (though very close).
    // This means that getSetting doesn't work (since there isn't a current executor set for a pipeline job)
    // So for isTestGeneration, or where else we need special behavior for ci sdk testing, we defer to 
    // isSDKTest instead
    private static boolean isSDKTest = false

    // Retrieves a setting value through the environment of the current build
    private static String getSetting(String setting) {
        assert !isSDKTest : "Do not call if SDK test"
        EnvVars env = Executor.currentExecutor().getCurrentExecutable().getEnvironment()
        return env.get(setting, null)
    }
    
    private static boolean getSettingAsBoolean(String setting, boolean defaultValue) {
        String value = getSetting(setting)
        if (value == null) {
            return defaultValue
        }
        return Boolean.parseBoolean(value)
    }
    
    public static boolean isTestGeneration() {
        if (isSDKTest) {
            return true
        }
        else {
            return getSettingAsBoolean("IsTestGeneration", false)
        }
    }

    public static setSDKTest() {
        isSDKTest = true
    }
}
