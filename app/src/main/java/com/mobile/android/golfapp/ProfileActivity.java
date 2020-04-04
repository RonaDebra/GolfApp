package com.mobile.android.golfapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;


public class ProfileActivity extends AppCompatActivity {

    private EditText edtUsername;
    private TextView mEmailField;

    private CircleImageView mProfileImage;

    private FirebaseAuth mAuth;
    private DatabaseReference mCustomerDatabase;

    private String userID;
    private String mName;
    private String mUsername,mEmail;
    private String mProfileImageUrl;
    private Uri resultUri;
    private ProgressDialog dialog;
    private static final int REQUEST_READ_EXTERNAL = 1234;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getSupportActionBar().setElevation(0);
        setContentView(R.layout.activity_profile);

        mEmailField = findViewById(R.id.email);
        edtUsername = findViewById(R.id.usernameP);

        mProfileImage = findViewById(R.id.profileImage);

        dialog = new ProgressDialog(this);
        dialog.setMessage("Updating info...");

        mAuth = FirebaseAuth.getInstance();
        userID = mAuth.getCurrentUser().getUid();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Name, email address, and profile photo Url
            String name = user.getDisplayName();
            String email = user.getEmail();
            String phone = user.getPhoneNumber();
            Uri photoUrl = user.getPhotoUrl();
            if (user.getEmail()!=null) {
                mEmailField.setText(email);
            }
            if (user.getDisplayName()!=null) {
                edtUsername.setText(name);
            }
            if (user.getPhotoUrl()!=null) {
                mProfileImage.setImageURI(photoUrl);
            }
            // Check if user's email is verified
            boolean emailVerified = user.isEmailVerified();

            String uid = user.getUid();
        }

        mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(userID);

        getUserInfo();

        mProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermissions();
            }
        });


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){

            case REQUEST_READ_EXTERNAL:{

                if (grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                    Intent intent = new Intent(Intent.ACTION_PICK);
                    intent.setType("image/*");
                    startActivityForResult(intent, 1);


                }else {

                    Toast.makeText(getApplicationContext(), "Please grant read permission to update your profile image", Toast.LENGTH_LONG).show();
                    //ActivityCompat.requestPermissions(CustomerSettingsActivity.this, new String[]{READ_EXTERNAL_STORAGE}, REQUEST_READ_EXTERNAL);


                }

                return;
            }
        }
    }


    private void checkPermissions() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){

            if ((ContextCompat.checkSelfPermission(ProfileActivity.this, READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) ){

                // Permission is not granted


                ActivityCompat.requestPermissions(ProfileActivity.this, new String[]{READ_EXTERNAL_STORAGE }, REQUEST_READ_EXTERNAL);



            }else {

                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 1);

            }



        }else {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, 1);
        }
    }


    private void getUserInfo(){
        mCustomerDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();

                    if(map.get("email")!=null){
                        mEmail = map.get("email").toString();
                        mEmailField.setText(mEmail);
                        //mEmailField.setText(mName);
                    }

                    if(map.get("username")!=null){
                        mUsername = map.get("username").toString();
                        edtUsername.setText(mUsername);
                    }
                    if(map.get("profileImageUrl")!=null){
                        mProfileImageUrl = (String) map.get("profileImageUrl");

                        Picasso.get().load(mProfileImageUrl).networkPolicy(NetworkPolicy.OFFLINE)
                                .placeholder(R.drawable.userprof).into(mProfileImage, new Callback() {
                            @Override
                            public void onSuccess() {

                            }

                            @Override
                            public void onError(Exception e) {

                                Picasso.get().load(mProfileImageUrl).placeholder(R.drawable.userprof).into(mProfileImage);

                            }



                        });
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }



    private void saveUserInformation() {

        dialog.show();
        mUsername = edtUsername.getText().toString();

        if (mUsername == null){
            dialog.dismiss();
            Toast.makeText(this, "Empty Username", Toast.LENGTH_SHORT).show();
        }
        else if(mUsername.length()<3){
            dialog.dismiss();

            Toast.makeText(ProfileActivity.this,"Your Username is too short",
                    Toast.LENGTH_SHORT).show();

        }
        else {

            Map userInfo = new HashMap();
            //userInfo.put("name", mEmail);

            userInfo.put("username", mUsername);
            userInfo.put("email", mAuth.getCurrentUser().getEmail());
            mCustomerDatabase.updateChildren(userInfo).addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if (task.isSuccessful()){
                        dialog.dismiss();
                        Toast.makeText(ProfileActivity.this, "Updated", Toast.LENGTH_SHORT).show();
                    }else {
                        dialog.dismiss();
                        Toast.makeText(ProfileActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();

                    }

                }
            });


            if (resultUri != null) {
                dialog.show();
                dialog.setTitle("Updating profile pic...");
                final StorageReference filePath = FirebaseStorage.getInstance().getReference().child("profile_images").child(userID);
                Bitmap bitmap = null;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(), resultUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 20, baos);
                byte[] data = baos.toByteArray();
                final UploadTask uploadTask = filePath.putBytes(data);


                uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        //Uri url = taskSnapshot.getDownloadUrl();

                        final Map newImage = new HashMap();

                        filePath.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                            @Override
                            public void onComplete(@NonNull Task<Uri> task) {

                                newImage.put("profileImageUrl", task.getResult().toString());
                                mCustomerDatabase.updateChildren(newImage);
                                dialog.dismiss();

                                Toast.makeText(ProfileActivity.this,
                                        "Profile photo updated", Toast.LENGTH_SHORT).show();

                            }
                        });


                    }
                });
                uploadTask.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ProfileActivity.this, "Error: "+ e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1 && resultCode == Activity.RESULT_OK){
            final Uri imageUri = data.getData();
            resultUri = imageUri;
            mProfileImage.setImageURI(resultUri);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.profile, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_save) {

            saveUserInformation();

        }

        //noinspection SimplifiableIfStatement

        if (id == R.id.action_moreP) {
            new AlertDialog.Builder(ProfileActivity.this).setIcon(R.drawable.ic_exit_to_app_black)
                    .setTitle("Sign Out").setMessage("Are you sure you want to Sign Out?")
                    .setPositiveButton("SIGN OUT", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            AuthUI.getInstance()
                                    .signOut(ProfileActivity.this)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        public void onComplete(@NonNull Task<Void> task) {
                                            // user is now signed out
                                            Toast.makeText(ProfileActivity.this, "You were Signed Out Successfully", Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(ProfileActivity.this, SignInActivity.class));
                                            finish();
                                        }
                                    });


                        }
                    }).setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    dialog.dismiss();
                }
            }).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}


