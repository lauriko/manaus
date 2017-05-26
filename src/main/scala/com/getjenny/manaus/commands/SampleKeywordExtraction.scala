package com.getjenny.manaus.commands

import com.getjenny.manaus.util._
import com.getjenny.manaus._
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

    val priorOccurrencesMap: Map[String, Int] = Source.fromFile(word_frequencies).getLines
      .map(line => {
        val splitted_line = line.split("\t")
        splitted_line(wordColumn).toLowerCase -> line.split("\t")(occurrenceColumn).toInt
      }).toMap.withDefaultValue(0)

    val priorOccurrences = new PriorTokensOccurrencesMap(priorOccurrencesMap)

    // instantiate the Conversations
    val rawConversationsLines = Source.fromFile(params.raw_conversation).getLines.toList

    val exchanges: List[List[(String, List[String])]] = rawConversationsLines.map(l => {
      split_sentences(l)
    }).filter(_.nonEmpty)

    // Prepare sentences (each is a list of Strings)
    val sentences: List[List[String]] = exchanges.flatMap(_.map( _._2  ))
    val observedVocabulary: List[String] = sentences.flatten

    val observedOccurrencesMap: Map[String, Int] =
      observedVocabulary.map(_.toLowerCase).groupBy(identity).mapValues(_.length) withDefaultValue 0

    val observedOccurrences = new ObservedTokensOccurrencesMap(observedOccurrencesMap)

    val keywordsExtraction = new KeywordsExtraction(priorOccurrences=priorOccurrences,
      observedOccurrences=observedOccurrences,
      exchanges=exchanges)

    val minWordsPerSentence: Int = 10 // a sentence with less than that is not considered
    // Because we want to check that keywords are correctly extracted,
    //  will have tuple like (original words, keywords, bigrams...)
    val rawBagOfKeywordsInfo: List[(List[String], List[(String, Double)])] =
      sentences.map(x => keywordsExtraction.pruneSentence(x)).filter(_.length >= minWordsPerSentence)
        .map(x => {(x, new keywordsExtraction.Sentence(x).keywords)})

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

    val ekList = extractedKeywords.toList.sortBy(_._2)
    val cutoff: Double = ekList(ekList.length/10)._2

    val bags: List[(List[String], Set[String])] =
      rawBagOfKeywordsInfo.map(sentence => {
        val pruned_sentence_tokens = sentence._1
        val extracted_keywords = sentence._2.map(token => (token, extractedKeywords(token._1))).filter(_._2 < cutoff)
          .map(x => x._1._1).toSet
        (pruned_sentence_tokens, extracted_keywords)
      })

    println("Raw Keywords:\n" + rawBagOfKeywordsInfo.take(100).mkString("\n"))

    println("Total Extracted Keywords: " + extractedKeywords.toList.length)
    println("Extracted Keywords:\n" + extractedKeywords.take(500))

    println("Clean Keywords:\n" + bags.take(500))

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