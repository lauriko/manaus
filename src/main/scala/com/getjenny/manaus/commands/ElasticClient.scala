package com.getjenny.manaus.commands

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 01/07/16.
  */

import java.net.InetAddress

import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.{InetSocketTransportAddress, TransportAddress}
import org.elasticsearch.transport.client.PreBuiltTransportClient

import scala.collection.immutable.{List, Map}

case class ElasticClient (type_name: String, query_min_threshold: Double,
                    index_name: String, cluster_name: String, ignore_cluster_name: Boolean,
                    index_language: String, host_map: Map[String, Int]) {


  /*
  val type_name: String = type_name
  val query_min_threshold: Double = query_min_threshold
  val index_name: String = index_name
  val cluster_name: String = cluster_name
  val ignore_cluster_name: Boolean = ignore_cluster_name
  val index_language: String = index_language
  val host_map: Map[String, Int] = host_map
  */

  val settings: Settings = Settings.builder()
    .put("cluster.name", cluster_name)
    .put("client.transport.ignore_cluster_name", ignore_cluster_name)
    .put("client.transport.sniff", false).build()

  val inet_addresses: List[TransportAddress] =
    host_map.map{ case(k,v) => new InetSocketTransportAddress(InetAddress.getByName(k), v) }.toList

  var client : TransportClient = open_client()

  def open_client(): TransportClient = {
    val client: TransportClient = new PreBuiltTransportClient(settings)
      .addTransportAddresses(inet_addresses:_*)
    client
  }

  def get_client(): TransportClient = {
    this.client
  }

  def close_client(client: TransportClient): Unit = {
    client.close()
  }
}
