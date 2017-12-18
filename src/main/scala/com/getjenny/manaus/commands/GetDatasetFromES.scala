package com.getjenny.manaus.commands

import java.io.{File, FileWriter}

import breeze.io.CSVWriter
import scopt.OptionParser

import com.typesafe.scalalogging.LazyLogging

object GetDatasetFromES extends LazyLogging {

  private case class Params(
    output_file: String = "",
    type_name: String = "question",
    query_min_threshold: Double = 0.0,
    index_name: String = "index_0.question",
    cluster_name: String = "starchat",
    ignore_cluster_name: Boolean = true,
    index_language: String = "english",
    host_map: Map[String, Int] = Map("localhost" -> 9300)
  )

  def doDataSerialization(params: Params): Unit = {

    val cmd_utils = CommandsUtils

    val elastic_client = ElasticClientKB(type_name=params.type_name,
      query_min_threshold = params.query_min_threshold, index_name = params.index_name,
      cluster_name = params.cluster_name, ignore_cluster_name = params.ignore_cluster_name,
      index_language = params.index_name, host_map = params.host_map)

    val search_hits = cmd_utils.search(elastic_client)

    def out_data = search_hits.map(hit => {
      IndexedSeq[String](hit._1, hit._2)
    })

    println("INFO: data serialization on file")

    val output_file = new File(params.output_file)
    val file_writer = new FileWriter(output_file)

    //sentence, type, conv_id, sentence_id
    val csv_writer = CSVWriter.write(output=file_writer,
      mat=out_data,
      separator=';',
      quote='"',
      escape='\\')

    println("INFO: data serialization completed")
  }

  def main(args: Array[String]) {
    val defaultParams = Params()
    val parser = new OptionParser[Params]("GetDatasetFromES") {
      head("extract the dataset from elasticsearch.")
      help("help").text("prints this usage text")
      opt[String]("output_file").required()
        .text(s"the output file")
        .action((x, c) => c.copy(output_file = x))
      opt[String]("type_name")
        .text(s"the type name on ElasticSearch" +
        s"  default: ${defaultParams.type_name}")
        .action((x, c) => c.copy(type_name = x))
      opt[Double]("query_min_threshold")
        .text(s"a min threshdold for search" +
        s"  default: ${defaultParams.query_min_threshold}")
        .action((x, c) => c.copy(query_min_threshold = x))
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
        doDataSerialization(params)
      case _ =>
        sys.exit(1)
    }
  }

}