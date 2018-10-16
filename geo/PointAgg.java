package cn.geo;

import com.alibaba.fastjson.JSONObject;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.geogrid.GeoHashGrid;
import org.elasticsearch.search.aggregations.bucket.range.GeoDistanceAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.range.Range;
import org.elasticsearch.search.aggregations.metrics.geobounds.GeoBounds;
import org.elasticsearch.search.aggregations.metrics.geobounds.GeoBoundsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.geocentroid.GeoCentroid;
import org.elasticsearch.search.aggregations.metrics.geocentroid.GeoCentroidAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * GeoPoint 聚合
 * 桶聚合(buckets Aggregations) :  hashGridAgg,distanceAgg
 * 指标聚合(Metrics Aggregations): geoCentroid ,geoBounds
 */
public class PointAgg {

    public static void main(String[] args) throws IOException {
        GeoPoint point =   new GeoPoint(20,70);
        String field ="location";

//        distanceAgg(field,point);
//        hashGridAgg(field);
        getCenterAgg(field);


    }


    public static void centerAgg(String field) throws IOException {
        RestHighLevelClient client =  ESRestClientUtil.getDefaultClient();
        SearchRequest request = new SearchRequest();
        request.indices("example").types("type");
        SearchSourceBuilder builder = new SearchSourceBuilder();

        //求中心
        GeoCentroidAggregationBuilder centroidBuilder  = AggregationBuilders.geoCentroid("center").field(field);
        //求最小矩形
        // GeoBoundsAggregationBuilder boundsAggregationBuilder = AggregationBuilders.geoBounds("bounds").field(field);


        builder.aggregation(centroidBuilder);
        // builder.aggregation(boundsAggregationBuilder);

        request.source(builder);

        SearchResponse response = client.search(request);

        GeoCentroid centroid = response.getAggregations().get("center");
        // GeoBounds bounds = response.getAggregations().get("bounds");


        GeoPoint point = centroid.centroid();
        System.out.println("center : "+point+", count: "+centroid.count());

        client.close();
    }




    private static void hashGridAgg(String field) throws IOException {

        RestHighLevelClient client = ESRestClientUtil.getDefaultClient();
        SearchRequest request = new SearchRequest();
        request.indices("example").types("type");

        SearchSourceBuilder builder = new SearchSourceBuilder();
        //网格聚合
        AggregationBuilder aggregationBuilder =  AggregationBuilders.geohashGrid("agg")
                                                                    .field(field)
                                                                    .precision(4);


        builder.aggregation(aggregationBuilder);
        request.source(builder);

        SearchResponse response= client.search(request);

        GeoHashGrid agg = response.getAggregations().get("agg");

        for(GeoHashGrid.Bucket entry :agg.getBuckets()){

            String keyString  = entry.getKeyAsString();  // key
            GeoPoint key = (GeoPoint)entry.getKey(); //bucket from value
            long docCount = entry.getDocCount();
            System.out.println("key [{"+keyString+"}],point [{"+key+"}], doc_count [{"+docCount+"}]");
            
            System.out.println("----------");

        }


        client.close();


    }


    private static void getCenterAgg(String field) throws IOException {
        RestHighLevelClient client = ESRestClientUtil.getDefaultClient();
        SearchRequest request = new SearchRequest();
        request.indices("example").types("type");

        SearchSourceBuilder builder = new SearchSourceBuilder();
        //
        AggregationBuilder aggregationBuilder =  AggregationBuilders.geohashGrid("agg")
                                                                    .field(field)
                                                                    .precision(5)
                                                                    .subAggregation(
                                                                                    AggregationBuilders.geoCentroid("center")
                                                                                                       .field(field));



        builder.aggregation(aggregationBuilder);
        request.source(builder);

        SearchResponse response= client.search(request);

        GeoHashGrid agg = response.getAggregations().get("agg");

        for(GeoHashGrid.Bucket entry :agg.getBuckets()) {

            String keyString = entry.getKeyAsString();
            GeoPoint key = (GeoPoint) entry.getKey();
            long docCount = entry.getDocCount();
            System.out.println("key [{" + keyString + "}],point [{" + key + "}], doc_count [{" + docCount + "}]");

            GeoCentroid center = entry.getAggregations().get("center");

            System.out.println("center : " + center.centroid() + ", count: " + center.count());
            System.out.println("--------------");

        }

        client.close();
    }



    private static void distanceAgg(String field, GeoPoint point) throws IOException {
        RestHighLevelClient client = ESRestClientUtil.getDefaultClient();
        SearchRequest request = new SearchRequest();
            request.indices("example").types("type");

        SearchSourceBuilder builder = new SearchSourceBuilder();
        //求范围中点
        GeoDistanceAggregationBuilder geo =  AggregationBuilders.geoDistance("agg", point)
                                                                .field(field)
                                                                .unit(DistanceUnit.KILOMETERS)
                                                                .addUnboundedTo(3.0)
                                                                .addRange(3.0,10)
                                                                .addRange(10.0,500);


        builder.aggregation(geo);
        request.source(builder);

        SearchResponse response= client.search(request);

        Range agg = response.getAggregations().get("agg");
        for(Range.Bucket entry :agg.getBuckets()){
            String key = entry.getKeyAsString();  // key
            Number from = (Number)entry.getFrom(); //bucket from value
            Number to = (Number)entry.getTo();
            long docCount = entry.getDocCount();
            System.out.println("key [{"+key+"}], from [{"+from+"}], to [{"+to+"}], doc_count [{"+docCount+"}]");
        }


        client.close();
    }




}