package cn.geo;



import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;

import java.util.Arrays;
import java.util.stream.Collectors;

public class ESRestClientUtil {

    final static String defaultSchema = "http";

    private static RestHighLevelClient restHighLevelClient;

    public static RestHighLevelClient createRestClient(ESHttpInfo... esHttpInfos) {

        HttpHost[] hosts = Arrays.stream(esHttpInfos).map(info -> {
            return new HttpHost(info.getIp(), info.getPort(), defaultSchema);
        }).collect(Collectors.toList()).toArray(new HttpHost[] {});

        RestClientBuilder restClientBuilder = RestClient.builder(hosts);
        restClientBuilder.setMaxRetryTimeoutMillis(600000);
        restClientBuilder.setRequestConfigCallback(new RestClientBuilder.RequestConfigCallback() {

            @Override
            public Builder customizeRequestConfig(Builder requestConfigBuilder) {
                requestConfigBuilder.setConnectionRequestTimeout(6000000);
                requestConfigBuilder.setConnectTimeout(5000000).setSocketTimeout(6000000);
                return requestConfigBuilder;
            }
        });

        restClientBuilder.setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
            @Override
            public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                httpClientBuilder
                        .setDefaultIOReactorConfig(IOReactorConfig.custom().setIoThreadCount(Runtime.getRuntime().availableProcessors() * 2).setRcvBufSize(Integer.MAX_VALUE).setSndBufSize(Integer.MAX_VALUE).build());

                return httpClientBuilder;
            }
        });
        return new RestHighLevelClient(restClientBuilder);
    }

    public static synchronized RestHighLevelClient getClient(ESHttpInfo... esHttpInfos) {
        if (restHighLevelClient == null) {
            restHighLevelClient = createRestClient(esHttpInfos);
        }
        return restHighLevelClient;
    }

    public static synchronized RestHighLevelClient   getDefaultClient(){
        //  es 的ip 地址
        String ip = null ; 
		ESHttpInfo esHttpInfo = new ESHttpInfo(ip, 9200);

        return getClient(esHttpInfo);
    }

}
