package models

case class Transaction(
                        sname: String,
                        fname: String,
                        description:Option[String],
                        domain:Long
                      ){
  def withDomainId(domain:Long) = {
    Transaction(this.sname, this.fname, this.description, domain)
  }
}