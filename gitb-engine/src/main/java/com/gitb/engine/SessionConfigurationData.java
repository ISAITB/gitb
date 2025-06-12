package com.gitb.engine;

import com.gitb.core.ActorConfiguration;

import java.util.ArrayList;
import java.util.List;

public class SessionConfigurationData {

    private final List<ActorConfiguration> actorConfigurations = new ArrayList<>();
    private ActorConfiguration domainConfiguration;
    private ActorConfiguration organisationConfiguration;
    private ActorConfiguration systemConfiguration;

    public SessionConfigurationData(List<ActorConfiguration> allConfigurations) {
        if (allConfigurations != null) {
            for (ActorConfiguration configuration: allConfigurations) {
                if ("com.gitb.DOMAIN".equals(configuration.getActor())) {
                    domainConfiguration = configuration;
                } else if ("com.gitb.ORGANISATION".equals(configuration.getActor())) {
                    organisationConfiguration = configuration;
                } else if ("com.gitb.SYSTEM".equals(configuration.getActor())) {
                    systemConfiguration = configuration;
                } else {
                    actorConfigurations.add(configuration);
                }
            }
        }
    }

    public List<ActorConfiguration> getActorConfigurations() {
        return actorConfigurations;
    }

    public ActorConfiguration getDomainConfiguration() {
        return domainConfiguration;
    }

    public ActorConfiguration getOrganisationConfiguration() {
        return organisationConfiguration;
    }

    public ActorConfiguration getSystemConfiguration() {
        return systemConfiguration;
    }
}
