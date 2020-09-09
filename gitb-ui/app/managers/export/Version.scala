package managers.export

class Version(_major: Int, _minor: Int, _patch: Int) extends Comparable[Version] {
  var major: Int = _major
  var minor: Int = _minor
  var patch: Int = _patch

  override def toString: String = {
    major+"."+minor+"."+patch
  }

  override def hashCode(): Int = {
    toString.hashCode
  }

  override def equals(other: Any): Boolean = {
    other.isInstanceOf[Version] &&
      other.asInstanceOf[Version].major == this.major &&
      other.asInstanceOf[Version].minor == this.minor &&
      other.asInstanceOf[Version].patch == this.patch
  }

  override def compareTo(other: Version): Int = {
    if (this.major > other.major) {
      1
    } else if (this.major < other.major) {
      -1
    } else {
      if (this.minor > other.minor) {
        1
      } else if (this.minor < other.minor) {
        -1
      } else {
        if (this.patch > other.patch) {
          1
        } else if (this.patch < other.patch) {
          -1
        } else {
          0
        }
      }
    }
  }
}
