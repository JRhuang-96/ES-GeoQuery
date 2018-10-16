package cn.geo;



import com.alibaba.fastjson.JSONObject;
import com.vividsolutions.jts.geom.Coordinate;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.geo.ShapeRelation;
import org.elasticsearch.common.geo.builders.CircleBuilder;
import org.elasticsearch.common.geo.builders.ShapeBuilders;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.geobounds.GeoBounds;
import org.elasticsearch.search.aggregations.metrics.geobounds.GeoBoundsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.geocentroid.GeoCentroid;
import org.elasticsearch.search.aggregations.metrics.geocentroid.GeoCentroidAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static cn.geo.GeoShapeQuery.getHitsInfo;
import static cn.geo.GetCenter.getCenterPoint4;
import static cn.geo.GetCenter.getPointCenter;


/**
 * 此类中的方法适应于 geo_point
 */
public class GeoPointQuery {
    private static String field = "location";

    private static GeoPoint center = new GeoPoint(3.074999, 3.24999);

    private static String distance = "500";

    private static List<GeoPoint> points =new ArrayList<>();

    private static GeoPoint topLeft = new GeoPoint(6,1);
    private static GeoPoint bottomRight =new GeoPoint(2,6);

    public static void main(String[] args) throws IOException {



        points.add(new GeoPoint(0,0));
        points.add(new GeoPoint(2,40));
        points.add(new GeoPoint(40,50));
        points.add(new GeoPoint(50,-42));
        points.add(new GeoPoint(-40,-50));
        // points.add(new GeoPoint(0,0));


        //多边形查询
         polygonSearch(field,points);
        //矩形查询
        // poRectangleSearch(field, topLeft, bottomRight);
        //圆形查询
        // distanceSearch(field, distance, center);


    }


    /**
     * 矩形范围查找
     * @param field
     * @param topLeftPoint
     * @param bottomRightPoint
     * @return
     * @throws IOException
     */
    public static SearchResult poRectangleSearch(String field,GeoPoint topLeftPoint,GeoPoint bottomRightPoint) throws IOException {
        //创建客户端
        RestHighLevelClient client = ESRestClientUtil.getDefaultClient();

        SearchResult result =new SearchResult();

        SearchRequest request = new SearchRequest();
            request.indices("example").types("type");


        SearchSourceBuilder builder = new SearchSourceBuilder();

        //矩形
        GeoBoundingBoxQueryBuilder geoBoundingBoxQueryBuilder  = QueryBuilders.geoBoundingBoxQuery(field)
                                                                              .setCorners(topLeftPoint,bottomRightPoint);

        //测试两种聚合, test the GeoBoundsAgg and GeoCentroidAgg
        // GeoCentroidAggregationBuilder centroid  = AggregationBuilders.geoCentroid("center").field(field);

        // GeoBoundsAggregationBuilder  boundsAggregationBuilder =  AggregationBuilders.geoBounds("bounds").field(field);



        builder.query(geoBoundingBoxQueryBuilder);
        //组合两种指标聚合
        // builder.aggregation(centroid);
        // builder.aggregation(boundsAggregationBuilder);

        request.source(builder);


        long start = System.currentTimeMillis();

        SearchResponse response =  client.search(request);

        //获取中心点
        // GeoCentroid center=  response.getAggregations().get("center");
        // System.out.println(center.centroid());

        //获取最小矩形
        // GeoBounds geoBounds = response.getAggregations().get("bounds");
        // System.out.println(geoBounds.bottomRight()+",\n"+geoBounds.topLeft());

        long end = System.currentTimeMillis();

        int searchTime = (int)(end-start);

        SearchHit[] hits= response.getHits().getHits();

        result = resultInfo(response,result,searchTime);

        System.out.println(result);
        for (SearchHit hit:hits) {
      
            System.out.println(hit.getSourceAsString());

        }
        System.out.println("-----------");

        client.close();
        return result;

    }


    /**
     * 查指定距离内点
     * @param field    :查询的字段
     * @param distance : 距离
     * @param center:   中心点
     * @return
     */
    private static SearchResult distanceSearch(String field, String distance, GeoPoint center) throws IOException {
        RestHighLevelClient client = ESRestClientUtil.getDefaultClient();

        SearchSourceBuilder builder = new SearchSourceBuilder();
        SearchRequest request = new SearchRequest();
            request.indices("example").types("type");


        SearchResult result = new SearchResult();

        //distance 查询
        GeoDistanceQueryBuilder geoDistanceQueryBuilder = QueryBuilders.geoDistanceQuery(field);
        geoDistanceQueryBuilder.point(center).distance(distance,DistanceUnit.KILOMETERS);

        //测试两种聚合 ,test the GeoBoundsAgg and GeocentroidAgg
        // GeoCentroidAggregationBuilder centroidBuilder = AggregationBuilders.geoCentroid("center").field(field);
        // GeoBoundsAggregationBuilder  boundsAggregationBuilder =  AggregationBuilders.geoBounds("bounds").field(field);




        builder.query(geoDistanceQueryBuilder);

        //创建
        // builder.aggregation(centroidBuilder);
        // builder.aggregation(boundsAggregationBuilder);

        request.source(builder);

        long start = System.currentTimeMillis();
        SearchResponse response = client.search(request);

        //从响应中拿到聚合结果
        // GeoCentroid centroid = response.getAggregations().get("center");
        // GeoBounds bounds =  response.getAggregations().get("bounds");

        //输出两种聚合结果
        // System.out.println(centroid.centroid());
        // System.out.println(bounds.bottomRight()+"\n"+bounds.topLeft());

        long end = System.currentTimeMillis();

        int searchTime = (int) (end - start);
        SearchHit[] hits= response.getHits().getHits();

        result = resultInfo(response,result,searchTime);
        System.out.println(result);
        client.close();
        return result;


    }


    /**
     *
     * @param Field : 查询字段
     * @param points : 构成多边形中的点,至少5个点
     * @return
     * @throws IOException
     */
    private static SearchResult polygonSearch(String Field,List<GeoPoint> points) throws IOException {
        RestHighLevelClient client =ESRestClientUtil.getDefaultClient();

        SearchSourceBuilder builder = new SearchSourceBuilder();
        SearchRequest request = new SearchRequest();
        request.indices("example").types("type");

        SearchResult result =new SearchResult();

        //根据 collection 中的点构成形状查询
        GeoPolygonQueryBuilder geoPolygonQueryBuilder = QueryBuilders.geoPolygonQuery(Field, points);

        builder.query(geoPolygonQueryBuilder);

        request.source(builder);


        long start = System.currentTimeMillis();
        SearchResponse response =  client.search(request);
        long end = System.currentTimeMillis();

        int searchTime = (int)(end-start);
        SearchHit[] hits= response.getHits().getHits();

        result = resultInfo(response,result,searchTime);
        System.out.println(result);


        client.close();
        return result;


    }



    static SearchResult resultInfo(SearchResponse response ,SearchResult result,int searchTime){

        int totalHits =(int)response.getHits().getTotalHits();
        int tookTime = (int)response.getTook().getMillis();
        int length =  response.getHits().getHits().length;


        result.setTotalHits(totalHits);
        result.setSearchTime(searchTime);
        result.setTooK(tookTime);
        result.setResultCount(length);
        result.setThreadSize(0);

        return result;

    }





}