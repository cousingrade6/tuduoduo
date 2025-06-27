package com.whisky.yupicturebackend.manager;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JsoupFetcher {
    private static final int DEFAULT_TIMEOUT = 10000; // 10秒
    private static final int MAX_RETRIES = 3;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";

    public static Document fetchDocument(String url) throws IOException {
        return fetchDocument(url, DEFAULT_TIMEOUT, MAX_RETRIES);
    }

    public static Document fetchDocument(String url, int timeout, int maxRetries) throws IOException {
        int retryCount = 0;
        IOException lastException = null;

        while (retryCount < maxRetries) {
            try {
                return Jsoup.connect(url)
                        .timeout(timeout)
                        .userAgent(USER_AGENT)
                        .ignoreHttpErrors(true)
                        .followRedirects(true)
                        .maxBodySize(0)
                        .get();
            } catch (IOException e) {
                lastException = e;
                retryCount++;
                if (retryCount < maxRetries) {
                    try {
                        Thread.sleep(1000 * retryCount); // 线性退避
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("请求被中断", ie);
                    }
                }
            }
        }
        throw new IOException("重试 " + maxRetries + " 次后仍然无法获取URL: " + url, lastException);
    }
}
