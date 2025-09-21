package com.auzienko.javamocks.transaction.app.filter;

import com.auzienko.javamocks.transaction.app.config.props.RequestLoggingFilterProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private final RequestLoggingFilterProperties properties;

    public RequestLoggingFilter(RequestLoggingFilterProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        return !properties.isEnabled();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // We need to wrap the request to be able to read the body multiple times.
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // Pass the wrapped request and response down the filter chain.
        // This is where the controller will handle the request.
        filterChain.doFilter(requestWrapper, responseWrapper);

        stopWatch.stop();
        long timeTaken = stopWatch.getTotalTimeMillis();

        // After the request is handled, the body is cached. We can now log it.
        String requestBody = getBody(requestWrapper.getContentAsByteArray(), request.getCharacterEncoding());
        String responseBody = getBody(responseWrapper.getContentAsByteArray(), response.getCharacterEncoding());

        log.info("Request: {} {} | Body: {} | Response: {} | Body: {} | Time: {}ms",
                request.getMethod(),
                request.getRequestURI(),
                requestBody.replaceAll("[\r\n\s]+", ""), // Remove newlines and excess whitespace for cleaner logs
                responseWrapper.getStatus(),
                responseBody.replaceAll("[\r\n\s]+", ""),
                timeTaken);

        // Finally, we must copy the cached response body to the actual response output stream.
        responseWrapper.copyBodyToResponse();
    }

    private String getBody(byte[] content, String encoding) {
        if (content == null || content.length == 0) {
            return "";
        }

        if (content.length > properties.getMaxPayloadSize()) {
            return "[PAYLOAD TOO LARGE]";
        }
        try {
            return new String(content, encoding != null ? encoding : StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return "[UNSUPPORTED ENCODING]";
        }
    }
}
