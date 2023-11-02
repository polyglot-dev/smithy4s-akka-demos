$version: "2.0"

namespace smithy4s.hello

use alloy#simpleRestJson

use smithytranslate#contentType

@simpleRestJson
service HelloWorldService {
  version: "1.0.0",
  operations: [
    Hello
  ]
}

@http(method: "POST", uri: "/person/{name}", code: 200)
operation Hello {
  input: Person,
  output: Greeting
  errors: [
    BadRequestError
    NotFoundError
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
