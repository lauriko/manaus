package com.getjenny.manaus.util

/**
  * Builds a Binomial prior. Successes should be Int, but are put Double for more flexibility.
  *
  * Created by Mario Alemi on 06/04/2017.
  */
case class Binomial(samples: Int, successes: Double) {
  val p: Double = successes * 1.0 / samples
  def pSuccess(n: Int, k: Int): Double = binomialFactor(n, k)*math.pow(p, k)*math.pow((1-p), n-k)
  def areaFromEnd(n: Int, k: Int): Double = if (n == k) pSuccess(n, k) else pSuccess(n, k) + areaFromEnd(n, k + 1)

  def rightSurprise(n: Int, k: Int): Double = math.max(0.0, -math.log(areaFromEnd(n, k) * 2.0) / math.log(2.0))

  /**
    * If k is the number of times a word appears as keyword, frequent keywords are
    * promoted, rare keywords (eg misspells and names) are not
    * @param k
    * @return
    */
  def inverseActivePotential(k: Int): Double = -math.log(p) * math.exp(k)

  /**
    * Active potential is defined as the surprise associated to penalize words which
    * doesn't occur enough
    */
  def activePotential(k: Int, d: Int = 10): Double = 1 - math.exp(-k/d)

}
