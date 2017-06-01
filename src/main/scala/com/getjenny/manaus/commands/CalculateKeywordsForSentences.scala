package com.getjenny.manaus.commands

import com.getjenny.manaus.util._
import com.getjenny.manaus._
import breeze.io.{CSVReader, CSVWriter}
import java.io.{File, FileWriter, FileReader}
import scopt.OptionParser
import scala.io.Source

object CalculateKeywordsForSentences {

  private case class Params(
    raw_conversations: String = "data/conversations.txt",
    word_frequencies: String = "data/word_frequency.tsv",
    output_file: String = ""
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

  def buildObservedOccurrencesMapFromConversations(conversations_file: String) = {

    // instantiate the Conversations
    val rawConversationsLines = Source.fromFile(conversations_file).getLines.toList

    val file = new File(conversations_file)
    val file_reader = new FileReader(file)
    lazy val file_entries = CSVReader.read(input=file_reader, separator=';',
      quote='"', escape='\\', skipLines=0)

    // list of tokenized sentences grouped by conversation
    // (sentence, tokenized_sentence, type, conv_id, sentence_id)
    val sentences = file_entries.map(line => {
      val sentence = line(0)
      val tokenized_sentence = sentence.split(" ").toList.filter(_ != "").map(w => w.toLowerCase)
      val entry_type = line(1)
      val conv_id = line(2)
      val sentence_id = line(3)
      (sentence, tokenized_sentence, entry_type, conv_id, sentence_id)
    })

    val observedOccurrencesMap: Map[String, Int] = sentences.flatMap(e => e._2)
      .groupBy(identity).mapValues(_.length) withDefaultValue 0

    val observedOccurrences = new ObservedTokensOccurrencesMap(observedOccurrencesMap)
    (sentences, observedOccurrences)
  }

  def doKeywordExtraction(params: Params, minWordsPerSentence: Int = 10): Unit = {
    // Load the prior occurrences
    val priorOccurrences = readPriorOccurrencesMap(params.word_frequencies)
    val (sentences, observedOccurrences) = buildObservedOccurrencesMapFromConversations(params.raw_conversations)

    val keywordsExtraction = new KeywordsExtraction(priorOccurrences=priorOccurrences,
      observedOccurrences=observedOccurrences)

    /* Informative words */
    val rawBagOfKeywordsInfo: Iterable[List[(String, Double)]] = sentences.map(sentence => {
      val informativeK = keywordsExtraction.extractInformativeWords(sentence._2)
      informativeK
    })

    /* Map(keyword -> active potential) */
    val activePotentialKeywordsMap = keywordsExtraction.getWordsActivePotentialMap(rawBagOfKeywordsInfo)

    val informativeKeywords: Iterable[(List[String], List[(String, Double)])] =
      sentences.zip(rawBagOfKeywordsInfo).map(sentence => {
      (sentence._1._2, sentence._2)
    })

    // list of the final keywords
    val bags: Iterable[(List[String], Set[String])] =
        keywordsExtraction.extractBags(activePotentialKeywordsMap = activePotentialKeywordsMap,
        informativeKeywords = informativeKeywords)

    /*
    println("Raw Keywords:\n" + sentences.map(_._2).zip(rawBagOfKeywordsInfo).take(100).mkString("\n"))
    println("Total Extracted Keywords: " + activePotentialKeywordsMap.toList.length)
    println("Extracted Keywords:\n" + activePotentialKeywordsMap.take(500))
    println("Clean Keywords:\n" + bags.toList)
    */

    val out_keywords = sentences.zip(bags).map(item => {
      val sentence = item._1
      val bag = item._2
      IndexedSeq(sentence._1, sentence._3, sentence._4, sentence._5, bag._2.mkString(" "))
    })

    val output_file = new File(params.output_file)
    val file_writer = new FileWriter(output_file)

    val entries = out_keywords.toTraversable

    //sentence, type, conv_id, sentence_id
    val csv_writer = CSVWriter.write(output=file_writer,
      mat=entries,
      separator=';',
      quote='"',
      escape='\\')

    /*
    val g = Bags(bags.toList)
    println("Bigrams:\n" + g.llrSignificativeBigrams)
    */
  }

  def main(args: Array[String]) {
    val defaultParams = Params()
    val parser = new OptionParser[Params]("KeywordExtractionSample") {
      head("extract the most relevant keywords from text.")
      help("help").text("prints this usage text")
      opt[String]("raw_conversations").required()
        .text(s"the file with raw conversation, a conversation per line with interactions separated by ;" +
          s"  default: ${defaultParams.raw_conversations}")
        .action((x, c) => c.copy(raw_conversations = x))
      opt[String]("word_frequencies").required()
        .text(s"the file with word frequencies" +
          s"  default: ${defaultParams.word_frequencies}")
        .action((x, c) => c.copy(word_frequencies = x))
      opt[String]("output_file").required()
        .text(s"the output file")
        .action((x, c) => c.copy(output_file = x))
    }

    parser.parse(args, defaultParams) match {
      case Some(params) =>
        doKeywordExtraction(params)
      case _ =>
        sys.exit(1)
    }
  }

}