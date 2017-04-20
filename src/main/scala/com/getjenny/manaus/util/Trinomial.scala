package com.getjenny.manaus.util

/** Trinomial distribution
  *
  * Created by Mario Alemi on 16/04/2017 in Manaus, Amazonas, Brazil.
  */
case class Trinomial(samples: Int, successes: List[Double]) {
  //////assert(successes.length == 2)
  val p: List[Double] = successes.map(_ * 1.0 / samples)

  def pSuccess(n: Int,  k: List[Int]): Double = multinomialFactor(n, k) * p.zip(k).map(x => math.pow(x._1, x._2)).product

  /**
    * Here we take the last "k" as the one we want to measure the significance. In the case of
    * bigrams, k(0) will be none of the bigrams' word appear, k(1) at least one, k(2) the bigram appear.
    * @param n number of times we test
    * @param k List of success for each event
    * @return
    */
  def areaFromEnd(n: Int, k: List[Int]): Double = {
    assert(k.length == 3, "Works only for 3 dimensions ATM " + k)
    //TODO should be 0 to n or 1 to n?
    val kComb = for (i <- 0 to n; j <- 0 to n; l <- k(2) to n if i + j + l == n) yield (i, j, l)

    def loop(acc: Double, kComb: IndexedSeq[(Int, Int, Int)]): Double = {
      // println("ACC " + acc + "\nkComb: " + kComb)
      if (kComb.tail.isEmpty) acc + pSuccess(n, List(kComb.head._1, kComb.head._2, kComb.head._3))
      else loop(acc + pSuccess(n, List(kComb.head._1, kComb.head._2, kComb.head._3)), kComb.tail)
    }

    loop(0, kComb)
  }

  def rightSurprise(n: Int, k: List[Int]): Double = math.max(0.0, -math.log(areaFromEnd(n, k) * 2.0) / math.log(2.0))

}
