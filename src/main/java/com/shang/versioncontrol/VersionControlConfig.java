package com.shang.versioncontrol;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.http.ZuulServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.cloud.netflix.zuul.EnableZuulServer;
import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.cloud.netflix.zuul.util.RequestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.UrlPathHelper;

import javax.annotation.PostConstruct;
import javax.servlet.MultipartConfigElement;
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
@EnableZuulServer
public class VersionControlConfig {
    @Bean
    VersionFilter versionFilter(RouteLocator routeLocator, ZuulProperties zuulProperties) {
        return new VersionFilter(zuulProperties, routeLocator);
    }

    @Bean
    public ServletRegistrationBean zuulServlet(ZuulProperties zuulProperties) {
        ServletRegistrationBean servlet = new ServletRegistrationBean(new ZuulServlet(),
                zuulProperties.getServletPattern());
        MultipartConfigElement multipartConfig = new MultipartConfigElement("",52428800,52428800,0);
        servlet.setMultipartConfig(multipartConfig);
        servlet.addInitParameter("buffer-requests", "false");
        return servlet;
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

    private static class VersionFilter extends ZuulFilter {
        @Autowired
        private RequestMappingHandlerMapping handlerMapping;

        @Autowired
        private Environment environment;

        private String defPath = "";

        private ZuulProperties properties;
        private RouteLocator routeLocator;

        public VersionFilter(ZuulProperties properties, RouteLocator routeLocator) {
            this.properties = properties;
            this.routeLocator = routeLocator;
        }

        private UrlPathHelper urlPathHelper = new UrlPathHelper();


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
            ////////////////
            RequestContext ctx = RequestContext.getCurrentContext();
            final String requestURI = this.urlPathHelper.getPathWithinApplication(ctx.getRequest());
            Route route = this.routeLocator.getMatchingRoute(requestURI);
            String fallBackUri = requestURI;

            if (RequestUtils.isZuulServletRequest()) {
                fallBackUri = fallBackUri.replaceFirst(this.properties.getServletPath(), "");
            } else {
                fallBackUri = fallBackUri.replaceFirst(this.defPath, "");
            }
            if (!fallBackUri.startsWith("/")) {
                fallBackUri = "/" + fallBackUri;
            }
            String forwardURI = defPath + fallBackUri;
            forwardURI = forwardURI.replaceAll("//", "/");
            ctx.set(FORWARD_TO_KEY, forwardURI);

            ////////////////
            RequestContext currentContext = RequestContext.getCurrentContext();
            String url = (String) currentContext.get(FORWARD_TO_KEY);
            if (!defPath.equals("")) {
                url = url.replaceFirst(defPath, "");
            }
            while (true) {
                VersionEntry versionEntry = globVersions.get(url);
                if (versionEntry == null) {
                    url = minusVersion(url);
                    if (url == null) {
                        break;
                    }
                } else {
                    if (!defPath.equals("")) {
                        currentContext.set(FORWARD_TO_KEY, defPath + url);
                    } else {
                        currentContext.set(FORWARD_TO_KEY, url);
                    }
                    break;
                }
            }
            return null;
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
            return "/v" + (versionEntry.getVersion() - 1) + versionEntry.getRealUrl();
        }
    }


}
