Akka Persistence Plugin Testkit
===============================

A compatibility testkit for Akka Persistence [storage plugins](http://doc.akka.io/docs/akka/2.3.0-RC4/scala/persistence.html#storage-plugins).

Current Status
--------------

- Basic test suite for journal plugins.
- No test suite yet for snapshot store plugins.

Installation
------------

### Testkit dependency

To include the testkit into your `sbt` project, add the following lines to `build.sbt`:

    resolvers += "krasserm at bintray" at "http://dl.bintray.com/krasserm/maven"

    libraryDependencies += "com.github.krasserm" %% "akka-persistence-testkit" % "0.1" % "test"

This version of the plugin depends on Akka 2.3.0-RC4 and Scala 2.10.3.

Implementation
--------------

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

Running
-------

Under `sbt`, the tests inherited from `JournalSpec` can be executed with

    > testOnly org.example.MyJournalSpec

Examples
--------

- [`LeveldbJournalNativeSpec`](https://github.com/krasserm/akka-persistence-testkit/blob/master/src/test/scala/akka/persistence/journal/leveldb/LeveldbJournalNativeSpec.scala) (this project)
- [`LeveldbJournalJavaSpec`](https://github.com/krasserm/akka-persistence-testkit/blob/master/src/test/scala/akka/persistence/journal/leveldb/LeveldbJournalJavaSpec.scala) (this project)
- [`CassandraJournalSpec`]((https://github.com/krasserm/akka-persistence-cassandra/blob/master/src/test/scala/akka/persistence/journal/cassandra/CassandraJournalSpec.scala)) (akka-persistence-cassandra)
