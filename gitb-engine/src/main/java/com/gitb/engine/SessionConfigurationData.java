/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package com.gitb.engine;

import com.gitb.core.ActorConfiguration;

import java.util.ArrayList;
import java.util.List;

import static com.gitb.PropertyConstants.*;

public class SessionConfigurationData {

    private final List<ActorConfiguration> actorConfigurations = new ArrayList<>();
    private ActorConfiguration domainConfiguration;
    private ActorConfiguration organisationConfiguration;
    private ActorConfiguration systemConfiguration;
    private final List<ActorConfiguration> testServiceConfigurations = new ArrayList<>();

    public SessionConfigurationData(List<ActorConfiguration> allConfigurations) {
        if (allConfigurations != null) {
            for (ActorConfiguration configuration: allConfigurations) {
                if (ACTOR_CONFIG_DOMAIN.equals(configuration.getActor())) {
                    domainConfiguration = configuration;
                } else if (ACTOR_CONFIG_ORGANISATION.equals(configuration.getActor())) {
                    organisationConfiguration = configuration;
                } else if (ACTOR_CONFIG_SYSTEM.equals(configuration.getActor())) {
                    systemConfiguration = configuration;
                } else if (configuration.getActor().startsWith(ACTOR_CONFIG_TEST_SERVICE)) {
                    // Passed as 'com.gitb.TEST_SERVICE|serviceTestKey'
                    var serviceConfig = new ActorConfiguration();
                    serviceConfig.setActor(configuration.getActor().substring(configuration.getActor().indexOf(ACTOR_CONFIG_TEST_SERVICE_SEPARATOR)+1));
                    serviceConfig.getConfig().addAll(configuration.getConfig());
                    testServiceConfigurations.add(serviceConfig);
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

    public List<ActorConfiguration> getTestServiceConfigurations() {
        return testServiceConfigurations;
    }
}
