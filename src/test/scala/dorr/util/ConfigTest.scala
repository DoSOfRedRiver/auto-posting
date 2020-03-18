package dorr.util

import java.time.LocalTime

import dorr.Configuration.Config
import dorr.MainPlugin
import izumi.distage.plugins.PluginConfig
import izumi.distage.testkit.TestConfig
import izumi.distage.testkit.scalatest.DistageSpecScalatest
import monix.eval.Task

class ConfigTest extends DistageSpecScalatest[Task] {
  override protected def config: TestConfig = super.config.copy(
    pluginConfig = PluginConfig.const(new MainPlugin)
  )

  "Config" should {
    "load without an error" in { v: Config => assert(v.schedule.queueInDays == 5); println(v.schedule.queueInDays) }

    "parse posting points correctly" in { (conf: Config) =>
      assert(
        conf.schedule.postingPoints == List(
          LocalTime.of(20, 0, 0, 0),
          LocalTime.of(18, 0, 0, 0),
          LocalTime.of(16, 0, 0, 0),
          LocalTime.of(14, 0, 0, 0),
          LocalTime.of(12, 0, 0, 0),
        )
      )
    }
  }
}
