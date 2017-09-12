package com.shang.versioncontrol;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.FORWARD_TO_KEY;

/**
 * /v2/hello ---> /v1/hello
 * Created by shangzebei on 2017/8/21.
 */
@Configuration
@EnableZuulProxy
public class VersionControlConfig {
    @Bean
    VersionFilter versionFilter() {
        return new VersionFilter();
    }


    private static class VersionEntry {
        private int version;
        private String url;//v1/hello
        private String realUrl;///hello
        private String[] cellUrl;

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getRealUrl() {
            return realUrl;
        }

        public void setRealUrl(String realUrl) {
            this.realUrl = realUrl;
        }

        public String[] getCellUrl() {
            return cellUrl;
        }

        public void setCellUrl(String[] cellUrl) {
            this.cellUrl = cellUrl;
        }
    }

    private interface VersionCache {
        /**
         * get urlMapping from versionCache
         *
         * @param key
         * @return
         */
        String getCache(String key);

        /**
         * set url to versionCache
         *
         * @param key
         * @param value
         */
        void setCache(String key, String value);
    }

    private static class VersionFilter extends ZuulFilter {
        @Autowired
        private RequestMappingHandlerMapping handlerMapping;

        @Autowired
        private Environment environment;

        @Autowired(required = false)
        private VersionCache versionCache;

        private String defPath = "";

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
            String relUrl = getRelUrl(currentContext);
            String _relUrl = relUrl;//start
            if (versionCache != null) {
                String cacheUrl = this.versionCache.getCache(relUrl);
                if (cacheUrl != null) {//find
                    setDesUrl(currentContext, cacheUrl);
                    return null;
                }
            }
            while (true) {
                VersionEntry versionEntry = globVersions.get(relUrl);
                if (versionEntry == null) {
                    relUrl = minusVersion(relUrl);
                    if (relUrl == null) {
                        break;
                    }
                } else {
                    if (versionCache != null) {
                        versionCache.setCache(_relUrl, relUrl);
                    }
                    setDesUrl(currentContext, relUrl);
                    break;
                }
            }

            return null;
        }

        private String getRelUrl(RequestContext currentContext) {
            String url = (String) currentContext.get(FORWARD_TO_KEY);
            if (!defPath.equals("")) {
                url = url.replaceFirst(defPath, "");
            }
            return url;
        }


        private void setDesUrl(RequestContext currentContext, String url) {
            if (!defPath.equals("")) {
                currentContext.set(FORWARD_TO_KEY, defPath + url);
            } else {
                currentContext.set(FORWARD_TO_KEY, url);
            }
        }

        @PostConstruct
        private void initURL() {
            String environmentProperty = environment.getProperty("server.servlet-path");
            if (environmentProperty != null) {
                defPath = environmentProperty;
            }
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
                        stringBuilder.append("/");
                        stringBuilder.append(strings[i + 1]);
                    }
                    versionEntry.setCellUrl(strings);
                    versionEntry.setRealUrl(stringBuilder.toString());
                    versionEntry.setUrl(url);
                    versionEntry.setVersion(version);
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
            if (versionEntry == null || versionEntry.getVersion() == 0) {
                return null;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("/v");
            stringBuilder.append(versionEntry.getVersion() - 1);
            stringBuilder.append(versionEntry.getRealUrl());
            return stringBuilder.toString();
        }
    }


}
