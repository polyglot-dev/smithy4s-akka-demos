package domain
package data

case class Address(name: Option[String], n: Option[Int])

case class Person(name: Option[String] = None, town: Option[String] = None, address: Option[Address] = None)

case class PersonInfo(name: Option[String] = None, town: Option[String] = None)
