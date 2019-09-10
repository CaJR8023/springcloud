package com.cajr.springcloud.service.impl;

import com.cajr.springcloud.mapper.NewsMapper;
import com.cajr.springcloud.mapper.NewslogsMapper;
import com.cajr.springcloud.mapper.RecommendationsMapper;
import com.cajr.springcloud.mapper.UsersMapper;
import com.cajr.springcloud.common.content.CustomizedHashMap;
import com.cajr.springcloud.util.JsonUtil;
import com.cajr.springcloud.vo.News;
import com.cajr.springcloud.vo.Newslogs;
import com.cajr.springcloud.vo.Recommendations;
import com.cajr.springcloud.vo.Users;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * TODO 提供推荐算法通用的一些方法
 * @Author CAJR
 * @create 2019/9/4 15:23
 */
@Component
public class RecommendService {

    public static final Logger logger= Logger.getLogger(RecommendService.class);

    @Autowired
    private  UsersMapper usersMapper;

    @Autowired
    private  NewsMapper newsMapper;

    @Autowired
    private  NewslogsMapper newslogsMapper;

    @Autowired
    private  RecommendationsMapper recommendationsMapper;


    /**
     * 推荐新闻的时效性天数，即从推荐当天开始到之前beforeDays天的新闻属于仍具有时效性的新闻，予以推荐。
     */
    @Value("${news.beforeDays}")
    private int beforeDays;

    @Value("${news.activeDays}")
    private int activeDays;

    /**
     *
     * @return 返回时效时间的"year-month-day"的格式表示，方便数据库的查询
     */
    public  String getInRecDate(){
        return getSpecificDayFormat(beforeDays);
    }

    public  Date getInRecDate1(){
        return getSpecificDateFormat(beforeDays);
    }

    /**
     * @return the inRecDate 返回时效时间的"year-month-day"的格式表示，方便数据库的查询
     */
    public  String getInRecDate(int beforeDays)
    {
        return getSpecificDayFormat(beforeDays);
    }

    /**
     * TODO 返回时效时间timestamp形式表示，方便其他推荐方法在比较时间先后时调用
     * @param beforeDays
     * @return
     */
    public static Timestamp getInRecTimestamp(int beforeDays){
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH,beforeDays);
        return new Timestamp(calendar.getTime().getTime());
    }

    /**
     * TODO 过滤掉失去时效的新闻
     * @param
     * @param userId
     */
    public  void filterOutDateNews(List<Long> longIds,Long userId){
        List<News> newsList = newsMapper.findSectionNews(longIds);
        if (!newsList.isEmpty()){
            newsList.forEach((news -> {
                if (news.getNewsTime().before(getInRecTimestamp(beforeDays))){
                    longIds.remove(news.getId());
                }
            }));
        }
    }

    /**
     * TODO 过滤方法filterBrowsedNews() 过滤掉已经用户已经看过的新闻
     * @param col
     * @param userId
     */
    public  void filterBrowsedNews(Collection<Long> col, Long userId){
        List<Newslogs> newsLogs = newslogsMapper.findAllByUserId(userId);
        if (!newsLogs.isEmpty()){
            newsLogs.forEach((newslogs) -> {
                if (col.contains(newslogs.getNewsId())){
                    col.remove(newslogs.getNewsId());
                }
            });
        }
    }

    /**
     * 过滤方法filterRecCedNews() 过滤掉已经推荐过的新闻（在recommend表中查找）
     */
    public  void filterRecCedNews(Collection<Long> col, Long userId) {
        List<Recommendations> recommendations = recommendationsMapper.findAllByUserId(userId);
        List<Recommendations> recommendationsList = new ArrayList<>();
        if (!recommendations.isEmpty()){
            recommendations.forEach(recommendations1 ->{
                if (recommendations1.getDeriveTime().after(getInRecDate1())){
                    recommendationsList.add(recommendations1);
                }
            });
            if (!recommendationsList.isEmpty()){
                recommendationsList.forEach(recommendations1 -> {
                    if (col.contains(recommendations1.getNewsId())){
                        col.remove(recommendations1.getNewsId());
                    }
                });
            }
        }
    }
    /**
     * 获取的userId的list
     * @return
     */
    public  List<Long> getUserList(){
        return usersMapper.findAllId();
    }


    public  String getSpecificDayFormat(int beforeDays)
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        // 得到日历
        Calendar calendar = Calendar.getInstance();
        // 设置为前beforeNum天
        calendar.add(Calendar.DAY_OF_MONTH, beforeDays);
        Date d = calendar.getTime();
        return "'" + dateFormat.format(d) + "'";
    }

    public  Date getSpecificDateFormat(int beforeDays)
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        // 得到日历
        Calendar calendar = Calendar.getInstance();
        // 设置为前beforeNum天
        calendar.add(Calendar.DAY_OF_MONTH, beforeDays);
        Date d = calendar.getTime();
        return d;
    }

//    public static void main(String[] args) {
//        RecommendService recommendService = new RecommendService();
//        String date = recommendService.getSpecificDayFormat(-10);
//        System.out.println(date);
//    }
    /**
     * TODO 获取所有用户的喜好关键词列表
     * @param userSet
     * @return
     */
    public  HashMap<Long, CustomizedHashMap<Integer, CustomizedHashMap<String, Double>>> getUserPrefListMap(Collection<Long> userSet){
        HashMap<Long, CustomizedHashMap<Integer, CustomizedHashMap<String, Double>>> userPrefListMap =new HashMap<>(16);
        List<Long> userIds = new ArrayList<>(userSet);
        List<Users> usersList = usersMapper.findSectionUsers(userIds);
        if (!usersList.isEmpty()){
            for (Users users : usersList) {
                userPrefListMap.put(users.getId(), JsonUtil.jsonPrefListtoMap(users.getPrefList()));
            }
        }
        return userPrefListMap;
    }

    public  void insertRecommend(Long userId, Iterator<Long> newsIte, int recAgo) {
        while (newsIte.hasNext())
        {
            Recommendations rec =new Recommendations();
            rec.setUserId(userId);
            rec.setDeriveAlgorithm(recAgo);
            rec.setNewsId(newsIte.next());
            recommendationsMapper.insertSelective(rec);
        }
    }

    /**
     * TODO 获取所有活跃用户ID
     * @return
     */
    public  List<Long> getActiveUsers(){
        List<Users> usersList = usersMapper.findAll();
        List<Long> activeUsersIdList = new ArrayList<>();
        if (!usersList.isEmpty()){
            usersList.forEach(users -> {
                if (users.getLatestLogTime().after(getInRecTimestamp(activeDays))){
                    activeUsersIdList.add(users.getId());
                }
            });
        }
        return activeUsersIdList;
    }

    /**
     * TODO  去除数量上超过为算法设置的推荐结果上限值的推荐结果
     * @param set
     * @param n
     */
    public void removeOverNews(List<Long> set,int n){
        int i = 0;
        Iterator<Long> iterator = set.iterator();
        while (iterator.hasNext()){
            if (i >= n){
                iterator.remove();
                iterator.next();
            }else {
                iterator.next();
            }
            i++;
        }
    }


}
