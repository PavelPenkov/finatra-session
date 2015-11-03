import java.io.{ByteArrayOutputStream, ObjectOutputStream}
import java.util.Base64
import me.penkov.crypto.MessageEncryptor
import scala.util.Random
import org.scalatest._
import org.scalatest.Matchers._

class MessageEncryptorTest extends FunSuite with BeforeAndAfterEach {
  val secret = "password"
  var me = MessageEncryptor(secret)
  val shortMessage = Array.fill(16)(0: Byte)
  val longerMessage = {
    val os = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(os)
    oos.writeObject(Map("name" -> "Vasya"))
    oos.close()
    os.toByteArray
  }

  val rng = new Random()

  override def beforeEach(): Unit = {
    me = MessageEncryptor(secret)
  }

  test("should encrypt byte array") {
    val ciphertext = me.encrypt(shortMessage)

    me.encrypt(shortMessage) should not be (null)
  }

  test("should decrypt ciphertext") {
    val ciphertext = me.encrypt(shortMessage)
    val plaintext = me.decrypt(ciphertext)

    for(i <- shortMessage.indices) {
      shortMessage(i) should equal (plaintext(i))
    }
  }

  test("should decrypt longer ciphertext") {
    val ciphertext = me.encrypt(longerMessage)
    val plaintext = me.decrypt(ciphertext)

    for(i <- longerMessage.indices) {
      longerMessage(i) should equal (plaintext(i))
    }
  }

  test("should verify short signed text") {
    val text = me.encryptAndSign(shortMessage)
    val plaintext = me.decryptAndVerify(text)

    for(i <- shortMessage.indices) {
      shortMessage(i) should equal (plaintext(i))
    }
  }

  test("should verify longer text") {
    val text = me.encryptAndSign(longerMessage)
  }

  test("should reject random Base64 data") {
    val buf = Array.ofDim[Byte](32)
    rng.nextBytes(buf)
    val randomSession = s"${Base64.getEncoder.encodeToString(buf)}--${Base64.getEncoder.encodeToString(buf)}"
    intercept[java.lang.SecurityException] {
      me.decryptAndVerify(randomSession)
    }
  }

  test("should reject random data") {
    pendingUntilFixed {
      val randomString = (1 to 32).map { _ => rng.nextPrintableChar } mkString ""
      me.decryptAndVerify(randomString)
    }
  }

  test("should reject tampered data") {
    val session = me.encryptAndSign(shortMessage)
    val chars = session.toCharArray
    if(chars(0) == 'a') {
      chars(0) = 'b'
    } else {
      chars(0) = 'a'
    }
    intercept[java.lang.SecurityException] {
      me.decryptAndVerify(chars.mkString)
    }
  }
}
