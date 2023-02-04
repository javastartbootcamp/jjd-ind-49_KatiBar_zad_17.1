package pl.javastart.streamsexercise;

import java.util.Comparator;

public class PaymentDateComparator implements Comparator<Payment> {

    @Override
    public int compare(Payment o1, Payment o2) {
        if (o1.getPaymentDate().isAfter(o2.getPaymentDate())) {
            return -1;
        } else if ((o1.getPaymentDate().isBefore(o2.getPaymentDate()))) {
            return 1;
        } else {
            return 0;
        }
    }
}
