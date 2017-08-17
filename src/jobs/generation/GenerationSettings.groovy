package jobs.generation;

import hudson.EnvVars;
import hudson.model.Executor;

class GenerationSettings {
    // Retrieves a setting value through the environment of the current build
    private static String getSetting(String setting) {
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
        return getSettingAsBoolean("IsTestGeneration", false)
    }

    public static String getServerName() {
        return getSetting("ServerName")
    }
}
