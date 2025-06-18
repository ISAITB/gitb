/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package managers.export

class Version(_major: Int, _minor: Int, _patch: Int) extends Comparable[Version] {
  var major: Int = _major
  var minor: Int = _minor
  var patch: Int = _patch

  override def toString: String = {
    s"$major.$minor.$patch"
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
