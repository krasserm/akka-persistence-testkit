package akka.persistence.journal.leveldb

import java.io.File

import org.apache.commons.io.FileUtils

import akka.persistence.journal.JournalSpec

trait LeveldbJournalCleanup extends JournalSpec {
  val storageLocations = List(
    "akka.persistence.journal.leveldb.dir",
    "akka.persistence.snapshot-store.local.dir").map(s ⇒ new File(system.settings.config.getString(s)))

  override def beforeAll() {
    storageLocations.foreach(FileUtils.deleteDirectory)
    super.beforeAll()
  }

  override def afterAll() {
    super.afterAll()
    storageLocations.foreach(FileUtils.deleteDirectory)
  }
}
