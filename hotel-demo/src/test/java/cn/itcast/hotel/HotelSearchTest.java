package cn.itcast.hotel;

import cn.itcast.hotel.pojo.HotelDoc;
import com.alibaba.fastjson.JSON;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.CollectionUtils;

import javax.xml.ws.Holder;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class HotelSearchTest {
    private RestHighLevelClient client;

    @BeforeEach
    void setup() {
        this.client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://43.139.46.197:9200")
        ));
    }

    @AfterEach
    void tearDown() throws IOException {
        this.client.close();
    }

    @Test
    void testMatchAll() throws IOException {
        //准备request
        SearchRequest request = new SearchRequest("hotel");
        //准备DSL
        request.source().query(QueryBuilders.matchAllQuery());
        //发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //解析结果
        handleResponse(response);
    }

    @Test
    void testMatch() throws IOException {
        //准备request
        SearchRequest request = new SearchRequest("hotel");
        //准备DSL
        request.source().query(QueryBuilders.matchQuery("all", "如家"));
        //发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //解析结果
        handleResponse(response);
    }

    @Test
    void testBool() throws IOException {
        //准备request
        SearchRequest request = new SearchRequest("hotel");
        //准备DSL
        /*准备boolQuery*/
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        /*添加term*/
        boolQuery.must(QueryBuilders.termQuery("city", "上海"));
        /*添加range*/
        boolQuery.filter(QueryBuilders.rangeQuery("price").lte(400));
        request.source().query(boolQuery);
        //发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //解析结果
        handleResponse(response);
    }

    @Test
    void testPageAndSort() throws IOException {
        int page = 1;
        int pageSize = 5;
        //准备request
        SearchRequest request = new SearchRequest("hotel");
        //准备DSL
        /*query*/
        request.source().query(QueryBuilders.matchQuery("all", "如家"));
        /*sort*/
        request.source().sort("price", SortOrder.ASC);
        /*page*/
        request.source().from((page - 1) * pageSize).size(pageSize);
        //发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //解析结果
        handleResponse(response);
    }

    @Test
    void testHighLight() throws IOException {
        //准备request
        SearchRequest request = new SearchRequest("hotel");
        //准备DSL
        request.source().query(QueryBuilders.matchQuery("all", "如家"));
        /*highLight*/
        request.source().highlighter(new HighlightBuilder().field("name").requireFieldMatch(false));
        //发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //解析结果
        handleResponse(response);
    }

    @Test
    void testAggregation() throws IOException {
        SearchRequest request = new SearchRequest("hotel");
        request.source().size(0);
        request.source().aggregation(AggregationBuilders
                .terms("brandAgg")
                .field("brand")
                .size(10)
        );
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        Aggregations aggregations = response.getAggregations();
        Terms terms = aggregations.get("brandAgg");
        List<? extends Terms.Bucket> buckets = terms.getBuckets();
        for (Terms.Bucket bucket : buckets) {
            String brandName = bucket.getKeyAsString();
            System.out.println(brandName);
        }
    }

    @Test
    void testSuggest() throws IOException {
        SearchRequest request = new SearchRequest("hotel");
        request.source().suggest(new SuggestBuilder().addSuggestion(
                "suggestions",
                SuggestBuilders.completionSuggestion("suggestion")
                        .prefix("wh")
                        .skipDuplicates(true)
                        .size(10)
        ));
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        Suggest suggest = response.getSuggest();
        CompletionSuggestion suggestions = suggest.getSuggestion("suggestions");
        List<CompletionSuggestion.Entry.Option> options = suggestions.getOptions();
        for (CompletionSuggestion.Entry.Option option : options) {
            String text = option.getText().toString();
            System.out.println(text);
        }
    }

    /**
     * 解析结果代码抽取
     *
     * @param response
     */
    private void handleResponse(SearchResponse response) {
        SearchHits searchHits = response.getHits();
        long total = searchHits.getTotalHits().value;
        System.out.println("共搜索到" + total + "条数据!");
        SearchHit[] hits = searchHits.getHits();
        for (SearchHit hit : hits) {
            String json = hit.getSourceAsString();
            HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
            /*获取高亮结果*/
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            if (!CollectionUtils.isEmpty(highlightFields)) {
                //根据字段获取高亮结果
                HighlightField highlightField = highlightFields.get("name");
                if (highlightField != null) {
                    //获取高亮值
                    String name = highlightField.getFragments()[0].string();
                    //覆盖非高亮值
                    hotelDoc.setName(name);
                }
            }
            System.out.println(hotelDoc);
        }
    }
}
