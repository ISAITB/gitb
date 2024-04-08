package models

import utils.MimeUtil

case class CommunityKeystore(id: Long, keystoreFile: String, keystoreType: String, keystorePassword: String, keyPassword: String, community: Long) {

  def withDecryptedKeys(): CommunityKeystore = {
    CommunityKeystore(id, keystoreFile, keystoreType,
      MimeUtil.decryptString(keystorePassword),
      MimeUtil.decryptString(keyPassword),
      community)
  }

}
