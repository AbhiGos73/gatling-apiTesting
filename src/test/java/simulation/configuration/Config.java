package simulation.configuration;

import simulation.utils.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

public class Config {

    private final Properties configProperties;

    public Config() throws IOException {
        this.configProperties = new Properties();
        String fileName = Constants.environmentConfig + ".config.properties";
        String targetEnvConfigFilePath = "environment/" + fileName;
        this.loadPropertiesFile(targetEnvConfigFilePath);
    }

    private void loadPropertiesFile(String filePath) throws IOException {
        URL resourceUrl = getClass().getClassLoader().getResource(filePath);
        if (resourceUrl == null) {
            throw new IOException("Resource not found: " + filePath);
        }
        try (InputStream input = resourceUrl.openStream()) {
            configProperties.load(input);
            /*for (String name : configProperties.stringPropertyNames()) {
                System.out.println(name + " = " + configProperties.getProperty(name));
            }*/
        }
    }

    public String getProperty(String name) {
        return configProperties.getProperty(name);
    }

    public void setProperty(String name, String value) {
        configProperties.setProperty(name, value);
    }
}
