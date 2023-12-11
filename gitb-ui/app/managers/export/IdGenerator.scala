package managers.`export`

class IdGenerator(initialIdSequenceValue: Int = 0) {

  private var idSequence: Int = initialIdSequenceValue

  def current(): Int = {
    idSequence
  }

  def next(): Int = {
    idSequence += 1
    idSequence
  }

}
