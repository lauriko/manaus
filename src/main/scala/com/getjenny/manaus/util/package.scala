package com.getjenny.manaus

/**
  * Created by Mario Alemi on 06/04/2017.
  */
package object util {
  val factorial:Map[Int, Double] = Map((0, 1.0), (1, 1.0), (2, 2.0), (3, 6.0), (4, 24.0), (5, 120.0), (6, 720.0), (7, 5040.0)).withDefault(x => x*factorial(x-1))
  def binomialFactor(n: Int, k: Int): Double = factorial(n)/(factorial(n-k)*factorial(k))
  def multinomialFactor(n: Int, k: List[Int]): Double = factorial(n)/ k.map(factorial(_)).product

  def bigramSet2tuple(bigram: Set[String]): (String, String) = {
    assert(bigram.size == 2, "b2t only for bigrams")
    (bigram.head, bigram.tail.head)
  }
  def bigramSet2tupleInverted(bigram: Set[String]): (String, String) = {
    assert(bigram.size == 2, "b2t only for bigrams")
    (bigram.tail.head, bigram.head)
  }

  /**
    * Given the occurrences of two words k1 and k2 in a sample of n bigrams, makes the expected relative frequencies:
    *
    * None/n=1-(1-P(1))*(1-P(2)), JustOne/n=P(1)+P(2)-2*P(1)*P(2), Bigram/n=P(1)*P(2)
    *
    * @param n
    * @param k1
    * @param k2
    */
  def expectedTriOccurrence(n: Int, k1: Int, k2: Int): List[Double] = {
    val p1 = k1*1.0/n
    val p2 = k2*1.0/n
    val noWord: Double = n*(1-p1)*(1-p2)
    val oneWord: Double = n*(p1+p2-2*p1*p2)
    val bothWords: Double = p1*p2
    List(noWord, oneWord, bothWords)
  }


  /**
    * Ad-hoc tokenizer for our (private) test data.
    *
    * @param line A string with the conversation in this format: """ "CLIENT: I want to renew a subscription...";"AGENT: Sure, tell me your name..."\n """
    * @return List(List("CLIENT", "I want to renew a subscription..."), List("AGENT", "Sure, tell me your name..."))
    */
  def tokenizer(line: String): List[(String, List[String])] = {
    try {
      val splitLine = line.split("\";\"").map(_.trim.replaceAll("\"", ""))
      def loop(pp: List[List[String]], splitLine: Array[String]): List[List[String]] = {
        if (splitLine.tail.isEmpty) {
          if (splitLine.head.trim.take(5) == "OTHER") pp
          else if (pp.isEmpty) splitLine.head.trim.stripPrefix("\"").stripSuffix("\"").split(": ").toList :: pp
          else if (pp.head.head == "CLIENT" && splitLine.head.take(6) == "CLIENT") List("CLIENT", pp.head(1) + " " + splitLine.head.trim.stripPrefix("\"").stripSuffix("\"").split(": ")(1)) :: pp.tail
          else if (pp.head.head == "AGENT" && splitLine.head.take(5) == "AGENT") List("AGENT", pp.head(1) + " " + splitLine.head.trim.stripPrefix("\"").stripSuffix("\"").split(": ")(1)) :: pp.tail
          else splitLine.head.trim.stripPrefix("\"").stripSuffix("\"").split(": ").toList :: pp
        } else {
          if (splitLine.head.trim.take(5) == "OTHER") loop(pp, splitLine.tail)
          else if (pp.isEmpty) loop(splitLine.head.trim.stripPrefix("\"").stripSuffix("\"").split(": ").toList :: pp, splitLine.tail)
          else if (pp.head.head == "CLIENT" && splitLine.head.take(6) == "CLIENT")
            loop( List("CLIENT", pp.head(1) + " " + splitLine.head.trim.stripPrefix("\"").stripSuffix("\"").split(": ")(1)) :: pp.tail, splitLine.tail)
          else if (pp.head.head == "AGENT" && splitLine.head.take(5) == "AGENT")
            loop( List("AGENT", pp.head(1) + " " + splitLine.head.trim.stripPrefix("\"").stripSuffix("\"").split(": ")(1) ) :: pp.tail, splitLine.tail)
          else loop(splitLine.head.trim.stripPrefix("\"").stripSuffix("\"").split(": ").toList :: pp, splitLine.tail)
        }
      }

      loop(List(), splitLine ).map(x => List(x.head, """[^a-zA-Z0-9]""".r.replaceAllIn(x(1).replaceAll("'", ""), " ") ) ).map(x => (x.head, x(1).split(" ").toList.filter(_.length > 0) ) )

    } catch {
      case e: Exception => List()
    }
  }
 }
