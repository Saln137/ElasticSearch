package cn.itcast.hotel;

import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

@SpringBootTest
public class HotelDocumentTest {

    @Autowired
    private IHotelService iHotelService;

    private RestHighLevelClient client;

    @BeforeEach
    void setup(){
        this.client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://43.139.46.197:9200")
        ));
    }

    @AfterEach
    void tearDown() throws IOException {
        this.client.close();
    }

    @Test
    void testAddDocument() throws IOException {
        //根据id查询到酒店数据
        Hotel hotel = iHotelService.getById(36934L);
        //装换为文档类型
        HotelDoc hotelDoc = new HotelDoc(hotel);
        //request对象
        IndexRequest request = new IndexRequest("hotel").id(hotelDoc.getId().toString());
        //json文档
        request.source(JSON.toJSONString(hotelDoc), XContentType.JSON);
        //发送请求
        client.index(request, RequestOptions.DEFAULT);
    }

    @Test
    void testGetDocument() throws IOException {
        GetRequest request = new GetRequest("hotel","36934");
        GetResponse response = client.get(request, RequestOptions.DEFAULT);
        //解析查询到的结果
        String json = response.getSourceAsString();
        HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
        System.out.println(hotelDoc);
    }

    @Test
    void testUpdateDocument() throws IOException {
        // 1.准备Request
        UpdateRequest request = new UpdateRequest("hotel", "36934");
        // 2.准备请求参数
        request.doc(
                "price", "952",
                "starName", "四钻"
        );
        // 3.发送请求
        client.update(request, RequestOptions.DEFAULT);
    }

    @Test
    void testDeleteDocument() throws IOException {
        // 1.准备Request
        DeleteRequest request = new DeleteRequest("hotel", "36934");
        // 2.发送请求
        client.delete(request, RequestOptions.DEFAULT);
    }

    /**
     * MySQL数据批量导入到ES文档
     */
    @Test
    void testBulkRequest() throws IOException {
        BulkRequest request = new BulkRequest();
        /*查询所有数据*/
        List<Hotel> hotels = iHotelService.list();
        /*使用stream流将hotel转化成hotel Doc对象*/
        hotels.stream().forEach(hotel -> {
            HotelDoc hotelDoc = new HotelDoc(hotel);
            request.add(new IndexRequest("hotel")
                    .id(hotelDoc.getId().toString())
                    .source(JSON.toJSONString(hotelDoc),XContentType.JSON));
        });
        /*批量添加*/
        client.bulk(request,RequestOptions.DEFAULT);
    }
}
