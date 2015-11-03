import me.penkov.crypto.MessageEncryptor
import org.scalameter.api._
import org.scalameter.picklers.noPickler._

object MessageEncryptorBenchmark extends Bench.LocalTime {
  val gen = Gen.single("k")(10000)
  val me = MessageEncryptor("password")
  val message = Array.ofDim[Byte](256)

  performance of "MessageEncryptor" in {
    measure method "encrytAndSign" in {
      using(gen) in { k =>
        var i = 0
        while(i < k) {
          me.encryptAndSign(message)
          i += 1
        }
      }
    }
  }
}
