package com.abjt.melnotes.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.abjt.melnotes.R;
import com.abjt.melnotes.database.NoteDatabase;
import com.abjt.melnotes.entities.Note;
import com.abjt.melnotes.utilities.Constants;
import com.abjt.melnotes.utilities.Toaster;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class CreateNoteActivity extends AppCompatActivity {

    //Views
    private EditText inputNoteTitle, inputNoteSubtitle, inputNoteText;
    private TextView textDateTime;
    private ImageView imageBack;
    private TextView imageSave;
    private View subtitleIndicator;
    private ImageView imageNote;
    private TextView textWebURL;
    private LinearLayout layoutWebURL;
    private ImageView removeImageNote;
    private ImageView removeWebUrl;

    //Utils
    private Toaster toaster;

    //variables
    private String selectedNoteColor;
    private String selectedImagePath;
    private Note availableNote;

    //Dialog
    private AlertDialog dialogAddUrl;
    private AlertDialog dialogDeleteNote;

    //BottomSheet
    BottomSheetBehavior<LinearLayout> bottomSheetBehavior;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_note);
        init();
        setListeners();
        initialiseMiscellaneous();
        setSubtitleIndicatorColor();
    }

    private void init() {
        toaster = new Toaster(getApplicationContext());

        imageBack = findViewById(R.id.imageBack);
        imageSave = findViewById(R.id.imageSave);
        inputNoteTitle = findViewById(R.id.inputNoteTitle);
        inputNoteSubtitle = findViewById(R.id.inputNoteSubtitle);
        inputNoteText = findViewById(R.id.inputNote);
        textDateTime = findViewById(R.id.textDateTime);
        subtitleIndicator = findViewById(R.id.viewSubtitleIndicator);
        imageNote = findViewById(R.id.imageNote);
        textWebURL = findViewById(R.id.textWebURL);
        layoutWebURL = findViewById(R.id.layoutWebURL);
        removeImageNote = findViewById(R.id.imageRemoveImage);
        removeWebUrl = findViewById(R.id.imageRemoveURL);

        textDateTime.setText(
                new SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm a", Locale.getDefault())
                        .format(new Date())
        );

        selectedNoteColor = "#333333";
        selectedImagePath = "";

        if (getIntent().getBooleanExtra(Constants.IS_VIEW_OR_UPDATE, false)) {
            availableNote = (Note) getIntent().getSerializableExtra(Constants.KEY_NOTE);
            setViewOrUpdateNote();
        }

    }

    private void setListeners() {
        imageBack.setOnClickListener(v -> onBackPressed());

        //SaveNote
        imageSave.setOnClickListener(v -> saveNote());

        //RemoveURL
        removeWebUrl.setOnClickListener(v -> {
            textWebURL.setText(null);
            layoutWebURL.setVisibility(View.GONE);
        });

        //RemoveImageNote
        removeImageNote.setOnClickListener(v -> {
            imageNote.setImageBitmap(null);
            imageNote.setVisibility(View.GONE);
            removeImageNote.setVisibility(View.GONE);
            selectedImagePath = "";
        });
    }


    private void setViewOrUpdateNote() {
        inputNoteTitle.setText(availableNote.getTitle());
        textDateTime.setText(availableNote.getDateTime());

        if (availableNote.getSubtitle().equals(" ")) {
            inputNoteSubtitle.setHint("subtitle");
        } else {
            inputNoteSubtitle.setText(availableNote.getSubtitle());
        }

        if (availableNote.getNoteText().equals(" ")) {
            inputNoteText.setHint("Your Note Here...");
        } else {
            inputNoteText.setText(availableNote.getNoteText());
        }

        if (availableNote.getImagePath() != null && !availableNote.getImagePath().trim().isEmpty()) {
            imageNote.setImageBitmap(BitmapFactory.decodeFile(availableNote.getImagePath()));
            imageNote.setVisibility(View.VISIBLE);
            removeImageNote.setVisibility(View.VISIBLE);
            selectedImagePath = availableNote.getImagePath();
        }

        if (availableNote.getWebLink() != null && !availableNote.getWebLink().trim().isEmpty()) {
            textWebURL.setText(availableNote.getWebLink());
            layoutWebURL.setVisibility(View.VISIBLE);
        }
    }

    private void saveNote() {
        String noteTitle = inputNoteTitle.getText().toString();
        String noteSubtitle = inputNoteSubtitle.getText().toString();
        String noteText = inputNoteText.getText().toString();
        String noteDateTime = textDateTime.getText().toString();

        if (noteTitle.isEmpty()) {
            int randomSuffix = new Random().nextInt(500);
            noteTitle = "note" + randomSuffix;
        }
        if (noteSubtitle.isEmpty()) {
            noteSubtitle = " ";
        }

        if (noteText.isEmpty()) {
            noteText = " ";
        }


        final Note note = new Note();
        note.setTitle(noteTitle);
        note.setSubtitle(noteSubtitle);
        note.setNoteText(noteText);
        note.setDateTime(noteDateTime);
        note.setColor(selectedNoteColor);
        note.setImagePath(selectedImagePath);
        if (layoutWebURL.getVisibility() == View.VISIBLE) {
            note.setWebLink(textWebURL.getText().toString());
        }


        if (availableNote != null) {
            note.setId(availableNote.getId());
        }

        @SuppressLint("StaticFieldLeak")
        class SaveNoteTask extends AsyncTask<Void, Void, Void> {
            @Override
            protected Void doInBackground(Void... voids) {
                NoteDatabase.getNoteDatabase(getApplicationContext()).noteDao().insertNote(note);
                return null;
            }

            @Override
            protected void onPostExecute(Void unused) {
                super.onPostExecute(unused);
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
            }
        }
        new SaveNoteTask().execute();
    }


    private void initialiseMiscellaneous() {
        final LinearLayout layoutMiscellaneous = findViewById(R.id.layoutMiscellaneous);
        bottomSheetBehavior = BottomSheetBehavior.from(layoutMiscellaneous);
        layoutMiscellaneous.findViewById(R.id.textMiscellaneous).setOnClickListener(v -> {
            if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });


        final ImageView theme1 = layoutMiscellaneous.findViewById(R.id.imageColor1);
        final ImageView theme2 = layoutMiscellaneous.findViewById(R.id.imageColor2);
        final ImageView theme3 = layoutMiscellaneous.findViewById(R.id.imageColor3);
        final ImageView theme4 = layoutMiscellaneous.findViewById(R.id.imageColor4);
        final ImageView theme5 = layoutMiscellaneous.findViewById(R.id.imageColor5);

        layoutMiscellaneous.findViewById(R.id.viewColor1).setOnClickListener(v -> {
            selectedNoteColor = "#333333";
            theme1.setImageResource(R.drawable.ic_done);
            theme2.setImageResource(0);
            theme3.setImageResource(0);
            theme4.setImageResource(0);
            theme5.setImageResource(0);
            setSubtitleIndicatorColor();
        });

        layoutMiscellaneous.findViewById(R.id.viewColor2).setOnClickListener(v -> {
            selectedNoteColor = "#DAA026";
            theme1.setImageResource(0);
            theme2.setImageResource(R.drawable.ic_done);
            theme3.setImageResource(0);
            theme4.setImageResource(0);
            theme5.setImageResource(0);
            setSubtitleIndicatorColor();
        });

        layoutMiscellaneous.findViewById(R.id.viewColor3).setOnClickListener(v -> {
            selectedNoteColor = "#7C0A06";
            theme1.setImageResource(0);
            theme2.setImageResource(0);
            theme3.setImageResource(R.drawable.ic_done);
            theme4.setImageResource(0);
            theme5.setImageResource(0);
            setSubtitleIndicatorColor();
        });

        layoutMiscellaneous.findViewById(R.id.viewColor4).setOnClickListener(v -> {
            selectedNoteColor = "#3A52Fc";
            theme1.setImageResource(0);
            theme2.setImageResource(0);
            theme3.setImageResource(0);
            theme4.setImageResource(R.drawable.ic_done);
            theme5.setImageResource(0);
            setSubtitleIndicatorColor();
        });

        layoutMiscellaneous.findViewById(R.id.viewColor5).setOnClickListener(v -> {
            selectedNoteColor = "#000000";
            theme1.setImageResource(0);
            theme2.setImageResource(0);
            theme3.setImageResource(0);
            theme4.setImageResource(0);
            theme5.setImageResource(R.drawable.ic_done);
            setSubtitleIndicatorColor();
        });


        if (availableNote != null && availableNote.getColor() != null && !availableNote.getColor().trim().isEmpty()) {
            switch (availableNote.getColor()) {
                case "#DAA026":
                    layoutMiscellaneous.findViewById(R.id.viewColor2).performClick();
                    break;
                case "#7C0A06":
                    layoutMiscellaneous.findViewById(R.id.viewColor3).performClick();
                    break;
                case "#3A52Fc":
                    layoutMiscellaneous.findViewById(R.id.viewColor4).performClick();
                    break;
                case "#000000":
                    layoutMiscellaneous.findViewById(R.id.viewColor5).performClick();
                    break;
            }
        }


        //Add Image
        layoutMiscellaneous.findViewById(R.id.layoutAddImage).setOnClickListener(v -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            if (ContextCompat.checkSelfPermission(
                    getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        CreateNoteActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        Constants.REQUEST_CODE_STORAGE_PERMISSION
                );
            } else {
                selectImage();
            }
        });


        //Add Web Url
        layoutMiscellaneous.findViewById(R.id.layoutAddUrl).setOnClickListener(v -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            showAddUrlDialog();
        });

        //Delete Note
        if (availableNote != null) {
            layoutMiscellaneous.findViewById(R.id.layoutDeleteNote).setVisibility(View.VISIBLE);
            layoutMiscellaneous.findViewById(R.id.layoutDeleteNote).setOnClickListener(v -> {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                showDeleteNoteDialog();
            });
        }
    }

    private void showDeleteNoteDialog() {
        if (dialogDeleteNote == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(CreateNoteActivity.this);
            View view = LayoutInflater.from(this).inflate(
                    R.layout.layout_delete_note,
                    findViewById(R.id.layoutDeleteNoteContainer)
            );
            builder.setView(view);
            dialogDeleteNote = builder.create();
            if (dialogDeleteNote.getWindow() != null) {
                dialogDeleteNote.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }
            view.findViewById(R.id.textDeleteNote).setOnClickListener(v -> {

                @SuppressLint("StaticFieldLeak")
                class DeleteNoteTask extends AsyncTask<Void, Void, Void> {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        NoteDatabase.getNoteDatabase(getApplicationContext()).noteDao()
                                .deleteNote(availableNote);
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void unused) {
                        super.onPostExecute(unused);
                        Intent intent = new Intent();
                        intent.putExtra(Constants.IS_NOTE_DELETED, true);
                        setResult(RESULT_OK, intent);
                        availableNote = null;
                        dialogDeleteNote.dismiss();
                        finish();
                    }
                }

                new DeleteNoteTask().execute();
            });

            view.findViewById(R.id.textCancel).setOnClickListener(v -> dialogDeleteNote.dismiss());
        }

        dialogDeleteNote.show();
    }

    private void setSubtitleIndicatorColor() {
        GradientDrawable gradientDrawable = (GradientDrawable) subtitleIndicator.getBackground();
        gradientDrawable.setColor(Color.parseColor(selectedNoteColor));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Constants.REQUEST_CODE_STORAGE_PERMISSION && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectImage();
            } else {
                toaster.showToast("Permission Denied");
            }
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_IMAGE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        imageNote.setImageBitmap(bitmap);
                        imageNote.setVisibility(View.VISIBLE);
                        removeImageNote.setVisibility(View.VISIBLE);

                        selectedImagePath = getPathFromUri(selectedImageUri);
                    } catch (Exception e) {
                        toaster.showToast(e.getMessage());
                    }
                }
            }
        }
    }

    private String getPathFromUri(Uri contentUri) {
        String filePath;
        Cursor cursor = getContentResolver()
                .query(contentUri, null, null, null, null);
        if (cursor == null) {
            filePath = contentUri.getPath();
        } else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex("_data");
            filePath = cursor.getString(index);
            cursor.close();
        }

        return filePath;
    }

    private void showAddUrlDialog() {
        if (dialogAddUrl == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(CreateNoteActivity.this);
            View view = LayoutInflater.from(this).inflate(
                    R.layout.layout_add_url,
                    findViewById(R.id.layoutAddUrlContainer)
            );

            builder.setView(view);
            dialogAddUrl = builder.create();

            if (dialogAddUrl.getWindow() != null) {
                dialogAddUrl.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            final EditText inputURL = view.findViewById(R.id.inputURL);
            inputURL.requestFocus();

            view.findViewById(R.id.textAdd).setOnClickListener(v -> {
                if (inputURL.getText().toString().isEmpty()) {
                    toaster.showToast("Enter URL");
                } else if (!Patterns.WEB_URL.matcher(inputURL.getText().toString()).matches()) {
                    toaster.showToast("Please Enter a valid URL");
                } else {
                    textWebURL.setText(inputURL.getText().toString());
                    layoutWebURL.setVisibility(View.VISIBLE);
                    dialogAddUrl.dismiss();
                }
            });

            view.findViewById(R.id.textCancel).setOnClickListener(v -> dialogAddUrl.dismiss());

            dialogAddUrl.show();
        }
    }


    @Override
    public void onBackPressed() {
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else {
            if (!inputNoteText.getText().toString().trim().isEmpty() || !inputNoteTitle.getText().toString().trim().isEmpty()
                    || !inputNoteSubtitle.getText().toString().trim().isEmpty() || !selectedImagePath.equals("")) {
                saveNote();
            } else {
                super.onBackPressed();
            }
        }
    }
}