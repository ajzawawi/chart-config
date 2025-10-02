MappingProcessor - Acceptance Criteria
Scenario 1: Flat to Flat Field Mapping
Given an input DataFrame with a column firstName containing "John"
When a mapping is configured from firstName to fullName
Then the output DataFrame should contain both firstName and fullName columns
And fullName should contain the value "John"

Scenario 2: Flat to Nested Single-Level Mapping
Given an input DataFrame with columns firstName = "John" and lastName = "Doe"
When mappings are configured:

firstName → name.first
lastName → name.last

Then the output DataFrame should contain a name struct column
And name.first should equal "John"
And name.last should equal "Doe"

Scenario 3: Deep Nested Structure Creation
Given an input DataFrame with a column fieldA containing "testValue"
When a mapping is configured from fieldA to fieldB.fieldC.fieldD
Then the output DataFrame should contain a fieldB struct column
And fieldB.fieldC.fieldD should equal "testValue"
And the nested structure should be correctly typed as nested structs

Scenario 4: One-to-Many Mapping (Fan-Out)
Given an input DataFrame with a column firstName containing "John"
When a mapping is configured from firstName to multiple targets: ["name.first", "fullName", "display.name"]
Then all target fields should be created
And all target fields should contain "John"
And both flat (fullName) and nested (name.first, display.name) targets should be populated correctly

Scenario 5: Multiple Mappings to Same Flat Target
Given an input DataFrame with firstName = "John" and lastName = "Doe"
When both fields are mapped to the same flat target fullName
Then the fullName field should contain "Doe" (last mapping wins)
And a warning should be logged about the conflicting mappings

Scenario 6: Missing Source Field Handling
Given an input DataFrame without a column named missingField
When a mapping is configured from missingField to target
Then the mapping should be skipped without error
And a warning should be logged listing the missing source column
And other valid mappings should still be applied successfully

Scenario 7: Mixed Flat and Nested Mappings to Same Top-Level
Given an input DataFrame with firstName = "John" and lastName = "Doe"
When mappings are configured:

firstName → user (flat)
lastName → user.name (nested)

Then a warning should be logged about mixed flat/nested mappings on user
And the nested mapping should take precedence (nested wins)
And user should be a struct containing name = "Doe"

Scenario 8: Configuration Parsing from HOCON
Given a valid HOCON configuration with:
hocon{
  type = "MappingProcessor"
  id = "test-mapper"
  mappings = [
    { from = "fieldA", to = ["fieldB"] }
  ]
}
When the MappingProcessor is instantiated from this config
Then the processor should be created successfully
And the processor name should be "test-mapper"
And the mappings should be correctly parsed

Scenario 9: Configuration Parsing Without Optional ID
Given a valid HOCON configuration without an id field
When the MappingProcessor is instantiated
Then the processor name should default to "MappingProcessor"
And all mappings should still be parsed and applied correctly

Scenario 10: Preserving Existing Struct Fields
Given an input DataFrame with an existing struct column user containing {age: 30}
When a mapping is configured from firstName to user.name
Then the output user struct should contain both age = 30 and name from firstName
And the original age field should remain unchanged

Scenario 11: Empty Mappings Configuration
Given a configuration with an empty mappings array
When the processor is applied to a DataFrame
Then the DataFrame should be returned unchanged
And no errors should be thrown

Scenario 12: Invalid Configuration Handling
Given a configuration missing the required mappings field
When attempting to instantiate the MappingProcessor
Then a ConfigException should be thrown
And the error message should clearly indicate the missing field

Scenario 13: Integration with Processor Trait
Given the MappingProcessor implements the Processor trait
When the process method is called with DataFrame, rules, and rulesetId
Then all configured mappings should be applied to the DataFrame
And the method should return a new DataFrame (not modify the original)
And the processor should work within the existing processing pipeline