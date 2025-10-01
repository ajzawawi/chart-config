import com.holdenkarau.spark.testing.{DataFrameSuiteBase, SharedSparkContext}
import com.typesafe.config.ConfigFactory
import org.apache.spark.sql.Row
import org.apache.spark.sql.types._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterEach

class MappingProcessorSpec 
  extends AnyFunSpec 
  with Matchers 
  with BeforeAndAfterEach 
  with DataFrameSuiteBase
  with SharedSparkContext {
  
  override def beforeEach(): Unit = {
    super.beforeEach()
  }
  
  override def afterEach(): Unit = {
    super.afterEach()
  }
  
  describe("MappingProcessor") {
    
    it("should map a single source field to a single target field") {
      val config = ConfigFactory.parseString("""
        mappings = [
          { from = "firstName", to = ["name"] }
        ]
      """)
      
      val processor = new MappingProcessor(config)
      
      import spark.implicits._
      val inputDf = Seq(
        ("John", 30),
        ("Jane", 25)
      ).toDF("firstName", "age")
      
      val result = processor.process(inputDf, List.empty, "test-ruleset")
      
      assert(result.columns.contains("name"))
      assert(result.select("name").collect().map(_.getString(0)) === Array("John", "Jane"))
    }
    
    it("should map a single source field to multiple target fields") {
      val config = ConfigFactory.parseString("""
        mappings = [
          { from = "firstName", to = ["name", "displayName"] }
        ]
      """)
      
      val processor = new MappingProcessor(config)
      
      import spark.implicits._
      val inputDf = Seq(
        ("John")
      ).toDF("firstName")
      
      val result = processor.process(inputDf, List.empty, "test-ruleset")
      
      assert(result.columns.contains("name"))
      assert(result.columns.contains("displayName"))
      assert(result.select("name").first().getString(0) === "John")
      assert(result.select("displayName").first().getString(0) === "John")
    }
    
    it("should map a source field to a single-level nested field") {
      val config = ConfigFactory.parseString("""
        mappings = [
          { from = "firstName", to = ["name.first"] }
        ]
      """)
      
      val processor = new MappingProcessor(config)
      
      import spark.implicits._
      val inputDf = Seq(
        ("John")
      ).toDF("firstName")
      
      val result = processor.process(inputDf, List.empty, "test-ruleset")
      
      assert(result.columns.contains("name"))
      
      val row = result.first()
      val nameStruct = row.getAs[Row]("name")
      assert(nameStruct.getAs[String]("first") === "John")
    }
    
    it("should map a source field to a multi-level nested field") {
      val config = ConfigFactory.parseString("""
        mappings = [
          { from = "fieldA", to = ["fieldB.fieldC.fieldD"] }
        ]
      """)
      
      val processor = new MappingProcessor(config)
      
      import spark.implicits._
      val inputDf = Seq(
        ("testValue")
      ).toDF("fieldA")
      
      val result = processor.process(inputDf, List.empty, "test-ruleset")
      
      assert(result.columns.contains("fieldB"))
      
      val row = result.first()
      val fieldB = row.getAs[Row]("fieldB")
      val fieldC = fieldB.getAs[Row]("fieldC")
      assert(fieldC.getAs[String]("fieldD") === "testValue")
    }
    
    it("should map multiple source fields to the same nested structure") {
      val config = ConfigFactory.parseString("""
        mappings = [
          { from = "firstName", to = ["name.first"] }
          { from = "lastName", to = ["name.last"] }
        ]
      """)
      
      val processor = new MappingProcessor(config)
      
      import spark.implicits._
      val inputDf = Seq(
        ("John", "Doe")
      ).toDF("firstName", "lastName")
      
      val result = processor.process(inputDf, List.empty, "test-ruleset")
      
      assert(result.columns.contains("name"))
      
      val row = result.first()
      val nameStruct = row.getAs[Row]("name")
      
      assert(nameStruct.getAs[String]("first") === "John")
      assert(nameStruct.getAs[String]("last") === "Doe")
    }
    
    it("should map to both flat and nested fields simultaneously") {
      val config = ConfigFactory.parseString("""
        mappings = [
          { from = "firstName", to = ["name.first", "fullName"] }
          { from = "lastName", to = ["name.last", "fullName"] }
        ]
      """)
      
      val processor = new MappingProcessor(config)
      
      import spark.implicits._
      val inputDf = Seq(
        ("John", "Doe")
      ).toDF("firstName", "lastName")
      
      val result = processor.process(inputDf, List.empty, "test-ruleset")
      
      assert(result.columns.contains("name"))
      assert(result.columns.contains("fullName"))
      
      val row = result.first()
      val nameStruct = row.getAs[Row]("name")
      
      assert(nameStruct.getAs[String]("first") === "John")
      assert(nameStruct.getAs[String]("last") === "Doe")
      // fullName will be overwritten by lastName (last mapping wins)
      assert(row.getAs[String]("fullName") === "Doe")
    }
    
    it("should handle missing source fields gracefully") {
      val config = ConfigFactory.parseString("""
        mappings = [
          { from = "nonExistentField", to = ["target"] }
          { from = "firstName", to = ["name"] }
        ]
      """)
      
      val processor = new MappingProcessor(config)
      
      import spark.implicits._
      val inputDf = Seq(
        ("John")
      ).toDF("firstName")
      
      val result = processor.process(inputDf, List.empty, "test-ruleset")
      
      assert(!result.columns.contains("target"))
      assert(result.columns.contains("name"))
      assert(result.select("name").first().getString(0) === "John")
    }
    
    it("should handle null values in source fields") {
      val config = ConfigFactory.parseString("""
        mappings = [
          { from = "firstName", to = ["name.first"] }
        ]
      """)
      
      val processor = new MappingProcessor(config)
      
      val inputSchema = StructType(Seq(
        StructField("firstName", StringType, nullable = true)
      ))
      
      val inputData = Seq(Row(null))
      
      import scala.jdk.CollectionConverters._
      val inputDf = spark.createDataFrame(
        inputData.asJava,
        inputSchema
      )
      
      val result = processor.process(inputDf, List.empty, "test-ruleset")
      
      assert(result.columns.contains("name"))
      
      val row = result.first()
      val nameStruct = row.getAs[Row]("name")
      assert(nameStruct.get(0) === null)
    }
    
    it("should handle empty mappings list") {
      val config = ConfigFactory.parseString("""
        mappings = []
      """)
      
      val processor = new MappingProcessor(config)
      
      import spark.implicits._
      val inputDf = Seq(
        ("John")
      ).toDF("firstName")
      
      val result = processor.process(inputDf, List.empty, "test-ruleset")
      
      assert(result.schema === inputDf.schema)
      assert(result.count() === 1)
    }
    
    it("should preserve existing columns not involved in mappings") {
      val config = ConfigFactory.parseString("""
        mappings = [
          { from = "firstName", to = ["name"] }
        ]
      """)
      
      val processor = new MappingProcessor(config)
      
      import spark.implicits._
      val inputDf = Seq(
        ("John", 30, "john@example.com")
      ).toDF("firstName", "age", "email")
      
      val result = processor.process(inputDf, List.empty, "test-ruleset")
      
      assert(result.columns.contains("firstName"))
      assert(result.columns.contains("age"))
      assert(result.columns.contains("email"))
      assert(result.columns.contains("name"))
    }
    
    it("should handle the full example configuration") {
      val config = ConfigFactory.parseString("""
        id = "customer-name-mapper"
        mappings = [
          { from = "firstName", to = ["name.first", "fullName"] }
          { from = "lastName", to = ["name.last",