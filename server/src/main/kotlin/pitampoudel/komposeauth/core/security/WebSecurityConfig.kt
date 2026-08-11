package pitampoudel.komposeauth.core.security

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.DispatcherType
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseCookie
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.authentication.DisabledException
import org.springframework.security.authentication.LockedException
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver
import org.springframework.security.web.DefaultRedirectStrategy
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.config.ObjectPostProcessor
import org.springframework.security.web.csrf.CsrfFilter
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy
import org.springframework.security.web.util.matcher.AndRequestMatcher
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher
import org.springframework.security.web.util.matcher.OrRequestMatcher
import org.springframework.security.web.util.matcher.NegatedRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.util.UriComponentsBuilder
import pitampoudel.core.data.MessageResponse
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.core.domain.ApiEndpoints.THIRD_FACTOR_KYC
import pitampoudel.komposeauth.core.domain.Constants.ACCESS_TOKEN_COOKIE_NAME
import pitampoudel.komposeauth.core.security.csrf.CrossOriginCsrfTokenRepository
import pitampoudel.komposeauth.core.security.csrf.authCookieDomain

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true)
class WebSecurityConfig {

    private fun clearTokenCookie(
        request: HttpServletRequest,
        response: HttpServletResponse,
        appConfigService: AppConfigService
    ) {
        val clearCookie = ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME, "")
            .httpOnly(true)
            .secure(request.isSecure)
            .path("/")
            .sameSite(if (request.isSecure) "None" else "Lax")
            .maxAge(0)
            .domain(authCookieDomain(appConfigService))
            .build()
        response.addHeader("Set-Cookie", clearCookie.toString())
    }

    /**
     * Cross-origin rules, and deliberately nothing to say about same-origin traffic.
     *
     * Two ways this used to lock an operator out of their own server, both of which ended as a bare
     * "Invalid CORS request":
     *
     * Handing back a configuration whose allow-list is empty is not the same as having no opinion —
     * it is an instruction to refuse every origin. Once `corsAllowedOrigins()` began discarding a
     * bare `*`, which it must, since matching every origin for credentialed requests lets any site
     * read authenticated responses, an operator who had configured exactly that was left with an
     * empty list and so a server that turned away anything carrying an `Origin`. Returning null
     * instead leaves CORS unmanaged: same-origin requests are untouched, and cross-origin ones are
     * refused by the browser for want of the headers, which is the right default before anything is
     * configured.
     *
     * The server's own origin is then always allowed, whatever the list says. Spring decides
     * same-origin by comparing scheme, host and port against the request's own — so a proxy that
     * terminates TLS without a usable `X-Forwarded-Proto` makes the page's `https://` origin look
     * foreign to a server that believes it is on `http://`, and the console starts refusing its own
     * form posts. Comparing hosts alone is enough to prevent that: an origin on this very host is
     * not a cross-site caller in any sense that matters here.
     */
    @Bean
    fun corsConfigurationSource(appConfigService: AppConfigService): CorsConfigurationSource {
        return CorsConfigurationSource { request ->
            val configured = appConfigService.corsAllowedOrigins()
            val ownOrigin = request.getHeader(HttpHeaders.ORIGIN)
                ?.takeIf { sameHostAsServer(it, request) }

            val origins = (configured + listOfNotNull(ownOrigin)).distinct()
            if (origins.isEmpty()) {
                // No opinion, rather than "refuse everyone".
                return@CorsConfigurationSource null
            }

            val configuration = CorsConfiguration()
            if (origins.any { it.contains("*") }) {
                configuration.allowedOriginPatterns = origins
            } else {
                configuration.allowedOrigins = origins
            }
            configuration.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            // Safe to reflect: the origin allow-list above is what actually gates access, and it is
            // never `*` while credentials are allowed.
            configuration.allowedHeaders = listOf("*")
            configuration.allowCredentials = true
            configuration.maxAge = 1800L
            configuration
        }
    }

    /** Whether [origin] names this very server, ignoring scheme and port. See the note above. */
    private fun sameHostAsServer(origin: String, request: HttpServletRequest): Boolean {
        val originHost = runCatching {
            UriComponentsBuilder.fromUriString(origin).build().host
        }.getOrNull() ?: return false
        return originHost.equals(request.serverName, ignoreCase = true)
    }

    /**
     * Exactly which requests must carry a CSRF token.
     *
     * This has to be forced onto the filter rather than merely configured, because
     * `OAuth2ResourceServerConfigurer` quietly adds a `BearerTokenRequestMatcher` of its own to the
     * CSRF exemptions. That matcher asks the [BearerTokenResolver] whether the request carries a
     * token, and this application's resolver falls back to reading the access-token cookie — so
     * every cookie-authenticated request was being exempted. Since that cookie is SameSite=None and
     * rides along on cross-site requests, it exempted precisely the requests CSRF protection exists
     * to stop. The exemption is sound for a real `Authorization` header, which a browser will not
     * attach on its own; it is not sound for a cookie.
     */
    private fun csrfProtectionMatcher(): RequestMatcher {
        val safeMethods = setOf("GET", "HEAD", "TRACE", "OPTIONS")
        val stateChanging = RequestMatcher { request -> request.method !in safeMethods }
        return AndRequestMatcher(
            stateChanging,
            NegatedRequestMatcher(
                OrRequestMatcher(
                    PublicEndpoints.csrfExemptRequestMatcher(),
                    headerOnlyBearerRequest()
                )
            )
        )
    }

    /**
     * A request that carries a bearer token in the `Authorization` header and no session or
     * access-token cookie cannot be forged cross-site: the browser will not attach that header on
     * its own. Native and server-to-server clients authenticate this way, so exempting them keeps
     * CSRF protection focused on the cookie-authenticated browser surface where it actually applies.
     */
    private fun headerOnlyBearerRequest(): RequestMatcher = RequestMatcher { request ->
        val hasBearerHeader = request.getHeader("Authorization")
            ?.startsWith("Bearer ", ignoreCase = true) == true
        val cookieNames = request.cookies?.map { it.name }.orEmpty()
        val hasAmbientCredential = cookieNames.any {
            it == ACCESS_TOKEN_COOKIE_NAME || it == "JSESSIONID" || it == "SESSION"
        }
        hasBearerHeader && !hasAmbientCredential
    }

    @Bean
    @Order(2)
    fun securityFilterChain(
        http: HttpSecurity,
        jwtAuthenticationConverter: JwtAuthenticationConverter,
        objectMapper: ObjectMapper,
        bearerTokenResolver: BearerTokenResolver,
        loginSuccessHandler: OAuth2LoginSuccessHandler,
        appConfigService: AppConfigService,
        csrfTokenRepository: CrossOriginCsrfTokenRepository,
        authorizationRequestResolver: OAuth2AuthorizationRequestResolver
    ): SecurityFilterChain {
        return http
            .cors { }
            // Session and access-token cookies are ambient authority, and the access-token cookie is
            // deliberately SameSite=None so it crosses sites. With CSRF off, any page on the internet
            // could drive a form-encoded POST — /config (every secret this server holds),
            // /update-profile, role grants — using a logged-in victim's credentials.
            .csrf { csrf ->
                // Scoped and same-site-configured to match the access-token cookie, so browser apps
                // on sibling origins can actually obtain a token. See the repository's own notes.
                csrf.csrfTokenRepository(csrfTokenRepository)
                // Spring's default handler. It masks the token with a fresh random pad on each
                // render, so the value in a page's markup differs every time and cannot be recovered
                // by measuring the size of a compressed response (BREACH). Everything that submits a
                // token here — Thymeleaf forms, the console's fetch calls, /csrf — takes it from the
                // rendered value rather than the cookie, so the masking is transparent to all of them.
                csrf.csrfTokenRequestHandler(XorCsrfTokenRequestAttributeHandler())
                csrf.requireCsrfProtectionMatcher(csrfProtectionMatcher())
                // Configuring the matcher above is not enough on its own: exemptions registered by
                // other configurers are AND-NOT-ed onto whatever is set here, and the resource
                // server registers one that matches any request carrying a token — including one
                // read from the cookie. Post-processing runs after the filter has been built and
                // its matcher assembled, so this is the last word on the subject.
                csrf.withObjectPostProcessor(object : ObjectPostProcessor<CsrfFilter> {
                    override fun <O : CsrfFilter> postProcess(filter: O): O {
                        filter.setRequireCsrfProtectionMatcher(csrfProtectionMatcher())
                        return filter
                    }
                })
            }
            .headers { headers ->
                headers
                    .frameOptions { it.deny() }
                    .httpStrictTransportSecurity { hsts ->
                        hsts.includeSubDomains(true).maxAgeInSeconds(31_536_000)
                    }
                    .referrerPolicy {
                        it.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                    }
                    .contentSecurityPolicy {
                        // The bundled Thymeleaf pages use inline script/style, so those stay allowed;
                        // everything else is same-origin only and the pages cannot be framed.
                        it.policyDirectives(
                            "default-src 'self'; " +
                                    "script-src 'self' 'unsafe-inline'; " +
                                    "style-src 'self' 'unsafe-inline'; " +
                                    "img-src 'self' data: https:; " +
                                    "connect-src 'self'; " +
                                    "object-src 'none'; " +
                                    "base-uri 'self'; " +
                                    "form-action 'self'; " +
                                    "frame-ancestors 'none'"
                        )
                    }
            }
            .logout { logout ->
                logout
                    .logoutUrl("/${ApiEndpoints.LOGOUT}")
                    .logoutSuccessHandler { request, response, _ ->
                        clearTokenCookie(request, response, appConfigService)
                        response.contentType = MediaType.APPLICATION_JSON_VALUE
                        response.writer.write(
                            objectMapper.writeValueAsString(
                                MessageResponse(message = "Logout successful")
                            )
                        )
                    }
            }
            .sessionManagement { sessions ->
                sessions.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            }
            .oauth2ResourceServer { conf ->
                conf.bearerTokenResolver(bearerTokenResolver)
                // An invalid/expired bearer token (from the Authorization header OR the access-token
                // cookie) is rejected directly by the resource-server filter, short-circuiting the
                // exceptionHandling entry point below. Clear the stale cookie here too so the browser
                // stops resending a token that will only keep producing 401s.
                val bearerEntryPoint = BearerTokenAuthenticationEntryPoint()
                conf.authenticationEntryPoint { request, response, authException ->
                    clearTokenCookie(request, response, appConfigService)
                    bearerEntryPoint.commence(request, response, authException)
                }
                conf.jwt {
                    it.jwtAuthenticationConverter(jwtAuthenticationConverter)
                }
            }
            .formLogin { formLogin ->
                formLogin
                    .loginPage("/session-login")
                    .loginProcessingUrl("/session-login")
                    // A locked account and a wrong password are different problems with different
                    // fixes, so the page needs to tell them apart.
                    .failureHandler { request, response, exception ->
                        val reason = when (exception) {
                            is LockedException, is DisabledException -> "locked"
                            else -> ""
                        }
                        DefaultRedirectStrategy()
                            .sendRedirect(request, response, "/session-login?error=$reason")
                    }
                    .permitAll()
            }
            .oauth2Login { oauth2 ->
                oauth2.successHandler(loginSuccessHandler)
                oauth2.authorizationEndpoint { endpoint ->
                    endpoint.authorizationRequestResolver(authorizationRequestResolver)
                }
            }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(*PublicEndpoints.purelyPublicPatterns.toTypedArray()).permitAll()
                    .requestMatchers(*PublicEndpoints.optionalAuthPatterns.toTypedArray()).permitAll()
                    .requestMatchers(HttpMethod.POST, "/$THIRD_FACTOR_KYC").permitAll()
                    .requestMatchers(
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**"
                    ).hasRole("ADMIN")
                    .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD)
                    .permitAll()
                    .anyRequest().authenticated()
            }
            .exceptionHandling { exceptions ->
                exceptions.defaultAuthenticationEntryPointFor(
                    { request, response, _ ->
                        clearTokenCookie(request, response, appConfigService)
                        response.status = 401
                        response.contentType = MediaType.APPLICATION_JSON_VALUE
                    },
                    NegatedRequestMatcher(MediaTypeRequestMatcher(MediaType.TEXT_HTML))
                )
            }
            .build()
    }
}
