
val nms = Seq("dos")


val (a::_, b)  = nms.toList.splitAt(1) : @unchecked
 