package simulation.utils;

public class Constants {
    // Environment configuration name
    public static final String environmentConfig = System.getProperty("env","");
    //-Denv=value
    public static final String CONFIG_FILE_PATH = "src/resources/environment/"+ environmentConfig +"config.properties";

    // Additional constants can be added here
}
