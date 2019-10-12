package com.github.liuhuagui.spring.boot.autoconfigure;

import com.github.liuhuagui.smalldoc.core.SmallDocContext;
import com.github.liuhuagui.smalldoc.properties.SmallDocProperties;
import com.github.liuhuagui.smalldoc.web.SmallDocServlet;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("dev")
@Configuration
@ConditionalOnWebApplication
@ConditionalOnClass(SmallDocContext.class)
@ConditionalOnProperty(name = "smalldoc.enabled", havingValue = "true", matchIfMissing = true)
public class SmallDocAutoConfiguration {

    @Bean
    @ConfigurationProperties("smalldoc")
    public SmallDocProperties smallDocProperties(){
        return new SmallDocProperties();
    }

    @Bean
    public ServletRegistrationBean smallDocServletRegistrationBean(SmallDocProperties smallDocProperties) {
        ServletRegistrationBean registrationBean = new ServletRegistrationBean();
        registrationBean.setServlet(new SmallDocServlet(smallDocProperties));
        registrationBean.addUrlMappings(smallDocProperties.getUrlPattern() != null ? smallDocProperties.getUrlPattern() : "/smalldoc/*");
        return registrationBean;
    }

}
