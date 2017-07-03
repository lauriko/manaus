package com.getjenny.manaus.commands

/**
  * Created by angelo on 03/07/17.
  */

import com.typesafe.scalalogging.LazyLogging
import org.elasticsearch.action.update.UpdateResponse
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.xcontent.XContentBuilder
import org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder
import org.elasticsearch.rest.RestStatus

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}

import scala.concurrent._
import ExecutionContext.Implicits.global

case class UpdateDocumentResult(index: String,
                                dtype: String,
                                id: String,
                                version: Long,
                                created: Boolean
                               )

case class KBDocumentUpdate(
                             question_scored_terms: Option[List[(String, Double)]] = None: Option[List[(String, Double)]] ,
                             answer_scored_terms: Option[List[(String, Double)]] = None: Option[List[(String, Double)]]
                           )

case class ElasticClientKB (val type_name: String, val query_min_threshold: Double,
                            val index_name: String, val cluster_name: String,
                            val ignore_cluster_name: Boolean,
                            val index_language: String, val host_map: Map[String, Int]) extends ElasticClient {

  def updateDocument(id: String, document: KBDocumentUpdate, elastic_client: ElasticClient):
                                                            Future[Option[UpdateDocumentResult]] = Future {
    val builder : XContentBuilder = jsonBuilder().startObject()
    document.question_scored_terms match {
      case Some(t) =>
        val array = builder.startArray("question_scored_terms")
        t.foreach(q => {
          array.startObject().field("term", q._1)
            .field("score", q._2).endObject()
        })
        array.endArray()
      case None => ;
    }

    document.answer_scored_terms match {
      case Some(t) =>
        val array = builder.startArray("answer_scored_terms")
        t.foreach(q => {
          array.startObject().field("term", q._1).field("score", q._2).endObject()
        })
        array.endArray()
      case None => ;
    }
    builder.endObject()

    val client: TransportClient = elastic_client.get_client()
    val response: UpdateResponse = client.prepareUpdate(elastic_client.index_name, elastic_client.type_name, id)
      .setDoc(builder)
      .get()

    val doc_result: UpdateDocumentResult = UpdateDocumentResult(index = response.getIndex,
      dtype = response.getType,
      id = response.getId,
      version = response.getVersion,
      created = response.status == RestStatus.CREATED
    )

    Option {doc_result}
  }

}

