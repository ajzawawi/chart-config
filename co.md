import org.apache.spark.sql.{Column, DataFrame}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._

/**
 * Represents a mapping from a source column to multiple target paths.
 * @param from Source column name
 * @param to Sequence of target paths (dot-notation for nested fields)
 */
case class Mapping(from: String, to: Seq[String])

object NestedColumnMapper {
  
  /**
   * Builds a nested struct column from a path and leaf value.
   * 
   * Example: buildStruct(List("a", "b", "c"), col("value"))
   * produces: struct(struct(struct(col("value") as "c") as "b") as "a")
   * 
   * @param path List of field names from root to leaf
   * @param value The column value to place at the leaf
   * @return A nested struct column
   */
  private def buildStruct(path: List[String], value: Column): Column =
    path.reverse.foldLeft(value)((acc, fieldName) => struct(acc.as(fieldName)))
  
  /**
   * Recursively upserts a value into a struct column at the specified path,
   * preserving all sibling fields.
   * 
   * @param structCol The struct column to update
   * @param structType The schema of the struct
   * @param path Remaining path segments to traverse
   * @param value The value to insert at the target location
   * @return Updated struct column
   */
  private def upsertIntoStruct(
      structCol: Column,
      structType: StructType,
      path: List[String],
      value: Column
  ): Column = path match {
    case Nil =>
      // Empty path - return unchanged
      structCol
      
    case leaf :: Nil =>
      // Base case: update or add the leaf field
      structCol.withField(leaf, value)
      
    case head :: tail =>
      // Recursive case: traverse deeper into the structure
      val childField = structType.fields.find(_.name == head)
      
      val nextChildCol = childField match {
        case Some(field) if field.dataType.isInstanceOf[StructType] =>
          // Child exists and is a struct - recurse into it
          val childStructType = field.dataType.asInstanceOf[StructType]
          upsertIntoStruct(structCol.getField(head), childStructType, tail, value)
          
        case _ =>
          // Child doesn't exist or isn't a struct - create new nested structure
          buildStruct(tail, value)
      }
      
      structCol.withField(head, nextChildCol)
  }
  
  /**
   * Sets a value at a nested path in a DataFrame, creating intermediate
   * struct columns as needed.
   * 
   * @param df Source DataFrame
   * @param targetPath Dot-separated path (e.g., "name.first.middle")
   * @param value Column value to set at the target path
   * @return DataFrame with the value set at the target path
   */
  private def setNested(df: DataFrame, targetPath: String, value: Column): DataFrame = {
    require(targetPath.nonEmpty, "Target path cannot be empty")
    
    val pathSegments = targetPath.split("\\.").toList
    
    pathSegments match {
      case topLevelField :: Nil =>
        // Simple case: top-level column
        df.withColumn(topLevelField, value)
        
      case topLevelField :: nestedPath =>
        // Complex case: nested column
        val topFieldExists = df.schema.fieldNames.contains(topLevelField)
        
        val updatedColumn = if (topFieldExists) {
          df.schema(topLevelField).dataType match {
            case structType: StructType =>
              // Existing struct - upsert into it
              upsertIntoStruct(col(topLevelField), structType, nestedPath, value)
              
            case _ =>
              // Existing non-struct field - replace with new nested structure
              buildStruct(nestedPath, value)
          }
        } else {
          // Field doesn't exist - create new nested structure
          buildStruct(nestedPath, value)
        }
        
        df.withColumn(topLevelField, updatedColumn)
        
      case Nil =>
        // Should never happen due to require, but handle gracefully
        df
    }
  }
  
  /**
   * Applies a sequence of mappings to a DataFrame, where each mapping
   * can fan out a source column to multiple target paths.
   * 
   * Example:
   * {{{
   * val mappings = Seq(
   *   Mapping("firstName", Seq("name.first", "special.student.name"))
   * )
   * val result = applyMappings(df, mappings)
   * }}}
   * 
   * @param df Source DataFrame
   * @param mappings Sequence of column mappings to apply
   * @return DataFrame with all mappings applied
   */
  def applyMappings(df: DataFrame, mappings: Seq[Mapping]): DataFrame = {
    mappings.foldLeft(df) { (currentDf, mapping) =>
      // Verify source column exists
      require(
        currentDf.schema.fieldNames.contains(mapping.from),
        s"Source column '${mapping.from}' does not exist in DataFrame"
      )
      
      // Apply all target mappings for this source column
      mapping.to.foldLeft(currentDf) { (intermediateDf, targetPath) =>
        setNested(intermediateDf, targetPath, col(mapping.from))
      }
    }
  }
}

// Usage example:
// import NestedColumnMapper._
// val mappings = Seq(
//   Mapping("firstName", Seq("name.first", "special.student.name"))
// )
// val resultDf = applyMappings(df, mappings)