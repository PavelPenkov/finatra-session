package me.penkov.crypto

import java.security._
import java.util.Base64
import javax.crypto._
import javax.crypto.spec._

case class MessageEncryptor private (enc: Cipher, dec: Cipher, mac: MessageVerifier) {
  def encrypt(plaintext: Array[Byte]): Array[Byte] = enc.doFinal(plaintext)

  def decrypt(ciphertext: Array[Byte]): Array[Byte] = dec.doFinal(ciphertext)

  def encrypt(plaintext: String): String = Base64.getEncoder.encodeToString(enc.doFinal(Base64.getDecoder.decode(plaintext)))

  def decrypt(ciphertext: String): String = Base64.getEncoder.encodeToString(dec.doFinal(Base64.getDecoder.decode(ciphertext)))

  def encryptAndSign(plaintext: Array[Byte]): String = {
    // TODO: Use streams
    val ciphertext = encrypt(plaintext)
    val digest = mac.sign(ciphertext)
    s"${Base64.getEncoder.encodeToString(ciphertext)}--${Base64.getEncoder.encodeToString(digest)}"
  }

  def decryptAndVerify(signedCiphertext: String): Array[Byte] = {
    // TODO: Use streams
    signedCiphertext.split("--") match {
      case Array(ciphertext, digest) => {
        val ctb = Base64.getDecoder.decode(ciphertext)
        if (mac.verify(Base64.getDecoder.decode(digest), ctb)) dec.doFinal(ctb) else throw new SecurityException("Invalid session signature")
      }
    }
  }
}

case class MessageVerifier private(mac: Mac) {
  def sign(data: Array[Byte]) = mac.doFinal(data)

  def verify(digest: Array[Byte], data: Array[Byte]) = MessageDigest.isEqual(digest, sign(data))
}

object MessageVerifier {
  val alg = "HmacSHA1"

  def apply(secret: String) = {
    val spec = new SecretKeySpec(secret.getBytes, alg)
    val mac = Mac.getInstance(alg)
    mac.init(spec)
    new MessageVerifier(mac)
  }
}

object MessageEncryptor {
  val alg = "PBEWithMD5AndDES"
  val stretchIterations = 1000

  def apply(secret: String) = {
    val keySpec = new PBEKeySpec(secret.toCharArray)
    val factory = SecretKeyFactory.getInstance(alg)
    val encryptKey = factory.generateSecret(keySpec)
    val pSpec = new PBEParameterSpec(Array.fill(8)(0: Byte), stretchIterations)
    val (enc, dec) = (Cipher.getInstance(alg), Cipher.getInstance(alg))
    enc.init(Cipher.ENCRYPT_MODE, encryptKey, pSpec)
    dec.init(Cipher.DECRYPT_MODE, encryptKey, pSpec)
    new MessageEncryptor(enc, dec, MessageVerifier(secret))
  }
}

class MessageEncryptorPool(secret: String) extends ThreadLocal[MessageEncryptor] {
  override protected def initialValue = MessageEncryptor(secret)
}
