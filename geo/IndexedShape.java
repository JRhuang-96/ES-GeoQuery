package cn.geo;

import cn.geo.ESRestClientUtil;
import cn.geo.GeoShapeQuery;
import cn.geo.SearchResult;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.geo.GeoDistance;
import org.elasticsearch.common.geo.ShapeRelation;
import org.elasticsearch.common.geo.builders.PointBuilder;
import org.elasticsearch.common.geo.builders.ShapeBuilder;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.mapper.MappedFieldType;
import org.elasticsearch.index.query.GeoDistanceQueryBuilder;
import org.elasticsearch.index.query.GeoShapeQueryBuilder;
import org.elasticsearch.index.query.PrefixQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.geobounds.GeoBounds;
import org.elasticsearch.search.aggregations.metrics.geobounds.GeoBoundsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.geocentroid.GeoCentroid;
import org.elasticsearch.search.aggregations.metrics.geocentroid.GeoCentroidAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.GeoDistanceSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.omg.PortableInterceptor.DISCARDING;

import java.io.IOException;

public class IndexedShape {
    public static void main(String[] args) throws IOException {
        useShapeQuery();



    }

    public static SearchResult useShapeQuery() throws IOException {

        RestHighLevelClient client = ESRestClientUtil.getDefaultClient();

        SearchSourceBuilder builder = new SearchSourceBuilder();
        SearchRequest request = new SearchRequest();
            request.indices("example").types("type");

        SearchResult result = new SearchResult();
        String shapeName = "location";
        
       // ShapeBuilder pointBuilder = new PointBuilder(1,2);

       // GeoShapeQueryBuilder shapeQuery  = QueryBuilders.geoShapeQuery(shapeName,pointBuilder );
       // shapeQuery.relation(ShapeRelation.WITHIN);

        GeoShapeQueryBuilder shapeQuery = QueryBuilders.geoShapeQuery(
                "location",                     //预索引的字段
                "bIF3cmYBYp-VlyyN4Pkr",         //预索引的索引id
                "doc");                         //预索引的索引类型


        shapeQuery.relation(ShapeRelation.WITHIN)
                .indexedShapeIndex("test-1")    //预索引形状所在的索引的名称,默认为形状。
                .indexedShapePath("location");  //指定为包含预索引形状的路径的字段。默认为形状。


       // GeoShapeQueryBuilder shapeQuery = QueryBuilders.geoWithinQuery(shapeName, pointBuilder);


        builder.query(shapeQuery);

        request.source(builder);

        long start = System.currentTimeMillis();
        SearchResponse response = client.search(request);

        //从响应中拿到聚合结果
        long end = System.currentTimeMillis();

        int searchTime = (int) (end - start);

        SearchHit[] hits = response.getHits().getHits();

        int totalHits = (int) response.getHits().getTotalHits();
        int tookTime = (int) response.getTook().getMillis();
        int length = response.getHits().getHits().length;


        result.setTotalHits(totalHits);
        result.setSearchTime(searchTime);
        result.setTooK(tookTime);
        result.setResultCount(length);
        result.setThreadSize(0);

        System.out.println(result);
        for (SearchHit hit:hits) {
            System.out.println(hit.getSourceAsString());

        }
        client.close();
        return result;

    }


}



