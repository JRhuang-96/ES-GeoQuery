/*

我没有实现 求Coordinate 的中心,因为geo_shape 没有聚合,若有需求,请进行相应的修改

we didn't write  the methods to get the center of Coordiante ,bacause of the geo_shape doesn't have aggregations 

*/
package cn.geo;

import com.vividsolutions.jts.geom.Coordinate;
import org.elasticsearch.common.geo.GeoPoint;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class GetCenter {
    public static void main(String[] args) {
        List<GeoPoint> points = new ArrayList<>();
        
        points.add(new GeoPoint(10.0,0.0));
        points.add(new GeoPoint(10,10));

        GeoPoint point = getPointCenter(points);
        System.out.println(point);

        point = getCenterPoint4(points);
        System.out.println("center: "+point);
    }


    /**
     * fit GeoPoint and the distance more than 400km ,return the center of List<GeoPoint>,
     * @param list
     * @return
     */
    public static GeoPoint getPointCenter(List<GeoPoint> list )  {
        if (list ==null || list.size() == 0){
            System.out.println("输入有误: ");
//            throw  new Exception("the list is empty or not defined ");
            return null;
        }
        if (list.size() ==1){return list.get(0);}

        double X = 0, Y = 0, Z = 0;
        double lat , lon, hyp;
        double x,y,z;

        for (GeoPoint point :list){

            lon = point.getLon()* Math.PI/180;
            lat = point.getLat() * Math.PI/180;

            x=Math.cos(lat)*Math.cos(lon);
            y=Math.cos(lat)*Math.sin(lon);
            z = Math.sin(lat);

            X +=x;
            Y +=y;
            Z +=z;
        }

        X = X/list.size();
        Y = Y/list.size();
        Z = Z/list.size();

        lon = Math.atan2(Y, X);
        hyp = Math.sqrt(X*X + Y*Y);
        lat = Math.atan2(Z,hyp );

        lon = new BigDecimal(lon*180/Math.PI).setScale(6, BigDecimal.ROUND_HALF_UP).doubleValue();

        lat = new BigDecimal(lat * 180 /Math.PI).setScale(6, BigDecimal.ROUND_HALF_UP).doubleValue();


        return new GeoPoint(lat,lon);


    }

    /**
     fit GeoPoint and the distance is less than 400km. return the center of List<GeoPoint>
     * */
    public static GeoPoint getCenterPoint4(List<GeoPoint> points) {

        if(points == null || points.size() == 0){
            System.out.println("the list is empty or not defined ");
//            throw new Exception("the list is empty or not defined ");
            return null;
        }
        if (points.size()==1){ return points.get(0); }

        int total = points.size();

        double lat = 0, lon = 0;
        for (GeoPoint g : points) {
            lat += g.lat() * Math.PI / 180;
            lon += g.lon() * Math.PI / 180;
        }
        lat /= total; lon /= total;

        return new GeoPoint(lat * 180 / Math.PI, lon * 180 / Math.PI); }

}
