package tw.com.flag.diaryya;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import com.google.android.gms.tasks.OnCompleteListener;
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

public class PostActivity extends AppCompatActivity {

    private ImageButton img_select;

    private EditText txv_title;
    private EditText txv_content;

    private Button btn_post;

    private ProgressDialog mProgress;

    private StorageReference mStorage;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private FirebaseUser mCurrentUser;
    private DatabaseReference mDatabaseUsers;

    private Uri imageUri=null;

    private static final int GALLERY_REQUEST=1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        mStorage= FirebaseStorage.getInstance().getReference();
        mDatabase=FirebaseDatabase.getInstance().getReference().child("Blog");
        mAuth=FirebaseAuth.getInstance();
        mCurrentUser=mAuth.getCurrentUser();
        if(FirebaseDatabase.getInstance().getReference().child("Users").child(mCurrentUser.getUid())!=null)
            mDatabaseUsers=FirebaseDatabase.getInstance().getReference().child("Users").child(mCurrentUser.getUid());

        mProgress=new ProgressDialog(this);

        txv_content=(EditText) findViewById(R.id.txv_content);
        txv_title=(EditText) findViewById(R.id.txv_title);

        btn_post=(Button) findViewById(R.id.btn_post);

        img_select=(ImageButton) findViewById(R.id.img_select);
        img_select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent,GALLERY_REQUEST);
            }
        });
        btn_post.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startPosting();
            }
        });
    }

    private void startPosting() {
        mProgress.setMessage("Posting");
        final String title_val=txv_title.getText().toString().trim();
        final String content_val=txv_content.getText().toString().trim();

        if(!TextUtils.isEmpty(title_val) && !TextUtils.isEmpty(content_val) && imageUri!=null){

            mProgress.show();
            StorageReference filepath=mStorage.child("Blog_Images").child(imageUri.getLastPathSegment());
            filepath.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    final Uri downloadUrl = taskSnapshot.getDownloadUrl();
                    final DatabaseReference newPost = mDatabase.push();

                    mDatabaseUsers.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            newPost.child("title").setValue(title_val);
                            newPost.child("content").setValue(content_val);
                            newPost.child("image").setValue(downloadUrl.toString());
                            newPost.child("uid").setValue(mCurrentUser.getUid());
                            newPost.child("username").setValue(dataSnapshot.child("name").getValue()).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(task.isSuccessful()){
                                        startActivity(new Intent(PostActivity.this,MainActivity.class));
                                    }
                                }
                            });
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                    mProgress.dismiss();
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==GALLERY_REQUEST && resultCode==RESULT_OK){
            imageUri=data.getData();
            img_select.setImageURI(imageUri);
        }
    }
}
