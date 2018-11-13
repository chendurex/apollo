package com.ctrip.framework.apollo.portal.spi.t8t;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.client.methods.HttpPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;

/**
 * @author sean.liu
 * @version 2017/9/22 17:49
 */
public class T8tAuthenticationProvider implements AuthenticationProvider {
    private static final Logger logger = LoggerFactory.getLogger(T8tAuthenticationProvider.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    @Autowired
    private T8tCallService t8TCallService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String plainPassword = authentication.getCredentials().toString();
        String password = DigestUtils.md5Hex(plainPassword);

        ObjectNode args = OBJECT_MAPPER.createObjectNode();
        args.put("name", username);
        args.put("password", password);
        args.put("appName", "oa-pc");

        HttpPost req = t8TCallService.createAccPost(T8tConstants.API_LOGIN, args.toString());
        JsonNode resp = t8TCallService.post(req);

        if (resp == null) {
            logger.debug("Authentication failed: password does not match stored value");
            throw new BadCredentialsException("login failed");
        }
        return new UsernamePasswordAuthenticationToken(
                username, password, Collections.singletonList(new SimpleGrantedAuthority("ROLE_user")));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
