package com.shang.versioncontrol;

/**
 * Created by shangzebei on 2017/8/23.
 */
public class VersionEntry {
    private int version;
    private String url;//v1/hello
    private String realUrl;///hello
    private String[] cellUrl;

    public String getRealUrl() {
        return realUrl;
    }

    public void setRealUrl(String realUrl) {
        this.realUrl = realUrl;
    }

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

    public String[] getCellUrl() {
        return cellUrl;
    }

    public void setCellUrl(String[] cellUrl) {
        this.cellUrl = cellUrl;
    }
}
