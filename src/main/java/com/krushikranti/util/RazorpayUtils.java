package com.krushikranti.util;

import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;

public final class RazorpayUtils {

    private RazorpayUtils() {
    }

    public static boolean verifySignature(String razorpayOrderId, String razorpayPaymentId,
            String razorpaySignature, String razorpayKeySecret) throws RazorpayException {
        JSONObject options = new JSONObject();
        options.put("razorpay_order_id", razorpayOrderId);
        options.put("razorpay_payment_id", razorpayPaymentId);
        options.put("razorpay_signature", razorpaySignature);
        return Utils.verifyPaymentSignature(options, razorpayKeySecret);
    }
}