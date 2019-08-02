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
    word_frequencies: String = "statistics_data/english/word_frequency.tsv",
    minWordsPerSentence: Int = 10,
    pruneTermsThreshold: Int = 100000,
    misspell_max_occurrence: Int = 5,
    output_file: String = "",
    active_potential_decay: Int = 10,
    total_info: Boolean = false,
    active_potential: Boolean = true
  )


  def doKeywordExtraction(params: Params): Unit = {

    val cmd_utils = CommandsUtils
    val minWordsPerSentence = params.minWordsPerSentence
    val pruneTermsThreshold = params.pruneTermsThreshold
    val misspell_max_occurrence = params.misspell_max_occurrence
    val priorOccurrences = cmd_utils.readPriorOccurrencesMap(params.word_frequencies)

    println("INFO: getting sentences and observedOccurrences")
    val (sentences, observedOccurrences) =
      cmd_utils.buildObservedOccurrencesMapFromConversationsFormat1(params.raw_conversations)

    println("INFO: extract keywords")
    val keywordsExtraction = new KeywordsExtraction(priorOccurrences=priorOccurrences,
      observedOccurrences=observedOccurrences)

    println("INFO: extract informativeWords")
    /* Informative words */
    val rawBagOfKeywordsInfo: Stream[List[(String, Double)]] = sentences.map(sentence => {
      val informativeK = keywordsExtraction.extractInformativeWords(sentence = sentence._2,
        pruneSentence = pruneTermsThreshold, minWordsPerSentence = minWordsPerSentence,
        totalInformationNorm = params.total_info)
      informativeK
    })

    println("INFO: calculating active potentials Map")
    /* Map(keyword -> active potential) */
    val activePotentialKeywordsMap = keywordsExtraction.getWordsActivePotentialMap(rawBagOfKeywordsInfo,
      params.active_potential_decay)

    println("INFO: getting informative words for sentences")
    val informativeKeywords: Stream[(List[String], List[(String, Double)])] =
      sentences.zip(rawBagOfKeywordsInfo).map(sentence => {
      (sentence._1._2, sentence._2)
    })

    println("INFO: calculating bags")
    // list of the final keywords
    val bags: Stream[(List[String], Map[String, Double])] =
      if(params.active_potential) {
        keywordsExtraction.extractBagsActive(activePotentialKeywordsMap = activePotentialKeywordsMap,
          informativeKeywords = informativeKeywords, misspellMaxOccurrence = misspell_max_occurrence)
      } else {
        keywordsExtraction.extractBagsNoActive(informativeKeywords = informativeKeywords,
          misspellMaxOccurrence = misspell_max_occurrence)
      }


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
      val keywords = bag._2.toSeq.sortBy(- _._2).map(x => x._1 + "|" + x._2.toString).mkString(" ")
      IndexedSeq(sentence._1, sentence._3, sentence._4, keywords)
    })

    println("INFO: results serialization on file")

    val output_file = new File(params.output_file)
    val file_writer = new FileWriter(output_file)

    //sentence, type, conv_id, sentence_id
    CSVWriter.write(output=file_writer,
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
      opt[Int]("min_words_in_sentence")
        .text(s"discard the sentences with less that N words" +
          s"  default: ${defaultParams.minWordsPerSentence}")
        .action((x, c) => c.copy(minWordsPerSentence = x))
      opt[Int]("prune_sentence_threshold")
        .text(s"threshold on the number of terms for trigger pruning" +
          s"  default: ${defaultParams.pruneTermsThreshold}")
        .action((x, c) => c.copy(pruneTermsThreshold = x))
      opt[Int]("mispell_max_occurrence")
        .text(s"given a big enough sample, min freq beyond what we consider the token a misspell" +
          s"  default: ${defaultParams.misspell_max_occurrence}")
        .action((x, c) => c.copy(misspell_max_occurrence = x))
      opt[Int]("active_potential_decay")
        .text(s"introduce a penalty on active potential for words which does not occur enough" +
          s"  default: ${defaultParams.active_potential_decay}")
        .action((x, c) => c.copy(active_potential_decay = x))
      opt[Boolean]("total_info")
        .text(s"normalize the information by total informations" +
          s"  default: ${defaultParams.total_info}")
        .action((x, c) => c.copy(total_info = x))
      opt[Boolean]("active_potential")
        .text(s"weight bags with active potential" +
          s"  default: ${defaultParams.active_potential}")
        .action((x, c) => c.copy(active_potential = x))
    }

    parser.parse(args, defaultParams) match {
      case Some(params) =>
        doKeywordExtraction(params)
      case _ =>
        sys.exit(1)
    }
  }

}