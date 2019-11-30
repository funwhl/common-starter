package com.eighteen.base.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by wangwei.
 * Date: 2019/11/17
 * Time: 2:47
 */
public class PageCache {
    Map<Integer,Cache> cacheMemory = new HashMap<>();
    Map<Integer, Record> dbData;
    int cm = 50;        //缓存大小
    int bucket ;        //当前缓存页
    int begin ;         //从缓存的第几条开始取
    int n ;             //一次请求多少条
    int p ;             //当前请求第几页
    int x = 2;          //提前

    public static void main(String args[]){
        PageCache pageCache = new PageCache();
        pageCache.dbData = pageCache.initData();
        pageCache.cacheMemory.put(1, pageCache.loadCache(pageCache.cm, 1));
        int total = 1000;
        int pageSize = 6;
        for(int i = 0; i < total/pageSize - 1; i++) {
            System.out.println("get "+ (i+1) +" page :" );
            pageCache.getPage(i + 1, pageSize);
        }
        System.out.println(pageCache.cacheMemory);
    }

    private Map<Integer, Record> initData(){
        Map<Integer, Record> data = new HashMap<Integer, Record>();
        for(int i = 0; i < 1000; i++){
            data.put(i+1, new Record(i+1));
        }
        return data;
    }

    public void getPage(int p, int n){
        Map<Integer, Record> page = new HashMap<Integer, Record>();
        bucket = (p * n) / cm + 1; //求当前取哪页缓存
        begin = ((p -1) * n)  + 1;
        if((p * n) % cm > n || (p * n) % cm == n){  //没跨缓存
            page = getFromCache(bucket, begin, n, page);
        }else {  //跨缓存
            page = getFromCache(bucket - 1, begin, n - (p * n) % cm, page);
            page = getFromCache(bucket, (bucket-1) * cm + 1, (p * n) % cm, page);
        }
        if((p * n) % cm > cm - n * x || (p * n) % cm == cm - n * x){
            System.out.println("load cache");
            cacheMemory.put(bucket + 1, loadCache(cm, bucket + 1));
        }
        System.out.println("page data : " + page);
    }

    /**
     *
     * @param bucket 第几页缓存
     * @param begin 从哪个开始取
     * @return
     */
    private Map<Integer, Record> getFromCache(int bucket, int begin, int n, Map<Integer, Record> page){
        Cache cache = cacheMemory.get(bucket);
        for(int i = 0; i < n; i++){
            Record r = cache.cache.get(begin + i);
            page.put(begin + i, r);
        }
        return page;
    }

    /**
     *
     * @param cm 缓存大小
     * @param bucket 第几页缓存
     * @return
     */
    private Cache loadCache(int cm, int bucket){
        Cache cache = new Cache();
        int deta = cm * (bucket-1) + 1;
        for(int i = 0; i < cm; i++){
            cache.cache.put(deta + i, dbData.get(deta + i));
        }
        return cache;
    }

    class Cache{
        Map<Integer, Record> cache = new HashMap<Integer, Record>();
        public String toString(){
            StringBuffer sb = new StringBuffer();
            for(Map.Entry entry : cache.entrySet()){
                sb.append(entry.getKey() + ":" + entry.getValue() + ",");
            }
            return String.valueOf(sb);
        }
    }

    class Record{
        Object value;
        Record(Object o){
            value = o;
        }
        public String toString(){
            return String.valueOf(value);
        }
    }
}
