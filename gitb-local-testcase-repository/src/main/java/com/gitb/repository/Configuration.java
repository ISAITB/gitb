package com.gitb.repository;

import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.SystemConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

/**
 * Created by senan on 10/2/14.
 */
public class Configuration {
    private String repositoryLocation;

    private Configuration(String location) {
        this.repositoryLocation = location;
    }

    public String getRepositoryLocation() {
        return repositoryLocation;
    }

    public static Configuration defaultConfiguration() {
        String location = null;
        CompositeConfiguration config = new CompositeConfiguration();

        try {
	        config.addConfiguration(new SystemConfiguration());
            config.addConfiguration(new Configurations().properties("local-testcase-repository-overridden.properties"));
            config.addConfiguration(new Configurations().properties("local-testcase-repository.properties"));

            location  = config.getString("repository.location");

            //check if local repository path is not provided
            if(location.startsWith("$")){
                throw new ConfigurationException();
            }

        } catch (ConfigurationException e) {
            //set {resource folder}/repository as the default repository location
            //if no global location is given
            location = config.getString("resource.location");
        }

        //get path in a safe way
        if(!location.endsWith("/")) {
            location = location + "/";
        }

        return new Configuration(location);
    }
}
