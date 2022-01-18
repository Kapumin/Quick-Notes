package com.abjt.melnotes.activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;

import com.abjt.melnotes.R;
import com.abjt.melnotes.adapters.NotesAdapter;
import com.abjt.melnotes.database.NoteDatabase;
import com.abjt.melnotes.entities.Note;
import com.abjt.melnotes.listeners.NotesListener;
import com.abjt.melnotes.utilities.Constants;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NotesListener {


    private ImageView imageAddNoteMain;
    private EditText inputSearch;

    private RecyclerView notesRecView;
    private List<Note> noteList;
    private NotesAdapter notesAdapter;

    private int noteClickedPosition = -1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SplashScreen.installSplashScreen(MainActivity.this);
        setContentView(R.layout.activity_main);

        init();
        //GetNotes
        getNotes(Constants.REQUEST_CODE_SHOW_NOTES, false);
        setListeners();


    }

    private void init() {
        imageAddNoteMain = findViewById(R.id.imageAddNoteMain);
        notesRecView = findViewById(R.id.notesRecView);
        inputSearch = findViewById(R.id.inputSearch);


        notesRecView.setLayoutManager(
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        );
        noteList = new ArrayList<>();
        notesAdapter = new NotesAdapter(noteList, this);
        notesRecView.setAdapter(notesAdapter);

    }

    private void setListeners() {
        imageAddNoteMain.setOnClickListener(v -> startActivityForResult(
                new Intent(getApplicationContext(), CreateNoteActivity.class),
                Constants.REQUEST_CODE_ADD_NOTE
        ));

        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                notesAdapter.cancelTimer();
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (noteList.size() != 0) {
                    notesAdapter.searchNote(s.toString());
                }
            }
        });
    }


    private void getNotes(final int requestCode, final Boolean isNoteDeleted) {
        @SuppressLint("StaticFieldLeak")
        class GetNotesTask extends AsyncTask<Void, Void, List<Note>> {
            @Override
            protected List<Note> doInBackground(Void... voids) {

                return NoteDatabase.getNoteDatabase(getApplicationContext())
                        .noteDao().getAllNotes();
            }

            @Override
            protected void onPostExecute(List<Note> notes) {
                super.onPostExecute(notes);

                if (requestCode == Constants.REQUEST_CODE_SHOW_NOTES) {
                    noteList.addAll(notes);
                    notesAdapter.notifyItemRangeInserted(0, notes.size());
                } else if (requestCode == Constants.REQUEST_CODE_ADD_NOTE) {
                    noteList.add(0, notes.get(0));
                    notesAdapter.notifyItemInserted(0);
                    notesRecView.smoothScrollToPosition(0);
                } else if (requestCode == Constants.REQUEST_CODE_UPDATE_NOTE) {
                    noteList.remove(noteClickedPosition);
                    if (isNoteDeleted) {
                        notesAdapter.notifyItemRemoved(noteClickedPosition);
                    } else {
                        noteList.add(noteClickedPosition, notes.get(noteClickedPosition));
                        notesAdapter.notifyItemChanged(noteClickedPosition);
                    }
                }
            }
        }
        new GetNotesTask().execute();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_CODE_ADD_NOTE && resultCode == RESULT_OK) {
            getNotes(Constants.REQUEST_CODE_ADD_NOTE, false);
        } else if (requestCode == Constants.REQUEST_CODE_UPDATE_NOTE && resultCode == RESULT_OK) {
            if (data != null) {
                getNotes(Constants.REQUEST_CODE_UPDATE_NOTE, data.getBooleanExtra(Constants.IS_NOTE_DELETED, false));
            }
        }
    }

    @Override
    public void OnNoteClicked(Note note, int position) {
        noteClickedPosition = position;
        Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
        intent.putExtra(Constants.IS_VIEW_OR_UPDATE, true);
        intent.putExtra(Constants.KEY_NOTE, note);
        startActivityForResult(intent, Constants.REQUEST_CODE_UPDATE_NOTE);
    }
}