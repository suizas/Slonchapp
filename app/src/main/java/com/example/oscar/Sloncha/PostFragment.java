package com.example.oscar.Sloncha;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import me.srodrigo.androidhintspinner.HintAdapter;
import me.srodrigo.androidhintspinner.HintSpinner;

import static android.app.Activity.RESULT_OK;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link PostFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link PostFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PostFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 555;
    private static final int CAMERA_REQUEST = 666;
    private static final int MY_PERMISSIONS_REQUEST_STORAGE = 777;
    private static final int STORAGE_REQUEST = 888;
    private static final int PICK_IMAGE = 111;
    private static String CategorieChoosen = null;
    // TODO: Rename and change types of parameters
    private ImageButton mSelectCamera;
    private ImageButton mSelectGallery;
    private ImageView mSelectedImage;
    private EditText mPostTitle;
    private EditText mPostDesc;
    private FirebaseAuth mAuth;
    private Button mSubmitButton;
    private Uri mImageUri = null;
    private Bitmap mCropedImage = null;
    private ProgressDialog mProgress;
    private Spinner spinner;

    private StorageReference mStorage;
    private DatabaseReference mDatabase;

    private OnFragmentInteractionListener mListener;

    public PostFragment() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters
    public static PostFragment newInstance(String param1, String param2) {
        PostFragment fragment = new PostFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_new_post, container, false);
        List l = new ArrayList();
        l.add("People");
        l.add("Party");
        l.add("Photography");
        l.add("Food");
        l.add("Travel");
        l.add("Games");
        l.add("Animals");
        mSelectedImage = (ImageView) view.findViewById(R.id.image_frame);
        mPostTitle = (EditText) view.findViewById(R.id.editText_title_news);
        mPostDesc = (EditText) view.findViewById(R.id.editText_description_news);
        mSubmitButton = (Button) view.findViewById(R.id.button_submit_post);
        mAuth = FirebaseAuth.getInstance();
        spinner = (Spinner) view.findViewById(R.id.spinner_categories);
        HintSpinner<String> hintSpinner = new HintSpinner<>(
                spinner,
                // Default layout - You don't need to pass in any layout id, just your hint text and
                // your list data
                new HintAdapter<String>(getContext(), "Choose a categorie", l),
                new HintSpinner.Callback<String>() {
                    @Override
                    public void onItemSelected(int position, String itemAtPosition) {
                        // Here you handle the on item selected event (this skips the hint selected event)
                        CategorieChoosen = itemAtPosition;
                        Toast.makeText(getContext(), itemAtPosition, Toast.LENGTH_SHORT).show();
                    }
                });
        hintSpinner.init();

        mStorage = FirebaseStorage.getInstance().getReference();
        mDatabase = FirebaseDatabase.getInstance().getReference().child("Posts");
        mProgress = new ProgressDialog(getContext());
        mSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startPosting();
            }

        });

        mSelectGallery = (ImageButton) view.findViewById(R.id.button_gallery);
        mSelectGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (ContextCompat.checkSelfPermission(getContext(),
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {

                    // Should we show an explanation?
                    if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                        AlertDialog alertDialog = new AlertDialog.Builder(getContext()).create();
                        alertDialog.setTitle("Permission Required");
                        alertDialog.setMessage("The app need permission to access the gallery");
                        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        ActivityCompat.requestPermissions(getActivity(),
                                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                                MY_PERMISSIONS_REQUEST_STORAGE);
                                    }
                                });

                        alertDialog.show();

                    } else {

                        // No explanation needed, we can request the permission.

                        ActivityCompat.requestPermissions(getActivity(),
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                MY_PERMISSIONS_REQUEST_STORAGE);
                        openGallery();
                    }
                } else {
                    openGallery();
                }
            }
        });

        mSelectCamera = (ImageButton) view.findViewById(R.id.button_camera);
        mSelectCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(getContext(),
                        Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {

                    // Should we show an explanation?
                    if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                            Manifest.permission.CAMERA)) {

                        AlertDialog alertDialog = new AlertDialog.Builder(getContext()).create();
                        alertDialog.setTitle("Permission Required");
                        alertDialog.setMessage("The app need permission to access the camera");
                        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        ActivityCompat.requestPermissions(getActivity(),
                                                new String[]{Manifest.permission.CAMERA},
                                                MY_PERMISSIONS_REQUEST_CAMERA);

                                    }
                                });

                        alertDialog.show();

                    } else {

                        // No explanation needed, we can request the permission.

                        ActivityCompat.requestPermissions(getActivity(),
                                new String[]{Manifest.permission.CAMERA},
                                MY_PERMISSIONS_REQUEST_CAMERA);


                        // MY_PERMISSIONS_REQUEST_CAMERA is an
                        // app-defined int constant. The callback method gets the
                        // result of the request.
                    }
                }

                if (ContextCompat.checkSelfPermission(getContext(),
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {

                    // Should we show an explanation?
                    if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                        AlertDialog alertDialog = new AlertDialog.Builder(getContext()).create();
                        alertDialog.setTitle("Permission Required");
                        alertDialog.setMessage("The app need permission to access the camera");
                        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        ActivityCompat.requestPermissions(getActivity(),
                                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                                MY_PERMISSIONS_REQUEST_STORAGE);

                                    }
                                });

                        alertDialog.show();

                    } else {

                        // No explanation needed, we can request the permission.

                        ActivityCompat.requestPermissions(getActivity(),
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                MY_PERMISSIONS_REQUEST_STORAGE);
                        openCamera();
                    }
                } else {
                    openCamera();
                }

            }
        });
        return view;
    }

    private void openGallery() {
        try {
            Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
            startActivityForResult(gallery, PICK_IMAGE);
        } catch (Exception error) {
            System.out.println(error.toString());
        }

    }


    private void startPosting() {
        mProgress.setMessage("Posting ...");
        mProgress.setCancelable(false);
        mProgress.show();

        final String title_val = mPostTitle.getText().toString().trim();
        final String desc_val = mPostDesc.getText().toString().trim();
        if (!TextUtils.isEmpty(title_val) && !TextUtils.isDigitsOnly(desc_val) && mImageUri != null && CategorieChoosen != null) {

            StorageReference filePath = mStorage.child("Post_Images").child(mImageUri.getLastPathSegment());

            filePath.putFile(mImageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Uri downloadUrl = taskSnapshot.getDownloadUrl();
                    DatabaseReference newPost = mDatabase.push();
                    newPost.child("userId").setValue(mAuth.getCurrentUser().getUid());
                    newPost.child("title").setValue(title_val);
                    newPost.child("desc").setValue(desc_val);
                    newPost.child("image").setValue(downloadUrl.toString());
                    newPost.child("categorie").setValue(CategorieChoosen);

                    Calendar calendar = new GregorianCalendar();

                    int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY); // 24 hour clock
                    int minute = calendar.get(Calendar.MINUTE);
                    int second = calendar.get(Calendar.SECOND);

                    int year = calendar.get(Calendar.YEAR);
                    int month = calendar.get(Calendar.MONTH) + 1; // Jan = 0, dec = 11
                    int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

                    newPost.child("date").setValue(dayOfMonth + "/" + month + "/" + year + "\t" + hourOfDay + ":" + minute + ":" + second);
                    mProgress.dismiss();
                    Toast.makeText(getContext(), "Correctly posted", Toast.LENGTH_LONG).show();

                    Intent i = new Intent(getContext(), MainActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                }
            });

        } else {
            mProgress.dismiss();
            Toast.makeText(getContext(), "Error try again", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
//                    openCamera();

                } else {

                    Toast.makeText(getContext(), "Permission denied", Toast.LENGTH_LONG).show();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            case MY_PERMISSIONS_REQUEST_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    //     openCamera();

                } else {

                    Toast.makeText(getContext(), "Permission denied", Toast.LENGTH_LONG).show();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            mImageUri = data.getData();

            CropImage.activity(mImageUri)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(1, 1)
                    .start(getContext(), this);

        }

        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
            mImageUri = data.getData();

            CropImage.activity(mImageUri)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(1, 1)
                    .start(getContext(), this);
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {

            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();

                mImageUri = resultUri;

                Picasso.with(getContext())
                        .load(resultUri)
                        .resize(300, 300)
                        .into(mSelectedImage);

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                Toast.makeText(getContext(), error.toString(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void openCamera() {
        try {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(cameraIntent, CAMERA_REQUEST);
        } catch (Exception error) {
            System.out.println(error.toString());
        }
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
