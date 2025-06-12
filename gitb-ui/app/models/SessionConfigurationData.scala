package models

import com.gitb.core.ActorConfiguration

import java.util

case class SessionConfigurationData(statementParameters: Option[List[TypedActorConfiguration]],
                                    domainParameters: Option[TypedActorConfiguration],
                                    organisationParameters: Option[TypedActorConfiguration],
                                    systemParameters: Option[TypedActorConfiguration]) {

  def apply(aggregatedConfiguration: util.List[ActorConfiguration]): Unit = {
    import scala.jdk.CollectionConverters._
    if (statementParameters.nonEmpty) {
      aggregatedConfiguration.addAll(statementParameters.get.map(toActorConfiguration).asJava)
    }
    if (domainParameters.nonEmpty) {
      aggregatedConfiguration.add(toActorConfiguration(domainParameters.get))
    }
    if (organisationParameters.nonEmpty) {
      aggregatedConfiguration.add(toActorConfiguration(organisationParameters.get))
    }
    if (systemParameters.nonEmpty) {
      aggregatedConfiguration.add(toActorConfiguration(systemParameters.get))
    }

  }

  private def toActorConfiguration(typedActorConfiguration: TypedActorConfiguration) = {
    val actorConfiguration = new ActorConfiguration()
    actorConfiguration.setActor(typedActorConfiguration.actor)
    actorConfiguration.setEndpoint(typedActorConfiguration.endpoint)
    import scala.jdk.CollectionConverters._
    actorConfiguration.getConfig.addAll(typedActorConfiguration.config.map(_.data).asJava)
    actorConfiguration
  }

}
