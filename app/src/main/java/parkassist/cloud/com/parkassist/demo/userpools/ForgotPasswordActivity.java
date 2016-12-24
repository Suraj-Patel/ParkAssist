//
// Copyright 2016 Amazon.com, Inc. or its affiliates (Amazon). All Rights Reserved.
//
// Code generated by AWS Mobile Hub. Amazon gives unlimited permission to 
// copy, distribute and modify it.
//
// Source code generated from template: aws-my-sample-app-android v0.13
//
package parkassist.cloud.com.parkassist.demo.userpools;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.amazonaws.mobile.user.signin.CognitoUserPoolsSignInProvider;
import parkassist.cloud.com.parkassist.R;
import parkassist.cloud.com.parkassist.util.ViewHelper;

/**
 * Activity to prompt for a new password along with the verification code.
 */
public class ForgotPasswordActivity extends Activity {
    /** Log tag. */
    private static final String LOG_TAG = ForgotPasswordActivity.class.getSimpleName();

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
    }

    /**
     * Retrieve input and return to caller.
     * @param view the Android View
     */
    public void forgotPassword(final View view) {
        final String password =
                ViewHelper.getStringValue(this, R.id.forgot_password_password);
        final String verificationCode =
                ViewHelper.getStringValue(this, R.id.forgot_password_verification_code);

        Log.d(LOG_TAG, "verificationCode = " + verificationCode);

        final Intent intent = new Intent();
        intent.putExtra(CognitoUserPoolsSignInProvider.AttributeKeys.PASSWORD, password);
        intent.putExtra(CognitoUserPoolsSignInProvider.AttributeKeys.VERIFICATION_CODE, verificationCode);

        setResult(RESULT_OK, intent);

        finish();
    }
}
