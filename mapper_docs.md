# MappingProcessor
The MappingProcessor supports three types of mappings:

1. Direct Field Mappings - Map source fields to target fields
2. Static Columns - Add constant values to all rows
3. Transformations - Apply functions to modify field values

## Basic Syntax
**Direct mapping**
{ from = "sourceField", to = ["targetField"] }

**Static column**
{ value = "constantValue", type = "string", to = ["targetField"] }

**Transformation**
{ from = "sourceField", transform = "functionName:args", to = ["targetField"] }


## Transformation Categories

+----------------+-------+--------------------------------------------------+
| Category       | Count | Use Cases                                        |
+----------------+-------+--------------------------------------------------+
| String         | 7     | Data cleaning, text standardization             |
| Numeric        | 6     | Calculations, price adjustments, rounding        |
| Date/Time      | 3     | Date parsing, formatting, conversion             |
| Conditional    | 2     | Null handling, fallback logic, defaults          |
+----------------+-------+--------------------------------------------------+
| TOTAL          | 18    | Complete data transformation pipeline            |
+----------------+-------+--------------------------------------------------+


### Transformation Functions

STRING FUNCTIONS:
  toLowerCase       - Convert to lowercase
  toUpperCase       - Convert to uppercase
  trim              - Remove leading/trailing whitespace
  removeSpaces      - Remove all whitespace
  substring:0:5     - Extract substring
  replace:old:new   - Replace text
  concat:f1:f2:sep  - Concatenate fields

NUMERIC FUNCTIONS:
  multiply:1.13     - Multiply by value
  divide:2          - Divide by value
  add:10            - Add value
  subtract:5        - Subtract value
  round:2           - Round to decimals
  abs               - Absolute value

DATE/TIME FUNCTIONS:
  dateFormat:pattern        - Date/Timestamp → String

CONDITIONAL FUNCTIONS:
  whenNull:default         - Replace null with default
  coalesce:f1:f2:default   - First non-null value


### Static Column Types

Supported Types:
  - string    - Text values
  - int       - Integer numbers
  - long      - Long integer numbers
  - double    - Decimal numbers
  - float     - Float numbers
  - boolean   - true/false values

Example:
  { value = "ACTIVE", type = "string", to = ["status"] }
  { value = "2025", type = "int", to = ["year"] }
  { value = "true", type = "boolean", to = ["verified"] }


### Date Format Patterns Reference

+----------------------+---------------------------+
| Pattern              | Example Output            |
+----------------------+---------------------------+
| yyyy-MM-dd           | 2025-10-03                |
| MM/dd/yyyy           | 10/03/2025                |
| dd-MMM-yyyy          | 03-Oct-2025               |
| yyyy-MM-dd HH:mm:ss  | 2025-10-03 14:30:45       |
| HH:mm:ss             | 14:30:45                  |
| MMMM dd, yyyy        | October 03, 2025          |
| EEEE, MMMM dd, yyyy  | Friday, October 03, 2025  |
| dd/MM/yyyy           | 03/10/2025                |
+----------------------+---------------------------+

Pattern Elements:
  yyyy - 4-digit year
  MM   - 2-digit month (01-12)
  dd   - 2-digit day (01-31)
  HH   - 2-digit hour (00-23)
  mm   - 2-digit minute (00-59)
  ss   - 2-digit second (00-59)
  MMM  - Abbreviated month (Jan, Feb, etc.)
  MMMM - Full month name (January, February, etc.)
  EEEE - Full day name (Monday, Tuesday, etc.)





## String Transformations

### toLowerCase
Converts all characters in a string to lowercase.

Syntax: transform: "toLowerCase"

Example Configuration:
{ from = "email", transform = "toLowerCase", to = ["contact.email"] }

Example Data:
+----------------------+----------------------+
| Input Column         | Output Column        |
+----------------------+----------------------+
| "JOHN@EXAMPLE.COM"   | "john@example.com"   |
| "Jane@Test.COM"      | "jane@test.com"      |
| "HELLO.WORLD"        | "hello.world"        |
+----------------------+----------------------+

Examples Use Cases:
- Email address standardization
- Text normalization for comparisons


### toUpperCase

Converts all characters in a string to uppercase.

Syntax: transform: "toUpperCase"

Example Configuration:
{ from = "code", transform = "toUpperCase", to = ["productCode"] }

Example Data:
+----------------------+----------------------+
| Input Column         | Output Column        |
+----------------------+----------------------+
| "abc123"             | "ABC123"             |
| "Hello World"        | "HELLO WORLD"        |
| "test-value"         | "TEST-VALUE"         |
+----------------------+----------------------+

Examples Use Cases:
- Product code standardization
- Country code normalization


### trim

Removes leading and trailing whitespace from a string.

Syntax: transform: "trim"

Example Configuration:
{ from = "name", transform = "trim", to = ["cleanName"] }

