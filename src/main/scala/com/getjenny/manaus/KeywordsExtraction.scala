package com.getjenny.manaus

import com.getjenny.manaus.util.Binomial


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
                         observedOccurrences: TokensOccurrences) {

  /**
    * @param sentence_tokens list of sentence tokens
    */
  class Sentence(sentence_tokens: List[String],
                 minSentenceInfoBit: Int = 32,
                 minKeywordInfo: Int = 8
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
    def keywords: List[(String, Double)]  = {
      if (totalInformation <= minSentenceInfoBit)
        List()
      else
        wordsInfo.filter(x => x._2 > minKeywordInfo).mapValues(_/totalInformation).toList.sortBy(-_._2)
    }
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
    * @param sentences list of sentences, each sentence is a list of words
    * @param minWordsPerSentence the minimum amount of words on each sentence
    * @return the list of most informative words for each sentence
    */
  def extractInformativeWords(sentences: List[List[String]], minWordsPerSentence: Int = 10):
                              List[(List[String], List[(String, Double)])] = {
    val rawBagOfKeywordsInfo: List[(List[String], List[(String, Double)])] =
      sentences
        .map(x => this.pruneSentence(x)).filter(_.length >= minWordsPerSentence)
        .map(x => {
          (x, new this.Sentence(x).keywords)
        })
    rawBagOfKeywordsInfo
  }

  /** Refined keywords list,
    *   Now we want to filter the important keywords. These are the ones
    *   who appear often enough not to surprise us anymore.
    * @param informativeKeywords the list of informative words for each sentence
    * @return the map of keywords weighted with active potential
    */
  def getWordsActivePotentialMap(informativeKeywords: List[(List[String], List[(String, Double)])]):
              Map[String, Double] = {
    val extractedKeywords: Map[String, Double] =
      (informativeKeywords.flatMap(_._2).map(_._1) groupBy (w => w))
        .map(p =>
         (p._1,
           Binomial(priorOccurrences.getTokenN + observedOccurrences.getTokenN,
             observedOccurrences.getOccurrence(p._1) + priorOccurrences.getOccurrence(p._1)
           ).activePotential(p._2.length)
         )
       )
    extractedKeywords
  }

  /** extract the final keywords
    *
    * @param activePotentialKeywordsMap map of keywords weighted by active potential (see getWordsActivePotentialMap)
    * @param informativeKeywords the list of informative keywords for each sentence
    * @param sentences the list of token for each sentence
    * @param cutoff_percentage a cutoff for low active potential tokens
    * @return the final list of keywords for each sentence
    */
  def extractBags(activePotentialKeywordsMap: Map[String, Double],
                  informativeKeywords: List[(List[String], List[(String, Double)])],
                  sentences: List[List[String]],
                 cutoff_percentage: Int = 10): List[(List[String], Set[String])] = {

    val extractedKeywordsList = activePotentialKeywordsMap.toList.sortBy(_._2)
    val cutoff: Double = extractedKeywordsList(extractedKeywordsList.length/cutoff_percentage)._2

    val bags: List[(List[String], Set[String])] =
      informativeKeywords.map(sentence => {
        val pruned_sentence_tokens = sentence._1
        val extracted_keywords = sentence._2.map(token =>
            (token, activePotentialKeywordsMap(token._1))).filter(_._2 < cutoff)
            .map(x => x._1._1).toSet
        (pruned_sentence_tokens, extracted_keywords)
      })
    bags
  }


}
