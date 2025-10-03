describe("MappingProcessor - Static Columns") {
  
  it("should add static string columns") {
    val config = ConfigFactory.parseString("""
      mappings = [
        { from = "firstName", to = ["name"] }
        { value = "ACTIVE", type = "string", to = ["status"] }
      ]
    """)
    
    val processor = new MappingProcessor(config)
    
    import spark.implicits._
    val inputDf = Seq(
      ("John"),
      ("Jane")
    ).toDF("firstName")
    
    val result = processor.process(inputDf, List.empty, "test-ruleset")
    
    assert(result.columns.contains("status"))
    assert(result.select("status").collect().map(_.getString(0)) === Array("ACTIVE", "ACTIVE"))
  }
  
  it("should add static columns with different data types") {
    val config = ConfigFactory.parseString("""
      mappings = [
        { value = "ACTIVE", type = "string", to = ["status"] }
        { value = "2025", type = "int", to = ["year"] }
        { value = "99.99", type = "double", to = ["price"] }
        { value = "true", type = "boolean", to = ["verified"] }
        { value = "1234567890", type = "long", to = ["userId"] }
      ]
    """)
    
    val processor = new MappingProcessor(config)
    
    import spark.implicits._
    val inputDf = Seq(
      ("John"),
      ("Jane")
    ).toDF("firstName")
    
    val result = processor.process(inputDf, List.empty, "test-ruleset")
    
    val row = result.first()
    
    assert(row.getAs[String]("status") === "ACTIVE")
    assert(row.getAs[Int]("year") === 2025)
    assert(row.getAs[Double]("price") === 99.99)
    assert(row.getAs[Boolean]("verified") === true)
    assert(row.getAs[Long]("userId") === 1234567890L)
  }
  
  it("should add static columns to nested paths") {
    val config = ConfigFactory.parseString("""
      mappings = [
        { value = "ACTIVE", type = "string", to = ["metadata.status"] }
        { value = "2025", type = "int", to = ["metadata.year"] }
      ]
    """)
    
    val processor = new MappingProcessor(config)
    
    import spark.implicits._
    val inputDf = Seq(
      ("John")
    ).toDF("firstName")
    
    val result = processor.process(inputDf, List.empty, "test-ruleset")
    
    assert(result.columns.contains("metadata"))
    
    val row = result.first()
    val metadataStruct = row.getAs[Row]("metadata")
    
    assert(metadataStruct.getAs[String]("status") === "ACTIVE")
    assert(metadataStruct.getAs[Int]("year") === 2025)
  }
  
  it("should default to string type when type is not specified") {
    val config = ConfigFactory.parseString("""
      mappings = [
        { value = "DEFAULT", to = ["status"] }
      ]
    """)
    
    val processor = new MappingProcessor(config)
    
    import spark.implicits._
    val inputDf = Seq(("John")).toDF("firstName")
    
    val result = processor.process(inputDf, List.empty, "test-ruleset")
    
    assert(result.select("status").first().getString(0) === "DEFAULT")
  }
}