Example Data:
+----------------------+----------------------+
| Input Column         | Output Column        |
+----------------------+----------------------+
| "  John  "           | "John"               |
| "  Hello World  "    | "Hello World"        |
| "\tTest\n"           | "Test"               |
+----------------------+----------------------+

Examples Use Cases:
- Cleaning user input
- Removing accidental spaces
- Data quality improvement


### removeSpaces

Removes all whitespace characters (spaces, tabs, newlines) from a string.

Syntax: transform: "removeSpaces"

Example Configuration:
{ from = "phone", transform = "removeSpaces", to = ["phoneClean"] }

Example Data:
+----------------------+----------------------+
| Input Column         | Output Column        |
+----------------------+----------------------+
| "555 123 4567"       | "5551234567"         |
| "(123) 456-7890"     | "(123)456-7890"      |
| "555 - 1234"         | "555-1234"           |
+----------------------+----------------------+

Examples Use Cases:
- Phone number normalization
- Removing formatting from identifiers


### substring

Extracts a portion of a string starting at a specified position for a given 
length.

Syntax: transform: "substring:start:length"
  - start: Starting position (0-based index)
  - length: Number of characters to extract

Example Configuration:
{ from = "text", transform = "substring:0:5", to = ["prefix"] }

Example Data:
+----------------------+----------------------+
| Input Column         | Output Column        |
+----------------------+----------------------+
| "HelloWorld"         | "Hello"              |
| "TestString123"      | "TestS"              |
| "abcdefgh"           | "abcde"              |
+----------------------+----------------------+

Examples Use Cases:
- Extracting prefixes/suffixes
- Creating abbreviations
- Truncating long text


### replace

Replaces all occurrences of a substring with another substring.

Syntax: transform: "replace:oldValue:newValue"

Example Configuration:
{ from = "filename", transform = "replace:-:_", to = ["cleanFilename"] }

Example Data:
+----------------------+----------------------+
| Input Column         | Output Column        |
+----------------------+----------------------+
| "test-value-here"    | "test_value_here"    |
| "file-name-2024"     | "file_name_2024"     |
| "my-document.txt"    | "my_document.txt"    |
+----------------------+----------------------+

Examples Use Cases:
- Filename sanitization
- Character substitution
- Data format conversion


### concat

Concatenates multiple fields with a specified separator.
Note: This transformation does not use a 'from' field.

Syntax: transform: "concat:field1:field2:...:separator"

Example Configuration:
{ transform = "concat:firstName:lastName: ", to = ["fullName"] }

Example Data:
+------------+------------+----------------------+
| firstName  | lastName   | fullName (Output)    |
+------------+------------+----------------------+
| "John"     | "Doe"      | "John Doe"           |
| "Jane"     | "Smith"    | "Jane Smith"         |
| "Bob"      | "Johnson"  | "Bob Johnson"        |
+------------+------------+----------------------+

Examples Use Cases:
- Creating full names from parts
- Building composite keys
- Generating display labels


## Numeric Transformations

### multiply

Multiplies a numeric column by a specified value.

Syntax: transform: "multiply:value"

Example Configuration:
{ from = "price", transform = "multiply:1.13", to = ["priceWithTax"] }

Example Data:
+----------------------+----------------------+
| Input Column         | Output Column        |
+----------------------+----------------------+
| 100.0                | 113.0                |
| 50.0                 | 56.5                 |
| 25.50                | 28.815               |
+----------------------+----------------------+

Examples Use Cases:
- Adding tax to prices
- Currency conversion


### divide

Divides a numeric column by a specified value.

Syntax: transform: "divide:value"

Example Configuration:
{ from = "total", transform = "divide:2", to = ["half"] }

Example Data:
+----------------------+----------------------+
| Input Column         | Output Column        |
+----------------------+----------------------+
| 100.0                | 50.0                 |
| 75.0                 | 37.5                 |
| 33.0                 | 16.5                 |
+----------------------+----------------------+

Examples Use Cases:
- Calculating averages
- Splitting amounts
- Unit conversions


### add

Adds a specified value to a numeric column.

Syntax: transform: "add:value"

Example Configuration:
{ from = "price", transform = "add:10", to = ["priceWithFee"] }

Example Data:
+----------------------+----------------------+
| Input Column         | Output Column        |
+----------------------+----------------------+
| 100.0                | 110.0                |
| 50.0                 | 60.0                 |
| 25.50                | 35.50                |
+----------------------+----------------------+

Examples Use Cases:
- Applying fixed charges
- Offset adjustments


### subtract

Subtracts a specified value from a numeric column.

Syntax: transform: "subtract:value"

Example Configuration:
{ from = "price", transform = "subtract:5", to = ["discountedPrice"] }

