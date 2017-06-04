package com.getjenny.manaus.commands

import com.getjenny.manaus.util._
import com.getjenny.manaus._
import breeze.io.{CSVReader, CSVWriter}
import java.io.{File, FileWriter, FileReader}
import scopt.OptionParser
import scala.io.Source
import scala.collection.SeqView

object CalculateKeywordsForSentences {

  private case class Params(
    raw_conversations: String = "data/conversations.txt",
    word_frequencies: String = "data/word_frequency.tsv",
    minWordsPerSentence: Int = 10,
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

  def getSentences(conversations_file: String): SeqView[IndexedSeq[String], Seq[_]] = {
    val file = new File(conversations_file)
    val file_reader = new FileReader(file)
    val file_entries = CSVReader.read(input=file_reader, separator=';',
      quote='"', escape='\\', skipLines=0)
    file_entries.view
  }

  def buildObservedOccurrencesMapFromConversations(conversations_file: String) = {


    // list of tokenized sentences grouped by conversation
    // (sentence, tokenized_sentence, type, conv_id, sentence_id)
    def sentences = getSentences(conversations_file).map(line => {
      val tokenized_sentence = line(0).split(" ").toList.filter(_ != "").map(w => w.toLowerCase)
      (line(0), tokenized_sentence, line(1), line(2), line(3))
    })

    println("INFO: calculating observedOcurrencesMap")
    val observedOccurrencesMap = sentences.flatMap(line => line._2)
      .foldLeft(Map.empty[String, Int]){
        (count, word) => count + (word -> (count.getOrElse(word, 0) + 1))
      }

    val observedOccurrences = new ObservedTokensOccurrencesMap(observedOccurrencesMap)

    (sentences, observedOccurrences)
  }

  def doKeywordExtraction(params: Params): Unit = {
    // Load the prior occurrences

    val minWordsPerSentence = params.minWordsPerSentence
    val priorOccurrences = readPriorOccurrencesMap(params.word_frequencies)

    println("INFO: getting sentences and observedOccurrences")
    val (sentences, observedOccurrences) = buildObservedOccurrencesMapFromConversations(params.raw_conversations)

    println("INFO: extract keywords")
    val keywordsExtraction = new KeywordsExtraction(priorOccurrences=priorOccurrences,
      observedOccurrences=observedOccurrences)

    println("INFO: extract informativeWords")
    /* Informative words */
    val rawBagOfKeywordsInfo: SeqView[List[(String, Double)], Seq[_]] = sentences.map(sentence => {
      val informativeK = keywordsExtraction.extractInformativeWords(sentence._2, minWordsPerSentence)
      informativeK
    })

    println("INFO: calculating active potentials Map")
    /* Map(keyword -> active potential) */
    val activePotentialKeywordsMap = keywordsExtraction.getWordsActivePotentialMap(rawBagOfKeywordsInfo)

    println("INFO: getting informative words for sentences")
    val informativeKeywords: SeqView[(List[String], List[(String, Double)]), Seq[_]] =
      sentences.zip(rawBagOfKeywordsInfo).map(sentence => {
      (sentence._1._2, sentence._2)
    })

    println("INFO: calculating bags")
    // list of the final keywords
    val bags: SeqView[(List[String], Set[String]), Seq[_]] =
        keywordsExtraction.extractBags(activePotentialKeywordsMap = activePotentialKeywordsMap,
        informativeKeywords = informativeKeywords)

    /*
    println("Raw Keywords:\n" + sentences.map(_._2).zip(rawBagOfKeywordsInfo).take(100).mkString("\n"))
    println("Total Extracted Keywords: " + activePotentialKeywordsMap.toList.length)
    println("Extracted Keywords:\n" + activePotentialKeywordsMap.take(500))
    println("Clean Keywords:\n" + bags.toList)
    */

    println("INFO: merging sentences with bags")

    val out_keywords = sentences.zip(bags).map(item => {
      val sentence = item._1
      val bag = item._2
      IndexedSeq(sentence._1, sentence._3, sentence._4, bag._2.mkString(" "))
    })

    println("INFO: results serialization on file")

    val output_file = new File(params.output_file)
    val file_writer = new FileWriter(output_file)

    //sentence, type, conv_id, sentence_id
    val csv_writer = CSVWriter.write(output=file_writer,
      mat=out_keywords,
      separator=';',
      quote='"',
      escape='\\')

    println("INFO: keywords calculation completed")

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
      opt[Int]("min_words_in_sentence").required()
        .text(s"discard the sentences with less that N words" +
          s"  default: ${defaultParams.minWordsPerSentence}")
        .action((x, c) => c.copy(minWordsPerSentence = x))
    }

    parser.parse(args, defaultParams) match {
      case Some(params) =>
        doKeywordExtraction(params)
      case _ =>
        sys.exit(1)
    }
  }

}