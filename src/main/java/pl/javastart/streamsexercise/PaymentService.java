package pl.javastart.streamsexercise;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class PaymentService {

    private PaymentRepository paymentRepository;
    private DateTimeProvider dateTimeProvider;

    PaymentService(PaymentRepository paymentRepository, DateTimeProvider dateTimeProvider) {
        this.paymentRepository = paymentRepository;
        this.dateTimeProvider = dateTimeProvider;
    }

    /*
    Znajdź i zwróć płatności posortowane po dacie malejąco
     */
    List<Payment> findPaymentsSortedByDateDesc() {
        Stream<Payment> paymentStream = paymentRepository.findAll().stream();
        return paymentStream.sorted(new PaymentDateComparator()).toList();
    }

    /*
    Znajdź i zwróć płatności dla aktualnego miesiąca
     */
    List<Payment> findPaymentsForCurrentMonth() {
        YearMonth yearMonth = dateTimeProvider.yearMonthNow();
        return findPaymentsForGivenMonth(yearMonth);
    }

    /*
    Znajdź i zwróć płatności dla wskazanego miesiąca
     */
    List<Payment> findPaymentsForGivenMonth(YearMonth yearMonth) {
        List<Payment> paymentList = paymentRepository.findAll();
        return paymentList.stream().filter(getGivenMonthPaymentPredicate(yearMonth)).toList();
    }

    private static Predicate<Payment> getGivenMonthPaymentPredicate(YearMonth yearMonth) {
        return x -> x.getPaymentDate().getYear() == yearMonth.getYear()
                && x.getPaymentDate().getMonth().ordinal() == yearMonth.getMonth().ordinal();
    }

    /*
    Znajdź i zwróć płatności dla ostatnich X dzni
     */
    List<Payment> findPaymentsForGivenLastDays(int days) {
        Stream<Payment> paymentStream = paymentRepository.findAll().stream();
        ZonedDateTime zonedDateTimeNow = dateTimeProvider.zonedDateTimeNow();
        ZonedDateTime zonedDateTimeMinusDaysFromUser = zonedDateTimeNow.minusDays(days);
        return paymentStream
                .filter(x -> x.getPaymentDate().isAfter(zonedDateTimeMinusDaysFromUser))
                .toList();
    }

    /*
    Znajdź i zwróć płatności z jednym elementem
     */
    Set<Payment> findPaymentsWithOnePaymentItem() {
        return paymentRepository.findAll().stream()
                .filter(x -> x.getPaymentItems().size() == 1).collect(Collectors.toSet());
    }

    /*
    Znajdź i zwróć nazwy produktów sprzedanych w aktualnym miesiącu
     */
    Set<String> findProductsSoldInCurrentMonth() {
        Stream<Payment> paymentStream = findPaymentsForCurrentMonth().stream();
        Stream<PaymentItem> paymentItemStream = paymentStream.map(Payment::getPaymentItems)
                .flatMap(Collection::stream);
        return paymentItemStream.map(PaymentItem::getName).collect(Collectors.toSet());
    }

    /*
    Policz i zwróć sumę sprzedaży dla wskazanego miesiąca
     */
    BigDecimal sumTotalForGivenMonth(YearMonth yearMonth) {
        Stream<Payment> paymentStream = findPaymentsForGivenMonth(yearMonth).stream();
        Stream<PaymentItem> paymentItemStream = paymentStream
                .map(Payment::getPaymentItems)
                .flatMap(Collection::stream);
        Stream<BigDecimal> bigDecimalStream = paymentItemStream.map(PaymentItem::getFinalPrice);
        return bigDecimalStream
                .reduce(BigDecimal::add)
                .get();
    }

    /*
    Policz i zwróć sumę przeyznanaych rabatów dla wskazanego miesiąca
     */
    BigDecimal sumDiscountForGivenMonth(YearMonth yearMonth) {
        Stream<Payment> paymentStream1 = findPaymentsForGivenMonth(yearMonth).stream();
        Stream<PaymentItem> paymentItemStream = paymentStream1
                .map(Payment::getPaymentItems)
                .flatMap(Collection::stream);
        Stream<BigDecimal> bigDecimalStream = paymentItemStream.map(x ->
                x.getRegularPrice().subtract(x.getFinalPrice()));
        return bigDecimalStream
                .reduce(BigDecimal::add)
                .get();
    }

    /*
    Znajdź i zwróć płatności dla użytkownika z podanym mailem
     */
    List<PaymentItem> getPaymentsForUserWithEmail(String userEmail) {
        return paymentRepository.findAll().stream()
                .filter(x -> x.getUser().getEmail().equals(userEmail))
                .map(Payment::getPaymentItems)
                .flatMap(Collection::stream)
                .toList();
    }

    /*
    Znajdź i zwróć płatności, których wartość przekracza wskazaną granicę
     */
    Set<Payment> findPaymentsWithValueOver(int value) {
        return paymentRepository.findAll().stream()
                .filter(x -> sumItemPayments(x) > value)
                .collect(Collectors.toSet());
    }

    private int sumItemPayments(Payment payment) {
        return payment.getPaymentItems().stream()
                .map(PaymentItem::getFinalPrice)
                .reduce(BigDecimal::add)
                .get()
                .intValue();
    }
}
