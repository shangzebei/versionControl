package com.shang.versioncontrol;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
