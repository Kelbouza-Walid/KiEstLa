package fr.m2.groupe7.kiestla.controller;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Redirection helpers shared by the controllers.
 */
public final class Redirects {

    private Redirects() {
    }

    /**
     * Redirects to the page the request was sent from, or to the home page if it is unknown.
     */
    public static String back(HttpServletRequest request) {
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
