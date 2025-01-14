package com.efficientgoose.book.feedback;

import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.efficientgoose.book.book.Book;
import com.efficientgoose.book.book.BookRepository;
import com.efficientgoose.book.common.PageResponse;
import com.efficientgoose.book.exception.OperationNotPermittedException;
import com.efficientgoose.book.user.User;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final BookRepository bookRepository;
    private final FeedbackMapper feedbackMapper;
    private final FeedbackRepository feedbackRepository;

    public Integer save(FeedbackRequest request, Authentication connectedUser) {
        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Book with id:" + request.bookId() + " not found"));

        if (book.isArchived() || !book.isShareable()) {
            throw new OperationNotPermittedException(
                    "You cannot give a feedback to an archived or non-shareable book");
        }

        User user = ((User) connectedUser.getPrincipal());

        if (Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You cannot give feedback to your own book");
        }

        Feedback feedback = feedbackMapper.toFeedback(request);

        return feedbackRepository.save(feedback).getId();

    }

    public PageResponse<FeedbackResponse> findAllFeedbacksByBook(Integer bookId, int page, int size,
            Authentication connectedUser) {

        Pageable pageable = PageRequest.of(page, size);
        User user = ((User) connectedUser.getPrincipal());
        Page<Feedback> feedbacks = feedbackRepository.findAllByBookId(bookId, pageable);

        List<FeedbackResponse> feedbackResponses = feedbacks.stream()
                .map(f -> feedbackMapper.toFeedbackResponse(f, user.getId()))
                .toList();

        return new PageResponse<>(
                feedbackResponses,
                feedbacks.getNumber(),
                feedbacks.getSize(),
                feedbacks.getTotalElements(),
                feedbacks.getTotalPages(),
                feedbacks.isFirst(),
                feedbacks.isLast());
    }

}
