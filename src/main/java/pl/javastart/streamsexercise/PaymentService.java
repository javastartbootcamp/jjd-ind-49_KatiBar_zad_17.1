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
        List<Payment> paymentList = paymentRepository.findAll();
        paymentList.sort(new PaymentDateComparator());
        return paymentList;
    }

    /*
    Znajdź i zwróć płatności dla aktualnego miesiąca
     */
    List<Payment> findPaymentsForCurrentMonth() {
        List<Payment> paymentList = paymentRepository.findAll();
        int yearNow = dateTimeProvider.zonedDateTimeNow().getYear();
        int monthNow = dateTimeProvider.yearMonthNow().getMonth().ordinal();
        return paymentList.stream()
                .filter(getCurrentMonthPaymentPredicate(yearNow, monthNow))
                .toList();

//        List<Payment> currentMonthPayments = new ArrayList<>();
//        for (Payment payment : paymentList) {
//            int paymentYear = payment.getPaymentDate().getYear();
//            int paymetMonth = payment.getPaymentDate().getMonth().ordinal();
//            if (paymentYear == yearNow && paymetMonth == monthNow) {
//                currentMonthPayments.add(payment);
//            }
//        }
//        return currentMonthPayments;
    }

    private static Predicate<Payment> getCurrentMonthPaymentPredicate(int yearNow, int monthNow) {
        return x ->
                x.getPaymentDate().getYear() == yearNow
                && x.getPaymentDate().getMonth().ordinal() == monthNow;
    }

    /*
    Znajdź i zwróć płatności dla wskazanego miesiąca
     */
    List<Payment> findPaymentsForGivenMonth(YearMonth yearMonth) {
        List<Payment> paymentList = paymentRepository.findAll();
        int yearFromUser = yearMonth.getYear();
        int monthFromUser = yearMonth.getMonth().ordinal();
        return paymentList.stream().filter(getGivenMonthPaymentPredicate(yearFromUser, monthFromUser)).toList();

//        List<Payment> givenMonthPayments = new ArrayList<>();
//        for (Payment payment : paymentList) {
//            int paymentYear = payment.getPaymentDate().getYear();
//            int paymetMonth = payment.getPaymentDate().getMonth().ordinal();
//            if (paymentYear == yearNow && paymetMonth == monthFromUser) {
//                givenMonthPayments.add(payment);
//            }
//        }
//        return givenMonthPayments;
    }

    private static Predicate<Payment> getGivenMonthPaymentPredicate(int yearNow, int monthFromUser) {
        return x -> x.getPaymentDate().getYear() == yearNow
                && x.getPaymentDate().getMonth().ordinal() == monthFromUser;
    }

    /*
    Znajdź i zwróć płatności dla ostatnich X dzni
     */
    List<Payment> findPaymentsForGivenLastDays(int days) {
        List<Payment> paymentList = paymentRepository.findAll();
        ZonedDateTime zonedDateTimeNow = dateTimeProvider.zonedDateTimeNow();
        ZonedDateTime zonedDateTimeMinusDaysFromUser = zonedDateTimeNow.minusDays(days);
        return paymentList.stream()
                .filter(x -> x.getPaymentDate().isAfter(zonedDateTimeMinusDaysFromUser))
                .toList();

//        List<Payment> givenLastDaysPayments = new ArrayList<>();
//        for (Payment payment : paymentList) {
//            ZonedDateTime paymentDate = payment.getPaymentDate();
//            if (paymentDate.isAfter(zonedDateTimeMinusDaysFromUser)) {
//                givenLastDaysPayments.add(payment);
//            }
//        }
//        return givenLastDaysPayments;
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
        List<Payment> paymentsForCurrentMonth = findPaymentsForCurrentMonth();
        List<String> names = new ArrayList<>();
        for (Payment payment : paymentsForCurrentMonth) {
            for (PaymentItem paymentItem : payment.getPaymentItems()) {
                String name = paymentItem.getName();
                names.add(name);
            }
        }
        return names.stream().collect(Collectors.toSet());
    }

    /*
    Policz i zwróć sumę sprzedaży dla wskazanego miesiąca
     */
    BigDecimal sumTotalForGivenMonth(YearMonth yearMonth) {
        Stream<Payment> paymentStream = paymentRepository.findAll().stream();
        Stream<Payment> paymentStreamInYearMonth = paymentStream.filter(x ->
                x.getPaymentDate().getYear() == yearMonth.getYear()
                        && x.getPaymentDate().getMonth() == yearMonth.getMonth());
        Stream<PaymentItem> paymentItemStream = paymentStreamInYearMonth
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
        Stream<Payment> paymentStream = paymentRepository.findAll().stream();
        Stream<Payment> paymentStreamInYearMonth = paymentStream.filter(x ->
                x.getPaymentDate().getYear() == yearMonth.getYear()
                        && x.getPaymentDate().getMonth() == yearMonth.getMonth());
        Stream<PaymentItem> paymentItemStream = paymentStreamInYearMonth
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
        Stream<Payment> paymentStream = paymentRepository.findAll().stream();
        return paymentStream.filter(x -> x.getUser().getEmail().equals(userEmail))
                .map(Payment::getPaymentItems)
                .flatMap(Collection::stream)
                .toList();
    }

    /*
    Znajdź i zwróć płatności, których wartość przekracza wskazaną granicę
     */
    Set<Payment> findPaymentsWithValueOver(int value) {
        List<Payment> paymentList = paymentRepository.findAll();
        Set<Payment> paymentsOverValue = new HashSet<>();
        BigDecimal sum = BigDecimal.valueOf(0);
        for (Payment payment : paymentList) {
            for (PaymentItem paymentItem : payment.getPaymentItems()) {
                sum = sum.add(paymentItem.getFinalPrice());
            }
            if (sum.intValue() > value) {
                paymentsOverValue.add(payment);
            }
            sum = BigDecimal.valueOf(0);
        }
        return paymentsOverValue;
    }
}
