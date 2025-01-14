package com.efficientgoose.book.book;

import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.efficientgoose.book.common.PageResponse;
import com.efficientgoose.book.exception.OperationNotPermittedException;
import com.efficientgoose.book.file.FileStorageService;
import com.efficientgoose.book.history.BookTransactionHistoryRepository;
import com.efficientgoose.book.history.BookTransactionHistory;
import com.efficientgoose.book.user.User;
import static com.efficientgoose.book.book.BookSpecification.withOwnerId;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookService {

	private final BookRepository bookRepository;
	private final BookTransactionHistoryRepository bookTransactionHistoryRepository;
	private final BookMapper bookMapper;
	private final FileStorageService fileStorageService;

	public Integer save(BookRequest request, Authentication connectedUser) {
		User user = ((User) connectedUser.getPrincipal());
		Book book = bookMapper.toBook(request);
		book.setOwner(user);
		return bookRepository.save(book).getId();
	}

	public BookResponse findById(Integer bookId) {
		return bookRepository.findById(bookId)
				.map(bookMapper::toBookResponse)
				.orElseThrow(() -> new EntityNotFoundException("No book found with ID: " + bookId));
	}

	public PageResponse<BookResponse> findAllBooks(int page, int size, Authentication connectedUser) {
		User user = ((User) connectedUser.getPrincipal());
		Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());

		Page<Book> books = bookRepository.findAllDisplayableBooks(pageable, user.getId());

		List<BookResponse> bookResponse = books.stream()
				.map(bookMapper::toBookResponse)
				.toList();

		return new PageResponse<>(
				bookResponse,
				books.getNumber(),
				books.getSize(),
				books.getTotalElements(),
				books.getTotalPages(),
				books.isFirst(),
				books.isLast());
	}

	public PageResponse<BookResponse> findAllBooksByOwner(int page, int size, Authentication connectedUser) {
		User user = ((User) connectedUser.getPrincipal());
		Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());

		Page<Book> books = bookRepository.findAll(withOwnerId(user.getId()), pageable);

		List<BookResponse> bookResponse = books.stream()
				.map(bookMapper::toBookResponse)
				.toList();

		return new PageResponse<>(
				bookResponse,
				books.getNumber(),
				books.getSize(),
				books.getTotalElements(),
				books.getTotalPages(),
				books.isFirst(),
				books.isLast());
	}

	public PageResponse<BorrowedBookResponse> findAllBorrowedBooks(int page, int size,
			Authentication connectedUser) {

		User user = ((User) connectedUser.getPrincipal());
		Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());

		Page<BookTransactionHistory> allBorrowedBooks = bookTransactionHistoryRepository.findAllBorrowedBooks(
				pageable,
				user.getId());

		List<BorrowedBookResponse> bookResponse = allBorrowedBooks.stream()
				.map(bookMapper::toBorrowedBookResponse)
				.toList();

		return new PageResponse<>(
				bookResponse,
				allBorrowedBooks.getNumber(),
				allBorrowedBooks.getSize(),
				allBorrowedBooks.getTotalElements(),
				allBorrowedBooks.getTotalPages(),
				allBorrowedBooks.isFirst(),
				allBorrowedBooks.isLast());
	}

	public PageResponse<BorrowedBookResponse> findAllReturnedBooks(int page, int size,
			Authentication connectedUser) {
		User user = ((User) connectedUser.getPrincipal());
		Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());

		Page<BookTransactionHistory> allBorrowedBooks = bookTransactionHistoryRepository.findAllReturnedBooks(
				pageable,
				user.getId());

		List<BorrowedBookResponse> bookResponse = allBorrowedBooks.stream()
				.map(bookMapper::toBorrowedBookResponse)
				.toList();

		return new PageResponse<>(
				bookResponse,
				allBorrowedBooks.getNumber(),
				allBorrowedBooks.getSize(),
				allBorrowedBooks.getTotalElements(),
				allBorrowedBooks.getTotalPages(),
				allBorrowedBooks.isFirst(),
				allBorrowedBooks.isLast());
	}

	public Integer updateShareableStatus(Integer bookId, Authentication connectedUser) {

		Book book = bookRepository.findById(bookId)
				.orElseThrow(() -> new EntityNotFoundException(
						"Book with id:" + bookId + " not found"));
		User user = ((User) connectedUser.getPrincipal());

		if (!Objects.equals(book.getOwner().getId(), user.getId())) {
			throw new OperationNotPermittedException(
					"Cannot update shareable status since you don't own the book");
		}

		book.setShareable((!book.isShareable()));
		bookRepository.save(book);

		return bookId;
	}

	public Integer updateArchivedStatus(Integer bookId, Authentication connectedUser) {

		Book book = bookRepository.findById(bookId)
				.orElseThrow(() -> new EntityNotFoundException(
						"Book with id:" + bookId + " not found"));
		User user = ((User) connectedUser.getPrincipal());

		if (!Objects.equals(book.getOwner().getId(), user.getId())) {
			throw new OperationNotPermittedException(
					"Cannot update archived status since you don't own the book");
		}

		book.setArchived((!book.isArchived()));
		bookRepository.save(book);

		return bookId;
	}

	public Integer borrowBook(Integer bookId, Authentication connectedUser) {
		Book book = bookRepository.findById(bookId)
				.orElseThrow(() -> new EntityNotFoundException(
						"Book with id:" + bookId + " not found"));

		if (book.isArchived() || !book.isShareable()) {
			throw new OperationNotPermittedException(
					"The requested book cannot be borrowed since it is either archived or not shareable");
		}

		User user = ((User) connectedUser.getPrincipal());

		if (Objects.equals(book.getOwner().getId(), user.getId())) {
			throw new OperationNotPermittedException("You cannot borrow your own book.");
		}

		final boolean isAlreadyBorrowed = bookTransactionHistoryRepository.isAlreadyBorrowedByUser(bookId,
				user.getId());

		if (isAlreadyBorrowed) {
			throw new OperationNotPermittedException("The requested book is already borrowed");
		}

		BookTransactionHistory bookTransactionHistory = BookTransactionHistory.builder()
				.user(user)
				.book(book)
				.returned(false)
				.returnApproved(false)
				.build();

		return bookTransactionHistoryRepository.save(bookTransactionHistory).getId();
	}

	public Integer returnBorrowedBook(Integer bookId, Authentication connectedUser) {

		Book book = bookRepository.findById(bookId)
				.orElseThrow(() -> new EntityNotFoundException(
						"Book with id:" + bookId + " not found"));

		if (book.isArchived() || !book.isShareable()) {
			throw new OperationNotPermittedException(
					"The requested book cannot be borrowed since it is either archived or not shareable");
		}

		User user = ((User) connectedUser.getPrincipal());

		if (Objects.equals(book.getOwner().getId(), user.getId())) {
			throw new OperationNotPermittedException("You cannot borrow  or return your own book.");
		}

		BookTransactionHistory bookTransactionHistory = bookTransactionHistoryRepository
				.findByBookIdAndUserId(bookId,
						user.getId())
				.orElseThrow(() -> new OperationNotPermittedException(
						"You haven't borrowed the book so it cannot be returned"));

		bookTransactionHistory.setReturned(true);

		return bookTransactionHistoryRepository.save(bookTransactionHistory).getId();
	}

	public Integer approveReturnBorrowedBook(Integer bookId, Authentication connectedUser) {

		Book book = bookRepository.findById(bookId)
				.orElseThrow(() -> new EntityNotFoundException(
						"Book with id:" + bookId + " not found"));

		if (book.isArchived() || !book.isShareable()) {
			throw new OperationNotPermittedException(
					"The requested book cannot be borrowed since it is either archived or not shareable");
		}

		User user = ((User) connectedUser.getPrincipal());

		if (Objects.equals(book.getOwner().getId(), user.getId())) {
			throw new OperationNotPermittedException("You cannot borrow  or return your own book.");
		}

		BookTransactionHistory bookTransactionHistory = bookTransactionHistoryRepository
				.findByBookIdAndOwnerId(bookId,
						user.getId())
				.orElseThrow(() -> new OperationNotPermittedException(
						"The book is not returned yet. You cannot approve its return"));

		bookTransactionHistory.setReturnApproved(true);

		return bookTransactionHistoryRepository.save(bookTransactionHistory).getId();
	}

	public void uploadBookCoverPicture(MultipartFile file, Authentication connectedUser, Integer bookId) {
		Book book = bookRepository.findById(bookId)
				.orElseThrow(() -> new EntityNotFoundException(
						"Book with id:" + bookId + " not found"));

		User user = ((User) connectedUser.getPrincipal());

		var bookCover = fileStorageService.saveFile(file, user.getId());
		book.setBookCover(bookCover);

		bookRepository.save(book);

	}

}
