package domain
package data

case class Campaign(id: Long, name: String)

case class Advertiser(id: Long, name: String, campaigns: List[Campaign] = List.empty)
