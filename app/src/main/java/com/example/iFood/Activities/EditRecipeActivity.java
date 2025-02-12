package com.example.iFood.Activities;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.example.iFood.R;
import com.example.iFood.Utils.ConnectionBCR;
import com.example.iFood.Utils.EditItemImage;
import com.example.iFood.Utils.FileUtils;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.example.iFood.Activities.Add_Recipe.addRecipe_New.featureList;
import static com.example.iFood.Activities.Add_Recipe.addRecipe_New.recipeFeature;

public class EditRecipeActivity extends AppCompatActivity {
    ConnectionBCR bcr = new ConnectionBCR();
    private EditText etTitle,etIngredients,etContent;
    private TextView tvRecipe_features,tvRecipe_type;
    private ImageView ivRecipeImage;
    private Button btnSave;
    private String recipeID,recipeImageURL,userName,userRole,recipeTitle,recipeIngredients,recipeInstructions,newValue,recipe_Type,recipe_Feature;
    private ProgressDialog progressDialog;
    List<String> edit_featureList = new ArrayList<>();
    List<String> edit_TypeList = new ArrayList<>();
    // Camera Handling
    private EditItemImage mEditItemImage;
    private Bitmap imageBitmap;

    private final FirebaseStorage mStorage = FirebaseStorage.getInstance();
    private final DatabaseReference recipesRef = FirebaseDatabase.getInstance().getReference().child("Recipes");
    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_recipe);

        // Set Variables
        initUiViews();

        // pull information from Intent
        initIntentData();

        // set the image into the Image view
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setMessage("Loading..");
        progressDialog.show();

        Handler handler = new Handler();
        handler.postDelayed(() -> runOnUiThread(() -> {
            Picasso.get().load(recipeImageURL).into(ivRecipeImage);
            progressDialog.dismiss();
        }),2000);
      //  Log.w("TAG","recipeID:"+recipeID+", username:"+userName);

        btnSave.setOnClickListener(v -> {
            if(etContent.getText().toString().isEmpty() ||
               etIngredients.getText().toString().isEmpty() ||
               etTitle.getText().toString().isEmpty() ||
               Objects.equals(ivRecipeImage.getDrawable().getConstantState(), getResources().getDrawable(R.drawable.no_image).getConstantState())){
                Toast.makeText(EditRecipeActivity.this,"Make sure you filled everything!",Toast.LENGTH_SHORT).show();
            }else{
                // Save the new information of the recipe
                ProgressDialog progressDialog = new ProgressDialog(EditRecipeActivity.this);
                progressDialog.setMessage("Applying Changes..");
                progressDialog.show();
                progressDialog.setCanceledOnTouchOutside(false);
                recipeTitle = etTitle.getText().toString();
                recipeIngredients = etIngredients.getText().toString();
                recipeInstructions = etContent.getText().toString();
                recipe_Feature = tvRecipe_features.getText().toString();
                recipe_Type = tvRecipe_type.getText().toString();
                // Log.w("TAG","Title:"+recipeTitle+", Ingredients:"+recipeIngredients+", Instructions:"+recipeInstructions);

                // if Image is not null then need to upload new image to Storage and delete the old one
                if(imageBitmap!=null){
                    StorageReference photoRef = mStorage.getReferenceFromUrl(recipeImageURL);
                    String recipePhotoName = photoRef.getName();
                   // Log.w("TAG","recipePhotoName: Photos/"+recipePhotoName);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    final byte[] data = baos.toByteArray();
                    final UploadTask uploadTask = photoRef.child("Photos/"+recipePhotoName).putBytes(data);
                    uploadTask.addOnFailureListener(e -> {

                    }).addOnSuccessListener(taskSnapshot -> {
                        if(taskSnapshot.getMetadata()!=null){
                            if(taskSnapshot.getMetadata().getReference()!=null){
                                Task<Uri> result = taskSnapshot.getStorage().getDownloadUrl();
                                result.addOnSuccessListener(uri -> {
                                    //Log.w("TAG","old value:"+recipeImageURL);
                                    newValue = uri.toString();
                                    //Log.w("TAG","new value:"+newValue);

                        String storageUrl = recipeImageURL;
                        StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(storageUrl);
                        storageReference.delete().addOnSuccessListener(aVoid -> {
                            // File deleted successfully so now change to the new file in main DB and update other values the user changed
                            recipesRef.child(recipeID).child(userName).child("recipeName").setValue(recipeTitle);
                            recipesRef.child(recipeID).child(userName).child("recipeIngredients").setValue(recipeIngredients);
                            recipesRef.child(recipeID).child(userName).child("recipe").setValue(recipeInstructions);
                            recipesRef.child(recipeID).child(userName).child("recipePicture").setValue(newValue);
                            recipesRef.child(recipeID).child(userName).child("feature").setValue(recipe_Feature);
                            recipesRef.child(recipeID).child(userName).child("type").setValue(recipe_Type);
                        }).addOnFailureListener(exception -> {
                            // Couldn't delete file so didn't update anything.
                            Toast.makeText(EditRecipeActivity.this, "Please try again", Toast.LENGTH_SHORT).show();
                        });

                                }).addOnFailureListener(e ->
                                        Log.w("TAG","Error:"+e.getMessage())
                                );
                                progressDialog.dismiss();
                                finish();
                            }
                        }
                    });
                }else{
                    recipesRef.child(recipeID).child(userName).child("feature").setValue(recipe_Feature);
                    recipesRef.child(recipeID).child(userName).child("type").setValue(recipe_Type);
                    recipesRef.child(""+recipeID).child(userName).child("recipeName").setValue(recipeTitle);
                    recipesRef.child(""+recipeID).child(userName).child("recipeIngredients").setValue(recipeIngredients);
                    recipesRef.child(""+recipeID).child(userName).child("recipe").setValue(recipeInstructions);
                    progressDialog.dismiss();
                    finish();
                }
            }
        });
        ivRecipeImage.setOnClickListener(v -> mEditItemImage.openDialog());
        tvRecipe_type.setOnClickListener(v -> {
         //   Log.d("TAG","clicked tvRecipe_type");
            AlertDialog.Builder builder = new AlertDialog.Builder(EditRecipeActivity.this);
            builder.setTitle("Select Recipe Features:");
            builder.setMultiChoiceItems(R.array.recipeType, null, (dialog, which, isChecked) -> {

                String[] arr =getResources().getStringArray(R.array.recipeType);
                if(isChecked){
                    edit_TypeList.add(arr[which]);
                }else if(edit_TypeList.contains(arr[which])){
                    edit_TypeList.remove(arr[which]);
                }
            });
            builder.setPositiveButton("Submit", (dialog, which) -> {
                String data="";
                int i = 0;
                for(String item:edit_TypeList){
                    if(i++ == edit_TypeList.size() - 1){
                        data = data + item;
                    }else {
                        data = data + item + ",";
                    }

                }
                recipe_Type = data;
                tvRecipe_type.setText(data);
                edit_TypeList.clear();
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
            builder.create();
            builder.show();
        });
        tvRecipe_features.setOnClickListener(v -> {
          //  Log.d("TAG","clicked tvRecipe_features");
            AlertDialog.Builder builder = new AlertDialog.Builder(EditRecipeActivity.this);
            builder.setTitle("Select Recipe Features:");
            builder.setMultiChoiceItems(R.array.recipeFeatures, null, (dialog, which, isChecked) -> {

                String[] arr =getResources().getStringArray(R.array.recipeFeatures);
                if(isChecked){
                    edit_featureList.add(arr[which]);
                }else if(edit_featureList.contains(arr[which])){
                    edit_featureList.remove(arr[which]);
                }
            });
            builder.setPositiveButton("Submit", (dialog, which) -> {
                String data="";
                int i = 0;
                for(String item:edit_featureList){
                    if(i++ == edit_featureList.size() - 1){
                        data = data + item;
                    }else {
                        data = data + item + ",";
                    }

                }
                recipe_Feature = data;
                tvRecipe_features.setText(data);
                edit_featureList.clear();
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
            builder.create();
            builder.show();
        });
    } // onCreate ends

    private void initIntentData() {
        recipeImageURL = getIntent().getStringExtra("Thumbnail");
        recipeID = getIntent().getStringExtra("recipeID");
        userName = getIntent().getStringExtra("userName");
        userRole = getIntent().getStringExtra("userRole");
        recipe_Type = getIntent().getStringExtra("recipeType");
        recipe_Feature = getIntent().getStringExtra("recipeFeature");
        etTitle.setText(getIntent().getStringExtra("RecipeName"));
        etIngredients.setText(getIntent().getStringExtra("RecipeIngredients"));
        etContent.setText(getIntent().getStringExtra("Recipe"));
        tvRecipe_type.setText(recipe_Type);
        tvRecipe_features.setText(recipe_Feature);
    }

    private void initUiViews() {
        tvRecipe_features = findViewById(R.id.edit_recipeFeatureSelection);
        tvRecipe_type = findViewById(R.id.edit_recipeType);
        progressDialog = new ProgressDialog(EditRecipeActivity.this);
        mEditItemImage = new EditItemImage(EditRecipeActivity.this);
        etContent = findViewById(R.id.recipeContent);
        etIngredients = findViewById(R.id.recipeIngredients);
        etTitle = findViewById(R.id.recipeTitle);
        ivRecipeImage = findViewById(R.id.recipeImage);
        btnSave = findViewById(R.id.btnSave);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
       // Log.i("RESULT_OK","RESULT_OK:"+RESULT_OK);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                default:
                case EditItemImage.TAKE_PICTURE:
                case EditItemImage.PICK_IMAGE:
                    setImage(requestCode, data);
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    public void setImage(int requestCode, Intent data) {
        String mPath = EditItemImage.mPath;
        switch (requestCode) {
            case EditItemImage.TAKE_PICTURE:
                FileUtils.addMediaToGallery(this, mPath);
                imageBitmap = BitmapFactory.decodeFile(mPath);

                try {
                    ExifInterface exifInterface = new ExifInterface(mPath);
                    int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
                    switch (orientation) {

                        case ExifInterface.ORIENTATION_ROTATE_90:
                            imageBitmap = rotateImage(imageBitmap, 90);
                            break;

                        case ExifInterface.ORIENTATION_ROTATE_180:
                            imageBitmap = rotateImage(imageBitmap, 180);
                            break;

                        case ExifInterface.ORIENTATION_ROTATE_270:
                            imageBitmap = rotateImage(imageBitmap, 270);
                            break;

                        case ExifInterface.ORIENTATION_NORMAL:
                            // do nothing
                            break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                compressImage();
                ivRecipeImage.setImageBitmap(imageBitmap);

                break;

            case EditItemImage.PICK_IMAGE:
                Uri selectedImage = data.getData();
                if (selectedImage != null) {
                    try {
                        EditItemImage.mPath = FileUtils.getPath(this, selectedImage);
                        int orientation = FileUtils.getOrientation(this, selectedImage);
                        InputStream inputStream = getContentResolver().openInputStream(selectedImage);
                        imageBitmap = BitmapFactory.decodeStream(inputStream);
                        imageBitmap = rotateImage(imageBitmap, orientation);
                        ivRecipeImage.setImageBitmap(imageBitmap);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }

    /**
     *
     * @param source the given Image
     * @param angle the current image angle
     * @return rotated image
     */
    public Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }
    /**
     * Function checking for permission and if there is none, asking for them ( camera related )
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        for (Fragment fragment : fragments) {
            fragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case EditItemImage.TAKE_PICTURE:
                    mEditItemImage.openCamera();
                    break;
                case EditItemImage.PICK_IMAGE:
                    mEditItemImage.openGallery();
                    break;
            }
        }
    }
    public void clearImage() {
        ivRecipeImage.setImageResource(R.drawable.no_image);
    }
    /**
     * This function is responsible to reduce file size of image taken ( when taken pictures with phone, the image size could be up tp 8MB and we want to reduced
     * file size to reduce loading time / uploading time.
     */
    private void compressImage() {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);//Compression quality, here 100 means no compression, the storage of compressed data to baos
        int options = 90;
        while (baos.toByteArray().length / 1024 > 400) {  //Loop if compressed picture is greater than 400kb, than to compression
            baos.reset();//Reset baos is empty baos
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, options, baos);//The compression options%, storing the compressed data to the baos
            options -= 10;//Every time reduced by 10
        }
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());//The storage of compressed data in the baos to ByteArrayInputStream
        imageBitmap = BitmapFactory.decodeStream(isBm, null, null);//The ByteArrayInputStream data generation
    }

    /**
     * Register our Broadcast Receiver when opening the app.
     */
    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(bcr,filter);
    }
    /**
     * Stop our Broadcast Receiver when the app is closed.
     */
    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(bcr);
    }
}