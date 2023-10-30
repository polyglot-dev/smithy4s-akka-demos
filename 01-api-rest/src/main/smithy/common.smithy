$version: "2"

namespace smithy4s.hello

structure Request {}

structure Response {}

@httpError(400)
@error("client")
structure BadRequestError {
  @required
  code: Integer
  @required
  message: String
}

@httpError(401)
@error("client")
structure UnauthorizedError {
  @required
  code: Integer
  @required
  message: String
}

@httpError(403)
@error("client")
structure ForbiddenError {
  @required
  code: Integer
  @required
  message: String
}

@httpError(406)
@error("client")
structure NotAcceptableError {
  @required
  code: Integer
  @required
  message: String
}

@httpError(408)
@error("client")
structure RequestTimeoutError {
  @required
  code: Integer
  @required
  message: String
}

@httpError(413)
@error("client")
structure ContentTooLargeError {
  @required
  code: Integer
  @required
  message: String
}

@httpError(416)
@error("client")
structure RangeNotSatisfiableError {
  @required
  code: Integer
  @required
  message: String
}

@httpError(422)
@error("client")
structure UnprocessableContentError {
  @required
  code: Integer
  @required
  message: String
}

@httpError(423)
@error("client")
structure LockedError {
  @required
  code: Integer
  @required
  message: String
}

@httpError(451)
@error("client")
structure UnavailableForLegalReasonsError {
  @required
  code: Integer
  @required
  message: String
}

@httpError(404)
@error("client")
structure NotFoundError {
  @required
  code: Integer
  @required
  message: String
}

@httpError(503)
@error("server")
structure ServiceUnavailableError {
  @required
  code: Integer
  @required
  message: String
}

@httpError(501)
@error("server")
structure NotImplementedError {
  @required
  code: Integer
  @required
  message: String
}

@httpError(507)
@error("server")
structure InsufficientStorageError {
  @required
  code: Integer
  @required
  message: String
}
