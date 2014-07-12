Akka Persistence Plugin Testkit
===============================

A compatibility testkit for Akka Persistence [storage plugins](http://doc.akka.io/docs/akka/2.3.4/scala/persistence.html#storage-plugins).

Installation
------------

### Testkit dependency

To include the testkit into your `sbt` project, add the following lines to `build.sbt`:

    resolvers += "krasserm at bintray" at "http://dl.bintray.com/krasserm/maven"

    libraryDependencies += "com.github.krasserm" %% "akka-persistence-testkit" % "0.3.4" % "test"

This version of the plugin depends on Akka 2.3.3 and is cross-built against Scala 2.10.4 and 2.11.0.

Implementation
--------------

### Journal plugins tests

The testkit defines an `akka.persistence.journal.JournalSpec` trait that can be implemented by plugin projects in the following way:

```scala
package org.example

import com.typesafe.config.ConfigFactory
import akka.persistence.journal.JournalSpec

class MyJournalSpec extends JournalSpec {
  lazy val config = ConfigFactory.parseString("...")

  override def beforeAll() {
    // before plugin initialization
    // ...

    // plugin initialization
    super.beforeAll()
  }

  override def afterAll() {
    // plugin shutdown
    super.afterAll()

    // after plugin shutdown
    // ...
  }
}
```

The plugin `config` must be defined as `lazy val`. It will be loaded by `JournalSpec` during initialization. Plugin initialization and shutdown can be customized by overriding `beforeAll()` and `afterAll()` (optional).

### Journal plugins tests for deprecated plugin API

`JournalSpec` only tests those part of the journal plugin API that are not deprecated in Akka 2.3.4. If you want to test the deprecated plugin API as well you should extend `LegacyJournalSpec` instead. This additionally tests your journal plugin to properly support channels and individual message deletions:

```scala
package org.example

import com.typesafe.config.ConfigFactory
import akka.persistence.journal.LegacyJournalSpec

class MyLegacyJournalSpec extends LegacyJournalSpec {
  lazy val config = ConfigFactory.parseString("...")

  override def beforeAll() {
    // before plugin initialization
    // ...

    // plugin initialization
    super.beforeAll()
  }

  override def afterAll() {
    // plugin shutdown
    super.afterAll()

    // after plugin shutdown
    // ...
  }
}
```

### Snapshot store plugin tests

In order to test snapshot store plugins, projects must implement `akka.persistence.snapshot.SnapshotStoreSpec`:

```scala
package org.example

import com.typesafe.config.ConfigFactory
import akka.persistence.snapshot.SnapshotStoreSpec

class MySnapshotStoreSpec extends SnapshotStoreSpec {
  lazy val config = ConfigFactory.parseString("...")

  override def beforeAll() {
    // before plugin initialization
    // ...

    // plugin initialization
    super.beforeAll()
  }

  override def afterAll() {
    // plugin shutdown
    super.afterAll()

    // after plugin shutdown
    // ...
  }
}
```

Running
-------

Under `sbt`, the tests inherited from `JournalSpec` and `SnapshotStoreSpec` can be executed with

    > testOnly org.example.MyJournalSpec org.example.MySnapshotStoreSpec

Examples
--------

- [`LeveldbJournalNativeSpec`](https://github.com/krasserm/akka-persistence-testkit/blob/master/src/test/scala/akka/persistence/journal/leveldb/LeveldbJournalNativeSpec.scala) (this project)
- [`LeveldbJournalJavaSpec`](https://github.com/krasserm/akka-persistence-testkit/blob/master/src/test/scala/akka/persistence/journal/leveldb/LeveldbJournalJavaSpec.scala) (this project)
- [`LocalSnapshotStoreSpec`](https://github.com/krasserm/akka-persistence-testkit/blob/master/src/test/scala/akka/persistence/snapshot/local/LocalSnapshotStoreSpec.scala) (this project)
- [`CassandraJournalSpec`](https://github.com/krasserm/akka-persistence-cassandra/blob/master/src/test/scala/akka/persistence/journal/cassandra/CassandraJournalSpec.scala) (akka-persistence-cassandra)
