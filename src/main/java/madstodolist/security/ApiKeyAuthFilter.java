package madstodolist.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import madstodolist.model.Comercio;
import madstodolist.repository.ComercioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-KEY";

    @Autowired
    private ComercioRepository comercioRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getServletPath();

        // Ignorar la validación de API-Key para rutas públicas como /h2-console
        if (path.startsWith("/h2-console")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Validar API-Key solo en rutas protegidas (ej: /pago/**)
        if (path.startsWith("/pago/")) {
            String apiKey = extractApiKey(request);

            if (StringUtils.hasText(apiKey)) {
                Optional<Comercio> comercioOpt = comercioRepository.findByApiKey(apiKey);
                if (comercioOpt.isPresent()) {
                    Comercio comercio = comercioOpt.get();
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            comercio, null, Collections.emptyList());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "API-Key inválida o no proporcionada");
                    return;
                }
            } else {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "API-Key no proporcionada");
                return;
            }
        }

        // Continuar con el filtro
        filterChain.doFilter(request, response);
    }

    private String extractApiKey(HttpServletRequest request) {
        return request.getHeader(API_KEY_HEADER);
    }
}