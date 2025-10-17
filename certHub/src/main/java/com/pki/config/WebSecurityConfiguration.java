package com.pki.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {
    @Value("${app.security.enabled:true}") // default true
    private boolean securityEnabled;

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        if( !securityEnabled ){
            // 🔓 Disable all security protections (for local/test use only)
            http
                    .authorizeRequests()
                    .anyRequest().permitAll()
                    .and()
                    .csrf().disable()
                    .oauth2ResourceServer().disable();
        }else{
            http
                    .authorizeHttpRequests(authorize -> authorize
                            .anyRequest().authenticated()
                    )
                    .oauth2ResourceServer(oauth2 -> oauth2.jwt());
        }

    }

}
