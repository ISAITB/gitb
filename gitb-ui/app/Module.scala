import com.google.inject.AbstractModule
import hooks.{BeforeStartHook, OnStopHook}

class Module extends AbstractModule{

  override def configure() = {
    bind(classOf[BeforeStartHook]).asEagerSingleton()
    bind(classOf[OnStopHook]).asEagerSingleton()
  }
}
