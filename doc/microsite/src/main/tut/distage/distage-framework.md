distage-framework
=======================

@@toc { depth=2 }

### Roles

A "Role" is an entrypoint for a specific application hosted in a larger software suite.
Bundling multiple roles in a single `.jar` file can simplify deployment and operations.

Add `distage-framework` library for Roles API:

@@dependency[sbt,Maven,Gradle] {
  group="io.7mind.izumi"
  artifact="distage-framework_2.13"
  version="$izumi.version$"
}

With default @scaladoc[RoleAppMain](izumi.distage.roles.RoleAppMain), roles to launch are specified on the command-line:

```
./launcher :rolename1 :rolename2`
```

Only the components required by the specified roles will be created, everything else will be pruned. (see: @ref[Dependency Pruning](advanced-features.md#dependency-pruning))

@scaladoc[BundledRolesModule](izumi.distage.roles.bundled.BundledRolesModule) contains two example roles: 

- @scaladoc[Help](izumi.distage.roles.bundled.Help) - prints help message when launched `./launcher :help` 
- @scaladoc[ConfigWriter](izumi.distage.roles.bundled.ConfigWriter) - writes reference config into files, split by roles (includes only parts of the config used by the application)

Further reading: 
- [Roles: a viable alternative to Microservices and Monoliths](https://github.com/7mind/slides/blob/master/02-roles/roles.pdf)

### Typesafe Config

`distage-extension-config` library allows parsing case classes and sealed traits from `typesafe-config` configuration files.

To use it, add the `distage-extension-config` library:

@@dependency[sbt,Maven,Gradle] {
  group="io.7mind.izumi"
  artifact="distage-extension-config_2.13"
  version="$izumi.version$"
}

Use helper functions in `ConfigModuleDef`, `makeConfig` and `make[_].fromConfig` to parse the bound `AppConfig` instance into case classes, sealed traits or literals:

```scala mdoc:reset-object:to-string
import distage.{Id, Injector}
import distage.config.{AppConfig, ConfigModuleDef}
import com.typesafe.config.ConfigFactory

final case class Conf(
  name: String,
  age: Int,
)

final case class OtherConf(
  other: Boolean
)

final class ConfigPrinter(
  conf: Conf,
  otherConf: OtherConf @Id("other"),
) {
  def print() = {
    println(s"name: ${conf.name}, age: ${conf.age}, other: ${otherConf.other}")
  }
}

val module = new ConfigModuleDef {
  make[ConfigPrinter]

  // declare paths to parse
  makeConfig[Conf]("conf")
  makeConfig[OtherConf]("conf").named("other")

  // add config instance
  make[AppConfig].from(AppConfig(
    ConfigFactory.parseString(
      """conf {
        |  name = "John"
        |  age = 33
        |  other = true
        |}""".stripMargin
    )
  ))
}

Injector().produceRun(module) {
  configPrinter: ConfigPrinter =>
    configPrinter.print()
}
```

Automatic derivation of config codecs is based on [pureconfig-magnolia](https://github.com/pureconfig/pureconfig).

When a given type already has custom `pureconfig.ConfigReader` instances in implicit scope, they will be used, otherwise they will be derived automatically.

You don't have to explicitly `make[AppConfig]` in @ref[distage-testkit](distage-testkit.md)'s tests and in @ref[distage-framework](distage-framework.md)'s Roles, unless you want to override default behavior.

By default, tests and roles will try to read the configurations from resources with the following names, in order:

- `${roleName}.conf`
- `${roleName}-reference.conf`
- `${roleName}-reference-dev.conf`
- `application.conf`
- `application-reference.conf`
- `application-reference-dev.conf`
- `common.conf`
- `common-reference.conf`
- `common-reference-dev.conf`

```scala mdoc:reset:invisible
type _ref = izumi.distage.testkit.TestConfig
def _ref = (_: izumi.distage.testkit.TestConfig).configBaseName
```

Where `distage-testkit` uses @scaladoc[`TestConfig#configBaseName`](izumi.distage.testkit.TestConfig#configBaseName) instead of `roleName`.

Explicit config files passed to the role launcher `-c file.conf` the command-line flag have a higher priority than resource configs.

Role-specific configs on the command-line, passed via `-c file.conf` option *after* a `:roleName` argument, in turn have a higher priority than the explicit config files passed *before* the first `:role` argument.

Example:

```
  ./launcher -c global.conf :role1 -c role1.conf :role2 -c role2.conf
```

Here configs will be loaded in the following order, with higher priority earlier and earlier configs overriding the values in later configs:

  - explicits: `role1.conf`, `role2.conf`, `global.conf`,
  - resources: `role1[-reference,-dev].conf`, `role2[-reference,-dev].conf`, ,`application[-reference,-dev].conf`, `common[-reference,-dev].conf`

### Plugins

`distage-extension-plugins` module adds classpath discovery for modules that inherit a marker trait `PluginBase`.
Plugins enable extreme late-binding; e.g. they allow a program to extend itself at launch time with new `Plugin` classes
on the classpath. Plugins are compatible with @ref[compile-time checks](distage-framework.md#compile-time-checks) as long
as they're defined in a separate module.

To use plugins, first add the `distage-extension-plugins` library:

@@dependency[sbt,Maven,Gradle] {
  group="io.7mind.izumi"
  artifact="distage-extension-plugins_2.13"
  version="$izumi.version$"
}

Create a module extending the `PluginDef` trait instead of `ModuleDef`:

```scala mdoc:reset:invisible
import com.example.petstore._
```

```scala mdoc:fakepackage:to-string
"fakepackage com.example.petstore": Unit

import distage.Injector
import distage.plugins.{PluginConfig, PluginDef, PluginLoader}

object PetStorePlugin extends PluginDef {
  make[PetRepository]
  make[PetStoreService]
  make[PetStoreController]
}
```

Collect all the `PluginDef` classes and objects in a package:

```scala mdoc:to-string
val pluginConfig = PluginConfig.cached(
  // packages to scan
  packagesEnabled = Seq("com.example.petstore")
)

val appModules = PluginLoader().load(pluginConfig)
```

Wire the collected modules as usual:

```scala mdoc:to-string
// combine all modules into one

val appModule = appModules.result.merge

// launch

Injector()
  .produceGet[PetStoreController](appModule)
  .use(_.run())
```

### Compile-time checks

WIP on current `1.0.0-M1`. Version`1.0.0` will have finalized API.


- https://github.com/7mind/izumi/pull/1285
- (https://github.com/7mind/izumi/issues/1226)


- @scaladoc[izumi.distage.framework.PlanCheck](izumi.distage.framework.PlanCheck)
- @scaladoc[izumi.distage.framework.PlanCheckMacro](izumi.distage.framework.PlanCheckMacro)
- @scaladoc[izumi.distage.planning.solver.PlanVerifier](izumi.distage.planning.solver.PlanVerifier)
