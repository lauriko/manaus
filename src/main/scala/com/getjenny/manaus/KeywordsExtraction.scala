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
  * @param exchanges list of tokenized sentences grouped by conversation
  * @param observedOccurrences occurrence of terms into the observed vocabulary
  *
  */
class KeywordsExtraction(priorOccurrences: TokensOccurrences,
                         observedOccurrences: TokensOccurrences,
                         exchanges: List[List[(String, List[String])]]) {

  /**
    * @param sentence_tokens list of sentence tokens
    */
  class Sentence(sentence_tokens: List[String],
                 minSentenceInfoBit: Int = 32,
                 minKeywordInfo: Int = 8
                ) {
    val localOccurrences: Map[String, Int] =
      sentence_tokens.map(_.toLowerCase).groupBy(identity).mapValues(_.length)

    val wordsInfo: Map[String, Double] = sentence_tokens.map(token => {
      (token, observedOccurrences.getOccurrence(token.toLowerCase))
    }).filter(_._2 > 0).map(token => {
      val lowercase_token = token._1.toLowerCase
      (lowercase_token,
        Binomial(priorOccurrences.getTokenN + observedOccurrences.getTokenN,
          observedOccurrences.getOccurrence(lowercase_token) + priorOccurrences.getOccurrence(lowercase_token))
          .rightSurprise(sentence_tokens.length, localOccurrences(lowercase_token)))
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
      sentence.filter(_.length > min_chars).map(token => token.toLowerCase)
        .map(token => (token, observedOccurrences.getOccurrence(token))).filter(_._2 > 1)
        .map(_._1)
    else
      sentence.filter(_.length > min_chars)
    pruned_sentence
  }

}
