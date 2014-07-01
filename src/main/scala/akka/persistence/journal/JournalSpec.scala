package akka.persistence.journal

import scala.collection.immutable.Seq

import akka.actor._
import akka.persistence._
import akka.persistence.JournalProtocol._
import akka.testkit._

import com.typesafe.config._

object JournalSpec {
  val config = ConfigFactory.parseString(
    """
      |akka.persistence.publish-confirmations = on
      |akka.persistence.publish-plugin-commands = on
    """.stripMargin)
}

trait JournalSpec extends PluginSpec {
  implicit lazy val system = ActorSystem("JournalSpec", config.withFallback(JournalSpec.config))

  private var _senderProbe: TestProbe = _
  private var _receiverProbe: TestProbe = _

  def senderProbe: TestProbe = _senderProbe
  def receiverProbe: TestProbe = _receiverProbe

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    _senderProbe = TestProbe()
    _receiverProbe = TestProbe()
    writeMessages(1, 5, pid, senderProbe.ref)
  }

  def journal: ActorRef =
    extension.journalFor(null)

  def replayedMessage(snr: Long, deleted: Boolean = false, confirms: Seq[String] = Nil): ReplayedMessage =
    ReplayedMessage(PersistentImpl(s"a-${snr}", snr, pid, deleted, confirms, senderProbe.ref))

  def writeMessages(from: Int, to: Int, pid: String, sender: ActorRef): Unit = {
    val msgs = from to to map { i => PersistentRepr(payload = s"a-${i}", sequenceNr = i, persistenceId = pid, sender = sender) }
    val probe = TestProbe()

    journal ! WriteMessages(msgs, probe.ref, 1)

    probe.expectMsg(WriteMessagesSuccessful)
    from to to foreach { i =>
      probe.expectMsgPF() { case WriteMessageSuccess(PersistentImpl(payload, `i`, `pid`, _, _, `sender`), 1) => payload should be (s"a-${i}") }
    }
  }

  "A journal" must {
    "replay all messages" in {
      journal ! ReplayMessages(1, Long.MaxValue, Long.MaxValue, pid, receiverProbe.ref)
      1 to 5 foreach { i => receiverProbe.expectMsg(replayedMessage(i)) }
      receiverProbe.expectMsg(ReplayMessagesSuccess)
    }
    "replay messages using a lower sequence number bound" in {
      journal ! ReplayMessages(3, Long.MaxValue, Long.MaxValue, pid, receiverProbe.ref)
      3 to 5 foreach { i => receiverProbe.expectMsg(replayedMessage(i)) }
      receiverProbe.expectMsg(ReplayMessagesSuccess)
    }
    "replay messages using an upper sequence number bound" in {
      journal ! ReplayMessages(1, 3, Long.MaxValue, pid, receiverProbe.ref)
      1 to 3 foreach { i => receiverProbe.expectMsg(replayedMessage(i)) }
      receiverProbe.expectMsg(ReplayMessagesSuccess)
    }
    "replay messages using a count limit" in {
      journal ! ReplayMessages(1, Long.MaxValue, 3, pid, receiverProbe.ref)
      1 to 3 foreach { i => receiverProbe.expectMsg(replayedMessage(i)) }
      receiverProbe.expectMsg(ReplayMessagesSuccess)
    }
    "replay messages using a lower and upper sequence number bound" in {
      journal ! ReplayMessages(2, 4, Long.MaxValue, pid, receiverProbe.ref)
      2 to 4 foreach { i => receiverProbe.expectMsg(replayedMessage(i)) }
      receiverProbe.expectMsg(ReplayMessagesSuccess)
    }
    "replay messages using a lower and upper sequence number bound and a count limit" in {
      journal ! ReplayMessages(2, 4, 2, pid, receiverProbe.ref)
      2 to 3 foreach { i => receiverProbe.expectMsg(replayedMessage(i)) }
      receiverProbe.expectMsg(ReplayMessagesSuccess)
    }
    "replay a single if lower sequence number bound equals upper sequence number bound" in {
      journal ! ReplayMessages(2, 2, Long.MaxValue, pid, receiverProbe.ref)
      2 to 2 foreach { i => receiverProbe.expectMsg(replayedMessage(i)) }
      receiverProbe.expectMsg(ReplayMessagesSuccess)
    }
    "replay a single message if count limit equals 1" in {
      journal ! ReplayMessages(2, 4, 1, pid, receiverProbe.ref)
      2 to 2 foreach { i => receiverProbe.expectMsg(replayedMessage(i)) }
      receiverProbe.expectMsg(ReplayMessagesSuccess)
    }
    "not replay messages if count limit equals 0" in {
      journal ! ReplayMessages(2, 4, 0, pid, receiverProbe.ref)
      receiverProbe.expectMsg(ReplayMessagesSuccess)
    }
    "not replay messages if lower  sequence number bound is greater than upper sequence number bound" in {
      journal ! ReplayMessages(3, 2, Long.MaxValue, pid, receiverProbe.ref)
      receiverProbe.expectMsg(ReplayMessagesSuccess)
    }
    "not replay permanently deleted messages (range deletion)" in {
      val cmd = DeleteMessagesTo(pid, 3, true)
      val sub = TestProbe()

      journal ! cmd
      subscribe[DeleteMessagesTo](sub.ref)
      sub.expectMsg(cmd)

      journal ! ReplayMessages(1, Long.MaxValue, Long.MaxValue, pid, receiverProbe.ref)
      List(4, 5) foreach { i => receiverProbe.expectMsg(replayedMessage(i)) }
    }
    "replay logically deleted messages with deleted field set to true (range deletion)" in {
      val cmd = DeleteMessagesTo(pid, 3, false)
      val sub = TestProbe()

      journal ! cmd
      subscribe[DeleteMessagesTo](sub.ref)
      sub.expectMsg(cmd)

      journal ! ReplayMessages(1, Long.MaxValue, Long.MaxValue, pid, receiverProbe.ref, replayDeleted = true)
      1 to 5 foreach { i =>
        i match {
          case 1 | 2 | 3 => receiverProbe.expectMsg(replayedMessage(i, deleted = true))
          case 4 | 5     => receiverProbe.expectMsg(replayedMessage(i))
        }
      }
    }
    "return a highest stored sequence number > 0 if the processor has already written messages and the message log is non-empty" in {
      journal ! ReadHighestSequenceNr(3L, pid, receiverProbe.ref)
      receiverProbe.expectMsg(ReadHighestSequenceNrSuccess(5))

      journal ! ReadHighestSequenceNr(5L, pid, receiverProbe.ref)
      receiverProbe.expectMsg(ReadHighestSequenceNrSuccess(5))
    }
    "return a highest stored sequence number == 0 if the processor has not yet written messages" in {
      journal ! ReadHighestSequenceNr(0L, "non-existing-pid", receiverProbe.ref)
      receiverProbe.expectMsg(ReadHighestSequenceNrSuccess(0))
    }
  }
}

