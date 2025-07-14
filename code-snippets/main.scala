val sample: Map[String, Any] = Map(
  "name" -> Map(
    "type" -> "text",
    "fields" -> Map(
      "keyword" -> Map("type" -> "keyword")
    )
  ),
  "age" -> Map("type" -> "integer"),
  "productDetails" -> Map(
    "type" -> "nested",
    "properties" -> Map(
      "openQuantity" -> Map("type" -> "double"),
      "sku" -> Map("type" -> "keyword"),
      "manufacturer" -> Map(
        "type" -> "object",
        "properties" -> Map(
          "name" -> Map("type" -> "text"),
          "country" -> Map("type" -> "keyword")
        )
      )
    )
  )
)


object MappingUtils {

  def flattenMappings(mapping: Map[String, Any], prefix: String = ""): Map[String, String] = {
    mapping.toList.flatMap {
      case (fieldName, fieldDef: Map[_, _]) =>
        val defMap = fieldDef.asInstanceOf[Map[String, Any]]
        val fullName = if (prefix.isEmpty) fieldName else s"$prefix.$fieldName"

        val baseField: Option[(String, String)] =
          defMap.get("type").collect {
            case fieldType: String => fullName -> fieldType
          }

        val nestedFields: Map[String, String] =
          defMap.get("properties") match {
            case Some(nestedProps: Map[_, _]) =>
              flattenMappings(nestedProps.asInstanceOf[Map[String, Any]], fullName)
            case _ => Map.empty
          }

        val multiFields: Map[String, String] =
          defMap.get("fields") match {
            case Some(fields: Map[_, _]) =>
              fields.asInstanceOf[Map[String, Map[String, Any]]].collect {
                case (subField, subDef) if subDef.contains("type") =>
                  s"$fullName.$subField" -> subDef("type").toString
              }
            case _ => Map.empty
          }

        baseField.toMap ++ nestedFields ++ multiFields

      case _ => Map.empty
    }.toMap
  }

}
