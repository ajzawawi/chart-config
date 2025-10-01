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
      assert(result.select("name.first").first().getString(0) === "John")
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
      assert(result.select("fieldB.fieldC.fieldD").first().getString(0) === "testValue")
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
      assert(result.select("name.first").first().getString(0) === "John")
      assert(result.select("name.last").first().getString(0) === "Doe")
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
      assert(result.select("name.first").first().getString(0) === "John")
      assert(result.select("name.last").first().getString(0) === "Doe")
      // fullName will be overwritten by lastName (last mapping wins)
      assert(result.select("fullName").first().getString(0) === "Doe")
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
      assert(result.select("name.first").first().get(0) === null)
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
          { from = "lastName", to = ["name.last", "fullName"] }
          { from = "fieldA", to = ["fieldB", "fieldB.fieldC", "fieldB.fieldC.fieldD"] }
          { from = "srcX", to = ["dst.one", "dst.two.three"] }
        ]
      """)
      
      val processor = new MappingProcessor(config)
      
      import spark.implicits._
      val inputDf = Seq(
        ("John", "Doe", "valueA", 42)
      ).toDF("firstName", "lastName", "fieldA", "srcX")
      
      val result = processor.process(inputDf, List.empty, "test-ruleset")
      
      // Verify name mappings
      assert(result.select("name.first").first().getString(0) === "John")
      assert(result.select("name.last").first().getString(0) === "Doe")
      assert(result.select("fullName").first().getString(0) === "Doe") // last mapping wins
      
      // Verify fieldA mappings
      assert(result.select("fieldB").first().getString(0) === "valueA")
      assert(result.select("fieldB.fieldC").first().getString(0) === "valueA")
      assert(result.select("fieldB.fieldC.fieldD").first().getString(0) === "valueA")
      
      // Verify srcX mappings
      assert(result.select("dst.one").first().getInt(0) === 42)
      assert(result.select("dst.two.three").first().getInt(0) === 42)
    }
    
    it("should handle different data types") {
      val config = ConfigFactory.parseString("""
        mappings = [
          { from = "stringField", to = ["nested.str"] }
          { from = "intField", to = ["nested.int"] }
          { from = "doubleField", to = ["nested.dbl"] }
          { from = "boolField", to = ["nested.bool"] }
        ]
      """)
      
      val processor = new MappingProcessor(config)
      
      import spark.implicits._
      val inputDf = Seq(
        ("test", 100, 3.14, true)
      ).toDF("stringField", "intField", "doubleField", "boolField")
      
      val result = processor.process(inputDf, List.empty, "test-ruleset")
      
      assert(result.select("nested.str").first().getString(0) === "test")
      assert(result.select("nested.int").first().getInt(0) === 100)
      assert(result.select("nested.dbl").first().getDouble(0) === 3.14)
      assert(result.select("nested.bool").first().getBoolean(0) === true)
    }
  }
}