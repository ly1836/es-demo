package com.ly.learn.esdemo.repositories;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ly.learn.esdemo.models.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;

import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.ml.*;
import org.elasticsearch.client.ml.datafeed.ChunkingConfig;
import org.elasticsearch.client.ml.datafeed.DatafeedConfig;
import org.elasticsearch.client.ml.datafeed.DatafeedUpdate;
import org.elasticsearch.client.ml.job.process.DataCounts;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * elasticSearh 操作类
 *
 * @author Stephen
 * @version 1.0
 * @date 2018/09/17 11:12:08
 */
@Slf4j
@Component
public class ElasticsearchUtils {

    /*@Autowired
    private TransportClient transportClient;*/

    private static RestClient client;

    private static RestHighLevelClient highLevelClient;

    @Autowired
    public ElasticsearchUtils(@Qualifier("elasticsearchClient") RestClient client, @Qualifier("getRestHighLevelClient") RestHighLevelClient highLevelClient) {
        this.client = client;
        this.highLevelClient = highLevelClient;
    }

    public ElasticsearchUtils() {
    }

    /**
     * 创建索引
     *
     * @param index
     * @return
     */
    public static boolean createIndex(String index, String id) throws IOException {
        if (!isIndexExist(index, id)) {
            log.info("Index is not exits!");
        }
        IndexRequest request = new IndexRequest("data");
        request.timeout(TimeValue.timeValueMinutes(3));

        File file = new File(ElasticsearchUtils.class.getResource("/es-mapping/user-mappering.json").getPath());
        String file1 = FileUtils.readFileToString(file);
        JSONObject jsonobject = JSON.parseObject(file1);
        log.info(jsonobject.toJSONString());
        request.source(jsonobject.toJSONString(), XContentType.JSON);


        IndexResponse createIndexResponse = highLevelClient.index(request, RequestOptions.DEFAULT);
        log.info("执行建立成功？:{}", createIndexResponse.getResult().getLowercase());

        return true;
    }

    /**
     * 删除索引
     *
     * @param index
     * @return
     */
    public static boolean deleteIndex(String index,String id) throws IOException {
        if (!isIndexExist(index, id)) {
            log.info("Index is not exits!");
        }
        DeleteIndexRequest request = new DeleteIndexRequest("posts");
        request.timeout(TimeValue.timeValueMinutes(2));
        request.timeout("2m");
        request.indices(index);

        AcknowledgedResponse deleteIndexResponse = highLevelClient.indices().delete(request, RequestOptions.DEFAULT);

        if (deleteIndexResponse.isAcknowledged()) {
            log.info("delete index [{}]  successfully!", index);
        } else {
            log.info("Fail to delete index [{}]", index);
        }

        return deleteIndexResponse.isAcknowledged();
    }

    /**
     * 判断索引是否存在
     *
     * @param index
     * @return
     */
    public static boolean isIndexExist(String index,String id) throws IOException {

        GetRequest getRequest = new GetRequest(index, id);
        boolean exists = highLevelClient.exists(getRequest, RequestOptions.DEFAULT);
        if (exists) {
            log.info("Index [{}] is exist!", index);
        } else {
            log.info("Index [{}] is not exist!", index);
        }
        return exists;
    }

    /**
     * 数据添加,指定ID
     *
     * @param user  要增加的数据
     * @param index 索引，类似数据库
     * @param type  类型，类似表
     * @param id    数据ID
     * @return
     */
    public static String updateOrAddData(User user, String index, String type, String id) throws IOException {
        String key = index + "/" + type;
        UpdateRequest request = new UpdateRequest(key, id);

        request.upsert(JSONObject.toJSONString(user), XContentType.JSON);
        request.timeout(TimeValue.timeValueSeconds(1));
        request.timeout("1s");
        request.scriptedUpsert(true);

        UpdateResponse updateResponse = highLevelClient.update(request, RequestOptions.DEFAULT);

        String responseIndex = updateResponse.getIndex();
        String responseId = updateResponse.getId();
        long version = updateResponse.getVersion();
        if (updateResponse.getResult() == DocWriteResponse.Result.CREATED) {
            log.info("index [{}] create!", responseIndex);
        } else if (updateResponse.getResult() == DocWriteResponse.Result.UPDATED) {
            log.info("index [{}] update!", responseIndex);
        }

        return responseId;
    }


