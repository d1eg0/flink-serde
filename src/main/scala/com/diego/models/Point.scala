package com.diego.models

final case class Point(id: Int, x: Double, y: Double)
final case class PointOpt(id: Int, x: Option[Double], y: Option[Double])

class PointPojo(var id: Int, var x: Double, var y: Double) {
  def this() =
    this(-1, -1, -1)
}

class PointPojoOpt(var id: Int, var x: Option[Double], var y: Option[Double]) {
  def this() =
    this(-1, None, None)
}
