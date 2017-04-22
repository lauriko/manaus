package com.getjenny.manaus

import com.getjenny.manaus.util.Binomial

/** e.g.
  * It receives two inputs:
  *
  * conversations: A List of `String`s, where each element is a conversation
  * priorOccurrences: A Map with occurrences of words as given by external corpora (wiki etc)
  *
  * E.g.:
  *
  *
  ```
    import scala.io.Source
    // Load the prior occurrences
    val wordColumn = 1
    val occurrenceColumn = 2
    val filePath = "/Users/mal/pCloud/Data/word_frequency.tsv"
    val priorOccurrences: Map[String, Int] = (for (line <- Source.fromFile(filePath).getLines)
      yield (line.split("\t")(wordColumn).toLowerCase -> line.split("\t")(occurrenceColumn).toInt)).toMap.withDefaultValue(0)
    // instantiate the Conversations
    val rawConversations = Source.fromFile("/Users/mal/pCloud/Scala/extractor/tieto_convs.head.csv").getLines.toList
    val conversations = new Conversations(rawConversations, priorOccurrences)
*```
  *
  *
  * Created by Mario Alemi on 07/04/2017 in El Estrecho, Putumayo, Peru
  */
class Conversations(val rawConversations: List[String], tokenizer: String => List[String],
                    val priorOccurrences: Map[String, Int] = Map() withDefaultValue(0),
                    val stopwords: Set[String] = Set()) {


  /**
    * @param s
    */
  class Sentence(s: List[String]) {
    private val minSentenceInfoBit = 32
    private val minKeywordInfo = 8
    val localOccurrences: Map[String, Int] = s.map(_.toLowerCase).groupBy(w => w).map(p => (p._1, p._2.length))
    val wordsInfo: Map[String, Double] = (for (w <- s if observedOccurrences(w.toLowerCase) > 0)
      yield (w.toLowerCase, Binomial(priorN+observedN, observedOccurrences(w.toLowerCase) + priorOccurrences(w.toLowerCase))
        .rightSurprise(s.length, localOccurrences(w.toLowerCase) ))).toMap
    val totalInformation: Double = wordsInfo.toList.foldLeft(0.0)((acc, v) => acc + v._2)

    /**
      *
      * @return List of words with high information (keywords) and associated information
      */
    def keywords: List[(String, Double)]  = {
      if (totalInformation <= minSentenceInfoBit) List()
      else wordsInfo.toList.filter(x => x._2 > minKeywordInfo).map(x => (x._1, x._2/totalInformation )).sortBy(-_._2)
    }
  }

  // No words with two letters, words which appear only once in the corpus (if this is big enough)
  private val minObservedNForPruning: Int = 100000
  private def pruneSentence(sentence: List[String]): List[String] = {
    if (observedN > minObservedNForPruning) for (w <- sentence if w.length > 2 && observedOccurrences(w.toLowerCase) > 1) yield w
    else for (w <- sentence if w.length > 2 ) yield w
  }

  val priorN: Int = priorOccurrences.toList.foldLeft(0.0)((acc, v) => acc + v._2).round.toInt
  // Prepare sentences (each is a list of Strings)
  val sentences: List[List[String]] = for (l <- rawConversations if tokenizer(l).nonEmpty)
    yield tokenizer(l).filterNot(x => stopwords.contains(x.toLowerCase))
  val observedVocabulary: List[String] =  sentences.flatten
  val observedN: Int = observedVocabulary.length
  val observedOccurrences: Map[String, Int] =
    observedVocabulary.map(_.toLowerCase).groupBy(w => w).map(p => (p._1, p._2.length)) withDefaultValue 0

  private val minWordsPerSentence: Int = 10 // a sentence with less than that is not considered
  val rawBagOfKeywordsInfo: List[List[(String, Double)]] =
    for (s <- sentences.map(x => pruneSentence(x)) if s.length >= minWordsPerSentence) yield new Sentence(s).keywords
  val rawKeywords: List[Set[String]] = for (l <- rawBagOfKeywordsInfo) yield (for (ki <- l) yield ki._1).toSet

  // Now we want to filter the important keywords. These are the ones
  // who appear often enough not to surprise us anymore.
  val extractedKeywords: Map[String, Double] = (rawBagOfKeywordsInfo.flatten.map(_._1) groupBy (w => w))
    .map(p => (p._1, Binomial(priorN+observedN, observedOccurrences(p._1.toLowerCase) + priorOccurrences(p._1.toLowerCase)).activePotential(p._2.length)))

  //TODO temporary solution, need to understand how to set a cutoff
  // (and for some reasons having extractedKeywords as Map creates problems in keywords:
  // extractedKeywords.toMap(ki._1)
  // type mismatch
  // found   : String
  // required: <:<[(String, Double),(?, ?)]
  private val ekList = extractedKeywords.toList.sortBy(_._2)
  val cutoff: Double = ekList(ekList.length/10)._2

  val keywords: List[Set[String]] = for (l <- rawBagOfKeywordsInfo) yield (for (ki <- l if extractedKeywords(ki._1) < cutoff) yield ki._1).toSet

}
