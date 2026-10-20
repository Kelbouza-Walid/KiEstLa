package fr.m2.groupe7.kiestla.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import fr.m2.groupe7.kiestla.dto.CommentRequest;
import fr.m2.groupe7.kiestla.exception.BusinessException;
import fr.m2.groupe7.kiestla.exception.ResourceNotFoundException;
import fr.m2.groupe7.kiestla.service.CommentService;

/**
 * Comments on absences, used by the comments panel
 * After a change, redirects back to the page the panel is on and reopens it on
 * the absence.
 */
@Controller
public class CommentController {

    private static final String EMPTY_COMMENT_MESSAGE = "Le commentaire ne peut pas être vide (2000 caractères maximum).";

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * Returns the comment list of an absence
     */
    @GetMapping("/absences/{absenceId}/comments")
    public String comments(@PathVariable Long absenceId, Authentication authentication, Model model) {
        try {
            model.addAttribute("comments", commentService.getCommentsForAbsence(absenceId, authentication.getName()));
        } catch (ResourceNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (BusinessException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        }
        return "components :: comment_list";
    }

    @PostMapping("/absences/{absenceId}/comments")
    public String addComment(
            @PathVariable Long absenceId,
            @Valid @ModelAttribute CommentRequest commentReq,
            BindingResult bindingResult,
            Authentication authentication,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("openCommentsFor", absenceId);
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("commentErrorMessage", EMPTY_COMMENT_MESSAGE);
            return Redirects.back(request);
        }
        commentService.addComment(absenceId, authentication.getName(), commentReq);
        return Redirects.back(request);
    }

    @PostMapping("/comments/{id}/edit")
    public String editComment(
            @PathVariable Long id,
            @Valid @ModelAttribute CommentRequest commentReq,
            BindingResult bindingResult,
            Authentication authentication,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("commentErrorMessage", EMPTY_COMMENT_MESSAGE);
            redirectAttributes.addFlashAttribute("openCommentsFor", commentService.getAbsenceIdOfComment(id));
            return Redirects.back(request);
        }
        Long absenceId = commentService.editComment(id, authentication.getName(), commentReq);
        redirectAttributes.addFlashAttribute("openCommentsFor", absenceId);
        return Redirects.back(request);
    }

    @PostMapping("/comments/{id}/delete")
    public String deleteComment(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        Long absenceId = commentService.deleteComment(id, authentication.getName());
        redirectAttributes.addFlashAttribute("openCommentsFor", absenceId);
        return Redirects.back(request);
    }
}
