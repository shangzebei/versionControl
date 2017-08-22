package com.shang.versioncontrol;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Map;

/**
 * Created by shangzebei on 2017/8/21.
 */
@RestController
public class HelloWord {
    @GetMapping("v1/hello")
    public String hellov1() {
        return "hello v1";
    }

    @GetMapping("v3/hello")
    public String hellov3() {
        return "hello v3";
    }

    @GetMapping("hello-v4")
    public String hellov4() {
        return "hello v4";
    }

    private final RequestMappingHandlerMapping handlerMapping;

    @Autowired
    public HelloWord(RequestMappingHandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
    }

    @GetMapping("doc")
    public Map<RequestMappingInfo, HandlerMethod> doc() {
       return handlerMapping.getHandlerMethods();
    }


}
