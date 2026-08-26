package org.riteshingle.campusgig.Security;

import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.JwtUtils.JwtUtils;
import org.riteshingle.campusgig.Service.CustomUserDetailsService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtUtils jwtUtils;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel){
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if(StompCommand.CONNECT.equals(accessor.getCommand())){

            String authorization = accessor.getFirstNativeHeader("Authorization");

            if (authorization == null || !authorization.startsWith("Bearer "))
                throw new IllegalArgumentException("Missing WebSocket Authorization token");

            String token = authorization.substring(7);
            String username = jwtUtils.extractEmail(token);

            System.out.println("WS USERNAME = " + username);

            if (username == null) throw new IllegalArgumentException("Invalid JWT token");

            UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

            if (!jwtUtils.validateToken(token, userDetails))
                throw new IllegalArgumentException("Invalid JWT token");

            Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            accessor.setUser(authentication);

            System.out.println("WS AUTHENTICATION = " + accessor.getUser());

            System.out.println("WS PRINCIPAL = " + accessor.getUser());

            return MessageBuilder.createMessage(
                    message.getPayload(),
                    accessor.getMessageHeaders()
            );
        }

        return message;
    }
}
