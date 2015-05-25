package com.snowplowanalytics.schemaguru.json

import scalaz._
import Scalaz._

import org.json4s._

/**
 * Offer some of JValue instances compare with
 * instances of same type
 */
object JValueOrdering extends Order[JValue] {
  def order(x: JValue, y: JValue): Ordering = (x, y) match {
    case (JInt(a), JInt(b))         => a ?|? b
    case (JString(a), JString(b))   => a ?|? b
    case (JDouble(a), JDouble(b))   => a ?|? b
    case (JDecimal(a), JDecimal(b)) => a ?|? b
    case (JArray(a), JArray(b))     => a.length ?|? b.length
    case _                          => Ordering.EQ
  }
}
