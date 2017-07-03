package main; /**
 * Created by atuladhar on 6/27/17.
 */

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilders.*;
import processing.data.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.elasticsearch.common.xcontent.XContentFactory.*;


class ES {

  static Client getClient() throws UnknownHostException {
    Settings settings = Settings.builder().put("cluster.name", "elasticsearch").build();
//    TransportClient client = new PreBuiltTransportClient(settings);
    TransportClient client = new PreBuiltTransportClient(Settings.EMPTY)
        .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9300));

    return client;
  }

  SearchHit[] getData() {
    Client client = null;
    try {
      client = getClient();
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }

    SearchResponse response = client.prepareSearch("test")
        .setTypes("test")
        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
        .setQuery(QueryBuilders.matchAllQuery())                 // Query
//        .setQuery(QueryBuilders.termQuery("multi", "test"))                 // Query
//        .setPostFilter(QueryBuilders.rangeQuery("age").from(12).to(18))     // Filter
        .setFrom(0).setSize(60).setExplain(true)
        .get();

    client.close();
    return response.getHits().getHits();
  }


  void insertData(JSONObject obj) throws UnknownHostException {
    Client client = getClient();
    BulkRequestBuilder bulkRequest = client.prepareBulk();

// either use client#prepare, or use Requests# to directly build index/delete requests
    bulkRequest.add(client.prepareIndex("test", "test", "1")
        .setSource()
    );


    BulkResponse bulkResponse = bulkRequest.get();
    if (bulkResponse.hasFailures()) {
      System.out.println("failure");
      // process failures by iterating through each bulk response item
    }
    client.close();
  }

}


