package managers.`export`

class IdGenerator {

  private var idSequence: Int = 0

  def current(): Int = {
    idSequence
  }

  def next(): Int = {
    idSequence += 1
    idSequence
  }

}