    /**
     * 通过ID删除数据
     *
     * @param index 索引，类似数据库
     * @param type  类型，类似表
     * @param id    数据ID
     */
    public static boolean deleteDataById(String index, String type, String id) throws IOException {
        String key = index + "/" + type;

        DeleteRequest request = new DeleteRequest(key, id);
        request.timeout(TimeValue.timeValueMinutes(2));
        request.timeout("2m");

        DeleteResponse deleteResponse = highLevelClient.delete(request, RequestOptions.DEFAULT);
        if (deleteResponse.getResult() == DocWriteResponse.Result.NOT_FOUND) {
            log.info("index not found : [{}]", key + "/" + id);
            return false;
        }
        log.info("deleteDataById response status:[{}]", deleteResponse.getResult().getLowercase());
        return true;
    }


    /**
     * 通过ID获取数据
     *
     * @param index  索引，类似数据库
     * @param type   类型，类似表
     * @param id     数据ID
     * @param fields 需要显示的字段，逗号分隔（缺省为全部字段）
     * @return
     */
    public static Map<String, Object> searchDataById(String index, String type, String id, String fields) throws IOException {
        String key = index + "/" + type;
        GetRequest getRequest = new GetRequest(key, id);
        getRequest.versionType(VersionType.EXTERNAL);
        getRequest.realtime(false);

        String[] includes = new String[]{"message", "*" + fields};
        String[] excludes = Strings.EMPTY_ARRAY;
        FetchSourceContext fetchSourceContext =
                new FetchSourceContext(true, includes, excludes);
        getRequest.fetchSourceContext(fetchSourceContext);

        GetResponse getResponse = highLevelClient.get(getRequest, RequestOptions.DEFAULT);
        if (getResponse.isExists()) {
            return getResponse.getSourceAsMap();
        } else {
            log.info("data not found index[{}]", index + "/" + id);
        }
        return null;
    }


