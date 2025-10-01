private def addNestedField(
  df: DataFrame, 
  sourceField: String, 
  targetPath: String
): DataFrame = {
  
  val pathParts = targetPath.split("\\.")
  
  // For flat fields, just copy directly
  if (pathParts.length == 1) {
    return df.withColumn(targetPath, col(sourceField))
  }
  
  // For nested fields, build the struct expression
  val topLevel = pathParts.head
  val nestedPath = pathParts.tail
  
  // Build nested struct from innermost to outermost
  val nestedValue = nestedPath.reverse.foldLeft(col(sourceField)) { (acc, fieldName) =>
    struct(acc.as(fieldName))
  }
  
  // If the top-level column exists, we need to merge structures
  if (df.columns.contains(topLevel)) {
    // Extract existing fields from the struct
    val existingFields = df.schema(topLevel).dataType match {
      case structType: org.apache.spark.sql.types.StructType =>
        structType.fieldNames.toList.map(fieldName => col(s"$topLevel.$fieldName").as(fieldName))
      case _ =>
        List.empty[org.apache.spark.sql.Column]
    }
    
    // Add the new nested field
    val newField = nestedValue.as(nestedPath.head)
    
    // Create struct with all fields (existing + new)
    df.withColumn(topLevel, struct((existingFields :+ newField): _*))
  } else {
    df.withColumn(topLevel, nestedValue)
  }
}