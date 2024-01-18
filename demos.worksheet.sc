import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.OffsetDateTime

val nms = Seq("dos")


val (a::_, b)  = nms.toList.splitAt(1) : @unchecked
 
val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:MM:ssZ")
// String formattedString = zonedDateTime.format(formatter);
val zonedDateTimeOf = ZonedDateTime.of(2018, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
zonedDateTimeOf.format(formatter)

// val instant = Instant.parse("2010-10-02T12:23:23Z")
val i: Instant = DateTimeFormatter.ISO_INSTANT.parse("2010-10-02T12:23:23Z", Instant.from)
DateTimeFormatter.ISO_INSTANT.parse("2024-01-02T22:26:47.572Z", Instant.from)

val zonedDateTime = ZonedDateTime.ofInstant( i, ZoneId.of( "UTC" ) )
val s = zonedDateTime.format(DateTimeFormatter.ISO_INSTANT)
DateTimeFormatter.ISO_INSTANT.format(i)
val str = " "
str.isEmpty
str.isBlank

val x = "yo"
val y = "yo"
if (y == x) then true else false

val x1: Instant = Instant.now()
val y1: OffsetDateTime = x1.atOffset(ZoneOffset.UTC)
val str1 = "2010-10-02T12:23:23Z"
val str2 = "2024-01-02T22:26:47.572Z"
val tt: OffsetDateTime = DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(str1, OffsetDateTime.from)
val tt2: OffsetDateTime = DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(str2, OffsetDateTime.from)
DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(tt)
DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(tt2)
tt.toInstant()

val rules = List(("rule1", true), ("rule2", false), ("rule3", true))

val rFalse = rules.filter(!_._2)
if (rFalse.isEmpty) then 
    Some(true)
else 
    rFalse.foreach{
        case (rule, _) => println(s"rule $rule is not satisfied")
    }
    None
