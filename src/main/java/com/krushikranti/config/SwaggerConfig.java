package com.krushikranti.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI krushiKrantiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("KrushiKranti API")
                        .description("""
                                ## REST API for KrushiKranti Agricultural Marketplace Platform
                                
                                ### Architecture Overview
                                ```
                                React Frontend
                                      ↓
                                Spring Boot Backend (This API)
                                      ↓
                                MySQL Database
                                      ↓
                                Cloudinary (Images)
                                      ↓
                                Razorpay (Payments)
                                ```
                                
                                ### Features
                                - **Authentication**: JWT-based authentication with role-based access
                                - **Products**: Farmers can list agricultural products
                                - **Orders**: Place and manage orders
                                - **Payments**: Razorpay integration for secure payments
                                - **Blogs**: Agricultural tips and news
                                - **Image Upload**: Cloudinary integration for product/user images
                                - **Real-time Chat**: WebSocket-based messaging
                                
                                ### Roles
                                - `USER` - Regular buyers
                                - `FARMER` - Can list products and write blogs
                                - `WHOLESALER` - Bulk buyers
                                - `ADMIN` - Full system access
                                """)
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("KrushiKranti Team")
                                .email("support@krushikranti.com")
                                .url("https://krushikranti.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .externalDocs(new ExternalDocumentation()
                        .description("KrushiKranti Documentation")
                        .url("https://docs.krushikranti.com"))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development Server"),
                        new Server().url("https://api.krushikranti.com").description("Production Server")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token obtained from /api/v1/auth/login")));
    }
}