describe("MappingProcessor - Transformations") {
  
  it("should apply toLowerCase transformation") {
    val config = ConfigFactory.parseString("""
      mappings = [
        { from = "email", transform = "toLowerCase", to = ["emailLower"] }
      ]
    """)
    
    val processor = new MappingProcessor(config)
    
    import spark.implicits._
    val inputDf = Seq(
      ("JOHN@EXAMPLE.COM"),
      ("JANE@TEST.COM")
    ).toDF("email")
    
    val result = processor.process(inputDf, List.empty, "test-ruleset")
    
    assert(result.select("emailLower").collect().map(_.getString(0)) === 
      Array("john@example.com", "jane@test.com"))
  }
  
  it("should apply toUpperCase transformation") {
    val config = ConfigFactory.parseString("""
      mappings = [
        { from = "name", transform = "toUpperCase", to = ["nameUpper"] }
      ]
    """)
    
    val processor = new MappingProcessor(config)
    
    import spark.implicits._
    val inputDf = Seq(("john"), ("jane")).toDF("name")
    
    val result = processor.process(inputDf, List.empty, "test-ruleset")
    
    assert(result.select("nameUpper").collect().map(_.getString(0)) === 
      Array("JOHN", "JANE"))
  }
  
  it("should apply trim transformation") {
    val config = ConfigFactory.parseString("""
      mappings = [
        { from = "name", transform = "trim", to = ["nameTrimmed"] }
      ]
    """)
    
    val processor = new MappingProcessor(config)
    
    import spark.implicits._
    val inputDf = Seq(("  john  "), ("  jane  ")).toDF("name")
    
    val result = processor.process(inputDf, List.empty, "test-ruleset")
    
    assert(result.select("nameTrimmed").collect().map(_.getString(0)) === 
      Array("john", "jane"))
  }
  
  it("should apply removeSpaces transformation") {
    val config = ConfigFactory.parseString("""
      mappings = [
        { from = "phone", transform = "removeSpaces", to = ["phoneClean"] }
      ]
    """)
    
    val processor = new MappingProcessor(config)
    
    import spark.implicits._
    val inputDf = Seq(
      ("555 123 4567"),
      ("555 987 6543")
    ).toDF("phone")
    
    val result = processor.process(inputDf, List.empty, "test-ruleset")
    
    assert(result.select("phoneClean").collect().map(_.getString(0)) === 
      Array("5551234567", "5559876543"))
  }
  
  it("should apply multiply transformation") {
    val config = ConfigFactory.parseString("""
      mappings = [
        { from = "price", transform = "multiply:1.13", to = ["priceWithTax"] }
      ]
    """)
    
    val processor = new MappingProcessor(config)
    
    import spark.implicits._
    val inputDf = Seq(
      (100.0),
      (50.0)
    ).toDF("price")
    
    val result = processor.process(inputDf, List.empty, "test-ruleset")
    
    val prices = result.select("priceWithTax").collect().map(_.getDouble(0))
    assert(prices(0) === 113.0 +- 0.01)
    assert(prices(1) === 56.5 +- 0.01)
  }
  
  it("should apply divide transformation") {
    val config = ConfigFactory.parseString("""
      mappings = [
        { from = "total", transform = "divide:2", to = ["half"] }
      ]
    """)
    
    val processor = new MappingProcessor(config)
    
    import spark.implicits._
    val inputDf = Seq((100.0), (50.0)).toDF("total")
    
    val result = processor.process(inputDf, List.empty, "test-ruleset")
    
    assert(result.select("half").collect().map(_.getDouble(0)) === Array(50.0, 25.0))
  }
  
  it("should apply round transformation") {
    val config = ConfigFactory.parseString("""
      mappings = [
        { from = "value", transform = "round:2", to = ["rounded"] }
      ]
    """)
    
    val processor = new MappingProcessor(config)
    
    import spark.implicits._
    val inputDf = Seq((3.14159), (2.71828)).toDF("value")
    
    val result = processor.process(inputDf, List.empty, "test-ruleset")
    
    assert(result.select("rounded").collect().map(_.getDouble(0)) === Array(3.14, 2.72))
  }
  
  it("should apply abs transformation") {
    val config = ConfigFactory.parseString("""
      mappings = [
        { from = "value", transform = "abs", to = ["absolute"] }
      ]
    """)
    
    val processor = new MappingProcessor(config)
    
    import spark.implicits._
    val inputDf = Seq((-10), (5), (-3)).toDF("value")
    
    val result = processor.process(inputDf, List.empty, "test-ruleset")
    
    assert(result.select("absolute").collect().map(_.getInt(0)) === Array(10, 5, 3))
  }
  
  it("should apply concat transformation") {
    val config = ConfigFactory.parseString("""
      mappings = [
        { transform = "concat:firstName:lastName: ", to = ["fullName"] }
      ]
    """)
    
    val processor = new MappingProcessor(config)
    
    import spark.implicits._
    val inputDf = Seq(
      ("John", "Doe"),
      ("Jane", "Smith")
    ).toDF("firstName", "lastName")
    
    val result = processor.process(inputDf, List.empty, "test-ruleset")
    
    assert(result.select("fullName").collect().map(_.getString(0)) === 
      Array("John Doe", "Jane Smith"))
  }
  
  it("should apply substring transformation") {
    val config = ConfigFactory.parseString("""
      mappings = [
        { from = "text", transform = "substring:0:5", to = ["prefix"] }
      ]
    """)
    
    val processor = new MappingProcessor(config)
    
    import spark.implicits._
    val inputDf = Seq(
      ("HelloWorld"),
      ("TestString")
    ).toDF("text")
    
    val result = processor.process(inputDf, List.empty, "test-ruleset")
    
    assert(result.select("prefix").collect().map(_.getString(0)) === 
      Array("Hello", "TestS"))
  }
  
  it("should apply replace transformation") {
    val config = ConfigFactory.parseString("""
      mappings = [
        { from = "text", transform = "replace:-:_", to = ["cleaned"] }
      ]
    """)
    
    val processor = new MappingProcessor(config)
    
    import spark.implicits._
    val inputDf = Seq(
      ("test-value-here"),
      ("another-test")
    ).toDF("text")
    
    val result = processor.process(inputDf, List.empty, "test-ruleset")
    
    assert(result.select("cleaned").collect().map(_.getString(0)) === 
      Array("test_value_here", "another_test"))
  }
  
  it("should apply whenNull transformation") {
    val config = ConfigFactory.parseString("""
      mappings = [
        { from = "value", transform = "whenNull:DEFAULT", to = ["valueOrDefault"] }
      ]
    """)
    
    val processor = new MappingProcessor(config)
    
    val inputSchema = StructType(Seq(
      StructField("value", StringType, nullable = true)
    ))
    
    val inputData = Seq(
      Row("actual"),
      Row(null)
    )
    
    import scala.jdk.CollectionConverters._
    val inputDf = spark.createDataFrame(inputData.asJava, inputSchema)
    
    val result = processor.process(inputDf, List.empty, "test-ruleset")
    
    assert(result.select("valueOrDefault").collect().map(_.getString(0)) === 
      Array("actual", "DEFAULT"))
  }
  
  it("should apply transformations to nested paths") {
    val config = ConfigFactory.parseString("""
      mappings = [
        { from = "email", transform = "toLowerCase", to = ["contact.email"] }
        { from = "price", transform = "multiply:1.13", to = ["pricing.withTax"] }
      ]
    """)
    
    val processor = new MappingProcessor(config)
    
    import spark.implicits._
    val inputDf = Seq(
      ("JOHN@EXAMPLE.COM", 100.0)
    ).toDF("email", "price")
    
    val result = processor.process(inputDf, List.empty, "test-ruleset")
    
    val row = result.first()
    val contactStruct = row.getAs[Row]("contact")
    val pricingStruct = row.getAs[Row]("pricing")
    
    assert(contactStruct.getAs[String]("email") === "john@example.com")
    assert(pricingStruct.getAs[Double]("withTax") === 113.0 +- 0.01)
  }
}

