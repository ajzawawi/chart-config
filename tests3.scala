it("should apply dateFormat transformation to convert date to string") {
  val config = ConfigFactory.parseString("""
    mappings = [
      { from = "createdAt", transform = "dateFormat:yyyy-MM-dd", to = ["createdDate"] }
    ]
  """)
  
  val processor = new MappingProcessor(config)
  
  import spark.implicits._
  
  val inputDf = Seq(
    ("2025-10-03 14:30:45"),
    ("2024-01-15 09:15:30")
  ).toDF("createdAt")
    .withColumn("createdAt", to_timestamp($"createdAt"))
  
  val result = processor.process(inputDf, List.empty, "test-ruleset")
  
  assert(result.select("createdDate").collect().map(_.getString(0)) === 
    Array("2025-10-03", "2024-01-15"))
}

it("should apply dateFormat transformation with custom format pattern") {
  val config = ConfigFactory.parseString("""
    mappings = [
      { from = "orderDate", transform = "dateFormat:MM/dd/yyyy", to = ["formatted.date"] }
    ]
  """)
  
  val processor = new MappingProcessor(config)
  
  import spark.implicits._
  
  val inputDf = Seq(
    ("2025-10-03"),
    ("2024-12-25")
  ).toDF("orderDate")
    .withColumn("orderDate", to_date($"orderDate"))
  
  val result = processor.process(inputDf, List.empty, "test-ruleset")
  
  val row = result.first()
  val formattedStruct = row.getAs[Row]("formatted")
  
  assert(formattedStruct.getAs[String]("date") === "10/03/2025")
}

it("should apply dateFormat transformation with time included") {
  val config = ConfigFactory.parseString("""
    mappings = [
      { from = "timestamp", transform = "dateFormat:yyyy-MM-dd HH:mm:ss", to = ["fullTimestamp"] }
    ]
  """)
  
  val processor = new MappingProcessor(config)
  
  import spark.implicits._
  
  val inputDf = Seq(
    ("2025-10-03 14:30:45")
  ).toDF("timestamp")
    .withColumn("timestamp", to_timestamp($"timestamp"))
  
  val result = processor.process(inputDf, List.empty, "test-ruleset")
  
  assert(result.select("fullTimestamp").first().getString(0) === "2025-10-03 14:30:45")
}

it("should apply dateFormat transformation to extract only time") {
  val config = ConfigFactory.parseString("""
    mappings = [
      { from = "eventTime", transform = "dateFormat:HH:mm:ss", to = ["timeOnly"] }
    ]
  """)
  
  val processor = new MappingProcessor(config)
  
  import spark.implicits._
  
  val inputDf = Seq(
    ("2025-10-03 14:30:45"),
    ("2025-10-03 09:15:20")
  ).toDF("eventTime")
    .withColumn("eventTime", to_timestamp($"eventTime"))
  
  val result = processor.process(inputDf, List.empty, "test-ruleset")
  
  assert(result.select("timeOnly").collect().map(_.getString(0)) === 
    Array("14:30:45", "09:15:20"))
}

it("should apply dateFormat transformation with month name") {
  val config = ConfigFactory.parseString("""
    mappings = [
      { from = "birthDate", transform = "dateFormat:MMMM dd, yyyy", to = ["formattedBirthDate"] }
    ]
  """)
  
  val processor = new MappingProcessor(config)
  
  import spark.implicits._
  
  val inputDf = Seq(
    ("1990-03-15"),
    ("1985-12-25")
  ).toDF("birthDate")
    .withColumn("birthDate", to_date($"birthDate"))
  
  val result = processor.process(inputDf, List.empty, "test-ruleset")
  
  assert(result.select("formattedBirthDate").collect().map(_.getString(0)) === 
    Array("March 15, 1990", "December 25, 1985"))
}