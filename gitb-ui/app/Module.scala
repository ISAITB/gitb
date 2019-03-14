import com.google.inject.AbstractModule
import hooks.{BeforeStartHook, OnStopHook, PostStartHook}

class Module extends AbstractModule{

  override def configure() = {
    bind(classOf[BeforeStartHook]).asEagerSingleton()
    bind(classOf[PostStartHook]).asEagerSingleton()
    bind(classOf[OnStopHook]).asEagerSingleton()
  }
}
