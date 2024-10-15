package models.automation

case class CustomPropertyInfo(key: String,
                              name: Option[String],
                              description: Option[Option[String]],
                              required: Option[Boolean],
                              editableByUsers: Option[Boolean],
                              inTests: Option[Boolean],
                              inExports: Option[Boolean],
                              inSelfRegistration: Option[Boolean],
                              hidden: Option[Boolean],
                              allowedValues: Option[Option[List[KeyValueRequired]]],
                              displayOrder: Option[Short],
                              dependsOn: Option[Option[String]],
                              dependsOnValue: Option[Option[String]],
                              defaultValue: Option[Option[String]]
                             )