    /**
     * 使用分词查询
     *
     * @param index    索引名称
     * @param type     类型名称,可传入多个type逗号分隔
     * @param fields   需要显示的字段，逗号分隔（缺省为全部字段）
     * @param matchStr 过滤条件（xxx=111,aaa=222）
     * @return
     */
    /*public static List<Map<String, Object>> searchListData(String index, String type, String fields, String matchStr) {
        return searchListData(index, type, 0, 0, null, fields, null, false, null, matchStr);
    }

    *//**
     * 使用分词查询
     *
     * @param index       索引名称
     * @param type        类型名称,可传入多个type逗号分隔
     * @param fields      需要显示的字段，逗号分隔（缺省为全部字段）
     * @param sortField   排序字段
     * @param matchPhrase true 使用，短语精准匹配
     * @param matchStr    过滤条件（xxx=111,aaa=222）
     * @return
     *//*
    public static List<Map<String, Object>> searchListData(String index, String type, String fields, String sortField, boolean matchPhrase, String matchStr) {
        return searchListData(index, type, 0, 0, null, fields, sortField, matchPhrase, null, matchStr);
    }


    *//**
     * 使用分词查询
     *
     * @param index          索引名称
     * @param type           类型名称,可传入多个type逗号分隔
     * @param size           文档大小限制
     * @param fields         需要显示的字段，逗号分隔（缺省为全部字段）
     * @param sortField      排序字段
     * @param matchPhrase    true 使用，短语精准匹配
     * @param highlightField 高亮字段
     * @param matchStr       过滤条件（xxx=111,aaa=222）
     * @return
     *//*
    public static List<Map<String, Object>> searchListData(String index, String type, Integer size, String fields, String sortField, boolean matchPhrase, String highlightField, String matchStr) {
        return searchListData(index, type, 0, 0, size, fields, sortField, matchPhrase, highlightField, matchStr);
    }


    *//**
     * 使用分词查询
     *
     * @param index          索引名称
     * @param type           类型名称,可传入多个type逗号分隔
     * @param startTime      开始时间
     * @param endTime        结束时间
     * @param size           文档大小限制
     * @param fields         需要显示的字段，逗号分隔（缺省为全部字段）
     * @param sortField      排序字段
     * @param matchPhrase    true 使用，短语精准匹配
     * @param highlightField 高亮字段
     * @param matchStr       过滤条件（xxx=111,aaa=222）
     * @return
     *//*
    public static List<Map<String, Object>> searchListData(String index, String type, long startTime, long endTime, Integer size, String fields, String sortField, boolean matchPhrase, String highlightField, String matchStr) {

        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(index);
        if (StringUtils.isNotEmpty(type)) {
            searchRequestBuilder.setTypes(type.split(","));
        }
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        if (startTime > 0 && endTime > 0) {
            boolQuery.must(QueryBuilders.rangeQuery("processTime")
                    .format("epoch_millis")
                    .from(startTime)
                    .to(endTime)
                    .includeLower(true)
                    .includeUpper(true));
        }

        //搜索的的字段
        if (StringUtils.isNotEmpty(matchStr)) {
            for (String s : matchStr.split(",")) {
                String[] ss = s.split("=");
                if (ss.length > 1) {
                    if (matchPhrase == Boolean.TRUE) {
                        boolQuery.must(QueryBuilders.matchPhraseQuery(s.split("=")[0], s.split("=")[1]));
                    } else {
                        boolQuery.must(QueryBuilders.matchQuery(s.split("=")[0], s.split("=")[1]));
                    }
                }

            }
        }

        // 高亮（xxx=111,aaa=222）
        if (StringUtils.isNotEmpty(highlightField)) {
            HighlightBuilder highlightBuilder = new HighlightBuilder();

            //highlightBuilder.preTags("<span style='color:red' >");//设置前缀
            //highlightBuilder.postTags("</span>");//设置后缀

            // 设置高亮字段
            highlightBuilder.field(highlightField);
            searchRequestBuilder.highlighter(highlightBuilder);
        }


        searchRequestBuilder.setQuery(boolQuery);

        if (StringUtils.isNotEmpty(fields)) {
            searchRequestBuilder.setFetchSource(fields.split(","), null);
        }
        searchRequestBuilder.setFetchSource(true);

        if (StringUtils.isNotEmpty(sortField)) {
            searchRequestBuilder.addSort(sortField, SortOrder.DESC);
        }

        if (size != null && size > 0) {
            searchRequestBuilder.setSize(size);
        }

        //打印的内容 可以在 Elasticsearch head 和 Kibana  上执行查询
        log.info("\n{}", searchRequestBuilder);

        SearchResponse searchResponse = searchRequestBuilder.execute().actionGet();

        long totalHits = searchResponse.getHits().totalHits;
        long length = searchResponse.getHits().getHits().length;

        log.info("共查询到[{}]条数据,处理数据条数[{}]", totalHits, length);

        if (searchResponse.status().getStatus() == 200) {
            // 解析对象
            return setSearchResponse(searchResponse, highlightField);
        }

        return null;

    }

    *//**
     * 使用分词查询,并分页
     *
     * @param index          索引名称
     * @param type           类型名称,可传入多个type逗号分隔
     * @param currentPage    当前页
     * @param pageSize       每页显示条数
     * @param startTime      开始时间
     * @param endTime        结束时间
     * @param fields         需要显示的字段，逗号分隔（缺省为全部字段）
     * @param sortField      排序字段
     * @param matchPhrase    true 使用，短语精准匹配
     * @param highlightField 高亮字段
     * @param matchStr       过滤条件（xxx=111,aaa=222）
     * @return
     *//*
    public static EsPage searchDataPage(String index, String type, int currentPage, int pageSize, long startTime, long endTime, String fields, String sortField, boolean matchPhrase, String highlightField, String matchStr) {
        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(index);
        if (StringUtils.isNotEmpty(type)) {
            searchRequestBuilder.setTypes(type.split(","));
        }
        searchRequestBuilder.setSearchType(SearchType.QUERY_THEN_FETCH);

        // 需要显示的字段，逗号分隔（缺省为全部字段）
        if (StringUtils.isNotEmpty(fields)) {
            searchRequestBuilder.setFetchSource(fields.split(","), null);
        }

        //排序字段
        if (StringUtils.isNotEmpty(sortField)) {
            searchRequestBuilder.addSort(sortField, SortOrder.DESC);
        }

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        if (startTime > 0 && endTime > 0) {
            boolQuery.must(QueryBuilders.rangeQuery("processTime")
                    .format("epoch_millis")
                    .from(startTime)
                    .to(endTime)
                    .includeLower(true)
                    .includeUpper(true));
        }

        // 查询字段
        if (StringUtils.isNotEmpty(matchStr)) {
            for (String s : matchStr.split(",")) {
                String[] ss = s.split("=");
                if (matchPhrase == Boolean.TRUE) {
                    boolQuery.must(QueryBuilders.matchPhraseQuery(s.split("=")[0], s.split("=")[1]));
                } else {
                    boolQuery.must(QueryBuilders.matchQuery(s.split("=")[0], s.split("=")[1]));
                }
            }
        }

        // 高亮（xxx=111,aaa=222）
        if (StringUtils.isNotEmpty(highlightField)) {
            HighlightBuilder highlightBuilder = new HighlightBuilder();

            //highlightBuilder.preTags("<span style='color:red' >");//设置前缀
            //highlightBuilder.postTags("</span>");//设置后缀

            // 设置高亮字段
            highlightBuilder.field(highlightField);
            searchRequestBuilder.highlighter(highlightBuilder);
        }

        searchRequestBuilder.setQuery(QueryBuilders.matchAllQuery());
        searchRequestBuilder.setQuery(boolQuery);

        // 分页应用
        searchRequestBuilder.setFrom(currentPage).setSize(pageSize);

        // 设置是否按查询匹配度排序
        searchRequestBuilder.setExplain(true);

        //打印的内容 可以在 Elasticsearch head 和 Kibana  上执行查询
        log.info("\n{}", searchRequestBuilder);

        // 执行搜索,返回搜索响应信息
        SearchResponse searchResponse = searchRequestBuilder.execute().actionGet();

        long totalHits = searchResponse.getHits().totalHits;
        long length = searchResponse.getHits().getHits().length;

        log.debug("共查询到[{}]条数据,处理数据条数[{}]", totalHits, length);

        if (searchResponse.status().getStatus() == 200) {
            // 解析对象
            List<Map<String, Object>> sourceList = setSearchResponse(searchResponse, highlightField);

            return new EsPage(currentPage, pageSize, (int) totalHits, sourceList);
        }

        return null;

    }

    *//**
     * 高亮结果集 特殊处理
     *
     * @param searchResponse
     * @param highlightField
     *//*
    private static List<Map<String, Object>> setSearchResponse(SearchResponse searchResponse, String highlightField) {
        List<Map<String, Object>> sourceList = new ArrayList<Map<String, Object>>();
        StringBuffer stringBuffer = new StringBuffer();

        for (SearchHit searchHit : searchResponse.getHits().getHits()) {
            searchHit.getSource().put("id", searchHit.getId());

            if (StringUtils.isNotEmpty(highlightField)) {

                System.out.println("遍历 高亮结果集，覆盖 正常结果集" + searchHit.getSource());
                Text[] text = searchHit.getHighlightFields().get(highlightField).getFragments();

                if (text != null) {
                    for (Text str : text) {
                        stringBuffer.append(str.string());
                    }
                    //遍历 高亮结果集，覆盖 正常结果集
                    searchHit.getSource().put(highlightField, stringBuffer.toString());
                }
            }
            sourceList.add(searchHit.getSource());
        }

        return sourceList;
    }*/

}
