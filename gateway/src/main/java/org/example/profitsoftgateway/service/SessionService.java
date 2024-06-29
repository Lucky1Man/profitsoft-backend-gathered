package org.example.profitsoftgateway.service;

import org.example.profitsoftgateway.auth.dto.UserInfo;
import org.example.profitsoftgateway.data.UserSession;
import org.example.profitsoftgateway.repository.UserSessionRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

import static org.example.profitsoftgateway.filter.AuthenticationFilter.COOKIE_SESSION_ID;


@Service
public class SessionService {

    public static final Duration SESSION_DURATION = Duration.ofHours(1);

    private final UserSessionRepository userSessionRepository;

    private final String reactAddress;

    public SessionService(UserSessionRepository userSessionRepository,
                          @Qualifier("reactAddress")
                          String reactAddress) {
        this.userSessionRepository = userSessionRepository;
        this.reactAddress = reactAddress;
    }

    public Mono<UserSession> checkSession(ServerWebExchange exchange) {
        HttpCookie sessionCookie = exchange.getRequest().getCookies().getFirst(COOKIE_SESSION_ID);
        if (sessionCookie == null) {
            return Mono.error(new UnauthorizedException("Session Cookie not found"));
        }
        return userSessionRepository.findById(sessionCookie.getValue())
                .flatMap(session ->
                        session.isExpired()
                                ? Mono.error(new UnauthorizedException("Session expired"))
                                : Mono.just(session)
                ).switchIfEmpty(Mono.error(new UnauthorizedException("Session not found")));
    }

    public Mono<UserSession> saveSession(UserInfo userInfo) {
        return userSessionRepository.createSession(userInfo, Instant.now().plus(SESSION_DURATION));
    }

    public Mono<Void> addSessionCookie(ServerWebExchange exchange, UserSession session) {
        return Mono.fromRunnable(() -> exchange.getResponse().addCookie(ResponseCookie.from(COOKIE_SESSION_ID)
                .value(session.getId())
                .path("/")
                .maxAge(SESSION_DURATION)
                .httpOnly(true)
                .build()));
    }


}
