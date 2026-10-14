package com.lipari.bank.shared.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(1)
@Slf4j
public class RequestAuditFilter extends OncePerRequestFilter {

  private static final String REQUEST_ID_ATTRIBUTE =
      MdcLoggingFilter.class.getName() + "-requestId";


  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {

    long startTime = System.nanoTime();

    try {
      filterChain.doFilter(request, response);
    } finally {
      long durationMs =
          (System.nanoTime() - startTime) / 1_000_000;

      Object requestId =
          request.getAttribute(REQUEST_ID_ATTRIBUTE);

      // Il filtro MDC interno ha già ripulito il contesto.
      // Ripristiniamo temporaneamente il requestId per il log di audit.
      if (requestId != null) {
        MDC.put("requestId", requestId.toString());
      }

      log.info(
          "AUDIT requestId={} method={} uri={} status={} durationMs={}",
          requestId != null ? requestId : "NO-REQ-ID",
          request.getMethod(),
          request.getRequestURI(),
          response.getStatus(),
          durationMs
      );

      MDC.remove("requestId");
    }
  }

}