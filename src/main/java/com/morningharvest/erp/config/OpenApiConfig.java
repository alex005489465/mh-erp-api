package com.morningharvest.erp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger) 配置類別
 * 提供 API 文件介面
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Morning Harvest ERP API")
                        .version("1.0.0")
                        .description("早餐店 ERP 系統 API 文件")
                        .contact(new Contact()
                                .name("Morning Harvest")
                                .email("support@morningharvest.com")));
    }

}
