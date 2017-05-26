package com.getjenny.manaus.commands

import com.getjenny.manaus.ExtractKeywords
import com.getjenny.manaus.util.Bags
import com.getjenny.manaus.util.split_sentences
import scopt.OptionParser

import scala.io.Source

object SampleKeywordExtraction {

  private case class Params(
    raw_conversation: String = "data/conversations.txt",
    word_frequencies: String = "data/word_frequency.tsv"
  )

  def doKeywordExtraction(params: Params): Unit = {
    // Load the prior occurrences
    val wordColumn = 1
    val occurrenceColumn = 2
    val word_frequencies = params.word_frequencies
    val priorOccurrences: Map[String, Int] = (for (line <- Source.fromFile(word_frequencies).getLines)
      yield line.split("\t")(wordColumn).toLowerCase -> line.split("\t")(occurrenceColumn).toInt)
      .toMap.withDefaultValue(0)

    // instantiate the Conversations
    val rawConversations = Source.fromFile(params.raw_conversation).getLines.toList
    val conversations = new ExtractKeywords(rawConversations=rawConversations, tokenizer=split_sentences,
      priorOccurrences=priorOccurrences)
    val bags = conversations.keywords

    println("Raw Keywords:\n" + conversations.rawBagOfKeywordsInfo.take(100).mkString("\n"))

    println("Total Extracted Keywords:\n" + conversations.extractedKeywords.toList.length)
    println("Extracted Keywords:\n" + conversations.extractedKeywords.take(500))

    println("Clean Keywords:\n" + conversations.keywords.take(500))

    val g = Bags(bags)
    //val g = LogLikelihoodScore(List(Set("A", "B", "C"), Set("A", "B", "D"), Set("A", "B")))

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