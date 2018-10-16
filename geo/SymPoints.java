package cn.geo;

import com.alibaba.fastjson.JSONObject;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static java.math.BigDecimal.ROUND_HALF_UP;

public class SymPoints {

    public static void main(String[] args) throws IOException {
        //load 五对数据进集群
        circle(5);
    }



    /**
     *将生成的对称点 加载到es 集群中
     * @param pairs : 多少对 对称点
     * @throws IOException
     */
    private static void circle(int pairs) throws IOException {
        RestHighLevelClient client  = ESRestClientUtil.getDefaultClient();
        BulkRequest request = new BulkRequest();

        List<GeoPoint> pointList  = new ArrayList<>();

        for(int i= 0 ;i<pairs;i++) {
            List<GeoPoint> points = circlePoint(1,1);

            for (GeoPoint p : points) {
                pointList.add(p);
                System.out.println(p);
            }
        }

        System.out.println("------");
        int count =0;
        for (GeoPoint geoPoint : pointList){
            for (int i = 0; i<pointList.size();i++) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("location", geoPoint);
                addIndex(request, jsonObject);
                count++;

                if (count%50==0){
                    client.bulk(request);
                    request = new BulkRequest();
                    count=0;
                }
            }
        }
        if (count!=0){
            client.bulk(request);

        }

        client.close();

    }


    public static void addIndex(BulkRequest request,JSONObject jsonObject) throws IOException {
        IndexRequest indexRequest = new IndexRequest();
        request.indices("example").types("type");

        indexRequest.source(jsonObject, XContentType.JSON);
        request.add(indexRequest);

    }

    /**
     * 按半径生成点
     * 没有考率 经纬度带来的影响,适合小范围的生成点
     * @param lon
     * @param lat
     * @return
     */
    private static List<GeoPoint> circlePoint(double lon , double lat){
        List<GeoPoint> list =new ArrayList<>();
        double x,y;

        double radius = 0.001;

        double angle = Math.random()*(360);

        double otherAngle = angle +180;

        x = lon +  radius * Math.cos(angle * Math.PI/ 180);
        y = lat +radius * Math.sin(angle *Math.PI /180);

        list.add(new GeoPoint(new BigDecimal(y).setScale(5,ROUND_HALF_UP).doubleValue(),
                new BigDecimal(x).setScale(5,ROUND_HALF_UP).doubleValue() ));


        x = lon +  radius * Math.cos(otherAngle * Math.PI/ 180);
        y = lat +radius * Math.sin(otherAngle *Math.PI /180);

        list.add(new GeoPoint(new BigDecimal(y).setScale(6,ROUND_HALF_UP).doubleValue(),
                new BigDecimal(x).setScale(6,ROUND_HALF_UP).doubleValue() ));

        return list;




    }
}
