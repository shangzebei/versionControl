package com.shang.versioncontrol;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.FORWARD_TO_KEY;

/**
 * Created by shangzebei on 2017/8/21.
 */
@Component
public class VersionFilter extends ZuulFilter {

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    HashMap<String, VersionEntry> globVersions = new HashMap<>();

    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return FilterConstants.PRE_DECORATION_FILTER_ORDER + 1;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        RequestContext currentContext = RequestContext.getCurrentContext();
        HttpServletRequest request = currentContext.getRequest();
        String url = (String) currentContext.get(FORWARD_TO_KEY);
        while (true) {
            VersionEntry versionEntry = globVersions.get(url);
            if (versionEntry == null) {
                url = minusVersion(url);
                if (url == null) {
                    break;
                }
            } else {
                currentContext.set(FORWARD_TO_KEY,url);
                break;
            }
        }
        return null;
    }

    @PostConstruct
    private void initURL() {
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();
        Set<RequestMappingInfo> requestMappingInfos = handlerMethods.keySet();
        for (RequestMappingInfo requestMappingInfo : requestMappingInfos) {
            String x = (String) requestMappingInfo.getPatternsCondition().getPatterns().toArray()[0];
            VersionEntry versionEntry = dealUrl(x);
            Optional.ofNullable(versionEntry).ifPresent(versionEntry1 -> globVersions.put(versionEntry1.getUrl(), versionEntry1));
        }
    }

    protected VersionEntry dealUrl(String url) {
        if (url.matches("/v[\\d]{0,5}.+")) {
            VersionEntry versionEntry = new VersionEntry();
            if (url.startsWith("/")) {
                String[] strings = url.split("/");
                int version = Integer.parseInt(strings[1].substring(1));
                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 1; i < strings.length - 1; i++) {
                    stringBuilder.append("/" + strings[i + 1]);
                }
                versionEntry.setCellUrl(strings);
                versionEntry.setRealUrl(stringBuilder.toString());
                versionEntry.setUrl(url);
                versionEntry.setVersion(version);
                System.out.println("real== " + stringBuilder);
                System.out.println("version== " + version);

            }
            return versionEntry;
        }
        return null;
    }

    /**
     * @param url /v1/hello
     * @return
     */
    protected String minusVersion(String url) {
        VersionEntry versionEntry = dealUrl(url);
        if (versionEntry == null||versionEntry.getVersion()==0) {
            return null;
        }
        return "/v"+(versionEntry.getVersion() - 1) + versionEntry.getRealUrl();
    }

}
