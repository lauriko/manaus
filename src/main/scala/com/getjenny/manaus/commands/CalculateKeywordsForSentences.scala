package com.getjenny.manaus.commands

import com.getjenny.manaus.util._
import com.getjenny.manaus._
import scopt.OptionParser
import scala.io.Source

object CalculateKeywordsForSentences {

  private case class Params(
    raw_conversation: String = "data/conversations.txt",
    word_frequencies: String = "data/word_frequency.tsv"
  )

  def readPriorOccurrencesMap(word_frequencies: String,
                              wordColumn: Int = 1, occurrenceColumn: Int = 2): TokensOccurrences = {
    val priorOccurrencesMap: Map[String, Int] = Source.fromFile(word_frequencies).getLines
      .map(line => {
        val splitted_line = line.split("\t")
        splitted_line(wordColumn).toLowerCase -> line.split("\t")(occurrenceColumn).toInt
      }).toMap.withDefaultValue(0)

    val priorOccurrences = new PriorTokensOccurrencesMap(priorOccurrencesMap)
    priorOccurrences
  }

  def buildObservedOccurrencesMapFromConversations(conversations_file: String):
    (List[List[String]], TokensOccurrences) = {

    // instantiate the Conversations
    val rawConversationsLines = Source.fromFile(conversations_file).getLines.toList

    // list of tokenized sentences grouped by conversation
    val exchanges: List[List[(String, List[String])]] = rawConversationsLines.map(line => {
      split_sentences(line)
    }).filter(_.nonEmpty)

    // Prepare sentences (each is a list of Strings)
    val sentences: List[List[String]] = exchanges.flatMap(_.map(_._2)).map(s => s.map(w => w.toLowerCase))
    val observedVocabulary: List[String] = sentences.flatten

    val observedOccurrencesMap: Map[String, Int] =
      observedVocabulary.groupBy(identity).mapValues(_.length) withDefaultValue 0

    val observedOccurrences = new ObservedTokensOccurrencesMap(observedOccurrencesMap)
    (sentences, observedOccurrences)
  }

  def doKeywordExtraction(params: Params, minWordsPerSentence: Int = 10): Unit = {
    // Load the prior occurrences
    val priorOccurrences = readPriorOccurrencesMap(params.word_frequencies)
    val (sentences, observedOccurrences) = buildObservedOccurrencesMapFromConversations(params.raw_conversation)

    val keywordsExtraction = new KeywordsExtraction(priorOccurrences=priorOccurrences,
      observedOccurrences=observedOccurrences)

    /* Informative words */
    val rawBagOfKeywordsInfo = keywordsExtraction.extractInformativeWords(sentences)

    /* Map(keyword -> active potential) */
    val activePotentialKeywordsMap = keywordsExtraction.getWordsActivePotentialMap(rawBagOfKeywordsInfo)

    val extractedKeywordsList = activePotentialKeywordsMap.toList.sortBy(_._2)
    val cutoff: Double = extractedKeywordsList(extractedKeywordsList.length/10)._2

    // list of the final keywords
    val bags: List[(List[String], Set[String])] =
        keywordsExtraction.extractBags(activePotentialKeywordsMap = activePotentialKeywordsMap,
        informativeKeywords = rawBagOfKeywordsInfo, sentences = sentences)

    println("Raw Keywords:\n" + rawBagOfKeywordsInfo.take(100).mkString("\n"))

    println("Total Extracted Keywords: " + activePotentialKeywordsMap.toList.length)
    println("Extracted Keywords:\n" + activePotentialKeywordsMap.take(500))

    println("Clean Keywords:\n" + bags.take(500))

    val g = Bags(bags)

    println("Bigrams:\n" + g.llrSignificativeBigrams)
  }

  def main(args: Array[String]) {
    val defaultParams = Params()
    val parser = new OptionParser[Params]("KeywordExtractionSample") {
      head("extract the most relevant keywords from text.")
      help("help").text("prints this usage text")
      opt[String]("raw_conversation").required()
        .text(s"the file with raw conversation, a conversation per line with interactions separated by ;" +
          s"  default: ${defaultParams.raw_conversation}")
        .action((x, c) => c.copy(raw_conversation = x))
      opt[String]("word_frequencies").required()
        .text(s"the file with word frequencies" +
          s"  default: ${defaultParams.word_frequencies}")
        .action((x, c) => c.copy(word_frequencies = x))
    }

    parser.parse(args, defaultParams) match {
      case Some(params) =>
        doKeywordExtraction(params)
      case _ =>
        sys.exit(1)
    }
  }

}