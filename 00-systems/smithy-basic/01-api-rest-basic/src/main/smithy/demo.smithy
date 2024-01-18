$version: "2.0"

metadata smithy4sErrorsAsScala3Unions = true
metadata smithy4sWildcardArgument = "?"

metadata selector = [
    {
        selector: "[:is(list > member)]"
        matches: [
            smithy4s.hello#Person2
        ]
    }
]

namespace smithy4s.hello

use alloy#simpleRestJson
use smithy4s.meta#generateServiceProduct
use smithytranslate#contentType

use smithy4s.meta#unwrap

@simpleRestJson
@generateServiceProduct
service HelloWorldService {
  version: "1.0.0",
  operations: [
    Hello
  ]
}

// @unwrap
string Email

structure Test {
  email: Email
  other: String
}

@unwrap
list CreateTargetCriteriaRequestSpendingRules {
    member: Person2
}

@http(method: "POST", uri: "/person/{name}", code: 200)
operation Hello{
  input: Person,
  output: Greeting
  errors: [
    BadRequestError
    NotFoundError
    ServiceUnavailableError
  ]
}

// @unwrap
structure Person2 {
  name: String,
  town: String
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
