package io.nextpos.shared.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * https://www.baeldung.com/spring-response-header
 */
@Component
public class RequestIdContextFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestIdContextFilter.class);

    private static final String MDC_REQUEST_ID = "request.id";

    private static final String REQUEST_ID_HEADER = "x-request-id";

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain) throws ServletException, IOException {

        final String requestId = UUID.randomUUID().toString();
        String requestURI = request.getRequestURI();

        try {
            MDC.put(MDC_REQUEST_ID, requestURI);
            response.setHeader(REQUEST_ID_HEADER, requestId);
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            LOGGER.error("Caught unexpected exception on request [{}]: {}", requestId, e.getMessage(), e);
            throw e;
        }
        finally {
            MDC.remove(MDC_REQUEST_ID);
        }
    }
}
