
val nms = Seq("dos")


val (a::_, b)  = nms.toList.splitAt(1) : @unchecked
 
// val town = Some("town")
// // val town = None
// // val addr = Some("addr")
// val addr = None
// val optionalFields = List[(String, Option[Any])](("town", town), ("address", addr))
// val fields = optionalFields.filter(_._2.isDefined)
// val fieldsNames = fields.map(_._1).mkString(start = if fields.isEmpty then "" else ", ", sep= ", ", end = "")
// val fieldsIndexForBindings = fields.indices.map(_ + 3).map(
//   "$" + _
// ).mkString(start = if fields.isEmpty then "" else ", ", sep = ", ", end = "")



val name = Some("pepe")
val town = Some("town")
// val town = None
val addr = Some("addr")
// val addr = None
val optionalFields = List[(String, Option[Any])](("name", name),  ("town", town), ("address", addr))
val fields = optionalFields.filter(_._2.isDefined)
val expr = fields
         .map(_._1)
         .zipWithIndex
         .map:
              case (name, index) => s"$name = $$${index + 1}"
        .mkString(", ")


//                 UPDATE person_projection
//                 SET name = $1, town = $2, address = $3
//                 WHERE id = $4
                          
"UPDATE person_projection SET " + 
expr + " WHERE id = $" + 
(fields.size + 1)
