package com.getjenny.manaus

import com.getjenny.manaus.util.Binomial
import com.typesafe.scalalogging.LazyLogging

import scala.collection.SeqView


/** Created by Mario Alemi on 07/04/2017 in El Estrecho, Putumayo, Peru
  *
  * conversations: A List of `String`s, where each element is a conversation.
  * tokenizer
  * priorOccurrences: A Map with occurrences of words as given by external corpora (wiki etc)
  *
  * Example of usage:
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
    val rawConversations = Source.fromFile("/Users/mal/pCloud/Scala/manaus/convs.head.csv").getLines.toList
    val conversations = new Conversations(rawConversations=rawConversations, tokenizer=tokenizer,
      priorOccurrences=priorOccurrences)
*```
  *
  * @param priorOccurrences Map with occurrence for each word from a corpus different from the conversation log.
  * @param observedOccurrences occurrence of terms into the observed vocabulary
  *
  */
class KeywordsExtraction(priorOccurrences: TokensOccurrences,
                         observedOccurrences: TokensOccurrences) extends LazyLogging {

  /**
    * @param sentence_tokens list of sentence tokens
    */
  class Sentence(sentence_tokens: List[String],
                 minSentenceInfoBit: Int = 32,
                 minKeywordInfo: Int = 8,
                 totalInformationNorm: Boolean = false
                ) {
    val localOccurrences: Map[String, Int] =
      sentence_tokens.groupBy(identity).mapValues(_.length)

    val wordsInfo: Map[String, Double] = sentence_tokens.map(token => {
      (token, observedOccurrences.getOccurrence(token))
    }).filter(_._2 > 0).map(token => {
      (token._1,
        Binomial(priorOccurrences.getTokenN + observedOccurrences.getTokenN,
          observedOccurrences.getOccurrence(token._1) + priorOccurrences.getOccurrence(token._1))
          .rightSurprise(sentence_tokens.length, localOccurrences(token._1)))
    }).toMap

    val totalInformation: Double = wordsInfo.values.sum

    /**
      *
      * @return List of words with high information (keywords) and associated information
      */
    def keywordsNotNorm: List[(String, Double)]  = {
      if (totalInformation <= minSentenceInfoBit)
        List()
      else
        wordsInfo.filter(x => x._2 > minKeywordInfo).mapValues(_.toDouble).toList.sortBy(-_._2)
    }

    /**
      *
      * @return List of words with high information (keywords) and associated information
      */
    def keywordsNormTotalInfo: List[(String, Double)]  = {
      if (totalInformation <= minSentenceInfoBit)
        List()
      else
        wordsInfo.filter(x => x._2 > minKeywordInfo).mapValues(_/totalInformation).toList.sortBy(-_._2)
    }

    def keywords = if (totalInformationNorm) keywordsNormTotalInfo else keywordsNotNorm
  }

  /** Clean a list of tokens e.g. No words with two letters,
    *   words which appear only once in the corpus (if this is big enough)
    * @param sentence the list of the token of the sentence
    * @param minObservedNForPruning the min number of occurrences of the word in the corpus vocabulary
    * @param min_chars the min number of character for a token
    * @return a cleaned list of tokens
    */
  def pruneSentence(sentence: List[String],
                    minObservedNForPruning: Int = 100000, min_chars: Int = 2): List[String] = {
    val pruned_sentence = if (observedOccurrences.getTokenN > minObservedNForPruning)
      sentence.filter(_.length > min_chars).map(token => token)
        .map(token => (token, observedOccurrences.getOccurrence(token))).filter(_._2 > 1)
        .map(_._1)
    else
      sentence.filter(_.length > min_chars)
    pruned_sentence
  }

    /** Informative words
    *   Because we want to check that keywords are correctly extracted,
    *   will have tuple like (original words, keywords, bigrams...)
    * @param sentence a sentence as a list of words
    * @param pruneSentence a threshold on the number of terms for trigger pruning
    * @param minWordsPerSentence the minimum amount of words on each sentence
    * @return the list of most informative words for each sentence
    */
  def extractInformativeWords(sentence: List[String], pruneSentence: Int = 100000, minWordsPerSentence: Int = 10,
                              totalInformationNorm: Boolean):
                      List[(String, Double)] = {
    val pruned = this.pruneSentence(sentence)
    val filtered = if(pruned.length > minWordsPerSentence) pruned else List.empty[String]
    val keywords = if(filtered.nonEmpty) new Sentence(sentence_tokens = filtered,
      totalInformationNorm = totalInformationNorm).keywords else List.empty[(String, Double)]
    keywords
  }

  /** Refined keywords list,
    *   Now we want to filter the important keywords. These are the ones
    *   who appear often enough not to surprise us anymore.
    * @param informativeKeywords the list of informative words for each sentence
    * @return the map of keywords weighted with active potential
    */
  def getWordsActivePotentialMap(informativeKeywords: Stream[List[(String, Double)]], decay: Int=10):
              Map[String, Double] = {

    logger.info("calculating informative keywords frequency")
    val informativeKeywordsFrequency = informativeKeywords.flatMap(_.map(_._1))
      .filter(_.nonEmpty).foldLeft(Map.empty[String, Int]){
        (count, word) => count + (word -> (count.getOrElse(word, 0) + 1))
      }

    logger.info("calculating active potential")
    val extractedKeywords: Map[String, Double] =
      informativeKeywordsFrequency.map(p => {
        val pair = (p._1,
          Binomial(priorOccurrences.getTokenN + observedOccurrences.getTokenN,
            observedOccurrences.getOccurrence(p._1) + priorOccurrences.getOccurrence(p._1)
          ).activePotential(p._2, decay)
        )
        pair
      })

    extractedKeywords
  }

  /** extract the final keywords without active potential weighting
    *
    * @param informativeKeywords the list of informative keywords for each sentence
    * @param misspellMaxOccurrence given a big enough sample, min freq beyond what we consider the token a misspell
    * @return the final list of keywords for each sentence
    */
  def extractBagsNoActive(informativeKeywords: Stream[(List[String], List[(String, Double)])],
                  misspellMaxOccurrence: Int = 5): Stream[(List[String], Map[String, Double])] = {

//    val extractedKeywordsList = activePotentialKeywordsMap.toList.sortBy(-_._2)
//    val highest_occurence = extractedKeywordsList.head
//    logger.debug("highest_occurence " + highest_occurence)
//    val cutoff: Double = Math.min( Math.round(highest_occurence._2 / 100.0), misspell_max_occurrence )
//      //extractedKeywordsList(extractedKeywordsList.length/cutoff_percentage)._2

    val bags: Stream[(List[String], Map[String, Double])] =
      informativeKeywords.map(bagOfKeywordsAndScore => {
        val bagOfKeywords = bagOfKeywordsAndScore._1
        val extractedKeywords = bagOfKeywordsAndScore._2.map(token =>
            (token._1, token._2)).toMap
        (bagOfKeywords, extractedKeywords)
      })
    bags
  }

  /** extract the final keywords with active potential weighting
    *
    * @param activePotentialKeywordsMap map of keywords weighted by active potential (see getWordsActivePotentialMap)
    * @param informativeKeywords the list of informative keywords for each sentence
    * @param misspellMaxOccurrence given a big enough sample, min freq beyond what we consider the token a misspell
    * @return the final list of keywords for each sentence
    */
  def extractBagsActive(activePotentialKeywordsMap: Map[String, Double],
                  informativeKeywords: Stream[(List[String], List[(String, Double)])],
                  misspellMaxOccurrence: Int = 5): Stream[(List[String], Map[String, Double])] = {

//    val extractedKeywordsList = activePotentialKeywordsMap.toList.sortBy(-_._2)
//    val highest_occurence = extractedKeywordsList.head
//    logger.debug("highest_occurence " + highest_occurence)
//    val cutoff: Double = Math.min( Math.round(highest_occurence._2 / 100.0), misspell_max_occurrence )
//      //extractedKeywordsList(extractedKeywordsList.length/cutoff_percentage)._2

    val bags: Stream[(List[String], Map[String, Double])] =
      informativeKeywords.map(bagOfKeywordsAndScore => {
        val bagOfKeywords = bagOfKeywordsAndScore._1
        val extractedKeywords = bagOfKeywordsAndScore._2.map(token =>
            (token._1, token._2 * activePotentialKeywordsMap(token._1))).toMap
        (bagOfKeywords, extractedKeywords)
      })
    bags
  }

}
