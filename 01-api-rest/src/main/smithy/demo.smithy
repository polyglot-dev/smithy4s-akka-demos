$version: "2.0"

namespace smithy4s.hello

use alloy#simpleRestJson

use smithytranslate#contentType

@simpleRestJson
service HelloWorldService {
  version: "1.0.0",
  operations: [
    Hello
    UpdateCategory
  ]
}

enum Category {
    uncategorized
    restaurant
    sport_equipment
    hotels
    airlines
    entertainment
    cinema
    coffee
    food_store_grocery
    gas
    discount_stores
    clothing
    speciality_retail_store
}

@http(
    method: "POST"
    uri: "/category/create"
    code: 200
)
operation UpdateCategory {
    input: UpdateCategoryInput
    output: Response
    errors: [
        BadRequestError
        NotFoundError
        ServiceUnavailableError
    ]
}

structure UpdateCategoryInput {
    @httpPayload
    @contentType("application/json")
    body: Category
}

@http(method: "POST", uri: "/person/{name}", code: 200)
operation Hello {
  input: Person,
  output: Greeting
}

structure Person {
  @httpLabel
  @required
  name: String,

  @httpQuery("town")
  town: String
}

structure Greeting {
  @required
  message: String
}
