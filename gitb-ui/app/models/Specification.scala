package models

case class Specifications(id: Long, shortname:String, fullname:String, urls:Option[String], diagram:Option[String],
                          description:Option[String], specificationType:Short, domain:Long) {

  def withDomainId(domain:Long):Specifications = {
    Specifications(this.id, this.shortname, this.fullname, this.urls, this.diagram, this.description,
                   this.specificationType, domain)
  }
}

class Specification(_id: Long, _sname: String, _fname: String, _urls: Option[List[String]], _diagram: Option[String],
                         _description:Option[String], _stype:Short, _actors:Option[List[Actors]])
{
	var id: Long = _id
  var shortname:String = _sname
  var fullname:String  = _fname
  var urls:Option[List[String]] = _urls
  var diagram:Option[String] = _diagram
  var description:Option[String] = _description
  var specificationType:Short = _stype
  var actors:Option[List[Actors]] = _actors

  def this(_case:Specifications) = {
    this(_case.id, _case.shortname, _case.fullname,
      if(_case.urls.isDefined) Some(_case.urls.get.split(",").toList) else None,
      _case.diagram, _case.description, _case.specificationType, None)
  }

  def this(_case:Specifications, actors:List[Actors]) = {
    this(_case.id, _case.shortname, _case.fullname,
      if(_case.urls.isDefined) Some(_case.urls.get.split(",").toList) else None,
      _case.diagram, _case.description, _case.specificationType, Some(actors))
  }

  def toCaseObject:Specifications = {
    Specifications(this.id, this.shortname, this.fullname,
      if(this.urls.isDefined) Some(this.urls.get.mkString(",")) else None ,
      this.diagram, this.description, this.specificationType, 0l)
  }

}

