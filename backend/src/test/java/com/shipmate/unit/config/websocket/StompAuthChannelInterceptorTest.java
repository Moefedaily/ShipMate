package com.shipmate.unit.config.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import com.shipmate.config.websocket.StompAuthChannelInterceptor;
import com.shipmate.security.JwtUtil;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;

@ExtendWith(MockitoExtension.class)
class StompAuthChannelInterceptorTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @Mock
    private MessageChannel channel;

    @InjectMocks
    private StompAuthChannelInterceptor interceptor;

    @Test
    void preSend_shouldPassThroughNonConnectFrames() {
        Message<byte[]> message = stompMessage(StompCommand.SEND, null);

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isSameAs(message);
    }

    @Test
    void preSend_shouldRejectConnectWithoutBearerHeader() {
        Message<byte[]> message = stompMessage(StompCommand.CONNECT, null);

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isNull();
    }

    @Test
    void preSend_shouldRejectInvalidToken() {
        UUID userId = UUID.randomUUID();
        String token = "bad-token";
        Claims claims = new DefaultClaims();
        claims.setSubject(userId.toString());

        when(jwtUtil.extractClaims(token)).thenReturn(claims);
        when(jwtUtil.isTokenValid(token, userId)).thenReturn(false);

        Message<?> result = interceptor.preSend(stompMessage(StompCommand.CONNECT, "Bearer " + token), channel);

        assertThat(result).isNull();
    }

    @Test
    void preSend_shouldRejectWhenClaimExtractionFails() {
        String token = "broken-token";
        when(jwtUtil.extractClaims(token)).thenThrow(new IllegalArgumentException("broken"));

        Message<?> result = interceptor.preSend(stompMessage(StompCommand.CONNECT, "Bearer " + token), channel);

        assertThat(result).isNull();
    }

    @Test
    void preSend_shouldAuthenticateValidConnectMessage() {
        UUID userId = UUID.randomUUID();
        String token = "good-token";
        Claims claims = new DefaultClaims();
        claims.setSubject(userId.toString());
        UserDetails userDetails = new User(
                userId.toString(),
                "secret",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        Message<byte[]> message = stompMessage(StompCommand.CONNECT, "Bearer " + token);

        when(jwtUtil.extractClaims(token)).thenReturn(claims);
        when(jwtUtil.isTokenValid(token, userId)).thenReturn(true);
        when(userDetailsService.loadUserByUsername(userId.toString())).thenReturn(userDetails);

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isSameAs(message);

        verify(userDetailsService).loadUserByUsername(userId.toString());
    }

    private Message<byte[]> stompMessage(StompCommand command, String authorizationHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (authorizationHeader != null) {
            accessor.setNativeHeader("Authorization", authorizationHeader);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
