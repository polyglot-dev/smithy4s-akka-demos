package util
package person

enum EventsTags(val value: String):
    case PersonCreated       extends EventsTags("person-created")
    case PersonUpdated       extends EventsTags("person-updated")
    case PersonCreateUpdated extends EventsTags("person-created-or-updated")