object LegacyJournalSpec {
  case class Confirmation(persistenceId: String, channelId: String, sequenceNr: Long) extends PersistentConfirmation
}

trait LegacyJournalSpec extends JournalSpec {
  import LegacyJournalSpec.Confirmation

  "A legacy journal" must {
    "not replay permanently deleted messages (individual deletion)" in {
      val msgIds = List(PersistentIdImpl(pid, 3), PersistentIdImpl(pid, 4))
      journal ! DeleteMessages(msgIds, true, Some(receiverProbe.ref))
      receiverProbe.expectMsg(DeleteMessagesSuccess(msgIds))

      journal ! ReplayMessages(1, Long.MaxValue, Long.MaxValue, pid, receiverProbe.ref)
      List(1, 2, 5) foreach { i => receiverProbe.expectMsg(replayedMessage(i)) }
      receiverProbe.expectMsg(ReplayMessagesSuccess)
    }
    "replay logically deleted messages with deleted field set to true (individual deletion)" in {
      val msgIds = List(PersistentIdImpl(pid, 3), PersistentIdImpl(pid, 4))
      journal ! DeleteMessages(msgIds, false, Some(receiverProbe.ref))
      receiverProbe.expectMsg(DeleteMessagesSuccess(msgIds))

      journal ! ReplayMessages(1, Long.MaxValue, Long.MaxValue, pid, receiverProbe.ref, replayDeleted = true)
      1 to 5 foreach { i =>
        i match {
          case 1 | 2 | 5 => receiverProbe.expectMsg(replayedMessage(i))
          case 3 | 4     => receiverProbe.expectMsg(replayedMessage(i, deleted = true))
        }
      }
    }
    "replay confirmed messages with corresponding channel ids contained in the confirmed field" in {
      val confs = List(Confirmation(pid, "c1", 3), Confirmation(pid, "c2", 3))
      val lpid = pid

      journal ! WriteConfirmations(confs, receiverProbe.ref)
      receiverProbe.expectMsg(WriteConfirmationsSuccess(confs))

      journal ! ReplayMessages(1, Long.MaxValue, Long.MaxValue, pid, receiverProbe.ref, replayDeleted = true)
      1 to 5 foreach { i =>
        i match {
          case 1 | 2 | 4 | 5 => receiverProbe.expectMsg(replayedMessage(i))
          case 3 => receiverProbe.expectMsgPF() {
            case ReplayedMessage(PersistentImpl(payload, `i`, `lpid`, false, confirms, _)) =>
              confirms should have length (2)
              confirms should contain ("c1")
              confirms should contain ("c2")
          }
        }
      }
    }
    "ignore orphan deletion markers" in {
      val msgIds = List(PersistentIdImpl(pid, 3), PersistentIdImpl(pid, 4))
      journal ! DeleteMessages(msgIds, true, Some(receiverProbe.ref)) // delete message
      receiverProbe.expectMsg(DeleteMessagesSuccess(msgIds))

      journal ! DeleteMessages(msgIds, false, Some(receiverProbe.ref)) // write orphan marker
      receiverProbe.expectMsg(DeleteMessagesSuccess(msgIds))

      journal ! ReplayMessages(1, Long.MaxValue, Long.MaxValue, pid, receiverProbe.ref)
      List(1, 2, 5) foreach { i => receiverProbe.expectMsg(replayedMessage(i)) }
    }
    "ignore orphan confirmation markers" in {
      val msgIds = List(PersistentIdImpl(pid, 3))
      journal ! DeleteMessages(msgIds, true, Some(receiverProbe.ref)) // delete message
      receiverProbe.expectMsg(DeleteMessagesSuccess(msgIds))

      val confs = List(Confirmation(pid, "c1", 3), Confirmation(pid, "c2", 3))
      journal ! WriteConfirmations(confs, receiverProbe.ref)
      receiverProbe.expectMsg(WriteConfirmationsSuccess(confs))

      journal ! ReplayMessages(1, Long.MaxValue, Long.MaxValue, pid, receiverProbe.ref)
      List(1, 2, 4, 5) foreach { i => receiverProbe.expectMsg(replayedMessage(i)) }
    }
  }
}
