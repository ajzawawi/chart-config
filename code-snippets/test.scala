import org.scalatest.funspec.AnyFunSpec

class MappingUtilsSpec extends AnyFunSpec {

  describe("MappingUtils.flattenMappings") {

    val sampleMapping: Map[String, Any] = Map(
      "title" -> Map(
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

    val expectedFlattened = Map(
      "title" -> "text",
      "title.keyword" -> "keyword",
      "age" -> "integer",
      "productDetails.openQuantity" -> "double",
      "productDetails.sku" -> "keyword",
      "productDetails.manufacturer.name" -> "text",
      "productDetails.manufacturer.country" -> "keyword"
    )

    it("should flatten all top-level and nested fields with correct types") {
      val flattened = MappingUtils.flattenMappings(sampleMapping)

      assert(flattened == expectedFlattened)
    }
  }

  describe("MappingUtils.getFieldType") {
    val flattened = Map(
      "title" -> "text",
      "title.keyword" -> "keyword",
      "age" -> "integer",
      "productDetails.openQuantity" -> "double"
    )

    it("should return the correct field type for existing fields") {
      assert(MappingUtils.getFieldType("title", flattened).contains("text"))
      assert(MappingUtils.getFieldType("title.keyword", flattened).contains("keyword"))
      assert(MappingUtils.getFieldType("age", flattened).contains("integer"))
    }

    it("should return None for unknown fields") {
      assert(MappingUtils.getFieldType("unknown", flattened).isEmpty)
    }
  }
}


import org.scalatest.funsuite.AnyFunSuite

class ExtractValueTest extends AnyFunSuite {

  test("extractValue with comma-separated values and * replacement") {
    val result = extractValue("tags", Some("one,two,th*ree"))
    assert(result == Right(Seq("one", "two", "th,ree")))
  }

  test("extractValue with single non-empty value and * replacement") {
    val result = extractValue("name", Some("fir*st"))
    assert(result == Left("fir,st"))
  }

  test("extractValue trims spaces and filters empty entries") {
    val result = extractValue("list", Some(" one , , *two , "))
    assert(result == Right(Seq("one", ",two")))
  }

  test("extractValue with empty trimmed value throws exception") {
    val thrown = intercept[IllegalArgumentException] {
      extractValue("emptyField", Some("   "))
    }
    assert(thrown.getMessage == "Invalid value for field emptyField")
  }

  test("extractValue with None throws exception") {
    val thrown = intercept[IllegalArgumentException] {
      extractValue("missingField", None)
    }
    assert(thrown.getMessage == "Missing value for field: missingField")
  }
}


import org.scalatest.funsuite.AnyFunSuite

class ExtractNumericValueTest extends AnyFunSuite {

  it("extractNumericValue with valid integer string") {
    val result = extractNumericValue("amount", Some("42"))
    assert(result == 42.0)
  }

  it("extractNumericValue with valid decimal string") {
    val result = extractNumericValue("price", Some("  3.1415 "))
    assert(result == 3.1415)
  }

  it("extractNumericValue with negative number") {
    val result = extractNumericValue("offset", Some("-12.34"))
    assert(result == -12.34)
  }

  it("extractNumericValue with invalid number throws exception") {
    val ex = intercept[IllegalArgumentException] {
      extractNumericValue("badNumber", Some("abc"))
    }
    assert(ex.getMessage == "Expected a numeric value for field: badNumber")
  }

  it("extractNumericValue with empty string throws exception") {
    val ex = intercept[IllegalArgumentException] {
      extractNumericValue("blank", Some("  "))
    }
    assert(ex.getMessage == "Invalid value for field blank")
  }

  it("extractNumericValue with None throws exception") {
    val ex = intercept[IllegalArgumentException] {
      extractNumericValue("missing", None)
    }
    assert(ex.getMessage == "Missing value for field missing")
  }
}


import org.scalatest.funsuite.AnyFunSuite

class ExtractDateValueTest extends AnyFunSuite {

  it("extractDateValue with valid two dates") {
    val result = extractDateValue("range", Some("2024-01-01,2024-12-31"))
    assert(result == ("2024-01-01", "2024-12-31"))
  }

  it("extractDateValue trims whitespace around dates") {
    val result = extractDateValue("range", Some(" 2024-01-01 ,  2024-12-31 "))
    assert(result == ("2024-01-01", "2024-12-31"))
  }

  it("extractDateValue with more than two dates throws exception") {
    val ex = intercept[IllegalArgumentException] {
      extractDateValue("range", Some("2024-01-01,2024-12-31,2025-01-01"))
    }
    assert(ex.getMessage == "Expected two comma-separated date values for range")
  }

  it("extractDateValue with only one date throws exception") {
    val ex = intercept[IllegalArgumentException] {
      extractDateValue("range", Some("2024-01-01"))
    }
    assert(ex.getMessage == "Expected two comma-separated date values for range")
  }

  it("extractDateValue with empty string throws exception") {
    val ex = intercept[IllegalArgumentException] {
      extractDateValue("range", Some(""))
    }
    assert(ex.getMessage == "Expected two comma-separated date values for range")
  }

  it("extractDateValue with None throws exception") {
    val ex = intercept[IllegalArgumentException] {
      extractDateValue("range", None)
    }
    assert(ex.getMessage == "Missing value for field: range")
  }

  it("extractDateValue filters out extra empty values") {
    val ex = intercept[IllegalArgumentException] {
      extractDateValue("range", Some("2024-01-01,,"))
    }
    assert(ex.getMessage == "Expected two comma-separated date values for range")
  }
}


class IsNumericTest extends AnyFunSuite {

  val numericTypes = Seq("integer", "long", "double", "float", "short", "byte", "scaled_float", "half_float")

  test("isNumeric returns true for all numeric types") {
    numericTypes.foreach { t =>
      val result = isNumeric("amount", Map("amount" -> t))
      assert(result, s"Expected $t to be numeric")
    }
  }

  test("isNumeric returns false for string type") {
    val result = isNumeric("amount", Map("amount" -> "keyword"))
    assert(!result)
  }

  test("isNumeric returns false for missing field") {
    val result = isNumeric("amount", Map.empty)
    assert(!result)
  }

  test("isNumeric returns false for unrelated field") {
    val result = isNumeric("amount", Map("price" -> "integer"))
    assert(!result)
  }
}
