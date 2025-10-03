import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import com.typesafe.config.Config

class MappingProcessor(config: Config) extends Processor {
  
  private val mappingConfig: MappingConfig = MappingConfig.fromConfig(config)
  
  override val name: String = 
    if (config.hasPath("id")) config.getString("id") 
    else "MappingProcessor"
  
  override def process(
    dataFrame: DataFrame, 
    rules: List[Rule], 
    rulesetId: String
  )(implicit spark: SparkSession): DataFrame = {
    
    var resultDf = dataFrame
    
    mappingConfig.mappings.foreach { mapping =>
      val sourceColumn = determineSourceColumn(dataFrame, mapping)
      
      sourceColumn.foreach { source =>
        mapping.to.foreach { targetPath =>
          resultDf = addNestedField(resultDf, source, targetPath)
        }
      }
    }
    
    resultDf
  }
  
  /**
   * Determine the source column based on the mapping rule type
   */
  private def determineSourceColumn(
    df: DataFrame, 
    mapping: MappingRule
  ): Option[org.apache.spark.sql.Column] = {
    
    (mapping.from, mapping.value, mapping.transform) match {
      // Case 1: Direct field mapping
      case (Some(field), None, None) if df.columns.contains(field) =>
        Some(col(field))
      
      // Case 2: Static value
      case (None, Some(value), None) =>
        Some(castLiteralValue(value, mapping.`type`))
      
      // Case 3: Transformation
      case (Some(field), None, Some(transform)) if df.columns.contains(field) =>
        Some(applyTransformation(col(field), transform, df))
      
      // Invalid configuration
      case _ => None
    }
  }
  
  /**
   * Cast a string value to the appropriate type
   */
  private def castLiteralValue(value: String, dataType: Option[String]): org.apache.spark.sql.Column = {
    dataType.map(_.toLowerCase) match {
      case Some("int") | Some("integer") => lit(value.toInt)
      case Some("long") => lit(value.toLong)
      case Some("double") => lit(value.toDouble)
      case Some("float") => lit(value.toFloat)
      case Some("boolean") | Some("bool") => lit(value.toBoolean)
      case Some("string") | None => lit(value)
      case Some(other) => 
        throw new IllegalArgumentException(s"Unsupported data type: $other")
    }
  }
  
  /**
   * Apply a transformation function to a column
   */
  private def applyTransformation(
    column: org.apache.spark.sql.Column, 
    transform: String,
    df: DataFrame
  ): org.apache.spark.sql.Column = {
    
    val parts = transform.split(":")
    val functionName = parts(0)
    val args = parts.drop(1)
    
    functionName.toLowerCase match {
      // String transformations
      case "lowercase" | "tolowercase" => lower(column)
      case "uppercase" | "touppercase" => upper(column)
      case "trim" => trim(column)
      case "removespaces" => regexp_replace(column, "\\s+", "")
      
      // Numeric transformations
      case "multiply" if args.length == 1 => column * lit(args(0).toDouble)
      case "divide" if args.length == 1 => column / lit(args(0).toDouble)
      case "add" if args.length == 1 => column + lit(args(0).toDouble)
      case "subtract" if args.length == 1 => column - lit(args(0).toDouble)
      case "round" if args.length == 1 => round(column, args(0).toInt)
      case "abs" => abs(column)
      
      // String operations
      case "concat" if args.length >= 1 => 
        // concat:field1:field2:separator
        val fields = args.dropRight(1).map(f => col(f))
        val separator = if (args.length > 1) args.last else ""
        concat_ws(separator, fields: _*)
      
      case "substring" if args.length == 2 => 
        // substring:start:length
        substring(column, args(0).toInt, args(1).toInt)
      
      case "replace" if args.length == 2 => 
        // replace:oldValue:newValue
        regexp_replace(column, args(0), args(1))
      
      // Date transformations
      case "dateformat" if args.length == 1 => 
        // dateformat:yyyy-MM-dd
        date_format(column, args(0))
      
      // Conditional transformations
      case "coalesce" if args.length >= 1 =>
        // coalesce:field1:field2:defaultValue
        val coalesceCols = args.dropRight(1).map(f => col(f)) :+ lit(args.last)
        coalesce(column +: coalesceCols: _*)
      
      case "whennull" if args.length == 1 =>
        // whennull:defaultValue
        when(column.isNull, lit(args(0))).otherwise(column)
      
      case _ => 
        throw new IllegalArgumentException(s"Unsupported transformation: $transform")
    }
  }
  
  /**
   * Adds a nested field to the DataFrame
   */
  private def addNestedField(
    df: DataFrame, 
    sourceCol: org.apache.spark.sql.Column, 
    targetPath: String
  ): DataFrame = {
    
    val pathParts = targetPath.split("\\.")
    
    if (pathParts.length == 1) {
      return df.withColumn(targetPath, sourceCol)
    }
    
    val topLevel = pathParts.head
    val nestedPath = pathParts.tail
    
    val nestedValue = nestedPath.reverse.foldLeft(sourceCol) { (acc, fieldName) =>
      struct(acc.as(fieldName))
    }
    
    if (df.columns.contains(topLevel)) {
      val existingFieldsMap = df.schema(topLevel).dataType match {
        case structType: org.apache.spark.sql.types.StructType =>
          structType.fieldNames.map(fieldName => 
            (fieldName, col(s"$topLevel.$fieldName").as(fieldName))
          ).toMap
        case _ =>
          Map.empty[String, org.apache.spark.sql.Column]
      }
      
      val newFieldName = nestedPath.head
      val newField = nestedValue.as(newFieldName)
      val updatedFieldsMap = existingFieldsMap + (newFieldName -> newField)
      
      val allFields = updatedFieldsMap.values.toList
      df.withColumn(topLevel, struct(allFields: _*))
    } else {
      df.withColumn(topLevel, nestedValue)
    }
  }
}

object MappingProcessor {
  def apply(config: Config): MappingProcessor = new MappingProcessor(config)
}