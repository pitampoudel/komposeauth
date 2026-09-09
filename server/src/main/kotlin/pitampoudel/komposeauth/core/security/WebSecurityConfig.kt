package pitampoudel.komposeauth.core.security

import com.fasterxml.jackson.databind.ObjectMapper
import io.sentry.Sentry
import jakarta.servlet.DispatcherType
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.core.Ordered
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
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.web.DefaultRedirectStrategy
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.savedrequest.HttpSessionRequestCache
import org.springframework.security.web.savedrequest.RequestCache
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher
import org.springframework.security.config.ObjectPostProcessor
import org.springframework.security.web.csrf.CsrfFilter
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy
import org.springframework.security.web.util.matcher.AndRequestMatcher
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher
import org.springframework.security.web.util.matcher.NegatedRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.filter.CorsFilter
import org.springframework.web.util.UriComponentsBuilder
import pitampoudel.core.data.MessageResponse
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.core.domain.ApiEndpoints.THIRD_FACTOR_KYC
import pitampoudel.komposeauth.core.domain.Constants.ACCESS_TOKEN_COOKIE_NAME
import pitampoudel.komposeauth.core.security.csrf.CrossOriginCsrfTokenRepository
import pitampoudel.komposeauth.core.security.csrf.CsrfProtectionPolicy
import pitampoudel.komposeauth.core.security.csrf.EagerCsrfTokenFilter
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

    @Bean
    fun requestCache(): RequestCache {
        val worthResuming = AndRequestMatcher(
            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/**"),
            MediaTypeRequestMatcher(MediaType.TEXT_HTML),
            NegatedRequestMatcher(PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/"))
        )
        return HttpSessionRequestCache().apply { setRequestMatcher(worthResuming) }
    }


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

    /**
     * The same CORS decision as the security chain's, taken early enough to be on every response.
     *
     * `.cors { }` below installs this inside the Spring Security chain, which is registered late —
     * after the abuse limiter, which sits at the very front by design so a throttled request is
     * turned away before anything expensive happens. A 429 therefore left without a single
     * `Access-Control-Allow-*` header, and a browser cannot read a response it was not allowed to
     * read: the app saw an opaque CORS failure rather than "too many requests", with nothing to say
     * a limit had been hit. That is the difference between a legible error and a mystery, and it
     * shows up exactly where the limiter is most likely to misfire — behind a proxy whose hop count
     * has not been declared, where one bucket holds every caller at once.
     *
     * Deciding twice is free: `DefaultCorsProcessor` returns immediately if the response already
     * carries `Access-Control-Allow-Origin`, so the chain's own filter simply finds the work done.
     */
    @Bean
    fun corsFilterRegistration(
        corsConfigurationSource: CorsConfigurationSource
    ): FilterRegistrationBean<CorsFilter> {
        val registration = FilterRegistrationBean(CorsFilter(corsConfigurationSource))
        // Ahead of the rate limiter at HIGHEST_PRECEDENCE + 10, behind the error reporter at
        // HIGHEST_PRECEDENCE, which only wraps the chain.
        registration.order = Ordered.HIGHEST_PRECEDENCE + 5
        registration.addUrlPatterns("/*")
        return registration
    }

    /**
     * Whether [origin] names this very server, ignoring scheme and port. See the note above.
     *
     * Three answers to the question "what host is this server reached at", because no single one
     * of them is available everywhere. `serverName` is the right one and is what `ForwardedHeaderFilter`
     * rewrites from `X-Forwarded-Host` — but only where the proxy sends that header and
     * `server.forward-headers-strategy` is left at `framework`. Where it is not, `serverName` is
     * the container's internal name, so the console's own origin looked foreign to it: with an
     * allow-list configured for the app's front end and not for the console, Spring's CORS
     * processor refused the configuration page's own form post outright with "Invalid CORS
     * request". The setting that would have fixed it was on the page that could no longer be
     * saved, and the whole thing depended on how the deployment's proxy was set up — which is why
     * the same build saved fine in one place and failed with a CORS error in another.
     *
     * `Host` and `X-Forwarded-Host` are the other two, and for this question they are sound ones:
     * both name the authority the browser addressed, and neither can be set by a page. `Host` is a
     * forbidden header name, and `X-Forwarded-Host` is not CORS-safelisted — a scripted request
     * carrying one is preflighted, and the preflight does not carry it, so the check below never
     * sees an attacker's value on a request that could still be allowed. Reading them costs nothing
     * where they are absent and settles the case where `serverName` alone is wrong.
     */
    private fun sameHostAsServer(origin: String, request: HttpServletRequest): Boolean {
        val originHost = runCatching {
            UriComponentsBuilder.fromUriString(origin).build().host
        }.getOrNull() ?: return false
        return ownHostCandidates(request).any { originHost.equals(it, ignoreCase = true) }
    }

    /** Every name this request suggests the server was reached at. */
    private fun ownHostCandidates(request: HttpServletRequest): List<String> = listOfNotNull(
        request.serverName,
        hostOf(request.getHeader(HttpHeaders.HOST)),
        // A list when several proxies appended to it; the leftmost is the one the browser addressed.
        hostOf(request.getHeader("X-Forwarded-Host")?.substringBefore(','))
    )

    /** The host named by an authority such as `Host`, with any port stripped. */
    private fun hostOf(authority: String?): String? {
        val host = authority?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        // IPv6 literals are bracketed, so the colon that separates the port is the one after `]`.
        val portSeparator = if (host.startsWith("[")) {
            host.indexOf(':', host.indexOf(']').takeIf { it >= 0 } ?: 0)
        } else {
            host.indexOf(':')
        }
        return if (portSeparator > 0) host.substring(0, portSeparator) else host
    }

    /**
     * Exactly which requests must carry a CSRF token — see [CsrfProtectionPolicy] for the rule.
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
    private fun csrfProtectionMatcher(): RequestMatcher = CsrfProtectionPolicy.matcher()

    /**
     * Where a failed sign-in through Google or Apple lands, and the only place its cause is kept.
     *
     * Both halves of that were missing, and together they are the whole of what a visitor saw when
     * the callback failed. Spring Security's default is `failureUrl(loginPage + "?error")`, and
     * `OAuth2LoginConfigurer`'s idea of `loginPage` is its own `/login` — so the visitor was sent to
     * `/login?error`, where the generated page (see the note at the call site) greeted them with
     * "Invalid credentials" and a Google button. Pressing it went back to the provider, back to the
     * failing callback, and back to that page: a closed loop, on a page this application never
     * wrote, saying something untrue — nothing was wrong with their credentials.
     *
     * Meanwhile the reason was recorded nowhere. A failed sign-in is not an exception that escapes
     * the chain, so [UnhandledErrorReportingFilter] never sees it, and the only trace Spring
     * Security leaves is a `TRACE` line from a logger this application runs at `WARN`. An operator
     * had a login loop and an empty log.
     *
     * So: back to the real login page, which explains itself and still offers the password form,
     * with the provider's own error code logged and reported. The saved authorization request
     * survives the trip — `/session-login` is public, so nothing overwrites it — and the relying
     * party's sign-in resumes once the visitor gets in.
     *
     * One code is handled apart from the rest, `authorization_request_not_found`; the reason is at
     * the branch itself.
     */
    private fun providerLoginFailureHandler(): AuthenticationFailureHandler {
        val log = LoggerFactory.getLogger("pitampoudel.komposeauth.core.security.oauth2")
        val redirectStrategy = DefaultRedirectStrategy()
        return AuthenticationFailureHandler { request, response, exception ->
            // The code names the leg that failed — `invalid_token_response` for the code exchange,
            // `invalid_id_token`, `invalid_user_info_response` — which is the one thing needed to
            // know where to look.
            val code = (exception as? OAuth2AuthenticationException)?.error?.errorCode ?: "unknown"

            if (code == "authorization_request_not_found") {
                log.info("A sign-in returned from the provider after its session had expired")
                redirectStrategy.sendRedirect(request, response, "/session-login?error=expired")
                return@AuthenticationFailureHandler
            }

            log.error("Sign-in through an identity provider failed [{}]", code, exception)
            Sentry.captureException(exception) { scope ->
                scope.setTag("origin", "oauth2-login")
                scope.setTag("oauth2.error", code)
            }
            redirectStrategy.sendRedirect(request, response, "/session-login?error=provider")
        }
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
            // Settles the token while the response can still carry its cookie. See the filter.
            .addFilterAfter(EagerCsrfTokenFilter(), CsrfFilter::class.java)
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
                        //
                        // `form-action` is deliberately absent, and has to be. Signing in with a
                        // password is a form POST, and the response to it is the whole point of an
                        // authorization server: a redirect on to `/oauth2/authorize`, which redirects
                        // again to the relying party's `redirect_uri` — another origin by definition,
                        // and for a native client not even an http one. Firefox and Safari apply
                        // `form-action` to every hop of a form submission's redirect chain (Chromium
                        // stops at the action URL), so `form-action 'self'` let the POST through,
                        // established the session, and then silently killed the navigation that was
                        // meant to carry the visitor back to the app. The page simply sat there with
                        // its button reading "Signing in…", and only ever for visitors who arrived
                        // from a relying party — signing in directly here redirects to `/`, which is
                        // same-origin and so was allowed, and "Continue with Google" is a link rather
                        // than a form and was never in scope. There is no value that fixes this:
                        // redirect URIs are per-client, registered at runtime, and may use a private
                        // scheme, so the directive cannot name them.
                        it.policyDirectives(
                            "default-src 'self'; " +
                                    "script-src 'self' 'unsafe-inline'; " +
                                    "style-src 'self' 'unsafe-inline'; " +
                                    "img-src 'self' data: https:; " +
                                    "connect-src 'self'; " +
                                    "object-src 'none'; " +
                                    "base-uri 'self'; " +
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
                // Naming the page here is not cosmetic. `OAuth2LoginConfigurer` keeps its own
                // default of `/login` when it is not told otherwise, and since no page is *named*
                // it also decides the application has none and switches on
                // `DefaultLoginPageGeneratingFilter` to render one. That generated page is then
                // served at `/login` — which `PublicEndpoints` makes public for the JSON login API
                // — so the server hands out a second, unbranded sign-in page nobody wrote.
                oauth2.loginPage("/session-login")
                oauth2.successHandler(loginSuccessHandler)
                oauth2.failureHandler(providerLoginFailureHandler())
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
