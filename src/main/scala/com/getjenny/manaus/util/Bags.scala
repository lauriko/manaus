package com.getjenny.manaus.util

/**
  * Given bags of (key)words, computes most important 2-tuples and 3-tuples
  *
  * Makes 4 co-location matrices for bigrams:
  *   m11(a, b): How often both terms "a" and "b" appear
  *   m10(a, b): "a" appears, "b" doesn't
  *   m10(a, b) = m01(b, a)
  *   m00(a, b) = none appears
  *
  * NB the keys are sometimes Set (m11 and m00), sometimes tuples (m10).
  *
  * Similarly for tri-grams there are m111, m110, m100, m000.
  *
  * The matrices above are used to compute:
  *
  * * llrSignificativeBigrams: significative bigrams with the log-likelihood Score
  * * binomialSignificativeBigrams: significative bigrams measuring a higher-than expected frequency of one word compared to the other
  * * trinomialSignificativeTrigrams:
  *
  *
  *
  * Created by Mario Alemi on 12/04/2017 in Jutai, Amazonas, Brazil
  */
case class Bags(bags: List[(List[String], Set[String])]) {

  val vocabulary: Set[String] = bags.flatMap(_._2).toSet
  val n: Int = bags.length
  val occurrences: Map[String, Int] = bags.flatMap(_._2).groupBy(identity).mapValues(_.length)

  // A Map Set(Bigram_1, Bigram_2) -> Occurrences of both words Bigram_1 and Bigram_2 in the same bag
  //TODO Inefficient?
  val m11: Map[Set[String], Int] =
    (for (bag <- bags) yield for (w1 <- bag._2; w2 <- bag._2 if w1 < w2) yield Set(w1, w2) ).flatten.groupBy(identity).mapValues(_.length).withDefaultValue(0)

  /**
    * How often the first word appear, and the second word doesn't, in all bags?
    * NB Keys of the map here are not Set2, but Tuples2.
    * Of course, m10(a, b) = m01(b, a)
    */
  val m10: Map[(String, String), Int] =
    (for (bag <- bags) yield for (w1 <- bag._2; w2 <- vocabulary.diff(bag._2))
      yield (w1, w2) ).flatten.groupBy(identity).mapValues(_.length).withDefaultValue(0)

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
    (for (bag <- bags) yield for (w1 <- bag._2; w2 <- bag._2; w3 <- bag._2 if w1 < w2 && w2 < w3)
      yield Set(w1, w2, w3) ).flatten.groupBy(identity).mapValues(_.length).withDefaultValue(0).filter(kv => kv._2 > 2)

  val trigrams: Set[Set[String]] = m111.keys.toSet

  // trigrams.contains(b.union(Set(w)))  because we are only interested in trigrams that exist
  val m110: Map[(String, String, String), Int] =
    (for (bag <- bags) yield for (t <- trigrams if t.size == 3 &&
      bag._2.contains(t.head) && bag._2.contains(t.tail.head) &&  !bag._2.contains(t.tail.tail.head) )
      yield (t.head, t.tail.head, t.tail.tail.head)).flatten.groupBy(x=>x).map(x => (x._1, x._2.length)).withDefaultValue(0)

  val m100: Map[(String, String, String), Int] =
    (for (bag <- bags) yield for (t <- trigrams if t.size == 3 &&
      bag._2.contains(t.head) && !bag._2.contains(t.tail.head) &&  !bag._2.contains(t.tail.tail.head) )
      yield (t.head, t.tail.head, t.tail.tail.head)).flatten.groupBy(identity).mapValues(_.length).withDefaultValue(0)

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

  // println(binomialSignificativeBigrams.filter(_._2 > 20).mkString("\n") )

  /**
    * Here we compute the surprise in a more rigorous way. Consider the bigram(A, B).
    * We construct three events:
    *
    *   None=1-(1-P(A))*(1-P(B)), JustOne=P(A)+P(B)-2*P(A)*P(B), Bigram=P(A)*P(B).
    *
    * We have the total number  bigrams (nBigrams), we can then build "None" with M00, "JustOne" with M10+M01,
    * and "Bigram" with M11.
    *
    * Let's compute the surprise for that number of Bigrams.
    *
    */
  val binomialSignificativeBigramsExact: List[(Set[String], Double)] = (for (b <- m11.keys if b.size == 2)
    yield (b, Trinomial(nBigrams, expectedTriOccurrence(nBigrams, occurrences(b.head), occurrences(b.tail.head)))
      .rightSurprise(nBigrams, List(m00(b), m10(bigramSet2tuple(b))+m10(bigramSet2tupleInverted(b)), m11(b)) ))).toList.sortBy(-_._2)

  val sb: List[Set[String]] = binomialSignificativeBigrams.filter(_._2 > 8).map(_._1.toSet)
  val bigramWord: List[(Set[String], String)] =  for (b <- sb; t <- trigrams if t.intersect(b) == b) yield (b, t.diff(b).head)

  def trinomialSignificativeTrigrams(): Map[Set[String], Double] = {
    def loop(acc: Map[Set[String], Double], bigramWord: List[(Set[String], String)] ): Map[Set[String], Double] = {
      if (bigramWord.tail.isEmpty) acc
      else {
        val k = bigramWord.head._1 + bigramWord.head._2
        val surprise = Binomial(samples = n, successes = m11(bigramWord.head._1))
          .rightSurprise(n = occurrences(bigramWord.head._2), k = m111(k))
        if (!surprise.isNaN) loop(acc = acc.updated(k, acc(k) + surprise), bigramWord = bigramWord.tail)
        else loop(acc = acc.updated(k, acc(k)), bigramWord = bigramWord.tail)
      }
    }
    loop(acc=Map().withDefaultValue(0.0), bigramWord)
  }




  // val bags = List(Set("A", "B"), Set("A", "B", "C"), Set("A", "B", "D"), Set("E", "F"), Set("C"), Set("C"), Set("C"), Set("C"))
}
