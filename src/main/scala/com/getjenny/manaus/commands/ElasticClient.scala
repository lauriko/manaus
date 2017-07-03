package com.getjenny.manaus.commands

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 01/07/16.
  */

import java.net.InetAddress

import com.typesafe.scalalogging.LazyLogging
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.{InetSocketTransportAddress, TransportAddress}
import org.elasticsearch.transport.client.PreBuiltTransportClient

import scala.collection.immutable.{List, Map}

abstract class ElasticClient extends LazyLogging {
  def type_name: String
  def query_min_threshold: Double
  def index_name: String
  def cluster_name: String
  def ignore_cluster_name: Boolean
  def index_language: String
  def host_map: Map[String, Int]

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
