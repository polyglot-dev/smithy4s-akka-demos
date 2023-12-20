$version: "2.0"

metadata smithy4sErrorsAsScala3Unions = true
metadata smithy4sWildcardArgument = "?"

namespace smithy4s.hello

use alloy#simpleRestJson
use smithy4s.meta#generateServiceProduct

use smithytranslate#contentType

@simpleRestJson
@generateServiceProduct
service HelloWorldService {
  version: "1.0.0",
  operations: [
    Hello
  ]
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
