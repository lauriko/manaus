package com.getjenny.manaus.commands

/**
  * Created by angelo on 02/07/17.
  */

import java.io.{File, FileWriter}

import breeze.io.CSVWriter
import scopt.OptionParser
import com.typesafe.scalalogging.LazyLogging
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}

object UploadKeywords extends LazyLogging {
  private case class Params(
                             input_file: String = "",
                             type_name: String = "question",
                             query_min_threshold: Double = 0.0,
                             index_name: String = "jenny-en-0",
                             cluster_name: String = "starchat",
                             ignore_cluster_name: Boolean = true,
                             index_language: String = "english",
                             host_map: Map[String, Int] = Map("localhost" -> 9300)
                           )

  def doUploadKeywords(params: Params): Unit = {
    val cmd_utils = CommandsUtils

    val elastic_client = ElasticClientKB(type_name=params.type_name,
      query_min_threshold = params.query_min_threshold, index_name = params.index_name,
      cluster_name = params.cluster_name, ignore_cluster_name = params.ignore_cluster_name,
      index_language = params.index_name, host_map = params.host_map)

    println("INFO: data serialization on file")

   val keywords_file_item = cmd_utils.getDataFromCSV(params.input_file).map(line => {
      val document_id = line(0)
      val keywords: List[(String, Double)] = line(1).split(" ").toList.filter(_ != "").map(w => {
        val item = w.split("\\|")
        (item(0), item(1).toDouble)
      })
      (document_id, keywords)
    })

    keywords_file_item.par.foreach(item => {
      val document = KBDocumentUpdate(question_scored_terms = Option{item._2})
      val result = elastic_client.updateDocument(id=item._1, document=document, elastic_client=elastic_client)
      val result_try: Try[Option[UpdateDocumentResult]] = Await.ready(result, 60.seconds).value.get
      result_try match {
        case Success(t) =>
          logger.info("ID(" + item._1 + ") Document(" + document + ")")
        case Failure(e) =>
          logger.error("ID(" + item._1 + ") Document(" + document + ") Error(" + e.getMessage + ")")
      }
    })

    println("INFO: indexing data completed")
  }

  def main(args: Array[String]) {
    val defaultParams = Params()
    val parser = new OptionParser[Params]("UploadKeywords") {
      head("upload extracted keywords on elasticsearch.")
      help("help").text("prints this usage text")
      opt[String]("input_file").required()
        .text(s"the input file")
        .action((x, c) => c.copy(input_file = x))
      opt[String]("type_name")
        .text(s"the type name on ElasticSearch" +
          s"  default: ${defaultParams.type_name}")
        .action((x, c) => c.copy(type_name = x))
      opt[String]("index_name")
        .text(s"the name of the index on ElasticSearch" +
          s"  default: ${defaultParams.index_name}")
        .action((x, c) => c.copy(index_name = x))
      opt[String]("cluster_name")
        .text(s"the name of the cluster on ElasticSearch" +
          s"  default: ${defaultParams.cluster_name}")
        .action((x, c) => c.copy(cluster_name = x))
      opt[String]("index_language")
        .text(s"the language of the datatype on ElasticSearch" +
          s"  default: ${defaultParams.index_language}")
        .action((x, c) => c.copy(index_language = x))
      opt[Boolean]("ignore_cluster_name")
        .text(s"tell if ElasticSearch cluster name should be ignored" +
          s"  default: ${defaultParams.ignore_cluster_name}")
        .action((x, c) => c.copy(ignore_cluster_name = x))
      opt[Map[String, Int]]("host_map")
        .text(s"a list of ElasticSearch nodes" +
          s"  default: ${defaultParams.host_map}")
        .action((x, c) => c.copy(host_map = x))
    }

    parser.parse(args, defaultParams) match {
      case Some(params) =>
        doUploadKeywords(params)
      case _ =>
        sys.exit(1)
    }
  }

}