describe("MappingProcessor - Mixed Mappings") {
  
  it("should handle direct mappings, static columns, and transformations together") {
    val config = ConfigFactory.parseString("""
      mappings = [
        { from = "firstName", to = ["name.first"] }
        { from = "lastName", to = ["name.last"] }
        { value = "ACTIVE", type = "string", to = ["status"] }
        { value = "2025", type = "int", to = ["year"] }
        { from = "email", transform = "toLowerCase", to = ["contact.email"] }
        { from = "price", transform = "multiply:1.13", to = ["priceWithTax"] }
      ]
    """)
    
    val processor = new MappingProcessor(config)
    
    import spark.implicits._
    val inputDf = Seq(
      ("John", "Doe", "JOHN@EXAMPLE.COM", 100.0)
    ).toDF("firstName", "lastName", "email", "price")
    
    val result = processor.process(inputDf, List.empty, "test-ruleset")
    
    val row = result.first()
    
    // Direct mappings
    val nameStruct = row.getAs[Row]("name")
    assert(nameStruct.getAs[String]("first") === "John")
    assert(nameStruct.getAs[String]("last") === "Doe")
    
    // Static columns
    assert(row.getAs[String]("status") === "ACTIVE")
    assert(row.getAs[Int]("year") === 2025)
    
    // Transformations
    val contactStruct = row.getAs[Row]("contact")
    assert(contactStruct.getAs[String]("email") === "john@example.com")
    assert(row.getAs[Double]("priceWithTax") === 113.0 +- 0.01)
  }
  
  it("should skip invalid transformations and continue with valid mappings") {
    val config = ConfigFactory.parseString("""
      mappings = [
        { from = "nonExistent", transform = "toLowerCase", to = ["result1"] }
        { from = "firstName", to = ["name"] }
        { value = "ACTIVE", type = "string", to = ["status"] }
      ]
    """)
    
    val processor = new MappingProcessor(config)
    
    import spark.implicits._
    val inputDf = Seq(("John")).toDF("firstName")
    
    val result = processor.process(inputDf, List.empty, "test-ruleset")
    
    // Invalid transformation should be skipped
    assert(!result.columns.contains("result1"))
    
    // Valid mappings should work
    assert(result.select("name").first().getString(0) === "John")
    assert(result.select("status").first().getString(0) === "ACTIVE")
  }
}