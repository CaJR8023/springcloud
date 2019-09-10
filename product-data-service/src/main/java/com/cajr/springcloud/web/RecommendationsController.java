package com.cajr.springcloud.web;

import com.cajr.springcloud.service.RecommendationsService;
import com.cajr.springcloud.vo.Recommendations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Author CAJR
 * @create 2019/9/10 16:03
 */
@RestController
@RequestMapping("/recommendations")
public class RecommendationsController {

    @Resource
    RecommendationsService recommendationsService;

    @GetMapping("/")
    public List<Recommendations> list(@RequestParam("userId") Long userId){
        return this.recommendationsService.findAllByUserId(userId);
    }
}
