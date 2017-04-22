import com.getjenny.manaus.Conversations
import com.getjenny.manaus.util.Bags
import com.getjenny.manaus.util.tokenizer

object testone {

  import scala.io.Source
  // Load the prior occurrences
  val wordColumn = 1
  val occurrenceColumn = 2
  val filePath = "/Users/mal/pCloud/Data/word_frequency.tsv"
  val priorOccurrences: Map[String, Int] = (for (line <- Source.fromFile(filePath).getLines)
    yield (line.split("\t")(wordColumn).toLowerCase -> line.split("\t")(occurrenceColumn).toInt)).toMap.withDefaultValue(0)
  // instantiate the Conversations
  val stopwords=Source.fromFile("en_stopwords.txt").getLines.toSet
  val rawConversations = Source.fromFile("/Users/mal/pCloud/Scala/manaus/convs.head5000.csv").getLines.toList
  val conversations = new Conversations(rawConversations=rawConversations, tokenizer=tokenizer,
    priorOccurrences=priorOccurrences, stopwords=stopwords)
  val bags = conversations.keywords


  println("Raw Keywords:\n" + conversations.rawBagOfKeywordsInfo.take(100).mkString("\n"))

  println("Total Extracted Keywords:\n" + conversations.extractedKeywords.toList.length)
  println("Extracted Keywords:\n" + conversations.extractedKeywords.take(500))

  println("Clean Keywords:\n" + conversations.keywords.take(500))

  val g = Bags(bags)
  //val g = LogLikelihoodScore(List(Set("A", "B", "C"), Set("A", "B", "D"), Set("A", "B")))

  println("Bigrams:\n" + g.llrSignificativeBigrams)

}