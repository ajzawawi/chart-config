import org.scalatest.funspec.AnyFunSpec
import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._

class FilterCriteriaAndRunModeSpec extends AnyFunSpec {

  describe("RunMode.fromString") {
    it("returns correct enum values") {
      assert(RunMode.fromString("unfiltered") == RunMode.Unfiltered)
      assert(RunMode.fromString("filtered") == RunMode.Filtered)
    }

    it("throws on invalid input") {
      intercept[IllegalArgumentException] {
        RunMode.fromString("unknown")
      }
    }

    it("decodes from config using Ficus") {
      val cfg = ConfigFactory.parseString("""mode = "filtered" """)
      val mode = cfg.as[RunMode]("mode")
      assert(mode == RunMode.Filtered)
    }
  }

  describe("FilterCriteria") {
    it("decodes Before from config") {
      val cfg = ConfigFactory.parseString(
        """
          |criteria {
          |  type = "before"
          |  field = "timestamp"
          |  cutoffValue = 1728000000
          |}
          |""".stripMargin
      )
      val c = cfg.as[FilterCriteria]("criteria")
      assert(c.isInstanceOf[FilterCriteria.Before])
      val before = c.asInstanceOf[FilterCriteria.Before]
      assert(before.field == "timestamp")
      assert(before.cutoffValue == 1728000000L)
    }

    it("decodes After from config") {
      val cfg = ConfigFactory.parseString(
        """
          |criteria {
          |  type = "after"
          |  field = "createdAt"
          |  cutoffValue = 1700000000
          |}
          |""".stripMargin
      )
      val c = cfg.as[FilterCriteria]("criteria")
      assert(c.isInstanceOf[FilterCriteria.After])
      val after = c.asInstanceOf[FilterCriteria.After]
      assert(after.field == "createdAt")
      assert(after.cutoffValue == 1700000000L)
    }

    it("decodes Between from config") {
      val cfg = ConfigFactory.parseString(
        """
          |criteria {
          |  type = "between"
          |  field = "modifiedAt"
          |  startValue = 1600000000
          |  endValue = 1650000000
          |}
          |""".stripMargin
      )
      val c = cfg.as[FilterCriteria]("criteria")
      assert(c.isInstanceOf[FilterCriteria.Between])
      val between = c.asInstanceOf[FilterCriteria.Between]
      assert(between.field == "modifiedAt")
      assert(between.startValue == 1600000000L)
      assert(between.endValue == 1650000000L)
    }

    it("throws for unknown type") {
      val cfg = ConfigFactory.parseString(
        """
          |criteria {
          |  type = "nonsense"
          |  field = "x"
          |  cutoffValue = 1
          |}
          |""".stripMargin
      )
      intercept[IllegalArgumentException] {
        cfg.as[FilterCriteria]("criteria")
      }
    }
  }
}
