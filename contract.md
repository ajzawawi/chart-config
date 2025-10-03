processor {
  type = "MappingProcessor"
  id = "customer-data-processor"
  mappings = [
    # Direct mappings
    { from = "firstName", to = ["name.first"] }
    { from = "lastName", to = ["name.last"] }
    
    # Static columns
    { value = "ACTIVE", type = "string", to = ["status"] }
    { value = "2025", type = "int", to = ["year"] }
    { value = "true", type = "boolean", to = ["verified"] }
    
    # Transformations
    { from = "email", transform = "toLowerCase", to = ["contact.email"] }
    { from = "phone", transform = "removeSpaces", to = ["contact.phone"] }
    { from = "price", transform = "multiply:1.13", to = ["priceWithTax"] }
    { from = "price", transform = "round:2", to = ["priceRounded"] }
    { transform = "concat:firstName:lastName: ", to = ["fullName"] }
    { from = "description", transform = "substring:0:100", to = ["summary"] }
  ]
}