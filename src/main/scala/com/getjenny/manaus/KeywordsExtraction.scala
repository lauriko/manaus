package com.getjenny.manaus

import com.getjenny.manaus.util.Binomial


/** Created by Mario Alemi on 07/04/2017 in El Estrecho, Putumayo, Peru
  *
  *
  *
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
  *
  *
  *
  * @param priorOccurrences Map with occurrence for each word from a corpus different from the conversation log.
  * @param exchanges list of tokenized sentences grouped by conversation
  * @param observedOccurrences occurrence of terms into the observed vocabulary
  * @param minObservedNForPruning the min number of occurrences of the word in the corpus vocabulary
  * @param minWordsPerSentence a sentence with less than that is not considered
  *
  *
  */
class KeywordsExtraction(priorOccurrences: TokensOccurrences,
                         observedOccurrences: TokensOccurrences,
                         exchanges: List[List[(String, List[String])]],
                         minObservedNForPruning: Int = 100000,
                         minWordsPerSentence: Int = 10) {

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

  // No words with two letters, words which appear only once in the corpus (if this is big enough)
  private def pruneSentence(sentence: List[String], min_chars: Int = 2): List[String] = {
    val pruned_sentence = if (observedOccurrences.getTokenN > minObservedNForPruning)
      sentence.filter(_.length > min_chars).map(token => token.toLowerCase)
        .map(token => (token, observedOccurrences.getOccurrence(token))).filter(_._2 > 1)
        .map(_._1)
    else
      sentence.filter(_.length > min_chars)
    pruned_sentence
  }

  // Prepare sentences (each is a list of Strings)
  val sentences: List[List[String]] = exchanges.flatMap(_.map( _._2  ))
  val observedVocabulary: List[String] = sentences.flatten

  // Because we want to check that keywords are correctly extracted,
  //  will have tuple like (original words, keywords, bigrams...)
  val rawBagOfKeywordsInfo: List[(List[String], List[(String, Double)])] =
    sentences.map(x => pruneSentence(x)).filter(_.length >= minWordsPerSentence)
      .map(x => {(x, new Sentence(x).keywords)})

  val rawKeywords: List[(List[String], Set[String])] = for (l <- rawBagOfKeywordsInfo)
    yield (l._1, (for (ki <- l._2) yield ki._1).toSet)

  // Now we want to filter the important keywords. These are the ones
  // who appear often enough not to surprise us anymore.
  val extractedKeywords: Map[String, Double] =
    (rawBagOfKeywordsInfo.flatMap(_._2).map(_._1) groupBy (w => w))
      .map(p =>
        (p._1,
          Binomial(priorOccurrences.getTokenN + observedOccurrences.getTokenN,
            observedOccurrences.getOccurrence(p._1.toLowerCase) + priorOccurrences.getOccurrence(p._1.toLowerCase)
          ).activePotential(p._2.length)
        )
      )

  //TODO temporary solution, need to understand how to set a cutoff
  private val ekList = extractedKeywords.toList.sortBy(_._2)
  val cutoff: Double = ekList(ekList.length/10)._2

  val keywords: List[(List[String], Set[String])] =
    rawBagOfKeywordsInfo.map(sentence => {
      val pruned_sentence_tokens = sentence._1
      val extracted_keywords = sentence._2.map(token => (token, extractedKeywords(token._1))).filter(_._2 < cutoff)
        .map(x => x._1._1).toSet
      (pruned_sentence_tokens, extracted_keywords)
    })

  //TODO :
  // * extract keywords of conversations
  // * extract key bi/trigrams in conversations

}
