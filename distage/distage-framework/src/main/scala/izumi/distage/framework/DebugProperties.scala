package izumi.distage.framework

import izumi.fundamentals.platform.properties

object DebugProperties extends properties.DebugProperties {
  /**
    * Check if all `makeConfig[T]("config.path")` bindings from [[izumi.distage.config.ConfigModuleDef]] parse correctly
    * during plan checking. This will ensure that default configs are well-formed.
    *
    * Default: `true`
    */
  final val `izumi.distage.plancheck.check-config` = BoolProperty("izumi.distage.plancheck.check-config")

  /**
    * Print full plan when a problem is found during plan checking.
    *
    * Default: `false`, due to noisiness of large plan printouts
    */
  final val `izumi.distage.plancheck.print-plan` = BoolProperty("izumi.distage.plancheck.print-plan")

  /**
    * Prevent compile-time plan checks from failing the build and print warnings instead.
    *
    * Default: `false`
    */
  final val `izumi.distage.plancheck.onlywarn` = BoolProperty("izumi.distage.plancheck.onlywarn")

  final val `izumi.distage.plancheck.bruteforce` = BoolProperty("izumi.distage.plancheck.bruteforce")
  final val `izumi.distage.plancheck.bruteforce.max-activations` = StrProperty("izumi.distage.plancheck.max-activations")

  /** Print debug messages during plan checking. Default: `false` */
  final val `izumi.debug.macro.distage.plancheck` = BoolProperty("izumi.debug.macro.distage.plancheck")
}
