# ES-GeoQuery

<h1> ES 地理位置查询 </h1>
<h3>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;----elasticSearch6.4<h3>
  
 <h3>下面将介绍ES 的geo_point与geo_shape的区别</h3>
 <ul>
<li><h3>geo_point 与geo_shape 的区别</li>
<li><h3>geo_point 的聚合查询</li>
<li><h3>geo_shape 的形状查询</li>
</ul>


********************
********************

### 一. geo_point 与geo_shape 的区别

  ||geo_point|geo_shape|
  |:---|:---|:---|
  |排序|可以|不可以|
  |聚合|可以|不可以|
  |存储空值|可以|可以|
  |存储方式|以(lat,lon)对存储的|以Geojson映射存储的|
  
 说明:1)由于GeoShape是映射GeoJson的,所以两者的本质区别就很明显了<br>
 &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;2) geo_shape 是图形,所以不能做排序,聚合<br>  
 两者本质区别: <br>	
 * #### geo_point 就是 存储 (lat,lon)对
 * #### geo_shape 是将geo_json几何对象映射到geo_shape类型.换句话说,其本质是因为geo_json的属性.
 
 下面就浅谈下[GeoJson](https://tools.ietf.org/html/rfc7946):
 <p>&nbsp;&nbsp;&nbsp;&nbsp;GeoJSON是一种用于编码各种地理数据结构的格式,基于JavaScript的地理空间数据交换格式. 
GeoJSON支持以下几何类型：Point，LineString， Polygon，MultiPoint，MultiLineString，和MultiPolygon。Geojson里的特征包含一个几何对象和其他属性,具有附加属性的几何对象是Feature对象。FeatureCollection对象包含要素集。</p></br>
<p>&nbsp;&nbsp;&nbsp;&nbsp;<b>一个完整的GeoJSON数据结构总是一个对象,GeoJSON总是由一个单独的对象组成。</b>这个对象（指的是下面的GeoJSON对象）表示几何、特征或者特征集合</p>
<ul>
<li>GeoJSON对象可能有任何数目成员（名/值对）。</li></br>
<li><b>GeoJSON对象必须有一个名字为"type"的成员。</b>这个成员的值是由GeoJSON对象的类型所确定的字符串。</li></br>
<li>type成员的值必须是下面之一：“Point”, “MultiPoint”, “LineString”, “MultiLineString”, “Polygon”, “MultiPolygon”, “GeometryCollection”, “Feature”, 或者 "FeatureCollection"。</li></br>
  <li>GeoJSON对象可能有一个可选的"crs"成员，它的值必须是一个坐标参考系统的对象。</li></br>
  <li>GeoJSON对象可能有一个"bbox"成员，它的值必须是边界框数组。</li>
</ul>

*******
#### geo_shape对于所有类型，内部type和coordinates字段都是必需的。
这点与 [GeoJson](https://tools.ietf.org/html/rfc7946) 是类似的.
而geo_point 却不需要存储这么多东西, 只需要存储经纬对(lat ,lon) 即可</br>
这就决定了两者在存储空间上的差异:<b>geo_shape 比geo_point 占的空间大,事实也是如此.</b>

**************
**************
### 二.geo_point 聚合(两种桶聚合,两种指标聚合)
#### 1.桶聚合(Buckets Aggregations) : </br>
* hashGridAgg(通过 Geohashes 生成的网格进行聚合,返回的是网格中心);
```
AggregationBuilder aggregation = AggregationBuilders .geohashGrid("agg")    
                                                     .field("address.location") 
                                                     .precision(4); 

```

* distanceAgg(查找范围内的点)

```
AggregationBuilder aggregation = AggregationBuilders.geoDistance("agg",newGeoPoint(48.8423717,2.3332002))
                                                    .field("address.location") 
                                                    .unit(DistanceUnit.KILOMETERS) 
                                                    .addUnboundedTo(3.0)
                                                    .addRange(3.0, 10.0) 
                                                    .addRange(10.0, 500.0); 


```

#### 2.指标聚合(Metrics Aggregations):</br>
* 	 geoCentroid(地理图形的中心) ;
```
GeoCentroidAggregationBuilder aggregation  = AggregationBuilders.geoCentroid("center").field(field);

```

*  geoBounds(包含点的最小矩形)
  ```
GeoBoundsAggregationBuilder  aggregation =  AggregationBuilders.geoBounds("bounds").field(field);

  ```

 #### 当然指标聚合可以加到桶聚合里面组合使用, 但这样还是有点局限性, 如果我们想自己确定图形并找出其范围内的点的中心 或最小矩形,那么可用行性将大大提高. 然而确实可以这样;
 
eg:找出矩形范围内的点的中心和最小矩形

```
        //rectangle
        GeoBoundingBoxQueryBuilder bounding  = QueryBuilders.geoBoundingBoxQuery(field)
                                                            .setCorners(topLeftPoint,bottomRightPoint);
                                                                              
        //test the GeoBoundsAgg and GeoCentroidAgg
        GeoCentroidAggregationBuilder centroid  = AggregationBuilders.geoCentroid("center").field(field);

        GeoBoundsAggregationBuilder  bounds =  AggregationBuilders.geoBounds("bounds").field(field);
        
        builder.query(bounding);
        builder.aggregation(centroid);
        builder.aggregation(bounds);
```

当然你也可以使用排序(建议与地理图形查询一起使用):
```
  GeoDistanceSortBuilder sortBuilder = SortBuilders.geoDistanceSort(field,1,2)  //(1,2)是排序的中心
                                                   .unit(DistanceUnit.MILES)
                                                   .order(SortOrder.ASC);
  builder.sort(sortBuilder);
```
 ps: 读者可以随意组合.作者已经实现 求几个点的[中心算法](https://github.com/JRhuang-96/ES-GeoQuery/blob/master/geo/GetCenter.java),有兴趣可以自行检验,差别不大,结果受精度影响. 我也将按半径生成点的[算法实现](https://github.com/JRhuang-96/ES-GeoQuery/blob/master/geo/SymPoints.java),但不适合半径很大的.
 
 *****************
 ****************
 ### 三.geo_shape 的形状查询
 

|类型|geo_point|geo_shape|
  |:---|:---|:---|
  |地理位置查询方式|bounding box(矩形)|Point (MultiPoint) 点-多点|
  |聚合|distance(圆)|LineString (MultiLineString) 线-多线|
  ||polygon(多边形)|Polygon(MultiPolygon) 多边形-多个多边形|
  |||GeometryCollection   组合图形|
  |||envelope   矩形|
  |||circle   圆形|
  |选择图形之间关系|不能|能|
  |使用预索引图形|不能|能|
  
说明:
* ####  geo_point 只能查到满足范围内的点,并作相应处理;
* ####  geo_shape 的对象都可以做查询图形;
* ####  geo_shape 只要图形满足选择的图形之间关系,就会把该满足要求的图形全部返回;
* ####  若字段映射为geo_shape ,且字段为geo_shape的一种.若进行图形查询,会将满足的结果集全部返回(只要类型是geo_shape的都会返回);
* ####  在多边形查询中,geo_point 可以不封闭,但geo_shape 必须封闭(即首尾的点必须相同)

### geo_point 查询方式:
```
//rectangle
GeoBoundingBoxQueryBuilder bounding = QueryBuilders.geoBoundingBoxQuery(field)
                                                    .setCorners(topLeftPoint,bottomRightPoint);
                                                    
                                               
//distance
GeoDistanceQueryBuilder distance = QueryBuilders.geoDistanceQuery(field)
                                                .point(center)
                                                .distance(distance, DistanceUnit.KILOMETERS);
                                                
//ploygon
GeoPolygonQueryBuilder geoPolygonQueryBuilder = QueryBuilders.geoPolygonQuery(Field, List<GeoPoint> points);


```


### geo_shape 查询方式:
```
//我就不一一列举了,实现方式都与如下类似,只是 new 不同的GeoShape 的对象,然后传入GeoShapeQueryBuilder() 中
//ploygen
PolygonBuilder  polygonBuilder  = new PolygonBuilder(polyList);

QueryBuilder builder = new GeoShapeQueryBuilder(field,polygonBuilder)
                                .relation(ShapeRelation.DISJOINT);//选择图形之间的关系(四种选择)
                                
                     
...
...
...

```



#### 下面讲下 geo_shape 的使用预索引图形聚合:
##### 使用geoShapeQuery()方法查询
```
List<Coordinate> lineString = new ArrayList<>();
lineString.add(new Coordinate(96.676839,52.444825 ));
lineString.add(new Coordinate(113.398215,21.217703 ));

GeoShapeQueryBuilder qb = geoShapeQuery(field,lineString)                        
                                                                 
qb.relation(ShapeRelation.WITHIN);

// ps: geoShapeQuery() 创建的对象还是传给了GeoShapeQueryBuilder()
```
##### 使用预索引方式查询。先在索引中创建好的图形:
```
SearchRequest request = new SearchRequest();
request.indices("example").types("type"); // 此处声明要查询的索引和类型


GeoShapeQueryBuilder shapeQuery = QueryBuilders.geoShapeQuery(
                "location",                     //预索引的字段
                "bIF3cmYBYp-VlyyN4Pkr",         //预索引的索引id
                "doc");                         //预索引的索引类型
                
        shapeQuery.relation(ShapeRelation.WITHIN) //选择关系
                .indexedShapeIndex("test-1")      //预索引形状所在的索引的名称,默认为shapes。
                .indexedShapePath("location");    //指定为包含预索引形状的路径的字段。默认为shape。

```

****
****
## 总结:
* ### geo_shape 的优势在于地理形状的查询.
* ### 它支持使用已在另一个索引和/或索引类型中已被索引的形状。当你可以预先定义好的形状列表，这样就不用每次必须提供它的坐标,这是geo_point做不到的.
* ### 若你想使用point 类型,并想使用geo_shape 的特性去查点,那么使用 geo_shape,然后设置 points_only : true 来提高使用”点”性能, 具体可在官网找到[此项配置](https://www.elastic.co/guide/en/elasticsearch/reference/current/geo-shape.html):





