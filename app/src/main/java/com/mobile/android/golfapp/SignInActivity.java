package com.mobile.android.golfapp;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseUserMetadata;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.Arrays;
import java.util.List;

public class SignInActivity extends AppCompatActivity {
    public static final int RC_SIGN_IN = 1997;
    private FirebaseAuth mAuth;
    private DatabaseReference mDtbUser;
    String user_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        mAuth = FirebaseAuth.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            user_id = mAuth.getCurrentUser().getUid();
            mDtbUser = FirebaseDatabase.getInstance()
                    .getReference().child("Users").child(user_id);
            mDtbUser.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    startActivity(new Intent(SignInActivity.this, MapsActivity.class));
                    finish();

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });


        }else {
            setContentView(R.layout.activity_sign_in);
            // Choose authentication providers
            List<AuthUI.IdpConfig> providers = Arrays.asList(
                    new AuthUI.IdpConfig.EmailBuilder().build());
            //new AuthUI.IdpConfig.PhoneBuilder().build());
            //new AuthUI.IdpConfig.GoogleBuilder().build());
            //new AuthUI.IdpConfig.FacebookBuilder().build(),
            //new AuthUI.IdpConfig.TwitterBuilder().build());

            // Create and launch sign-in intent
            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setAvailableProviders(providers)
                            .setTheme(R.style.AppTheme_NoActionBar)
                            .setLogo(R.mipmap.ic_launcher)
                            .build(),
                    RC_SIGN_IN);
        }



    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {

                // Successfully signed in

                FirebaseInstanceId.getInstance().getInstanceId()
                        .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                            @Override
                            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                                if (!task.isSuccessful()) {
                                    Toast.makeText(SignInActivity.this, "getInstanceId failed"+task.getException(), Toast.LENGTH_SHORT).show();

                                }else{

                                    String token = task.getResult().getToken();
                                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                    if (user!=null){
                                        FirebaseUserMetadata metadata = mAuth.getCurrentUser().getMetadata();
                                        if (metadata.getCreationTimestamp() == metadata.getLastSignInTimestamp()) {
                                            // The user is new, show them a fancy intro screen!
                                            // Get new Instance ID token
                                            DatabaseReference current_user_db = FirebaseDatabase.getInstance()
                                                    .getReference().child("Users").child(user.getUid());
                                            DatabaseReference not_db = FirebaseDatabase.getInstance()
                                                    .getReference().child("notificationTokens");
                                            String email = user.getEmail();
                                            String username = user.getDisplayName();
                                            current_user_db.child("device_token").setValue(token);
                                            current_user_db.child("email").setValue(email);
                                            current_user_db.child("username").setValue(username);
                                            not_db.child(token).setValue(true);
                                            startActivity(new Intent(SignInActivity.this, MapsActivity.class));
                                            finish();
                                        } else {
                                            // This is an existing user, show them a welcome back screen.

                                            DatabaseReference current_user_db = FirebaseDatabase.getInstance()
                                                    .getReference().child("Users").child(user.getUid());
                                            DatabaseReference not_db = FirebaseDatabase.getInstance()
                                                    .getReference().child("notificationTokens");
                                            String email = user.getEmail();
                                            String username = user.getDisplayName();
                                            current_user_db.child("device_token").setValue(token);
                                            current_user_db.child("email").setValue(email);
                                            //current_user_db.child("username").setValue(username);
                                            not_db.child(token).setValue(true);
                                            startActivity(new Intent(SignInActivity.this, MapsActivity.class));
                                            finish();
                                        }




                                        // Log and toast
                                        //String msg = getString(R.string.msg_token_fmt, token);
                                        //Toast.makeText(PhoneLoginActivity.this, ""+token, Toast.LENGTH_SHORT).show();

                                    }

                                }


                            }
                        });

            }

            // ...
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        startActivity(new Intent(SignInActivity.this, SignInActivity.class));
    }
}