Example Data:
+----------------------+----------------------+
| Input Column         | Output Column        |
+----------------------+----------------------+
| 100.0                | 95.0                 |
| 50.0                 | 45.0                 |
| 25.50                | 20.50                |
+----------------------+----------------------+

Examples Use Cases:
- Quantity or stock calculations
- Adjusting baselines


### round

Rounds a numeric value to a specified number of decimal places.

Syntax: transform: "round:decimals"

Example Configuration:
{ from = "value", transform = "round:2", to = ["rounded"] }

Example Data:
+----------------------+----------------------+
| Input Column         | Output Column        |
+----------------------+----------------------+
| 3.14159              | 3.14                 |
| 2.71828              | 2.72                 |
| 10.999               | 11.0                 |
+----------------------+----------------------+

Examples Use Cases:
- Formatting currency
- Display precision control
- Removing floating-point errors


### abs

Returns the absolute value of a number (removes negative sign).

Syntax: transform: "abs"

Example Configuration:
{ from = "temperature", transform = "abs", to = ["absoluteTemp"] }

Example Data:
+----------------------+----------------------+
| Input Column         | Output Column        |
+----------------------+----------------------+
| -42                  | 42                   |
| 10                   | 10                   |
| -3.5                 | 3.5                  |
+----------------------+----------------------+

Examples Use Cases:
- Direction indication


### dateFormat

Formats a date or timestamp as a string using a specified pattern.

Syntax: transform: "dateFormat:pattern"

Example Configuration:
{ from = "createdAt", transform = "dateFormat:yyyy-MM-dd", 
  to = ["displayDate"] }

Example Data:
+----------------------------------+---------------------------+
| Input Column (Timestamp)         | Output Column (String)    |
+----------------------------------+---------------------------+
| Timestamp(2025-10-03 14:30:45)   | "2025-10-03"              |
| Timestamp(2024-01-15 09:15:30)   | "2024-01-15"              |
| Timestamp(2023-12-25 18:00:00)   | "2023-12-25"              |
+----------------------------------+---------------------------+

Common Patterns and Examples:
+----------------------+-------------------------+
| Pattern              | Example Output          |
+----------------------+-------------------------+
| yyyy-MM-dd           | 2025-10-03              |
| MM/dd/yyyy           | 10/03/2025              |
| dd-MMM-yyyy          | 03-Oct-2025             |
| HH:mm:ss             | 14:30:45                |
| MMMM dd, yyyy        | October 03, 2025        |
| yyyy-MM-dd HH:mm:ss  | 2025-10-03 14:30:45     |
| EEEE, MMMM dd, yyyy  | Friday, October 03, 2025|
+----------------------+-------------------------+

Examples Use Cases:
- Display formatting
- Report generation
- API response formatting
- Creating user-friendly dates


## Conditional Transformations

### whenNull

Returns a default value when the source column is null, otherwise returns the 
original value.

Syntax: transform: "whenNull:defaultValue"

Example Configuration:
{ from = "optionalField", transform = "whenNull:N/A", to = ["fieldOrDefault"] }

Example Data:
+----------------------+----------------------+
| Input Column         | Output Column        |
+----------------------+----------------------+
| "actual value"       | "actual value"       |
| null                 | "N/A"                |
| "test"               | "test"               |
+----------------------+----------------------+

Examples Use Cases:
- Providing default values for missing data
- Ensuring non-null displays
- Data completeness


### coalesce

Returns the first non-null value from a list of columns. Checks multiple 
fields in order and returns a default if all are null.

Syntax: transform: "coalesce:field1:field2:...:defaultValue"

Example Configuration:
{ from = "primaryEmail", transform = "coalesce:secondaryEmail:NOEMAIL", 
  to = ["email"] }

Example Data:
+------------------------+------------------------+------------------------+
| primaryEmail           | secondaryEmail         | email (Output)         |
+------------------------+------------------------+------------------------+
| "primary@example.com"  | "secondary@example.com"| "primary@example.com"  |
| null                   | "secondary@example.com"| "secondary@example.com"|
| null                   | null                   | "NOEMAIL"              |
+------------------------+------------------------+------------------------+

Multi-Field Example:
{ from = "mobile", transform = "coalesce:home:work:NO_PHONE", to = ["phone"] }

+-------------+-------------+-------------+------------------+
| mobile      | home        | work        | phone (Output)   |
+-------------+-------------+-------------+------------------+
| "555-1111"  | "555-2222"  | "555-3333"  | "555-1111"       |
| null        | "555-2222"  | "555-3333"  | "555-2222"       |
| null        | null        | "555-3333"  | "555-3333"       |
| null        | null        | null        | "NO_PHONE"       |
+-------------+-------------+-------------+------------------+

Examples Use Cases:
- Email fallback chains
- Contact information consolidation
- Prioritized field selection

Difference from whenNull:
- whenNull: Checks only the source field (2 options)
- coalesce: Checks multiple fields in order (N options)
