package com.getjenny.manaus.util

import scala.collection.immutable.Set.Set2

/**
  * Given bags of keywords, computes the most significative associations.
  *
  * 1. Makes 4 co-location matrices (AB, A!B, !AB, !A!B)
  *
  *
  * Created by Mario Alemi on 12/04/2017 in Jutai, Amazonas, Brazil
  */
case class Bags(bags: List[Set[String]]) {

  val vocabulary: Set[String] = bags.flatten.toSet
  val n: Int = bags.length
  val occurrences: Map[String, Int] = bags.flatten.groupBy(x=>x).map(x => (x._1, x._2.length))

  ///A Map Set(Bigram_1, Bigram_2) -> Occurrences of both words Bigram_1 and Bigram_2 in the same bag
  //TODO Inefficient?
  val m11: Map[Set[String], Int] =
    (for (bag <- bags) yield for (w1 <- bag; w2 <- bag if w1 < w2) yield Set(w1, w2) ).flatten.groupBy(x=>x).map(x => (x._1, x._2.length)).withDefaultValue(0)

  /**
    * How often the first word appear, and the second word doesn't, in all bags?
    * NB Keys of the map here are not Set2, but Tuples2.
    * Of course, m10(a, b) = m01(b, a)
    */
  val m10: Map[(String, String), Int] =
    (for (bag <- bags) yield for (w1 <- bag; w2 <- vocabulary.diff(bag))
      yield (w1, w2) ).flatten.groupBy(x=>x).map(x => (x._1, x._2.length)).withDefaultValue(0)

  // Map Set(Bigram_1, Bigram_2) -> Occurrences of neither word Bigram_1 nor Bigram_2 in a bag
  //val m00: Map[Set[String], Int] = (for (bag <- bags) yield for (w1 <- vocabulary.diff(bag); w2 <- vocabulary.diff(bag)) yield Set(w1, w2) ).flatten.groupBy(x=>x).map(x => (x._1, x._2.length)).withDefaultValue(0)
  def m00(bigram: Set[String]): Int = bags.length - m11(bigram) - m10((bigram.head, bigram.tail.head)) - m10((bigram.tail.head, bigram.head))

  // Total number of produced bigrams
  val nBigrams: Int = m11.values.sum

  // List of List of 2 words, each ordered by decreasing occurrence
  val orderedBigrams: List[List[String]] = m11.toList.map(_._1.toList.sortBy(-occurrences(_))).filter(_.length == 2)

  // Trigrams and associated occurrences
  //TODO NB filtering out occ < 2)
  val m111: Map[Set[String], Int] =
    (for (bag <- bags) yield for (w1 <- bag; w2 <- bag; w3 <- bag if w1 < w2 && w2 < w3)
      yield Set(w1, w2, w3) ).flatten.groupBy(x=>x).map(x => (x._1, x._2.length)).withDefaultValue(0).filter(kv => kv._2 > 2)

  val trigrams: Set[Set[String]] = m111.keys.toSet

  // trigrams.contains(b.union(Set(w)))  because we are only interested in trigrams that exist
  val m110: Map[(String, String, String), Int] =
    (for (bag <- bags) yield for (t <- trigrams if t.size == 3 &&
      bag.contains(t.head) && bag.contains(t.tail.head) &&  !bag.contains(t.tail.tail.head) )
      yield (t.head, t.tail.head, t.tail.tail.head)).flatten.groupBy(x=>x).map(x => (x._1, x._2.length)).withDefaultValue(0)

  val m100: Map[(String, String, String), Int] =
    (for (bag <- bags) yield for (t <- trigrams if t.size == 3 &&
      bag.contains(t.head) && !bag.contains(t.tail.head) &&  !bag.contains(t.tail.tail.head) )
      yield (t.head, t.tail.head, t.tail.tail.head)).flatten.groupBy(x=>x).map(x => (x._1, x._2.length)).withDefaultValue(0)

  def m000(t: List[String]): Int = {
    bags.length - m111(t.toSet) -
      m100((t.head, t(1), t(2))) - m100((t(1), t.head, t(2))) - m100((t(2), t(1), t.head)) -
      m110((t.head, t(1), t(2))) - m110((t.head, t(2), t(1))) - m110((t(2), t(1), t.head))
  }

  // Total number of produced trigrams
  val nTrigrams: Int = m111.values.sum

  // List of List of 3 words, each ordered by decreasing occurrence
  val orderedTrigrams: List[List[String]] = m111.toList.map(_._1.toList.sortBy(-occurrences(_))).filter(_.length == 3)

  /**
    * The LLR Score for Bigrams
    * @return
    */
  def llr2(bigram: Set[String]): Double = {
    val bigramTuple = (bigram.head, bigram.tail.head)
    m11(bigram) * m00(bigram) - m10(bigramTuple._1, bigramTuple._2) * m10(bigramTuple._2, bigramTuple._1)
  }

  val llr2Matrix: Map[Set[String], Double] = (for (bigram <- m11.keys) yield (bigram, llr2(bigram))).toMap

  val llrSignificativeBigrams: List[(Set[String], Double)] = llr2Matrix.toList.filterNot(_._1.size == 1)sortBy(-_._2)

  /**
    * Here, we consider "A" more freq than "B". We then consider occurrence(A) as the
    * samples, and A && B as the success.
    *
    * This gives an approximated result of binomialSignificativeBigrams
    */
  val binomialSignificativeBigrams: List[(List[String], Double)] = (for (b <- orderedBigrams)
    yield (b, Binomial(samples=n, successes=occurrences(b.tail.head))
      .rightSurprise(n=occurrences(b.head), k=m11(b.toSet)))).sortBy(-_._2).filterNot(_._2.isNaN)

  // println(binomialSignificativeBigramsFast.filter(_._2 > 20).mkString("\n") )

  /**
    * Here we do the correct thing. Consider the bigram(A, B). Three events:
    *   None=1-(1-P(A))*(1-P(B)), JustOne=P(A)+P(B)-2*P(A)*P(B), Bigram=P(A)*P(B).
    *
    * We have produced nBigrams bigrams, with M00 None, M10+M01 JustOne, M11 Bigram.
    *
    * Let's compute the surprise for that number of Bigrams.
    *
    */
  val binomialSignificativeBigramsExact: List[(Set[String], Double)] = (for (b <- m11.keys if b.size == 2)
    yield (b, Trinomial(nBigrams, expectedTriOccurrence(nBigrams, occurrences(b.head), occurrences(b.tail.head)))
      .rightSurprise(nBigrams, List(m00(b), m10(b2t(b))+m10(b2tInv(b)), m11(b)) ))).toList.sortBy(-_._2)

//  def makeTrigramProbabilities(t: List[String]): List[Int] =
//    List(m100((t.head, t(1), t(2))), m110((t.head, t(1), t(2)))+m110((t.head, t(2), t(1))), m111(t.toSet))
//
//  val usefulTrigrams: Map[List[String], List[Int]] = (for (t <- orderedTrigrams if makeTrigramProbabilities(t).product > 0)
//    yield (t, makeTrigramProbabilities(t))).toMap
//
//  // Not really good. The best would probably be to consider the "bigram" (significativeBigram, word).
//  val trinomialSignificativeTrigramsFastFirstAttemp: List[(List[String], Double)] = (for (tp <- usefulTrigrams.toList)
//    yield (tp._1, Trinomial(samples=n, expectedTriOccurrence(nBigrams, occurrences(tp._1(1)), occurrences(tp._1(2))))
//        .rightSurprise(n=occurrences(tp._1.head), k=tp._2))).sortBy(-_._2)

  val sb: List[Set[String]] = binomialSignificativeBigrams.filter(_._2 > 8).map(_._1.toSet)
  val bigramWord: List[(Set[String], String)] =  for (b <- sb; t <- trigrams if t.intersect(b) == b) yield (b, t.diff(b).head)

  // This looks better, but
  //TODO need to sum now all values corresponding to same key (see polynomial in course)
  val trinomialSignificativeTrigramsFast: List[(Set[String], Double)] = (for (bw <- bigramWord.take(1000)  if occurrences(bw._2) > m11(bw._1) )
    yield (bw._1 + bw._2, Binomial(samples=n, successes=m11(bw._1))
      .rightSurprise(n=occurrences(bw._2), k=m111(bw._1 + bw._2)))).sortBy(-_._2)




  // val bags = List(Set("A", "B"), Set("A", "B", "C"), Set("A", "B", "D"), Set("E", "F"), Set("C"), Set("C"), Set("C"), Set("C"))
}
