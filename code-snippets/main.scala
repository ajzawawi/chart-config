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


def flattenMappings(mapping: Map[String, Any], prefix: String = ""): Map[String, String] = {
  mapping.toList.flatMap {
    case (fieldName, fieldDef: Map[_, _]) =>
      val defMap = fieldDef.asInstanceOf[Map[String, Any]]
      val fullName = if (prefix.isEmpty) fieldName else s"$prefix.$fieldName"

      defMap.get("type") match {
        case Some("object") | Some("nested") =>
          defMap.get("properties") match {
            case Some(nestedProps: Map[_, _]) =>
              flattenMappings(nestedProps.asInstanceOf[Map[String, Any]], fullName)
            case _ =>
              // edge case: object with no properties
              Map.empty
          }

        case Some(fieldType: String) =>
          Map(fullName -> fieldType)

        case _ =>
          Map.empty
      }

    case _ => Map.empty
  }.toMap
}
