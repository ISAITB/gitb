package managers.`export`

class IdGenerator(initialIdSequenceValue: Int = 0) {

  private var idSequence: Int = initialIdSequenceValue

  def reset(newValue: Int): Int = {
    idSequence = newValue
    idSequence
  }

  def current(): Int = {
    idSequence
  }

  def next(): Int = {
    idSequence += 1
    idSequence
  }

}
