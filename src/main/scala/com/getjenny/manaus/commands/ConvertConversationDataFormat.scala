package com.getjenny.manaus.commands

import java.io.{File, FileWriter}

import breeze.io.CSVWriter
import com.getjenny.manaus.util._
import scopt.OptionParser

import scala.io.Source

object ConvertConversationDataFormat {

  private case class Params(
    input_file: String = "data/in_conversations.txt",
    output_file: String = ""
  )

  def doFormatConversion(params: Params): Unit = {

    // list of tokenized sentences grouped by conversation
    val rawConversationsLines = Source.fromFile(params.input_file).getLines
    val exchanges: Iterator[(List[((String, String, List[String]), Int)], Int)] =
      rawConversationsLines.map(line => {
        split_sentences(line)
    }).zipWithIndex

    val new_format_conversations = exchanges.map(exchange => {
      exchange._1.map(conv => {
        IndexedSeq(conv._1._1, conv._1._2, exchange._2.toString, conv._2.toString)
      })
    }).flatten

    val output_file = new File(params.output_file)
    val file_writer = new FileWriter(output_file)

    val entries = new_format_conversations.toTraversable

    //sentence, type, conv_id, sentence_id
    val csv_writer = CSVWriter.write(output=file_writer,
      mat=entries,
      separator=';',
      quote='"',
      escape='\\')

  }

  def main(args: Array[String]) {
    val defaultParams = Params()
    val parser = new OptionParser[Params]("ConvertConversationDataFormat") {
      head("convert conversation data from an input format to another.")
      help("help").text("prints this usage text")
      opt[String]("input_file")
        .text(s"the file with raw conversation, a conversation per line with interactions separated by ;" +
          s"  default: ${defaultParams.input_file}")
        .action((x, c) => c.copy(input_file = x))
      opt[String]("output_file").required()
        .text(s"a semi column separated file with the following fields (sentence, type, conv_id, sentence_id)")
        .action((x, c) => c.copy(output_file = x))
    }

    parser.parse(args, defaultParams) match {
      case Some(params) =>
        doFormatConversion(params)
      case _ =>
        sys.exit(1)
    }
  }

}