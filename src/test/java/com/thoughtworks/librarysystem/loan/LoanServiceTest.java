package com.thoughtworks.librarysystem.loan;

import com.thoughtworks.librarysystem.book.BookRepository;
import com.thoughtworks.librarysystem.commons.factories.CopyFactory;
import com.thoughtworks.librarysystem.copy.Copy;
import com.thoughtworks.librarysystem.copy.CopyRepository;
import com.thoughtworks.librarysystem.copy.CopyStatus;
import com.thoughtworks.librarysystem.loan.exceptions.CopyIsNotAvailableException;
import com.thoughtworks.librarysystem.loan.exceptions.LoanNotExistsException;
import com.thoughtworks.librarysystem.loan.exceptions.UserNotFoundException;
import com.thoughtworks.librarysystem.user.User;
import com.thoughtworks.librarysystem.user.UserRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.sql.Date;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class LoanServiceTest {

    @Mock
    CopyRepository copyRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    LoanRepository loanRepository;

    @Mock
    BookRepository bookRepository;

    @InjectMocks
    LoanService service;

    private final String USER_EMAIL = "some@email.com";
    private final int LOAN_ID = 9;

    private User user;
    private Copy copy;
    private Loan loan;
    List<User> userList;

    @Before
    public void setUp() throws Exception {
        user = new User();
        user.setEmail(USER_EMAIL);

        copy = new CopyFactory().createStandardCopyWithSameIsbnAndLibrary();
        copy.setId(1);
        copy.setStatus(CopyStatus.AVAILABLE);

        List<Copy> copyList = Arrays.asList(copy);

        loan = new LoanBuilder()
                .withCopy(copy)
                .withUser(user)
                .build();

        userList = Arrays.asList(user);

        when(copyRepository.findOne(copy.getId())).thenReturn(copy);
        when(copyRepository.findDistinctCopiesByLibrarySlugAndBookIdAndStatus(copy.getLibrary().getSlug(), copy.getBook().getId(), CopyStatus.AVAILABLE)).thenReturn(copyList);
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(userList);
        when(loanRepository.findOne(LOAN_ID)).thenReturn(loan);
    }

    @Test
    public void shouldSetCopyStatusToBorrowed() throws Exception {
        service.borrowCopy(copy.getLibrary().getSlug(), copy.getBook().getId(), USER_EMAIL);
        assertThat(copy.getStatus(), is(CopyStatus.BORROWED));
    }

    @Test(expected = CopyIsNotAvailableException.class)
    public void shouldThrowExceptionWhenBorrowingAlreadyBorrowedCopy() throws Exception {
        List<Copy> emptyCopyList = Arrays.asList();
        when(copyRepository.findDistinctCopiesByLibrarySlugAndBookIdAndStatus(copy.getLibrary().getSlug(), copy.getBook().getId(), CopyStatus.AVAILABLE)).thenReturn(emptyCopyList);
        service.borrowCopy(copy.getLibrary().getSlug(), copy.getBook().getId(), USER_EMAIL);
    }

    @Test(expected = UserNotFoundException.class)
    public void shouldThrowExceptionWhenNoUserIsFound() throws Exception {
        copy.setStatus(CopyStatus.AVAILABLE);
        List<User> emptyUserList = Arrays.asList();
        when(userRepository.findByEmail(anyString())).thenReturn(emptyUserList);
        service.borrowCopy(copy.getLibrary().getSlug(), copy.getBook().getId(), USER_EMAIL);
    }

    @Test(expected = UserNotFoundException.class)
    public void shouldThrowExceptionWhenListOfUsersIsNull() throws Exception {
        List<User> nullUserList = null;
        when(userRepository.findByEmail(anyString())).thenReturn(nullUserList);
        service.borrowCopy(copy.getLibrary().getSlug(), copy.getBook().getId(), USER_EMAIL);
    }


    @Test
    public void shouldSetCopyStatusAsAvailableWhenCopyIsReturned() throws Exception {
        copy.setStatus(CopyStatus.BORROWED);
        loan.setId(LOAN_ID);
        service.returnCopy(LOAN_ID);
        assertThat(copy.getStatus(), is(CopyStatus.AVAILABLE));
    }

    @Test(expected = LoanNotExistsException.class)
    public void shouldThrowExceptionWhenLoanIsNotFound() throws Exception {
        when(loanRepository.findOne(LOAN_ID)).thenReturn(null);
        service.returnCopy(LOAN_ID);
    }

    @Test
    public void shouldSetLoanEndDateWhenReturningCopy() throws Exception {
        copy.setStatus(CopyStatus.BORROWED);
        Loan returnedLoan = service.returnCopy(LOAN_ID);
        Date expectedLoanEndDate = new Date(System.currentTimeMillis());

        assertThat(returnedLoan.getEndDate(), is((expectedLoanEndDate)));
    }

    @Test
    public void shouldSaveLoanWhenThereIsNoEndDate() throws Exception {
        copy.setStatus(CopyStatus.BORROWED);
        service.returnCopy(LOAN_ID);
        verify(loanRepository).save(loan);
    }

    @Test
    public void shouldNotSaveLoanWhenThereIsNoEndDateAndCopyIsReturned() throws Exception {
        Date loanEndDate = new Date(System.currentTimeMillis());

        loan.setEndDate(loanEndDate);
        copy.setStatus(CopyStatus.BORROWED);

        service.returnCopy(LOAN_ID);

        verify(loanRepository, times(0)).save(loan);
    }
}