package akka.persistence.journal.leveldb

import scala.collection.immutable.Seq

import com.typesafe.config.ConfigFactory

import akka.persistence._
import akka.persistence.journal._
import akka.persistence.PluginCleanup

class LeveldbJournalNoLegacySpec extends JournalSpec with PluginCleanup {
  lazy val config = ConfigFactory.parseString(
    """
      |akka.persistence.journal.plugin = "akka.persistence.journal.leveldb"
      |akka.persistence.journal.leveldb.class = "akka.persistence.journal.leveldb.LeveldbJournalNoLegacy"
      |akka.persistence.journal.leveldb.native = off
      |akka.persistence.journal.leveldb.dir = "target/journal-java"
      |akka.persistence.snapshot-store.local.dir = "target/snapshots-java/"
    """.stripMargin)

}

class LeveldbJournalNoLegacy extends LeveldbJournal {
  override def deleteMessages(messageIds: Seq[PersistentId], permanent: Boolean): Unit =
    throw new UnsupportedOperationException("not supported")

  override def writeConfirmations(confirmations: Seq[PersistentConfirmation]): Unit =
    throw new UnsupportedOperationException("not supported")
}
