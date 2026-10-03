package fr.m2.groupe7.kiestla.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public String handleBusinessException(BusinessException ex, HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return redirectBack(request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return redirectBack(request);
    }

    private String redirectBack(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        // Only redirect to relative/internal paths to prevent open-redirect attacks.
        if (referer != null && !referer.isEmpty()) {
            try {
                java.net.URI uri = java.net.URI.create(referer);
                if (!uri.isAbsolute()) {
                    return "redirect:" + referer;
                }
                // Strip to just the path+query for same-server absolute URLs
                String path = uri.getPath();
                String query = uri.getQuery();
                String safePath = (query != null) ? path + "?" + query : path;
                if (safePath != null && safePath.startsWith("/")) {
                    return "redirect:" + safePath;
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return "redirect:/";
    }
}
